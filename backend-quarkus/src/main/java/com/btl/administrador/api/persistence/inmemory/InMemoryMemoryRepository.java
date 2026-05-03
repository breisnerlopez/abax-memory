package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.persistence.MemoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMemoryRepository implements MemoryRepository {

    private final Map<String, MemoryRecord> memories = new ConcurrentHashMap<>();

    @Override
    public MemoryRecord save(MemoryRecord memoryRecord) {
        memories.put(memoryRecord.id, memoryRecord);
        return memoryRecord;
    }

    @Override
    public Optional<MemoryRecord> findById(String id) {
        return Optional.ofNullable(memories.get(id));
    }

    @Override
    public List<MemoryRecord> findAll() {
        return new ArrayList<>(memories.values());
    }

    @Override
    public void clear() {
        memories.clear();
    }
}
