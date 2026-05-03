package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.MemoryVersionRecord;

import java.util.List;
import java.util.Optional;

public interface MemoryVersionRepository {
    MemoryVersionRecord save(MemoryVersionRecord versionRecord);

    Optional<MemoryVersionRecord> findById(String id);

    List<MemoryVersionRecord> findByMemoryId(String memoryId);

    void clear();
}
