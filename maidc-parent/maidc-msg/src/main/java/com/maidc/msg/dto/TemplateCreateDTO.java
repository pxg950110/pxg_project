package com.maidc.msg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreateDTO {

    @NotBlank(message = "模板编码不能为空")
    private String code;

    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    private String channel;

    private String eventType;
}
