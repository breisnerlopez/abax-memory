# Code Review Anti-Mock — Abax-Memory v2.0.0

- **Fase**: 4 — Construcción
- **Responsable**: tech-lead
- **Versión**: 2.0.0
- **Estado**: Completado (re-evaluado)
- **Resultado**: ⚠️ APROBADO CON OBSERVACIONES — 0 mocks silenciosos, 4 observaciones no bloqueantes
- **Iteraciones**:
  - v1.0.0: 2026-05-03 — tech-lead — ❌ RECHAZADO (InMemoryGitProvider sin marca)
  - v2.0.0: 2026-05-03 — tech-lead — ⚠️ APROBADO CON OBSERVACIONES (ver bloque "## Re-evaluación 2026-05-03")

---

## 1. Matriz de Integraciones

Cada integración declarada en el technical-design (fase 3) se evalúa contra la evidencia en el código de producción.

| Integración | Módulo | Declarada | Dep en pom.xml | Import en src/main | Cliente instanciado | Test integración | Estado |
|---|---|---|---|---|---|---|---|
| **PostgreSQL** | btl + v2 | ✅ | `quarkus-jdbc-postgresql` (compile) | Panache, Hibernate, Flyway | `Postgres*Repository` (6 beans CDI) | ❌ No IT | ✅ REAL |
| **OpenAI Chat** (`gpt-4o-mini`, `gpt-4o`) | btl | ✅ | `langchain4j-open-ai` (compile) | `OpenAiChatModel` | `OpenAiConfigProducer` + directo en `StructuredExtractionService`, `ValidationService` | ❌ No IT | ✅ REAL |
| **OpenAI Embeddings** (`text-embedding-3-large`) | btl | ✅ | `langchain4j-open-ai` (compile) | `OpenAiEmbeddingModel` | `OpenAiConfigProducer` → `QdrantEmbeddingService` | ❌ No IT | ✅ REAL |
| **Qdrant** (vector search) | btl | ✅ | `qdrant` (**test scope** ⚠️) | N/A (raw HTTP) | `QdrantEmbeddingService` (HTTP directo a Qdrant) | ❌ No IT | ✅ REAL |
| **OpenAI Embeddings** (`text-embedding-3-large`) | v2 | ✅ | `langchain4j-open-ai` (compile) | `EmbeddingProvider` (custom iface) | `InMemoryEmbeddingProvider` | ❌ No IT | ⚠️ MOCK (convencional) |
| **Qdrant** (vector search) | v2 | ✅ | `qdrant` (**test scope** ⚠️) | `QdrantClient` (custom iface) | `InMemoryQdrantClient` | ❌ No IT | ⚠️ MOCK (convencional) |
| **Keycloak OIDC** | v2 | ✅ | `quarkus-oidc` (implícito) | `TenantContext`, `TenantFilter` | Header simulado (`X-Tenant-Id`, `X-Role`) | ❌ No IT | ⚠️ MOCK (convencional) |
| **Git** (persistencia PR) | btl | ✅ | Ninguna | `GitProvider` (custom iface) | `InMemoryGitProvider` (único bean CDI) | ❌ No IT | ❌ MOCK SILENCIOSO |
| **Keycloak OIDC** | Frontend | ✅ | N/A | `api.ts` | Headers hardcodeados (`tenant-001`, `memory-admin`) | N/A | ⚠️ MOCK (convencional) |

---

## 2. Hallazgos de Mocks — Detalle

### 2.1 Mock Silencioso Crítico (RECHAZO)

#### `InMemoryGitProvider.java` — Sin convención, activo en producción

