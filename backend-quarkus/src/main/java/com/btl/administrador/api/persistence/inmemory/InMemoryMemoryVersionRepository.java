package com.btl.administrador.api.persistence.inmemory;

import com.btl.administrador.api.domain.MemoryVersionRecord;
import com.btl.administrador.api.persistence.MemoryVersionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMemoryVersionRepository implements MemoryVersionRepository {

    private final Map<String, MemoryVersionRecord> versions = new ConcurrentHashMap<>();

    @Override
    public MemoryVersionRecord save(MemoryVersionRecord versionRecord) {
        versions.put(versionRecord.id, versionRecord);
        return versionRecord;
    }

    @Override
    public Optional<MemoryVersionRecord> findById(String id) {
        return Optional.ofNullable(versions.get(id));
    }

    @Override
    public List<MemoryVersionRecord> findByMemoryId(String memoryId) {
        return versions.values().stream()
                .filter(version -> version.memoryId.equals(memoryId))
                .sorted(Comparator.comparingInt(version -> version.versionNumber))
                .toList();
    }

    @Override
    public void clear() {
        versions.clear();
    }
}
