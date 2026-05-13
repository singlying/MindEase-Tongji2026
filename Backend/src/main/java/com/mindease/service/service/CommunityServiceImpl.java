package com.mindease.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.CommunityPostMapper;
import com.mindease.mapper.CommunityCommentMapper;
import com.mindease.mapper.CommunityLikeMapper;
import com.mindease.pojo.dto.CommunityPostDTO;
import com.mindease.pojo.entity.CommunityPost;
import com.mindease.pojo.entity.CommunityComment;
import com.mindease.pojo.entity.CommunityLike;
import com.mindease.pojo.vo.*;
import com.mindease.service.CommunityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 心理社区服务实现类
 */
@Service
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private static final String COMMUNITY_LIKE_KEY = "mindease:community:like:";
    private static final String COMMUNITY_VIEW_KEY = "mindease:community:view:";
    private static final String COMMUNITY_HOT_TOPICS_KEY = "mindease:community:hot_topics";
    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
            "自杀", "自残", "暴力", "色情", "赌博", "毒品"
    );

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private CommunityCommentMapper communityCommentMapper;

    @Autowired
    private CommunityLikeMapper communityLikeMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CommunityPostVO createPost(CommunityPostDTO postDTO, Long userId) {
        log.info("用户 {} 发布社区帖子", userId);

        // 敏感词检测
        checkSensitiveContent(postDTO.getContent());
        if (postDTO.getTitle() != null) {
            checkSensitiveContent(postDTO.getTitle());
        }

        // 内容长度限制检查
        if (postDTO.getContent() != null && postDTO.getContent().length() > 10000) {
            throw new BaseException("帖子内容不能超过10000字");
        }

        // 构建实体
        CommunityPost post = new CommunityPost();
        BeanUtils.copyProperties(postDTO, post);
        post.setUserId(userId);
        post.setAnonymous(postDTO.getIsAnonymous() != null ? postDTO.getIsAnonymous() : false);
        post.setViewCount(0L);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setCommentCount(0);
        post.setIsPinned(false);
        // 新用户发布的前3篇帖子需要审核
        boolean needsReview = isFirstTimePoster(userId);
        post.setStatus(needsReview ? "PENDING_REVIEW" : "PUBLISHED");
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        // 处理封面图
        if (postDTO.getCoverImage() != null && !postDTO.getCoverImage().isEmpty()) {
            post.setCoverImage(postDTO.getCoverImage());
        }

        // 处理标签JSON
        if (postDTO.getTags() != null && !postDTO.getTags().isEmpty()) {
            try {
                post.setTagsJson(objectMapper.writeValueAsString(postDTO.getTags()));
            } catch (JsonProcessingException e) {
                log.error("标签序列化失败", e);
                post.setTagsJson("[]");
            }
        } else {
            post.setTagsJson("[]");
        }

        communityPostMapper.insert(post);

        // 更新热门话题缓存
        refreshHotTopicsCache(postDTO.getTags());

        log.info("帖子发布成功, ID: {}, 状态: {}", post.getId(), post.getStatus());

        return convertToPostVO(post);
    }

    @Override
    public CommunityPostListVO getPostList(String topic, String sortType,
                                           Integer page, Integer pageSize) {
        log.info("获取社区帖子列表, topic={}, sortType={}, page={}", topic, sortType, page);

        int offset = (page - 1) * pageSize;

        Long total = communityPostMapper.countByCondition(topic, "PUBLISHED");
        List<CommunityPost> posts = communityPostMapper.selectByCondition(
                topic, sortType != null ? sortType : "latest",
                "PUBLISHED", pageSize, offset
        );

        List<CommunityPostItemVO> items = posts.stream()
                .map(this::convertToPostItemVO)
                .collect(Collectors.toList());

        return CommunityPostListVO.builder()
                .total(total)
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public CommunityPostDetailVO getPostDetail(Long postId, Long currentUserId) {
        log.info("获取帖子详情, postId={}", postId);

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BaseException("帖子不存在或已删除");
        }

        // 异步增加浏览量（Redis + 延迟落库）
        incrementViewCount(postId);

        // 获取评论列表
        List<CommunityComment> comments = communityCommentMapper.selectByPostId(postId);
        List<CommunityCommentItemVO> commentItems = comments.stream()
                .map(this::convertToCommentItemVO)
                .collect(Collectors.toList());

        // 解析标签
        List<String> tags = parseTagsJson(post.getTagsJson());

        // 查询当前用户与该帖子的交互状态（是否点赞、收藏）
        boolean isLiked = false;
        boolean isCollected = false;
        if (currentUserId != null) {
            isLiked = communityLikeMapper.existsByUserAndPost(currentUserId, postId) > 0;
            isCollected = communityLikeMapper.existsCollectByUserAndPost(currentUserId, postId) > 0;
        }

        return CommunityPostDetailVO.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .tags(tags)
                .authorName(post.getIsAnonymous() ? "匿名用户" : post.getAuthorNickname())
                .viewCount(getViewCountFromCache(postId))
                .likeCount(post.getLikeCount())
                .collectCount(post.getCollectCount())
                .isPinned(post.getIsPinned())
                .isCollected(isCollected)
                .isLiked(isLiked)
                .commentCount(post.getCommentCount())
                .createTime(post.getCreateTime())
                .comments(commentItems)
                .build();
    }

    @Override
    public CommunityLikeVO toggleLike(Long postId, Long userId) {
        log.info("切换点赞状态, postId={}, userId={}", postId, userId);

        String redisKey = COMMUNITY_LIKE_KEY + postId + ":" + userId;

        // 检查是否已点赞（先查Redis，没有则查DB）
        Boolean isLiked = stringRedisTemplate.hasKey(redisKey);
        if (isLiked == null || !isLiked) {
            isLiked = communityLikeMapper.existsByUserAndPost(userId, postId) > 0;
        }

        if (isLiked) {
            // 取消点赞
            communityLikeMapper.deleteByUserAndPost(userId, postId);
            communityPostMapper.decrementLikeCount(postId);
            stringRedisTemplate.delete(redisKey);
            log.info("取消点赞成功, postId={}, userId={}", postId, userId);
        } else {
            // 新增点赞
            CommunityLike likeRecord = new CommunityLike();
            likeRecord.setUserId(userId);
            likeRecord.setTargetId(postId);
            likeRecord.setTargetType("POST");
            likeRecord.setCreateTime(LocalDateTime.now());
            communityLikeMapper.insert(likeRecord);
            communityPostMapper.incrementLikeCount(postId);
            // Redis缓存点赞状态，7天过期
            stringRedisTemplate.opsForValue().set(redisKey, "liked", 7, TimeUnit.DAYS);
            log.info("点赞成功, postId={}, userId={}", postId, userId);
        }

        CommunityPost updated = communityPostMapper.selectById(postId);
        return CommunityLikeVO.builder()
                .postId(postId)
                .isLiked(!isLiked)
                .likeCount(updated.getLikeCount())
                .build();
    }

    @Override
    public CommunityCommentVO addComment(Long postId, Long userId, String content) {
        log.info("添加评论, postId={}, userId={}", postId, userId);

        // 验证帖子存在
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BaseException("目标帖子不存在");
        }

        // 敏感词检测
        checkSensitiveContent(content);

        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setParentId(null);  // 顶层评论
        comment.setStatus("NORMAL");
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());

        communityCommentMapper.insert(comment);
        communityPostMapper.incrementCommentCount(postId);

        log.info("评论发布成功, commentId={}", comment.getId());

        return CommunityCommentVO.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .createTime(comment.getCreateTime())
                .build();
    }

    @Override
    public Boolean deleteContent(Long targetId, Long userId, String type) {
        log.info("删除内容, targetId={}, userId={}, type={}", targetId, userId, type);

        if ("post".equalsIgnoreCase(type)) {
            CommunityPost post = communityPostMapper.selectById(targetId);
            if (post == null) {
                throw new BaseException("帖子不存在");
            }
            if (!post.getUserId().equals(userId)) {
                throw new BaseException("无权删除他人的帖子");
            }
            communityPostMapper.updateStatus(targetId, "DELETED");
        } else if ("comment".equalsIgnoreCase(type)) {
            CommunityComment comment = communityCommentMapper.selectById(targetId);
            if (comment == null) {
                throw new BaseException("评论不存在");
            }
            if (!comment.getUserId().equals(userId)) {
                throw new BaseException("无权删除他人的评论");
            }
            communityCommentMapper.updateStatus(targetId, "DELETED");
            communityPostMapper.decrementCommentCount(comment.getPostId());
        } else {
            throw new BaseException("无效的内容类型: " + type);
        }

        log.info("内容删除成功");
        return true;
    }

    @Override
    public CommunityLikeVO toggleCollect(Long postId, Long userId) {
        log.info("切换收藏状态, postId={}, userId={}", postId, userId);

        String redisKey = COMMUNITY_LIKE_KEY + "collect:" + postId + ":" + userId;
        Boolean isCollected = stringRedisTemplate.hasKey(redisKey);
        if (isCollected == null || !isCollected) {
            isCollected = communityLikeMapper.existsCollectByUserAndPost(userId, postId) > 0;
        }

        if (isCollected) {
            communityLikeMapper.deleteCollectByUserAndPost(userId, postId);
            communityPostMapper.decrementCollectCount(postId);
            stringRedisTemplate.delete(redisKey);
            log.info("取消收藏成功, postId={}, userId={}", postId, userId);
        } else {
            CommunityLike collectRecord = new CommunityLike();
            collectRecord.setUserId(userId);
            collectRecord.setTargetId(postId);
            collectRecord.setTargetType("COLLECT");
            collectRecord.setCreateTime(LocalDateTime.now());
            communityLikeMapper.insert(collectRecord);
            communityPostMapper.incrementCollectCount(postId);
            stringRedisTemplate.opsForValue().set(redisKey, "collected", 30, TimeUnit.DAYS);
            log.info("收藏成功, postId={}, userId={}", postId, userId);
        }

        CommunityPost updated = communityPostMapper.selectById(postId);
        return CommunityLikeVO.builder()
                .postId(postId)
                .isLiked(!isCollected)
                .likeCount(updated.getCollectCount())
                .build();
    }

    @Override
    public Boolean togglePinPost(Long postId, Long userId) {
        log.info("切换置顶状态, postId={}, userId={}", postId);

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BaseException("帖子不存在或已删除");
        }

        // 仅管理员或作者本人可以置顶（此处简化为仅作者）
        if (!post.getUserId().equals(userId)) {
            throw new BaseException("只有作者才能置顶自己的帖子");
        }

        boolean newPinStatus = !Boolean.TRUE.equals(post.getIsPinned());
        communityPostMapper.togglePin(postId, newPinStatus);

        log.info("置顶状态更新为: {}", newPinStatus);
        return newPinStatus;
    }

    @Override
    public Boolean reportContent(Long targetId, Long userId, String type, String reason) {
        log.info("举报内容, targetId={}, userId={}, type={}, reason={}", targetId, userId, type, reason);

        if (reason == null || reason.trim().isEmpty()) {
            throw new BaseException("请提供举报原因");
        }
        if (reason.length() > 500) {
            throw new BaseException("举报原因不能超过500字");
        }

        // 验证目标内容存在
        if ("post".equalsIgnoreCase(type)) {
            CommunityPost post = communityPostMapper.selectById(targetId);
            if (post == null) {
                throw new BaseException("被举报的帖子不存在");
            }
        } else if ("comment".equalsIgnoreCase(type)) {
            CommunityComment comment = communityCommentMapper.selectById(targetId);
            if (comment == null) {
                throw new BaseException("被举报的评论不存在");
            }
        } else {
            throw new BaseException("无效的举报类型");
        }

        // 记录举报信息到Redis有序集合，按时间排序供管理员审核
        String reportKey = "mindease:community:reports";
        try {
            String reportData = objectMapper.writeValueAsString(new HashMap<String, Object>() {{
                put("targetId", targetId);
                put("type", type);
                put("reporterId", userId);
                put("reason", reason.trim());
                put("reportTime", LocalDateTime.now().toString());
            }});
            stringRedisTemplate.opsForZSet().add(reportKey, reportData, System.currentTimeMillis());
        } catch (JsonProcessingException e) {
            log.error("举报数据序列化失败", e);
        }

        log.info("举报已提交, 等待管理员审核");
        return true;
    }

    @Override
    public List<CommunityTopicVO> getHotTopics(Integer limit) {
        log.info("获取热门话题");

        // 先尝试从Redis获取
        try {
            String cached = stringRedisTemplate.opsForValue().get(COMMUNITY_HOT_TOPICS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<CommunityTopicVO>>() {});
            }
        } catch (Exception e) {
            log.warn("从缓存读取热门话题失败", e);
        }

        // 缓存未命中，查数据库
        List<Map<String, Object>> topics = communityPostMapper.queryHotTopics(limit != null ? limit : 10);

        List<CommunityTopicVO> result = topics.stream()
                .map(row -> CommunityTopicVO.builder()
                        .tag(String.valueOf(row.get("tag")))
                        .postCount(((Number) row.get("cnt")).longValue())
                        .build())
                .collect(Collectors.toList());

        // 写入缓存，30分钟过期
        try {
            stringRedisTemplate.opsForValue().set(
                    COMMUNITY_HOT_TOPICS_KEY,
                    objectMapper.writeValueAsString(result),
                    30, TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.warn("热门话题缓存写入失败", e);
        }

        return result;
    }

    @Override
    public CommunityPostListVO searchPosts(String keyword, Integer page, Integer pageSize) {
        log.info("搜索社区内容, keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BaseException("搜索关键词不能为空");
        }

        int offset = (page - 1) * pageSize;
        String searchKeyword = "%" + keyword.trim() + "%";

        Long total = communityPostMapper.countBySearchKeyword(searchKeyword);
        List<CommunityPost> posts = communityPostMapper.selectBySearchKeyword(
                searchKeyword, pageSize, offset
        );

        List<CommunityPostItemVO> items = posts.stream()
                .map(this::convertToPostItemVO)
                .collect(Collectors.toList());

        return CommunityPostListVO.builder()
                .total(total)
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 敏感词检查
     */
    private void checkSensitiveContent(String content) {
        if (content == null) return;
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (content.contains(keyword)) {
                log.warn("检测到敏感词: {}", keyword);
                throw new BaseException("内容包含不合规信息，请修改后提交");
            }
        }
    }

    /**
     * 增加浏览量（Redis计数器 + 异步批量落库）
     */
    private void incrementViewCount(Long postId) {
        String key = COMMUNITY_VIEW_KEY + postId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null) {
            // 设置24小时自动过期
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
            // 每100次浏览量或首次时触发异步同步到DB
            if (count % 100 == 0) {
                communityPostMapper.addViewCount(postId, count);
                stringRedisTemplate.opsForValue().set(key, "0", 24, TimeUnit.HOURS);
            }
        }
    }

    /**
     * 从Redis获取当前浏览量（含缓存中的增量）
     */
    private Long getViewCountFromCache(Long postId) {
        String key = COMMUNITY_VIEW_KEY + postId;
        String cachedView = stringRedisTemplate.opsForValue().get(key);
        long extraView = 0;
        if (cachedView != null) {
            try {
                extraView = Long.parseLong(cachedView);
            } catch (NumberFormatException ignored) {}
        }
        CommunityPost post = communityPostMapper.selectById(postId);
        long baseView = post != null ? post.getViewCount() : 0L;
        return baseView + extraView;
    }

    /**
     * 刷新热门话题缓存
     */
    private void refreshHotTopicsCache(List<String> newTags) {
        if (newTags == null || newTags.isEmpty()) return;
        // 使旧缓存失效，下次查询时会重新生成
        stringRedisTemplate.delete(COMMUNITY_HOT_TOPICS_KEY);
    }

    /**
     * 解析标签JSON字符串
     */
    private List<String> parseTagsJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析标签JSON失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 转换为帖子VO
     */
    private CommunityPostVO convertToPostVO(CommunityPost post) {
        return CommunityPostVO.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .contentPreview(truncateContent(post.getContent(), 100))
                .tags(parseTagsJson(post.getTagsJson()))
                .authorName(post.getIsAnonymous() ? "匿名用户" : post.getAuthorNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createTime(post.getCreateTime())
                .build();
    }

    /**
     * 转换为帖子列表项VO
     */
    private CommunityPostItemVO convertToPostItemVO(CommunityPost post) {
        return CommunityPostItemVO.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .contentPreview(truncateContent(post.getContent(), 80))
                .authorName(post.getIsAnonymous() ? "匿名用户" : post.getAuthorNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createTime(post.getCreateTime())
                .build();
    }

    /**
     * 转换为评论项VO
     */
    private CommunityCommentItemVO convertToCommentItemVO(CommunityComment comment) {
        return CommunityCommentItemVO.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getIsAnonymous() ? "匿名用户" : comment.getAuthorNickname())
                .likeCount(comment.getLikeCount())
                .createTime(comment.getCreateTime())
                .build();
    }

    /**
     * 截断文本内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 判断是否为新用户首次发帖（前3篇需审核）
     */
    private boolean isFirstTimePoster(Long userId) {
        Integer publishedCount = communityPostMapper.countByUserId(userId);
        return publishedCount != null && publishedCount < 3;
    }
}
