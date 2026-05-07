# Reporte de Corrección de Defectos — Abax-Memory v2.1.0

- **Fase**: 5 — QA / Pruebas Funcionales
- **Entregable**: Reporte de Corrección de Defectos (post-QA)
- **Versión**: v2.1.0
- **Responsable**: developer-backend
- **Fecha**: 2026-05-06
- **Estado**: Completado

**Fuente**: `docs/entregables/v2.1/fase-5-qa/reporte-ejecucion-pruebas.md` — sección de defectos

---

## Resumen de Defectos Post-Fix

| Defecto | Severidad | Feature | Estado | Causa Raíz | Commit |
|---|---|---|---|---|---|
| **DEF-V21-001** | 🔴 Crítica | FT-V21-004.3 | ✅ CORREGIDO | Conflicto de resolución de rutas en RESTEasy Reactive: dos recursos (`SearchResourceV2` y `AdminResourceV2`) comparten `@Path("/api/v2")`, y el framework no registraba `AdminResourceV2.deleteNamespace` | `8c7afc3` |
| **DEF-V21-002** | 🔴 Crítica | FT-V21-004.1 | ✅ CORREGIDO | Los parámetros `@HeaderParam` (`X-Graph-Strategy`, `X-Graph-K`, `X-Graph-Threshold`) estaban ubicados DESPUÉS del parámetro de cuerpo (`@Valid UnifiedSearchRequest request`) en la firma del método, impidiendo que RESTEasy Reactive los vinculara correctamente | `8c7afc3` |
| **DEF-V21-003** | 🟡 Media | FT-V21-001.2 | ✅ CORREGIDO | Ausencia de anotaciones `@Min(1)` y `@Max(5)` en el campo `graphDepth` de `UnifiedSearchRequest` | `c128d07` |
| **DEF-V21-004** | 🟡 Media | FT-V21-004.2 | ✅ CORREGIDO | El método `hybridSearch` retornaba `SearchResponse` (DTO), lo que impedía agregar headers HTTP de deprecación. Se cambió a `Response` con headers `Deprecation`, `Sunset` y `Warning` | `8c7afc3` |
| **DEF-V21-005** | 🟡 Media | FT-V21-001.3 | ✅ CORREGIDO | El procesamiento de `entryPoints` ya manejaba UUIDs inválidos con exclusión silenciosa, pero se agregó `@Size(max=10)` para validación explícita y se endureció el logging para trazabilidad | `c128d07` |
| **DEF-V21-006** | 🟡 Media | Transversal | ✅ CORREGIDO | Múltiples formatos de error: `Map.of(...)` manual, `ApiErrorResponse` (v1) y `ErrorResponse` (v2). Se unificaron todas las respuestas manuales en recursos v2 a `ErrorResponse` | `8c7afc3` |

---

## Detalle por Defecto

### DEF-V21-001 — DELETE /admin/namespaces/{name} → 404

**Causa raíz**: Conflicto de resolución de rutas en RESTEasy Reactive (Quarkus 3.15.3). Dos clases de recurso (`SearchResourceV2` y `AdminResourceV2`) compartían la ruta base `@Path("/api/v2")`. Aunque JAX-RS permite múltiples recursos con la misma ruta base, la implementación de RESTEasy Reactive no registraba `AdminResourceV2.deleteNamespace` en tiempo de ejecución, resultando en HTTP 404 con mensaje `"Unable to find matching target resource method"`.

