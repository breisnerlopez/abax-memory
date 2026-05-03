# Publicacion de Imagen Docker en GHCR y GitHub Release
- **Fase**: 7 — Despliegue
- **Responsable**: devops
- **Fecha**: 2026-05-02 (actualizacion 2026-05-03)
- **Estado**: Completado ✅

---

## 1. Resumen Ejecutivo

Se completo la publicacion del proyecto Abax-Memory v1.0.0-MVP como GitHub Release con todos los artefactos necesarios para despliegue. La imagen Docker fue construida exitosamente de forma local, tagueada para GHCR y **publicada exitosamente** con un nuevo token Classic PAT con scope `write:packages`. La incidencia INC-PUB-001 queda **resuelta y cerrada**.

---

## 2. URLs de Publicacion

| Recurso | URL |
|---|---|
| **GitHub Release v1.0.0** | https://github.com/breisnerlopez/abax-memory/releases/tag/v1.0.0 |
| **GHCR Package (Web UI)** | https://github.com/users/breisnerlopez/packages/container/package/abax-memory |
| **Imagen Docker (GHCR) — latest** | `ghcr.io/breisnerlopez/abax-memory:latest` |
| **Imagen Docker (GHCR) — v1.0.0** | `ghcr.io/breisnerlopez/abax-memory:v1.0.0` |
| **Repositorio** | https://github.com/breisnerlopez/abax-memory |

---

## 3. Artefactos del Release

| Archivo | Descripcion | Tamanio |
|---|---|---|
| `abax-memory-backend-1.0.0-SNAPSHOT-runner.jar` | JAR ejecutable Quarkus (Java 21) | 55 MB |
| `docker-compose.yml` | Stack completo: backend + PostgreSQL + Qdrant + Keycloak | 4.7 KB |
| `Dockerfile` | Build de imagen Docker con eclipse-temurin:21-jre | 1.2 KB |
| `CHANGELOG.md` | Registro de cambios v1.0.0 | 2.0 KB |

---

## 4. Imagen Docker — GHCR (Push Exitoso)

### 4.1 Estado del push

| Indicador | Estado |
|---|---|
| Build local | ✅ Exitoso |
| Tags aplicados | `ghcr.io/breisnerlopez/abax-memory:latest`, `ghcr.io/breisnerlopez/abax-memory:v1.0.0` |
| Login GHCR (nuevo token) | ✅ Exitoso (`docker login ghcr.io`) |
| Push `:latest` a GHCR | ✅ Exitoso |
| Push `:v1.0.0` a GHCR | ✅ Exitoso |
| Verificacion de pull | ✅ Imagen pullable desde GHCR |

### 4.2 Digest de la imagen

| Tag | Digest |
|---|---|
| `:latest` | `sha256:57698c7bbc92cc3ca5389e1903f18ddc4ae12d73dda6185b2fe96c4f5d6de0f0` |
| `:v1.0.0` | `sha256:57698c7bbc92cc3ca5389e1903f18ddc4ae12d73dda6185b2fe96c4f5d6de0f0` |

Ambos tags apuntan al mismo digest (misma imagen).

### 4.3 Visibilidad del paquete

| Propiedad | Valor |
|---|---|
| **Visibilidad** | `private` |
| **Motivo** | El repositorio `breisnerlopez/abax-memory` es privado. GHCR hereda la visibilidad del repositorio vinculado. |
| **Acceso** | Solo usuarios autenticados con token GitHub con scope `read:packages` pueden hacer `docker pull`. |
| **Version count** | 1 (ambos tags son la misma version de paquete) |
| **Package ID** | `11892827` |
| **Creado** | 2026-05-03T01:41:14Z |

> ⚠️ **Nota**: Para hacer la imagen publica, es necesario cambiar la visibilidad del repositorio a publico, o cambiar manualmente la visibilidad del paquete en **Settings > Packages** del repositorio en GitHub.

### 4.4 Comando para pull desde cualquier entorno

```bash
# Login con token GitHub que tenga read:packages
echo "TU_TOKEN_GITHUB" | docker login ghcr.io -u TU_USUARIO --password-stdin

# Pull de la imagen
docker pull ghcr.io/breisnerlopez/abax-memory:latest
# o version especifica:
docker pull ghcr.io/breisnerlopez/abax-memory:v1.0.0
```

