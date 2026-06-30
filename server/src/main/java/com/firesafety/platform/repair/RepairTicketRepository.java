package com.firesafety.platform.repair;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RepairTicketRepository {
    RepairTicket save(RepairTicket ticket);
    Optional<RepairTicket> findById(Long id);
    default Optional<RepairTicket> findByIdForUpdate(Long id) { return findById(id); }
    List<RepairTicket> findAll();
    List<RepairTicket> findByEnterpriseIds(Set<Long> enterpriseIds);
    List<RepairTicket> findByReporterUserId(Long reporterUserId);
}
