# Verificación de Entorno y Dependencias — Abax-Memory v2.0.0

- **Fase**: 4 — Construcción
- **Responsable**: DevOps / Release Engineer
- **Fecha**: 2026-05-03
- **Estado**: APROBADO por Tech Lead — 2026-05-03
- **Bloqueante**: SÍ — primer entregable de Fase 4

---

## 1. Resumen Ejecutivo

Verificación del runtime, dependencias y build del proyecto Abax-Memory v2.0.0 (backend Quarkus). El proyecto **compila exitosamente** (`mvn validate`, `mvn compile`, `mvn test-compile`). Las **8 dependencias requeridas para v2 están presentes**, incluyendo Testcontainers 1.20.1 (agregado 2026-05-03). El runtime (Java 21, Maven 3.9.9, Docker 29.4.2) cumple todos los requisitos. Se documentan **4 riesgos no bloqueantes** (ver sección 13).

| Indicador | Resultado |
|---|---|
| Compilación | ✅ BUILD SUCCESS |
| Tests compilan | ✅ BUILD SUCCESS |
| Dependencias v2 requeridas | ✅ 8/8 presentes |
| Runtime Java | ✅ 21.0.10 |
| Runtime Maven | ✅ 3.9.9 (SDKMAN) |
| Entorno | Host Linux (no container) |

---

## 2. Verificación de Runtime (Paso 1)

### 2.1 Java

| Campo | Valor |
|---|---|
| Comando | `java -version` |
| Versión detectada | OpenJDK **21.0.10** (2026-01-20) |
| Requerido | Java 21+ |
| Estado | ✅ **PASA** |

```
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10+7-Ubuntu-124.04)
OpenJDK 64-Bit Server VM (build 21.0.10+7-Ubuntu-124.04, mixed mode, sharing)
```

### 2.2 Maven

| Campo | Valor |
|---|---|
| Comando | `mvn --version` |
| Versión detectada | Apache Maven **3.9.9** |
| Requerido | Maven 3.9+ |
| Estado | ✅ **PASA** — actualizado vía SDKMAN (2026-05-03) |

```
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /root/.sdkman/candidates/maven/current
Java version: 21.0.10, vendor: Ubuntu, runtime: /usr/lib/jvm/java-21-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.8.0-58-generic", arch: "amd64", family: "unix"
```

**Análisis**: Maven **3.9.9 instalado vía SDKMAN** (2026-05-03). Cumple con el requisito 3.9+ para Quarkus 3.15.3. SDKMAN gestiona Maven en `~/.sdkman/candidates/maven/current`, sin afectar el Maven del sistema (`/usr/share/maven`).

> **Nota**: Actualización de Maven completada vía SDKMAN (2026-05-03). El PATH de SDKMAN se carga automáticamente desde `.bashrc`. Para usar Maven 3.9.9 en nuevas sesiones: `source "$HOME/.sdkman/bin/sdkman-init.sh"`.

### 2.3 Node.js

| Campo | Valor |
|---|---|
| Comando | `node --version` |
| Versión detectada | **v22.22.0** |
| Requerido | 20+ |
| Estado | ✅ **PASA** |

> **Nota**: Node.js está disponible pero el proyecto v2.0.0 no tiene frontend propio. La presencia de Node.js es útil para tooling auxiliar (linters, pre-commit hooks).

### 2.4 Docker / Podman

| Campo | Valor |
|---|---|
| Comando | `docker --version` |
| Versión detectada | Docker **29.4.2** |
| Podman | No instalado |
| Estado | ✅ **PASA** (Docker disponible) |

---

## 3. Detección de Entorno (Paso 2)

| Indicador | Resultado |
|---|---|
| `/.dockerenv` | No existe → **Host machine** |
| `$ABAX_ISOLATED` | No establecida → **Host machine** |
| Container | **NO** — ejecutando en el host del usuario |