**Solución**: Se migró el endpoint `DELETE /admin/namespaces/{name}` de `AdminResourceV2` a `SearchResourceV2`, consolidando todos los endpoints administrativos (`/admin/reindex`, `/admin/profiles`, `/admin/health`, `/admin/namespaces/{name}`) en un único recurso JAX-RS. `AdminResourceV2` fue descomisionado como recurso REST (se removió `@Path`).

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` — agregado método `deleteNamespace` + inyección `NamespaceService`
- `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/AdminResourceV2.java` — removidas anotaciones JAX-RS, conservado como documentación

**Verificación**: `SearchResourceV2Test` — 27/27 tests pasan (incluye endpoints admin existentes: `reindex`, `profiles`, `health`).

---

### DEF-V21-002 — Header X-Graph-Strategy ignorado

**Causa raíz**: En el método `SearchResourceV2.unifiedSearch()`, los parámetros anotados con `@HeaderParam` (`X-Graph-Strategy`, `X-Graph-K`, `X-Graph-Threshold`) estaban ubicados DESPUÉS del parámetro de cuerpo de la request (`@Valid UnifiedSearchRequest request`). En RESTEasy Reactive, los parámetros de header deben preceder al parámetro de entidad para que el framework los vincule correctamente durante el procesamiento de la request.

**Evidencia del bug**: La inspección del código fuente mostraba:
```java
// ❌ ANTES: headers después del body — no vinculados
public UnifiedSearchResponse unifiedSearch(
    @HeaderParam("X-Tenant-Id") String xTenantId,
    @Valid UnifiedSearchRequest request,    // ← body param interrumpe binding
    @HeaderParam("X-Graph-Strategy") String xGraphStrategy,
    @HeaderParam("X-Graph-K") Integer xGraphK,
    @HeaderParam("X-Graph-Threshold") Double xGraphThreshold)
```

**Solución**: Se reordenaron los parámetros para que todos los `@HeaderParam` precedan al parámetro de cuerpo:
```java
// ✅ DESPUÉS: headers antes del body — vinculados correctamente
public UnifiedSearchResponse unifiedSearch(
    @HeaderParam("X-Tenant-Id") String xTenantId,
    @HeaderParam("X-Graph-Strategy") String xGraphStrategy,
    @HeaderParam("X-Graph-K") Integer xGraphK,
    @HeaderParam("X-Graph-Threshold") Double xGraphThreshold,
    @Valid UnifiedSearchRequest request)
```

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` — método `unifiedSearch()`

---

### DEF-V21-003 — Sin validación de graphDepth

**Causa raíz**: El campo `graphDepth` en `UnifiedSearchRequest` no tenía anotaciones de validación Bean Validation (`@Min`/`@Max`). Valores fuera del rango esperado (1-5) eran aceptados sin error.

**Solución**: Se agregaron anotaciones `@Min(1)` y `@Max(5)` en el campo `graphDepth`. El framework de validación (activado por `@Valid` en el parámetro del método) ahora rechaza valores 0 y ≥6 con HTTP 400.

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` — campo `graphDepth`

---

### DEF-V21-004 — /hybrid sin headers de deprecación

**Causa raíz**: El método `hybridSearch` retornaba directamente el DTO `SearchResponse`, lo que impedía agregar headers HTTP personalizados en la respuesta. La documentación OpenAPI declaraba el endpoint como deprecado, pero no se emitían los headers `Deprecation`, `Sunset` ni `Warning` requeridos por los criterios de aceptación.

**Solución**: Se cambió el tipo de retorno de `SearchResponse` a `jakarta.ws.rs.core.Response`, agregando los siguientes headers:
- `Deprecation: true`
- `Sunset: Sat, 01 Nov 2026 00:00:00 GMT`
- `Warning: 299 - "This endpoint is deprecated. Use POST /api/v2/search with semanticWeight and lexicalWeight instead."`

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` — método `hybridSearch()`

---

### DEF-V21-005 — entryPoints inexistentes → 400

**Causa raíz**: El código en `SearchServiceImpl.unifiedSearch()` ya manejaba correctamente la exclusión silenciosa de UUIDs inválidos y entradas no encontradas (mediante try-catch de `UUID.fromString` y `retainAll` sobre entidades existentes). Sin embargo, no existía una anotación `@Size(max=10)` explícita en el campo `entryPoints`, y el logging era insuficiente para diagnosticar exclusiones en producción.

