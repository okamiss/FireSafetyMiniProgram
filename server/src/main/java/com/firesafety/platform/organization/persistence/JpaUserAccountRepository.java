package com.firesafety.platform.organization.persistence;

import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {
    private final UserAccountJpaRepository jpa;

    public JpaUserAccountRepository(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserAccount save(UserAccount user) {
        var entity = user.id() == null
                ? new UserAccountEntity()
                : jpa.findById(user.id()).orElseThrow();
        var now = Instant.now();
        if (entity.createdAt == null) entity.createdAt = now;
        entity.updatedAt = now;
        entity.enterpriseId = user.enterpriseId();
        entity.username = user.username();
        entity.passwordHash = user.passwordHash();
        entity.displayName = user.displayName();
        entity.phone = user.phone();
        entity.openid = user.openid();
        entity.role = user.role().name();
        entity.status = user.enabled() ? "ENABLED" : "DISABLED";
        return toDomain(jpa.save(entity));
    }

    @Override public Optional<UserAccount> findByUsername(String username) {
        return jpa.findByUsername(username).map(this::toDomain);
    }
    @Override public Optional<UserAccount> findByPhone(String phone) {
        return jpa.findByPhone(phone).map(this::toDomain);
    }
    @Override public Optional<UserAccount> findByOpenid(String openid) {
        return jpa.findByOpenid(openid).map(this::toDomain);
    }
    @Override public Optional<UserAccount> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }
    @Override public List<UserAccount> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }
    @Override public List<UserAccount> findByEnterpriseId(Long enterpriseId) {
        return jpa.findAllByEnterpriseIdOrderByIdAsc(enterpriseId).stream().map(this::toDomain).toList();
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return UserAccount.restore(
                entity.id,
                entity.enterpriseId,
                entity.username,
                entity.passwordHash,
                entity.displayName,
                entity.phone,
                entity.openid,
                UserRole.valueOf(entity.role),
                "ENABLED".equals(entity.status));
    }
}
