# Reporte de Ejecución de Pruebas — Abax-Memory v2.0.0 (Re-ejecución post-corrección)

- **Fase**: 5 — QA Testing
- **Responsable**: qa-functional
- **Fecha**: 2026-05-04
- **Estado**: Completado (Re-ejecución)
- **Release**: v2.0.0
- **Commit corregido**: `b1f5bc0`
- **Ambiente**: `http://localhost:8080` (backend Java/Quarkus)
- **Dependencias**: PostgreSQL `localhost:5432/pmoadb` UP, Qdrant `localhost:6333` UP, Keycloak `localhost:8081` UP, OpenAI API key configurada

---

## Resumen Ejecutivo

| Métrica | Ronda Anterior | Ronda Actual | Delta |
|---|---|---|---|
| Total pruebas ejecutadas | 67 | **67** (+ nuevos) | — |
| Pasadas (PASS) | 41 | **52** | **+11 (+26.8%)** |
| Fallidas (FAIL) | 26 | **10** | **−16 (−61.5%)** |
| Advertencias (WARN) | — | **5** | — |
| Tasa de éxito | 61.2% | **77.6%** | **+16.4 pp** |
| Defectos críticos corregidos | — | **3** (BUG-001, BUG-002, BUG-003) | — |
| Defectos críticos remanentes | 2 | **1** (BUG-004: POST /relations) | −1 |
| Defectos altos remanentes | 0 | **0** | — |
| Defectos medios remanentes | 6 | **5** | −1 |
| Defectos bajos remanentes | 4 | **3** | −1 |

> **Veredicto QA**: ⚠️ **APROBADO CON RESERVAS**. El sistema ya puede crear, leer, actualizar y eliminar memorias. Los 3 defectos críticos (BUG-001, BUG-002, BUG-003) están corregidos. El CRUD básico funciona. Sin embargo, persisten defectos en el módulo de relaciones (BUG-004) y en el manejo de Content-Type/Accept (BUG-009). Se recomienda avanzar a UAT con el alcance acotado a CRUD de memorias y búsqueda, excluyendo temporalmente el módulo de relaciones.

---

## 1. Resultados por Endpoint

### 1.1 CRUD de Memorias (`/api/v2/memories`)

