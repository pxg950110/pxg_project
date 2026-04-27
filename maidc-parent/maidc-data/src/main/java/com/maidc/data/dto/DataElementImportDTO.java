package com.maidc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementImportDTO {
    private String elementCode;
    private String name;
    private String nameEn;
    private String definition;
    private String objectClassName;
    private String objectClassId;
    private String propertyName;
    private String propertyId;
    private String dataType;
    private String representationClass;
    private String valueDomainName;
    private String valueDomainId;
    private Integer minLength;
    private Integer maxLength;
    private String format;
    private String unitOfMeasure;
    private String category;
    private String standardSource;
    private String registrationStatus;
    private String version;
    /** Row number in Excel (1-based) for error reporting */
    private int rowNumber;
}
