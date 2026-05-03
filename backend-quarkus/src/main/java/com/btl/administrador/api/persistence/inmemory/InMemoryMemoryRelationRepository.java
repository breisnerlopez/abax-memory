package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.MemoryRelationRef;
import com.btl.administrador.api.persistence.MemoryRelationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMemoryRelationRepository implements MemoryRelationRepository {

    private final Map<String, MemoryRelationRef> relations = new ConcurrentHashMap<>();

    @Override
    public MemoryRelationRef save(MemoryRelationRef relationRef) {
        relations.put(relationRef.id, relationRef);
        return relationRef;
    }

    @Override
    public List<MemoryRelationRef> findByMemoryId(String memoryId) {
        List<MemoryRelationRef> result = new ArrayList<>();
        for (MemoryRelationRef relation : relations.values()) {
            if (relation.sourceMemoryId.equals(memoryId) || relation.targetMemoryId.equals(memoryId)) {
                result.add(relation);
            }
        }
        return result;
    }

    @Override
    public void clear() {
        relations.clear();
    }
}
