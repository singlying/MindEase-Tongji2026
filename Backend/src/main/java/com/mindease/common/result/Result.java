package com.mindease.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; //编码：200成功，其它数字为失败
    private String message; //提示信息
    private T data; //数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = 200;
        result.message = "success";
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 200;
        result.message = "success";
        return result;
    }

    public static <T> Result<T> success(T object, String message) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 200;
        result.message = message;
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.message = message;
        result.code = 0;
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.message = message;
        result.code = code;
        return result;
    }
}