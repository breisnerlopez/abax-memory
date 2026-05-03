# Quinto redespliegue y verificación final de Qdrant con formato de vectores corregido
- **Fase**: Fase 5 — Deploy & Verification
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-02
- **Estado**: Completado — Verificación exitosa ✅

---

## 1. Objetivo

Verificar que el formato de vectores corregido para Qdrant v1.17.1 (array directo, sin wrapper de clave vacía) funciona correctamente en el pipeline completo de creación, indexación y búsqueda semántica.

## 2. Resultado final

| Componente | Estado | Detalle |
|-----------|--------|---------|
| Backend | ✅ UP | `abax-memory-backend` como servicio systemd |
| Health Check | ✅ UP | DB connections OK |
| Qdrant Collection | ✅ `status: green` | `points_count: 1` |
| OpenAI Embedding | ✅ Activo | `text-embedding-3-large`, 3072 dims |
| Indexación | ✅ Éxito | `Indexed memory MEM-ace6f311 in Qdrant` |
| Búsqueda semántica | ✅ 1 resultado | score: `0.476`, memoria: "QA v5 final test" |
| Formato vectores | ✅ Validado | `{"vectors": {"size": 3072, "distance": "Cosine"}}` |

## 3. Búsqueda final (evidencia exacta)

```json
[{
  "memoryId": "MEM-ace6f311",
  "title": "QA v5 final test",
  "summary": "id: MEM-ace6f311 title: \"QA v5 final test\" type: caso origin: manual...",
  "score": 0.47602892,
  "state": "APROBADA",
  "origin": "MANUAL",
  "criticality": "BAJA",
  "domains": ["qa"],
  "tags": [],
  "commitSha": "commit-657b8072-d5a5-45d9-92c0-74eaa79970ed"
}]
```

## 4. Qdrant Collection Status

```
status: green
indexed_vectors_count: 0
points_count: 1
config.vectors: {"size":3072,"distance":"Cosine"}
```

## 5. Problemas encontrados y soluciones

### 5.1 JAR sin clases de aplicación
- **Causa**: `mvn clean quarkus:build` no ejecuta `process-resources` ni `compile`
- **Fix**: Usar `mvn clean compile quarkus:build -Dquarkus.package.jar.type=uber-jar`

### 5.2 CDI Client Proxy — acceso directo a campos
- **Causa**: `QdrantEmbeddingService` accedía a `qdrantConfig.vectorSize` como campo package-private. CDI crea un proxy para `@ApplicationScoped` cuyos campos no están poblados → retorna 0 (default int).
- **Fix**: Agregar getters públicos `getVectorSize()` y `getCollection()` en `QdrantConfig`, y usarlos en `QdrantEmbeddingService`.
- **Archivos modificados**:
  - `backend-quarkus/src/main/java/.../qdrant/QdrantConfig.java` — getters agregados
  - `backend-quarkus/src/main/java/.../qdrant/QdrantEmbeddingService.java` — acceso por getters

### 5.3 Expresión de config no resuelta
- **Causa**: `${ABAX_QDRANT_VECTOR_SIZE:3072}` no se resolvía correctamente en runtime
- **Fix**: Hardcodear `abax.qdrant.vector-size=3072` en `application.properties`

### 5.4 Procesos matados por timeout de herramienta
- **Fix**: Desplegar backend como servicio systemd (`abax-memory-backend.service`)

## 6. Logs clave

```
2026-05-02 19:55:27,291 INFO  Creating OpenAI EmbeddingModel: model=text-embedding-3-large
2026-05-02 19:55:27,693 INFO  Qdrant collection abax-memories already exists
2026-05-02 19:55:27,693 INFO  Qdrant collection abax-memories ready at http://localhost:6333
2026-05-02 19:55:28,105 INFO  Indexed memory MEM-ace6f311 in Qdrant
2026-05-02 19:56:10,219 INFO  Qdrant search for 'Qdrant vectores indexacion' returned 1 hits
```

**Nota**: No hay errores de `Embedding dimension mismatch` ni `Wrong input: Vector dimension error`.

## 7. Memoria creada (payload)

```json
{
  "title": "QA v5 final test",
  "type": "CASO",
  "criticality": "BAJA",
  "domains": ["qa"],
  "contenidoMarkdown": "## Contexto\nPrueba final con vectores corregidos Qdrant v1.17.1\n\n## Resolucion\nFormato de vectores como array directo sin wrapper",
  "metadata": {"fuente": "qa-final"},
  "frontmatter": {
    "title": "QA v5 final test",
    "type": "caso",
    "origin": "manual",
    "criticality": "baja",
    "domains": ["qa"],
    "metadata": {"fuente": "qa-final"}
  }
}
```

**Respuesta**: `id: MEM-ace6f311`, `state: APROBADA`, `processingStatus: PENDING_INDEX` → luego indexada exitosamente.

## 8. Infraestructura

| Servicio | Endpoint | Estado |
|----------|----------|--------|
| Backend Quarkus | `http://localhost:8080` | UP (systemd) |
| Qdrant | `http://localhost:6333` | UP |
| Keycloak | `http://localhost:8443` | UP |
| PostgreSQL | `localhost:5432/aba` | UP |

## 9. Conclusión

El formato de vectores como array directo (`{"size": N, "distance": "Cosine"}`) funciona correctamente con Qdrant v1.17.1. El pipeline end-to-end (crear memoria → embedding OpenAI → indexar en Qdrant → búsqueda semántica) opera sin errores. El quinto redespliegue se completa con verificación exitosa.