**Implicaciones**:
- **NO** se puede usar `sudo apt-get install` para instalar dependencias faltantes (afectaría el SO del usuario).
- Cualquier instalación debe hacerse con gestores de versiones de usuario (`sdkman`, `nvm`, etc.).
- Para desarrollo reproducible, se recomienda usar el **devcontainer** o `docker compose up` para servicios de infraestructura (PostgreSQL, Qdrant, Keycloak).

---

## 4. Declaración de Dependencias (Paso 4)

### 4.1 Archivo manifest analizado

| Archivo | Ruta |
|---|---|
| POM principal | `backend-quarkus/pom.xml` |
| Quarkus BOM | `io.quarkus.platform:quarkus-bom:3.15.3` |
| LangChain4j BOM | `dev.langchain4j:langchain4j-bom:1.0.0-beta1` |

### 4.2 Matriz de dependencias requeridas para v2.0.0

| # | Dependencia v2 | Grupo/Artifact | En POM | Estado | Notas |
|---|---|---|---|---|---|
| 1 | **Quarkus 3.15.3+** | `io.quarkus.platform:quarkus-bom` | ✅ 3.15.3 | **PASA** | Versión exacta requerida |
| 2 | **PostgreSQL driver** | `io.quarkus:quarkus-jdbc-postgresql` | ✅ Línea 53-55 | **PASA** | |
| 3 | **Qdrant client** | — (custom HTTP) | ⚠️ Sin SDK | **ADVERTENCIA** | Usa `java.net.http.HttpClient` directo, sin SDK tipado |
| 4 | **Keycloak OIDC** | `io.quarkus:quarkus-oidc` | ✅ Línea 74-75 | **PASA** | + `quarkus-smallrye-jwt` para JWT verification |
| 5 | **OpenAI client** | `dev.langchain4j:langchain4j-open-ai:1.0.0-beta1` | ✅ Línea 90-92 | **PASA** | LangChain4j puro, NO la extensión Quarkiverse |
| 6 | **Flyway** | `io.quarkus:quarkus-flyway` | ✅ Línea 62-64 | **PASA** | Migraciones en `db/migration/V1__baseline.sql` |
| 7 | **Testcontainers** | `org.testcontainers:testcontainers`, `postgresql`, `qdrant` | ✅ 1.20.1 | **PASA** | Agregado a `pom.xml` (líneas 119-134) — 2026-05-03. Versión gestionada por `quarkus-bom` |
| 8 | **RestAssured** | `io.rest-assured:rest-assured` | ✅ Línea 99-102 | **PASA** | Scope test |

**Resumen**: **8/8** dependencias presentes. **1 requiere atención futura** (Qdrant sin SDK tipado — aceptable para inicio de construcción, evaluar migración en v2.1).

### 4.3 Análisis detallado de hallazgos

#### 4.3.1 ✅ Testcontainers — RESUELTO (2026-05-03)

Testcontainers fue agregado a `pom.xml` (líneas 119-134) con los tres artefactos necesarios para v2.0.0:

```xml
<!-- Testcontainers para tests de integracion v2 (version gestionada por quarkus-bom) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>qdrant</artifactId>
    <scope>test</scope>
</dependency>
```

**Versión resuelta**: `1.20.1` (gestionada por el BOM de Quarkus 3.15.3, confirmada en `mvn dependency:tree`).

**Verificación**:
- `mvn compile` → BUILD SUCCESS ✅
- `mvn test-compile` → BUILD SUCCESS ✅
- `mvn dependency:tree` → `org.testcontainers:testcontainers:jar:1.20.1:test` ✅
- `org.testcontainers:postgresql:jar:1.20.1:test` ✅
- `org.testcontainers:qdrant:jar:1.20.1:test` ✅

> **Nota**: La configuración de tests (`application.properties`, perfil `%test`) mantiene **H2 en memoria** para tests unitarios rápidos. Testcontainers se usará para tests de integración v2 que requieran servicios reales (PostgreSQL, Qdrant).

#### 4.3.2 ⚠️ Qdrant — Integración HTTP custom (sin SDK)

