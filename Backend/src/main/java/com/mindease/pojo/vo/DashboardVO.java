package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class DashboardVO {

    private MoodSummaryVO moodSummary;

    private List<UpcomingAppointmentVO> upcomingAppointments;

    private Integer unreadNotifications;
}

