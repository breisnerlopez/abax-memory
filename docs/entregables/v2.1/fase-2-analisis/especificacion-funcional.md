---
fase: 2 — Análisis Funcional
entregable: Especificación Funcional
version: v2.1.0
responsable: business-analyst
fecha: 2026-05-05
estado: Completado
fuentes:
  - docs/entregables/v2.1/fase-0-descubrimiento/vision-producto.md
  - docs/entregables/v2.1/fase-0-descubrimiento/epicas-features.md
  - docs/entregables/v2.1/fase-0-descubrimiento/historias-usuario.md
  - docs/entregables/v2.1/fase-0-descubrimiento/backlog-priorizado.md
  - docs/entregables/v2/fase-2-analisis/especificacion-funcional.md (baseline v2.0.9)
  - docs/entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md
---

# Especificación Funcional — Abax-Memory v2.1.0
## Hardening y Optimización del Motor de Memoria Multi-Dominio

---

## Tabla de Contenidos

- [1. Introducción](#1-introducción)
  - [1.1 Propósito](#11-propósito)
  - [1.2 Relación con v2.0.9](#12-relación-con-v209)
  - [1.3 Principios de diseño funcional](#13-principios-de-diseño-funcional)
- [2. Especificación por Feature](#2-especificación-por-feature)
  - [2.1 EP-V21-001 — Precisión del Motor de Búsqueda](#21-ep-v21-001--precisión-del-motor-de-búsqueda)
    - [2.1.1 FT-001.1: Pipeline con Reranker Cross-Encoder](#211-ft-0011-pipeline-con-reranker-cross-encoder)
    - [2.1.2 FT-001.2: Búsqueda Semántica Pura (`search` sin `expandGraph`)](#212-ft-0012-búsqueda-semántica-pura-search-sin-expandgraph)
    - [2.1.3 FT-001.3: Expansión de Grafo desde Top-3 con `entryPoints` Explícitos](#213-ft-0013-expansión-de-grafo-desde-top-3-con-entrypoints-explícitos)
    - [2.1.4 FT-001.4: `POST /extract` con OpenAI Real](#214-ft-0014-post-extract-con-openai-real)
  - [2.2 EP-V21-002 — Velocidad y Latencia](#22-ep-v21-002--velocidad-y-latencia)
    - [2.2.1 FT-002.1: Preservación N+1 + Cache de Resultados de Grafo](#221-ft-0021-preservación-n1--cache-de-resultados-de-grafo)
    - [2.2.2 FT-002.2: Mitigación de Cold Start / Lock en Qdrant](#222-ft-0022-mitigación-de-cold-start--lock-en-qdrant)
    - [2.2.3 FT-002.3: Cache de Validación JWT en Backend](#223-ft-0023-cache-de-validación-jwt-en-backend)
  - [2.3 EP-V21-003 — Eficiencia Operativa](#23-ep-v21-003--eficiencia-operativa)
    - [2.3.1 FT-003.1: Diagnóstico y Resolución del Worker Inactivo](#231-ft-0031-diagnóstico-y-resolución-del-worker-inactivo)
    - [2.3.2 FT-003.2: Unificación de Colecciones Qdrant](#232-ft-0032-unificación-de-colecciones-qdrant)
    - [2.3.3 FT-003.3: `graphEntryStrategy` Configurable por Perfil de Dominio](#233-ft-0033-graphentrystrategy-configurable-por-perfil-de-dominio)
  - [2.4 EP-V21-004 — API y Developer Experience](#24-ep-v21-004--api-y-developer-experience)
    - [2.4.1 FT-004.1: Header `X-Graph-Strategy`](#241-ft-0041-header-x-graph-strategy)
    - [2.4.2 FT-004.2: Unificación de Endpoints `search` y `hybrid`](#242-ft-0042-unificación-de-endpoints-search-y-hybrid)
    - [2.4.3 FT-004.3: `DELETE /admin/namespaces/{name}`](#243-ft-0043-delete-adminnamespacesname)
- [3. Matriz de Trazabilidad](#3-matriz-de-trazabilidad)
  - [3.1 Features → Historias de Usuario](#31-features--historias-de-usuario)
  - [3.2 Features → Criterios de Éxito](#32-features--criterios-de-éxito)
- [4. Reglas de Negocio Consolidadas](#4-reglas-de-negocio-consolidadas)
- [5. Compatibilidad con v2.0.9](#5-compatibilidad-con-v209)
- [6. Alcance y Exclusiones](#6-alcance-y-exclusiones)
- [7. Glosario](#7-glosario)

---

## 1. Introducción

### 1.1 Propósito

Este documento constituye la **especificación funcional completa** de Abax-Memory v2.1.0: una iteración de hardening y optimización sobre v2.0.9. Define, para cada una de las 13 features distribuidas en 4 épicas, el **comportamiento esperado** desde la perspectiva del consumidor de la API, las **entradas** aceptadas, las **salidas** producidas, los **casos de borde** manejados y la **compatibilidad** con el comportamiento existente de v2.0.9.

Es un documento de **QUÉ** hace el sistema, no de **CÓMO** lo implementa. Está dirigido al equipo de diseño técnico, implementación, QA y al product owner como referencia contractual de lo que se construirá y verificará.

### 1.2 Relación con v2.0.9

v2.1.0 **no es una re-arquitectura**. Es una mejora incremental sobre el stack y la API de v2.0.9. Por tanto:

| Aspecto | Comportamiento en v2.1.0 |
|---|---|
| **Stack tecnológico** | Inalterado: Quarkus 3.15.3, Java 21, PostgreSQL 16, Qdrant 1.17, Keycloak 26, OpenAI (`text-embedding-3-large`, `gpt-4o-mini`). |
| **Modelo de datos** | Sin cambios en entidades JPA, tablas, columnas, constraints o migraciones Flyway. |
| **API v2** | Backward compatible. Se añaden endpoints/headers/parámetros; se depreca `hybrid` con warning; no hay breaking changes. |
| **Perfiles de dominio** | Se extienden con el campo `graphEntryStrategy` (nuevo atributo de configuración). |
| **Roles RBAC** | Sin cambios. Se añade el requerimiento `memory-admin` para `DELETE /admin/namespaces/{name}`. |

### 1.3 Principios de diseño funcional

1. **Transparencia**: el consumidor siempre sabe qué pipeline se usó para producir los resultados (semántico puro, grafo, híbrido).
2. **Control granular**: el consumidor puede controlar por request la estrategia de búsqueda vía headers y parámetros explícitos.
3. **Degradación graceful**: si un componente del pipeline no está disponible (cross-encoder, OpenAI), el sistema degrada al pipeline base sin fallar.
4. **Atomicidad**: las operaciones administrativas de destrucción de datos (`DELETE namespace`) son atómicas: todo o nada.
5. **Backward compatibility**: ningún consumidor existente de la API v2 debe romper. Todo cambio es aditivo o marcado como deprecated.

---

## 2. Especificación por Feature

### 2.1 EP-V21-001 — Precisión del Motor de Búsqueda

**Meta de la épica**: Top-1 ≥ 0.90 en suite multi-dominio, NDCG@10 ≥ 0.85 en SciFact, extracción de entidades con IA real.

---

#### 2.1.1 FT-001.1: Pipeline con Reranker Cross-Encoder

**ID**: FT-001.1
**Feature canónica**: FT-V21-001.1
**Prioridad**: 1 — Máxima
**Historias vinculadas**: HU-V21-001, HU-V21-002
**Criterios de éxito vinculados**: CE-01, CE-03, CE-04

##### Comportamiento esperado

El pipeline de búsqueda se transforma de single-stage (dense retrieval → top-K) a **two-stage**:

1. **Stage 1 — Dense retrieval**: el query embedding se compara contra Qdrant vía similitud de coseno. Se recuperan los **top-20** candidatos por score semántico.
2. **Stage 2 — Cross-encoder reranker**: cada par (query, documento candidato) se evalúa con un modelo cross-encoder que determina relevancia fina (entailment). Los 20 candidatos se reordenan por el score del cross-encoder y se retorna el **top-5 final**.
3. **Si el cross-encoder no está disponible** (API key ausente, modelo no desplegado), el pipeline **degrada gracefulmente** a dense retrieval puro (comportamiento v2.0.9). Se registra `CROSS_ENCODER_UNAVAILABLE` en logs y el endpoint sigue funcionando sin error.

Este pipeline aplica cuando:
- `POST /memories/search` con `expandGraph: false` (búsqueda semántica pura).
- `POST /memories/search` con `expandGraph: true` en la etapa de recuperación de entry points (el cross-encoder reordena los entry points antes de la expansión de grafo).

##### Entradas

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `query` | Body | string | Sí | — | Texto de la consulta. Mínimo 1 carácter no blanco. |
| `topK` | Body | integer | No | 10 | Número de resultados a retornar. El reranker opera sobre top-20 y retorna los mejores `topK` (máximo 5 tras reranker). |
| `rerank` | Body | boolean | No | `true` | Activa/desactiva la etapa de cross-encoder. Si `false`, el pipeline opera como dense retrieval puro (v2.0.9). |
| `expandGraph` | Body | boolean / object | No | `false` | Controla expansión de grafo (ver FT-001.2). |
| `filters` | Body | object | No | — | Filtros estructurados multidimensionales (sin cambios respecto a v2.0.9). |

> **Nota**: el parámetro `topK` solicitado por el cliente puede ser mayor que 5, pero el reranker solo reordena el top-20 y produce un ranking de hasta 20. Si `topK > 5`, el sistema retorna hasta `topK` resultados del ranking rerankeado (máximo 20). La recomendación funcional es `topK ≤ 5` para aprovechar el reranker.

##### Salidas

**Response `200 OK`** — búsqueda exitosa:

```json
{
  "results": [
    {
      "memoryId": "MEM-a1b2c3d4",
      "kind": "fact",
      "summary": "The mRNA vaccine showed 95% efficacy in clinical trials",
      "content": "...",
      "score": 0.94,
      "scoreComponents": {
        "semantic": 0.82,
        "crossEncoder": 0.94
      },
      "pipeline": "two-stage",
      "lifecycle": { "status": "active", "importance": 0.8, "confidence": 0.95 },
      "topics": ["biomedical", "vaccine"],
      "entities": ["mRNA", "clinical trial"]
    }
  ],
  "totalResults": 5,
  "queryTimeMs": 380,
  "pipeline": {
    "stages": ["dense-retrieval", "cross-encoder-reranker"],
    "denseRetrievalCandidates": 20,
    "crossEncoderApplied": true
  }
}
```

**Campos nuevos respecto a v2.0.9**:

| Campo | Tipo | Descripción |
|---|---|---|
| `scoreComponents` | object | Desglose del score final. `semantic` = score del dense retrieval. `crossEncoder` = score del reranker (solo si `rerank: true`). Si `rerank: false`, `scoreComponents` contiene solo `semantic`. |
| `pipeline` (resultado) | string | Indica qué pipeline produjo este resultado: `"dense-only"` o `"two-stage"`. |
| `pipeline` (respuesta) | object | Metadatos del pipeline ejecutado: stages aplicados, número de candidatos evaluados, si el cross-encoder se aplicó. |

**Response cuando el cross-encoder no está disponible**:

El endpoint retorna `200 OK` con resultados del dense retrieval puro. El campo `pipeline.stages` contiene solo `["dense-retrieval"]` y `pipeline.crossEncoderApplied: false`. En logs se registra `CROSS_ENCODER_UNAVAILABLE` con nivel `WARN`.

**Response cuando no hay resultados**: `200 OK` con `results: []` y `totalResults: 0`.

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-01 | `query` vacío o solo espacios | `HTTP 400 VALIDATION_ERROR`: `"query must not be blank"`. |
| CE-02 | `topK: 0` | `HTTP 400 VALIDATION_ERROR`: `"topK must be >= 1"`. |
| CE-03 | `topK > 100` | `HTTP 400 VALIDATION_ERROR`: `"topK must be <= 100"`. |
| CE-04 | Dense retrieval retorna < 20 candidatos | El cross-encoder evalúa los N candidatos disponibles (ej. 7). No es error. `denseRetrievalCandidates` refleja el número real. |
| CE-05 | Dense retrieval retorna 0 candidatos | No se invoca al cross-encoder. Se retorna `results: []`. |
| CE-06 | `rerank: true` pero cross-encoder no disponible | Degradación graceful a dense retrieval. `HTTP 200`. Log `WARN`. `crossEncoderApplied: false`. |
| CE-07 | Cross-encoder retorna error en 1 de los 20 pares | Se omite ese candidato del reranking y se evalúan los 19 restantes. El candidato omitido se coloca al final del ranking con su score semántico original. Log `ERROR` con detalles (sin exponer al cliente). |
| CE-08 | Latencia del cross-encoder excede timeout | Timeout de 2s por lote de 20 pares. Si se excede, se cancela la etapa de reranking y se retorna el orden del dense retrieval. Log `WARN CROSS_ENCODER_TIMEOUT`. |

##### Compatibilidad

- **Request**: El parámetro `rerank` es nuevo. Si no se envía, el default es `true` (aplica reranker). Consumidores que no conozcan este parámetro obtienen automáticamente el pipeline two-stage, que produce resultados iguales o mejores que v2.0.9.
- **Response**: Se añaden `scoreComponents`, `pipeline` (en cada resultado y a nivel respuesta). Los campos existentes (`score`, `memoryId`, etc.) se mantienen sin cambios. Consumidores que ignoren los nuevos campos no se ven afectados.
- **Parámetros existentes**: `filters`, `expandGraph`, `topK` operan idéntico a v2.0.9.
- **Sin degradación de recall**: el cross-encoder reordena, no descarta. El Recall@10 debe mantenerse ≥ 0.90 (CE-04).

---

#### 2.1.2 FT-001.2: Búsqueda Semántica Pura (`search` sin `expandGraph`)

**ID**: FT-001.2
**Feature canónica**: FT-V21-001.2
**Prioridad**: 2 — Alta
**Historias vinculadas**: HU-V21-003
**Criterios de éxito vinculados**: CE-07

##### Comportamiento esperado

El endpoint `POST /memories/search` con `expandGraph: false` (o sin el parámetro `expandGraph`) debe comportarse como **búsqueda exclusivamente semántica**: dense retrieval + cross-encoder reranker. **Ningún** resultado debe provenir de expansión de grafo de conocimiento.

La activación del grafo solo ocurre cuando el consumidor lo solicita explícitamente con `expandGraph: true` (o el objeto `expandGraph` con `depth` y `includeKinds`).

Este comportamiento elimina la ambigüedad de v2.0.9, donde el endpoint `search` podía incluir resultados del grafo sin que el consumidor lo supiera o lo hubiera solicitado explícitamente.

##### Entradas

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `expandGraph` | Body | boolean / object | No | `false` | Si es `false` o está ausente: búsqueda semántica pura. Si es `true` o un objeto con `depth`: se activa la expansión de grafo (comportamiento definido en FT-001.3). |

El resto de parámetros (`query`, `topK`, `rerank`, `filters`) se comportan idéntico a FT-001.1.

##### Salidas

**Con `expandGraph: false`** (búsqueda semántica pura):

```json
{
  "results": [
    {
      "memoryId": "MEM-x1y2z3a4",
      "score": 0.88,
      "graphExpanded": false,
      "scoreComponents": { "semantic": 0.82, "crossEncoder": 0.88 }
    }
  ],
  "pipeline": {
    "stages": ["dense-retrieval", "cross-encoder-reranker"],
    "graphExpanded": false
  }
}
```

**Con `expandGraph: true`** (búsqueda con grafo):

```json
{
  "results": [
    {
      "memoryId": "MEM-x1y2z3a4",
      "score": 0.88,
      "graphExpanded": true,
      "expandedFrom": "MEM-a1b2c3d4",
      "relationType": "caused_by",
      "depth": 1
    }
  ],
  "pipeline": {
    "stages": ["dense-retrieval", "cross-encoder-reranker", "graph-expansion"],
    "graphExpanded": true,
    "graphExpandedNodes": {
      "entryPointIds": ["MEM-a1b2c3d4", "MEM-b5c6d7e8", "MEM-f9g0h1i2"],
      "entryPointCount": 3,
      "totalExpandedNodes": 12,
      "maxDepth": 2
    }
  }
}
```

**Campos nuevos respecto a v2.0.9**:

| Campo | Ubicación | Tipo | Descripción |
|---|---|---|---|
| `graphExpanded` | result | boolean | Si este resultado proviene del grafo (`true`) o del pipeline semántico (`false`). |
| `expandedFrom` | result | string | ID de la memoria desde la que se expandió (solo si `graphExpanded: true`). |
| `pipeline.graphExpandedNodes` | response | object | Resumen de la expansión de grafo: entry points usados, conteo, nodos expandidos, profundidad máxima. |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-09 | `expandGraph` ausente en el body | Se comporta como `expandGraph: false`. Búsqueda semántica pura. |
| CE-10 | `expandGraph: false` explícito | Búsqueda semántica pura. `pipeline.graphExpanded: false`. Campo `graphExpandedNodes` ausente en la respuesta. |
| CE-11 | `expandGraph: true` pero sin `depth` especificado | `depth` default = 1 (vecinos directos). |
| CE-12 | `expandGraph.depth: 0` | `HTTP 400 VALIDATION_ERROR`: `"expandGraph.depth must be >= 1"`. |
| CE-13 | `expandGraph.depth > 5` | `HTTP 400 VALIDATION_ERROR`: `"expandGraph.depth must be <= 5"`. |

##### Compatibilidad

- El parámetro `expandGraph` ya existía en v2.0.9 como objeto `{"depth": N, "includeKinds": [...]}`. En v2.0.9, su semántica no estaba claramente aislada del pipeline semántico (el grafo podía contribuir resultados sin que `expandGraph` estuviera explícitamente activo). En v2.1.0, la semántica se endurece: sin `expandGraph` o con `expandGraph: false`, **cero contribuciones del grafo**.
- Consumidores de v2.0.9 que sí usaban `expandGraph` explícitamente no ven cambio de comportamiento: envían `expandGraph: {depth: N}` y reciben resultados con grafo.
- Consumidores de v2.0.9 que usaban `search` sin `expandGraph` y **dependían** de que el grafo contribuyera resultados: su comportamiento puede cambiar. Este es el único caso de cambio de comportamiento, y es intencional (la ambigüedad se corrige). Ver sección 5 — Compatibilidad.

---

#### 2.1.3 FT-001.3: Expansión de Grafo desde Top-3 con `entryPoints` Explícitos

**ID**: FT-001.3
**Feature canónica**: FT-V21-001.3
**Prioridad**: 3 — Alta
**Historias vinculadas**: HU-V21-004, HU-V21-005
**Criterios de éxito vinculados**: CE-01

##### Comportamiento esperado

Cuando el consumidor solicita `expandGraph: true`, el motor expande el grafo de conocimiento desde **los 3 nodos más relevantes** del dense retrieval (en lugar del mejor match único como en v2.0.9). Esta expansión multi-origen mejora la cobertura cross-dominio al recuperar vecinos de múltiples entry points.

Adicionalmente, el consumidor puede especificar **explícitamente** los `entryPoints` (IDs de memorias) desde los cuales expandir el grafo, ignorando la selección automática del dense retrieval. Esto da control total a consumidores avanzados que conocen los nodos semilla correctos.

El comportamiento varía según la estrategia de entrada al grafo configurada (ver FT-003.3 y FT-004.1):

| Estrategia | Comportamiento |
|---|---|
| `single-best` | Expande solo desde el mejor match del dense retrieval (comportamiento v2.0.9). |
| `top-k` (default para v2.1.0) | Expande desde los K mejores matches. K default = 3. |
| `threshold` | Expande desde todos los matches con score ≥ umbral configurable. |
| `none` | No expande el grafo, incluso si `expandGraph: true`. Equivalente funcional a `expandGraph: false`. |

La profundidad de expansión (`graphDepth`) es configurable por request (1-5, default 1). La expansión usa BFS por niveles y respeta `includeKinds` para filtrar tipos de memoria en los vecinos.

##### Entradas

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `expandGraph` | Body | boolean / object | No | `false` | `true` activa expansión. Objeto con `depth` e `includeKinds` para control fino. |
| `expandGraph.depth` | Body | integer | No | 1 | Profundidad de BFS (1-5). |
| `expandGraph.includeKinds` | Body | array[enum] | No | null (todos) | Tipos de memoria a incluir en la expansión. |
| `entryPoints` | Body | array[string] | No | null | IDs de memorias semilla. Si se especifica, ignora la selección automática del dense retrieval. Máximo 10 entry points explícitos. |
| `X-Graph-Strategy` | Header | string | No | Config del perfil | `none`, `single`, `top-k`, `threshold`. Ver FT-004.1. |
| `X-Graph-K` | Header | integer | No | 3 | Número de entry points para estrategia `top-k`. |
| `X-Graph-Threshold` | Header | float | No | 0.80 | Score mínimo para estrategia `threshold`. Rango [0.0, 1.0]. |

##### Salidas

**Response con `expandGraph: true` y expansión top-3**:

```json
{
  "results": [
    {
      "memoryId": "MEM-sem-001",
      "score": 0.92,
      "graphExpanded": false,
      "scoreComponents": { "semantic": 0.85, "crossEncoder": 0.92 }
    },
    {
      "memoryId": "MEM-graph-001",
      "score": 0.78,
      "graphExpanded": true,
      "expandedFrom": "MEM-sem-001",
      "relationType": "caused_by",
      "depth": 1
    }
  ],
  "pipeline": {
    "stages": ["dense-retrieval", "cross-encoder-reranker", "graph-expansion"],
    "graphExpandedNodes": {
      "entryPointIds": ["MEM-sem-001", "MEM-sem-002", "MEM-sem-003"],
      "entryPointCount": 3,
      "entryPointSource": "dense-retrieval-top-3",
      "totalExpandedNodes": 14,
      "maxDepth": 2
    }
  }
}
```

**Response con `entryPoints` explícitos**:

```json
{
  "pipeline": {
    "graphExpandedNodes": {
      "entryPointIds": ["MEM-user-001", "MEM-user-002"],
      "entryPointCount": 2,
      "entryPointSource": "client-provided",
      "totalExpandedNodes": 8,
      "maxDepth": 1
    }
  }
}
```

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-14 | Dense retrieval retorna solo 1 resultado (score > 0) | Se expande desde 1 entry point (el único disponible). `entryPointCount: 1`. No es error. |
| CE-15 | `entryPoints` contiene un ID inexistente (`"MEM-999"`) | Log `WARN ENTRY_POINT_NOT_FOUND` para ese ID. Se excluye silenciosamente de la expansión. Se expande desde los entry points restantes válidos. Si ningún entry point es válido, se retorna `expandGraph: true` pero con `graphExpandedNodes.totalExpandedNodes: 0`. |
| CE-16 | `entryPoints` y `X-Graph-Strategy: top-k` simultáneos | Los `entryPoints` explícitos tienen **precedencia**. Se ignoran `X-Graph-Strategy` y `X-Graph-K`. Log `INFO` indicando `"Using client-provided entryPoints; X-Graph-Strategy ignored for this request"`. |
| CE-17 | `entryPoints` con más de 10 IDs | `HTTP 400 VALIDATION_ERROR`: `"entryPoints: maximum 10 entries allowed"`. |
| CE-18 | `entryPoints` con IDs duplicados | Se deduplican silenciosamente. Log `DEBUG`. |
| CE-19 | `graphDepth: 3` solicitado pero el grafo solo tiene 2 niveles desde cierto entry point | Se expande hasta la profundidad máxima disponible. `maxDepth` en respuesta refleja el valor real alcanzado. |
| CE-20 | `includeKinds: ["procedure"]` pero ningún vecino es `procedure` | La expansión retorna 0 vecinos para ese entry point. No es error. |

##### Compatibilidad

- **Cambio de default**: en v2.0.9, la expansión era siempre desde el mejor match único (equivalente a `single-best`). En v2.1.0, el default es `top-k` con K=3. Esto significa que consumidores que usaban `expandGraph` en v2.0.9 y no especifican estrategia verán **más nodos expandidos** en v2.1.0 (potencialmente mejores resultados de cobertura cross-dominio, pero también más resultados en la respuesta).
- **`entryPoints` es nuevo**: no existía en v2.0.9. Consumidores existentes no lo usan y no se ven afectados.
- **Parámetros existentes**: `expandGraph.depth` e `expandGraph.includeKinds` operan idéntico a v2.0.9.
- **Estructura de respuesta**: se añade `pipeline.graphExpandedNodes` como metadata. Los resultados individuales ya incluían `expandedFrom` y `relationType` en v2.0.9; esto se preserva.

---

#### 2.1.4 FT-001.4: `POST /extract` con OpenAI Real

**ID**: FT-001.4
**Feature canónica**: FT-V21-001.4
**Prioridad**: 6 — Media-Alta
**Historias vinculadas**: HU-V21-006
**Criterios de éxito vinculados**: CE-06

##### Comportamiento esperado

El endpoint `POST /memories/extract` utiliza **OpenAI `gpt-4o-mini`** (no `MockLlmService`) para extraer entidades del texto de entrada mediante análisis semántico. Las entidades extraídas incluyen:

- **Nombres propios**: servidores, servicios, personas, organizaciones, productos.
- **Códigos y condiciones**: códigos de error, estados de sistema.
- **Temporalidad**: timestamps, fechas, duraciones.
- **Entidades de dominio**: cualquier concepto relevante al dominio del texto.

Cada entidad incluye:
- `name`: nombre canónico de la entidad.
- `type`: clasificación (ej. `SERVER`, `SERVICE`, `ERROR_CONDITION`, `TIMESTAMP`, `PERSON`, `ORGANIZATION`, `METRIC`, `TECHNOLOGY`, `LOCATION`).
- `confidence`: score de confianza [0.0, 1.0] asignado por el modelo.

##### Entradas

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `content` | Body | string | Sí | — | Texto del cual extraer entidades. Mínimo 1 carácter no blanco. Máximo 5,000 caracteres. |
| `domain` | Body | string | No | `"general"` | Sugerencia de dominio para orientar la extracción (`"ops"`, `"legal"`, `"biomedical"`, `"general"`). |

##### Salidas

**Response `200 OK`** — extracción exitosa:

```json
{
  "entities": [
    { "name": "nginx-prod-01", "type": "SERVER", "confidence": 0.95 },
    { "name": "OOM", "type": "ERROR_CONDITION", "confidence": 0.92 },
    { "name": "14:32 UTC", "type": "TIMESTAMP", "confidence": 0.99 },
    { "name": "api-gateway", "type": "SERVICE", "confidence": 0.88 }
  ],
  "source": "openai-gpt-4o-mini",
  "extractionTimeMs": 1200
}
```

**Campos respecto a v2.0.9**:

| Campo | v2.0.9 (MockLlmService) | v2.1.0 (OpenAI real) |
|---|---|---|
| `entities[].name` | Extraído por regex (patrones superficiales) | Extraído por IA semántica |
| `entities[].type` | Limitado a ~3 tipos hardcodeados | Tipos variados según dominio |
| `entities[].confidence` | No existía | Nuevo: score de confianza |
| `source` | No existía | Nuevo: indica el proveedor de IA |
| `extractionTimeMs` | No existía | Nuevo: latencia de la extracción |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-21 | `content` vacío (`""`) | `HTTP 400 VALIDATION_ERROR`: `"content must not be blank"`. |
| CE-22 | `content` excede 5,000 caracteres | `HTTP 400 VALIDATION_ERROR`: `"content exceeds maximum length of 5000 characters"`. |
| CE-23 | API key de OpenAI no configurada | `HTTP 503 SERVICE_UNAVAILABLE`: `"Entity extraction unavailable: LLM service not configured"`. Log `ERROR`. |
| CE-24 | API key de OpenAI vencida o sin crédito | `HTTP 502 BAD_GATEWAY`: `"Entity extraction failed: LLM provider error"`. Log `ERROR` con detalles (sin exponer la API key). |
| CE-25 | OpenAI retorna respuesta vacía (sin entidades detectadas) | `HTTP 200 OK` con `entities: []`. No es error. |
| CE-26 | Timeout de OpenAI (> 5s) | `HTTP 504 GATEWAY_TIMEOUT`: `"Entity extraction timed out"`. Log `ERROR`. |
| CE-27 | Texto en idioma no inglés | El modelo `gpt-4o-mini` soporta múltiples idiomas. Las entidades se extraen en el idioma del texto. Los tipos de entidad siempre se retornan en inglés. |

##### Compatibilidad

- **Request**: el campo `domain` es nuevo. El campo `content` se mantiene idéntico a v2.0.9 (aunque en v2.0.9 se llamaba `text` en algunos contextos; se unifica como `content` para consistencia con el resto de la API).
- **Response**: la estructura base (`entities[]`) se mantiene, pero se añaden `confidence`, `source` y `extractionTimeMs`. Consumidores que solo lean `entities[].name` y `entities[].type` no se ven afectados.
- **Calidad**: la extracción con OpenAI es **significativamente superior** a MockLlmService (regex). Consumidores que dependían del mock verán más entidades, de mayor calidad, y con tipos más precisos. Esto es una mejora, no una ruptura.

---

### 2.2 EP-V21-002 — Velocidad y Latencia

**Meta de la épica**: p95 ≤ 500ms estable para búsqueda semántica (CE-02).

---

#### 2.2.1 FT-002.1: Preservación N+1 + Cache de Resultados de Grafo

**ID**: FT-002.1
**Feature canónica**: FT-V21-002.1
**Prioridad**: 7 — Media
**Historias vinculadas**: HU-V21-007, HU-V21-008
**Criterios de éxito vinculados**: CE-02

##### Comportamiento esperado

Dos acciones complementarias sobre la expansión de grafo:

1. **Preservación de optimización N+1**: la estrategia de batch fetching de relaciones del grafo implementada en v2.0.x (una consulta batch por nivel de profundidad en lugar de N consultas individuales por relación) **se mantiene y verifica** que no se degrada con los cambios de v2.1.0 (expansión top-3, estrategias configurables).

2. **Cache de resultados de grafo**: los resultados de la expansión BFS desde un conjunto de entry points con un depth dado se cachean en memoria. Si dos queries comparten exactamente los mismos `entryPoints` (mismo conjunto de IDs) y `graphDepth`, el subgrafo resultado se sirve desde caché.

Reglas funcionales del caché:

| Regla | Descripción |
|---|---|
| **Clave de caché** | Hash del conjunto ordenado de `entryPointIds` + `graphDepth` + `includeKinds`. |
| **TTL** | Configurable, default 60 segundos. |
| **Invalidación** | Ante cualquier mutación del grafo que afecte a los entry points cacheados (nueva relación, eliminación de relación, cambio de status de una memoria vecina), la entrada de caché correspondiente se invalida. |
| **Límite de capacidad** | Configurable, default 1,000 entradas. Política de evicción: LRU. |
| **Métricas** | Se exponen `graph_cache_hit_ratio`, `graph_cache_size`, `graph_cache_evictions` vía `/api/v2/metrics`. |

##### Entradas

Sin cambios en la interfaz de entrada respecto a FT-001.3. El caché es transparente para el consumidor.

##### Salidas

**Response con cache hit** (segunda query idéntica):

```json
{
  "results": [...],
  "pipeline": {
    "graphExpandedNodes": {
      "entryPointIds": ["MEM-sem-001", "MEM-sem-002", "MEM-sem-003"],
      "cacheHit": true
    }
  },
  "queryTimeMs": 85
}
```

El campo `pipeline.graphExpandedNodes.cacheHit` indica si el resultado del grafo provino de caché.

**Response con cache miss** (primera query):

```json
{
  "pipeline": {
    "graphExpandedNodes": {
      "cacheHit": false
    }
  },
  "queryTimeMs": 320
}
```

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-28 | Mutación del grafo entre query 1 y query 2 (nueva relación sobre un entry point cacheado) | Cache miss en query 2. BFS fresco. Resultados incluyen la nueva relación. |
| CE-29 | Eliminación de una memoria que era entry point cacheado | Invalidación de todas las entradas de caché que referencien ese entry point. |
| CE-30 | Caché lleno (1,000 entradas) y nueva entrada | Evicción LRU de la entrada menos recientemente usada. Sin error. |
| CE-31 | Mismos entry points pero distinto `includeKinds` | Cache miss. La clave de caché incluye `includeKinds`. |
| CE-32 | `entryPoints` explícitos vs automáticos con el mismo conjunto de IDs | Cache hit. La clave se basa en el conjunto de IDs, no en la fuente. |

##### Compatibilidad

- Totalmente transparente para el consumidor. Los resultados son idénticos con o sin caché; solo cambia la latencia.
- La optimización N+1 ya existía en v2.0.9 y se preserva sin cambios.
- El campo `cacheHit` es nuevo en la respuesta. Consumidores que lo ignoren no se ven afectados.

---

#### 2.2.2 FT-002.2: Mitigación de Cold Start / Lock en Qdrant

**ID**: FT-002.2
**Feature canónica**: FT-V21-002.2
**Prioridad**: 8 — Media
**Historias vinculadas**: HU-V21-009
**Criterios de éxito vinculados**: CE-02

##### Comportamiento esperado

Se diagnostican y mitigan los escenarios que producen spikes de latencia a ~2s en el pipeline de búsqueda semántica. El resultado funcional observable por el consumidor es: **latencia p95 estable ≤ 500ms** en `POST /memories/search` (sin `expandGraph`) bajo tres condiciones:

1. **Cold start**: inmediatamente tras el despliegue del servicio Qdrant o la aplicación.
2. **Steady state**: operación normal con carga continua.
3. **Bajo escritura concurrente**: ingesta de memorias simultánea a búsquedas.

La feature no expone nuevos parámetros ni endpoints. Es una optimización interna cuyo resultado se mide en latencia.

##### Entradas

Sin cambios en la interfaz. La verificación se realiza mediante pruebas de carga automatizadas.

##### Salidas

Sin cambios en la interfaz de respuesta. La latencia (`queryTimeMs`) debe ser ≤ 500ms en el percentil 95 bajo las 3 condiciones descritas.

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-33 | Reinicio del servicio Qdrant (cold start real) | La primera query post-reinicio tiene latencia ≤ 1s. Las siguientes 10 queries convergen a ≤ 500ms. |
| CE-34 | 10 escrituras concurrentes + 300 queries de búsqueda simultáneas | p95 de búsqueda ≤ 500ms. Ninguna query excede 2s. |
| CE-35 | Índice Qdrant con ≥ 10,000 puntos vectoriales | p95 ≤ 500ms. Sin degradación por tamaño de índice. |

##### Compatibilidad

Totalmente transparente. El comportamiento funcional del endpoint `search` es idéntico. Solo cambia la latencia observada.

> **Nota**: Esta feature requiere una fase de diagnóstico antes de aplicar mitigaciones. Si el diagnóstico revela que se requiere upgrade de versión de Qdrant (ej. de 1.17 a 1.18+), esto requeriría un ADR aprobado por el sponsor por entrar en conflicto con la restricción R-01 (stack inalterado). Ver riesgo RSK-04 en el backlog priorizado.

---

#### 2.2.3 FT-002.3: Cache de Validación JWT en Backend

**ID**: FT-002.3
**Feature canónica**: FT-V21-002.3
**Prioridad**: 4 — Alta
**Historias vinculadas**: HU-V21-010
**Criterios de éxito vinculados**: CE-02

##### Comportamiento esperado

El backend cachea en memoria el resultado de la validación de tokens JWT contra Keycloak. Un token validado exitosamente se almacena con TTL igual al campo `exp` del JWT (típicamente 1 hora). Requests subsecuentes con el mismo token se validan contra el caché local sin llamar a Keycloak.

La caché se invalida proactivamente ante eventos de revocación (logout, cambio de roles) recibidos de Keycloak Admin Events. Si Keycloak Admin Events no está disponible, el TTL del caché actúa como ventana máxima de aceptación de un token revocado.

##### Entradas

Sin cambios en la interfaz. El header `Authorization: Bearer <jwt>` se procesa idéntico a v2.0.9.

##### Salidas

Sin cambios en la interfaz de respuesta de los endpoints protegidos. La caché JWT es transparente para el consumidor.

**Métricas expuestas** (nuevas):

| Métrica | Descripción |
|---|---|
| `jwt_cache_hit_ratio` | Proporción de validaciones servidas desde caché vs total. |
| `jwt_cache_size` | Número de tokens actualmente en caché. |
| `jwt_cache_evictions` | Total de evicciones (por TTL o invalidación). |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-36 | Primer request de un cliente con JWT válido | Cache miss. Validación contra Keycloak. Token almacenado en caché con TTL = `exp - now`. |
| CE-37 | Request subsecuente con el mismo JWT (99 requests más) | Cache hit. Validación local ≤ 5ms. Sin llamada a Keycloak. |
| CE-38 | JWT expirado en caché (TTL vencido) | Evicción por TTL. Validación contra Keycloak. Si Keycloak rechaza → `HTTP 401`. Si Keycloak acepta → nuevo ciclo de caché. |
| CE-39 | Token revocado en Keycloak (logout) | Al recibir evento de revocación, invalidación de caché en ≤ 5s. Siguiente request con ese token → `HTTP 401` tras validación contra Keycloak. |
| CE-40 | Keycloak inaccesible durante validación de token no cacheado | `HTTP 503 SERVICE_UNAVAILABLE`: `"Authentication service unavailable"`. |
| CE-41 | Keycloak inaccesible, pero token está en caché y no expirado | Cache hit exitoso. La request se procesa normalmente. Esto proporciona resiliencia ante caídas temporales de Keycloak. |
| CE-42 | Mismo token, distintos tenants o roles | El caché valida claims contra el token original. Si los roles o tenant cambiaron y el token fue revocado (CE-39), se rechaza. Si el token no fue revocado, se aceptan los claims originales del JWT. |

##### Compatibilidad

Totalmente transparente. La autenticación y autorización se comportan idéntico a v2.0.9 desde la perspectiva del consumidor. La mejora es puramente de latencia.

---

### 2.3 EP-V21-003 — Eficiencia Operativa

**Meta de la épica**: Una sola colección Qdrant en producción (CE-05), worker diagnosticado/resuelto, estrategia de grafo configurable por perfil de dominio.

---

#### 2.3.1 FT-003.1: Diagnóstico y Resolución del Worker Inactivo

**ID**: FT-003.1
**Feature canónica**: FT-V21-003.1
**Prioridad**: 9 — Media
**Historias vinculadas**: HU-V21-011
**Criterios de éxito vinculados**: CE-02 (indirecto)

##### Comportamiento esperado

El worker de procesamiento asíncrono que reporta `Claimed = 0` (sin trabajo procesado) es diagnosticado y resuelto. Hay dos escenarios posibles, mutuamente excluyentes:

**Escenario A — El worker es innecesario**: el procesamiento de embeddings y entidades tras la ingesta de una memoria (`POST /memories`) ya ocurre de forma síncrona dentro del mismo request. En este caso, el worker se **elimina** del despliegue sin afectar la funcionalidad de ingesta.

**Escenario B — El worker es necesario pero está roto**: el procesamiento asíncrono es requerido (ej. para generación de embeddings de alta latencia que no pueden ser síncronos sin degradar la API). En este caso, el worker se **repara** (conexión a cola, configuración de polling) y debe procesar trabajo correctamente.

**Comportamiento funcional resultante en AMBOS escenarios**:

- `POST /memories` sigue creando memorias correctamente (HTTP 201).
- La memoria creada es buscable semánticamente en ≤ 5 segundos tras la ingesta.
- No hay workers inactivos consumiendo recursos en el despliegue.

##### Entradas

Sin cambios en la interfaz de ingesta (`POST /memories`, `POST /memories/ingest`).

##### Salidas

Sin cambios en la interfaz de respuesta de ingesta. La verificación es interna: tras crear una memoria, una búsqueda semántica con el contenido de la memoria la encuentra en el top-10 en ≤ 5 segundos.

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-43 | Escenario A: worker eliminado, ingesta de 10 memorias | Las 10 memorias son buscables semánticamente en ≤ 5s post-ingesta. Sin worker en ejecución. |
| CE-44 | Escenario B: worker reparado, ingesta de 50 memorias | Worker procesa ≥ 45 en ≤ 30s post-ingesta. Claimed ≥ 45. Las 50 memorias buscables en ≤ 5s tras procesamiento. |
| CE-45 | Ingesta durante diagnóstico del worker | Sin interrupción. `POST /memories` funciona normalmente. |

##### Compatibilidad

Totalmente transparente para el consumidor de la API.

---

#### 2.3.2 FT-003.2: Unificación de Colecciones Qdrant

**ID**: FT-003.2
**Feature canónica**: FT-V21-003.2
**Prioridad**: 5 — Alta
**Historias vinculadas**: HU-V21-012
**Criterios de éxito vinculados**: CE-05

##### Comportamiento esperado

Las dos colecciones Qdrant actuales (`abax-memories-v1` y `abax-memories-v2`) se consolidan en una sola colección llamada `abax-memories`. La colección `abax-memories-v1` (datos residuales de v1.0.0 sin uso funcional) se elimina del cluster.

**Precondición**: verificación de que `abax-memories-v1` no contiene puntos referenciados activamente desde PostgreSQL (tabla `memories`). Si contiene datos necesarios, se ejecuta migración antes de la eliminación.

**Resultado funcional**:

- `GET /collections` en Qdrant muestra exactamente 1 colección: `abax-memories`.
- Todas las operaciones de búsqueda semántica operan contra `abax-memories`.
- La ingesta de nuevas memorias almacena puntos vectoriales en `abax-memories`.
- La eliminación de memorias elimina puntos de `abax-memories`.

##### Entradas

Sin cambios en la interfaz de la API.

##### Salidas

Sin cambios en la interfaz de respuesta. La verificación es operativa:

| Verificación | Resultado esperado |
|---|---|
| `GET /collections` (API Qdrant) | Lista con exactamente 1 colección: `abax-memories` |
| `GET /collections/abax-memories-v1` (API Qdrant) | `HTTP 404` |
| `POST /memories/search` (50 queries de suite multi-dominio) | 100% resultados esperados |
| `POST /memories` (ingesta de 10 memorias) | Puntos creados en `abax-memories`, buscables en ≤ 2s |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-46 | Verificación pre-migración encuentra puntos activos en `abax-memories-v1` | Se ejecuta script de migración a `abax-memories` antes de eliminar. La eliminación solo ocurre tras migración exitosa verificada. |
| CE-47 | Error durante la eliminación de `abax-memories-v1` | Rollback: ambas colecciones permanecen intactas. Log `ERROR`. No hay pérdida de datos. |
| CE-48 | Operación de búsqueda durante la migración | Sin interrupción. Las búsquedas se ejecutan contra la colección activa (`abax-memories-v2` antes de la renombración final). |

##### Compatibilidad

Totalmente transparente para el consumidor de la API. La colección subyacente es un detalle de infraestructura.

---

#### 2.3.3 FT-003.3: `graphEntryStrategy` Configurable por Perfil de Dominio

**ID**: FT-003.3
**Feature canónica**: FT-V21-003.3
**Prioridad**: 10 — Media
**Historias vinculadas**: HU-V21-013
**Criterios de éxito vinculados**: CE-01, CE-09

##### Comportamiento esperado

La estrategia de entrada al grafo de conocimiento se expone como un atributo configurable a nivel de **perfil de dominio**, permitiendo que cada perfil optimice su comportamiento de expansión según las características de su dominio.

El nuevo campo `graphEntryStrategy` en la configuración del perfil de dominio acepta:

| Valor | Comportamiento | Parámetros adicionales |
|---|---|---|
| `single-best` | Expande solo desde el mejor match del dense retrieval | — |
| `top-k` | Expande desde los K mejores matches | `graphK` (default 3, rango 1-10) |
| `threshold` | Expande desde todos los matches con score ≥ umbral | `graphThreshold` (default 0.80, rango 0.0-1.0) |

El valor configurado en el perfil de dominio es el **default** para todas las queries en ese dominio. Puede ser sobrescrito por request vía el header `X-Graph-Strategy` (FT-004.1).

**Estructura del perfil de dominio extendida**:

```json
{
  "name": "infrastructure",
  "version": "1.1",
  "graphEntryStrategy": {
    "strategy": "top-k",
    "graphK": 3,
    "graphThreshold": null
  }
}
```

##### Entradas

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `graphEntryStrategy.strategy` | Perfil de dominio | enum | No | `"top-k"` | `single-best`, `top-k`, `threshold`. |
| `graphEntryStrategy.graphK` | Perfil de dominio | integer | No | 3 | Número de entry points para `top-k`. Rango 1-10. |
| `graphEntryStrategy.graphThreshold` | Perfil de dominio | float | No | 0.80 | Score mínimo para `threshold`. Rango [0.0, 1.0]. |

##### Salidas

Sin cambios en la interfaz de respuesta de búsqueda. La estrategia utilizada se refleja en `pipeline.graphExpandedNodes.entryPointSource`:

| `entryPointSource` | Significado |
|---|---|
| `"dense-retrieval-top-3"` | Estrategia `top-k` con K=3 (automática) |
| `"dense-retrieval-single-best"` | Estrategia `single-best` |
| `"dense-retrieval-threshold-0.80"` | Estrategia `threshold` con umbral 0.80 |
| `"client-provided"` | `entryPoints` explícitos del cliente |
| `"header-override"` | Estrategia sobrescrita vía `X-Graph-Strategy` |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-49 | `graphK: 1` en estrategia `top-k` | Funcionalmente equivalente a `single-best`. |
| CE-50 | `graphThreshold: 1.0` en estrategia `threshold` | Solo se expande desde matches con score perfecto (1.0). En la práctica, casi nunca se expande. No es error. |
| CE-51 | `graphThreshold: 0.0` en estrategia `threshold` | Todos los resultados del dense retrieval son entry points (potencialmente muchos). Se aplica límite interno de 10 entry points máximo. |
| CE-52 | Cambio de estrategia en perfil de dominio activo | Se aplica inmediatamente a nuevas queries, sin necesidad de reinicio. |

##### Compatibilidad

- **Cambio de default**: en v2.0.9, la estrategia era `single-best` hardcodeada. En v2.1.0, el default es `top-k` con K=3. Los perfiles de dominio existentes que no especifiquen `graphEntryStrategy` heredan el nuevo default.
- **Perfiles existentes**: se les añade el campo `graphEntryStrategy` con valor default `top-k`. No se requiere migración manual de perfiles.

---

### 2.4 EP-V21-004 — API y Developer Experience

**Meta de la épica**: API unificada con un solo endpoint de búsqueda, header `X-Graph-Strategy` funcional, `DELETE /admin/namespaces/{name}` operativo (CE-08, CE-09, CE-10).

---

#### 2.4.1 FT-004.1: Header `X-Graph-Strategy`

**ID**: FT-004.1
**Feature canónica**: FT-V21-004.1
**Prioridad**: 11 — Media
**Historias vinculadas**: HU-V21-014
**Criterios de éxito vinculados**: CE-09

##### Comportamiento esperado

El consumidor de la API puede controlar la estrategia de expansión del grafo **por request individual** mediante headers HTTP, sobrescribiendo la configuración del perfil de dominio.

##### Entradas (headers nuevos)

| Header | Tipo | Obligatorio | Default | Valores aceptados | Descripción |
|---|---|---|---|---|---|
| `X-Graph-Strategy` | string | No | Config del perfil | `none`, `single`, `top-k`, `threshold` | Estrategia de expansión. `none` = sin expansión, incluso si `expandGraph: true`. |
| `X-Graph-K` | integer | No | 3 (del perfil) | 1–10 | Número de entry points para estrategia `top-k`. Solo aplica con `X-Graph-Strategy: top-k`. |
| `X-Graph-Threshold` | float | No | 0.80 (del perfil) | 0.0–1.0 | Score mínimo para estrategia `threshold`. Solo aplica con `X-Graph-Strategy: threshold`. |

**Reglas de interacción entre headers**:

1. Si `X-Graph-Strategy` está presente, **sobrescribe** completamente la estrategia del perfil de dominio para esa request.
2. Si `X-Graph-Strategy: none`, la expansión de grafo se desactiva para esa request, **independientemente** del valor de `expandGraph` en el body. `expandGraph` se ignora.
3. Si `X-Graph-Strategy: top-k` y `X-Graph-K` está ausente, se usa K=3.
4. Si `X-Graph-Strategy: threshold` y `X-Graph-Threshold` está ausente, se usa 0.80.
5. Si `X-Graph-Strategy: top-k` y el cliente envía `X-Graph-Threshold`, este último se ignora (no aplica a `top-k`). Log `DEBUG`.
6. Los `entryPoints` explícitos en el body tienen **precedencia** sobre cualquier header `X-Graph-Strategy` (ver FT-001.3, CE-16).

##### Salidas

Sin cambios en la interfaz de respuesta respecto a FT-001.3:

```json
{
  "pipeline": {
    "graphExpandedNodes": {
      "entryPointSource": "header-override",
      "entryPointCount": 5
    }
  }
}
```

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-53 | `X-Graph-Strategy: invalid` | `HTTP 400 VALIDATION_ERROR`: `"Invalid X-Graph-Strategy: 'invalid'. Supported: none, single, top-k, threshold"`. |
| CE-54 | `X-Graph-Strategy: top-k` con `X-Graph-K: 0` | `HTTP 400 VALIDATION_ERROR`: `"X-Graph-K must be >= 1"`. |
| CE-55 | `X-Graph-Strategy: top-k` con `X-Graph-K: 15` | `HTTP 400 VALIDATION_ERROR`: `"X-Graph-K must be <= 10"`. |
| CE-56 | `X-Graph-Strategy: threshold` con `X-Graph-Threshold: 1.5` | `HTTP 400 VALIDATION_ERROR`: `"X-Graph-Threshold must be between 0.0 and 1.0"`. |
| CE-57 | `X-Graph-Strategy: none` con `expandGraph: true` en body | Sin expansión de grafo. `pipeline.graphExpanded: false`. `expandGraph` en body es ignorado. Log `DEBUG`. |
| CE-58 | `X-Graph-K` enviado sin `X-Graph-Strategy` (o con `X-Graph-Strategy: single`) | El header `X-Graph-K` se ignora. Log `DEBUG`. No es error. |

##### Compatibilidad

- **Header nuevo**: no existía en v2.0.9. Consumidores que no lo envíen no se ven afectados; se aplica la estrategia del perfil de dominio.
- **Sobrescritura**: el header permite a un consumidor que conoce la nueva API obtener comportamiento `single-best` (equivalente a v2.0.9) incluso si el perfil de dominio migró a `top-k`. Esto garantiza backward compatibility operativa.

---

#### 2.4.2 FT-004.2: Unificación de Endpoints `search` y `hybrid`

**ID**: FT-004.2
**Feature canónica**: FT-V21-004.2
**Prioridad**: 12 — Media
**Historias vinculadas**: HU-V21-015
**Criterios de éxito vinculados**: CE-10

##### Comportamiento esperado

Los endpoints `POST /memories/search` y `POST /memories/hybrid` se unifican en un solo endpoint `POST /memories/search` con parámetros explícitos que cubren todos los modos de búsqueda:

- **Búsqueda semántica pura**: `semanticWeight: 1.0, lexicalWeight: 0.0, expandGraph: false`.
- **Búsqueda léxica pura**: `semanticWeight: 0.0, lexicalWeight: 1.0`.
- **Búsqueda híbrida**: `semanticWeight: 0.6, lexicalWeight: 0.4` (combinación lineal).
- **Búsqueda semántica + grafo**: `semanticWeight: 1.0, lexicalWeight: 0.0, expandGraph: true`.

El endpoint legacy `POST /memories/hybrid` se mantiene **funcional** pero retorna headers de deprecación. Es un alias de `POST /memories/search` con `semanticWeight: 0.5, lexicalWeight: 0.5`.

##### Entradas (endpoint unificado `POST /memories/search`)

| Parámetro | Ubicación | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|---|
| `query` | Body | string | Sí | — | Texto de la consulta. |
| `semanticWeight` | Body | float | No | 1.0 | Peso del componente semántico (vectorial). Rango [0.0, 1.0]. |
| `lexicalWeight` | Body | float | No | 0.0 | Peso del componente léxico (keyword/texto). Rango [0.0, 1.0]. |
| `expandGraph` | Body | boolean / object | No | `false` | Control de expansión de grafo (ver FT-001.2, FT-001.3). |
| `topK` | Body | integer | No | 10 | Número de resultados. |
| `filters` | Body | object | No | — | Filtros estructurados. |
| `rerank` | Body | boolean | No | `true` | Activar cross-encoder (ver FT-001.1). |

**Regla de validación de pesos**: al menos uno de `semanticWeight` o `lexicalWeight` debe ser > 0. Si ambos son 0, `HTTP 400`.

**Entradas (endpoint legacy `POST /memories/hybrid`)**:

Sin cambios respecto a v2.0.9. Internamente se traduce a `POST /memories/search` con `semanticWeight: 0.5, lexicalWeight: 0.5`.

##### Salidas

**Endpoint unificado `POST /memories/search`**:

```json
{
  "results": [...],
  "totalResults": 6,
  "queryTimeMs": 290,
  "pipeline": {
    "stages": ["dense-retrieval", "cross-encoder-reranker"],
    "weights": { "semantic": 1.0, "lexical": 0.0 }
  }
}
```

**Endpoint legacy `POST /memories/hybrid`**:

```json
HTTP/1.1 200 OK
Deprecation: true
Warning: 299 - "POST /memories/hybrid is deprecated. Use POST /memories/search with semanticWeight and lexicalWeight parameters."

{
  "results": [...],
  "pipeline": {
    "weights": { "semantic": 0.5, "lexical": 0.5 }
  }
}
```

**Headers HTTP de deprecación**:

| Header | Valor |
|---|---|
| `Deprecation` | `true` |
| `Warning` | `299 - "POST /memories/hybrid is deprecated. Use POST /memories/search with semanticWeight and lexicalWeight parameters."` |

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-59 | `semanticWeight: 0.0, lexicalWeight: 0.0` | `HTTP 400 VALIDATION_ERROR`: `"At least one of semanticWeight or lexicalWeight must be > 0"`. |
| CE-60 | `semanticWeight: 1.5` | `HTTP 400 VALIDATION_ERROR`: `"semanticWeight must be between 0.0 and 1.0"`. |
| CE-61 | `semanticWeight: 0.7, lexicalWeight: 0.7` (suman > 1.0) | Se normalizan internamente: `semanticWeight = 0.5, lexicalWeight = 0.5`. Log `DEBUG`. |
| CE-62 | `lexicalWeight: 1.0` sin índice léxico disponible | Se degrada a `semanticWeight: 1.0`. Log `WARN`: `"Lexical search unavailable; falling back to semantic-only"`. |
| CE-63 | `POST /memories/hybrid` con `semanticWeight` en body | El parámetro se ignora. Los pesos son fijos (0.5/0.5) para backward compatibility. |
| CE-64 | Suite multi-dominio de 100 test cases con `semanticWeight: 1.0, lexicalWeight: 0.0` | Resultados funcionalmente equivalentes a `POST /memories/search` de v2.0.9. Misma precisión top-1, mismo recall. |

##### Compatibilidad

- **`POST /memories/hybrid` sigue funcional**: los consumidores existentes que usan este endpoint no experimentan interrupción. Reciben los mismos resultados que en v2.0.9, más los headers de deprecación.
- **`POST /memories/search` extiende su funcionalidad**: los nuevos parámetros `semanticWeight` y `lexicalWeight` son opcionales con defaults que preservan el comportamiento de v2.0.9 (semántico puro). Consumidores existentes que no los envíen obtienen `semanticWeight: 1.0, lexicalWeight: 0.0`.
- **Plan de deprecación**: `POST /memories/hybrid` se mantiene funcional durante al menos 1 release (v2.1.0). En v2.2.0 se evaluará su eliminación definitiva.

---

#### 2.4.3 FT-004.3: `DELETE /admin/namespaces/{name}`

**ID**: FT-004.3
**Feature canónica**: FT-V21-004.3
**Prioridad**: 13 — Media
**Historias vinculadas**: HU-V21-016
**Criterios de éxito vinculados**: CE-08

##### Comportamiento esperado

Nuevo endpoint administrativo que elimina **atómica e irreversiblemente** todos los recursos asociados a un namespace:

- Todas las memorias cuyo `scope.namespace` coincide con `{name}` (dentro del tenant autenticado).
- Todas las relaciones que involucran esas memorias.
- Todas las entidades asociadas a esas memorias.
- Todos los puntos vectoriales en Qdrant correspondientes a esas memorias.

La operación es **atómica**: o bien se eliminan todos los recursos, o bien (en caso de fallo parcial) el namespace permanece intacto sin estado inconsistente.

**Requerimiento de autorización**: rol `memory-admin`.

##### Entradas

| Elemento | Valor |
|---|---|
| **Método** | `DELETE` |
| **Path** | `/api/v2/admin/namespaces/{name}` |
| **Path param** | `name` (string): nombre del namespace a eliminar. |
| **Auth** | Bearer JWT con rol `memory-admin`. |
| **Body** | No aplica. |

##### Salidas

**Response `200 OK`** — eliminación exitosa:

```json
{
  "namespace": "benchmark-sifact",
  "deleted": {
    "memories": 50,
    "relations": 20,
    "entities": 15,
    "qdrantPoints": 50
  },
  "operationTimeMs": 2300
}
```

**Response `404 Not Found`** — namespace no existe:

```json
{
  "errorCode": "NOT_FOUND",
  "message": "Namespace 'nonexistent' not found in tenant 'acme-corp'"
}
```

**Response `403 Forbidden`** — sin permisos:

```json
{
  "errorCode": "FORBIDDEN",
  "message": "Forbidden: memory-admin role required"
}
```

##### Casos de borde

| # | Condición | Comportamiento esperado |
|---|---|---|
| CE-65 | Namespace con 0 recursos | `HTTP 200 OK` con `deleted` mostrando todos los contadores en 0. |
| CE-66 | Namespace con 10,000+ memorias | Operación completada en ≤ 30 segundos. Atómica. |
| CE-67 | Fallo parcial durante la eliminación (ej. Qdrant inaccesible a mitad de operación) | Rollback completo. `HTTP 500 INTERNAL_ERROR`. El namespace queda intacto (sin pérdida parcial de datos). |
| CE-68 | Namespace con `name` que contiene caracteres especiales (`/`, `%`, espacios) | `HTTP 400 VALIDATION_ERROR`: `"Invalid namespace name. Use alphanumeric characters, hyphens and underscores only."`. |
| CE-69 | Dos requests `DELETE` concurrentes para el mismo namespace | La primera retorna `200`. La segunda retorna `404` (namespace ya eliminado). Sin condición de carrera. |
| CE-70 | `DELETE` de namespace mientras hay búsquedas en curso en ese namespace | Las búsquedas en curso se completan con los datos existentes. Nuevas búsquedas post-DELETE retornan 0 resultados. |

##### Compatibilidad

- **Endpoint nuevo**: no existía en v2.0.9. No hay riesgo de romper consumidores existentes.
- **Roles**: el rol `memory-admin` ya existía en v2.0.9. Este endpoint añade un permiso adicional a ese rol.
- **Irreversibilidad**: el endpoint debe documentarse claramente como **destructivo e irreversible**. No hay soft-delete ni papelera de reciclaje.

---

## 3. Matriz de Trazabilidad

### 3.1 Features → Historias de Usuario

| Feature ID | Feature | Historias vinculadas |
|---|---|---|
| FT-001.1 | Pipeline con Reranker Cross-Encoder | HU-V21-001, HU-V21-002 |
| FT-001.2 | Búsqueda Semántica Pura | HU-V21-003 |
| FT-001.3 | Expansión de Grafo Top-3 + `entryPoints` | HU-V21-004, HU-V21-005 |
| FT-001.4 | `POST /extract` con OpenAI Real | HU-V21-006 |
| FT-002.1 | Preservación N+1 + Cache de Grafo | HU-V21-007, HU-V21-008 |
| FT-002.2 | Mitigación Cold Start / Lock Qdrant | HU-V21-009 |
| FT-002.3 | Cache de Validación JWT | HU-V21-010 |
| FT-003.1 | Diagnóstico Worker Inactivo | HU-V21-011 |
| FT-003.2 | Unificación Colecciones Qdrant | HU-V21-012 |
| FT-003.3 | `graphEntryStrategy` Configurable | HU-V21-013 |
| FT-004.1 | Header `X-Graph-Strategy` | HU-V21-014 |
| FT-004.2 | Unificación `search`/`hybrid` | HU-V21-015 |
| FT-004.3 | `DELETE /admin/namespaces/{name}` | HU-V21-016 |

### 3.2 Features → Criterios de Éxito

| Feature ID | CE-01 Top-1 ≥ 0.90 | CE-02 p95 ≤ 500ms | CE-03 NDCG ≥ 0.85 | CE-04 Recall ≥ 0.90 | CE-05 1 Col. | CE-06 /extract | CE-07 Search puro | CE-08 DELETE ns | CE-09 X-Graph | CE-10 Unif. |
|---|---|---|---|---|---|---|---|---|---|---|
| FT-001.1 Reranker | **X** | | **X** | **X** | | | | | | |
| FT-001.2 Search puro | | | | | | | **X** | | | |
| FT-001.3 Grafo top-3 | **X** | | | | | | | | | |
| FT-001.4 Fix /extract | | | | | | **X** | | | | |
| FT-002.1 N+1 + Cache | | **X** | | | | | | | | |
| FT-002.2 Cold start Qdrant | | **X** | | | | | | | | |
| FT-002.3 Cache JWT | | **X** | | | | | | | | |
| FT-003.1 Worker | | **X** | | | | | | | | |
| FT-003.2 Unificar Qdrant | | | | | **X** | | | | | |
| FT-003.3 graphEntryStrat | **X** | | | | | | | | **X** | |
| FT-004.1 X-Graph-Strategy | | | | | | | | | **X** | |
| FT-004.2 Unificar search | | | | | | | | | | **X** |
| FT-004.3 DELETE ns | | | | | | | | **X** | | |

**Cobertura**: 10 de 10 criterios de éxito. CE-02 es atacado por 4 features.

---

## 4. Reglas de Negocio Consolidadas

Las siguientes reglas de negocio son nuevas en v2.1.0 o modifican reglas existentes de v2.0.9.

| ID | Regla | Condición | Acción | Excepciones |
|---|---|---|---|---|
| **BR-V21-001** | Pipeline two-stage | `rerank: true` (o default) y cross-encoder disponible | Dense retrieval top-20 → cross-encoder → top-5 final | Si cross-encoder no disponible: degradar a dense retrieval puro. |
| **BR-V21-002** | Aislamiento semántico | `expandGraph: false` o ausente | Resultados exclusivamente del pipeline semántico. Cero contribuciones del grafo. | — |
| **BR-V21-003** | Expansión multi-origen | `expandGraph: true` sin `entryPoints` explícitos | Expandir grafo desde los top-K matches del dense retrieval (K según estrategia). | Si dense retrieval retorna < K resultados, expandir desde los N disponibles. |
| **BR-V21-004** | Precedencia de `entryPoints` | `entryPoints` explícitos en el body | Usar solo los IDs proporcionados. Ignorar estrategia automática y headers `X-Graph-*`. | IDs inválidos: excluir silenciosamente con warning. |
| **BR-V21-005** | Extracción con IA real | `POST /memories/extract` | Usar OpenAI `gpt-4o-mini`. Retornar entidades con nombre, tipo y confianza. | Si OpenAI no disponible: HTTP 503. |
| **BR-V21-006** | Cache de grafo | Mismos `entryPointIds` + `depth` + `includeKinds` | Servir subgrafo desde caché. TTL 60s default. Invalidar ante mutaciones del grafo. | — |
| **BR-V21-007** | Cache JWT | Token validado contra Keycloak | Cachear con TTL = `exp - now`. Requests subsecuentes validan contra caché. Invalidar ante evento de revocación. | Si Keycloak inaccesible y token no cacheado: HTTP 503. |
| **BR-V21-008** | Unificación de colecciones | Despliegue post-migración | Exactamente 1 colección Qdrant (`abax-memories`). Eliminar `abax-memories-v1`. | Si v1 contiene datos activos: migrar antes de eliminar. |
| **BR-V21-009** | Estrategia de grafo configurable | Perfil de dominio con `graphEntryStrategy` | Usar estrategia configurada como default para búsquedas en ese dominio. | Header `X-Graph-Strategy` sobrescribe. `entryPoints` explícitos tienen precedencia. |
| **BR-V21-010** | Header `X-Graph-Strategy` | Header presente y válido | Sobrescribir estrategia del perfil para esa request. | `X-Graph-Strategy: none` desactiva el grafo incluso con `expandGraph: true`. |
| **BR-V21-011** | Unificación `search`/`hybrid` | `POST /memories/hybrid` | Internamente delega a `POST /memories/search` con `semanticWeight: 0.5, lexicalWeight: 0.5`. Retornar headers `Deprecation: true` y `Warning`. | — |
| **BR-V21-012** | Eliminación atómica de namespace | `DELETE /admin/namespaces/{name}` | Eliminar memorias, relaciones, entidades y puntos Qdrant. Operación atómica. Requiere `memory-admin`. | Namespace no existe: HTTP 404. Sin permisos: HTTP 403. |
| **BR-V21-013** | Pesos de búsqueda válidos | `semanticWeight` y `lexicalWeight` en body | Al menos uno debe ser > 0. Ambos en rango [0.0, 1.0]. | Si ambos son 0: HTTP 400. |

---

## 5. Compatibilidad con v2.0.9

### 5.1 Cambios de comportamiento

| Aspecto | v2.0.9 | v2.1.0 | Impacto |
|---|---|---|---|
| **Default de expansión de grafo** | `single-best` (1 entry point) | `top-k` con K=3 | Consumidores que usaban `expandGraph` sin especificar estrategia verán más nodos expandidos. Potencialmente mejores resultados. |
| **`search` sin `expandGraph`** | Podía incluir resultados del grafo (ambigüedad) | Exclusivamente semántico | Consumidores que dependían de resultados del grafo sin `expandGraph` explícito pueden ver menos resultados. Corrección intencional de ambigüedad. |
| **`POST /extract`** | MockLlmService (regex) | OpenAI `gpt-4o-mini` | Mejora significativa en calidad de entidades. Tipos más variados. Campo `confidence` nuevo. |
| **`POST /memories/hybrid`** | Endpoint independiente | Redirige a `search` con headers de deprecación | Misma funcionalidad. Headers nuevos en la respuesta. |
| **Colección Qdrant** | `abax-memories-v1` + `abax-memories-v2` | Solo `abax-memories` | Transparente para la API. |

### 5.2 Garantías de no ruptura

- **Endpoints existentes**: `POST /memories/search`, `POST /memories`, `GET /memories/{id}`, `PATCH`, `DELETE`, `POST /memories/{id}/relations`, `POST /memories/{id}/review`, `POST /memories/extract`, `GET /entities/*` — todos mantienen sus paths, métodos y roles requeridos.
- **Parámetros existentes**: `query`, `topK`, `filters`, `expandGraph` (objeto), `content`, `kind`, `scope` — todos mantienen sus nombres, tipos y defaults.
- **Códigos de error**: sin cambios en los códigos existentes. Se añaden nuevos (`503` para `/extract` sin OpenAI, `400` para headers inválidos).
- **Formato de respuesta**: los campos existentes (`memoryId`, `score`, `summary`, `lifecycle`, etc.) se mantienen. Los nuevos campos son aditivos.
- **Suite multi-dominio**: los 100 test cases de v2.0.0 deben pasar con resultados funcionalmente equivalentes o mejores.

---

## 6. Alcance y Exclusiones

### 6.1 Dentro del alcance (v2.1.0)

Las 13 features descritas en este documento, distribuidas en 4 épicas:

| Épica | Features | Prioridad global |
|---|---|---|
| EP-V21-001 — Precisión | FT-001.1, FT-001.2, FT-001.3, FT-001.4 | 1, 2, 3, 6 |
| EP-V21-002 — Velocidad | FT-002.1, FT-002.2, FT-002.3 | 7, 8, 4 |
| EP-V21-003 — Eficiencia | FT-003.1, FT-003.2, FT-003.3 | 9, 5, 10 |
| EP-V21-004 — API/DX | FT-004.1, FT-004.2, FT-004.3 | 11, 12, 13 |

### 6.2 Fuera del alcance (v2.1.0)

| # | Ítem | Justificación |
|---|---|---|
| 1 | Compresión de payload en Qdrant | No validado por el sponsor. Requiere control de cambios. |
| 2 | Rate limiting por API key | No validado por el sponsor. CE-12 fue PARTIAL en v2.0.9 pero está fuera del alcance explícito. |
| 3 | Eliminación completa de `MockLlmService` | Solo se reemplaza en `POST /extract`. Permanece para otros contextos. Diferido a iteración de deuda técnica. |
| 4 | Nuevas funcionalidades de negocio | v2.1.0 es hardening, no feature release. Sin nuevos tipos de memoria, dominios, o capacidades de ingesta. |
| 5 | Cambios de stack tecnológico | Stack inalterado por restricción R-01. |
| 6 | Frontend (UI React) | Sin cambios funcionales. Las nuevas capacidades se exponen solo vía API. |
| 7 | SDKs multi-lenguaje adicionales | SDK Python básico sin cambios. |
| 8 | Internacionalización (i18n) | Mensajes de error en inglés. Consistente con English-Only internals. |
| 9 | Cambios en el modelo de datos | Sin nuevas entidades JPA, tablas, columnas, constraints o migraciones Flyway. |

---

## 7. Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10. Meta v2.1.0: ≥ 0.85 en SciFact.
- **Cross-encoder**: Modelo de reranking que procesa pares (consulta, documento) simultáneamente para calcular relevancia fina. Pieza central de FT-001.1 para cerrar la brecha de precisión.
- **Qdrant**: Base de datos vectorial open-source usada para almacenar embeddings y búsqueda semántica por similitud de coseno. v2.1.0 unifica las dos colecciones de v2.0.9 en una sola.
- **p95**: Percentil 95 — valor de latencia por debajo del cual se completa el 95% de las solicitudes. Meta v2.1.0: ≤ 500ms estable.
- **BFS**: Breadth-First Search — algoritmo de recorrido de grafos por niveles (profundidad), usado para expandir el grafo de conocimiento desde entry points.
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación. Abax-Memory valida JWTs contra Keycloak. FT-002.3 implementa caché para reducir latencia de autenticación.
- **entryPoint**: Nodo semilla desde el cual se inicia la expansión del grafo de conocimiento. En v2.0.9 era solo el mejor match; en v2.1.0 puede ser top-K, threshold-based, o especificado explícitamente por el cliente.
