# Descomposicion Tecnica de Tareas
- **Fase**: 3-Diseno Tecnico
- **Entregable**: Descomposicion Tecnica de Tareas
- **Responsable**: tech-lead
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Objetivo

Sincronizar la descomposicion tecnica de construccion del MVP de **PMOA / Abax-Memory** con la **Baseline oficial para construccion** definida en `docs/entregables/fase-3-diseno-tecnico/documento-arquitectura.md`, eliminando contradicciones previas y dejando secuencia, dependencias, DoD y riesgos totalmente alineados.

## 2. Baseline oficial aplicada a construccion

### 2.1 Alcance MVP obligatorio

El MVP **incluye**:

- API REST en **Quarkus Java 21**.
- Seguridad **OIDC/JWT/RBAC** con Keycloak.
- Persistencia operacional en **PostgreSQL**.
- Contenido canonico de memorias en **Git/GitHub**.
- Busqueda semantica en **Qdrant**.
- Asincronia con **tabla `processing_jobs` + worker Quarkus**.
- Relaciones minimas de trazabilidad en **PostgreSQL + frontmatter**.
- Aprobacion humana por **PR** para memorias de criticidad alta/critica.
- Auditoria, observabilidad y despliegue en Kubernetes.

### 2.2 Exclusiones obligatorias del MVP

Quedan **fuera del MVP**:

- **Kafka** como broker de eventos.
- **Neo4j** como store o proyeccion de grafo.
- **Redis** como cache, lock distribuido o coordinador.
- Navegacion avanzada de relaciones basada en grafo.
- Optimizaciones de cache distribuida.

### 2.3 Reglas de alineacion no negociables

1. El mecanismo asincrono oficial del MVP es **PostgreSQL `processing_jobs` + worker Quarkus**.
2. Ninguna tarea MVP debe depender de **Kafka**, **Neo4j** o **Redis**.
3. Las relaciones minimas requeridas se resuelven en **PostgreSQL y frontmatter**, no en un grafo especializado.
4. Qdrant **si forma parte del MVP** porque la busqueda semantica es alcance aprobado.
5. Las capacidades R2 deben prepararse **por contrato**, sin introducir infraestructura fuera de baseline en R1.

## 3. Distribucion oficial por release

| Capacidad | MVP | Release posterior |
|---|---|---|
| API REST Quarkus | Si | Evolutivos |
| Git/GitHub + PR manual | Si | Hardening multi-provider |
| PostgreSQL transaccional | Si | Optimizaciones |
| `processing_jobs` + worker Quarkus | Si | Escalado / migracion futura |
| Qdrant | Si | Reindexaciones avanzadas |
| Relaciones basicas en PostgreSQL | Si | Evoluciones funcionales |
| Kafka / mensajeria dedicada | No | Evaluacion R2/R3 |
| Neo4j | No | Posible R2 |
| Redis | No | Posible R2 |
| Navegacion avanzada de grafo | No | R2 |
| Portabilidad Git ampliada | No | R2 |

## 4. Modulos tecnicos y responsabilidad

| Modulo | Responsabilidad | Tecnologia principal | Release |
|---|---|---|---|
| API Layer | Endpoints, contratos, validacion y errores HTTP | Quarkus REST + OpenAPI | MVP |
| Auth/RBAC | JWT, roles y autorizacion | Keycloak/OIDC + MP JWT | MVP |
| Case Service | Gestion de casos y trazabilidad origen | Quarkus + PostgreSQL | MVP |
| Memory Service | Alta, actualizacion, archivado y estados | Quarkus + PostgreSQL + Git | MVP |
| Validation Engine | Reglas de negocio, criticidad y consistencia | Servicios de dominio | MVP |
| Git Orchestrator | Branch, commit, push, PR y referencias | Puertos + adapter GitHub | MVP |
| Job Manager | Registro, toma, retries y estado de jobs | PostgreSQL | MVP |
| Processing Worker | Indexacion y reconciliacion operacional | Quarkus worker | MVP |
| Search Service | Busqueda semantica con filtros | Qdrant + PostgreSQL | MVP |
| Relation Reference Service | Persistencia/consulta de relaciones basicas | PostgreSQL + frontmatter | MVP |
| Audit Service | Eventos, evidencias, commitSha y prRef | PostgreSQL | MVP |
| Observabilidad | Logs, metricas, trazas y alertas | OTel, Micrometer, health | MVP |
| Portabilidad Git | Nuevos providers Git | SPI / adapters | Post-MVP |
| Grafo avanzado | Navegacion contextual avanzada | Neo4j | Post-MVP |
| Cache distribuida | Cache y optimizacion de latencia | Redis | Post-MVP |
| Broker dedicado | Desacoplamiento de alto volumen | Kafka | Post-MVP |

