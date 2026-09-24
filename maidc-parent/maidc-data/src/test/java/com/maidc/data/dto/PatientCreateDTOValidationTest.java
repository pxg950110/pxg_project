package com.maidc.data.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 患者创建入参校验：gender/id_card_no 为 DDL NOT NULL 列，缺失应 400（bean validation）而非 500（库约束）
 */
class PatientCreateDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void missingGenderAndIdCardProduceViolations() {
        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setName("张三");

        Set<ConstraintViolation<PatientCreateDTO>> violations = validator.validate(dto);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("gender")),
                "gender 缺 @NotBlank");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idCardNo")),
                "idCardNo 缺 @NotBlank");
    }

    @Test
    void completePayloadPassesValidation() {
        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setName("张三");
        dto.setGender("M");
        dto.setIdCardNo("320102199001011234");

        Set<ConstraintViolation<PatientCreateDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), () -> "完整入参不应有违例: " + violations);
    }
}
