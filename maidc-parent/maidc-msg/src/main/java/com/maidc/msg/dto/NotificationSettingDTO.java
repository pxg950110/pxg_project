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
public class NotificationSettingDTO {

    @NotBlank(message = "通知渠道不能为空")
    private String channel;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @Builder.Default
    private Boolean enabled = true;
}