## 5. Estructura de trabajo recomendada

```text
backend-quarkus/
└── src/main/java/.../
    ├── resource/
    ├── service/
    ├── domain/
    ├── persistence/postgres/
    ├── integration/git/
    ├── integration/qdrant/
    ├── integration/keycloak/
    ├── worker/
    ├── audit/
    ├── security/
    └── exception/
```

## 6. Descomposicion tecnica de tareas

> Estimacion orientativa en dias-persona. Todas las tareas incluyen revision tecnica y evidencia verificable.

| ID | Tarea | Componente | Descripcion tecnica | Dependencias | Est. | Release |
|---|---|---|---|---|---:|---|
| DT-01 | Bootstrap Quarkus y estandares base | Plataforma | Crear base del servicio, perfiles, configuracion externa, paquetes y dependencias core | Ninguna | 2 | MVP |
| DT-02 | OpenAPI y contratos iniciales | API | Definir contratos REST para casos, memorias, busqueda, archivado, auditoria, webhooks y errores | DT-01 | 2 | MVP |
| DT-03 | Modelo de dominio y maquina de estados | Dominio | Modelar Caso, Memoria, Version, ValidationRequest, ProcessingJob, AuditEvent y estados operativos oficiales | DT-01 | 3 | MVP |
| DT-04 | Esquema PostgreSQL y migraciones | Persistencia | Implementar tablas, constraints, indices y migraciones para metadata, auditoria, relaciones basicas y jobs | DT-03 | 3 | MVP |
| DT-05 | Repositorios PostgreSQL | Persistencia | Implementar acceso a datos para casos, memorias, versiones, auditoria, validation_requests, memory_relation_ref y processing_jobs | DT-04 | 3 | MVP |
| DT-06 | Modelo canonico Markdown + frontmatter | Canonico | Implementar parser/render, serializacion estable y validaciones de contenido canonico | DT-03 | 2 | MVP |
| DT-07 | AuthN/AuthZ y RBAC | Seguridad | Integrar Keycloak/OIDC, JWT, roles y propagacion de identidad | DT-02, DT-01 | 3 | MVP |
| DT-08 | Manejo estandar de errores | Cross-cutting | Implementar taxonomy de errores, exception mappers y payload consistente con correlationId | DT-02 | 2 | MVP |
| DT-09 | Observabilidad base | Operacion | Logs JSON, metricas, trazas, correlationId y health checks iniciales | DT-01 | 3 | MVP |
| DT-10 | API de casos MVP | Backend | Implementar alta y consulta de casos con auditoria y validaciones | DT-05, DT-07, DT-08 | 2 | MVP |
| DT-11 | Git provider port + adapter GitHub | Integracion | Implementar puertos Git/PR y adapter inicial GitHub desacoplado | DT-01, DT-02 | 4 | MVP |
| DT-12 | Alta manual de memoria | Backend | Implementar `POST /api/memorias`, persistencia inicial en Postgres y estado `PENDING_GIT` | DT-05, DT-06, DT-07, DT-08 | 3 | MVP |
| DT-13 | Alta de memoria desde caso | Backend | Implementar flujo desde caso con trazabilidad y reglas de existencia | DT-10, DT-12 | 2 | MVP |
| DT-14 | Persistencia Git y actualizacion de estado | Integracion | Orquestar branch/commit/push, registrar `commitSha` y transicionar a `GIT_PERSISTED` o `GIT_FAILED` | DT-11, DT-12 | 4 | MVP |
| DT-15 | Validacion de criticidad y flujo PR | Gobierno | Detectar memorias alta/critica, crear PR manual, guardar `prRef` y dejar estado `EN_REVISION` | DT-11, DT-12 | 4 | MVP |
| DT-16 | Webhook y polling de aprobacion | Integracion | Sincronizar merge/aprobacion de PR y actualizar estado a `GIT_PERSISTED` / `PENDING_INDEX` | DT-15, DT-07, DT-08 | 3 | MVP |
| DT-17 | Job Manager async oficial | Async | Registrar jobs `INDEX_MEMORY` y `RECONCILE_MEMORY`, controlar lifecycle, retries y toma segura en PostgreSQL | DT-04, DT-05, DT-14 | 3 | MVP |
| DT-18 | Worker Quarkus e idempotencia | Async | Implementar scheduler/worker, idempotencia por `memoryId + versionId + jobType` y procesamiento con `FOR UPDATE SKIP LOCKED` o equivalente | DT-17, DT-09 | 3 | MVP |
| DT-19 | Indexacion semantica y adapter Qdrant | Integracion | Generar embeddings, crear coleccion, upsert de payload y actualizar estados `PENDING_INDEX/INDEXING/AVAILABLE/INDEX_FAILED` | DT-18, DT-17 | 4 | MVP |
| DT-20 | Reconciliacion operacional | Async | Reprocesar `GIT_FAILED` e `INDEX_FAILED`, detectar drift Git/DB/Qdrant y corregir divergencias de forma idempotente | DT-18, DT-19 | 3 | MVP |
| DT-21 | Relaciones basicas de trazabilidad | Backend | Persistir y consultar `memory_relation_ref` y referencias en frontmatter sin proyeccion de grafo | DT-05, DT-06, DT-12 | 2 | MVP |
| DT-22 | Consulta detalle y listado de memorias | Backend | Implementar `GET /api/memorias/{id}` y `GET /api/memorias` con filtros, estados y metadata | DT-05, DT-07, DT-08 | 3 | MVP |
| DT-23 | Busqueda semantica MVP | API/Integracion | Implementar endpoint de busqueda usando Qdrant + filtros estructurados en PostgreSQL, sin cache distribuida | DT-19, DT-22 | 4 | MVP |
| DT-24 | API minima de relaciones | Backend | Implementar alta y consulta minima de relaciones basicas alineadas a trazabilidad MVP | DT-21, DT-22 | 2 | MVP |
| DT-25 | Archivado y ciclo de vida minimo | Backend | Implementar archivado, exclusion por defecto y auditoria del cambio de estado | DT-22, DT-14 | 2 | MVP |
| DT-26 | Auditoria extremo a extremo | Audit | Registrar actor, accion, entity, memoryId, caseId, commitSha, prRef y correlationId | DT-05, DT-07, DT-14, DT-16 | 3 | MVP |
| DT-27 | Testing unitario | Calidad | Cubrir dominio, validadores, parser, estados, Job Manager, worker e integraciones aisladas | DT-10 a DT-26 | 5 | MVP |
| DT-28 | Testing de integracion | Calidad | Validar flujos principales y negativos con RESTAssured + PostgreSQL + Qdrant + GitHub mock/webhook | DT-10 a DT-26 | 6 | MVP |
| DT-29 | Hardening operativo y despliegue | Operacion | Readiness/liveness, timeouts, configuracion por ambiente, dashboards, alertas y runbook | DT-09, DT-20, DT-28 | 4 | MVP |
| DT-30 | Evolucion de portabilidad Git | Post-MVP | Agregar adapters GitLab/Bitbucket y mapping de webhooks por provider | DT-11, DT-29 | 4 | Post-MVP |
| DT-31 | Evolucion de grafo avanzado | Post-MVP | Evaluar e implementar proyeccion Neo4j para navegacion contextual avanzada | DT-24, DT-29 | 5 | Post-MVP |
| DT-32 | Evolucion de cache distribuida | Post-MVP | Evaluar Redis para cache, locks y optimizaciones de concurrencia | DT-23, DT-29 | 4 | Post-MVP |
| DT-33 | Evolucion de broker dedicado | Post-MVP | Analizar migracion de `processing_jobs` hacia Kafka o mensajeria dedicada segun volumetria | DT-20, DT-29 | 3 | Post-MVP |

