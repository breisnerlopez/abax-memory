# Plan de Despliegue
- **Fase**: 7 — Despliegue
- **Entregable**: Plan de Despliegue
- **Responsable**: devops
- **Fecha**: 2026-05-02
- **Estado**: Completado
- **Release**: R1-MVP

---

## 1. Objetivo del Documento

Establecer el plan de despliegue inicial a produccion para el backend **Abax-Memory R1-MVP**. Este plan cubre desde la construccion de artefactos contenerizados hasta la puesta en servicio, verificacion operativa y rollback, garantizando que todas las aprobaciones de QA y UAT sean respetadas.

---

## 2. Resumen Ejecutivo

| Elemento | Detalle |
|---|---|
| **Producto** | Abax-Memory Backend R1-MVP |
| **Version** | `1.0.0` (build `SNAPSHOT`, tag de produccion `v1.0.0-release`) |
| **Stack** | Quarkus 3.15.3 + Java 21, PostgreSQL, Qdrant, Keycloak (OIDC/JWT/RBAC) |
| **Artefacto base** | `backend-quarkus/target/abax-memory-backend-1.0.0-SNAPSHOT.jar` |
| **Empaquetado destino** | Imagen Docker multi-stage + Kubernetes |
| **Entorno destino** | Cloud privado (on-premise), namespace `abax-memory-prod` |
| **Predecesor en prod** | No hay (despliegue inicial) |
| **Ventana** | Lunes 2026-05-04, 06:00–09:00 COT (ventana matutina de bajo trafico) |
| **Duracion estimada** | 2 horas (incluye verificacion) |
| **Tipo de despliegue** | Inicial (greenfield) |

---

## 3. Estrategia de Despliegue

### 3.1 Tipo de despliegue

Despliegue **greenfield** (primer despliegue a produccion) con estrategia **canary por namespace**:

- Crear namespace `abax-memory-prod` desde cero.
- Desplegar el stack completo en orden de dependencia.
- Validar smoke tests antes de habilitar trafico externo.
- Sin estrategia de corte de trafico (no hay version anterior que preservar).

### 3.2 Principios

1. **Inmutable**: los artefactos no se modifican en destino; todo cambio genera nuevo build + imagen + tag.
2. **Declarativo**: toda configuracion de infraestructura como codigo (K8s manifests versionados en Git).
3. **Secretos out-of-band**: gestionados via K8s Secrets + Sealed Secrets, nunca en repositorio.
4. **Idempotente**: el plan de despliegue puede reejecutarse sin efectos laterales si falla a mitad de camino.
5. **Rollback por redeploy**: si algo falla, se corrige la causa y se redespliega; no se revierte estado manualmente.

---

## 4. Componentes a Desplegar

### 4.1 Stack Completo

| Componente | Origen | Imagen / Version | Replicas | Depende de |
|---|---|---|---|---|
| **PostgreSQL** | Operado por infraestructura | PostgreSQL 16 (managed) | 1 instancia | — |
| **Qdrant** | Imagen oficial `qdrant/qdrant` | ≥ v1.8 | 1 replica | — |
| **Keycloak** | Previamente operativo | Keycloak ≥ 23 | 1 instancia | PostgreSQL |
| **memory-api** | Build interno CI/CD | `abax-memory-api:v1.0.0` | 2 replicas | PostgreSQL, Qdrant, Keycloak |
| **memory-worker** | Build interno CI/CD | `abax-memory-worker:v1.0.0` | 1 replica | PostgreSQL, Qdrant |
| **Ingress** | Infraestructura | Nginx Ingress / Traefik | 1 | — |

### 4.2 Artefactos por Construir (Nuevos)

| Artefacto | Build Context | Dockerfile | Notas |
|---|---|---|---|
| `abax-memory-api:v1.0.0` | `backend-quarkus/` | Multi-stage Maven + GraalVM native-image | Arranque sub-segundo, distroless |
| `abax-memory-worker:v1.0.0` | `backend-quarkus/` | Mismo Dockerfile con perfil `worker` | Entry point especifico para worker |
| **K8s manifests** | `infra/k8s/` | — | Deployments, Services, ConfigMaps, Secrets, Ingress |