| Campo | Detalle |
|---|---|
| **Archivo** | `backend-quarkus/src/main/java/com/btl/administrador/api/integration/git/InMemoryGitProvider.java` |
| **Línea** | 10–11 |
| **CDI scope** | `@ApplicationScoped` |
| **Implementa** | `GitProvider` (única implementación en el classpath) |
| **Inyectado en** | `MemoryService.java:69` → `@Inject GitProvider gitProvider` |
| **Convención** | ❌ **AUSENTE** — No tiene `// MOCK:` ni `// REPLACE_BEFORE_PROD` |
| **Riesgo** | 🔴 **CRÍTICO** — Toda memoria aprobada que pasa por `MemoryService` genera un commit falso (`commit-<UUID>`) sin persistir en Git real. Los Pull Requests de revisión también son simulados. |
| **Acción requerida** | **RECHAZAR** — Debe marcarse con `// MOCK: Git provider not integrated // REPLACE_BEFORE_PROD` o implementarse con integración real (GitHub/GitLab API). Bloquea el pase a Fase 5. |

**Código incriminado** (`InMemoryGitProvider.java:13-14`):
```java
@ApplicationScoped  // ← CDI lo recoge automáticamente
public class InMemoryGitProvider implements GitProvider {
    // Sin marca MOCK/REPLACE_BEFORE_PROD
    public GitPersistResult persistApprovedMemory(...) {
        return new GitPersistResult("commit-" + UUID.randomUUID(), null); // ← commit fantasma
    }
}
```

---

### 2.2 Mocks Convencionales (Aceptables con seguimiento)

Estos mocks tienen la marca `// MOCK: <razón> // REPLACE_BEFORE_PROD`. Son aceptables bajo la política de mock temporal, **siempre que** exista un plan de reemplazo documentado y un ticket de bloqueo para el deployment.

| # | Archivo:línea | Integración | Razón documentada | Severidad |
|---|---|---|---|---|
| 1 | `InMemoryQdrantClient.java:29–30` | Qdrant v2 | Servidor Qdrant no disponible en entorno de build | 🔴 Alta |
| 2 | `InMemoryEmbeddingProvider.java:21–22` | OpenAI Embeddings v2 | API key no disponible en entorno de build | 🔴 Alta |
| 3 | `TenantFilter.java:27–29,74–75` | Keycloak OIDC v2 | MVP usa header simulado, JWT pendiente | 🟡 Media |
| 4 | `TenantContext.java:26–29,37–38` | Keycloak OIDC v2 | Ídem | 🟡 Media |
| 5 | `MemoryResourceV2.java:75–81` | Keycloak OIDC v2 | Extracción de tenant sin validación OIDC | 🟡 Media |
| 6 | `SearchResourceV2.java:77–78` | Keycloak OIDC v2 | Ídem | 🟡 Media |
| 7 | `SearchResourceV2.java:278–279` | RBAC v2 | Simulación de roles via `X-Role` header | 🟡 Media |
| 8 | `MemoryServiceImpl.java:165–166` | Indexación async | Indexación síncrona con stubs in-memory | 🟢 Baja |
| 9 | `MemoryServiceImpl.java:609–610` | Identidad usuario | Usa tenant ID como actor, sin JWT `sub` | 🟡 Media |
| 10 | `SearchServiceImpl.java:64–65` | Qdrant collection name | Hardcodeado, sin configuración por tenant | 🔴 Alta |
| 11 | `SearchServiceImpl.java:329–331` | Qdrant retry | Sin retry con backoff exponencial | 🟢 Baja |
| 12 | `SearchServiceImpl.java:440–441` | Full-text scoring | Keyword frequency en lugar de `ts_rank` o BM25 | 🟡 Media |
| 13 | Frontend `api.ts:4,23–25` | OIDC Frontend | `MOCK_TENANT_ID` y `MOCK_ROLE` hardcodeados | 🟡 Media |

---

### 2.3 InMemory Repos en `src/main/` (Code Smell — No bloqueante)

Seis clases `InMemory*Repository` en el paquete `com.btl.administrador.api.persistence.inmemory`:

