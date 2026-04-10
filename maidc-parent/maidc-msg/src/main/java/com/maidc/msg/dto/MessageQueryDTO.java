package com.maidc.msg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueryDTO {

    private Long userId;

    private String type;

    private Boolean isRead;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
