# Estado Real del Despliegue — Verificación Funcional Honesta
- **Fase**: 7 — Despliegue
- **Responsable**: devops
- **Fecha**: 2026-05-02
- **Estado**: **NO FUNCIONAL — BLOQUEANTE**
- **Versión**: v1.0

---

## 0. Conclusión Anticipada (Resumen Ejecutivo)

> **El sistema NO está funcional para ningún flujo de negocio real.** El backend arranca y responde health checks (`UP`), pero todos los endpoints de API fallan (401 o 500). El usuario tiene razón: **no hay API keys configuradas para modelos de IA porque el código ni siquiera implementa integración con modelos de IA** — usa stubs 100% regex e in-memory. De las 10 condiciones del gate F7, **2 están cumplidas, 2 parciales, y 6 no cumplidas**. La ventana de despliegue del 2026-05-04 **no puede iniciar** en estas condiciones.

---

## 1. Condiciones del Gate F7 — Estado Real Verificado

Cada condición fue verificada con comandos reales ejecutados en el servidor. La evidencia se documenta con comando y salida.

| # | ID | Condición | Estado Real | Evidencia |
|---|---|---|---|---|
| 1 | F7-C01 | **Dockerfile multi-stage validado** | ❌ NO CUMPLIDA | `find / -name Dockerfile` en todo el repo → **0 resultados**. No existe ningún Dockerfile. |
| 2 | F7-C02 | **K8s manifests validados** | ❌ NO CUMPLIDA | No existe directorio `infra/k8s/`. No hay archivos YAML de Deployment, Service, Ingress. |
| 3 | F7-C03 | **Despliegue en staging exitoso** | ⚠️ PARCIAL | Backend corre en `localhost:8080`, health `UP`, pero todos los endpoints funcionales fallan (401/500). No hay smoke tests automatizados ejecutándose. |
| 4 | F7-C04 | **PostgreSQL prod creado y accesible** | ✅ CUMPLIDA | `pg_isready -h localhost -p 5432 -U pmoa -d pmoadb` → `accepting connections`. DB `pmoadb` existe, Flyway aplicó migración v1. |
| 5 | F7-C05 | **Qdrant prod desplegado con health check UP** | ✅ CUMPLIDA | `curl localhost:6333/healthz` → `healthz check passed`. Versión 1.17.1 corriendo. **Pero**: 0 colecciones creadas. |
| 6 | F7-C06 | **Keycloak realm `abax-memory` configurado** | ❌ NO CUMPLIDA | `curl localhost:8443/realms/abax-memory` → `404 Realm does not exist`. Keycloak corre pero el realm nunca fue creado. |
| 7 | F7-C07 | **Secretos K8s cargados en namespace prod** | ❌ NO CUMPLIDA | Kubernetes no está instalado. No existe namespace `abax-memory-prod`. |
| 8 | F7-C08 | **Registry de imágenes accesible desde cluster** | ❌ NO CUMPLIDA | No hay imágenes Docker construidas, no hay registry, no hay cluster K8s. |
| 9 | F7-C09 | **Tag `v1.0.0-release` aplicado al commit aprobado** | ❌ NO CUMPLIDA | `git tag -l "v1.0.0*"` → **sin resultados**. HEAD = `880fa50 docs: finalize publication report`. |
| 10 | F7-C10 | **GitHub deploy token generado y almacenado en Secret** | ❌ NO CUMPLIDA | No hay Secret de K8s, no hay token de deploy. |

### Resumen Cuantitativo

| Estado | Cantidad | Porcentaje |
|---|---|---|
| ✅ Cumplida | 2 | 20% |
| ⚠️ Parcial | 1 (C03) + C05 (Qdrant OK pero 0 cols) | ~15% |
| ❌ No cumplida | 7 | ~65% |

### Condiciones Bloqueantes para la Ventana de Despliegue

