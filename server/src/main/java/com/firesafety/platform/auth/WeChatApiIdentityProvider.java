package com.firesafety.platform.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.firesafety.platform.common.BusinessException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WeChatApiIdentityProvider implements WeChatIdentityProvider {
    private final WeChatProperties properties;
    private final RestClient client;

    public WeChatApiIdentityProvider(WeChatProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.client = builder.baseUrl("https://api.weixin.qq.com").build();
    }

    @Override
    public String exchangeLoginCode(String loginCode) {
        if (properties.mockEnabled()) return "mock-openid-" + loginCode;
        requireCredentials();
        var response = client.get()
                .uri(uri -> uri.path("/sns/jscode2session")
                        .queryParam("appid", properties.appId())
                        .queryParam("secret", properties.appSecret())
                        .queryParam("js_code", loginCode)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .body(LoginCodeResponse.class);
        if (response == null || response.openid() == null) {
            throw providerError(response == null ? null : response.errcode(),
                    response == null ? null : response.errmsg());
        }
        return response.openid();
    }

    @Override
    public String exchangePhoneCode(String phoneCode) {
        if (properties.mockEnabled()) {
            if (!phoneCode.startsWith("mock-phone:")) {
                throw new BusinessException("WECHAT_PHONE_EXCHANGE_FAILED", "本地模拟手机号凭证格式错误");
            }
            return phoneCode.substring("mock-phone:".length());
        }
        requireCredentials();
        var tokenResponse = client.post()
                .uri("/cgi-bin/stable_token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "grant_type", "client_credential",
                        "appid", properties.appId(),
                        "secret", properties.appSecret(),
                        "force_refresh", false))
                .retrieve()
                .body(AccessTokenResponse.class);
        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw providerError(tokenResponse == null ? null : tokenResponse.errcode(),
                    tokenResponse == null ? null : tokenResponse.errmsg());
        }
        var phoneResponse = client.post()
                .uri(uri -> uri.path("/wxa/business/getuserphonenumber")
                        .queryParam("access_token", tokenResponse.accessToken())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("code", phoneCode))
                .retrieve()
                .body(PhoneResponse.class);
        if (phoneResponse == null || phoneResponse.errcode() != 0
                || phoneResponse.phoneInfo() == null || phoneResponse.phoneInfo().phoneNumber() == null) {
            throw providerError(phoneResponse == null ? null : phoneResponse.errcode(),
                    phoneResponse == null ? null : phoneResponse.errmsg());
        }
        return phoneResponse.phoneInfo().phoneNumber();
    }

    private void requireCredentials() {
        if (properties.appId() == null || properties.appId().isBlank()
                || properties.appSecret() == null || properties.appSecret().isBlank()) {
            throw new BusinessException("WECHAT_NOT_CONFIGURED", "微信小程序认证尚未配置");
        }
    }

    private BusinessException providerError(Integer code, String message) {
        return new BusinessException(
                "WECHAT_API_ERROR", "微信接口调用失败" + (code == null ? "" : "（" + code + "）"));
    }

    private record LoginCodeResponse(String openid, Integer errcode, String errmsg) {}
    private record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken, Integer errcode, String errmsg) {}
    private record PhoneResponse(
            Integer errcode,
            String errmsg,
            @JsonProperty("phone_info") PhoneInfo phoneInfo) {}
    private record PhoneInfo(@JsonProperty("phoneNumber") String phoneNumber) {}
}
