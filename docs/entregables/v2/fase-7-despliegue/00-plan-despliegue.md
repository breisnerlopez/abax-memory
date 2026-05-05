# Plan de Despliegue — Abax-Memory v2.0.0

- **Fase**: 7-Despliegue
- **Entregable**: Plan de Despliegue
- **Responsable**: devops
- **Aprobador**: Usuario sponsor
- **Fecha**: 2026-05-04
- **Estado**: Borrador — Pendiente de aprobacion del sponsor
- **Tipo de servicio**: API + Web App
- **Release**: v2.0.0
- **Predecesor**: `docs/entregables/v2/fase-6-uat/acta-aceptacion-uat.md` — APROBADO CON CONDICIONES (80%)

---

## 1. Donde se despliega

- **Cloud / on-prem**: Localhost (entorno de desarrollo/productivo unificado para MVP)
- **Region**: N/A (localhost)
- **Ambientes**:
  | Ambiente | Proposito | Estado |
  |---|---|---|
  | dev | Desarrollo activo, `localhost:8080` | Operativo |
  | qa | Pruebas QA, `localhost:8080` | Operativo (validado en Fase 5) |
  | uat | Aceptacion de usuario, `localhost:8080` | Operativo (validado en Fase 6) |
  | prod | Produccion, `localhost:8080` | **Este despliegue** |
- **Cuenta / proyecto**: Maquina local del sponsor. No requiere cuenta cloud.
- **Contenedores Docker**:
  | Componente | Imagen | Puerto |
  |---|---|---|
  | Backend Quarkus | `ghcr.io/breisnerlopez/abax-memory:v2.0.0` | `8080` |
  | PostgreSQL | `postgres:16-alpine` | `5432` |
  | Qdrant | `qdrant/qdrant:v1.17.1` | `6333` (REST), `6334` (gRPC) |
  | Keycloak | `quay.io/keycloak/keycloak:26.1.0` | `8443` |

---

## 2. Como se despliega

- **Estrategia**: Recreate (all-at-once). Despliegue greenfield con datos semilla.
- **Tooling**:
  - **Manual**: `docker compose up -d` desde el directorio raiz del proyecto.
  - **CI/CD**: GitHub Actions para build de imagen y push a GHCR.
- **Frecuencia**: One-shot para v2.0.0. Despliegues posteriores con CI/CD.
- **Tiempo estimado de deploy**: 5-10 minutos (build de imagen + startup de contenedores).
- **Ventana de despliegue**: Bajo demanda. No requiere ventana de mantenimiento (entorno local).

### Pipeline de despliegue

```
Git push (main) → GitHub Actions → Build + Test → Push GHCR → docker compose pull → docker compose up -d
```

### Comandos de despliegue manual

```bash
# 1. Construir backend
cd abax-memory/
mvn clean package -DskipTests

# 2. Construir imagen Docker
docker build -t ghcr.io/breisnerlopez/abax-memory:v2.0.0 .

# 3. Push a GHCR (si se usa CI/CD)
docker push ghcr.io/breisnerlopez/abax-memory:v2.0.0

# 4. Levantar stack con Docker Compose
docker compose up -d

# 5. Verificar health checks
curl -s http://localhost:8080/q/health | jq .status
```

---

## 3. URL publica y dominio

- **URL final**: `http://localhost:8080`
- **Dominio**: N/A (localhost, entorno de desarrollo)
- **Path**: Raiz (`/`). API en `/api/v2/`.
- **Justificacion de no-publicacion**: El MVP v2.0.0 se despliega en entorno local del sponsor. La exposicion publica (dominio, TLS, DNS) esta fuera del alcance del MVP y se planificara para releases futuras cuando se requiera acceso externo.
- **GitHub Pages**: La documentacion y presentaciones se publican via GitHub Pages en `https://breisnerlopez.github.io/abax-memory/`.
- **GHCR**: La imagen Docker se publica en `ghcr.io/breisnerlopez/abax-memory:v2.0.0`.

> **Nota**: Si el sponsor requiere acceso publico en el futuro, se debera crear un plan de exposicion complementario con dominio, TLS y DNS.

---

## 4. DNS + TLS

- **DNS**: N/A para localhost.
- **TLS**: N/A para localhost. Keycloak opera en `http://localhost:8443` sin TLS en entorno dev.
- **Provider DNS**: No aplica. Si se migra a cloud en el futuro, se evaluara Cloudflare o Route53.
- **Certificado**: No aplica. Para produccion publica se usaria Let's Encrypt + cert-manager.
- **Renovacion automatica**: No aplica en MVP.

