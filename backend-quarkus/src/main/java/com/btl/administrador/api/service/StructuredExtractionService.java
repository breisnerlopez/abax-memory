package com.btl.administrador.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class StructuredExtractionService {

    private static final Logger LOG = Logger.getLogger(StructuredExtractionService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EXTRACTION_PROMPT = """
            Eres un extractor de conocimiento operativo. Analiza el siguiente contenido
            y extrae informacion estructurada en formato JSON.

            Debes identificar:
            1. entities: personas, sistemas, herramientas o procesos mencionados
            2. type: tipo de memoria (runbook, procedimiento, incidente, hallazgo, decision, conocimiento)
            3. domain: dominio funcional (redes, seguridad, infraestructura, aplicaciones, operaciones, etc.)
            4. criticality: criticidad inferida del contenido (BAJA, MEDIA, ALTA, CRITICA)
            5. tags: palabras clave relevantes (maximo 10)
            6. steps: pasos o acciones identificados
            7. decisions: decisiones documentadas
            8. evidences: evidencias o fuentes mencionadas
            9. results: resultados o conclusiones
            10. relaciones: entidades relacionadas mencionadas en el texto

            Criterios de criticidad:
            - CRITICA: afecta seguridad, disponibilidad del servicio, datos sensibles o cumplimiento normativo
            - ALTA: afecta procesos operativos principales, requiere aprobacion
            - MEDIA: mejora o documenta procesos existentes
            - BAJA: informacion general o referencia

            Responde UNICAMENTE en formato JSON con esta estructura:
            {
              "entities": ["entidad1", "entidad2"],
              "type": "tipo_inferido",
              "domain": "dominio_inferido",
              "criticality": "BAJA|MEDIA|ALTA|CRITICA",
              "tags": ["tag1", "tag2"],
              "steps": ["paso1", "paso2"],
              "decisions": ["decision1"],
              "evidences": ["evidencia1"],
              "results": ["resultado1"],
              "relaciones": ["relacion1"]
            }

            NO incluyas texto fuera del JSON. NO uses markdown code blocks.
            """;

    private final ChatLanguageModel extractionModel;

    @Inject
    public StructuredExtractionService(
            @ConfigProperty(name = "quarkus.langchain4j.openai.api-key") String apiKey,
            @ConfigProperty(name = "quarkus.langchain4j.openai.chat-model.model-name", defaultValue = "gpt-4o-mini") String modelName,
            @ConfigProperty(name = "quarkus.langchain4j.openai.timeout", defaultValue = "90s") Duration timeout) {
        this.extractionModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .temperature(0.0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    public Map<String, String> enrichMetadata(String body, Map<String, String> originalMetadata) {
        Map<String, String> enriched = new LinkedHashMap<>();
        if (originalMetadata != null) {
            enriched.putAll(originalMetadata);
        }

        if (body == null || body.isBlank()) {
            enriched.put("extractionStatus", "SKIPPED");
            enriched.put("missingFields", "empty_body");
            return enriched;
        }

        try {
            // Truncate body to avoid excessive token usage (model mini context window)
            String truncatedBody = body.length() > 8000 ? body.substring(0, 8000) + "..." : body;
            ChatResponse chatResponse = extractionModel.chat(
                    UserMessage.from(EXTRACTION_PROMPT + "\n\n---\nContenido:\n" + truncatedBody));
            String response = chatResponse.aiMessage().text();

            String json = extractJson(response);
            JsonNode root = OBJECT_MAPPER.readTree(json);

            // Extract entities
            if (root.has("entities") && root.get("entities").isArray()) {
                List<String> entities = new ArrayList<>();
                root.get("entities").forEach(node -> entities.add(node.asText()));
                if (!entities.isEmpty()) {
                    enriched.put("extractedEntities", String.join(" | ", entities));
                }
            }

            // Extract type (if not already set by user)
            if (!enriched.containsKey("type") && root.has("type") && !root.get("type").asText().isBlank()) {
                enriched.put("extractedType", root.get("type").asText().toLowerCase(Locale.ROOT));
            }

            // Extract domain (if not already set)
            if (!enriched.containsKey("domain") && root.has("domain") && !root.get("domain").asText().isBlank()) {
                enriched.put("extractedDomain", root.get("domain").asText().toLowerCase(Locale.ROOT));
            }

            // Extract criticality
            if (root.has("criticality") && !root.get("criticality").asText().isBlank()) {
                enriched.put("extractedCriticality", root.get("criticality").asText().toUpperCase(Locale.ROOT));
            }

            // Extract tags
            if (root.has("tags") && root.get("tags").isArray()) {
                List<String> tags = new ArrayList<>();
                root.get("tags").forEach(node -> tags.add(node.asText()));
                if (!tags.isEmpty()) {
                    enriched.put("extractedTags", String.join(",", tags));
                }
            }

            // Extract steps
            if (root.has("steps") && root.get("steps").isArray()) {
                List<String> steps = new ArrayList<>();
                root.get("steps").forEach(node -> steps.add(node.asText()));
                if (!steps.isEmpty()) {
                    enriched.put("extractedSteps", String.join(" | ", steps));
                }
            }

            // Extract decisions
            if (root.has("decisions") && root.get("decisions").isArray()) {
                List<String> decisions = new ArrayList<>();
                root.get("decisions").forEach(node -> decisions.add(node.asText()));
                if (!decisions.isEmpty()) {
                    enriched.put("extractedDecisions", String.join(" | ", decisions));
                }
            }

            // Extract evidences
            if (root.has("evidences") && root.get("evidences").isArray()) {
                List<String> evidences = new ArrayList<>();
                root.get("evidences").forEach(node -> evidences.add(node.asText()));
                if (!evidences.isEmpty()) {
                    enriched.put("extractedEvidences", String.join(" | ", evidences));
                }
            }

            // Extract results
            if (root.has("results") && root.get("results").isArray()) {
                List<String> results = new ArrayList<>();
                root.get("results").forEach(node -> results.add(node.asText()));
                if (!results.isEmpty()) {
                    enriched.put("extractedResults", String.join(" | ", results));
                }
            }

            // Extract relaciones
            if (root.has("relaciones") && root.get("relaciones").isArray()) {
                List<String> relaciones = new ArrayList<>();
                root.get("relaciones").forEach(node -> relaciones.add(node.asText()));
                if (!relaciones.isEmpty()) {
                    enriched.put("extractedRelaciones", String.join(" | ", relaciones));
                }
            }

            // Determine extraction status
            List<String> missingFields = new ArrayList<>();
            if (!enriched.containsKey("extractedEntities")) missingFields.add("entities");
            if (!enriched.containsKey("extractedSteps")) missingFields.add("steps");
            enriched.put("extractionStatus", missingFields.isEmpty() ? "COMPLETE" : "PARTIAL");
            if (!missingFields.isEmpty()) {
                enriched.put("missingFields", String.join(",", missingFields));
            }

            LOG.infov("AI extraction completed: status={0}, missingFields={1}",
                    enriched.get("extractionStatus"), enriched.getOrDefault("missingFields", "none"));
        } catch (Exception e) {
            LOG.errorv(e, "AI extraction failed, using fallback");
            enriched.put("extractionStatus", "FAILED");
            enriched.put("extractionError", e.getMessage());
        }

        return enriched;
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }
        String trimmed = response.trim();
        // Remove markdown code blocks if present
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            }
        }
        // Find JSON object boundaries
        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        return "{}";
    }
}