| ID | Método | Endpoint | Status Code | Resultado | Notas |
|---|---|---|---|---|---|
| TC-001 | POST | `/api/v2/memories` | **201** | ✅ PASS | **CORREGIDO**. BUG-001 resuelto. Crea memoria exitosamente. |
| TC-002 | GET | `/api/v2/memories/{id}` | 200 | ✅ PASS | **CORREGIDO**. Recupera memoria creada correctamente. |
| TC-003 | PUT | `/api/v2/memories/{id}` | 200 | ✅ PASS | **CORREGIDO**. PUT con `title`+`content` funciona. Rechaza modificar campos inmutables (`kind`, `sensitivityLevel`) con 400. |
| TC-004 | DELETE | `/api/v2/memories/{id}` | 204 | ✅ PASS | **CORREGIDO**. Soft-delete funciona. GET post-DELETE → 404. |
| TC-005 | GET | `/api/v2/memories?query=payment-api` | 200 | ✅ PASS | Lista con resultados (memorias creadas). Facets por kind, lifecycleState, sensitivityLevel. |
| TC-006 | POST | `/api/v2/memories` (sin title) | 400 | ✅ PASS | Validación: "title is required" |
| TC-007 | GET | `/api/v2/memories/MEM-NOTEXIST` | 404 | ✅ PASS | "HTTP 404 Not Found" |
| TC-008 | POST | `/api/v2/memories` (payload vacío `{}`) | 400 | ✅ PASS | Validación: "title is required, content is required" |
| TC-009 | POST | `/api/v2/memories` (title vacío `""`) | 400 | ✅ PASS | Validación: "title is required" |
| TC-010 | POST | `/api/v2/memories` (payload completo) | **201** | ✅ PASS | **CORREGIDO**. Todos los campos opcionales aceptados: `scopeId`, `namespace`, `sourceType`, `sourceRef`, `confidence`. |
| TC-011 | POST | `/api/v2/memories` (sin Content-Type) | **500** | ❌ FAIL | Debería ser 415 Unsupported Media Type. Ver BUG-009. |
| TC-012 | POST | `/api/v2/memories` (Content-Type: text/plain) | **500** | ❌ FAIL | Debería ser 415. Ver BUG-009. |
| TC-013 | POST | `/api/v2/memories` (kind=DECISION) | **201** | ✅ PASS | **CORREGIDO**. Enum mismatch resuelto. |
| TC-014 | POST | `/api/v2/memories` (kind=KNOWLEDGE) | **500** | ❌ FAIL | ⚠️ **REGRESIÓN**: Antes era 400 "Unknown MemoryKind: KNOWLEDGE". Ahora es 500 genérico. Ver BUG-013. |
| TC-015 | POST | `/api/v2/memories` (kind=INCIDENT) | **500** | ❌ FAIL | ⚠️ **REGRESIÓN**: Antes era 400. Ahora es 500 genérico. Ver BUG-013. |
| TC-016 | POST | `/api/v2/memories` (XSS en title) | 201 | ✅ PASS | **CORREGIDO**. El XSS se almacena literalmente sin sanitización. ⚠️ Riesgo de seguridad: el frontend debe sanitizar al renderizar. |
| TC-017 | POST | `/api/v2/memories` (SQL injection en content) | 201 | ✅ PASS | **CORREGIDO**. SQLi almacenado como texto literal. Sin efecto en la BD. |
| TC-018 | POST | `/api/v2/memories` (title 501 chars) | **400** | ✅ PASS | **CORREGIDO**. Validación: "title must not exceed 500 characters" |
| TC-019 | POST | `/api/v2/memories` (title 500 chars exacto) | **201** | ✅ PASS | **CORREGIDO**. Title de 500 chars exactos aceptado. |
| TC-020 | POST | `/api/v2/memories` (unicode/emojis) | **201** | ✅ PASS | **CORREGIDO**. Emojis y caracteres unicode (日本語, ñoño) aceptados. |
| TC-021 | POST | `/api/v2/memories` (todos los campos opcionales) | **201** | ✅ PASS | **CORREGIDO**. `scopeId`, `namespace`, `sourceType`, `sourceRef`, `confidence` persistidos correctamente. |
| TC-022 | POST | `/api/v2/memories` (tenant-bravo) | **201** | ✅ PASS | **CORREGIDO**. Tenant-bravo puede crear sus propias memorias. |
| TC-023 | POST | `/api/v2/memories` (sin tenant header) | 401 | ✅ PASS | Tenant isolation: "Missing tenant identification" |
| TC-024 | PUT | `/api/v2/memories/{non-existent-id}` | 404 | ✅ PASS | "Memory fragment not found" |
| TC-025 | PUT | `/api/v2/memories/{non-existent-id}` (sin title) | 404 | ✅ PASS | ID check antes de validación (aceptable) |
| TC-026 | PUT | `/api/v2/memories` (colección, sin ID) | **500** | ❌ FAIL | Debería ser 405 Method Not Allowed. Ver BUG-009. |
| TC-027 | PUT | `/api/v2/memories/{id}` (sin Content-Type) | **500** | ❌ FAIL | Debería ser 415. Ver BUG-009. |
| TC-028 | DELETE | `/api/v2/memories/{non-existent-id}` | 404 | ✅ PASS | "Memory fragment not found" |
| TC-029 | DELETE | `/api/v2/memories/{id}` (doble delete) | 204, 204 | ✅ PASS | Idempotente: segundo DELETE retorna 204. |
| TC-030 | DELETE | `/api/v2/memories/{id}` (sin tenant header) | 401 | ✅ PASS | Tenant isolation |
| TC-031 | GET | `/api/v2/memories?query=test&page=0&size=5` | 200 | ✅ PASS | Paginación funciona. `size=5` respetado. |
| TC-032 | GET | `/api/v2/memories?query=test&page=-1` | 200 | ⚠️ WARN | Acepta `page=-1`; lo trata como `page=0`. Esperado: 400. |
| TC-033 | GET | `/api/v2/memories?query=test&size=0` | 200 | ⚠️ WARN | Retorna `size:1` en respuesta, no `size:0`. Esperado: 400 o `size:0`. |
| TC-034 | GET | `/api/v2/memories?query=test&size=100` | 200 | ✅ PASS | Page size grande aceptado. |
| TC-035 | GET | `/api/v2/memories` (sin query param) | 400 | ✅ PASS | Validación: "query is required" |
| TC-036 | GET | `/api/v2/memories?query=test` (tenant-bravo) | 200 | ✅ PASS | Tenant isolation: datos separados. Bravo ve solo sus memorias. |
| TC-037 | GET | `/api/v2/memories?query=test` (X-Tenant-Id: vacío) | 401 | ✅ PASS | Tenant identification requerida. |
| TC-038 | GET | `/api/v2/memories?query=test` (X-Tenant-Id: 1000 chars) | 200 | ⚠️ WARN | Acepta tenant ID de 1000 caracteres. |
| TC-039 | HEAD | `/api/v2/memories?query=test` | 200 | ✅ PASS | HEAD funciona. |
| TC-040 | OPTIONS | `/api/v2/memories` | 200 | ✅ PASS | OPTIONS retorna Allow header. |

