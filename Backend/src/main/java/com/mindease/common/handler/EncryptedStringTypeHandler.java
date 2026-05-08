package com.mindease.common.handler;

import com.mindease.common.utils.AesEncryptUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 加密字段 TypeHandler
 * 自动对指定字段进行透明加解密
 * 
 * 使用方式：在实体类字段上添加 @TableField(typeHandler = EncryptedStringTypeHandler.class)
 * 或在 Mapper XML 中指定 typeHandler="com.mindease.common.handler.EncryptedStringTypeHandler"
 */
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置参数（写入数据库前加密）
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 加密后写入数据库
        String encrypted = AesEncryptUtil.encrypt(parameter);
        ps.setString(i, encrypted);
    }

    /**
     * 根据列名获取结果（从数据库读取后解密）
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return AesEncryptUtil.decrypt(encrypted);
    }

    /**
     * 根据列索引获取结果（从数据库读取后解密）
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return AesEncryptUtil.decrypt(encrypted);
    }

    /**
     * 获取存储过程结果（从数据库读取后解密）
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return AesEncryptUtil.decrypt(encrypted);
    }
}

