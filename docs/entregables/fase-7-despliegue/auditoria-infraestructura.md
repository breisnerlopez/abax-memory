# Auditoría de Infraestructura para Despliegue
- **Fase**: 7 - Despliegue
- **Responsable**: DevOps Engineer (devops)
- **Fecha**: 2026-05-02
- **Estado**: Completado
---
## 1. Resumen Ejecutivo

Se realizó auditoría completa de la infraestructura del servidor productivo (`x86_64`, Ubuntu 24.04.4 LTS, 6 CPUs, 11 GB RAM, 115 GB disco disponible) para determinar la viabilidad del despliegue del stack MVP. Se verificaron todos los componentes requeridos mediante comandos ejecutados directamente en el servidor.

**Conclusión general**: El servidor tiene la mayoría de componentes base instalados. **Faltan Qdrant y Keycloak**, que son críticos para el MVP. Las imágenes Docker de OpenMemory ya existen localmente (`openmemory-openmemory:latest`, `openmemory-dashboard:latest`), lo que indica despliegues previos. No hay orquestador (Kubernetes) instalado; el despliegue operaría sobre Docker Compose directamente.

---

## 2. Matriz de Componentes del Stack MVP

| Componente | Estado | Versión | Detalles |
|---|---|---|---|
| **Java 21** | ✅ INSTALADO | OpenJDK 21.0.10 | `JAVA_HOME` no definido. GraalVM native-image NO instalado |
| **PostgreSQL** | ⚠️ PARCIAL | Cliente: psql 16.13 / Servidor: PostgreSQL 16.13 (Alpine, Docker) | Servidor corriendo en contenedor `aba-stage-db-1`. Base de datos `aba` existente. `pg_isready` falla en socket local (esperado, corre en Docker) |
| **Qdrant** | ❌ NO INSTALADO | — | Sin binario, sin imagen Docker, sin contenedor. Puerto 6333 libre |
| **Keycloak** | ❌ NO INSTALADO | — | Sin binario, sin imagen Docker, sin contenedor. Puerto 8443 libre |
| **Git** | ✅ INSTALADO | 2.43.0 | Operativo |
| **Maven** | ✅ INSTALADO | 3.8.7 | Compatible con Java 21 |
| **Docker** | ✅ INSTALADO | 29.4.2 | Docker Compose v2.27.1 incluido |
| **Node.js** | ✅ INSTALADO | v22.22.0 / npm 10.9.4 | Necesario para build del frontend Angular |
| **Nginx** | ✅ INSTALADO | 1.24.0 (nativo) | En uso en puertos 80 y 443 |
| **systemctl** | ✅ INSTALADO | systemd 255 | Operativo |
| **Kubernetes** | ❌ NO INSTALADO | — | Sin kubectl, k3s, ni minikube |
| **GraalVM native-image** | ❌ NO INSTALADO | — | No disponible. Backend debe ejecutarse como JVM (`java -jar`) o vía Docker |

---

## 3. Verificación Detallada por Componente

### 3.1 Java 21 — INSTALADO ✅

**Evidencia:**
```bash
$ java -version
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10+7-Ubuntu-124.04)
OpenJDK 64-Bit Server VM (build 21.0.10+7-Ubuntu-124.04, mixed mode, sharing)
EXIT_CODE=0

$ which java
/usr/lib/jvm/java-21-openjdk-amd64/bin/java
```

**Observaciones:**
- Java 21 LTS instalado y funcional.
- `JAVA_HOME` no está definido como variable de entorno global. **Acción requerida**: definir `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- GraalVM `native-image` NO está instalado. Esto descarta la compilación nativa en este servidor. El backend debe ejecutarse en modo JVM (`java -jar`) o dentro de contenedor Docker con una imagen precompilada.

**Comando para instalar GraalVM (si se requiere native-image):**
```bash
# Opcion A: Instalar via SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.6-graal
# Opcion B: Instalar native-image sobre JDK existente
sudo apt install gcc libz-dev -y  # dependencias
gu install native-image  # Si se usa GraalVM CE
```

---

### 3.2 PostgreSQL — PARCIAL ⚠️

**Evidencia:**
```bash
$ psql --version
psql (PostgreSQL) 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)

$ pg_isready
/var/run/postgresql:5432 - no response   # <-- Busca socket local, no Docker

$ docker exec aba-stage-db-1 psql -U postgres -c "SELECT version();"
PostgreSQL 16.13 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit

$ docker exec aba-stage-db-1 psql -U postgres -c "\l"
List of databases
   Name    |  Owner   | Encoding | ...
