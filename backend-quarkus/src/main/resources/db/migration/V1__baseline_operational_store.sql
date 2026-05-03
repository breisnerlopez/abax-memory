CREATE TABLE cases (
    id VARCHAR(32) PRIMARY KEY,
    origin VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(50) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    criticality VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE case_tags (
    case_id VARCHAR(32) NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (case_id, list_order)
);

CREATE TABLE case_participants (
    case_id VARCHAR(32) NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    participant VARCHAR(150) NOT NULL,
    PRIMARY KEY (case_id, list_order)
);

CREATE TABLE memories (
    id VARCHAR(32) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    origin VARCHAR(20) NOT NULL,
    source_case_id VARCHAR(32),
    criticality VARCHAR(20) NOT NULL,
    state VARCHAR(30) NOT NULL,
    processing_status VARCHAR(30) NOT NULL,
    current_version_id VARCHAR(36),
    current_markdown TEXT,
    commit_sha VARCHAR(120),
    pull_request_ref VARCHAR(120),
    canonical_memory_id VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_memories_source_case FOREIGN KEY (source_case_id) REFERENCES cases(id)
);

CREATE TABLE memory_domains (
    memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    domain VARCHAR(100) NOT NULL,
    PRIMARY KEY (memory_id, list_order)
);

CREATE TABLE memory_tags (
    memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (memory_id, list_order)
);

CREATE TABLE memory_metadata (
    memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT NOT NULL,
    PRIMARY KEY (memory_id, metadata_key)
);

CREATE TABLE memory_versions (
    id VARCHAR(36) PRIMARY KEY,
    memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    markdown_content TEXT NOT NULL,
    commit_sha VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_memory_versions UNIQUE (memory_id, version_number)
);

ALTER TABLE memories
    ADD CONSTRAINT fk_memories_current_version FOREIGN KEY (current_version_id) REFERENCES memory_versions(id);

CREATE TABLE memory_relation_ref (
    id VARCHAR(36) PRIMARY KEY,
    source_memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    target_memory_id VARCHAR(32) NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    relation_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    entity_id VARCHAR(36) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    action VARCHAR(60) NOT NULL,
    detail TEXT,
    actor VARCHAR(150),
    commit_sha VARCHAR(120),
    pull_request_ref VARCHAR(120),
    correlation_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_entity_id ON audit_events(entity_id, created_at DESC);

CREATE TABLE processing_jobs (
    id VARCHAR(36) PRIMARY KEY,
    memory_id VARCHAR(32) NOT NULL,
    version_id VARCHAR(36) NOT NULL,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    locked_by VARCHAR(120),
    locked_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_processing_jobs UNIQUE (memory_id, version_id, job_type)
);

CREATE INDEX idx_processing_jobs_claim ON processing_jobs(status, next_attempt_at, created_at);
CREATE INDEX idx_processing_jobs_memory ON processing_jobs(memory_id);
