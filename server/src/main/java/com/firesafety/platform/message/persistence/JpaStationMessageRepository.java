package com.firesafety.platform.message.persistence;

import com.firesafety.platform.message.ExternalDeliveryStatus;
import com.firesafety.platform.message.StationMessage;
import com.firesafety.platform.message.StationMessageRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStationMessageRepository implements StationMessageRepository {
    private final StationMessageJpaRepository jpa;

    public JpaStationMessageRepository(StationMessageJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public StationMessage save(StationMessage value) {
        var entity = new StationMessageEntity();
        entity.id = value.id();
        entity.enterpriseId = value.enterpriseId();
        entity.recipientUserId = value.recipientUserId();
        entity.messageType = value.messageType();
        entity.title = value.title();
        entity.content = value.content();
        entity.businessType = value.businessType();
        entity.businessId = value.businessId();
        entity.read = value.read();
        entity.readAt = value.readAt();
        entity.externalStatus = value.externalStatus().name();
        entity.externalErrorCode = value.externalErrorCode();
        entity.externalErrorMessage = value.externalErrorMessage();
        entity.externalSentAt = value.externalSentAt();
        entity.createdAt = value.createdAt();
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<StationMessage> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(this::toDomain);
    }
    @Override public List<StationMessage> findByRecipientUserId(Long userId) {
        return jpa.findAllByRecipientUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDomain).toList();
    }
    @Override public List<StationMessage> findAll() { return jpa.findAll().stream().map(this::toDomain).toList(); }

    private StationMessage toDomain(StationMessageEntity entity) {
        return StationMessage.restore(entity.id, entity.enterpriseId, entity.recipientUserId, entity.messageType,
                entity.title, entity.content, entity.businessType, entity.businessId, entity.read, entity.readAt,
                ExternalDeliveryStatus.valueOf(entity.externalStatus), entity.externalErrorCode,
                entity.externalErrorMessage, entity.externalSentAt, entity.createdAt);
    }
}
