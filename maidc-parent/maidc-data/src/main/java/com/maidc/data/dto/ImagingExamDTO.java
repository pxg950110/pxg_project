package com.maidc.data.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 影像检查DTO
 */
@Data
public class ImagingExamDTO {
    private Long id;
    private String examNo;
    private String examType; // CT/MRI/ULTRASOUND/XRAY
    private String bodyPart;
    private String modality;
    private LocalDateTime examTime;
    private LocalDateTime reportTime;
    private String status;
    private String performingDoctor;
    private String reportDoctor;
    private String findings;
    private String conclusion;
}
