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
 * CDI producers for ChatLanguageModel and EmbeddingModel — v1 baseline.
 *
 * <p>These producers create the primary CDI beans for {@link ChatLanguageModel}
 * and {@link EmbeddingModel}. The {@code quarkus-langchain4j-openai} extension
 * (added in fix #12) produces default beans via {@code SyntheticBeanBuildItem},
 * which have lower priority — our {@code @Produces} beans take precedence,
 * avoiding ambiguity.</p>
 *
 * <p><strong>Fix #13 (revised):</strong> Previously the {@code @Produces}
 * annotations were removed in fix #12 under the assumption that the extension
 * would auto-produce these beans for direct {@code @Inject} usage.  The
 * extension only creates beans when an AI Service ({@code @RegisterAiService})
 * or a direct injection point is detected at build time — and since v2 uses
 * an abstraction layer ({@link com.abax.memory.domain.service.LlmService}),
 * no such injection point existed, leaving {@code ChatLanguageModel}
 * unresolvable.  We now re-enable these {@code @Produces} to make the beans
 * available for direct CDI injection throughout the application.</p>
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

    @Produces
    @Singleton
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

    @Produces
    @Singleton
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
