package com.maidc.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    private String realName;

    private String email;

    private String phone;

    private List<Long> roleIds;

    private Long orgId;

    private String status;
}