| Archivo | CDI Scope | Riesgo runtime |
|---|---|---|
| `InMemoryMemoryRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — Postgres version tiene `@ApplicationScoped` |
| `InMemoryCaseRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — ídem |
| `InMemoryAuditRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — ídem |
| `InMemoryMemoryVersionRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — ídem |
| `InMemoryMemoryRelationRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — ídem |
| `InMemoryProcessingJobRepository.java` | ❌ Sin anotación CDI | ⚠️ Bajo — ídem |
| `InMemorySearchIndexer.java` | ❌ Sin anotación CDI | ⚠️ Bajo — `QdrantEmbeddingService` (CDI bean) es la implementación activa |

**Análisis**: Estas clases no son beans CDI (sin `@ApplicationScoped`). Las versiones `Postgres*Repository` equivalentes **sí** son beans CDI y son las que el contenedor resuelve. Por tanto, no hay riesgo de que se usen en runtime. Sin embargo:

- Están en `src/main/java/` en lugar de `src/test/java/`, violando la separación de código de producción vs. prueba.
- Podrían confundir a un desarrollador nuevo o ser importadas accidentalmente.
- **Recomendación**: Mover a `src/test/java/` o al menos añadir comentario `// FOR TESTS ONLY — NOT A CDI BEAN`.

---

### 2.4 Dependencia `qdrant` en scope `test`

**Hallazgo**: En `pom.xml`, la dependencia del cliente Qdrant está declarada con `<scope>test</scope>`:

```xml
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>qdrant</artifactId>
    <scope>test</scope>   <!-- ← No disponible en classpath de producción -->
</dependency>
```

**Impacto real**: **Nulo** para el módulo `btl` — `QdrantEmbeddingService` no usa la librería `qdrant`, sino HTTP directo con `java.net.http.HttpClient`. Para el módulo `v2`, el `InMemoryQdrantClient` tampoco la necesita.

**Recomendación**: Cambiar a `compile` (o eliminar si no se usa) cuando se implemente el cliente Qdrant real para v2.

---

## 3. Análisis de Regex / Extracción sin IA

### `StructuredExtractionService` (btl)
- ✅ Usa `OpenAiChatModel` (langchain4j) real con modelo `gpt-4o-mini`
- ✅ Prompt de extracción estructurada con JSON output forzado
- ✅ `extractJson()` solo limpia markdown code blocks del output del LLM
- **Veredicto**: Integración REAL — no es regex simulado.

### `ValidationService` (btl)
- ✅ Usa `OpenAiChatModel` real con modelo `gpt-4o` (configurable)
- ✅ Validación vía LLM, no regex
- **Veredicto**: Integración REAL.

### `MemoryServiceImpl.extractDefaultKind/extractDefaultSensitivity/extractDefaultConfidence` (v2)
- ✅ Extraen valores de un `Map<String, Object> config` — no son NLP, son config defaults legítimos.
- **Veredicto**: No sospechoso.

### `CaseService.extractDetailValue` (btl)
- ✅ Método utilitario para parsear `key:value` de un string de detalle. No simula IA.
- **Veredicto**: No sospechoso.

---

## 4. Verificación de Instanciación de Clientes Externos

### 4.1 OpenAI — Módulo btl (REAL)
| Componente | Instanciación | Via |
|---|---|---|
| `EmbeddingModel` | `OpenAiEmbeddingModel.builder().apiKey(apiKey).modelName("text-embedding-3-large")...` | `OpenAiConfigProducer.embeddingModel()` @Produces |
| `ChatLanguageModel` (chat) | `OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-4o-mini")...` | `OpenAiConfigProducer.chatLanguageModel()` @Produces |
| `ChatLanguageModel` (validación) | `OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-4o")...` | `ValidationService` constructor @Inject |
| `ChatLanguageModel` (extracción) | `OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-4o-mini")...` | `StructuredExtractionService` constructor @Inject |

⚠️ **Observación**: `ValidationService` y `StructuredExtractionService` instancian `OpenAiChatModel` **directamente** en sus constructores en lugar de inyectar el bean CDI producido por `OpenAiConfigProducer`. Esto crea instancias duplicadas. No es un mock, pero es una duplicación de configuración.

