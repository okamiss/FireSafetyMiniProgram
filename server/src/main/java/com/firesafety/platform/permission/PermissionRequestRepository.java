package com.firesafety.platform.permission;

import java.util.Optional;
import java.util.List;

public interface PermissionRequestRepository {
    PermissionRequest save(PermissionRequest request);
    Optional<PermissionRequest> findById(Long id);
    default Optional<PermissionRequest> findByIdForUpdate(Long id) { return findById(id); }
    default List<PermissionRequest> findAll() { return List.of(); }
    default List<PermissionRequest> findByEnterpriseId(Long enterpriseId) { return List.of(); }
    default boolean existsPendingByPhone(String phone) { return false; }
}
