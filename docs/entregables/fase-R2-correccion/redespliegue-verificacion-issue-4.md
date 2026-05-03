# Redespliegue y Verificación del Fix — ISSUE #4
- **Fase**: R2 - Corrección
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-03
- **Estado**: Completado

---

## 1. Objetivo

Verificar que el fix del **ISSUE #4** (_Quarkus envuelve JsonParseException en WebApplicationException causando HTTP 500 en lugar de 400_) funciona correctamente tras el redespliegue del backend.

## 2. Contexto del Fix

| Aspecto | Detalle |
|---|---|
| **Issue** | #4 — JsonParseException wrapped in WebApplicationException |
| **Causa raíz** | Quarkus/RESTEasy envuelve `JsonParseException` dentro de `WebApplicationException`, y el `ExceptionMapper` original solo capturaba la excepción superficial |
| **Fix aplicado** | El `ExceptionMapper` ahora recorre la cadena de causas (`getCause()`) para encontrar la `JsonParseException` original y devolver 400 en lugar de 500 |
| **Developer** | Backend Developer |

## 3. Redespliegue

### 3.1 Artefacto desplegado

| Propiedad | Valor |
|---|---|
| **Archivo** | `abax-memory-backend-1.0.0-SNAPSHOT-runner.jar` |
| **Tipo** | Uber-JAR (fat jar) |
| **Tamaño** | 54 MB |
| **Main-Class** | `io.quarkus.runner.GeneratedMain` |
| **Quarkus** | 3.15.3 |
| **Build** | `mvn quarkus:build -DskipTests -Dquarkus.package.jar.type=uber-jar` |

### 3.2 Configuración del entorno

| Variable de entorno | Valor |
|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | `jdbc:postgresql://localhost:5432/pmoadb` |
| `QUARKUS_DATASOURCE_USERNAME` | `pmoa` |
| `QUARKUS_DATASOURCE_PASSWORD` | `***` |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `http://localhost:8443/realms/abax-memory` |
| `QUARKUS_OIDC_CLIENT_ID` | `abax-memory-api` |
| `ABAX_QDRANT_HOST` | `localhost` |
| `ABAX_QDRANT_PORT` | `6333` |
| `OPENAI_API_KEY` | `***` (configurada) |

### 3.3 Comando de inicio

```bash
setsid java -jar backend-quarkus/target/abax-memory-backend-1.0.0-SNAPSHOT-runner.jar \
  > /tmp/abax-memory.log 2>&1 &
```

### 3.4 Verificación de arranque

```json
GET /q/health → 200 OK
{
  "status": "UP",
  "checks": [
    {
      "name": "Database connections health check",
      "status": "UP",
      "data": { "<default>": "UP" }
    }
  ]
}
```

**Tiempo de arranque**: 6.540s  
**Perfil activo**: prod  
**Extensiones cargadas**: agroal, cdi, flyway, hibernate-orm, hibernate-orm-panache, hibernate-validator, jdbc-postgresql, narayana-jta, oidc, rest, rest-jackson, scheduler, security, smallrye-context-propagation, smallrye-health, smallrye-jwt, smallrye-openapi, swagger-ui, vertx

---

## 4. Verificación del ISSUE #4

### 4.1 Autenticación

Token obtenido vía OIDC Password Grant desde Keycloak:
```
POST /realms/abax-memory/protocol/openid-connect/token
client_id=abax-memory-api
username=operator
grant_type=password
→ 200 OK, access_token emitido
```

### 4.2 Casos de prueba

#### Caso A: JSON truncado en `/api/casos`

**Request:**
```bash
curl -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{bad'
```

**Response:**
```json
{
  "code": "INVALID_JSON",
  "message": "Invalid JSON format",
  "correlationId": "577527a8-ba51-4416-b8b2-a1541dd75114",
  "details": [],
  "timestamp": "2026-05-03T00:49:10.460676521-05:00"
}
```
**HTTP Status: `400`** ✅

---

#### Caso B: JSON truncado en `/api/memorias`

**Request:**
```bash
curl -X POST http://localhost:8080/api/memorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{bad'
```

**Response:**
```json
{
  "code": "INVALID_JSON",
  "message": "Invalid JSON format",
  "correlationId": "0a9a51b3-4fbb-4322-8755-912aa913cd31",
  "details": [],
  "timestamp": "2026-05-03T00:49:10.520502581-05:00"
}
```
**HTTP Status: `400`** ✅

---

#### Caso C: JSON malformado (campo `invalid`) en `/api/casos`

**Request:**
```bash
curl -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test",invalid}'
```

**Response:**
```json
{
  "code": "INVALID_JSON",
  "message": "Invalid JSON format",
  "correlationId": "e438e38b-ee81-48a1-8ed9-b82abe10ef0a",
  "details": [],
  "timestamp": "2026-05-03T00:49:10.55318292-05:00"
}
```
**HTTP Status: `400`** ✅

---

## 5. Resumen de resultados

| Caso | Endpoint | Payload | HTTP Esperado | HTTP Obtenido | Resultado |
|---|---|---|---|---|---|
| A | `POST /api/casos` | `{bad` | 400 | **400** | ✅ PASS |
| B | `POST /api/memorias` | `{bad` | 400 | **400** | ✅ PASS |
| C | `POST /api/casos` | `{"title":"Test",invalid}` | 400 | **400** | ✅ PASS |

### Conclusión

- ✅ **100% de casos pasan** (3/3)
- ✅ **Ningún caso devuelve HTTP 500** — el fix elimina el error original
- ✅ El `ExceptionMapper` ahora recorre correctamente la cadena de causas y transforma `JsonParseException` en respuesta `INVALID_JSON` con código 400
- ✅ La respuesta incluye `correlationId` para trazabilidad en logs
- ✅ El fix funciona de manera consistente en ambos endpoints (`/api/casos` y `/api/memorias`)

---

## 6. Notas operativas

- **Plan de rollback**: Si se detecta regresión, revertir al commit anterior y redesplegar el JAR correspondiente
- **Monitoreo**: Observar métricas de errores 4xx/5xx en los endpoints `/api/casos` y `/api/memorias` durante las siguientes 24h
- **Secretos**: La API key de OpenAI se pasa como variable de entorno, nunca en código ni en logs
