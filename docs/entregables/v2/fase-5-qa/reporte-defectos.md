# Reporte de Defectos — Abax-Memory v2.0.0
- **Fase**: 5 — QA Testing
- **Responsable**: qa-functional
- **Fecha**: 2026-05-04 (actualizado: re-ejecución post b1f5bc0)
- **Estado**: Actualizado (re-ejecución)
- **Release**: v2.0.0
- **Commit corrección**: `b1f5bc0`
- **Ambiente**: `http://localhost:8080` — PostgreSQL `localhost:5432/pmoadb`, Qdrant `localhost:6333`

---

## Resumen

| Total Defectos | Resueltos | Críticos | Altos | Medios | Bajos |
|---|---|---|---|---|---|
| **14** | **3** (BUG-001, 002, 003) | **1** | **1** | **5** | **3** |

> **Nota**: 3 defectos críticos corregidos y verificados. 10 defectos activos. 2 nuevos defectos detectados (BUG-013 regresión, BUG-014).

---

## Defectos Críticos

### ~~BUG-001~~ ✅ RESUELTO (verificado 2026-05-04 R2)

| Campo | Valor |
|---|---|
| **ID** | BUG-001 |
| **Título** | La creación de memorias falla por triple mismatch de enums entre API, dominio Java y base de datos |
| **Severidad** | 🔴 **Crítica** → ✅ **Resuelta** |
| **Prioridad** | ⚡ **Urgente** → ✅ **Cerrada** |
| **Estado** | **RESUELTO** — Verificado en re-ejecución post commit `b1f5bc0` |
| **Endpoint** | `POST /api/v2/memories` |
| **Verificación** | Los 8 MemoryKinds crean correctamente (201). Las 4 SensitivityLevels crean correctamente (201). TC-001, TC-010, TC-013, TC-016–TC-022 pasan. |
| **Evidencia de verificación** | `POST /api/v2/memories` con `kind: DECISION` → `HTTP 201 {"id":"cca4c9fb-...", "kind":"decision"}`. `kind: ENTITY` → `HTTP 201`. `kind: FACT` → `HTTP 201`. Ver reporte de ejecución R2. |
| **Casos de prueba asociados** | TC-001 ✅, TC-010 ✅, TC-013 ✅, TC-016 ✅, TC-017 ✅, TC-018 ✅, TC-019 ✅, TC-020 ✅, TC-021 ✅, TC-022 ✅ |
| **Requerimiento asociado** | AC-E4-01 |

---

### ~~BUG-002~~ ✅ RESUELTO (verificado 2026-05-04 R2)

