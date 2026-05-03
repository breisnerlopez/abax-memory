# Setup del Proyecto — Abax-Memory v2.0.0

- **Versión**: 2.0.0
- **Stack**: Quarkus 3.15.3 + PostgreSQL 16 + Qdrant v1.17.1 + Keycloak 26.1
- **Última actualización**: 2026-05-03
- **Responsable**: DevOps / Release Engineer

---

## Prerrequisitos

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| Java (OpenJDK) | 21+ | `java -version` |
| Maven | 3.9+ (3.8.7 funcional) | `mvn --version` |
| Docker | 24+ | `docker --version` |
| Docker Compose | 2.x (incluido en Docker) | `docker compose version` |
| Git | 2.40+ | `git --version` |
| Node.js (opcional, tooling) | 20+ | `node --version` |

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
export OPENAI_API_KEY="sk-tu-api-key-aqui"
docker compose up -d
```

### Paso 5 — Ejecutar la Aplicacion

```bash
# Con Docker (recomendado para evaluacion rapida)
export OPENAI_API_KEY="sk-..."
docker compose up -d

# Verificar que todo esta funcionando
curl http://localhost:8080/q/health          # Backend
curl http://localhost:6333/healthz            # Qdrant
curl http://localhost:8443/realms/abax-memory # Keycloak

# Modo desarrollo Quarkus (hot reload)
cd backend-quarkus
export OPENAI_API_KEY="sk-..."
export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/pmoadb"
export QUARKUS_DATASOURCE_USERNAME="pmoa"
export QUARKUS_DATASOURCE_PASSWORD="pmoa"
export ABAX_QDRANT_HOST="localhost"
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
| `ABAX_QDRANT_HOST` | `localhost` | Servidor Qdrant |
| `ABAX_QDRANT_PORT` | `6333` | Puerto REST de Qdrant |
| `ABAX_QDRANT_COLLECTION` | `abax-memories` | Coleccion de vectores |
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

## Glosario

- **Quarkus**: Framework Java nativo para Kubernetes optimizado para arranque rapido y bajo consumo de memoria.
- **Qdrant**: Base de datos vectorial para busqueda semantica con embeddings de IA.
- **Keycloak**: Identity provider open-source que implementa OIDC/OAuth 2.0 para autenticacion y autorizacion.
- **Flyway**: Herramienta de migracion de bases de datos versionada (cambios de esquema SQL controlados).
- **OIDC**: OpenID Connect — protocolo de autenticacion sobre OAuth 2.0.
