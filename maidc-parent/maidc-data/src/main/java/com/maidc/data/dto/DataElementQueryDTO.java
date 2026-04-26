package com.maidc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementQueryDTO {
    private String category;
    private String registrationStatus;
    private String keyword;
    private String dataType;
    private Integer page;
    private Integer pageSize;
}