### 4.5 Detalles de la imagen

| Propiedad | Valor |
|---|---|
| Imagen base | `eclipse-temurin:21-jre` |
| Puerto expuesto | 8080 |
| Workdir | `/app` |
| Entrypoint | `java -jar app.jar` |
| Healthcheck | `http://localhost:8080/q/health/live` |
| Variable requerida | `OPENAI_API_KEY` (sin valor default) |
| Labels OCI | `org.opencontainers.image.*` |
| Tamanio comprimido | 7 capas, ~1786 bytes manifest |

### 4.6 Capas de la imagen

```
f6e6efcf5e78  — Aplicacion y configuracion
81350493f61d  — Dependencias Java
9a376521bf1b  — Runtime
2cce48cf6a50  — Sistema base
845dac72d2b2  — Librerias del sistema
fc9116e853d5  — Configuracion de sistema
538812a4b9bd  — Capa base eclipse-temurin
```

---

## 5. Instrucciones de Despliegue con Docker Compose

### 5.1 Prerequisitos

- Docker Engine 24+ con Docker Compose v2
- API Key de OpenAI (`OPENAI_API_KEY`)
- Puertos disponibles: 8080, 5432, 6333, 6334, 8443

### 5.2 Despliegue rapido

```bash
# 1. Descargar docker-compose.yml del release
wget https://github.com/breisnerlopez/abax-memory/releases/download/v1.0.0/docker-compose.yml

# 2. Configurar API key de OpenAI (NUNCA hardcodear en el archivo)
export OPENAI_API_KEY="sk-proj-..."

# 3. Iniciar el stack completo
docker compose up -d

# 4. Verificar salud de los servicios
curl http://localhost:8080/q/health
curl http://localhost:6333/healthz
curl http://localhost:8443/realms/abax-memory
```

### 5.3 Usar la imagen de GHCR en docker-compose

En lugar del build local, se puede modificar `docker-compose.yml` para usar la imagen de GHCR:

```yaml
services:
  abax-memory:
    image: ghcr.io/breisnerlopez/abax-memory:latest
    # Eliminar la seccion 'build:' si existe
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    # ... resto de configuracion
```

### 5.4 Detener el stack

```bash
# Detener sin perder datos
docker compose down

# Detener y eliminar volumenes (datos persistentes)
docker compose down -v
```

---

## 6. Variables de Entorno Requeridas

### 6.1 Obligatorias

| Variable | Descripcion | Como configurarla |
|---|---|---|
| `OPENAI_API_KEY` | API Key de OpenAI para embeddings, extraccion y validacion | `export OPENAI_API_KEY="sk-..."` o en archivo `.env` |

### 6.2 Opcionales (con defaults en docker-compose)

| Variable | Default | Descripcion |
|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | `jdbc:postgresql://postgres:5432/pmoadb` | URL de conexion a PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | `pmoa` | Usuario de base de datos |
| `QUARKUS_DATASOURCE_PASSWORD` | `pmoa` | Password de base de datos |
| `ABAX_QDRANT_HOST` | `qdrant` | Hostname de Qdrant |
| `ABAX_QDRANT_PORT` | `6333` | Puerto REST de Qdrant |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `http://keycloak:8080/realms/abax-memory` | URL de Keycloak |
| `QUARKUS_OIDC_CLIENT_ID` | `abax-memory-api` | Client ID OIDC |
| `ABAX_OPENAI_VALIDATION_MODEL` | `gpt-4o` | Modelo para validacion semantica |

### 6.3 Seguridad

> ⚠️ **IMPORTANTE**: La variable `OPENAI_API_KEY` se configura exclusivamente via variable de entorno. **NUNCA** debe hardcodearse en:
> - `docker-compose.yml`
> - `Dockerfile`
> - Codigo fuente
> - Archivos de configuracion
> - Logs o salidas de consola
>
> Se recomienda rotar la API key despues del desarrollo y antes de cualquier exposicion publica.

---

## 7. Arquitectura del docker-compose.yml