-----------+----------+----------+-----
 aba       | postgres | UTF8     | ...
 postgres  | postgres | UTF8     | ...
 template0 | postgres | UTF8     | ...
 template1 | postgres | UTF8     | ...
```

**Estado real:**
- Cliente `psql` instalado nativamente (v16.13).
- El servidor PostgreSQL no corre como servicio nativo de sistema.
- Dos instancias PostgreSQL corriendo en Docker:
  - `aba-stage-db-1` (postgres:16-alpine) → puerto `127.0.0.1:5432`, base de datos `aba` existente.
  - `polymarket-pg` (postgres:16-alpine) → puerto `127.0.0.1:5433`.
- Conexión funcional: se puede acceder a PostgreSQL vía `docker exec` o vía `psql -h 127.0.0.1 -p 5432 -U postgres`.

**Recomendación para el MVP:**
- Usar el contenedor `aba-stage-db-1` existente o crear un nuevo contenedor dedicado para producción.
- Crear base de datos específica para OpenMemory (ej: `openmemory_prod`).
- Si se requiere PostgreSQL nativo (sin Docker): `sudo apt install postgresql-16`.

---

### 3.3 Qdrant — NO INSTALADO ❌

**Evidencia:**
```bash
$ which qdrant
# No existe

$ docker ps -a | grep qdrant
# Sin resultados

$ docker images | grep qdrant
# Sin resultados

$ curl -s http://localhost:6333/health
# Sin respuesta (puerto libre)

$ ss -tlnp | grep 6333
PUERTO 6333: LIBRE
```

**Estado real:** Qdrant no existe en ninguna forma (binario, contenedor, imagen). El puerto 6333 está completamente libre.

**Comandos para instalar Qdrant (vía Docker — recomendado):**
```bash
# Pull de la imagen oficial
docker pull qdrant/qdrant:latest

# Ejecutar contenedor
docker run -d \
  --name qdrant-prod \
  --restart unless-stopped \
  -p 127.0.0.1:6333:6333 \
  -p 127.0.0.1:6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  qdrant/qdrant:latest

# Verificar salud
curl http://localhost:6333/health
```

**Opcional — docker-compose snippet:**
```yaml
qdrant:
  image: qdrant/qdrant:latest
  container_name: qdrant-prod
  restart: unless-stopped
  ports:
    - "127.0.0.1:6333:6333"
    - "127.0.0.1:6334:6334"
  volumes:
    - qdrant_storage:/qdrant/storage
```

---

### 3.4 Keycloak — NO INSTALADO ❌

**Evidencia:**
```bash
$ which keycloak
# No existe

$ docker ps -a | grep keycloak
# Sin resultados

$ docker images | grep keycloak
# Sin resultados

$ curl -s -o /dev/null -w "%{http_code}" http://localhost:8443/
000   # Sin respuesta

$ ss -tlnp | grep 8443
PUERTO 8443: LIBRE
```

**Estado real:** Keycloak no existe en ninguna forma. El puerto 8443 está libre.

**Comandos para instalar Keycloak (vía Docker — recomendado):**
```bash
# Pull de la imagen oficial
docker pull quay.io/keycloak/keycloak:26.0

# Ejecutar en modo desarrollo (para pruebas iniciales)
docker run -d \
  --name keycloak-prod \
  --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=<PASSWORD_SEGURO> \
  quay.io/keycloak/keycloak:26.0 \
  start-dev

# Para producción, usar start en lugar de start-dev con TLS configurado:
docker run -d \
  --name keycloak-prod \
  --restart unless-stopped \
  -p 127.0.0.1:8443:8443 \
  -v /opt/keycloak/certs:/etc/x509/https \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=<PASSWORD_SEGURO> \
  -e KC_HOSTNAME=<DOMINIO> \
  quay.io/keycloak/keycloak:26.0 \
  start --https-port=8443
```

**Nota importante sobre el puerto 8080:**
- Keycloak por defecto usa el puerto 8080 (mismo que Quarkus backend).
- Si ambos van a coexistir en el mismo host, se debe:
  - Mover Keycloak a otro puerto (ej: 8080→8090 mapeado internamente), o
  - Mover el backend Quarkus a otro puerto.
  - O usar Nginx como reverse proxy para enrutar por dominio.

---

### 3.5 Git — INSTALADO ✅

```bash
$ git --version
git version 2.43.0
```

Sin observaciones. Operativo.

---

### 3.6 Maven — INSTALADO ✅

```bash
$ mvn --version
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java version: 21.0.10, vendor: Ubuntu
OS name: "linux", version: "6.8.0-58-generic", arch: "amd64"
```

Sin observaciones. Compatible con Java 21.

---

### 3.7 Docker — INSTALADO ✅

```bash
$ docker --version
Docker version 29.4.2, build 055a478

