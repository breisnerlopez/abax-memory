package com.btl.administrador.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "memory_versions")
public class MemoryVersionRecord {

    @Id
    @Column(length = 36, nullable = false)
    public String id;

    @Column(name = "memory_id", nullable = false, length = 32)
    public String memoryId;

    @Column(name = "version_number", nullable = false)
    public int versionNumber;

    @Column(name = "markdown_content", nullable = false, columnDefinition = "TEXT")
    public String markdownContent;

    @Column(name = "commit_sha", length = 120)
    public String commitSha;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
