package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AppointmentCreateVO {

    private Long appointmentId;

    private String status;
}

