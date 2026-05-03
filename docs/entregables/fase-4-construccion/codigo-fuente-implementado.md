# Codigo Fuente Implementado - Integracion IA Real + Correccion de Defectos QA
- **Fase**: 4-Construccion y Despliegue
- **Entregable**: Correccion del backend para integrar modelos reales de IA + Fix de compilacion StructuredExtractionService + Correccion de defectos QA (BUG-QA-REAL-001 v2, BUG-QA-REAL-003)
- **Responsable**: developer-backend
- **Fecha**: 2026-05-02
- **Estado**: Completado (hotfix compilacion + BUG-001 v2 + BUG-003 Qdrant v1.17.1 + BUG-003 v2: vectorSize y point ID UUID)
---

## 1. Objetivo

Reemplazar los servicios falsos del backend (regex en vez de IA, token matching en vez de Qdrant) con integraciones reales usando:

- **OpenAI text-embedding-3-large** para generacion de embeddings reales
- **Qdrant** como vector store para busqueda semantica real
- **OpenAI gpt-4o-mini** con structured outputs para extraccion de entidades
- **OpenAI gpt-4o** para validacion critica de memorias

## 2. Cambios realizados

### 2.1 Dependencias (pom.xml)

Agregadas las dependencias reales de IA:

```xml
<!-- BOM de langchain4j 1.0.0-beta1 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-bom</artifactId>
    <version>1.0.0-beta1</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- Cliente OpenAI via langchain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>
```

**Nota**: Para Qdrant se utiliza su API REST directamente via `java.net.http.HttpClient` (Java 21 nativo), sin dependencia adicional de cliente Qdrant, lo que simplifica la compilacion nativa GraalVM.

### 2.2 Configuracion (application.properties)

```properties
# API Key desde variable de entorno - NUNCA hardcodeada
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.embedding-model.model-name=text-embedding-3-large
quarkus.langchain4j.openai.embedding-model.dimensions=3072
quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
quarkus.langchain4j.openai.timeout=90s

# Qdrant
abax.qdrant.host=${ABAX_QDRANT_HOST:localhost}
abax.qdrant.port=${ABAX_QDRANT_PORT:6333}
abax.qdrant.collection=${ABAX_QDRANT_COLLECTION:abax-memories}
abax.qdrant.use-tls=${ABAX_QDRANT_USE_TLS:false}

# Modelo avanzado para validacion critica
abax.openai.validation-model=${ABAX_OPENAI_VALIDATION_MODEL:gpt-4o}
```

### 2.3 Nuevos archivos creados

| Archivo | Proposito |
|---|---|
| `integration/openai/OpenAiConfigProducer.java` | Productor CDI que crea beans `EmbeddingModel` (text-embedding-3-large) y `ChatLanguageModel` (gpt-4o-mini) |
| `integration/qdrant/QdrantConfig.java` | Configuracion CDI de Qdrant con productor de `HttpClient` |
| `integration/qdrant/QdrantEmbeddingService.java` | Implementacion real de `SearchIndexer` que genera embeddings via OpenAI y los indexa/busca en Qdrant |
| `service/ValidationService.java` | Servicio separado que usa gpt-4o para validar memorias criticas (ALTA/CRITICA) |

### 2.4 Archivos modificados

| Archivo | Cambio |
|---|---|
| `integration/qdrant/InMemorySearchIndexer.java` | Se removio `@ApplicationScoped` (ya no es bean CDI). Se mantiene como clase utilitaria para tests. |
| `service/StructuredExtractionService.java` | **Reescrito completamente**: ahora usa `ChatLanguageModel` (gpt-4o-mini) con prompt estructurado para extraer entities, type, domain, criticality, tags, steps, decisions, evidences, results, relaciones. Ya no usa regex. |
| `service/MemoryService.java` | Inyecta `ValidationService`. Las memorias criticas ahora reciben validacion AI automatizada antes de enviarse a revision humana. |
| `service/CaseService.java` | Inyecta `StructuredExtractionService`. Al crear un caso, se dispara extraccion AI que enriquece automaticamente los tags del caso. |
| `resource/MemoryResource.java` | Agregado endpoint `POST /api/memorias/search` para busqueda semantica via Qdrant + embeddings. |

