# Mapa de Épicas y Features — Abax-Memory v2.1.0
## Hardening y Optimización del Motor de Memoria Multi-Dominio

- **Fase**: 0 — Descubrimiento
- **Entregable**: Mapa de Épicas y Features
- **Versión**: v2.1.0
- **Responsable**: business-analyst
- **Aprobado por**: product-owner (pendiente de revisión)
- **Fecha**: 2026-05-05
- **Estado**: Completado
- **Fuentes**:
  - Propuesta del usuario (14 items en 4 categorías)
  - `docs/entregables/v2.1/fase-0-descubrimiento/vision-producto.md` (16 items en Visión extendida)
  - `docs/entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md` (7 benchmarks v2.0.0)

---

> **Nota sobre el conteo**: Este documento incluye **13 features** correspondientes a los items que el usuario listó explícitamente en su propuesta de v2.1.0. La Visión del Producto contiene 16 items porque el business-analyst agregó 2 items adicionales (compresión de payload Qdrant y rate limiting por API key) y desdobló el item "Optimización N+1 + Cache de grafo" en dos items separados. **Para este documento de Épicas/Features**, se respeta estrictamente el alcance definido por el usuario (13 items), agrupados en 4 épicas. Las features extras de la Visión no se incluyen aquí por no haber sido validadas explícitamente por el sponsor.

---

## Tabla de Contenidos

