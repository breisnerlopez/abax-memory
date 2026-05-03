# Corrección definitiva ISSUE #4 — JSON malformado devuelve 400 en lugar de 500
- **Fase**: R2 - Corrección de bugs
- **Responsable**: developer-backend
- **Fecha**: 2026-05-03
- **Estado**: Completado

---

## Resumen Ejecutivo

Se diagnosticó y corrigió definitivamente el ISSUE #4. Tras el diagnóstico, se identificó que Quarkus RESTEasy Reactive envuelve `JsonParseException` dentro de `jakarta.ws.rs.WebApplicationException` **antes** de que llegue a cualquier `ExceptionMapper`, por lo que el `instanceof JsonParseException` nunca coincidía. La solución implementada recorre la cadena de causas buscando `JsonProcessingException` en cualquier nivel de la jerarquía de excepciones.

**Resultado**: JSON malformado ahora devuelve **400 Bad Request** (`INVALID_JSON`), no 500.

---

## Diagnóstico de Causa Raíz

### Evidencia del log (`/tmp/abax-memory.log`)

```log
ERROR [com.btl.adm.api.exc.ApiExceptionMapper] Unexpected server error: 
jakarta.ws.rs.WebApplicationException: HTTP 400 Bad Request
    at io.quarkus.resteasy.reactive.jackson.runtime.serialisers
        .ServerJacksonMessageBodyReader.readFrom(ServerJacksonMessageBodyReader.java:60)
Caused by: com.fasterxml.jackson.core.JsonParseException: 
    Unexpected character ('b' (code 98)): was expecting double-quote to start field name
```

### Cadena de eventos

```
1. Cliente envía JSON malformado → {"foo bar}
2. Jackson lanza JsonParseException (checked)
3. ServerJacksonMessageBodyReader.doReadFrom() captura JsonParseException
4. Lo re-lanza como WebApplicationException(JsonParseException, 400)
5. El ExceptionMapper<Exception> recibe WebApplicationException (NO JsonParseException)
6. instanceof JsonParseException → FALSE (es WebApplicationException)
7. instanceof JsonProcessingException → FALSE (es WebApplicationException)
8. Cae al catch-all → 500 Internal Server Error
```

### Por qué fallaba el fix anterior

```java
// CÓDIGO ANTERIOR (NO FUNCIONABA)
if (exception instanceof JsonParseException || exception instanceof JsonProcessingException) {
    // ↓ NUNCA se ejecuta porque la excepción que llega es WebApplicationException
    return build(400, "INVALID_JSON", "Invalid JSON format", List.of());
}
```

El `instanceof` solo verifica el tipo **inmediato** de la excepción, no su causa.

---

## Solución Implementada

### Enfoque: Recorrido de cadena de causas

Se reemplazó el `instanceof` simple por un método `isJsonProcessingError(Throwable)` que recorre TODA la cadena de causas (`getCause()`) buscando `JsonProcessingException` (superclase de `JsonParseException`).

**Archivo modificado**: `backend-quarkus/src/main/java/com/btl/administrador/api/exception/ApiExceptionMapper.java`

### Cambio principal

```java
// NUEVO: verificación por cadena de causas
if (isJsonProcessingError(exception)) {
    LOG.warnv("Invalid JSON format: {0}", exception.getMessage());
    return build(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_JSON",
            "Invalid JSON format", List.of());
}
```

### Método auxiliar

```java
/**
 * Recorre la cadena de causas para detectar errores de procesamiento JSON.
 * Quarkus envuelve JsonParseException en WebApplicationException antes de
 * que llegue a cualquier ExceptionMapper, por lo que un instanceof simple
 * sobre la excepción de nivel superior es insuficiente.
 */
private boolean isJsonProcessingError(Throwable exception) {
    Throwable current = exception;
    while (current != null) {
        if (current instanceof JsonProcessingException) {
            return true;
        }
        current = current.getCause();
    }
    return false;
}
```

### Escenarios cubiertos

| Escenario | Excepción recibida | Resultado |
|---|---|---|
| `JsonParseException` directa | `JsonParseException` | ✅ 400 `INVALID_JSON` |
| `WebApplicationException` envolviendo `JsonParseException` (caso Quarkus) | `WebApplicationException` → cause: `JsonParseException` | ✅ 400 `INVALID_JSON` |
| `WebApplicationException` → `RuntimeException` → `JsonProcessingException` (doble envoltura) | `WebApplicationException` → cause → cause: `JsonProcessingException` | ✅ 400 `INVALID_JSON` |
| `RuntimeException` genérica (sin JSON en la causa) | `RuntimeException("algo falló")` | ✅ 500 `UNEXPECTED_ERROR` |
| Excepción de seguridad | `UnauthorizedException` | ✅ 401 (se verifica antes en el mapper) |

---

## Pruebas Unitarias

**Archivo**: `backend-quarkus/src/test/java/com/btl/administrador/api/exception/ApiExceptionMapperTest.java`

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

| Test | Descripción | Resultado |
|---|---|---|
| `directJsonProcessingException_returns400InvalidJson` | `JsonProcessingException` directa → 400 | ✅ PASS |
| `webApplicationExceptionWrappingJsonProcessing_returns400InvalidJson` | `WebApplicationException` envolviendo `JsonProcessingException` (caso Quarkus real) → 400 | ✅ PASS |
| `deepCauseChainWithJsonProcessing_returns400InvalidJson` | Triple envoltura con `JsonProcessingException` en la causa profunda → 400 | ✅ PASS |
| `genericExceptionWithoutJsonCause_returns500UnexpectedError` | `RuntimeException` sin JSON → 500 (sin regresión) | ✅ PASS |
| `responseIncludesCorrelationIdAndTimestamp` | Verifica que el cuerpo de respuesta incluye `correlationId` y `timestamp` | ✅ PASS |

---

## Build y Despliegue

```bash
cd backend-quarkus && mvn clean package -DskipTests
# BUILD SUCCESS — Total time: 9.272 s
```

---

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `backend-quarkus/src/main/java/.../exception/ApiExceptionMapper.java` | Reemplazo de `instanceof JsonParseException \|\| JsonProcessingException` por `isJsonProcessingError(exception)` con recorrido de cadena de causas. Eliminación de import no utilizado (`JsonParseException`). |
| `backend-quarkus/src/test/java/.../exception/ApiExceptionMapperTest.java` | **Nuevo archivo**. 5 tests unitarios que cubren todos los escenarios de JSON malformado y el caso de regresión (excepción genérica). |
