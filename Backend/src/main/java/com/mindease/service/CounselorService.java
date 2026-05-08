package com.mindease.service;

import com.mindease.pojo.dto.ReviewSubmitDTO;
import com.mindease.pojo.vo.*;

public interface CounselorService {

    /**
     * 智能推荐咨询师
     *
     * @param userId 当前用户ID
     * @param keyword 搜索关键词
     * @param sort 排序方式
     * @return
     */
    RecommendResultVO recommendCounselors(Long userId, String keyword, String sort);

    /**
     * 检查推荐前置状态
     *
     * @param userId
     * @return
     */
    RecommendStatusVO getRecommendStatus(Long userId);

    /**
     * 获取咨询师详情
     *
     * @param counselorId
     * @return
     */
    CounselorDetailVO getCounselorDetail(Long counselorId);

    /**
     * 获取咨询师评价列表
     *
     * @param counselorId
     * @param limit
     * @param offset
     * @return
     */
    ReviewListVO getCounselorReviews(Long counselorId, Integer limit, Integer offset);

    /**
     * 提交评价
     *
     * @param userId
     * @param reviewSubmitDTO
     * @return 评价ID
     */
    Long submitReview(Long userId, ReviewSubmitDTO reviewSubmitDTO);
}