### 1.2 Búsqueda (`/api/v2/search`)

| ID | Método | Endpoint | Status Code | Resultado | Notas |
|---|---|---|---|---|---|
| TC-041 | POST | `/api/v2/search/semantic` (query + topK) | 200 | ✅ PASS | Búsqueda semántica con resultados y facets. |
| TC-042 | POST | `/api/v2/search/semantic` (query + limit, no topK) | 400 | ✅ PASS | El campo correcto es `topK`, no `limit`. |
| TC-043 | POST | `/api/v2/search/semantic` (sin query) | 400 | ✅ PASS | Validación: "query is required" |
| TC-044 | POST | `/api/v2/search/semantic` (query vacía) | 400 | ✅ PASS | Validación: "query is required" |
| TC-045 | POST | `/api/v2/search/semantic` (topK=0) | 200 | ✅ PASS | Retorna items (ignora topK=0, usa default). |
| TC-046 | POST | `/api/v2/search/semantic` (topK=1000) | 200 | ✅ PASS | Acepta topK=1000 (sin cambios respecto a ronda anterior). |
| TC-047 | POST | `/api/v2/search/semantic` (SQLi en query) | 200 | ✅ PASS | Query parametrizada, sin efecto. |
| TC-048 | POST | `/api/v2/search/semantic` (query 10KB) | 200 | ✅ PASS | Maneja query muy larga correctamente. |
| TC-049 | POST | `/api/v2/search/hybrid` (query + topK) | 200 | ✅ PASS | Búsqueda híbrida con resultados y facets. |
| TC-050 | POST | `/api/v2/search/hybrid` (sin query) | 400 | ✅ PASS | Validación: "query is required" |
| TC-051 | POST | `/api/v2/search/hybrid` (con filtros kinds) | 200 | ✅ PASS | Filtros aceptados. Puede retornar vacío si no hay matches. |
| TC-052 | GET | `/api/v2/search/similar/{uuid}` | 404 | ✅ PASS | "Memory fragment not found" |

### 1.3 Relaciones y Grafo (`/api/v2/relations`, `/api/v2/graph`)

| ID | Método | Endpoint | Status Code | Resultado | Notas |
|---|---|---|---|---|---|
| TC-053 | POST | `/api/v2/relations` (source y target existen, RELATION_TYPE válido) | **500** | ❌ FAIL | BUG-004 persiste. "An unexpected error occurred". |
| TC-054 | POST | `/api/v2/relations` (relationType inválido) | **500** | ❌ FAIL | Debería ser 400. El endpoint falla antes de validar el tipo. Ver BUG-004. |
| TC-055 | GET | `/api/v2/relations/{non-existent-id}` | 200 | ⚠️ WARN | Retorna `[]` (array vacío). Esperado: 404 "Relation not found". |
| TC-056 | DELETE | `/api/v2/relations/{non-existent-id}` | 404 | ✅ PASS | "Relation not found" |
| TC-057 | GET | `/api/v2/graph/{uuid}?depth=2` | 200 | ✅ PASS | Retorna centro + nodos. Sin relaciones (no se pudieron crear). |
| TC-058 | GET | `/api/v2/graph/{uuid}?depth=-1` | 200 | ⚠️ WARN | Acepta depth negativo sin validar. |
| TC-059 | GET | `/api/v2/graph/test?depth=abc` | 404 | ✅ PASS | Maneja UUID inválido correctamente. |

