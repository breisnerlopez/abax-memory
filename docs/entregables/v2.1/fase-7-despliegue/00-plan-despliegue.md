---
fase: 7-despliegue
entregable: 00-plan-despliegue
version: v2.1.0
build-commit: "0ef46b8"
responsable: devops
aprobador: pendiente — requiere aprobación explícita del sponsor
fecha: 2026-05-06
estado: borrador
tipo-servicio: API interna
rama: abax/abax-memory → merge a main activa GitHub Pages
---

# Plan de Despliegue — Abax-Memory v2.1.0

---

## Tabla de Contenidos

- [1. Dónde se despliega](#1-donde-se-despliega)
- [2. Cómo se despliega](#2-como-se-despliega)
- [3. URL pública y dominio](#3-url-publica-y-dominio)
- [4. DNS + TLS](#4-dns--tls)
- [5. Exposición](#5-exposicion)
- [6. Secrets](#6-secrets)
- [7. Monitoring + Alerting](#7-monitoring--alerting)
- [8. Rollback](#8-rollback)
- [9. Backup + Restore](#9-backup--restore)
- [10. Comunicación](#10-comunicacion)
- [11. Compliance](#11-compliance)
- [12. SLO/SLA](#12-slosla)
- [13. Procedimiento de Deploy — Paso a Paso](#13-procedimiento-de-deploy--paso-a-paso)
- [14. Verificación Post-Deploy](#14-verificacion-post-deploy)
- [15. Checklist Pre-Go-Live](#15-checklist-pre-go-live)
- [16. Aprobación del Sponsor](#16-aprobacion-del-sponsor)
- [17. Glosario](#17-glosario)

---

## 1. Dónde se despliega

| Aspecto | Detalle | Estado |
|---------|---------|--------|
| **Modelo** | Single-host Docker (Linux VM o bare-metal) | ✅ Configurado para dev |
| **Proveedor cloud** | No aplica — no se usa ningún proveedor cloud para el runtime de la aplicación | N/A |
| **Región** | No aplica — depende del host donde se ejecute | N/A |
| **Orquestación** | Docker Compose v2 (4 contenedores: Quarkus, PostgreSQL, Qdrant, Keycloak) | ✅ `docker-compose.yml` validado |
| **Cuenta / proyecto** | GitHub: `breisnerlopez/abax-memory` | ✅ Activo |
| **Container registry** | `ghcr.io/breisnerlopez/abax-memory` | ✅ v2.0.9 publicado; v2.1.0 pendiente de build + push |
| **Ambiente dev** | Local — `docker compose up -d` desde el repositorio | ✅ Operativo |
| **Ambiente QA** | Mismo host local, datos de prueba poblados con `scripts/populate-qa-data.sh` | ✅ Usado en fase 5 |
| **Ambiente staging** | **No existe** — no hay entorno de pre-producción separado | ⚠️ GAP |
| **Ambiente prod** | **No existe como entorno diferenciado** — el modelo actual es single-host que funciona como dev/QA/prod según los datos cargados | ⚠️ GAP |

> **Honestidad operativa**: Actualmente, "producción" es el mismo host Docker donde se ejecuta dev/QA. No existe un entorno productivo aislado con su propia infraestructura, red, ni controles de acceso. Si se requiere un verdadero despliegue productivo (accesible desde fuera de la máquina local, con disponibilidad garantizada), se necesita:
> 1. Un host Linux dedicado (o VM) con Docker Engine 24+.
> 2. Separación de redes y credenciales.
> 3. Estrategia de backup y monitoreo (ver gaps en secciones correspondientes).

---

## 2. Cómo se despliega

| Aspecto | Detalle | Estado |
|---------|---------|--------|
| **Estrategia** | **Recreate** — `docker compose down && docker compose up -d` (con volúmenes persistentes) | ✅ Probado en dev |
| **Frecuencia** | One-shot — despliegue manual por release | ✅ |
| **Tooling** | Docker Compose v2 + Docker CLI | ✅ |
| **CI/CD para app** | **No existe** — solo hay GitHub Actions para publicar docs (GitHub Pages en `main`). No hay pipeline de build/test/deploy del backend. | ⚠️ GAP |
| **Build de imagen** | Manual — `docker build -f Dockerfile -t ghcr.io/breisnerlopez/abax-memory:v2.1.0 .` tras compilar con Maven/Gradle | ✅ Procedimiento documentado |
| **Push de imagen** | Manual — `docker push ghcr.io/breisnerlopez/abax-memory:v2.1.0` | ✅ GHCR autenticado |
| **Tiempo estimado** | ~15 minutos (build + push + pull + restart + smoke test) | |
| **Ventana de mantenimiento** | ~5 minutos de downtime (entre `down` y `up`) | |
| **Downtime aceptable** | Sí — para un motor de búsqueda interno, 5 min es aceptable. Si se requiere zero-downtime, migrar a blue-green con otro puerto/container. | |

### Procedimiento de despliegue actual (alto nivel)

```
# 1. Build local de la imagen Docker
docker build -f Dockerfile -t ghcr.io/breisnerlopez/abax-memory:v2.1.0 .

# 2. Push al registry
docker push ghcr.io/breisnerlopez/abax-memory:v2.1.0

# 3. En el host de destino
docker compose pull abax-memory          # O editar docker-compose.yml para usar tag v2.1.0
docker compose down                       # Detiene contenedores, preserva volúmenes
docker compose up -d                      # Levanta con la nueva imagen

# 4. Verificar
./scripts/verify-stack.sh
```

> **Nota sobre `docker-compose.yml`**: El compose actual define `build:` con `context: .` y `image: ghcr.io/breisnerlopez/abax-memory:latest`. Al hacer `docker compose up -d`, Docker reconstruirá la imagen localmente si hay cambios, o usará la imagen remota si se elimina la sección `build:`. Para producción, se recomienda eliminar `build:` y depender exclusivamente de la imagen publicada en GHCR.

---

## 3. URL pública y dominio

### API (servicio principal)

| Aspecto | Detalle |
|---------|---------|
| **URL pública** | **No aplica** — la API no está expuesta a Internet |
| **Acceso** | `http://localhost:8080` (solo desde el host Docker) o `http://<host-ip>:8080` (red interna) |
| **Razón** | Abax-Memory es un motor de búsqueda interno (API). No es un SaaS público. Se consume desde otras aplicaciones dentro de la misma red o desde el mismo host. La exposición pública requeriría un API Gateway con autenticación, rate limiting, y TLS — elementos actualmente no configurados. |
| **Dominio** | No asignado |
| **Path** | Raíz (`/`) con endpoints bajo `/api/v2/`, `/q/health`, `/q/swagger-ui` |

### Documentación (GitHub Pages)

| Aspecto | Detalle |
|---------|---------|
| **URL pública** | **[https://breisnerlopez.github.io/abax-memory/](https://breisnerlopez.github.io/abax-memory/)** |
| **Activación** | Merge de `abax/abax-memory` a `main` dispara el workflow `pages.yml` |
| **Contenido** | Documentación del proyecto (entregables, arquitectura, runbooks) |
| **Estado** | ✅ Publicado |

> **Si en el futuro se requiere exponer la API públicamente**: se necesitará un dominio propio (`api.abax-memory.example.com`), un API Gateway o reverse proxy (Nginx/Traefik) con terminación TLS, y un proveedor DNS (Cloudflare/Route53). Esto está fuera del alcance de v2.1.0.

---

## 4. DNS + TLS

### API

| Aspecto | Detalle |
|---------|---------|
| **DNS** | No aplica — sin dominio público |
| **TLS** | **No configurado** — la API se comunica por HTTP en localhost o red Docker interna |
| **Certificado** | No aplica |
| **Renovación** | No aplica |

### Documentación (GitHub Pages)

| Aspecto | Detalle |
|---------|---------|
| **Proveedor DNS** | GitHub (`*.github.io`) |
| **TLS** | ✅ HTTPS automático con certificado gestionado por GitHub (Let's Encrypt) |
| **Renovación** | Automática — gestionada por GitHub |

### Comunicación con servicios externos

| Conexión | Protocolo | TLS |
|----------|-----------|-----|
| Backend → OpenAI API | HTTPS | ✅ TLS 1.2+ (gestionado por OpenAI) |
| Backend → GHCR (pull images) | HTTPS | ✅ TLS (gestionado por GitHub) |

> **GAP**: Las conexiones internas entre contenedores Docker (Quarkus → PostgreSQL, Quarkus → Qdrant, Quarkus → Keycloak) son HTTP plano dentro de la red `abax-net`. Para un entorno productivo real, se recomienda habilitar TLS interno o al menos asegurar que la red Docker es privada y no expuesta al host.

---

## 5. Exposición

| Componente | Detalle | Estado |
|------------|---------|--------|
| **Load Balancer** | No — single host, tráfico directo al puerto 8080 | N/A |
| **API Gateway** | No — sin gateway ni reverse proxy | ⚠️ GAP |
| **CDN** | No aplica — API, no contenido estático | N/A |
| **WAF** | No — sin protección de capa 7 | ⚠️ GAP si se expone públicamente |
| **Rate Limiting** | **No implementado** — fue excluido del scope de v2.1.0 por decisión del usuario (V21-API-04) | ⚠️ POSTERGADO |
| **IP Whitelist** | No — sin restricción de IPs | ⚠️ GAP |
| **Network** | Red Docker bridge interna `abax-net` (172.x.x.x). Puertos mapeados al host: 8080 (API), 5432 (PG), 6333-6334 (Qdrant), 8443 (Keycloak) | ✅ Para dev; ⚠️ en prod restringir exposición de PG y Qdrant |

> **Nota**: En el `docker-compose.yml` actual, PostgreSQL (5432) y Qdrant (6333) están expuestos en el host para facilitar desarrollo. En producción, estos puertos deben cerrarse o limitarse a `127.0.0.1`.

---

## 6. Secrets

### Estado actual

| Secreto | Dónde vive | Rotación | Estado |
|---------|-----------|----------|--------|
| `OPENAI_API_KEY` | Variable de entorno (`export OPENAI_API_KEY="..."`) o archivo `.env` | Manual — el usuario la rota desde la consola de OpenAI | ✅ Buena práctica: no hardcodeado. ⚠️ Sin rotación automática. |
| `QUARKUS_DATASOURCE_PASSWORD` (`pmoa`) | **Hardcodeado** en `docker-compose.yml` líneas 28, 111, 113 | Nunca rotado | ❌ INCORRECTO para producción |
| `QUARKUS_OIDC_CREDENTIALS_SECRET` (`ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI`) | **Hardcodeado** en `docker-compose.yml` línea 124 y `application.properties` línea 17 | Nunca rotado | ❌ INCORRECTO para producción |
| `KC_BOOTSTRAP_ADMIN_PASSWORD` (`admin`) | **Hardcodeado** en `docker-compose.yml` línea 76 | Nunca rotado | ❌ INCORRECTO para producción |
| Qdrant API Key | No configurada (`ABAX_V2_QDRANT_API_KEY` vacío) | N/A | ⚠️ Qdrant sin autenticación |
| Docker Hub / GHCR creds | `gh` CLI autenticado con token de alcance amplio | Manual | ✅ Para push de imágenes |

### Gestión de secrets

- **No existe secrets manager** (Vault, AWS Secrets Manager, GCP Secret Manager, Doppler).
- **No existe política de rotación**.
- **Acceso**: cualquier persona con acceso al repositorio o al host puede leer los secrets hardcodeados.
- **Inyección al runtime**: variables de entorno en `docker-compose.yml` (líneas 106-133).

### Recomendaciones para producción

1. **Inmediato (antes de cualquier despliegue fuera de dev)**:
   - Mover TODOS los secrets a variables de entorno (nunca en el compose).
   - Usar un archivo `.env` excluido de git (`.gitignore`).
   - Generar contraseñas fuertes aleatorias para PostgreSQL y Keycloak.
2. **Medio plazo**: Integrar con un secrets manager (HashiCorp Vault, AWS Secrets Manager, o Doppler).
3. **Rotación**: Definir política de rotación trimestral para API keys y credenciales.

---

## 7. Monitoring + Alerting

### Estado actual

| Aspecto | Detalle | Estado |
|---------|---------|--------|
| **Health checks** | Quarkus: `/q/health`, `/q/health/live`, `/q/health/ready`. Qdrant: `/healthz`. Keycloak: `/health/ready`. PG: `pg_isready`. | ✅ Configurados en `docker-compose.yml` |
| **Métricas (Prometheus)** | `/q/metrics` retorna **404** — la extensión `quarkus-micrometer-registry-prometheus` no está en el classpath | ❌ NO DISPONIBLE (condición 3 del UAT) |
| **Logging** | Logs de contenedor Docker (`docker logs <container>`). Sin agregador centralizado. Formato: texto plano (no JSON). | ⚠️ Básico |
| **Tracing** | No configurado (sin OpenTelemetry, Jaeger, ni X-Ray) | ❌ NO DISPONIBLE |
| **Dashboard** | **No existe** — sin Grafana, Datadog, ni equivalente | ❌ NO DISPONIBLE |
| **Alerting** | **No existe** — sin PagerDuty, Opsgenie, ni alertas por Slack | ❌ NO DISPONIBLE |
| **Runbook oncall** | **No existe** — no hay procedimiento documentado de respuesta a incidentes | ❌ NO DISPONIBLE |

### Métricas mínimas disponibles hoy

| Métrica | Fuente | Acceso |
|---------|--------|--------|
| Backend UP/DOWN | `curl -s http://localhost:8080/q/health` | Manual |
| PostgreSQL UP/DOWN | `docker exec abax-postgres pg_isready -U pmoa` | Manual |
| Qdrant UP/DOWN | `curl -s http://localhost:6333/healthz` | Manual |
| Keycloak UP/DOWN | `curl -s http://localhost:8443/health/ready` | Manual |
| Uso de disco (volúmenes) | `docker system df -v` | Manual |
| Logs de aplicación | `docker logs abax-memory-backend --tail 100` | Manual |

### Compromiso post-deploy (Condición 3 del UAT)

La [Condición 3 del Acta de Aceptación UAT](../fase-6-uat/acta-de-aceptacion-uat.md#43-condicion-3--observabilidad-metrics) exige habilitar `quarkus-micrometer-registry-prometheus` en el primer patch post-deploy (v2.1.1). Esto es **no bloqueante** para el despliegue inicial.

> **GAP crítico**: Sin métricas no hay observabilidad. Sin observabilidad no hay despliegue seguro. Se recomienda que v2.1.1 sea desplegada **inmediatamente después** de verificar v2.1.0, para cerrar este gap.

---

## 8. Rollback

### Estrategia

**Recreate con imagen anterior** — revertir al tag de la versión previa conocida como estable.

| Aspecto | Detalle |
|---------|---------|
| **Versión de rollback** | `ghcr.io/breisnerlopez/abax-memory:v2.0.9` (última imagen publicada y probada) |
| **Imagen actual (v2.0.9)** | SHA del image digest: disponible en GHCR |
| **Estrategia** | Redeploy de la versión anterior |
| **Comando de rollback** | Ver abajo |
| **¿Probado en staging?** | **NO** — no existe entorno de staging donde probarlo |
| **¿Probado en QA?** | **NO** — el rollback no fue ejercitado durante la fase 5 |
| **Tiempo estimado (RTO rollback)** | ~5 minutos |
| **Ventana segura** | Inmediatamente tras detectar el fallo. Los volúmenes Docker preservan los datos. |
| **Migraciones de BD** | v2.1.0 no introduce nuevas migraciones Flyway (la migración V13 ya existe y es forward-compatible). Si en el futuro se añaden migraciones, se requerirá `flyway:undo` o restore de backup. |

### Comando de rollback

```bash
# 1. Detener la versión defectuosa
docker compose down

# 2. Editar docker-compose.yml para apuntar a la imagen anterior, o usar:
#    (alternativa: sed para cambiar el tag temporalmente)
export ROLLBACK_IMAGE="ghcr.io/breisnerlopez/abax-memory:v2.0.9"

# 3. Levantar con la versión anterior
# Opción A: usar variable de entorno + modificar el compose para usar ${ROLLBACK_IMAGE}
# Opción B: docker run directo (más rápido si compose tarda)
docker compose up -d

# 4. Verificar
./scripts/verify-stack.sh
curl -s http://localhost:8080/q/health | jq '.status'
```

### Opción de rollback mínimo (docker run directo sobre el compose existente)

```bash
# Detener solo el backend, mantener infraestructura
docker stop abax-memory-backend && docker rm abax-memory-backend

# Levantar backend con la imagen anterior
docker run -d --name abax-memory-backend \
  --network abax-net \
  -p 8080:8080 \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://abax-postgres:5432/pmoadb \
  -e QUARKUS_DATASOURCE_USERNAME=pmoa \
  -e QUARKUS_DATASOURCE_PASSWORD=pmoa \
  -e ABAX_QDRANT_HOST=abax-qdrant \
  -e ABAX_QDRANT_PORT=6333 \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://abax-keycloak:8080/realms/abax-memory \
  -e QUARKUS_OIDC_CLIENT_ID=abax-memory-api \
  -e QUARKUS_OIDC_CREDENTIALS_SECRET=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI \
  -e MP_JWT_VERIFY_ISSUER=http://abax-keycloak:8080/realms/abax-memory \
  -e MP_JWT_VERIFY_AUDIENCES=abax-memory-api \
  -e ABAX_OPENAI_VALIDATION_MODEL=gpt-4o \
  -e ABAX_PROCESSING_AUTO_RUN=true \
  ghcr.io/breisnerlopez/abax-memory:v2.0.9

# Verificar
sleep 10
curl -s http://localhost:8080/q/health | jq '.status'
```

> ⚠️ **Importante**: Este comando de rollback NO ha sido probado. La primera ejecución de este plan DEBE incluir una prueba de rollback inmediatamente después del deploy exitoso para validar que el procedimiento funciona. Ver [Sección 14 — Verificación Post-Deploy](#14-verificación-post-deploy).

---

## 9. Backup + Restore

### Estado actual

| Aspecto | Detalle | Estado |
|---------|---------|--------|
| **Estrategia de backup** | **No existe** — no hay backups automatizados de ningún tipo | ❌ GAP |
| **Persistencia** | Volúmenes Docker nombrados: `abax-pgdata` (PostgreSQL), `abax-qdrant-storage` (Qdrant) | ✅ Datos sobreviven a `docker compose down` |
| **RPO** | **No definido** | ❌ |
| **RTO** | **No definido** | ❌ |
| **Ubicación de backups** | No aplica — no se generan | ❌ |
| **Restore probado** | **No** — nunca se ha ejecutado un restore | ❌ |

### Backup manual (procedimiento de emergencia)

```bash
# Backup de PostgreSQL
docker exec abax-postgres pg_dump -U pmoa pmoadb | gzip > "abax-memory-pg-$(date +%Y%m%d-%H%M).sql.gz"

# Backup de Qdrant (snapshot)
curl -X POST "http://localhost:6333/collections/abax-memories/snapshots"

# Backup de volúmenes Docker (método brute-force)
docker run --rm -v abax-pgdata:/data -v $(pwd):/backup alpine \
  tar czf "/backup/abax-pgdata-$(date +%Y%m%d-%H%M).tar.gz" -C /data .
docker run --rm -v abax-qdrant-storage:/data -v $(pwd):/backup alpine \
  tar czf "/backup/abax-qdrant-storage-$(date +%Y%m%d-%H%M).tar.gz" -C /data .
```

### Restore manual (procedimiento de emergencia)

```bash
# Restore de PostgreSQL
gunzip < abax-memory-pg-YYYYMMDD-HHMM.sql.gz | docker exec -i abax-postgres psql -U pmoa pmoadb

# Restore de volumen Docker
docker run --rm -v abax-pgdata:/data -v $(pwd):/backup alpine \
  tar xzf "/backup/abax-pgdata-YYYYMMDD-HHMM.tar.gz" -C /data
```

> ⚠️ **GAP crítico para producción**: Sin backups automatizados, cualquier fallo de disco o corrupción de datos resulta en pérdida irreversible. Para un entorno productivo real, se recomienda:
> 1. `pg_dump` programado (cron) con retención de 7 días en local + 30 días en cloud (S3/GCS).
> 2. Snapshots de volúmenes si el host tiene LVM o el proveedor cloud ofrece snapshots de disco.
> 3. Probar el restore al menos una vez al mes.

---

## 10. Comunicación

### Stakeholders internos

| Destinatario | Mensaje | Canal | Timing |
|-------------|---------|-------|--------|
| **Sponsor (Product Owner)** | Aprobación del plan, confirmación de go-live, resultado del deploy | Chat del proyecto / email | Antes (aprobación), durante (ejecución), después (resultado) |
| **Tech Lead** | Ventana de despliegue, versión desplegada, SHA, resultado de smoke tests | Chat del proyecto | 30 min antes + inmediatamente post-deploy |
| **QA Functional** | Solicitud de smoke test funcional post-deploy | Chat del proyecto | Inmediatamente post-deploy |

### Usuarios finales

| Aspecto | Detalle |
|---------|---------|
| **Usuarios afectados** | **No hay usuarios finales externos** — la API es consumida por otras aplicaciones internas |
| **Comunicación a usuarios** | No aplica para v2.1.0. Las aplicaciones consumidoras deben ser notificadas del cambio de versión si hay cambios breaking (no los hay en v2.1.0 — es hardening compatible hacia atrás). |
| **Página de status** | **No existe** — GitHub Pages podría usarse como status page improvisada, pero no está configurada. |

### Plan de comunicación en caso de incidente

| Aspecto | Detalle |
|---------|---------|
| **Responsable** | devops notifica al tech-lead y al sponsor |
| **Canal primario** | Chat del proyecto |
| **Contenido** | Naturaleza del incidente, impacto, ETA de resolución, si se ejecutó rollback |
| **Post-mortem** | Documentar en `docs/entregables/v2.1/fase-8-estabilizacion/` si ocurre un incidente |

---

## 11. Compliance

| Aspecto | Detalle |
|---------|---------|
| **Regulación aplicable** | **No aplica** — Abax-Memory es un motor de búsqueda interno. No procesa datos personales de usuarios finales, no almacena PII, no está sujeto a GDPR, HIPAA, PCI-DSS, SOC2, ni ISO27001 en su deployment actual. |
| **Audit log** | ✅ La aplicación registra eventos en la tabla `audit_records` de PostgreSQL (acciones: `MEMORY_CREATE`, `MEMORY_UPDATE`, `RELATION_CREATE`, `RELATION_UPDATE`, `NAMESPACE_DELETE`, etc.). Retención: indefinida (hasta que se borre manualmente). |
| **Datos sensibles** | Las memorias almacenadas pueden contener texto operativo, pero no PII por diseño. Los embeddings vectoriales no son reversibles a texto plano. |
| **Encripción at-rest** | **No** — los volúmenes Docker no están encriptados. |
| **Encripción in-transit** | Solo en llamadas externas a OpenAI (HTTPS). Tráfico interno Docker en HTTP plano. |
| **Residencia de datos** | Depende de la ubicación del host. OpenAI procesa embeddings en sus servidores (EE.UU.). |

> **Nota**: Si en el futuro Abax-Memory se despliega en un contexto regulado (financiero, salud, datos personales), se requerirá un análisis de cumplimiento completo con el security-architect.

---

## 12. SLO/SLA

| Aspecto | Detalle | Estado |
|---------|---------|--------|
| **Disponibilidad target** | **No definida** | ❌ |
| **Latencia p95 target** | CE-02 definió ≤ 500ms, pero no se cumple (5,255ms dominado por OpenAI embedding). Aceptado como restricción externa en UAT. | ⚠️ Documentado en acta UAT |
| **Error budget** | No definido | ❌ |
| **Política de freeze** | No definida | ❌ |
| **Responsable SLO** | No asignado | ❌ |
| **Reporte mensual** | No implementado | ❌ |

### Métricas de referencia (desde QA)

| Métrica | Valor observado en QA (build `0ef46b8`) |
|---------|---------------------------------------|
| Precisión top-1 (benchmark 30 queries) | **96.67%** (29/30) |
| MRR (benchmark 30 queries) | **0.967** |
| Latencia p95 dense retrieval puro | **213ms** |
| Latencia p95 con embedding OpenAI + cross-encoder | **5,255ms** |
| Throughput con 100 VUs (k6) | **972 iteraciones, 0% error rate** |

> **GAP**: Incluso como herramienta interna, se recomienda definir SLOs básicos (ej. disponibilidad 99.5% en horario laboral, latencia p95 < 6s para el pipeline completo). Esto permite tomar decisiones informadas sobre priorización de mejoras.

---

## 13. Procedimiento de Deploy — Paso a Paso

### Pre-condiciones

- [ ] Plan de despliegue aprobado por el sponsor (firma en Sección 16).
- [ ] Rama `abax/abax-memory` mergeada a `main` (para activar GitHub Pages con docs actualizados).
- [ ] `OPENAI_API_KEY` configurada en el host de destino.
- [ ] Docker Engine 24+ y Docker Compose v2 instalados en el host de destino.
- [ ] Puertos requeridos libres: 8080, 5432, 6333, 6334, 8443.
- [ ] Espacio en disco suficiente para la nueva imagen Docker (~500 MB) y volúmenes.

### Paso 1: Build de la imagen Docker

```bash
# Desde la raíz del repositorio, en la rama abax/abax-memory
git checkout abax/abax-memory
git pull origin abax/abax-memory

# Verificar que estamos en el commit correcto
git rev-parse HEAD
# Debe imprimir: 0ef46b8c12b4f683d9b78ab8675dda4d1001289a

# Compilar el backend Quarkus (si no está ya compilado)
# Opción A: si ya existe backend-quarkus/target/quarkus-app/
ls backend-quarkus/target/quarkus-app/quarkus-run.jar || ./mvnw -f backend-quarkus package -DskipTests

# Construir imagen Docker
docker build -f Dockerfile -t ghcr.io/breisnerlopez/abax-memory:v2.1.0 .
docker tag ghcr.io/breisnerlopez/abax-memory:v2.1.0 ghcr.io/breisnerlopez/abax-memory:latest
```

### Paso 2: Push al GitHub Container Registry

```bash
# Verificar autenticación con GHCR
echo "TOKEN" | docker login ghcr.io -u breisnerlopez --password-stdin
# O usar: gh auth token | docker login ghcr.io -u breisnerlopez --password-stdin

docker push ghcr.io/breisnerlopez/abax-memory:v2.1.0
docker push ghcr.io/breisnerlopez/abax-memory:latest
```

### Paso 3: Ejecutar backup pre-deploy

```bash
# Backup rápido de PostgreSQL antes de desplegar
docker exec abax-postgres pg_dump -U pmoa pmoadb | gzip > "pre-deploy-v2.1.0-$(date +%Y%m%d-%H%M).sql.gz"
echo "Backup pre-deploy completado: $(ls -lh pre-deploy-v2.1.0-*.sql.gz)"
```

### Paso 4: Desplegar en el host de destino

```bash
# Opción A: Si se usa docker compose con la imagen publicada
# Modificar docker-compose.yml para quitar la sección 'build:' y usar solo 'image:'
# O simplemente hacer pull de la nueva imagen
docker compose pull abax-memory

# Detener servicios (preserva volúmenes)
docker compose down

# Levantar con nueva imagen
docker compose up -d

# Opción B: Si se construyó localmente (sin push a GHCR)
docker compose up -d --build
```

### Paso 5: Verificación post-deploy

Ejecutar en orden (ver Sección 14 para detalles):

```bash
# 1. Esperar a que los contenedores estén healthy (~30s)
sleep 30

# 2. Verificar stack
./scripts/verify-stack.sh

# 3. Smoke test funcional
curl -s http://localhost:8080/q/health | jq '.status'

# 4. Verificar versión en logs
docker logs abax-memory-backend 2>&1 | grep -i "Abax-Memory" | tail -3
```

### Si algo falla → Rollback inmediato

```bash
# Volver a la imagen v2.0.9
docker compose down
# Editar docker-compose.yml: cambiar tag de latest a v2.0.9, o usar variable de entorno
docker compose up -d
sleep 15
./scripts/verify-stack.sh
```

---

## 14. Verificación Post-Deploy

### 14.1 Verificación operacional (responsabilidad: devops)

```bash
# 1. Health checks de los 4 servicios
./scripts/verify-stack.sh
# Esperado: 4/4 servicios UP

# 2. Endpoint de salud del backend
curl -s http://localhost:8080/q/health | jq
# Esperado: {"status": "UP", "checks": [...]}

# 3. Qdrant: verificar colección unificada
curl -s http://localhost:6333/collections | jq '.result.collections[].name'
# Esperado: ["abax-memories"] (una sola colección)

# 4. Keycloak: verificar realm
curl -s -o /dev/null -w "%{http_code}" http://localhost:8443/realms/abax-memory
# Esperado: 200

# 5. PostgreSQL: verificar migraciones Flyway
docker logs abax-memory-backend 2>&1 | grep -i "flyway" | tail -5
# Esperado: "Schema 'public' is up to date. No migration necessary."

# 6. Verificar versión de la imagen
docker inspect abax-memory-backend --format '{{.Config.Image}}'
# Esperado: ghcr.io/breisnerlopez/abax-memory:v2.1.0 o latest
```

### 14.2 Smoke test funcional (responsabilidad: devops)

```bash
# 1. Obtener JWT de Keycloak
export JWT=$(curl -s -X POST "http://localhost:8443/realms/abax-memory/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=abax-memory-api" \
  -d "client_secret=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

if [ -z "$JWT" ] || [ "$JWT" = "null" ]; then
  echo "ERROR: No se pudo obtener JWT"
  exit 1
fi

# 2. Smoke test: búsqueda semántica básica
curl -s -X POST "http://localhost:8080/api/v2/search" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: default-tenant" \
  -d '{"query": "test", "size": 1, "expandGraph": false}' | jq '{total: .total, queryTimeMs: .queryTimeMs}'
# Esperado: HTTP 200, total >= 0, queryTimeMs numérico

# 3. Smoke test: health endpoint responde
curl -s http://localhost:8080/q/health | jq '.status'
# Esperado: "UP"
```

### 14.3 Prueba de rollback (responsabilidad: devops)

**Obligatorio**: inmediatamente después de confirmar que v2.1.0 funciona, ejecutar una prueba de rollback controlada:

```bash
# 1. Ejecutar rollback a v2.0.9
docker stop abax-memory-backend && docker rm abax-memory-backend
docker run -d --name abax-memory-backend \
  --network abax-net -p 8080:8080 \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://abax-postgres:5432/pmoadb \
  -e QUARKUS_DATASOURCE_USERNAME=pmoa \
  -e QUARKUS_DATASOURCE_PASSWORD=pmoa \
  -e ABAX_QDRANT_HOST=abax-qdrant \
  -e ABAX_QDRANT_PORT=6333 \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://abax-keycloak:8080/realms/abax-memory \
  -e QUARKUS_OIDC_CLIENT_ID=abax-memory-api \
  -e QUARKUS_OIDC_CREDENTIALS_SECRET=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI \
  -e MP_JWT_VERIFY_ISSUER=http://abax-keycloak:8080/realms/abax-memory \
  -e MP_JWT_VERIFY_AUDIENCES=abax-memory-api \
  -e ABAX_OPENAI_VALIDATION_MODEL=gpt-4o \
  -e ABAX_PROCESSING_AUTO_RUN=true \
  ghcr.io/breisnerlopez/abax-memory:v2.0.9

sleep 15
curl -s http://localhost:8080/q/health | jq '.status'
# Esperado: "UP" con la imagen v2.0.9

# 2. RE-desplegar v2.1.0
docker stop abax-memory-backend && docker rm abax-memory-backend
docker run -d --name abax-memory-backend \
  --network abax-net -p 8080:8080 \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://abax-postgres:5432/pmoadb \
  -e QUARKUS_DATASOURCE_USERNAME=pmoa \
  -e QUARKUS_DATASOURCE_PASSWORD=pmoa \
  -e ABAX_QDRANT_HOST=abax-qdrant \
  -e ABAX_QDRANT_PORT=6333 \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://abax-keycloak:8080/realms/abax-memory \
  -e QUARKUS_OIDC_CLIENT_ID=abax-memory-api \
  -e QUARKUS_OIDC_CREDENTIALS_SECRET=ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI \
  -e MP_JWT_VERIFY_ISSUER=http://abax-keycloak:8080/realms/abax-memory \
  -e MP_JWT_VERIFY_AUDIENCES=abax-memory-api \
  -e ABAX_OPENAI_VALIDATION_MODEL=gpt-4o \
  -e ABAX_PROCESSING_AUTO_RUN=true \
  ghcr.io/breisnerlopez/abax-memory:v2.1.0

sleep 15
curl -s http://localhost:8080/q/health | jq '.status'
# Esperado: "UP" con la imagen v2.1.0 nuevamente
```

### 14.4 Verificación funcional post-deploy (responsabilidad: qa-functional)

El qa-functional ejecuta una batería mínima de smoke tests funcionales contra el entorno desplegado. **Esta verificación no es responsabilidad de devops** según la matriz `role-boundaries`. El qa-functional debe:

1. Ejecutar los 4 escenarios UAT core: UAT-PREC-01, UAT-API-01, UAT-API-03, UAT-EFI-01.
2. Verificar que los resultados coinciden con los documentados en fase 6.
3. Reportar cualquier discrepancia al tech-lead.

---

## 15. Checklist Pre-Go-Live

Ejecutar en orden el día del despliegue. Si algún ítem falla: **ABORTAR**, no desplegar.

| # | Ítem | Comando / Verificación | Estado |
|---|------|------------------------|--------|
| 1 | **Plan aprobado por sponsor** | Firma en Sección 16 de este documento | ⬜ |
| 2 | **Imagen v2.1.0 publicada en GHCR** | `docker pull ghcr.io/breisnerlopez/abax-memory:v2.1.0` → éxito | ⬜ |
| 3 | **Commit correcto** | `git rev-parse HEAD` = `0ef46b8c12b4f683d9b78ab8675dda4d1001289a` | ⬜ |
| 4 | **Backup pre-deploy reciente** | `ls -lh pre-deploy-v2.1.0-*.sql.gz` → archivo existe | ⬜ |
| 5 | **Stack actual healthy** | `./scripts/verify-stack.sh` → 4/4 UP | ⬜ |
| 6 | **OPENAI_API_KEY configurada** | `[ -n "$OPENAI_API_KEY" ] && echo "SET" || echo "MISSING"` | ⬜ |
| 7 | **Equipo notificado** | Mensaje enviado al chat del proyecto con ventana de deploy | ⬜ |
| 8 | **Ejecutar deploy** | `docker compose down && docker compose up -d` | ⬜ |
| 9 | **Smoke test post-deploy** | `./scripts/verify-stack.sh && curl http://localhost:8080/q/health` → UP | ⬜ |
| 10 | **Prueba de rollback** | Ejecutar Sección 14.3 → rollback a v2.0.9 OK, re-deploy v2.1.0 OK | ⬜ |
| 11 | **Smoke test funcional qa-functional** | qa-functional ejecuta batería mínima y reporta | ⬜ |
| 12 | **Actualizar GitHub Pages** | Merge `abax/abax-memory` → `main` para publicar docs actualizados | ⬜ |

---

## 16. Aprobación del Sponsor

**ANTES de ejecutar cualquier comando de despliegue**, el sponsor debe aprobar este plan explícitamente.

- [ ] El sponsor confirma que las 12 secciones tienen respuesta concreta (o justificación documentada de no-aplica).
- [ ] El sponsor acepta los gaps documentados (staging inexistente, secrets hardcodeados en dev, sin monitoreo, sin backups automatizados, sin SLOs).
- [ ] El sponsor aprueba el modelo de despliegue (single-host Docker, sin exposición pública de la API).
- [ ] El sponsor aprueba el plan de comunicación a stakeholders internos.
- [ ] El sponsor aprueba el plan de rollback y acepta el riesgo de que no ha sido probado previamente.
- [ ] Fecha de go-live: _______________
- [ ] Sponsor (Product Owner): _______________
- [ ] Fecha de aprobación: _______________

### Firmas

| Rol | Nombre | Fecha | Firma |
|-----|--------|-------|-------|
| **Sponsor (Product Owner)** | _______________ | _______________ | _______________ |
| **devops** | Agente devops Abax | 2026-05-06 | Electrónica (commit de este plan) |
| **Tech Lead** | _______________ | _______________ | _______________ |
| **Solution Architect** | _______________ | _______________ | _______________ |

---

## 17. Glosario

- **RPO**: Recovery Point Objective — cantidad máxima de datos que se acepta perder en un desastre (medido en tiempo: ej. 1 hora de datos).
- **RTO**: Recovery Time Objective — tiempo máximo aceptable para restaurar el servicio tras un incidente.
- **SLO**: Service Level Objective — objetivo medible de rendimiento del servicio (ej. disponibilidad 99.9%, latencia p95 < 500ms).
- **GHCR**: GitHub Container Registry — registro de imágenes Docker integrado con GitHub.
- **PII**: Personally Identifiable Information — datos que permiten identificar a una persona específica.
- **UAT**: User Acceptance Testing — fase donde el sponsor valida que el producto cumple sus necesidades antes del despliegue.
- **Flyway**: Herramienta de migraciones de base de datos versionadas para Java.

---

## Resumen de GAPS detectados

Este plan documenta honestamente las brechas entre el estado actual y lo que se consideraría un despliegue productivo robusto. Los gaps **no bloquean** el despliegue de v2.1.0 en el entorno actual (single-host Docker, uso interno), pero DEBEN ser abordados antes de cualquier exposición pública o uso en misión crítica.

| # | Gap | Severidad | Sección | Recomendación |
|---|-----|-----------|---------|---------------|
| 1 | Sin entorno de staging separado | Alta | 1 | Crear entorno staging idéntico a prod antes del próximo release |
| 2 | Secrets hardcodeados en `docker-compose.yml` | Crítica | 6 | Mover a `.env` + `.gitignore`. Rotar secretos existentes. |
| 3 | Sin CI/CD para build/test/deploy de la app | Alta | 2 | Agregar GitHub Actions workflow para build + test + push a GHCR |
| 4 | Sin monitoreo (Prometheus, dashboards, alertas) | Alta | 7 | Habilitar `quarkus-micrometer-registry-prometheus` en v2.1.1 |
| 5 | Rollback nunca probado | Alta | 8 | Ejecutar prueba de rollback en el primer despliegue |
| 6 | Sin backups automatizados | Crítica | 9 | Implementar `pg_dump` cron + restore test mensual |
| 7 | Sin SLO/SLA definidos | Media | 12 | Definir SLOs básicos (disponibilidad, latencia) para el equipo |
| 8 | Sin runbook de oncall | Media | 7 | Documentar procedimientos de diagnóstico y respuesta a incidentes |
| 9 | Qdrant sin autenticación | Media | 6 | Configurar API key de Qdrant (`ABAX_V2_QDRANT_API_KEY`) |
| 10 | Rate limiting no implementado | Baja | 5 | Incluir en roadmap futuro (excluido de v2.1.0 por el usuario) |