---

## 5. Modelo de exposicion

- **Puerto**: `8080` (backend Quarkus).
- **Load balancer**: No aplica (single instance localhost).
- **API Gateway**: No aplica en MVP. Quarkus sirve directamente.
- **Whitelist de IPs**: No aplica (localhost).
- **Rate limiting**: Implementado en backend — umbral configurable (actual: 1000 req/min). UAT-S09 detecto que el umbral no se supero en pruebas. Se recomienda ajustar a 100 req/min para produccion.
- **Proteccion contra abuso**: Rate limiting via Quarkus. Autenticacion OIDC via Keycloak para endpoints protegidos.

---

## 6. Secrets management

- **Donde viven**: Variables de entorno en archivo `.env` (no commiteado).
- **Como se inyectan**: Docker Compose lee `.env` y las inyecta como environment variables al contenedor del backend.
- **Secrets requeridos**:

  | Secret | Variable | Proposito |
  |---|---|---|
  | OpenAI API Key | `OPENAI_API_KEY` | Embeddings (`text-embedding-3-large`) y extraccion (`gpt-4o-mini`) |
  | DB Password | `DB_PASSWORD` | Conexion PostgreSQL |
  | Keycloak Admin | `KEYCLOAK_ADMIN_PASSWORD` | Administracion de realm OIDC |

- **Quien tiene acceso**: Solo el sponsor (dueno de la maquina local).
- **Politica de rotacion**: Rotar `OPENAI_API_KEY` cada 90 dias o ante sospecha de exposicion. Procedimiento: generar nueva key en OpenAI dashboard → actualizar `.env` → reiniciar backend.
- **Exclusion de Git**: `.env` esta en `.gitignore`. Las variables NUNCA se hardcodean en codigo fuente ni en archivos de configuracion.

---

## 7. Monitoring + alerting

- **Dashboard**: No hay dashboard externo en MVP. Se usan health endpoints.
- **Metricas minimas**:
  - Latencia p50/p95/p99: disponible via `/q/metrics` (Quarkus Micrometer).
  - Error rate: disponible via `/q/metrics`.
  - Throughput: disponible via `/q/metrics`.
  - Saturacion: memory/heap via `/q/metrics`.

- **Health endpoints**:
  | Endpoint | Proposito | Respuesta esperada |
  |---|---|---|
  | `/q/health` | Health agregado | `{"status": "UP"}` |
  | `/q/health/live` | Liveness probe | `{"status": "UP"}` |
  | `/q/health/ready` | Readiness probe (incluye BD, Qdrant, OpenAI) | `{"status": "UP"}` |
  | `/q/metrics` | Metricas Prometheus | Formato Prometheus |
  | `/q/openapi` | OpenAPI spec | OpenAPI 3.0.3 |

- **Logging**: Logs de contenedores via `docker compose logs -f backend`. Formato: Quarkus default (JSON o texto). Retencion: local, rotacion manual.
- **Tracing**: No implementado en MVP. Recomendado OpenTelemetry para futuro.
- **Alerting**: No implementado en MVP. El sponsor monitorea manualmente via health checks.
- **Runbook oncall**: No aplica (entorno local, single user). Comandos de diagnostico iniciales:

  ```bash
  # Health del backend
  curl -s http://localhost:8080/q/health | jq .

  # Estado de Qdrant
  curl -s http://localhost:6333/healthz

  # Estado de PostgreSQL
  docker compose exec postgres pg_isready

  # Logs recientes del backend
  docker compose logs --tail=50 backend
  ```

---

## 8. Rollback

- **Estrategia**: Revertir a imagen Docker anterior en GHCR.
- **Comando exacto**:

  ```bash
  # 1. Detener stack actual
  docker compose down

  # 2. Cambiar tag en docker-compose.yml a la version anterior
  #    Ej: ghcr.io/breisnerlopez/abax-memory:v1.0.0

  # 3. Pull y levantar version anterior
  docker compose pull backend
  docker compose up -d

  # 4. Verificar health
  curl -s http://localhost:8080/q/health | jq .status
  ```

- **Rollback rapido (sin cambiar compose file)**:

  ```bash
  # Si la imagen anterior aun esta en cache local
  docker compose down
  docker tag ghcr.io/breisnerlopez/abax-memory:v1.0.0 ghcr.io/breisnerlopez/abax-memory:v2.0.0
  docker compose up -d
  ```

