package com.firesafety.platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void createsStableSuccessAndErrorContracts() {
        var success = ApiResponse.ok("payload");
        var error = ApiResponse.error("INVALID_REQUEST", "invalid request");

        assertThat(success.status()).isEqualTo("ok");
        assertThat(success.code()).isEqualTo("SUCCESS");
        assertThat(success.data()).isEqualTo("payload");
        assertThat(success.message()).isNull();

        assertThat(error.status()).isEqualTo("error");
        assertThat(error.code()).isEqualTo("INVALID_REQUEST");
        assertThat(error.data()).isNull();
        assertThat(error.message()).isEqualTo("invalid request");
    }
}