Según el acta del gate: _"Si 1 o más condiciones no se cumplen, la ventana de despliegue no podrá iniciar."_ Con 7-8 condiciones no cumplidas, **la ventana del 2026-05-04 06:00 COT es inviable**.

---

## 2. API Keys para Modelos de IA — Verificación Exhaustiva

### 2.1 ¿Qué modelos espera usar el proyecto según la documentación?

- **Embeddings**: `text-embedding-3-large` de OpenAI para indexación semántica en Qdrant
- **Extracción estructurada**: Modelo con structured outputs (JSON mode)
- **Validación crítica**: Modelo avanzado para revisión de memorias críticas

Fuentes: `docs/entregables/fase-3-diseno-tecnico/documento-arquitectura.md` §17.1.7 — "Integración de embeddings + Qdrant".

### 2.2 ¿Qué implementa REALMENTE el código?

| Servicio | Archivo | Qué HACE Realmente | Qué DEBERÍA Hacer |
|---|---|---|---|
| `StructuredExtractionService` | `backend-quarkus/src/main/java/.../service/StructuredExtractionService.java` | **Regex puro** sobre Markdown (`Pattern.compile`, `Normalizer`). Extrae "pasos", "decisiones", "evidencias" buscando headings `##` y bullets. | Llamar a un LLM (OpenAI mini) con structured outputs para extraer entidades del texto. |
| `InMemorySearchIndexer` | `backend-quarkus/src/main/java/.../integration/qdrant/InMemorySearchIndexer.java` | **Token matching en RAM** con un `HashMap<String, String>`. "Similitud" = intersección de tokens dividida por tamaño del query. Grupos semánticos hardcodeados (`"contrasena" → "clave", "password"`). | Llamar a `text-embedding-3-large`, generar vectores reales, indexar en Qdrant, buscar por similitud coseno real. |
| `SearchIndexer` (interfaz) | `backend-quarkus/src/main/java/.../integration/qdrant/SearchIndexer.java` | Interfaz con 3 métodos: `index()`, `search()`, `clear()`. **Única implementación**: `InMemorySearchIndexer`. | Debería haber una implementación `QdrantSearchIndexer` que use el SDK de Qdrant + embeddings de OpenAI. |

### 2.3 Búsqueda de API Keys y Configuración

Se ejecutaron las siguientes búsquedas:

```bash
# 1. Archivos .env
find /root/proyectos-personales/Abax-Memory -name ".env*" -type f
→ 0 resultados

# 2. Variables de entorno con API keys
env | grep -i "openai\|api_key\|api\.key\|embedding"
→ 0 resultados

# 3. Referencias en properties
grep -r "openai\|OPENAI\|embedding\|text-embedding\|api\.key\|API_KEY" backend-quarkus/src/main/resources/
→ 0 resultados

# 4. Referencias en código Java
grep -r "openai\|OpenAI\|embedding\|Embedding\|langchain4j\|LangChain4j" backend-quarkus/src/main/java/
→ 0 resultados

# 5. Dependencias Maven
grep -i "langchain\|openai\|embedding\|hugging\|onnx" backend-quarkus/pom.xml
→ 0 resultados
```

### 2.4 Mecanismo de Configuración Requerido (NO Implementado)

Para que el sistema use modelos de IA reales, se necesitaría:

1. **Dependencia Maven**: Agregar `langchain4j-openai` o `quarkus-langchain4j-openai` al `pom.xml`
2. **API Key de OpenAI**: Configurar `quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}` en `application.properties`
3. **Modelo de embeddings**: `quarkus.langchain4j.openai.embedding-model.name=text-embedding-3-large`
4. **Variable de entorno**: `export OPENAI_API_KEY=sk-...` O vía K8s Secret `openai-api-key`
5. **Implementación real de SearchIndexer**: Clase `QdrantSearchIndexer` que use el SDK de Qdrant + embeddings
6. **Implementación real de StructuredExtraction**: Usar LangChain4j `AiServices` con `@SystemMessage` y `@UserMessage`

**Nada de esto existe en el código actual.**