- [Épicas y Features](#épicas-y-features)
  - [EP-V21-001: Precisión del Motor de Búsqueda](#ep-v21-001-precisión-del-motor-de-búsqueda)
  - [EP-V21-002: Velocidad y Latencia](#ep-v21-002-velocidad-y-latencia)
  - [EP-V21-003: Eficiencia Operativa](#ep-v21-003-eficiencia-operativa)
  - [EP-V21-004: API y Developer Experience](#ep-v21-004-api-y-developer-experience)
- [Matriz Resumen](#matriz-resumen)
- [Matriz de Trazabilidad — Features a Criterios de Éxito](#matriz-de-trazabilidad--features-a-criterios-de-éxito)
- [Priorización Global](#priorización-global)
- [Nota sobre features de la Visión excluidas de este documento](#nota-sobre-features-de-la-visión-excluidas-de-este-documento)
- [Glosario](#glosario)

---

## Épicas y Features

### EP-V21-001: Precisión del Motor de Búsqueda

**Descripción**: Mejoras que aumentan la precisión top-1 de las queries directas, la robustez del pipeline de recuperación semántica y la calidad de la extracción de entidades. Esta épica ataca el único benchmark fallido de v2.0.0 (CE-01 NDCG@10 = 0.7771) y consolida el comportamiento del endpoint `search` como búsqueda semántica pura cuando no se solicita expansión de grafo.

**Gap de v2.0.9 que ataca**: Precisión top-1 ~0.78 (dense-only sin reranker), `POST /extract` con MockLlmService, grafo expandiendo desde un solo entry point.

**Meta de la épica**: Top-1 ≥ 0.90 en suite multi-dominio, NDCG@10 ≥ 0.85 en SciFact, extracción de entidades con IA real.

---

#### FT-V21-001.1: Reranker Cross-Encoder en el Pipeline

- **ID**: FT-V21-001.1
- **Título**: Agregar etapa de re-ranking con modelo cross-encoder
- **Descripción**: Incorporar un modelo cross-encoder como segunda etapa del pipeline de búsqueda. El flujo propuesto es: dense retrieval produce top-20 candidatos → cross-encoder evalúa cada par (query, documento) → reordenamiento → top-5 final. El cross-encoder puede usar OpenAI (`gpt-4o-mini`) o un modelo local fine-tuned (`allenai/scifact`). Esta mejora es la pieza central para cerrar la brecha de precisión detectada en el único benchmark fallido de v2.0.0.
- **Qué resuelve**: La búsqueda dense-only (bi-encoder) mide similitud semántica general pero no fine-grained entailment. SciFact requiere verificación de claims, donde el cross-encoder sobresale. Proyección conservadora: NDCG@10 de 0.78 → 0.82–0.87.
- **Vinculación con Criterios de Éxito**: CE-01 (top-1 ≥ 0.90), CE-03 (NDCG@10 ≥ 0.85), CE-04 (Recall@10 ≥ 0.90)
- **Prioridad**: **1** — Máxima. Es el habilitador principal de la mejora de precisión.

---

#### FT-V21-001.2: Búsqueda Semántica Pura (`search` sin `expandGraph`)

- **ID**: FT-V21-001.2
- **Título**: Recalibrar `search` para que sin `expandGraph` use solo pipeline semántico
- **Descripción**: Modificar el endpoint `POST /memories/search` para que, cuando `expandGraph=false` (o ausente), el comportamiento sea exclusivamente semántico: dense retrieval + reranker cross-encoder. Ningún resultado debe provenir de expansión de grafo. El grafo solo se activa cuando el consumidor lo solicita explícitamente con `expandGraph=true`.
- **Qué resuelve**: Elimina la ambigüedad actual donde `search` podía incluir mezcla de resultados semánticos y de grafo sin que el consumidor lo supiera. Clarifica la semántica del endpoint y permite al cliente controlar el comportamiento.
- **Vinculación con Criterios de Éxito**: CE-07 (`search` sin `expandGraph` = semantic puro)
- **Prioridad**: **2** — Alta. Es prerequisito para que el reranker (FT-V21-001.1) opere en un pipeline limpio.

---

#### FT-V21-001.3: Expansión de Grafo Robusta (top-3 + `entryPoints`)

- **ID**: FT-V21-001.3
- **Título**: Expandir grafo desde top-3 resultados con `entryPoints` configurables
- **Descripción**: Al activar `expandGraph`, expandir el grafo de conocimiento desde los 3 nodos más relevantes del dense retrieval (en lugar del mejor match único), recuperando vecinos hasta una profundidad configurable (`graphDepth`). Adicionalmente, permitir que el cliente especifique explícitamente los `entryPoints` (IDs de memorias) desde los cuales expandir, para casos donde el usuario conoce los nodos semilla correctos.
- **Qué resuelve**: La expansión desde un solo entry point limita la cobertura cross-dominio (ABM-MULTI-01: recall con grafo = 69.4%). Expandir desde múltiples orígenes mejora la cobertura a 85–92%. La opción `entryPoints` explícitos da control total al consumidor avanzado.
- **Vinculación con Criterios de Éxito**: CE-01 (top-1 ≥ 0.90, indirectamente vía mejor cobertura cross-dominio)
- **Prioridad**: **3** — Alta. Mejora la cobertura donde el grafo aplica.

---

#### FT-V21-001.4: Extracción de Entidades Funcional (`POST /extract` con OpenAI)

- **ID**: FT-V21-001.4
- **Título**: Reparar `POST /extract` para usar OpenAI real en lugar de MockLlmService
- **Descripción**: Reemplazar `MockLlmService` (extracción basada en regex que devuelve 400 vacío o patrones superficiales) por llamadas reales a OpenAI `gpt-4o-mini` para extracción semántica de entidades. El endpoint debe devolver entidades detectadas por IA: nombres de servidores, códigos de error, timestamps, entidades de dominio, etc., con tipos correctos y confianza.
- **Qué resuelve**: F8v2-ISS-001 documentó que `POST /extract` usaba MockLlmService con regex. UAT-S08 pasó pero con mock. Sin IA real, la extracción de entidades no es competitiva frente a Zep/Letta. Este fix es bloqueante para la precisión del producto.
- **Vinculación con Criterios de Éxito**: CE-06 (`POST /extract` funcional con OpenAI real)
- **Prioridad**: **6** — Media-Alta. Depende de la disponibilidad de la API key de OpenAI (D-02).

---

### EP-V21-002: Velocidad y Latencia

**Descripción**: Mejoras que reducen la latencia del pipeline de búsqueda, atacando los spikes observados en el monitoreo operativo (p95 ~2s en ciertas condiciones). Incluye preservación de optimización existente, caché de resultados costosos de grafo, diagnóstico y mitigación de cold starts/locks en Qdrant, y caché de validación JWT.

**Gap de v2.0.9 que ataca**: Latencia p95 oscilante (~2s en pipeline con grafo, cold start y locks de Qdrant), validación JWT por cada request sin caché.

**Meta de la épica**: p95 ≤ 500ms estable en búsqueda semántica (CE-02).

---

#### FT-V21-002.1: Optimización N+1 del Grafo + Cache de Resultados

- **ID**: FT-V21-002.1
- **Título**: Preservar optimización N+1 de queries de grafo y agregar caché por ID
- **Descripción**: Dos acciones complementarias: (a) verificar que la optimización N+1 de queries de grafo (evitar consultas individuales por relación, usando batch fetching) no se degrade con los cambios de v2.1, documentándola y monitoreándola; (b) implementar caché en memoria (Caffeine) para resultados de expansión de grafo: si dos queries comparten los mismos `entryPoints` y `graphDepth`, el resultado del BFS se sirve desde caché sin recalcular el subgrafo.
- **Qué resuelve**: La expansión de grafo es el componente más costoso del pipeline de búsqueda. La optimización N+1 existente redujo las consultas individuales; el caché elimina la recomputación del mismo subgrafo en queries repetidas. Juntos apuntan a la meta de p95 ≤ 500ms.
- **Vinculación con Criterios de Éxito**: CE-02 (p95 ≤ 500ms)
- **Prioridad**: **7** — Media. Depende de que FT-V21-001.3 (expansión top-3) esté implementada para saber qué granularidad cachear.
- **Nota**: En la Visión del Producto, esta feature aparece como dos items separados (V21-VEL-01 y V21-VEL-02). El usuario los propuso como un solo item. Se documentan unificados respetando la propuesta original.

---

#### FT-V21-002.2: Investigación y Mitigación de Cold Start / Lock en Qdrant

- **ID**: FT-V21-002.2
- **Título**: Diagnosticar y mitigar causas de latencia anómala en Qdrant
- **Descripción**: Investigar los escenarios que producen spikes de latencia a ~2s en el pipeline semántico: (a) cold start del índice de Qdrant tras despliegue o inactividad prolongada, (b) GC pauses en la JVM de Qdrant o en el cliente Quarkus, (c) locks durante escrituras concurrentes que bloquean lecturas, (d) contención en búsquedas con filtros compuestos (`must` + `must_not`). Aplicar mitigaciones: pre-calentamiento de segmentos al inicio, ajuste de `optimizers_config` en Qdrant, isolation de lecturas, tuning de JVM.
- **Qué resuelve**: El benchmark CE-04 midió 213ms en dense retrieval puro, pero el pipeline real con grafo muestra spikes a ~2s. La variabilidad es inaceptable para un motor de memoria en producción. Esta feature ataca la causa raíz de la inestabilidad.
- **Vinculación con Criterios de Éxito**: CE-02 (p95 ≤ 500ms)
- **Prioridad**: **8** — Media. Requiere investigación de causa raíz antes de aplicar mitigaciones. Puede requerir colaboración con DevOps para monitoreo de Qdrant.

---

#### FT-V21-002.3: Cache JWT en el Cliente por TTL

- **ID**: FT-V21-002.3
- **Título**: Cachear validación de JWT en backend con TTL igual al `exp` del token
- **Descripción**: Implementar caché en memoria para la validación de tokens JWT en el backend. Un token validado contra Keycloak se almacena con TTL igual al campo `exp` del JWT (típicamente 1 hora). Requests subsecuentes del mismo cliente con el mismo token se validan contra el caché sin llamar a Keycloak. El caché se invalida ante eventos de revocación (logout, cambio de roles) mediante notificaciones de Keycloak Admin Events.
- **Qué resuelve**: Cada request a la API v2 requiere validación JWT contra Keycloak. En benchmarks con 300+ queries, esta validación añade latencia acumulativa significativa (50–200ms por request según latencia de red a Keycloak). El caché elimina esta latencia para requests repetidos.
- **Vinculación con Criterios de Éxito**: CE-02 (p95 ≤ 500ms, contribución parcial)
- **Prioridad**: **4** — Alta. Impacto directo en latencia percibida por el cliente. Es una mejora de bajo riesgo y alto retorno.
- **Supuesto asociado**: S-06 — el cache JWT no debe introducir vulnerabilidades de seguridad por tokens revocados aún en caché.

---

### EP-V21-003: Eficiencia Operativa

**Descripción**: Mejoras que reducen la deuda operativa y la complejidad de despliegue, eliminando componentes innecesarios o unificando recursos duplicados. Incluye diagnóstico del worker inactivo, unificación de colecciones Qdrant, y exposición de estrategia de entrada al grafo como parámetro configurable.

**Gap de v2.0.9 que ataca**: Worker con Claimed=0 (recursos desperdiciados), dos colecciones Qdrant coexistiendo (overhead de mantenimiento y backups), `graphEntryStrategy` hardcodeada.

**Meta de la épica**: Una sola colección Qdrant en producción (CE-05), worker diagnosticado/resuelto, estrategia de grafo configurable por perfil de dominio.

---

#### FT-V21-003.1: Diagnóstico del Worker de Procesamiento (Claimed = 0)

- **ID**: FT-V21-003.1
- **Título**: Diagnosticar worker inactivo y eliminar o unificar procesamiento
- **Descripción**: El worker de procesamiento asíncrono (encargado de generar embeddings y entidades post-ingesta) reporta `Claimed = 0` — no está procesando trabajo. Diagnosticar la causa raíz: (a) conexión a la cola de mensajes rota, (b) configuración de polling incorrecta, (c) worker innecesario porque el procesamiento ya es síncrono. Si el procesamiento puede ser síncrono sin degradar la latencia de los endpoints de ingesta, eliminar el worker. Si el worker es necesario pero está roto, repararlo. En cualquier caso, debe haber un solo mecanismo de procesamiento claro.
- **Qué resuelve**: Workers inactivos consumen recursos (CPU, memoria, conexiones), añaden complejidad de despliegue, y oscurecen el diagnóstico de fallos en la ingesta. Un worker con Claimed=0 es deuda operativa pura.
- **Vinculación con Criterios de Éxito**: CE-02 (indirectamente, reduce superficie de fallos que afectan latencia)
- **Prioridad**: **9** — Media. Requiere diagnóstico de causa raíz. Puede resolverse con eliminación simple si el worker es innecesario.
- **Supuesto asociado**: S-03 — el procesamiento puede ser síncrono sin degradar latencia de ingesta.

---

#### FT-V21-003.2: Unificación de Colecciones Qdrant (Eliminar `abax-memories-v1`)

- **ID**: FT-V21-003.2
- **Título**: Migrar todo a `abax-memories-v2` y eliminar colección v1 obsoleta
- **Descripción**: Consolidar las dos colecciones Qdrant (`abax-memories-v1` y `abax-memories-v2`) en una sola. Verificar que la colección v1 solo contiene datos residuales de v1.0.0 sin uso funcional (F8v2-ISS-002). Migrar cualquier punto residual necesario y eliminar la colección obsoleta del cluster de producción. El resultado debe ser una sola colección activa.
- **Qué resuelve**: Dos colecciones duplican overhead de mantenimiento (backups, monitoreo, optimización de índices), ocupan memoria en el cluster Qdrant, y añaden confusión operativa. La colección v1 ya no tiene propósito funcional desde el cierre de v1.0.0.
- **Vinculación con Criterios de Éxito**: CE-05 (exactamente 1 colección Qdrant activa en producción)
- **Prioridad**: **5** — Alta. Deuda operativa que crece con el tiempo. Impacta la meta medible CE-05.
- **Supuesto asociado**: S-02 — la colección v1 solo tiene datos residuales sin uso activo. Si contiene datos necesarios, se requiere migración previa.

---

#### FT-V21-003.3: `graphEntryStrategy` Configurable

- **ID**: FT-V21-003.3
- **Título**: Exponer estrategia de entrada al grafo como parámetro configurable
- **Descripción**: Parametrizar la estrategia de entrada al grafo de conocimiento, permitiendo elegir entre: `single-best` (un solo entry point, comportamiento actual de v2.0.9), `top-k` (k entry points configurables, default 3), o `threshold` (todos los resultados del dense retrieval con score ≥ umbral configurable). La estrategia se configura a nivel de perfil de dominio o a nivel de request individual vía el header `X-Graph-Strategy` (FT-V21-004.1).
- **Qué resuelve**: En v2.0.9, la expansión de grafo siempre parte del mejor match único (hardcodeado). Esto limita la cobertura cross-dominio (ABM-MULTI-01: recall 69.4%). Hacer la estrategia configurable permite a cada perfil de dominio optimizar su comportamiento: dominios con entidades muy relacionadas se benefician de `top-k`; dominios con relaciones ruidosas prefieren `single-best` o `threshold` alto.
- **Vinculación con Criterios de Éxito**: CE-01 (top-1 ≥ 0.90, indirectamente), CE-09 (`X-Graph-Strategy` funcional)
- **Prioridad**: **10** — Media. Habilitador para FT-V21-004.1 (header `X-Graph-Strategy`) y FT-V21-001.3 (expansión top-3).
- **Nota**: Esta feature define la configurabilidad a nivel de backend. El control por request del cliente corresponde a FT-V21-004.1.

---

### EP-V21-004: API y Developer Experience

**Descripción**: Mejoras que simplifican, unifican y extienden la API v2 para mejorar la experiencia del desarrollador consumidor. Incluye nuevo header para control granular del grafo, unificación de endpoints redundantes con deprecación del legacy, y nuevo endpoint administrativo para eliminación de namespaces completos.

**Gap de v2.0.9 que ataca**: API con endpoints redundantes (`search`/`hybrid`), sin control de grafo por request, sin endpoint para limpiar namespaces completos.

**Meta de la épica**: API unificada con un solo endpoint de búsqueda, header `X-Graph-Strategy` funcional, `DELETE /admin/namespaces/{name}` operativo (CE-08, CE-09, CE-10).

---

#### FT-V21-004.1: Header `X-Graph-Strategy`

- **ID**: FT-V21-004.1
- **Título**: Agregar header HTTP `X-Graph-Strategy` para control de expansión por request
- **Descripción**: Nuevo header HTTP `X-Graph-Strategy` que permite al cliente especificar la estrategia de expansión de grafo por request. Valores soportados: `none` (sin expansión de grafo), `single` (mejor match como entry point), `top-k` (k entry points, requiere parámetro complementario `X-Graph-K`), `threshold` (score mínimo para entry points, requiere `X-Graph-Threshold`). El header es opcional; si no se envía, se usa el default configurado en el perfil de dominio (FT-V21-003.3). Debe ser respetado por el endpoint `POST /memories/search`.
- **Qué resuelve**: En v2.0.9, la decisión de expandir el grafo (y cómo) está hardcodeada en el backend. El consumidor no tiene control granular. Este header devuelve el control al cliente, permitiendo que agentes, aplicaciones y SDKs decidan la estrategia óptima según su caso de uso.
- **Vinculación con Criterios de Éxito**: CE-09 (header `X-Graph-Strategy` funcional para `none`, `single`, `top-k`)
- **Prioridad**: **11** — Media. Depende de FT-V21-003.3 (`graphEntryStrategy` configurable) para que el backend entienda las estrategias.

---

#### FT-V21-004.2: Unificación de Endpoints `search` y `hybrid`

- **ID**: FT-V21-004.2
- **Título**: Unificar `search`/`hybrid` en un solo endpoint con parámetros explícitos
- **Descripción**: Fusionar los endpoints `POST /memories/search` y `POST /memories/hybrid` en un solo endpoint `POST /memories/search` con parámetros explícitos: `semanticWeight` (0.0–1.0, peso del vector), `lexicalWeight` (0.0–1.0, peso de keyword), `expandGraph` (boolean), `graphStrategy` (string, mismo valor que `X-Graph-Strategy`). El endpoint legacy `POST /memories/hybrid` se mantiene funcional pero devuelve header `Deprecation: true` y `Warning: 299 - "POST /memories/hybrid is deprecated. Use POST /memories/search with semanticWeight and lexicalWeight parameters."`.
- **Qué resuelve**: Dos endpoints con semántica solapada confunden a los consumidores (¿cuál usar? ¿son intercambiables?). La documentación OpenAPI muestra parámetros duplicados. La unificación simplifica la API y reduce la superficie de mantenimiento.
- **Vinculación con Criterios de Éxito**: CE-10 (un solo endpoint funcional + `hybrid` con deprecated warning)
- **Prioridad**: **12** — Media. Debe mantener backward compatibility (restricción R-02). El endpoint `hybrid` legacy debe seguir funcional con warning.
- **Restricción asociada**: R-02 — backward compatibility de la API v2. No se permite breaking change.

---

#### FT-V21-004.3: `DELETE /admin/namespaces/{name}`

- **ID**: FT-V21-004.3
- **Título**: Endpoint administrativo para eliminar namespaces completos
- **Descripción**: Nuevo endpoint `DELETE /admin/namespaces/{name}` que elimina atómicamente todos los recursos asociados a un namespace: memorias, relaciones, entidades, puntos vectoriales en Qdrant. Requiere rol `memory-admin`. La operación debe ser atómica (todo o nada) y debe retornar 200 con resumen de recursos eliminados, o 404 si el namespace no existe.
- **Qué resuelve**: En v2.0.9, no existe forma de limpiar un namespace completo. Durante los benchmarks, se requirieron tenants efímeros y la limpieza manual (borrar memorias una por una, limpiar Qdrant por separado) fue frágil, lenta y propensa a residuos. Este endpoint es esencial para escenarios de prueba, benchmarks y administración multi-tenant.
- **Vinculación con Criterios de Éxito**: CE-08 (`DELETE /admin/namespaces/{name}` operativo con eliminación completa)
- **Prioridad**: **13** — Media. Utilidad administrativa importante pero no bloqueante para las metas de precisión/velocidad.

---

## Matriz Resumen

| Épica | ID | Feature | Prioridad | CEs vinculados | Gap v2.0.9 |
|---|---|---|---|---|---|
| **EP-V21-001: Precisión** | FT-V21-001.1 | Reranker Cross-Encoder | 1 | CE-01, CE-03, CE-04 | CE-01 NDCG@10 = 0.7771 (FAIL) |
| | FT-V21-001.2 | Búsqueda Semántica Pura (`search` sin grafo) | 2 | CE-07 | API/DX confusa (`search`/`hybrid`) |
| | FT-V21-001.3 | Expansión de Grafo Top-3 + `entryPoints` | 3 | CE-01 | ABM-MULTI-01 recall 69.4% |
| | FT-V21-001.4 | Fix `POST /extract` (OpenAI real) | 6 | CE-06 | F8v2-ISS-001 MockLlmService |
| **EP-V21-002: Velocidad** | FT-V21-002.1 | Optimización N+1 + Cache de Grafo | 7 | CE-02 | Latencia p95 ~2s oscilante |
| | FT-V21-002.2 | Cold Start / Lock Qdrant | 8 | CE-02 | Spikes de latencia p95 |
| | FT-V21-002.3 | Cache JWT Cliente | 4 | CE-02 | Latencia acumulativa en 300+ queries |
| **EP-V21-003: Eficiencia** | FT-V21-003.1 | Diagnóstico Worker (Claimed=0) | 9 | CE-02 (indirecto) | Workers inactivos consumen recursos |
| | FT-V21-003.2 | Unificar Colecciones Qdrant | 5 | CE-05 | 2 colecciones (`v1` + `v2`) |
| | FT-V21-003.3 | `graphEntryStrategy` Configurable | 10 | CE-01, CE-09 | Estrategia hardcodeada (single-best) |
| **EP-V21-004: API/DX** | FT-V21-004.1 | Header `X-Graph-Strategy` | 11 | CE-09 | Sin control granular por request |
| | FT-V21-004.2 | Unificar `search`/`hybrid` | 12 | CE-10 | Endpoints redundantes |
| | FT-V21-004.3 | `DELETE /admin/namespaces/{name}` | 13 | CE-08 | Sin endpoint de limpieza |

---

## Matriz de Trazabilidad — Features a Criterios de Éxito

| Feature | CE-01 Top-1 | CE-02 p95 | CE-03 NDCG | CE-04 Recall | CE-05 1 Col. | CE-06 /extract | CE-07 Search | CE-08 DELETE ns | CE-09 X-Graph | CE-10 Unif. |
|---|---|---|---|---|---|---|---|---|---|---|
| FT-V21-001.1 Reranker | **X** | | **X** | **X** | | | | | | |
| FT-V21-001.2 Search puro | | | | | | | **X** | | | |
| FT-V21-001.3 Grafo top-3 | **X** | | | | | | | | | |
| FT-V21-001.4 Fix /extract | | | | | | **X** | | | | |
| FT-V21-002.1 N+1 + Cache | | **X** | | | | | | | | |
| FT-V21-002.2 Cold start Qdrant | | **X** | | | | | | | | |
| FT-V21-002.3 Cache JWT | | **X** | | | | | | | | |
| FT-V21-003.1 Worker | | **X** | | | | | | | | |
| FT-V21-003.2 Unificar Qdrant | | | | | **X** | | | | | |
| FT-V21-003.3 graphEntryStrat | **X** | | | | | | | | **X** | |
| FT-V21-004.1 X-Graph-Strategy | | | | | | | | | **X** | |
| FT-V21-004.2 Unificar search | | | | | | | | | | **X** |
| FT-V21-004.3 DELETE ns | | | | | | | | **X** | | |

**Cobertura**: 10 de 10 criterios de éxito tienen al menos una feature vinculada. 3 features contribuyen a CE-02 (p95 ≤ 500ms). CE-01 (top-1 ≥ 0.90) es atacado por 3 features.

---

## Priorización Global

Según lo indicado por el usuario, la prioridad de implementación es:

| # | Feature | Épica | Justificación |
|---|---|---|---|
| **1** | FT-V21-001.1 — Reranker Cross-Encoder | Precisión | Principal habilitador de mejora de precisión. Ataca el único benchmark fallido. |
| **2** | FT-V21-001.2 — Búsqueda Semántica Pura | Precisión | Prerrequisito para que el reranker opere en pipeline limpio. Clarifica API. |
| **3** | FT-V21-001.3 — Expansión Grafo Top-3 | Precisión | Mejora cobertura cross-dominio. Complementa al reranker. |
| **4** | FT-V21-002.3 — Cache JWT | Velocidad | Bajo riesgo, alto retorno. Impacto directo en latencia percibida. |
| **5** | FT-V21-003.2 — Unificar Colecciones Qdrant | Eficiencia | Deuda operativa creciente. Meta medible CE-05. |
| **6+** | Resto de features según categoría | — | Precisión > Velocidad > Eficiencia > API/DX |

---

## Nota sobre features de la Visión excluidas de este documento

La Visión del Producto (`vision-producto.md`) contiene **16 items**, mientras que este documento contiene **13 features**. Las diferencias son:

| Item en Visión | ID en Visión | ¿En este documento? | Motivo de exclusión |
|---|---|---|---|
| Compresión de payload en Qdrant | V21-EFI-04 | **No** | El usuario no lo mencionó en su propuesta original. Es una adición del BA no validada explícitamente por el sponsor. |
| Rate limiting por API key | V21-API-04 | **No** | El usuario no lo mencionó en su propuesta original. CE-12 fue PARTIAL en v2.0.9 pero no es parte del alcance explícito de v2.1.0. |
| Optimización N+1 (separado) | V21-VEL-01 | Unificado en FT-V21-002.1 | El usuario listó N+1 + cache de grafo como un solo item. El BA los separó en la Visión por granularidad de criterios de aceptación. |

Si el sponsor desea incluir compresión de payload o rate limiting en v2.1.0, debe solicitarse mediante **control de cambios formal** con evaluación de impacto.

---

## Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10. Meta v2.1.0: ≥ 0.85 en SciFact.
- **Cross-encoder**: Modelo de reranking que procesa pares (query, documento) simultáneamente para calcular relevancia fina. Más costoso pero más preciso que el dense retrieval (bi-encoder).
- **p95**: Percentil 95 — valor de latencia por debajo del cual se completa el 95% de las solicitudes. Meta v2.1.0: ≤ 500ms estable.
- **Qdrant**: Base de datos vectorial open-source usada para almacenar embeddings y búsqueda semántica por similitud de coseno.
- **MockLlmService**: Servicio simulado que usa regex para extracción de entidades en lugar de IA real (OpenAI). Debe reemplazarse en `POST /extract`.
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación. Abax-Memory valida JWTs contra Keycloak en cada request.
- **DX**: Developer Experience — calidad de la experiencia del desarrollador al consumir una API. Incluye claridad, consistencia y control granular.
