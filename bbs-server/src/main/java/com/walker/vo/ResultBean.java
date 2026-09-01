package com.walker.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公共返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "ResultBean对象", description = "公共返回对象")
public class ResultBean<T> {
    private long code;
    private String message;
    private T obj;

    public static <T> ResultBean<T> success(String message) {
        return new ResultBean<>(200, message, null);
    }

    public static <T> ResultBean<T> success(String message, T obj) {
        return new ResultBean<>(200, message, obj);
    }

    public static <T> ResultBean<T> success(T obj) {
        return new ResultBean<>(200, null, obj);
    }

    public static <T> ResultBean<T> error(String message) {
        return new ResultBean<>(500, message, null);
    }

    public static <T> ResultBean<T> error(String message, T obj) {
        return new ResultBean<>(500, message, obj);
    }
}
