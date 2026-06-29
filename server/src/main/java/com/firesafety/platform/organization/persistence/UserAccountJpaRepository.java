package com.firesafety.platform.organization.persistence;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByPhone(String phone);
    Optional<UserAccountEntity> findByOpenid(String openid);
    List<UserAccountEntity> findAllByEnterpriseIdOrderByIdAsc(Long enterpriseId);
}
