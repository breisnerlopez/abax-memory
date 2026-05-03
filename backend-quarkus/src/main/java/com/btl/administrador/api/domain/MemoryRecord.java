package com.btl.administrador.api.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "memories")
public class MemoryRecord {

    @Id
    @Column(length = 32, nullable = false)
    public String id;

    @Column(nullable = false, length = 255)
    public String title;

    @Column(nullable = false, length = 100)
    public String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public MemoryOrigin origin;

    @Column(name = "source_case_id", length = 32)
    public String sourceCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Criticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public MemoryState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    public ProcessingStatus processingStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_domains", joinColumns = @JoinColumn(name = "memory_id"))
    @Column(name = "domain", nullable = false, length = 100)
    @OrderColumn(name = "list_order")
    public List<String> domains = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_tags", joinColumns = @JoinColumn(name = "memory_id"))
    @Column(name = "tag", nullable = false, length = 100)
    @OrderColumn(name = "list_order")
    public List<String> tags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_metadata", joinColumns = @JoinColumn(name = "memory_id"))
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", nullable = false, columnDefinition = "TEXT")
    public Map<String, String> metadata = new LinkedHashMap<>();

    @Column(name = "current_version_id", length = 36)
    public String currentVersionId;

    @Column(name = "current_markdown", columnDefinition = "TEXT")
    public String currentMarkdown;

    @Column(name = "commit_sha", length = 120)
    public String commitSha;

    @Column(name = "pull_request_ref", length = 120)
    public String pullRequestRef;

    @Column(name = "canonical_memory_id", length = 32)
    public String canonicalMemoryId;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;
}
