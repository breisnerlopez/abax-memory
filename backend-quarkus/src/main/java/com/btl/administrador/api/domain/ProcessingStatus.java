package com.btl.administrador.api.domain;

public enum ProcessingStatus {
    PENDING_GIT,
    GIT_PERSISTED,
    PENDING_INDEX,
    INDEXING,
    AVAILABLE,
    INDEX_FAILED,
    GIT_FAILED
}
