package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingExamVO {
    private Long id;
    private Long encounterId;
    private String accessionNo;
    private String examType;
    private String bodyPart;
    private LocalDateTime studyDate;
    private String modality;
    private String status;
    private String reportText;
}
