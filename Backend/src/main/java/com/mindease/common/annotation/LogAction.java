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
 * // 基础审计日志
 * @LogAction(action = "删除用户", module = "用户管理")
 * @DeleteMapping("/users/{id}")
 * public Result<?> deleteUser(@PathVariable Long id) { ... }
 *
 * // 敏感操作 + 不记录参数
 * @LogAction(action = "修改密码", module = "账户安全",
 *            recordParams = false, level = AuditLevel.HIGH)
 * @PutMapping("/password")
 * public Result<?> changePassword(@RequestBody PwdDTO dto) { ... }
 *
 * // 排除特定敏感字段
 * @LogAction(action = "导出报告", module = "数据分析",
 *            excludeParams = {"token", "sessionKey"})
 * @GetMapping("/reports/export")
 * public void exportReport(HttpServletResponse resp) { ... }
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
     * 审计级别，用于区分普通操作与高风险敏感操作
     *
     * @return 审计等级
     */
    AuditLevel level() default AuditLevel.NORMAL;

    /**
     * 排除的参数字段名列表
     * <p>当 {@link #recordParams()} 为 true 时，
     * 此列表中的字段将被脱敏或省略。</p>
     *
     * @return 需排除的参数名数组
     */
    String[] excludeParams() default {};

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

    /**
     * 审计级别枚举
     */
    enum AuditLevel {
        /** 普通操作（浏览、查询等） */
        LOW,
        /** 一般操作（常规增删改） */
        NORMAL,
        /** 敏感操作（密码变更、支付、权限调整等） */
        HIGH,
        /** 高危操作（批量删除、数据导出、配置变更等） */
        CRITICAL
    }
}
