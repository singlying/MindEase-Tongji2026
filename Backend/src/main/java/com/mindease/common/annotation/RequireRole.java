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
 * @RequireRole(Role.ADMIN)
 * @GetMapping("/admin/users")
 * public Result<?> listUsers() { ... }
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
     * 要求的角色名称列表（满足其一即可）
     *
     * @return 允许访问的角色名数组
     */
    String[] value();

    /**
     * 权限校验失败时的提示信息
     *
     * @return 自定义错误提示，默认 "权限不足"
     */
    String message() default "权限不足";
}