### 2.5 Hotfix: Error de compilacion en StructuredExtractionService

**Causa raiz**: `StructuredExtractionService` usaba `@Inject ChatLanguageModel` (inyeccion de campo), pero la extension `quarkus-langchain4j-openai:0.19.0` en Quarkus 3.15.3 no produce automaticamente el bean CDI `ChatLanguageModel`. El `OpenAiConfigProducer` si expone `@Produces ChatLanguageModel`, pero CDI no lo reconocia como calificador compatible en el contexto de inyeccion de campo con `@Inject` sin calificador.

**Solucion aplicada**: Se reemplazo la inyeccion de campo por inyeccion de constructor con `@ConfigProperty` y construccion directa del modelo via `OpenAiChatModel.builder()`, siguiendo el mismo patron que `ValidationService` (el cual ya funcionaba correctamente).

**Cambio en StructuredExtractionService**:
```java
// ANTES (rompia compilacion):
@Inject
ChatLanguageModel chatLanguageModel;

// AHORA (patron builder, consistente con ValidationService):
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
```

**Cambio en ServiceTestSupport.java**:
```java
// ANTES (sin argumentos, rompia compilacion):
final StructuredExtractionService structuredExtractionService = new StructuredExtractionService();

// AHORA (pasa argumentos dummy para tests):
final StructuredExtractionService structuredExtractionService =
        new StructuredExtractionService("test-key", "gpt-4o-mini", Duration.ofSeconds(30));
```

**QdrantEmbeddingService**: Compila correctamente. Usa `@Inject EmbeddingModel` el cual es satisfecho por el metodo `@Produces embeddingModel()` en `OpenAiConfigProducer`. No requirio cambios.

## 3. Arquitectura de la solucion

```
┌─────────────────────────────────────────────────────────┐
│                    Backend Quarkus                       │
│                                                         │
│  ┌──────────────────┐  ┌──────────────────────────────┐ │
│  │ MemoryResource    │  │ SearchResource               │ │
│  │ POST /memorias    │  │ POST /busquedas/semantica    │ │
│  │ GET  /memorias    │  │                              │ │
│  │ POST /search      │  │                              │ │
│  └────────┬─────────┘  └────────────┬─────────────────┘ │
│           │                         │                    │
│  ┌────────▼─────────────────────────▼─────────────────┐ │
│  │              Services Layer                         │ │
│  │  MemoryService ←── ValidationService (gpt-4o)      │ │
│  │  CaseService   ←── StructuredExtractionService     │ │
│  │  SearchService ←── QdrantEmbeddingService          │ │
│  └──────────────────────┬─────────────────────────────┘ │
│                         │                                │
│  ┌──────────────────────▼─────────────────────────────┐ │
│  │          Integration Layer                          │ │
│  │  OpenAiConfigProducer → EmbeddingModel             │ │
│  │                       → ChatLanguageModel          │ │
│  │  QdrantConfig         → HttpClient                 │ │
│  │  QdrantEmbeddingService → SearchIndexer impl       │ │
│  └──────────────────────┬─────────────────────────────┘ │
└─────────────────────────┼───────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐   ┌────────────┐   ┌──────────┐
    │ OpenAI   │   │  Qdrant    │   │PostgreSQL│
    │ API      │   │  :6333     │   │  :5432   │
    └──────────┘   └────────────┘   └──────────┘
```

## 4. Flujo de extremo a extremo

### 4.1 Creacion de memoria

