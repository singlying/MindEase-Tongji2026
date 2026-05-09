package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpcomingAppointmentVO {

    private Long id;

    private String time;

    private String counselor;
}

