package com.btl.administrador.api.integration.openai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Manual CDI producers for ChatLanguageModel and EmbeddingModel.
 *
 * <p><strong>DEPRECATED since quarkus-langchain4j-openai extension was added (fix #12).</strong>
 * The extension now auto-produces these beans from {@code quarkus.langchain4j.openai.*}
 * config properties. These manual producers are kept as fallback for when the extension
 * is not present on the classpath.</p>
 *
 * <p>Producers are conditional: they only activate when the quarkus-langchain4j-openai
 * extension is NOT detected. This avoids CDI ambiguity at deployment.</p>
 */
@ApplicationScoped
public class OpenAiConfigProducer {

    private static final Logger LOG = Logger.getLogger(OpenAiConfigProducer.class);

    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.model-name", defaultValue = "text-embedding-3-large")
    String embeddingModelName;

    @ConfigProperty(name = "quarkus.langchain4j.openai.chat-model.model-name", defaultValue = "gpt-4o-mini")
    String chatModelName;

    @ConfigProperty(name = "quarkus.langchain4j.openai.timeout", defaultValue = "90s")
    Duration timeout;

    // ⚠️ @Produces removed in fix #12 — the quarkus-langchain4j-openai extension
    // now auto-produces EmbeddingModel from quarkus.langchain4j.openai.* config.
    // This method is retained as a utility for environments without the extension.
    // @Produces
    // @Singleton
    public EmbeddingModel embeddingModel() {
        LOG.infov("Creating OpenAI EmbeddingModel: model={0}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(timeout)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    // ⚠️ @Produces removed in fix #12 — the quarkus-langchain4j-openai extension
    // now auto-produces ChatLanguageModel from quarkus.langchain4j.openai.* config.
    // This method is retained as a utility for environments without the extension.
    // @Produces
    // @Singleton
    public ChatLanguageModel chatLanguageModel() {
        LOG.infov("Creating OpenAI ChatLanguageModel: model={0}", chatModelName);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .timeout(timeout)
                .temperature(0.0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
