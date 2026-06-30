package com.firesafety.platform.repair.persistence;

import com.firesafety.platform.repair.CreateRepairCommand;
import com.firesafety.platform.repair.RepairStatus;
import com.firesafety.platform.repair.RepairTicket;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.repair.RepairUrgency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRepairTicketRepository implements RepairTicketRepository {
    private final RepairTicketJpaRepository jpa;

    public JpaRepairTicketRepository(RepairTicketJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public RepairTicket save(RepairTicket ticket) {
        var entity = ticket.id() == null ? new RepairTicketEntity() : jpa.findById(ticket.id()).orElseThrow();
        entity.enterpriseId = ticket.enterpriseId();
        entity.reporterUserId = ticket.reporterUserId();
        entity.urgency = ticket.urgency().name();
        entity.faultType = ticket.faultType();
        entity.location = ticket.location();
        entity.description = ticket.description();
        entity.contactName = ticket.contactName();
        entity.contactPhone = ticket.contactPhone();
        entity.status = ticket.status().name();
        entity.handlerUserId = ticket.handlerUserId();
        entity.result = ticket.result();
        entity.createdAt = ticket.createdAt();
        entity.updatedAt = ticket.updatedAt();
        entity.completedAt = ticket.completedAt();
        entity.closedAt = ticket.closedAt();
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<RepairTicket> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public Optional<RepairTicket> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(this::toDomain);
    }
    @Override public List<RepairTicket> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }
    @Override public List<RepairTicket> findByEnterpriseIds(Set<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jpa.findAllByEnterpriseIdInOrderByCreatedAtDesc(ids).stream().map(this::toDomain).toList();
    }
    @Override public List<RepairTicket> findByReporterUserId(Long id) {
        return jpa.findAllByReporterUserIdOrderByCreatedAtDesc(id).stream().map(this::toDomain).toList();
    }

    private RepairTicket toDomain(RepairTicketEntity entity) {
        return RepairTicket.restore(
                entity.id, entity.enterpriseId, entity.reporterUserId, RepairUrgency.valueOf(entity.urgency),
                entity.faultType, entity.location, entity.description, entity.contactName, entity.contactPhone,
                RepairStatus.valueOf(entity.status), entity.handlerUserId, entity.result, entity.createdAt,
                entity.updatedAt, entity.completedAt, entity.closedAt);
    }
}
