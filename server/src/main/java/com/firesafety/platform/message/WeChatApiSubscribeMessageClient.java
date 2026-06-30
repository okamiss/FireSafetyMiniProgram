package com.firesafety.platform.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.firesafety.platform.auth.WeChatProperties;
import com.firesafety.platform.organization.UserAccount;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class WeChatApiSubscribeMessageClient implements WeChatSubscribeMessageClient {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeChatProperties weChat;
    private final WeChatSubscribeProperties properties;
    private final RestClient client;

    public WeChatApiSubscribeMessageClient(
            WeChatProperties weChat, WeChatSubscribeProperties properties, RestClient.Builder builder) {
        this.weChat = weChat;
        this.properties = properties;
        this.client = builder.baseUrl("https://api.weixin.qq.com").build();
    }

    @Override
    public DeliveryOutcome send(StationMessage message, UserAccount recipient) {
        if (!properties.enabled()) return DeliveryOutcome.skipped("SUBSCRIBE_DISABLED", "微信订阅消息未启用");
        if (weChat.mockEnabled()) return DeliveryOutcome.skipped("MOCK_MODE", "本地模拟模式不发送微信消息");
        if (recipient.openid() == null || recipient.openid().isBlank()) {
            return DeliveryOutcome.skipped("OPENID_MISSING", "用户尚未绑定微信");
        }
        var templateId = templateId(message.messageType());
        if (templateId == null || templateId.isBlank()) {
            return DeliveryOutcome.skipped("TEMPLATE_NOT_CONFIGURED", "消息模板未配置");
        }
        if (weChat.appId() == null || weChat.appId().isBlank()
                || weChat.appSecret() == null || weChat.appSecret().isBlank()) {
            return DeliveryOutcome.skipped("WECHAT_NOT_CONFIGURED", "微信 AppID 或密钥未配置");
        }
        var token = client.post().uri("/cgi-bin/stable_token").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("grant_type", "client_credential", "appid", weChat.appId(),
                        "secret", weChat.appSecret(), "force_refresh", false))
                .retrieve().body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            return DeliveryOutcome.failed("WECHAT_TOKEN_FAILED", token == null ? "微信令牌响应为空" : token.errmsg());
        }
        var data = new LinkedHashMap<String, Object>();
        data.put(valueOr(properties.titleKey(), "thing1"), Map.of("value", truncate(message.title(), 20)));
        data.put(valueOr(properties.contentKey(), "thing2"), Map.of("value", truncate(message.content(), 20)));
        data.put(valueOr(properties.timeKey(), "time3"), Map.of("value",
                TIME_FORMAT.format(message.createdAt().atZone(BUSINESS_ZONE))));
        var response = client.post()
                .uri(uri -> uri.path("/cgi-bin/message/subscribe/send")
                        .queryParam("access_token", token.accessToken()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("touser", recipient.openid(), "template_id", templateId,
                        "page", page(message), "miniprogram_state", valueOr(properties.miniprogramState(), "formal"),
                        "lang", "zh_CN", "data", data))
                .retrieve().body(SendResponse.class);
        if (response != null && response.errcode() == 0) return DeliveryOutcome.sent();
        return DeliveryOutcome.failed(response == null ? "WECHAT_EMPTY_RESPONSE" : String.valueOf(response.errcode()),
                response == null ? "微信发送响应为空" : response.errmsg());
    }

    private String templateId(String messageType) {
        return switch (messageType) {
            case "PERMISSION_REQUEST" -> properties.permissionTemplateId();
            case "REPAIR_STATUS" -> properties.repairTemplateId();
            case "TRAINING_TASK" -> properties.trainingTemplateId();
            default -> null;
        };
    }

    private String page(StationMessage message) {
        return switch (message.messageType()) {
            case "REPAIR_STATUS" -> "pages/repair/index";
            case "TRAINING_TASK" -> "pages/training/index";
            default -> "pages/messages/index";
        };
    }

    private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String truncate(String value, int maxCodePoints) {
        var points = value.codePoints().limit(maxCodePoints).toArray();
        return new String(points, 0, points.length);
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken, Integer errcode, String errmsg) {}
    private record SendResponse(Integer errcode, String errmsg) {}
}