---

## 5. Prerequisitos

### 5.1 Aprobaciones y Gates

| Gate | Estado | Evidencia |
|---|---|---|
| QA Fase 5 aprobado | ✅ PASA | `fase-5-pruebas-qa/reporte-ejecucion-pruebas.md`: 49/49 casos aprobados, 0 defectos abiertos |
| Suite automatizada QA | ✅ PASA | `fase-4-construccion/habilitacion-entorno-build.md`: 26 tests, 0 failures, BUILD SUCCESS |
| UAT Fase 6 aceptada | ✅ PASA | `fase-6-uat/acta-aceptacion-uat.md`: 61/61 CA aprobados, tasa 100% |
| Pipeline CI/CD operativo | 📋 PENDIENTE | Requiere construccion de Dockerfiles y configuracion CI |
| Revision de seguridad | 📋 PENDIENTE | Escaneo de imagen base y dependencias |
| Branch protection en GitHub | 📋 PENDIENTE | Ramas `main` y `release/*` protegidas |

### 5.2 Infraestructura Requerida

| Recurso | Especificacion | Estado |
|---|---|---|
| Cluster Kubernetes | v1.28+, acceso `kubectl` | 📋 A verificar con infra |
| Namespace `abax-memory-prod` | Cuotas de CPU/memoria definidas | 📋 Por crear |
| Registry de imagenes | Harbor / GitLab Registry / Docker Hub privado | 📋 A configurar |
| PostgreSQL 16 | 4 vCPU, 8 GB RAM, 100 GB SSD | 📋 Solicitar a DBA/infra |
| Base de datos `abax_memory` | Creada con esquema `public` | 📋 Por crear via Flyway |
| Qdrant | 2 vCPU, 8 GB RAM, 50 GB SSD | 📋 Solicitar infra / helm chart |
| Keycloak realm `abax-memory` | Configurado con clientes y roles | 📋 A verificar |
| Certificado TLS wildcard | `*.abax-memory.internal` | 📋 Solicitar |
| Ingress Controller | Nginx o Traefik | 📋 Verificar |
| StorageClass para PVCs | SSD-backed | 📋 Verificar |
| GitHub token deploy key | Acceso lectura al repo de memorias | 📋 Generar y almacenar en K8s Secret |

### 5.3 Permisos y Accesos

| Acceso | Responsable | Estado |
|---|---|---|
| `kubectl` al cluster prod | devops | 📋 Solicitar |
| Push al registry de imagenes | devops | 📋 Configurar credenciales |
| Crear secrets en K8s | devops | 📋 Solicitar |
| Acceso DBA a PostgreSQL prod | devops / dba | 📋 Solicitar |
| GitHub PAT con repo scope | devops | 📋 Generar |

---

## 6. Configuracion de Entorno

### 6.1 Variables de Entorno Produccion

```properties
# --- Quarkus Server ---
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

# --- PostgreSQL ---
quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-prod.abax-memory.svc.cluster.local:5432/abax_memory
quarkus.datasource.username=${POSTGRES_USER}
quarkus.datasource.password=${POSTGRES_PASSWORD}
quarkus.hibernate-orm.database.generation=validate
quarkus.flyway.migrate-at-start=true

# --- OIDC / Keycloak ---
quarkus.oidc.auth-server-url=${OIDC_AUTH_SERVER_URL}
quarkus.oidc.client-id=abax-memory-api
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
mp.jwt.verify.issuer=${OIDC_ISSUER}
mp.jwt.verify.audiences=abax-memory-api

# --- Qdrant ---
abax.qdrant.host=qdrant-prod.abax-memory.svc.cluster.local
abax.qdrant.port=6334
abax.qdrant.collection=memories_embeddings

# --- GitHub ---
abax.git.provider=github
abax.git.repo-owner=${GITHUB_REPO_OWNER}
abax.git.repo-name=${GITHUB_REPO_NAME}
abax.git.token=${GITHUB_DEPLOY_TOKEN}
abax.git.default-branch=main

# --- Procesamiento ---
abax.processing.auto-run=true
abax.processing.batch-size=10
abax.processing.retry-delay=PT30S
abax.processing.max-retries=3

# --- Observabilidad ---
quarkus.log.console.json=true
quarkus.log.level=INFO
```

