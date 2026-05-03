package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryRecord;
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

@ApplicationScoped
public class ValidationService {

    private static final Logger LOG = Logger.getLogger(ValidationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String VALIDATION_PROMPT = """
            Eres un validador critico de memorias operativas. Revisa el siguiente contenido
            y determina si cumple con los estandares de calidad y completitud requeridos.

            Evalua:
            1. Claridad y precision del contenido
            2. Completitud de la informacion (pasos, decisiones, resultados)
            3. Consistencia con el tipo de memoria y dominio indicado
            4. Ausencia de contradicciones o ambiguedades
            5. Trazabilidad con casos o evidencias mencionados

            Responde UNICAMENTE en formato JSON con esta estructura:
            {
              "valid": true/false,
              "score": 0.0-1.0,
              "issues": ["problema1", "problema2"],
              "recommendation": "recomendacion breve",
              "requiresHumanReview": true/false
            }

            NO incluyas texto fuera del JSON. NO uses markdown code blocks.
            """;

    private final ChatLanguageModel validationModel;

    @Inject
    public ValidationService(
            @ConfigProperty(name = "abax.openai.validation-model", defaultValue = "gpt-4o") String modelName,
            @ConfigProperty(name = "quarkus.langchain4j.openai.api-key") String apiKey,
            @ConfigProperty(name = "quarkus.langchain4j.openai.timeout", defaultValue = "90s") Duration timeout) {
        this.validationModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .temperature(0.0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    public ValidationResult validate(MemoryRecord memory, String markdown) {
        if (memory.criticality != Criticality.CRITICA && memory.criticality != Criticality.ALTA) {
            return ValidationResult.skip();
        }

        try {
            String content = buildValidationContent(memory, markdown);
            ChatResponse chatResponse = validationModel.chat(
                    UserMessage.from(VALIDATION_PROMPT + "\n\n---\nContenido a validar:\n" + content));
            String response = chatResponse.aiMessage().text();

            String json = extractJson(response);
            JsonNode root = OBJECT_MAPPER.readTree(json);

            boolean valid = root.has("valid") && root.get("valid").asBoolean();
            double score = root.has("score") ? root.get("score").asDouble() : 0.0;
            boolean requiresHumanReview = root.has("requiresHumanReview") && root.get("requiresHumanReview").asBoolean();

            LOG.infov("Validation for memory {0}: valid={1}, score={2}, requiresHumanReview={3}",
                    memory.id, valid, score, requiresHumanReview);

            return new ValidationResult(valid, score, requiresHumanReview);
        } catch (Exception e) {
            LOG.errorv(e, "Validation failed for memory {0}", memory.id);
            return ValidationResult.error("Validation service error: " + e.getMessage());
        }
    }

    private String buildValidationContent(MemoryRecord memory, String markdown) {
        return String.format("""
                        Tipo: %s
                        Dominio(s): %s
                        Criticidad: %s
                        Estado: %s
                        ---
                        %s""",
                memory.type,
                String.join(", ", memory.domains),
                memory.criticality.name(),
                memory.state.name(),
                markdown != null ? markdown : "");
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

    public record ValidationResult(boolean valid, double score, boolean requiresHumanReview,
                                   String error, boolean skipped) {
        public ValidationResult(boolean valid, double score, boolean requiresHumanReview) {
            this(valid, score, requiresHumanReview, null, false);
        }

        public static ValidationResult skip() {
            return new ValidationResult(true, 1.0, false, null, true);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, 0.0, true, message, false);
        }
    }
}