El proyecto **no utiliza** un SDK de Qdrant (ni `io.quarkiverse.langchain4j:quarkus-langchain4j-qdrant` ni `io.qdrant:client`). En su lugar, implementa una integración HTTP manual:

| Archivo | Rol |
|---|---|
| `QdrantConfig.java` | Configuración (`abax.qdrant.*`) y construcción de URLs |
| `QdrantEmbeddingService.java` | Cliente HTTP raw (`java.net.http.HttpClient`) — implementa `SearchIndexer` |
| `SearchIndexer.java` | Interfaz de contrato (index, search, remove, clear) |
| `InMemorySearchIndexer.java` | Stub en memoria (⚠️ NO usar en producción) |

**Riesgos de la implementación custom**:
- Sin tipado fuerte de la API Qdrant (points, payloads, filtros).
- Manejo manual de serialización JSON (`JsonObject`, `JsonArray`).
- Sin retry/backoff/circuit-breaker del SDK oficial.
- El `InMemorySearchIndexer` es un **stub** — la anti-mock-review de v1 ya detectó este patrón como riesgo.

**Recomendación para v2**: Evaluar migrar a `quarkus-langchain4j-qdrant` (Quarkiverse) o al SDK oficial `io.qdrant:client`.

#### 4.3.3 ⚠️ OpenAI — LangChain4j puro (sin extensión Quarkiverse)

El proyecto usa `dev.langchain4j:langchain4j-open-ai:1.0.0-beta1` (LangChain4j puro) y crea beans manualmente en `OpenAiConfigProducer.java`:

```java
@Produces @Singleton
public EmbeddingModel embeddingModel() {
    return OpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelName)
        .build();
}
```

**No utiliza** `io.quarkiverse.langchain4j:quarkus-langchain4j-openai` (extensión Quarkiverse), lo cual implica:
- Sin auto-configuración de beans CDI.
- Sin integración nativa con Quarkus Dev Services.
- Sin métricas/tracing automáticos.
- Las propiedades `quarkus.langchain4j.openai.*` en `application.properties` son leídas manualmente por `OpenAiConfigProducer`.

La extensión Quarkiverse (versión `0.23.0` compatible con Quarkus 3.15) proporcionaría configuración declarativa sin código boilerplate.

---

## 5. Verificación de Build (Paso 5)

### 5.1 `mvn validate`

```
[INFO] Building abax-memory-backend 1.0.0-SNAPSHOT
[INFO] BUILD SUCCESS
[INFO] Total time: 2.993 s
```

### 5.2 `mvn compile`

```
[INFO] --- maven-compiler-plugin:3.13.0:compile ---
[INFO] BUILD SUCCESS
[INFO] Total time: 4.983 s
```

### 5.3 `mvn test-compile`

```
[INFO] --- maven-compiler-plugin:3.13.0:testCompile ---
[INFO] BUILD SUCCESS
[INFO] Total time: 4.358 s
```

**Todas las fases de build pasan exitosamente.** El artefacto generado existe en:
```
backend-quarkus/target/quarkus-app/
backend-quarkus/target/abax-memory-backend-1.0.0-SNAPSHOT.jar
```

### 5.4 Árbol de dependencias relevantes

```
dev.langchain4j:langchain4j-open-ai:jar:1.0.0-beta1:compile
  └─ dev.langchain4j:langchain4j-core:jar:1.0.0-beta1:compile
io.quarkus:quarkus-jdbc-postgresql:jar:3.15.3:compile
io.quarkus:quarkus-oidc:jar:3.15.3:compile
io.quarkus:quarkus-flyway:jar:3.15.3:compile
io.rest-assured:rest-assured:jar (test)
```

No se encontraron en el árbol:
- `testcontainers`
- `qdrant` (ningún artifact)
- `quarkiverse-langchain4j`

---

## 6. Estructura del Proyecto