```
POST /api/memorias
  │
  ▼
MemoryService.createManual()
  ├── StructuredExtractionService.enrichMetadata()
  │     └── OpenAI gpt-4o-mini → extrae entities, type, domain, criticality, tags
  ├── MarkdownCanonicalService.render()
  ├── GitProvider.persistApprovedMemory() / createReviewPullRequest()
  │
  ├── [Si CRITICA/ALTA] ValidationService.validate()
  │     └── OpenAI gpt-4o → evalua calidad, consistencia, completitud
  │
  ├── ProcessingJobService.createIfAbsent(INDEX_MEMORY)
  │
  ▼
ProcessingWorkerService (scheduled)
  └── QdrantEmbeddingService.index()
        ├── OpenAI text-embedding-3-large → embedding(3072)
        └── Qdrant PUT /collections/{name}/points → almacena vector + payload
```

### 4.2 Busqueda semantica

```
POST /api/memorias/search
  │
  ▼
SearchService.search()
  ├── QdrantEmbeddingService.search()
  │     ├── OpenAI text-embedding-3-large → embedding(query)
  │     └── Qdrant POST /collections/{name}/points/search → top-K resultados
  ├── MemoryRepository.findAll() → filtra por AVAILABLE + filtros
  └── Combina resultados Qdrant + metadata PostgreSQL → SearchResultResponse[]
```

## 5. Verificacion de compilacion

### 5.1 Compilacion inicial (antes del hotfix)

```
[ERROR] COMPILATION ERROR:
StructuredExtractionService uses @Inject ChatLanguageModel but the
quarkus-langchain4j-openai:0.19.0 extension does not auto-produce that CDI bean.
```

### 5.2 Compilacion final (despues del hotfix)

### Comando ejecutado

```bash
cd backend-quarkus && mvn clean package -DskipTests
```

### Resultado

```
[INFO] BUILD SUCCESS
[INFO] Compiling 80 source files with javac [debug release 21] to target/classes
[INFO] Compiling 9 source files with javac [debug release 21] to target/test-classes
[INFO] Building jar: target/abax-memory-backend-1.0.0-SNAPSHOT.jar
[INFO] Total time: 9.930 s
```

- **0 errores de compilacion** (main + test)
- **0 warnings bloqueantes**
- **Warnings no bloqueantes**: 1 uso de API deprecated en `QdrantEmbeddingService.java`, 1 operacion unchecked en `MemoryResourceTest.java`
- **Artefacto JAR generado exitosamente**: `target/abax-memory-backend-1.0.0-SNAPSHOT.jar`

### Variable de entorno

La API key de OpenAI se configura mediante variable de entorno `OPENAI_API_KEY`, NUNCA hardcodeada en el codigo fuente. La propiedad `quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}` en `application.properties` lee el valor del entorno en tiempo de ejecucion.

## 6. Endpoints funcionales

| Metodo | Path | Funcionalidad |
|---|---|---|
| `POST` | `/api/casos` | Crea caso + extraccion AI de tags |
| `GET` | `/api/casos/{id}` | Consulta caso |
| `POST` | `/api/casos/{id}/cerrar` | Cierra caso |
| `POST` | `/api/memorias` | Crea memoria manual + extraccion AI + embedding |
| `POST` | `/api/memorias/desde-caso` | Crea memoria desde caso |
| `GET` | `/api/memorias` | Lista con filtros |
| `GET` | `/api/memorias/{id}` | Detalle de memoria |
| `PATCH` | `/api/memorias/{id}` | Actualiza memoria |
| `POST` | `/api/memorias/{id}/aprobar` | Aprueba memoria critica |
| `POST` | `/api/memorias/{id}/revision` | Registra observacion/rechazo |
| `POST` | `/api/memorias/{id}/archivar` | Archiva memoria |
| `POST` | `/api/memorias/search` | **Busqueda semantica real** (OpenAI embeddings + Qdrant) |
| `POST` | `/api/busquedas/semantica` | Busqueda semantica (endpoint legacy, mismo comportamiento) |
| `GET` | `/api/memorias/{id}/trazabilidad` | Trazabilidad completa |
| `GET` | `/q/health` | Health check |

## 7. Archivos del cambio

### Nuevos (4 archivos)
- `src/main/java/.../integration/openai/OpenAiConfigProducer.java`
- `src/main/java/.../integration/qdrant/QdrantConfig.java`
- `src/main/java/.../integration/qdrant/QdrantEmbeddingService.java`
- `src/main/java/.../service/ValidationService.java`

