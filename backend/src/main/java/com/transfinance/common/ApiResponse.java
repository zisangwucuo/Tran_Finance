package com.transfinance.common;

/**
 * 统一接口响应结构，避免 Controller 直接暴露零散返回格式。
 *
 * @param code    业务状态码，200 表示成功
 * @param message 响应说明
 * @param data    响应数据
 * @param <T>     响应数据类型
 */
public record ApiResponse<T>(Integer code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
