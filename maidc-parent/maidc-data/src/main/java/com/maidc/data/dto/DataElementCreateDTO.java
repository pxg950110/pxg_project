package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementCreateDTO {

    @NotBlank(message = "标识符不能为空")
    @Size(max = 64)
    private String elementCode;

    @NotBlank(message = "名称不能为空")
    @Size(max = 256)
    private String name;

    @Size(max = 256)
    private String nameEn;

    @NotBlank(message = "定义不能为空")
    private String definition;

    private String objectClassName;
    private String objectClassId;
    private String propertyName;
    private String propertyId;

    @NotBlank(message = "数据类型不能为空")
    @Size(max = 32)
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
    private String[] synonyms;
    private String[] keywords;
    private String extraAttrs;
}