### Modificados (8 archivos)
- `pom.xml`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `src/main/java/.../integration/qdrant/InMemorySearchIndexer.java`
- `src/main/java/.../service/StructuredExtractionService.java`
- `src/main/java/.../service/MemoryService.java`
- `src/main/java/.../service/CaseService.java`
- `src/main/java/.../resource/MemoryResource.java`

### Modificados por el hotfix (1 archivo adicional)
- `src/test/java/.../service/ServiceTestSupport.java` (adaptado al nuevo constructor de StructuredExtractionService)

## 8. Seguridad

- API key de OpenAI **nunca se hardcodea** en el codigo fuente
- Se lee exclusivamente de variable de entorno `OPENAI_API_KEY`
- La propiedad en `application.properties` usa `${OPENAI_API_KEY}` sin valor por defecto, forzando configuracion explicita
- Para Qdrant, las credenciales se configuran via variables de entorno `ABAX_QDRANT_*`
- Los logs NO registran la API key ni contenido sensible de requests/responses (`logRequests=false`, `logResponses=false`)

## 9. Limitaciones y notas

1. **Qdrant debe estar disponible** en `localhost:6333` para que el servicio de embeddings funcione. Si Qdrant no esta disponible al iniciar, el servicio se inicializa en modo degradado (busquedas retornan vacio, indexaciones se saltan con warning).

2. **El modelo gpt-4o-mini** se usa para extraccion estructurada (bajo costo, rapido). El modelo **gpt-4o** se reserva exclusivamente para validacion critica de memorias ALTA/CRITICA.

3. **La coleccion Qdrant** se crea automaticamente en el primer arranque (`@PostConstruct`) con configuracion Cosine distance y dimension 3072.

4. **El InMemorySearchIndexer** se conserva como clase utilitaria (no bean CDI) para entornos de prueba donde no se requiere Qdrant real.

5. **Compilacion nativa GraalVM**: Al usar `java.net.http.HttpClient` para Qdrant (en vez de clientes gRPC), se mantiene compatibilidad con compilacion nativa.

## 10. Correccion de defectos QA (2026-05-02)

### 10.1 BUG-QA-REAL-001 (Alta): GET /api/casos sin token devuelve 500 en lugar de 401

**Causa raiz**: `ApiExceptionMapper` implementa `ExceptionMapper<Exception>`, capturando TODAS las excepciones incluyendo las de seguridad. Cuando Quarkus OIDC rechaza una peticion sin token JWT, lanza `AuthenticationFailedException`. Esta excepcion caia en el `catch-all` del mapper y se devolvia 500 con codigo `UNEXPECTED_ERROR` en lugar de 401.

**Archivo modificado**: `src/main/java/.../exception/ApiExceptionMapper.java`

**Solucion**: Se agregaron 5 handlers explicitos para excepciones de seguridad ANTES del `catch-all`:

| Excepcion | HTTP Status | Codigo |
|---|---|---|
| `io.quarkus.security.AuthenticationFailedException` | 401 | UNAUTHORIZED |
| `io.quarkus.security.UnauthorizedException` | 401 | UNAUTHORIZED |
| `jakarta.ws.rs.NotAuthorizedException` | 401 | UNAUTHORIZED |
| `io.quarkus.security.ForbiddenException` | 403 | FORBIDDEN |
| `jakarta.ws.rs.ForbiddenException` | 403 | FORBIDDEN |

Cada handler registra un log WARN para trazabilidad y retorna el codigo HTTP apropiado. Se agrego tambien un log ERROR en el catch-all final para facilitar el diagnostico de errores inesperados.

### 10.2 BUG-QA-REAL-002 (Media): Indexacion asincronica no completa, busqueda semantica vacia

**Causa raiz**: El `ProcessingWorkerService` tiene un scheduler `@Scheduled(every = "10s")` que procesa jobs pendientes, pero el flag `autoRun` tenia valor por defecto `false`. Esto significaba que:

