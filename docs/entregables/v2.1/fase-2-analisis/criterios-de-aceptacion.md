---
fase: 2 — Análisis Funcional
entregable: Criterios de Aceptación
version: v2.1.0
responsable: business-analyst
fecha: 2026-05-05
estado: Completado
fuentes:
  - docs/entregables/v2.1/fase-0-descubrimiento/historias-usuario.md
  - docs/entregables/v2.1/fase-0-descubrimiento/epicas-features.md
  - docs/entregables/v2.1/fase-0-descubrimiento/vision-producto.md
---

# Criterios de Aceptación — Abax-Memory v2.1.0
## Hardening y Optimización del Motor de Memoria Multi-Dominio

---

## Tabla de Contenidos

- [1. Introducción](#1-introducción)
  - [1.1 Propósito en el Gate Fase 2 → Fase 3](#11-propósito-en-el-gate-fase-2--fase-3)
  - [1.2 Convenciones de este documento](#12-convenciones-de-este-documento)
- [2. Criterios de Aceptación por Feature](#2-criterios-de-aceptación-por-feature)
  - [2.1 EP-V21-001 — Precisión del Motor de Búsqueda](#21-ep-v21-001--precisión-del-motor-de-búsqueda)
    - [2.1.1 FT-V21-001.1: Reranker Cross-Encoder en el Pipeline](#211-ft-v21-0011-reranker-cross-encoder-en-el-pipeline)
    - [2.1.2 FT-V21-001.2: Búsqueda Semántica Pura](#212-ft-v21-0012-búsqueda-semántica-pura)
    - [2.1.3 FT-V21-001.3: Expansión de Grafo Robusta](#213-ft-v21-0013-expansión-de-grafo-robusta)
    - [2.1.4 FT-V21-001.4: Extracción de Entidades Funcional](#214-ft-v21-0014-extracción-de-entidades-funcional)
  - [2.2 EP-V21-002 — Velocidad y Latencia](#22-ep-v21-002--velocidad-y-latencia)
    - [2.2.1 FT-V21-002.1: Optimización N+1 del Grafo + Cache de Resultados](#221-ft-v21-0021-optimización-n1-del-grafo--cache-de-resultados)
    - [2.2.2 FT-V21-002.2: Mitigación de Cold Start / Lock en Qdrant](#222-ft-v21-0022-mitigación-de-cold-start--lock-en-qdrant)
    - [2.2.3 FT-V21-002.3: Cache de Validación JWT en Backend](#223-ft-v21-0023-cache-de-validación-jwt-en-backend)
  - [2.3 EP-V21-003 — Eficiencia Operativa](#23-ep-v21-003--eficiencia-operativa)
    - [2.3.1 FT-V21-003.1: Diagnóstico del Worker de Procesamiento](#231-ft-v21-0031-diagnóstico-del-worker-de-procesamiento)
    - [2.3.2 FT-V21-003.2: Unificación de Colecciones Qdrant](#232-ft-v21-0032-unificación-de-colecciones-qdrant)
    - [2.3.3 FT-V21-003.3: graphEntryStrategy Configurable](#233-ft-v21-0033-graphentrystrategy-configurable)
  - [2.4 EP-V21-004 — API y Developer Experience](#24-ep-v21-004--api-y-developer-experience)
    - [2.4.1 FT-V21-004.1: Header X-Graph-Strategy](#241-ft-v21-0041-header-x-graph-strategy)
    - [2.4.2 FT-V21-004.2: Unificación de Endpoints search y hybrid](#242-ft-v21-0042-unificación-de-endpoints-search-y-hybrid)
    - [2.4.3 FT-V21-004.3: DELETE /admin/namespaces/{name}](#243-ft-v21-0043-delete-adminnamespacesname)
- [3. Criterios de Aceptación Transversales](#3-criterios-de-aceptación-transversales)
  - [3.1 Backward Compatibility con v2.0.9](#31-backward-compatibility-con-v209)
  - [3.2 Manejo de Errores y Códigos HTTP](#32-manejo-de-errores-y-códigos-http)
  - [3.3 Idempotencia](#33-idempotencia)
- [4. Matriz de Cobertura](#4-matriz-de-cobertura)
- [5. Criterios de NO Aceptación](#5-criterios-de-no-aceptación)
- [6. Definición de Done](#6-definición-de-done)
- [7. Glosario](#7-glosario)

---

## 1. Introducción

### 1.1 Propósito en el Gate Fase 2 → Fase 3

Este documento es el artefacto de cierre de la **Fase 2 — Análisis Funcional** para Abax-Memory v2.1.0. Su propósito es:

1. **Consolidar** los criterios de aceptación dispersos en las 16 historias de usuario del descubrimiento en un formato unificado, refinado y verificable.
2. **Servir como contrato de calidad** entre el negocio (Product Owner) y el equipo técnico (Solution Architect, Tech Lead, QA Lead). Cada criterio aquí documentado debe ser verificable antes de declarar la feature como completada.
3. **Habilitar el gate Fase 2 → Fase 3** (Diseño Técnico): ningún diseño técnico debe iniciarse sin que los criterios de aceptación estén formalizados y aprobados.
4. **Proveer a QA** la base para diseñar casos de prueba en la Fase 5 (Pruebas Funcionales) y los escenarios UAT en la Fase 6.

> **Principio rector**: todo criterio de aceptación en este documento está trazado a una historia de usuario (`HU-V21-XXX`), a los Criterios de Éxito de la Visión del Producto (`CE-XX`), y es medible con datos concretos — no opiniones.

### 1.2 Convenciones de este documento

- **Formato**: `Given / When / Then` para cada criterio, con datos concretos en los ejemplos.
- **Identificador de criterio**: `CA-V21-<feature>.<n>` donde `<feature>` es el número de feature (ej. `001.1`) y `<n>` es el número secuencial del criterio.
- **Prioridad del criterio**: `Must` (bloqueante para el éxito de la feature), `Should` (importante pero no bloqueante), `Could` (deseable si hay margen). La prioridad del criterio hereda la prioridad MoSCoW de la historia de usuario de origen.
- **Escenarios**: se incluyen escenarios positivos (happy path), negativos (error handling) y de borde (edge cases) donde aplica.

---

## 2. Criterios de Aceptación por Feature

### 2.1 EP-V21-001 — Precisión del Motor de Búsqueda

#### 2.1.1 FT-V21-001.1: Reranker Cross-Encoder en el Pipeline

**Feature**: Agregar etapa de re-ranking con modelo cross-encoder sobre el top-20 de candidatos del dense retrieval para producir un top-5 final con precisión superior.
**Historias de origen**: HU-V21-001, HU-V21-002
**Criterios de Éxito vinculados**: CE-01, CE-03, CE-04

---

**CA-V21-001.1.1**: Precisión top-1 en suite multi-dominio con reranker

- **Given** el pipeline two-stage configurado con cross-encoder activo (`gpt-4o-mini` o modelo local `allenai/scifact`) y un índice Qdrant con ≥ 5,000 documentos del dominio biomédico
- **When** se ejecutan 100 queries de la suite multi-dominio con ground truth conocido vía `POST /memories/search` con `{"expandGraph": false}`
- **Then** la proporción de queries cuyo top-1 es el documento ground-truth esperado es **≥ 0.90**, superando la línea base de v2.0.9 (~0.75–0.80 dense-only) por al menos +0.10 puntos
- **Prioridad**: Must

---

**CA-V21-001.1.2**: NDCG@10 en benchmark SciFact completo

- **Given** el pipeline two-stage completamente operativo con cross-encoder y el dataset SciFact cargado (5,183 documentos, 300 queries)
- **When** se ejecuta el benchmark SciFact completo y se calcula NDCG@10 sobre los resultados reordenados por el cross-encoder
- **Then** el NDCG@10 obtenido es **≥ 0.85**, superando la línea base de v2.0.9 (0.7771) por al menos +0.07 puntos
- **Prioridad**: Must

---

**CA-V21-001.1.3**: Recall@10 mantenido tras reranking

- **Given** el mismo benchmark SciFact que en CA-V21-001.1.2
- **When** se calcula Recall@10 sobre los resultados post-reranker
- **Then** el Recall@10 es **≥ 0.90**, sin degradación respecto a la línea base de v2.0.9 (0.9006), demostrando que el cross-encoder reordena sin descartar documentos relevantes
- **Prioridad**: Must

---

**CA-V21-001.1.4**: Verificación de corrección activa de ranking

- **Given** el pipeline con cross-encoder activo y una muestra de 50 queries con ground truth conocido
- **When** se comparan los scores de ranking antes (dense retrieval) y después (cross-encoder) del reranker
- **Then** el documento ground-truth asciende en el ranking en **al menos el 80%** de los casos donde no estaba en top-1 en el dense retrieval, demostrando que el cross-encoder corrige activamente errores de ranking
- **Prioridad**: Must

---

**CA-V21-001.1.5**: Latencia adicional del reranker dentro de presupuesto

- **Given** un presupuesto de latencia total de 500ms para `POST /memories/search` sin `expandGraph`
- **When** el cross-encoder procesa un top-20 de candidatos (máximo 20 pares query-documento)
- **Then** la etapa de re-ranking **no añade más de 200ms** de latencia incremental respecto al dense retrieval puro, manteniendo la latencia total del endpoint en **p95 ≤ 500ms**
- **Prioridad**: Must

---

**CA-V21-001.1.6**: Degradación graceful cuando el cross-encoder no está disponible

- **Given** que el cross-encoder no está disponible (API key de OpenAI ausente, vencida, o modelo local no desplegado)
- **When** se ejecuta `POST /memories/search`
- **Then** el pipeline **degrada gracefulmente** a dense retrieval sin cross-encoder, registrando un warning `CROSS_ENCODER_UNAVAILABLE` en logs, retornando resultados con la precisión de v2.0.9, y **sin** retornar error 500 ni bloquear la búsqueda
- **Prioridad**: Must

---

#### 2.1.2 FT-V21-001.2: Búsqueda Semántica Pura

**Feature**: Recalibrar `POST /memories/search` para que sin `expandGraph` use exclusivamente el pipeline semántico (dense retrieval + cross-encoder), sin contribuciones del grafo de conocimiento.
**Historia de origen**: HU-V21-003
**Criterio de Éxito vinculado**: CE-07

---

**CA-V21-001.2.1**: `expandGraph: false` — resultados exclusivamente semánticos

- **Given** un tenant con 50 memorias y un grafo de conocimiento con 20 relaciones, donde el grafo no tiene entidades relacionadas con la query
- **When** se ejecuta `POST /memories/search` con `{"query": "database connection timeout", "expandGraph": false}`
- **Then** **todos** los resultados retornados provienen exclusivamente del pipeline semántico (dense retrieval + cross-encoder), verificable porque ningún resultado incluye nodos vecinos expandidos desde el grafo (campo `graphExpandedNodes` vacío o `graphExpanded: false` en la respuesta)
- **Prioridad**: Must

---

**CA-V21-001.2.2**: `expandGraph: true` — activación del grafo controlada por parámetro

- **Given** el mismo tenant del criterio anterior
- **When** se ejecuta la misma query pero con `"expandGraph": true`
- **Then** la respuesta **incluye** resultados expandidos desde el grafo (campo `graphExpandedNodes` no vacío), demostrando que el parámetro `expandGraph` controla efectivamente la activación del grafo
- **Prioridad**: Must

---

**CA-V21-001.2.3**: Precisión top-1 en dominio no-relacional sin grafo

- **Given** un dataset de 50 queries donde el ground truth no tiene relaciones de grafo relevantes (dominio de conocimiento factual no relacional)
- **When** se ejecutan las 50 queries con `expandGraph: false`
- **Then** la proporción top-1 es **≥ 0.90** y **ningún resultado** del top-5 tiene origen en el grafo (verificable por trazabilidad de `source` en cada resultado)
- **Prioridad**: Must

---

#### 2.1.3 FT-V21-001.3: Expansión de Grafo Robusta

**Feature**: Expandir el grafo de conocimiento desde los 3 nodos más relevantes del dense retrieval (en lugar del mejor match único), con soporte para `entryPoints` explícitos especificados por el cliente.
**Historias de origen**: HU-V21-004, HU-V21-005
**Criterios de Éxito vinculados**: CE-01

---

**CA-V21-001.3.1**: Expansión desde top-3 entry points

- **Given** un tenant con 100 memorias interrelacionadas en un dominio multi-tópico (ej. incidencias de infraestructura con relaciones causales entre servidores, servicios y errores) donde la búsqueda semántica retorna ≥ 3 nodos en el top-10 del dense retrieval
- **When** se ejecuta `POST /memories/search` con `{"query": "cascading failure root cause", "expandGraph": true, "graphDepth": 2}`
- **Then** la respuesta incluye nodos expandidos desde exactamente 3 entry points distintos, verificable porque el campo `graphExpandedNodes` contiene al menos 3 `entryPointIds` distintos y el total de nodos expandidos es mayor que si se expandiera desde un único entry point
- **Prioridad**: Must

---

**CA-V21-001.3.2**: Recall multi-dominio con expansión top-3

- **Given** el dataset multi-dominio ABM-MULTI-01 de v2.0.0 (50 queries con ground truth que requiere cruzar múltiples dominios vía relaciones de grafo)
- **When** se calcula el recall con el pipeline de v2.1.0 (expansión top-3)
- **Then** el recall es **≥ 85%**, superando la línea base de v2.0.9 (69.4%, single entry) en al menos +15.6 puntos
- **Prioridad**: Must

---

**CA-V21-001.3.3**: Degradación graceful con menos de 3 resultados del dense retrieval

- **Given** que la búsqueda semántica retorna menos de 3 resultados (score mínimo insuficiente o índice con pocos documentos)
- **When** se ejecuta `POST /memories/search` con `expandGraph: true`
- **Then** el grafo expande únicamente desde los **N nodos disponibles** (1 o 2) sin error, y el campo `graphExpandedNodes` refleja correctamente el número real de entry points usados
- **Prioridad**: Must

---

**CA-V21-001.3.4**: Respeto del parámetro `graphDepth`

- **Given** un `graphDepth` configurado en 3 y el grafo expandiéndose desde los top-3 entry points
- **When** se completa la expansión BFS
- **Then** **ningún nodo** en el resultado excede la profundidad 3 desde su entry point origen, verificable porque la distancia máxima (`maxDistance` en la metadata de respuesta) es ≤ 3
- **Prioridad**: Must

---

**CA-V21-001.3.5**: Entry points explícitos vía parámetro `entryPoints`

- **Given** un tenant con un grafo de conocimiento que contiene relaciones entre las memorias `mem-001`, `mem-002` y `mem-003`
- **When** se ejecuta `POST /memories/search` con `{"expandGraph": true, "entryPoints": ["mem-001", "mem-002"], "graphDepth": 1}`
- **Then** la respuesta incluye todos los nodos vecinos de `mem-001` y `mem-002` a profundidad 1, verificable porque el campo `graphExpandedNodes` contiene ambos `entryPointIds` explícitos y sus respectivos vecinos
- **Prioridad**: Must

---

**CA-V21-001.3.6**: Manejo de entry point inexistente

- **Given** que se especifica `entryPoints` con un ID que no existe en el grafo (`"mem-999"`)
- **When** se ejecuta la búsqueda
- **Then** el sistema registra un warning `ENTRY_POINT_NOT_FOUND` en logs para ese ID, lo excluye silenciosamente de la expansión sin fallar la request, y expande desde los entry points restantes válidos
- **Prioridad**: Must

---

**CA-V21-001.3.7**: Precedencia de `entryPoints` explícitos sobre estrategia automática

- **Given** que se especifican simultáneamente `entryPoints` explícitos y una estrategia `top-k` vía header `X-Graph-Strategy` (o configuración de perfil de dominio)
- **When** se ejecuta la búsqueda
- **Then** los `entryPoints` explícitos tienen **precedencia** sobre cualquier selección automática y el sistema registra un log info indicando que se usaron entry points proporcionados por el cliente
- **Prioridad**: Must

---

#### 2.1.4 FT-V21-001.4: Extracción de Entidades Funcional

**Feature**: Reemplazar `MockLlmService` (regex) por llamadas reales a OpenAI `gpt-4o-mini` en `POST /memories/extract`.
**Historia de origen**: HU-V21-006
**Criterio de Éxito vinculado**: CE-06

---

**CA-V21-001.4.1**: Extracción semántica con OpenAI real

- **Given** el endpoint `POST /memories/extract` configurado con OpenAI `gpt-4o-mini` y una API key válida
- **When** se envía `{"text": "The server nginx-prod-01 crashed due to OOM at 14:32 UTC. Service api-gateway was affected."}`
- **Then** la respuesta incluye al menos las entidades `nginx-prod-01` (tipo `SERVER`), `OOM` (tipo `ERROR_CONDITION`), `14:32 UTC` (tipo `TIMESTAMP`), y `api-gateway` (tipo `SERVICE`), cada una con un campo `confidence` > 0.0
- **Prioridad**: Should

---

**CA-V21-001.4.2**: Texto vacío — HTTP 400

- **Given** el endpoint `POST /memories/extract`
- **When** se envía `{"text": ""}` (texto vacío)
- **Then** el sistema retorna **HTTP 400** con mensaje `"text must not be blank"` y **no se realiza ninguna llamada a OpenAI**
- **Prioridad**: Should

---

**CA-V21-001.4.3**: API key no configurada o vencida — HTTP 503

- **Given** que la API key de OpenAI no está configurada o está vencida
- **When** se ejecuta `POST /memories/extract`
- **Then** el sistema retorna **HTTP 503** con mensaje `"Entity extraction unavailable: LLM service not configured"`, registra el error en logs con nivel `ERROR`, y **no expone** la API key ni detalles internos en la respuesta
- **Prioridad**: Should

---

**CA-V21-001.4.4**: Latencia de extracción dentro de presupuesto

- **Given** un texto de entrada de 5,000 caracteres (límite máximo razonable para extracción)
- **When** se ejecuta `POST /memories/extract`
- **Then** la respuesta se recibe en **≤ 3 segundos** (latencia p95), y el sistema **no trunca** el texto ni rechaza la request por longitud
- **Prioridad**: Should

---

### 2.2 EP-V21-002 — Velocidad y Latencia

#### 2.2.1 FT-V21-002.1: Optimización N+1 del Grafo + Cache de Resultados

**Feature**: Preservar la optimización N+1 de queries de grafo (batch fetching) y agregar caché en memoria (Caffeine) para resultados de expansión de grafo.
**Historias de origen**: HU-V21-007, HU-V21-008
**Criterio de Éxito vinculado**: CE-02

---

**CA-V21-002.1.1**: Batch fetching preservado con expansión top-3

- **Given** un grafo de conocimiento con 50 nodos interrelacionados donde la expansión top-3 desde 3 entry points implicaría potencialmente `3 × N` queries individuales sin optimización
- **When** se ejecuta `POST /memories/search` con `expandGraph: true` y `graphDepth: 2`
- **Then** el número de queries SQL ejecutadas para recuperar relaciones del grafo es **≤ 3** (una consulta batch por nivel de profundidad, o una consulta batch total), verificable mediante el log de queries SQL en nivel DEBUG
- **Prioridad**: Could

---

**CA-V21-002.1.2**: Sin regresión de latencia respecto a v2.0.9

- **Given** el pipeline de v2.1.0 con expansión de grafo y el pipeline de v2.0.9, ambos configurados con single entry point para comparación justa
- **When** se compara la latencia de la expansión de grafo entre ambas versiones para la misma query y mismo grafo
- **Then** la latencia en v2.1.0 **no excede** la de v2.0.9 en más del 10%, demostrando que no hubo regresión por los cambios de v2.1.0
- **Prioridad**: Could

---

**CA-V21-002.1.3**: Cobertura de tests unitarios para batch fetching

- **Given** el código de batch fetching documentado en el módulo de grafo
- **When** se ejecuta la suite de tests unitarios
- **Then** existen **al menos 2 tests específicos** que verifican el batch fetching (no queries individuales), y **ambos pasan** (estado verde)
- **Prioridad**: Could

---

**CA-V21-002.1.4**: Cache hit en queries repetidas de grafo

- **Given** un caché de grafo configurado con TTL de 60 segundos y capacidad máxima de 1,000 entradas
- **When** se ejecutan 3 queries consecutivas idénticas (misma query, mismos entry points, mismo `graphDepth`)
- **Then** la primera query ejecuta el BFS completo (cache miss) y las queries 2 y 3 sirven el resultado desde caché (cache hit), verificable porque la latencia de las queries 2 y 3 es **al menos 50% menor** que la query 1 y los logs muestran `GRAPH_CACHE_HIT`
- **Prioridad**: Could

---

**CA-V21-002.1.5**: Invalidación de caché por modificación del grafo

- **Given** que se añade una nueva relación al grafo entre la query 1 y la query 2 que conecta uno de los entry points cacheados
- **When** se ejecuta la query 2
- **Then** el sistema **detecta la modificación** del grafo (vía evento de invalidación o timestamp de última modificación), **invalida** la entrada del caché para ese entry point, y ejecuta un BFS fresco (cache miss), sirviendo resultados que incluyen la nueva relación
- **Prioridad**: Could

---

**CA-V21-002.1.6**: Política de evicción LRU y límite de capacidad

- **Given** un caché con 900 entradas (cerca de su capacidad máxima de 1,000)
- **When** una nueva entrada se añade (total = 901)
- **Then** la política de evicción (LRU) elimina la entrada menos recientemente usada sin error, y el caché **nunca excede** las 1,000 entradas, verificable mediante métricas expuestas (`cache.size ≤ maxSize`)
- **Prioridad**: Could

---

#### 2.2.2 FT-V21-002.2: Mitigación de Cold Start / Lock en Qdrant

**Feature**: Diagnosticar causas raíz de spikes de latencia (~2s) en Qdrant y aplicar mitigaciones de configuración.
**Historia de origen**: HU-V21-009
**Criterio de Éxito vinculado**: CE-02

---

**CA-V21-002.2.1**: Latencia p95 estable en tres condiciones de operación

- **Given** un clúster Qdrant en condiciones de producción (≥ 10,000 puntos vectoriales, 3 tenants activos)
- **When** se ejecuta una prueba de carga de 300 queries `POST /memories/search` con `expandGraph: false` en 3 momentos distintos: (a) cold start tras despliegue, (b) steady state tras 5 minutos de operación, y (c) bajo carga de 10 escrituras concurrentes
- **Then** la latencia p95 en **los 3 momentos** es **≤ 500ms**, verificable mediante el reporte de percentiles de la herramienta de carga
- **Prioridad**: Could

---

**CA-V21-002.2.2**: Documento de diagnóstico con causa raíz y mitigación

- **Given** el diagnóstico de latencia completado
- **When** se revisa la documentación de hallazgos
- **Then** existe un documento de diagnóstico (ADR o runbook de operaciones) que identifica **al menos una causa raíz específica** de los spikes de latencia (ej. `optimizers_config.default_segment_number` subconfigurado, `flush_interval_sec` inadecuado, o falta de `read_consistency`), la mitigación aplicada, y el impacto medido (antes vs después), sin "TODO" sin asignar
- **Prioridad**: Could

---

**CA-V21-002.2.3**: Cold start mitigado tras reinicio

- **Given** que las mitigaciones están aplicadas
- **When** se reinicia el servicio Qdrant (simulando un cold start real)
- **Then** el pre-calentamiento de segmentos (o la mitigación equivalente) completa en **≤ 30 segundos**, la primera query post-reinicio tiene latencia **≤ 1 segundo**, y la latencia converge a **≤ 500ms** en las siguientes 10 queries
- **Prioridad**: Could

---

#### 2.2.3 FT-V21-002.3: Cache de Validación JWT en Backend

**Feature**: Cachear validación de tokens JWT en memoria con TTL igual al `exp` del token, invalidando ante eventos de revocación de Keycloak.
**Historia de origen**: HU-V21-010
**Criterio de Éxito vinculado**: CE-02

---

**CA-V21-002.3.1**: Cache hit en requests repetidas del mismo cliente

- **Given** un cliente autenticado con un JWT válido (expira en 1 hora)
- **When** ejecuta 100 requests consecutivas a `POST /memories/search`
- **Then** la primera request valida el JWT contra Keycloak (cache miss) y las 99 requests subsecuentes validan contra la caché local (cache hit), verificable porque los logs muestran exactamente **1 llamada a Keycloak** y 99 `JWT_CACHE_HIT`, y la latencia de autenticación en las requests 2-100 es **≤ 5ms**
- **Prioridad**: Should

---

**CA-V21-002.3.2**: Expiración de JWT cacheado por TTL

- **Given** un JWT en caché cuyo `exp` ha vencido
- **When** el cliente ejecuta una request con ese token
- **Then** el sistema detecta la expiración vía TTL de la caché, descarta la entrada cacheada (evicción por TTL), intenta validar contra Keycloak, y — si Keycloak rechaza el token expirado — retorna **HTTP 401**
- **Prioridad**: Should

---

**CA-V21-002.3.3**: Invalidación por revocación de token en Keycloak

- **Given** que un token es revocado en Keycloak (logout del usuario o cambio de roles)
- **When** el sistema recibe la notificación de revocación (Keycloak Admin Event)
- **Then** la entrada correspondiente en la caché JWT se invalida en **≤ 5 segundos** desde la recepción del evento, y la siguiente request con ese token es rechazada (**HTTP 401**) tras validación contra Keycloak
- **Prioridad**: Should

---

**CA-V21-002.3.4**: Métrica de hit ratio del caché JWT

- **Given** que la caché JWT está activa
- **When** se inspeccionan las métricas del sistema
- **Then** existe una métrica `jwt_cache_hit_ratio` que refleja la proporción de hits vs misses, y el ratio es **≥ 0.90** en condiciones normales de operación (requests repetidas del mismo cliente)
- **Prioridad**: Should

---

### 2.3 EP-V21-003 — Eficiencia Operativa

#### 2.3.1 FT-V21-003.1: Diagnóstico del Worker de Procesamiento

**Feature**: Diagnosticar la causa raíz del worker de procesamiento asíncrono con `Claimed = 0`, y decidir entre eliminar (si el procesamiento síncrono es suficiente) o reparar.
**Historia de origen**: HU-V21-011
**Criterio de Éxito vinculado**: CE-02 (indirecto)

---

**CA-V21-003.1.1**: Documento de diagnóstico con causa raíz y acción concreta

- **Given** el worker de procesamiento en su estado actual (`Claimed = 0`)
- **When** se ejecuta el diagnóstico
- **Then** existe un documento que identifica explícitamente **la causa raíz** (conexión a cola rota, configuración de polling incorrecta, o worker innecesario porque `POST /memories` ya procesa sincrónicamente embeddings) y propone una **acción concreta**: eliminar o reparar
- **Prioridad**: Could

---

**CA-V21-003.1.2**: Escenario A — Eliminación del worker (si procesamiento síncrono es suficiente)

- **Given** que la acción decidida es "eliminar el worker"
- **When** se verifica el despliegue post-eliminación
- **Then** (a) no existe ningún proceso worker en ejecución, (b) `POST /memories` sigue generando embeddings y entidades correctamente (verificable con un test de ingesta de 10 memorias), y (c) la latencia p95 de `POST /memories` **no excede** la de v2.0.9 en más del 10%
- **Prioridad**: Could

---

**CA-V21-003.1.3**: Escenario B — Reparación del worker (si procesamiento asíncrono es necesario)

- **Given** que la acción decidida es "reparar el worker"
- **When** se ejecuta una ingesta de 50 memorias
- **Then** el worker procesa **al menos 45** de ellas en ≤ 30 segundos post-ingesta (`Claimed ≥ 45`), y los embeddings generados son correctos (verificable porque las 50 memorias son encontrables vía búsqueda semántica en ≤ 5 segundos tras la ingesta)
- **Prioridad**: Could

---

#### 2.3.2 FT-V21-003.2: Unificación de Colecciones Qdrant

**Feature**: Consolidar las dos colecciones Qdrant en una sola (`abax-memories`), eliminando la colección v1 obsoleta.
**Historia de origen**: HU-V21-012
**Criterio de Éxito vinculado**: CE-05

---

**CA-V21-003.2.1**: Verificación pre-migración — colección v1 sin referencias activas

- **Given** el clúster Qdrant con las colecciones `abax-memories-v1` y `abax-memories-v2`
- **When** se ejecuta la verificación pre-migración
- **Then** se confirma que la colección `abax-memories-v1` **no contiene puntos** que estén referenciados activamente desde la base de datos PostgreSQL (ningún `memory_id` en la tabla `memories` apunta a un punto exclusivo de v1), documentando el resultado en un checklist de verificación
- **Prioridad**: Should

---

**CA-V21-003.2.2**: Unificación completa con verificación post-migración

- **Given** que la verificación pre-migración confirma que `abax-memories-v1` es segura de eliminar
- **When** se ejecuta el procedimiento de unificación
- **Then** al finalizar: (a) `GET /collections` en Qdrant muestra exactamente **1 colección** llamada `abax-memories`, (b) `GET /collections/abax-memories-v1` retorna **404**, (c) **todas** las queries de búsqueda semántica contra la colección unificada retornan resultados correctos (verificable con 50 queries de la suite multi-dominio, 100% de resultados esperados), y (d) **ninguna** operación de la API v2 falla por referencia a la colección eliminada
- **Prioridad**: Should

---

**CA-V21-003.2.3**: Ingesta post-unificación

- **Given** que la unificación está completada
- **When** se ejecuta una ingesta de 10 nuevas memorias
- **Then** los puntos vectoriales se almacenan correctamente en `abax-memories`, son encontrables vía búsqueda semántica en **≤ 2 segundos**, y el conteo de puntos en la colección se incrementa exactamente en 10
- **Prioridad**: Should

---

#### 2.3.3 FT-V21-003.3: graphEntryStrategy Configurable

**Feature**: Parametrizar la estrategia de entrada al grafo (`single-best`, `top-k`, `threshold`) a nivel de perfil de dominio.
**Historia de origen**: HU-V21-013
**Criterios de Éxito vinculados**: CE-01, CE-09

---

**CA-V21-003.3.1**: Estrategia `top-k` con k configurable

- **Given** un perfil de dominio "infraestructura" con `graphEntryStrategy: top-k` y `graphK: 3`
- **When** un consumidor ejecuta `POST /memories/search` con `expandGraph: true` en ese dominio (sin especificar estrategia vía header `X-Graph-Strategy`)
- **Then** la expansión de grafo se realiza desde exactamente **3 entry points** (los 3 mejores matches del dense retrieval), verificable porque el campo `graphExpandedNodes.entryPointCount` en la respuesta es 3
- **Prioridad**: Could

---

**CA-V21-003.3.2**: Estrategia `threshold` con `minScore` configurable

- **Given** un perfil de dominio "legal" con `graphEntryStrategy: threshold` y `graphThreshold: 0.85`
- **When** un consumidor ejecuta `POST /memories/search` con `expandGraph: true` y el dense retrieval retorna 10 candidatos con scores `[0.92, 0.88, 0.84, 0.81, ...]`
- **Then** la expansión de grafo se realiza únicamente desde los **2 entry points** con score ≥ 0.85, verificable porque `graphExpandedNodes.entryPointCount` es 2
- **Prioridad**: Could

---

**CA-V21-003.3.3**: Cambio de estrategia en caliente sin reinicio

- **Given** que un administrador cambia `graphEntryStrategy` de `single-best` a `top-k` con `graphK: 5` en un perfil de dominio activo
- **When** se ejecutan 10 queries de búsqueda con `expandGraph: true` en ese dominio (sin calentamiento previo)
- **Then** las 10 queries usan la nueva estrategia `top-k` con 5 entry points, verificable porque `entryPointCount` es consistentemente 5 en las 10 respuestas (siempre que el dense retrieval retorne ≥ 5 resultados), **sin necesidad de reiniciar el servicio**
- **Prioridad**: Could

---

### 2.4 EP-V21-004 — API y Developer Experience

#### 2.4.1 FT-V21-004.1: Header X-Graph-Strategy

**Feature**: Header HTTP `X-Graph-Strategy` para control granular de la expansión de grafo por request, con valores `none`, `single`, `top-k`, `threshold`.
**Historia de origen**: HU-V21-014
**Criterio de Éxito vinculado**: CE-09

---

**CA-V21-004.1.1**: `X-Graph-Strategy: none` — sin expansión de grafo

- **Given** un tenant con un grafo de conocimiento poblado
- **When** se ejecuta `POST /memories/search` con header `X-Graph-Strategy: none` (sin importar el valor de `expandGraph`) y sin especificar `entryPoints`
- **Then** la respuesta **no incluye ningún nodo expandido** desde el grafo (`graphExpandedNodes` vacío o `graphExpanded: false`), independientemente de la estrategia configurada en el perfil de dominio
- **Prioridad**: Could

---

**CA-V21-004.1.2**: `X-Graph-Strategy: top-k` con `X-Graph-K: 5`

- **Given** el mismo tenant
- **When** se ejecuta `POST /memories/search` con headers `X-Graph-Strategy: top-k` y `X-Graph-K: 5`
- **Then** la expansión de grafo se realiza desde exactamente **5 entry points** (los 5 mejores matches del dense retrieval), verificable porque `graphExpandedNodes.entryPointCount` es 5, y este comportamiento **sobrescribe** cualquier configuración del perfil de dominio para esta request
- **Prioridad**: Could

---

**CA-V21-004.1.3**: Valor inválido — HTTP 400

- **Given** que se envía un header con valor inválido (`X-Graph-Strategy: invalid-strategy`)
- **When** se ejecuta `POST /memories/search`
- **Then** el sistema retorna **HTTP 400** con mensaje `"Invalid X-Graph-Strategy: 'invalid-strategy'. Supported values: none, single, top-k, threshold"` y **no ejecuta la búsqueda**
- **Prioridad**: Could

---

**CA-V21-004.1.4**: `top-k` sin `X-Graph-K` — uso de default

- **Given** que se envía `X-Graph-Strategy: top-k` sin el header complementario `X-Graph-K`
- **When** se ejecuta la búsqueda
- **Then** el sistema usa el valor default `k=3` (configurado en el perfil de dominio) y registra un log info indicando que `X-Graph-K` no fue provisto y se usó el default
- **Prioridad**: Could

---

#### 2.4.2 FT-V21-004.2: Unificación de Endpoints search y hybrid

**Feature**: Fusionar `search`/`hybrid` en un solo endpoint `POST /memories/search` con parámetros explícitos, deprecando `POST /memories/hybrid` sin breaking change.
**Historia de origen**: HU-V21-015
**Criterio de Éxito vinculado**: CE-10

---

**CA-V21-004.2.1**: Búsqueda híbrida con pesos configurables

- **Given** el endpoint unificado `POST /memories/search`
- **When** se ejecuta una búsqueda híbrida con `{"query": "nginx timeout error", "semanticWeight": 0.6, "lexicalWeight": 0.4, "expandGraph": false}`
- **Then** los resultados combinan similitud semántica (60% peso) y matching léxico (40% peso), y el score final de cada resultado refleja la combinación proporcional de ambas fuentes, verificable porque el orden de resultados difiere del obtenido con `semanticWeight: 1.0, lexicalWeight: 0.0`
- **Prioridad**: Could

---

**CA-V21-004.2.2**: Deprecación de `POST /memories/hybrid` con backward compatibility

- **Given** el endpoint legacy `POST /memories/hybrid`
- **When** se ejecuta una request con parámetros equivalentes a los de v2.0.9
- **Then** la respuesta incluye el header **`Deprecation: true`** y el header **`Warning: 299 - "POST /memories/hybrid is deprecated. Use POST /memories/search with semanticWeight and lexicalWeight parameters."`**, pero la funcionalidad de búsqueda es **idéntica a v2.0.9** (backward compatibility)
- **Prioridad**: Could

---

**CA-V21-004.2.3**: Equivalencia funcional con v2.0.9

- **Given** el endpoint unificado `POST /memories/search`
- **When** se ejecuta la suite de 100 test cases multi-dominio usando exclusivamente este endpoint con distintas combinaciones de parámetros (`semanticWeight=1.0` para semántico puro, `semanticWeight=0.5, lexicalWeight=0.5` para híbrido balanceado, `expandGraph=true` para búsqueda con grafo)
- **Then** los resultados son **funcionalmente equivalentes** a los obtenidos en v2.0.9 con los endpoints separados para los mismos casos de uso (misma precisión top-1, mismo recall), verificable comparando contra los resultados benchmark de v2.0.0
- **Prioridad**: Could

---

**CA-V21-004.2.4**: Parámetros contradictorios — HTTP 400

- **Given** que un consumidor envía `"semanticWeight": 0.0, "lexicalWeight": 0.0`
- **When** se ejecuta `POST /memories/search`
- **Then** el sistema retorna **HTTP 400** con mensaje `"At least one of semanticWeight or lexicalWeight must be > 0"`, **sin ejecutar la búsqueda**
- **Prioridad**: Could

---

#### 2.4.3 FT-V21-004.3: DELETE /admin/namespaces/{name}

**Feature**: Nuevo endpoint administrativo para eliminar atómicamente todos los recursos de un namespace (memorias, relaciones, entidades, puntos Qdrant).
**Historia de origen**: HU-V21-016
**Criterio de Éxito vinculado**: CE-08

---

**CA-V21-004.3.1**: Eliminación completa de namespace con resumen

- **Given** un namespace `benchmark-sifact` con 50 memorias, 20 relaciones, 15 entidades, y 50 puntos vectoriales en Qdrant
- **When** se ejecuta `DELETE /admin/namespaces/benchmark-sifact` con autenticación de rol `memory-admin`
- **Then** el sistema retorna **HTTP 200** con un body que resume los recursos eliminados (`{"deleted": {"memories": 50, "relations": 20, "entities": 15, "qdrantPoints": 50}}`), y una verificación posterior confirma: (a) `GET /admin/namespaces/benchmark-sifact` retorna **404**, (b) `POST /memories/search` dentro del namespace retorna **0 resultados**, (c) los 50 puntos fueron eliminados de Qdrant
- **Prioridad**: Could

---

**CA-V21-004.3.2**: Atomicidad de la operación para namespaces grandes

- **Given** un namespace `prod-critical` con 10,000+ memorias
- **When** se ejecuta `DELETE /admin/namespaces/prod-critical`
- **Then** la operación completa en **≤ 30 segundos** y es **atómica**: o bien se eliminan todos los recursos, o bien (en caso de fallo parcial) el namespace permanece intacto y el sistema retorna **HTTP 500** con mensaje describiendo el fallo, sin dejar el namespace en estado inconsistente (sin memorias pero con puntos Qdrant residuales)
- **Prioridad**: Could

---

**CA-V21-004.3.3**: Control de acceso — solo `memory-admin`

- **Given** un usuario con rol `memory-operator` (sin rol `memory-admin`)
- **When** intenta ejecutar `DELETE /admin/namespaces/test-ns`
- **Then** el sistema retorna **HTTP 403** con mensaje `"Forbidden: memory-admin role required"` y **no se elimina ningún recurso**
- **Prioridad**: Could

---

**CA-V21-004.3.4**: Namespace inexistente — HTTP 404

- **Given** que el namespace especificado no existe (`DELETE /admin/namespaces/nonexistent`)
- **When** se ejecuta la request con rol `memory-admin`
- **Then** el sistema retorna **HTTP 404** con mensaje `"Namespace 'nonexistent' not found"`
- **Prioridad**: Could

---

## 3. Criterios de Aceptación Transversales

Los siguientes criterios no pertenecen a una feature específica sino que aplican transversalmente a todas las features de v2.1.0. Deben verificarse como parte del Definition of Done de cada feature y como gate global de la release.

### 3.1 Backward Compatibility con v2.0.9

| ID | Criterio | Given | When | Then |
|---|---|---|---|---|
| **CA-TRANS-01** | API v2 sin breaking changes | La API v2 desplegada en v2.0.9 con consumidores activos | Se actualiza a v2.1.0 | Todos los endpoints, parámetros y formatos de respuesta existentes en v2.0.9 se mantienen funcionalmente equivalentes. Solo se permiten: (a) nuevos endpoints, (b) parámetros opcionales, (c) headers de deprecación. Restricción R-02. |
| **CA-TRANS-02** | Suite de regresión v2.0.9 pasa al 100% | La suite de tests funcionales de v2.0.9 (100+ test cases) | Se ejecuta contra la build de v2.1.0 | 100% de los tests pasan sin modificaciones. Cualquier fallo debe trazarse a un cambio deliberado documentado en un ADR. |
| **CA-TRANS-03** | Documentación v2.0.x preservada como solo-lectura | Los entregables bajo `docs/entregables/v2/` | Se completa la release de v2.1.0 | Ningún archivo bajo `docs/entregables/v2/` fue modificado. La documentación de v2.1.0 reside exclusivamente en `docs/entregables/v2.1/`. Restricción R-08. |

### 3.2 Manejo de Errores y Códigos HTTP

| ID | Criterio | Given | When | Then |
|---|---|---|---|---|
| **CA-TRANS-04** | Códigos HTTP semánticamente correctos | Cualquier endpoint de la API v2 | Ocurre un error | El código HTTP retornado corresponde al estándar: 400 para errores del cliente, 401/403 para autenticación/autorización, 404 para recursos no encontrados, 409 para conflictos, 422 para validación, 500/503 para errores del servidor. |
| **CA-TRANS-05** | Mensajes de error descriptivos y en inglés | Cualquier endpoint de la API v2 | Ocurre un error | El cuerpo de la respuesta incluye un campo `error` con un mensaje descriptivo en inglés que permite al consumidor diagnosticar el problema sin exponer detalles internos (stack traces, API keys, configuración sensible). Restricción R-05. |
| **CA-TRANS-06** | Errores registrados en logs con nivel adecuado | El sistema en operación | Ocurre cualquier error o warning | Los errores del servidor (5xx) se registran con nivel `ERROR`. Los errores del cliente (4xx) se registran con nivel `WARN` o `INFO`. Las condiciones degradadas (cross-encoder no disponible, entry point no encontrado) se registran con nivel `WARN`. Ningún log expone credenciales ni secretos. |
| **CA-TRANS-07** | Sin silent failures | Cualquier operación de la API v2 | Una operación falla parcial o totalmente | El sistema retorna un código de error explícito y un mensaje. No se permite que una operación falle silenciosamente retornando HTTP 200 con datos incompletos o incorrectos. |

### 3.3 Idempotencia

| ID | Criterio | Given | When | Then |
|---|---|---|---|---|
| **CA-TRANS-08** | `DELETE /admin/namespaces/{name}` idempotente | Un namespace ha sido previamente eliminado | Se ejecuta `DELETE /admin/namespaces/{name}` por segunda vez | Se retorna HTTP 404 con mensaje `"Namespace '{name}' not found"`, sin efectos secundarios ni errores 500. |
| **CA-TRANS-09** | Ingesta de memorias con IDs explícitos idempotente | Una memoria con un `id` específico ya existe en el namespace | Se ejecuta una ingesta con el mismo `id` | Si el contenido es idéntico, se retorna HTTP 200 o 409 sin duplicar la memoria. Si el contenido difiere, se comporta como upsert. |
| **CA-TRANS-10** | Operaciones de configuración idempotentes | Un perfil de dominio ya tiene configurada una estrategia | Se aplica la misma configuración nuevamente | La operación es idempotente (no genera errores ni duplicados de configuración) y retorna HTTP 200. |

---

## 4. Matriz de Cobertura

La siguiente matriz muestra la cobertura entre features y sus criterios de aceptación, así como la trazabilidad a los Criterios de Éxito (CE) de la Visión del Producto.

### 4.1 Features × Criterios de Aceptación × Criterios de Éxito

| Feature | CAs | Total CAs | Must | Should | Could | CEs vinculados |
|---|---|---|---|---|---|---|
| FT-V21-001.1 Reranker Cross-Encoder | CA-V21-001.1.1 a CA-V21-001.1.6 | 6 | 6 | 0 | 0 | CE-01, CE-03, CE-04 |
| FT-V21-001.2 Búsqueda Semántica Pura | CA-V21-001.2.1 a CA-V21-001.2.3 | 3 | 3 | 0 | 0 | CE-07 |
| FT-V21-001.3 Expansión de Grafo Robusta | CA-V21-001.3.1 a CA-V21-001.3.7 | 7 | 7 | 0 | 0 | CE-01 |
| FT-V21-001.4 Extracción de Entidades | CA-V21-001.4.1 a CA-V21-001.4.4 | 4 | 0 | 4 | 0 | CE-06 |
| FT-V21-002.1 Optimización N+1 + Cache | CA-V21-002.1.1 a CA-V21-002.1.6 | 6 | 0 | 0 | 6 | CE-02 |
| FT-V21-002.2 Cold Start / Lock Qdrant | CA-V21-002.2.1 a CA-V21-002.2.3 | 3 | 0 | 0 | 3 | CE-02 |
| FT-V21-002.3 Cache JWT | CA-V21-002.3.1 a CA-V21-002.3.4 | 4 | 0 | 4 | 0 | CE-02 |
| FT-V21-003.1 Diagnóstico Worker | CA-V21-003.1.1 a CA-V21-003.1.3 | 3 | 0 | 0 | 3 | CE-02 (indirecto) |
| FT-V21-003.2 Unificación Colecciones Qdrant | CA-V21-003.2.1 a CA-V21-003.2.3 | 3 | 0 | 3 | 0 | CE-05 |
| FT-V21-003.3 graphEntryStrategy Configurable | CA-V21-003.3.1 a CA-V21-003.3.3 | 3 | 0 | 0 | 3 | CE-01, CE-09 |
| FT-V21-004.1 Header X-Graph-Strategy | CA-V21-004.1.1 a CA-V21-004.1.4 | 4 | 0 | 0 | 4 | CE-09 |
| FT-V21-004.2 Unificación search/hybrid | CA-V21-004.2.1 a CA-V21-004.2.4 | 4 | 0 | 0 | 4 | CE-10 |
| FT-V21-004.3 DELETE /admin/namespaces/{name} | CA-V21-004.3.1 a CA-V21-004.3.4 | 4 | 0 | 0 | 4 | CE-08 |
| **Subtotal Features** | — | **54** | **16** | **11** | **27** | — |
| Transversales | CA-TRANS-01 a CA-TRANS-10 | **10** | — | — | — | R-02, R-05, R-08 |
| **Total** | — | **64** | — | — | — | 10/10 CE |

### 4.2 Criterios de Éxito × Features (Matriz de Trazabilidad)

| CE | Descripción | Features que lo verifican |
|---|---|---|
| CE-01 | Top-1 ≥ 0.90 | FT-V21-001.1, FT-V21-001.3, FT-V21-003.3 |
| CE-02 | p95 ≤ 500ms | FT-V21-002.1, FT-V21-002.2, FT-V21-002.3, FT-V21-003.1 |
| CE-03 | NDCG@10 ≥ 0.85 | FT-V21-001.1 |
| CE-04 | Recall@10 ≥ 0.90 | FT-V21-001.1 |
| CE-05 | 1 colección Qdrant | FT-V21-003.2 |
| CE-06 | /extract con OpenAI real | FT-V21-001.4 |
| CE-07 | search sin expandGraph = semántico puro | FT-V21-001.2 |
| CE-08 | DELETE namespace operativo | FT-V21-004.3 |
| CE-09 | X-Graph-Strategy funcional | FT-V21-003.3, FT-V21-004.1 |
| CE-10 | Unificación search/hybrid | FT-V21-004.2 |

**Cobertura**: 10 de 10 Criterios de Éxito tienen al menos una feature con criterios de aceptación que los verifican.

---

## 5. Criterios de NO Aceptación

Los siguientes escenarios **NO** se considerarán defectos ni bloquearán la aceptación de v2.1.0. Están documentados explícitamente para evitar ambigüedad durante la verificación (UAT) y el control de calidad.

| # | Condición NO defectuosa | Justificación |
|---|---|---|
| **NA-01** | La latencia del reranker cross-encoder es mayor en entornos sin GPU que en entornos con GPU | El cross-encoder puede operar sobre CPU. La métrica CE-02 (p95 ≤ 500ms) se mide en el entorno de referencia (CPU únicamente, sin GPU). Si el cliente despliega con GPU y obtiene mejor latencia, es una mejora, no un incumplimiento de la línea base. |
| **NA-02** | El reranker cross-encoder no mejora la precisión en queries en idiomas distintos del inglés | El modelo cross-encoder (OpenAI `gpt-4o-mini` o `allenai/scifact`) está optimizado para inglés. Los benchmarks de v2.1.0 (SciFact, suite multi-dominio) son exclusivamente en inglés. La precisión en otros idiomas no es parte del alcance de esta iteración. |
| **NA-03** | `POST /extract` no extrae entidades de dominios ultra-especializados (ej. bioinformática, derecho fiscal) con el mismo nivel de precisión que del dominio general de infraestructura IT | OpenAI `gpt-4o-mini` es un modelo generalista. La extracción de entidades se verifica con el caso de uso principal (infraestructura IT: servidores, errores, timestamps). Dominios especializados pueden requerir fine-tuning futuro. |
| **NA-04** | La expansión de grafo desde top-3 no produce mejoras de recall si el dense retrieval no retorna 3 resultados con score suficiente | CA-V21-001.3.3 ya define el comportamiento esperado (expandir desde N disponibles sin error). Si el dense retrieval es de baja calidad para ciertas queries, la expansión opera con los entry points disponibles. |
| **NA-05** | `DELETE /admin/namespaces/{name}` no elimina recursos en sistemas externos no gestionados por Abax-Memory (ej. logs en Elasticsearch, métricas en Prometheus) | El scope de `DELETE namespace` está acotado a los recursos bajo gestión directa de Abax-Memory: PostgreSQL (memorias, relaciones, entidades) y Qdrant (puntos vectoriales). La limpieza de sistemas externos es responsabilidad del operador. |
| **NA-06** | El caché JWT no invalida instantáneamente (< 1 segundo) tras una revocación en Keycloak | CA-V21-002.3.3 define el SLA en ≤ 5 segundos. Ventanas menores a 5 segundos donde un token revocado aún es aceptado no se consideran defecto. Este es un trade-off deliberado entre seguridad y latencia, documentado en el supuesto S-06. |
| **NA-07** | El endpoint `POST /memories/hybrid` no soporta los nuevos parámetros (`semanticWeight`, `lexicalWeight`, `expandGraph`) | El endpoint `hybrid` se preserva con backward compatibility exacta a v2.0.9. Los nuevos parámetros solo están disponibles en `POST /memories/search`. El mensaje de deprecación guía a los consumidores hacia el nuevo endpoint. |
| **NA-08** | El `MockLlmService` sigue activo para contextos distintos de `POST /extract` | La feature FT-V21-001.4 solo reemplaza `MockLlmService` en el contexto de `POST /memories/extract`. Otros contextos (validación, sugerencias) que usan `MockLlmService` no son parte del alcance de v2.1.0 (ver item #7 en FUERA del alcance de la Visión del Producto). |
| **NA-09** | El frontend React (6 pantallas) no expone controles para las nuevas capacidades (`X-Graph-Strategy`, `DELETE namespace`, `graphEntryStrategy`) | Las nuevas capacidades de v2.1.0 se exponen exclusivamente vía API. El frontend se mantiene sin cambios funcionales (ver item #6 en FUERA del alcance de la Visión del Producto). |
| **NA-10** | No se añaden nuevas entidades JPA, columnas ni migraciones Flyway | Restricción R-04: el modelo de datos es inalterado en v2.1.0. La ausencia de cambios en el esquema no es un defecto, es una restricción de diseño. |

---

## 6. Definición de Done

Una feature de v2.1.0 se considera **terminada** (Done) cuando se cumplen **todas** las siguientes condiciones. Esta definición aplica a las 13 features y debe ser verificada por el Tech Lead antes del gate Fase 4 (Construcción) → Fase 5 (Pruebas Funcionales).

### 6.1 Done por Feature

| # | Condición | Verificación | Responsable |
|---|---|---|---|
| **DOD-01** | Código implementado y commiteado en la rama de desarrollo | `git log` muestra commits con mensajes que referencian el ID de la feature (`FT-V21-XXX.X`) | Developer |
| **DOD-02** | Code review aprobado por Tech Lead | PR aprobado en el sistema de control de versiones. El Tech Lead verificó que no hay implementaciones mock en features core. | Tech Lead |
| **DOD-03** | Todos los criterios de aceptación de la feature verificados | Cada CA de la feature tiene evidencia de verificación (resultado de test, benchmark, o inspección manual documentada). | QA Lead |
| **DOD-04** | Sin marcas `MOCK` o `TODO` en features core (Must y Should) | Búsqueda de patrones `MOCK`, `TODO`, `FIXME`, `HACK` en el código fuente de la feature. Las features Could pueden tener `TODO` si están documentados y priorizados. | Tech Lead |
| **DOD-05** | Criterios transversales aplicables cumplidos | CA-TRANS-01 a CA-TRANS-10 verificados para el alcance de la feature. | QA Lead |
| **DOD-06** | Documentación técnica actualizada | Si la feature introduce nueva configuración, endpoints o comportamiento, el runbook de operaciones y/o el ADR correspondiente están actualizados. | Developer + Tech Lead |
| **DOD-07** | Sin regresión en tests existentes | La suite de tests de v2.0.9 pasa al 100% contra la build que incluye esta feature (CA-TRANS-02). | QA Lead |

### 6.2 Done Global de la Release v2.1.0

| # | Condición | Verificación |
|---|---|---|
| **DOD-G-01** | 13/13 features implementadas y verificadas según su Definición de Done individual | Dashboard de entregables muestra 13/13 features en estado "Done" |
| **DOD-G-02** | 10/10 Criterios de Éxito (CE-01 a CE-10) cumplidos con evidencia medible | Matriz de Criterios de Éxito con valores medidos vs metas |
| **DOD-G-03** | 10/10 Criterios Transversales (CA-TRANS-01 a CA-TRANS-10) cumplidos | Checklist de criterios transversales verificado |
| **DOD-G-04** | Gate Fase 2 → Fase 3 aprobado por Product Owner | Firma o aprobación explícita del PO en el deliverable registry |
| **DOD-G-05** | Sin defects críticos (severidad P1 o P2) abiertos | Issue tracker sin defects P1/P2 en estado "Open" o "In Progress" |
| **DOD-G-06** | Documentación de release (`CHANGELOG.md`, migration notes) actualizada | CHANGELOG refleja las 13 features con referencias a los CAs |

---

## 7. Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10. Meta v2.1.0: ≥ 0.85 en SciFact.
- **p95**: Percentil 95 — valor de latencia por debajo del cual se completa el 95% de las solicitudes. Meta v2.1.0: ≤ 500ms estable.
- **Cross-encoder**: Modelo de reranking que procesa pares (query, documento) simultáneamente para calcular relevancia fina. Más costoso pero más preciso que el dense retrieval puro (bi-encoder).
- **BFS**: Breadth-First Search — algoritmo de recorrido de grafos por niveles (profundidad), usado para expandir el grafo de conocimiento desde entry points.
- **MockLlmService**: Servicio simulado de v2.0.x que usa regex para extracción de entidades en lugar de IA real. Debe ser reemplazado por OpenAI en `POST /extract`.
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación. Abax-Memory valida JWTs contra Keycloak en cada request.
- **LRU**: Least Recently Used — política de evicción de caché que elimina la entrada menos recientemente usada cuando se alcanza la capacidad máxima.
