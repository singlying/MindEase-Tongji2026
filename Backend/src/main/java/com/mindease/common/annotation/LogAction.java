package com.mindease.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户行为审计日志注解
 * <p>
 * 标注在 Controller 方法上，用于声明需要记录的用户操作行为。
 * 配合 AOP 切面使用时，可自动记录：操作人、操作类型、操作描述、IP 地址、
 * 请求参数、执行耗时等信息，用于安全审计与行为追溯。
 * </p>
 *
 * <h3>使用示例（未来接入切面后）：</h3>
 * <pre>{@code
 * @LogAction(action = "删除用户", module = "用户管理")
 * @DeleteMapping("/users/{id}")
 * public Result<?> deleteUser(@PathVariable Long id) { ... }
 * }</pre>
 *
 * <h3>安全说明：</h3>
 * <ul>
 *   <li>本注解仅做元数据声明（纯 {@code @interface}），不包含任何日志采集逻辑</li>
 *   <li>未编写或未启用对应 AOP 切面时，该注解在运行时不产生任何效果</li>
 *   <li>对现有系统完全透明、零侵入</li>
 * </ul>
 *
 * @author MindEase Team
 * @since 2026-06-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAction {

    /**
     * 操作动作描述（如："登录"、"发布帖子"、"删除评论"）
     *
     * @return 动作描述文本
     */
    String action();

    /**
     * 所属功能模块（如："用户管理"、"内容审核"、"系统设置"）
     *
     * @return 模块名称
     */
    String module() default "";

    /**
     * 是否记录详细的请求参数
     * <p>敏感接口建议设为 {@code false}</p>
     *
     * @return 是否记录参数
     */
    boolean recordParams() default true;

    /**
     * 日志存储策略
     *
     * @return 存储策略枚举
     */
    LogStore store() default LogStore.DATABASE;

    /**
     * 日志存储策略枚举
     */
    enum LogStore {
        /** 存入数据库审计表 */
        DATABASE,
        /** 输出到日志文件 */
        FILE,
        /** 同时存入数据库和输出到文件 */
        BOTH
    }
}