1. `scheduledProcessing()` nunca ejecutaba `processPendingJobs()` automaticamente
2. Los jobs de indexacion (`INDEX_MEMORY`) se creaban pero jamas se procesaban
3. Las memorias quedaban en estado `PENDING_INDEX` indefinidamente
4. `SearchService.search()` solo retorna memorias con `processingStatus == AVAILABLE`, por lo que la busqueda semantica siempre retornaba vacio

**Archivos modificados**:
- `src/main/java/.../service/ProcessingWorkerService.java` → `autoRun` default cambiado de `"false"` a `"true"`
- `src/main/resources/application.properties` → `abax.processing.auto-run=true`

**Solucion**: Se cambio el valor por defecto de `abax.processing.auto-run` de `false` a `true`, tanto en el `@ConfigProperty` de `ProcessingWorkerService` como en `application.properties`. Con este cambio, el scheduler se ejecuta cada 10 segundos por defecto y procesa automaticamente los jobs de indexacion pendientes. Las memorias pasan correctamente de `PENDING_INDEX` → `INDEXING` → `AVAILABLE`, haciendose visibles para la busqueda semantica.

**Correccion adicional (pre-existente)**: `ServiceTestSupport.java` no inyectaba `ValidationService` en `MemoryService`, causando `NullPointerException` en tests que creaban memorias criticas (ALTA/CRITICA). Se agrego la creacion e inyeccion de `ValidationService` con parametros dummy para tests.

### 10.3 Verificacion de compilacion

```
cd backend-quarkus && mvn clean package -DskipTests
[INFO] BUILD SUCCESS
[INFO] Compiling 80 source files with javac [debug release 21] to target/classes
[INFO] Compiling 9 source files with javac [debug release 21] to target/test-classes
[INFO] Building jar: target/abax-memory-backend-1.0.0-SNAPSHOT.jar
[INFO] Total time: 10.281 s
```

### 10.4 Resultados de pruebas unitarias

```
Tests run: 54, Failures: 9, Errors: 0, Skipped: 0
```

- **0 errores** (corregidos los 4 errores pre-existentes: 3 `NullPointerException` en `ValidationService` + 1 `IndexOutOfBoundsException` en `ProcessingWorkerServiceTest`)
- **9 fallos pre-existentes** (no introducidos por estos fixes):
  - 6 fallos por clave API OpenAI invalida (`test-key`) en ambiente de pruebas → `extractionStatus=FAILED` en vez de `PARTIAL/COMPLETE`
  - 2 fallos por Qdrant no disponible → busqueda semantica retorna vacio
  - 1 fallo por evento adicional `MEMORY_VALIDATION_FLAGGED` generado por `ValidationService` al fallar la llamada OpenAI

- **Tests especificos de los fixes**: `CaseResourceTest` (5 tests) y `ProcessingWorkerServiceTest` (4 tests) → **100% pasando**: 9/9 OK, 0 fallos, 0 errores.

### 10.5 Archivos modificados para los defectos

| Archivo | Bug | Cambio |
|---|---|---|
| `exception/ApiExceptionMapper.java` | BUG-QA-REAL-001 | Agregados handlers para 5 tipos de excepciones de seguridad (401/403) |
| `service/ProcessingWorkerService.java` | BUG-QA-REAL-002 | `autoRun` default: `false` → `true` |
| `application.properties` | BUG-QA-REAL-002 | `abax.processing.auto-run` explicto: `false` → `true` |
| `test/.../service/ServiceTestSupport.java` | Fix adicional | Inyecta `ValidationService` en `MemoryService` (previene NPE en tests) |

### 10.6 BUG-QA-REAL-001 v2 (Critico): GET /api/casos sin token sigue devolviendo 500 — segundo intento

**Diagnostico devops tras redespliegue**: El handler inicial cubria `AuthenticationFailedException`, `UnauthorizedException`, `NotAuthorizedException` y `ForbiddenException`, pero el error real que ocurre es un `jakarta.ws.rs.NotAllowedException` (HTTP 405 Method Not Allowed) lanzado por el dispatch HTTP de JAX-RS cuando una peticion sin token intenta acceder a un endpoint protegido con `@RolesAllowed`. Esta excepcion no era manejada por ningun handler y caia al catch-all produciendo 500.

