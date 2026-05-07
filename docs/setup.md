# Setup del Proyecto — Abax-Memory v2.1.0

- **Versión actual**: 2.1.0 (Hardening & Producción Real)
- **Stack**: Quarkus 3.15.3 + PostgreSQL 16 + Qdrant v1.17.1 + Keycloak 26.1
- **Última actualización**: 2026-05-06
- **Responsable**: DevOps / Release Engineer
- **Iteraciones**:
  - `v1.0.0`: Motor PMOA (IT ops) — 2026-04
  - `v2.0.0`: Motor Multi-Dominio — 2026-05-03, business-analyst
  - `v2.1.0`: Hardening & Producción Real — 2026-05-06, devops

---

## Prerrequisitos

| Herramienta | Versión mínima | Verificación | v2.1.0 |
|---|---|---|---|
| Java (OpenJDK) | 21+ | `java -version` | ✅ 21.0.10 |
| Maven | 3.9+ (3.8.7 funcional) | `mvn --version` | ⚠️ 3.8.7 |
| Docker | 24+ | `docker --version` | ✅ 29.4.2 |
| Docker Compose | 2.x (incluido en Docker) | `docker compose version` | ✅ v5.1.3 |
| Git | 2.40+ | `git --version` | ✅ 2.43.0 |
| Node.js (opcional, tooling) | 20+ | `node --version` | ✅ 22.22.0 |
| PostgreSQL Client (`psql`) | 16+ | `psql --version` | ✅ 16.13 |
| `curl` + `python3` | cualquier | `curl --version` | ✅ requeridos por verify-stack.sh |

---

## Opcion 1 — Devcontainer (Recomendado)

> **Nota**: El proyecto tiene configuracion en `.devcontainer/`. Esta es la forma mas rapida de obtener un entorno reproducible.