## 7. Dependencias clave y aclaraciones obligatorias

### 7.1 Dependencias bloqueantes

1. **DT-01** desbloquea la construccion base.
2. **DT-03 + DT-04 + DT-05** desbloquean persistencia, auditoria, jobs y relaciones basicas.
3. **DT-11** es prerequisito del flujo Git/GitHub y aprobaciones por PR.
4. **DT-17 + DT-18** definen el mecanismo asincrono oficial del MVP.
5. **DT-19** es bloqueante para cerrar RF de busqueda semantica MVP.
6. **DT-20** es bloqueante para la consistencia operacional minima exigida por baseline.

### 7.2 Dependencias explicitamente descartadas

- Ninguna tarea MVP depende de **Kafka**.
- Ninguna tarea MVP depende de **Neo4j**.
- Ninguna tarea MVP depende de **Redis**.
- No debe abrirse trabajo MVP para topics, brokers, consumers/producers, Bolt, cache-aside ni locks distribuidos externos.

## 8. Ruta critica MVP

```text
DT-01 -> DT-03 -> DT-04 -> DT-05 -> DT-12 -> DT-11 -> DT-14 -> DT-17 -> DT-18 -> DT-19 -> DT-20 -> DT-23 -> DT-28 -> DT-29
```

Ruta critica complementaria de gobierno:

