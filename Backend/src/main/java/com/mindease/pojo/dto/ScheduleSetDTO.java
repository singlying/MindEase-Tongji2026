package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ScheduleSetDTO implements Serializable {

    private List<Integer> workDays;

    private List<WorkHour> workHours;

    @Data
    public static class WorkHour {
        private String start;
        private String end;
    }
}