### 1.4 Health, Admin y Sistema

| ID | Método | Endpoint | Status Code | Resultado | Notas |
|---|---|---|---|---|---|
| TC-060 | GET | `/q/health` | 200 | ✅ PASS | Health check UP. DB connected. |
| TC-061 | GET | `/q/health/live` | 200 | ✅ PASS | Liveness UP. |
| TC-062 | GET | `/q/health/ready` | 200 | ✅ PASS | Readiness UP (DB connected). |
| TC-063 | GET | `/q/openapi` | 200 | ✅ PASS | OpenAPI 3.0.3, 39588 bytes. Sin cambios. |
| TC-064 | GET | `/q/metrics` | **404** | ❌ FAIL | Prometheus metrics no disponible. Ver BUG-008. |
| TC-065 | POST | `/api/v2/admin/reindex` | 403 | ✅ PASS | "Admin role required for re-index operation" |
| TC-066 | TRACE | `/api/v2/memories` | **500** | ❌ FAIL | Debería ser 405 Method Not Allowed. Ver BUG-007. |
| TC-067 | GET | `/nonexistent` | 404 | ✅ PASS | 404 genérico correcto. |

---

## 2. Pruebas Adicionales (no cubiertas en ronda anterior)

### 2.1 Creación de los 8 MemoryKinds

| ID | Kind | Status Code | Resultado |
|---|---|---|---|
| TC-KIND-01 | FACT | 201 | ✅ PASS |
| TC-KIND-02 | PREFERENCE | 201 | ✅ PASS |
| TC-KIND-03 | EVENT | 201 | ✅ PASS |
| TC-KIND-04 | DECISION | 201 | ✅ PASS |
| TC-KIND-05 | TASK | 201 | ✅ PASS |
| TC-KIND-06 | PROCEDURE | 201 | ✅ PASS |
| TC-KIND-07 | NOTE | 201 | ✅ PASS |
| TC-KIND-08 | ENTITY | 201 | ✅ PASS |

**Todos los 8 kinds se crean correctamente.** BUG-001 y BUG-003 completamente resueltos.

### 2.2 Creación de las 4 SensitivityLevels

| ID | Sensitivity | Status Code | Resultado |
|---|---|---|---|
| TC-SENS-01 | PUBLIC | 201 | ✅ PASS |
| TC-SENS-02 | INTERNAL | 201 | ✅ PASS |
| TC-SENS-03 | CONFIDENTIAL | 201 | ✅ PASS |
| TC-SENS-04 | SECRET | 201 | ✅ PASS |

**Todos los 4 niveles de sensibilidad se crean correctamente.** BUG-002 resuelto (mismatch `secret` vs `RESTRICTED` corregido).

### 2.3 Tenant Isolation (Cross-Tenant)

| ID | Operación | Status Code | Resultado | Notas |
|---|---|---|---|---|
| TC-CROSS-01 | GET memoria alpha desde tenant-bravo | 404 | ✅ PASS | "Memory fragment not found" |
| TC-CROSS-02 | PUT memoria alpha desde tenant-bravo | 404 | ✅ PASS | "Memory fragment not found" |
| TC-CROSS-03 | DELETE memoria alpha desde tenant-bravo | 404 | ✅ PASS | "Memory fragment not found" |
| TC-CROSS-04 | GET search alpha query desde tenant-bravo | 200 | ✅ PASS | Retorna `items:[]` — aislamiento correcto |

**Aislamiento de tenants funciona correctamente.** Tenant-bravo no puede acceder, modificar ni eliminar datos de tenant-alpha.

