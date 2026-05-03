# Redespliegue y Verificación — ISSUES #5, #9, #10
- **Fase**: R2 (Corrección)
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-03
- **Estado**: VERIFICACIÓN COMPLETADA

---

## Resumen de Resultados

| Issue | Descripción | HTTP Esperado | HTTP Real | Estado |
|-------|------------|---------------|-----------|--------|
| **#5** | Priority inválida → 400 | 400 | **400** | ✅ FIXED |
| **#5** | Priority válida → 201 | 201 | **201** | ✅ FIXED |
| **#9** | api-consumer NO ve EN_REVISION (search) | No results | **0 results** | ✅ FIXED |
| **#9** | operator SÍ ve EN_REVISION (search) | Results | **1 result** | ✅ FIXED |
| **#9** | api-consumer GET/list EN_REVISION | 403/filtered | **200** (expuesto) | 🔴 BUG |
| **#10** | Relaciones materializadas | Relations no vacío | **FK violation** | 🔴 BUG |

---

## Detalle por Issue

### ISSUE #5 — Validación de Priority ✅

**Prueba 1: Priority inválida**
```bash
POST /api/casos { "priority": "INVALIDA", ... }
→ HTTP 400
→ Validation failed: "priority must be one of: BAJA, MEDIA, ALTA, CRITICA"
```

**Prueba 2: Priority válida**
```bash
POST /api/casos { "priority": "BAJA", "criticality": "MEDIA", "domain": "qa", "title": "Test", "description": "Test", "origin": "Manual test" }
→ HTTP 201
→ Caso creado: CASO-09fe903b
```

**Conclusión:** ISSUE #5 corregido correctamente. La validación de `Priority` rechaza valores inválidos con HTTP 400 y acepta valores del enum con HTTP 201.

---

### ISSUE #9 — Filtrado EN_REVISION para api-consumer ⚠️

**Contexto:** Memoria `MEM-b5d31090` creada con `criticality: ALTA`, estado `EN_REVISION`.

**Prueba 3: Search con api-consumer**
```bash
POST /api/memorias/search {"consulta":"PostgreSQL Qdrant Keycloak","topK":10}
Authorization: Bearer <token api-consumer>
→ 0 resultados. MEM-b5d31090 (EN_REVISION) NO visible.
→ HTTP 200
```

**Prueba 4: Search con operator**
```bash
POST /api/memorias/search {"consulta":"PostgreSQL Qdrant Keycloak","topK":10}
Authorization: Bearer <token operator>
→ 1 resultado. MEM-b5d31090 (EN_REVISION) SÍ visible.
→ HTTP 200
```

**Prueba 5: GET /api/memorias/{id} con api-consumer**
```bash
GET /api/memorias/MEM-b5d31090
Authorization: Bearer <token api-consumer>
→ HTTP 200 — api-consumer PUEDE ver detalle completo de memoria EN_REVISION
→ Respuesta incluye state: "EN_REVISION", contenido markdown, metadata, etc.
```

**Prueba 6: GET /api/memorias (list) con api-consumer**
```bash
GET /api/memorias
Authorization: Bearer <token api-consumer>
→ HTTP 200 — api-consumer PUEDE listar memorias EN_REVISION (3 memorias visibles, todas EN_REVISION)
```

**Conclusión:** ISSUE #9 parcialmente corregido.

- **SearchService.buildRoleFilter()** (línea 108): ✅ Implementa correctamente el filtro: api-consumer solo ve memorias `APROBADA`.
- **MemoryService.getById()** (línea 214): 🔴 Sin filtro de rol — api-consumer puede acceder a cualquier memoria por ID, incluyendo EN_REVISION.
- **MemoryService.list()** (línea 243): 🔴 Sin filtro de rol — api-consumer ve todas las memorias no archivadas, incluyendo EN_REVISION.

**Fix requerido:** Agregar `buildRoleFilter()` (o equivalente) en `MemoryService.getById()` y `MemoryService.list()`, o delegar el filtrado a nivel de `SecurityIdentity` en el Resource layer.

**Usuario usado como api-consumer:** `api` (roles: `api-consumer`)

---

### ISSUE #10 — Relaciones materializadas 🔴

