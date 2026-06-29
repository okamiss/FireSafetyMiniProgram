package com.firesafety.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

final class ApiSecurityWriter {
    private ApiSecurityWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
    }
}
