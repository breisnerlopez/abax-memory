package com.btl.administrador.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(length = 36, nullable = false)
    public String id;

    @Column(name = "entity_id", nullable = false, length = 36)
    public String entityId;

    @Column(name = "entity_type", nullable = false, length = 30)
    public String entityType;

    @Column(nullable = false, length = 60)
    public String action;

    @Column(columnDefinition = "TEXT")
    public String detail;

    @Column(length = 150)
    public String actor;

    @Column(name = "commit_sha", length = 120)
    public String commitSha;

    @Column(name = "pull_request_ref", length = 120)
    public String pullRequestRef;

    @Column(name = "correlation_id", length = 120)
    public String correlationId;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
