package com.rahman.commonlib;

public record ApiResponse<T> (boolean success, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
