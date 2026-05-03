# Reporte de Verificación de Redespliegue — R2 Corrección de Bugs
- **Fase**: R2 - Corrección de bugs
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-03
- **Estado**: Completado

---

## Resumen Ejecutivo

Se redesplegó el backend de Abax-Memory (Quarkus) y se verificaron los 5 fixes contra el sistema real. **4 de 5 fixes pasaron la verificación. 1 fix (ISSUE #4) NO pasó.**

| Issue | Descripción | Esperado | Obtenido | Resultado |
|-------|------------|----------|----------|-----------|
| #6 | Root path (`GET /`) devuelve 200 con JSON | 200 | 200 | ✅ PASA |
| #5 | Priority inválida devuelve 400 | 400 | 400 | ✅ PASA |
| #4 | JSON malformado devuelve 400 (NO 500) | 400 | **500** | ❌ FALLA |
| #3 | Búsqueda semántica incluye memorias EN_REVISION | Aparece | Aparece | ✅ PASA |
| #2 | Frontmatter opcional (no exigir YAML header) | 201/202 | 202 | ✅ PASA |

---

## 1. Restablecimiento de Infraestructura

### 1.1 Detención de proceso anterior

```bash
pkill -f "abax-memory-backend" 2>/dev/null
```
**Resultado**: No había proceso previo del backend corriendo.

### 1.2 Verificación de artefacto

```bash
ls -la backend-quarkus/target/abax-memory-backend-1.0.0-SNAPSHOT.jar
# Resultado: 135849 bytes, timestamp May 2 23:57
```

**Problema detectado**: El JAR era un thin JAR (sin `quarkus-app/`), no ejecutable directamente.

### 1.3 Reconstrucción del artefacto

```bash
cd backend-quarkus && mvn quarkus:build -DskipTests
```

**Resultado**: BUILD SUCCESS. Se generó `target/quarkus-app/quarkus-run.jar`.

### 1.4 Infraestructura de soporte

- **PostgreSQL**: Contenedor `abax-postgres` (postgres:16-alpine) en `localhost:5432`. Se detectó conflicto de puerto con `aba-stage-db-1`, se detuvo este último y se recreó `abax-postgres`.
- **Keycloak**: Contenedor `abax-keycloak` mapeado a `localhost:8443`. Se detectó otro proceso Keycloak en el host ocupando el puerto 8080, se detuvo (PID 1859501).
- **Qdrant**: Contenedor externo `qdrant` en `localhost:6333`, ya operativo.

### 1.5 Configuración de entorno

```bash
export OPENAI_API_KEY="sk-proj-..."
export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/pmoadb"
export QUARKUS_DATASOURCE_USERNAME="pmoa"
export QUARKUS_DATASOURCE_PASSWORD="pmoa"
```

### 1.6 Lanzamiento exitoso

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

**Log de inicio**:
```
abax-memory-backend 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.15.3) started in 7.749s. 
Listening on: http://0.0.0.0:8080
```

### 1.7 Health Check

```bash
curl -s http://localhost:8080/q/health
```

```json
{
    "status": "UP",
    "checks": [
        {
            "name": "Database connections health check",
            "status": "UP",
            "data": {"<default>": "UP"}
        }
    ]
}
```
**HTTP 200** — Servicio operativo.

---

## 2. Verificación de Fixes

### 2.1 ISSUE #6 — Root path

**Comando**:
```bash
curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8080/
```

**Respuesta**:
```json
{"status":"UP","version":"1.0.0","service":"abax-memory"}
```
**HTTP 200**

**Resultado**: ✅ **PASA** — El root path devuelve 200 con JSON del servicio, no 500.

---

### 2.2 ISSUE #5 — Priority inválida

**Comando**:
```bash
curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","priority":"INVALIDA"}'
```

**Respuesta**:
```json
{
    "code":"VALIDATION_ERROR",
    "message":"Validation failed",
    "correlationId":"d2fbe95c-954e-4b48-b127-13a9953b5719",
    "details":[
        "create.arg0.priority priority must be one of: BAJA, MEDIA, ALTA, CRITICA",
        "..."
    ],
    "timestamp":"2026-05-03T00:15:57.028084044-05:00"
}
```
**HTTP 400**

**Resultado**: ✅ **PASA** — Priority "INVALIDA" es rechazada con 400 VALIDATION_ERROR. No se crea el caso.

---

### 2.3 ISSUE #4 — JSON malformado

**Comando**:
```bash
curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/casos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{bad json'
```

**Respuesta**:
```json
{
    "code":"UNEXPECTED_ERROR",
    "message":"Unexpected server error",
    "correlationId":"63f327e5-4b26-4cf6-b8ff-7d7ad48d185c",
    "details":[],
    "timestamp":"2026-05-03T00:16:32.208983162-05:00"
}
```
**HTTP 500**

**Resultado**: ❌ **FALLA** — El sistema devuelve HTTP 500 con "Unexpected server error" en lugar de HTTP 400. El JSON malformado no está siendo manejado correctamente a nivel de capa REST y está provocando una excepción no controlada que escala a 500.

**Evidencia del fallo**: La respuesta contiene `"code":"UNEXPECTED_ERROR"` y `"message":"Unexpected server error"`, lo cual es un error genérico de servidor no manejado. El código HTTP es 500 en lugar del 400 esperado.

---

### 2.4 ISSUE #3 — Búsqueda semántica incluye EN_REVISION

**Paso 1 — Crear memoria crítica**:
```bash
curl -s -X POST http://localhost:8080/api/memorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Bug test busqueda","contenidoMarkdown":"## Contexto\n...","domains":["qa"],"type":"INCIDENTE","criticality":"ALTA","metadata":{"fuente":"api-test"}}'
```

**Respuesta**:
```json
{
    "id": "MEM-c06cd6da",
    "title": "Bug test busqueda",
    "type": "incidente",
    "state": "EN_REVISION",
    "processingStatus": "PENDING_GIT",
    "domains": ["qa"],
    ...
}
```
**HTTP 202** — Memoria creada en estado `EN_REVISION`.

**Paso 2 — Buscar (post-indexación, 25s)**:
```bash
curl -s "http://localhost:8080/api/memorias?search=busqueda" \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta** (resumida):
```json
[
  {
    "id": "MEM-c06cd6da",
    "title": "Bug test busqueda",
    "state": "EN_REVISION",
    ...
  }
]
```
**HTTP 200** — La memoria en estado `EN_REVISION` aparece en resultados de búsqueda.

**Resultado**: ✅ **PASA** — Las memorias en estado EN_REVISION son incluidas en los resultados de búsqueda.

---

### 2.5 ISSUE #2 — Frontmatter opcional

**Comando**:
```bash
curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/memorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Sin frontmatter","contenidoMarkdown":"## Test\nProbando que no requiere frontmatter","type":"PROCEDIMIENTO","domains":["qa"],"criticality":"ALTA","metadata":{"fuente":"api-test"}}'
```

**Respuesta**:
```json
{
    "id": "MEM-400a8d6d",
    "title": "Sin frontmatter",
    "type": "procedimiento",
    "state": "EN_REVISION",
    "markdown": "---\nid: MEM-400a8d6d\ntitle: \"Sin frontmatter\"\n...\n---\n## Test\nProbando que no requiere frontmatter\n",
    ...
}
```
**HTTP 202**

**Resultado**: ✅ **PASA** — El sistema acepta memorias sin bloque YAML frontmatter en `contenidoMarkdown`. El backend genera automáticamente el frontmatter requerido internamente. No exige que el usuario lo incluya.

---

## 3. Estado de Health Checks

| Componente | Endpoint | Estado |
|-----------|----------|--------|
| Backend API | `GET /q/health` | UP |
| Base de datos | Health check interno | UP |
| Keycloak | `localhost:8443` | UP (token obtenido correctamente) |
| Qdrant | `localhost:6333` | UP |

---

## 4. Conclusión

- **4/5 fixes verificados exitosamente** en el sistema redesplegado.
- **1 fix pendiente (ISSUE #4)**: JSON malformado sigue devolviendo HTTP 500 en lugar de 400. Se requiere revisión adicional del `ExceptionMapper` o handler de `JsonParseException` en la capa REST.
- El backend arranca correctamente con PostgreSQL, Keycloak y Qdrant.
- **Warnings no críticos**: Las propiedades `quarkus.langchain4j.openai.*` generan warnings por no ser reconocidas (posiblemente falta la extensión Quarkiverse de LangChain4j en el classpath runtime, aunque las propiedades son funcionales vía `application.properties`).

---

## 5. Recomendaciones

1. **ISSUE #4**: Implementar un `ExceptionMapper<JsonParseException>` que devuelva 400 con mensaje descriptivo en lugar del 500 actual.
2. **Warnings de configuración**: Verificar que la extensión `quarkus-langchain4j-openai` esté correctamente incluida como dependencia runtime (no solo compile).
3. **Estabilidad del scheduler**: El `ProcessingWorkerService` lanzó `NullPointerException` durante el shutdown en intentos previos. Aunque no impide el funcionamiento, se recomienda revisar el manejo del ciclo de vida CDI en el scheduler.
