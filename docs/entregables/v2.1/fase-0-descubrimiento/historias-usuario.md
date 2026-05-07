# Historias de Usuario — Abax-Memory v2.1.0
## Hardening y Optimización del Motor de Memoria Multi-Dominio

- **Fase**: 0 — Descubrimiento
- **Entregable**: Historias de Usuario
- **Versión**: v2.1.0
- **Responsable**: business-analyst
- **Aprobado por**: product-owner (pendiente de revisión)
- **Fecha**: 2026-05-05
- **Estado**: Completado
- **Fuentes**:
  - `docs/entregables/v2.1/fase-0-descubrimiento/vision-producto.md` (visión v2.1.0)
  - `docs/entregables/v2.1/fase-0-descubrimiento/epicas-features.md` (mapa de 13 features en 4 épicas)
  - Propuesta del usuario (14 items en 4 categorías)

---

## Tabla de Contenidos

- [Resumen de Historias por Épica](#resumen-de-historias-por-épica)
- [EP-V21-001: Precisión del Motor de Búsqueda](#ep-v21-001-precisión-del-motor-de-búsqueda)
- [EP-V21-002: Velocidad y Latencia](#ep-v21-002-velocidad-y-latencia)
- [EP-V21-003: Eficiencia Operativa](#ep-v21-003-eficiencia-operativa)
- [EP-V21-004: API y Developer Experience](#ep-v21-004-api-y-developer-experience)
- [Matriz de Trazabilidad — Historias a Features](#matriz-de-trazabilidad--historias-a-features)
- [Distribución MoSCoW](#distribución-moscow)
- [Glosario](#glosario)

---

## Resumen de Historias por Épica

| Épica | Historias | Must | Should | Could | Won't |
|---|---|---|---|---|---|
| EP-V21-001: Precisión | 6 (HU-001 a HU-006) | 5 | 1 | 0 | 0 |
| EP-V21-002: Velocidad | 4 (HU-007 a HU-010) | 0 | 1 | 3 | 0 |
| EP-V21-003: Eficiencia | 3 (HU-011 a HU-013) | 0 | 1 | 2 | 0 |
| EP-V21-004: API/DX | 3 (HU-014 a HU-016) | 0 | 0 | 3 | 0 |
| **Total** | **16** | **5** | **3** | **8** | **0** |

---

## EP-V21-001: Precisión del Motor de Búsqueda

---

### HU-V21-001: Integrar Cross-Encoder en el Pipeline Two-Stage de Búsqueda

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.1 — Reranker Cross-Encoder en el Pipeline

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que el pipeline de búsqueda semántica aplique una segunda etapa de re-ranking con un modelo cross-encoder sobre el top-20 de candidatos del dense retrieval
**Para** obtener un top-5 final con precisión significativamente superior a la búsqueda dense-only actual, cerrando la brecha de NDCG@10 detectada en el benchmark SciFact (0.7771 → meta ≥ 0.85)

**Criterios de aceptación**:

- Dado un pipeline de búsqueda configurado con cross-encoder activo, cuando ejecuto `POST /memories/search` con `{"query": "The mRNA vaccine showed 95% efficacy in clinical trials", "expandGraph": false}` sobre un índice con ≥ 5,000 documentos del dominio biomédico, entonces el top-1 retornado es el documento ground-truth que afirma "95% efficacy" (verificable contra el dataset SciFact) con una proporción top-1 ≥ 0.90 sobre 100 queries de la suite multi-dominio.

- Dado el mismo pipeline con cross-encoder activo, cuando ejecuto las 300 queries del benchmark SciFact, entonces el NDCG@10 calculado sobre los resultados reordenados es ≥ 0.85 y el Recall@10 se mantiene ≥ 0.90 (el cross-encoder reordena sin descartar documentos relevantes).

- Dado un presupuesto de latencia de 500ms para el endpoint `POST /memories/search` sin `expandGraph`, cuando el cross-encoder procesa un top-20 de candidatos (máximo 20 pares query-documento), entonces la etapa de re-ranking no añade más de 200ms de latencia incremental respecto al dense retrieval puro, manteniendo la latencia total del endpoint en p95 ≤ 500ms.

- Dado que el cross-encoder no está disponible (API key de OpenAI ausente o modelo local no desplegado), cuando se ejecuta `POST /memories/search`, entonces el pipeline degrada gracefulmente a dense retrieval sin cross-encoder, registrando un warning `CROSS_ENCODER_UNAVAILABLE` en logs y retornando resultados con la precisión de v2.0.9 (no debe retornar error 500 ni bloquear la búsqueda).

**Prioridad**: Must

---

### HU-V21-002: Verificar Mejora de Precisión con Benchmark SciFact y Suite Multi-Dominio

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.1 — Reranker Cross-Encoder en el Pipeline

**Como** desarrollador consumidor de la API de Abax-Memory (y stakeholder que valida la calidad del producto)
**Quiero** que la mejora de precisión del pipeline two-stage sea verificada cuantitativamente ejecutando el mismo benchmark SciFact de v2.0.0 y la suite multi-dominio de 100 test cases
**Para** tener evidencia medible de que el cross-encoder cierra la brecha de precisión del único benchmark fallido de v2.0.0 y que la inversión en el reranker produce el retorno esperado

**Criterios de aceptación**:

- Dado el pipeline two-stage (dense retrieval + cross-encoder) completamente operativo, cuando ejecuto el benchmark SciFact completo (5,183 documentos, 300 queries) y calculo NDCG@10, entonces el valor obtenido es ≥ 0.85, superando la línea base de v2.0.9 (0.7771) por al menos +0.07 puntos.

- Dado el mismo pipeline, cuando ejecuto la suite multi-dominio de 100 test cases con ground truth conocido, entonces la proporción de queries cuyo top-1 es el documento ground-truth esperado es ≥ 0.90, superando la línea base de v2.0.9 (~0.75–0.80 dense-only) por al menos +0.10 puntos.

- Dado el pipeline con cross-encoder, cuando comparo los scores de ranking antes y después del reranker para una muestra de 50 queries con ground truth conocido, entonces el documento ground-truth asciende en el ranking en al menos 80% de los casos donde no estaba en top-1 en el dense retrieval, demostrando que el cross-encoder corrige activamente errores de ranking.

**Prioridad**: Must

---

### HU-V21-003: Aislar la Búsqueda Semántica de la Expansión de Grafo

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.2 — Búsqueda Semántica Pura (`search` sin `expandGraph`)

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que el endpoint `POST /memories/search` sin el parámetro `expandGraph` (o con `expandGraph: false`) retorne resultados basados exclusivamente en el pipeline semántico (dense retrieval + cross-encoder), sin ninguna contribución del grafo de conocimiento
**Para** tener control explícito sobre si la búsqueda usa o no el grafo, eliminando la ambigüedad actual donde `search` podía mezclar resultados de ambas fuentes sin transparencia

**Criterios de aceptación**:

- Dado un tenant con 50 memorias y un grafo de conocimiento con 20 relaciones, cuando ejecuto `POST /memories/search` con `{"query": "database connection timeout", "expandGraph": false}` sobre un dominio donde el grafo no tiene entidades relacionadas con "timeout", entonces todos los resultados retornados provienen exclusivamente del pipeline semántico (dense retrieval + cross-encoder), verificable porque ningún resultado incluye nodos vecinos expandidos desde el grafo (campo `graphExpandedNodes` vacío o `graphExpanded: false` en la respuesta).

- Dado el mismo tenant, cuando ejecuto la misma query con `expandGraph: true`, entonces la respuesta incluye resultados expandidos desde el grafo (campo `graphExpandedNodes` no vacío), demostrando que el parámetro `expandGraph` controla efectivamente la activación del grafo.

- Dado un dataset de 50 queries donde el ground truth no tiene relaciones de grafo relevantes (dominio de conocimiento factual no relacional), cuando ejecuto las 50 queries con `expandGraph: false`, entonces la proporción top-1 es ≥ 0.90 y ningún resultado del top-5 tiene origen en el grafo (verificable por trazabilidad de source en cada resultado).

**Prioridad**: Must

---

### HU-V21-004: Expandir Grafo de Conocimiento desde los Top-3 Nodos Más Relevantes

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.3 — Expansión de Grafo Robusta (top-3 + `entryPoints`)

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que al solicitar `expandGraph: true` en una búsqueda, el grafo de conocimiento se expanda desde los 3 nodos más relevantes del dense retrieval (en lugar de solo el mejor match) recuperando sus vecinos hasta una profundidad configurable
**Para** obtener una cobertura cross-dominio superior al 85% (desde el 69.4% actual) en dominios con entidades altamente interrelacionadas, donde el mejor match único no captura todas las conexiones relevantes

**Criterios de aceptación**:

- Dado un tenant con 100 memorias interrelacionadas en un dominio multi-tópico (ej. incidencias de infraestructura con relaciones causales entre servidores, servicios y errores), cuando ejecuto `POST /memories/search` con `{"query": "cascading failure root cause", "expandGraph": true, "graphDepth": 2}` y la búsqueda semántica retorna 3 nodos distintos en el top-10 del dense retrieval, entonces la respuesta incluye nodos expandidos desde los 3 entry points, verificable porque el campo `graphExpandedNodes` contiene al menos 3 `entryPointIds` distintos y el total de nodos expandidos es mayor que si se expandiera desde un único entry point.

- Dado el mismo escenario, cuando calculo el recall multi-dominio sobre 50 queries con ground truth que requiere cruzar múltiples dominios vía relaciones de grafo, entonces el recall mejora de la línea base 69.4% (v2.0.9, single entry) a ≥ 85% (v2.1.0, top-3 entry), medido con el mismo dataset multi-dominio ABM-MULTI-01 de v2.0.0.

- Dado que la búsqueda semántica retorna menos de 3 resultados (score mínimo insuficiente o índice con pocos documentos), cuando ejecuto `POST /memories/search` con `expandGraph: true`, entonces el grafo expande únicamente desde los N nodos disponibles (1 o 2) sin error, y el campo `graphExpandedNodes` refleja correctamente el número real de entry points usados.

- Dado un `graphDepth` configurado en 3, cuando el grafo se expande desde los top-3 entry points, entonces ningún nodo en el resultado del BFS excede la profundidad 3 desde su entry point origen, verificable porque la distancia máxima (`maxDistance` en la metadata de respuesta) es ≤ 3.

**Prioridad**: Must

---

### HU-V21-005: Soportar Puntos de Entrada Explícitos para Expansión de Grafo

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.3 — Expansión de Grafo Robusta (top-3 + `entryPoints`)

**Como** desarrollador consumidor avanzado de la API de Abax-Memory
**Quiero** poder especificar explícitamente los IDs de memorias (`entryPoints`) desde los cuales se debe expandir el grafo de conocimiento en una búsqueda con `expandGraph: true`, en lugar de depender de los resultados del dense retrieval
**Para** tener control total sobre los nodos semilla de la expansión en escenarios donde conozco los nodos correctos (ej. debugging de una incidencia donde sé qué servidores están involucrados), maximizando la precisión del subgrafo recuperado

**Criterios de aceptación**:

- Dado un tenant con un grafo de conocimiento que contiene relaciones entre las memorias `mem-001`, `mem-002` y `mem-003`, cuando ejecuto `POST /memories/search` con `{"expandGraph": true, "entryPoints": ["mem-001", "mem-002"], "graphDepth": 1}`, entonces la respuesta incluye todos los nodos vecinos de `mem-001` y `mem-002` a profundidad 1, verificable porque el campo `graphExpandedNodes` contiene ambos `entryPointIds` explícitos y sus respectivos vecinos.

- Dado que especifico `entryPoints` con un ID que no existe en el grafo (`"mem-999"`), cuando ejecuto la búsqueda, entonces el sistema registra un warning `ENTRY_POINT_NOT_FOUND` en logs para ese ID, lo excluye silenciosamente de la expansión sin fallar la request, y expande desde los entry points restantes válidos.

- Dado que especifico simultáneamente `entryPoints` explícitos y una estrategia `top-k` vía header `X-Graph-Strategy`, cuando ejecuto la búsqueda, entonces los `entryPoints` explícitos tienen precedencia sobre cualquier selección automática y el sistema registra un log info indicando que se usaron entry points proporcionados por el cliente.

**Prioridad**: Must

---

### HU-V21-006: Extracción de Entidades con Inteligencia Artificial Real

**Épica**: EP-V21-001 — Precisión del Motor de Búsqueda
**Feature**: FT-V21-001.4 — Extracción de Entidades Funcional (`POST /extract` con OpenAI)

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que el endpoint `POST /memories/extract` procese el texto de entrada usando OpenAI `gpt-4o-mini` para extraer entidades semánticamente (nombres de servidores, códigos de error, timestamps, entidades de dominio, etc.) en lugar de patrones regex superficiales del `MockLlmService`
**Para** obtener entidades con tipos correctos y relevancia semántica real, haciendo que la extracción de entidades de Abax-Memory sea competitiva frente a soluciones como Zep y Letta

**Criterios de aceptación**:

- Dado el endpoint `POST /memories/extract` configurado con OpenAI `gpt-4o-mini` y una API key válida, cuando envío `{"text": "The server nginx-prod-01 crashed due to OOM at 14:32 UTC. Service api-gateway was affected."}`, entonces la respuesta incluye al menos las entidades `nginx-prod-01` (tipo `SERVER`), `OOM` (tipo `ERROR_CONDITION`), `14:32 UTC` (tipo `TIMESTAMP`), y `api-gateway` (tipo `SERVICE`), cada una con un campo `confidence` > 0.0.

- Dado el mismo endpoint, cuando envío `{"text": ""}` (texto vacío), entonces el sistema retorna HTTP 400 con mensaje `"text must not be blank"` y no se realiza ninguna llamada a OpenAI.

- Dado que la API key de OpenAI no está configurada o está vencida, cuando ejecuto `POST /memories/extract`, entonces el sistema retorna HTTP 503 con mensaje `"Entity extraction unavailable: LLM service not configured"` y registra el error en logs con nivel `ERROR`, sin exponer la API key ni detalles internos.

- Dado un texto de entrada de 5,000 caracteres (límite máximo razonable para extracción), cuando ejecuto `POST /memories/extract`, entonces la respuesta se recibe en ≤ 3 segundos (latencia p95), y el sistema no trunca el texto ni rechaza la request por longitud.

**Prioridad**: Should

---

## EP-V21-002: Velocidad y Latencia

---

### HU-V21-007: Preservar la Optimización N+1 de Queries de Grafo

**Épica**: EP-V21-002 — Velocidad y Latencia
**Feature**: FT-V21-002.1 — Optimización N+1 del Grafo + Cache de Resultados

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que la optimización N+1 existente en v2.0.9 (consulta batch de relaciones del grafo en lugar de queries individuales por nodo) se mantenga funcional y no se degrade con los cambios introducidos en v2.1.0 (expansión top-3, estrategias configurables)
**Para** garantizar que la latencia del pipeline de búsqueda no aumente por reintroducción de patrones N+1, preservando la ganancia de velocidad lograda en la iteración anterior

**Criterios de aceptación**:

- Dado un grafo de conocimiento con 50 nodos interrelacionados donde la expansión top-3 desde 3 entry points implicaría potencialmente `3 × N` queries individuales sin optimización, cuando ejecuto `POST /memories/search` con `expandGraph: true` y `graphDepth: 2`, entonces el número de queries SQL ejecutadas para recuperar relaciones del grafo es ≤ 3 (una consulta batch por nivel de profundidad, o una consulta batch total), verificable mediante el log de queries SQL en nivel DEBUG.

- Dado el mismo escenario, cuando comparo la latencia de la expansión de grafo en v2.1.0 contra la latencia en v2.0.9 para la misma query y mismo grafo (single entry point para comparación justa), entonces la latencia en v2.1.0 no excede la de v2.0.9 en más de 10%, demostrando que no hubo regresión.

- Dado que el código de batch fetching está documentado y cubierto por tests unitarios, cuando ejecuto la suite de tests unitarios del módulo de grafo, entonces existen al menos 2 tests específicos que verifican el batch fetching (no queries individuales), y ambos pasan (estado verde).

**Prioridad**: Could

---

### HU-V21-008: Cachear Resultados de Expansión de Grafo en Memoria

**Épica**: EP-V21-002 — Velocidad y Latencia
**Feature**: FT-V21-002.1 — Optimización N+1 del Grafo + Cache de Resultados

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que los resultados de expansión del grafo de conocimiento (BFS desde entry points con un depth dado) se almacenen en una caché en memoria (Caffeine) con TTL configurable
**Para** que queries repetidas que comparten los mismos entry points y profundidad no recalculen el mismo subgrafo, reduciendo la latencia p95 del pipeline con `expandGraph` y contribuyendo a la meta de p95 ≤ 500ms

**Criterios de aceptación**:

- Dado un caché de grafo configurado con TTL de 60 segundos y capacidad máxima de 1,000 entradas, cuando ejecuto 3 queries consecutivas idénticas (`misma query`, `mismos entry points`, `mismo graphDepth`), entonces la primera query ejecuta el BFS completo (cache miss), y la segunda y tercera queries sirven el resultado desde caché (cache hit), verificable porque la latencia de las queries 2 y 3 es al menos 50% menor que la query 1 y los logs muestran `GRAPH_CACHE_HIT`.

- Dado que se añade una nueva relación al grafo entre la query 1 y la query 2 que conecta uno de los entry points cacheados, cuando ejecuto la query 2, entonces el sistema detecta la modificación del grafo (vía evento de invalidación o timestamp de última modificación), invalida la entrada del caché para ese entry point, y ejecuta un BFS fresco (cache miss), sirviendo resultados que incluyen la nueva relación.

- Dado un caché con 900 entradas (cerca de su capacidad máxima de 1,000), cuando una nueva entrada se añade (total = 901), entonces la política de evicción (LRU) elimina la entrada menos recientemente usada sin error, y el caché nunca excede las 1,000 entradas, verificable mediante métricas expuestas (`cache.size ≤ maxSize`).

**Prioridad**: Could

---

### HU-V21-009: Diagnosticar y Mitigar Latencia Anómala en Qdrant

**Épica**: EP-V21-002 — Velocidad y Latencia
**Feature**: FT-V21-002.2 — Investigación y Mitigación de Cold Start / Lock en Qdrant

**Como** administrador del sistema Abax-Memory
**Quiero** que se diagnostiquen las causas raíz de los spikes de latencia a ~2s observados en Qdrant (cold start, locks por escrituras concurrentes, contención en filtros compuestos) y se apliquen mitigaciones de configuración
**Para** lograr una latencia p95 estable ≤ 500ms en el pipeline de búsqueda semántica, eliminando la variabilidad que degrada la experiencia del consumidor de la API

**Criterios de aceptación**:

- Dado un clúster Qdrant en condiciones de producción (≥ 10,000 puntos vectoriales, 3 tenants activos), cuando ejecuto una prueba de carga de 300 queries `POST /memories/search` con `expandGraph: false` en 3 momentos distintos (cold start tras despliegue, steady state tras 5 minutos de operación, y bajo carga de 10 escrituras concurrentes), entonces la latencia p95 en los 3 momentos es ≤ 500ms, verificable mediante el reporte de percentiles de la herramienta de carga.

- Dado el diagnóstico completado, cuando reviso la documentación de hallazgos, entonces existe un documento de diagnóstico (ADR o runbook de operaciones) que identifica al menos una causa raíz específica de los spikes de latencia (ej. `optimizers_config.default_segment_number` subconfigurado, `flush_interval_sec` inadecuado, o falta de `read_consistency`), la mitigación aplicada, y el impacto medido (antes vs después), sin "TODO" sin asignar.

- Dado que las mitigaciones están aplicadas, cuando reinicio el servicio Qdrant (simulando un cold start real), entonces el pre-calentamiento de segmentos (o la mitigación equivalente) completa en ≤ 30 segundos y la primera query post-reinicio tiene latencia ≤ 1 segundo, convergiendo a ≤ 500ms en las siguientes 10 queries.

**Prioridad**: Could

---

### HU-V21-010: Cachear Validación de Tokens JWT en Backend

**Épica**: EP-V21-002 — Velocidad y Latencia
**Feature**: FT-V21-002.3 — Cache JWT en el Cliente por TTL

**Como** administrador del sistema Abax-Memory
**Quiero** que la validación de tokens JWT contra Keycloak se almacene en una caché en memoria con TTL igual al campo `exp` del token
**Para** que requests subsecuentes del mismo cliente con el mismo token no realicen una llamada de red a Keycloak en cada request, reduciendo la latencia acumulativa de autenticación (50–200ms por request) y contribuyendo a la meta de p95 ≤ 500ms

**Criterios de aceptación**:

- Dado un cliente autenticado con un JWT válido (expira en 1 hora), cuando ejecuta 100 requests consecutivas a `POST /memories/search`, entonces la primera request valida el JWT contra Keycloak (cache miss), y las 99 requests subsecuentes validan contra la caché local (cache hit), verificable porque los logs muestran exactamente 1 llamada a Keycloak y 99 `JWT_CACHE_HIT`, y la latencia de autenticación en las requests 2-100 es ≤ 5ms.

- Dado un JWT en caché cuyo `exp` ha vencido, cuando el cliente ejecuta una request con ese token, entonces el sistema detecta la expiración vía TTL de la caché, descarta la entrada cacheada (evicción por TTL), e intenta validar contra Keycloak — si Keycloak rechaza el token expirado, retorna HTTP 401.

- Dado que un token es revocado en Keycloak (logout del usuario o cambio de roles), cuando el sistema recibe la notificación de revocación (Keycloak Admin Event), entonces la entrada correspondiente en la caché JWT se invalida en ≤ 5 segundos desde la recepción del evento, y la siguiente request con ese token es rechazada (HTTP 401) tras validación contra Keycloak.

- Dado que la caché JWT está activa, cuando inspecciono las métricas del sistema, entonces existe una métrica `jwt_cache_hit_ratio` que refleja la proporción de hits vs misses, y el ratio es ≥ 0.90 en condiciones normales de operación (requests repetidas del mismo cliente).

**Prioridad**: Should

---

## EP-V21-003: Eficiencia Operativa

---

### HU-V21-011: Diagnosticar y Resolver Worker de Procesamiento Inactivo

**Épica**: EP-V21-003 — Eficiencia Operativa
**Feature**: FT-V21-003.1 — Diagnóstico del Worker de Procesamiento (Claimed = 0)

**Como** administrador del sistema Abax-Memory
**Quiero** que se diagnostique la causa raíz del worker de procesamiento asíncrono que reporta `Claimed = 0` (sin trabajo procesado) y que se resuelva el problema: eliminando el worker si es innecesario porque el procesamiento ya es síncrono, o reparándolo si es requerido para la generación asíncrona de embeddings y entidades
**Para** eliminar un componente que consume recursos (CPU, memoria, conexiones) sin producir valor, y simplificar la arquitectura de despliegue del sistema

**Criterios de aceptación**:

- Dado el worker de procesamiento en su estado actual (Claimed = 0), cuando ejecuto el diagnóstico, entonces existe un documento que identifica explícitamente la causa raíz (conexión a cola rota, configuración de polling incorrecta, o worker innecesario porque `POST /memories` ya procesa sincrónicamente embeddings), y propone una acción concreta: eliminar o reparar.

- Dado que la acción decidida es "eliminar el worker" (procesamiento síncrono es suficiente), cuando verifico el despliegue post-eliminación, entonces no existe ningún proceso worker en ejecución, `POST /memories` sigue generando embeddings y entidades correctamente (verificable con un test de ingesta de 10 memorias), y la latencia p95 de `POST /memories` no excede la de v2.0.9 en más de 10%.

- Dado que la acción decidida es "reparar el worker" (procesamiento asíncrono necesario), cuando ejecuto una ingesta de 50 memorias, entonces el worker procesa al menos 45 de ellas en ≤ 30 segundos post-ingesta (Claimed ≥ 45), y los embeddings generados son correctos (verificable porque las 50 memorias son encontrables vía búsqueda semántica en ≤ 5 segundos tras la ingesta).

**Prioridad**: Could

---

### HU-V21-012: Unificar Colecciones Qdrant en una Sola

**Épica**: EP-V21-003 — Eficiencia Operativa
**Feature**: FT-V21-003.2 — Unificación de Colecciones Qdrant (Eliminar `abax-memories-v1`)

**Como** administrador del sistema Abax-Memory
**Quiero** consolidar las dos colecciones Qdrant actuales (`abax-memories-v1` y `abax-memories-v2`) en una sola colección (`abax-memories`), eliminando la colección v1 que solo contiene datos residuales de v1.0.0 sin uso funcional
**Para** reducir el overhead operativo (backups, monitoreo, optimización de índices duplicados), liberar memoria en el clúster Qdrant, y cumplir la meta CE-05 (exactamente 1 colección activa en producción)

**Criterios de aceptación**:

- Dado el clúster Qdrant con las colecciones `abax-memories-v1` y `abax-memories-v2`, cuando ejecuto la verificación pre-migración, entonces confirmo que la colección `abax-memories-v1` no contiene puntos que estén referenciados activamente desde la base de datos PostgreSQL (ningún `memory_id` en la tabla `memories` apunta a un punto exclusivo de v1), documentando el resultado en un checklist de verificación.

- Dado que la verificación pre-migración confirma que `abax-memories-v1` es segura de eliminar, cuando ejecuto el procedimiento de unificación, entonces al finalizar: (a) `GET /collections` en Qdrant muestra exactamente 1 colección llamada `abax-memories`, (b) `GET /collections/abax-memories-v1` retorna 404, (c) todas las queries de búsqueda semántica contra la colección unificada retornan resultados correctos (verificable con 50 queries de la suite multi-dominio, 100% de resultados esperados), y (d) ninguna operación de la API v2 falla por referencia a la colección eliminada.

- Dado que la unificación está completada, cuando ejecuto una ingesta de 10 nuevas memorias, entonces los puntos vectoriales se almacenan correctamente en `abax-memories`, son encontrables vía búsqueda semántica en ≤ 2 segundos, y el conteo de puntos en la colección se incrementa exactamente en 10.

**Prioridad**: Should

---

### HU-V21-013: Configurar Estrategia de Entrada al Grafo por Perfil de Dominio

**Épica**: EP-V21-003 — Eficiencia Operativa
**Feature**: FT-V21-003.3 — `graphEntryStrategy` Configurable

**Como** administrador del sistema Abax-Memory
**Quiero** poder configurar la estrategia de entrada al grafo de conocimiento (`graphEntryStrategy`) a nivel de perfil de dominio, eligiendo entre `single-best`, `top-k` (con `k` configurable), o `threshold` (con `minScore` configurable)
**Para** que cada perfil de dominio pueda optimizar su comportamiento de expansión de grafo según sus características: dominios con entidades muy relacionadas usan `top-k` para máxima cobertura, mientras que dominios con relaciones ruidosas usan `single-best` o `threshold` alto para precisión

**Criterios de aceptación**:

- Dado un perfil de dominio "infraestructura" con `graphEntryStrategy: top-k` y `graphK: 3`, cuando un consumidor ejecuta `POST /memories/search` con `expandGraph: true` en ese dominio (sin especificar estrategia vía header `X-Graph-Strategy`), entonces la expansión de grafo se realiza desde exactamente 3 entry points (los 3 mejores matches del dense retrieval), verificable porque el campo `graphExpandedNodes.entryPointCount` en la respuesta es 3.

- Dado un perfil de dominio "legal" con `graphEntryStrategy: threshold` y `graphThreshold: 0.85`, cuando un consumidor ejecuta `POST /memories/search` con `expandGraph: true` y el dense retrieval retorna 10 candidatos con scores [0.92, 0.88, 0.84, 0.81, ...], entonces la expansión de grafo se realiza únicamente desde los 2 entry points con score ≥ 0.85, verificable porque `entryPointCount` es 2.

- Dado que un administrador cambia `graphEntryStrategy` de `single-best` a `top-k` con `graphK: 5` en un perfil de dominio activo, cuando ejecuto 10 queries de búsqueda con `expandGraph: true` en ese dominio (sin calentamiento previo), entonces las 10 queries usan la nueva estrategia `top-k` con 5 entry points, verificable porque `entryPointCount` es consistentemente 5 en las 10 respuestas (siempre que el dense retrieval retorne ≥ 5 resultados), sin necesidad de reiniciar el servicio.

**Prioridad**: Could

---

## EP-V21-004: API y Developer Experience

---

### HU-V21-014: Controlar Estrategia de Expansión de Grafo por Request vía Header HTTP

**Épica**: EP-V21-004 — API y Developer Experience
**Feature**: FT-V21-004.1 — Header `X-Graph-Strategy`

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** poder especificar la estrategia de expansión del grafo de conocimiento por request individual usando el header HTTP `X-Graph-Strategy` con valores `none`, `single`, `top-k`, o `threshold`, y parámetros complementarios `X-Graph-K` y `X-Graph-Threshold`
**Para** tener control granular sobre cómo se comporta cada query sin depender de la configuración del perfil de dominio, permitiendo que agentes y aplicaciones consumidoras adapten la estrategia a cada caso de uso específico

**Criterios de aceptación**:

- Dado un tenant con un grafo de conocimiento poblado, cuando ejecuto `POST /memories/search` con header `X-Graph-Strategy: none` (sin importar el valor de `expandGraph`), entonces la respuesta no incluye ningún nodo expandido desde el grafo (`graphExpandedNodes` vacío o `graphExpanded: false`), independientemente de la estrategia configurada en el perfil de dominio.

- Dado el mismo tenant, cuando ejecuto `POST /memories/search` con headers `X-Graph-Strategy: top-k` y `X-Graph-K: 5`, entonces la expansión de grafo se realiza desde exactamente 5 entry points (los 5 mejores matches del dense retrieval), verificable porque `graphExpandedNodes.entryPointCount` es 5, y este comportamiento sobrescribe cualquier configuración del perfil de dominio para esta request.

- Dado que envío un header con valor inválido (`X-Graph-Strategy: invalid-strategy`), cuando ejecuto `POST /memories/search`, entonces el sistema retorna HTTP 400 con mensaje `"Invalid X-Graph-Strategy: 'invalid-strategy'. Supported values: none, single, top-k, threshold"` y no ejecuta la búsqueda.

- Dado que envío `X-Graph-Strategy: top-k` sin el header complementario `X-Graph-K`, cuando ejecuto la búsqueda, entonces el sistema usa el valor default `k=3` (configurado en el perfil de dominio) y registra un log info indicando que `X-Graph-K` no fue provisto y se usó el default.

**Prioridad**: Could

---

### HU-V21-015: Unificar Endpoints de Búsqueda con Parámetros Explícitos y Deprecar `hybrid`

**Épica**: EP-V21-004 — API y Developer Experience
**Feature**: FT-V21-004.2 — Unificación de Endpoints `search` y `hybrid`

**Como** desarrollador consumidor de la API de Abax-Memory
**Quiero** que exista un solo endpoint `POST /memories/search` que soporte todos los modos de búsqueda (semántica pura, híbrida léxica+semántica, y con grafo) mediante parámetros explícitos (`semanticWeight`, `lexicalWeight`, `expandGraph`, `graphStrategy`), y que el endpoint legacy `POST /memories/hybrid` retorne un warning de deprecación
**Para** tener una API más clara y consistente sin la ambigüedad actual de dos endpoints con semántica solapada, y un período de migración sin breaking change

**Criterios de aceptación**:

- Dado el endpoint unificado `POST /memories/search`, cuando ejecuto una búsqueda híbrida con `{"query": "nginx timeout error", "semanticWeight": 0.6, "lexicalWeight": 0.4, "expandGraph": false}`, entonces los resultados combinan similitud semántica (60% peso) y matching léxico (40% peso), y el score final de cada resultado refleja la combinación proporcional de ambas fuentes, verificable porque el orden de resultados difiere del obtenido con `semanticWeight: 1.0, lexicalWeight: 0.0`.

- Dado el endpoint legacy `POST /memories/hybrid`, cuando ejecuto una request con parámetros equivalentes a los de v2.0.9, entonces la respuesta incluye el header `Deprecation: true` y el header `Warning: 299 - "POST /memories/hybrid is deprecated. Use POST /memories/search with semanticWeight and lexicalWeight parameters."`, pero la funcionalidad de búsqueda es idéntica a v2.0.9 (backward compatibility).

- Dado el endpoint unificado `POST /memories/search`, cuando ejecuto la suite de 100 test cases multi-dominio usando exclusivamente este endpoint con distintas combinaciones de parámetros (`semanticWeight=1.0` para semántico puro, `semanticWeight=0.5,lexicalWeight=0.5` para híbrido balanceado, `expandGraph=true` para búsqueda con grafo), entonces los resultados son funcionalmente equivalentes a los obtenidos en v2.0.9 con los endpoints separados para los mismos casos de uso (misma precisión top-1, mismo recall), verificable comparando contra los resultados benchmark de v2.0.0.

- Dado que un consumidor envía parámetros contradictorios (`semanticWeight: 0.0, lexicalWeight: 0.0`), cuando ejecuto `POST /memories/search`, entonces el sistema retorna HTTP 400 con mensaje `"At least one of semanticWeight or lexicalWeight must be > 0"`, sin ejecutar la búsqueda.

**Prioridad**: Could

---

### HU-V21-016: Eliminar Namespace Completo en Operación Atómica

**Épica**: EP-V21-004 — API y Developer Experience
**Feature**: FT-V21-004.3 — `DELETE /admin/namespaces/{name}`

**Como** administrador del sistema Abax-Memory
**Quiero** disponer de un endpoint `DELETE /admin/namespaces/{name}` que elimine atómicamente todas las memorias, relaciones, entidades y puntos vectoriales en Qdrant asociados a un namespace en una sola operación, requiriendo rol `memory-admin`
**Para** poder limpiar tenants completos de forma rápida y confiable sin residuos, facilitando escenarios de prueba, benchmarks con tenants efímeros, y administración multi-tenant en producción

**Criterios de aceptación**:

- Dado un namespace `benchmark-sifact` con 50 memorias, 20 relaciones, 15 entidades, y 50 puntos vectoriales en Qdrant, cuando ejecuto `DELETE /admin/namespaces/benchmark-sifact` con autenticación de rol `memory-admin`, entonces el sistema retorna HTTP 200 con un body que resume los recursos eliminados (`{"deleted": {"memories": 50, "relations": 20, "entities": 15, "qdrantPoints": 50}}`), y una verificación posterior confirma: (a) `GET /admin/namespaces/benchmark-sifact` retorna 404, (b) `POST /memories/search` dentro del namespace retorna 0 resultados, (c) los 50 puntos fueron eliminados de Qdrant.

- Dado un namespace `prod-critical` con 10,000+ memorias, cuando ejecuto `DELETE /admin/namespaces/prod-critical`, entonces la operación completa en ≤ 30 segundos y es atómica: o bien se eliminan todos los recursos, o bien (en caso de fallo parcial) el namespace permanece intacto y el sistema retorna HTTP 500 con mensaje describiendo el fallo, sin dejar el namespace en estado inconsistente (sin memorias pero con puntos Qdrant residuales).

- Dado que un usuario con rol `memory-operator` (sin rol `memory-admin`) intenta ejecutar `DELETE /admin/namespaces/test-ns`, entonces el sistema retorna HTTP 403 con mensaje `"Forbidden: memory-admin role required"` y no se elimina ningún recurso.

- Dado que el namespace especificado no existe (`DELETE /admin/namespaces/nonexistent`), cuando ejecuto la request con rol `memory-admin`, entonces el sistema retorna HTTP 404 con mensaje `"Namespace 'nonexistent' not found"`.

**Prioridad**: Could

---

## Matriz de Trazabilidad — Historias a Features

| Historia | Épica | Feature | CEs vinculados |
|---|---|---|---|
| HU-V21-001 | EP-V21-001 | FT-V21-001.1 | CE-01, CE-03, CE-04 |
| HU-V21-002 | EP-V21-001 | FT-V21-001.1 | CE-01, CE-03 |
| HU-V21-003 | EP-V21-001 | FT-V21-001.2 | CE-07 |
| HU-V21-004 | EP-V21-001 | FT-V21-001.3 | CE-01 |
| HU-V21-005 | EP-V21-001 | FT-V21-001.3 | CE-01 |
| HU-V21-006 | EP-V21-001 | FT-V21-001.4 | CE-06 |
| HU-V21-007 | EP-V21-002 | FT-V21-002.1 | CE-02 |
| HU-V21-008 | EP-V21-002 | FT-V21-002.1 | CE-02 |
| HU-V21-009 | EP-V21-002 | FT-V21-002.2 | CE-02 |
| HU-V21-010 | EP-V21-002 | FT-V21-002.3 | CE-02 |
| HU-V21-011 | EP-V21-003 | FT-V21-003.1 | CE-02 (indirecto) |
| HU-V21-012 | EP-V21-003 | FT-V21-003.2 | CE-05 |
| HU-V21-013 | EP-V21-003 | FT-V21-003.3 | CE-01, CE-09 |
| HU-V21-014 | EP-V21-004 | FT-V21-004.1 | CE-09 |
| HU-V21-015 | EP-V21-004 | FT-V21-004.2 | CE-10 |
| HU-V21-016 | EP-V21-004 | FT-V21-004.3 | CE-08 |

---

## Distribución MoSCoW

### Must (5 historias) — Alta prioridad, bloqueantes para el éxito de v2.1.0

| Historia | Feature | Justificación |
|---|---|---|
| HU-V21-001 | FT-V21-001.1 — Cross-encoder en pipeline | Habilitador central de precisión. Sin esta historia, CE-01 y CE-03 no se cumplen. |
| HU-V21-002 | FT-V21-001.1 — Verificar benchmark SciFact | Evidencia cuantitativa del cumplimiento de metas. Requisito de gobernanza R-07 (trazabilidad). |
| HU-V21-003 | FT-V21-001.2 — Aislar búsqueda semántica | Prerrequisito para pipeline limpio. Sin esta historia, el reranker opera sobre un pipeline con ruido del grafo. |
| HU-V21-004 | FT-V21-001.3 — Expandir desde top-3 | Mejora cobertura cross-dominio de 69.4% → ≥ 85%. Complementa al reranker. |
| HU-V21-005 | FT-V21-001.3 — Entry points explícitos | Control avanzado del grafo para consumidores que conocen los nodos semilla. Feature completa de FT-V21-001.3. |

### Should (3 historias) — Alta prioridad, contribuyen significativamente a las metas

| Historia | Feature | Justificación |
|---|---|---|
| HU-V21-006 | FT-V21-001.4 — Extracción con OpenAI | Bloqueante para la precisión del producto frente a competidores (Zep/Letta). Depende de D-02 (API key OpenAI). |
| HU-V21-010 | FT-V21-002.3 — Cache JWT | Bajo riesgo, alto retorno. Impacto directo y medible en latencia percibida. |
| HU-V21-012 | FT-V21-003.2 — Unificar colecciones Qdrant | Deuda operativa creciente. Meta medible CE-05. Depende de S-02 (v1 solo tiene datos residuales). |

### Could (8 historias) — Media prioridad, aportan valor pero no son bloqueantes

| Historia | Feature | Justificación |
|---|---|---|
| HU-V21-007 | FT-V21-002.1 — Preservar optimización N+1 | Previene regresión pero es verificativo, no constructivo. |
| HU-V21-008 | FT-V21-002.1 — Cache de grafo | Mejora latencia pero depende de FT-V21-001.3. |
| HU-V21-009 | FT-V21-002.2 — Cold start / lock Qdrant | Requiere investigación previa; las mitigaciones son condicionales al diagnóstico. |
| HU-V21-011 | FT-V21-003.1 — Diagnosticar worker | Requiere diagnóstico previo; la acción es condicional al hallazgo. |
| HU-V21-013 | FT-V21-003.3 — graphEntryStrategy configurable | Habilitador para FT-V21-004.1. Sin esta, el header X-Graph-Strategy no tiene backend. |
| HU-V21-014 | FT-V21-004.1 — Header X-Graph-Strategy | Mejora DX pero depende de FT-V21-003.3. |
| HU-V21-015 | FT-V21-004.2 — Unificar search/hybrid | Mejora DX pero es principalmente cosmética; el comportamiento legacy se preserva. |
| HU-V21-016 | FT-V21-004.3 — DELETE namespace | Utilidad administrativa valiosa pero no impacta precisión ni velocidad. |

### Won't (0 historias)

Ninguna feature de v2.1.0 se difiere a futuro. Las features excluidas del alcance (compresión de payload Qdrant, rate limiting por API key) no están en este documento porque no fueron validadas por el sponsor. Si se desean incluir, debe tramitarse mediante control de cambios formal.

---

## Glosario

- **MoSCoW**: Técnica de priorización con cuatro niveles: Must (obligatorio para el éxito), Should (importante pero no bloqueante), Could (deseable si hay margen), Won't (diferido a iteración futura).
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10. Meta v2.1.0: ≥ 0.85 en SciFact.
- **Cross-encoder**: Modelo de reranking que procesa pares (query, documento) simultáneamente para calcular relevancia fina. Más costoso pero más preciso que el dense retrieval puro (bi-encoder).
- **Qdrant**: Base de datos vectorial open-source usada para almacenar embeddings y búsqueda semántica por similitud de coseno.
- **BFS**: Breadth-First Search — algoritmo de recorrido de grafos por niveles (profundidad), usado para expandir el grafo de conocimiento desde entry points.
- **p95**: Percentil 95 — valor de latencia por debajo del cual se completa el 95% de las solicitudes. Meta v2.1.0: ≤ 500ms estable.
- **MockLlmService**: Servicio simulado de v2.0.x que usa regex para extracción de entidades en lugar de IA real. Debe ser reemplazado por OpenAI en `POST /extract` (HU-V21-006).
