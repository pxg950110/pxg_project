package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 手术DTO
 */
@Data
public class OperationDTO {
    private Long id;
    private String operationNo;
    private String operationName;
    private String operationCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String surgeonCode;
    private String surgeonName;
    private String assistantDoctor;
    private String anesthesiaMethod;
    private String anesthesiaDoctor;
    private String operatingRoom;
    private String status;
}
