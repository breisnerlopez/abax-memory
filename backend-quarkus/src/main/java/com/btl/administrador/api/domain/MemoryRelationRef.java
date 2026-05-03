package com.btl.administrador.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "memory_relation_ref")
public class MemoryRelationRef {

    @Id
    @Column(length = 36, nullable = false)
    public String id;

    @Column(name = "source_memory_id", nullable = false, length = 32)
    public String sourceMemoryId;

    @Column(name = "target_memory_id", nullable = false, length = 32)
    public String targetMemoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 40)
    public RelationType relationType;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