| Campo | Valor |
|---|---|
| **ID** | BUG-002 |
| **Título** | LifecycleState y SensitivityLevel tienen valores incompatibles entre API/OpenAPI y la base de datos |
| **Severidad** | 🔴 **Crítica** → ✅ **Resuelta** |
| **Prioridad** | ⚡ **Urgente** → ✅ **Cerrada** |
| **Estado** | **RESUELTO** — Verificado en re-ejecución post commit `b1f5bc0` |
| **Verificación** | Las 4 SensitivityLevels (`PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `SECRET`) crean correctamente. Las transiciones de LifecycleState (`draft→pending→active→archived`) funcionan vía PUT. |
| **Evidencia de verificación** | `POST` con `sensitivityLevel: SECRET` → `HTTP 201`. `PUT` con `lifecycleState: pending` → `HTTP 200`. Ver reporte de ejecución R2 secciones 2.2 y 2.4. |
| **Caso de prueba asociado** | TC-SENS-01–04 ✅, TC-LC-01–05 ✅ |

---

### ~~BUG-003~~ ✅ RESUELTO (verificado 2026-05-04 R2)

| Campo | Valor |
|---|---|
| **ID** | BUG-003 |
| **Título** | Error "column diff is of type jsonb but expression is of type character varying" al crear memoria con kind=ENTITY |
| **Severidad** | 🔴 **Crítica** → ✅ **Resuelta** |
| **Prioridad** | ⚡ **Urgente** → ✅ **Cerrada** |
| **Estado** | **RESUELTO** — Verificado en re-ejecución post commit `b1f5bc0` |
| **Verificación** | `POST` con `kind: ENTITY` crea correctamente (201). La columna `diff` recibe valores JSON válidos sin error. |
| **Evidencia de verificación** | `POST /api/v2/memories` con `kind: ENTITY` → `HTTP 201 {"id":"1a2d37ea-...", "kind":"entity"}`. Sin errores en logs. |
| **Caso de prueba asociado** | TC-KIND-08 ✅, EXP-008 ✅ |

---

## Defectos Altos

### BUG-004: POST /api/v2/relations falla con 500 INTERNAL_ERROR (ACTUALIZADO R2)

| Campo | Valor |
|---|---|
| **ID** | BUG-004 |
| **Título** | La creación de relaciones entre memorias retorna error interno del servidor incluso con IDs válidos |
| **Severidad** | 🔴 **Crítica** (único defecto crítico remanente) |
| **Prioridad** | 🔺 **Alta** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | 2 memorias creadas exitosamente (POST 201). Ambas existen y pertenecen al mismo tenant. |
| **Pasos para reproducir** | 1. Crear source memory: `POST /api/v2/memories` → ID `SOURCE`. 2. Crear target memory: `POST /api/v2/memories` → ID `TARGET`. 3. Enviar `POST /api/v2/relations` con payload: `{"sourceId":"SOURCE","targetId":"TARGET","relationType":"RELATED_TO"}`. |
| **Resultado actual (R2)** | `HTTP 500 INTERNAL_ERROR: "An unexpected error occurred"`. Mismo comportamiento que en R1. No se crea la relación. El endpoint falla antes de procesar la solicitud. |
| **Resultado esperado** | `HTTP 201 Created` con la relación creada. |
| **Evidencia (R2)** | `{"errorCode":"INTERNAL_ERROR","message":"An unexpected error occurred","path":"/api/v2/relations"}`. Source y target confirmados como existentes (GET 200). |
| **Nota R2** | El endpoint valida correctamente que source existe (404 si sourceId no existe), y requiere tenant (401 sin header). La falla es en la lógica de negocio interna tras la validación. |
| **Caso de prueba asociado** | TC-053, TC-054 |
| **Requerimiento asociado** | AC-E4-05 |

---

## Defectos Medios

### BUG-005: Sin rate limiting — 20 requests no producen 429

| Campo | Valor |
|---|---|
| **ID** | BUG-005 |
| **Título** | Ausencia total de rate limiting en los endpoints de la API |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar 20 requests GET consecutivos a `/api/v2/memories?query=test`. 2. Verificar si algún request retorna 429. |
| **Resultado actual** | Los 20 requests retornan `200 OK`. No hay throttling. |
| **Resultado esperado** | Al menos 1 request debería retornar `429 Too Many Requests` bajo una política de rate limiting configurada. |
| **Evidencia** | 20/20 requests → HTTP 200. |
| **Caso de prueba asociado** | TC-RL-01 |

---

### BUG-006: CORS no configurado — sin headers Access-Control-Allow-*

| Campo | Valor |
|---|---|
| **ID** | BUG-006 |
| **Título** | La API no retorna headers CORS, impidiendo el acceso desde navegadores |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `OPTIONS /api/v2/memories?query=test` con headers: `Origin: http://localhost:3000`, `Access-Control-Request-Method: GET`, `Access-Control-Request-Headers: X-Tenant-Id`. 2. Verificar headers de respuesta. |
| **Resultado actual** | Respuesta solo contiene `Allow: HEAD, DELETE, POST, GET, OPTIONS, PUT`. No hay headers `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`. |
| **Resultado esperado** | Deberían retornarse los headers CORS correspondientes para permitir acceso desde el frontend. |
| **Evidencia** | `curl -I -X OPTIONS` no muestra ningún header `Access-Control-*`. |

---

### BUG-007: Método TRACE retorna 500 en vez de 405

| Campo | Valor |
|---|---|
| **ID** | BUG-007 |
| **Título** | El método HTTP TRACE no está deshabilitado y retorna error interno |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `TRACE /api/v2/memories`. |
| **Resultado actual** | `HTTP 500 INTERNAL_ERROR`. |
| **Resultado esperado** | `HTTP 405 Method Not Allowed` o `HTTP 501 Not Implemented`. El método TRACE es un riesgo de seguridad (Cross-Site Tracing) y debe estar deshabilitado. |
| **Evidencia** | `curl -X TRACE` → HTTP 500. |

---

### BUG-008: /q/metrics no disponible (404)

| Campo | Valor |
|---|---|
| **ID** | BUG-008 |
| **Título** | El endpoint de métricas Prometheus no está expuesto |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `GET /q/metrics`. |
| **Resultado actual** | `HTTP 404 Not Found`. |
| **Resultado esperado** | `HTTP 200` con métricas en formato Prometheus, si está configurado. O `HTTP 404` solo si la extensión no está incluida — pero debe documentarse. |
| **Evidencia** | `{"errorCode":"NOT_FOUND","message":"Unable to find matching target resource method","path":"/q/metrics"}` |

---

### BUG-009: Headers de Content-Type/Accept incorrectos causan 500 en vez de 4xx

| Campo | Valor |
|---|---|
| **ID** | BUG-009 |
| **Título** | Content-Type y Accept incorrectos retornan 500 en lugar de 415/406 |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `POST /api/v2/memories` sin header `Content-Type`. 2. Enviar `POST /api/v2/memories` con `Content-Type: text/plain`. 3. Enviar `GET /api/v2/memories?query=test` con `Accept: application/xml`. |
| **Resultado actual** | Los 3 casos retornan `HTTP 500 INTERNAL_ERROR`. |
| **Resultado esperado** | Casos 1 y 2: `HTTP 415 Unsupported Media Type`. Caso 3: `HTTP 406 Not Acceptable`. |
| **Evidencia** | Los 3 curls retornan: `{"errorCode":"INTERNAL_ERROR","message":"An unexpected error occurred"}` |

---

## Defectos Bajos

### BUG-010: GET /api/v2/memories?size=0 retorna size=1 en la respuesta

| Campo | Valor |
|---|---|
| **ID** | BUG-010 |
| **Título** | Respuesta inconsistente: se solicita size=0 pero se retorna size=1 |
| **Severidad** | 🟢 **Baja** |
| **Prioridad** | 🔹 **Baja** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `GET /api/v2/memories?query=test&size=0`. 2. Observar el campo `size` en la respuesta. |
| **Resultado actual** | `{"size":1}` — la respuesta muestra size=1 aunque se solicitó size=0. |
| **Resultado esperado** | `{"size":0}` consistente con el parámetro enviado, o `HTTP 400` si size=0 no es válido. |
| **Evidencia** | `{"items":[],"total":0,"page":0,"size":1,...}` |

---

### BUG-011: V1 API usa nombres en español (viola code-naming-convention)

| Campo | Valor |
|---|---|
| **ID** | BUG-011 |
| **Título** | Schemas y endpoints de API V1 usan identificadores en español |
| **Severidad** | 🟢 **Baja** |
| **Prioridad** | 🔹 **Baja** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Revisar OpenAPI: `SearchMemoryRequest` tiene campos `consulta`, `filtros`. 2. `RelationType1` tiene valores `RELACIONADA_CON`, `COMPLEMENTA`, etc. 3. `ReviewDecision` tiene `OBSERVADA`, `RECHAZADA`. |
| **Resultado actual** | Identificadores en español en schemas de API. |
| **Resultado esperado** | Todos los identificadores del sistema deben estar en inglés según code-naming-convention. |
| **Evidencia** | OpenAPI schemas: `"consulta"`, `"filtros"`, `"RELACIONADA_CON"`, `"OBSERVADA"`. |

---

### ~~BUG-012~~ ✅ RESUELTO (verificado 2026-05-04 R2)

| Campo | Valor |
|---|---|
| **ID** | BUG-012 |
| **Título** | Title con 501 caracteres causa 500 en vez de error de validación 400 |
| **Severidad** | 🟢 **Baja** → ✅ **Resuelta** |
| **Estado** | **RESUELTO** — Verificado en R2. La validación ahora se aplica correctamente en la capa de presentación. |
| **Verificación** | `POST` con title de 501 chars → `HTTP 400`: "title must not exceed 500 characters". |
| **Evidencia** | `{"code":"VALIDATION_ERROR","details":["create.arg1.title title must not exceed 500 characters"]}`. |

---

### BUG-013 🆕: MemoryKind inválido retorna 500 en vez de 400 (REGRESIÓN)

| Campo | Valor |
|---|---|
| **ID** | BUG-013 |
| **Título** | Regresión: kinds inválidos (`KNOWLEDGE`, `INCIDENT`) ahora causan 500 genérico en lugar del 400 con validación descriptiva |
| **Severidad** | 🟡 **Media** |
| **Prioridad** | 🔸 **Normal** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Backend UP. |
| **Pasos para reproducir** | 1. Enviar `POST /api/v2/memories` con `kind: "KNOWLEDGE"`. 2. Enviar `POST /api/v2/memories` con `kind: "INCIDENT"`. |
| **Resultado actual (R2)** | Ambos retornan `HTTP 500 INTERNAL_ERROR: "An unexpected error occurred"`. |
| **Resultado esperado** | `HTTP 400 Bad Request` con mensaje: "Unknown MemoryKind: KNOWLEDGE" / "Unknown MemoryKind: INCIDENT". |
| **Impacto** | **REGRESIÓN**: En R1, estos casos retornaban 400 correctamente. La corrección de BUG-001 alineó los enums válidos pero eliminó el mensaje descriptivo para valores inválidos. |
| **Caso de prueba asociado** | TC-014, TC-015 |
| **Evidencia** | `curl -X POST ... -d '{"title":"test","kind":"KNOWLEDGE",...}'` → `HTTP 500 {"errorCode":"INTERNAL_ERROR"}` |

---

### BUG-014 🆕: PUT con lifecycleState inválido retorna 500 en vez de 400

| Campo | Valor |
|---|---|
| **ID** | BUG-014 |
| **Título** | Transiciones de lifecycle state a valores no reconocidos causan error interno del servidor |
| **Severidad** | 🟢 **Baja** |
| **Prioridad** | 🔹 **Baja** |
| **Ambiente** | QA — `localhost:8080` |
| **Precondiciones** | Memoria existente en cualquier estado. |
| **Pasos para reproducir** | 1. `PUT /api/v2/memories/{id}` con `{"lifecycleState": "INVALID"}`. |
| **Resultado actual** | `HTTP 500 INTERNAL_ERROR: "An unexpected error occurred"`. |
| **Resultado esperado** | `HTTP 400 Bad Request` con mensaje: "Invalid lifecycle state: INVALID" o "Unknown lifecycle state". |
| **Evidencia** | `curl -X PUT ... -d '{"lifecycleState":"INVALID"}'` → `HTTP 500`. Las transiciones válidas y las inválidas conocidas (ej: ARCHIVED→ACTIVE) sí retornan 400 correctamente. |
| **Caso de prueba asociado** | TC-LC-06 |

---

## Defectos Encontrados en Exploración (no cubiertos por casos de prueba)

| ID | Descripción | Severidad | Estado | Evidencia |
|---|---|---|---|---|
| EXP-001 | `MemoryKind` enum: API (`fact,preference,...`) vs DB (`DECISION,INCIDENT,...`) completamente diferentes | 🔴 Crítica | ✅ **RESUELTO** (BUG-001) | Verificado: los 8 kinds crean correctamente en R2 |
| EXP-003 | `LifecycleState` mismatch: API (`draft,pending,active,rejected,archived,deleted`) vs DB (`DRAFT,IN_REVIEW,APPROVED,DEPRECATED,ARCHIVED,DELETED`) | 🔴 Crítica | ✅ **RESUELTO** (BUG-002) | Verificado: transiciones de lifecycleState funcionan en R2 |
| EXP-004 | `SensitivityLevel`: API tiene `secret`, DB tiene `RESTRICTED` — mismatch | 🟠 Alta | ✅ **RESUELTO** (BUG-002) | Verificado: las 4 sensitivities crean correctamente en R2 |
| EXP-005 | `ReviewDecision` enum en español: `OBSERVADA, RECHAZADA` | 🟡 Media | Abierto | Sin cambios |
| EXP-006 | V1 schemas en español (`consulta`, `filtros`) | 🟡 Media | Abierto | Sin cambios |
| EXP-008 | Columna `diff` jsonb recibe `character varying` en operaciones de auditoría | 🔴 Crítica | ✅ **RESUELTO** (BUG-003) | Verificado: ENTITY kind crea sin error en R2 |

---

## Resumen de Causas Raíz

1. ~~**Desacople de esquemas**~~: ✅ CORREGIDO. Los enums de API, dominio Java y base de datos fueron alineados (commit `b1f5bc0`). Los 8 MemoryKinds y 4 SensitivityLevels funcionan correctamente.

2. ~~**Falta de pruebas de integración**~~: Pendiente. Aunque los defectos de enum fueron corregidos, el módulo de relaciones (BUG-004) sigue sin funcionar, lo que sugiere que no hay tests de integración para ese flujo.

3. **Múltiples convenciones de lenguaje**: V1 usa español, V2 usa inglés — los enums de V1 (`ReviewDecision`, `RelationType1`) siguen teniendo valores en español.

4. **Manejo deficiente de Content-Type/Accept**: El backend no maneja correctamente headers HTTP estándar, retornando 500 en lugar de 415/406.

---

## Recomendaciones Actualizadas (post-corrección)

1. **BUG-004 (crítico)**: Investigar y corregir el endpoint `POST /api/v2/relations`. Es el último defecto crítico que bloquea funcionalidad principal del sistema.

2. **BUG-013 (regresión)**: Restaurar mensajes de validación descriptivos para MemoryKind inválidos.

3. **BUG-009**: Implementar manejo adecuado de Content-Type y Accept headers (415/406).

4. **BUG-005 / BUG-006**: Rate limiting y CORS — necesarios antes de despliegue público, pero no bloqueantes para UAT interno.

5. **Tests de integración**: Agregar tests automatizados que validen el flujo completo POST → DB → Qdrant → GET para prevenir regresiones futuras.

---

## Resumen de Cambios R1 → R2

| Defecto | R1 | R2 | Cambio |
|---|---|---|---|
| BUG-001 | 🔴 Crítico | ✅ Resuelto | Corrección verificada |
| BUG-002 | 🔴 Crítico | ✅ Resuelto | Corrección verificada |
| BUG-003 | 🔴 Crítico | ✅ Resuelto | Corrección verificada |
| BUG-004 | 🟠 Alta | 🔴 Crítica (reclasificado) | Misma falla; reclasificado por impacto en funcionalidad completa |
| BUG-012 | 🟢 Baja | ✅ Resuelto | Validación de title length ahora funciona |
| BUG-013 | — | 🟡 Media (NUEVO) | Regresión detectada |
| BUG-014 | — | 🟢 Baja (NUEVO) | Nuevo hallazgo |

---

## Glosario
- **CRUD**: Create, Read, Update, Delete — operaciones básicas de persistencia.
- **CORS**: Cross-Origin Resource Sharing — mecanismo de seguridad para peticiones cross-origen desde navegadores.
- **Qdrant**: Motor de base de datos vectorial para búsqueda semántica por embeddings.
- **XSS**: Cross-Site Scripting — vulnerabilidad de inyección de scripts maliciosos.
- **SQLi**: SQL Injection — vulnerabilidad de inyección de comandos SQL.
- **Jackson**: Biblioteca Java para serialización/deserialización JSON.
- **OpenAPI**: Especificación estándar para describir APIs REST.
