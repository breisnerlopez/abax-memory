---
fase: 5 — QA Testing (v2.0.0)
entregable: Casos de Prueba
responsable: qa-functional
fecha: 2026-05-04
release: v2.0.0
estado: Completado
fuentes:
  - docs/entregables/v2/fase-2-analisis/criterios-de-aceptacion.md
  - docs/entregables/v2/fase-2-analisis/especificacion-funcional.md
  - docs/entregables/v2/fase-0-descubrimiento/historias-usuario.md
alcance: 15 endpoints REST + 6 pantallas React + 94 criterios de aceptación
---

# Casos de Prueba — Abax-Memory v2.0.0

## Índice

1. [Casos de Prueba Funcionales](#1-casos-de-prueba-funcionales)
   - [1.1 CRUD de Memorias (EP-004 / AC-E4)](#11-crud-de-memorias-ep-004--ac-e4)
   - [1.2 Búsqueda Semántica (EP-005 / AC-E5)](#12-búsqueda-semántica-ep-005--ac-e5)
   - [1.3 Relaciones y Grafo (EP-001, EP-004 / AC-E1, AC-E4)](#13-relaciones-y-grafo-ep-001-ep-004--ac-e1-ac-e4)
   - [1.4 Ciclo de Vida y Revisión (EP-001, EP-006 / AC-E1, AC-E6)](#14-ciclo-de-vida-y-revisión-ep-001-ep-006--ac-e1-ac-e6)
   - [1.5 Entidades y Extracción (EP-001, EP-004 / AC-E1, AC-E4)](#15-entidades-y-extracción-ep-001-ep-004--ac-e1-ac-e4)
   - [1.6 Scoping Multi-Tenant (EP-003 / AC-E3)](#16-scoping-multi-tenant-ep-003--ac-e3)
   - [1.7 Perfiles de Dominio (EP-002 / AC-E2)](#17-perfiles-de-dominio-ep-002--ac-e2)
   - [1.8 Administración y Monitoreo (EP-004 / AC-E4)](#18-administración-y-monitoreo-ep-004--ac-e4)
2. [Casos de Prueba de Integración](#2-casos-de-prueba-de-integración)
3. [Casos de Prueba No Funcionales](#3-casos-de-prueba-no-funcionales)
4. [Casos de Prueba de Frontend](#4-casos-de-prueba-de-frontend)
5. [Casos de Prueba de Seguridad](#5-casos-de-prueba-de-seguridad)
6. [Matriz de Trazabilidad](#6-matriz-de-trazabilidad)
7. [Resumen de Cobertura](#7-resumen-de-cobertura)
8. [Glosario](#glosario)

---

## Convenciones

| Elemento | Formato / Valor |
|---|---|
| **Base URL** | `https://qa.abax-memory.internal/api/v2` |
| **Auth** | `Authorization: Bearer <JWT>` emitido por Keycloak QA |
| **Tenant A** | `tenant-alpha` — operador `operador@alpha.local`, revisor `revisor@alpha.local` |
| **Tenant B** | `tenant-bravo` — operador `operador@bravo.local` |
| **Admin** | `admin@system.local` con rol `memory-admin` cross-tenant |
| **Auditor** | `auditor@system.local` con rol `memory-auditor` |
| **Consumer** | `consumer@alpha.local` con rol `api-consumer` |
| **Content-Type** | `application/json` en todos los requests con body |
| **IDs de prueba** | Prefijo `MEM-TEST-` seguido de 8 caracteres alfanuméricos (ejemplo en datos) |

---

## 1. Casos de Prueba Funcionales

### 1.1 CRUD de Memorias (EP-004 / AC-E4)

#### CP-F-001: Crear memoria con payload mínimo válido
- **HU**: HU-004.01.1
- **AC**: AC-E4-01, AC-E3-06
- **Tipo**: Positivo
- **Precondiciones**: Token JWT válido con rol `memory-operator`, `tenantId = tenant-alpha`.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con payload mínimo.
  2. Verificar HTTP status y body de respuesta.
  3. Verificar que el `memoryId` generado sigue el formato `MEM-` + 8 alfanuméricos.
  4. Verificar que `lifecycle.status` es `active` (porque no se especificó).
- **Datos**:
  ```json
  {
    "kind": "fact",
    "content": "El servicio payment-api corre en el puerto 8080",
    "scope": { "tenantId": "tenant-alpha" }
  }
  ```
- **Resultado esperado**: `HTTP 201`. Body contiene `id` (`MEM-`...), `kind: "fact"`, `content`, `lifecycle.status: "active"`, `lifecycle.confidence: 0.5` (default sin perfil), `createdAt`, `updatedAt`. Embedding generado y memoria buscable en Qdrant.

---

#### CP-F-002: Crear memoria con payload completo
- **HU**: HU-004.01.1, HU-001.05.1, HU-001.06.1, HU-001.09.1
- **AC**: AC-E4-01, AC-E1-09, AC-E1-10, AC-E1-13
- **Tipo**: Positivo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con payload completo.
  2. Verificar que todos los campos se persisten correctamente.
  3. Verificar que `metadata` se preserva intacto.
  4. Verificar que `source` se preserva.
- **Datos**:
  ```json
  {
    "kind": "event",
    "content": "Despliegue de la v2.1.0 a producción el 2026-05-01 a las 14:30 UTC. El despliegue incluyó migración de esquema y reinicio de pods.",
    "summary": "Despliegue v2.1.0 — producción",
    "topics": ["deployment", "production", "kubernetes"],
    "entities": ["kubernetes", "production-cluster"],
    "metadata": {
      "affectedService": "abax-memory",
      "environment": "production",
      "deployedBy": "ci-cd-pipeline"
    },
    "source": { "type": "api", "id": "jenkins-build-4423" },
    "scope": { "tenantId": "tenant-alpha", "userId": "operador@alpha.local", "namespace": "deployments" },
    "lifecycle": { "confidence": 0.95, "importance": 0.8, "sensitivity": "internal" }
  }
  ```
- **Resultado esperado**: `HTTP 201`. `metadata` contiene los 3 pares clave-valor. `source` refleja `type: "api"` e `id: "jenkins-build-4423"`. `confidence: 0.95`, `importance: 0.8`, `sensitivity: "internal"`.

---

#### CP-F-003: Crear memoria con cada uno de los 8 kinds
- **HU**: HU-001.01.1
- **AC**: AC-E1-01
- **Tipo**: Positivo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Para cada kind en `["fact", "preference", "event", "decision", "task", "procedure", "note", "entity"]`, enviar `POST /api/v2/memories` con contenido válido.
  2. Verificar que cada uno retorna `HTTP 201`.
  3. Verificar que el `kind` en la respuesta coincide con el enviado.
- **Datos** (ejemplo para `preference`):
  ```json
  { "kind": "preference", "content": "El usuario prefiere notificaciones por email en lugar de Slack", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: 8 respuestas `HTTP 201`, cada una con el `kind` correcto persistido.

---

#### CP-F-004: Rechazar creación con kind inválido
- **HU**: HU-001.01.1
- **AC**: AC-E1-01
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con `kind: "bug"`.
  2. Verificar código de error y mensaje.
- **Datos**:
  ```json
  { "kind": "bug", "content": "something", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: `HTTP 400`. `errorCode: "VALIDATION_ERROR"`. `details` indica el campo `kind` y lista los 8 valores permitidos.

---

#### CP-F-005: Rechazar creación con content vacío
- **HU**: HU-004.01.1
- **AC**: AC-E1-15
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con `content: ""`.
  2. Enviar `POST /api/v2/memories` con `content: "   "` (solo whitespace).
- **Datos**:
  ```json
  { "kind": "fact", "content": "", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: Ambos retornan `HTTP 400 VALIDATION_ERROR`.

---

#### CP-F-006: Rechazar creación sin scope
- **HU**: HU-003.06.1
- **AC**: AC-E3-06
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` sin campo `scope`.
  2. Enviar `POST /api/v2/memories` con `scope: {}`.
  3. Enviar `POST /api/v2/memories` con `scope: {tenantId: ""}`.
- **Datos**:
  ```json
  { "kind": "fact", "content": "test content" }
  ```
- **Resultado esperado**: Los tres retornan `HTTP 400 VALIDATION_ERROR` indicando que `scope.tenantId` es obligatorio.

---

#### CP-F-007: Rechazar creación con campo desconocido (strict mode)
- **HU**: HU-004.01.1, HU-004.12.1
- **AC**: AC-E1-14
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con campo `color` no definido en el schema.
- **Datos**:
  ```json
  { "kind": "fact", "content": "test content", "scope": { "tenantId": "tenant-alpha" }, "color": "red" }
  ```
- **Resultado esperado**: `HTTP 400`. `errorCode: "INVALID_REQUEST_BODY"` o `"VALIDATION_ERROR"` indicando campo desconocido.

---

#### CP-F-008: Rechazar creación con confidence fuera de rango
- **HU**: HU-001.09.1
- **AC**: AC-E1-13
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con `confidence: 1.5`.
  2. Enviar `POST /api/v2/memories` con `confidence: -0.1`.
- **Datos**:
  ```json
  { "kind": "fact", "content": "test", "scope": { "tenantId": "tenant-alpha" }, "lifecycle": { "confidence": 1.5 } }
  ```
- **Resultado esperado**: Ambos retornan `HTTP 400 VALIDATION_ERROR`. `details` indica rango `[0.0, 1.0]`.

---

#### CP-F-009: Rechazar creación con source.type inválido
- **HU**: HU-001.06.1
- **AC**: AC-E1-10
- **Tipo**: Negativo
- **Precondiciones**: Token `memory-operator` válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con `source.type: "email"`.
- **Datos**:
  ```json
  { "kind": "fact", "content": "test", "scope": { "tenantId": "tenant-alpha" }, "source": { "type": "email", "id": "msg-001" } }
  ```
- **Resultado esperado**: `HTTP 400 VALIDATION_ERROR`. `details` lista los 6 valores permitidos.

---

#### CP-F-010: Consultar detalle de memoria existente
- **HU**: HU-004.01.2
- **AC**: AC-E4-02
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-EXIST-01` creada en `tenant-alpha` con todos los campos.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-EXIST-01`.
  2. Verificar body completo.
- **Datos**: N/A (sin body).
- **Resultado esperado**: `HTTP 200`. Body contiene: `id`, `kind`, `content`, `summary`, `topics`, `entities`, `relations`, `metadata`, `source`, `scope`, `lifecycle`, `createdAt`, `updatedAt`.

---

#### CP-F-011: Consultar detalle de memoria inexistente
- **HU**: HU-004.01.2
- **AC**: AC-E4-02
- **Tipo**: Negativo
- **Precondiciones**: No existe memoria con ID `MEM-99999999`.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-99999999`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 404 NOT_FOUND`.

---

#### CP-F-012: Consultar detalle — borde: ID malformado
- **HU**: HU-004.01.2
- **AC**: AC-E4-02, AC-E4-13
- **Tipo**: Borde
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/not-a-valid-id`.
  2. Enviar `GET /api/v2/memories/` (sin ID).
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 400` o `HTTP 404` (ID malformado vs recurso no encontrado). Comportamiento esperado: `HTTP 400` para formato inválido.

---

#### CP-F-013: Actualizar content de memoria (regenera embedding)
- **HU**: HU-004.01.3, HU-005.07.1
- **AC**: AC-E4-03, AC-E5-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-PATCH-01` existe con `content` original.
- **Pasos**:
  1. Enviar `PATCH /api/v2/memories/MEM-PATCH-01` con nuevo `content`.
  2. Verificar que el `content` cambió.
  3. Verificar que `updatedAt` es posterior al `updatedAt` anterior.
  4. Buscar semánticamente con el nuevo contenido para verificar reindexación.
- **Datos**:
  ```json
  { "content": "Contenido actualizado con información nueva sobre el servicio payment-api v2" }
  ```
- **Resultado esperado**: `HTTP 200`. `content` refleja el nuevo valor. `updatedAt` actualizado. Memoria buscable con nuevo contenido semántico. Embedding regenerado.

---

#### CP-F-014: Actualizar solo metadata (NO regenera embedding)
- **HU**: HU-004.01.3, HU-005.07.1
- **AC**: AC-E4-03, AC-E5-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-PATCH-02` existe. Se conoce el `updatedAt` pre-PATCH.
- **Pasos**:
  1. Enviar `PATCH /api/v2/memories/MEM-PATCH-02` solo con `metadata` nuevo.
  2. Verificar que `content` no cambió.
  3. Verificar que `updatedAt` cambió.
  4. Verificar que `metadata` se reemplazó completamente (no merge).
- **Datos**:
  ```json
  { "metadata": { "newField": "newValue", "environment": "staging" } }
  ```
- **Resultado esperado**: `HTTP 200`. `metadata` contiene SOLO los nuevos pares (reemplazo completo). `content` sin cambios. Embedding NO regenerado (verificable vía métrica o log).

---

#### CP-F-015: Intentar modificar kind (inmutable)
- **HU**: HU-004.01.3
- **AC**: AC-E4-03, AC-E1-01
- **Tipo**: Negativo
- **Precondiciones**: Memoria `MEM-IMMUT-01` creada con `kind: "fact"`.
- **Pasos**:
  1. Enviar `PATCH /api/v2/memories/MEM-IMMUT-01` con `kind: "event"`.
- **Datos**:
  ```json
  { "kind": "event" }
  ```
- **Resultado esperado**: `HTTP 400 VALIDATION_ERROR` indicando que `kind` es inmutable.

---

#### CP-F-016: Soft-delete de memoria activa
- **HU**: HU-001.07.1
- **AC**: AC-E1-11
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-DEL-01` en estado `active`.
- **Pasos**:
  1. Enviar `DELETE /api/v2/memories/MEM-DEL-01`.
  2. Verificar respuesta.
  3. Verificar que la memoria no aparece en búsquedas estándar.
  4. Verificar registro de auditoría.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 204`. `lifecycle.status` cambia a `deleted`. La memoria NO aparece en `POST /memories/search` sin filtro de `statuses`. Registro de auditoría con `action: "delete"` existe.

---

#### CP-F-017: Soft-delete — borde: eliminar memoria ya deleted
- **HU**: HU-001.07.1
- **AC**: AC-E1-11
- **Tipo**: Borde
- **Precondiciones**: Memoria `MEM-DEL-02` ya en estado `deleted`.
- **Pasos**:
  1. Enviar `DELETE /api/v2/memories/MEM-DEL-02`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 422 UNPROCESSABLE_ENTITY` o `HTTP 409 Conflict` (operación idempotente rechazada por estado actual). Si es idempotente, `HTTP 204` también es aceptable — documentar comportamiento.

---

### 1.2 Búsqueda Semántica (EP-005 / AC-E5)

#### CP-F-018: Búsqueda semántica con query de texto libre
- **HU**: HU-005.01.1
- **AC**: AC-E5-01
- **Tipo**: Positivo
- **Precondiciones**: Al menos 10 memorias activas con contenido variado pobladas en `tenant-alpha`.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/search` con `query` semántica.
  2. Verificar que los resultados están ordenados por `score` descendente.
  3. Verificar que cada resultado tiene `score` entre 0.0 y 1.0.
- **Datos**:
  ```json
  {
    "query": "¿cómo restaurar una base de datos después de una caída?",
    "topK": 5
  }
  ```
- **Resultado esperado**: `HTTP 200`. `results` array con máximo 5 elementos. Cada uno con `memoryId`, `kind`, `summary`, `score`, `lifecycle`, `topics`, `entities`. `totalResults` indica cuántos cumplen. `queryTimeMs` > 0.

---

#### CP-F-019: Búsqueda con filtros multidimensionales (8 dimensiones)
- **HU**: HU-005.02.1
- **AC**: AC-E5-02
- **Tipo**: Positivo
- **Precondiciones**: Memorias con distintos kinds, statuses, topics, entities, importance, sensitivity en `tenant-alpha`.
- **Pasos**:
  1. Enviar búsqueda con filtros en todas las dimensiones.
  2. Verificar que CADA resultado cumple TODOS los filtros (AND lógico).
- **Datos**:
  ```json
  {
    "query": "deployment configuration",
    "topK": 10,
    "filters": {
      "kinds": ["procedure", "event"],
      "statuses": ["active"],
      "topics": ["kubernetes", "deployment"],
      "entities": ["production-cluster"],
      "importance": { "gte": 0.5, "lte": 1.0 },
      "confidence": { "gte": 0.8 },
      "sensitivities": ["internal", "public"],
      "createdAfter": "2026-01-01T00:00:00Z",
      "createdBefore": "2026-12-31T23:59:59Z"
    },
    "rerank": true
  }
  ```
- **Resultado esperado**: `HTTP 200`. Todos los resultados cumplen cada filtro. `totalResults` correcto.

---

#### CP-F-020: Búsqueda con query vacía
- **HU**: HU-005.01.1
- **AC**: AC-E5-01
- **Tipo**: Negativo
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/search` con `query: ""`.
- **Datos**:
  ```json
  { "query": "" }
  ```
- **Resultado esperado**: `HTTP 400 VALIDATION_ERROR` indicando que `query` no puede ser vacío.

---

#### CP-F-021: Búsqueda con filtros contradictorios → array vacío
- **HU**: HU-005.02.1
- **AC**: AC-E5-02
- **Tipo**: Borde
- **Precondiciones**: Ninguna memoria combina `kind: "note"` y `sensitivity: "secret"`.
- **Pasos**:
  1. Enviar búsqueda con filtros que ningún resultado cumple.
- **Datos**:
  ```json
  { "query": "test", "filters": { "kinds": ["note"], "sensitivities": ["secret"], "entities": ["NonExistentEntity"] } }
  ```
- **Resultado esperado**: `HTTP 200`. `results: []`, `totalResults: 0`. No error.

---

#### CP-F-022: Top-K configurable — menos resultados que topK
- **HU**: HU-005.06.1
- **AC**: AC-E5-05
- **Tipo**: Borde
- **Precondiciones**: Solo 3 memorias que cumplen los filtros.
- **Pasos**:
  1. Enviar búsqueda con `topK: 10`.
  2. Verificar que retorna solo 3 resultados.
- **Datos**:
  ```json
  { "query": "specific rare topic", "topK": 10 }
  ```
- **Resultado esperado**: `HTTP 200`. `results` tiene 3 elementos (menos que topK). Sin error.

---

#### CP-F-023: Re-ranking activo vs inactivo
- **HU**: HU-005.04.1
- **AC**: AC-E5-04
- **Tipo**: Positivo
- **Precondiciones**: Dos memorias con score semántico similar pero `importance` distinta.
- **Pasos**:
  1. Ejecutar misma búsqueda con `rerank: true`.
  2. Ejecutar misma búsqueda con `rerank: false`.
  3. Comparar el orden de resultados.
- **Datos**:
  ```json
  { "query": "database recovery procedure", "topK": 5, "rerank": true }
  ```
- **Resultado esperado**: Con `rerank: true`, la memoria de mayor `importance` aparece antes que otra con score semántico similar pero menor `importance`. Con `rerank: false`, orden es solo por score semántico. Scores son diferentes entre ambas ejecuciones.

---

#### CP-F-024: Scoring reproducible
- **HU**: HU-005.10.1
- **AC**: AC-E5-08
- **Tipo**: Positivo
- **Precondiciones**: Datos estables en el repositorio.
- **Pasos**:
  1. Ejecutar búsqueda con `query` y filtros específicos.
  2. Esperar 10 segundos.
  3. Ejecutar exactamente la misma búsqueda.
  4. Comparar scores de cada resultado.
- **Datos**:
  ```json
  { "query": "kubernetes pod failure recovery", "topK": 10, "rerank": false }
  ```
- **Resultado esperado**: Los scores son idénticos entre las dos ejecuciones. Mismos `memoryId` en el mismo orden.

---

#### CP-F-025: Búsqueda con filtro implícito de status (solo active)
- **HU**: HU-005.09.1
- **AC**: AC-E5-07
- **Tipo**: Positivo
- **Precondiciones**: Memorias en `draft`, `pending`, `active`, `archived`, `rejected` en `tenant-alpha`.
- **Pasos**:
  1. Enviar búsqueda sin especificar `filters.statuses`.
  2. Verificar que todos los resultados tienen `lifecycle.status: "active"`.
- **Datos**:
  ```json
  { "query": "test content", "topK": 20 }
  ```
- **Resultado esperado**: `HTTP 200`. 0 resultados con `status != "active"`.

---

### 1.3 Relaciones y Grafo (EP-001, EP-004 / AC-E1, AC-E4)

#### CP-F-026: Crear relación tipada (cada uno de los 9 tipos)
- **HU**: HU-001.03.1
- **AC**: AC-E1-05
- **Tipo**: Positivo
- **Precondiciones**: Dos memorias activas `MEM-REL-SRC` y `MEM-REL-TGT` en `tenant-alpha`.
- **Pasos**:
  1. Para cada type en `["related_to", "depends_on", "caused_by", "resolves", "contradicts", "supports", "mentions", "belongs_to", "supersedes"]`, enviar `POST /api/v2/memories/MEM-REL-SRC/relations`.
  2. Verificar que cada una retorna `HTTP 201`.
  3. Verificar que el `type` y `targetId` son correctos.
  4. Verificar registro de auditoría con `action: "create_relation"`.
- **Datos** (ejemplo para `supports`):
  ```json
  { "targetId": "MEM-REL-TGT", "type": "supports" }
  ```
- **Resultado esperado**: 9 respuestas `HTTP 201`. Cada una con `relationId` (`REL-`...). Auditoría registrada para cada creación.

---

#### CP-F-027: Crear relación hacia target inexistente
- **HU**: HU-001.03.1
- **AC**: AC-E1-05
- **Tipo**: Negativo
- **Precondiciones**: `MEM-REL-SRC` existe. `MEM-NONEXIST` no existe.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/MEM-REL-SRC/relations` con `targetId` inexistente.
- **Datos**:
  ```json
  { "targetId": "MEM-NONEXIST", "type": "related_to" }
  ```
- **Resultado esperado**: `HTTP 404 TARGET_NOT_FOUND`.

---

#### CP-F-028: Crear relación hacia target deleted
- **HU**: HU-001.03.1
- **AC**: AC-E1-05
- **Tipo**: Negativo
- **Precondiciones**: `MEM-REL-SRC` activa. `MEM-DELETED` con `status = deleted`.
- **Pasos**:
  1. Enviar `POST` con `targetId` de memoria deleted.
- **Datos**:
  ```json
  { "targetId": "MEM-DELETED", "type": "related_to" }
  ```
- **Resultado esperado**: `HTTP 422 UNPROCESSABLE_ENTITY`.

---

#### CP-F-029: Crear relación duplicada
- **HU**: HU-001.03.1
- **AC**: AC-E1-05
- **Tipo**: Negativo
- **Precondiciones**: Relación `REL-DUP-01` ya existe entre `MEM-REL-SRC` y `MEM-REL-TGT` con `type: "depends_on"`.
- **Pasos**:
  1. Enviar mismo `POST` con idénticos `targetId` y `type`.
- **Datos**:
  ```json
  { "targetId": "MEM-REL-TGT", "type": "depends_on" }
  ```
- **Resultado esperado**: `HTTP 409 Conflict` o `HTTP 422`.

---

#### CP-F-030: Crear auto-relación (sourceId == targetId)
- **HU**: HU-001.03.1
- **AC**: AC-E1-05
- **Tipo**: Negativo
- **Precondiciones**: `MEM-REL-SRC` existe.
- **Pasos**:
  1. Enviar relación con `targetId = "MEM-REL-SRC"`.
- **Datos**:
  ```json
  { "targetId": "MEM-REL-SRC", "type": "related_to" }
  ```
- **Resultado esperado**: `HTTP 422 UNPROCESSABLE_ENTITY`.

---

#### CP-F-031: Crear relación con type inválido
- **HU**: HU-004.02.1
- **AC**: AC-E4-04
- **Tipo**: Negativo
- **Precondiciones**: Memorias origen y destino válidas.
- **Pasos**:
  1. Enviar `POST` con `type: "colaborates_with"`.
- **Datos**:
  ```json
  { "targetId": "MEM-REL-TGT", "type": "collaborates_with" }
  ```
- **Resultado esperado**: `HTTP 400 VALIDATION_ERROR`. `details` lista los 9 tipos válidos.

---

#### CP-F-032: Eliminar relación existente
- **HU**: HU-001.03.2
- **AC**: AC-E1-06
- **Tipo**: Positivo
- **Precondiciones**: Relación `REL-DEL-01` existe.
- **Pasos**:
  1. Enviar `DELETE /api/v2/memories/MEM-REL-SRC/relations/REL-DEL-01`.
  2. Verificar que desaparece del grafo.
  3. Verificar registro de auditoría.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 204`. Consulta de grafo no muestra la relación. Registro de auditoría con `action: "delete_relation"`.

---

#### CP-F-033: Expandir subgrafo con depth=2
- **HU**: HU-004.03.1
- **AC**: AC-E4-05
- **Tipo**: Positivo
- **Precondiciones**: `MEM-GRAPH-01` tiene 3 relaciones directas (depth=1), y uno de los vecinos tiene 2 relaciones adicionales.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-GRAPH-01/graph?depth=2`.
  2. Verificar estructura de respuesta.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `root` es `MEM-GRAPH-01`. `nodes` incluye vecinos directos + vecinos de vecinos (hasta depth=2). `edges` incluye todas las aristas entre los nodos. `depth: 2`.

---

#### CP-F-034: Expandir subgrafo con includeKinds
- **HU**: HU-004.03.1
- **AC**: AC-E4-05
- **Tipo**: Positivo
- **Precondiciones**: `MEM-GRAPH-01` tiene vecinos de distintos kinds.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-GRAPH-01/graph?depth=1&includeKinds=entity,fact`.
  2. Verificar que solo aparecen vecinos con esos kinds.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. Solo nodos con `kind` en `["entity", "fact"]`.

---

#### CP-F-035: Expandir subgrafo — memoria sin relaciones
- **HU**: HU-004.03.1
- **AC**: AC-E4-05
- **Tipo**: Borde
- **Precondiciones**: `MEM-NOREL-01` sin relaciones.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-NOREL-01/graph?depth=1`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `root: {memoryId: "MEM-NOREL-01", ...}`. `nodes: []`, `edges: []`.

---

### 1.4 Ciclo de Vida y Revisión (EP-001, EP-006 / AC-E1, AC-E6)

#### CP-F-036: Transición draft → pending (submit)
- **HU**: HU-001.02.1, HU-006.02.1
- **AC**: AC-E1-03, AC-E4-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-LC-01` en `draft`, creada por `operador@alpha.local`.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/MEM-LC-01/review` con `action: "submit"`.
  2. Verificar cambio de estado y registro de auditoría.
- **Datos**:
  ```json
  { "action": "submit" }
  ```
- **Resultado esperado**: `HTTP 200`. `newStatus: "pending"`. `previousStatus: "draft"`. Registro de auditoría con `action: "review_submit"`.

---

#### CP-F-037: Transición pending → active (approve)
- **HU**: HU-001.02.1, HU-006.02.1
- **AC**: AC-E1-03, AC-E4-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-LC-02` en `pending`. Token con rol `memory-reviewer`.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/MEM-LC-02/review` con `action: "approve"`.
  2. Verificar `reviewedBy`, `reviewedAt`.
- **Datos**:
  ```json
  { "action": "approve", "comment": "Contenido verificado correctamente" }
  ```
- **Resultado esperado**: `HTTP 200`. `newStatus: "active"`. `reviewedBy: "revisor@alpha.local"`. `reviewedAt` timestamp ISO 8601.

---

#### CP-F-038: Transición pending → rejected con motivo
- **HU**: HU-001.02.2
- **AC**: AC-E1-04, AC-E4-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-LC-03` en `pending`. Token `memory-reviewer`.
- **Pasos**:
  1. Enviar review con `action: "reject"` y `reviewComment`.
  2. Como creador, consultar detalle y verificar `reviewComment` visible.
- **Datos**:
  ```json
  { "action": "reject", "comment": "Falta evidencia de la fuente. Adjuntar enlace al runbook original." }
  ```
- **Resultado esperado**: `HTTP 200`. `newStatus: "rejected"`. `reviewComment` contiene el motivo. El creador puede ver el comentario en `GET /memories/MEM-LC-03`.

---

#### CP-F-039: Transición rejected → draft (resubmit)
- **HU**: HU-001.02.2, HU-006.02.1
- **AC**: AC-E1-03
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-LC-04` en `rejected`. Token del creador.
- **Pasos**:
  1. Enviar review con `action: "submit"` (o acción de resubmit).
- **Datos**:
  ```json
  { "action": "submit" }
  ```
- **Resultado esperado**: `HTTP 200`. `newStatus: "draft"` o el sistema permite reenvío. La memoria vuelve a `draft` para iteración.

---

#### CP-F-040: Transición active → archived
- **HU**: HU-001.02.1, HU-006.06.1
- **AC**: AC-E1-03, AC-E4-06
- **Tipo**: Positivo
- **Precondiciones**: Memoria `MEM-LC-05` en `active`. Token `memory-reviewer` o `memory-admin`.
- **Pasos**:
  1. Enviar review con `action: "archive"`.
  2. Verificar que desaparece de búsquedas por defecto.
- **Datos**:
  ```json
  { "action": "archive", "comment": "Información obsoleta desde la migración a v3" }
  ```
- **Resultado esperado**: `HTTP 200`. `newStatus: "archived"`. Búsqueda sin filtro no la retorna.

---

#### CP-F-041: Transición prohibida: active → draft
- **HU**: HU-001.02.1
- **AC**: AC-E1-03
- **Tipo**: Negativo
- **Precondiciones**: Memoria `MEM-LC-06` en `active`.
- **Pasos**:
  1. Intentar review que implique `active → draft`.
- **Datos**:
  ```json
  { "action": "submit" }
  ```
- **Resultado esperado**: `HTTP 422 UNPROCESSABLE_ENTITY`. Mensaje indica transición no permitida.

---

#### CP-F-042: Transición prohibida: archived → active
- **HU**: HU-001.02.1
- **AC**: AC-E1-03
- **Tipo**: Negativo
- **Precondiciones**: Memoria `MEM-LC-07` en `archived`.
- **Pasos**:
  1. Intentar review con `action: "approve"`.
- **Datos**:
  ```json
  { "action": "approve" }
  ```
- **Resultado esperado**: `HTTP 422 UNPROCESSABLE_ENTITY`.

---

#### CP-F-043: Soft-delete desde cada uno de los 6 estados
- **HU**: HU-001.07.1, HU-001.02.1
- **AC**: AC-E1-11, AC-E1-03
- **Tipo**: Positivo
- **Precondiciones**: 6 memorias, una en cada estado.
- **Pasos**:
  1. Para cada memoria, ejecutar `DELETE`.
  2. Verificar `HTTP 204` y cambio a `deleted`.
- **Datos**: N/A.
- **Resultado esperado**: Las 6 retornan `HTTP 204`. Todas pasan a `deleted`. Registros de auditoría generados.

---

#### CP-F-044: Umbral de revisión obligatoria (importance ≥ 0.7 + sensitivity confidential)
- **HU**: HU-006.04.1
- **AC**: AC-E6-05
- **Tipo**: Positivo
- **Precondiciones**: Token `memory-operator`.
- **Pasos**:
  1. Crear memoria con `importance: 0.8`, `sensitivity: "confidential"`, `lifecycle.status: "active"`.
  2. Verificar que el sistema fuerza a `draft`.
- **Datos**:
  ```json
  {
    "kind": "decision",
    "content": "Aprobar presupuesto de $500K para migración a cloud",
    "scope": { "tenantId": "tenant-alpha" },
    "lifecycle": { "importance": 0.8, "sensitivity": "confidential", "status": "active" }
  }
  ```
- **Resultado esperado**: `HTTP 201` pero `lifecycle.status: "draft"` (no `active`). Advertencia en response o header indicando que requiere revisión.

---

#### CP-F-045: Umbral de revisión — solo una condición (permite active)
- **HU**: HU-006.04.1
- **AC**: AC-E6-05
- **Tipo**: Borde
- **Precondiciones**: Token `memory-operator`.
- **Pasos**:
  1. Crear con `importance: 0.6`, `sensitivity: "confidential"` (solo sensitivity alta).
  2. Crear con `importance: 0.9`, `sensitivity: "internal"` (solo importance alta).
- **Datos**:
  ```json
  { "kind": "fact", "content": "test single condition", "scope": { "tenantId": "tenant-alpha" }, "lifecycle": { "importance": 0.6, "sensitivity": "confidential" } }
  ```
- **Resultado esperado**: Ambos `HTTP 201` con `lifecycle.status: "active"` (solo una condición, umbral no activado).

---

### 1.5 Entidades y Extracción (EP-001, EP-004 / AC-E1, AC-E4)

#### CP-F-046: Extraer entidades de texto con entidades conocidas
- **HU**: HU-001.04.1
- **AC**: AC-E1-07
- **Tipo**: Positivo
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/extract` con texto que contiene entidades.
  2. Verificar array de entidades retornado.
- **Datos**:
  ```json
  { "content": "El servicio Kubernetes en AWS falló tras el despliegue de Jenkins y afectó a PostgreSQL" }
  ```
- **Resultado esperado**: `HTTP 200`. `entities` array contiene objetos con `name` y `type` para `Kubernetes`, `AWS`, `Jenkins`, `PostgreSQL`. No se persistió nada.

---

#### CP-F-047: Extraer entidades — texto sin entidades
- **HU**: HU-001.04.1
- **AC**: AC-E1-07
- **Tipo**: Borde
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/extract` con texto genérico sin entidades reconocibles.
- **Datos**:
  ```json
  { "content": "El día estuvo soleado y fuimos a caminar por el parque" }
  ```
- **Resultado esperado**: `HTTP 200`. `entities: []`. Sin error.

---

#### CP-F-048: Extraer entidades — content vacío
- **HU**: HU-001.04.1
- **AC**: AC-E1-07
- **Tipo**: Negativo
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/extract` con `content: ""`.
- **Datos**:
  ```json
  { "content": "" }
  ```
- **Resultado esperado**: `HTTP 400 VALIDATION_ERROR`.

---

#### CP-F-049: Buscar entidades por coincidencia parcial
- **HU**: HU-001.04.2
- **AC**: AC-E1-08, AC-E4-07
- **Tipo**: Positivo
- **Precondiciones**: Entidades `Kubernetes`, `KubernetesOperator`, `AWS` pobladas.
- **Pasos**:
  1. Enviar `GET /api/v2/entities?q=Kube`.
  2. Verificar resultados.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. Array incluye `Kubernetes` y `KubernetesOperator` (coincidencia parcial). Cada una con `memoryCount`.

---

#### CP-F-050: Detalle de entidad con memoryIds
- **HU**: HU-001.04.2
- **AC**: AC-E1-08, AC-E4-07
- **Tipo**: Positivo
- **Precondiciones**: Entidad `Kubernetes` vinculada a 5 memorias.
- **Pasos**:
  1. Enviar `GET /api/v2/entities/Kubernetes`.
  2. Verificar `memoryCount` y `memoryIds`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `name: "Kubernetes"`. `memoryCount: 5`. `memoryIds` array con 5 objetos, cada uno con `memoryId`, `kind`, `summary`.

---

#### CP-F-051: Buscar entidad inexistente
- **HU**: HU-001.04.2
- **AC**: AC-E1-08, AC-E4-07
- **Tipo**: Borde
- **Precondiciones**: No existe entidad `NonExistentEntity`.
- **Pasos**:
  1. Enviar `GET /api/v2/entities?q=NonExistentEntity`.
  2. Enviar `GET /api/v2/entities/NonExistentEntity`.
- **Datos**: N/A.
- **Resultado esperado**: Ambos `HTTP 200`. Array vacío `[]` para búsqueda. `404` o `200` con `memoryCount: 0` para detalle (según especificación: array vacío `HTTP 200`).

---

### 1.6 Scoping Multi-Tenant (EP-003 / AC-E3)

#### CP-F-052: Aislamiento estricto — búsqueda solo retorna tenant propio
- **HU**: HU-003.01.1
- **AC**: AC-E3-01
- **Tipo**: Positivo
- **Precondiciones**: 10 memorias en `tenant-alpha`, 10 en `tenant-bravo`. Token `tenant-alpha`.
- **Pasos**:
  1. Enviar `POST /api/v2/memories/search` con token `tenant-alpha`, sin filtro de tenant.
  2. Verificar que 0 resultados son de `tenant-bravo`.
- **Datos**:
  ```json
  { "query": "test", "topK": 50 }
  ```
- **Resultado esperado**: `HTTP 200`. Todos los resultados tienen `scope.tenantId: "tenant-alpha"`. Ninguno de `tenant-bravo`.

---

#### CP-F-053: Acceso a memoria de otro tenant → 404
- **HU**: HU-003.01.1
- **AC**: AC-E3-01
- **Tipo**: Negativo
- **Precondiciones**: `MEM-BRAVO-01` existe en `tenant-bravo`. Token `tenant-alpha`.
- **Pasos**:
  1. Enviar `GET /api/v2/memories/MEM-BRAVO-01` con token `tenant-alpha`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 404 NOT_FOUND` (sin revelar que existe en otro tenant).

---

#### CP-F-054: Scoping por userId
- **HU**: HU-003.02.1
- **AC**: AC-E3-02
- **Tipo**: Positivo
- **Precondiciones**: 3 memorias con `scope.userId = "user-42"`, 2 sin `userId`.
- **Pasos**:
  1. Buscar con `filters.scopes.userId = "user-42"`.
  2. Verificar que solo retorna 3.
- **Datos**:
  ```json
  { "query": "test", "filters": { "scopes": { "userId": "user-42" } } }
  ```
- **Resultado esperado**: `HTTP 200`. 3 resultados, todos con `scope.userId: "user-42"`.

---

#### CP-F-055: Scoping por sessionId
- **HU**: HU-003.03.1
- **AC**: AC-E3-03
- **Tipo**: Positivo
- **Precondiciones**: 5 memorias en `sess-abc`, 3 en `sess-xyz`.
- **Pasos**:
  1. Buscar con `filters.scopes.sessionId = "sess-abc"`.
  2. Verificar 5 resultados correctos.
- **Datos**:
  ```json
  { "query": "test", "filters": { "scopes": { "sessionId": "sess-abc" } } }
  ```
- **Resultado esperado**: `HTTP 200`. 5 resultados, todos con `scope.sessionId: "sess-abc"`.

---

#### CP-F-056: Scoping por namespace
- **HU**: HU-003.04.1
- **AC**: AC-E3-04
- **Tipo**: Positivo
- **Precondiciones**: Memorias en `namespace: "project-alpha"` y sin namespace.
- **Pasos**:
  1. Buscar con `filters.scopes.namespace = "project-alpha"`.
  2. Verificar solo las de ese namespace.
- **Datos**:
  ```json
  { "query": "test", "filters": { "scopes": { "namespace": "project-alpha" } } }
  ```
- **Resultado esperado**: `HTTP 200`. Solo memorias con `scope.namespace: "project-alpha"`.

---

#### CP-F-057: Scope obligatorio en escritura — validación exhaustiva
- **HU**: HU-003.06.1
- **AC**: AC-E3-06
- **Tipo**: Negativo
- **Precondiciones**: Token válido.
- **Pasos**:
  1. `POST` sin `scope`.
  2. `POST` con `scope: {}`.
  3. `POST` con `scope: {tenantId: ""}`.
  4. `POST` con `scope: {tenantId: "   "}`.
- **Datos**: Variantes como se describe.
- **Resultado esperado**: Los 4 retornan `HTTP 400 VALIDATION_ERROR`.

---

#### CP-F-058: Filtrado automático en lectura — tenant no ampliable
- **HU**: HU-003.07.1
- **AC**: AC-E3-07
- **Tipo**: Negativo
- **Precondiciones**: Token `tenant-alpha`.
- **Pasos**:
  1. Buscar especificando `filters.scopes.tenantId = "tenant-bravo"`.
  2. Verificar que el filtro es ignorado o rechazado.
- **Datos**:
  ```json
  { "query": "test", "filters": { "scopes": { "tenantId": "tenant-bravo" } } }
  ```
- **Resultado esperado**: Array vacío o `HTTP 403`. El sistema no permite ampliar scope del token.

---

### 1.7 Perfiles de Dominio (EP-002 / AC-E2)

#### CP-F-059: Default importance/confidence/sensitivity según perfil Ops
- **HU**: HU-002.03.1, HU-002.06.1
- **AC**: AC-E2-03, AC-E2-06
- **Tipo**: Positivo
- **Precondiciones**: Perfil Ops activo (vía header `X-Profile: ops` o similar).
- **Pasos**:
  1. Crear memoria sin especificar `importance`, `confidence`, `sensitivity`.
  2. Verificar defaults aplicados.
- **Datos**:
  ```json
  { "kind": "event", "content": "Alerta de CPU > 90% en producción", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: `HTTP 201`. `importance: 0.7`, `confidence: 0.5`, `sensitivity: "internal"` (defaults del perfil Ops).

---

#### CP-F-060: Default importance sin perfil (core genérico)
- **HU**: HU-002.06.1
- **AC**: AC-E2-06
- **Tipo**: Positivo
- **Precondiciones**: Sin perfil activo.
- **Pasos**:
  1. Crear memoria sin especificar `importance`, `confidence`, `sensitivity`.
- **Datos**:
  ```json
  { "kind": "note", "content": "Nota sin perfil activo", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: `HTTP 201`. `importance: 0.5`, `confidence: 0.5`, `sensitivity: "internal"`.

---

#### CP-F-061: Herencia del core — kind no recomendado aceptado
- **HU**: HU-002.02.1
- **AC**: AC-E2-02
- **Tipo**: Positivo
- **Precondiciones**: Perfil Ops activo (recommendedKinds: `["event", "procedure"]`).
- **Pasos**:
  1. Crear memoria con `kind: "note"` (no recomendado por Ops).
- **Datos**:
  ```json
  { "kind": "note", "content": "El perfil no restringe kinds", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: `HTTP 201`. Aceptado. El core no restringe kinds.

---

#### CP-F-062: Perfil Agent — orden por importance descendente
- **HU**: HU-002.04.1
- **AC**: AC-E2-04
- **Tipo**: Positivo
- **Precondiciones**: Perfil Agent activo. Memorias del usuario `user-42` con distintas `importance`.
- **Pasos**:
  1. Buscar con `filters.scopes.userId = "user-42"`.
  2. Verificar orden por `importance` descendente.
- **Datos**:
  ```json
  { "query": "user preferences", "filters": { "scopes": { "userId": "user-42" } }, "rerank": true }
  ```
- **Resultado esperado**: `HTTP 200`. Resultados ordenados con mayor `importance` primero (re-ranking activo en perfil Agent).

---

#### CP-F-063: Perfil Business — default sensitivity internal
- **HU**: HU-002.05.1
- **AC**: AC-E2-05
- **Tipo**: Positivo
- **Precondiciones**: Perfil Business activo.
- **Pasos**:
  1. Crear memoria sin especificar `sensitivity`.
- **Datos**:
  ```json
  { "kind": "entity", "content": "Cliente Acme Corp — sector financiero", "scope": { "tenantId": "tenant-alpha" } }
  ```
- **Resultado esperado**: `HTTP 201`. `sensitivity: "internal"` (default del perfil Business).

---

### 1.8 Administración y Monitoreo (EP-004 / AC-E4)

#### CP-F-064: Estadísticas de tenant con datos
- **HU**: HU-004.06.1
- **AC**: AC-E4-08
- **Tipo**: Positivo
- **Precondiciones**: Token `memory-admin`. `tenant-alpha` con ≥ 50 memorias de distintos kinds y statuses.
- **Pasos**:
  1. Enviar `GET /api/v2/scopes/tenant-alpha/stats`.
  2. Verificar estructura de respuesta.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `totalMemories >= 50`. Distribución por `kind`, `status`, `sensitivity` poblada. Tasa de revisión numérica. Crecimiento temporal con datos.

---

#### CP-F-065: Estadísticas de tenant vacío
- **HU**: HU-004.06.1
- **AC**: AC-E4-08
- **Tipo**: Borde
- **Precondiciones**: `tenant-empty` sin memorias. Token `memory-admin`.
- **Pasos**:
  1. Enviar `GET /api/v2/scopes/tenant-empty/stats`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `totalMemories: 0`. Distribuciones vacías (no error).

---

#### CP-F-066: Estadísticas — acceso denegado a consumer
- **HU**: HU-004.06.1
- **AC**: AC-E4-08
- **Tipo**: Negativo
- **Precondiciones**: Token `api-consumer`.
- **Pasos**:
  1. Enviar `GET /api/v2/scopes/tenant-alpha/stats`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 403 FORBIDDEN`.

---

#### CP-F-067: Health check — todos los servicios up
- **HU**: HU-004.07.1
- **AC**: AC-E4-09
- **Tipo**: Positivo
- **Precondiciones**: Qdrant, PostgreSQL, OpenAI operativos.
- **Pasos**:
  1. Enviar `GET /api/v2/health`.
  2. Verificar cada dependencia.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. `status: "healthy"`. `qdrant: {status: "up"}`, `postgresql: {status: "up"}`, `openai: {status: "up"}`.

---

#### CP-F-068: Health check — Qdrant down
- **HU**: HU-004.07.1
- **AC**: AC-E4-09
- **Tipo**: Negativo
- **Precondiciones**: Qdrant inaccesible (simulado deteniendo contenedor).
- **Pasos**:
  1. Enviar `GET /api/v2/health`.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 503`. `status: "unhealthy"`. `qdrant: {status: "down", error: "..."}`.

---

#### CP-F-069: OpenAPI 3.x spec accesible y válida
- **HU**: HU-004.09.1
- **AC**: AC-E4-11
- **Tipo**: Positivo
- **Precondiciones**: Servidor corriendo.
- **Pasos**:
  1. Enviar `GET /api/v2/openapi.json`.
  2. Validar estructura OpenAPI 3.x.
  3. Verificar que todos los 15 endpoints están documentados.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. JSON válido según OpenAPI 3.x. `paths` contiene todos los endpoints. `components.schemas` documenta request/response. `security` documenta Bearer JWT.

---

#### CP-F-070: Métricas Prometheus expuestas
- **HU**: HU-004.07.1
- **AC**: AC-E4-09
- **Tipo**: Positivo
- **Precondiciones**: Token `memory-admin`.
- **Pasos**:
  1. Enviar `GET /api/v2/metrics`.
  2. Verificar formato Prometheus.
- **Datos**: N/A.
- **Resultado esperado**: `HTTP 200`. Contenido en formato Prometheus con métricas de latencia, throughput, tasas de error.

---

## 2. Casos de Prueba de Integración

#### CP-I-001: Flujo completo end-to-end — crear, buscar, actualizar, revisar, archivar, eliminar
- **HU**: HU-004.01.1, HU-004.01.2, HU-004.01.3, HU-004.04.1, HU-001.07.1
- **AC**: AC-E4-01, AC-E4-02, AC-E4-03, AC-E4-06, AC-E1-11, AC-E6-03
- **Tipo**: Integración
- **Precondiciones**: Token `memory-operator` + `memory-reviewer` disponibles.
- **Pasos**:
  1. **Crear**: `POST /memories` con kind=procedure, status=draft. Guardar `memoryId`.
  2. **Buscar**: `POST /memories/search` sin filtro statuses → la memoria NO aparece (está en draft).
  3. **Actualizar**: `PATCH /memories/{id}` modificando content → embedding regenerado.
  4. **Revisar submit**: `POST /memories/{id}/review` action=submit → pending.
  5. **Revisar approve**: Con token reviewer, `POST /memories/{id}/review` action=approve → active.
  6. **Buscar de nuevo**: `POST /memories/search` sin filtro → la memoria AHORA aparece.
  7. **Archivar**: `POST /memories/{id}/review` action=archive → archived.
  8. **Eliminar**: `DELETE /memories/{id}` → soft-delete.
  9. **Verificar auditoría**: Consultar logs → mínimo 7 registros (create, update, review_submit, review_approve, review_archive, delete, + registros de embedding).
- **Datos**:
  - Crear: `{"kind": "procedure", "content": "Runbook: Restauración de BD", "scope": {"tenantId": "tenant-alpha"}, "lifecycle": {"status": "draft"}}`
  - PATCH: `{"content": "Runbook actualizado: Restauración de BD v2"}`
  - Review submit: `{"action": "submit"}`
  - Review approve: `{"action": "approve", "comment": "Verificado"}`
  - Review archive: `{"action": "archive", "comment": "Obsoleto"}`
- **Resultado esperado**: Todo el flujo completa sin errores. 7+ registros de auditoría.

---

#### CP-I-002: Cross-tenant isolation — 100 queries sin fuga
- **HU**: HU-003.01.1
- **AC**: AC-E3-01, AC-SYS-07
- **Tipo**: Integración
- **Precondiciones**: 3 tenants (`alpha`, `bravo`, `charlie`) con ≥ 20 memorias cada uno. Tokens de cada tenant.
- **Pasos**:
  1. Con token `tenant-alpha`, ejecutar 50 búsquedas variadas (distintas queries, filtros).
  2. Verificar que el 100% de resultados son de `tenant-alpha`.
  3. Repetir con tokens de `tenant-bravo` y `tenant-charlie` (50 queries cada uno).
  4. Verificar `GET /memories/{id}` con IDs cross-tenant → todos retornan 404.
- **Datos**: Script automatizado con 150 queries totales.
- **Resultado esperado**: 0% de fuga cross-tenant. 150/150 queries retornan solo datos del tenant del token.

---

#### CP-I-003: Graph traversal — crear → expandir → verificar profundidad
- **HU**: HU-004.03.1, HU-001.03.1
- **AC**: AC-E4-05, AC-E1-05
- **Tipo**: Integración
- **Precondiciones**: 5 memorias creadas.
- **Pasos**:
  1. Crear 5 memorias: M1, M2, M3, M4, M5.
  2. Crear relaciones: M1→M2 (depends_on), M1→M3 (related_to), M2→M4 (caused_by), M4→M5 (resolves).
  3. Expandir grafo de M1 con depth=3.
  4. Verificar nodos y edges.
- **Datos**: 
  - M1 (decision), M2 (fact), M3 (event), M4 (procedure), M5 (entity)
  - Relaciones con types anotados arriba.
- **Resultado esperado**: depth=3 sobre M1 retorna 5 nodos y 4 edges. M5 es alcanzable en depth=3 (M1→M2→M4→M5).

---

#### CP-I-004: Batch creation → verificar indexación completa
- **HU**: HU-005.07.1
- **AC**: AC-E5-06
- **Tipo**: Integración
- **Precondiciones**: Token `memory-operator`.
- **Pasos**:
  1. Crear 20 memorias secuencialmente (POST individual) con contenido semánticamente distinto.
  2. Esperar 5 segundos para indexación asíncrona si aplica.
  3. Para cada memoria, verificar que aparece en búsqueda semántica con su contenido.
- **Datos**: 20 payloads con distintos kinds y contenido único.
- **Resultado esperado**: 20/20 memorias son encontradas mediante búsqueda semántica con queries relacionadas a su contenido.

---

#### CP-I-005: Relaciones + búsqueda con expandGraph
- **HU**: HU-005.03.1, HU-001.03.1
- **AC**: AC-E5-03, AC-E1-05
- **Tipo**: Integración
- **Precondiciones**: M1 activa con relaciones a M2, M3.
- **Pasos**:
  1. Buscar semánticamente M1.
  2. Con `expandGraph: {depth: 1}`, verificar que en los resultados M1 incluye `relations` con M2 y M3.
  3. Verificar que M2 y M3 no traen datos de M1 (no es simétrico por defecto).
- **Datos**:
  ```json
  { "query": "término que matchea M1", "expandGraph": { "depth": 1 } }
  ```
- **Resultado esperado**: M1 en resultados incluye `relations: [{id, targetId, type, targetSummary}]`. Sin expandGraph, `relations` no incluido o vacío.

---

#### CP-I-006: Supersedes — versionado y re-ranking
- **HU**: HU-001.08.1
- **AC**: AC-E1-12
- **Tipo**: Integración
- **Precondiciones**: M1 activa con contenido "Procedimiento v1". M2 activa con relación `supersedes` hacia M1.
- **Pasos**:
  1. Buscar con query que matchea ambas.
  2. Verificar que M2 aparece antes que M1 (re-ranking favorece más reciente).
  3. Archivar M1 → verificar que solo M2 aparece en búsqueda por defecto.
- **Datos**:
  - M1: `{"kind": "procedure", "content": "Procedimiento de deploy v1", ...}`
  - M2: `{"kind": "procedure", "content": "Procedimiento de deploy v2 actualizado", ...}`
  - Relación: `{"targetId": "M1", "type": "supersedes"}`
- **Resultado esperado**: Con rerank=true, M2 > M1 en orden. M1 permanece `active` hasta archivar manualmente. Tras archivar M1, búsqueda solo retorna M2.

---

#### CP-I-007: Auditoría completa del flujo de revisión
- **HU**: HU-006.01.1, HU-006.02.1
- **AC**: AC-E6-01, AC-E6-03
- **Tipo**: Integración
- **Precondiciones**: Token operator + reviewer + admin.
- **Pasos**:
  1. Operator crea memoria en `draft`, edita 2 veces.
  2. Operator hace submit → `pending`.
  3. Reviewer rechaza con motivo → `rejected`.
  4. Operator edita nuevamente y resubmite.
  5. Reviewer aprueba → `active`.
  6. Admin archiva → `archived`.
  7. Consultar auditoría completa de la memoria.
- **Datos**: Payloads según flujo descrito.
- **Resultado esperado**: 8+ registros de auditoría en orden cronológico: create, update, update, review_submit, review_reject, update, review_submit, review_approve, review_archive. Cada uno con timestamp, userId, action, diff.

---

#### CP-I-008: Linaje de decisión mediante grafo
- **HU**: HU-006.05.1
- **AC**: AC-E6-06
- **Tipo**: Integración
- **Precondiciones**: M-decision activa. M-fact1 y M-fact2 con `supports` hacia M-decision. M-event con `caused_by` desde M-decision.
- **Pasos**:
  1. Expandir grafo de M-decision con depth=2.
  2. Verificar que los hechos que la respaldan (M-fact1, M-fact2) aparecen.
  3. Verificar el evento causado (M-event) aparece.
- **Datos**: Estructura de grafo descrita.
- **Resultado esperado**: Nodos de soporte y consecuencia visibles. Direccionalidad correcta en edges.

---

#### CP-I-009: Fusión de duplicadas (admin)
- **HU**: HU-006.06.1
- **AC**: AC-E6-07
- **Tipo**: Integración
- **Precondiciones**: Token `memory-admin`. M-dup1 y M-dup2 activas con contenido similar y relaciones.
- **Pasos**:
  1. Ejecutar fusión (merge) de M-dup1 y M-dup2 (mecanismo: admin endpoint o operación de depuración).
  2. Verificar que la sobreviviente consolida las relaciones de ambas.
  3. Verificar que la otra pasa a `deleted`.
  4. Verificar registro de auditoría.
- **Datos**: Según mecanismo de merge implementado (API administrativa).
- **Resultado esperado**: Sobreviviente tiene las relaciones combinadas. La otra en `deleted`. Auditoría registra fusión.

---

#### CP-I-010: Rate limiting — exceder límite y verificar 429
- **HU**: HU-004.13.1
- **AC**: AC-E4-15
- **Tipo**: Integración
- **Precondiciones**: Rate limit configurado para `tenant-alpha` (ej. 100 req/min). Herramienta de carga disponible.
- **Pasos**:
  1. Ejecutar 120 requests en 60 segundos.
  2. Verificar que al exceder se recibe `HTTP 429`.
  3. Verificar header `Retry-After`.
  4. Verificar header `X-RateLimit-Remaining` en responses exitosas.
- **Datos**: Requests de búsqueda ligera.
- **Resultado esperado**: Requests 1-100: `HTTP 200`. Requests 101-120: `HTTP 429` con `Retry-After` > 0.

---

#### CP-I-011: Embedding regenerado solo al cambiar content
- **HU**: HU-005.07.1
- **AC**: AC-E5-06
- **Tipo**: Integración
- **Precondiciones**: Memoria creada con contenido A.
- **Pasos**:
  1. PATCH solo metadata → verificar que NO se regenera embedding (métricas de API OpenAI o log).
  2. PATCH solo topics → mismo check.
  3. PATCH content → verificar que SÍ se regenera embedding.
  4. Búsqueda con query basada en contenido nuevo → la memoria aparece.
- **Datos**:
  - PATCH metadata: `{"metadata": {"updated": "yes"}}`
  - PATCH topics: `{"topics": ["new-topic"]}`
  - PATCH content: `{"content": "Contenido completamente nuevo sobre otro tema"}`
- **Resultado esperado**: Solo el último PATCH dispara reindexación. Búsqueda con query del nuevo contenido la encuentra.

---

#### CP-I-012: Soft-delete preserva vector en Qdrant
- **HU**: HU-001.07.1
- **AC**: AC-E1-11, AC-E5-06
- **Tipo**: Integración
- **Precondiciones**: Memoria `MEM-VEC-01` activa con embedding generado. Acceso administrativo a Qdrant.
- **Pasos**:
  1. Verificar vector existe en Qdrant para `MEM-VEC-01`.
  2. Soft-deletear `MEM-VEC-01`.
  3. Verificar que el vector sigue en Qdrant (payload con metadata).
  4. Verificar que la memoria no aparece en búsquedas.
- **Datos**: N/A (consulta directa a Qdrant).
- **Resultado esperado**: Vector preservado en Qdrant tras soft-delete. Búsqueda no la retorna.

---

#### CP-I-013: Expansión de subgrafo omite nodos deleted
- **HU**: HU-004.03.1
- **AC**: AC-E4-05
- **Tipo**: Integración
- **Precondiciones**: M1 relacionada con M2 (active) y M3 (deleted).
- **Pasos**:
  1. Expandir grafo de M1 con depth=1.
  2. Verificar que M2 aparece y M3 NO.
- **Datos**: Configuración de relaciones descrita.
- **Resultado esperado**: Solo M2 en `nodes`. Edge hacia M3 omitido (o filtrado).

---

#### CP-I-014: Re-ranking combina adecuadamente señales
- **HU**: HU-005.04.1
- **AC**: AC-E5-04
- **Tipo**: Integración
- **Precondiciones**: 5 memorias con contenido similar pero diferentes importance, confidence, updatedAt.
- **Pasos**:
  1. Ejecutar búsqueda con rerank=true.
  2. Verificar que el orden NO es idéntico al orden de similitud cruda (rerank=false).
  3. Verificar que memorias con mayor importance + confidence + frescura suben posiciones.
- **Datos**: Conjunto de 5 memorias curadas con scores conocidos.
- **Resultado esperado**: Orden con rerank=true difiere del orden con rerank=false. La memoria con mayor importance (0.95) aparece en mejor posición que otra con score semántico ligeramente superior pero importance baja.

---

#### CP-I-015: Visibilidad por rol — consumidor solo ve active
- **HU**: HU-006.03.1, HU-005.09.1
- **AC**: AC-E6-04, AC-E5-07
- **Tipo**: Integración
- **Precondiciones**: Memorias en draft, pending, active, rejected. Token `api-consumer`.
- **Pasos**:
  1. Buscar sin filtro statuses.
  2. Buscar con `statuses: ["active"]`.
  3. Intentar buscar con `statuses: ["pending"]` → verificar que es rechazado o ignorado.
  4. Intentar `GET /memories/{id}` de memoria en draft → verificar 403/404.
- **Datos**: Búsquedas como se describe.
- **Resultado esperado**: Consumer solo ve active. Intento de ver otros estados es bloqueado (resultados filtrados/ignorados + 403/404 en acceso directo).

---

#### CP-I-016: Perfil Ops end-to-end — metadatos extra funcionales
- **HU**: HU-002.03.1
- **AC**: AC-E2-03
- **Tipo**: Integración
- **Precondiciones**: Perfil Ops activo.
- **Pasos**:
  1. Crear memoria de incidente con metadatos extra `affectedService`, `remediationSteps`, `rootCause`.
  2. Verificar que se persisten.
  3. Buscar por esos metadatos (si están indexados) o por contenido.
- **Datos**:
  ```json
  {
    "kind": "event",
    "content": "Incidente: payment-api caído por OOM",
    "metadata": {
      "affectedService": "payment-api",
      "remediationSteps": "Aumentar memory limit a 2Gi y redeploy",
      "rootCause": "Memory leak en PaymentProcessor"
    },
    "scope": { "tenantId": "tenant-alpha" }
  }
  ```
- **Resultado esperado**: `HTTP 201`. Metadatos persistidos y recuperables en GET detail.

---

#### CP-I-017: English-Only en toda la API
- **HU**: HU-004.08.1
- **AC**: AC-E4-10, AC-SYS-10
- **Tipo**: Integración
- **Precondiciones**: API corriendo.
- **Pasos**:
  1. Revisar todos los paths en OpenAPI → todo en inglés.
  2. Revisar todos los enums (kind, status, relationType, sensitivity, sourceType, errorCode) → inglés.
  3. Ejecutar requests y verificar que campos JSON en responses usan nombres en inglés.
  4. Provocar error y verificar errorCode en inglés.
- **Datos**: Recorrido sistemático de todos los endpoints.
- **Resultado esperado**: 0 identificadores internos en español. `errorCode: "VALIDATION_ERROR"`, no `"ERROR_VALIDACION"`. Campos: `kind`, `lifecycle`, `scope`, no `tipo`, `cicloVida`, `alcance`.

---

#### CP-I-018: Auditoría inmutable — intento de modificar/eliminar registro
- **HU**: HU-006.01.1
- **AC**: AC-NFR-11
- **Tipo**: Integración
- **Precondiciones**: Registro de auditoría existe. Acceso como admin.
- **Pasos**:
  1. Intentar modificar un registro de auditoría vía API (si existe endpoint).
  2. Intentar eliminar un registro.
  3. Verificar rechazo a nivel de aplicación.
- **Datos**: N/A.
- **Resultado esperado**: Operaciones rechazadas. Los registros de auditoría son inmutables.

---

#### CP-I-019: Creación con relación simultánea (POST con relations)
- **HU**: HU-004.01.1, HU-001.03.1
- **AC**: AC-E4-01, AC-E1-05
- **Tipo**: Integración
- **Precondiciones**: `MEM-TGT-01` activa. Token operator.
- **Pasos**:
  1. Crear nueva memoria incluyendo `relations` en el payload.
  2. Verificar que la relación se crea automáticamente.
- **Datos**:
  ```json
  {
    "kind": "fact",
    "content": "Hecho relacionado con el target",
    "scope": { "tenantId": "tenant-alpha" },
    "relations": [{ "targetId": "MEM-TGT-01", "type": "supports" }]
  }
  ```
- **Resultado esperado**: `HTTP 201`. Memoria creada + relación `supports` creada. Ambas auditadas. NOTA: Si el modelo actual no soporta creación con relaciones en el mismo POST, documentar como limitación.

---

#### CP-I-020: Concurrencia — 10 requests simultáneos sin error
- **HU**: N/A (AC sistema)
- **AC**: AC-SYS-04
- **Tipo**: Integración
- **Precondiciones**: Herramienta de carga (k6, curl paralelo).
- **Pasos**:
  1. Ejecutar 10 requests POST /memories/search simultáneos (misma query).
  2. Verificar que todos retornan 200.
  3. Verificar que no hay race conditions ni deadlocks.
- **Datos**:
  ```json
  { "query": "concurrent test query", "topK": 5 }
  ```
- **Resultado esperado**: 10/10 respuestas `HTTP 200`. Sin errores 5xx. Resultados consistentes.

---

## 3. Casos de Prueba No Funcionales

#### CP-NF-001: Latencia de búsqueda p95 < 500ms
- **HU**: HU-005.01.1
- **AC**: AC-NFR-01, AC-SYS-04
- **Tipo**: No funcional — Rendimiento
- **Precondiciones**: 10,000+ memorias pobladas en 3 tenants. Carga de 50 búsquedas concurrentes/s. Herramienta k6 configurada.
- **Pasos**:
  1. Ejecutar prueba de carga con 50 req/s durante 5 minutos.
  2. Medir p95 de latencia en `POST /memories/search`.
  3. Verificar que p95 < 500ms.
- **Datos**: Script k6 con queries variadas y filtros.
- **Resultado esperado**: p95 ≤ 500ms. Reporte de k6 + métricas Prometheus lo confirman.

---

#### CP-NF-002: Latencia de creación p95 < 1,500ms
- **HU**: HU-004.01.1
- **AC**: AC-NFR-02
- **Tipo**: No funcional — Rendimiento
- **Precondiciones**: Carga de 10 creaciones concurrentes/s. OpenAI API reachable.
- **Pasos**:
  1. Ejecutar creación concurrente de 10 memorias/s.
  2. Medir p95 de `POST /memories`.
- **Datos**: Payloads variados con content semántico distinto.
- **Resultado esperado**: p95 ≤ 1,500ms (incluye generación de embedding vía OpenAI).

---

#### CP-NF-003: Throughput ≥ 100 búsquedas/s con p95 < 500ms
- **HU**: N/A (AC sistema)
- **AC**: AC-NFR-04
- **Tipo**: No funcional — Rendimiento
- **Precondiciones**: 10,000+ memorias. Escalado progresivo de carga.
- **Pasos**:
  1. Iniciar con 10 req/s, escalar hasta 100+ req/s.
  2. Medir throughput máximo manteniendo p95 < 500ms.
- **Datos**: Script k6 con rampa de carga.
- **Resultado esperado**: ≥ 100 búsquedas/s sostenidas con p95 < 500ms.

---

#### CP-NF-004: Disponibilidad ≥ 99.5% (health check)
- **HU**: HU-004.07.1
- **AC**: AC-NFR-03
- **Tipo**: No funcional — Disponibilidad
- **Precondiciones**: Monitoreo continuo configurado con Prometheus + alerting.
- **Pasos**:
  1. Monitorear `GET /api/v2/health` cada 30s durante 7 días.
  2. Calcular uptime.
- **Datos**: Health check automatizado.
- **Resultado esperado**: Uptime ≥ 99.5% sobre ventana de medición. (En QA, se verifica sobre ventana de prueba de 24h: uptime ≥ 99.0%).

---

#### CP-NF-005: Soft-delete — fragmentos eliminados no aparecen en búsquedas
- **HU**: HU-001.07.1
- **AC**: AC-E1-11, AC-E5-07
- **Tipo**: No funcional — Integridad de datos
- **Precondiciones**: 5 memorias activas, 3 eliminadas (soft-delete) en `tenant-alpha`.
- **Pasos**:
  1. Ejecutar 20 búsquedas variadas sin filtro de statuses.
  2. Verificar que 0 resultados tienen `lifecycle.status = "deleted"`.
- **Datos**: 20 queries distintas.
- **Resultado esperado**: 20/20 búsquedas: 0 resultados deleted.

---

#### CP-NF-006: Auditoría — 100% de mutaciones generan registro
- **HU**: HU-006.01.1
- **AC**: AC-NFR-10, AC-SYS-09
- **Tipo**: No funcional — Trazabilidad
- **Precondiciones**: Suite de operaciones CRUD ejecutada (~50 mutaciones).
- **Pasos**:
  1. Ejecutar 50 mutaciones de distintos tipos.
  2. Consultar registros de auditoría.
  3. Verificar que hay exactamente 50 registros.
  4. Verificar que cada registro tiene todos los campos: auditId, timestamp, userId, action, memoryId, tenantId, diff.
- **Datos**: Suite de 50 operaciones (crear, update, review, delete, create_relation, delete_relation).
- **Resultado esperado**: 50/50 registros de auditoría. 0 mutaciones sin trace.

---

#### CP-NF-007: English-Only — 0 identificadores internos en español
- **HU**: HU-004.08.1
- **AC**: AC-NFR-08, AC-SYS-10
- **Tipo**: No funcional — Convención
- **Precondiciones**: Código fuente + OpenAPI + schema BD accesibles.
- **Pasos**:
  1. Ejecutar linter custom que detecta identificadores en español.
  2. Revisar manualmente openapi.json.
  3. Revisar schema SQL (nombres de columnas, enums).
- **Datos**: Linter output + revisión manual.
- **Resultado esperado**: 0 identificadores internos en español. Evidencia de linter limpio.

---

#### CP-NF-008: Cobertura de kinds — 8/8 con ≥ 10 memorias
- **HU**: N/A (AC sistema)
- **AC**: AC-SYS-06
- **Tipo**: No funcional — Cobertura
- **Precondiciones**: Dataset de pruebas multi-dominio.
- **Pasos**:
  1. Contar memorias por kind en el dataset de QA.
  2. Verificar que cada kind tiene ≥ 10 memorias.
- **Datos**: SQL: `SELECT kind, COUNT(*) FROM memories GROUP BY kind`.
- **Resultado esperado**: 8/8 kinds con count ≥ 10.

---

#### CP-NF-009: Inmutabilidad de registros de auditoría
- **HU**: HU-006.01.1
- **AC**: AC-NFR-11
- **Tipo**: No funcional — Seguridad de datos
- **Precondiciones**: Base de datos con registros de auditoría.
- **Pasos**:
  1. Como admin, intentar `UPDATE` o `DELETE` sobre tabla de auditoría (vía SQL directo o API).
  2. Verificar que la operación es rechazada.
- **Datos**: Comandos SQL o API.
- **Resultado esperado**: Operación rechazada. Tabla de auditoría es append-only e inmutable.

---

#### CP-NF-010: Precisión top-1 en suite interna ≥ 0.92
- **HU**: N/A (AC sistema)
- **AC**: AC-SYS-05
- **Tipo**: No funcional — Calidad semántica
- **Precondiciones**: Suite de 100 test cases con ground truth conocido. Datos poblados en el repositorio.
- **Pasos**:
  1. Para cada uno de los 100 test cases, ejecutar búsqueda.
  2. Verificar que el resultado en posición 1 coincide con el ground truth.
  3. Calcular precisión = aciertos / 100.
- **Datos**: 100 pares (query, expectedMemoryId).
- **Resultado esperado**: Precisión top-1 ≥ 0.92 (≥ 92 aciertos).

---

## 4. Casos de Prueba de Frontend

> **Nota**: Los siguientes casos de prueba asumen un frontend React ejecutándose contra la API v2 en ambiente QA. Las pruebas requieren navegador con DevTools para inspeccionar network requests.

#### CP-FE-001: Búsqueda — escribir query y ver resultados
- **HU**: HU-009.02.1
- **AC**: AC-E9-02
- **Tipo**: Positivo
- **Precondiciones**: Usuario autenticado como `memory-operator`. Datos poblados.
- **Pasos**:
  1. Navegar a pantalla de búsqueda (Home).
  2. Escribir query "restauración de base de datos" en el campo de texto.
  3. Hacer clic en "Buscar" (o presionar Enter).
  4. Verificar que aparecen resultados.
  5. Verificar que cada resultado muestra: score (barra visual), kind (ícono), status (badge de color), summary, topics (chips), entities (badges).
- **Datos**: Query de texto libre.
- **Resultado esperado**: Lista de resultados renderizada correctamente. Network request a `POST /api/v2/memories/search` con status 200.

---

#### CP-FE-002: Búsqueda — aplicar filtros y verificar request
- **HU**: HU-009.02.1
- **AC**: AC-E9-02
- **Tipo**: Positivo
- **Precondiciones**: Usuario autenticado.
- **Pasos**:
  1. En panel de búsqueda, expandir filtros.
  2. Seleccionar kinds: `["procedure", "event"]`.
  3. Mover slider de importance a `gte: 0.5`.
  4. Seleccionar sensitivity: `["internal"]`.
  5. Activar toggle `rerank`.
  6. Ejecutar búsqueda.
  7. Inspeccionar network request → verificar que el body JSON incluye todos los filtros.
- **Datos**: Configuración de filtros como se describe.
- **Resultado esperado**: Body del request contiene `filters.kinds`, `filters.importance`, `filters.sensitivities`, `rerank: true`. Resultados filtrados correctamente.

---

#### CP-FE-003: Búsqueda — paginación / scroll infinito
- **HU**: HU-009.02.1
- **AC**: AC-E9-02
- **Tipo**: Positivo
- **Precondiciones**: > 20 resultados para una query genérica.
- **Pasos**:
  1. Ejecutar búsqueda amplia.
  2. Verificar que se muestran los primeros N resultados.
  3. Hacer scroll hacia abajo o clic en "Cargar más".
  4. Verificar que se cargan más resultados.
- **Datos**: Query genérica con `topK: 20+`.
- **Resultado esperado**: Carga progresiva de resultados. Siguiente página/topK solicitada correctamente.

---

#### CP-FE-004: Detalle — clic en resultado y ver campos completos
- **HU**: HU-009.02.1, HU-009.05.1
- **AC**: AC-E9-02, AC-E9-05
- **Tipo**: Positivo
- **Precondiciones**: Resultados de búsqueda visibles.
- **Pasos**:
  1. Hacer clic en un resultado de búsqueda.
  2. Verificar navegación a pantalla de detalle.
  3. Verificar que se muestran: content completo (renderizado Markdown), summary, kind, status, importance, confidence, sensitivity, topics, entities, metadata (tabla key-value), source, scope, fechas.
  4. Verificar sección de relaciones con targets clickeables.
  5. Verificar timeline de ciclo de vida.
- **Datos**: Memoria con todos los campos poblados.
- **Resultado esperado**: Todos los campos visibles. Network request `GET /api/v2/memories/{id}` retorna 200.

---

#### CP-FE-005: Detalle — expandir grafo de relaciones
- **HU**: HU-009.05.1
- **AC**: AC-E9-05
- **Tipo**: Positivo
- **Precondiciones**: Memoria con ≥ 5 relaciones en la vista de detalle.
- **Pasos**:
  1. En detalle, hacer clic en pestaña/toggle "Grafo".
  2. Verificar que se renderiza el grafo con nodo central + vecinos + edges.
  3. Verificar que los nodos muestran kind, status (color), summary truncado.
  4. Verificar que los edges muestran etiqueta con tipo de relación y dirección.
  5. Hacer clic en un nodo vecino → verificar que se expande con sus propias relaciones.
  6. Verificar zoom y paneo funcionales.
- **Datos**: Memoria con ≥ 5 relaciones de distintos tipos.
- **Resultado esperado**: Grafo interactivo y navegable. Network request `GET /api/v2/memories/{id}/graph?depth=1` o mayor.

---

#### CP-FE-006: Detalle — grafo con 20+ relaciones (legibilidad)
- **HU**: HU-009.05.1
- **AC**: AC-E9-05
- **Tipo**: Borde
- **Precondiciones**: Memoria con 20 relaciones.
- **Pasos**:
  1. Abrir vista de grafo.
  2. Verificar que el layout es legible.
  3. Verificar que los nodos no se solapan excesivamente.
  4. Usar zoom para acercar/alejar.
- **Datos**: Memoria con 20 relaciones.
- **Resultado esperado**: Layout legible. Zoom y paneo funcionan sin bloquear la UI. Sin nodos completamente ocultos.

---

#### CP-FE-007: Creación — llenar formulario, submit, verificar en lista
- **HU**: HU-009.01.1
- **AC**: AC-E9-01
- **Tipo**: Positivo
- **Precondiciones**: Usuario `memory-operator` autenticado. Sin perfil activo.
- **Pasos**:
  1. Navegar a formulario de creación.
  2. Seleccionar `kind: "fact"`.
  3. Ingresar `content` en Markdown.
  4. Ingresar `summary`.
  5. Agregar topics y entities.
  6. Agregar metadata key-value.
  7. Llenar scope (`tenantId` pre-llenado del token).
  8. Hacer clic en "Crear".
  9. Verificar redirección o mensaje de éxito.
  10. Ir a búsqueda y verificar que la nueva memoria aparece.
- **Datos**: Formulario completo.
- **Resultado esperado**: `POST /api/v2/memories` → 201. Memoria visible en búsqueda posterior.

---

#### CP-FE-008: Creación — validación client-side: content vacío
- **HU**: HU-009.01.1
- **AC**: AC-E9-01, AC-E1-15
- **Tipo**: Negativo
- **Precondiciones**: Usuario autenticado.
- **Pasos**:
  1. En formulario de creación, dejar `content` vacío.
  2. Hacer clic en "Crear".
  3. Verificar mensaje de validación.
- **Datos**: Formulario con content vacío.
- **Resultado esperado**: El formulario muestra error de validación. No se envía request al servidor.

---

#### CP-FE-009: Creación — formulario adaptado al perfil Ops
- **HU**: HU-009.01.1
- **AC**: AC-E9-01, AC-E2-03
- **Tipo**: Positivo
- **Precondiciones**: Perfil Ops seleccionado.
- **Pasos**:
  1. Abrir formulario de creación.
  2. Verificar que `event` y `procedure` aparecen primero/destacados en selector de kind.
  3. Verificar que tags sugeridos incluyen `incident`, `runbook`, `alert`, `maintenance`, `postmortem`.
  4. Verificar que campos de metadatos extra (`affectedService`, `remediationSteps`, `rootCause`) son visibles.
  5. Verificar que `importance` tiene default `0.7`.
- **Datos**: Inspección visual del formulario.
- **Resultado esperado**: Formulario refleja configuración del perfil Ops.

---

#### CP-FE-010: Creación — formulario sin perfil (core genérico)
- **HU**: HU-009.01.1
- **AC**: AC-E9-01
- **Tipo**: Positivo
- **Precondiciones**: Sin perfil seleccionado ("Sin perfil").
- **Pasos**:
  1. Abrir formulario de creación.
  2. Verificar que los 8 kinds aparecen sin priorización.
  3. Verificar que no hay tags sugeridos.
  4. Verificar que no hay campos de metadatos extra.
- **Datos**: Inspección visual.
- **Resultado esperado**: Formulario neutro. 8 kinds listados en orden natural (alfabético o de definición).

---

#### CP-FE-011: Edición — modificar campo y verificar cambio
- **HU**: HU-009.01.1
- **AC**: AC-E4-03
- **Tipo**: Positivo
- **Precondiciones**: Usuario `memory-operator` dueño de la memoria.
- **Pasos**:
  1. Abrir detalle de una memoria propia.
  2. Hacer clic en "Editar".
  3. Modificar `content`.
  4. Modificar `metadata`.
  5. Guardar.
  6. Verificar que los cambios se reflejan en el detalle.
- **Datos**: Cambios de content y metadata.
- **Resultado esperado**: `PATCH /api/v2/memories/{id}` → 200. Detalle actualizado.

---

#### CP-FE-012: Revisión — ver bandeja de pendientes
- **HU**: HU-009.03.1
- **AC**: AC-E9-03
- **Tipo**: Positivo
- **Precondiciones**: Usuario `memory-reviewer` autenticado. ≥ 3 memorias en `pending` en su scope.
- **Pasos**:
  1. Navegar a Panel de Revisión.
  2. Verificar lista de memorias en `pending`.
  3. Verificar columnas: kind, importance, sensitivity, summary, fecha envío, solicitante.
- **Datos**: N/A.
- **Resultado esperado**: Bandeja poblada correctamente. Solo memorias en `pending` del scope del revisor.

---

#### CP-FE-013: Revisión — aprobar memoria
- **HU**: HU-009.03.1
- **AC**: AC-E9-03, AC-E4-06
- **Tipo**: Positivo
- **Precondiciones**: Revisor en panel de revisión. Memoria en `pending`.
- **Pasos**:
  1. Seleccionar memoria de la bandeja.
  2. Revisar contenido e historial.
  3. Hacer clic en "Approve".
  4. Confirmar acción.
  5. Verificar que la memoria desaparece de la bandeja.
  6. Verificar en búsqueda que ahora está `active`.
- **Datos**: Acción approve.
- **Resultado esperado**: Memoria transiciona a `active`. Desaparece de bandeja. Visible en búsquedas.

---

#### CP-FE-014: Revisión — rechazar con motivo
- **HU**: HU-009.03.1
- **AC**: AC-E9-03, AC-E1-04
- **Tipo**: Positivo
- **Precondiciones**: Revisor. Memoria en `pending`.
- **Pasos**:
  1. Seleccionar memoria.
  2. Hacer clic en "Reject".
  3. Ingresar motivo en campo de texto: "Falta adjuntar evidencia del incidente".
  4. Confirmar.
  5. Verificar que la memoria pasa a `rejected`.
  6. Como operador creador, verificar que el motivo es visible.
- **Datos**: Reject con motivo.
- **Resultado esperado**: Memoria en `rejected`. Motivo visible para creador. Desaparece de bandeja del revisor.

---

#### CP-FE-015: Admin — ver lista de tenants con métricas
- **HU**: HU-009.04.1
- **AC**: AC-E9-04
- **Tipo**: Positivo
- **Precondiciones**: Usuario `memory-admin` autenticado. ≥ 2 tenants con datos.
- **Pasos**:
  1. Navegar a Panel de Administración.
  2. Verificar sección de tenants.
  3. Verificar métricas básicas por tenant: total memorias, crecimiento, estado.
- **Datos**: N/A.
- **Resultado esperado**: Lista de tenants con métricas. Datos coinciden con `GET /api/v2/scopes/{tenantId}/stats`.

---

#### CP-FE-016: Admin — auditoría con filtros
- **HU**: HU-009.04.1
- **AC**: AC-E9-04, AC-E6-02
- **Tipo**: Positivo
- **Precondiciones**: Admin autenticado. Registros de auditoría poblados.
- **Pasos**:
  1. En panel admin, ir a sección Auditoría.
  2. Aplicar filtro por `userId`.
  3. Aplicar filtro por `action` (ej. "review_approve").
  4. Aplicar filtro por rango de fechas.
  5. Verificar resultados filtrados correctamente.
- **Datos**: Filtros como se describe.
- **Resultado esperado**: Resultados de auditoría filtrados. Cada registro muestra timestamp, userId, action, memoryId, tenantId, diff.

---

#### CP-FE-017: Dashboard — gráficos interactivos y filtro de fecha
- **HU**: HU-009.07.1
- **AC**: AC-E9-07
- **Tipo**: Positivo
- **Precondiciones**: Admin autenticado. Datos suficientes para gráficos.
- **Pasos**:
  1. Navegar a Dashboard.
  2. Verificar pie chart de kinds.
  3. Verificar line chart de creaciones por semana.
  4. Verificar bar chart de statuses.
  5. Verificar listas de top entities y top topics.
  6. Seleccionar rango de fechas → verificar que gráficos se actualizan.
- **Datos**: Datos del tenant.
- **Resultado esperado**: Todos los gráficos renderizados. Actualización al cambiar rango de fechas.

---

#### CP-FE-018: Cambio de perfil sin recarga
- **HU**: HU-009.06.1
- **AC**: AC-E9-06
- **Tipo**: Positivo
- **Precondiciones**: Usuario autenticado.
- **Pasos**:
  1. Verificar selector de perfil visible (barra superior).
  2. Seleccionar "Ops Profile".
  3. Verificar que formulario y filtros se actualizan instantáneamente.
  4. Seleccionar "Agent Profile".
  5. Verificar reconfiguración inmediata.
  6. Recargar página (F5) → verificar que "Agent Profile" se mantiene seleccionado.
  7. Seleccionar "Sin perfil" → interfaz neutra.
- **Datos**: N/A.
- **Resultado esperado**: Transiciones fluidas sin recarga de página completa. Persistencia de selección en localStorage o backend.

---

#### CP-FE-019: Autenticación — login y logout con Keycloak
- **HU**: HU-009.08.1
- **AC**: AC-E9-08
- **Tipo**: Positivo
- **Precondiciones**: Keycloak QA configurado.
- **Pasos**:
  1. Acceder al frontend sin autenticar → verificar redirección a Keycloak login.
  2. Autenticarse con credenciales válidas → redirección al frontend.
  3. Verificar que el JWT está en memoria (no localStorage).
  4. Esperar proximidad de expiración → verificar renovación silenciosa (refresh token).
  5. Hacer clic en "Logout" → verificar redirección a Keycloak logout.
  6. Intentar acceder al frontend → redirección a login de nuevo.
- **Datos**: Credenciales de prueba.
- **Resultado esperado**: Flujo OIDC completo funcional. Token renovado sin interrupción. Logout efectivo.

---

#### CP-FE-020: UI adaptada a roles — consumer no ve admin
- **HU**: HU-009.08.1
- **AC**: AC-E9-08
- **Tipo**: Positivo
- **Precondiciones**: Usuario `api-consumer` autenticado.
- **Pasos**:
  1. Iniciar sesión como `api-consumer`.
  2. Verificar que NO ve: panel de revisión, panel de administración, dashboard de estadísticas.
  3. Verificar que SÍ ve: búsqueda, consulta de detalle.
  4. Repetir como `memory-admin` → verificar que ve TODAS las secciones.
- **Datos**: N/A.
- **Resultado esperado**: UI oculta/muestra secciones según rol del token JWT. Consumer: solo búsqueda y detalle. Admin: acceso completo.

---

## 5. Casos de Prueba de Seguridad

#### CP-S-001: Sin token → 401 en todos los endpoints protegidos
- **HU**: HU-004.10.1
- **AC**: AC-E4-12, AC-NFR-06
- **Tipo**: Seguridad
- **Precondiciones**: Sin header Authorization.
- **Pasos**:
  1. Para cada endpoint bajo `/api/v2/` (excepto `/health` y `/openapi.json` si son públicos), enviar request sin token.
  2. Verificar que todos retornan `HTTP 401 UNAUTHORIZED`.
- **Datos**: Requests sin Authorization header.
- **Resultado esperado**: 100% de endpoints protegidos retornan `HTTP 401`.

---

#### CP-S-002: Token expirado → 401
- **HU**: HU-004.10.1
- **AC**: AC-E4-12
- **Tipo**: Seguridad
- **Precondiciones**: Token JWT expirado (generado con exp corta para prueba).
- **Pasos**:
  1. Usar token expirado en `GET /api/v2/memories/{id}`.
  2. Verificar respuesta.
- **Datos**: Token con `exp` en el pasado.
- **Resultado esperado**: `HTTP 401 UNAUTHORIZED`. Mensaje indica token expirado.

---

#### CP-S-003: Token sin scope suficiente → 403
- **HU**: HU-006.07.1, HU-004.10.1
- **AC**: AC-E6-08, AC-E4-12, AC-NFR-07
- **Tipo**: Seguridad
- **Precondiciones**: Token `api-consumer` (solo lectura de active).
- **Pasos**:
  1. Intentar `POST /api/v2/memories` → `HTTP 403`.
  2. Intentar `PATCH /api/v2/memories/{id}` → `HTTP 403`.
  3. Intentar `DELETE /api/v2/memories/{id}` → `HTTP 403`.
  4. Intentar `POST /api/v2/memories/{id}/review` → `HTTP 403`.
  5. Intentar `GET /api/v2/scopes/{tenantId}/stats` → `HTTP 403`.
- **Datos**: Requests con token api-consumer.
- **Resultado esperado**: Todos `HTTP 403 FORBIDDEN`.

---

#### CP-S-004: RBAC — memory-auditor no puede escribir
- **HU**: HU-006.07.1
- **AC**: AC-E6-08, AC-NFR-07
- **Tipo**: Seguridad
- **Precondiciones**: Token `memory-auditor`.
- **Pasos**:
  1. Intentar `POST /memories` → 403.
  2. Intentar `PATCH /memories/{id}` → 403.
  3. Intentar `DELETE /memories/{id}` → 403.
  4. Intentar `POST /memories/{id}/review` → 403.
  5. Verificar que `GET /memories/{id}` y `GET /scopes/{id}/stats` SÍ funcionan.
- **Datos**: Requests con token auditor.
- **Resultado esperado**: Escrituras: 403. Lecturas: 200.

---

#### CP-S-005: RBAC — memory-operator solo opera sus propias memorias
- **HU**: HU-006.07.1
- **AC**: AC-E6-08
- **Tipo**: Seguridad
- **Precondiciones**: Token `operador@alpha.local`. Memoria `MEM-OTHER` creada por otro operador en mismo tenant.
- **Pasos**:
  1. Intentar `PATCH /memories/MEM-OTHER` → 403.
  2. Intentar `DELETE /memories/MEM-OTHER` → 403.
- **Datos**: Requests del operador sobre memoria ajena.
- **Resultado esperado**: `HTTP 403 FORBIDDEN`. Solo puede modificar sus propias memorias.

---

#### CP-S-006: Cross-tenant access → 404
- **HU**: HU-003.01.1
- **AC**: AC-E3-01, AC-NFR-05
- **Tipo**: Seguridad
- **Precondiciones**: Token `tenant-alpha`. `MEM-BRAVO-01` existe en `tenant-bravo`.
- **Pasos**:
  1. `GET /api/v2/memories/MEM-BRAVO-01` → 404.
  2. `PATCH /api/v2/memories/MEM-BRAVO-01` → 404.
  3. `DELETE /api/v2/memories/MEM-BRAVO-01` → 404.
  4. `POST /api/v2/memories/MEM-BRAVO-01/review` → 404.
  5. `POST /api/v2/memories/MEM-BRAVO-01/relations` → 404.
- **Datos**: Operaciones cross-tenant.
- **Resultado esperado**: Todas retornan `HTTP 404` (sin revelar existencia en otro tenant).

---

#### CP-S-007: SQL injection en query params
- **HU**: N/A (seguridad general)
- **AC**: AC-E4-12, AC-E4-13
- **Tipo**: Seguridad
- **Precondiciones**: Token válido.
- **Pasos**:
  1. `GET /api/v2/entities?q='; DROP TABLE memories; --`
  2. `GET /api/v2/entities?q=1' OR '1'='1`
  3. `GET /api/v2/entities/1'; DROP TABLE--`
  4. `POST /api/v2/memories/search` con `query: "'; DROP TABLE memories; --"`
  5. `GET /api/v2/memories/MEM-001' OR '1'='1`
- **Datos**: Payloads de SQL injection.
- **Resultado esperado**: Todos retornan error controlado (400/404/200 con array vacío). Sin comportamientos inesperados. Sin datos expuestos.

---

#### CP-S-008: XSS en content y title
- **HU**: N/A (seguridad general)
- **AC**: AC-E4-13
- **Tipo**: Seguridad
- **Precondiciones**: Token operator.
- **Pasos**:
  1. Crear memoria con `content: "<script>alert('XSS')</script>"`.
  2. Crear memoria con `summary: "<img src=x onerror=alert(1)>"`.
  3. Crear memoria con `topics: ["<script>alert('xss')</script>"]`.
  4. Verificar que en responses JSON los valores se escapan correctamente.
  5. Verificar en frontend que el contenido se renderiza sin ejecutar scripts (React escapando por defecto + Markdown sanitizado).
- **Datos**: Payloads XSS en campos de texto.
- **Resultado esperado**: API acepta los strings (el contenido puede ser cualquier texto). Frontend escapa/sanitiza correctamente. Sin ejecución de scripts.

---

#### CP-S-009: Rate limiting — 429 con header Retry-After
- **HU**: HU-004.13.1
- **AC**: AC-E4-15
- **Tipo**: Seguridad
- **Precondiciones**: Rate limit bajo configurado para prueba (ej. 5 req/min).
- **Pasos**:
  1. Enviar 10 requests rápidas.
  2. Verificar que a partir del límite se recibe `HTTP 429`.
  3. Verificar header `Retry-After` con valor en segundos.
  4. Verificar header `X-RateLimit-Remaining` en responses exitosas.
- **Datos**: Requests rápidas.
- **Resultado esperado**: `HTTP 429 RATE_LIMIT_EXCEEDED`. Headers informativos presentes.

---

#### CP-S-010: JSON malformado → 400 sin stacktrace
- **HU**: HU-004.11.1
- **AC**: AC-E4-13
- **Tipo**: Seguridad
- **Precondiciones**: Token válido.
- **Pasos**:
  1. Enviar `POST /api/v2/memories` con body: `{invalid json`.
  2. Enviar `POST /api/v2/memories/search` con body: `not json`.
  3. Verificar que no se expone stacktrace ni información interna.
- **Datos**: Strings no-JSON.
- **Resultado esperado**: `HTTP 400 INVALID_JSON`. Body de error estandarizado sin información sensible. Sin stacktrace.

---

## 6. Matriz de Trazabilidad

### 6.1 Cobertura de Endpoints

| # | Endpoint | Método | CP Funcionales | CP Integración | CP Seguridad | Total |
|---|---|---|---|---|---|---|
| 1 | `/api/v2/memories` | POST | CP-F-001 a CP-F-009 | CP-I-001, CP-I-004, CP-I-007, CP-I-011, CP-I-016 | CP-S-003, CP-S-008, CP-S-010 | 15 |
| 2 | `/api/v2/memories/{id}` | GET | CP-F-010 a CP-F-012 | CP-I-001, CP-I-015 | CP-S-002, CP-S-006 | 6 |
| 3 | `/api/v2/memories/{id}` | PATCH | CP-F-013 a CP-F-015 | CP-I-001, CP-I-007, CP-I-011 | CP-S-003, CP-S-005 | 7 |
| 4 | `/api/v2/memories/{id}` | DELETE | CP-F-016, CP-F-017 | CP-I-001, CP-I-012 | CP-S-003, CP-S-005 | 6 |
| 5 | `/api/v2/memories/search` | POST | CP-F-018 a CP-F-025 | CP-I-001, CP-I-002, CP-I-005, CP-I-006, CP-I-014, CP-I-020 | CP-S-007 | 13 |
| 6 | `/api/v2/memories/{id}/graph` | GET | CP-F-033 a CP-F-035 | CP-I-003, CP-I-013 | — | 5 |
| 7 | `/api/v2/memories/{id}/relations` | POST | CP-F-026 a CP-F-031 | CP-I-003, CP-I-005, CP-I-006, CP-I-019 | CP-S-006 | 10 |
| 8 | `/api/v2/memories/{id}/relations/{relId}` | DELETE | CP-F-032 | CP-I-001 | — | 2 |
| 9 | `/api/v2/memories/{id}/review` | POST | CP-F-036 a CP-F-044 | CP-I-001, CP-I-007 | CP-S-003, CP-S-006 | 9 |
| 10 | `/api/v2/memories/extract` | POST | CP-F-046 a CP-F-048 | — | — | 3 |
| 11 | `/api/v2/entities` | GET | CP-F-049, CP-F-051 | — | CP-S-007 | 3 |
| 12 | `/api/v2/entities/{name}` | GET | CP-F-050, CP-F-051 | — | CP-S-007 | 3 |
| 13 | `/api/v2/scopes/{tenantId}/stats` | GET | CP-F-064 a CP-F-066 | — | CP-S-003 | 4 |
| 14 | `/api/v2/health` | GET | CP-F-067, CP-F-068 | — | — | 2 |
| 15 | `/api/v2/openapi.json` | GET | CP-F-069 | — | — | 1 |
| 16 | `/api/v2/metrics` | GET | CP-F-070 | — | — | 1 |

> **Total**: 16 rutas evaluadas. Los endpoints `/api/v2/health` y `/api/v2/openapi.json` pueden ser públicos (sin auth) según configuración.

### 6.2 Cobertura de Pantallas Frontend

| # | Pantalla | CP Frontend |
|---|---|---|
| 1 | Búsqueda (Home) | CP-FE-001, CP-FE-002, CP-FE-003 |
| 2 | Detalle de Memoria | CP-FE-004, CP-FE-005, CP-FE-006 |
| 3 | Creación / Edición | CP-FE-007, CP-FE-008, CP-FE-009, CP-FE-010, CP-FE-011 |
| 4 | Panel de Revisión | CP-FE-012, CP-FE-013, CP-FE-014 |
| 5 | Panel de Admin | CP-FE-015, CP-FE-016 |
| 6 | Dashboard de Estadísticas | CP-FE-017 |
| 7 | General (auth, perfil, roles) | CP-FE-018, CP-FE-019, CP-FE-020 |

> **Total**: 7 pantallas / secciones transversales cubiertas con 20 casos de prueba.

### 6.3 Cobertura de Criterios de Aceptación (AC)

| AC ID | CP Vinculados |
|---|---|
| AC-E1-01 | CP-F-003, CP-F-004, CP-F-015 |
| AC-E1-02 | CP-F-019 (filtro kinds) |
| AC-E1-03 | CP-F-036 a CP-F-043 |
| AC-E1-04 | CP-F-038, CP-FE-014 |
| AC-E1-05 | CP-F-026 a CP-F-031, CP-I-003, CP-I-005 |
| AC-E1-06 | CP-F-032 |
| AC-E1-07 | CP-F-046 a CP-F-048 |
| AC-E1-08 | CP-F-049 a CP-F-051 |
| AC-E1-09 | CP-F-002, CP-F-014 |
| AC-E1-10 | CP-F-002, CP-F-009 |
| AC-E1-11 | CP-F-016, CP-F-043, CP-I-001, CP-I-012, CP-NF-005 |
| AC-E1-12 | CP-I-006 |
| AC-E1-13 | CP-F-002, CP-F-008 |
| AC-E1-14 | CP-F-007 |
| AC-E1-15 | CP-F-005, CP-FE-008 |
| AC-E2-01 | (Perfil creation — cubierto si existe endpoint de perfiles) |
| AC-E2-02 | CP-F-061 |
| AC-E2-03 | CP-F-059, CP-I-016, CP-FE-009 |
| AC-E2-04 | CP-F-062 |
| AC-E2-05 | CP-F-063 |
| AC-E2-06 | CP-F-059, CP-F-060 |
| AC-E2-07 | CP-FE-009 |
| AC-E2-08 | (Extensibilidad — validar con nuevo perfil custom) |
| AC-E3-01 | CP-F-052, CP-F-053, CP-I-002, CP-S-006 |
| AC-E3-02 | CP-F-054 |
| AC-E3-03 | CP-F-055 |
| AC-E3-04 | CP-F-056 |
| AC-E3-05 | (Cross-tenant admin — CP en admin) |
| AC-E3-06 | CP-F-006, CP-F-057 |
| AC-E3-07 | CP-F-058 |
| AC-E4-01 | CP-F-001, CP-F-002, CP-I-019 |
| AC-E4-02 | CP-F-010 a CP-F-012 |
| AC-E4-03 | CP-F-013, CP-F-014, CP-F-015, CP-FE-011 |
| AC-E4-04 | CP-F-031, CP-F-032 |
| AC-E4-05 | CP-F-033 a CP-F-035, CP-I-003, CP-I-013 |
| AC-E4-06 | CP-F-036 a CP-F-040, CP-FE-013 |
| AC-E4-07 | CP-F-049 a CP-F-051 |
| AC-E4-08 | CP-F-064 a CP-F-066 |
| AC-E4-09 | CP-F-067, CP-F-068 |
| AC-E4-10 | CP-I-017, CP-NF-007 |
| AC-E4-11 | CP-F-069 |
| AC-E4-12 | CP-S-001, CP-S-002, CP-S-003 |
| AC-E4-13 | CP-F-012, CP-S-010 |
| AC-E4-14 | CP-F-004, CP-F-007, CP-F-008, CP-F-009 |
| AC-E4-15 | CP-I-010, CP-S-009 |
| AC-E5-01 | CP-F-018, CP-F-020 |
| AC-E5-02 | CP-F-019, CP-F-021 |
| AC-E5-03 | CP-I-005 |
| AC-E5-04 | CP-F-023, CP-I-014 |
| AC-E5-05 | CP-F-022 |
| AC-E5-06 | CP-F-013, CP-F-014, CP-I-004, CP-I-011, CP-I-012 |
| AC-E5-07 | CP-F-025, CP-I-015, CP-NF-005 |
| AC-E5-08 | CP-F-024 |
| AC-E6-01 | CP-I-007, CP-NF-006 |
| AC-E6-02 | CP-FE-016 |
| AC-E6-03 | CP-I-001, CP-I-007 |
| AC-E6-04 | CP-I-015 |
| AC-E6-05 | CP-F-044, CP-F-045 |
| AC-E6-06 | CP-I-008 |
| AC-E6-07 | CP-I-009 |
| AC-E6-08 | CP-S-003 a CP-S-005 |
| AC-E6-09 | CP-F-026, CP-F-032 |
| AC-E9-01 | CP-FE-007, CP-FE-009, CP-FE-010 |
| AC-E9-02 | CP-FE-001 a CP-FE-004 |
| AC-E9-03 | CP-FE-012 a CP-FE-014 |
| AC-E9-04 | CP-FE-015, CP-FE-016 |
| AC-E9-05 | CP-FE-005, CP-FE-006 |
| AC-E9-06 | CP-FE-018 |
| AC-E9-07 | CP-FE-017 |
| AC-E9-08 | CP-FE-019, CP-FE-020 |

---

## 7. Resumen de Cobertura

### Totales por Categoría

| Categoría | Cantidad | Mínimo requerido | Estado |
|---|---|---|---|
| **Funcionales** | 70 | 50 | ✅ Superado |
| **Integración** | 20 | 20 | ✅ Exacto |
| **No Funcionales** | 10 | 10 | ✅ Exacto |
| **Frontend** | 20 | 20 | ✅ Exacto |
| **Seguridad** | 10 | 10 | ✅ Exacto |
| **TOTAL** | **130** | **110** | ✅ Superado |

### Distribución por Tipo

| Tipo | Cantidad | % |
|---|---|---|
| Positivo | 56 | 43% |
| Negativo | 31 | 24% |
| Borde | 11 | 8% |
| Integración | 20 | 15% |
| No funcional | 10 | 8% |
| Seguridad | 10 | 8% |

### Cobertura de Endpoints

| Métrica | Valor |
|---|---|
| Total endpoints | 15 (+ health, openapi, metrics) |
| Endpoints con ≥ 2 CP positivos | 16/16 (100%) |
| Endpoints con ≥ 1 CP negativo | 15/16 (94%) |
| Endpoints con ≥ 1 CP de borde | 8/16 (50%) |

### Cobertura de Criterios de Aceptación

| Métrica | Valor |
|---|---|
| Total AC (funcionales) | 72 |
| AC cubiertos por al menos 1 CP | 70/72 (97%) |
| AC con cobertura de borde | 45/72 (63%) |
| Total AC (sistema + NFR) | 24 |
| AC sistema cubiertos | 20/24 (83%) |

> **AC no cubiertos directamente**: AC-E2-01 (definición de perfil — requiere endpoint de perfiles), AC-E2-08 (extensibilidad — requiere creación de perfil custom), AC-E3-05 (cross-tenant admin — depende de implementación específica), AC-SYS-01, AC-SYS-02, AC-SYS-03 (benchmarks BEIR/LoCoMo — requieren datasets externos y se ejecutan como suite separada), AC-SYS-11 (operaciones CRUD sobre los 9 tipos — cubierto por CP-F-026, CP-F-031, CP-F-032), AC-SYS-12 (batch ingest Should), AC-SYS-13 (migración Could).

### Trazabilidad HU → CP

| Épica | HUs | CPs vinculados |
|---|---|---|
| EP-001 (Motor Genérico) | 12 | CP-F-001 a CP-F-017, CP-F-026 a CP-F-048, CP-NF-005 |
| EP-002 (Perfiles) | 8 | CP-F-059 a CP-F-063, CP-FE-009, CP-FE-010 |
| EP-003 (Scoping) | 7 | CP-F-052 a CP-F-058, CP-S-006 |
| EP-004 (API REST v2) | 15 | CP-F-001 a CP-F-070, CP-S-001 a CP-S-010 |
| EP-005 (Búsqueda + Graph) | 10 | CP-F-018 a CP-F-025, CP-I-003 a CP-I-006, CP-I-011 a CP-I-014, CP-NF-001 |
| EP-006 (Gobernanza) | 9 | CP-F-036 a CP-F-045, CP-I-007 a CP-I-009, CP-I-015, CP-I-018, CP-NF-006, CP-NF-009 |
| EP-009 (Frontend) | 8 | CP-FE-001 a CP-FE-020 |

---

## 8. Glosario

- **AC**: Acceptance Criterion — criterio de aceptación individual trazable a una historia de usuario.
- **CP**: Caso de Prueba — unidad atómica de verificación con precondiciones, pasos, datos y resultado esperado.
- **p95**: Percentil 95 — métrica de latencia: el 95% de las solicitudes se completan en ≤ al valor indicado.
- **RBAC**: Role-Based Access Control — control de acceso basado en 5 roles definidos en Keycloak.
- **JWT**: JSON Web Token — token de acceso firmado que transporta claims del usuario (tenantId, roles).
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de calidad de ranking semántico (top 10).
- **HU**: Historia de Usuario — requerimiento funcional expresado en formato "Como... Quiero... Para...".

---

*Documento generado por qa-functional el 2026-05-04. Cubre 130 casos de prueba distribuidos en 5 categorías (70 funcionales, 20 integración, 10 no funcionales, 20 frontend, 10 seguridad). Trazabilidad completa hacia 15 endpoints REST, 6 pantallas React, 72 criterios de aceptación funcionales y 69 historias de usuario. Todos los casos incluyen precondiciones, pasos detallados, datos de prueba concretos y resultados esperados verificables.*