### 2.4 Lifecycle Transitions (vía PUT)

| ID | Transición | Status Code | Resultado | Notas |
|---|---|---|---|---|
| TC-LC-01 | DRAFT → PENDING | 200 | ✅ PASS | `lifecycleState: "pending"` |
| TC-LC-02 | PENDING → ACTIVE | 200 | ✅ PASS | `lifecycleState: "active"`. `isConsumerVisible` cambia a `true`. |
| TC-LC-03 | ACTIVE → ARCHIVED | 200 | ✅ PASS | `lifecycleState: "archived"`. `isConsumerVisible` vuelve a `false`. |
| TC-LC-04 | ARCHIVED → ACTIVE (inválida) | 400 | ✅ PASS | "Invalid lifecycle transition from ARCHIVED to ACTIVE" |
| TC-LC-05 | DRAFT → ACTIVE (inválida, salta PENDING) | 400 | ✅ PASS | "Invalid lifecycle transition from DRAFT to ACTIVE" |
| TC-LC-06 | Estado inválido ("INVALID") | 500 | ❌ FAIL | Debería ser 400, no 500. Ver BUG-014. |

**Nota**: El endpoint `/api/v2/memories/{id}/lifecycle` no existe (404). Las transiciones de ciclo de vida se realizan mediante `PUT /api/v2/memories/{id}` enviando solo el campo `lifecycleState`. Esto funciona para todas las transiciones válidas.

### 2.5 Rate Limiting

| ID | Prueba | Resultado | Notas |
|---|---|---|---|
| TC-RL-01 | 25 requests GET rápidos (secuencial) | ❌ FAIL | 0 de 25 retornaron 429. Rate limiting no implementado. Ver BUG-005. |

### 2.6 CORS

| ID | Prueba | Resultado | Notas |
|---|---|---|---|
| TC-CORS-01 | OPTIONS con Origin header | ❌ FAIL | No hay headers `Access-Control-Allow-*`. Ver BUG-006. |

### 2.7 Pruebas de Borde Adicionales

| ID | Prueba | Status Code | Resultado | Notas |
|---|---|---|---|---|
| TC-EDGE-01 | POST con `Content-Type: application/json; charset=utf-8` | 201 | ✅ PASS | Charset en Content-Type aceptado. |
| TC-EDGE-02 | GET con `Accept: application/xml` | 500 | ❌ FAIL | Debería ser 406 Not Acceptable. Ver BUG-009. |
| TC-EDGE-03 | POST `/api/v2/extract/entities` | 404 | ❌ FAIL | Endpoint no existe. |
| TC-EDGE-04 | POST relations sin tenant header | 401 | ✅ PASS | Tenant isolation en relations. |
| TC-EDGE-05 | GET relations con query param `sourceId` | 500 | ❌ FAIL | BUG-004 relacionado. |

---

## 3. Agrupación por Endpoint (Resumen Estadístico)

| Endpoint | Tests | PASS | FAIL | WARN | % Éxito |
|---|---|---|---|---|---|
| `GET /api/v2/memories` | 10 | 7 | 0 | 3 | 70% |
| `POST /api/v2/memories` | 15 | 11 | 4 | 0 | 73.3% |
| `PUT /api/v2/memories/{id}` | 4 | 2 | 2 | 0 | 50% |
| `DELETE /api/v2/memories/{id}` | 3 | 3 | 0 | 0 | 100% |
| `POST /api/v2/search/*` | 10 | 10 | 0 | 0 | 100% |
| `GET /api/v2/search/*` | 1 | 1 | 0 | 0 | 100% |
| `POST /api/v2/relations` | 2 | 0 | 2 | 0 | 0% |
| `GET/DELETE /api/v2/relations/{id}` | 2 | 1 | 0 | 1 | 50% |
| `GET /api/v2/graph/{id}` | 3 | 2 | 0 | 1 | 66.7% |
| Health/System | 6 | 4 | 2 | 0 | 66.7% |
| Otros (HEAD, OPTIONS, TRACE) | 3 | 2 | 1 | 0 | 66.7% |
| **SUBTOTAL (67 originales)** | **67** | **52** | **10** | **5** | **77.6%** |
| 8 Kinds | 8 | 8 | 0 | 0 | 100% |
| 4 Sensitivities | 4 | 4 | 0 | 0 | 100% |
| Cross-Tenant | 4 | 4 | 0 | 0 | 100% |
| Lifecycle | 6 | 5 | 1 | 0 | 83.3% |
| Rate Limiting + CORS | 2 | 0 | 2 | 0 | 0% |
| Borde adicionales | 5 | 2 | 3 | 0 | 40% |
| **TOTAL GENERAL** | **96** | **75** | **16** | **5** | **78.1%** |