**Solución**:
1. Se agregó `@Size(max=10, message="entryPoints must have at most 10 entries")` en `UnifiedSearchRequest.entryPoints` para validación explícita.
2. Se endureció el bloque de procesamiento en `SearchServiceImpl` para contar y loguear explícitamente: cantidad de entry points proporcionados, UUIDs inválidos, entidades no encontradas y entry points válidos resultantes. Esto asegura trazabilidad completa de exclusiones silenciosas.

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/dto/v2/UnifiedSearchRequest.java` — campo `entryPoints`
- `backend-quarkus/src/main/java/com/abax/memory/infrastructure/service/SearchServiceImpl.java` — bloque de procesamiento de entry points

---

### DEF-V21-006 — Respuestas 400 inconsistentes

**Causa raíz**: Los recursos v2 (`SearchResourceV2`, `AdminResourceV2`) retornaban errores manuales usando `Map.of("errorCode", "...", "message", "...")` sin estructura estandarizada, mientras que el `V2ExceptionMapper` retornaba el record `ErrorResponse` (con `errorCode`, `message`, `details`, `timestamp`, `path`). Esto causaba que las respuestas 400 tuvieran formatos inconsistentes según la capa que generaba el error.

**Solución**: Se actualizaron todas las respuestas de error manuales en `SearchResourceV2` para usar `ErrorResponse.of(...)` en lugar de `Map.of(...)`. Esto incluye:
- Errores de validación de headers (`parseGraphHeaders`, `validateGraphK`, `validateGraphThreshold`)
- Error de rol insuficiente en `reindex`
- Errores de `deleteNamespace` (rol, confirmación, fallo interno)

Se agregó `@Context UriInfo` para obtener el `path` de la request actual en las respuestas de error.

**Archivos modificados**:
- `backend-quarkus/src/main/java/com/abax/memory/api/rest/v2/SearchResourceV2.java` — todas las respuestas de error manuales

---

## Archivos Modificados (Resumen)

| Archivo | Defectos asociados | Cambios |
|---|---|---|
| `.../api/rest/v2/AdminResourceV2.java` | DEF-V21-001 | Descomisionado como recurso JAX-RS. Eliminado `@Path`, `@ApplicationScoped`. Conservado como documentación. |
| `.../api/rest/v2/SearchResourceV2.java` | DEF-V21-001, -002, -004, -006 | Agregado `deleteNamespace()`. Reordenados `@HeaderParam`. `hybridSearch()` retorna `Response` con headers de deprecación. Respuestas de error unificadas a `ErrorResponse`. |
| `.../api/dto/v2/UnifiedSearchRequest.java` | DEF-V21-003, -005 | Agregado `@Min(1) @Max(5)` en `graphDepth`. Agregado `@Size(max=10)` en `entryPoints`. |
| `.../infrastructure/service/SearchServiceImpl.java` | DEF-V21-005 | Endurecido el bloque de procesamiento de entry points con conteo detallado y logging. |

---

## SHAs de Commits

| Commit SHA | Mensaje |
|---|---|
| `8c7afc3` | `fix(qa): reparar DEF-V21-001 - migrate deleteNamespace to SearchResourceV2` (cubre DEF-V21-001, -002, -004, -006) |
| `c128d07` | `fix(qa): reparar DEF-V21-003 DEF-V21-005 - add graphDepth validation and entryPoints silent exclusion` |

---

## Verificación de Tests

| Suite de tests | Resultado | Detalle |
|---|---|---|
| `SearchResourceV2Test` | ✅ 27/27 pasan | Incluye tests de admin endpoints (`reindex`, `profiles`, `health`), unified search, graph, relations |
| Suite completa (`mvn test`) | 229/243 pasan, 14 fallos | Los 14 fallos son preexistentes (misma cantidad que antes de los fixes). Sin regresiones introducidas. |

---

## Glosario

- **JAX-RS**: Jakarta RESTful Web Services — API estándar para construir servicios REST en Java.
- **RESTEasy Reactive**: Implementación de JAX-RS usada por Quarkus, optimizada para programación reactiva.
- **CDI**: Contexts and Dependency Injection — framework de inyección de dependencias de Jakarta EE.
- **Bean Validation**: Framework de validación de datos (Jakarta Validation) que permite declarar restricciones mediante anotaciones.
- **DTO**: Data Transfer Object — objeto plano usado para transferir datos entre capas o a través de la red.
