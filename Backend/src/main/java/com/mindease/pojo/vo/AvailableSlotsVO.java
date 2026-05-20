package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AvailableSlotsVO {

    private String date;

    private List<TimeSlotVO> slots;
}

