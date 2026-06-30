package com.firesafety.platform.message;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.wechat-subscribe")
public record WeChatSubscribeProperties(
        boolean enabled,
        String permissionTemplateId,
        String repairTemplateId,
        String trainingTemplateId,
        String titleKey,
        String contentKey,
        String timeKey,
        String miniprogramState) {
    public List<String> configuredTemplateIds() {
        return java.util.stream.Stream.of(permissionTemplateId, repairTemplateId, trainingTemplateId)
                .filter(value -> value != null && !value.isBlank()).distinct().toList();
    }
}
