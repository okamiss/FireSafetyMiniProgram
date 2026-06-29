package com.firesafety.platform.organization;

import java.util.Optional;
import java.util.List;

public interface UserAccountRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByPhone(String phone);
    Optional<UserAccount> findByOpenid(String openid);
    Optional<UserAccount> findById(Long id);
    default List<UserAccount> findAll() { return List.of(); }
    default List<UserAccount> findByEnterpriseId(Long enterpriseId) { return List.of(); }
}