1. Instala [Docker Desktop](https://www.docker.com/products/docker-desktop/) (o equivalente: Rancher Desktop, Podman).
2. Abre el proyecto en [VS Code](https://code.visualstudio.com/).
3. Instala la extension **Dev Containers** (`ms-vscode-remote.remote-containers`).
4. Cuando aparezca **"Reopen in Container?"**, acepta.
5. Espera a que el container se construya (~5min la primera vez).
6. El container incluira Java 21, Maven 3.9+, Docker CLI, y todas las herramientas necesarias.

---

## Opcion 2 — Host (sin Devcontainer)

### Paso 1 — Instalar Runtimes

#### Java 21 (via sdkman, recomendado)

```bash
# Instalar sdkman (gestor de versiones, no requiere sudo)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Instalar Java 21
sdk install java 21.0.10-tem

# Verificar
java -version
# Debe mostrar: OpenJDK 21.0.10
```

#### Maven 3.9+ (via sdkman)

```bash
sdk install maven 3.9.9

# Verificar
mvn --version
# Debe mostrar: Apache Maven 3.9.x
```

#### Docker (via repositorio oficial)

```bash
# Ubuntu / Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Cerrar sesion y volver a abrir para que el grupo docker tenga efecto
docker --version
# Debe mostrar: Docker version 24+
```

### Paso 2 — Clonar el Repositorio

```bash
git clone https://github.com/breisnerlopez/abax-memory.git
cd abax-memory

# Cambiar a la rama de desarrollo
git checkout abax/abax-memory
```

### Paso 3 — Verificar Build

```bash
# Compilar el backend (sin tests)
cd backend-quarkus
mvn compile

# Salida esperada: BUILD SUCCESS
```

### Paso 4 — Levantar Servicios de Infraestructura

```bash
# Volver a la raiz del proyecto
cd ..

# Opcion A: Solo infraestructura (para desarrollo con hot reload)
docker compose up -d postgres qdrant keycloak

# Opcion B: Stack completo (backend incluido)
# Cargar API key desde .env (recomendado)
export $(cat .env | xargs)
docker compose up -d
```

### Paso 5 — Ejecutar la Aplicacion

```bash
# Con Docker (recomendado para evaluacion rapida)
# Cargar API key desde .env
export $(cat .env | xargs)
docker compose up -d

# Verificar que todo esta funcionando
curl http://localhost:8080/q/health          # Backend
curl http://localhost:6333/healthz            # Qdrant
curl http://localhost:8443/realms/abax-memory # Keycloak

# Modo desarrollo Quarkus (hot reload)
cd backend-quarkus
# Cargar API key desde .env (raiz del proyecto)
export $(cat ../.env | xargs)
export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/pmoadb"
export QUARKUS_DATASOURCE_USERNAME="pmoa"
export QUARKUS_DATASOURCE_PASSWORD="pmoa"
export ABAX_V2_QDRANT_HOST="localhost"
mvn quarkus:dev
```

### Paso 6 — Ejecutar Tests

```bash
cd backend-quarkus

# Tests unitarios (usan H2 en memoria, no requieren Docker)
mvn test

# Para tests de integracion (requieren Docker):
# (requiere agregar Testcontainers al pom.xml primero)
mvn test -Dtest="*IT"
```

---

## Verificacion

Si todo esta bien, deberias poder:

1. ✅ **Build exitoso**: `mvn compile` → `BUILD SUCCESS`.
2. ✅ **API responde**: `curl http://localhost:8080/q/health` → `{"status": "UP"}`.
3. ✅ **Qdrant responde**: `curl http://localhost:6333/healthz` → `{"title":"...","version":"1.17.1"}`.
4. ✅ **Keycloak responde**: `curl http://localhost:8443/realms/abax-memory` → JSON con configuracion del realm.
5. ✅ **Swagger UI**: Abrir `http://localhost:8080/q/swagger-ui` en el navegador.

---

## Verificación del Stack

Para una verificación rápida de todos los servicios de infraestructura, ejecuta:

```bash
./scripts/verify-stack.sh
```

### Salida esperada (stack saludable)

```
══════════════════════════════════════════════════════════
  Abax-Memory v2.1.0 — Stack Verification
  2026-05-04 HH:MM:SS
══════════════════════════════════════════════════════════

  [1/4] PostgreSQL (localhost:5432) ... UP
  [2/4] Qdrant (localhost:6333) ...... UP (v1.17.1)
  [3/4] Keycloak (localhost:8443) .... UP (realm: abax-memory)
  [4/4] OpenAI API Key ............... SET (164 chars)

──────────────────────────────────────────────────────────
  Resultado: 4/4 servicios UP  ✓ STACK HEALTHY
══════════════════════════════════════════════════════════
```

### Resultados posibles

| Servicios UP | Estado | Exit Code | Acción |
|---|---|---|---|
| 4/4 | `✓ STACK HEALTHY` | 0 | Listo para desarrollar |
| 3/4 | `⚠ STACK DEGRADED` | 1 | Revisar el servicio caído |
| ≤2/4 | `✗ STACK DOWN` | 1 | Levantar infraestructura con `docker compose up -d` |

> **Nota sobre OpenAI**: si la API key no está configurada, el backend usará `InMemoryEmbeddingProvider` (solo para desarrollo/tests, NO para producción). Esto permite trabajar sin costo de API durante el desarrollo.

### Troubleshooting común

**PostgreSQL DOWN**:
```bash
docker compose up -d postgres
docker compose ps postgres  # debe mostrar "healthy"
```

**Qdrant DOWN**:
```bash
docker compose up -d qdrant
curl http://localhost:6333/healthz  # debe mostrar "healthz check passed"
```

**Keycloak DOWN**:
```bash
docker compose up -d keycloak
# Keycloak puede tardar 30-60s en iniciar. Espera y reintenta:
sleep 30 && curl -s http://localhost:8443/realms/abax-memory | head -c 50
```

**OpenAI API Key no configurada**:
```bash
# Opcion A — Cargar desde .env (recomendado)
export $(cat .env | xargs)

# Opcion B — Exportar directamente
export OPENAI_API_KEY="sk-proj-..."

# Verificar longitud (debe ser ~164 caracteres):
echo ${#OPENAI_API_KEY}
```

**Puertos ocupados**:
```bash
sudo lsof -i :5432  # PostgreSQL
sudo lsof -i :6333  # Qdrant
sudo lsof -i :8443  # Keycloak
sudo lsof -i :8080  # Backend Quarkus
```

---

## Credenciales de Desarrollo

Esta seccion describe como configurar las credenciales necesarias en entorno local de desarrollo. **Ninguna de estas credenciales debe usarse en produccion.**

### OPENAI_API_KEY

La API Key de OpenAI es la unica variable **obligatoria** para que el backend funcione
(validacion de requerimientos con IA y generacion de embeddings).

```bash
export OPENAI_API_KEY="sk-proj-..."
```

> **Donde obtenerla**: [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
> **Formato esperado**: `sk-proj-...` (164 caracteres). Keys que comienzan con `sk-svcacct-`
> tambien son validas si son service accounts del proyecto.

**Verificacion**:
```bash
echo "Key length: ${#OPENAI_API_KEY}"               # Debe ser ~164
curl -s https://api.openai.com/v1/models \
  -H "Authorization: Bearer ${OPENAI_API_KEY}" | \
  python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']), 'models')"
```

#### Persistencia de la API key (`.env`)

Para evitar perder la API key al reiniciar sesion, guardala en un archivo `.env` en la raiz
del proyecto (ya esta en `.gitignore` para que nunca se commitee):

**Crear `.env`**:
```bash
echo 'OPENAI_API_KEY=sk-proj-...' > .env
```

**Cargar las variables antes de iniciar el backend**:
```bash
# Opcion A — exportar todas las variables del .env
export $(cat .env | xargs)

# Opcion B — cargar variables en el shell actual (bash/zsh)
set -a && source .env && set +a
```

**Verificar que la key se cargo**:
```bash
echo "Key length: ${#OPENAI_API_KEY}"
# Debe mostrar: 164
```

> **Nota**: El script de verificacion `scripts/verify-stack.sh` detecta automaticamente
> si `OPENAI_API_KEY` esta configurada en el entorno.

### Qdrant (Base de Datos Vectorial)

Qdrant almacena los embeddings semánticos de los requerimientos y permite búsqueda por similitud.

**Levantar con Docker**:
```bash
docker run -d --name qdrant \
  -p 6333:6333 -p 6334:6334 \
  qdrant/qdrant:v1.17.1
```

**Levantar con Docker Compose** (recomendado, desde la raiz del proyecto):
```bash
docker compose up -d qdrant
```

**Verificacion**:
```bash
curl -s http://localhost:6333/          # Debe mostrar JSON con version 1.17.1
curl -s http://localhost:6333/healthz   # Debe mostrar "healthz check passed"
```

### Keycloak (Identity Provider)

Keycloak gestiona autenticacion y autorizacion via OIDC/OAuth 2.0 para el backend y los usuarios.

> **⚠️ v2.1.0 — Realm debe crearse manualmente**: El contenedor Keycloak arranca con el realm `master` por defecto. El realm `abax-memory` y su cliente OIDC `abax-memory-api` **no se crean automáticamente**. Debes crearlos antes de iniciar el backend:
> 1. Accede a `http://localhost:8443/admin` (credenciales `admin`/`admin`)
> 2. Crea el realm `abax-memory`
> 3. Crea el client `abax-memory-api` (confidential, OIDC)
> 4. Configura `Client secret`: `ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI`
> 5. Verifica con: `curl -s http://localhost:8443/realms/abax-memory/.well-known/openid-configuration | python3 -c "import sys,json; print(json.load(sys.stdin)['issuer'])"`

**Levantar con Docker**:
```bash
docker run -d --name keycloak \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -p 8443:8080 \
  quay.io/keycloak/keycloak:26.1.0 start-dev
```

**Levantar con Docker Compose** (recomendado, desde la raiz del proyecto):
```bash
docker compose up -d keycloak
```

> **Nota**: Keycloak en modo `start-dev` puede tardar **30-60s** en iniciar completamente.
> Durante ese tiempo, el endpoint `/` devolvera HTTP 503 o Connection Refused. Espera y reintenta.

**Verificacion**:
```bash
# Verificar que responde (puede devolver HTTP 302 - redirect, eso es correcto)
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8443/

# Verificar el realm de Abax-Memory
curl -s http://localhost:8443/realms/abax-memory | python3 -m json.tool
```

**Credenciales de administrador (solo desarrollo)**:
| Campo | Valor |
|---|---|
| URL Admin Console | [http://localhost:8443/admin](http://localhost:8443/admin) |
| Usuario | `admin` |
| Contraseña | `admin` |
| Realm | `abax-memory` |
| Client ID (API) | `abax-memory-api` |

### PostgreSQL

Aunque la mayoria de los tests unitarios usan H2 en memoria, el entorno completo requiere PostgreSQL.

**Levantar con Docker Compose**:
```bash
docker compose up -d postgres
```

**Verificacion**:
```bash
docker compose ps postgres  # Debe mostrar estado "healthy"
```

**Credenciales de desarrollo**:
| Campo | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `pmoadb` |
| Usuario | `pmoa` |
| Contraseña | `pmoa` |

---

## Variables de Entorno

### Obligatorias

| Variable | Proposito | Ejemplo |
|---|---|---|
| `OPENAI_API_KEY` | Clave de API de OpenAI (nunca hardcodear) | `sk-proj-abc123...` |

### Opcionales (tienen defaults para desarrollo local)

| Variable | Default | Proposito |
|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | `jdbc:postgresql://localhost:5432/pmoadb` | Conexion a PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | `pmoa` | Usuario de base de datos |
| `QUARKUS_DATASOURCE_PASSWORD` | `pmoa` | Contrasena de base de datos |
| `ABAX_V2_QDRANT_HOST` | `localhost` | Servidor Qdrant |
| `ABAX_V2_QDRANT_PORT` | `6333` | Puerto REST de Qdrant |
| `ABAX_V2_QDRANT_COLLECTION` | `abax-memories` | Coleccion de vectores |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `http://localhost:8443/realms/abax-memory` | Auth server URL |
| `QUARKUS_OIDC_CLIENT_ID` | `abax-memory-api` | Client ID OIDC |
| `ABAX_OPENAI_VALIDATION_MODEL` | `gpt-4o` | Modelo de validacion |

---

## Estructura de Directorios Relevante

```
abax-memory/
├── backend-quarkus/             # Codigo fuente del backend
│   ├── pom.xml                  # Dependencias Maven
│   ├── src/main/java/           # Codigo Java
│   │   └── com/btl/administrador/api/
│   │       ├── resource/        # Endpoints REST
│   │       ├── service/         # Logica de negocio
│   │       ├── domain/          # Entidades
│   │       ├── persistence/     # Repositorios (Postgres + InMemory)
│   │       └── integration/     # Qdrant, OpenAI, Git
│   ├── src/main/resources/
│   │   ├── application.properties  # Configuracion
│   │   └── db/migration/           # Migraciones Flyway
│   └── src/test/                # Tests
├── docker-compose.yml           # Stack de desarrollo local
├── Dockerfile                   # Imagen de produccion
├── docs/                        # Documentacion
│   ├── setup.md                 # Este archivo
│   └── entregables/             # Entregables por fase
└── .github/                     # CI/CD workflows
```

---

## JWT Caching (v2.1.0 Feature FT-V21-002.3)

### Comportamiento

El backend cachea la validacion de tokens JWT en memoria (Caffeine) para reducir la latencia de autenticacion en cada request. Un token validado exitosamente se almacena con TTL igual al campo `exp` del JWT (tipicamente 1 hora). Requests subsecuentes con el mismo token se validan contra el cache local sin llamar a Keycloak:

1. **Primer request**: cache miss → validacion contra Keycloak (50-200ms)
2. **Requests 2-100+**: cache hit → validacion local ≤ 5ms

### Configuracion

| Propiedad | Default | Descripcion |
|---|---|---|
| `abax.v2.jwt-cache.enabled` | `true` | Habilita el cache JWT |
| `abax.v2.jwt-cache.max-size` | `10000` | Maximo de tokens en cache |
| `abax.v2.jwt-cache.admin-events-enabled` | `true` | Polling de eventos Keycloak para invalidacion |
| `abax.v2.jwt-cache.admin-events-poll-interval` | `5s` | Intervalo de polling de eventos de revocacion |

### Invalidacion

Ante eventos de revocacion en Keycloak (logout, cambio de roles), la entrada correspondiente se invalida en ≤ 5s (via polling de Keycloak Admin Events). Si Keycloak Admin Events no esta disponible, la invalidacion depende del TTL del JWT (ventana maxima de hasta 1 hora).

### Cacheo del lado del cliente (recomendado para consumidores de API)

Los consumidores de la API deben reutilizar el mismo JWT durante su TTL:

```
Authorization: Bearer <mismo-token>
```

Para obtener un JWT con TTL correcto:
```bash
curl -X POST http://localhost:8443/realms/abax-memory/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=abax-memory-api" \
  -d "client_secret=<secret>"
```

La respuesta incluye:
- `access_token`: el JWT (cachear este valor)
- `expires_in`: TTL en segundos (ej. 3600 = 1 hora)
- `refresh_token`: para renovar sin re-autenticar

### Resiliencia ante caidas de Keycloak

Si Keycloak esta inaccesible:
- **Tokens cacheados NO expirados**: se aceptan normalmente (cache hit)
- **Tokens NO cacheados**: HTTP 503 `"Authentication service unavailable"`

Esto proporciona resiliencia ante caidas temporales de Keycloak para clientes ya autenticados.

---

## Solucion de Problemas

### Error: `Cannot connect to PostgreSQL`

```bash
# Verificar que PostgreSQL esta corriendo
docker compose ps postgres
# Debe mostrar estado "healthy"

# Si no esta corriendo, levantarlo
docker compose up -d postgres
```

### Error: `QDRANT_INIT_FAILED` o `Connection refused` a Qdrant

```bash
# Verificar que Qdrant esta corriendo
curl http://localhost:6333/healthz

# Si falla, levantar Qdrant
docker compose up -d qdrant
```

### Error: `OPENAI_API_KEY` no configurada

```bash
# Asegurate de exportar la variable antes de levantar el backend
export OPENAI_API_KEY="sk-..."
docker compose up -d abax-memory
```

### Error: Maven `BUILD FAILURE` por version

Si ves errores relacionados con la version de Maven:

```bash
# Verificar version
mvn --version

# Si es < 3.9, instalar via sdkman
sdk install maven 3.9.9
```

### Error: Conflictos de merge en Git

Si estas en la rama `abax/abax-memory` y ves conflictos en `docs/bitacora.md` o `docs/registro-entregables.md`, no los resuelvas manualmente — escala al orquestador del proyecto.

### Error: Puertos ocupados (5432, 6333, 8080, 8443)

```bash
# Verificar que proceso ocupa el puerto
sudo lsof -i :5432
sudo lsof -i :6333
sudo lsof -i :8080
sudo lsof -i :8443

# Si es otra instancia de Docker, detenerla
docker compose down
```

---

## Cambios v2.1.0 — Hardening & Producción Real (2026-05-06)

### Nuevas Dependencias en `pom.xml`

Para la Fase 4 — Construcción de v2.1.0, el `pom.xml` requiere dependencias adicionales
que no estaban en v2.0.9:

| Artefacto | Propósito | Features que lo usan |
|---|---|---|
| `quarkus-cache` | Caché con Caffeine (`@CacheResult`, `@CacheInvalidate`) | FT-V21-002.1 (Cache de Grafo), FT-V21-002.3 (Cache JWT) |
| `caffeine` (opcional) | API directa de Caffeine para `GraphCacheServiceImpl` | FT-V21-002.1 |

Agregar al `pom.xml` antes de iniciar la construcción:

```xml
<!-- v2.1.0: Cache con Caffeine -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-cache</artifactId>
</dependency>
```

### Configuración de Keycloak

El realm `abax-memory` **debe crearse manualmente** en esta versión. Ver instrucciones
en la sección [Keycloak (Identity Provider)](#keycloak-identity-provider) arriba.

### Verificación de Stack

El script `scripts/verify-stack.sh` fue actualizado para v2.1.0. Ahora verifica 5
servicios (incluye conectividad a OpenAI API). Ejecutar antes de iniciar desarrollo:

```bash
export $(grep OPENAI_API_KEY .env | xargs)
./scripts/verify-stack.sh
```

### Qué se mantiene de v2.0.0

- Stack tecnológico idéntico: Quarkus 3.15.3, PostgreSQL 16, Qdrant 1.17.1, Keycloak 26.1.
- Docker Compose para infraestructura local.
- Flujo de desarrollo: `mvn quarkus:dev` con hot reload.
- Tests unitarios con H2 en memoria.
- `OPENAI_API_KEY` como única variable obligatoria.

### Qué se deprecia de v2.0.0

- `MockLlmService` reemplazado por OpenAI real en `POST /extract` (FT-V21-001.4).
- `InMemoryEmbeddingProvider` reemplazado por OpenAI `text-embedding-3-large` real.
- `expandGraph=true` como default — ahora es `false` (FT-V21-001.2).

### Versión del Artefacto

- `pom.xml` versión: `1.0.0-SNAPSHOT` → debe actualizarse a `2.1.0-SNAPSHOT`.
- Imagen Docker: `ghcr.io/breisnerlopez/abax-memory:v2.1.0` (a construir).

---

## Glosario

- **Quarkus**: Framework Java nativo para Kubernetes optimizado para arranque rapido y bajo consumo de memoria.
- **Qdrant**: Base de datos vectorial para busqueda semantica con embeddings de IA.
- **Keycloak**: Identity provider open-source que implementa OIDC/OAuth 2.0 para autenticacion y autorizacion.
- **Flyway**: Herramienta de migracion de bases de datos versionada (cambios de esquema SQL controlados).
- **OIDC**: OpenID Connect — protocolo de autenticacion sobre OAuth 2.0.
