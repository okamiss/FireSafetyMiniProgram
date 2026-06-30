package com.firesafety.platform.audit;

import java.util.List;

public interface OperationLogRepository {
    OperationLog save(OperationLog value);
    List<OperationLog> findAll();
}