---

## 3. Prueba Funcional Real de Endpoints

### 3.1 Servicios Externos

| Servicio | URL | Estado | Detalle |
|---|---|---|---|
| **Backend Quarkus** | `http://localhost:8080` | 🟢 UP | Health OK, PID 1299495 |
| **Qdrant** | `http://localhost:6333` | 🟢 UP | v1.17.1, 0 colecciones |
| **Keycloak** | `http://localhost:8443` | 🟡 UP parcial | Corre pero realm `abax-memory` no existe (404) |
| **PostgreSQL** | `localhost:5432/pmoadb` | 🟢 UP | Conexión OK, migración Flyway v1 aplicada |
| **Dockerfiles** | N/A | 🔴 NO EXISTEN | 0 archivos Dockerfile en el repo |
| **Git Tag v1.0.0-release** | N/A | 🔴 NO EXISTE | Sin tags de release |

### 3.2 Endpoints de API — Resultados Reales

Comandos ejecutados y respuestas reales:

```
=== POST /api/casos ===
$ curl -s -X POST http://localhost:8080/api/casos -H "Content-Type: application/json" -d '{"titulo":"Caso prueba"}'
→ HTTP 401 (cuerpo vacío)
Causa: OIDC está deshabilitado en build-time pero el endpoint espera autenticación JWT.

=== GET /api/casos ===
$ curl -s http://localhost:8080/api/casos
→ HTTP 500
Body: {"code":"UNEXPECTED_ERROR","message":"Unexpected server error","correlationId":null,"details":[],...}
Causa: Sin OIDC configurado, el endpoint no tiene contexto de seguridad. Lanza excepción no manejada.

=== POST /api/memorias ===
$ curl -s -X POST http://localhost:8080/api/memorias -H "Content-Type: application/json" -d '{"titulo":"Memoria prueba"}'
→ HTTP 401 (cuerpo vacío)
Causa: Igual que /api/casos — endpoint protegido requiere JWT.

=== GET /api/memorias ===
$ curl -s http://localhost:8080/api/memorias
→ HTTP 401 (cuerpo vacío)
Causa: Endpoint protegido.

=== POST /api/memorias/search ===
$ curl -s -X POST http://localhost:8080/api/memorias/search -H "Content-Type: application/json" -d '{"consulta":"test","topK":5}'
→ HTTP 500
Body: {"code":"UNEXPECTED_ERROR","message":"Unexpected server error",...}
Causa: Probablemente el mismo problema de falta de contexto de seguridad OIDC + InMemorySearchIndexer vacío.
```

### 3.3 Endpoints que SÍ Funcionan

| Endpoint | Código | Nota |
|---|---|---|
| `/q/health` | 200 | `{"status":"UP"}` con DB check OK |
| `/q/health/live` | 200 | `{"status":"UP"}` |
| `/q/health/ready` | 200 | `{"status":"UP"}` con DB check OK |
| `/q/openapi` | 200 | Especificación OpenAPI 3.0.3 completa |
| `/q/swagger-ui` | 302 | Redirección a Swagger UI |

**Conclusión**: Solo funcionan los endpoints de infraestructura (health, OpenAPI). **Cero endpoints de negocio funcionales**.

---

## 4. Diagnóstico de Causa Raíz

### 4.1 ¿Por qué fallan los endpoints con 401?

El build del backend se realizó con **OIDC explícitamente deshabilitado**:

```bash
# Comando de build real (documentado en ejecucion-despliegue.md §2.4)
mvn quarkus:build -DskipTests \
  -Dquarkus.oidc.enabled=false \
  -Dquarkus.datasource.devservices.enabled=false \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/pmoadb \
  ...
```

`quarkus.oidc.enabled=false` es una propiedad **build-time fixed** en Quarkus. Una vez deshabilitada, no hay forma de habilitarla en runtime. Esto significa que:
- Los endpoints anotados con `@RolesAllowed` o `@Authenticated` **siempre** rechazan peticiones sin JWT → 401
- Algunos endpoints que requieren inyección de `SecurityIdentity` lanzan excepción → 500