- **Probado en staging**: ⚠️ **No probado aun** — El entorno de staging es el mismo que produccion (localhost unificado). Se recomienda probar rollback con v1.0.0 antes del go-live de v2.0.0.
- **Tiempo estimado de rollback (RTO)**: ≤ 5 minutos (bajar contenedores + subir version anterior + health check).
- **Ventana de rollback**: En cualquier momento. Sin perdida de datos porque PostgreSQL y Qdrant usan volumenes persistentes.

---

## 9. Backup + restore

- **PostgreSQL**:
  - **Estrategia**: Dump completo antes del despliegue de v2.0.0.
  - **Comando backup**:
    ```bash
    docker compose exec postgres pg_dump -U pmoa pmoadb > backup_v1.0.0_$(date +%Y%m%d_%H%M%S).sql
    ```
  - **Comando restore**:
    ```bash
    docker compose exec -T postgres psql -U pmoa pmoadb < backup_v1.0.0_YYYYMMDD_HHMMSS.sql
    ```
  - **RPO (Recovery Point Objective)**: 0 (backup inmediatamente antes del deploy).
  - **RTO (Recovery Time Objective)**: ≤ 10 minutos (restore + verificacion).
  - **Ubicacion backups**: Directorio local `./backups/`. No se commitean a Git.
  - **Restore probado**: ⚠️ **No probado aun** — Probar restore en entorno aislado antes del go-live.

- **Qdrant**:
  - **Estrategia**: Qdrant snapshots (si estan habilitados) o rebuild desde PostgreSQL.
  - **RPO**: 0 — los vectores pueden re-generarse desde PostgreSQL si es necesario.
  - **Nota**: El defecto UAT-BUG-F1 (indice Qdrant vacio) requiere crear el indice antes del despliegue. Ver Seccion 13.

- **Volumenes Docker**: Los volumenes persistentes (`postgres_data`, `qdrant_data`) no se eliminan durante el deploy. En caso de fallo catastrofico, restaurar desde backup SQL.

---

## 10. Comunicacion

- **Stakeholders internos**: Chat directo con el sponsor (usuario unico del MVP). No hay equipo ampliado.
- **Usuarios finales**: No aplica (MVP local, usuario unico = sponsor).
- **Pagina de status**: No aplica para MVP local. GitHub Pages muestra documentacion.
- **Plan en caso de incidente**:
  1. El sponsor detecta el incidente (health check falla, endpoint no responde).
  2. Ejecutar comandos de diagnostico (Seccion 7).
  3. Si el incidente es grave: ejecutar rollback (Seccion 8).
  4. Notificar al developer via chat/issue en GitHub.

- **Canales**:
  | Evento | Canal | Audiencia |
  |---|---|---|
  | Inicio de despliegue | Chat con sponsor | Sponsor |
  | Despliegue completado | Chat con sponsor + verificacion health | Sponsor |
  | Rollback (si ocurre) | Chat con sponsor + GitHub issue | Sponsor + Developer |
  | Go-live confirmado | Chat con sponsor | Sponsor |

---

## 11. Compliance + auditoria

- **Regulacion aplicable**: N/A para MVP local. No se manejan datos personales de terceros.
- **Audit log**: El backend registra eventos de cambios en la tabla `audit_events` (trazabilidad de memorias). Retencion: indefinida (mientras dure la BD).
- **Datos sensibles**:
  - **API Key de OpenAI**: Almacenada en `.env` (excluida de Git). Encriptacion en reposo: depende del filesystem del host. En transito: TLS de OpenAI API.
  - **PII**: No se almacenan datos personales identificables en el MVP. Las memorias contienen texto operativo/tecnico.
- **Tratamiento de PII**: No aplica. Si en el futuro se almacenan datos personales, implementar anonimizacion y residencia geografica.
- **GDPR / HIPAA / PCI / SOC2**: No aplican para el alcance actual del MVP.

---

## 12. SLO/SLA

- **Disponibilidad target**: 99.0% (localhost, single user). Definicion: backend responde a health checks durante el horario de uso del sponsor.
- **Latencia p95 target en endpoints criticos**:
  | Endpoint | p95 Target | Medicion UAT (S10) |
  |---|---|---|
  | `POST /api/v2/search/semantic` | < 500ms | 7.4ms avg ⚠️ (caveat: Qdrant sin indexar) |
  | `POST /api/v2/memories` | < 2s (incluye embedding OpenAI) | ~2-3.5s (esperado por OpenAI) |
  | `GET /api/v2/memories/{id}` | < 100ms | No medido en UAT |
  | `GET /api/v2/memories` | < 200ms | No medido en UAT |

