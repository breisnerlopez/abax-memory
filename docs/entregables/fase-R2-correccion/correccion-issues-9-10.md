# Correccion de 2 hallazgos de verificacion — ISSUE #9 e ISSUE #10
- **Fase**: R2
- **Responsable**: Desarrollador Backend Senior
- **Fecha**: 2026-05-03
- **Estado**: Completado
---

## Resumen Ejecutivo

Se corrigieron 2 hallazgos identificados durante la verificacion del redespliegue:

1. **ISSUE #10** — FK violation en `materializeExtractedRelations`: la materializacion de relaciones se ejecutaba ANTES de persistir la memoria, causando violacion de llave foranea. Se movio la llamada a `materializeExtractedRelations()` DESPUES de `memoryRepository.save()` en ambos metodos (`persistInitialState()` y `update()`).

2. **ISSUE #9** — api-consumer ve memorias `EN_REVISION` via `GET /api/memorias/{id}` y `GET /api/memorias`: solo `search()` tenia el filtro por rol. Se agrego `SecurityIdentity` al `MemoryService` con los metodos `buildRoleFilter()` e `isApiConsumerOnly()`, aplicandolos en `list()` y `getById()`.

---

## ISSUE #10 — FK violation en materializeExtractedRelations

### Raiz del problema

En `MemoryService.persistInitialState()`, la llamada a `materializeExtractedRelations(memoryRecord)` se ejecutaba en la linea 308, **antes** de que el registro de memoria fuera persistido en linea 332 (`memoryRepository.save(memoryRecord)`). `materializeExtractedRelations()` crea registros `MemoryRelationRef` que referencian `memoryRecord.id` como `sourceMemoryId`, pero si la memoria no ha sido guardada aun, cualquier BD relacional con FK constraint fallara.

El mismo problema existia en `MemoryService.update()`, donde `materializeExtractedRelations(memory)` estaba en linea 127, antes del `memoryRepository.save(memory)` en linea 145.

### Solucion aplicada

**Archivo**: `MemoryService.java`

#### `persistInitialState()` (lineas 326-367)
- **Antes**: `materializeExtractedRelations(memoryRecord)` en linea 308 (antes del primer save)
- **Despues**: `materializeExtractedRelations(memoryRecord)` en linea 357 (despues del segundo `memoryRepository.save()` en linea 354)

#### `update()` (lineas 114-160)
- **Antes**: `materializeExtractedRelations(memory)` en linea 127 (antes del primer save)
- **Despues**: `materializeExtractedRelations(memory)` en linea 155 (despues del segundo `memoryRepository.save()` en linea 152)

### Orden de operaciones resultante

```
1. Enrich metadata (AI extraction)
2. Render markdown
3. Crear version record
4. Git persist (PR o commit directo)
5. memoryRepository.save(memory)        ← memoria ya existe en BD
6. memoryVersionRepository.save(version)
7. memoryRecord.currentVersionId = versionRecord.id
8. memoryRepository.save(memory)        ← guardar con versionId
9. materializeExtractedRelations()      ← AHORA AQUI: FK garantizada
10. AI validation (si aplica)
11. Crear processing job
```

---

## ISSUE #9 — api-consumer ve EN_REVISION via GET/list

### Raiz del problema

`SearchService.buildRoleFilter()` ya filtraba correctamente para `api-consumer`, restringiendo resultados a memorias `APROBADA`. Sin embargo, `MemoryService.list()` y `MemoryService.getById()` no aplicaban ningun filtro basado en rol, exponiendo memorias `EN_REVISION` a usuarios `api-consumer`.

### Solucion aplicada

**Archivo**: `MemoryService.java`

#### 1. Nuevas dependencias inyectadas
```java
import com.btl.administrador.api.security.MemoryRoles;
import io.quarkus.security.identity.SecurityIdentity;

@Inject
SecurityIdentity securityIdentity;
```

