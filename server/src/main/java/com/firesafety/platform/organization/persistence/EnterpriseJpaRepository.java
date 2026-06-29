package com.firesafety.platform.organization.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface EnterpriseJpaRepository extends JpaRepository<EnterpriseEntity, Long> {
    List<EnterpriseEntity> findAllByParentId(Long parentId);
}