- **Error budget**:
  - Disponibilidad 99.0% → 7.2h/mes de downtime permitido.
  - Politica de freeze: Si se consumen > 50% del error budget en una semana, congelar nuevos deploys hasta diagnosticar causa.
  - **Nota**: Para MVP local con usuario unico, el error budget es amplio. No se requiere politica estricta de freeze en esta etapa.

- **Responsable del cumplimiento del SLO**: devops (monitoreo) + sponsor (reporte de incidencias).

- **SLO de busqueda semantica**: p95 < 500ms. **Condicion**: requiere que el indice Qdrant este correctamente creado (ver Seccion 13).

---

## 13. Paso previo critico — Creacion de indice Qdrant

> **🚨 BLOQUEANTE**: El defecto UAT-BUG-F1 detecto que la coleccion `abax-memories-v2` tiene 175 puntos pero **0 vectores indexados**. La busqueda semantica retorna 0 resultados. Este paso debe ejecutarse **ANTES** de considerar el despliegue como exitoso.

### Comando de creacion de indice

```bash
curl -X POST http://localhost:6333/collections/abax-memories-v2/index \
  -H 'Content-Type: application/json' \
  -d '{"field_name": "content", "field_schema": "text"}'
```

### Verificacion post-creacion

```bash
# Verificar que el indice se creo correctamente
curl -s http://localhost:6333/collections/abax-memories-v2 | jq '.result.config.params.vectors'

# Verificar que los vectores estan indexados
curl -s http://localhost:6333/collections/abax-memories-v2 | jq '.result.indexed_vectors_count'
# Esperado: > 0 (debe reflejar los 175 puntos almacenados)

# Probar busqueda semantica
curl -s -X POST http://localhost:8080/api/v2/search/semantic \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tenant-alpha' \
  -d '{"query": "database migration"}' | jq '.results | length'
# Esperado: > 0 resultados
```

### Impacto si no se ejecuta

- UAT-S02 permanece en FAIL.
- La funcionalidad estrella del producto (busqueda semantica) no funciona.
- El despliegue NO puede declararse exitoso sin este paso.

---

## Checklist Pre-Go-Live

> Ejecutar en orden el dia del despliegue. Si algun item falla, **ABORTAR** y notificar al sponsor.

### Bloque A — Pre-deploy

| # | Item | Estado | Evidencia |
|---|---|---|---|
| A-01 | Plan de despliegue aprobado por sponsor | ⬜ Pendiente | Firma en este documento (Seccion 14) |
| A-02 | Indice Qdrant creado (Seccion 13) | ⬜ Pendiente | `indexed_vectors_count > 0` |
| A-03 | Busqueda semantica funcional (re-test UAT-S02) | ⬜ Pendiente | `POST /api/v2/search/semantic` retorna > 0 resultados |
| A-04 | Backup PostgreSQL ejecutado | ⬜ Pendiente | Archivo `backup_v1.0.0_*.sql` en `./backups/` |
| A-05 | `.env` configurado con `OPENAI_API_KEY` valida | ⬜ Pendiente | Health check `/q/health` incluye OpenAI UP |
| A-06 | Docker Compose file validado (`docker compose config`) | ⬜ Pendiente | Sin errores de sintaxis |
| A-07 | Imagen Docker v2.0.0 construida localmente o pull desde GHCR | ⬜ Pendiente | `docker images \| grep abax-memory` |
| A-08 | Rollback probado (o plan de rollback revisado) | ⬜ Pendiente | Simulacion o revision documental |

### Bloque B — Deploy

| # | Item | Estado | Evidencia |
|---|---|---|---|
| B-01 | `docker compose down` (detener stack anterior si aplica) | ⬜ Pendiente | Contenedores detenidos |
| B-02 | `docker compose up -d` (levantar v2.0.0) | ⬜ Pendiente | Contenedores UP |
| B-03 | Esperar 30s para startup completo | ⬜ Pendiente | — |

### Bloque C — Post-deploy (smoke tests)