```
Abax-Memory/
├── backend-quarkus/              # Backend Quarkus 3.15.3
│   ├── pom.xml                   # Manifest Maven (147 líneas)
│   ├── src/main/java/            # Código fuente
│   │   └── com/btl/administrador/api/
│   │       ├── domain/           # Entidades de negocio (8 archivos)
│   │       ├── dto/              # Objetos de transferencia (13 archivos)
│   │       ├── resource/         # Endpoints REST (4 archivos)
│   │       ├── service/          # Lógica de negocio (7 archivos)
│   │       ├── persistence/      # Repositorios (12 archivos: 6 interfaces + 6 Postgres + 6 InMemory)
│   │       ├── integration/     # Integraciones externas
│   │       │   ├── qdrant/       # Qdrant (custom HTTP, 3 archivos)
│   │       │   ├── openai/       # OpenAI (LangChain4j, 1 archivo)
│   │       │   └── git/          # Git provider (stub InMemory, 2 archivos)
│   │       ├── exception/        # Manejo de errores (4 archivos)
│   │       └── security/         # Roles RBAC (1 archivo)
│   ├── src/main/resources/
│   │   ├── application.properties  # Config principal (51 líneas)
│   │   └── db/migration/
│   │       └── V1__baseline_operational_store.sql  # Flyway
│   └── src/test/java/            # Tests (8 archivos)
├── Dockerfile                     # Imagen Quarkus JRE 21
├── docker-compose.yml             # Servicios: PostgreSQL, Qdrant, Keycloak, Backend
├── docs/                          # Documentación del proyecto
└── .github/                       # CI/CD workflows
```

### 6.1 Servicios de infraestructura (`docker-compose.yml`)

| Servicio | Imagen | Puerto | Healthcheck |
|---|---|---|---|
| PostgreSQL | `postgres:16-alpine` | 5432 | `pg_isready` |
| Qdrant | `qdrant/qdrant:v1.17.1` | 6333 (REST), 6334 (gRPC) | `/healthz` |
| Keycloak | `quay.io/keycloak/keycloak:26.1` | 8443 (host) → 8080 (container) | `/health/ready` |
| Backend | `ghcr.io/breisnerlopez/abax-memory:latest` | 8080 | `/q/health/live` |

---

## 7. Hallazgos de Seguridad

| # | Hallazgo | Severidad | Acción |
|---|---|---|---|
| SEC-01 | Secret en `application.properties`: OIDC client secret hardcodeado (`ZN8NB5ra...`) | 🔴 ALTA | Migrar a variable de entorno o vault |
| SEC-02 | `docker-compose.yml` contiene credenciales en texto plano (`pmoa`/`pmoa`) | 🟡 MEDIA | Aceptable solo para desarrollo local |
| SEC-03 | Secret en `application.properties`: Keycloak admin password (`admin`/`admin`) | 🟡 MEDIA | Aceptable solo para desarrollo local |
| SEC-04 | `OPENAI_API_KEY` se inyecta via `${OPENAI_API_KEY}` — buena práctica | ✅ OK | |
| SEC-05 | `quarkus.test-security` presente — permite testing sin OIDC real | ✅ OK | |

---

## 8. Variable de Entorno Requeridas

### 8.1 Obligatorias para ejecución

| Variable | Propósito | Dónde se configura |
|---|---|---|
| `OPENAI_API_KEY` | Clave de API de OpenAI | `docker-compose.yml` → `${OPENAI_API_KEY}` |
| `QUARKUS_DATASOURCE_JDBC_URL` | URL de conexión PostgreSQL | `docker-compose.yml` (servicio abax-memory) |
| `QUARKUS_DATASOURCE_USERNAME` | Usuario PostgreSQL | `docker-compose.yml` |
| `QUARKUS_DATASOURCE_PASSWORD` | Contraseña PostgreSQL | `docker-compose.yml` |

### 8.2 Qdrant

| Variable | Default | Descripción |
|---|---|---|
| `ABAX_QDRANT_HOST` | `localhost` | Host del servidor Qdrant |
| `ABAX_QDRANT_PORT` | `6333` | Puerto REST de Qdrant |
| `ABAX_QDRANT_COLLECTION` | `abax-memories` | Nombre de la colección |
| `ABAX_QDRANT_USE_TLS` | `false` | Usar TLS para la conexión |
| `ABAX_QDRANT_VECTOR_SIZE` | `3072` | Dimensionalidad del embedding |

