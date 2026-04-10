package com.maidc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCompareVO {

    private VersionVO v1;

    private VersionVO v2;

    private Map<String, Object> diff;
}
