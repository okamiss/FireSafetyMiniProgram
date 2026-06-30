package com.firesafety.platform.message;

import com.firesafety.platform.organization.UserAccount;

public interface WeChatSubscribeMessageClient {
    DeliveryOutcome send(StationMessage message, UserAccount recipient);
}
