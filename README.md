# Abax-Memory — Motor de Memoria Genérica Multi-Dominio con IA

[![Release](https://img.shields.io/github/v/release/breisnerlopez/abax-memory?label=v2.1.0&color=success)](https://github.com/breisnerlopez/abax-memory/releases)
[![Tests](https://img.shields.io/badge/tests-228%20passed%20%7C%2015%20pre--existing%20failures-brightgreen)](#calidad)
[![Docker](https://img.shields.io/badge/ghcr-v2.1.0-blue)](https://github.com/breisnerlopez/abax-memory/pkgs/container/abax-memory)
[![Java](https://img.shields.io/badge/java-21-orange)](https://adoptium.net/)
[![Quarkus](https://img.shields.io/badge/quarkus-3.15.3-purple)](https://quarkus.io/)
[![License](https://img.shields.io/badge/license-Proprietary-red)](#licencia)

**Motor de memoria genérica multi-dominio con IA**, con búsqueda semántica vectorial, grafo de relaciones y perfiles de dominio configurables. Construido bajo metodología corporativa en cascada con 10 fases de verificación formal. Backend Quarkus + React frontend con 19 endpoints REST, multi-tenancy, y 3 perfiles de dominio pre-cargados (IT Ops, AI Agent, Business).

> **v2.1.0** — Hardening & Producción Real: 13 features (cross-encoder reranker, unificación Qdrant, cache JWT, DELETE namespace, X-Graph-Strategy), 96.67% top-1, 228 tests, stack 4/4 healthy. **v2.0.0** evolucionó de PMOA (IT Operations) a motor genérico multi-dominio con perfiles configurables, English-Only internals, y frontend React.

---

## ¿Qué es Abax-Memory?

Abax-Memory es un **motor de memoria genérica multi-dominio** que proporciona a agentes de IA y aplicaciones una **memoria persistente, consultable semánticamente y navegable por grafo de relaciones**. A diferencia de un simple key-value store, Abax-Memory entiende el significado de lo que guarda mediante embeddings vectoriales (OpenAI `text-embedding-3-large`, 3072 dimensiones) y permite recuperar información por similitud semántica, no por palabras clave.

**Cinco capacidades principales**:

1. **CRUD de fragmentos de memoria** con ciclo de vida completo (DRAFT → PENDING → ACTIVE → ARCHIVED → DELETED) y flujo de revisión.
2. **Búsqueda semántica unificada** (vectorial + híbrida + grafo) sobre Qdrant con embeddings de OpenAI.
3. **Grafo de relaciones dirigidas tipadas** (9 tipos: RELATED_TO, DEPENDS_ON, CAUSED_BY, RESOLVES, CONTRADICTS, SUPPORTS, MENTIONS, BELONGS_TO, SUPERSEDES) con expansión BFS.
4. **Perfiles de dominio configurables** (JSONB) — 3 perfiles pre-cargados: `ops` (IT/SRE), `agent` (AI Agents), `business` (CRM/Legal/Finance).
5. **Multi-tenancy** con aislamiento estricto (cross-tenant access → 404), rate limiting y gobierno RBAC (Keycloak OIDC en roadmap).

Todo expuesto como API REST documentada con OpenAPI 3.0.3 y un frontend React 18 con 6 pantallas.

> **Documentación completa del proyecto** en GitHub Pages: **[https://breisnerlopez.github.io/abax-memory/](https://breisnerlopez.github.io/abax-memory/)**

---

## Quick Start

Con Docker instalado, en **4 pasos**:

```bash
# 1. Clonar
git clone https://github.com/breisnerlopez/abax-memory.git && cd abax-memory

# 2. Configurar tu API key de OpenAI (recomendado: .env)
#    Crea un archivo .env con: OPENAI_API_KEY="sk-..."
#    Luego carga: export $(cat .env | xargs)
#    O exporta directamente:
export OPENAI_API_KEY="sk-..."

# 3. Levantar el stack completo (PostgreSQL + Qdrant + Keycloak + Backend)
docker compose up -d

# 4. Verificar que los 4 servicios están UP
curl http://localhost:8080/q/health          # Backend Quarkus
curl http://localhost:6333/healthz            # Qdrant
curl http://localhost:8443/realms/abax-memory # Keycloak
```

> **Swagger UI**: [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui) · **OpenAPI spec**: [http://localhost:8080/q/openapi](http://localhost:8080/q/openapi)

Para detener:

```bash
docker compose down           # preserva datos
docker compose down -v        # elimina todo (⚠️)
```

---

## Arquitectura

```mermaid
graph TD
    subgraph "Client Layer"
        REACT["React 18 SPA<br/>6 pantallas · 7 componentes<br/>fetch /api/v2/*"]
    end

    subgraph "Abax-Memory Stack"
        subgraph "API Layer v2"
            QK["Quarkus 3.15.3 · Java 21<br/>19 endpoints REST<br/>OpenAPI 3.0.3 · :8080"]
        end
        subgraph "Domain"
            DOM["MemoryFragment · Relation<br/>DomainProfile · LifecycleState<br/>8 kinds · 9 relation types"]
        end
        subgraph "Data Layer"
            PG[(PostgreSQL 16<br/>12 Flyway migrations<br/>:5432)]
            QD[(Qdrant 1.17.1<br/>Vector Search · 3072 dims<br/>:6333)]
        end
        subgraph "Identity"
            KC[Keycloak 26.1<br/>OIDC/JWT/RBAC<br/>:8443]
        end
    end

    subgraph "External AI"
        OA[OpenAI API<br/>text-embedding-3-large<br/>gpt-4o-mini]
    end

    REACT -->|"X-Tenant-Id header"| QK
    QK <-->|JPA/Panache| PG
    QK <-->|Vector Search| QD
    QK -->|OIDC Auth (MVP: simulado)| KC
    QK -->|Embeddings + Chat| OA
    QK -->|Flyway Migrations| PG
```

Flujo principal v2: **Cliente React** → API REST v2 Quarkus (`/api/v2/*`) → PostgreSQL (metadatos + fragmentos) + Qdrant (embeddings semánticos 3072d) + OpenAI (generación de embeddings, extracción de entidades). Multi-tenancy vía header `X-Tenant-Id` con aislamiento estricto (cross-tenant → 404).

---

## Documentación del Proyecto

El proyecto fue ejecutado bajo metodología en cascada con 9-10 fases, tres releases mayores (v1.0.0, v2.0.0, v2.1.0), cada uno con entregables formales, aprobaciones y gates de calidad. La documentación completa está disponible en el repositorio y en GitHub Pages.

### Fases completadas — v2.1.0

| Fase | Estado | Gate | Entregables clave |
|---|---|---|---|
| F0 — Descubrimiento | 🟢 Completada | Aprobada | Visión de producto, épicas (4), features (13), historias (16) |
| F1 — Inicio | 🟢 Completada | Aprobada | Acta de constitución, cronograma, matriz de riesgos |
| F2 — Análisis Funcional | 🟢 Completada | Aprobada | Especificación funcional, 64 CAs, 13 reglas de negocio |
| F3 — Diseño Técnico | 🟢 Completada | Aprobada | 13 ADRs, descomposición técnica (98 tareas), arquitectura |
| F4 — Construcción | 🟢 Completada | Aprobada | 13/13 features, 228 tests, anti-mock review (0 mocks silenciosos) |
| F5 — QA | 🟢 Completada | Aprobada | 56/56 CPs (100%), 12 defectos cerrados, 3 profundizaciones |
| F6 — UAT | 🟢 Completada | Aprobada | 14/14 escenarios (100%), 7/8 CEs, 96.67% top-1 |
| F7 — Despliegue | 🟢 Completada | Desplegada | Docker Compose 4/4 healthy, rollback ≤7 min |
| F8 — Estabilización | 🟢 Completada | Aprobada | 0 incidentes críticos, 48h monitoreo, runbook documentado |
| F9 — Cierre | 🟢 Completada | **Proyecto CERRADO** | Informe de cierre, lecciones aprendidas, dashboard final |

### Fases completadas — v2.0.0

| Fase | Estado | Gate | Entregables clave |
|---|---|---|---|
| F0 — Descubrimiento | 🟢 Completada | Documentada | Visión de producto, épicas, historias de usuario, backlog |
| F1 — Inicio | 🟢 Completada | Documentada | Charter, cronograma, matriz de riesgos, stakeholders |
| F2 — Análisis Funcional | 🟢 Completada | Documentada | Especificación funcional, criterios de aceptación, reglas de negocio |
| F3 — Diseño Técnico | 🟢 Completada | Documentada | Arquitectura, descomposición técnica, ADRs |
| F4 — Construcción | 🟢 Completada | Aprobada | Backend + Frontend, anti-mock review (40 marcas, 0 en prod) |
| F5 — Pruebas QA | 🟢 Completada | Aprobada | 96 casos de prueba, reporte de defectos |
| F6 — UAT | 🟢 Completada | Aceptada | 10/10 escenarios (curl real, 4 rondas iterativas) |
| F7 — Despliegue | 🟢 Completada | Desplegada | Go-live, GHCR v2.0.3–v2.0.7, rollback plan |
| F8 — Estabilización | 🟢 Completada | Aprobada | 7 benchmarks (6/7 PASS), burn-in, incidentes |
| F9 — Cierre | 🟢 Completada | **Proyecto CERRADO** | Lecciones aprendidas (11), informe de cierre |

### Documentos clave

| Documento | Enlace |
|---|---|
| Análisis de estructura v2 | [docs/analisis-estructura-v2.md](docs/analisis-estructura-v2.md) |
| Release mapping v1 ↔ v2 ↔ v2.1 | [docs/release-mapping.md](docs/release-mapping.md) |
| Iteration log (decisiones v2/v2.1) | [docs/iteration-log.md](docs/iteration-log.md) |
| Registro de entregables | [docs/registro-entregables.md](docs/registro-entregables.md) |
| Documentación v2.1 completa | [docs/entregables/v2.1/](docs/entregables/v2.1/) |
| Documentación v2 (congelada) | [docs/entregables/v2/](docs/entregables/v2/) |
| Documentación v1 (congelada) | [docs/entregables/v1/](docs/entregables/v1/) |

---

## Presentaciones

Las presentaciones ejecutivas y técnicas del proyecto están disponibles como documentos HTML autónomos — abren en cualquier navegador y se pueden imprimir como PDF.

> **🔗 Índice completo en GitHub Pages:** [https://breisnerlopez.github.io/abax-memory/](https://breisnerlopez.github.io/abax-memory/)

### Presentaciones v2.1.0 (10 fases)

| # | Fase | Presentación | Enlace |
|---|---|---|---|
| 1 | F0 · Descubrimiento v2.1 | Visión de hardening y mejoras | [Ver](docs/entregables/v2.1/fase-0-descubrimiento/presentacion-descubrimiento.html) |
| 2 | F1 · Inicio v2.1 | Kickoff, equipo y cronograma | [Ver](docs/entregables/v2.1/fase-1-inicio/presentacion-kickoff.html) |
| 3 | F2 · Análisis v2.1 | Propuesta funcional y CAs | [Ver](docs/entregables/v2.1/fase-2-analisis/presentacion-propuesta-funcional.html) |
| 4 | F3 · Diseño Técnico v2.1 | Arquitectura, 13 ADRs, pipeline two-stage | [Ver](docs/entregables/v2.1/fase-3-diseno-tecnico/presentacion-arquitectura.html) |
| 5 | F4 · Construcción v2.1 | Avance y anti-mock review | [Ver](docs/entregables/v2.1/fase-4-construccion/presentacion-avance-fase4.html) |
| 6 | F5 · QA v2.1 | 56/56 CPs, 12 defectos, k6+benchmark | [Ver](docs/entregables/v2.1/fase-5-qa/presentacion-resultados-qa.html) |
| 7 | F6 · UAT v2.1 | 14/14 escenarios, 96.67% top-1 | [Ver](docs/entregables/v2.1/fase-6-uat/presentacion-resultados-uat.html) |
| 8 | F7 · Despliegue v2.1 | Go-Live y verificación post-deploy | [Ver](docs/entregables/v2.1/fase-7-despliegue/presentacion-go-live.html) |
| 9 | F8 · Estabilización v2.1 | 0 críticos, runbook, monitoreo 48h | [Ver](docs/entregables/v2.1/fase-8-estabilizacion/presentacion-estabilizacion.html) |
| 10 | F9 · Cierre v2.1 | Resumen ejecutivo, logros y lecciones | [Ver](docs/entregables/v2.1/fase-9-cierre/presentacion-cierre.html) |

### Presentaciones v2.0.0 (8 fases)

| # | Fase | Presentación | Enlace |
|---|---|---|---|
| 1 | F0 · Descubrimiento v2 | Visión multi-dominio y hallazgos iniciales | [Ver](docs/entregables/v2/fase-0-descubrimiento/presentacion-descubrimiento.html) |
| 2 | F1 · Inicio v2 | Kickoff, equipo, cronograma y alcance | [Ver](docs/entregables/v2/fase-1-inicio/presentacion-kickoff.html) |
| 3 | F2 · Análisis v2 | Propuesta funcional y criterios de aceptación | [Ver](docs/entregables/v2/fase-2-analisis/presentacion-propuesta-funcional.html) |
| 4 | F3 · Diseño Técnico v2 | Arquitectura, ADRs y descomposición | [Ver](docs/entregables/v2/fase-3-diseno-tecnico/presentacion-arquitectura.html) |
| 5 | F4 · Construcción v2 | Avance de implementación y anti-mock review | [Ver](docs/entregables/v2/fase-4-construccion/presentacion-avance.html) |
| 6 | F6 · UAT v2 | Resultados de User Acceptance Testing (10/10) | [Ver](docs/entregables/v2/fase-6-uat/presentacion-resultados-uat.html) |
| 7 | F7 · Despliegue v2 | Plan de Go-Live y verificación post-deploy | [Ver](docs/entregables/v2/fase-7-despliegue/presentacion-go-live.html) |
| 8 | F9 · Cierre v2 | Resumen ejecutivo, logros y 11 lecciones | [Ver](docs/entregables/v2/fase-9-cierre/presentacion-cierre.html) |

---

## Stack Tecnológico

| Componente | Tecnología | Versión | Propósito |
|---|---|---|---|
| **Backend** | Quarkus (Java) | 3.15.3 | Framework REST reactivo, CDI, Hibernate ORM |
| **Lenguaje** | Java | 21 (LTS) | JDK base |
| **Frontend** | React + TypeScript | 18 | 6 pantallas, 7 componentes, fetch nativo |
| **Base de Datos** | PostgreSQL (Alpine) | 16 | Persistencia operativa (12 migraciones Flyway) |
| **Migraciones** | Flyway | — | 12 migraciones: V1 (baseline) a V12 (fix relations) |
| **Vector DB** | Qdrant | 1.17.1 | Búsqueda semántica sobre embeddings (3072 dims) |
| **IA** | OpenAI API | text-embedding-3-large, gpt-4o-mini | Embeddings + extracción de entidades |
| **Integración IA** | LangChain4j + Quarkiverse | 1.0.0-beta1 / 0.24.0 | Cliente OpenAI declarativo CDI para Quarkus |
| **Identity** | Keycloak | 26.1 | OIDC Provider con JWT, realm `abax-memory` (MVP: simulado) |
| **Contenedores** | Docker + Compose | 3.9 | Despliegue local completo con 4 servicios |
| **Registro** | GitHub Container Registry | ghcr.io | Imagen pública `ghcr.io/breisnerlopez/abax-memory` |
| **Documentación API** | OpenAPI 3.0.3 | `/q/openapi` | Contrato API auto-generado por SmallRye OpenAPI |

---

## Instalacion y Despliegue

### Requisitos Previos

| Herramienta | Version minima | Nota |
|---|---|---|
| Docker | 24+ | Con soporte para Compose v2 (`docker compose`) |
| Docker Compose | 2.x | Incluido en Docker Desktop |
| OpenAI API Key | — | Cuenta con acceso a modelos `text-embedding-3-large`, `gpt-4o-mini`, `gpt-4o` |
| Java (solo desarrollo) | 21 (Temurin/Eclipse) | Para compilacion local con Maven |
| Maven (solo desarrollo) | 3.9+ | Wrapper incluido (`./mvnw`) |
| Puertos disponibles | 8080, 5432, 6333, 6334, 8443 | Verificar que no esten en uso |

### Despliegue con Docker Compose

```bash
# 1. Clonar repositorio
git clone https://github.com/breisnerlopez/abax-memory.git
cd abax-memory

# 2. Configurar API key de OpenAI (NUNCA hardcodear)
export OPENAI_API_KEY="sk-..."

# 3. Desplegar stack completo
docker compose up -d

# 4. Verificar que los 4 servicios estan UP
curl http://localhost:8080/q/health       # Backend Quarkus
curl http://localhost:6333/healthz         # Qdrant
curl http://localhost:8443/realms/abax-memory  # Keycloak
```

### Servicios levantados

| Servicio | URL interna | Puerto host | Contenedor |
|---|---|---|---|
| Backend Quarkus | `http://abax-memory:8080` | `8080` | `abax-memory-backend` |
| PostgreSQL | `postgres://postgres:5432` | `5432` | `abax-postgres` |
| Qdrant | `http://qdrant:6333` | `6333` | `abax-qdrant` |
| Keycloak | `http://keycloak:8080` | `8443` | `abax-keycloak` |

---

## Endpoints API v2

Swagger UI disponible en: `http://localhost:8080/q/swagger-ui`  
OpenAPI spec: `http://localhost:8080/q/openapi`

### Gestión de Memorias (`/api/v2/memories`)

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `POST` | `/` | Crear nuevo fragmento de memoria | X-Tenant-Id |
| `GET` | `/` | Listar con filtros y paginación | X-Tenant-Id |
| `GET` | `/{id}` | Obtener fragmento por ID | X-Tenant-Id |
| `PUT` | `/{id}` | Actualizar parcialmente | X-Tenant-Id |
| `DELETE` | `/{id}` | Soft-delete | X-Tenant-Id |
| `PUT` | `/{id}/review` | Ciclo de revisión (submit/approve/reject) | X-Tenant-Id |
| `GET` | `/{id}/audit` | Traza de auditoría del fragmento | X-Tenant-Id |
| `POST` | `/extract` | Extraer entidades con LLM | X-Tenant-Id |

### Búsqueda y Grafo (`/api/v2`)

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `POST` | `/search/semantic` | Búsqueda vectorial pura (Qdrant) | X-Tenant-Id |
| `POST` | `/search/hybrid` | Búsqueda híbrida (vector + keyword) | X-Tenant-Id |
| `POST` | `/search` | Búsqueda unificada (vector + grafo) | X-Tenant-Id |
| `GET` | `/search/similar/{id}` | Fragmentos similares a uno dado | X-Tenant-Id |
| `GET` | `/graph/{id}` | Expandir grafo de relaciones (BFS) | X-Tenant-Id |
| `POST` | `/relations` | Crear relación entre fragmentos | X-Tenant-Id |
| `GET` | `/relations/{id}` | Listar relaciones de un fragmento | X-Tenant-Id |
| `DELETE` | `/relations/{id}` | Eliminar relación | X-Tenant-Id |

### Administración (`/api/v2/admin`)

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `POST` | `/admin/reindex` | Re-indexar todas las memorias activas | X-Tenant-Id + admin |
| `GET` | `/admin/profiles` | Listar perfiles de dominio activos | X-Tenant-Id |
| `GET` | `/admin/health` | Health check de latencia | X-Tenant-Id |

### Health y Monitoreo

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `GET` | `/q/health` | Health check agregado | Público |
| `GET` | `/q/health/live` | Liveness probe | Público |
| `GET` | `/q/health/ready` | Readiness probe | Público |
| `GET` | `/q/openapi` | Especificación OpenAPI 3.0.3 | Público |

---

## 📖 Guía de Uso

### Modos de Uso Rápido

| Modo | Endpoint | Una línea |
|---|---|---|
| **Búsqueda unificada** ⭐ | `POST /api/v2/search` | Vector + keyword + grafo. Usa `expandGraph: true` solo para queries relacionales (dependencia, causalidad, impacto). |
| **Búsqueda semántica** | `POST /api/v2/search/semantic` | Similitud vectorial pura sobre Qdrant (3072 dims). Ideal para "encuentra documentos parecidos a este". |
| **Búsqueda híbrida** | `POST /api/v2/search/hybrid` | Vector + full-text PostgreSQL. Para queries con IDs, códigos de error o términos exactos. |
| **Exploración de grafo** | `GET /api/v2/graph/{id}?depth=2` | Navega relaciones BFS desde una memoria. Descubre conexiones estructurales. |
| **CRUD de memorias** | `POST/GET/PUT/DELETE /api/v2/memories` | Gestión completa con ciclo de vida (DRAFT → PENDING → ACTIVE), revisión y auditoría. |

### Ejemplo Rápido — IT Ops: Incidente + Causa Raíz

```bash
BASE="http://localhost:8080/api/v2"

# 1. Crear incidente
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-alpha" \
  -d '{"title":"Database pool exhaustion","content":"Pool exhausted at 14:32 UTC. 503 errors on /checkout.","kind":"EVENT","sensitivityLevel":"INTERNAL","confidence":0.95}'

# 2. Crear decisión
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-alpha" \
  -d '{"title":"Increase pool to 200 connections","content":"Decision: increase HikariCP maximumPoolSize from 100 to 200.","kind":"DECISION","sensitivityLevel":"INTERNAL","confidence":0.90}'

# 3. Relacionarlos
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-alpha" \
  -d '{"sourceId":"$DECISION_ID","targetId":"$INCIDENT_ID","relationType":"CAUSED_BY"}'

# 4. Buscar con grafo
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-alpha" \
  -d '{"query":"database connection problems","expandGraph":true,"graphDepth":2,"graphTopK":5}' | jq '.'
```

### Las 3 Recomendaciones Más Importantes

| # | Recomendación | Por qué |
|---|---|---|
| 1 | **`expandGraph: true` según tipo de consulta** | +33pp en queries 1-hop, +20pp cross-dominio, +0pp en directas. Usar solo para dependencia/causalidad/impacto. Para búsquedas factuales usar semantic. |
| 2 | **Relaciona toda memoria nueva** | El grafo es el diferenciador. Sin relaciones, Abax-Memory es solo un vector store. |
| 3 | **Aprueba antes de buscar** | Solo memorias `ACTIVE` son visibles. Usa `PUT /review` con `APPROVE` para indexar. |

### Guía Completa

Casos de ejemplo detallados (IT Ops, Legal, CRM, Agente IA, Multi-dominio), anti-patrones, perfiles de dominio y flujo completo en:

📄 **[docs/guia-uso.md](docs/guia-uso.md)**

---

## Autenticación — Keycloak OIDC

El backend requiere autenticación vía **OIDC con Keycloak** para endpoints de negocio. En el MVP actual, la autenticación se simula mediante headers `X-Tenant-Id` y `X-Role`. La migración a validación JWT real está en roadmap para producción (`REPLACE_BEFORE_PROD`).

### Flujo de Autenticación (MVP)

1. Incluir header `X-Tenant-Id` en todas las peticiones a `/api/v2/*`.
2. Para endpoints admin, incluir header `X-Role: admin`.

### Realm y Cliente OIDC

| Parámetro | Valor |
|---|---|
| Realm | `abax-memory` |
| Client ID | `abax-memory-api` |
| Grant types | `password`, `client_credentials` |
| JWT Issuer | `http://keycloak:8080/realms/abax-memory` |

---

## Benchmarks

Durante la fase de Estabilización (F8) se ejecutaron **7 benchmarks** independientes. Resultado global: **6/7 aprobados (85.7%)**.

| ID | Dataset | Métrica | Resultado | Meta | Veredicto |
|---|---|---|---|---|---|
| CE-01 | BEIR SciFact (5,183 docs) | NDCG@10 | 0.7771 | ≥ 0.80 | ❌ FAIL (−0.023) |
| CE-02 | BEIR SciFact | Recall@10 | 0.9006 | ≥ 0.90 | ✅ PASS |
| CE-03 | LoCoMo Sintético | NDCG@10 | 0.9820 | ≥ 0.80 | ✅ PASS |
| CE-04 | Latencia (300 queries) | p95 | 213ms | < 500ms | ✅ PASS |
| ABM-GRAPH-01 | Graph-enhanced (IT) | Completitud | 100% | ≥ 80% | ✅ PASS |
| ABM-MULTI-01 | Multi-dominio | Recall c/grafo | 69.4% | ≥ 70% | ❌ FAIL (−0.6pp) |
| ABM-UNIFIED-01 | Búsqueda unificada | Cobertura | 93% | ≥ 80% | ✅ PASS |

> **Detalle completo**: [docs/entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md](docs/entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md)

---

## Releases

| Versión | Fecha | Tipo | Cambios clave | Docker tag |
|---|---|---|---|---|
| **v2.1.0** | 2026-05-07 | Minor (Hardening) | 13 features: cross-encoder reranker, unificación Qdrant, cache JWT, X-Graph-Strategy, DELETE namespace. 228 tests, 96.67% top-1 | `v2.1.0`, `latest` |
| **v2.0.9** | 2026-05-05 | Patch | Fix: unifiedSearch con expandGraph=true de ~9s a ~400ms (95% reducción) | `v2.0.9` |
| **v2.0.8** | 2026-05-05 | Patch | Fix #18: Qdrant relevance scores mapeados correctamente | `v2.0.8` |
| **v2.0.7** | 2026-05-05 | Patch | Fix #17: `embedding_id` persistido en PostgreSQL tras upsert Qdrant | `v2.0.7` |
| **v2.0.6** | 2026-05-05 | Patch | Fix #16: `approveReview()` indexa automáticamente en Qdrant | `v2.0.6` |
| **v2.0.5** | 2026-05-05 | Patch | Fix #15: acción REQUEST (antes SUBMIT) en ReviewAction (140 tests) | `v2.0.5` |
| **v2.0.4** | 2026-05-05 | Patch | Fix #13 (CDI `Instance<T>`) + Fix #14 (Qdrant host config) | `v2.0.4` |
| **v2.0.3** | 2026-05-05 | Patch | Fix #12 (langchain4j-openai) + Fix #11 (columna `next_retry_at`) + UBI9/JRE | `v2.0.3` |
| **v1.0.0** | 2026-05-02 | Major (MVP) | Release inicial: 13 endpoints, 54 tests, 61/61 CA | `v1.0.0` |

- **GitHub Release**: [Releases](https://github.com/breisnerlopez/abax-memory/releases)
- **GitHub Container Registry**: `ghcr.io/breisnerlopez/abax-memory`
- **Changelog completo**: [CHANGELOG.md](CHANGELOG.md)

---

## Calidad

| Indicador | Valor |
|---|---|
| Tests automatizados v2.1.0 | **228** (93.8% pass rate, 15 pre-existing infrastructure failures) |
| Tests automatizados v2.0.0 | **163** (115 backend + 48 frontend) |
| Casos de prueba QA v2.1.0 | **56/56** (100%) |
| Escenarios UAT v2.1.0 | **14/14** (100%) |
| Precisión semántica top-1 | **96.67%** (30 queries multi-dominio) |
| Defectos v2.1.0 cerrados | **12/12** (100% tasa de cierre) |
| Anti-mock review | **0 mocks** silenciosos en producción |
| Defectos críticos abiertos | **0** |
| Releases totales | 9 (v1.0.0 + v2.0.3–v2.0.9 + v2.1.0) |

---

## Configuración

La única variable de entorno obligatoria es `OPENAI_API_KEY`.

### Variables de Entorno principales

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `OPENAI_API_KEY` | **SÍ** | *(vacía)* | API key de OpenAI |
| `ABAX_V2_QDRANT_HOST` | No | `localhost` | Host de Qdrant |
| `ABAX_V2_QDRANT_PORT` | No | `6333` | Puerto REST de Qdrant |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | No | `http://localhost:8443/realms/abax-memory` | URL del servidor OIDC |

> **Nota de seguridad**: La API key se gestiona exclusivamente vía variable de entorno. No se almacena en código fuente, archivos de configuración, ni imágenes Docker.

---

## Estructura del Proyecto

```
abax-memory/
├── backend-quarkus/                  # Backend (Quarkus 3.15.3 + Java 21)
│   ├── pom.xml                       # Maven POM (CDI, Panache, LangChain4j, OIDC, Testcontainers)
│   └── src/
│       ├── main/java/com/abax/memory/
│       │   ├── api/rest/v2/          # 2 Resources + ExceptionMapper (19 endpoints)
│       │   ├── api/dto/v2/           # 17 DTOs
│       │   ├── domain/model/         # 8 entidades de dominio
│       │   ├── domain/enums/         # 4 enums (MemoryKind, LifecycleState, RelationType, SensitivityLevel)
│       │   ├── domain/service/       # 6 interfaces de servicio
│       │   ├── infrastructure/       # Persistencia, AI, Qdrant, Security, Service Impls
│       │   └── config/               # InfrastructureConfig (CDI wiring)
│       ├── main/resources/db/migration/  # 12 migraciones Flyway
│       └── test/                     # 115 tests (JUnit 5 + REST Assured + Testcontainers)
├── frontend-v2/                      # Frontend (React 18 + TypeScript)
│   └── src/
│       ├── pages/                    # 6 pantallas (Search, Detail, Editor, Review, Admin, Dashboard)
│       ├── components/               # 7 componentes (Layout, MemoryCard, FilterPanel, Badges, Pagination)
│       ├── services/api.ts           # Cliente HTTP (fetch)
│       ├── types/index.ts            # Tipos TypeScript (263 líneas)
│       └── __tests__/                # 48 tests (Vitest + React Testing Library)
├── docker-compose.yml                # Stack local: Backend + PG + Qdrant + Keycloak
├── Dockerfile                         # Imagen UBI9/JRE
├── CHANGELOG.md                       # Historial de versiones
├── docs/                              # Documentación cascada por release
│   ├── analisis-estructura-v2.md      # Análisis estructural completo
│   ├── release-mapping.md             # Mapeo v1 ↔ v2
│   ├── iteration-log.md               # Bitácora de decisiones de iteración
│   └── entregables/
│       ├── v1/                        # Documentación v1.0.0 (congelada, solo-lectura)
│       └── v2/                        # Documentación v2.0.0 (10 fases, 55+ entregables)
└── .opencode/                         # Configuración de agentes OpenCode (50+ skills)
```

---

## Licencia

Software propietario. Todos los derechos reservados. Consulte los términos de licencia corporativa aplicables.

---

## Cambios v2.1.0 — 2026-05-07 (Hardening & Producción Real)

### 13 Features en 4 Categorías

#### Precisión (Accuracy)
- **Cross-encoder reranker**: etapa de re-ranking con modelo cross-encoder para mejorar precisión de resultados semánticos.
- **Ajuste de pesos search/semantic/hybrid**: recalibración de combinación de scores basada en benchmarks reales.
- **Expansión de grafo top-3**: recuperación de los 3 nodos más relevantes en expansión BFS.

#### Velocidad (Speed)
- **Cache JWT (Caffeine)**: validación de tokens cacheados en memoria, latencia ≤5ms en cache hit.
- **Optimización de queries Qdrant**: consolidación de filtros y payload retrieval, reducción de round-trips.
- **Cold Start Qdrant**: warmup de colecciones al iniciar el backend.

#### Eficiencia (Efficiency)
- **Unificación de colecciones Qdrant**: consolidación multi-colección en una sola con filtros por namespace.
- **Diagnóstico Worker**: worker de processing jobs reparado y verificado.
- **graphEntryStrategy configurable**: estrategia de entrada al grafo parametrizable.

#### API / Developer Experience
- **Header X-Graph-Strategy**: permite al cliente especificar estrategia de expansión de grafo vía header HTTP.
- **DELETE /api/v2/admin/namespaces/{name}**: eliminación de todos los recursos de un namespace en una sola operación.
- **Fix POST /extract**: corrección de bug en endpoint de extracción (OpenAI real, nunca MockLlmService).
- **Unificación endpoints search/hybrid**: consolidación de endpoints de búsqueda para API más limpia.

### Métricas de Calidad v2.1.0

| Métrica | Valor |
|---|---|
| Tests pasando | 228/243 (93.8%) |
| Casos QA | 56/56 CPs (100%) |
| Defectos cerrados | 12/12 (100%) |
| UAT | 14/14 escenarios (100%) |
| Precisión top-1 | 96.67% (30 queries) |
| Carga k6 | 0% errores (100 VUs) |
| Stack | Docker Compose 4/4 healthy |
| Rollback | ≤7 min (probado) |

> **Changelog completo**: [CHANGELOG.md](CHANGELOG.md)  
> **Link al diff**: [v2.0.9...v2.1.0](https://github.com/breisnerlopez/abax-memory/compare/v2.0.9...v2.1.0)

---

## Cambios v2.0.0 — 2026-05-05

### Qué cambia respecto a v1.0.0

- **Dominio**: de IT Operations fijo a multi-dominio con 3 perfiles configurables (`ops`, `agent`, `business`).
- **Endpoints**: de `/api/casos` + `/api/memorias` (13 endpoints) a `/api/v2/memories` + `/api/v2/search` (19 endpoints).
- **Frontend**: nuevo — 6 pantallas React 18 + TypeScript (no existía en v1).
- **Multi-tenancy**: aislamiento estricto con `X-Tenant-Id`, cross-tenant access → 404.
- **Modelo de datos**: de `Caso` + `MemoriaOperativa` a `MemoryFragment` + `Relation` + `DomainProfile` + `TenantConfig`.
- **Migraciones**: de 1 (V1 baseline) a 12 (V1–V12).
- **Convención de código**: English-Only estricto (`code-naming-convention`).
- **Tests**: de 54 a 163 (115 backend + 48 frontend).
- **QA**: de 49 a 96 casos de prueba.
- **UAT**: de 61/61 CA (análisis documental) a 10/10 escenarios (curl real, 4 rondas iterativas).
- **Anti-mock**: 3 capas de revisión (40 marcas detectadas, 0 mocks en producción).
- **Benchmarks**: 7 benchmarks independientes (6/7 aprobados).
- **Releases**: 5 patches post-MVP (v2.0.3 → v2.0.7).
- **Documentación**: Guía de Uso completa con 5 casos de ejemplo curl, recomendaciones, anti-patrones y perfiles de dominio ([docs/guia-uso.md](docs/guia-uso.md)).

### Qué se mantiene de v1.0.0

- Stack base: Quarkus 3.15.3 + Java 21 + PostgreSQL 16 + Qdrant 1.17.1 + Keycloak 26.1.
- Integración IA: OpenAI `text-embedding-3-large` + `gpt-4o-mini` vía LangChain4j.
- Metodología cascada: 10 fases (F0–F9) con gates formales de calidad.
- Despliegue: Docker Compose + GHCR.
- Documentación API: OpenAPI 3.0.3 auto-generado.

### Qué se depreca de v1.0.0

- Endpoints `/api/casos`, `/api/memorias`, `/api/audit` — reemplazados por `/api/v2/*`.
- Entidades `Caso`, `MemoriaOperativa` — reemplazadas por `MemoryFragment`.
- Convención de identificadores en español — migrada a English-Only.
- Dominio fijo IT Operations — reemplazado por perfiles configurables.

---

## Cambios v2.0.8 — 2026-05-05 (corrección de documentación)

### Qué cambia

- **Sección "Guía de Uso"**: corregida recomendación de `expandGraph: true` de "siempre" a condicional (solo para queries relacionales), con benchmarks reales segmentados (+33pp 1-hop, +20pp cross-dominio, +0pp directas).
- **Sección "Guía de Uso"**: actualizada descripción de búsqueda unificada para reflejar uso condicional de `expandGraph`.
- **Release v2.0.5**: changelog actualizado para reflejar renombre `SUBMIT` → `REQUEST`.

### Consistencia con docs/guia-uso.md

Ambos documentos ahora son coherentes respecto a:
- Flujo de revisión: `REQUEST` → `PENDING`, `APPROVE` → `ACTIVE`.
- Uso de `expandGraph`: condicional, no absoluto.
- Benchmarks: uplift segmentado real v2.0.8, no "+17pp siempre".
