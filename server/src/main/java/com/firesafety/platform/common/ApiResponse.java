package com.firesafety.platform.common;

public record ApiResponse<T>(String status, String code, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("ok", "SUCCESS", data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>("error", code, null, message);
    }
}