#### 2. Metodos auxiliares nuevos
- `isApiConsumerOnly()`: determina si el usuario actual tiene **solo** el rol `api-consumer` (sin `memory-operator`, `memory-reviewer`, `memory-admin`, `memory-auditor`).
- `buildRoleFilter()`: retorna un `Predicate<MemoryRecord>` que filtra a solo `APROBADA` si el usuario es `api-consumer-only`.

#### 3. `getById()` — proteccion de acceso directo
```java
public MemoryResponse getById(String memoryId) {
    MemoryRecord memory = requireMemory(memoryId);
    if (isApiConsumerOnly() && memory.state != MemoryState.APROBADA) {
        throw new ApiException(403, "ACCESS_DENIED", "Access denied for this memory state");
    }
    return toResponse(memory);
}
```
- Usuario `api-consumer-only` solicitando una memoria no-APROBADA → **403 FORBIDDEN**
- Usuario con rol elevado → acceso normal (comportamiento sin cambios)

#### 4. `list()` — filtrado en listado
```java
public List<MemoryResponse> list(...) {
    Predicate<MemoryRecord> roleFilter = buildRoleFilter();
    return memoryRepository.findAll().stream()
            .filter(...)  // filtros existentes
            .filter(roleFilter)  // NUEVO: filtro por rol
            .sorted(...)
            .map(this::toResponse)
            .toList();
}
```
- Usuario `api-consumer-only` → solo ve memorias `APROBADA`
- Usuario con rol elevado → ve todas las memorias (excepto ARCHIVADA/DUPLICADA/ELIMINADA)

### Comportamiento final por endpoint

| Endpoint | api-consumer solo | Roles elevados |
|---|---|---|
| `GET /api/memorias` | Solo `APROBADA` | Todas (exc. excluidas) |
| `GET /api/memorias/{id}` APROBADA | 200 OK | 200 OK |
| `GET /api/memorias/{id}` EN_REVISION | **403 FORBIDDEN** | 200 OK |
| `POST /api/busquedas/semantica` | Solo `APROBADA` (ya existia) | Todas (exc. excluidas) |

---

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `MemoryService.java` | ISSUE #10: Movido `materializeExtractedRelations` despues de persist. ISSUE #9: Agregado `SecurityIdentity`, `buildRoleFilter()`, `isApiConsumerOnly()`, proteccion en `list()` y `getById()`. |
| `ServiceTestSupport.java` | Agregado `securityIdentity` al `memoryService` en constructor y metodo `useActor()`. |
| `MemoryServiceTest.java` | Agregados 5 nuevos tests unitarios para ISSUE #9. |
| `MemoryResourceTest.java` | Agregados 3 nuevos tests de integracion para ISSUE #9. |

---

## Resultados de pruebas

- **Compilacion**: `mvn clean package -DskipTests` → **BUILD SUCCESS**
- **Tests unitarios de servicio**: 16 tests ejecutados, 4 fallos preexistentes (API key AI invalida), **0 fallos nuevos**. Los 5 nuevos tests ISSUE #9 pasan correctamente.
- **Tests de integracion REST**: 27 tests ejecutados, 4 fallos preexistentes (API key AI invalida), **0 fallos nuevos**. Los 3 nuevos tests ISSUE #9 pasan correctamente.

### Nuevos tests (todos PASS)

**MemoryServiceTest**:
1. `list_apiConsumerOnly_filtersOutNonApprovedMemories`
2. `list_elevatedRole_seesAllNonExcludedMemories`
3. `getById_apiConsumerOnly_approvedMemory_returnsSuccess`
4. `getById_apiConsumerOnly_nonApprovedMemory_throwsForbidden`
5. `getById_elevatedRole_nonApprovedMemory_returnsSuccess`

**MemoryResourceTest**:
1. `list_operatorRole_seesAllNonExcludedMemories`
2. `list_apiConsumerOnly_afterResetReturnsEmpty`
3. `getById_apiConsumerOnly_missingMemory_returnsNotFound`