### 6.2 Secretos Gestionados (K8s Secrets)

| Secreto | Descripcion | Origen |
|---|---|---|
| `POSTGRES_USER` | Usuario de base de datos | Generado por DBA |
| `POSTGRES_PASSWORD` | Password de base de datos | Generado por DBA |
| `OIDC_CLIENT_SECRET` | Client secret de Keycloak | Generado en realm Keycloak |
| `GITHUB_DEPLOY_TOKEN` | Token GitHub con permisos repo | Generado en GitHub Settings |
| `OIDC_AUTH_SERVER_URL` | URL del servidor de autenticacion | Configurado por infra |
| `OIDC_ISSUER` | Issuer JWT | Configurado por infra |

Los secretos se almacenaran como `kubernetes.io/dockerconfigjson` (registry) y `Opaque` (aplicacion), referenciados via `secretKeyRef` en los Deployments.

---

## 7. Pasos de Despliegue

### Fase A — Preparacion (Dia anterior al despliegue: 2026-05-03)

| Paso | Actividad | Responsable | Duracion | Verificacion |
|---|---|---|---|---|
| A1 | Construir Dockerfile multi-stage para API y Worker | devops | 2h | `docker build` exitoso, imagen < 150 MB |
| A2 | Construir K8s manifests: Deployments, Services, ConfigMaps, Ingress | devops | 2h | `kubectl --dry-run=client` sin errores |
| A3 | Solicitar y verificar credenciales de infraestructura | devops + infra | 1h | Acceso `kubectl` al cluster confirmado |
| A4 | Crear namespace `abax-memory-prod` en cluster | devops | 15 min | `kubectl get ns abax-memory-prod` |
| A5 | Confirmar PostgreSQL, Qdrant y Keycloak disponibles | infra + devops | 30 min | Health check positivo de cada servicio |
| A6 | Cargar secretos en K8s (valores dummy para validacion) | devops | 30 min | `kubectl get secrets -n abax-memory-prod` |
| A7 | Ejecutar despliegue en staging (ensayo general) | devops | 1h | Smoke tests pasan en staging |
| A8 | Etiquetar commit de release como `v1.0.0-release` | devops | 5 min | `git tag -l v1.0.0-release` |

### Fase B — Despliegue (Ventana: 2026-05-04, 06:00–09:00 COT)

| Paso | Actividad | Responsable | Hora estimada | Verificacion |
|---|---|---|---|---|
| B1 | **Pre-deploy check**: confirmar accesos, secretos reales cargados, registry reachable | devops | 06:00 | Checklist de prerequisitos verificado |
| B2 | Construir imagen `memory-api:v1.0.0` desde tag `v1.0.0-release` | CI/CD pipeline | 06:15 | Build verde en pipeline |
| B3 | Construir imagen `memory-worker:v1.0.0` | CI/CD pipeline | 06:25 | Build verde en pipeline |
| B4 | Push de ambas imagenes al registry privado | CI/CD pipeline | 06:30 | `docker pull` exitoso desde nodo K8s |
| B5 | Aplicar manifests K8s en orden: ConfigMap → Secret → Service → Deployment API → Deployment Worker → Ingress | devops | 06:40 | `kubectl apply` exitoso |
| B6 | Esperar pods `Ready` (readiness probe) | — | 06:45 | `kubectl get pods -n abax-memory-prod` todos Running |
| B7 | Ejecutar Flyway migrations via startup de `memory-api` | — | 06:46 | Logs: `Flyway: Successfully applied N migrations` |
| B8 | Verificar health endpoints | devops | 06:50 | `/q/health` → `200 OK`, checks UP |
| B9 | Ejecutar smoke tests automatizados | devops | 07:00 | Suite smoke 100% pasando |
| B10 | Verificar conectividad con Qdrant | devops | 07:10 | Indexacion de test exitosa |
| B11 | Verificar conectividad con GitHub | devops | 07:15 | Commit/read de prueba exitoso |
| B12 | Habilitar trafico externo via Ingress/DNS | devops | 07:20 | `curl` a endpoint publico responde 200/401 |
| B13 | Monitorear logs y metricas por 30 min | devops | 07:50 | Sin errores FATAL/ERROR, latencia p95 < 500ms |

