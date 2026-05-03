package com.btl.administrador.api.persistence;

import com.btl.administrador.api.domain.MemoryRelationRef;

import java.util.List;

public interface MemoryRelationRepository {
    MemoryRelationRef save(MemoryRelationRef relationRef);

    List<MemoryRelationRef> findByMemoryId(String memoryId);

    void clear();
}
