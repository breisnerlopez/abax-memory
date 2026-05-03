# Corrección de formato de vectores en QdrantEmbeddingService para Qdrant v1.17.1
- **Fase**: Fase 4 — Construcción
- **Responsable**: developer-backend
- **Fecha**: 2026-05-02
- **Estado**: Completado

---

## Resumen

Se corrigió el formato de vectores en `QdrantEmbeddingService` para cumplir con la API de Qdrant v1.17.1, que **rechaza el formato de named vectors con clave vacía** `{"": [...]}` y requiere arrays de floats directamente `[...]`.

## Archivo modificado

`backend-quarkus/src/main/java/com/btl/administrador/api/integration/qdrant/QdrantEmbeddingService.java`

## Cambios realizados

### 1. `initCollection()` — Configuración de vectors en creación de colección

**Antes** (named vector wrapper con clave vacía):
```json
{"vectors": {"": {"size": 3072, "distance": "Cosine"}}}
```

**Después** (configuración directa):
```json
{"vectors": {"size": 3072, "distance": "Cosine"}}
```

Código modificado (líneas 214-221):
```java
// Qdrant v1.17.1+: vectors config is directly {"size": N, "distance": "Cosine"}
// without the named-vector wrapper (no empty-string key).
ObjectNode vectorsConfig = OBJECT_MAPPER.createObjectNode();
vectorsConfig.put("size", qdrantConfig.vectorSize);
vectorsConfig.put("distance", "Cosine");

ObjectNode body = OBJECT_MAPPER.createObjectNode();
body.set("vectors", vectorsConfig);
```

### 2. `index()` — Vector en upsert de puntos

**Antes**:
```json
"vector": {"": [0.1, 0.2, ...]}
```

**Después**:
```json
"vector": [0.1, 0.2, ...]
```

Código modificado (líneas 77-80):
```java
// Qdrant v1.17.1+: vector is a plain float array, no named-vector wrapper
ObjectNode point = OBJECT_MAPPER.createObjectNode();
point.put("id", toUUID(memoryId).toString());
point.set("vector", toArrayNode(vector));
```

### 3. `search()` — Vector en búsqueda

**Antes**:
```json
"vector": {"": [0.1, 0.2, ...]}
```

**Después**:
```json
"vector": [0.1, 0.2, ...]
```

Código modificado (líneas 125-127):
```java
// Qdrant v1.17.1+: vector is a plain float array, no named-vector wrapper
ObjectNode body = OBJECT_MAPPER.createObjectNode();
body.set("vector", toArrayNode(vector));
```

## Verificación

- **Compilación**: `mvn clean package -DskipTests` → **BUILD SUCCESS** (80 archivos fuente compilados, 0 errores)
- **Regresiones**: Sin cambios en firma de métodos públicos, lógica de negocio o interfaz `SearchIndexer`
- **Eliminación de variable intermedia**: Se eliminó la variable local `vectorNode` (ya no necesaria) tanto en `index()` como en `search()`

## Nota

Esta corrección es **complementaria** a la migración previa de point IDs a UUID, que ya era necesaria para Qdrant v1.17+. Con ambos cambios, el servicio es completamente compatible con Qdrant v1.17.1.
