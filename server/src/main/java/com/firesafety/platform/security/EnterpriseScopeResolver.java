package com.firesafety.platform.security;

import java.util.Set;

public interface EnterpriseScopeResolver {
    Set<Long> allEnterpriseIds();

    Set<Long> descendantsIncludingSelf(long enterpriseId);
}
