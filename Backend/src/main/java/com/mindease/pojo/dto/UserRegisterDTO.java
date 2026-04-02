package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {

    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String role;
}

