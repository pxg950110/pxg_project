package com.maidc.common.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaidcMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String eventType;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;
    private String source;

    public static MaidcMessage of(String eventType, Map<String, Object> payload, String source) {
        return MaidcMessage.builder()
                .eventType(eventType)
                .payload(payload)
                .source(source)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