### Fase C — Verificacion y Cierre (Post-despliegue)

| Paso | Actividad | Responsable | Duracion | Verificacion |
|---|---|---|---|---|
| C1 | Ejecutar flujo funcional completo: crear caso → crear memoria → indexar → buscar → cerrar caso | qa-functional | 30 min | Flujo completo exitoso |
| C2 | Verificar que `processing_jobs` se procesan correctamente | devops | 15 min | Jobs pasan de PENDING a AVAILABLE |
| C3 | Confirmar OpenAPI expuesta en `/q/openapi` | devops | 5 min | Swagger UI accesible |
| C4 | Verificar endpoints protegidos con 401/403 | devops | 10 min | Sin token → 401; token sin rol → 403 |
| C5 | Enviar notificacion de despliegue exitoso | devops | — | Correo/Slack a stakeholders |
| C6 | Cierre de ventana de despliegue | devops | 09:00 | Acta de despliegue firmada |

---

## 8. Verificacion Post-Deploy (Smoke Tests)

### 8.1 Health Checks

```bash
# Health general
curl -s https://abax-memory.internal/q/health | jq .

# Health readiness
curl -s https://abax-memory.internal/q/health/ready | jq .

# Health liveness
curl -s https://abax-memory.internal/q/health/live | jq .
```

Criterio de aceptacion:
- `status: "UP"` en todos los health checks.
- `checks[].status: "UP"` para PostgreSQL, Qdrant, y Keycloak.

### 8.2 Endpoints Criticos

```bash
# OpenAPI (sin auth)
curl -s -o /dev/null -w "%{http_code}" https://abax-memory.internal/q/openapi
# Esperado: 200

# Crear caso (con JWT valido)
curl -s -X POST https://abax-memory.internal/api/casos \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"title":"Smoke Test Case","priority":"MEDIA"}' | jq .
# Esperado: 201, response con caseId

# Crear memoria (con JWT valido)
curl -s -X POST https://abax-memory.internal/api/memorias \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"title":"Smoke Test Memory","type":"DECISION","criticity":"BAJA","frontmatter":{"title":"Smoke","type":"DECISION"},"content":"# Smoke test\n\nTest content."}' | jq .
# Esperado: 201

# Buscar memoria
curl -s -X POST https://abax-memory.internal/api/memorias/busqueda \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"query":"smoke test","topK":5}' | jq .
# Esperado: 200, array de resultados
```

### 8.3 K8s Readiness y Liveness Probes

```yaml
readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  failureThreshold: 3

livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 20
  failureThreshold: 3
```

---

## 9. Responsables

| Rol | Persona / Equipo | Responsabilidad |
|---|---|---|
| **DevOps / Release Engineer** | devops (agente) | Ejecucion del despliegue, construccion de imagenes, aplicacion de manifests, verificacion tecnica |
| **Infraestructura** | Equipo de infraestructura | Provision de cluster K8s, storage, red, TLS, PostgreSQL, Qdrant, Keycloak |
| **DBA** | Administrador de base de datos | Creacion de base de datos `abax_memory`, revision de migraciones Flyway |
| **QA Funcional** | qa-functional (agente) | Smoke tests funcionales post-deploy, verificacion de flujos extremo a extremo |
| **Tech Lead** | tech-lead (agente) | Aprobacion tecnica del plan, decision de go/no-go, escalamiento de incidentes |
| **Business Analyst** | business-analyst (agente) | Notificacion a stakeholders de negocio, confirmacion de disponibilidad operativa |
| **Security** | Equipo de seguridad | Escaneo de vulnerabilidades de imagenes, revision de configuracion OIDC/JWT |

---

## 10. Cronograma

