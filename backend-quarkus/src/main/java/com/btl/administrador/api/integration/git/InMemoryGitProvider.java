package com.btl.administrador.api.integration.git;

import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
public class InMemoryGitProvider implements GitProvider {

    @Override
    public GitPersistResult persistApprovedMemory(MemoryRecord memoryRecord, String markdown) {
        failWhenRequested(memoryRecord);
        return new GitPersistResult("commit-" + UUID.randomUUID(), null);
    }

    @Override
    public GitPersistResult createReviewPullRequest(MemoryRecord memoryRecord, String markdown) {
        failWhenRequested(memoryRecord);
        String token = UUID.randomUUID().toString().substring(0, 8);
        return new GitPersistResult("commit-" + token, "PR-" + token);
    }

    private void failWhenRequested(MemoryRecord memoryRecord) {
        if (memoryRecord.metadata != null && "true".equalsIgnoreCase(memoryRecord.metadata.get("forceGitFailure"))) {
            throw new ApiException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "GIT_PERSISTENCE_FAILED", "Git persistence failed");
        }
    }
}
