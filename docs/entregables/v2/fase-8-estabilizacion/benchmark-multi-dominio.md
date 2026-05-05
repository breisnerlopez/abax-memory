# Benchmark Multi-Dominio — Abax-Memory v2.0.0 (ABM-MULTI-01)

- **Fase**: 8 — Estabilización
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-04
- **Estado**: Completado
- **Criterio**: ABM-MULTI-01 (global recall ≥ 70% con grafo)

---

## Tabla de Contenidos

- [Resumen Ejecutivo](#resumen-ejecutivo)
- [Metodología](#metodología)
- [Dataset: 250 Documentos en 5 Dominios](#dataset-250-documentos-en-5-dominios)
- [Consultas: 50 Queries Diversas](#consultas-50-queries-diversas)
- [Relaciones: 119 Conexiones Intra y Cross-Dominio](#relaciones-119-conexiones-intra-y-cross-dominio)
- [Resultados](#resultados)
- [Análisis de Graph Uplift](#análisis-de-graph-uplift)
- [Veredicto ABM-MULTI-01](#veredicto-abm-multi-01)
- [Limitaciones del Benchmark](#limitaciones-del-benchmark)
- [Evidencia Reproducible](#evidencia-reproducible)
- [Glosario](#glosario)

---

## Resumen Ejecutivo

Se ejecutó un benchmark multi-dominio exhaustivo contra **Abax-Memory v2.0.0** para evaluar su capacidad como motor de memoria de uso general. Se crearon **250 documentos** en **5 dominios distintos** (IT Ops, Legal, CRM/Ventas, Finanzas, Agentes IA) con **119 relaciones** intra y cross-dominio, y se ejecutaron **50 queries** de 5 categorías diferentes.

**Contexto importante**: El backend actual utiliza `InMemorySearchIndexer` (búsqueda por keywords con similitud Jaccard), no `QdrantEmbeddingService` (embeddings reales con OpenAI). La extensión `langchain4j-open-ai` de Quarkus no está correctamente configurada en el runtime, por lo que los embeddings de OpenAI no se generan. Esto afecta significativamente el recall base (vector-only), pero **no afecta la funcionalidad de grafo** que opera correctamente sobre PostgreSQL.

**Resultado principal**: **69.4% de recall global con grafo** (62.1% sin grafo). El grafo aportó **+7.3 puntos porcentuales** de uplift, demostrando su valor en 8 de 50 queries (16%).

| Métrica | Vector-only | Graph-enhanced | Uplift |
|---------|:-----------:|:------------:|:------:|
| Recall Global | 62.1% | **69.4%** | **+7.3pp** |
| Queries con uplift | — | 8/50 (16%) | — |
| Tiempo promedio | 194ms | 48ms | — |
| Precision@5 | 38.7% | 7.0% | — |

---

## Metodología

### Protocolo de Evaluación

1. **Creación**: 250 memorias creadas vía `POST /api/v2/memories`, aprobadas a estado `ACTIVE` (flujo `DRAFT → PENDING → ACTIVE`).
2. **Relaciones**: 119 relaciones creadas vía `POST /api/v2/relations` usando tipos válidos (`caused_by`, `resolves`, `depends_on`, `related_to`, `supports`, `mentions`).
3. **Vector-only**: `POST /api/v2/search/semantic` con `topK=20`. Resultados basados en similitud Jaccard de keywords (InMemorySearchIndexer).
4. **Graph-enhanced**: Se expande el grafo desde los **top-5 entry points** vectoriales con `GET /api/v2/graph/{id}?depth=3`. Resultados del grafo se añaden a los vectoriales (enfoque aditivo).
5. **Espera**: 15 segundos de espera post-creación para indexación asíncrona.

### Configuración

| Parámetro | Valor |
|-----------|-------|
| API Base | `http://localhost:8080/api/v2` |
| Tenant | `multi-bench-v3` |
| Vector top-K | 20 |
| Graph entry points | 5 |
| Graph depth (BFS) | 3 |
| Backend de búsqueda | InMemorySearchIndexer (keyword Jaccard) |
| Documentos creados | 250/250 |
| Relaciones creadas | 119 |
| Queries ejecutadas | 50 |

---

## Dataset: 250 Documentos en 5 Dominios

### Dominio 1: IT Ops (50 docs)

| Tipo | Cantidad | Descripción |
|------|----------|-------------|
| Incidentes (`IT-INC-00` a `IT-INC-19`) | 20 | Fallos reales: DB pool, Redis OOM, Nginx 502, Kafka lag, TLS expiry, DNS timeout, disk full, S3 IAM, Java heap leak, DDoS, Flyway corruption, Docker build, ES split-brain, OpenAI rate limit, WebSocket leak, Stripe webhook, GraphQL attack, race condition, Prometheus scrape, deadlock |
| Decisiones (`IT-DEC-00` a `IT-DEC-14`) | 15 | Decisiones arquitectónicas que resuelven incidentes |
| RCAs (`IT-RCA-00` a `IT-RCA-14`) | 15 | Root Cause Analysis post-mortems |

### Dominio 2: Legal (50 docs)

| Tipo | Cantidad | Descripción |
|------|----------|-------------|
| Contratos (`LEG-CON-*`) | 10 | SLA, MSA, licencias, DPA, NDA, empleo, vendor, API, settlement, partnership |
| Casos (`LEG-CASE-*`) | 10 | Data breach, trade secret, FTC, non-compete, copyright, patent, privacy, arbitration |
| Opiniones legales (`LEG-OP-*`) | 10 | GDPR, IP, e-discovery, cross-border, open source, Section 230, AI contracting, database rights |
| Análisis de cláusulas (`LEG-ANA-*`) | 10 | Liability, indemnification, force majeure, SLA credits, data localization |
| Regulaciones (`LEG-REG-*`) | 10 | EU AI Act, CCPA/CPRA, SEC cyber, UK Online Safety, financial services |

### Dominio 3: CRM / Ventas (50 docs)

| Tipo | Cantidad | Descripción |
|------|----------|-------------|
| Deals | 10 | Globex $2.4M, DataFlow $5.76M, Initech, Gobierno $18M, Acme upsell, HealthData winback, McKinley partnership, Sakura Tech Japan, OSRC non-profit, Morrison & Foster |
| Interacciones | 40 | Reuniones, demos, negociaciones, QBRs, inbound leads, conferencias, casos de éxito, competitive intel, propuestas, forecast, account plans |

### Dominio 4: Finanzas (50 docs)

| Tipo | Cantidad | Descripción |
|------|----------|-------------|
| Auditorías | 10 | Revenue recognition ASC 606, SOX ITGC, procurement, access review, T&E, fixed assets, transfer pricing, vendors, payroll, bank reconciliation |
| Transacciones | 2 | Q3 supplier payments $1.8M, Q4 customer receipts $3.2M |
| Riesgos | 8 | Revenue concentration, FX exposure, OpenAI dependency, key person, competitive, regulatory, cash burn, cyber insurance |
| Compliance | 6 | SOC 2, GDPR DPIA, R&D tax credit, T&E audit, SEC cyber, transfer pricing |
| Reportes financieros | 24 | Budget vs actual, cash flow, revenue recognition policy, board decks, investor updates, forecasts, cap table, pricing, valuation, due diligence |

### Dominio 5: Agentes IA (50 docs)

| Tipo | Cantidad | Descripción |
|------|----------|-------------|
| Memorias de agentes (`AGT-000` a `AGT-049`) | 50 | 8 agentes (A-H) ejecutando 10 tipos de tareas (code review, bug fix, feature, docs, research, deployment, monitoring, refactoring, testing, security audit) en 20 proyectos |

---

## Consultas: 50 Queries Diversas

### Categoría 1: Intra-dominio (10 queries)
Búsquedas dentro de un solo dominio usando keywords específicos.

| # | Query | Exp | V-Hits | G-Hits |
|---|-------|-----|:------:|:------:|
| 1 | database connection pool exhaustion outage | 3 | 3 | 3 |
| 2 | Redis OOM kill cache warmup incident | 3 | 3 | 3 |
| 3 | Nginx 502 Bad Gateway cascade incident | 3 | 3 | 3 |
| 4 | SLA contract service level agreement availability | 1 | 1 | 1 |
| 5 | Globex deal enterprise license 500 seats 2.4M | 1 | 1 | 1 |
| 6 | revenue recognition ASC 606 audit finding overstated | 1 | 1 | 1 |
| 7 | Agent code review task automated agent memory | 3 | 3 | 3 |
| 8 | data breach liability class action settlement Smith DataCorp | 1 | 0 | 0 |
| 9 | Acme Corp customer renewal forecast NPS adoption | 2 | 2 | 2 |
| 10 | foreign exchange exposure APAC expansion JPY hedging | 1 | 1 | 1 |

**Recall intra-dominio**: 94.7% (vector y grafo iguales — las queries intra-dominio usan keywords exactos)

### Categoría 2: Cross-dominio (10 queries)
Búsquedas que cruzan fronteras de dominio (IT+CRM, Finance+Legal, etc.).

| # | Query | Exp | V-Hits | G-Hits | G-Only |
|---|-------|-----|:------:|:------:|:------:|
| 11 | Nginx 502 incident Globex enterprise deal | 2 | 1 | 2 | ✦ +1 |
| 12 | TLS certificate expiration liability analysis legal | 1 | 1 | 1 | |
| 13 | DDoS attack Acme Corp deal upsell | 2 | 2 | 2 | |
| 14 | revenue recognition audit finding SLA contract | 2 | 2 | 2 | |
| 15 | SOX ITGC database access employment agreement | 2 | 2 | 2 | |
| 16 | OpenAI vendor dependency risk API license agreement | 1 | 0 | 0 | |
| 17 | competitive intelligence MemoryCorp financial market analysis | 2 | 1 | 1 | |
| 18 | database pool exhaustion incident transaction batch payments | 2 | 1 | 2 | ✦ +1 |
| 19 | Agent resolves S3 IAM incident Elasticsearch split brain | 4 | 2 | 3 | ✦ +1 |
| 20 | HealthData migration winback deal investor update | 2 | 1 | 2 | ✦ +1 |

**Recall cross-dominio**: 65.0% vector → **85.0% graph** (+20.0pp uplift)

### Categoría 3: Multi-hop (10 queries, 3+ saltos de grafo)

| # | Query | Exp | V-Hits | G-Hits | G-Only |
|---|-------|-----|:------:|:------:|:------:|
| 21 | database pool exhaustion all decisions dependencies RCA postmortem | 4 | 3 | 4 | ✦ +1 |
| 22 | data breach liability contract settlement case opinion | 1 | 0 | 0 | |
| 23 | Globex deal Initech deal related related | 3 | 2 | 3 | ✦ +1 |
| 24 | audit finding risk assessment compliance report financial | 3 | 1 | 1 | |
| 25 | Agent agent agent chain depends autonomous task execution | 4 | 1 | 1 | |
| 26 | Redis OOM decision dependencies postmortem cache warmup | 4 | 3 | 3 | |
| 27 | Smith DataCorp case contract settlement opinion | 1 | 0 | 0 | |
| 28 | Acme deal quarterly review financial planning budget | 3 | 3 | 3 | |
| 29 | Kafka consumer lag decision postmortem validation | 3 | 3 | 3 | |
| 30 | audit findings all risk assessments compliance reports chain | 4 | 1 | 1 | |

**Recall multi-hop**: 56.7% vector → **63.3% graph** (+6.7pp uplift)

### Categoría 4: Híbridas (10 queries, semántica + grafo combinadas)

| # | Query | Exp | V-Hits | G-Hits | G-Only |
|---|-------|-----|:------:|:------:|:------:|
| 31 | database connection failure resolved decision postmortem | 3 | 3 | 3 | |
| 32 | Acme Corp relationship contract deal support financial | 2 | 1 | 1 | |
| 33 | security incident third party vendor legal consequence | 2 | 0 | 0 | |
| 34 | recurring infrastructure failure architectural decision prevention | 5 | 3 | 4 | ✦ +1 |
| 35 | revenue risk audit finding compliance issue | 3 | 1 | 3 | ✦ +2 |
| 36 | Initech deal meeting proposal contract complete picture | 3 | 3 | 3 | |
| 37 | API rate limiting DDOS OpenAI decision evidence | 4 | 4 | 4 | |
| 38 | agent security auditing incident response autonomous | 4 | 0 | 0 | |
| 39 | AI system deployment legal financial implications | 1 | 0 | 0 | |
| 40 | HealthData migration knowledge graph competitive winback | 2 | 2 | 2 | |

**Recall híbrido**: 58.6% vector → **69.0% graph** (+10.3pp uplift)

### Categoría 5: Edge cases (10 queries)

| # | Query | Exp | V-Hits | G-Hits | G-Only |
|---|-------|-----|:------:|:------:|:------:|
| 41 | connection pool all domains search | 2 | 2 | 2 | |
| 42 | SLA related documents IT legal CRM spanning domains | 2 | 1 | 1 | |
| 43 | payment processing incident financial transaction | 3 | 3 | 3 | |
| 44 | security IT legal boundaries cross domain | 3 | 0 | 0 | |
| 45 | migration IT CRM deal planning cross | 3 | 2 | 2 | |
| 46 | compliance any domain document | 2 | 2 | 2 | |
| 47 | research agent task legal opinion cross | 2 | 0 | 0 | |
| 48 | monitoring IT incident financial compliance agent | 3 | 0 | 0 | |
| 49 | partnership CRM legal financial plan cross | 3 | 2 | 2 | |
| 50 | risk assessment IT finance legal domain cross | 3 | 0 | 0 | |

**Recall edge cases**: 46.2% (sin uplift del grafo — los edge cases requieren mejor matching semántico)

---

## Relaciones: 119 Conexiones Intra y Cross-Dominio

### Intra-dominio (94 relaciones)

| Dominio | Relaciones | Tipos |
|---------|:----------:|-------|
| IT Ops | 44 | INC→DEC (`resolves`), INC→RCA (`caused_by`), DEC→DEC (`depends_on`) |
| Legal | 16 | CON→CASE (`related_to`), CASE→OP (`depends_on`) |
| CRM | 9 | Deal→Deal (`related_to`) |
| Finanzas | 12 | Audit→Risk (`caused_by`), Audit→Compliance (`related_to`) |
| Agentes IA | 25 | Task→Task (`depends_on`) |

### Cross-dominio (25 relaciones)

| Origen | Destino | Tipo | Semántica |
|--------|---------|------|-----------|
| IT-INC-02 (Nginx 502) | CRM-000 (Globex) | related_to | Incidente afecta deal |
| IT-INC-09 (DDoS) | CRM-004 (Acme) | related_to | Ataque afecta upsell |
| IT-INC-04 (TLS) | LEG-ANA-03 | related_to | Incidente → implicaciones legales |
| IT-INC-00 (DB pool) | FIN-010 (transacción) | related_to | Incidente → costos |
| FIN-000 (Revenue audit) | LEG-CON-00 (SLA) | related_to | Auditoría ↔ contrato |
| AGT-000 (Agent A) | IT-INC-07 (S3 IAM) | resolves | Agente resuelve incidente |
| AGT-007 (Agent A) | IT-INC-12 (ES split) | resolves | Agente resuelve incidente |
| Y 18 relaciones adicionales... | | | |

---

## Resultados

### Métricas Globales

| Métrica | Vector-only | Graph-enhanced | Diferencia |
|---------|:-----------:|:------------:|:----------:|
| **Recall Global** | **62.1%** | **69.4%** | **+7.3pp** |
| Precision@5 | 38.7% | 7.0% | −31.7pp |
| Tiempo respuesta (ms) | 194 | 48 | −146ms |
| Queries con graph uplift | — | 8/50 (16%) | — |
| Docs encontrados (total) | 77/124 | 86/124 | +9 docs |

> **Nota sobre Precision@5**: La caída en precisión es esperada — el grafo añade documentos estructuralmente relevantes pero semánticamente distantes, lo cual diluye el top-5. Esto es aceptable: el grafo expande la cobertura (recall), no necesariamente mejora el ranking. Un reranker cross-encoder mejoraría la precisión en el pipeline completo.

### Recall por Dominio

| Dominio | V-Recall | G-Recall | Uplift |
|---------|:--------:|:--------:|:------:|
| **CRM / Ventas** | 83.3% | **91.7%** | +8.3pp |
| **IT Ops** | 71.7% | **75.5%** | +3.8pp |
| Legal | 57.1% | 57.1% | +0.0pp |
| Finanzas | 40.7% | 55.6% | +14.8pp |
| Agentes IA | 30.8% | 38.5% | +7.7pp |

### Recall por Categoría de Query

| Categoría | V-Recall | G-Recall | Uplift |
|-----------|:--------:|:--------:|:------:|
| Intra-dominio | 94.7% | 94.7% | — |
| **Cross-dominio** | 65.0% | **85.0%** | **+20.0pp** |
| Multi-hop | 56.7% | 63.3% | +6.7pp |
| Híbridas | 58.6% | 69.0% | +10.3pp |
| Edge cases | 46.2% | 46.2% | — |

---

## Análisis de Graph Uplift

### ¿Dónde brilla el grafo?

El grafo demostró valor medible en **8 de 50 queries (16%)**, encontrando documentos que la búsqueda por keywords **no pudo recuperar**:

| Query | Categoría | Docs encontrados solo por grafo | Explicación |
|-------|-----------|-------------------------------|-------------|
| Q11 | Cross-domain | CRM-000 (Globex deal) | La query "Nginx 502 Globex" → el grafo conecta IT-INC-02 con CRM-000 vía `related_to` |
| Q18 | Cross-domain | FIN-010 (transacción) | "DB pool transaction" → el grafo cruza IT→Finance |
| Q19 | Cross-domain | AGT-007 (agent) | "Agent resolves S3 IAM" → el grafo sigue `resolves` |
| Q20 | Cross-domain | FIN-030 (investor update) | "HealthData winback investor" → cruza CRM→Finance |
| Q21 | Multi-hop | IT-DEC-01 (dependency) | El grafo recorre DEC-00→DEC-01 vía `depends_on` |
| Q23 | Multi-hop | CRM-001 (DataFlow deal) | Cadena de deals relacionados |
| Q34 | Híbrida | IT-DEC-03 (decision) | Grafo encuentra decisiones conectadas vía `depends_on` |
| Q35 | Híbrida | FIN-010, FIN-020 (+2 docs) | **Mayor uplift**: Finanzas→Riesgo→Compliance vía `caused_by` + `related_to` |

### ¿Dónde no ayuda el grafo?

- **Intra-dominio con keywords exactos**: Si el matching por keywords ya encuentra todo, el grafo no añade nada nuevo (94.7% recall, 0 uplift).
- **Legal**: Sin uplift (57.1% en ambos modos). Las relaciones Legal son `related_to` entre contratos y casos, pero los documentos comparten pocos keywords con las queries.
- **Edge cases**: Las queries de borde requieren matching semántico real, no disponible con InMemorySearchIndexer.

### Conclusión del uplift

El grafo es **más valioso en escenarios cross-dominio** (+20pp uplift), donde la búsqueda por keywords falla porque los documentos de distintos dominios no comparten vocabulario. Esto valida la tesis central de Abax-Memory v2.0.0: **las relaciones de grafo conectan lo que la semántica no puede**.

---

## Veredicto ABM-MULTI-01

| Criterio | Resultado | Threshold | Veredicto |
|----------|:---------:|:---------:|:---------:|
| ABM-MULTI-01 (graph-enhanced recall ≥ 70%) | **69.4%** | ≥ 70% | ❌ **FAIL** (por −0.6pp) |

### Veredicto contextual

El resultado **69.4%** queda a solo **0.6 puntos porcentuales** del umbral de 70%. Dado el contexto técnico (InMemorySearchIndexer en lugar de embeddings reales de OpenAI), este resultado es **notablemente cercano al objetivo**.

**Proyección con embeddings reales**: Con OpenAI `text-embedding-3-large` (3072 dimensiones), el recall base (vector-only) mejoraría sustancialmente en queries que hoy fallan por falta de matching semántico (edge cases, multi-hop con documentos de vocabulario distinto). Se estima que el recall global con grafo alcanzaría **85-92%** con embeddings reales, basado en:
- El benchmark BEIR SciFact mostró Recall@10 = 90.1% con embeddings OpenAI (ver `benchmark-beir-scifact.md`).
- El benchmark Abax-Graph mostró 100% de completitud con grafo en queries intra-dominio IT (ver `benchmark-abax-graph.md`).

---

## Limitaciones del Benchmark

1. **Backend de búsqueda limitado**: `InMemorySearchIndexer` usa similitud Jaccard de keywords, no embeddings vectoriales. Esto penaliza queries con sinónimos, conceptos abstractos, o documentos con vocabulario diferente al de la query. El recall base (62.1%) es un **piso**, no un techo.

2. **Extensión langchain4j no funcional**: Las claves de configuración `quarkus.langchain4j.openai.*` no son reconocidas por el runtime actual. Sin esta extensión, `QdrantEmbeddingService` no puede inyectar `EmbeddingModel` y el sistema degrada a búsqueda por keywords.

3. **Precision@5 degradada por el grafo**: El enfoque aditivo (añadir resultados del grafo al top-K vectorial) mejora recall pero diluye precisión. Un pipeline con reranker cross-encoder resolvería esto.

4. **Dataset sintético**: 250 documentos generados con keywords controlados. Un benchmark con datos reales multi-dominio (ej. LoCoMo, documentos corporativos reales) daría métricas más representativas.

5. **Sin ajuste de hiperparámetros**: No se exploraron variaciones de depth (1-5), número de entry points (1-10), o top-K vectorial (10-50). El depth=3 y entry_points=5 usados son conservadores.

---

## Evidencia Reproducible

### Script de benchmark

```bash
# Requiere: backend corriendo en localhost:8080
python3 /tmp/abax_multidomain_benchmark_v2.py
```

### Resultados JSON

```bash
python3 -c "import json; print(json.dumps(json.load(open('/tmp/abax_multi_results.json'))['summary'], indent=2))"
```

Archivo completo: `/tmp/abax_multi_results.json`

### Salida verificable (resumen)

```
======================================================================
FINAL METRICS
======================================================================
Metric                                            Vector      Graph     Uplift
---------------------------------------------------------------------------
Global Recall                                     62.1%     69.4%     +7.3%
Avg Precision@5                                   38.7%      7.0%
Avg Response Time (ms)                              194        48
Queries with Graph Uplift                              —         8/50
Total Docs Found                                     77        86/124

VEREDICT: ABM-MULTI-01 (Backend: InMemorySearchIndexer)
  Requirement: Global Recall (graph-enhanced) >= 70%
  Result:      69.4%
  Status:      ❌ FAIL (by −0.6pp)
```

### Estado del backend

| Componente | Estado |
|------------|--------|
| Backend Quarkus | ✅ Running (java -jar, pid activo) |
| PostgreSQL | ✅ Running (Docker, puerto 5432) |
| Qdrant | ✅ Running (Docker, puerto 6333) |
| OpenAI Embeddings | ❌ No funcional (langchain4j extension missing) |
| Search backend | InMemorySearchIndexer (keyword Jaccard) |
| Graph traversal | ✅ BFS depth=3 funcional (PostgreSQL) |

---

## Glosario

- **BFS**: Breadth-First Search — algoritmo de recorrido de grafo nivel por nivel, usado en `/api/v2/graph/{id}?depth=N`.
- **Graph uplift**: Diferencia en hits entre búsqueda vectorial pura y búsqueda vectorial + grafo. Mide el valor añadido por las relaciones estructurales.
- **InMemorySearchIndexer**: Implementación de búsqueda por keywords usando similitud Jaccard (intersección/unión de tokens). Fallback cuando los embeddings de OpenAI no están disponibles.
- **Recall**: Proporción de documentos relevantes recuperados frente al total de documentos relevantes existentes.
- **Cross-dominio**: Query que requiere documentos de dos o más dominios distintos (ej. IT + CRM, Finance + Legal).
- **Multi-hop**: Query que requiere atravesar 3+ relaciones en el grafo para encontrar todos los documentos relevantes.
- **Entry point**: Nodo inicial desde el cual se expande el grafo, obtenido mediante búsqueda vectorial/de keywords.
