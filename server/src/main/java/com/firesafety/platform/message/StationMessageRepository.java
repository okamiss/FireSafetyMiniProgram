package com.firesafety.platform.message;

import java.util.List;
import java.util.Optional;

public interface StationMessageRepository {
    StationMessage save(StationMessage message);
    Optional<StationMessage> findByIdForUpdate(Long id);
    List<StationMessage> findByRecipientUserId(Long userId);
    List<StationMessage> findAll();
}