### 4.2 Qdrant — Módulo btl (REAL)
| Componente | Instanciación | Via |
|---|---|---|
| `HttpClient` | `HttpClient.newBuilder().connectTimeout(10s)...` | `QdrantConfig.httpClient()` @Produces |
| `QdrantEmbeddingService` | CDI — `@ApplicationScoped` | Inyecta `QdrantConfig`, `HttpClient`, `EmbeddingModel` |

✅ Usa HTTP directo contra Qdrant REST API. No requiere librería cliente.

### 4.3 Qdrant — Módulo v2 (MOCK)
| Componente | Instanciación | Via |
|---|---|---|
| `InMemoryQdrantClient` | CDI — `@ApplicationScoped` | Bean directo, simulando `ConcurrentHashMap` |

### 4.4 OpenAI Embeddings — Módulo v2 (MOCK)
| Componente | Instanciación | Via |
|---|---|---|
| `InMemoryEmbeddingProvider` | `new InMemoryEmbeddingProvider()` | CDI — `@ApplicationScoped`, genera vectores aleatorios normalizados |

---

## 5. Tests de Integración

**Resultado**: ❌ **Cero tests de integración** (`*IT.java`) encontrados para ninguna integración externa.

| Tipo de test | Cantidad | Qué prueban |
|---|---|---|
| Unit tests (`*Test.java`) | 19 archivos, ~89 tests | Lógica de negocio con dependencias mockeadas |
| Integration tests (`*IT.java`) | **0** | — |

Los tests existentes usan perfil `%test` con H2 en memoria y `quarkus.oidc.enabled=false`, `quarkus.langchain4j.openai.api-key=test-key-not-used-in-tests`. No validan que las integraciones externas funcionen.

---

## 6. Veredicto Final

### ❌ RECHAZADO

**Motivo**: El mock silencioso `InMemoryGitProvider` (Sección 2.1) es bloqueante. Esta clase:

1. Tiene `@ApplicationScoped` → CDI la activa en producción.
2. Es la **única** implementación de `GitProvider` → no hay alternativa real.
3. Carece de la marca `// MOCK: ... // REPLACE_BEFORE_PROD` → nadie sabe que es un mock.
4. Es inyectada en `MemoryService` → cada memoria aprobada genera un commit fantasma.
5. Representa el **mismo patrón que originó el incidente Abax-Memory**: código que simula integración real y pasa desapercibido hasta producción.

### Acciones requeridas antes de re-evaluación

| # | Acción | Responsable | Bloqueante |
|---|---|---|---|
| 1 | Marcar `InMemoryGitProvider` con `// MOCK: ... // REPLACE_BEFORE_PROD` **o** implementar `GitProvider` real (GitHub/GitLab API) | developer-backend | ✅ Sí |
| 2 | Mover 6 `InMemory*Repository` + `InMemorySearchIndexer` a `src/test/java/` o marcarlos `// FOR TESTS ONLY` | developer-backend | ❌ No |
| 3 | Documentar plan de reemplazo para los 13 mocks convencionales (Sección 2.2) con fechas y responsables | project-manager | ❌ No |
| 4 | Implementar al menos 1 test de integración por cada integración externa declarada | developer-backend + qa-functional | ❌ No (puede postergarse a Fase 5 QA) |
| 5 | Unificar instanciación de `OpenAiChatModel`: usar el bean CDI de `OpenAiConfigProducer` en lugar de construir nuevas instancias en `ValidationService` y `StructuredExtractionService` | developer-backend | ❌ No |

### Próximo paso

Una vez corregido el punto #1, el developer debe notificar al tech-lead para **re-evaluación**. No se aprueba el pase a Fase 5 (QA) hasta que el mock silencioso esté resuelto.

---

## 7. Anexo: Registro de Escaneos Ejecutados

