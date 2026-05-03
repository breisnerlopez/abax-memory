# Redespliegue y Verificacion de ISSUE #7 y #8
- **Fase**: R2 (Correccion de Bugs)
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-03
- **Estado**: Completado
---

## Resumen Ejecutivo

Se realizo el redespliegue del backend Quarkus con los fixes del developer-backend para los ISSUE #7 y #8, y se ejecuto la verificacion completa de cada caso. Se detecto y corrigio una brecha en el fix original (caso C: body vacio retornaba 500), la cual fue parchada in-situ por DevOps. Todos los casos verifican correctamente.

---

## 1. Despliegue

### Stack verificado
| Componente | Estado |
|---|---|
| PostgreSQL (`abax-postgres`) | UP (healthy) |
| Keycloak (`abax-keycloak:26.1`) | UP |
| Qdrant (`qdrant:v1.17.1`) | UP |
| Backend Quarkus 3.15.3 | UP, port 8080 |

### Configuracion aplicada
- `quarkus.oidc.application-type=service` + client credentials grant
- Service account `abax-memory-api` con rol `memory-operator` asignado via Keycloak Admin API
- OpenAI API Key configurada via `OPENAI_API_KEY` env var (no hardcodeada)

### Comando de arranque
```bash
java -jar target/quarkus-app/quarkus-run.jar
```
Con todas las variables de entorno del `docker-compose.yml` exportadas.

---

## 2. Verificacion ISSUE #7 — Validacion de Content-Type y Entrada

**Objetivo**: Todos los casos deben devolver HTTP 400 o 415. NINGUNO debe devolver 500.

### Resultados

| Caso | Descripcion | HTTP | Respuesta | Veredicto |
|---|---|---|---|---|
| A | POST /api/casos sin header Content-Type | **415** | `UNSUPPORTED_MEDIA_TYPE: Unsupported Content-Type` | ✅ PASS |
| B | POST /api/casos con Content-Type: text/plain | **415** | `UNSUPPORTED_MEDIA_TYPE: Unsupported Content-Type` | ✅ PASS |
| C | POST /api/casos body vacio (Content-Type: application/json) | **400** | `INVALID_REQUEST_BODY: Request body is required` | ✅ PASS |
| D | POST /api/memorias/search Content-Type: x-www-form-urlencoded | **415** | `UNSUPPORTED_MEDIA_TYPE: Unsupported Content-Type` | ✅ PASS |

### Comandos ejecutados
```bash
# A) Sin Content-Type
curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"x"}'
# → HTTP 415

# B) text/plain
curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: text/plain" \
  -d '{"title":"x"}'
# → HTTP 415

# C) Body vacio
curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d ''
# → HTTP 400

# D) Form content type
curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/memorias/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'consulta=test&topK=1'
# → HTTP 415
```

### Nota sobre fix adicional (Caso C)
El fix original del developer-backend cubria los casos A, B, D via `NotSupportedException → 415` e `IOException → 400`. Sin embargo, el caso C (body vacio con Content-Type valido) producia un `NullPointerException` porque RESTEasy asigna `null` al parametro `@Valid CreateCaseRequest request` cuando el body esta vacio, en lugar de lanzar `IOException`. El `NullPointerException` no era manejado por el mapper original, resultando en HTTP 500.

**Fix aplicado por DevOps**: Se agrego un handler especifico en `ApiExceptionMapper` que detecta `NullPointerException` causado por request body nulo y retorna HTTP 400 con mensaje `INVALID_REQUEST_BODY: Request body is required`.

```java
// ISSUE #7: NullPointerException caused by null request body
if (exception instanceof NullPointerException npe) {
    if (isNullRequestBody(npe)) {
        LOG.warnv("Null request body detected: {0}", npe.getMessage());
        return build(Response.Status.BAD_REQUEST.getStatusCode(),
            "INVALID_REQUEST_BODY", "Request body is required", List.of());
    }
}
```

---

## 3. Verificacion ISSUE #8 — Simulacion Caida de Base de Datos

**Objetivo**: Al detener PostgreSQL, las peticiones deben devolver **HTTP 503** (Service Unavailable). NUNCA 500.

### Resultado

| Caso | Descripcion | HTTP | Respuesta | Veredicto |
|---|---|---|---|---|
| E | POST /api/casos con BD detenida | **503** | `DATABASE_UNAVAILABLE: Database is temporarily unavailable. Please retry later.` | ✅ PASS |

### Procedimiento
```bash
# 1. Detener PostgreSQL
docker stop abax-postgres

# 2. Esperar 4 segundos para que el pool de conexiones detecte la caida

# 3. Ejecutar peticion
curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"DB outage test","description":"x","priority":"MEDIA","criticality":"MEDIA","origin":"TEST","domain":"qa"}'
# → HTTP 503

# 4. Restaurar PostgreSQL
docker start abax-postgres
```

### Mecanismo de deteccion
El `ApiExceptionMapper.isDatabaseConnectionError()` recorre la cadena de causas de la excepcion buscando:
- `java.sql.SQLException` (cubre `org.postgresql.util.PSQLException`)
- Clases con `JDBCConnectionException` en el nombre (Hibernate)
- `jakarta.persistence.PersistenceException` con mensaje que menciona "connection"
- Clases cuyo nombre contiene `org.postgresql`, `jdbc`, o `sql`
- Mensajes con keywords `jdbc`, `org.postgresql`, o `sqlstate`

Cuando detecta un error de conexion, mapea a HTTP 503 con el codigo `DATABASE_UNAVAILABLE`.

---

## 4. Pruebas Unitarias

### ApiExceptionMapperTest: 18/18 PASS ✅
```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

### CaseResourceTest: 5/5 PASS ✅
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

Nota: Existen 8 fallos preexistentes en `MemoryResourceTest` (4) y `MemoryServiceTest` (4), no relacionados con ISSUE #7 ni #8 (son fallos de integracion con OpenAI API key de test y transiciones de estado de memoria).

---

## 5. Health Check Final

```json
{
    "status": "UP",
    "checks": [{
        "name": "Database connections health check",
        "status": "UP",
        "data": {"<default>": "UP"}
    }]
}
```

PostgreSQL: `accepting connections` ✅

---

## 6. Cambios Realizados

### Archivo modificado
`backend-quarkus/src/main/java/com/btl/administrador/api/exception/ApiExceptionMapper.java`

### Cambios:
1. Agregado handler para `NullPointerException` con deteccion de request body nulo → HTTP 400
2. Agregado metodo helper `isNullRequestBody()` con deteccion por mensaje y stack trace

### Codigo agregado: +35 lineas (handler + metodo helper)

---

## 7. Veredicto Final

| ISSUE | Casos | Resultado |
|---|---|---|
| #7 | A, B, C, D | ✅ Todos PASS (400/415, ningun 500) |
| #8 | E | ✅ PASS (503, no 500) |

**Estado: VERIFICADO y COMPLETADO.** Ambos bugs corregidos satisfactoriamente. El backend esta operativo y saludable.
