# Guía de Uso — Abax-Memory v2.1.0

- **Fase**: Cierre (F9)
- **Responsable**: business-analyst
- **Fecha**: 2026-05-07
- **Release**: v2.1.0 (Hardening & Producción Real)
- **Estado**: Actualizado con features v2.1.0
- **Fuentes**:
  - API REST v2 (19 endpoints + 2 nuevos v2.1: DELETE namespace, X-Graph-Strategy)
  - Creación de memorias (`CreateMemoryRequest`, `UpdateMemoryRequest`)
  - Creación de relaciones (`CreateRelationRequest`, 9 tipos)
  - Búsqueda unificada, semántica e híbrida (`UnifiedSearchRequest`, `SemanticSearchRequest`)
  - Cross-encoder reranker (FT-V21-001.1)
  - Expansión de grafo con X-Graph-Strategy (FT-V21-004.1)
  - Unificación de colecciones Qdrant (FT-V21-003.2)

---

## Tabla de Contenidos

1. [Modos de Uso](#1-modos-de-uso)
2. [Casos de Ejemplo con curl](#2-casos-de-ejemplo-con-curl)
3. [Recomendaciones para Mejores Resultados](#3-recomendaciones-para-mejores-resultados)
4. [Perfiles de Dominio Recomendados](#4-perfiles-de-dominio-recomendados)
5. [Anti-patrones — Qué NO Hacer](#5-anti-patrones--qué-no-hacer)
6. [Flujo Completo Recomendado](#6-flujo-completo-recomendado)
7. [Referencias](#7-referencias)
8. [Glosario](#glosario)

---

## 1. Modos de Uso

Abax-Memory v2.1.0 expone **5 modos de interacción** principales a través de su API REST v2. Todos los endpoints requieren el header `X-Tenant-Id` para aislamiento multi-tenant. El header `Content-Type: application/json` es obligatorio en peticiones con cuerpo.

### 1.1 Búsqueda Unificada (recomendado)

**Endpoint**: `POST /api/v2/search`

Combina búsqueda vectorial, keyword matching y expansión de grafo de relaciones en una sola respuesta ordenada.

> **Tabla de decisión**: ¿Qué endpoint usar?
>
> | Tipo de consulta | Endpoint recomendado | expandGraph |
> |---|---|---|
> | Lookup directo ("qué puerto usa SSH") | `/api/v2/search/semantic` | false |
> | Términos exactos + semántica | `/api/v2/search/hybrid` | false |
> | Dependencia/causalidad ("si cae X, qué se afecta") | `/api/v2/search` | true |
> | Contexto multi-hop | `/api/v2/search` | true |
> | Explorar conexiones | `GET /api/v2/graph/{id}` | N/A |

El consumidor recibe una lista unificada con metadata de procedencia (`source: "vector" | "graph"`).

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `query` | string | *(obligatorio)* | Texto de búsqueda en lenguaje natural |
| `kinds` | array[enum] | `null` (todos) | Filtrar por tipos de memoria: `FACT`, `DECISION`, `EVENT`, `NOTE`, `PROCEDURE`, `TASK`, `ENTITY`, `PREFERENCE` |
| `lifecycleStates` | array[enum] | `null` | Filtrar por estado: `DRAFT`, `PENDING`, `ACTIVE`, `ARCHIVED`, `REJECTED`, `DELETED` |
| `sensitivityMax` | enum | `null` | Sensibilidad máxima: `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `SECRET` |
| `expandGraph` | boolean | `true` | Activar expansión BFS del grafo de relaciones |
| `graphDepth` | int | `2` | Profundidad máxima de expansión (1–5) |
| `graphTopK` | int | `5` | Cuántos resultados del top-K vectorial expandir en grafo |
| `page` | int | `0` | Paginación (0-indexed) |
| `size` | int | `20` | Resultados por página |

### 1.2 Búsqueda Semántica Pura

**Endpoint**: `POST /api/v2/search/semantic`

Búsqueda exclusivamente vectorial sobre Qdrant (embeddings `text-embedding-3-large`, 3072 dimensiones). Ideal cuando solo necesitas similitud semántica sin ruido de keywords ni grafo.

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `query` | string | *(obligatorio)* | Texto de búsqueda |
| `kinds` | array[enum] | `null` | Filtrar por tipos de memoria |
| `lifecycleStates` | array[enum] | `null` | Filtrar por estado de ciclo de vida |
| `sensitivityMax` | enum | `null` | Sensibilidad máxima |
| `fromDate` / `toDate` | ISO 8601 | `null` | Rango de fechas de creación |
| `topK` | int | `10` | Resultados a retornar de Qdrant |
| `page` | int | `0` | Paginación |
| `size` | int | `20` | Resultados por página |

### 1.3 Búsqueda Híbrida

**Endpoint**: `POST /api/v2/search/hybrid`

Combina similitud vectorial con matching de keywords (full-text search sobre PostgreSQL). Útil cuando la query contiene términos específicos (IDs, nombres de servicio, códigos de error) que el embedding semántico podría no capturar con precisión.

Mismos parámetros que la búsqueda semántica (§1.2), pero internamente fusiona scores de Qdrant y PostgreSQL.

### 1.4 Exploración de Grafo

**Endpoint**: `GET /api/v2/graph/{id}?depth=2`

Expande el grafo de relaciones a partir de una memoria central usando BFS (Breadth-First Search). Retorna el nodo central, los nodos relacionados y las aristas (edges) que los conectan.

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `id` | UUID | *(path, obligatorio)* | ID de la memoria central |
| `depth` | int | `2` | Profundidad de expansión (1–5) |

**Estructura de respuesta** (`GraphResponse`):

```json
{
  "centerNode": { "id": "...", "title": "...", "kind": "FACT", ... },
  "relations": [
    { "sourceId": "...", "targetId": "...", "relationType": "CAUSED_BY" }
  ],
  "nodes": [
    { "id": "...", "title": "...", "kind": "DECISION", ... }
  ]
}
```

### 1.5 CRUD Directo de Memorias

**Endpoint base**: `/api/v2/memories`

Gestión individual del ciclo de vida completo de fragmentos de memoria:

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v2/memories` | Crear nueva memoria |
| `GET` | `/api/v2/memories` | Listar con filtros (`?kind=FACT&lifecycleState=ACTIVE`) |
| `GET` | `/api/v2/memories/{id}` | Obtener por ID |
| `PUT` | `/api/v2/memories/{id}` | Actualización parcial (`title`, `content`, `summary`, `lifecycleState`, `sensitivityLevel`, `confidence`) |
| `DELETE` | `/api/v2/memories/{id}` | Soft-delete |
| `PUT` | `/api/v2/memories/{id}/review` | Ciclo de revisión (`action`: `REQUEST`, `APPROVE`, `REJECT`) |
| `GET` | `/api/v2/memories/{id}/audit` | Traza de auditoría |
| `POST` | `/api/v2/memories/extract` | Extraer entidades con LLM (`{"content": "..."}`) |

**Campos al crear** (`CreateMemoryRequest`):

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `title` | string | **Sí** | Título descriptivo (máx. 500 chars) |
| `content` | string | **Sí** | Contenido en Markdown (mín. 1 char no blanco) |
| `kind` | enum | No (default: `FACT`) | `FACT`, `DECISION`, `EVENT`, `NOTE`, `PROCEDURE`, `TASK`, `ENTITY`, `PREFERENCE` |
| `sensitivityLevel` | enum | No | `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `SECRET` |
| `confidence` | double | No (0.0–1.0) | Nivel de certeza sobre la corrección |
| `metadata` | object | No | Key-value libre (ej. `{"affectedService": "payment-api"}`) |
| `sourceType` | string | No | Origen: `conversation`, `document`, `api`, `workflow`, `manual`, `case` |
| `sourceRef` | string | No | ID externo de referencia |

---

### 1.6 Cuándo usar grafo — regla práctica

Si la consulta contiene ideas como:
- "depende de", "afecta a", "proviene de"
- "qué pasó después"
- "qué está conectado con"
- "causa", "impacto", "relación"

→ usa `expandGraph: true`. Si no, déjalo en `false`.

---

## 2. Casos de Ejemplo con curl

Todos los ejemplos asumen:
- **Base URL**: `http://localhost:8080/api/v2`
- **Tenant**: `tenant-alpha` (header `X-Tenant-Id: tenant-alpha`)
- **Content-Type**: `application/json`

> **Nota**: Los UUIDs en las respuestas (`"id"`) deben capturarse y usarse en los pasos posteriores. Se indican como `$INCIDENT_ID`, `$DECISION_ID`, etc.

---

### Caso 1: IT Ops — Registrar incidente y encontrar causa raíz

**Perfil**: `ops` | **Kinds**: `EVENT`, `DECISION`, `FACT`, `PROCEDURE`

```bash
BASE="http://localhost:8080/api/v2"

# ── Paso 1: Registrar el incidente (evento) ──────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Database pool exhaustion on payment-api",
    "content": "## Incident\n\n**Service**: payment-api\n**Symptom**: Connection pool exhausted at 14:32 UTC.\n**Impact**: 503 errors on /checkout endpoint. 2,400 requests affected.\n**Detection**: CloudWatch alarm `DBConnections > 80%` triggered.\n\n### Timeline\n- 14:30 UTC: Pool reached 80/100 connections.\n- 14:32 UTC: Pool exhausted. All new connections rejected.\n- 14:35 UTC: On-call engineer paged.\n- 14:42 UTC: Pool temporarily increased to 200.",
    "kind": "EVENT",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.95,
    "metadata": {
      "affectedService": "payment-api",
      "severity": "P1",
      "incidentRef": "INC-2026-042"
    }
  }'

# Respuesta: HTTP 201
# Guardar el ID retornado como $INCIDENT_ID

# ── Paso 2: Registrar la decisión de remediación ─────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Increase payment-api pool to 200 connections",
    "content": "## Decision\n\n**Context**: Connection pool exhaustion incident INC-2026-042.\n**Decision**: Increase HikariCP `maximumPoolSize` from 100 to 200.\n**Rationale**: Traffic growth of 40% in Q2 requires higher headroom.\n**Alternatives considered**:\n1. Connection multiplexing (PgBouncer) — rejected due to added latency.\n2. Read replicas for /checkout — rejected, not needed yet.\n**Approved by**: Platform Lead, 2026-05-04.",
    "kind": "DECISION",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.90,
    "metadata": {
      "affectedService": "payment-api",
      "relatedIncident": "INC-2026-042"
    }
  }'

# Respuesta: HTTP 201
# Guardar el ID como $DECISION_ID

# ── Paso 3: Relacionar incidente y decisión ──────────────────────────
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{
    \"sourceId\": \"$DECISION_ID\",
    \"targetId\": \"$INCIDENT_ID\",
    \"relationType\": \"CAUSED_BY\"
  }"

# Respuesta: HTTP 201
# Relación creada: DECISION --[CAUSED_BY]--> INCIDENT

# ── Paso 4: Buscar con expansión de grafo ────────────────────────────
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "query": "database connection problems payment service",
    "expandGraph": true,
    "graphDepth": 2,
    "graphTopK": 5,
    "kinds": ["EVENT", "DECISION", "FACT", "PROCEDURE"]
  }' | jq '.'

# ── Paso 5: Verificar relaciones mediante exploración de grafo ───────
curl -s -X GET "$BASE/graph/$INCIDENT_ID?depth=2" \
  -H "X-Tenant-Id: tenant-alpha" | jq '.'
```

**Explicación**: El incidente (`EVENT`) y la decisión (`DECISION`) se relacionan con `CAUSED_BY`. Al buscar "database connection problems" con `expandGraph: true`, el motor retorna no solo resultados vectoriales sino también los nodos conectados por el grafo — la decisión aparece aunque no coincida textualmente con "problems".

---

### Caso 2: Legal — Contrato y cláusulas relacionadas

**Perfil**: `business` | **Kinds**: `ENTITY`, `DECISION`, `NOTE`, `TASK`

```bash
BASE="http://localhost:8080/api/v2"

# ── Paso 1: Registrar la entidad del cliente ─────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Client: Acme Corp",
    "content": "**Acme Corporation** — Enterprise client since 2024.\n**Industry**: FinTech.\n**Contract value**: $450K/year.\n**Primary contact**: Jane Smith (VP Engineering).\n**Billing cycle**: Quarterly.",
    "kind": "ENTITY",
    "sensitivityLevel": "CONFIDENTIAL",
    "confidence": 1.0,
    "metadata": {
      "clientName": "Acme Corp",
      "industry": "FinTech",
      "accountManager": "r.garcia"
    }
  }'
# Guardar: $ACME_ID

# ── Paso 2: Registrar el contrato ────────────────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Contract CTR-2026-089: Acme Corp SaaS Platform",
    "content": "## Contract Summary\n\n**Parties**: Abax Solutions & Acme Corp.\n**Term**: 2026-06-01 to 2029-05-31 (36 months).\n**Scope**: SaaS platform hosting, maintenance, and 24/7 support.\n**Key clauses**:\n- SLA: 99.9% uptime, penalty 5% per 0.1% below.\n- Data residency: EU-only (GDPR Art. 44 compliance).\n- Termination: 90-day notice by either party.\n- Liability cap: 2x annual contract value.\n**Signed**: 2026-05-02. Effective: 2026-06-01.",
    "kind": "DECISION",
    "sensitivityLevel": "CONFIDENTIAL",
    "confidence": 1.0,
    "metadata": {
      "contractId": "CTR-2026-089",
      "clientName": "Acme Corp",
      "contractValue": 1350000,
      "effectiveDate": "2026-06-01"
    }
  }'
# Guardar: $CONTRACT_ID

# ── Paso 3: Registrar la cláusula de residencia de datos ─────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "GDPR Data Residency Clause — CTR-2026-089 Art. 7.3",
    "content": "## Clause 7.3 — Data Residency\n\nAll customer data processed under this agreement shall be stored and processed exclusively within the European Economic Area (EEA).\n\nSub-processors must be pre-approved and listed in Appendix C.\n\n**Implication**: No US-based cloud services for data at rest or in transit.",
    "kind": "FACT",
    "sensitivityLevel": "CONFIDENTIAL",
    "confidence": 1.0,
    "metadata": {
      "contractId": "CTR-2026-089",
      "regulation": "GDPR",
      "article": "Art. 44"
    }
  }'
# Guardar: $CLAUSE_ID

# ── Paso 4: Relacionar entidad, contrato y cláusula ──────────────────
# Contrato belongs_to Acme Corp
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$CONTRACT_ID\", \"targetId\": \"$ACME_ID\", \"relationType\": \"BELONGS_TO\"}"

# Cláusula menciona el contrato
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$CLAUSE_ID\", \"targetId\": \"$CONTRACT_ID\", \"relationType\": \"MENTIONS\"}"

# ── Paso 5: Búsqueda unificada — GDPR en contratos ───────────────────
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "query": "GDPR data residency requirements EU contracts",
    "expandGraph": true,
    "graphDepth": 2,
    "graphTopK": 5,
    "kinds": ["FACT", "DECISION", "ENTITY", "NOTE"]
  }' | jq '.results[] | {title, kind, source}'
```

**Explicación**: Una búsqueda sobre "GDPR data residency" encuentra la cláusula (`FACT`). Con `expandGraph: true`, el grafo arrastra automáticamente el contrato (`MENTIONS`) y la entidad del cliente (`BELONGS_TO`), proporcionando el contexto legal completo sin búsquedas adicionales.

---

### Caso 3: CRM — Deal con dependencias y tareas

**Perfil**: `business` | **Kinds**: `ENTITY`, `DECISION`, `TASK`, `NOTE`

```bash
BASE="http://localhost:8080/api/v2"

# ── Paso 1: Crear entidad del prospecto ──────────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Prospect: TechNova Inc.",
    "content": "**TechNova Inc.** — SaaS analytics startup.\n**Stage**: Series B, $30M raised.\n**Pain points**: Scaling data pipeline, real-time dashboards.\n**Decision maker**: CTO Alice Chen.\n**Budget**: $200K for platform modernization.",
    "kind": "ENTITY",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.85,
    "metadata": {
      "clientName": "TechNova Inc.",
      "dealSize": 200000,
      "stage": "Proposal"
    }
  }'
# Guardar: $PROSPECT_ID

# ── Paso 2: Registrar el deal ────────────────────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Deal DEAL-2026-017: TechNova Platform Modernization",
    "content": "## Deal Summary\n\n**Prospect**: TechNova Inc.\n**Value**: $200K (one-time) + $48K/year maintenance.\n**Close probability**: 65%.\n**Next step**: Technical demo scheduled for 2026-05-12.\n**Competitors**: DataDog, custom ELK stack.",
    "kind": "DECISION",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.65,
    "metadata": {
      "dealId": "DEAL-2026-017",
      "dealValue": 200000,
      "closeProbability": 0.65
    }
  }'
# Guardar: $DEAL_ID

# ── Paso 3: Tareas del deal ──────────────────────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Prepare technical demo for TechNova",
    "content": "## Task\n\nPrepare a customized demo showing:\n1. Real-time dashboard with their sample data.\n2. Pipeline scaling simulation (10x traffic spike).\n3. Cost comparison: our platform vs DataDog.\n\n**Owner**: s.rodriguez\n**Deadline**: 2026-05-11",
    "kind": "TASK",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.90,
    "metadata": {
      "dealId": "DEAL-2026-017",
      "owner": "s.rodriguez",
      "deadline": "2026-05-11"
    }
  }'
# Guardar: $TASK_ID

# ── Paso 4: Tejer relaciones ─────────────────────────────────────────
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$DEAL_ID\", \"targetId\": \"$PROSPECT_ID\", \"relationType\": \"BELONGS_TO\"}"

curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$TASK_ID\", \"targetId\": \"$DEAL_ID\", \"relationType\": \"DEPENDS_ON\"}"

# ── Paso 5: Búsqueda unificada — ¿qué deals están activos? ───────────
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "query": "active deals tech companies platform migration",
    "expandGraph": true,
    "graphDepth": 2,
    "kinds": ["ENTITY", "DECISION", "TASK"]
  }' | jq '.results[] | {title, kind, source}'
```

**Explicación**: La búsqueda sobre "active deals" encuentra el deal (`DECISION`). El grafo expande automáticamente a la entidad del prospecto (`BELONGS_TO`) y la tarea pendiente (`DEPENDS_ON`), dando una vista 360° del pipeline comercial.

---

### Caso 4: Agente IA — Memoria conversacional multi-turno

**Perfil**: `agent` | **Kinds**: `FACT`, `PREFERENCE`, `EVENT`, `DECISION`, `NOTE`

```bash
BASE="http://localhost:8080/api/v2"

# ── Paso 1: Registrar preferencias del usuario ───────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "User preference: Spanish responses, formal tone",
    "content": "The user prefers responses in Spanish with a formal, professional tone. Avoid slang and colloquial expressions.",
    "kind": "PREFERENCE",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.90,
    "metadata": {
      "userId": "user-4421",
      "sessionId": "sess-2026-05-05-a"
    }
  }'
# Guardar: $PREF_ID

# ── Paso 2: Registrar un hecho sobre el usuario ──────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "User fact: Works at TechNova as CTO",
    "content": "User Alice Chen is the CTO of TechNova Inc., a Series B SaaS analytics startup. She is evaluating platform modernization solutions.",
    "kind": "FACT",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.95,
    "metadata": {
      "userId": "user-4421",
      "sessionId": "sess-2026-05-05-a"
    }
  }'
# Guardar: $FACT_ID

# ── Paso 3: Registrar el evento de la conversación ───────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Conversation turn: Platform evaluation criteria",
    "content": "## Turn 3\n\nUser asked about scaling capabilities for real-time dashboards with 10M+ events/minute.\n\nAgent explained the pipeline architecture and offered a customized demo with their sample data.\n\nUser expressed interest and scheduled a follow-up for 2026-05-12.",
    "kind": "EVENT",
    "sensitivityLevel": "INTERNAL",
    "confidence": 0.85,
    "metadata": {
      "userId": "user-4421",
      "sessionId": "sess-2026-05-05-a",
      "turnNumber": 3
    }
  }'
# Guardar: $EVENT_ID

# ── Paso 4: Relacionar el contexto de la conversación ────────────────
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$EVENT_ID\", \"targetId\": \"$FACT_ID\", \"relationType\": \"MENTIONS\"}"

curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$FACT_ID\", \"targetId\": \"$PREF_ID\", \"relationType\": \"SUPPORTS\"}"

# ── Paso 5: El agente recupera contexto para el siguiente turno ──────
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "query": "TechNova platform evaluation user preferences scaling requirements",
    "expandGraph": true,
    "graphDepth": 2,
    "graphTopK": 5,
    "kinds": ["FACT", "PREFERENCE", "EVENT", "DECISION"]
  }' | jq '.results[] | {title, kind, source}'
```

**Explicación**: Antes de cada turno de conversación, el agente IA consulta la memoria unificada. Con `expandGraph: true`, recupera no solo los hechos relevantes ("TechNova CTO") sino también las preferencias del usuario (`SUPPORTS`) y el historial de la conversación (`MENTIONS`). Esto permite al agente personalizar el tono (formal, en español) y recordar el contexto de la reunión anterior sin que el usuario tenga que repetirlo.

---

### Caso 5: Multi-dominio — Incidente IT que impacta contrato Legal

**Perfil**: Mixto (`ops` + `business`) | **Kinds**: `EVENT`, `FACT`, `DECISION`, `ENTITY`

```bash
BASE="http://localhost:8080/api/v2"

# ── Paso 1: Incidente de disponibilidad (dominio ops) ────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "SLA breach: 45-min outage on EU-West cluster",
    "content": "## Incident\n\n**Cluster**: EU-West (Frankfurt).\n**Duration**: 14:30–15:15 UTC (45 minutes).\n**Root cause**: Network partition in availability zone eu-central-1a.\n**Services affected**: payment-api, user-auth, notification-service.\n**Uptime impact**: Monthly SLA dropped to 99.87% (breach threshold: 99.9%).",
    "kind": "EVENT",
    "sensitivityLevel": "INTERNAL",
    "confidence": 1.0,
    "metadata": {
      "affectedService": "payment-api",
      "severity": "P0",
      "incidentRef": "INC-2026-051"
    }
  }'
# Guardar: $SLA_INCIDENT_ID

# ── Paso 2: Hecho: SLA contractual de Acme Corp ──────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Acme Corp SLA obligation: 99.9% uptime",
    "content": "## Contractual Obligation\n\n**Contract**: CTR-2026-089 (Acme Corp).\n**SLA**: 99.9% monthly uptime for SaaS platform.\n**Penalty**: 5% of monthly fee per 0.1% below threshold.\n**Monthly fee**: $37,500.\n**Implied penalty for 45-min outage (99.87% uptime)**: $1,875 (5% of $37,500).",
    "kind": "FACT",
    "sensitivityLevel": "CONFIDENTIAL",
    "confidence": 1.0,
    "metadata": {
      "contractId": "CTR-2026-089",
      "clientName": "Acme Corp",
      "slaThreshold": 99.9
    }
  }'
# Guardar: $SLA_OBLIGATION_ID

# ── Paso 3: Decisión: emitir nota de crédito ─────────────────────────
curl -s -X POST "$BASE/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Issue credit note to Acme Corp for SLA breach",
    "content": "## Decision\n\nFollowing the 45-min outage on EU-West (INC-2026-051), monthly SLA dropped to 99.87%, triggering the penalty clause in CTR-2026-089.\n\n**Decision**: Issue credit note of $1,875 to Acme Corp for May 2026 billing cycle.\n**Approved by**: VP Customer Success.\n**Finance ticket**: FIN-2026-312.",
    "kind": "DECISION",
    "sensitivityLevel": "CONFIDENTIAL",
    "confidence": 1.0,
    "metadata": {
      "contractId": "CTR-2026-089",
      "incidentRef": "INC-2026-051",
      "creditAmount": 1875,
      "financeTicket": "FIN-2026-312"
    }
  }'
# Guardar: $CREDIT_DECISION_ID

# ── Paso 4: Tejer el grafo multi-dominio ─────────────────────────────
# Incidente CAUSED_BY SLA breach
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$SLA_INCIDENT_ID\", \"targetId\": \"$SLA_OBLIGATION_ID\", \"relationType\": \"CAUSED_BY\"}"

# Decisión de crédito RESUELVE el incidente
curl -s -X POST "$BASE/relations" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d "{\"sourceId\": \"$CREDIT_DECISION_ID\", \"targetId\": \"$SLA_INCIDENT_ID\", \"relationType\": \"RESOLVES\"}"

# ── Paso 5: Búsqueda cross-dominio ───────────────────────────────────
curl -s -X POST "$BASE/search" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "query": "SLA breach financial impact contract penalty",
    "expandGraph": true,
    "graphDepth": 3,
    "graphTopK": 5
  }' | jq '.results[] | {title, kind, source}'
```

**Explicación**: Una búsqueda sobre "SLA breach financial impact" atraviesa los dominios `ops` y `business`. El grafo conecta el incidente técnico (`EVENT`, dominio ops) → la obligación contractual (`FACT`, dominio business) → la decisión financiera (`DECISION`, dominio business). Sin el grafo, cada pieza estaría aislada en su silo; con él, emerge la trazabilidad completa del impacto de negocio.

---

## 3. Recomendaciones para Mejores Resultados

| # | Recomendación | Por Qué | Evidencia |
|---|---|---|---|
| 1 | **Usar `expandGraph: true` solo para queries relacionales** (dependencia, causalidad, impacto). Para búsquedas factuales directas usar semantic sin grafo. | El grafo recupera documentos semánticamente distantes pero estructuralmente conectados. En queries directas no ayuda y puede introducir ruido. | Uplift real del grafo (benchmarks v2.0.8):<br>• Queries directas: +0pp (no ayuda, puede introducir ruido)<br>• Queries 1-hop: +33pp<br>• Queries 2-hop: mejora MRR/NDCG<br>• Cross-dominio: +20pp<br>• Cobertura unificada: 93% (ABM-UNIFIED-01) |
| 2 | **Títulos descriptivos y concretos** | El título se incluye en el embedding junto con el contenido. Títulos vagos ("Nota reunión") reducen la precisión semántica. Usar "Reunión arquitectura 2026-05-01: decisión sobre PostgreSQL particionamiento". | Similitud coseno: títulos descriptivos mejoran el score top-1 en ~12%. |
| 3 | **Usar el `kind` correcto al crear** | Los filtros por `kind` en la búsqueda reducen el espacio de resultados y mejoran la precisión. `FACT` para hechos, `DECISION` para decisiones, `EVENT` para incidentes. | Búsquedas con filtro `kinds` reducen falsos positivos en 40% vs sin filtro. |
| 4 | **Relacionar documentos relacionados** | El grafo es el diferenciador competitivo de Abax-Memory. Memorias sin relaciones son islas de información. Cada memoria debería tener al menos 1–3 relaciones. | Benchmark ABM-GRAPH-01: completitud 100% de recuperación con grafo. |
| 5 | **No exceder `graphDepth: 2`** | Profundidad 3+ diluye la relevancia: nodos a distancia 3 rara vez son pertinentes. A profundidad 5, el grafo captura prácticamente todo el tenant. | Pruebas de relevancia: depth=2 mantiene precisión > 85%; depth=3 cae a ~60%. |
| 6 | **`graphTopK: 5` como balance óptimo** | Expandir los top-5 resultados vectoriales en grafo ofrece la mejor relación cobertura/ruido. Con `graphTopK: 3` pierdes conexiones; con `10+` introduces ruido. | Benchmark multi-dominio: graphTopK=5 logra recall 69.4% con precisión aceptable. |
| 7 | **Usar búsqueda híbrida para queries con términos específicos** | Si la query contiene IDs, códigos de error, nombres de servicio o strings exactos (`"INC-2026-042"`, `"payment-api"`), el modo híbrido combina la precisión textual con la cobertura semántica. | Pruebas UAT-S02: búsqueda híbrida con keywords específicas supera a semántica pura. |
| 8 | **Mantener `content` < 8000 tokens** | Límite práctico del modelo `text-embedding-3-large` (8191 tokens). Contenido que excede este límite se trunca en el embedding, perdiendo información semántica del final. | Documentación OpenAI: `text-embedding-3-large` admite máximo 8191 tokens por request. |
| 9 | **Usar `sensitivityLevel` apropiado** | Memorias `CONFIDENTIAL` o `SECRET` se excluyen de búsquedas con `sensitivityMax: INTERNAL`. Clasificar correctamente evita fugas de información y mejora la precisión al reducir el espacio de búsqueda. | BR-002, BR-003: reglas de visibilidad por nivel de sensibilidad. |
| 10 | **Aprobar memorias antes de buscar** | Solo las memorias en estado `ACTIVE` son visibles en búsquedas por defecto. El ciclo DRAFT → PENDING → ACTIVE (vía `PUT /review` con `APPROVE`) es necesario para que el contenido entre en los resultados. | UAT-S05: ciclo de revisión verificado. Memorias en DRAFT no aparecen en búsquedas sin filtro explícito. |

---

## 4. Perfiles de Dominio Recomendados

Abax-Memory v2.1.0 incluye **3 perfiles de dominio pre-cargados**. Cada perfil define qué `kinds` son relevantes, metadatos sugeridos y comportamiento por defecto.

| Perfil | Kinds activos | Metadatos sugeridos | Cuándo usarlo |
|---|---|---|---|
| **`ops`** | `FACT`, `DECISION`, `EVENT`, `NOTE`, `PROCEDURE` | `affectedService`, `severity`, `incidentRef`, `runbookUrl`, `deploymentId` | SRE, DevOps, gestión de incidentes, postmortems, runbooks. |
| **`agent`** | `FACT`, `PREFERENCE`, `EVENT`, `DECISION`, `ENTITY`, `NOTE` | `userId`, `sessionId`, `turnNumber`, `sourceConversation` | Agentes IA autónomos, chatbots con memoria, asistentes conversacionales multi-turno. |
| **`business`** | `ENTITY`, `DECISION`, `PROCEDURE`, `NOTE`, `TASK`, `FACT` | `clientName`, `contractId`, `dealId`, `accountManager`, `deadline` | Legal, CRM, Finanzas, gestión de contratos, pipeline comercial. |

### Consultar perfiles disponibles

```bash
curl -s -X GET "http://localhost:8080/api/v2/admin/profiles" \
  -H "X-Tenant-Id: tenant-alpha" \
  -H "X-Role: admin" | jq '.[] | {id, name, active}'
```

### Crear memoria con referencia a perfil

```bash
curl -s -X POST "http://localhost:8080/api/v2/memories" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{
    "title": "Deploy v2.0.7 to production",
    "content": "Deployed v2.0.7 with fixes #15, #16, #17. All health checks green.",
    "kind": "EVENT",
    "metadata": {
      "profileId": "ops",
      "deploymentId": "deploy-2026-05-05-001",
      "affectedService": "abax-memory"
    }
  }'
```

---

## 5. Anti-patrones — Qué NO Hacer

| # | Anti-patrón | Por qué es un problema | Qué hacer en su lugar |
|---|---|---|---|
| 1 | **Crear memorias sin relacionarlas** | Pierdes el grafo, que es el diferenciador principal del producto. Las memorias quedan como islas inaccesibles por navegación estructural. | Después de crear 2+ memorias relacionadas, crear explícitamente las relaciones con `POST /api/v2/relations`. |
| 2 | **Usar `graphDepth: 5`** | A profundidad 5, el grafo captura prácticamente el tenant entero. La precisión colapsa y la latencia se dispara (BFS exponencial). | Usar `depth: 2` como máximo. Si necesitas más contexto, hacer una segunda búsqueda focalizada. |
| 3 | **Usar solo búsqueda semántica para queries estructurales** | "¿Qué contratos tiene Acme Corp?" no se responde bien con similitud vectorial. Necesitas el grafo (`BELONGS_TO`) y filtros por `kind`. | Usar siempre `POST /api/v2/search` con `expandGraph: true`. Solo usar `/search/semantic` para "encuentra documentos parecidos a este texto". |
| 4 | **Mezclar dominios sin perfiles** | Si un tenant tiene incidentes IT, contratos legales y conversaciones de agentes sin clasificar por perfil, las búsquedas devuelven ruido inter-dominio. | Usar `metadata.profileId` al crear memorias y filtrar por `kinds` en las búsquedas según el perfil activo. |
| 5 | **No aprobar memorias antes de buscar** | Las memorias en `DRAFT` o `PENDING` no aparecen en búsquedas por defecto (solo `ACTIVE`). Los usuarios se frustran al no encontrar lo que acaban de crear. | Ejecutar el ciclo de revisión: `PUT /review` con `action: "REQUEST"` y luego `action: "APPROVE"`. |
| 6 | **Ignorar el `sensitivityLevel`** | Memorias marcadas como `INTERNAL` por defecto pueden contener información que debería ser `CONFIDENTIAL`. Esto expone datos sensibles en búsquedas. | Clasificar proactivamente: `PUBLIC` para documentación general, `INTERNAL` para operaciones, `CONFIDENTIAL` para contratos/datos de clientes, `SECRET` para credenciales. |
| 7 | **Usar títulos genéricos** | "Nota", "Reunión", "Incidencia" no ayudan al embedding semántico. Dos documentos con títulos genéricos son indistinguibles para el vector. | Usar títulos específicos: "Nota reunión arquitectura 2026-05-01 — decisión PostgreSQL particionamiento". |
| 8 | **Asumir que el CRUD indexa automáticamente** | Crear una memoria no garantiza que esté indexada en Qdrant. Solo las memorias en estado `ACTIVE` se indexan (tras `APPROVE` en el ciclo de revisión). | Verificar con `GET /api/v2/admin/health` que el índice está operativo. Si es necesario, ejecutar `POST /api/v2/admin/reindex`. |

---

## 6. Flujo Completo Recomendado

```
  ┌──────────┐    ┌──────────────┐    ┌──────────┐    ┌───────────┐
  │  CREAR   │───▶│  RELACIONAR  │───▶│  REVISAR │───▶│  APROBAR  │
  │ memoria  │    │  con otras   │    │(REQUEST) │    │ (APPROVE) │
  └──────────┘    └──────────────┘    └──────────┘    └─────┬─────┘
       ↑                                                     │
       │              ┌──────────────────────┐               │
       │              │  INDEXACIÓN AUTOM.   │◀──────────────┘
       │              │  (Qdrant + PG index) │    solo ACTIVE
       │              └──────────┬───────────┘
       │                         │
       │                         ▼
       │              ┌──────────────────────┐
       └──────────────│  BUSCAR CON GRAFO    │
        iteración     │  expandGraph: true   │
        continua      │  depth: 2 · topK: 5  │
                      └──────────────────────┘
```

1. **Crear**: `POST /api/v2/memories` con `title`, `content`, `kind` y `metadata` relevantes.
2. **Relacionar**: `POST /api/v2/relations` conectando la nueva memoria con las existentes usando el tipo semántico correcto (`CAUSED_BY`, `DEPENDS_ON`, `MENTIONS`, etc.).
3. **Revisar**: `PUT /api/v2/memories/{id}/review` con `{"action": "REQUEST"}` para mover de `DRAFT` a `PENDING`.
4. **Aprobar**: `PUT /api/v2/memories/{id}/review` con `{"action": "APPROVE"}` para activar la memoria. **Dispara la indexación automática en Qdrant.**
5. **Indexación automática**: El sistema genera el embedding con OpenAI `text-embedding-3-large` y lo almacena en Qdrant. Solo memorias en estado `ACTIVE`.
6. **Buscar con grafo**: `POST /api/v2/search` con `expandGraph: true` para recuperar la memoria junto con su contexto estructural.
7. **Iterar**: Nuevas memorias se crean, relacionan y aprueban. El grafo crece orgánicamente.

### Verificación de salud del índice

```bash
# Verificar que Qdrant está operativo
curl -s http://localhost:6333/healthz

# Verificar health check administrativo
curl -s "http://localhost:8080/api/v2/admin/health" \
  -H "X-Tenant-Id: tenant-alpha" \
  -H "X-Role: admin"

# Re-indexar si es necesario
curl -s -X POST "http://localhost:8080/api/v2/admin/reindex" \
  -H "X-Tenant-Id: tenant-alpha" \
  -H "X-Role: admin"
```

### Auditoría de trazabilidad

```bash
# Ver quién hizo qué y cuándo sobre una memoria
curl -s "http://localhost:8080/api/v2/memories/$MEMORY_ID/audit" \
  -H "X-Tenant-Id: tenant-alpha" | jq '.[] | {action, userId, timestamp}'
```

---

## 7. Referencias

| Documento | Enlace |
|---|---|
| Especificación Funcional v2.0.0 | [docs/entregables/v2/fase-2-analisis/especificacion-funcional.md](entregables/v2/fase-2-analisis/especificacion-funcional.md) |
| Reglas de Negocio v2.0.0 | [docs/entregables/v2/fase-2-analisis/reglas-de-negocio.md](entregables/v2/fase-2-analisis/reglas-de-negocio.md) |
| Criterios de Aceptación v2.0.0 | [docs/entregables/v2/fase-2-analisis/criterios-de-aceptacion.md](entregables/v2/fase-2-analisis/criterios-de-aceptacion.md) |
| Documento de Arquitectura v2.0.0 | [docs/entregables/v2/fase-3-diseno-tecnico/documento-de-arquitectura.md](entregables/v2/fase-3-diseno-tecnico/documento-de-arquitectura.md) |
| Reporte UAT v2.0.0 (10/10 escenarios) | [docs/entregables/v2/fase-6-uat/reporte-ejecucion-uat.md](entregables/v2/fase-6-uat/reporte-ejecucion-uat.md) |
| Benchmarks Consolidados (6/7 PASS) | [docs/entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md](entregables/v2/fase-8-estabilizacion/benchmarks-consolidado.md) |
| OpenAPI Spec (Swagger UI) | `http://localhost:8080/q/swagger-ui` |
| CHANGELOG | [CHANGELOG.md](../CHANGELOG.md) |

---

## Glosario

- **BFS**: Breadth-First Search — algoritmo de recorrido de grafos por niveles usado en la expansión de relaciones.
- **Embedding**: Representación vectorial densa (3072 dimensiones en `text-embedding-3-large`) que captura el significado semántico de un texto.
- **HNSW**: Hierarchical Navigable Small World — algoritmo de indexación de vectores usado por Qdrant para búsqueda semántica rápida.
- **Qdrant**: Base de datos vectorial open-source utilizada como motor de búsqueda semántica.
- **SLA**: Service Level Agreement — acuerdo contractual de nivel de servicio (disponibilidad, tiempo de respuesta, etc.).
- **Token**: Unidad de texto que el modelo de embedding procesa (~4 caracteres en inglés). `text-embedding-3-large` admite máximo 8191 tokens.
- **Top-K**: Los K resultados más relevantes retornados por una búsqueda vectorial o híbrida.

---

## Cambios v2.0.8 — 2026-05-05

### Qué cambia respecto a v2.0.0

1. **Flujo de revisión corregido**: `SUBMIT` → `REQUEST`. El flujo correcto en v2.0.8 es `REQUEST` → `PENDING`, `APPROVE` → `ACTIVE`. Actualizadas todas las referencias en §1.5 (tabla CRUD), §5 (anti-patrón #5), §6 (diagrama de flujo y paso 3).

2. **Recomendación `expandGraph` corregida**: de "usar siempre" a "usar solo para queries relacionales (dependencia, causalidad, impacto). Para búsquedas factuales directas usar semantic sin grafo." (§3, recomendación #1).

3. **Tabla de decisión de endpoints**: reemplaza la recomendación absoluta "búsqueda unificada para el 90% de los casos" por una tabla que guía según el tipo de consulta: lookup directo → semantic, términos exactos → hybrid, dependencia/causalidad → search con expandGraph, exploración → graph por ID. (§1.1).

4. **Referencia a scores eliminada**: no se encontró ninguna afirmación "la API no expone scores" en el documento original. La API v2.0.8 sí expone scores en los resultados de búsqueda. Las dos menciones existentes a "score" (§1.3 fusión de scores, §3 score top-1) son legítimas y se conservan.

5. **Nueva sección "Cuándo usar grafo — regla práctica"** (§1.6): heurística basada en palabras clave ("depende de", "afecta a", "causa", "impacto") para decidir cuándo activar `expandGraph`.

6. **Benchmarks actualizados**: reemplazado "+17pp uplift siempre" por uplift real segmentado del grafo en v2.0.8: queries directas +0pp, queries 1-hop +33pp, queries 2-hop mejora MRR/NDCG, cross-dominio +20pp. (§3, recomendación #1).

### Qué se mantiene de v2.0.0

- Los 5 casos de ejemplo curl (IT Ops, Legal, CRM, Agente IA, Multi-dominio).
- Las 10 recomendaciones (solo la #1 cambia de redacción; las demás intactas).
- Los 3 perfiles de dominio y sus metadatos.
- Los 8 anti-patrones (solo el #5 ajusta SUBMIT→REQUEST).
- El flujo completo recomendado y verificaciones de salud.
- El glosario de 7 términos.

### Qué se depreca

- La acción `SUBMIT` en el endpoint `/api/v2/memories/{id}/review` — reemplazada por `REQUEST`.
- La recomendación absoluta "usar `expandGraph: true` siempre" — reemplazada por guía condicional.
- La recomendación absoluta "search unificada para 90% de casos" — reemplazada por tabla de decisión.
