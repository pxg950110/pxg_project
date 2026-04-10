package com.maidc.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String status;
    private List<String> roleNames;
    private Long orgId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Boolean mustChangePwd;
}