| # | Item | Estado | Evidencia |
|---|---|---|---|
| C-01 | Health check `/q/health` → UP | ⬜ Pendiente | `{"status":"UP"}` |
| C-02 | Health check `/q/health/ready` → UP (incluye BD, Qdrant, OpenAI) | ⬜ Pendiente | `{"status":"UP"}` |
| C-03 | PostgreSQL accesible | ⬜ Pendiente | `pg_isready` → accepting connections |
| C-04 | Qdrant health `/healthz` → OK | ⬜ Pendiente | `healthz check passed` |
| C-05 | Busqueda semantica funcional (re-test UAT-S02) | ⬜ Pendiente | > 0 resultados |
| C-06 | CRUD de memoria funcional (re-test UAT-S01) | ⬜ Pendiente | POST 201 + GET 200 |
| C-07 | Tenant isolation funcional (re-test UAT-S07) | ⬜ Pendiente | Cross-tenant → 404 |
| C-08 | Sin errores FATAL/ERROR en logs (ultimos 2 min) | ⬜ Pendiente | `docker compose logs --tail=30 backend` |
| C-09 | Rate limiting activo | ⬜ Pendiente | 30+ requests rapidos no rompen el backend |
| C-10 | OpenAPI spec accesible | ⬜ Pendiente | `GET /q/openapi` → 200, OpenAPI 3.0.3 |

---

## 14. Aprobacion del sponsor

- [ ] El sponsor confirma que las 12 secciones tienen respuesta concreta.
- [ ] El sponsor aprueba la URL final (`http://localhost:8080`).
- [ ] El sponsor comprende que no hay exposicion publica en este MVP (localhost).
- [ ] El sponsor aprueba el plan de comunicacion (chat directo).
- [ ] El sponsor aprueba el paso previo critico de creacion de indice Qdrant (Seccion 13).
- [ ] Fecha de go-live: `___________`
- [ ] Sponsor: `___________`
- [ ] Fecha aprobacion: `___________`

---

## Matriz de riesgos — Fase 7 Despliegue v2.0.0

| ID | Riesgo | Probabilidad | Impacto | Mitigacion | Estado |
|---|---|---|---|---|---|
| R7v2-01 | Indice Qdrant no creado → busqueda semantica no funciona | **Alta** (ya detectado en UAT) | Critico | Crear indice como paso previo bloqueante (Seccion 13). Verificar post-creacion. | **Vigente** |
| R7v2-02 | OpenAI API no disponible durante despliegue | Baja | Alto | Health check `/q/health/ready` incluye OpenAI. Timeouts configurados. Degradacion graceful pendiente. | Vigente |
| R7v2-03 | Exposicion de API key de OpenAI | Baja | Critico | `.env` en `.gitignore`. Rotacion programada. Verificacion pre-deploy. | Vigente |
| R7v2-04 | Incompatibilidad de imagen Docker entre build y runtime | Baja | Medio | Build local + test pre-deploy. Mismo entorno (localhost). | Vigente |
| R7v2-05 | Perdida de datos en PostgreSQL por migraciones Flyway | Baja | Alto | Backup pre-deploy. Flyway maneja versionado. Rollback con restore de backup. | Vigente |
| R7v2-06 | Falla en rate limiting → abuso de API | Baja | Medio | Rate limiter presente. UAT-S09 verifico funcionalidad basica. Ajustar umbral en produccion. | Vigente |
| R7v2-07 | Regresion funcional no detectada en UAT | Media | Alto | Smoke tests post-deploy cubren escenarios criticos (S01, S02, S07). Checklist Bloque C. | Vigente |
| R7v2-08 | Rollback no probado → falla en recuperacion | Media | Alto | Probar rollback antes del go-live. Documentar procedimiento exacto (Seccion 8). | Vigente |

---

## Glosario

- **GHCR**: GitHub Container Registry — registro de imagenes Docker asociado al repositorio GitHub.
- **OIDC**: OpenID Connect — protocolo de autenticacion sobre OAuth 2.0 usado por Keycloak.
- **RTO**: Recovery Time Objective — tiempo maximo aceptable para recuperar el servicio tras una falla.
- **RPO**: Recovery Point Objective — cantidad maxima de datos que se acepta perder tras una falla.
- **SLO**: Service Level Objective — objetivo medible de rendimiento del servicio (ej. p95 < 500ms).
- **p95**: Percentil 95 — el 95% de las requests deben completarse por debajo de este umbral.
- **Flyway**: Herramienta de migracion de base de datos versionada para PostgreSQL.