```bash
# Paso 1: InMemory/Mock/Fake/Stub en src/main
grep -rn "InMemory\|Mock\|Fake\|Stub\|Dummy" backend-quarkus/src/main/ | grep -v "/test/"
# → 12 archivos detectados (10 InMemory + 2 clases con comentarios MOCK)

# Paso 2: InMemory/Mock en frontend
grep -rn "InMemory\|Mock\|Fake\|Stub\|Dummy" frontend-v2/src/ | grep -v ".test." | grep -v "__tests__"
# → 1 archivo (api.ts, mock convencional con REPLACE_BEFORE_PROD)

# Paso 3: Regex sospechosos de extracción
grep -rn "extract\|parse.*entity\|regex.*kind\|split.*title" backend-quarkus/src/main/ | grep -v "/test/"
# → 35 líneas, todas legítimas (config parsing, JSON cleanup post-LLM)

# Paso 4: Imports de librerías de integración externa
grep -rn "import.*qdrant\|import.*openai\|import.*keycloak" backend-quarkus/src/main/
# → 7 imports de langchain4j (OpenAI real), 2 de Qdrant (interfaces custom), 
#   Keycloak sin imports directos (usado vía Quarkus OIDC extension)

# Paso 5: Búsqueda de tests de integración
find backend-quarkus/src/test -name "*IT*" -o -name "*IntegrationTest*"
# → 0 resultados
```

---

## Glosario

- **CDI**: Contexts and Dependency Injection — contenedor de inyección de dependencias estándar de Jakarta EE, usado por Quarkus para resolver beans en runtime.
- **Mock silencioso**: Clase que simula una integración externa sin la marca de convención `// MOCK: ... // REPLACE_BEFORE_PROD`, haciéndola indistinguible de una implementación real.
- **IT (Integration Test)**: Test que valida la interacción real entre componentes, frecuentemente con servicios externos reales o Testcontainers, en contraste con tests unitarios que usan mocks.
- **OIDC**: OpenID Connect — capa de identidad sobre OAuth 2.0 usada por Keycloak para autenticación y autorización.
- **CDI Producer** (`@Produces`): Método anotado que fabrica beans para el contenedor CDI, típicamente usado para crear clientes de servicios externos con configuración.

---

## Re-evaluación 2026-05-03

### Resultado: ⚠️ APROBADO CON OBSERVACIONES

El **mock silencioso crítico que causó el rechazo anterior ha sido corregido**. Todas las clases InMemory/Mock activas en CDI tienen ahora la marca `// MOCK: ... // REPLACE_BEFORE_PROD`. Además, dos integraciones reales nuevas (OpenAIEmbeddingProvider y OpenAiLlmService) elevan la calidad del entregable.

---

### 1. Correcciones Verificadas

#### 1.1 Bloqueante Anterior Resuelto: `InMemoryGitProvider` ✅

| Campo | Antes (v1 — RECHAZADO) | Ahora (v2 — CORREGIDO) |
|---|---|---|
| **Archivo** | `InMemoryGitProvider.java:10-11` | `InMemoryGitProvider.java:11-19` |
| **Marca MOCK** | ❌ Ausente | `// MOCK: Git provider not integrated` (línea 11) |
| **Marca REPLACE** | ❌ Ausente | `// REPLACE_BEFORE_PROD: Implement real Git persistence (GitHub/GitLab API)` (línea 12) |
| **Log runtime** | ❌ Sin advertencia | `LOG.warn("MOCK ACTIVE: InMemoryGitProvider generates fake commits. REPLACE_BEFORE_PROD with real Git integration.")` (línea 19) |
| **Estado** | 🔴 RECHAZADO | 🟢 CORREGIDO |

**Evidencia**: `backend-quarkus/src/main/java/com/btl/administrador/api/integration/git/InMemoryGitProvider.java:11-19`

---

#### 1.2 Verificación Global: TODOS los mocks marcados ✅

Se ejecutó escaneo de `REPLACE_BEFORE_PROD` en todo `src/main/`:

```
grep -rn "REPLACE_BEFORE_PROD\|MOCK:" backend-quarkus/src/main/
→ 40 matches en 11 archivos
```

