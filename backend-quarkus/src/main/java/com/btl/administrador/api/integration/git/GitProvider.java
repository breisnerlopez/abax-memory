package com.btl.administrador.api.integration.git;

import com.btl.administrador.api.domain.MemoryRecord;

public interface GitProvider {
    GitPersistResult persistApprovedMemory(MemoryRecord memoryRecord, String markdown);

    GitPersistResult createReviewPullRequest(MemoryRecord memoryRecord, String markdown);
}
