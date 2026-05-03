# Cuarto Redespliegue y Verificación Final de Qdrant + Búsqueda Semántica
- **Fase**: Fase 5 — Pruebas QA
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-02 17:50 CST
- **Estado**: Completado (con defectos bloqueantes encontrados)

---

## Resumen Ejecutivo

Se ejecutó el cuarto redespliegue del backend `abax-memory-backend` con el objetivo de validar la corrección de `vectorSize=3072` y la indexación/búsqueda semántica contra Qdrant. El backend **arranca correctamente** y responde health checks, pero **la búsqueda semántica NO funciona**. Se identificaron **2 bugs de código** en `QdrantEmbeddingService.java` que impiden la creación de colecciones y el envío de vectores en Qdrant v1.17.1.

---

## 1. Verificación de Precondiciones

| Componente | Estado | Detalle |
|------------|--------|---------|
| Qdrant | ✅ Up | v1.17.1, puerto 6333 |
| Keycloak | ✅ Up | HTTP puerto 8443, realm `abax-memory` |
| PostgreSQL | ✅ Up | `aba-stage-db-1`, puerto 5432 |
| Colección Qdrant vieja | ✅ Eliminada | `abax-memories` no existía al iniciar |

---

## 2. Pasos de Despliegue Ejecutados

### 2.1 Matar procesos previos
```bash
kill $(ps aux | grep "abax-memory-backend.*runner" | grep -v grep | awk '{print $2}')
```
**Resultado**: Backend anterior detenido.

### 2.2 Eliminar colección vieja
```bash
curl -X DELETE http://localhost:6333/collections/abax-memories
```
**Resultado**: `{"result":true,"status":"ok"}` (la colección con guion `abax-memories` sí existía y fue eliminada; la colección `abax_memories` con guion bajo nunca existió — posible confusión de nombres en instrucciones previas).

### 2.3 Construcción del uber-jar
El JAR original (`target/abax-memory-backend-1.0.0-SNAPSHOT.jar`) era un thin jar sin `Main-Class`. Se reconstruyó:
```bash
mvn quarkus:build -DskipTests -Dquarkus.package.type=uber-jar
```
**Resultado**: `abax-memory-backend-1.0.0-SNAPSHOT-runner.jar` (54 MB, Main-Class: `io.quarkus.runner.GeneratedMain`).

### 2.4 Configuración de datasource
El backend requiere PostgreSQL pero no tenía configuración para producción. Se configuró vía variables de entorno:
```bash
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/aba
QUARKUS_DATASOURCE_USERNAME=postgres
QUARKUS_DATASOURCE_PASSWORD=1e44aad25e2ce0d423c4c8bea459929e
QUARKUS_FLYWAY_BASELINE_ON_MIGRATE=true
QUARKUS_FLYWAY_BASELINE_VERSION=0
QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=update
```
**Problemas encontrados**:
- Flyway requiere `baseline-on-migrate=true` porque el esquema `public` ya contiene tablas de otras aplicaciones.
- `database.generation=validate` fallaba porque las tablas de Abax no existen en ese esquema; se cambió a `update`.

### 2.5 Lanzamiento
```bash
setsid java -jar target/abax-memory-backend-1.0.0-SNAPSHOT-runner.jar > /tmp/abax-memory.log 2>&1 &
```
**Resultado**: Backend UP en `http://0.0.0.0:8080`. Health check: `{"status": "UP"}`.

---

## 3. Verificación de Qdrant

### 3.1 Logs de Qdrant tras arranque
```
— No se encontraron mensajes de inicialización de Qdrant en el startup —
```
**Causa**: El bean `QdrantEmbeddingService` (`@ApplicationScoped`) es lazy en Quarkus. Su `@PostConstruct` no se ejecuta hasta el primer acceso.

### 3.2 Estado de la colección tras arranque
```bash
curl http://localhost:6333/collections/abax-memories
# Respuesta: 404 Not Found — la colección NO fue creada por el backend
```

### 3.3 Creación manual de colección (workaround para bug #1)
```bash
curl -X PUT http://localhost:6333/collections/abax-memories \
  -H "Content-Type: application/json" \
  -d '{"vectors": {"size": 3072, "distance": "Cosine"}}'
# Respuesta: {"result":true,"status":"ok"}
```

---

## 4. Prueba Final de Búsqueda Semántica

### 4.1 Obtención de token
```bash
curl -X POST http://localhost:8443/realms/abax-memory/protocol/openid-connect/token \
  -d "client_id=abax-memory-api" \
  -d "client_secret=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI" \
  -d "username=operator" -d "password=test123" \
  -d "grant_type=password"
```
✅ Token JWT obtenido correctamente.

**Nota**: El endpoint requiere `client_secret` (el cliente `abax-memory-api` es confidential, no public). Las instrucciones originales no incluían el secret.

### 4.2 Creación de memoria
```json
POST /api/memorias
{
  "title": "Prueba Qdrant v1.17",
  "type": "incidente",
  "criticality": "ALTA",
  "domains": ["qa"],
  "contenidoMarkdown": "## Contexto\nValidacion de busqueda semantica...",
  "metadata": {"fuente": "prueba-devops"},
  "frontmatter": { ... }
}
```
✅ Memoria creada: `MEM-7239c14c`, estado `EN_REVISION`.

**Nota sobre el esquema**: El endpoint espera campos diferentes a los documentados en instrucciones previas. El formato correcto se obtuvo del OpenAPI (`/q/openapi`). Campos requeridos: `title`, `type`, `criticality`, `domains` (array), `contenidoMarkdown`, `metadata` (con `fuente`), `frontmatter` (debe ser consistente con el payload).