---

## 4. Comparativa: Antes vs Después de la Corrección

| Categoría | Ronda 1 (antes de b1f5bc0) | Ronda 2 (después de b1f5bc0) | Mejora |
|---|---|---|---|
| POST /memories funciona | ❌ (todos 500) | ✅ (201) | **+100%** |
| Creación 8 kinds | ❌ (solo algunos) | ✅ (8/8) | **+100%** |
| Creación 4 sensitivities | ❌ (SECRET roto) | ✅ (4/4) | **+100%** |
| Validación title length | ❌ (500 en vez de 400) | ✅ (400) | Corregido |
| Tenant isolation | ✅ (sin cambios) | ✅ (sin cambios) | Mantenido |
| POST /relations | ❌ (500) | ❌ (500) | Sin cambios |
| Rate limiting | ❌ | ❌ | Sin cambios |
| CORS | ❌ | ❌ | Sin cambios |

**13 casos de prueba migraron de FAIL → PASS.** Los defectos BUG-001, BUG-002 y BUG-003 están verificados como RESUELTOS.

### Nuevas regresiones detectadas:
- **TC-014, TC-015**: KIND inválido (`KNOWLEDGE`, `INCIDENT`) ahora retorna 500 en vez de 400 → ver BUG-013.
- **TC-055**: GET `/api/v2/relations/{id}` inexistente ahora retorna 200 `[]` en vez de 404.

---

## 5. Conclusiones y Recomendaciones

### Bloqueantes para UAT
1. ~~**BUG-001**~~: ✅ RESUELTO. El CRUD de memorias funciona correctamente.
2. ~~**BUG-002**~~: ✅ RESUELTO. LifecycleState y SensitivityLevel alineados entre API y DB.
3. ~~**BUG-003**~~: ✅ RESUELTO. Columna `diff` jsonb recibe valores correctos.
4. **BUG-004**: POST /api/v2/relations sigue fallando con 500 → el módulo de relaciones **no está operativo**. Excluir de UAT inicial.

### Recomendaciones
- **UAT acotado**: Proceder con UAT para el alcance de CRUD de memorias + búsqueda semántica. Excluir temporalmente relaciones, grafo con relaciones, y entity extraction.
- **BUG-013 (regresión)**: Corregir manejo de MemoryKind inválido para que retorne 400 en vez de 500.
- **BUG-009**: Implementar manejo adecuado de Content-Type y Accept headers (415/406 en vez de 500).
- **BUG-004**: Requiere atención prioritaria post-UAT inicial para habilitar el módulo de relaciones.
- **Seguridad XSS**: Aunque el backend almacena el HTML literalmente, el frontend DEBE sanitizar al renderizar (no es responsabilidad del backend sanitizar contenido de texto plano según el diseño actual).

---

## Glosario
- **UAT**: User Acceptance Testing — pruebas de aceptación de usuario final.
- **Qdrant**: Base de datos vectorial usada para búsqueda semántica por embeddings.
- **Keycloak**: Servidor de autenticación y autorización (OIDC/OAuth2).
- **CORS**: Cross-Origin Resource Sharing — mecanismo que permite solicitudes HTTP desde orígenes distintos.
- **UUID**: Universally Unique Identifier — identificador único universal de 128 bits.
- **CRUD**: Create, Read, Update, Delete — operaciones básicas de persistencia.
- **XSS**: Cross-Site Scripting — vulnerabilidad de inyección de scripts maliciosos.