```text
DT-02 -> DT-07 -> DT-15 -> DT-16 -> DT-26 -> DT-28
```

## 9. Secuencia de construccion recomendada

### Bloque 0 - Fundacion tecnica

- DT-01 Bootstrap Quarkus y estandares base
- DT-02 OpenAPI y contratos iniciales
- DT-08 Manejo estandar de errores
- DT-09 Observabilidad base

**Salida del bloque**: servicio base ejecutable, contratos definidos, error model y trazabilidad tecnica inicial.

### Bloque 1 - Core transaccional y seguridad

- DT-03 Modelo de dominio y maquina de estados
- DT-04 Esquema PostgreSQL y migraciones
- DT-05 Repositorios PostgreSQL
- DT-06 Modelo canonico Markdown + frontmatter
- DT-07 AuthN/AuthZ y RBAC

**Salida del bloque**: dominio, persistencia, seguridad y estructuras de relaciones basicas listas.

### Bloque 2 - Captura, versionado y aprobacion

- DT-10 API de casos MVP
- DT-11 Git provider port + adapter GitHub
- DT-12 Alta manual de memoria
- DT-13 Alta de memoria desde caso
- DT-14 Persistencia Git y actualizacion de estado
- DT-15 Validacion de criticidad y flujo PR
- DT-16 Webhook y polling de aprobacion
- DT-26 Auditoria extremo a extremo

**Salida del bloque**: memorias creadas en Git/Postgres, con aprobacion manual por PR para criticidad alta/critica.

### Bloque 3 - Asincronia oficial del MVP

- DT-17 Job Manager async oficial
- DT-18 Worker Quarkus e idempotencia
- DT-19 Indexacion semantica y adapter Qdrant
- DT-20 Reconciliacion operacional

**Salida del bloque**: procesamiento asincrono implementado sin Kafka, con jobs en PostgreSQL, worker idempotente y consistencia eventual controlada.

### Bloque 4 - Consulta, trazabilidad y ciclo de vida

- DT-21 Relaciones basicas de trazabilidad
- DT-22 Consulta detalle y listado de memorias
- DT-23 Busqueda semantica MVP
- DT-24 API minima de relaciones
- DT-25 Archivado y ciclo de vida minimo

**Salida del bloque**: lectura operativa, busqueda semantica y trazabilidad basica disponibles sin grafo especializado.

### Bloque 5 - Calidad de salida MVP

- DT-27 Testing unitario
- DT-28 Testing de integracion
- DT-29 Hardening operativo y despliegue

**Salida del bloque**: MVP verificable, desplegable y monitoreable.

### Bloque 6 - Evoluciones posteriores

- DT-30 Evolucion de portabilidad Git
- DT-31 Evolucion de grafo avanzado
- DT-32 Evolucion de cache distribuida
- DT-33 Evolucion de broker dedicado

**Salida del bloque**: roadmap tecnico post-MVP desacoplado de la baseline R1.

## 10. Plan resumido por bloques

| Bloque | Objetivo | Equipos sugeridos | Duracion estimada | Gate de salida |
|---|---|---|---:|---|
| B0 | Fundacion tecnica | Backend + DevOps | 1 semana | App arranca, OpenAPI base, error model, telemetria base |
| B1 | Core transaccional y seguridad | Backend | 1.5 semanas | Migraciones ejecutables, RBAC activo, modelo canonico validado |
| B2 | Captura y gobierno | Backend | 2 semanas | Git/GitHub PR operativo, estados, auditoria y webhook/polling |
| B3 | Asincronia oficial MVP | Backend + DevOps | 1.5 semanas | `processing_jobs`, worker, retries e indexacion Qdrant activos |
| B4 | Consulta y trazabilidad | Backend | 1.5 semanas | Busqueda semantica, relaciones basicas y archivado funcional |
| B5 | Calidad de salida MVP | Backend + QA + DevOps | 1 semana | Tests, hardening, alertas, runbook y checklist tecnico |
| B6 | Evoluciones posteriores | Backend + Arquitectura | 1 a 2 semanas | Roadmap tecnico post-MVP documentado |

