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
 * // 基础限流：每分钟最多 10 次请求
 * @RateLimiter(limit = 10, window = 60)
 * @PostMapping("/login")
 * public Result<?> login(@RequestBody LoginDTO dto) { ... }
 *
 * // 令牌桶模式：支持短时突发
 * @RateLimiter(limit = 5, window = 60, burst = 3)
 * @GetMapping("/search")
 * public Result<?> search(@RequestParam String q) { ... }
 *
 * // 被限流时返回自定义提示 + HTTP 429 状态码
 * @RateLimiter(limit = 2, window = 300,
 *               message = "发送过于频繁，请 5 分钟后再试",
 *               fallback = FallbackAction.REJECT_WITH_CODE)
 * @PostMapping("/sms/send")
 * public Result<?> sendSms(@RequestBody SmsRequest req) { ... }
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

    /**
     * 突发容量（令牌桶模式下的 burst 上限）
     * <p>设为 0 或与 limit 相等则退化为固定窗口计数器。</p>
     *
     * @return 突发请求数，默认 0（不启用突发）
     */
    int burst() default 0;

    /**
     * 被限流时的降级行为策略
     *
     * @return 降级处理方式
     */
    FallbackAction fallback() default FallbackAction.REJECT;

    /**
     * 是否对管理员角色豁免限流
     *
     * @return true 则 ADMIN 角色不受此限制约束
     */
    boolean adminBypass() default false;

    /**
     * 降级行为枚举
     */
    enum FallbackAction {
        /** 直接拒绝请求，返回错误消息 */
        REJECT,
        /** 拒绝并在响应头中设置 Retry-After / HTTP 429 */
        REJECT_WITH_CODE,
        /** 放行但记录告警日志 */
        WARN_AND_PASS,
        /** 进入排队等待（异步场景适用） */
        QUEUE_AND_WAIT,
    }
}
