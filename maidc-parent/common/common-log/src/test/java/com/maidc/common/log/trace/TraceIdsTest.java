package com.maidc.common.log.trace;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdsTest {

    private static final Pattern HEX32 = Pattern.compile("^[0-9a-f]{32}$");

    @Test
    void generateShouldReturn32LowercaseHex() {
        for (int i = 0; i < 100; i++) {
            assertThat(TraceIds.generate()).matches(HEX32);
        }
    }

    @Test
    void normalizeShouldStripDashesAndLowercase() {
        String dashed = "550E8400-E29B-41D4-A716-446655440000";
        assertThat(TraceIds.normalize(dashed)).isEqualTo("550e8400e29b41d4a716446655440000");
    }

    @Test
    void normalizeShouldRejectBlankTooLongOrNonHex() {
        assertThat(TraceIds.normalize(null)).isNull();
        assertThat(TraceIds.normalize("  ")).isNull();
        assertThat(TraceIds.normalize("g".repeat(32))).isNull();
        assertThat(TraceIds.normalize("a".repeat(65))).isNull();
    }

    @Test
    void isValidShouldAcceptGeneratedAndDashedUuid() {
        assertThat(TraceIds.isValid(TraceIds.generate())).isTrue();
        assertThat(TraceIds.isValid(UUID.randomUUID().toString())).isTrue();
        assertThat(TraceIds.isValid("eval-123")).isFalse();
    }

    @Test
    void resolveShouldNormalizeValidInputElseGenerate() {
        assertThat(TraceIds.resolve("550E8400-E29B-41D4-A716-446655440000"))
                .isEqualTo("550e8400e29b41d4a716446655440000");
        assertThat(TraceIds.resolve("!!!")).matches(HEX32);
        assertThat(TraceIds.resolve(null)).matches(HEX32);
    }
}
