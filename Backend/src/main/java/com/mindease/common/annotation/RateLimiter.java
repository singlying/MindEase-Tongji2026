package com.mindease.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流 / 防刷注解
 * <p>
 * 标注在 Controller 方法上，用于声明该接口的访问频率限制策略。
 * 配合 AOP 切面 + Redis/本地缓存可实现基于 IP 或用户的接口级别限流，
 * 有效防止恶意请求、爬虫刷接口等场景。
 * </p>
 *
 * <h3>使用示例（未来接入切面后）：</h3>
 * <pre>{@code
 * @RateLimiter(limit = 10, window = 60)
 * @PostMapping("/login")
 * public Result<?> login(@RequestBody LoginDTO dto) { ... }
 * }</pre>
 *
 * <h3>安全说明：</h3>
 * <ul>
 *   <li>本注解仅做元数据声明（纯 {@code @interface}），不包含任何拦截逻辑</li>
 *   <li>未编写或未启用对应 AOP 切面时，该注解在运行时不产生任何效果</li>
 *   <li>对现有系统完全透明、零侵入</li>
 * </ul>
 *
 * @author MindEase Team
 * @since 2026-06-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * 时间窗口内允许的最大请求数
     *
     * @return 最大请求数阈值
     */
    int limit();

    /**
     * 限流时间窗口大小（单位由 {@link #timeUnit()} 决定）
     *
     * @return 窗口大小数值
     */
    long window() default 60;

    /**
     * 时间单位，默认秒
     *
     * @return 时间单位枚举
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流的 key 前缀（用于 Redis 场景下区分不同接口）
     *
     * @return key 前缀字符串
     */
    String keyPrefix() default "rate_limit";

    /**
     * 被限流时的提示信息
     *
     * @return 自定义提示，默认 "操作过于频繁，请稍后再试"
     */
    String message() default "操作过于频繁，请稍后再试";
}
