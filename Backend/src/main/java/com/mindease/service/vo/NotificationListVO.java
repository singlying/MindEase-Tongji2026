package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 通知列表VO
 */
@Data
@Builder
public class NotificationListVO {
    private Long total;
    private List<NotificationItemVO> items;
    private Integer page;
    private Integer pageSize;
}