**Todos los hallazgos MOCK están ahora correctamente marcados con la convención**. Cero mocks silenciosos detectados.

| Archivo | Marcas MOCK/REPLACE | Estado |
|---|---|---|
| `InMemoryGitProvider.java` | `// MOCK: Git provider not integrated` + `// REPLACE_BEFORE_PROD` + `LOG.warn(...)` | ✅ Corregido |
| `InMemoryEmbeddingProvider.java` | `// MOCK: OpenAI API key...` + `// REPLACE_BEFORE_PROD` | ✅ Ya estaba |
| `InMemoryQdrantClient.java` | `// MOCK: Qdrant no disponible...` + `// REPLACE_BEFORE_PROD` | ✅ Ya estaba |
| `TenantFilter.java` | `// MOCK: Reads X-Tenant-Id...` + `// REPLACE_BEFORE_PROD` | ✅ Ya estaba |
| `TenantContext.java` | `// MOCK: Accepts X-Tenant-Id...` + `// REPLACE_BEFORE_PROD` (×2) | ✅ Ya estaba |
| `MemoryResourceV2.java` | `// MOCK: Direct header-to-tenant...` + `// REPLACE_BEFORE_PROD` | ✅ Ya estaba |
| `SearchResourceV2.java` | `// MOCK: Direct header-to-tenant...` + `// MOCK: RBAC simulation...` + `// REPLACE_BEFORE_PROD` (×2) | ✅ Ya estaba |
| `MemoryServiceImpl.java` | `// MOCK: Synchronous indexing...` + `// MOCK: Uses tenant ID as actor...` + `// REPLACE_BEFORE_PROD` (×2) | ✅ Ya estaba |
| `SearchServiceImpl.java` | `// MOCK: Collection name hardcoded...` + `// MOCK: Qdrant is in-memory...` + `// MOCK: Simple keyword frequency...` + `// REPLACE_BEFORE_PROD` (×3) | ✅ Ya estaba |
| `InfrastructureConfig.java` | `REPLACE_BEFORE_PROD` warning en log al usar fallback in-memory | ✅ Ya estaba |
| Frontend `api.ts` | `// MOCK: OIDC mock... // REPLACE_BEFORE_PROD` + `// MOCK: Hardcoded tenant/role... // REPLACE_BEFORE_PROD` | ✅ Ya estaba |

---

### 2. Nuevas Implementaciones Reales Detectadas

#### 2.1 `OpenAIEmbeddingProvider` — REAL ✅

| Campo | Detalle |
|---|---|
| **Archivo** | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/ai/OpenAIEmbeddingProvider.java` |
| **Dimensión** | 3072 (matching `text-embedding-3-large`) |
| **Dependencia** | `dev.langchain4j.model.embedding.EmbeddingModel` (langchain4j) |
| **Método real** | `embeddingModel.embed(text)` → `response.content().vector()` |
| **Método batch** | `embeddingModel.embedAll(segments)` con `TextSegment` |
| **Estado** | 🟢 REAL — No es mock. Usa la API de OpenAI vía langchain4j. |
| **Fallback** | `InfrastructureConfig.java:102-121` — Si no hay API key o `EmbeddingModel` no disponible, cae a `InMemoryEmbeddingProvider` con warning `REPLACE_BEFORE_PROD`. |

**Evidencia**: `OpenAIEmbeddingProvider.java:52-56`, `InfrastructureConfig.java:82-121`

#### 2.2 `OpenAiLlmService` — REAL ✅

| Campo | Detalle |
|---|---|
| **Archivo** | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/ai/OpenAiLlmService.java` |
| **Interfaz** | `com.abax.memory.domain.service.LlmService` |
| **Dependencia** | `dev.langchain4j.model.chat.ChatLanguageModel` (CDI bean) |
| **Capacidades** | `extractEntities()`, `inferRelations()`, `generateSummary()`, `validateMemory()`, `estimateConfidence()` |
| **Modelo** | `gpt-4o-mini` (default, via CDI ChatLanguageModel bean) |
| **Estado** | 🟢 REAL — 333 líneas de código. Prompts estructurados, parseo JSON real, manejo de errores. |
| **Features cubiertas** | FT-001.04 (Entity Extraction), FT-005.05 (Relation Inference), validación, summarization, confidence |

