package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReviewSubmitDTO implements Serializable {

    private Long appointmentId;

    private Integer rating;

    private String content;

    private Boolean isAnonymous;
}