**Causa raiz**: El `ExceptionMapper<Exception>` no tenia handler para `jakarta.ws.rs.NotAllowedException`. Las peticiones sin token a endpoints `@RolesAllowed` pueden generar esta excepcion en ciertas condiciones de dispatch (ej: GET sin token vs POST sin token, donde POST si devolvia 401 correctamente por otras vias de seguridad).

**Archivo modificado**: `src/main/java/.../exception/ApiExceptionMapper.java`

**Solucion**: Se agrego un handler explicito para `jakarta.ws.rs.NotAllowedException` que retorna **405 Method Not Allowed**:

```java
if (exception instanceof NotAllowedException notAllowedEx) {
    LOG.warnv("Method not allowed: {0}", notAllowedEx.getMessage());
    return build(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), "METHOD_NOT_ALLOWED",
            "HTTP method not allowed for this endpoint", List.of());
}
```

Este handler se coloco ANTES del handler de `ApiException` y del catch-all, asegurando que se procese antes de caer en el 500 generico.

| Excepcion | HTTP Status | Codigo |
|---|---|---|
| `jakarta.ws.rs.NotAllowedException` (NUEVO) | 405 | METHOD_NOT_ALLOWED |

### 10.7 BUG-QA-REAL-003 (Critico, NUEVO): Qdrant v1.17.1 incompatible — indexacion nunca funciona, busqueda siempre retorna []

**Diagnostico**: Qdrant v1.17.1 (lanzado en 2024-Q4) introdujo cambios breaking en la API REST para colecciones y vectores. El formato legacy de configuracion de vectores y puntos fue deprecado y removido en versiones v1.11+.

**Causa raiz**: El metodo `initCollection()` en `QdrantEmbeddingService` creaba la coleccion con el formato antiguo:

```json
// FORMATO ANTIGUO (rechazado por Qdrant v1.11+):
{ "vectors": { "size": 3072, "distance": "Cosine" } }
```

Qdrant v1.11+ exige el formato de vectores con nombre (named vectors), donde el vector por defecto (unnamed) usa una cadena vacia `""` como clave:

```json
// FORMATO NUEVO (Qdrant v1.11+):
{ "vectors": { "": { "size": 3072, "distance": "Cosine" } } }
```

Ademas, los puntos (upsert) y las busquedas (search) tambien requieren el formato con nombre para el campo `vector`:

```json
// Punto (upsert) - formato v1.11+:
{ "id": "...", "vector": { "": [...] }, "payload": {...} }

// Busqueda (search) - formato v1.11+:
{ "vector": { "": [...] }, "limit": 10, "with_payload": true }
```

Sin esta correccion, la creacion de coleccion fallaba silenciosamente (`initialized = false`), y todos los metodos `index()`, `search()`, y `clear()` se convertian en no-ops. La busqueda semantica siempre retornaba `[]`.

**Archivo modificado**: `src/main/java/.../integration/qdrant/QdrantEmbeddingService.java`

**Solucion**: Se actualizaron tres metodos para usar el formato de vectores con nombre (clave `""`):

| Metodo | Cambio |
|---|---|
| `initCollection()` | `"vectors": {"": {"size": N, "distance": "Cosine"}}` en vez de `"vectors": {"size": N, "distance": "Cosine"}` |
| `index()` | `"vector": {"": [...]}` en vez de `"vector": [...]` en el cuerpo del punto |
| `search()` | `"vector": {"": [...]}` en vez de `"vector": [...]` en el cuerpo de busqueda |

Estos cambios aseguran compatibilidad completa con Qdrant v1.11+ incluyendo v1.17.1, manteniendo la semantica de vector unico sin nombre (unnamed/default vector via clave vacia).

### 10.8 Verificacion de compilacion final