$ docker compose version
Docker Compose version v2.27.1
```

**Evidencia adicional:**
- 12 contenedores ejecutándose actualmente (incluyendo `aba-stage-app-1`, `aba-stage-db-1` de la base de staging del proyecto).
- Imágenes Docker del proyecto ya presentes:
  - `openmemory-openmemory:latest` (857 MB) — backend
  - `openmemory-dashboard:latest` (1.46 GB) — frontend

**Observación:** Docker ya está siendo usado activamente para workloads de staging. La infraestructura de contenedorización está madura en este servidor.

---

### 3.8 Nginx — INSTALADO ✅

```bash
$ nginx -v
nginx version: nginx/1.24.0 (Ubuntu)
```

Nginx corre nativamente (no en Docker) y está sirviendo en puertos 80 y 443. Puede usarse como reverse proxy para el frontend Angular y el backend Quarkus.

---

### 3.9 Kubernetes — NO INSTALADO ❌

```bash
$ kubectl version --client   # KUBECTL_NOT_FOUND
$ k3s --version              # K3S_NOT_FOUND
$ minikube version           # MINIKUBE_NOT_FOUND
$ ls /etc/rancher/k3s/       # NO_K3S_CONFIG
```

**Impacto en el despliegue:** No se puede usar el plan de despliegue con K8s. El MVP debe desplegarse con Docker Compose directamente, lo cual es viable dado que el servidor ya opera así para staging. Si en el futuro se requiere K8s, se puede instalar k3s:

```bash
# Instalar k3s (Kubernetes ligero)
curl -sfL https://get.k3s.io | sh -
```

---

### 3.10 GraalVM native-image — NO INSTALADO ❌

```bash
$ native-image --version
NATIVE_IMAGE_NOT_FOUND
```

**Impacto:** No se puede compilar el backend Quarkus a binario nativo en este servidor. Se debe usar una de estas alternativas:
- **Opción A (recomendada)**: Ejecutar el JAR en modo JVM (`java -jar`), que ya funciona con Java 21 instalado.
- **Opción B**: Usar la imagen Docker preexistente `openmemory-openmemory:latest` (857 MB) que ya contiene el backend compilado.
- **Opción C**: Compilar la imagen nativa en CI/CD (otro servidor con GraalVM) y desplegar la imagen nativa aquí.

---

## 4. Disponibilidad de Puertos

| Puerto | Servicio Previsto | Estado | Ocupado por |
|---|---|---|---|
| 22 | SSH | OCUPADO | `sshd` |
| 53 | DNS interno | OCUPADO | `systemd-resolve` |
| 80 | HTTP / Frontend | OCUPADO | `nginx` (nativo) |
| 443 | HTTPS | OCUPADO | `nginx` (nativo) |
| 3000 | Frontend Angular (dev) | LIBRE | — |
| 3100 | Frontend staging | OCUPADO | `aba-stage-app-1` (Docker) |
| 3900 | OpenProject MCP | OCUPADO | `openproject-mcp` |
| 5432 | PostgreSQL | OCUPADO | `aba-stage-db-1` (Docker) |
| 5678 | n8n | OCUPADO | `n8n-docker-n8n-1` |
| 6000 | OpenProject | OCUPADO | `openproject-docker-openproject-1` |
| 6333 | Qdrant | **LIBRE** | — |
| 6334 | Qdrant (gRPC) | **LIBRE** | — |
| 8010 | Polymarket | OCUPADO | `polymarket-app` |
| 8055 | OpenCode | OCUPADO | `.opencode` |
| 8060 | Python dev | OCUPADO | `python` |
| 8080 | Quarkus backend | **LIBRE** ✅ | — |
| 8081 | IT Tools | OCUPADO | `it-tools-it-tools-1` |
| 8082 | WebDAV | OCUPADO | `obsidian-webdav` |
| 8083 | Quartz | OCUPADO | `obsidian-quartz` |
| 8090 | Elice | OCUPADO | `elice-app-1` |
| 8443 | Keycloak HTTPS | **LIBRE** ✅ | — |
| 9000 | Debug/metrics | LIBRE | — |

**Análisis:**
- ✅ Puerto **8080** (backend Quarkus) está LIBRE — sin conflictos.
- ✅ Puerto **8443** (Keycloak) está LIBRE.
- ✅ Puerto **6333** (Qdrant) está LIBRE.
- ⚠️ Puerto **5432** ocupado por `aba-stage-db-1`. Si se necesita un PostgreSQL dedicado para producción, usar otro puerto (ej: 5434) o reutilizar el existente.
- ✅ Nginx en 80/443 puede actuar como reverse proxy para frontend y backend.

---

## 5. Imágenes Docker Existentes del Proyecto

Se detectaron imágenes previamente construidas del proyecto OpenMemory:

| Imagen | Tag | Tamaño | Fecha de Creación |
|---|---|---|---|
| `openmemory-openmemory` | `latest` | 857 MB | 2026-03-01 |
| `openmemory-dashboard` | `latest` | 1.46 GB | 2026-03-01 |

Estas imágenes sugieren que ya hubo un despliegue previo (probablemente staging). Podrían reutilizarse si el código no ha cambiado, o reconstruirse desde el repositorio actual.

---

## 6. Capacidad del Servidor para Ejecución Directa (java -jar)

**¿Puede este servidor ejecutar el backend directamente?** → **SÍ**, con las siguientes consideraciones:

| Factor | Estado | Detalle |
|---|---|---|
| Java 21 Runtime | ✅ | OpenJDK 21.0.10 instalado |
| Memoria disponible | ✅ | 6.2 GB disponibles (suficiente para JVM heap ~2 GB) |
| CPU | ✅ | 6 cores (suficiente para GC concurrente) |
| Puerto 8080 | ✅ | Libre |
| Conexión a PostgreSQL | ✅ | Disponible vía Docker en 127.0.0.1:5432 |
| `JAVA_HOME` | ⚠️ | No definido (necesario para scripts) |
| GraalVM native | ❌ | No disponible; solo modo JVM |

**Recomendación:** Para el MVP, ejecutar el backend como JVM (`java -jar`) es totalmente viable y más simple que configurar una imagen nativa. Alternativamente, usar Docker con la imagen existente `openmemory-openmemory:latest` proporciona mejor aislamiento y consistencia.

---

## 7. Evaluación de Riesgos

| Riesgo | Severidad | Mitigación |
|---|---|---|
| Qdrant no instalado | **Alta** 🔴 | Bloquea búsqueda semántica. Instalar vía Docker (5 min) |
| Keycloak no instalado | **Alta** 🔴 | Bloquea autenticación OIDC/JWT. Instalar vía Docker (10 min) |
| Sin Kubernetes | **Media** 🟡 | Desplegar con Docker Compose en lugar de K8s. El servidor ya usa este modelo |
| Sin GraalVM native-image | **Baja** 🟢 | Usar modo JVM o imagen Docker preexistente |
| `JAVA_HOME` no definido | **Baja** 🟢 | Definir en `.bashrc` o en script de despliegue |
| Puerto 5432 ocupado | **Baja** 🟢 | Usar el PostgreSQL existente `aba-stage-db-1` o mapear nuevo contenedor a otro puerto |
| Sin firewall activo | **Media** 🟡 | `ufw` instalado pero inactivo. Activar si el servidor tiene IP pública |
| Imágenes Docker de 1 mes de antigüedad | **Media** 🟡 | Reconstruir imágenes desde el código actual antes del despliegue productivo |

---

## 8. Plan de Acción para Completar Infraestructura

### Paso 1: Instalar Qdrant (estimado: 5 minutos)
```bash
docker pull qdrant/qdrant:latest
docker run -d --name qdrant-prod --restart unless-stopped \
  -p 127.0.0.1:6333:6333 \
  -p 127.0.0.1:6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  qdrant/qdrant:latest