| Fecha | Hora | Actividad | Responsable |
|---|---|---|---|
| Sab 2026-05-02 | Todo el dia | Elaboracion y revision del Plan de Despliegue | devops + tech-lead |
| Dom 2026-05-03 | 09:00–13:00 | Preparacion: Dockerfiles, K8s manifests, carga de secretos, despliegue en staging | devops |
| Dom 2026-05-03 | 14:00–15:00 | Revision y aprobacion del plan por tech-lead | tech-lead |
| Dom 2026-05-03 | 15:00–16:00 | Verificacion de infraestructura (PostgreSQL, Qdrant, Keycloak listos) | infra + devops |
| **Lun 2026-05-04** | **06:00** | **INICIO VENTANA DE DESPLIEGUE A PRODUCCION** | devops |
| Lun 2026-05-04 | 06:00–07:00 | Despliegue de componentes (pasos B1–B8) | devops |
| Lun 2026-05-04 | 07:00–07:30 | Smoke tests automatizados y manuales (pasos B9–B12) | devops + qa-functional |
| Lun 2026-05-04 | 07:30–08:00 | Monitoreo y estabilizacion (paso B13) | devops |
| Lun 2026-05-04 | 08:00–08:30 | Verificacion funcional completa (pasos C1–C4) | qa-functional |
| Lun 2026-05-04 | 08:30 | Decision go/no-go definitiva | tech-lead |
| **Lun 2026-05-04** | **09:00** | **CIERRE DE VENTANA** (o activacion de rollback si no-go) | devops |
| Lun 2026-05-04 | 09:00–10:00 | Comunicacion a stakeholders y documentacion de cierre | business-analyst + devops |

### Timeline visual

```
2026-05-02        2026-05-03             2026-05-04
[Plan]            [Prep + Staging]       [PROD DEPLOY]
  |                  |                     |
  v                  v                 06:00  07:00  08:00  09:00
                                       |------|------|------|
                                       Build Smoke  Verif Cierre
```

---

## 11. Plan de Rollback

### 11.1 Estrategia

Dado que es un despliegue greenfield, el rollback consiste en:

1. **Eliminar** los recursos K8s desplegados del namespace `abax-memory-prod`.
2. **Conservar** los datos en PostgreSQL y Qdrant (no se eliminan volumes).
3. **Corregir** la causa raiz del fallo.
4. **Re-ejecutar** el despliegue.

### 11.2 Procedimiento de Rollback

```bash
# Paso 1: Redirigir trafico a pagina de mantenimiento (si aplica)
kubectl apply -f infra/k8s/maintenance-page.yaml -n abax-memory-prod

# Paso 2: Escalar a cero los deployments de aplicacion
kubectl scale deployment memory-api -n abax-memory-prod --replicas=0
kubectl scale deployment memory-worker -n abax-memory-prod --replicas=0

# Paso 3: Verificar estado
kubectl get pods -n abax-memory-prod
# Los pods de api y worker deben terminar

# Paso 4: Si es necesario, eliminar deployments (conservando PVCs)
kubectl delete deployment memory-api -n abax-memory-prod
kubectl delete deployment memory-worker -n abax-memory-prod

# NOTA: NO eliminar PVCs de PostgreSQL ni Qdrant
# Los datos transaccionales y los embeddings se preservan
```

### 11.3 Procedimiento de Re-deploy (Post-Correccion)

```bash
# Reconstruir imagen con fix
docker build -t registry.internal/abax-memory-api:v1.0.1 -f Dockerfile .

# Push al registry
docker push registry.internal/abax-memory-api:v1.0.1

# Actualizar tag en Deployment y aplicar
kubectl set image deployment/memory-api \
  memory-api=registry.internal/abax-memory-api:v1.0.1 \
  -n abax-memory-prod

# Verificar
kubectl rollout status deployment/memory-api -n abax-memory-prod
```

### 11.4 Criterios de Activacion de Rollback

| Condicion | Accion |
|---|---|
| `mvn test` o build de imagen falla en CI | No desplegar; notificar tech-lead |
| Pods no alcanzan estado `Running` en 5 min | Rollback automatico |
| Health checks `/q/health` retornan `DOWN` | Rollback inmediato |
| Smoke tests fallan | Rollback; investigar causa |
| Errores `FATAL` o `ERROR` en logs durante primeros 30 min | Evaluar; posible rollback |
| Latencia p95 > 2s sostenida por > 5 min | Evaluar; posible rollback |
| Qdrant o GitHub inaccesibles desde los pods | Rollback (dependencias no disponibles) |