## 11. Definition of Done tecnica alineada a baseline

Una tarea tecnica solo puede declararse completada si cumple todo lo siguiente:

1. Implementacion alineada con `documento-arquitectura.md` y sin contradicciones de alcance MVP.
2. Codigo revisado tecnicamente y consistente con contratos, estados y ADRs oficiales.
3. Tests unitarios e integracion del cambio en verde.
4. OpenAPI y documentacion tecnica actualizadas cuando aplique.
5. Logs, metricas, trazas y `correlationId` disponibles para el flujo implementado.
6. Seguridad aplicada con JWT/RBAC y evidencia de control por rol.
7. Sin secretos hardcodeados ni configuracion sensible embebida.
8. Si la tarea toca asincronia, debe evidenciar uso de **tabla de jobs + worker Quarkus + control de concurrencia nativo PostgreSQL**, no Kafka ni Redis.
9. Si la tarea toca relaciones, debe mantenerse en **PostgreSQL + frontmatter** dentro del MVP, sin introducir Neo4j.
10. Si la tarea toca busqueda, debe integrar **Qdrant + filtros en PostgreSQL** sin cache distribuida MVP.
11. Si hay cambio de base de datos, debe existir migracion versionada y validacion DBA previa al pase.
12. Debe existir evidencia verificable de build, tests y prueba tecnica reproducible.

## 12. Riesgos de implementacion alineados a baseline

| ID | Riesgo | Impacto | Prob. | Mitigacion |
|---|---|---|---|---|
| RI-01 | Divergencia entre Git y PostgreSQL durante persistencia principal | Alta | Media | Estados transitorios, auditoria, reconciliacion e idempotencia |
| RI-02 | Falla de webhook GitHub deja memorias en revision inconsistente | Alta | Media | Polling de respaldo, reproceso idempotente y auditoria |
| RI-03 | Cuello de botella en `processing_jobs` por crecimiento de volumetria | Media | Media | Indices, tuning SQL, escalado worker y evaluacion futura de broker |
| RI-04 | Reprocesos simultaneos generan duplicidad de indexacion | Alta | Media | Idempotencia por `memoryId + versionId + jobType` y toma segura en PostgreSQL |
| RI-05 | Caida o latencia de Qdrant degrada busqueda semantica | Media | Media | Reintentos, rebuild de indice y degradacion controlada |
| RI-06 | Relaciones basicas resultan insuficientes para navegacion avanzada | Media | Media | Mantener contratos desacoplados y reservar Neo4j para R2 |
| RI-07 | Error de criticidad permite bypass de PR obligatorio | Alta | Baja | Reglas centralizadas, tests de borde y auditoria de transicion |
| RI-08 | Reintroduccion indebida de Kafka, Redis o Neo4j en backlog MVP | Media | Media | Checklist de alineacion contra baseline y revision tecnica obligatoria |
| RI-09 | Latencia de consultas o busquedas sin cache distribuida | Media | Media | Indices SQL, topK acotado, tuning y eventual Redis solo en R2 |

## 13. Recomendaciones de asignacion tecnica

- **Backend Dev A**: dominio, PostgreSQL, casos, memorias y estados.
- **Backend Dev B**: Git/GitHub, PR manual, auditoria y seguridad.
- **Backend Dev C**: Job Manager, worker Quarkus, Qdrant y reconciliacion.
- **DevOps**: secretos, despliegue K8s, observabilidad, networking y webhooks.
- **QA tecnico/funcional**: contratos API, negativos, seguridad, regresion async y consistencia Git/Postgres/Qdrant.

## 14. Evidencia de sincronizacion aplicada

Se ajusto la descomposicion tecnica para reflejar exactamente la baseline oficial en estos puntos:

1. **Kafka** eliminado del MVP y movido a evolucion posterior.
2. **Neo4j** eliminado del MVP y reservado a R2.
3. **Redis** eliminado del MVP y reservado a R2.
4. **Asincronia MVP** consolidada en **`processing_jobs` + worker Quarkus + concurrencia nativa PostgreSQL**.
5. **Relaciones MVP** limitadas a **PostgreSQL + frontmatter**.
6. **Busqueda MVP** mantenida con **Qdrant + PostgreSQL**.

## 15. Cierre

La descomposicion final queda alineada con la baseline definitiva de construccion y elimina las contradicciones previas de alcance. Con esto, el backlog tecnico de Fase 3 queda listo para entregar a construccion con secuencia, dependencias, DoD y riesgos consistentes con la arquitectura aprobada.
