package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class MoodLogListVO {
    private Long total;
    private List<MoodLogItemVO> logs;
}