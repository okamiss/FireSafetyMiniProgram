package com.firesafety.platform.auth;

import java.util.Optional;

public interface WeChatBindingTicketStore {
    String create(String openid);
    Optional<String> consume(String ticket);
}