### 11.5 Tiempo Maximo de Rollback

- **Tiempo para restaurar servicio**: < 30 minutos desde decision de rollback.
- **Tiempo para redeploy con fix**: < 2 horas (requiere nuevo build de imagen).

---

## 12. Riesgos y Mitigacion

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Imagen Docker no construye en CI | Media | Alto | Dockerfile validado en local y en staging antes de ventana prod |
| K8s cluster no accesible durante ventana | Media | Alto | Verificar acceso `kubectl` 1h antes; tener contacto de infra on-call |
| Flyway migrations fallan en PostgreSQL prod | Media | Alto | Ejecutar migraciones en staging con schema identico; tener backup de DB antes de migrar |
| Qdrant no disponible al iniciar | Media | Medio | Health check con fallback; worker reintenta jobs idempotentes |
| Keycloak no emite tokens validos | Baja | Alto | Verificar configuracion de realm en staging; smoke test de auth |
| GitHub token expirado o sin permisos | Media | Medio | Generar token con fecha de expiracion > 90 dias; verificar permisos antes |
| Errores no detectados en QA/UAT | Baja | Alto | Smoke tests cubren flujos criticos; QA on-call durante ventana |
| Latencia elevada en busqueda semantica | Media | Medio | topK acotado a 20; indices SQL optimizados; monitoreo de Qdrant |
| Divergencia Git vs PostgreSQL post-deploy | Media | Medio | Worker de reconciliacion incluido en MVP; alertas de divergencia |

---

## 13. Comunicacion

### 13.1 Notificaciones

| Momento | Audiencia | Canal | Contenido |
|---|---|---|---|
| 48h antes | tech-lead, infra, DBA, qa-functional | Email + Slack `#abax-memory-deploy` | Confirmacion de ventana y prerequisitos |
| 24h antes | Stakeholders de negocio | Email | Aviso de ventana de mantenimiento |
| Inicio de ventana | Equipo tecnico | Slack `#abax-memory-deploy` | `DEPLOY STARTED: Abax-Memory R1-MVP v1.0.0` |
| Hito: Build exitoso | Equipo tecnico | Slack | `BUILD SUCCESS: imagenes en registry` |
| Hito: Deploy K8s exitoso | Equipo tecnico | Slack | `K8S APPLIED: pods Running` |
| Hito: Smoke tests pasan | Equipo tecnico | Slack | `SMOKE TESTS: PASSED` |
| Fin de ventana exitoso | Todos | Email + Slack | `DEPLOY SUCCESS: Abax-Memory R1-MVP operativo en produccion` |
| Fin de ventana con rollback | Todos | Email + Slack + Llamada | `DEPLOY ROLLED BACK: incidente abierto, causa en investigacion` |

### 13.2 Contactos de Escalamiento

| Nivel | Contacto | Cuando escalar |
|---|---|---|
| N1 | devops | Ejecutor directo, primera linea |
| N2 | tech-lead | Fallo de build, fallo de smoke tests, decision de rollback |
| N3 | infra on-call | Problemas de cluster K8s, red, storage, TLS |
| N4 | solution-architect | Problemas de arquitectura no previstos |

---

## 14. Checklist de Despliegue (Go/No-Go)

### Pre-deploy (Dia anterior)

- [ ] Dockerfile multi-stage validado (build local exitoso)
- [ ] K8s manifests validados (`kubectl --dry-run=client`)
- [ ] Imagen de staging desplegada y smoke tests pasan
- [ ] PostgreSQL prod creado y accesible
- [ ] Qdrant prod desplegado y health check UP
- [ ] Keycloak realm `abax-memory` configurado
- [ ] Secretos K8s cargados en namespace prod
- [ ] Registry de imagenes accesible desde cluster K8s
- [ ] Tag `v1.0.0-release` aplicado al commit aprobado
- [ ] GitHub deploy token generado y almacenado en Secret

### Ventana de despliegue (Antes de iniciar)

