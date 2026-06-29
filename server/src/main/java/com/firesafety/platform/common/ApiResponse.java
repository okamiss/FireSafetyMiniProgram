package com.firesafety.platform.common;

public record ApiResponse<T>(String status, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("ok", data, null);
    }
}
