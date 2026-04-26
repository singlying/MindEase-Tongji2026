package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserLoginVO {

    private Long userId;

    private String username;

    private String nickname;

    private String token;

    private String role;
}