**Prueba 7: Crear memoria con contenido relacional**
```bash
POST /api/memorias
{
  "title": "Diagnostico incidente base de datos PostgreSQL",
  "type": "incidente",
  "criticality": "MEDIA",
  "contenidoMarkdown": "## Diagnostico\n\nSe detecto un problema en PostgreSQL que afecto a Qdrant. La causa raiz esta relacionada con el bug test busqueda reportado previamente. La memoria sin frontmatter tambien contiene informacion relevante.",
  ...
}
→ HTTP 500 (DATABASE_UNAVAILABLE)
→ Causa raíz: Foreign Key violation en tabla `memory_relation_ref`
```

**Stack trace relevante:**
```
ERROR: insert or update on table "memory_relation_ref" 
  violates foreign key constraint "memory_relation_ref_source_memory_id_fkey"
  Key (source_memory_id)=(MEM-9483c81d) is not present in table "memories".
```

**Causa raíz identificada:**

En `MemoryService.persistInitialState()` (línea 305-349), el orden de operaciones es incorrecto:

```java
private void persistInitialState(MemoryRecord memoryRecord, String body, ...) {
    memoryRecord.metadata = structuredExtractionService.enrichMetadata(body, memoryRecord.metadata);
    // ISSUE #10: Materialize extracted relations into MemoryRelationRef entries
    materializeExtractedRelations(memoryRecord);   // L308 ← FK VIOLATION: source memory no existe aún
    String markdown = markdownCanonicalService.render(memoryRecord, body);
    // ... (version record, git persist, etc.)
    memoryRepository.save(memoryRecord);           // L332 ← Demasiado tarde: la memoria se guarda después
    memoryVersionRepository.save(versionRecord);   // L333
    // ...
    processingJobService.createIfAbsent(memoryRecord.id, versionRecord.id, ProcessingJobType.INDEX_MEMORY);
}
```

`materializeExtractedRelations()` en línea 308 intenta crear registros en `memory_relation_ref` con `sourceMemoryId = memoryRecord.id`, pero el registro en `memories` no existe aún (se guarda en línea 332). La base de datos rechaza la inserción por violación de FK.

**Fix requerido:**
1. Mover `materializeExtractedRelations(memoryRecord)` DESPUÉS de `memoryRepository.save(memoryRecord)` (línea 332).
2. Envolver en `@Transactional` para asegurar atomicidad.

**Nota adicional:** El mismo problema existe en el método `update()` (línea 127). Aunque en update la memoria ya existe, debe verificarse que el orden de operaciones no cause condiciones de carrera.

---

## Evidencias del Ambiente

| Componente | Estado | Detalle |
|-----------|--------|---------|
| Backend | ✅ Running | `java -jar quarkus-run.jar` PID 3767986, puerto 8080 |
| PostgreSQL | ✅ Healthy | Docker `abax-postgres`, puerto 5432 |
| Qdrant | ✅ Running | Docker `qdrant`, puerto 6333 |
| Keycloak | ✅ Running | Docker `keycloak`, puerto 8443 |
| Health Check | ✅ UP | `GET /q/health` → `{"status":"UP"}` |

### Usuarios y Roles

| Usuario | Roles | Token |
|---------|-------|-------|
| `operator` | `memory-operator` | `/tmp/token_op.txt` |
| `api` | `api-consumer` | `/tmp/token_con_api.txt` |
| `qatest` | `default-roles-abax-memory` | `/tmp/token_con.txt` |

### Memorias de Prueba

| ID | Title | State | Criticality |
|----|-------|-------|-------------|
| `MEM-b5d31090` | Memoria EN_REVISION test ISSUE#9 | EN_REVISION | ALTA |
| `MEM-f3937f9a` | Memoria simple test | APROBADA | BAJA |
| `MEM-c06cd6da` | Bug test busqueda | EN_REVISION | - |
| `MEM-400a8d6d` | Sin frontmatter | EN_REVISION | - |

---

## Recomendaciones

1. **ISSUE #9**: Completar el filtro `buildRoleFilter()` en `MemoryService.getById()` y `MemoryService.list()` para que api-consumer no pueda acceder a memorias EN_REVISION por ningún endpoint.
2. **ISSUE #10**: Reordenar `materializeExtractedRelations()` después de `memoryRepository.save()` en `persistInitialState()` y verificar el mismo orden en `update()`.
3. **Pruebas de regresión**: Re-ejecutar suite completa de tests después de aplicar los fixes pendientes.