### 4.2 ¿Por qué no está configurado el realm de Keycloak?

Keycloak se instaló en modo `start-dev` con admin temporal (`admin/admin`). El realm `abax-memory` nunca fue creado. Para crearlo se requiere:
1. Acceder a `http://localhost:8443/admin` con admin/admin
2. Crear realm `abax-memory`
3. Crear client `abax-memory-api` con OIDC config
4. Configurar roles: `memory-operator`, `memory-reviewer`, `memory-admin`, `memory-auditor`, `api-consumer`
5. Crear usuarios de prueba con roles asignados
6. Obtener `jwks_uri` del realm para configurar en `application.properties`

### 4.3 ¿Por qué no hay integración con modelos de IA?

El código implementa **stubs** (InMemorySearchIndexer, StructuredExtractionService regex) en lugar de integraciones reales con OpenAI/Qdrant. Las razones son:

1. **No hay dependencias Maven** para LangChain4j ni cliente OpenAI en `pom.xml`
2. **No hay implementación real de `SearchIndexer`** que use Qdrant. Solo existe `InMemorySearchIndexer`.
3. **`StructuredExtractionService` es 100% regex**, sin llamadas HTTP a ningún modelo
4. **No hay capa de embedding**: no se genera ningún vector, no se llama a `text-embedding-3-large`
5. **Qdrant tiene 0 colecciones** — confirmando que nunca se ha indexado nada

---

## 5. Lo Que Realmente Funciona

| Componente | Estado | Descripción |
|---|---|---|
| **Health Checks** | 🟢 Funcional | `/q/health`, `/q/health/live`, `/q/health/ready` responden 200 |
| **OpenAPI Spec** | 🟢 Funcional | Documentación de API expuesta en `/q/openapi` |
| **PostgreSQL** | 🟢 Funcional | Conexión OK, migración Flyway aplicada, tablas creadas |
| **Qdrant Engine** | 🟢 Funcional | Servicio corriendo, health OK. **Pero vacío** (sin colecciones) |
| **Keycloak Engine** | 🟢 Funcional | Servicio corriendo en `localhost:8443`. **Pero sin realm configurado** |
| **Backend JVM** | 🟢 Funcional | Quarkus arranca en 5.2s, escucha en `:8080` |

## 6. Lo Que NO Funciona y Por Qué

| Componente | Estado | Causa |
|---|---|---|
| **Endpoints de negocio** | 🔴 No funcional | OIDC deshabilitado en build → 401/500 en todos los endpoints |
| **Autenticación JWT** | 🔴 No funcional | Realm `abax-memory` no existe en Keycloak (404) |
| **Búsqueda semántica** | 🔴 No funcional | `InMemorySearchIndexer` es token matching, no embeddings. Qdrant vacío. |
| **Extracción estructurada** | 🔴 No funcional con IA | `StructuredExtractionService` es regex, no LLM. Sin API key de OpenAI. |
| **Indexación en Qdrant** | 🔴 Nunca ejecutada | No hay implementación real de `SearchIndexer` que hable con Qdrant |
| **Despliegue contenerizado** | 🔴 Inexistente | 0 Dockerfiles. 0 imágenes construidas. |
| **Orquestación K8s** | 🔴 Inexistente | Kubernetes no instalado. 0 manifests. |
| **Pipeline CI/CD** | 🔴 Inexistente | No hay pipeline configurado (GitHub Actions, Jenkins, etc.) |
| **Git tag de release** | 🔴 Inexistente | `v1.0.0-release` no existe |

---

## 7. Lo Que Falta Configurar Para Que El Sistema Sea Funcional

### Bloque 1: Autenticación (Crítico — Bloquea todo)