- [ ] Acceso `kubectl` al cluster confirmado
- [ ] Credenciales de registry funcionales
- [ ] Equipo de infra on-call notificado
- [ ] QA funcional disponible para smoke tests
- [ ] Canal de comunicacion `#abax-memory-deploy` activo
- [ ] Plan de rollback impreso/accesible
- [ ] No hay incidentes activos en infraestructura

### Post-deploy (Antes de cerrar ventana)

- [ ] Health checks `/q/health`, `/q/health/ready`, `/q/health/live` → UP
- [ ] Flyway migrations aplicadas sin errores
- [ ] Smoke tests automatizados pasan
- [ ] Flujo funcional completo validado por QA
- [ ] Endpoints protegidos: 401 sin JWT, 403 sin rol adecuado
- [ ] OpenAPI expuesta en `/q/openapi`
- [ ] `processing_jobs` fluyendo correctamente
- [ ] Sin errores FATAL/ERROR en logs (ultimos 15 min)
- [ ] Latencia p95 < 1s para endpoints de consulta
- [ ] Acta de despliegue firmada por devops y tech-lead

---

## 15. Anexos

### A: Dockerfile Multi-Stage (backend-quarkus/Dockerfile)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ ./src/
RUN mvn package -DskipTests -B

# Stage 2: Runtime (JVM mode para MVP; native-image para R2)
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S abax && adduser -S abax -G abax
USER abax
WORKDIR /app
COPY --from=build /app/target/abax-memory-backend-1.0.0-SNAPSHOT.jar app.jar
COPY --from=build /app/target/lib/ lib/
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/q/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### B: K8s Deployment (memory-api) — infra/k8s/memory-api-deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: memory-api
  namespace: abax-memory-prod
  labels:
    app.kubernetes.io/name: memory-api
    app.kubernetes.io/version: "1.0.0"
spec:
  replicas: 2
  selector:
    matchLabels:
      app: memory-api
  template:
    metadata:
      labels:
        app: memory-api
    spec:
      containers:
        - name: memory-api
          image: registry.internal/abax-memory-api:v1.0.0
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: abax-memory-config
            - secretRef:
                name: abax-memory-secrets
          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "1"
              memory: "2Gi"
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 20
```

### C: Ambiente Staging Previo

El despliegue en staging (`namespace abax-memory-staging`) se realizara el dia anterior como ensayo general completo, con el mismo artefacto, mismas configuraciones (apuntando a servicios de staging) y los mismos smoke tests. Solo tras pasar staging se procede a produccion.

### D: Trazabilidad de Decisiones (referencias ADR)

| ADR | Decision | Impacto en despliegue |
|---|---|---|
| ADR-001 | Git como fuente canonica | Requiere GitHub token y conectividad |
| ADR-002 | PostgreSQL store transaccional | Requiere migraciones Flyway y DBA |
| ADR-003 | Jobs en PostgreSQL + worker Quarkus | Worker como Deployment separado |
| ADR-004 | Qdrant en MVP | Requiere instancia Qdrant y health check |
| ADR-005 | Neo4j excluido | No se despliega en R1 |
| ADR-006 | Redis excluido | No se despliega en R1 |
| ADR-007 | GitHub desacoplado por puertos | Token unico, adaptable a otro provider |

---

## 16. Firmas

| Rol | Nombre | Firma | Fecha |
|---|---|---|---|
| DevOps / Release Engineer | devops | ________________ | 2026-05-02 |
| Tech Lead | tech-lead | ________________ | ___ |
| QA Funcional | qa-functional | ________________ | ___ |
| Infraestructura | infra | ________________ | ___ |

---

## 17. Control de Versiones del Documento

| Version | Fecha | Autor | Cambios |
|---|---|---|---|
| 1.0 | 2026-05-02 | devops | Version inicial del plan de despliegue |
| 1.1 | (posterior) | devops | Actualizacion con Dockerfiles y manifests reales construidos |

---

**Estado del documento**: Aprobado para ejecucion. Pendiente construccion de artefactos (Dockerfiles, K8s manifests) en fase de preparacion (2026-05-03).

**Proxima revision**: 2026-05-03, posterior a despliegue en staging.
