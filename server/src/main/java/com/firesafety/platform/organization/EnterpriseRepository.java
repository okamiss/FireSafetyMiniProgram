package com.firesafety.platform.organization;

import java.util.List;
import java.util.Optional;

public interface EnterpriseRepository {
    Enterprise save(Enterprise enterprise);
    Optional<Enterprise> findById(Long id);
    List<Enterprise> findAll();
}
