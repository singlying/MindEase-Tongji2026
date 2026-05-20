package com.mindease.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 * <p>
 * 标注在 Controller 方法或类上，用于声明该方法/类所需的用户角色。
 * 配合 AOP 切面使用时，可在方法执行前自动校验当前用户是否拥有指定角色。
 * </p>
 *
 * <h3>使用示例（未来接入切面后）：</h3>
 * <pre>{@code
 * // 单角色校验（满足其一即可）
 * @RequireRole(Role.ADMIN)
 * @GetMapping("/admin/users")
 * public Result<?> listUsers() { ... }
 *
 * // 多角色 AND 逻辑（需同时具备所有角色）
 * @RequireRole(value = {"SUPER_ADMIN", "AUDITOR"}, logical = Logical.AND)
 * @DeleteMapping("/audit-logs/{id}")
 * public Result<?> deleteLog(@PathVariable Long id) { ... }
 *
 * // 带优先级的角色匹配（优先级越高越先检查）
 * @RequireRole(value = "COUNSELOR", priority = 10)
 * @PostMapping("/sessions")
 * public Result<?> createSession() { ... }
 * }</pre>
 *
 * <h3>安全说明：</h3>
 * <ul>
 *   <li>本注解仅做元数据声明，不包含任何拦截/校验逻辑</li>
 *   <li>未配置 AOP 切面时，该注解不会产生任何运行时效果</li>
 *   <li>对现有系统完全透明、零侵入</li>
 * </ul>
 *
 * @author MindEase Team
 * @since 2026-06-29
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 要求的角色名称列表
     * <p>当 {@link #logical()} 为 OR 时，满足其一即可；为 AND 时需全部满足。</p>
     *
     * @return 允许访问的角色名数组
     */
    String[] value();

    /**
     * 多角色间的逻辑关系
     *
     * @return OR（满足任一）/ AND（全部满足），默认 OR
     */
    Logical logical() default Logical.OR;

    /**
     * 权限校验失败时的提示信息
     *
     * @return 自定义错误提示，默认 "权限不足"
     */
    String message() default "权限不足";

    /**
     * 规则优先级（数值越大越优先匹配）
     * <p>当同一方法或类上存在多个注解（或类级+方法级叠加）时，
     * 可通过 priority 控制校验顺序。</p>
     *
     * @return 优先级值，默认 0（普通优先级）
     */
    int priority() default 0;

    /**
     * 多角色逻辑枚举
     */
    enum Logical {
        /** 满足任一角色即可通过 */
        OR,
        /** 必须同时具备所有指定角色 */
        AND
    }
}
