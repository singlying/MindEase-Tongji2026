package com.mindease.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 * 用于敏感数据的透明加密存储
 * 
 * 安全特性：
 * - 使用 AES-256-GCM 模式（带认证加密）
 * - 每次加密生成随机 IV（防止相同明文产生相同密文）
 * - 自动验证数据完整性（防止篡改）
 */
@Slf4j
@Component
public class AesEncryptUtil {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;        // GCM 标准 IV 长度 12 字节
    private static final int GCM_TAG_LENGTH = 128;      // 认证标签长度 128 位
    
    /**
     * 默认密钥（32字节 = 256位）
     * ⚠️ 仅用于配置文件未设置时的备用密钥
     */
    private static final String DEFAULT_SECRET_KEY = "MindEase2024SecretKey32Bytes!!"; // 32字符 = 256位
    
    /**
     * 实际使用的密钥（从配置文件读取）
     */
    private static String actualSecretKey;
    
    private final com.mindease.config.SecurityProperties securityProperties;
    
    /**
     * 构造函数注入配置
     */
    public AesEncryptUtil(com.mindease.config.SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }
    
    /**
     * 初始化密钥（Spring Bean 初始化后执行）
     */
    @PostConstruct
    public void init() {
        String configKey = securityProperties.getAesKey();
        
        if (configKey == null || configKey.trim().isEmpty()) {
            log.warn("⚠️ 未在配置文件中设置 mindease.security.aes-key，使用默认密钥（不适用于生产环境）");
            actualSecretKey = DEFAULT_SECRET_KEY;
        } else {
            actualSecretKey = configKey;
            log.info("✅ AES-256 加密密钥已从配置文件加载");
        }
        
        // 验证密钥长度
        if (actualSecretKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("❌ AES-256 密钥必须为 32 字节，当前长度: " 
                    + actualSecretKey.getBytes(StandardCharsets.UTF_8).length);
        }
    }
    
    /**
     * 获取密钥字节数组
     */
    private static byte[] getSecretKey() {
        if (actualSecretKey == null) {
            // 如果在 Spring 容器初始化前调用，使用默认密钥
            log.warn("密钥尚未初始化，使用默认密钥");
            return DEFAULT_SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        }
        return actualSecretKey.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 加密
     * 
     * @param plaintext 明文
     * @return Base64 编码的密文（包含 IV）
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            // 1. 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            // 2. 初始化密钥
            SecretKeySpec keySpec = new SecretKeySpec(getSecretKey(), "AES");
            
            // 3. 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            
            // 4. 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 5. 将 IV 和密文拼接（IV 在前，密文在后）
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            
            // 6. Base64 编码后返回
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES-GCM 加密失败，明文长度: {}", plaintext.length(), e);
            throw new RuntimeException("数据加密失败", e);
        }
    }
    
    /**
     * 解密
     * 
     * @param ciphertext Base64 编码的密文（包含 IV）
     * @return 明文
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        try {
            // 1. Base64 解码
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            
            // 2. 分离 IV 和密文
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertextBytes = new byte[buffer.remaining()];
            buffer.get(ciphertextBytes);
            
            // 3. 初始化密钥
            SecretKeySpec keySpec = new SecretKeySpec(getSecretKey(), "AES");
            
            // 4. 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            
            // 5. 解密
            byte[] plaintext = cipher.doFinal(ciphertextBytes);
            
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-GCM 解密失败，密文: {}", ciphertext.substring(0, Math.min(50, ciphertext.length())), e);
            // 解密失败可能是数据损坏或密钥错误，返回特殊标记
            return "[解密失败]";
        }
    }
}