### 8.3 Keycloak OIDC

| Variable | Default | Descripción |
|---|---|---|
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `http://localhost:8443/realms/abax-memory` | URL del servidor de autenticación |
| `QUARKUS_OIDC_CLIENT_ID` | `abax-memory-api` | Client ID registrado en Keycloak |
| `QUARKUS_OIDC_CREDENTIALS_SECRET` | *(hardcodeado en properties)* | Client secret |

### 8.4 OpenAI / Modelos

| Variable | Default | Descripción |
|---|---|---|
| `ABAX_OPENAI_VALIDATION_MODEL` | `gpt-4o` | Modelo para validación (más potente) |
| `quarkus.langchain4j.openai.chat-model.model-name` | `gpt-4o-mini` | Modelo de chat por defecto |
| `quarkus.langchain4j.openai.embedding-model.model-name` | `text-embedding-3-large` | Modelo de embeddings |

---

## 9. Comandos de Build, Test y Run

### 9.1 Build

```bash
# Compilación (sin tests)
cd backend-quarkus
mvn compile

# Empaquetado (JAR)
mvn package -DskipTests

# Build nativo (requiere GraalVM)
mvn package -Pnative -DskipTests
```

### 9.2 Test

```bash
# Tests unitarios (usan H2 en memoria, no requieren Docker)
mvn test

# Tests con perfil específico
mvn test -Dtest=MemoryResourceTest
```

### 9.3 Run — Desarrollo Local sin Docker

```bash
# Requiere servicios externos corriendo (PostgreSQL, Qdrant, Keycloak)
# Opción A: Levantar solo infraestructura con Docker
docker compose up -d postgres qdrant keycloak

# Opción B: Quarkus Dev Mode (hot reload)
cd backend-quarkus
export OPENAI_API_KEY="sk-..."
export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/pmoadb"
export QUARKUS_DATASOURCE_USERNAME="pmoa"
export QUARKUS_DATASOURCE_PASSWORD="pmoa"
export ABAX_QDRANT_HOST="localhost"
mvn quarkus:dev
```

### 9.4 Run — Stack Completo con Docker

```bash
# Levantar todo el stack
export OPENAI_API_KEY="sk-..."
docker compose up -d

# Verificar salud
curl http://localhost:8080/q/health
curl http://localhost:6333/healthz
curl http://localhost:8443/realms/abax-memory

# Detener
docker compose down
```

---

## 10. Conflictos de Git Detectados

Durante la preparación del entorno para este entregable, al hacer checkout a la rama de desarrollo `abax/abax-memory` y restaurar el stash, se detectaron **merge conflicts** en:

| Archivo | Origen del conflicto |
|---|---|
| `docs/bitacora.md` | v2 content agregado en `main`, divergente del branch `abax/abax-memory` |
| `docs/registro-entregables.md` | v2 content agregado en `main`, divergente del branch `abax/abax-memory` |

**Estado**: Conflictos **NO resueltos**. Se escala al orquestador para decidir estrategia de merge. Los archivos afectados no son de mi autoría (corresponden a fases anteriores).

---

## 11. Plan de Acción Recomendado

| # | Acción | Prioridad | Responsable |
|---|---|---|---|
| 1 | ~~Actualizar Maven a 3.9+~~ ✅ **COMPLETADO** (Maven 3.9.9 vía SDKMAN, 2026-05-03) | ALTA | DevOps ✅ |
| 2 | ~~Agregar dependencia `testcontainers` (PostgreSQL + Qdrant) en `pom.xml`~~ ✅ **COMPLETADO** (2026-05-03) | ALTA | Developer Backend ✅ |
| 3 | Evaluar migración a `quarkus-langchain4j-qdrant` (SDK tipado Qdrant) | MEDIA | Tech Lead |
| 4 | Evaluar migración a `quarkus-langchain4j-openai` (Quarkiverse extension) | MEDIA | Tech Lead |
| 5 | Remover secret hardcodeado `QUARKUS_OIDC_CREDENTIALS_SECRET` de `application.properties` | ALTA | DevOps |
| 6 | Resolver git conflicts en `bitacora.md` y `registro-entregables.md` | ALTA | Orquestador |
| 7 | Agregar `quarkus-jacoco` o `quarkus-junit5-mockito` para coverage reporting | BAJA | Developer Backend |

