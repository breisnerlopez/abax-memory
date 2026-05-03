package com.btl.administrador.api.integration.qdrant;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@ApplicationScoped
public class QdrantConfig {

    private static final Logger LOG = Logger.getLogger(QdrantConfig.class);

    @ConfigProperty(name = "abax.qdrant.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "abax.qdrant.port", defaultValue = "6333")
    int port;

    @ConfigProperty(name = "abax.qdrant.collection", defaultValue = "abax-memories")
    String collection;

    @ConfigProperty(name = "abax.qdrant.use-tls", defaultValue = "false")
    boolean useTls;

    @ConfigProperty(name = "abax.qdrant.vector-size", defaultValue = "3072")
    int vectorSize;

    @PostConstruct
    void init() {
        LOG.infov("QdrantConfig initialized: vectorSize={0}, port={1}, collection={2}, host={3}", 
                vectorSize, port, collection, host);
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public String getCollection() {
        return collection;
    }

    public String baseUrl() {
        String protocol = useTls ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }

    public URI collectionUri() {
        return URI.create(baseUrl() + "/collections/" + collection);
    }

    public URI pointsUri() {
        return URI.create(baseUrl() + "/collections/" + collection + "/points");
    }

    @Produces
    @Singleton
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

}