```
┌──────────────────────────────────────────────────┐
│                  abax-net (bridge)                │
│                                                   │
│  ┌──────────┐  ┌──────────┐  ┌────────────────┐  │
│  │ postgres │  │  qdrant  │  │    keycloak    │  │
│  │  :5432   │  │  :6333   │  │  :8080→8443    │  │
│  └────┬─────┘  └────┬─────┘  └───────┬────────┘  │
│       │              │                │           │
│       └──────────────┼────────────────┘           │
│                      │                            │
│              ┌───────┴────────┐                   │
│              │  abax-memory   │                   │
│              │   :8080        │                   │
│              └────────────────┘                   │
│                                                   │
└──────────────────────────────────────────────────┘
```

### Health checks configurados

| Servicio | Healthcheck | Intervalo |
|---|---|---|
| PostgreSQL | `pg_isready -U pmoa -d pmoadb` | 10s |
| Qdrant | `curl http://localhost:6333/healthz` | 10s |
| Keycloak | `curl http://localhost:8080/health/ready` | 30s |
| Abax-Memory | `curl http://localhost:8080/q/health/live` | 15s |

---

## 8. Verificacion Post-Publicacion

### 8.1 Release verificado

| Verificacion | Estado |
|---|---|
| Release `v1.0.0` creado | ✅ https://github.com/breisnerlopez/abax-memory/releases/tag/v1.0.0 |
| JAR adjunto (55 MB) | ✅ `abax-memory-backend-1.0.0-SNAPSHOT-runner.jar` |
| docker-compose.yml adjunto | ✅ 4,676 bytes |
| Dockerfile adjunto | ✅ 1,197 bytes |
| CHANGELOG.md adjunto | ✅ 2,044 bytes |
| Tag `v1.0.0` en Git | ✅ Apunta a commit `80d66c8` |

### 8.2 Imagen Docker — GHCR

| Verificacion | Estado |
|---|---|
| Build local exitoso | ✅ `docker build` completado sin errores |
| Tags aplicados | ✅ `latest` y `v1.0.0` |
| Imagen base | ✅ `eclipse-temurin:21-jre` (oficial) |
| Sin secretos en capas | ✅ `OPENAI_API_KEY=""` (placeholder, sin valor real) |
| Login GHCR | ✅ Nuevo token Classic PAT con `write:packages` |
| Push `:latest` a GHCR | ✅ Completado — digest `sha256:57698c...` |
| Push `:v1.0.0` a GHCR | ✅ Completado — mismo digest |
| Pull desde GHCR | ✅ Verificado — imagen descargable |
| Visibilidad | ⚠️ `private` (repositorio privado) |

---

## 9. Incidencias Registradas

### INC-PUB-001 — Push a GHCR denegado por permisos de token (RESUELTA)

| Campo | Detalle |
|---|---|
| **ID** | INC-PUB-001 |
| **Severidad** | Media |
| **Fecha de apertura** | 2026-05-02 |
| **Fecha de cierre** | 2026-05-03 |
| **Componente** | GitHub Container Registry |
| **Sintoma** | `docker push` retornaba `permission_denied: The token provided does not match expected scopes.` con fine-grained PAT |
| **Causa raiz** | Fine-grained PAT sin permisos `packages:read` ni `packages:write` |
| **Resolucion** | Se creo un Classic PAT (token `ghp_...`) con scope `write:packages`. El push se completo exitosamente con ambas tags. |
| **Estado** | 🟢 **Cerrada — Resuelta** |
| **Evidencia** | `docker push ghcr.io/breisnerlopez/abax-memory:latest` y `:v1.0.0` completados sin errores. Verificado via `docker pull`. |

---

## 10. Conclusion

La publicacion del proyecto Abax-Memory v1.0.0-MVP se completo integralmente:

1. **GitHub Release v1.0.0** — Creado con los 4 artefactos principales (JAR, docker-compose.yml, Dockerfile, CHANGELOG.md).
2. **GHCR Container Package** — Imagen Docker construida, tagueada y publicada exitosamente con ambas tags (`latest` y `v1.0.0`).
3. **Incidencia INC-PUB-001** — Resuelta mediante la creacion de un Classic PAT con scope `write:packages`.
4. **Verificacion de pull** — Confirmada. La imagen es funcional y descargable desde GHCR.
5. **Visibilidad** — El paquete es privado (hereda la visibilidad del repositorio). Para acceso publico, cambiar visibilidad del repositorio o del paquete en GitHub Settings.

**URL final de GHCR**: `ghcr.io/breisnerlopez/abax-memory:latest`
**Web UI**: https://github.com/users/breisnerlopez/packages/container/package/abax-memory
