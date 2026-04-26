package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RecommendContextVO {

    private String strategy;

    private String basedOn;

    private List<String> userTags;
}

