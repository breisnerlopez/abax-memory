---
fase: 2 — Análisis Funcional (v2.0.0)
entregable: Criterios de Aceptación
responsable: business-analyst
fecha: 2026-05-03
release: v2.0.0
estado: Completado
fuentes:
  - docs/entregables/v2/fase-0-descubrimiento/historias-usuario.md
  - docs/entregables/v2/fase-0-descubrimiento/vision-producto.md
  - docs/entregables/v2/fase-2-analisis/especificacion-funcional.md
---

# Criterios de Aceptación — Abax-Memory v2.0.0

## Tabla de Contenidos

- [1. Criterios de Aceptación por Épica](#1-criterios-de-aceptación-por-épica)
  - [1.1 EP-001: Motor de Memoria Genérico](#11-ep-001-motor-de-memoria-genérico)
  - [1.2 EP-002: Perfiles de Dominio](#12-ep-002-perfiles-de-dominio)
  - [1.3 EP-003: Scoping Multi-Tenant](#13-ep-003-scoping-multi-tenant)
  - [1.4 EP-004: API REST v2](#14-ep-004-api-rest-v2)
  - [1.5 EP-005: Búsqueda Semántica + Graph](#15-ep-005-búsqueda-semántica--graph)
  - [1.6 EP-006: Gobernanza y Trazabilidad](#16-ep-006-gobernanza-y-trazabilidad)
  - [1.7 EP-009: Frontend Multi-Dominio](#17-ep-009-frontend-multi-dominio)
- [2. Criterios de Aceptación del Sistema](#2-criterios-de-aceptación-del-sistema)
- [3. Criterios de Aceptación No Funcionales](#3-criterios-de-aceptación-no-funcionales)
- [4. Matriz de Trazabilidad](#4-matriz-de-trazabilidad)
  - [4.1 Trazabilidad AC → HU → Feature → Épica](#41-trazabilidad-ac--hu--feature--épica)
  - [4.2 Trazabilidad Criterio de Éxito → AC Sistema](#42-trazabilidad-criterio-de-éxito--ac-sistema)
- [5. Checklist de Verificación QA](#5-checklist-de-verificación-qa)
- [6. Glosario](#6-glosario)

---

## Introducción

Este documento consolida, refina y organiza todos los criterios de aceptación de Abax-Memory v2.0.0. Se construye a partir de 69 historias de usuario (Given/When/Then), 13 criterios de éxito (CE-01 a CE-13), 20 reglas de negocio (BR-001 a BR-020) y la especificación funcional completa.

**Propósito**: Proveer a QA y al equipo de desarrollo una lista verificable y trazable de todo lo que el sistema debe cumplir para ser aceptado.

**Alcance**: Cubre las 7 épicas Must (EP-001 a EP-006, EP-009), 63 features y 69 historias de usuario del MVP v2.0.0. Las épicas Should/Could (EP-007, EP-008, EP-010) están fuera del MVP.

---

## 1. Criterios de Aceptación por Épica

Cada criterio de aceptación se identifica con un código único (`AC-ENN-MM`), está redactado en formato verificable, y referencia las historias de usuario y reglas de negocio que lo originan.

### 1.1 EP-001: Motor de Memoria Genérico

#### AC-E1-01 — Clasificación con kind universal

**Origen**: HU-001.01.1, BR-010  
**Criterio**: El sistema acepta exclusivamente los 8 valores del enum `kind` (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`) en toda operación de creación. Cualquier valor fuera de este conjunto es rechazado con `HTTP 400` y código `VALIDATION_ERROR`. El campo `kind` es inmutable tras la creación (BR-011).

**Verificación**:
- Crear una memoria con cada uno de los 8 kinds → `HTTP 201` para todos.
- Crear una memoria con `kind = "bug"` → `HTTP 400` + `VALIDATION_ERROR`.
- Intentar modificar `kind` vía `PATCH` → `HTTP 400`.

---

#### AC-E1-02 — Búsqueda con filtro por kinds

**Origen**: HU-001.01.1  
**Criterio**: El filtro `kinds` en `POST /memories/search` restringe los resultados exclusivamente a los kinds especificados. Si se especifica `kinds: ["fact", "decision"]`, ninguna memoria con `kind = "event"` u otro aparece en resultados.

**Verificación**:
- Crear 3 memorias con kinds distintos. Buscar con `kinds: ["fact"]` → solo aparece la de kind `fact`.
- Buscar con `kinds: []` (array vacío) → el sistema acepta y aplica el filtro vacío (0 resultados) o ignora el filtro.

---

#### AC-E1-03 — Transiciones de estado permitidas

**Origen**: HU-001.02.1, HU-001.02.2, BR-005  
**Criterio**: La máquina de estados solo permite las transiciones definidas en BR-005:
- `draft → pending` (submit)
- `pending → active` (approve)
- `pending → rejected` (reject)
- `rejected → draft` (resubmit)
- `active → archived` (archive)
- Cualquier estado → `deleted` (soft-delete)

Toda transición no listada —incluyendo `active → draft`, `archived → active`, `rejected → active`, `deleted → cualquier estado`— es rechazada con `HTTP 422` y código `UNPROCESSABLE_ENTITY`.

**Verificación**:
- Ejecutar cada transición permitida → `HTTP 200`.
- Intentar `active → draft` → `HTTP 422`.
- Intentar `archived → active` → `HTTP 422`.
- Intentar `rejected → active` → `HTTP 422`.
- Ejecutar soft-delete desde cada uno de los 6 estados → `HTTP 204` en todos.

---

#### AC-E1-04 — Rechazo de memoria con motivo registrado

**Origen**: HU-001.02.2  
**Criterio**: Al rechazar una memoria (`pending → rejected`), el sistema registra `reviewedBy`, `reviewedAt` y `reviewComment` (motivo del rechazo). El creador original puede consultar el motivo del rechazo en el campo `reviewComment` del ciclo de vida.

**Verificación**:
- Rechazar una memoria en `pending` con `reviewComment: "Falta evidencia"` → la memoria transiciona a `rejected` y el `reviewComment` se almacena.
- Consultar `GET /memories/{id}` como creador → `reviewComment` visible.

---

#### AC-E1-05 — Creación de relaciones tipadas

**Origen**: HU-001.03.1, BR-007, BR-014, BR-015  
**Criterio**: El sistema permite crear relaciones con cualquiera de los 9 tipos (`related_to`, `depends_on`, `caused_by`, `resolves`, `contradicts`, `supports`, `mentions`, `belongs_to`, `supersedes`). Se rechazan relaciones con `targetId` inexistente (`HTTP 404 TARGET_NOT_FOUND`), `targetId` en estado `deleted` (`HTTP 422`), relaciones duplicadas (`HTTP 409`), y auto-relaciones (`HTTP 422`).

**Verificación**:
- Crear relación entre dos memorias activas con cada uno de los 9 tipos → `HTTP 201` para todos.
- Crear relación hacia `targetId` inexistente → `HTTP 404`.
- Crear relación hacia memoria `deleted` → `HTTP 422`.
- Crear misma relación (mismo source+target+type) dos veces → `HTTP 409`.
- Crear relación con `sourceId == targetId` → `HTTP 422`.

---

#### AC-E1-06 — Eliminación de relaciones con trazabilidad

**Origen**: HU-001.03.2, FT-006.08  
**Criterio**: Al eliminar una relación, el sistema la borra definitivamente, retorna `HTTP 204`, y registra en auditoría quién eliminó, cuándo y los detalles de la relación (sourceId, targetId, type). Consultas posteriores al grafo no incluyen la relación eliminada.

**Verificación**:
- Eliminar relación existente → `HTTP 204`. Consultar grafo → la relación no aparece.
- Verificar registro de auditoría con `action: "delete_relation"`.

---

#### AC-E1-07 — Extracción de entidades de texto sin persistir

**Origen**: HU-001.04.1  
**Criterio**: `POST /api/v2/memories/extract` recibe `{"content": "<texto>"}`, analiza el texto y retorna un array de entidades detectadas con su nombre y tipo. No persiste nada en el repositorio. Si el texto no contiene entidades reconocibles, retorna `[]` con `HTTP 200`. Si `content` es vacío o nulo, retorna `HTTP 400`.

**Verificación**:
- Enviar texto con entidades conocidas → array poblado con nombres y tipos.
- Enviar texto sin entidades → `[]` con `HTTP 200`.
- Enviar `content: ""` → `HTTP 400 VALIDATION_ERROR`.

---

#### AC-E1-08 — Búsqueda de entidades y memorias vinculadas

**Origen**: HU-001.04.2  
**Criterio**: `GET /api/v2/entities?q=<término>` retorna entidades que coinciden parcialmente con el término de búsqueda, cada una con `memoryCount`. `GET /api/v2/entities/{name}` retorna detalle de la entidad con `memoryCount` y array de `memoryIds` vinculados (incluyendo `kind` y `summary`). Entidad inexistente → array vacío con `HTTP 200`.

**Verificación**:
- Buscar entidad existente por nombre parcial → resultados con `memoryCount > 0`.
- Consultar detalle de entidad → `memoryIds` poblados con `kind` y `summary`.
- Buscar entidad inexistente → array vacío con `HTTP 200`.

---

#### AC-E1-09 — Metadatos extensibles key-value

**Origen**: HU-001.05.1  
**Criterio**: El campo `metadata` acepta un objeto key-value libre. Se persiste intacto y se retorna en consultas de detalle. No afecta el ranking semántico. La actualización mediante `PATCH` reemplaza el objeto completo (no hace merge parcial).

**Verificación**:
- Crear memoria con `metadata: {key1: "val1", key2: "val2"}` → se persiste y retorna correctamente.
- Actualizar `metadata` con `PATCH` → el objeto anterior se reemplaza completamente.
- Verificar que `metadata` no altera scores de búsqueda semántica.

---

#### AC-E1-10 — Registro de source tipado

**Origen**: HU-001.06.1  
**Criterio**: El campo `source` acepta `type` con uno de 6 valores (`conversation`, `document`, `api`, `workflow`, `manual`, `case`) e `id` externo libre. `source` es opcional; si no se especifica, queda `null`. Valores de `source.type` fuera del enum → `HTTP 400`.

**Verificación**:
- Crear memoria con `source: {type: "conversation", id: "chat-4423"}` → se persiste.
- Crear memoria sin `source` → se acepta, campo queda `null`.
- Usar `source.type = "email"` → `HTTP 400 VALIDATION_ERROR`.

---

#### AC-E1-11 — Soft-delete con preservación de trazabilidad

**Origen**: HU-001.07.1, BR-002  
**Criterio**: `DELETE /api/v2/memories/{id}` marca `lifecycle.status = deleted`, NO elimina físicamente de PostgreSQL ni Qdrant. La memoria desaparece de búsquedas estándar. Se registra auditoría con `action: "delete"`, `userId`, `timestamp`. El vector en Qdrant se preserva.

**Verificación**:
- Soft-deletear una memoria activa → `HTTP 204`. Buscar sin filtro → no aparece.
- Consultar directamente PostgreSQL → el registro existe con `status = 'deleted'`.
- Verificar registro de auditoría con `action: "delete"`.

---

#### AC-E1-12 — Versionado mediante supersedes

**Origen**: HU-001.08.1  
**Criterio**: Una memoria nueva puede declarar que reemplaza una anterior mediante la relación `supersedes`. La memoria anterior NO cambia de estado automáticamente. El re-ranking favorece la versión más reciente. Si la versión anterior se archiva manualmente, la nueva queda como única activa visible.

**Verificación**:
- Crear MEM-002 con relación `supersedes` hacia MEM-001 → MEM-001 sigue en `active`.
- Buscar contenido donde ambas versiones son relevantes → MEM-002 aparece primero.
- Archivar MEM-001 manualmente → solo MEM-002 visible en búsqueda por defecto.

---

#### AC-E1-13 — Modelo de confidence con validación de rango

**Origen**: HU-001.09.1, BR-017  
**Criterio**: `lifecycle.confidence` acepta valores en [0.0, 1.0]. Si no se especifica, aplica el default del perfil activo o `0.5` sin perfil (BR-009). Valores fuera de rango → `HTTP 400`. Búsquedas pueden filtrar por `confidence: {gte: N}`.

**Verificación**:
- Crear memoria con `confidence: 0.95` → se persiste correctamente.
- Crear memoria con `confidence: 1.5` → `HTTP 400 VALIDATION_ERROR`.
- Buscar con `confidence: {gte: 0.9}` → solo memorias con `confidence >= 0.9`.
- Crear sin especificar `confidence` → se asigna default del perfil o `0.5`.

---

#### AC-E1-14 — Rechazo de campos desconocidos en creación

**Origen**: HU-004.01.1, BR-013  
**Criterio**: `POST /api/v2/memories` rechaza cualquier campo no definido en el schema del modelo v2 con `HTTP 400` y código `INVALID_REQUEST_BODY` (modo estricto).

**Verificación**:
- Enviar `{"kind": "fact", "content": "...", "scope": {...}, "color": "red"}` → `HTTP 400`.

---

#### AC-E1-15 — Contenido no vacío

**Origen**: HU-004.01.1, BR-019  
**Criterio**: `content` es obligatorio y no puede ser string vacío o solo whitespace.

**Verificación**:
- Crear con `content: ""` → `HTTP 400`.
- Crear con `content: "   "` → `HTTP 400`.

---

### 1.2 EP-002: Perfiles de Dominio

#### AC-E2-01 — Definición de perfil como configuración

**Origen**: HU-002.01.1  
**Criterio**: Un System Operator puede definir un perfil de dominio mediante configuración (JSON, YAML o registro en BD) especificando `name` (obligatorio), `recommendedKinds`, `suggestedTags`, `suggestedTopics`, `extraMetadataFields`, `defaultImportance`, `defaultConfidence`, `defaultSensitivity`. El perfil queda disponible para activación inmediata sin deploy.

**Verificación**:
- Crear perfil "legal" con `recommendedKinds: ["entity", "decision", "note"]` → persiste y está disponible.
- Intentar crear perfil sin `name` → `HTTP 400`.

---

#### AC-E2-02 — Herencia completa del core genérico

**Origen**: HU-002.02.1  
**Criterio**: Todo perfil hereda automáticamente los 8 kinds, 6 estados, 9 tipos de relación y 4 niveles de sensibilidad del core. Un perfil NUNCA restringe las capacidades base. Si un perfil recomienda solo 3 kinds, los 8 siguen estando disponibles.

**Verificación**:
- Con perfil "legal" activo (recommendedKinds: 3), crear memoria con `kind = "procedure"` → aceptada.
- Con perfil "ops" activo, crear relación con `type = "contradicts"` (no mencionado en el perfil) → aceptada.
- Con perfil activo que define `defaultSensitivity: "confidential"`, crear con `sensitivity: "public"` explícito → respeta "public".

---

#### AC-E2-03 — Perfil Ops: kinds, tags y metadatos

**Origen**: HU-002.03.1  
**Criterio**: Al activar el perfil Ops, el sistema expone `event` y `procedure` como kinds principales, sugiere tags `incident`, `runbook`, `alert`, `maintenance`, `postmortem`, y despliega campos de metadatos extra `affectedService`, `remediationSteps`, `rootCause`. El `defaultImportance` es `0.7`.

**Verificación**:
- Activar perfil Ops → kinds destacados son `event` y `procedure`.
- Crear memoria sin `importance` → se asigna `0.7`.
- Campos de metadatos extra visibles en formulario y API.

---

#### AC-E2-04 — Perfil Agent: kinds conversacionales y scoping

**Origen**: HU-002.04.1  
**Criterio**: Al activar el perfil Agent, los kinds principales son `fact`, `preference`, `event`, `decision`. El scoping por `userId` y `sessionId` es prominente. Resultados de búsqueda se ordenan por `importance` descendente. `defaultSensitivity` es `confidential`.

**Verificación**:
- Activar perfil Agent → kinds principales visibles.
- Crear memoria con `scope.userId` y `scope.sessionId` → se persiste y se recupera filtrando por esos campos.
- Buscar memorias de `user-42` → ordenadas por `importance` descendente.

---

#### AC-E2-05 — Perfil Business: kinds corporativos y metadatos

**Origen**: HU-002.05.1  
**Criterio**: Al activar el perfil Business, los kinds principales son `entity`, `decision`, `note`, `task`. Sugiere tags `client`, `contract`, `meeting`, `opportunity`. Expone metadatos extra `clientName`, `contractId`, `opportunityValue`. `defaultSensitivity` es `internal`.

**Verificación**:
- Activar perfil Business → kinds principales visibles.
- Crear memoria con `kind = "note"` y relación `belongs_to` hacia una entidad → grafo refleja la pertenencia.
- Crear memoria sin `sensitivity` → se asigna `internal`.

---

#### AC-E2-06 — Aplicación de defaults según perfil

**Origen**: HU-002.06.1, BR-009  
**Criterio**: Al crear una memoria sin especificar `importance`, `confidence` o `sensitivity`, el sistema aplica los defaults del perfil activo. Sin perfil: `importance = 0.5`, `confidence = 0.5`, `sensitivity = "internal"`. Si el usuario especifica explícitamente un valor, el default no se aplica.

**Verificación**:
- Perfil Ops activo, crear sin `importance` → se asigna `0.7`.
- Sin perfil, crear sin `importance` → se asigna `0.5`.
- Perfil Business activo, crear con `sensitivity: "public"` explícito → se respeta `"public"`.

---

#### AC-E2-07 — Vocabulario controlado por perfil

**Origen**: HU-002.07.1  
**Criterio**: Cada perfil sugiere tags y topics predefinidos. El usuario puede usarlos o definir los propios. Las búsquedas por tag funcionan independientemente del perfil activo al momento de la búsqueda.

**Verificación**:
- Perfil Ops activo → sugiere `incident`, `runbook`, `alert`, `maintenance`, `postmortem`.
- Usar tag sugerido → se persiste. Buscar por ese tag desde otro perfil → se encuentra.

---

#### AC-E2-08 — Extensibilidad: nuevo perfil sin modificar core

**Origen**: HU-002.08.1  
**Criterio**: Un System Operator puede definir un nuevo perfil (ej. "healthcare") como configuración adicional sin modificar el código del core ni la API base. El perfil queda disponible inmediatamente sin deploy. La API base (`/api/v2/memories`) sigue funcionando exactamente igual. El core no restringe kinds por perfil.

**Verificación**:
- Crear perfil "healthcare" con configuración JSON → disponible sin deploy.
- Con perfil "healthcare" activo, todos los endpoints de API v2 siguen disponibles sin cambios.
- Crear memoria con kind no recomendado por el perfil → aceptada.

---

### 1.3 EP-003: Scoping Multi-Tenant

#### AC-E3-01 — Aislamiento estricto por tenantId

**Origen**: HU-003.01.1, BR-004  
**Criterio**: Un usuario del tenant A nunca ve memorias del tenant B. Si intenta acceder a una memoria de otro tenant por `memoryId`, recibe `HTTP 404` (sin revelar su existencia). Tests de seguridad confirman 0% de fuga cross-tenant.

**Verificación**:
- Crear memorias en tenant-A y tenant-B. Buscar con token de tenant-A → solo resultados de tenant-A.
- Usar token de tenant-A para `GET /memories/{id-de-tenant-B}` → `HTTP 404`.
- Suite de 100+ queries cross-tenant → 100% retornan 0 resultados del tenant equivocado.

---

#### AC-E3-02 — Scoping por userId

**Origen**: HU-003.02.1  
**Criterio**: Las memorias pueden asociarse a `scope.userId`. Búsquedas con filtro `scopes.userId` retornan solo las memorias de ese usuario. Memorias sin `userId` no aparecen en búsquedas filtradas por `userId`.

**Verificación**:
- Crear 3 memorias para user-42 y 2 sin userId. Buscar con `scopes.userId = "user-42"` → solo 3 resultados.
- Buscar sin filtro de userId → todas las memorias del tenant son elegibles.

---

#### AC-E3-03 — Scoping por sessionId

**Origen**: HU-003.03.1  
**Criterio**: Las memorias pueden asociarse a `scope.sessionId`. Búsquedas filtradas por `sessionId` aíslan correctamente el contexto. Las memorias sobreviven al cierre de sesión y pueden recuperarse posteriormente.

**Verificación**:
- Crear 10 memorias en sesión sess-abc, 5 en sess-xyz. Buscar por sess-abc → solo 10 resultados.
- Finalizar sesión, buscar por `sessionId` más tarde → las memorias se preservan.

---

#### AC-E3-04 — Namespace como subdivisión

**Origen**: HU-003.04.1  
**Criterio**: `scope.namespace` permite subdividir memorias dentro de un tenant. Búsquedas por `namespace` retornan solo las de ese namespace. Memorias sin `namespace` son elegibles en búsquedas sin filtro de namespace.

**Verificación**:
- Crear memorias con `namespace = "project-alpha"` y sin namespace. Buscar por "project-alpha" → solo las de ese namespace.
- Buscar sin filtro de namespace → todas aparecen.

---

#### AC-E3-05 — Cross-tenant access para memory-admin

**Origen**: HU-003.05.1  
**Criterio**: Un `memory-admin` con permisos explícitos puede consultar y operar cross-tenant. Toda operación cross-tenant se registra en auditoría con los `tenantIds` afectados. Un usuario sin rol `memory-admin` no puede ampliar su scope.

**Verificación**:
- Admin busca sin restricción de tenant → resultados de todos los tenants.
- Admin archiva una memoria cross-tenant → registrado en auditoría.
- Usuario sin rol admin intenta ampliar scope → solo ve su tenant.

---

#### AC-E3-06 — Scope obligatorio en escritura

**Origen**: HU-003.06.1, BR-003, BR-020  
**Criterio**: `POST /api/v2/memories` requiere `scope.tenantId`. Sin `scope` → `HTTP 400`. Con `scope: {}` → `HTTP 400`. Con `scope.tenantId: ""` → `HTTP 400`. El payload mínimo aceptable es `scope: {tenantId: "tenant-A"}`.

**Verificación**:
- Crear memoria sin campo `scope` → `HTTP 400`.
- Crear con `scope: {}` → `HTTP 400`.
- Crear con `scope: {tenantId: ""}` → `HTTP 400`.
- Crear con `scope: {tenantId: "tenant-A"}` → `HTTP 201`.

---

#### AC-E3-07 — Filtrado automático en lectura

**Origen**: HU-003.07.1, BR-004  
**Criterio**: Toda operación de lectura aplica automáticamente el filtro de `tenantId` del token JWT. El usuario no necesita especificarlo. Si especifica `scopes.tenantId` en la búsqueda, solo puede igualar o restringir (nunca ampliar) el scope del token.

**Verificación**:
- Buscar sin especificar tenant → filtrado automático al tenant del token.
- Especificar `scopes.tenantId = "tenant-B"` con token de tenant-A → rechazado o ignorado.

---

### 1.4 EP-004: API REST v2

#### AC-E4-01 — Crear memoria vía API v2

**Origen**: HU-004.01.1  
**Criterio**: `POST /api/v2/memories` con payload JSON válido crea la memoria, genera embedding vía OpenAI, indexa en Qdrant y retorna `HTTP 201` con el `memoryId` asignado, el embedding generado y todos los campos del modelo. `content` vacío → `HTTP 400`. Campo desconocido → `HTTP 400`.

**Verificación**:
- POST con payload completo → `HTTP 201`, embedding generado, memoria buscable.
- POST con `content: ""` → `HTTP 400`.
- POST con campo `color` → `HTTP 400 INVALID_REQUEST_BODY`.

---

#### AC-E4-02 — Consultar detalle de memoria

**Origen**: HU-004.01.2  
**Criterio**: `GET /api/v2/memories/{id}` retorna `HTTP 200` con el objeto completo (kind, content, summary, topics, entities, relations, metadata, source, scope, lifecycle, timestamps). Memoria inexistente → `HTTP 404`. Memoria de otro tenant → `HTTP 404`.

**Verificación**:
- GET memoria existente en mi tenant → `HTTP 200`, objeto completo.
- GET memoria inexistente → `HTTP 404`.
- GET memoria de otro tenant → `HTTP 404`.

---

#### AC-E4-03 — Actualizar memoria con regeneración condicional de embedding

**Origen**: HU-004.01.3, BR-011, BR-016  
**Criterio**: `PATCH /api/v2/memories/{id}` actualiza `content`, `summary`, `topics` o `metadata`. Si `content` cambia, regenera embedding y reindexa en Qdrant. Si solo cambian `metadata`, `topics` o `summary`, NO regenera embedding. `kind` es inmutable. Se registra auditoría con diff.

**Verificación**:
- PATCH solo metadata → no regenera embedding, auditoría registrada.
- PATCH content → regenera embedding, memoria reindexada.
- PATCH intentando cambiar `kind` → `HTTP 400`.

---

#### AC-E4-04 — API de relaciones CRUD

**Origen**: HU-004.02.1  
**Criterio**: `POST /api/v2/memories/{id}/relations` crea relación tipada. `DELETE /api/v2/memories/{id}/relations/{relId}` la elimina. Ambos endpoints validan tipos permitidos (9 tipos), target existente no `deleted`, y registran en auditoría.

**Verificación**:
- POST relación con type válido → `HTTP 201`.
- DELETE relación existente → `HTTP 204`, auditoría registrada.
- POST con type no válido → `HTTP 400`.

---

#### AC-E4-05 — Expansión de subgrafo

**Origen**: HU-004.03.1  
**Criterio**: `GET /api/v2/memories/{id}/graph?depth=N` retorna el subgrafo con la memoria raíz, nodos vecinos hasta profundidad N, y todos los edges. Soporta filtro `includeKinds`. Memoria sin relaciones → `relations: []`. Nodos en estado `deleted` se omiten.

**Verificación**:
- depth=2 con memoria que tiene 3 vecinos directos → retorna raíz + vecinos + vecinos de vecinos.
- `includeKinds=entity` → solo vecinos con kind `entity`.
- Memoria sin relaciones → `relations: []`.

---

#### AC-E4-06 — Revisión de estados vía API

**Origen**: HU-004.04.1  
**Criterio**: `POST /api/v2/memories/{id}/review` con acciones `approve`, `reject`, `archive`, `submit`. Valida permisos del rol y transiciones permitidas. Registra `reviewedBy`, `reviewedAt`, `reviewComment`. Usuario sin permisos → `HTTP 403`.

**Verificación**:
- Approve de `pending → active` → `HTTP 200`, `reviewedBy` registrado.
- Reject de `pending → rejected` con comentario → `HTTP 200`.
- Intentar approve de `draft → active` (sin pasar por pending) → `HTTP 422`.
- Memory Consumer intenta review → `HTTP 403`.

---

#### AC-E4-07 — API de búsqueda y detalle de entidades

**Origen**: HU-004.05.1  
**Criterio**: `GET /api/v2/entities?q=...` busca por coincidencia parcial. `GET /api/v2/entities/{name}` retorna detalle con `memoryCount` y `memoryIds`. Entidad inexistente → array vacío `HTTP 200`.

**Verificación**:
- Buscar "Kube" → retorna "Kubernetes".
- Detalle de entidad existente → `memoryCount` y lista de `memoryIds`.
- Buscar entidad inexistente → `[]`.

---

#### AC-E4-08 — Estadísticas por tenant

**Origen**: HU-004.06.1  
**Criterio**: `GET /api/v2/scopes/{tenantId}/stats` retorna `totalMemories`, distribución por `kind`, `status`, `sensitivity`, tasa de revisión y crecimiento temporal. Solo accesible por `memory-admin` y `memory-auditor`. Tenant sin memorias → `totalMemories: 0` (no error).

**Verificación**:
- Admin consulta stats → métricas pobladas correctamente.
- api-consumer consulta stats → `HTTP 403`.
- Tenant vacío → `totalMemories: 0`, distribuciones vacías.

---

#### AC-E4-09 — Health check de dependencias

**Origen**: HU-004.07.1  
**Criterio**: `GET /api/v2/health` retorna estado de Qdrant, PostgreSQL y OpenAI. Todos up → `HTTP 200` + `status: "healthy"`. Algún servicio down → `HTTP 503` + `status: "unhealthy"` con detalle del servicio caído. `GET /api/v2/metrics` expone métricas Prometheus (p95, throughput, tasas de error).

**Verificación**:
- Todos los servicios up → `HTTP 200`, cada dependencia `status: "up"`.
- Qdrant down → `HTTP 503`, `qdrant: {status: "down"}`.
- GET /metrics → formato Prometheus con métricas.

---

#### AC-E4-10 — English-Only en identificadores de API

**Origen**: HU-004.08.1, BR-010, R-04  
**Criterio**: El 100% de paths, query params, enums, códigos de error y nombres de campos de la API están en inglés. Cero identificadores internos en español u otros idiomas. La especificación OpenAPI lo refleja.

**Verificación**:
- Revisar OpenAPI: todos los paths, enums y errorCodes en inglés.
- Verificar response de GET /memories/{id}: campos en inglés (`kind`, no `tipo`).
- Verificar error 400: `errorCode: "VALIDATION_ERROR"`, no `"ERROR_VALIDACION"`.

---

#### AC-E4-11 — Documentación OpenAPI 3.x completa

**Origen**: HU-004.09.1  
**Criterio**: `GET /api/v2/openapi.json` retorna especificación OpenAPI 3.x válida con todos los endpoints, schemas de request/response, códigos de error, ejemplos y requisitos de autenticación. Se genera del código (no archivo estático).

**Verificación**:
- GET /openapi.json → JSON válido según estándar OpenAPI 3.x.
- Cada endpoint documenta método, path, parámetros, body schema, responses y auth.
- Modificar un endpoint → openapi.json refleja el cambio.

---

#### AC-E4-12 — Autenticación OIDC con JWT

**Origen**: HU-004.10.1  
**Criterio**: Toda request a `/api/v2/` requiere header `Authorization: Bearer <JWT>`. El sistema valida firma, expiración y claims. Sin token → `HTTP 401`. Token expirado → `HTTP 401`. Rol insuficiente → `HTTP 403`. `api-consumer` no puede crear memorias.

**Verificación**:
- Request con JWT válido → acceso permitido según roles.
- Request sin Authorization → `HTTP 401 UNAUTHORIZED`.
- Token expirado → `HTTP 401`.
- api-consumer intenta POST /memories → `HTTP 403`.

---

#### AC-E4-13 — Códigos de error estandarizados

**Origen**: HU-004.11.1  
**Criterio**: Todos los errores siguen el formato `{errorCode, message, details?}`. Cada situación de error tiene su código HTTP y `errorCode` apropiado: `400 INVALID_JSON`, `400 VALIDATION_ERROR`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `404 TARGET_NOT_FOUND`, `422 UNPROCESSABLE_ENTITY`, `429 RATE_LIMIT_EXCEEDED`, `500 INTERNAL_ERROR`, `503 DATABASE_UNAVAILABLE`.

**Verificación**:
- JSON malformado → `400 INVALID_JSON`.
- Validación fallida → `400 VALIDATION_ERROR` con `details` array.
- Rate limit excedido → `429 RATE_LIMIT_EXCEEDED` + `Retry-After` header.
- DB caída → `503 DATABASE_UNAVAILABLE`.

---

#### AC-E4-14 — Validación estricta de payloads

**Origen**: HU-004.12.1, BR-013, BR-017, BR-018  
**Criterio**: La API valida estrictamente tipos (importance debe ser float, no string), rangos (confidence e importance en [0.0, 1.0]), enums (kind uno de 8 valores), y campos desconocidos (rechazados en modo estricto). Errores de validación incluyen `details` con el campo que falló y la razón.

**Verificación**:
- Enviar `importance: "high"` → `HTTP 400`, details indica que debe ser número.
- Enviar `kind: "bug"` → `HTTP 400`, details lista los 8 valores permitidos.
- Enviar `confidence: 2.0` → `HTTP 400`, details indica rango [0.0, 1.0].

---

#### AC-E4-15 — Rate limiting por tenant y usuario (Should)

**Origen**: HU-004.13.1  
**Criterio**: El sistema limita la tasa de requests por tenant y usuario. Al exceder → `HTTP 429` + `Retry-After`. Límites configurables por tenant sin reinicio del servicio. Headers informativos: `X-RateLimit-Remaining`.

**Verificación**:
- Exceder límite → `HTTP 429` + `Retry-After`.
- Cambiar límite de un tenant → aplicado sin reinicio.
- Dos tenants con límites diferentes → cada uno respeta su límite.

---

### 1.5 EP-005: Búsqueda Semántica + Graph

#### AC-E5-01 — Búsqueda semántica por texto libre

**Origen**: HU-005.01.1  
**Criterio**: `POST /api/v2/memories/search` con `query` de texto libre retorna las top-K memorias con mayor similitud semántica, cada una con `score` entre 0.0 y 1.0, ordenadas de mayor a menor relevancia. Query vacía → `HTTP 400`. El embedding es multilingüe (query en español funciona sobre contenido en español).

**Verificación**:
- Buscar "¿cómo restaurar la base de datos?" → resultados relevantes ordenados por score.
- Buscar con `query: ""` → `HTTP 400`.
- Buscar en español sobre contenido en español → resultados correctos.

---

#### AC-E5-02 — Filtros estructurados multidimensionales

**Origen**: HU-005.02.1  
**Criterio**: La búsqueda soporta filtros simultáneos en 8 dimensiones: kinds, statuses, topics, entities, importance (gte/lte), confidence (gte/lte), sensitivities, rango de fechas (createdAfter/createdBefore). Todos los filtros se combinan con AND lógico. Filtros que ningún resultado cumple → array vacío (no error). Sin filtros explícitos → solo se aplica `statuses: ["active"]` implícito (BR-001) y `tenantId` del token.

**Verificación**:
- Buscar con 4 filtros simultáneos → resultados cumplen todos.
- Filtros contradictorios → array vacío con `HTTP 200`.
- Sin filtros → solo resultados `active` del tenant del token.

---

#### AC-E5-03 — Expansión de subgrafo en resultados

**Origen**: HU-005.03.1  
**Criterio**: Con `expandGraph: {depth: N, includeKinds: [...]}` activo, cada resultado de búsqueda incluye sus vecinos expandidos. Sin `expandGraph`, respuesta más ligera sin vecinos. Memoria sin relaciones → `relations: []`.

**Verificación**:
- Buscar con expandGraph depth=1 → cada resultado incluye `relations` poblado.
- Buscar sin expandGraph → resultados sin campo `relations` expandido.
- Memoria sin relaciones → `relations: []`.

---

#### AC-E5-04 — Re-ranking de resultados

**Origen**: HU-005.04.1  
**Criterio**: Con `rerank: true`, el orden de resultados combina score semántico (~0.50), importance (~0.20), confidence (~0.15), frescura (~0.10), riqueza de relaciones (~0.05). Con `rerank: false`, el orden es solo similitud de coseno. Una memoria con mayor importance aparece antes que otra con score semántico similar pero menor importance.

**Verificación**:
- Buscar con rerank=true → orden refleja scoring combinado.
- Buscar con rerank=false → orden solo por score semántico.
- Dos memorias con score similar, importance diferente → la de mayor importance primero con rerank=true.

---

#### AC-E5-05 — Top-K configurable

**Origen**: HU-005.06.1  
**Criterio**: `topK` controla el número de resultados. Default: 10. Límite máximo configurable por tenant. Si el número de resultados que cumplen filtros es menor que topK, se retornan los que hay.

**Verificación**:
- `topK: 5` → máximo 5 resultados.
- Sin `topK` → default 10.
- `topK: 500` → limitado al máximo del tenant.

---

#### AC-E5-06 — Embedding automático al crear/actualizar

**Origen**: HU-005.07.1, BR-016  
**Criterio**: Toda creación de memoria (`POST`) genera embedding vía OpenAI `text-embedding-3-large` (3072 dimensiones) e indexa en Qdrant. Si `PATCH` modifica `content`, regenera embedding. Si `PATCH` solo modifica `metadata`, `topics` o `summary`, NO regenera embedding. Soft-delete preserva el vector en Qdrant.

**Verificación**:
- POST → embedding generado, memoria buscable inmediatamente.
- PATCH solo metadata → sin regeneración de embedding.
- PATCH content → embedding regenerado, buscable con nuevo contenido.
- Soft-delete → vector preservado en Qdrant.

---

#### AC-E5-07 — Visibilidad gobernada por estado en búsqueda

**Origen**: HU-005.09.1, BR-001, FT-006.03  
**Criterio**: Búsqueda sin filtro explícito de `statuses` solo retorna memorias `active` (BR-001). `memory-reviewer` y `memory-admin` pueden filtrar explícitamente por `pending` y `draft`. `api-consumer` no puede ver `pending`, `draft`, `archived`, `rejected`, `deleted`. Memorias `deleted` nunca aparecen en búsquedas estándar.

**Verificación**:
- Buscar sin filtro → solo `active`.
- Reviewer busca con `statuses: ["pending", "active"]` → ve ambos.
- api-consumer intenta `statuses: ["pending"]` → rechazado o ignorado.
- Buscar con memoria deleted → nunca aparece.

---

#### AC-E5-08 — Scoring transparente y reproducible

**Origen**: HU-005.10.1  
**Criterio**: Cada resultado de búsqueda incluye `score` entre 0.0 y 1.0. Misma query + mismos datos = mismos scores (reproducible). Con `rerank: true`, el score refleja el scoring combinado post-re-ranking. El score es trazable para auditoría.

**Verificación**:
- Ejecutar misma búsqueda dos veces → scores idénticos.
- Con rerank=true → scores diferentes al score crudo de Qdrant.
- Todos los resultados tienen `score` en [0.0, 1.0].

---

### 1.6 EP-006: Gobernanza y Trazabilidad

#### AC-E6-01 — Auditoría completa de mutaciones

**Origen**: HU-006.01.1, CE-09  
**Criterio**: Toda operación de escritura (crear, modificar, review_approve, review_reject, review_archive, review_submit, delete, create_relation, delete_relation) genera un registro de auditoría inmutable con: `auditId`, `timestamp`, `userId`, `action`, `memoryId`, `tenantId`, `diff` (antes/después), `ipAddress`, `userAgent`. El 100% de las mutaciones tienen su registro de auditoría.

**Verificación**:
- Ejecutar 50 mutaciones de distintos tipos → 50 registros de auditoría.
- Verificar que cada registro tiene todos los campos requeridos.
- Intentar modificar/eliminar un registro de auditoría (como admin) → no permitido.

---

#### AC-E6-02 — Consulta del historial de auditoría

**Origen**: HU-006.01.2  
**Criterio**: Los roles `memory-admin` y `memory-auditor` pueden consultar el historial de auditoría con filtros por `userId`, `action`, `memoryId`, rango de fechas, `tenantId`. El historial muestra todos los cambios en orden cronológico. Roles sin permisos → `HTTP 403`.

**Verificación**:
- Auditor consulta historial de una memoria con 5 cambios → 5 registros cronológicos.
- Filtrar por `action: "review_approve"` → solo registros de aprobación.
- api-consumer intenta acceder → `HTTP 403`.

---

#### AC-E6-03 — Flujo de revisión humana completo

**Origen**: HU-006.02.1  
**Criterio**: El flujo `draft → pending → active` o `draft → pending → rejected → draft` funciona íntegramente. Cada transición genera registro de auditoría. Una memoria en `draft` puede editarse múltiples veces antes de enviarse a revisión. Una memoria en `rejected` puede reenviarse a `draft` para iteración del creador.

**Verificación**:
- Crear en draft → editar 3 veces → submit → pending → approve → active. Verificar 5+ registros de auditoría.
- Reject con comentario → el creador ve el motivo → resubmit → vuelve a draft.

---

#### AC-E6-04 — Visibilidad gobernada por estado y rol

**Origen**: HU-006.03.1  
**Criterio**: La matriz de visibilidad (especificación funcional §7.3) se cumple estrictamente: `api-consumer` solo ve `active`; `memory-operator` ve `active` + sus propias `draft`, `pending`, `rejected`; `memory-reviewer` ve `pending` y `draft` en su scope; `memory-admin` y `memory-auditor` ven todo. `archived`, `rejected`, `deleted` no son visibles para consumidores.

**Verificación**:
- api-consumer busca → solo `active`.
- Operator busca sus propias `draft` → visibles.
- Admin busca sin restricción → ve todos los estados.

---

#### AC-E6-05 — Umbral de revisión obligatoria

**Origen**: HU-006.04.1, BR-006  
**Criterio**: Si al crear una memoria `importance >= 0.7` Y `sensitivity IN (confidential, secret)`, el sistema fuerza `status = "draft"` (nunca `active` directo), advirtiendo que requiere revisión. Si solo se cumple una condición, se permite `active` directo. Admin con flag administrativo puede saltar el umbral, dejando justificación en auditoría.

**Verificación**:
- Crear con importance=0.8, sensitivity="confidential", status="active" → sistema asigna "draft".
- Crear con importance=0.6, sensitivity="confidential" → permite "active" (solo una condición).
- Admin con flag de bypass → permite "active" directo, justificación en auditoría.

---

#### AC-E6-06 — Linaje de decisiones mediante grafo

**Origen**: HU-006.05.1  
**Criterio**: Consultando el grafo de una `decision`, se pueden identificar todos los hechos/eventos que la respaldan (via `supports`, `caused_by`, `depends_on`). Consultando el grafo inverso de un `procedure`, se identifican todas las decisiones que se basaron en él. La relación `supersedes` permite trazar versiones.

**Verificación**:
- Expandir grafo de una decisión → se ven los hechos/eventos vinculados.
- Consultar qué memorias referencian un procedimiento → decisiones que lo usan.
- Trail de `supersedes` entre versiones → la más reciente es identificable.

---

#### AC-E6-07 — Depuración de repositorio (archivar, fusionar, soft-delete)

**Origen**: HU-006.06.1  
**Criterio**: `memory-admin` puede archivar (`active → archived`), fusionar duplicadas (merge de contenido + relaciones + marcar una como `deleted`), y soft-deletear cualquier memoria. Cada operación se registra en auditoría. Las métricas de calidad incluyen drafts huérfanos (>30 días), memorias sin relaciones y tasa de revisión.

**Verificación**:
- Archivar memoria activa → `active → archived`, fuera de búsquedas por defecto.
- Fusionar dos memorias → sobreviviente consolida relaciones, la otra marcada `deleted`.
- Consultar métricas de calidad → incluyen drafts huérfanos, sin relaciones, tasa revisión.

---

#### AC-E6-08 — Control de acceso RBAC con 5 roles

**Origen**: HU-006.07.1  
**Criterio**: La matriz de permisos RBAC (especificación funcional §5.7.2) se cumple exactamente: `api-consumer` solo lectura de `active`; `memory-operator` crea/modifica/soft-deletea propias; `memory-reviewer` revisa; `memory-admin` administra todo; `memory-auditor` solo lectura de todo. Cada rol que intenta una operación no permitida recibe `HTTP 403`.

**Verificación**:
- api-consumer intenta POST /memories → `HTTP 403`.
- memory-operator intenta GET /scopes/{id}/stats → `HTTP 403`.
- memory-auditor intenta POST /memories → `HTTP 403`.
- memory-admin accede a todo → permitido.

---

#### AC-E6-09 — Auditoría de creación y eliminación de relaciones

**Origen**: HU-006.08.1  
**Criterio**: Crear y eliminar relaciones genera registros de auditoría con `action: "create_relation"` / `"delete_relation"`, incluyendo `sourceId`, `targetId`, `type`. Si una memoria con relaciones es soft-deleteada, la auditoría registra qué relaciones quedaron huérfanas.

**Verificación**:
- Crear relación → registro de auditoría con `action: "create_relation"`, sourceId, targetId, type.
- Eliminar relación → registro con `action: "delete_relation"` y detalles.
- Soft-delete de memoria con relaciones → auditoría lista relaciones huérfanas.

---

### 1.7 EP-009: Frontend Multi-Dominio

#### AC-E9-01 — Formulario de creación adaptado al perfil

**Origen**: HU-009.01.1  
**Criterio**: El formulario de creación se reconfigura dinámicamente según el perfil activo: kinds destacados, tags sugeridos, campos de metadatos extra, defaults precargados, campos de scope visibles. Sin perfil activo, se muestran los 8 kinds sin priorización y sin sugerencias.

**Verificación**:
- Perfil Ops activo → kinds `event`/`procedure` destacados, tags `incident`/`runbook` sugeridos, metadatos `affectedService` visible.
- Perfil Agent activo → kinds `fact`/`preference`/`event`/`decision` destacados, campos `userId`/`sessionId` prominentes.
- Sin perfil → 8 kinds sin priorización, sin sugerencias de tags.

---

#### AC-E9-02 — Panel de búsqueda con todos los filtros

**Origen**: HU-009.02.1  
**Criterio**: El panel de búsqueda expone: campo de texto libre, selectores de kinds (multi-select), statuses, topics, entities, sliders de importance/confidence, selector de sensitivity, date pickers, toggles de expandGraph/rerank. Resultados muestran score (barra visual), kind (ícono), status (color), summary, topics (chips), entities (badges).

**Verificación**:
- Abrir panel → todos los filtros visibles y funcionales.
- Ejecutar búsqueda → resultados con score visual, kind con ícono, status con badge de color.
- Sin filtros → se aplica filtro implícito `statuses: ["active"]`.

---

#### AC-E9-03 — Panel de revisión con bandeja de pendientes

**Origen**: HU-009.03.1  
**Criterio**: El panel de revisión lista memorias `pending` en el scope del revisor, mostrando kind, importance, sensitivity, fecha de envío, solicitante. Al abrir detalle: contenido completo, historial de cambios, botones Approve/Reject (requiere motivo)/Request Changes. Tras la acción, la memoria desaparece de la bandeja.

**Verificación**:
- Revisor accede → lista de pendientes poblada correctamente.
- Abrir detalle → contenido, historial, botones de acción.
- Approve → memoria transiciona a `active`, desaparece de bandeja.
- Reject con motivo → memoria a `rejected`, motivo visible para el creador.

---

#### AC-E9-04 — Panel de administración multi-tenant

**Origen**: HU-009.04.1  
**Criterio**: El panel de administración permite: ver lista de tenants con métricas, crear/suspender/configurar tenants; buscar duplicadas y ejecutar merge; acceder a vista de auditoría con filtros; activar modo cross-tenant para navegar múltiples tenants. Solo accesible por `memory-admin`.

**Verificación**:
- Admin accede → tenants listados con métricas.
- Depuración → sugerencias de duplicadas por similitud, merge funcional.
- Auditoría → filtros por usuario, acción, fecha, tenant.
- Cross-tenant toggle → permite ver múltiples tenants simultáneamente.

---

#### AC-E9-05 — Visualización interactiva de grafo

**Origen**: HU-009.05.1  
**Criterio**: La vista de grafo renderiza nodos (tarjetas con kind, status codificado por color, summary truncado) y edges (líneas con etiqueta del tipo de relación y dirección). Navegación progresiva: clic en nodo vecino expande sus relaciones. Layout legible con zoom/paneo incluso con 20+ relaciones.

**Verificación**:
- Abrir detalle de memoria con 5 relaciones → grafo con 1 nodo central + 5 vecinos + 5 edges.
- Clic en nodo vecino → se expande mostrando sus propias relaciones.
- Clic en edge → muestra tipo de relación, dirección, fecha.
- Memoria con 20 relaciones → layout legible, sin solapamiento excesivo.

---

#### AC-E9-06 — Selección y cambio de perfil sin recarga

**Origen**: HU-009.06.1  
**Criterio**: Un selector de perfil siempre visible permite cambiar entre "Sin perfil", "Ops", "Agent", "Business" y perfiles custom. Al cambiar, la interfaz se reconfigura inmediatamente sin recargar la página. La selección se persiste (localStorage o backend) y se mantiene al recargar.

**Verificación**:
- Cambiar de Ops a Agent → formulario y filtros se actualizan instantáneamente.
- Recargar página → perfil seleccionado se mantiene.
- Seleccionar "Sin perfil" → interfaz neutra sin especialización.

---

#### AC-E9-07 — Dashboard de estadísticas con gráficos

**Origen**: HU-009.07.1  
**Criterio**: El dashboard muestra: pie chart de kinds, line chart de creaciones por semana, bar chart de statuses, top entities, top topics, tasa de revisión. Gráficos interactivos se actualizan al seleccionar rango de fechas. Solo accesible por `memory-admin` y `memory-auditor`.

**Verificación**:
- Admin accede → gráficos poblados con datos del tenant.
- Seleccionar rango de fechas → gráficos se actualizan.
- api-consumer intenta acceder → enlace oculto, acceso denegado.

---

#### AC-E9-08 — Autenticación integrada con Keycloak OIDC

**Origen**: HU-009.08.1  
**Criterio**: Usuario no autenticado → redirigido a login de Keycloak. Autenticación exitosa → redirigido al frontend con JWT. Token almacenado en memoria (no localStorage). Renovación silenciosa con refresh token. Logout → invalida token local + redirige a logout de Keycloak. UI se adapta a roles: menús/botones visibles/ocultos según permisos.

**Verificación**:
- Acceder sin autenticar → redirigido a Keycloak.
- Login exitoso → JWT en memoria, frontend funcional.
- Token próximo a expirar → renovación silenciosa sin interrumpir sesión.
- api-consumer → no ve panel de revisión, admin, ni dashboard.
- memory-admin → ve todas las secciones.
- Logout → token invalidado, redirigido a Keycloak.

---

## 2. Criterios de Aceptación del Sistema

Los 13 criterios de éxito (CE-01 a CE-13) definidos en la Visión del Producto se desglosan a continuación con detalle de verificación: herramienta, dataset, umbral y responsable.

### AC-SYS-01 — Rendimiento semántico en benchmark estándar (CE-01)

| Aspecto | Detalle |
|---|---|
| **Métrica** | NDCG@10 |
| **Dataset** | BEIR SciFact (5,183 documentos científicos) |
| **Umbral** | ≥ 0.80 |
| **Herramienta** | Suite de benchmark BEIR con adaptador para Abax-Memory v2 |
| **Procedimiento** | 1. Indexar los 5,183 documentos de SciFact como memorias con `kind = fact`. 2. Ejecutar las queries estándar del benchmark. 3. Calcular NDCG@10 comparando resultados contra ground truth. |
| **Responsable** | QA-Lead + Solution Architect |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-02 — Recall en benchmark estándar (CE-02)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Recall@10 |
| **Dataset** | BEIR SciFact (5,183 documentos) |
| **Umbral** | ≥ 0.90 |
| **Herramienta** | Suite de benchmark BEIR con adaptador para Abax-Memory v2 |
| **Procedimiento** | 1. Misma indexación que AC-SYS-01. 2. Para cada query, verificar cuántos documentos relevantes aparecen en el top-10. |
| **Responsable** | QA-Lead + Solution Architect |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-03 — Recall en memoria conversacional (CE-03)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Recall |
| **Dataset** | LoCoMo (Long-Context Memory benchmark) |
| **Umbral** | ≥ 0.80 |
| **Herramienta** | Adaptación del benchmark LoCoMo para perfil Agent |
| **Procedimiento** | 1. Configurar perfil Agent. 2. Simular sesiones conversacionales con memorias de tipo `fact`, `preference`, `event`, `decision`. 3. Verificar recall de memoria relevante en consultas contextuales. |
| **Responsable** | QA-Lead + Business Analyst |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-04 — Latencia de búsqueda p95 (CE-04)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Percentil 95 de latencia en `POST /memories/search` |
| **Dataset** | 10,000+ memorias distribuidas en 3 tenants |
| **Umbral** | < 500ms |
| **Herramienta** | k6, Locust o JMeter para pruebas de carga; Prometheus para medición |
| **Procedimiento** | 1. Poblar repositorio con 10K+ memorias en 3 tenants. 2. Ejecutar carga sostenida de búsquedas concurrentes (50 req/s). 3. Medir p95 sobre ventana de 5 minutos. |
| **Responsable** | DevOps + QA-Lead |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-05 — Precisión top-1 en suite interna (CE-05)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Precisión top-1 (¿el primer resultado es el correcto?) |
| **Dataset** | Suite interna de ~100 test cases multi-dominio con ground truth conocido |
| **Umbral** | ≥ 0.92 |
| **Herramienta** | Test suite automatizada (pytest o equivalente) |
| **Procedimiento** | 1. Preparar 100 queries con respuesta conocida en el repositorio. 2. Ejecutar búsqueda para cada query. 3. Verificar que el resultado en posición 1 es el ground truth esperado. |
| **Responsable** | QA-Functional |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-06 — Cobertura de tipos de memoria (CE-06)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de los 8 kinds con al menos 10 memorias de prueba |
| **Dataset** | Dataset de pruebas multi-dominio |
| **Umbral** | 100% (8/8 kinds) |
| **Herramienta** | Conteo SQL o script de validación |
| **Procedimiento** | 1. Verificar en el dataset de pruebas que cada uno de los 8 kinds tiene ≥ 10 memorias. 2. Confirmar que todas las operaciones CRUD, filtros y búsquedas funcionan correctamente con cada kind. |
| **Responsable** | QA-Functional |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-07 — Aislamiento multi-tenant (CE-07)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de queries cross-tenant que retornan 0 resultados del tenant equivocado |
| **Dataset** | 3 tenants con ≥ 100 memorias cada uno |
| **Umbral** | 100% (0 fugas) |
| **Herramienta** | Suite de tests de seguridad automatizados |
| **Procedimiento** | 1. Crear 3 tenants con datos. 2. Ejecutar 200+ queries con tokens de cada tenant. 3. Verificar que ningún resultado pertenece a un tenant distinto al del token. |
| **Responsable** | QA-Functional + Solution Architect |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-08 — Visibilidad por estado (CE-08)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Búsqueda sin filtro de status solo retorna memorias `active` (0 falsos positivos) |
| **Dataset** | Repositorio con memorias en los 6 estados (≥ 5 por estado) |
| **Umbral** | 100% (0 falsos positivos) |
| **Herramienta** | Tests automatizados |
| **Procedimiento** | 1. Crear memorias en draft, pending, active, archived, rejected, deleted. 2. Ejecutar búsqueda sin filtro de statuses. 3. Verificar que el 100% de los resultados tienen `status = active`. |
| **Responsable** | QA-Functional |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-09 — Trazabilidad de operaciones (CE-09)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de mutaciones con registro de auditoría completo (timestamp, userId, action, diff) |
| **Dataset** | Suite completa de operaciones CRUD (~200 mutaciones) |
| **Umbral** | 100% |
| **Herramienta** | Auditoría de logs tras ejecutar suite CRUD |
| **Procedimiento** | 1. Ejecutar todas las operaciones de la suite funcional. 2. Verificar que cada mutación tiene su registro de auditoría con todos los campos requeridos. 3. Confirmar inmutabilidad de los registros. |
| **Responsable** | QA-Functional + Business Analyst |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-10 — English-Only compliance (CE-10)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de identificadores internos (endpoints, enums, columnas, códigos de error) en inglés |
| **Dataset** | Código fuente completo + especificación OpenAPI + schema de base de datos |
| **Umbral** | 100% |
| **Herramienta** | Linter custom (escaneo de código) + revisión manual de OpenAPI |
| **Procedimiento** | 1. Ejecutar linter que detecta identificadores en español (ej. `tipo`, `estado`, `EN_REVISION`). 2. Revisar manualmente todos los paths, enums y errorCodes en openapi.json. 3. Verificar schema SQL: columnas en inglés. |
| **Responsable** | Tech-Lead + Business Analyst |
| **Fase de verificación** | Fase 4 (Construcción) y Fase 5 (UAT) |

---

### AC-SYS-11 — Operaciones sobre relaciones (CE-11)

| Aspecto | Detalle |
|---|---|
| **Métrica** | CRUD de relaciones funcional para los 9 tipos |
| **Dataset** | Suite de tests de integración (mínimo 2 tests por tipo de relación) |
| **Umbral** | 9/9 tipos operativos |
| **Herramienta** | Tests de integración automatizados |
| **Procedimiento** | 1. Para cada tipo de relación: crear, consultar (en grafo), expandir y eliminar. 2. Verificar direccionalidad correcta (dirigida vs. bidireccional). 3. Confirmar auditoría de cada operación. |
| **Responsable** | QA-Functional |
| **Fase de verificación** | Fase 5 (UAT) |

---

### AC-SYS-12 — Batch ingest (CE-12) — Should (fuera del MVP)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Ingesta de 100 memorias en una llamada con atomicidad |
| **Dataset** | Batches de 100 memorias con fallos simulados en ítems individuales |
| **Umbral** | Tasa de éxito ≥ 99% sin inconsistencias |
| **Herramienta** | Tests de carga con simulación de fallos |
| **Procedimiento** | 1. Enviar batch de 100 memorias válidas → todas persisten. 2. Enviar batch con 1 memoria inválida → ninguna persiste (atomicidad). 3. Verificar integridad post-ingesta. |
| **Responsable** | QA-Functional + DevOps |
| **Fase de verificación** | Post-MVP (release futuro) |
| **Nota** | Clasificado como **Should**. No bloquea el MVP. |

---

### AC-SYS-13 — Migración v1→v2 (CE-13) — Could (fuera del MVP)

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de memorias v1 correctamente migradas al modelo v2 sin pérdida semántica |
| **Dataset** | Muestra de validación de ≥ 20 memorias v1 migradas |
| **Umbral** | 100% en la muestra de validación |
| **Herramienta** | Script de migración + muestreo manual comparativo |
| **Procedimiento** | 1. Ejecutar script de migración. 2. Seleccionar 20 memorias migradas. 3. Comparar contenido, tipo mapeado, estado y relaciones contra el original v1. |
| **Responsable** | Business Analyst + Solution Architect |
| **Fase de verificación** | Solo si el usuario tiene datos v1 que desea migrar |
| **Nota** | Clasificado como **Could**. Script opcional externo. |

---

## 3. Criterios de Aceptación No Funcionales

### AC-NFR-01 — Latencia de búsqueda p95

| Aspecto | Detalle |
|---|---|
| **Métrica** | p95 de `POST /api/v2/memories/search` |
| **Umbral** | < 500ms |
| **Condiciones** | 10,000+ memorias, 3 tenants, carga de 50 búsquedas concurrentes/s |
| **Verificación** | Pruebas de carga con k6/Locust. Medición vía Prometheus. |
| **Vínculo** | AC-SYS-04 (CE-04) |

---

### AC-NFR-02 — Latencia de creación de memoria

| Aspecto | Detalle |
|---|---|
| **Métrica** | p95 de `POST /api/v2/memories` |
| **Umbral** | < 1,500ms (incluye generación de embedding vía OpenAI) |
| **Condiciones** | Carga de 10 creaciones concurrentes/s |
| **Verificación** | Pruebas de carga. Medición vía Prometheus. |
| **Nota** | La latencia incluye la llamada a OpenAI para embedding, que introduce latencia de red externa. |

---

### AC-NFR-03 — Disponibilidad del servicio

| Aspecto | Detalle |
|---|---|
| **Métrica** | Uptime del endpoint `/api/v2/health` |
| **Umbral** | ≥ 99.5% (medido en ventana de 30 días) |
| **Degradación** | Si Qdrant cae, búsquedas semánticas fallan pero CRUD básico (PostgreSQL) sigue operativo |
| **Verificación** | Monitoreo continuo vía health checks + Prometheus alerting |

---

### AC-NFR-04 — Throughput de búsquedas

| Aspecto | Detalle |
|---|---|
| **Métrica** | Búsquedas por segundo sostenidas |
| **Umbral** | ≥ 100 búsquedas/segundo con p95 < 500ms |
| **Condiciones** | 10,000+ memorias, 3 tenants |
| **Verificación** | Pruebas de carga con escalado progresivo hasta saturación |

---

### AC-NFR-05 — Seguridad: Aislamiento multi-tenant

| Aspecto | Detalle |
|---|---|
| **Métrica** | 0% de fuga de datos cross-tenant |
| **Umbral** | 100% de queries cross-tenant retornan 0 resultados |
| **Verificación** | Suite de seguridad automatizada (≥ 200 queries cross-tenant) |
| **Vínculo** | AC-SYS-07 (CE-07), AC-E3-01 |

---

### AC-NFR-06 — Seguridad: Autenticación obligatoria

| Aspecto | Detalle |
|---|---|
| **Métrica** | 100% de endpoints bajo `/api/v2/` requieren autenticación (excepto `/health` y `/openapi.json` si se configuran públicos) |
| **Umbral** | 0 endpoints accesibles sin token JWT válido |
| **Verificación** | Suite de tests que recorre todos los endpoints sin token → todos retornan `HTTP 401` |

---

### AC-NFR-07 — Seguridad: RBAC estricto

| Aspecto | Detalle |
|---|---|
| **Métrica** | 100% de operaciones no autorizadas son rechazadas |
| **Umbral** | 0 operaciones permitidas a roles sin permiso |
| **Verificación** | Suite de tests que recorre la matriz completa de permisos RBAC (§5.7.2 de la especificación funcional) |

---

### AC-NFR-08 — English-Only compliance

| Aspecto | Detalle |
|---|---|
| **Métrica** | 100% de identificadores internos en inglés |
| **Umbral** | 0 identificadores internos en español u otros idiomas |
| **Verificación** | Linter custom + revisión manual de OpenAPI + revisión de schema SQL |
| **Vínculo** | AC-SYS-10 (CE-10), BR-010, R-04 |

---

### AC-NFR-09 — Atomicidad de operaciones batch

| Aspecto | Detalle |
|---|---|
| **Métrica** | Batch ingest: todas las memorias del batch o ninguna |
| **Umbral** | 100% de batches con fallo en un ítem → 0 memorias persistidas |
| **Verificación** | Tests con batches que incluyen 1 memoria inválida |
| **Nota** | Clasificado como Should. No bloquea el MVP. |

---

### AC-NFR-10 — Trazabilidad completa

| Aspecto | Detalle |
|---|---|
| **Métrica** | Porcentaje de mutaciones con registro de auditoría |
| **Umbral** | 100% |
| **Verificación** | Auditoría post-suite CRUD completa |
| **Vínculo** | AC-SYS-09 (CE-09) |

---

### AC-NFR-11 — Inmutabilidad de registros de auditoría

| Aspecto | Detalle |
|---|---|
| **Métrica** | Los registros de auditoría no pueden ser modificados ni eliminados |
| **Umbral** | 0 modificaciones/eliminaciones posibles (incluso como admin) |
| **Verificación** | Intentar UPDATE/DELETE sobre tabla de auditoría → rechazado a nivel de aplicación y/o base de datos |

---

## 4. Matriz de Trazabilidad

### 4.1 Trazabilidad AC → HU → Feature → Épica

| AC ID | Descripción resumida | Historia de Usuario | Feature | Épica |
|---|---|---|---|---|
| AC-E1-01 | Clasificación con kind universal | HU-001.01.1 | FT-001.01 | EP-001 |
| AC-E1-02 | Búsqueda con filtro por kinds | HU-001.01.1 | FT-001.01 | EP-001 |
| AC-E1-03 | Transiciones de estado permitidas | HU-001.02.1, HU-001.02.2 | FT-001.02 | EP-001 |
| AC-E1-04 | Rechazo con motivo registrado | HU-001.02.2 | FT-001.02 | EP-001 |
| AC-E1-05 | Creación de relaciones tipadas | HU-001.03.1 | FT-001.03 | EP-001 |
| AC-E1-06 | Eliminación de relaciones con trazabilidad | HU-001.03.2 | FT-001.03, FT-006.08 | EP-001 |
| AC-E1-07 | Extracción de entidades sin persistir | HU-001.04.1 | FT-001.04 | EP-001 |
| AC-E1-08 | Búsqueda de entidades y memorias vinculadas | HU-001.04.2 | FT-001.04 | EP-001 |
| AC-E1-09 | Metadatos extensibles key-value | HU-001.05.1 | FT-001.05 | EP-001 |
| AC-E1-10 | Registro de source tipado | HU-001.06.1 | FT-001.06 | EP-001 |
| AC-E1-11 | Soft-delete con preservación de trazabilidad | HU-001.07.1 | FT-001.07 | EP-001 |
| AC-E1-12 | Versionado mediante supersedes | HU-001.08.1 | FT-001.08 | EP-001 |
| AC-E1-13 | Modelo de confidence con validación | HU-001.09.1 | FT-001.09 | EP-001 |
| AC-E1-14 | Rechazo de campos desconocidos | HU-004.01.1 | FT-004.12 | EP-001/EP-004 |
| AC-E1-15 | Contenido no vacío | HU-004.01.1 | FT-004.01 | EP-001/EP-004 |
| AC-E2-01 | Definición de perfil como configuración | HU-002.01.1 | FT-002.01 | EP-002 |
| AC-E2-02 | Herencia completa del core genérico | HU-002.02.1 | FT-002.02 | EP-002 |
| AC-E2-03 | Perfil Ops: kinds, tags y metadatos | HU-002.03.1 | FT-002.03 | EP-002 |
| AC-E2-04 | Perfil Agent: kinds conversacionales y scoping | HU-002.04.1 | FT-002.04 | EP-002 |
| AC-E2-05 | Perfil Business: kinds corporativos | HU-002.05.1 | FT-002.05 | EP-002 |
| AC-E2-06 | Aplicación de defaults según perfil | HU-002.06.1 | FT-002.06 | EP-002 |
| AC-E2-07 | Vocabulario controlado por perfil | HU-002.07.1 | FT-002.07 | EP-002 |
| AC-E2-08 | Extensibilidad sin modificar core | HU-002.08.1 | FT-002.08 | EP-002 |
| AC-E3-01 | Aislamiento estricto por tenantId | HU-003.01.1 | FT-003.01 | EP-003 |
| AC-E3-02 | Scoping por userId | HU-003.02.1 | FT-003.02 | EP-003 |
| AC-E3-03 | Scoping por sessionId | HU-003.03.1 | FT-003.03 | EP-003 |
| AC-E3-04 | Namespace como subdivisión | HU-003.04.1 | FT-003.04 | EP-003 |
| AC-E3-05 | Cross-tenant access para admin | HU-003.05.1 | FT-003.05 | EP-003 |
| AC-E3-06 | Scope obligatorio en escritura | HU-003.06.1 | FT-003.06 | EP-003 |
| AC-E3-07 | Filtrado automático en lectura | HU-003.07.1 | FT-003.07 | EP-003 |
| AC-E4-01 | Crear memoria vía API v2 | HU-004.01.1 | FT-004.01 | EP-004 |
| AC-E4-02 | Consultar detalle de memoria | HU-004.01.2 | FT-004.01 | EP-004 |
| AC-E4-03 | Actualizar con regeneración condicional | HU-004.01.3 | FT-004.01 | EP-004 |
| AC-E4-04 | API de relaciones CRUD | HU-004.02.1 | FT-004.02 | EP-004 |
| AC-E4-05 | Expansión de subgrafo | HU-004.03.1 | FT-004.03 | EP-004 |
| AC-E4-06 | Revisión de estados vía API | HU-004.04.1 | FT-004.04 | EP-004 |
| AC-E4-07 | API de búsqueda y detalle de entidades | HU-004.05.1 | FT-004.05 | EP-004 |
| AC-E4-08 | Estadísticas por tenant | HU-004.06.1 | FT-004.06 | EP-004 |
| AC-E4-09 | Health check de dependencias | HU-004.07.1 | FT-004.07 | EP-004 |
| AC-E4-10 | English-Only en identificadores de API | HU-004.08.1 | FT-004.08 | EP-004 |
| AC-E4-11 | Documentación OpenAPI 3.x | HU-004.09.1 | FT-004.09 | EP-004 |
| AC-E4-12 | Autenticación OIDC con JWT | HU-004.10.1 | FT-004.10 | EP-004 |
| AC-E4-13 | Códigos de error estandarizados | HU-004.11.1 | FT-004.11 | EP-004 |
| AC-E4-14 | Validación estricta de payloads | HU-004.12.1 | FT-004.12 | EP-004 |
| AC-E4-15 | Rate limiting (Should) | HU-004.13.1 | FT-004.13 | EP-004 |
| AC-E5-01 | Búsqueda semántica por texto libre | HU-005.01.1 | FT-005.01 | EP-005 |
| AC-E5-02 | Filtros estructurados multidimensionales | HU-005.02.1 | FT-005.02 | EP-005 |
| AC-E5-03 | Expansión de subgrafo en resultados | HU-005.03.1 | FT-005.03 | EP-005 |
| AC-E5-04 | Re-ranking de resultados | HU-005.04.1 | FT-005.04 | EP-005 |
| AC-E5-05 | Top-K configurable | HU-005.06.1 | FT-005.06 | EP-005 |
| AC-E5-06 | Embedding automático al crear/actualizar | HU-005.07.1 | FT-005.07 | EP-005 |
| AC-E5-07 | Visibilidad gobernada por estado | HU-005.09.1 | FT-005.09, FT-006.03 | EP-005, EP-006 |
| AC-E5-08 | Scoring transparente y reproducible | HU-005.10.1 | FT-005.10 | EP-005 |
| AC-E6-01 | Auditoría completa de mutaciones | HU-006.01.1 | FT-006.01 | EP-006 |
| AC-E6-02 | Consulta del historial de auditoría | HU-006.01.2 | FT-006.01 | EP-006 |
| AC-E6-03 | Flujo de revisión humana completo | HU-006.02.1 | FT-006.02 | EP-006 |
| AC-E6-04 | Visibilidad gobernada por estado y rol | HU-006.03.1 | FT-006.03 | EP-006 |
| AC-E6-05 | Umbral de revisión obligatoria | HU-006.04.1 | FT-006.04 | EP-006 |
| AC-E6-06 | Linaje de decisiones mediante grafo | HU-006.05.1 | FT-006.05 | EP-006 |
| AC-E6-07 | Depuración de repositorio | HU-006.06.1 | FT-006.06 | EP-006 |
| AC-E6-08 | Control de acceso RBAC con 5 roles | HU-006.07.1 | FT-006.07 | EP-006 |
| AC-E6-09 | Auditoría de relaciones | HU-006.08.1 | FT-006.08 | EP-006 |
| AC-E9-01 | Formulario de creación adaptado al perfil | HU-009.01.1 | FT-009.01 | EP-009 |
| AC-E9-02 | Panel de búsqueda con filtros | HU-009.02.1 | FT-009.02 | EP-009 |
| AC-E9-03 | Panel de revisión con bandeja | HU-009.03.1 | FT-009.03 | EP-009 |
| AC-E9-04 | Panel de administración multi-tenant | HU-009.04.1 | FT-009.04 | EP-009 |
| AC-E9-05 | Visualización interactiva de grafo | HU-009.05.1 | FT-009.05 | EP-009 |
| AC-E9-06 | Selección y cambio de perfil | HU-009.06.1 | FT-009.06 | EP-009 |
| AC-E9-07 | Dashboard de estadísticas | HU-009.07.1 | FT-009.07 | EP-009 |
| AC-E9-08 | Autenticación integrada con Keycloak | HU-009.08.1 | FT-009.08 | EP-009 |

**Resumen de cobertura**: 72 criterios de aceptación funcionales cubren 69 historias de usuario y 63 features Must. 2 criterios Should (AC-E4-15, AC-E9-03) se incluyen como referencia.

---

### 4.2 Trazabilidad Criterio de Éxito → AC Sistema

| CE | AC Sistema | Descripción |
|---|---|---|
| CE-01 | AC-SYS-01 | Rendimiento semántico NDCG@10 ≥ 0.80 en BEIR SciFact |
| CE-02 | AC-SYS-02 | Recall@10 ≥ 0.90 en BEIR SciFact |
| CE-03 | AC-SYS-03 | Recall ≥ 0.80 en LoCoMo (memoria conversacional) |
| CE-04 | AC-SYS-04 | Latencia p95 < 500ms en búsqueda |
| CE-05 | AC-SYS-05 | Precisión top-1 ≥ 0.92 en suite interna de 100 test cases |
| CE-06 | AC-SYS-06 | Cobertura 100% de los 8 kinds con ≥ 10 memorias cada uno |
| CE-07 | AC-SYS-07 | Aislamiento multi-tenant 100% (0 fugas cross-tenant) |
| CE-08 | AC-SYS-08 | Visibilidad por estado 100% (0 falsos positivos) |
| CE-09 | AC-SYS-09 | Trazabilidad 100% de mutaciones con registro de auditoría |
| CE-10 | AC-SYS-10 | English-Only compliance 100% |
| CE-11 | AC-SYS-11 | CRUD de relaciones operativo para 9/9 tipos |
| CE-12 | AC-SYS-12 | Batch ingest con atomicidad ≥ 99% (Should) |
| CE-13 | AC-SYS-13 | Migración v1→v2 100% en muestra (Could) |

---

## 5. Checklist de Verificación QA

Lista plana de TODOS los criterios de aceptación para que QA pueda verificar uno por uno. Cada ítem se marca `[ ]` (pendiente), `[x]` (aprobado) o `[!]` (fallido — requiere defecto).

### EP-001: Motor de Memoria Genérico

- [ ] **AC-E1-01** — Clasificación: los 8 kinds aceptados, valores inválidos rechazados, kind inmutable
- [ ] **AC-E1-02** — Búsqueda con filtro `kinds` restringe correctamente
- [ ] **AC-E1-03** — Máquina de estados: 6 transiciones permitidas OK, resto rechazadas con 422
- [ ] **AC-E1-04** — Rechazo de memoria: `reviewedBy`, `reviewedAt`, `reviewComment` registrados
- [ ] **AC-E1-05** — Relaciones: 9 tipos aceptados, target inexistente/devuelto/duplicado/auto-referencia rechazados
- [ ] **AC-E1-06** — Eliminación de relación: 204, desaparece de grafo, auditoría registrada
- [ ] **AC-E1-07** — Extracción de entidades: detecta entidades, texto sin entidades → [], content vacío → 400
- [ ] **AC-E1-08** — Búsqueda de entidades: coincidencia parcial, detalle con memoryCount + memoryIds
- [ ] **AC-E1-09** — Metadatos: key-value libre, PATCH reemplaza objeto completo, no afecta ranking
- [ ] **AC-E1-10** — Source tipado: 6 valores aceptados, opcional, valores inválidos rechazados
- [ ] **AC-E1-11** — Soft-delete: status=deleted, no eliminación física, desaparece de búsquedas, auditoría
- [ ] **AC-E1-12** — Supersedes: nueva versión no cambia estado de anterior, re-rank favorece nueva
- [ ] **AC-E1-13** — Confidence: rango [0.0, 1.0], default de perfil, filtro gte
- [ ] **AC-E1-14** — Rechazo de campos desconocidos en creación (modo estricto)
- [ ] **AC-E1-15** — Content no vacío: string vacío o whitespace → 400

### EP-002: Perfiles de Dominio

- [ ] **AC-E2-01** — Definición de perfil: name obligatorio, queda disponible sin deploy
- [ ] **AC-E2-02** — Herencia del core: 8 kinds, 6 estados, 9 relaciones, 4 sensibilidades siempre disponibles
- [ ] **AC-E2-03** — Perfil Ops: kinds destacados, tags sugeridos, metadatos extra, defaultImportance=0.7
- [ ] **AC-E2-04** — Perfil Agent: kinds conversacionales, scoping userId/sessionId, orden por importance
- [ ] **AC-E2-05** — Perfil Business: kinds corporativos, metadatos clientName/contractId, defaultSensitivity=internal
- [ ] **AC-E2-06** — Defaults según perfil: sin perfil importance=0.5 sensitivity=internal; valor explícito respetado
- [ ] **AC-E2-07** — Vocabulario controlado: tags/topics sugeridos por perfil, búsquedas cross-perfil funcionan
- [ ] **AC-E2-08** — Extensibilidad: nuevo perfil sin modificar core ni API, sin deploy

### EP-003: Scoping Multi-Tenant

- [ ] **AC-E3-01** — Aislamiento estricto: 0% fuga cross-tenant, 404 por tenant equivocado
- [ ] **AC-E3-02** — Scoping por userId: filtro restringe correctamente, memorias sin userId excluidas
- [ ] **AC-E3-03** — Scoping por sessionId: aislamiento correcto entre sesiones, datos sobreviven al cierre
- [ ] **AC-E3-04** — Namespace: subdivisión funcional, memorias sin namespace en búsquedas sin filtro
- [ ] **AC-E3-05** — Cross-tenant admin: admin puede consultar/operar múltiples tenants, auditoría registrada
- [ ] **AC-E3-06** — Scope obligatorio: sin scope → 400, scope sin tenantId → 400, tenantId vacío → 400
- [ ] **AC-E3-07** — Filtrado automático: token tenant aplicado en toda lectura, no ampliable por usuario

### EP-004: API REST v2

- [ ] **AC-E4-01** — POST /memories: 201 con memoryId + embedding, content vacío → 400
- [ ] **AC-E4-02** — GET /memories/{id}: 200 objeto completo, 404 inexistente/otro tenant
- [ ] **AC-E4-03** — PATCH /memories/{id}: regenera embedding solo si cambia content, kind inmutable
- [ ] **AC-E4-04** — API relaciones: POST/DELETE funcionales, tipos válidos, auditoría
- [ ] **AC-E4-05** — Graph expand: depth=N funcional, includeKinds, omit deleted
- [ ] **AC-E4-06** — Review API: approve/reject/archive/submit con permisos, transiciones válidas
- [ ] **AC-E4-07** — Entities API: búsqueda parcial, detalle con memoryCount, array vacío si no existe
- [ ] **AC-E4-08** — Stats API: métricas agregadas, 403 para roles sin permiso, tenant vacío → 0
- [ ] **AC-E4-09** — Health check: 200 healthy, 503 unhealthy con detalle de dependencia
- [ ] **AC-E4-10** — English-Only API: 100% paths/enums/errorCodes en inglés
- [ ] **AC-E4-11** — OpenAPI 3.x: JSON válido, todos los endpoints documentados, generado del código
- [ ] **AC-E4-12** — Auth OIDC: 401 sin token/expirado, 403 rol insuficiente
- [ ] **AC-E4-13** — Errores estandarizados: formato {errorCode, message, details}, códigos HTTP correctos
- [ ] **AC-E4-14** — Validación payloads: tipos, rangos, enums, strict mode
- [ ] **AC-E4-15** — Rate limiting (Should): 429 + Retry-After, límites configurables por tenant

### EP-005: Búsqueda Semántica + Graph

- [ ] **AC-E5-01** — Búsqueda semántica: resultados por score, query vacía → 400, multilingüe
- [ ] **AC-E5-02** — Filtros multidimensionales: 8 dimensiones, AND lógico, array vacío sin error
- [ ] **AC-E5-03** — Expansión subgrafo: expandGraph depth/ includeKinds, sin expand → respuesta ligera
- [ ] **AC-E5-04** — Re-ranking: rerank=true combina señales, rerank=false solo coseno
- [ ] **AC-E5-05** — Top-K: configurable, default 10, máximo por tenant
- [ ] **AC-E5-06** — Embedding automático: POST siempre, PATCH solo si cambia content, soft-delete preserva
- [ ] **AC-E5-07** — Visibilidad por estado: default active, reviewer ve pending/draft, api-consumer restringido
- [ ] **AC-E5-08** — Scoring transparente: score [0.0, 1.0], reproducible, trazable

### EP-006: Gobernanza y Trazabilidad

- [ ] **AC-E6-01** — Auditoría mutaciones: 100% cobertura, registro inmutable, campos completos
- [ ] **AC-E6-02** — Consulta historial: filtros por userId/action/fecha/tenant, 403 sin permisos
- [ ] **AC-E6-03** — Flujo revisión: draft→pending→active y draft→pending→rejected→draft completos
- [ ] **AC-E6-04** — Visibilidad por estado y rol: matriz completa respetada
- [ ] **AC-E6-05** — Umbral revisión: importance≥0.7 + sensitivity∈{confidential,secret} → fuerza draft
- [ ] **AC-E6-06** — Linaje decisiones: grafo forward/backward, supersedes trail
- [ ] **AC-E6-07** — Depuración: archivar, fusionar, soft-delete, métricas calidad
- [ ] **AC-E6-08** — RBAC 5 roles: matriz de permisos exacta, 403 en operaciones no autorizadas
- [ ] **AC-E6-09** — Auditoría relaciones: create_relation, delete_relation, relaciones huérfanas

### EP-009: Frontend Multi-Dominio

- [ ] **AC-E9-01** — Formulario adaptado: kinds/tags/metadatos según perfil, sin perfil → neutro
- [ ] **AC-E9-02** — Panel búsqueda: todos los filtros, resultados con score/íconos/badges
- [ ] **AC-E9-03** — Panel revisión: bandeja pending, detalle + botones approve/reject/request changes
- [ ] **AC-E9-04** — Panel admin: tenants, depuración, auditoría, cross-tenant
- [ ] **AC-E9-05** — Visualización grafo: nodos/edges interactivos, navegación progresiva, zoom/paneo
- [ ] **AC-E9-06** — Cambio perfil: selector sin recarga, persistencia, core genérico disponible
- [ ] **AC-E9-07** — Dashboard stats: gráficos interactivos, filtro fecha, acceso restringido
- [ ] **AC-E9-08** — Auth Keycloak: login/logout OIDC, renovación silenciosa, UI adaptada a roles

### Sistema (CE-01 a CE-13)

- [ ] **AC-SYS-01** — NDCG@10 ≥ 0.80 en BEIR SciFact
- [ ] **AC-SYS-02** — Recall@10 ≥ 0.90 en BEIR SciFact
- [ ] **AC-SYS-03** — Recall ≥ 0.80 en LoCoMo
- [ ] **AC-SYS-04** — p95 búsqueda < 500ms con 10K+ memorias
- [ ] **AC-SYS-05** — Precisión top-1 ≥ 0.92 en suite 100 test cases
- [ ] **AC-SYS-06** — 8/8 kinds con ≥ 10 memorias c/u
- [ ] **AC-SYS-07** — 0 fugas cross-tenant (100% queries limpias)
- [ ] **AC-SYS-08** — 0 falsos positivos en visibilidad por estado
- [ ] **AC-SYS-09** — 100% mutaciones con registro de auditoría
- [ ] **AC-SYS-10** — 100% identificadores internos en inglés
- [ ] **AC-SYS-11** — 9/9 tipos de relación operativos
- [ ] **AC-SYS-12** — Batch ingest atómico ≥ 99% (Should — post-MVP)
- [ ] **AC-SYS-13** — Migración v1→v2 100% en muestra (Could — opcional)

### No Funcionales

- [ ] **AC-NFR-01** — p95 búsqueda < 500ms (carga 50 req/s, 10K+ memorias)
- [ ] **AC-NFR-02** — p95 creación < 1,500ms (incluye embedding OpenAI)
- [ ] **AC-NFR-03** — Disponibilidad ≥ 99.5% (ventana 30 días)
- [ ] **AC-NFR-04** — Throughput ≥ 100 búsquedas/s con p95 < 500ms
- [ ] **AC-NFR-05** — 0% fuga cross-tenant (≥ 200 queries de seguridad)
- [ ] **AC-NFR-06** — 100% endpoints requieren autenticación
- [ ] **AC-NFR-07** — 100% operaciones RBAC respetan matriz de permisos
- [ ] **AC-NFR-08** — 100% identificadores internos en inglés
- [ ] **AC-NFR-09** — Batch atómico: 1 fallo → 0 persistidos (Should)
- [ ] **AC-NFR-10** — 100% mutaciones con registro de auditoría
- [ ] **AC-NFR-11** — 0 modificaciones/eliminaciones de registros de auditoría

---

### Totales del Checklist

| Categoría | Cantidad | Prioridad |
|---|---|---|
| EP-001 (Motor Genérico) | 15 | Must |
| EP-002 (Perfiles) | 8 | Must |
| EP-003 (Scoping) | 7 | Must |
| EP-004 (API REST v2) | 15 | 14 Must + 1 Should |
| EP-005 (Búsqueda + Graph) | 8 | Must |
| EP-006 (Gobernanza) | 9 | Must |
| EP-009 (Frontend) | 8 | Must |
| Sistema (CE) | 13 | 11 Must + 1 Should + 1 Could |
| No Funcionales | 11 | 9 Must + 1 Should |
| **Total** | **94** | **86 Must + 3 Should + 1 Could + 4 duplicados (alianza NFR/sistema)** |

> **Nota**: AC-SYS-04 ≡ AC-NFR-01, AC-SYS-07 ≡ AC-NFR-05, AC-SYS-09 ≡ AC-NFR-10, AC-SYS-10 ≡ AC-NFR-08. Estos criterios aparecen tanto en sistema como en no funcionales porque se verifican en ambos contextos. La verificación puede unificarse.

---

## 6. Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de calidad de ranking que evalúa las primeras 10 posiciones, penalizando resultados relevantes en posiciones bajas.
- **BEIR**: Benchmarking Information Retrieval — conjunto de datasets estándar para evaluar sistemas de búsqueda semántica. SciFact es su subconjunto de 5,183 documentos científicos.
- **LoCoMo**: Long-Context Memory — benchmark para evaluación de memoria conversacional de largo plazo en agentes de IA.
- **p95**: Percentil 95 — métrica de latencia: el 95% de las solicitudes se completan en un tiempo ≤ al valor indicado.
- **OIDC**: OpenID Connect — protocolo de autenticación sobre OAuth 2.0. Permite verificar identidad y obtener claims (roles, tenantId) desde un proveedor como Keycloak.
- **JWT**: JSON Web Token — token de acceso firmado digitalmente que transporta claims del usuario (identidad, roles, tenantId).
- **Qdrant**: Base de datos vectorial open-source para almacenar embeddings (vectores de 3072 dimensiones) y ejecutar búsqueda semántica por similitud de coseno.

---

*Documento generado por business-analyst el 2026-05-03. Consolida 72 criterios de aceptación funcionales (agrupados en 7 épicas), 13 criterios de aceptación del sistema (alineados con CE-01 a CE-13), 11 criterios no funcionales, matriz de trazabilidad completa (AC → HU → Feature → Épica) y checklist de verificación QA con 94 ítems verificables. Cubre la totalidad de las 69 historias de usuario Must y las 7 épicas del MVP v2.0.0.*
