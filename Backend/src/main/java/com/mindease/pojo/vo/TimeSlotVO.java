package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TimeSlotVO {

    private String startTime;

    private String endTime;

    private Boolean available;
}