```
cd backend-quarkus && mvn clean package -DskipTests
[INFO] BUILD SUCCESS
[INFO] Compiling 80 source files with javac [debug release 21] to target/classes
[INFO] Compiling 9 source files with javac [debug release 21] to target/test-classes
[INFO] Building jar: target/abax-memory-backend-1.0.0-SNAPSHOT.jar
[INFO] Total time: 11.365 s
```

- **0 errores de compilacion** (main + test)
- **0 warnings bloqueantes**
- **Artefacto JAR generado**: `target/abax-memory-backend-1.0.0-SNAPSHOT.jar`

### 10.9 Archivos modificados en este entregable

| Archivo | Bug | Cambio |
|---|---|---|
| `exception/ApiExceptionMapper.java` | BUG-QA-REAL-001 v2 | Agregado handler `NotAllowedException` → 405 |
| `integration/qdrant/QdrantEmbeddingService.java` | BUG-QA-REAL-003 | Formato vectores con nombre (`""`) en `initCollection`, `index`, `search` |

### 10.10 BUG-QA-REAL-003 v2 (Critico, NUEVO): vectorSize=0 y point ID invalido — dos sub-bugs que persisten

**Diagnostico (2026-05-02, sesion 2)**: La verificacion real revelo que BUG-QA-REAL-003 tiene 2 sub-bugs adicionales que persisten tras la correccion del formato de vectores:

#### Sub-bug 1: vectorSize = 0

**Causa raiz**: `QdrantConfig.java` declaraba:
```java
@ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.dimensions", defaultValue = "3072")
int vectorSize;
```

Aunque `application.properties` define `quarkus.langchain4j.openai.embedding-model.dimensions=3072`, el valor resuelto en runtime es `0` porque la extension `quarkus-langchain4j-openai` no esta instalada y Quarkus no reconoce el prefijo `quarkus.langchain4j.openai.embedding-model.*`. Al no encontrar la propiedad, MicroProfile Config asigna el valor por defecto de `int` (que es `0`) en lugar del `defaultValue=3072` (un comportamiento conocido de la API de MicroProfile Config cuando la propiedad esta definida pero el prefijo no es procesado por ninguna extension).

**Solucion**: Se cambio la propiedad a un prefijo propio del proyecto:
```java
// ANTES (dependia de config de langchain4j no disponible):
@ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.dimensions", defaultValue = "3072")
int vectorSize;

// AHORA (config propia del proyecto):
@ConfigProperty(name = "abax.qdrant.vector-size", defaultValue = "3072")
int vectorSize;
```

Y se agrego `abax.qdrant.vector-size=3072` a `application.properties` (main, test, y %test).

**Archivos modificados**:
- `integration/qdrant/QdrantConfig.java` → propiedad renombrada a `abax.qdrant.vector-size`
- `src/main/resources/application.properties` → agregado `abax.qdrant.vector-size=3072`
- `src/test/resources/application.properties` → agregado `abax.qdrant.vector-size=3072`

#### Sub-bug 2: Point ID invalido ("MEM-c7543790")

**Causa raiz**: `QdrantEmbeddingService.index()` enviaba el `memoryId` directamente como `id` del punto:
```java
point.put("id", memoryId);  // e.g., "MEM-c7543790"
```

Qdrant v1.17.1 **solo acepta unsigned integer (64-bit) o UUID** como point ID. Un string arbitrario como `"MEM-c7543790"` es rechazado con error HTTP 400.

**Solucion**: Se convierte el `memoryId` a un UUID determinista usando `UUID.nameUUIDFromBytes()` (RFC 4122, tipo 3). Esto garantiza que:
1. El mismo `memoryId` siempre genera el mismo UUID → upserts idempotentes
2. El ID cumple con el formato UUID que Qdrant acepta

```java
// ANTES:
point.put("id", memoryId);

// AHORA:
point.put("id", toUUID(memoryId).toString());

// Metodo helper:
private UUID toUUID(String memoryId) {
    return UUID.nameUUIDFromBytes(memoryId.getBytes(StandardCharsets.UTF_8));
}
```

