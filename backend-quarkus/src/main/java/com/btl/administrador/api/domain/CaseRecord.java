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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
public class CaseRecord {

    @Id
    @Column(length = 32, nullable = false)
    public String id;

    @Column(nullable = false, length = 100)
    public String origin;

    @Column(nullable = false, length = 255)
    public String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 50)
    public String priority;

    @Column(nullable = false, length = 100)
    public String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Criticality criticality;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_tags", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "tag", nullable = false, length = 100)
    @OrderColumn(name = "list_order")
    public List<String> tags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_participants", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "participant", nullable = false, length = 150)
    @OrderColumn(name = "list_order")
    public List<String> participants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public CaseStatus status;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
