package com.abax.memory.config;

import com.abax.memory.infrastructure.qdrant.QdrantClient;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-warms Qdrant segments by executing health-check queries at startup — v2.1.0.
 *
 * <p>Addresses FT-V21-002.2: executes a configurable number of warm-up
 * queries against the Qdrant collection to force HNSW segment loading.
 * Mitigates cold-start latency spikes (p95 ~2s → p95 ≤ 500ms).</p>
 *
 * <p>The warm-up only executes if {@code abax.v2.qdrant.warmup.enabled=true}.
 * Each warm-up query uses a random query vector and a small `topK` (10).
 * In production, if the collection is empty, search returns empty results
 * but the segments are still loaded into memory.</p>
 *
 * <p>References: ADR-006, FT-V21-002.2</p>
 */
@ApplicationScoped
@Startup
public class QdrantWarmup {

    private static final Logger LOG = Logger.getLogger(QdrantWarmup.class);

    @Inject
    QdrantClient qdrantClient;

    @ConfigProperty(name = "abax.v2.qdrant.warmup.enabled", defaultValue = "true")
    boolean warmupEnabled;

    @ConfigProperty(name = "abax.v2.qdrant.warmup.queries", defaultValue = "20")
    int warmupQueries;

    @ConfigProperty(name = "abax.v2.qdrant.collection", defaultValue = "abax-memories")
    String qdrantCollection;

    @PostConstruct
    void warmup() {
        if (!warmupEnabled) {
            LOG.info("Qdrant warm-up disabled (abax.v2.qdrant.warmup.enabled=false)");
            return;
        }

        if (!qdrantClient.isHealthy()) {
            LOG.warn("Qdrant warm-up skipped: Qdrant server is not healthy");
            return;
        }

        LOG.infov("Qdrant warm-up starting: {0} queries against collection={1}",
                warmupQueries, qdrantCollection);

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        long totalLatencyMs = 0;

        float[] dummyVector = new float[3072];
        // Use a simple seed vector — Qdrant doesn't care about the vector content
        // for warm-up; the HNSW graph traversal is what loads segments.
        for (int i = 0; i < dummyVector.length; i++) {
            dummyVector[i] = (float) Math.sin(i * 0.01);
        }

        Map<String, Object> emptyFilters = Map.of();
        for (int i = 0; i < warmupQueries; i++) {
            long queryStart = System.nanoTime();
            try {
                qdrantClient.search(qdrantCollection, dummyVector, emptyFilters, 10);
                long queryLatencyNs = System.nanoTime() - queryStart;
                totalLatencyMs += queryLatencyNs / 1_000_000;
                successCount++;
            } catch (Exception e) {
                LOG.debugv("Warm-up query {0} failed: {1}", i, e.getMessage());
            }
        }

        long totalTimeMs = System.currentTimeMillis() - startTime;
        double avgLatencyMs = successCount > 0 ? (double) totalLatencyMs / successCount : 0;

        LOG.infov("Qdrant warm-up complete: {0}/{1} queries succeeded, "
                + "totalTime={2}ms, avgLatency={3}ms",
                successCount, warmupQueries, totalTimeMs, String.format("%.1f", avgLatencyMs));
    }
}