**Evidencia**: `OpenAiLlmService.java:1-333`, `LlmService.java:1-63`

---

### 3. Matriz de Integraciones Actualizada

| Integración | Módulo | Declarada | Dep en pom.xml | Import real | Cliente instanciado | Estado Mayo 03 | Cambio vs v1 |
|---|---|---|---|---|---|---|---|
| **PostgreSQL** | btl + v2 | ✅ | `quarkus-jdbc-postgresql` (compile) | Panache, Hibernate, Flyway | `Postgres*Repository` (6 beans CDI) | ✅ REAL | Sin cambio |
| **OpenAI Chat** | btl | ✅ | `langchain4j-open-ai` (compile) | `OpenAiChatModel` | `OpenAiConfigProducer` + directo en servicios | ✅ REAL | Sin cambio |
| **OpenAI Embeddings** | btl | ✅ | `langchain4j-open-ai` (compile) | `OpenAiEmbeddingModel` | `OpenAiConfigProducer` → `QdrantEmbeddingService` | ✅ REAL | Sin cambio |
| **OpenAI Embeddings** | v2 | ✅ | `langchain4j-open-ai` (compile) | `OpenAIEmbeddingProvider` (REAL) | `InfrastructureConfig.embeddingProvider()` → `OpenAIEmbeddingProvider` o fallback `InMemoryEmbeddingProvider` | 🟡 PARCIAL (real si hay API key) | ⬆️ Mejoró: antes 100% MOCK |
| **Qdrant** | v2 | ✅ | `testcontainers.qdrant` (test scope) | `QdrantClient` (custom iface) | `InMemoryQdrantClient` (@ApplicationScoped, único bean CDI) | ⚠️ MOCK (convencional) | Sin cambio |
| **Keycloak OIDC** | v2 | ✅ | `quarkus-oidc` (compile) | `TenantContext`, `TenantFilter` | Header simulado (`X-Tenant-Id`, `X-Role`) | ⚠️ MOCK (convencional) | Sin cambio |
| **Git** | btl | ✅ | Ninguna | `GitProvider` (custom iface) | `InMemoryGitProvider` (@ApplicationScoped) | ⚠️ MOCK (convencional) | ⬆️ Corregido: ahora marcado |
| **LLM Service** | v2 | ✅ | `langchain4j-open-ai` (compile) | `OpenAiLlmService` (REAL) | CDI `@ApplicationScoped`, inyecta `ChatLanguageModel` | ✅ REAL | 🆕 NUEVO |
| **Keycloak OIDC** | Frontend | ✅ | N/A | `api.ts` | Headers hardcodeados (`tenant-001`, `memory-admin`) | ⚠️ MOCK (convencional) | Sin cambio |

---

### 4. Observaciones No Bloqueantes

Estos items no impiden el pase a Fase 5 pero deben registrarse como deuda técnica:

| # | Observación | Severidad | Responsable | Plazo sugerido |
|---|---|---|---|---|
| **OB-01** | `QdrantRestClient` real no implementado. `InMemoryQdrantClient` es el único bean CDI para `QdrantClient`. Docker Compose tiene Qdrant v1.17.1 UP en puerto 6333 — solo falta escribir el adapter REST. | 🟡 Media | @developer-backend | Antes de Fase 6 (UAT) |
| **OB-02** | 6 clases `InMemory*Repository` (btl) en `src/main/java/` sin marca `FOR TESTS ONLY` ni `REPLACE_BEFORE_PROD`. No son beans CDI (sin `@ApplicationScoped`), riesgo bajo. | 🟢 Baja | @developer-backend | Fase 5 (QA) |
| **OB-03** | `InMemorySearchIndexer.java` (btl) con comentario "NOT a CDI bean" pero sin `REPLACE_BEFORE_PROD`. Riesgo bajo. | 🟢 Baja | @developer-backend | Fase 5 (QA) |
| **OB-04** | 0 tests de integración (`*IT.java`). Tests existentes (~276) son unitarios con H2 e InMemory stubs. No validan integraciones externas reales. | 🟡 Media | @qa-functional + @developer-backend | Fase 5 (QA) |
| **OB-05** | Dependencia `testcontainers.qdrant` está en scope `test` en `pom.xml`. Si se implementa `QdrantRestClient`, se necesitará dependencia de cliente Qdrant en scope `compile`. | 🟢 Baja | @developer-backend | Antes de OB-01 |

