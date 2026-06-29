package com.firesafety.platform.security;

import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RepositoryEnterpriseScopeResolver implements EnterpriseScopeResolver {
    private final EnterpriseRepository enterprises;

    public RepositoryEnterpriseScopeResolver(EnterpriseRepository enterprises) {
        this.enterprises = enterprises;
    }

    @Override
    public Set<Long> allEnterpriseIds() {
        return enterprises.findAll().stream().map(Enterprise::id).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<Long> descendantsIncludingSelf(long enterpriseId) {
        var all = enterprises.findAll();
        var result = new HashSet<Long>();
        result.add(enterpriseId);
        boolean changed;
        do {
            changed = false;
            for (var enterprise : all) {
                if (enterprise.parentId() != null && result.contains(enterprise.parentId())) {
                    changed |= result.add(enterprise.id());
                }
            }
        } while (changed);
        return Set.copyOf(result);
    }
}