curl -s http://localhost:6333/health  # Debe retornar {"title":"qdrant - vector search engine","version":"..."}
```

### Paso 2: Instalar Keycloak (estimado: 10 minutos)
```bash
docker pull quay.io/keycloak/keycloak:26.0
docker run -d --name keycloak-prod --restart unless-stopped \
  -p 127.0.0.1:8443:8443 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=<GENERAR_PASSWORD_SEGURO> \
  quay.io/keycloak/keycloak:26.0 start-dev
```

### Paso 3: Definir variables de entorno globales
```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' | sudo tee -a /etc/environment
source /etc/environment
```

### Paso 4: Preparar base de datos PostgreSQL
```bash
# Crear base de datos para OpenMemory en el PostgreSQL existente
docker exec aba-stage-db-1 psql -U postgres -c "CREATE DATABASE openmemory_prod;"
# O crear nuevo contenedor PostgreSQL dedicado
docker run -d --name openmemory-db --restart unless-stopped \
  -p 127.0.0.1:5434:5432 \
  -e POSTGRES_DB=openmemory_prod \
  -e POSTGRES_PASSWORD=<PASSWORD_SEGURO> \
  -v openmemory_pgdata:/var/lib/postgresql/data \
  postgres:16-alpine
```

### Paso 5: Crear docker-compose.yml unificado
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    container_name: openmemory-db
    restart: unless-stopped
    ports:
      - "127.0.0.1:5434:5432"
    environment:
      POSTGRES_DB: openmemory_prod
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  qdrant:
    image: qdrant/qdrant:latest
    container_name: openmemory-qdrant
    restart: unless-stopped
    ports:
      - "127.0.0.1:6333:6333"
      - "127.0.0.1:6334:6334"
    volumes:
      - qdrant_storage:/qdrant/storage

  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    container_name: openmemory-keycloak
    restart: unless-stopped
    ports:
      - "127.0.0.1:8443:8443"
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    command: start-dev

  backend:
    image: openmemory-openmemory:latest
    container_name: openmemory-backend
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/openmemory_prod
      QUARKUS_DATASOURCE_USERNAME: postgres
      QUARKUS_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      QDRANT_URL: http://qdrant:6334
      QUARKUS_OIDC_AUTH_SERVER_URL: http://keycloak:8443/realms/openmemory
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    image: openmemory-dashboard:latest
    container_name: openmemory-frontend
    restart: unless-stopped
    ports:
      - "127.0.0.1:3000:80"
    depends_on:
      - backend

volumes:
  pgdata:
  qdrant_storage:
```

