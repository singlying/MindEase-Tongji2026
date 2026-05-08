package com.mindease.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "mindease.security")
public class SecurityProperties {
    
    /**
     * AES-256 加密密钥（32字节）
     */
    private String aesKey;
}

