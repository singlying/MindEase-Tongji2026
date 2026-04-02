package com.mindease.controller.counselor;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.ReviewSubmitDTO;
import com.mindease.pojo.vo.*;
import com.mindease.service.CounselorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 咨询师推荐控制器
 */
@RestController
@RequestMapping("/counselor")
@Slf4j
public class CounselorController {

    @Autowired
    private CounselorService counselorService;

    /**
     * 智能推荐咨询师
     *
     * @param userId 当前用户ID（从token中获取）
     * @param keyword 搜索关键词（可选）
     * @param sort 排序方式（可选）: smart, price_asc, rating_desc
     * @return
     */
    @GetMapping("/recommend")
    public Result<RecommendResultVO> recommendCounselors(@RequestAttribute Long userId,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false, defaultValue = "smart") String sort) {
        log.info("推荐咨询师，用户ID:{}，关键词:{}，排序:{}", userId, keyword, sort);

        RecommendResultVO result = counselorService.recommendCounselors(userId, keyword, sort);
        return Result.success(result);
    }

    /**
     * 检查推荐前置状态
     *
     * @param userId
     * @return
     */
    @GetMapping("/recommend/status")
    public Result<RecommendStatusVO> getRecommendStatus(@RequestAttribute Long userId) {
        log.info("检查推荐状态，用户ID:{}", userId);

        RecommendStatusVO status = counselorService.getRecommendStatus(userId);
        return Result.success(status);
    }

    /**
     * 获取咨询师详情
     *
     * @param id 咨询师ID
     * @return
     */
    @GetMapping("/{id}")
    public Result<CounselorDetailVO> getCounselorDetail(@PathVariable Long id) {
        log.info("获取咨询师详情，ID:{}", id);

        CounselorDetailVO detail = counselorService.getCounselorDetail(id);
        return Result.success(detail);
    }

    /**
     * 获取咨询师评价列表
     *
     * @param id 咨询师ID
     * @param limit 每页数量
     * @param offset 偏移量
     * @return
     */
    @GetMapping("/{id}/reviews")
    public Result<ReviewListVO> getCounselorReviews(@PathVariable Long id,
                                                     @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                     @RequestParam(required = false, defaultValue = "0") Integer offset) {
        log.info("获取咨询师评价，ID:{}，limit:{}，offset:{}", id, limit, offset);

        ReviewListVO reviews = counselorService.getCounselorReviews(id, limit, offset);
        return Result.success(reviews);
    }

    /**
     * 提交评价
     *
     * @param userId 当前用户ID（从token中获取）
     * @param reviewSubmitDTO 评价内容
     * @return
     */
    @PostMapping("/review")
    public Result<Map<String, Object>> submitReview(@RequestAttribute Long userId,
                                                     @RequestBody ReviewSubmitDTO reviewSubmitDTO) {
        log.info("提交评价，用户ID:{}，内容:{}", userId, reviewSubmitDTO);

        Long reviewId = counselorService.submitReview(userId, reviewSubmitDTO);

        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", reviewId);

        return Result.success(data, "评价提交成功");
    }
}