1. Crear realm `abax-memory` en Keycloak (admin console `localhost:8443/admin`)
2. Crear client `abax-memory-api` con:
   - Access Type: `confidential`
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Service Account Enabled: `true`
3. Crear roles: `memory-operator`, `memory-reviewer`, `memory-admin`, `memory-auditor`, `api-consumer`
4. Crear usuarios de prueba con roles
5. Recompilar backend con `quarkus.oidc.enabled=true` y config OIDC correcta:
   ```properties
   quarkus.oidc.auth-server-url=http://localhost:8443/realms/abax-memory
   quarkus.oidc.client-id=abax-memory-api
   quarkus.oidc.credentials.secret=<client-secret>
   ```

### Bloque 2: Modelos de IA (Crítico — Sin esto no hay valor de negocio)

1. Agregar dependencia LangChain4j OpenAI al `pom.xml`
2. Configurar API key de OpenAI (variable de entorno o K8s Secret)
3. Implementar `QdrantSearchIndexer` que:
   - Genere embeddings con `text-embedding-3-large`
   - Indexe vectores en Qdrant (crear colección)
   - Busque por similitud coseno real
4. Reemplazar `StructuredExtractionService` regex por llamada a LLM con structured outputs
5. Configurar modelo de validación crítica

### Bloque 3: Contenedorización y CI/CD

1. Crear `Dockerfile` multi-stage (Maven build + distroless runtime)
2. Crear `docker-compose.yml` para desarrollo local con todos los servicios
3. Configurar pipeline CI/CD (GitHub Actions)
4. Construir y publicar imágenes Docker

### Bloque 4: K8s y Producción

1. Instalar/verificar cluster Kubernetes
2. Crear manifests (Deployment, Service, ConfigMap, Secret, Ingress)
3. Crear namespace `abax-memory-prod`
4. Cargar secretos (DB creds, OpenAI API key, Keycloak secret)

---

## 8. Veredicto Final

**El sistema NO está listo para despliegue productivo.** El gate F7 fue marcado como "APROBADA CON CONDICIONES" pero la verificación real muestra que solo 2 de 10 condiciones se cumplen, y las 2 cumplidas (PostgreSQL, Qdrant) son insuficientes sin el resto.

El cuestionamiento del usuario es **totalmente válido y preciso**: no hay API keys configuradas porque el código jamás implementó integración real con modelos de IA. Las "pruebas" que pasaron en fases anteriores (UAT con 61/61 CA) se ejecutaron contra stubs in-memory que simulan embeddings con token matching y extracción con regex — no contra el sistema real.

**Recomendación**: No proceder con la ventana de despliegue del 2026-05-04. Priorizar:
1. Implementación real de integración con OpenAI (embeddings + structured extraction)
2. Configuración completa de OIDC/Keycloak
3. Dockerfiles y pipeline CI/CD
4. K8s manifests

---

## 9. Evidencia Adjunta

### 9.1 Qdrant — Sin Colecciones

```json
// GET http://localhost:6333/collections
{"result":{"collections":[]},"status":"ok","time":0.000142164}
```

### 9.2 Keycloak — Realm Inexistente

```json
// GET http://localhost:8443/realms/abax-memory
{"error":"Realm does not exist"}
```

### 9.3 Git Tags

```bash
$ git tag -l "v1.0.0*"
# (sin salida — cero tags de release)
```

### 9.4 Dockerfiles en el Repo

```bash
$ find /root/proyectos-personales/Abax-Memory -name "Dockerfile*" -type f
# (sin salida — cero archivos)
```

### 9.5 API Keys en Variables de Entorno

```bash
$ env | grep -iE "openai|api_key|embedding"
# (sin salida — ninguna variable configurada)
```

### 9.6 Dependencias de IA en pom.xml

```bash
$ grep -iE "langchain|openai|embedding" backend-quarkus/pom.xml
# (sin salida — cero dependencias de IA)
```

---

*Documento generado por devops con verificación de comandos reales. 2026-05-02.*
*Todas las afirmaciones están respaldadas por evidencia ejecutable reproducible.*
