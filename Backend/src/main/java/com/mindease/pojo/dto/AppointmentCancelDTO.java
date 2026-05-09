package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppointmentCancelDTO implements Serializable {

    private String cancelReason;
}

