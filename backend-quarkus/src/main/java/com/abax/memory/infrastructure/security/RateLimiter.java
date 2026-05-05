package com.abax.memory.infrastructure.security;

import com.abax.memory.infrastructure.persistence.TenantConfigEntity;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that enforces per-tenant request limits
 * using a token-bucket algorithm in memory.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Reads {@code rate_limit_per_min} from {@code tenant_configs}.</li>
 *   <li>If the tenant exceeds its limit, returns HTTP 429 with
 *       a {@code Retry-After} header (60 seconds).</li>
 *   <li>Token buckets are reset every 60 seconds.</li>
 *   <li>Only applies to {@code /api/v2/*} paths.</li>
 * </ul>
 *
 * <p><strong>MOCK: In-memory HashMap, not distributed.</strong>
 * REPLACE_BEFORE_PROD with Redis-backed rate limiter for
 * multi-instance deployments.</p>
 *
 * <p>References: FT-004.12, BR-004, SC-03</p>
 */
@Provider
@ApplicationScoped
public class RateLimiter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RateLimiter.class);
    private static final String API_V2_PREFIX = "/api/v2";
    private static final int DEFAULT_RATE_LIMIT = 1000;
    private static final int WINDOW_SECONDS = 60;

    /**
     * Token buckets: tenantId → BucketState (count + window start).
     */
    // MOCK: ConcurrentHashMap — not distributed across instances.
    // REPLACE_BEFORE_PROD: use Redis with Lua scripts for atomicity.
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var uriPath = requestContext.getUriInfo().getPath();

        // Only apply to /api/v2/* paths
        if (!uriPath.startsWith(API_V2_PREFIX)) {
            return;
        }

        String tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return; // let TenantFilter handle missing tenant
        }

        int limit = resolveRateLimit(tenantId);
        if (limit <= 0) {
            // No rate limit configured → unlimited
            return;
        }

        if (isRateLimited(tenantId, limit)) {
            LOG.warnv("Rate limit exceeded for tenant={0}, limit={1}/min, path={2}",
                    tenantId, limit, uriPath);
            requestContext.abortWith(
                    Response.status(429)
                            .header("Retry-After", String.valueOf(WINDOW_SECONDS))
                            .entity("""
                                    {"errorCode":"RATE_LIMITED","message":"Request rate limit exceeded. Retry after %d seconds."}"""
                                    .formatted(WINDOW_SECONDS))
                            .build()
            );
        }
    }

    /**
     * Resolves the rate limit for a tenant. Falls back to
     * {@value #DEFAULT_RATE_LIMIT} if the config is not found.
     */
    private int resolveRateLimit(String tenantId) {
        try {
            var config = TenantConfigEntity.findByTenantId(tenantId);
            if (config != null) {
                return config.getRateLimitPerMin();
            }
        } catch (Exception e) {
            LOG.debugv("Failed to resolve rate limit for tenant {0}: {1}", tenantId, e.getMessage());
        }
        return DEFAULT_RATE_LIMIT;
    }

    /**
     * Checks and increments the token bucket for the given tenant.
     * Returns {@code true} if the tenant has exceeded the limit.
     */
    private boolean isRateLimited(String tenantId, int limit) {
        long now = Instant.now().getEpochSecond();
        BucketState bucket = buckets.compute(tenantId, (key, current) -> {
            if (current == null || (now - current.windowStart) >= WINDOW_SECONDS) {
                // New window
                return new BucketState(1, now);
            }
            // Within current window — increment count
            current.count++;
            return current;
        });

        // bucket is guaranteed non-null after compute()
        boolean exceeded = bucket.count > limit;
        if (exceeded) {
            LOG.debugv("Tenant {0} exceeded rate limit: count={1}, limit={2}, window_start={3}",
                    tenantId, bucket.count, limit, bucket.windowStart);
        }
        return exceeded;
    }

    /**
     * Internal state for a single tenant's token bucket.
     */
    private static class BucketState {
        long count;
        long windowStart;

        BucketState(long count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