---

## 12. Veredicto de Gate

| Gate | Estado | Condición |
|---|---|---|
| Runtime (Java 21) | ✅ | |
| Runtime (Maven 3.9+) | ✅ | Maven 3.9.9 vía SDKMAN |
| Build (`mvn compile`) | ✅ | |
| Dependencias v2 completas | ✅ | 8/8 presentes (Testcontainers agregado 2026-05-03) |
| Entorno documentado | ✅ | `docs/setup.md` generado |

**Gate de entrada a Construcción v2**: ✅ **APROBADO** — Todas las dependencias requeridas están presentes (8/8). Maven 3.9.9 actualizado. Testcontainers 1.20.1 agregado a `pom.xml`. Compilación y test-compilación exitosas. El Qdrant HTTP client custom es aceptable para iniciar construcción (migrar a SDK tipado se evalúa para v2.1).

---

## Glosario

- **Quarkus**: Framework Java nativo para Kubernetes que optimiza el tiempo de arranque y consumo de memoria.
- **LangChain4j**: Framework Java para construir aplicaciones con LLMs (Large Language Models), equivalente a LangChain para Python.
- **Testcontainers**: Librería que permite ejecutar bases de datos y servicios reales en contenedores Docker durante tests de integración.
- **OIDC**: OpenID Connect — protocolo de autenticación sobre OAuth 2.0, usado por Keycloak.
- **Flyway**: Herramienta de migración de base de datos versionada (control de cambios de esquema SQL).
- **RestAssured**: Librería Java para testing de APIs REST con sintaxis fluida estilo BDD.
- **Stub / InMemory**: Implementación simulada de un servicio que opera en memoria RAM sin conexión al servicio real — útil para desarrollo pero riesgo en producción.

---

## 13. Aprobación Tech Lead

| Campo | Valor |
|---|---|
| **Revisor** | Tech Lead |
| **Fecha** | 2026-05-03 |
| **Veredicto** | ✅ **APROBADO** |
| **Gate** | **ENV-VERIFICATION APPROVED. Fase 4 Construcción puede comenzar.** |

### Evidencia revisada

- `pom.xml` — Testcontainers presente (líneas 119-134, 3 artefactos, v1.20.1)
- `mvn compile` → BUILD SUCCESS
- `mvn test-compile` → BUILD SUCCESS
- `mvn dependency:tree` → `org.testcontainers:*:1.20.1:test` confirmado
- Java 21.0.10, Maven 3.9.9 (SDKMAN), Docker 29.4.2, Node 22.22.0

### Riesgos aceptados (no bloqueantes)

| # | Riesgo | Mitigación |
|---|---|---|
| R1 | Qdrant HTTP client custom sin SDK tipado (sin retry/backoff, JSON manual) | Aceptable para inicio. Evaluar migración a `quarkus-langchain4j-qdrant` en v2.1. El `InMemorySearchIndexer` NO debe usarse en producción — ya identificado por anti-mock-review de v1. |
| R2 | Secret OIDC hardcodeado en `application.properties` (SEC-01) | No bloquea construcción. Debe resolverse antes de despliegue a producción. Acción asignada a DevOps en Plan de Acción #5. |
| R3 | LangChain4j usado sin extensión Quarkiverse (configuración manual de beans) | Deuda técnica menor. No bloquea. La extensión Quarkiverse (`quarkus-langchain4j-openai`) simplificaría configuración pero es funcionalmente equivalente. |
| R4 | Git conflicts en `bitacora.md` y `registro-entregables.md` | Escalados al Orquestador. No afectan el código fuente.
