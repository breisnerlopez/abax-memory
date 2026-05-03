# ============================================================
# Abax-Memory Backend Docker Image
# Stack: Quarkus 3.15.3 + Java 21
# ============================================================

FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="Abax-Memory"
LABEL org.opencontainers.image.description="Backend API-first de memoria operativa con IA (OpenAI + Qdrant)"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="breisnerlopez"
LABEL org.opencontainers.image.source="https://github.com/breisnerlopez/abax-memory"

WORKDIR /app

# Copy the Quarkus application (fast-jar layout)
COPY backend-quarkus/target/quarkus-app/lib/ /app/lib/
COPY backend-quarkus/target/quarkus-app/*.jar /app/
COPY backend-quarkus/target/quarkus-app/app/ /app/app/
COPY backend-quarkus/target/quarkus-app/quarkus/ /app/quarkus/

# Expose backend port
EXPOSE 8080

# Healthcheck via Quarkus liveness probe
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=15s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/q/health/live 2>/dev/null || exit 1

# OpenAI API Key — MUST be provided at runtime, NEVER hardcoded
ENV OPENAI_API_KEY=""

# Run the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