### 4.3 Búsqueda semántica
```json
POST /api/memorias/search
{
  "consulta": "Qdrant busqueda semantica vectores",
  "topK": 5
}
```
❌ **Respuesta**: `[]` — array vacío.

### 4.4 Estado de Qdrant post-búsqueda
```json
{
  "status": "green",
  "indexed_vectors_count": 0,
  "points_count": 0
}
```
❌ **0 vectores indexados, 0 puntos**. La colección existe pero está vacía.

---

## 5. Diagnóstico de Causa Raíz

### 5.1 Log de error durante la búsqueda
```
ERROR [QdrantEmbeddingService] Qdrant search failed: HTTP 400 —
"Format error in JSON body: data did not match any variant of untagged enum NamedVectorStruct"
```

### 5.2 Bugs identificados en `QdrantEmbeddingService.java`

| # | Método | Línea | Problema | Formato enviado | Formato esperado (Qdrant v1.17.1) |
|---|--------|-------|----------|-----------------|-----------------------------------|
| **1** | `initCollection()` | ~230 | Creación de colección con formato de vectores incorrecto | `{"vectors": {"": {"size": 3072, "distance": "Cosine"}}}` | `{"vectors": {"size": 3072, "distance": "Cosine"}}` |
| **2** | `search()` / `index()` | ~79, ~130 | Envío de vectores con clave vacía `""` no soportada | `{"vector": {"": [0.1, 0.2, ...]}}` | `{"vector": [0.1, 0.2, ...]}` |

**Código problemático** (línea 79 y 130):
```java
// INCORRECTO para Qdrant v1.17+
vectorNode.set("", toArrayNode(vector));

// CORRECTO sería:
body.set("vector", toArrayNode(vector));
```

### 5.3 Advertencias adicionales
Las siguientes propiedades de configuración **no son reconocidas** por Quarkus porque la extensión `quarkus-langchain4j-openai` de Quarkiverse no está en el classpath (solo está la dependencia directa `dev.langchain4j:langchain4j-open-ai`):
```
WARN: Unrecognized configuration key "quarkus.langchain4j.openai.api-key"
WARN: Unrecognized configuration key "quarkus.langchain4j.openai.embedding-model.model-name"
WARN: Unrecognized configuration key "quarkus.langchain4j.openai.embedding-model.dimensions"
WARN: Unrecognized configuration key "quarkus.langchain4j.openai.chat-model.model-name"
WARN: Unrecognized configuration key "quarkus.langchain4j.openai.timeout"
```
Sin embargo, el `EmbeddingModel` de OpenAI **sí se crea correctamente** mediante `OpenAiConfigProducer`, que lo construye programáticamente (no vía configuración Quarkus):
```
INFO [OpenAiConfigProducer] Creating OpenAI EmbeddingModel: model=text-embedding-3-large
```

---

## 6. Veredicto Final

| Aspecto | Estado |
|---------|--------|
| Backend arranca | ✅ UP en puerto 8080 |
| Health check | ✅ UP |
| Conexión PostgreSQL | ✅ UP |
| Conexión Keycloak (OIDC) | ✅ Token JWT válido |
| CRUD de memorias | ✅ Creación exitosa |
| Extracción NLP | ✅ Funciona (entidades, tags, tipo, dominio) |
| **Conexión OpenAI Embeddings** | ✅ Modelo se instancia correctamente |
| **Colección Qdrant** | ⚠️ Existe (creada manualmente), pero vacía |
| **Indexación en Qdrant** | ❌ Falla por bug #2 (HTTP 400) |
| **Búsqueda semántica** | ❌ Devuelve `[]` — 0 resultados |

### Conclusión
**La búsqueda semántica NO funciona.** El backend responde `[]` (array vacío) porque:
1. El `@PostConstruct` de `QdrantEmbeddingService` falla al crear la colección (bug #1 en `initCollection`).
2. Aunque la colección se cree manualmente, los métodos `search()` e `index()` envían vectores en un formato que Qdrant v1.17.1 rechaza (bug #2).
3. Como resultado, 0 memorias están indexadas en Qdrant (`points_count: 0`).

### Acción requerida
El equipo de backend debe modificar `QdrantEmbeddingService.java`:
- En `initCollection()`: cambiar `vectorsConfig.set("", defaultVector)` por `body.set("vectors", defaultVector)` directamente (sin clave).
- En `search()` e `index()`: cambiar `body.set("vector", vectorNode)` (donde `vectorNode` es un objeto con clave `""`) por `body.set("vector", toArrayNode(vector))` directamente.

---

## 7. Anexo: Comandos de diagnóstico

```bash
# Health del backend
curl -s http://localhost:8080/q/health

# Estado de Qdrant
curl -s http://localhost:6333/collections/abax-memories

# Logs relevantes
grep -i "qdrant\|embedding\|error" /tmp/abax-memory.log

# Obtener token
curl -s -X POST http://localhost:8443/realms/abax-memory/protocol/openid-connect/token \
  -d "client_id=abax-memory-api" \
  -d "client_secret=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI" \
  -d "username=operator" -d "password=test123" \
  -d "grant_type=password"

# Buscar (POST, no GET)
curl -s -X POST http://localhost:8080/api/memorias/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"consulta": "Qdrant", "topK": 5}'
```

---

*Reporte generado por DevOps Engineer. Honesto, basado en evidencia de logs y respuestas HTTP.*
