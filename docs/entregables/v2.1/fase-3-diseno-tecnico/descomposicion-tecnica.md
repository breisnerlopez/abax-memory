# Descomposición Técnica de Tareas — Abax-Memory v2.1.0

- **Fase**: 3 — Diseño Técnico
- **Entregable**: Descomposición Técnica de Tareas
- **Versión**: v2.1.0
- **Responsable**: tech-lead
- **Fecha**: 2026-05-06
- **Estado**: Completado

**Fuentes**:
- `docs/entregables/v2.1/fase-3-diseno-tecnico/documento-de-arquitectura.md`
- `docs/entregables/v2.1/fase-2-analisis/especificacion-funcional.md`
- `docs/entregables/v2.1/fase-2-analisis/criterios-de-aceptacion.md`
- Codebase: `backend-quarkus/src/main/java/com/abax/memory/`
- Build: `backend-quarkus/pom.xml`

---

## Tabla de Contenidos

- [1. Resumen de Esfuerzo](#1-resumen-de-esfuerzo)
- [2. Orden de Ejecución Recomendado](#2-orden-de-ejecución-recomendado)
- [3. MVP (R1) vs R2](#3-mvp-r1-vs-r2)
- [4. Asignación de Recursos](#4-asignación-de-recursos)
- [5. Ruta Crítica](#5-ruta-crítica)
- [6. Descomposición por Feature](#6-descomposición-por-feature)
  - [6.1 EP-V21-001 — Precisión del Motor de Búsqueda](#61-ep-v21-001--precisión-del-motor-de-búsqueda)
  - [6.2 EP-V21-002 — Velocidad y Latencia](#62-ep-v21-002--velocidad-y-latencia)
  - [6.3 EP-V21-003 — Eficiencia Operativa](#63-ep-v21-003--eficiencia-operativa)
  - [6.4 EP-V21-004 — API y Developer Experience](#64-ep-v21-004--api-y-developer-experience)
- [7. Criterios de Completitud por Feature](#7-criterios-de-completitud-por-feature)
- [8. Glosario](#8-glosario)

---

## 1. Resumen de Esfuerzo

### 1.1 Totales por Épica

| Épica | Features | Tareas | Horas totales | Días (8h) |
|---|---|---|---|---|
| **EP-V21-001** — Precisión | 4 (FT-001.1 a FT-001.4) | 29 | 92h | 11.5d |
| **EP-V21-002** — Velocidad | 3 (FT-002.1 a FT-002.3) | 25 | 70h | 8.75d |
| **EP-V21-003** — Eficiencia | 3 (FT-003.1 a FT-003.3) | 17 | 42h | 5.25d |
| **EP-V21-004** — API y DX | 3 (FT-004.1 a FT-004.3) | 27 | 64h | 8d |
| **Total v2.1.0** | **13 features** | **98 tareas** | **268h** | **~33.5d** |

### 1.2 Totales por Tipo de Tarea

| Tipo | Cantidad | Horas | % Esfuerzo |
|---|---|---|---|
| **Backend** | 42 | 130h | 48.5% |
| **Config** | 14 | 18h | 6.7% |
| **Test** | 22 | 56h | 20.9% |
| **QA / Benchmark** | 8 | 28h | 10.4% |
| **DevOps** | 7 | 24h | 9.0% |
| **Documentación** | 5 | 12h | 4.5% |

### 1.3 Paralelismo Máximo

- **Fase 1 (EP-V21-001 + EP-V21-003 parcial)**: 6 tareas simultáneas
- **Fase 2 (EP-V21-002)**: 5 tareas simultáneas
- **Fase 3 (EP-V21-004)**: 6 tareas simultáneas

---

## 2. Orden de Ejecución Recomendado

El orden está optimizado para maximizar el paralelismo entre los dos developers disponibles (developer-backend y devops) y QA, respetando las dependencias entre features.

```
FASE 1 — Semana 1-2: Núcleo del motor (Precisión)
├── FT-001.1: Cross-Encoder (Alta, 3d) ← BLOQUEANTE para benchmarks
├── FT-001.2: Aislamiento Semántico (Baja, 0.5d) ← Paralelo a FT-001.1
├── FT-001.3: Expansión Multi-Origen (Media, 2d) ← Depende de FT-001.2 + FT-003.3 diseño
├── FT-003.3: graphEntryStrategy Config (Baja, 0.5d) ← Prerrequisito para FT-001.3
└── FT-001.4: Extract OpenAI Real (Media, 1.5d) ← Independiente del pipeline search

FASE 2 — Semana 2-3: Velocidad + Eficiencia
├── FT-002.1: Cache de Grafo (Media, 1.5d) ← Depende de FT-001.3 completado
├── FT-002.2: Latencia Qdrant (Baja-Media, 3d) ← Paralelo, diagnóstico desde día 1
├── FT-002.3: Cache JWT (Media, 2d) ← Independiente
├── FT-003.1: Worker (Baja, 1.5d) ← Independiente
└── FT-003.2: Unificar Qdrant (Media, 1.5d) ← DevOps lidera, dev backend soporta

FASE 3 — Semana 4: API + Developer Experience
├── FT-004.1: X-Graph-Strategy (Baja, 0.5d) ← Depende de FT-003.3
├── FT-004.2: Unificar Endpoints (Media, 2d) ← Depende de FT-001.1 + FT-001.3
└── FT-004.3: DELETE Namespace (Alta, 2.5d) ← Independiente
```

---

## 3. MVP (R1) vs R2

### R1 — Release Mínimo Viable (Must Have)

Features que **deben** estar completas para que v2.1.0 entregue valor. Coinciden con prioridades MoSCoW Must y Should de la especificación funcional.

| Feature | Prioridad | Horas | Justificación |
|---|---|---|---|
| **FT-001.1** Cross-Encoder | 1 — Máxima | 28h | Habilita CE-01 (top-1 ≥ 0.90), CE-03 (NDCG@10 ≥ 0.85), CE-04 (Recall@10 ≥ 0.90) |
| **FT-001.2** Aislamiento Semántico | 2 — Alta | 4h | Habilita CE-07 (búsqueda semántica pura) |
| **FT-001.3** Expansión Multi-Origen | 3 — Alta | 18h | Habilita CE-01 (recall multi-dominio ≥ 85%) |
| **FT-001.4** Extract OpenAI Real | 6 — Media-Alta | 16h | Habilita CE-06 (extracción con IA real) |
| **FT-002.3** Cache JWT | 4 — Alta | 18h | Contribuye a CE-02 (p95 ≤ 500ms), latencia auth |
| **FT-003.2** Unificar Qdrant | 5 — Alta | 16h | Habilita CE-05 (1 colección) |
| **Subtotal R1** | 6 features | **100h (12.5d)** | |

### R2 — Release Completa (Should/Could)

Features que completan la calidad pero no bloquean el despliegue.

| Feature | Prioridad | Horas | Justificación |
|---|---|---|---|
| **FT-002.1** Cache de Grafo | 7 — Media | 16h | Optimización de latencia (no bloquea CE-02) |
| **FT-002.2** Latencia Qdrant | 8 — Media | 24h | Diagnóstico + mitigación (no bloquea CE-02 por sí solo) |
| **FT-003.1** Worker | 9 — Media | 16h | Limpieza operativa (no afecta funcionalidad core) |
| **FT-003.3** graphEntryStrategy | 10 — Media | 6h | Configuración granular (mejora, no requisito) |
| **FT-004.1** X-Graph-Strategy | 11 — Media | 8h | Control granular por request |
| **FT-004.2** Unificar Endpoints | 12 — Media | 18h | Mejora DX, backward compatible |
| **FT-004.3** DELETE Namespace | 13 — Media | 22h | Endpoint administrativo (no bloquea búsquedas) |
| **Subtotal R2** | 7 features | **110h (13.75d)** | |

---

## 4. Asignación de Recursos

### 4.1 Responsabilidades por Rol

| Rol | Features asignadas | Horas totales | % del proyecto |
|---|---|---|---|
| **developer-backend** | FT-001.1, FT-001.2, FT-001.3, FT-002.1, FT-002.3, FT-003.3, FT-004.1, FT-004.2, FT-004.3 | 196h | 73% |
| **devops** | FT-002.2 (diagnóstico Qdrant), FT-003.1 (worker), FT-003.2 (unificación Qdrant), FT-004.3 (parcial: transacción PG + Qdrant) | 36h | 13% |
| **QA** | Benchmarks (FT-001.1), tests de integración (FT-001.4, FT-002.3), pruebas de carga (FT-002.2), verificación regresión | 28h | 10% |
| **tech-lead** | Code review (todas), diseño técnico, aprobación de PRs | 8h | 3% |

### 4.2 Notas de Asignación

- **developer-backend** es el rol principal (73% del esfuerzo). Debe estar dedicado full-time.
- **devops** lidera las 3 features de infraestructura (Qdrant, worker, unificación) con soporte de developer-backend para cambios de código.
- **QA** ejecuta benchmarks de precisión y latencia en paralelo al desarrollo.
- **tech-lead** revisa código incrementalmente (no al final, para evitar cuellos de botella).

---

## 5. Ruta Crítica

La ruta crítica determina la **duración mínima** del proyecto. Son las tareas sin holgura (cualquier retraso en ellas retrasa todo el proyecto).

```
T-001.1.1 (pom.xml Caffeine) ──> T-001.1.2 (CrossEncoderService) ──> T-001.1.3 (SearchServiceImpl integración)
  ──> T-001.1.6 (benchmark precisión) ──> T-001.1.9 (A/B test reranker)
                                      ──> T-004.2.3 (UnifiedSearchRequest pesos) ──> T-004.2.7 (integración endpoint)
  ──> T-001.2.1 (expandGraph default) ──> T-001.3.3 (expandGraphConsolidated top-3) ──> T-002.1.3 (graph cache integración)
```

**Duración de la ruta crítica**: ~15 días hábiles (de 33.5 totales), asumiendo 1 developer-backend + 1 devops.

**Implicaciones**:
- FT-001.1 (Cross-Encoder) está en la ruta crítica. Cualquier retraso aquí retrasa todo.
- FT-001.2 (Aislamiento Semántico) es rápido (0.5d) pero está en la ruta crítica por ser dependencia de FT-001.3.
- FT-004.2 (Unificar Endpoints) y FT-004.3 (DELETE Namespace) NO están en la ruta crítica. Pueden ejecutarse en paralelo y tienen holgura.

---

## 6. Descomposición por Feature

### 6.1 EP-V21-001 — Precisión del Motor de Búsqueda

---

#### FT-V21-001.1: Reranker Cross-Encoder

**Complejidad**: Alta | **Esfuerzo estimado**: 3 días (24h + 4h QA) | **Desarrollador**: developer-backend

**Descripción**: Implementar pipeline two-stage: Stage 1 (dense retrieval top-20) → Stage 2 (cross-encoder reranker con OpenAI `gpt-4o-mini` → top-5). Incluye graceful degradation si el reranker no está disponible.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-001.1.1** | Agregar dependencia Caffeine en `pom.xml` (gestión de dependencias + BOM si aplica) y configurar `quarkus.cache.caffeine` en `application.properties` | Config | `backend-quarkus/pom.xml`, `backend-quarkus/src/main/resources/application.properties` | 0.5h | — |
| **T-001.1.2** | Crear interfaz `CrossEncoderService` en `domain/service/` con método `List<RerankedHit> rerank(String query, List<ScoredHit> candidates, int topK)` y DTO `RerankedHit` con campos `memoryId`, `semanticScore`, `crossEncoderScore`, `finalScore` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/service/CrossEncoderService.java`, `backend-quarkus/src/main/java/com/abax/memory/domain/model/RerankedHit.java` | 2h | T-001.1.1 |
| **T-001.1.3** | Implementar `CrossEncoderServiceImpl` en `infrastructure/ai/` usando `ChatLanguageModel` CDI bean existente. Construir prompt de entailment por lotes: evaluar 20 pares (query, documento) en una sola llamada, parsear respuesta JSON con scores. Timeout 2s | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/ai/CrossEncoderServiceImpl.java` | 6h | T-001.1.2 |
| **T-001.1.4** | Implementar graceful degradation en `CrossEncoderServiceImpl`: si `ChatLanguageModel` no es resoluble → log `WARN CROSS_ENCODER_UNAVAILABLE` + retornar `Optional.empty()`. Si timeout > 2s → cancelar, log `WARN CROSS_ENCODER_TIMEOUT`. Si respuesta malformada → log `ERROR`, omitir candidato problemático | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/ai/CrossEncoderServiceImpl.java` | 2h | T-001.1.3 |
| **T-001.1.5** | Producir bean `CrossEncoderService` en `InfrastructureConfig`. Si `ChatLanguageModel` es resoluble → `CrossEncoderServiceImpl`. Si no → stub que retorna `Optional.empty()` con log `WARN` | Config | `backend-quarkus/src/main/java/com/abax/memory/config/InfrastructureConfig.java` | 1.5h | T-001.1.4 |
| **T-001.1.6** | Modificar `SearchServiceImpl.semanticSearch()`: añadir Stage 2 después del dense retrieval. Invocar `crossEncoderService.rerank(query, top20Candidates, topK)`. Si `Optional.empty()` → retornar dense-only. Poblar `scoreComponents` en `ScoredMemory` con `semantic` y `crossEncoder`. Poblar `pipeline` metadata en `UnifiedSearchResponse` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 5h | T-001.1.5 |
| **T-001.1.7** | Añadir campo `rerank` (boolean, default `true`) a `UnifiedSearchRequest`. Cuando `false`, `SearchServiceImpl` omite Stage 2. Añadir campos `crossEncoder` a `ScoredMemory.scoreComponents` y `PipelineMetadata` a `UnifiedSearchResponse` | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java`, `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchResponse.java`, `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/ScoredMemory.java` | 2h | T-001.1.2 |
| **T-001.1.8** | Escribir tests unitarios para `CrossEncoderServiceImpl`: (a) con `MockLlmService` simulando respuesta JSON válida, verificar reordenamiento correcto; (b) con mock que lanza timeout, verificar graceful degradation; (c) con mock que retorna JSON malformado, verificar que se omite el candidato problemático; (d) con mock inalcanzable, verificar `Optional.empty()` | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/ai/CrossEncoderServiceTest.java` | 3h | T-001.1.4 |
| **T-001.1.9** | Ejecutar benchmark de precisión SciFact (5,183 docs, 300 queries): medir NDCG@10, Recall@10, top-1 accuracy con y sin cross-encoder. Documentar delta en ADR-001 | QA | `scripts/benchmark-scifact.sh` (crear si no existe, basado en lógica de v2.0.0), `docs/entregables/v2.1/fase-5-qa/benchmark-cross-encoder.md` | 4h | T-001.1.6 |
| **T-001.1.10** | Ejecutar A/B test offline: 100 queries multi-dominio con `rerank: true` vs `rerank: false`. Medir porcentaje de casos donde el ground-truth asciende en el ranking. Documentar en dashboard de benchmarks | QA | `scripts/benchmark-ab-test.sh` (crear), `docs/entregables/v2.1/fase-5-qa/ab-test-reranker.md` | 2h | T-001.1.9 |

---

#### FT-V21-001.2: Aislamiento de Búsqueda Semántica Pura

**Complejidad**: Baja | **Esfuerzo estimado**: 0.5 día (4h) | **Desarrollador**: developer-backend

**Descripción**: Cambiar default de `expandGraph` de `true` a `false` en `UnifiedSearchRequest`. Modificar `SearchServiceImpl.unifiedSearch()` para que `expandGraph=false` aísle completamente la búsqueda semántica del grafo.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-001.2.1** | Cambiar default de `expandGraph` de `true` a `false` en `UnifiedSearchRequest`. Añadir Javadoc explicando el cambio de comportamiento respecto a v2.0.9 | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` | 0.5h | — |
| **T-001.2.2** | Modificar `SearchServiceImpl.unifiedSearch()`: cuando `expandGraph == false`, ejecutar pipeline semántico puro (dense + cross-encoder), NO invocar `expandGraphConsolidated()`, retornar `pipeline.graphExpanded: false` sin campo `graphExpandedNodes` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-001.2.1 |
| **T-001.2.3** | Extender `UnifiedSearchResponse` con campo `pipeline` (objeto con `stages`, `graphExpanded`, `graphExpandedNodes`, `crossEncoderApplied`). Añadir campo `graphExpanded` (boolean) a cada `ScoredMemory` en la respuesta | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchResponse.java`, `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/ScoredMemory.java` | 1h | T-001.2.1 |
| **T-001.2.4** | Verificar que los tests existentes de `SearchServiceImpl` pasan con el nuevo default. Si algún test asumía `expandGraph=true` implícito, actualizarlo para pasar `expandGraph: true` explícitamente | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/service/SearchServiceImplTest.java`, `backend-quarkus/src/test/java/com/abax/memory/api/rest/v2/SearchResourceV2Test.java` | 0.5h | T-001.2.2 |

---

#### FT-V21-001.3: Expansión de Grafo Multi-Origen

**Complejidad**: Media | **Esfuerzo estimado**: 2 días (16h + 2h QA) | **Desarrollador**: developer-backend

**Descripción**: Extender `expandGraphConsolidated()` para expansión desde top-3 (cambio desde top-1 en v2.0.9). Añadir soporte para `entryPoints` explícitos con máxima precedencia. Implementar estrategias `single-best`, `top-k`, `threshold`. Cambiar default `graphTopK` de 5 a 3.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-001.3.1** | Añadir campo `entryPoints` (`List<String>`, opcional, max 10) a `UnifiedSearchRequest`. Añadir validación: máximo 10 IDs, formato UUID válido. Deduplicar silenciosamente con log `DEBUG` | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` | 1h | T-001.2.1 |
| **T-001.3.2** | Cambiar default `graphTopK` de 5 a 3 en `UnifiedSearchRequest`. Actualizar Javadoc | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` | 0.5h | — |
| **T-001.3.3** | Modificar `SearchServiceImpl.expandGraphConsolidated()`: aceptar nuevo parámetro `entryPoints` explícitos. Si `entryPoints` no vacío → usar esos como seeds con máxima precedencia (ignorar dense retrieval). Si vacío → seleccionar automáticamente según estrategia (top-3 por defecto). Extraer lógica de selección de entry points a método privado `selectEntryPoints()` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 4h | T-001.3.1, T-001.3.2 |
| **T-001.3.4** | Implementar validación de `entryPoints`: verificar que cada ID exista en PostgreSQL (`MemoryFragmentEntity.findByIdOptional()`). IDs inexistentes → log `WARN ENTRY_POINT_NOT_FOUND`, excluir silenciosamente. Si ningún entry point válido → `graphExpandedNodes.totalExpandedNodes: 0` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-001.3.3 |
| **T-001.3.5** | Implementar estrategia `single-best`: expandir solo desde el mejor match del dense retrieval (K=1). Comportamiento backward compatible con v2.0.9 | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 1h | T-001.3.3 |
| **T-001.3.6** | Implementar estrategia `threshold`: expandir desde todos los matches con score ≥ `graphThreshold`. Límite interno de 10 entry points máximo. Si ningún match supera el threshold → `graphExpandedNodes.totalExpandedNodes: 0` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-001.3.3 |
| **T-001.3.7** | Extender `graphExpandedNodes` en `UnifiedSearchResponse` con campos: `entryPointIds`, `entryPointCount`, `entryPointSource` (valores: `dense-retrieval-top-3`, `dense-retrieval-single-best`, `dense-retrieval-threshold-N`, `client-provided`, `header-override`), `totalExpandedNodes`, `maxDepth`, `cacheHit` | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchResponse.java` | 1.5h | T-001.3.3 |
| **T-001.3.8** | Escribir tests unitarios para `expandGraphConsolidated`: (a) top-3 desde dense retrieval, (b) `entryPoints` explícitos con IDs válidos, (c) `entryPoints` con ID inexistente → warning + exclusión, (d) `entryPoints` > 10 → validación 400, (e) estrategia `single-best`, (f) estrategia `threshold` con varios scores, (g) `entryPoints` explícitos ignoran estrategia automática | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/service/SearchServiceImplTest.java` (extender) | 3h | T-001.3.7 |
| **T-001.3.9** | Ejecutar benchmark recall multi-dominio (ABM-MULTI-01, 50 queries) con expansión top-3. Comparar contra línea base v2.0.9 (69.4%). Documentar | QA | `scripts/benchmark-multi-domain.sh` (crear), `docs/entregables/v2.1/fase-5-qa/benchmark-graph-expansion.md` | 2h | T-001.3.3 |

---

#### FT-V21-001.4: Extracción de Entidades con OpenAI Real

**Complejidad**: Media | **Esfuerzo estimado**: 1.5 días (12h + 4h QA) | **Desarrollador**: developer-backend

**Descripción**: Modificar `POST /memories/extract` para usar exclusivamente OpenAI `gpt-4o-mini`, nunca degradar a `MockLlmService`. Errores explícitos: 503 (sin API key), 502 (error OpenAI), 504 (timeout).

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-001.4.1** | Modificar `MemoryServiceImpl.extractEntities()`: en lugar de usar `LlmService` inyectado (que puede ser `MockLlmService`), inyectar `OpenAiLlmService` directamente. Si `OpenAiLlmService` no es resoluble (sin `ChatLanguageModel` CDI) → lanzar `ServiceUnavailableException` con mensaje `"Entity extraction unavailable: LLM service not configured"` mapeado a HTTP 503 | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/MemoryServiceImpl.java` | 3h | — |
| **T-001.4.2** | Implementar manejo de errores OpenAI en `MemoryServiceImpl`: (a) `OpenAiHttpException` (API key inválida/sin crédito) → HTTP 502 `"Entity extraction failed: LLM provider error"`, (b) timeout > 5s → HTTP 504 `"Entity extraction timed out"`, (c) respuesta vacía (0 entidades) → HTTP 200 con `entities: []` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/MemoryServiceImpl.java` | 2h | T-001.4.1 |
| **T-001.4.3** | Extender `ExtractRequest` con campo `domain` (String, opcional, default `"general"`). Validar `content`: min 1 char no blanco, max 5000 chars | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/ExtractRequest.java` | 1h | — |
| **T-001.4.4** | Extender `ExtractResponse` con campos `source` (String, `"openai-gpt-4o-mini"`), `extractionTimeMs` (long). Añadir campo `confidence` (double, 0.0-1.0) a cada entidad extraída | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/ExtractResponse.java`, `backend-quarkus/src/main/java/com/abax/memory/domain/model/ExtractedEntity.java` | 1.5h | T-001.4.3 |
| **T-001.4.5** | Actualizar `MemoryResourceV2` endpoint `POST /memories/extract`: mapear nuevas excepciones a códigos HTTP (503, 502, 504). Añadir `@APIResponse` en OpenAPI | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/MemoryResourceV2.java` | 1h | T-001.4.2 |
| **T-001.4.6** | Escribir tests unitarios: (a) `OpenAiLlmService` disponible → extracción exitosa con entidades + confidence, (b) `content` vacío → 400, (c) `ChatLanguageModel` no resoluble → 503, (d) mock OpenAI error → 502, (e) mock timeout → 504, (f) mock retorna 0 entidades → 200 con lista vacía | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/service/MemoryServiceImplTest.java` (extender) | 3h | T-001.4.5 |
| **T-001.4.7** | Ejecutar test de integración con OpenAI real (requiere API key): enviar 10 textos del dominio infraestructura IT y verificar que las entidades extraídas tienen tipos `SERVER`, `SERVICE`, `ERROR_CONDITION`, `TIMESTAMP` con `confidence` > 0. Medir latencia p95 ≤ 3s | QA | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/ai/OpenAiE2ETest.java` (extender) | 4h | T-001.4.5 |

---

### 6.2 EP-V21-002 — Velocidad y Latencia

---

#### FT-V21-002.1: Cache de Grafo con Caffeine

**Complejidad**: Media | **Esfuerzo estimado**: 1.5 días (12h + 4h test) | **Desarrollador**: developer-backend

**Descripción**: Implementar `GraphCacheService` con Caffeine. Clave = `hash(sorted(entryPointIds) + ":" + depth + ":" + hash(includeKinds))`. TTL 60s, max 1000 entradas LRU. Invalidación por evento CDI ante mutaciones de `RelationEntity`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-002.1.1** | Crear interfaz `GraphCacheService` en `domain/service/` con métodos `get(String key)`, `put(String key, GraphExpansionResult result)`, `invalidateByMemoryId(UUID memoryId)`, `getMetrics()` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/service/GraphCacheService.java` | 0.5h | — |
| **T-002.1.2** | Crear DTO `GraphExpansionResult` (inmutable) con campos: `entryPointIds`, `expandedNodes`, `maxDepth`, `cacheHit` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/model/GraphExpansionResult.java` | 0.5h | — |
| **T-002.1.3** | Implementar `GraphCacheServiceImpl` en `infrastructure/cache/` usando `com.github.benmanes.caffeine.cache.Cache`. Configurar: TTL 60s (leer de `abax.v2.graph-cache.ttl-seconds`), maxSize 1000 (leer de `abax.v2.graph-cache.max-size`), política LRU. Método `buildKey(Set<UUID> entryPointIds, int depth, Set<MemoryKind> kinds)` que genera hash determinista | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/cache/GraphCacheServiceImpl.java` | 4h | T-002.1.1, T-002.1.2 |
| **T-002.1.4** | Integrar `GraphCacheService` en `SearchServiceImpl.unifiedSearch()`: antes de `expandGraphConsolidated()`, consultar caché con `graphCacheService.get(key)`. Si hit → usar resultado cacheado con `cacheHit: true`. Si miss → ejecutar BFS, almacenar en caché, retornar con `cacheHit: false` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-002.1.3, T-001.3.3 |
| **T-002.1.5** | Crear evento CDI `GraphMutatedEvent` con campos `sourceId`, `targetId`. Dispararlo desde `RelationServiceImpl.createRelation()` y `RelationServiceImpl.deleteRelation()` usando `jakarta.enterprise.event.Event.fire()` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/model/GraphMutatedEvent.java`, `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/RelationServiceImpl.java` | 1.5h | T-002.1.3 |
| **T-002.1.6** | Implementar invalidación reactiva en `GraphCacheServiceImpl`: método `@ObservesAsync onGraphMutated(GraphMutatedEvent event)` que invalida todas las entradas de caché cuyos `entryPointIds` contengan `event.getSourceId()` o `event.getTargetId()` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/cache/GraphCacheServiceImpl.java` | 1.5h | T-002.1.5 |
| **T-002.1.7** | Producir bean `GraphCacheService` en `InfrastructureConfig`. Exponer métricas vía `GraphCacheService.getMetrics()`: `hitRatio`, `size`, `evictions` | Config | `backend-quarkus/src/main/java/com/abax/memory/config/InfrastructureConfig.java` | 1h | T-002.1.6 |
| **T-002.1.8** | Escribir tests unitarios: (a) cache miss → BFS ejecutado, (b) cache hit → BFS omitido, latencia reducida ≥ 50%, (c) misma clave con distintos `includeKinds` → cache miss, (d) invalidación por evento CDI tras nueva relación → cache miss en siguiente query, (e) evicción LRU cuando caché lleno | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/cache/GraphCacheServiceTest.java` | 3h | T-002.1.7 |
| **T-002.1.9** | Agregar test de rendimiento: 3 queries idénticas con `expandGraph=true`. Verificar que queries 2-3 tienen latencia ≥ 50% menor que query 1 | QA | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/service/SearchServiceImplTest.java` (extender) | 1h | T-002.1.4 |

---

#### FT-V21-002.2: Mitigación de Latencia Qdrant

**Complejidad**: Baja-Media | **Esfuerzo estimado**: 3 días (16h diagnóstico + 8h mitigación) | **Responsable**: devops + developer-backend

**Descripción**: Diagnosticar causas de spikes de latencia (~2s) y aplicar mitigaciones de configuración. Ejecutar pruebas de carga en 3 escenarios: cold start, steady state, escritura concurrente.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-002.2.1** | Configurar entorno de pruebas de carga: instalar k6 o Apache Bench. Crear script `load-test.js` que ejecute 300 queries `POST /api/v2/search` con 10 VUs. Medir p50, p95, p99 | DevOps | `scripts/perf/load-test-search.js` (crear) | 3h | — |
| **T-002.2.2** | Ejecutar diagnóstico Semana 1: medir latencia en 3 escenarios: (a) cold start (inmediatamente tras reinicio Qdrant), (b) steady state (tras 5 min de carga), (c) escritura concurrente (10 POST /memories + 300 búsquedas simultáneas). Medir latencia por componente: embedding, Qdrant search, cross-encoder, BFS | DevOps | `scripts/perf/diagnose-latency.sh` (crear), reporte en `docs/entregables/v2.1/fase-5-qa/diagnostico-latencia-qdrant.md` | 8h | T-002.2.1 |
| **T-002.2.3** | Verificar hipótesis de cold start: inspeccionar `optimizers_config` de Qdrant. Si `default_segment_number` está subconfigurado o segmentos no se pre-calentan → implementar `@Startup` CDI bean que ejecute 20 queries de warm-up al iniciar Quarkus | Backend / Config | `backend-quarkus/src/main/java/com/abax/memory/config/QdrantWarmup.java` (crear), `application.properties` | 3h | T-002.2.2 |
| **T-002.2.4** | Verificar hipótesis de lock contention: revisar `wal_config` y `optimizers_config.flush_interval_sec` de Qdrant. Si no están configurados → añadir `wal_config.wal_capacity_mb=64` y `optimizers_config.flush_interval_sec=5` en configuración de despliegue Qdrant | DevOps | `docker/qdrant/config.yaml` (crear/modificar) | 2h | T-002.2.2 |
| **T-002.2.5** | Verificar hipótesis de network latency: revisar timeouts y connection pooling en `QdrantEmbeddingClient`. Configurar timeouts HTTP (connect 2s, read 5s). Configurar HTTP/1.1 keep-alive | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/qdrant/QdrantEmbeddingClient.java` | 2h | T-002.2.2 |
| **T-002.2.6** | Verificar hipótesis de GC pauses: revisar configuración JVM. Si no hay tuning → añadir `-XX:MaxGCPauseMillis=100` en `application.properties` (`quarkus.native.additional-build-args`) | Config | `backend-quarkus/src/main/resources/application.properties` | 1h | T-002.2.2 |
| **T-002.2.7** | Re-ejecutar pruebas de carga post-mitigaciones. Verificar p95 ≤ 500ms en los 3 escenarios. Documentar resultados (antes vs después) en el runbook de operaciones | QA / Docs | `scripts/perf/load-test-search.js` (reusar), `docs/runbooks/latency-tuning.md` | 3h | T-002.2.3, T-002.2.4, T-002.2.5, T-002.2.6 |
| **T-002.2.8** | Documentar decisión: si el diagnóstico revela necesidad de upgrade Qdrant → escalar al sponsor vía ADR (riesgo RSK-04). No aplicar upgrade sin aprobación explícita | Documentación | `docs/entregables/v2.1/fase-3-diseno-tecnico/ADR-006b-upgrade-qdrant.md` (crear si aplica) | 2h | T-002.2.7 |

---

#### FT-V21-002.3: Cache de Validación JWT

**Complejidad**: Media | **Esfuerzo estimado**: 2 días (16h + 2h QA) | **Desarrollador**: developer-backend

**Descripción**: Cachear resultado de validación JWT en memoria con Caffeine. Clave = SHA-256 del token. TTL = `exp - now`. Invalidación por Keycloak Admin Events.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-002.3.1** | Crear interfaz `JwtCacheService` en `infrastructure/security/` con métodos `get(String tokenHash)`, `put(String tokenHash, JwtClaims claims, long ttlSeconds)`, `invalidate(String tokenHash)`, `getMetrics()` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/security/JwtCacheService.java` | 0.5h | — |
| **T-002.3.2** | Implementar `JwtCacheServiceImpl` usando Caffeine. Clave = SHA-256 del JWT. TTL = `min(exp - now, 3600)`. Almacenar `JwtClaims` (tenant, roles, sub). Métricas: `hitRatio`, `size`, `evictions` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/security/JwtCacheServiceImpl.java` | 3h | T-002.3.1 |
| **T-002.3.3** | Integrar `JwtCacheService` en `TenantFilter.filter()`: antes de validar contra Keycloak, calcular SHA-256 del token → consultar caché. Si hit → usar claims cacheados. Si miss → validar contra Keycloak → almacenar en caché. Si Keycloak inaccesible y token no en caché → 503 | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/security/TenantFilter.java` | 4h | T-002.3.2 |
| **T-002.3.4** | Crear clase `KeycloakAdminEventPoller` con método `@Scheduled(every="5s")` que consulta `GET /admin/realms/{realm}/events?type=LOGOUT,REVOKE_GRANT`. Al detectar evento de revocación → invalidar entrada en `JwtCacheService`. Configurable vía `abax.v2.jwt-cache.admin-events-enabled` (default `true`) | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/security/KeycloakAdminEventPoller.java` | 4h | T-002.3.2 |
| **T-002.3.5** | Configurar propiedades en `application.properties`: `abax.v2.jwt-cache.enabled=true`, `abax.v2.jwt-cache.max-size=10000`, `abax.v2.jwt-cache.admin-events-enabled=true`, `abax.v2.jwt-cache.admin-events-poll-interval=5s` | Config | `backend-quarkus/src/main/resources/application.properties` | 0.5h | T-002.3.2 |
| **T-002.3.6** | Producir beans `JwtCacheService` y `KeycloakAdminEventPoller` en `InfrastructureConfig`. Si `abax.v2.jwt-cache.enabled=false` → stub que siempre retorna cache miss | Config | `backend-quarkus/src/main/java/com/abax/memory/config/InfrastructureConfig.java` | 1h | T-002.3.5 |
| **T-002.3.7** | Escribir tests unitarios: (a) cache miss → validación contra Keycloak, (b) cache hit → sin llamada a Keycloak, latencia ≤ 5ms, (c) TTL vencido → nueva validación, (d) evento de revocación → invalidación + siguiente request rechazado, (e) Keycloak inaccesible + token en caché → request procede | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/security/JwtCacheServiceTest.java` | 3h | T-002.3.6 |
| **T-002.3.8** | Verificar con 100 requests consecutivas: métrica `jwt_cache_hit_ratio` ≥ 0.90. Latencia de autenticación en requests 2-100 ≤ 5ms | QA | `scripts/perf/jwt-cache-benchmark.sh` (crear) | 2h | T-002.3.3 |

---

### 6.3 EP-V21-003 — Eficiencia Operativa

---

#### FT-V21-003.1: Diagnóstico y Resolución del Worker Inactivo

**Complejidad**: Baja (Escenario A) | **Esfuerzo estimado**: 1.5 días (8h diagnóstico + 4h acción + 4h verificación) | **Responsable**: devops + developer-backend

**Descripción**: Diagnosticar causa raíz del worker con `Claimed=0`. Escenario A (más probable): eliminar worker porque procesamiento es síncrono. Escenario B: reparar conexión a cola.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-003.1.1** | Inspeccionar código del worker legacy: `com.btl.administrador.api.service.ProcessingWorkerService`. Verificar si está conectado a alguna cola de mensajes o solo hace polling de `ProcessingJobRepository` | Diagnóstico | `backend-quarkus/src/main/java/com/btl/administrador/api/service/ProcessingWorkerService.java`, `backend-quarkus/src/main/java/com/btl/administrador/api/persistence/ProcessingJobRepository.java` | 2h | — |
| **T-003.1.2** | Revisar configuración en `application.properties`: `abax.v2.processing.auto-run` vs `abax.processing.auto-run`. Verificar qué worker está activo y si el v2 está implementado | Diagnóstico | `backend-quarkus/src/main/resources/application.properties` | 1h | — |
| **T-003.1.3** | Verificar en logs de staging: ¿el worker arranca? ¿intenta procesar jobs? ¿hay errores de conexión? Documentar hallazgos en `diagnostico-worker.md` | Diagnóstico | `docs/entregables/v2.1/fase-3-diseno-tecnico/diagnostico-worker.md` (crear) | 3h | T-003.1.1, T-003.1.2 |
| **T-003.1.4** | Confirmar que `MemoryServiceImpl` procesa embeddings y entidades de forma síncrona en `POST /memories`: verificar llamadas a `indexFragment()` y `extractEntities()` dentro del método `createMemory()` | Diagnóstico | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/MemoryServiceImpl.java` | 2h | — |
| **T-003.1.5** | **Escenario A** (confirmado): Eliminar configuración `abax.v2.processing.*` de `application.properties`. Eliminar o deshabilitar worker del despliegue (quitar `@Startup` o beans relacionados). Documentar que el procesamiento es 100% síncrono | Backend / Config | `backend-quarkus/src/main/resources/application.properties`, `backend-quarkus/src/main/java/com/btl/administrador/api/service/ProcessingWorkerService.java` | 2h | T-003.1.3, T-003.1.4 |
| **T-003.1.6** | **Escenario B** (si aplica): Reparar conexión del worker a la cola de mensajes. Configurar polling. Verificar que procesa jobs correctamente | Backend / DevOps | `backend-quarkus/src/main/java/com/btl/administrador/api/service/ProcessingWorkerService.java` | 4h | T-003.1.3 |
| **T-003.1.7** | Prueba de verificación: (a) worker eliminado/reparado, (b) ingestar 10 memorias → buscables en ≤ 5s, (c) latencia p95 de `POST /memories` no excede v2.0.9 en >10% | QA | `backend-quarkus/src/test/java/com/abax/memory/api/rest/v2/MemoryResourceV2Test.java` (extender) | 2h | T-003.1.5 o T-003.1.6 |

---

#### FT-V21-003.2: Unificación de Colecciones Qdrant

**Complejidad**: Media | **Esfuerzo estimado**: 1.5 días (8h script + 4h código + 4h verificación) | **Responsable**: devops + developer-backend

**Descripción**: Consolidar colecciones `abax-memories-v1` y `abax-memories-v2` en una sola `abax-memories`. Script offline pre-deploy. Eliminar hardcode de colección en `SearchServiceImpl`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-003.2.1** | Ejecutar query de verificación pre-migración en PostgreSQL: determinar si hay `memory_id` activos cuyos puntos vectoriales estén exclusivamente en `abax-memories-v1`. Si count > 0 → plan de migración. Si count == 0 → seguro eliminar | DevOps | Script SQL ad-hoc, `docs/entregables/v2.1/fase-3-diseno-tecnico/checklist-migracion-qdrant.md` | 2h | — |
| **T-003.2.2** | Crear script de migración offline: (a) `GET /collections/abax-memories-v1/points/scroll` → batch de puntos, (b) `PUT /collections/abax-memories-v2/points` con upsert, (c) crear snapshot de v1, (d) `DELETE /collections/abax-memories-v1`. Script idempotente y con rollback | DevOps | `scripts/qdrant-unify-collections.sh` (crear) | 4h | T-003.2.1 |
| **T-003.2.3** | Renombrar colección `abax-memories-v2` → `abax-memories`. Si Qdrant 1.17 no soporta rename → crear alias `abax-memories` → `abax-memories-v2` | DevOps | `scripts/qdrant-unify-collections.sh` (extender) | 2h | T-003.2.2 |
| **T-003.2.4** | Eliminar hardcode `QDRANT_COLLECTION = "abax-memories-v2"` en `SearchServiceImpl`. Leer colección de propiedad `abax.v2.qdrant.collection`. Actualizar default en `InfrastructureConfig` de `"abax-memories-v2"` a `"abax-memories"` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java`, `backend-quarkus/src/main/java/com/abax/memory/config/InfrastructureConfig.java` | 2h | — |
| **T-003.2.5** | Actualizar `application.properties`: cambiar `abax.v2.qdrant.collection=abax-memories-v2` → `abax.v2.qdrant.collection=abax-memories`. Eliminar `abax.qdrant.collection=abax-memories` (legado v1) si ya no se usa | Config | `backend-quarkus/src/main/resources/application.properties` | 0.5h | T-003.2.4 |
| **T-003.2.6** | Ejecutar verificación post-migración: (a) `GET /collections` → solo `abax-memories`, (b) `GET /collections/abax-memories-v1` → 404, (c) suite multi-dominio 50 queries → 100% resultados esperados, (d) ingesta 10 memorias → puntos en `abax-memories`, buscables en ≤ 2s | QA | `scripts/qdrant-verify-unification.sh` (crear) | 4h | T-003.2.5 |
| **T-003.2.7** | Actualizar tests de integración Qdrant para usar la nueva colección unificada | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/qdrant/QdrantIntegrationTest.java` | 1h | T-003.2.5 |

---

#### FT-V21-003.3: graphEntryStrategy Configurable

**Complejidad**: Baja | **Esfuerzo estimado**: 0.5 día (4h + 2h test) | **Desarrollador**: developer-backend

**Descripción**: Almacenar `graphEntryStrategy` en el campo JSONB `config` de `DomainProfileEntity`. Parsear en runtime. Valores: `single-best`, `top-k` (default), `threshold`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-003.3.1** | Crear enum `GraphEntryStrategy` en `domain/enums/` con valores: `SINGLE_BEST`, `TOP_K`, `THRESHOLD` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/enums/GraphEntryStrategy.java` (crear) | 0.5h | — |
| **T-003.3.2** | Implementar método `parseGraphEntryStrategy()` en `SearchServiceImpl` que lee `config.graphEntryStrategy` del JSONB de `DomainProfileEntity.findDefault()`. Valores default: `strategy=TOP_K`, `graphK=3`, `graphThreshold=0.80`. Validar en runtime con log `WARN` si configuración malformada → fallback a default | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-003.3.1 |
| **T-003.3.3** | Integrar con `expandGraphConsolidated()`: usar estrategia parseada del perfil como default. Permitir sobrescritura por header `X-Graph-Strategy` (FT-004.1). Asegurar que `entryPoints` explícitos tienen máxima precedencia | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 1.5h | T-003.3.2, T-001.3.3 |
| **T-003.3.4** | Escribir tests: (a) perfil con `top-k` y K=5 → 5 entry points, (b) perfil con `threshold` y threshold=0.85 → solo matches ≥ 0.85, (c) perfil sin `graphEntryStrategy` → default `top-k` con K=3, (d) cambio de estrategia en caliente sin reinicio | Test | `backend-quarkus/src/test/java/com/abax/memory/infrastructure/service/DomainProfileTest.java` (extender) | 2h | T-003.3.3 |

---

### 6.4 EP-V21-004 — API y Developer Experience

---

#### FT-V21-004.1: Header X-Graph-Strategy

**Complejidad**: Baja | **Esfuerzo estimado**: 0.5 día (4h + 2h test + 2h docs) | **Desarrollador**: developer-backend

**Descripción**: Headers HTTP `X-Graph-Strategy`, `X-Graph-K`, `X-Graph-Threshold` para control granular por request. Parsear en `SearchResourceV2`, pasar a `SearchServiceImpl`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-004.1.1** | Añadir `@HeaderParam` en `SearchResourceV2.unifiedSearch()`: `X-Graph-Strategy` (String), `X-Graph-K` (Integer), `X-Graph-Threshold` (Double). Validar valores: `X-Graph-Strategy` ∈ {`none`, `single`, `top-k`, `threshold`}, `X-Graph-K` ∈ [1, 10], `X-Graph-Threshold` ∈ [0.0, 1.0]. Valor inválido → HTTP 400 | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` | 1.5h | T-003.3.1 |
| **T-004.1.2** | Crear DTO `GraphStrategyOverride` (inmutable) con campos `strategy`, `graphK`, `graphThreshold`. Pasar de `SearchResourceV2` a `SearchServiceImpl.unifiedSearch()` como parámetro opcional | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/model/GraphStrategyOverride.java` (crear) | 0.5h | T-004.1.1 |
| **T-004.1.3** | Modificar `SearchServiceImpl.unifiedSearch()`: aceptar `GraphStrategyOverride`. Si presente → sobrescribe estrategia del perfil. Si `X-Graph-Strategy: none` → sin expansión de grafo (incluso si `expandGraph: true` en body). `entryPointSource` en respuesta = `"header-override"` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-004.1.2, T-003.3.3 |
| **T-004.1.4** | Escribir tests: (a) `X-Graph-Strategy: none` → 0 nodos expandidos, (b) `X-Graph-Strategy: top-k` + `X-Graph-K: 5` → 5 entry points, (c) valor inválido → 400, (d) `X-Graph-K` sin `X-Graph-Strategy` → ignorado (log DEBUG), (e) `top-k` con `X-Graph-Threshold` enviado → ignorado | Test | `backend-quarkus/src/test/java/com/abax/memory/api/rest/v2/SearchResourceV2Test.java` (extender) | 2h | T-004.1.3 |
| **T-004.1.5** | Actualizar anotaciones OpenAPI en `SearchResourceV2`: documentar los 3 headers con `@Parameter`, valores aceptados, defaults | Documentación | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` | 1h | T-004.1.1 |
| **T-004.1.6** | Actualizar documentación de API: añadir sección "Graph Strategy Headers" en README o docs de API | Documentación | `docs/entregables/v2.1/fase-4-construccion/api-reference.md` (crear/extender) | 1h | T-004.1.5 |

---

#### FT-V21-004.2: Unificación de Endpoints search/hybrid

**Complejidad**: Media | **Esfuerzo estimado**: 2 días (16h + 2h test) | **Desarrollador**: developer-backend

**Descripción**: Unificar `POST /search`, `POST /search/hybrid` y `POST /search/semantic` en un solo endpoint con parámetros `semanticWeight` y `lexicalWeight`. Deprecar `/hybrid` con headers `Deprecation` y `Warning`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-004.2.1** | Añadir campos `semanticWeight` (double, default 1.0) y `lexicalWeight` (double, default 0.0) a `UnifiedSearchRequest`. Validar: ambos en [0.0, 1.0], al menos uno > 0. Si suman > 1.0 → normalizar (log DEBUG). Añadir campo `rerank` (boolean, default true) — mover desde DTO de semantic si existía | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` | 1.5h | — |
| **T-004.2.2** | Crear endpoint `POST /memories/search` como alias de `POST /search`. Internamente delega al mismo `SearchResourceV2.unifiedSearch()`. Path: añadir `@Path("/memories/search")` en `SearchResourceV2` o crear método que redirige | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` | 1h | — |
| **T-004.2.3** | Modificar `SearchServiceImpl.hybridSearch()` existente para que acepte pesos configurables en lugar de los hardcodeados (0.7 semántico / 0.3 léxico). El método legacy `hybridSearch` con endpoint `/hybrid` usa pesos fijos 0.5/0.5 | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 3h | T-004.2.1 |
| **T-004.2.4** | Implementar lógica de fusión de scores en `SearchServiceImpl`: `finalScore = semanticWeight * normalizedSemanticScore + lexicalWeight * normalizedLexicalScore`. Si `lexicalWeight > 0` y no hay índice léxico → degradar a `semanticWeight=1.0`, log `WARN` | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 2h | T-004.2.3 |
| **T-004.2.5** | Añadir headers de deprecación en `POST /search/hybrid`: `Deprecation: true`, `Warning: 299 - "POST /search/hybrid is deprecated. Use POST /search with semanticWeight and lexicalWeight parameters."`. Internamente delega a `unifiedSearch` con pesos 0.5/0.5 | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` | 1h | T-004.2.3 |
| **T-004.2.6** | Añadir campo `weights` (`semantic`, `lexical`) en metadata `pipeline` del `UnifiedSearchResponse` | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchResponse.java` | 0.5h | T-004.2.1 |
| **T-004.2.7** | Integrar `SearchResourceV2.unifiedSearch()` con `SearchServiceImpl`: pasar `semanticWeight`, `lexicalWeight`, `rerank` del DTO al servicio. El endpoint unificado decide internamente si llamar a `hybridSearch` (ambos pesos > 0) o `semanticSearch` (`lexicalWeight == 0`) o búsqueda léxica pura (`semanticWeight == 0`) | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java`, `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` | 3h | T-004.2.4, T-004.2.6 |
| **T-004.2.8** | Escribir tests: (a) `semanticWeight=1.0, lexicalWeight=0.0` → equivalente a semántico puro v2.0.9, (b) `semanticWeight=0.6, lexicalWeight=0.4` → combinación híbrida, (c) ambos pesos = 0 → 400, (d) pesos suman > 1.0 → normalización, (e) `/hybrid` legacy → headers deprecation + funcionalidad idéntica a v2.0.9, (f) suite multi-dominio 100 queries → equivalencia funcional con v2.0.9 | Test | `backend-quarkus/src/test/java/com/abax/memory/api/rest/v2/SearchResourceV2Test.java` (extender) | 2h | T-004.2.7 |
| **T-004.2.9** | Actualizar documentación OpenAPI: nuevo endpoint canónico `/search`, parámetros `semanticWeight`/`lexicalWeight`, headers de deprecación en `/hybrid` | Documentación | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` | 1h | T-004.2.7 |

---

#### FT-V21-004.3: DELETE /admin/namespaces/{name}

**Complejidad**: Alta | **Esfuerzo estimado**: 2.5 días (20h + 2h test) | **Desarrollador**: developer-backend

**Descripción**: Nuevo endpoint administrativo atómico. Crear `AdminResourceV2` y `NamespaceService`. Transacción PostgreSQL + eliminación Qdrant. Requiere rol `memory-admin`.

**Tareas**:

| ID | Tarea | Tipo | Archivo(s) | Esfuerzo | Dependencias |
|---|---|---|---|---|---|
| **T-004.3.1** | Crear interfaz `NamespaceService` en `domain/service/` con método `DeleteNamespaceResult deleteNamespace(String name, String tenantId)` y DTO `DeleteNamespaceResult` con campos `deletedMemories`, `deletedRelations`, `deletedEntities`, `deletedQdrantPoints` | Backend | `backend-quarkus/src/main/java/com/abax/memory/domain/service/NamespaceService.java`, `backend-quarkus/src/main/java/com/abax/memory/domain/model/DeleteNamespaceResult.java` | 1h | — |
| **T-004.3.2** | Implementar `NamespaceServiceImpl` en `infrastructure/service/` con lógica de eliminación atómica: (1) `BEGIN TRANSACTION`, (2) contar y eliminar `MemoryFragmentEntity` por namespace + tenant, (3) contar y eliminar `RelationEntity` cuyos `sourceId` o `targetId` pertenezcan a las memorias eliminadas, (4) contar y eliminar entidades asociadas, (5) `COMMIT`. Si éxito → eliminar puntos Qdrant. Si Qdrant falla → log `ERROR` crítico. Retornar conteos | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/NamespaceServiceImpl.java` | 6h | T-004.3.1 |
| **T-004.3.3** | Crear `AdminResourceV2` en `api/rest/v2/` con endpoint `DELETE /admin/namespaces/{name}`. Anotar con `@RolesAllowed("memory-admin")`. Extraer tenant de `TenantContext`. Delegar en `NamespaceService`. Retornar 200 con `DeleteNamespaceResult` o 404 si namespace no existe | Backend | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/AdminResourceV2.java` (crear) | 3h | T-004.3.2 |
| **T-004.3.4** | Configurar autorización: extender `TenantFilter` (o usar anotación `@RolesAllowed`) para verificar claim `realm_access.roles` contiene `memory-admin`. Sin rol → 403. Configurar `quarkus.oidc.roles.role-claim-path=realm_access/roles` si no está ya | Backend / Config | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/security/TenantFilter.java`, `backend-quarkus/src/main/resources/application.properties` | 2h | — |
| **T-004.3.5** | Producir bean `NamespaceService` en `InfrastructureConfig` | Config | `backend-quarkus/src/main/java/com/abax/memory/config/InfrastructureConfig.java` | 0.5h | T-004.3.2 |
| **T-004.3.6** | Añadir `AuditRecord` en cada `DELETE` exitoso: `action: "NAMESPACE_DELETE"`, `actor_id`, `tenant_id`, `details: {namespace, deleted_counts}`. Usar `AuditService` existente | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/NamespaceServiceImpl.java` | 1.5h | T-004.3.2 |
| **T-004.3.7** | Implementar batch delete de puntos Qdrant para namespaces grandes (>1000 puntos). Dividir en batches de 100 puntos. Timeout total 30s. Usar `QdrantClient.deletePoints()` con filtro por `namespace` en payload | Backend | `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/NamespaceServiceImpl.java` | 2h | T-004.3.2 |
| **T-004.3.8** | Escribir tests: (a) namespace con 50 memorias + relaciones + entidades + puntos Qdrant → DELETE exitoso con conteos correctos, (b) namespace inexistente → 404, (c) sin rol `memory-admin` → 403, (d) `DELETE` idempotente → segunda llamada retorna 404, (e) transacción atómica: simular fallo Qdrant → PostgreSQL intacto (rollback) | Test | `backend-quarkus/src/test/java/com/abax/memory/api/rest/v2/AdminResourceV2Test.java` (crear) | 2h | T-004.3.7 |
| **T-004.3.9** | Documentar endpoint en OpenAPI con `@Operation`, `@APIResponse` para 200, 400, 403, 404, 500 | Documentación | `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/AdminResourceV2.java` | 1h | T-004.3.3 |
| **T-004.3.10** | Actualizar `CHANGELOG.md` y migration notes documentando el nuevo endpoint, sus requisitos de autorización, y la irreversibilidad de la operación | Documentación | `CHANGELOG.md` (raíz del proyecto) | 1h | T-004.3.9 |

---

## 7. Criterios de Completitud por Feature

Cada feature se considera **Done** cuando cumple TODOS los siguientes criterios, verificados por el tech-lead antes del gate Fase 3 → Fase 4.

### 7.1 Checklist por Feature

| # | Criterio | FT-001.1 | FT-001.2 | FT-001.3 | FT-001.4 | FT-002.1 | FT-002.2 | FT-002.3 | FT-003.1 | FT-003.2 | FT-003.3 | FT-004.1 | FT-004.2 | FT-004.3 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **DOD-01** | Código commiteado con mensaje que referencia `FT-V21-XXX.X` | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-02** | Code review aprobado por tech-lead | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-03** | Tests unitarios pasan (verde) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-04** | Criterios de aceptación cumplidos (ver §7.2) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-05** | Sin `MOCK` ni `TODO` en features core (Must/Should) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-06** | Documentación técnica actualizada (ADR, runbook, OpenAPI) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| **DOD-07** | Suite de regresión v2.0.9 pasa al 100% | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |

### 7.2 Criterios de Aceptación Vinculados

| Feature | CAs a verificar | Método de verificación |
|---|---|---|
| **FT-001.1** | CA-V21-001.1.1 a CA-V21-001.1.6 (6 Must) | Benchmarks SciFact + A/B test + inspección de logs |
| **FT-001.2** | CA-V21-001.2.1 a CA-V21-001.2.3 (3 Must) | Tests unitarios: expandGraph=false → 0 nodos grafo |
| **FT-001.3** | CA-V21-001.3.1 a CA-V21-001.3.7 (7 Must) | Tests unitarios + benchmark ABM-MULTI-01 |
| **FT-001.4** | CA-V21-001.4.1 a CA-V21-001.4.4 (4 Should) | Tests unitarios + integración con OpenAI real |
| **FT-002.1** | CA-V21-002.1.1 a CA-V21-002.1.6 (6 Could) | Tests unitarios + verificación batch fetching |
| **FT-002.2** | CA-V21-002.2.1 a CA-V21-002.2.3 (3 Could) | Pruebas de carga con k6 en 3 escenarios |
| **FT-002.3** | CA-V21-002.3.1 a CA-V21-002.3.4 (4 Should) | Tests unitarios + verificación de métricas |
| **FT-003.1** | CA-V21-003.1.1 a CA-V21-003.1.3 (3 Could) | Documento diagnóstico + verificación ingesta |
| **FT-003.2** | CA-V21-003.2.1 a CA-V21-003.2.3 (3 Should) | Script verificación + suite multi-dominio |
| **FT-003.3** | CA-V21-003.3.1 a CA-V21-003.3.3 (3 Could) | Tests unitarios con perfiles de dominio |
| **FT-004.1** | CA-V21-004.1.1 a CA-V21-004.1.4 (4 Could) | Tests de integración con headers HTTP |
| **FT-004.2** | CA-V21-004.2.1 a CA-V21-004.2.4 (4 Could) | Suite 100 queries multi-dominio + verificación headers deprecation |
| **FT-004.3** | CA-V21-004.3.1 a CA-V21-004.3.4 (4 Could) | Tests de integración: delete + verificar 0 resultados |
| **Transversales** | CA-TRANS-01 a CA-TRANS-10 | Suite regresión v2.0.9 + inspección documentación |

### 7.3 Done Global de la Release

| # | Condición | Estado |
|---|---|---|
| **DOD-G-01** | 13/13 features en estado Done | ☐ |
| **DOD-G-02** | 10/10 Criterios de Éxito cumplidos (CE-01 a CE-10) | ☐ |
| **DOD-G-03** | 10/10 Criterios Transversales cumplidos (CA-TRANS-01 a CA-TRANS-10) | ☐ |
| **DOD-G-04** | Suite de regresión v2.0.9 pasa al 100% | ☐ |
| **DOD-G-05** | Sin defects P1/P2 abiertos | ☐ |
| **DOD-G-06** | `CHANGELOG.md` actualizado con 13 features | ☐ |

---

## 8. Glosario

- **BFS**: Breadth-First Search — algoritmo de recorrido de grafos por niveles usado en `expandGraphConsolidated()`.
- **Caffeine**: Biblioteca Java de caché en memoria de alto rendimiento, nueva dependencia en v2.1.0.
- **CDI**: Contexts and Dependency Injection — estándar Jakarta EE para inyección de dependencias usado por Quarkus.
- **Cross-encoder**: Modelo de reranking que evalúa pares (consulta, documento) simultáneamente para calcular relevancia fina por entailment.
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación validados contra Keycloak.
- **LRU**: Least Recently Used — política de evicción de caché que elimina la entrada menos recientemente usada.
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10.
- **p95**: Percentil 95 — latencia por debajo de la cual se completa el 95% de las solicitudes.
- **Qdrant**: Base de datos vectorial open-source para búsqueda semántica por similitud de coseno (3072-dim).
- **R1/R2**: Release 1 (MVP, Must/Should) y Release 2 (completa, Should/Could) — estrategia de entregables incrementales.
