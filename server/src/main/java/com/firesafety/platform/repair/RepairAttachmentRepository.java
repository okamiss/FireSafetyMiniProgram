package com.firesafety.platform.repair;

import java.util.List;
import java.util.Optional;

public interface RepairAttachmentRepository {
    RepairAttachment save(RepairAttachment attachment);
    List<RepairAttachment> findByRepairId(Long repairId);
    Optional<RepairAttachment> findById(Long id);
}
