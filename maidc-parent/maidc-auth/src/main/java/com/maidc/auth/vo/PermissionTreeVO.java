package com.maidc.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionTreeVO {

    private Long id;
    private String code;
    private String name;
    private String resourceType;
    private String description;
    private List<PermissionTreeVO> children;
}
