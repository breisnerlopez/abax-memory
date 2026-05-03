package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.MemoryRecord;

import java.util.List;
import java.util.Optional;

public interface MemoryRepository {
    MemoryRecord save(MemoryRecord memoryRecord);

    Optional<MemoryRecord> findById(String id);

    List<MemoryRecord> findAll();

    void clear();
}
