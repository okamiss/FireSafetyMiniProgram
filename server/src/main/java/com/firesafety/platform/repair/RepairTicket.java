package com.firesafety.platform.repair;

import com.firesafety.platform.common.BusinessException;
import java.time.Instant;

public class RepairTicket {
    private Long id;
    private final Long enterpriseId;
    private final Long reporterUserId;
    private final RepairUrgency urgency;
    private final String faultType;
    private final String location;
    private final String description;
    private final String contactName;
    private final String contactPhone;
    private final Instant createdAt;
    private RepairStatus status;
    private Long handlerUserId;
    private String result;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant closedAt;

    private RepairTicket(
            Long enterpriseId,
            Long reporterUserId,
            CreateRepairCommand command,
            Instant createdAt) {
        this.enterpriseId = enterpriseId;
        this.reporterUserId = reporterUserId;
        this.urgency = command.urgency();
        this.faultType = command.faultType();
        this.location = command.location();
        this.description = command.description();
        this.contactName = command.contactName();
        this.contactPhone = command.contactPhone();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = RepairStatus.PENDING_ACCEPTANCE;
    }

    public static RepairTicket create(Long enterpriseId, Long reporterUserId, CreateRepairCommand command) {
        return new RepairTicket(enterpriseId, reporterUserId, command, Instant.now());
    }

    public static RepairTicket restore(
            Long id,
            Long enterpriseId,
            Long reporterUserId,
            RepairUrgency urgency,
            String faultType,
            String location,
            String description,
            String contactName,
            String contactPhone,
            RepairStatus status,
            Long handlerUserId,
            String result,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            Instant closedAt) {
        var ticket = new RepairTicket(enterpriseId, reporterUserId,
                new CreateRepairCommand(urgency, faultType, location, description, contactName, contactPhone),
                createdAt);
        ticket.id = id;
        ticket.status = status;
        ticket.handlerUserId = handlerUserId;
        ticket.result = result;
        ticket.updatedAt = updatedAt;
        ticket.completedAt = completedAt;
        ticket.closedAt = closedAt;
        return ticket;
    }

    public void accept(Long operatorUserId) {
        requireStatus(RepairStatus.PENDING_ACCEPTANCE);
        status = RepairStatus.PROCESSING;
        handlerUserId = operatorUserId;
        updatedAt = Instant.now();
    }

    public void complete(Long operatorUserId, String result) {
        requireStatus(RepairStatus.PROCESSING);
        status = RepairStatus.COMPLETED;
        handlerUserId = operatorUserId;
        this.result = result;
        completedAt = Instant.now();
        updatedAt = completedAt;
    }

    public void close() {
        requireStatus(RepairStatus.COMPLETED);
        status = RepairStatus.CLOSED;
        closedAt = Instant.now();
        updatedAt = closedAt;
    }

    private void requireStatus(RepairStatus expected) {
        if (status != expected) {
            throw new BusinessException("INVALID_REPAIR_STATUS", "当前报修状态不允许执行该操作");
        }
    }

    public void assignId(long id) {
        if (this.id != null) throw new IllegalStateException("报修编号已经分配");
        this.id = id;
    }

    public Long id() { return id; }
    public Long enterpriseId() { return enterpriseId; }
    public Long reporterUserId() { return reporterUserId; }
    public RepairUrgency urgency() { return urgency; }
    public String faultType() { return faultType; }
    public String location() { return location; }
    public String description() { return description; }
    public String contactName() { return contactName; }
    public String contactPhone() { return contactPhone; }
    public RepairStatus status() { return status; }
    public Long handlerUserId() { return handlerUserId; }
    public String result() { return result; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant completedAt() { return completedAt; }
    public Instant closedAt() { return closedAt; }
}