---

### 5. Escaneos Re-ejecutados (Evidencia)

```bash
# 1. Clases InMemory/Mock/Fake/Stub/Dummy en src/main
grep -rn "InMemory\|Mock\|Fake\|Stub\|Dummy" backend-quarkus/src/main/ | grep -v "/test/"
# → 21 archivos. Todos con MOCK o REPLACE_BEFORE_PROD o explicación de no-CDI.

# 2. Verificación de marcas REPLACE_BEFORE_PROD
grep -rn "REPLACE_BEFORE_PROD" backend-quarkus/src/main/
# → 40 matches en 11 archivos. Cero mocks sin marca.

# 3. Verificación de imports OpenAI reales
grep -rn "import dev.langchain4j" backend-quarkus/src/main/java/com/abax/
# → OpenAIEmbeddingProvider.java:3-5 (Embedding, TextSegment, EmbeddingModel)
# → OpenAiLlmService.java:9 (ChatLanguageModel)
# → Ambos con instanciación real, no mock.

# 4. Búsqueda de regex/extract sospechosos
grep -rn "Pattern.compile\|\.matches(" backend-quarkus/src/main/java/com/abax/ | grep -v "/test/"
# → 0 resultados. Toda extracción usa LLM real.

# 5. Tests de integración
find backend-quarkus/src/test -name "*IT*" -o -name "*IntegrationTest*"
# → 0 resultados (sin cambio vs v1).
```

---

### 6. Veredicto Final

#### ⚠️ APROBADO CON OBSERVACIONES

**Fundamento**: El mock silencioso crítico `InMemoryGitProvider` que causó el rechazo en v1 ahora está correctamente marcado con `// MOCK: Git provider not integrated // REPLACE_BEFORE_PROD`. Se verificó que **los 40 marcadores MOCK/REPLACE_BEFORE_PROD en 11 archivos** cubren todas las implementaciones simuladas activas. Cero mocks silenciosos.

Adicionalmente, dos nuevas integraciones reales elevan la calidad del entregable:
- `OpenAIEmbeddingProvider` — embeddings 3072-dim reales vía langchain4j (cuando API key disponible)
- `OpenAiLlmService` — 5 capacidades LLM reales (extractEntities, inferRelations, generateSummary, validateMemory, estimateConfidence)

**Lo que impide APROBADO limpio**:
- `InMemoryQdrantClient` sigue siendo el único bean CDI para búsqueda vectorial (aunque correctamente marcado).
- Auth OIDC sigue siendo simulada con headers (aunque correctamente marcada).
- 0 tests de integración contra servicios externos reales.

**Recomendación**: Avanzar a Fase 5 (QA) con estas observaciones registradas como deuda técnica. Las features críticas de búsqueda semántica (EP-005) requieren Qdrant real para ser validadas en QA; si no está listo, los tests de EP-005 deben acotarse a lo validable con el mock actual.

### Próximo paso

El **@qa-functional** puede iniciar Fase 5 sobre las features REALES (EP-001, EP-002, EP-006, EP-004 parcial, EP-009 parcial). Las features en MOCK (EP-005 búsqueda semántica, EP-004.10 auth) deben probarse contra los stubs documentados, con la expectativa de re-validar cuando las integraciones reales estén completas.