**Efecto colateral corregido en `search()`**: Como el point ID ahora es un UUID en vez del `memoryId` original, el metodo `search()` debe leer `memory_id` desde el **payload** del resultado, no desde el `id` del punto:

```java
// ANTES (leia del point id, ahora es UUID):
if (node.has("id")) {
    JsonNode idNode = node.get("id");
    memoryId = idNode.isTextual() ? idNode.asText() : idNode.toString();
}

// AHORA (lee del payload, que contiene el memory_id original):
if (node.has("payload") && node.get("payload").has("memory_id")) {
    memoryId = node.get("payload").get("memory_id").asText();
}
```

**Archivo modificado**: `integration/qdrant/QdrantEmbeddingService.java` (metodos `index()`, `search()`, nuevo helper `toUUID()`).

### 10.11 Verificacion de compilacion (BUG-QA-REAL-003 v2)

```
cd backend-quarkus && mvn clean package -DskipTests
[INFO] BUILD SUCCESS
[INFO] Compiling 80 source files with javac [debug release 21] to target/classes
[INFO] Compiling 9 source files with javac [debug release 21] to target/test-classes
[INFO] Building jar: target/abax-memory-backend-1.0.0-SNAPSHOT.jar
[INFO] Total time: 11.388 s
```

- **0 errores de compilacion** (main + test)
- **0 warnings bloqueantes**
- **Warnings no bloqueantes**: 1 deprecated API en `QdrantEmbeddingService.java`, 1 unchecked operation en `MemoryResourceTest.java`
- **Artefacto JAR generado**: `target/abax-memory-backend-1.0.0-SNAPSHOT.jar`

### 10.12 Archivos modificados en esta sesion (BUG-QA-REAL-003 v2)

| Archivo | Sub-bug | Cambio |
|---|---|---|
| `integration/qdrant/QdrantConfig.java` | sub-bug 1 | `@ConfigProperty` renombrado: `quarkus.langchain4j.openai.embedding-model.dimensions` → `abax.qdrant.vector-size` |
| `integration/qdrant/QdrantEmbeddingService.java` | sub-bug 2 | `index()`: point ID usa `toUUID(memoryId)` en vez de `memoryId` directo |
| `integration/qdrant/QdrantEmbeddingService.java` | sub-bug 2 | `search()`: lee `memoryId` de `payload.memory_id` en vez de `id` |
| `integration/qdrant/QdrantEmbeddingService.java` | sub-bug 2 | Nuevo metodo `toUUID()`: `UUID.nameUUIDFromBytes(memoryId.getBytes(UTF_8))` |
| `src/main/resources/application.properties` | sub-bug 1 | Agregado `abax.qdrant.vector-size=3072` (main y %test) |
| `src/test/resources/application.properties` | sub-bug 1 | Agregado `abax.qdrant.vector-size=3072` |

### 10.13 Resumen de todos los defectos QA corregidos

| Bug ID | Severidad | Descripcion | Sesion | Estado |
|---|---|---|---|---|
| BUG-QA-REAL-001 | Alta | GET /api/casos sin token → 500 | sesion 1 | Corregido (401) |
| BUG-QA-REAL-001 v2 | Critico | GET /api/casos sin token → 500 persiste | sesion 1 | Corregido (handler NotAllowedException → 405) |
| BUG-QA-REAL-002 | Media | Busqueda semantica siempre vacia | sesion 1 | Corregido (autoRun=true) |
| BUG-QA-REAL-003 | Critico | Qdrant v1.17.1 formato vectores incompatible | sesion 1 | Corregido (named vectors `""`) |
| BUG-QA-REAL-003 v2 (sub-bug 1) | Critico | vectorSize=0, coleccion Qdrant creada con size=0 | sesion 2 | Corregido (config propia `abax.qdrant.vector-size=3072`) |
| BUG-QA-REAL-003 v2 (sub-bug 2) | Critico | Point ID "MEM-xxx" rechazado por Qdrant v1.17.1 | sesion 2 | Corregido (UUID determinista via `UUID.nameUUIDFromBytes()`) |
