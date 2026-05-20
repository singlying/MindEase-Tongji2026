package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserRegisterVO {

    private Long userId;

    private String role;

    private Integer status;

    private String token;
}

