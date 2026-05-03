package com.btl.administrador.api.integration.qdrant;

import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.service.model.SearchHit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-memory search indexer for testing environments.
 * NOT a CDI bean - instantiate manually in tests.
 * Production use: QdrantEmbeddingService (real embeddings + Qdrant).
 */
public class InMemorySearchIndexer implements SearchIndexer {

    private static final Map<String, Set<String>> SEMANTIC_GROUPS = Map.of(
            "contrasena", Set.of("contrasena", "clave", "password", "credencial", "acceso"),
            "onboarding", Set.of("onboarding", "ingreso", "incorporacion", "induccion"),
            "runbook", Set.of("runbook", "procedimiento", "guia"),
            "incidente", Set.of("incidente", "hallazgo", "problema"),
            "usuario", Set.of("usuario", "operador", "persona"));

    private final Map<String, String> indexedContent = new HashMap<>();

    @Override
    public void index(String memoryId, String title, String markdown) {
        indexedContent.put(memoryId, normalize(title + " " + markdown));
    }

    @Override
    public List<SearchHit> search(String query, int topK, SearchFiltersRequest filters) {
        Set<String> queryTokens = tokenize(query);
        List<SearchHit> hits = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexedContent.entrySet()) {
            Set<String> contentTokens = tokenize(entry.getValue());
            double score = similarity(queryTokens, contentTokens);
            if (score > 0) {
                hits.add(new SearchHit(entry.getKey(), score));
            }
        }

        return hits.stream()
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public void clear() {
        indexedContent.clear();
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase();
    }

    private Set<String> tokenize(String input) {
        Set<String> tokens = new HashSet<>();
        for (String token : normalize(input).split("[^a-z0-9áéíóúñ]+")) {
            if (!token.isBlank()) {
                String canonical = canonicalToken(token);
                tokens.add(canonical);
                tokens.addAll(SEMANTIC_GROUPS.getOrDefault(canonical, Set.of()));
            }
        }
        return tokens;
    }

    private String canonicalToken(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Set<String>> entry : SEMANTIC_GROUPS.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        return normalized;
    }

    private double similarity(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return (double) intersection.size() / (double) left.size();
    }
}
