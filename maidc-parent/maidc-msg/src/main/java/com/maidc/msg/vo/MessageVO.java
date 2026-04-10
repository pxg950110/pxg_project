package com.maidc.msg.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String type;

    private Boolean isRead;

    private Long bizId;

    private String bizType;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
