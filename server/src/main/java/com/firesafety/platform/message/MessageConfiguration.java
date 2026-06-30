package com.firesafety.platform.message;

import com.firesafety.platform.auth.WeChatProperties;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MessageConfiguration {
    @Bean StationMessageService stationMessageService(
            StationMessageRepository messages, ApplicationEventPublisher events, Clock clock) {
        return new StationMessageService(messages, events, clock);
    }

    @Bean WeChatSubscribeMessageClient weChatSubscribeMessageClient(
            WeChatProperties weChat, WeChatSubscribeProperties properties, RestClient.Builder builder) {
        return new WeChatApiSubscribeMessageClient(weChat, properties, builder);
    }

    @Bean MessageDeliveryService messageDeliveryService(
            StationMessageRepository messages, UserAccountRepository users,
            WeChatSubscribeMessageClient client, Clock clock) {
        return new MessageDeliveryService(messages, users, client, clock);
    }

    @Bean StationMessageEventListener stationMessageEventListener(MessageDeliveryService delivery) {
        return new StationMessageEventListener(delivery);
    }
}