### Paso 6: Configurar Nginx como reverse proxy
```nginx
# /etc/nginx/sites-available/openmemory
server {
    listen 80;
    server_name <DOMINIO>;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
    }
}
```

---

## 9. Verificación de Evidencia

| # | Verificación | Evidencia |
|---|---|---|
| 1 | Java 21 | `openjdk version "21.0.10"` — exit code 0 |
| 2 | PostgreSQL funcional | Conexión exitosa a `aba-stage-db-1`: `SELECT version()` retornó `PostgreSQL 16.13` |
| 3 | Qdrant ausente | `which qdrant`, `docker ps \| grep qdrant`, `curl :6333/health` — todos sin resultado |
| 4 | Keycloak ausente | `which keycloak`, `docker ps \| grep keycloak`, `curl :8443` (HTTP 000) — sin resultado |
| 5 | Git operativo | `git version 2.43.0` |
| 6 | Maven operativo | `Apache Maven 3.8.7` — compatible con Java 21 |
| 7 | Docker operativo | `Docker version 29.4.2`, 12 contenedores corriendo, imágenes del proyecto presentes |
| 8 | Docker Compose | `Docker Compose version v2.27.1` |
| 9 | Nginx operativo | `nginx version: nginx/1.24.0` — sirviendo en 80 y 443 |
| 10 | Puertos verificados | `ss -tlnp` — 8080, 8443, 6333, 3000, 9000 todos LIBRES |
| 11 | Node.js | `v22.22.0`, npm `10.9.4` |
| 12 | Recursos | 6 CPUs, 11 GB RAM (6.2 GB disponibles), 115 GB disco libre |
| 13 | SO | Ubuntu 24.04.4 LTS, x86_64 |
| 14 | systemd | `systemd 255` operativo |

---

## 10. Recomendaciones Finales

1. **Prioridad #1**: Instalar Qdrant y Keycloak vía Docker — son bloqueantes para el MVP.
2. **Prioridad #2**: Definir `JAVA_HOME` y configurar variables de entorno del stack.
3. **Prioridad #3**: Crear un `docker-compose.yml` unificado para todo el stack productivo.
4. **Prioridad #4**: Reconstruir las imágenes Docker desde el código actual (las existentes son de marzo 2026).
5. **Consideración**: Activar `ufw` si el servidor está expuesto a Internet (actualmente inactivo).
6. **Estrategia de despliegue**: Docker Compose directo (sin K8s) es la opción recomendada para este servidor, alineado con cómo ya opera el entorno de staging.
7. **Modo de ejecución del backend**: Se recomienda usar la imagen Docker `openmemory-openmemory:latest` en lugar de `java -jar` directo, para consistencia con el resto del stack y facilidad de rollback.

---

*Informe generado el 2026-05-02 por DevOps Engineer. Toda la evidencia fue recolectada mediante comandos ejecutados directamente en el servidor objetivo.*
