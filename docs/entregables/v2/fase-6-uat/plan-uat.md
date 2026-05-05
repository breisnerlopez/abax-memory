# Plan de UAT — Abax-Memory v2.0.0
- **Fase**: 6 — UAT (User Acceptance Testing)
- **Entregable**: plan-uat
- **Responsable**: business-analyst
- **Aprobado por**: product-owner (pendiente de revisión y firma)
- **Fecha**: 2026-05-03
- **Release**: v2.0.0
- **Estado**: Completado (pendiente de aprobación)
- **Fuentes**:
  - `docs/entregables/v2/fase-0-descubrimiento/historias-usuario.md` — 69 historias de usuario
  - `docs/entregables/v2/fase-0-descubrimiento/vision-producto.md` — 13 criterios de éxito (CE-01 a CE-013)
  - `docs/entregables/v2/fase-5-qa/casos-de-prueba.md` — 130 casos de prueba
  - `docs/entregables/v2/fase-5-qa/reporte-defectos.md` — 14 defectos (3 resueltos, 11 activos)

---

## Índice

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Alcance de UAT](#2-alcance-de-uat)
3. [Precondiciones y Ambiente](#3-precondiciones-y-ambiente)
4. [Escenarios UAT](#4-escenarios-uat)
5. [Criterios de Éxito UAT](#5-criterios-de-éxito-uat)
6. [Calendario UAT](#6-calendario-uat)
7. [Procedimiento de Aprobación](#7-procedimiento-de-aprobación)
8. [Riesgos UAT](#8-riesgos-uat)
9. [Matriz de Trazabilidad UAT → CE](#9-matriz-de-trazabilidad-uat--ce)
10. [Glosario](#10-glosario)

---

## 1. Resumen Ejecutivo

El **Plan de User Acceptance Testing (UAT)** para Abax-Memory v2.0.0 define cómo el usuario sponsor y los stakeholders validarán que el producto cumple con sus necesidades de negocio antes de la aceptación formal. El plan se construye sobre la base de 69 historias de usuario, 13 criterios de éxito de la Visión del Producto, 130 casos de prueba QA y 14 defectos documentados (3 críticos corregidos, 1 crítico remanente).

**Objetivo**: Verificar que el Motor de Memoria Genérica Multi-Dominio con IA satisface las necesidades de un usuario universal desde una perspectiva de negocio, no técnica.

**Estado actual pre-UAT**:
- 15 endpoints REST funcionales (con defectos conocidos)
- 6 pantallas frontend implementadas
- 101 tests unitarios ejecutados
- 96 pruebas QA ejecutadas (78.1% pass rate)
- 14 defectos identificados: 3 críticos resueltos, 1 crítico activo (BUG-004: relaciones), 5 medios, 3 bajos, 2 nuevos

> **Condición de entrada a UAT**: El defecto crítico BUG-004 (creación de relaciones retorna HTTP 500) debe estar corregido y verificado antes de iniciar la Sesión UAT-02 (Escenario de Relaciones y Grafo). Los demás defectos medios y bajos no bloquean el inicio de UAT pero deben documentarse como _known issues_ en cada escenario afectado.

---

## 2. Alcance de UAT

### 2.1 Qué funcionalidades se validan

| # | Funcionalidad | Épica(s) asociada(s) | Endpoints / Pantallas | Escenario UAT |
|---|---|---|---|---|
| 1 | CRUD de memorias con kinds universales | EP-001, EP-004 | `POST/GET/PUT /memories` | UAT-S01 |
| 2 | Búsqueda semántica con filtros | EP-005 | `POST /memories/search` | UAT-S02 |
| 3 | Perfiles de dominio configurables | EP-002 | Configuración de perfil + creación adaptada | UAT-S03 |
| 4 | Relaciones estructuradas y grafo | EP-001, EP-004 | `POST /relations`, `expandGraph` | UAT-S04 |
| 5 | Ciclo de vida y revisión humana | EP-001, EP-006 | `POST /memories/{id}/review` | UAT-S05 |
| 6 | Trazabilidad y auditoría | EP-006 | Registros de auditoría | UAT-S06 |
| 7 | Aislamiento multi-tenant | EP-003 | Validación cross-tenant | UAT-S07 |
| 8 | Extracción de entidades | EP-001, EP-004 | `POST /memories/extract` | UAT-S08 |
| 9 | Rate limiting | EP-004 (HU-004.13, Should) | Todos los endpoints | UAT-S09 |
| 10 | Latencia y rendimiento | EP-004, EP-005 | `POST /memories/search` | UAT-S10 |
| 11 | Frontend: creación multi-dominio | EP-009 | Pantalla de creación | UAT-S03 |
| 12 | Frontend: panel de búsqueda | EP-009 | Pantalla de búsqueda | UAT-S02 |
| 13 | Frontend: panel de revisión | EP-009 | Pantalla de revisión | UAT-S05 |
| 14 | Frontend: visualización de grafo | EP-009 | Componente de grafo | UAT-S04 |
| 15 | Frontend: panel de administración | EP-009 | Panel admin | UAT-S06, UAT-S07 |

### 2.2 Qué funcionalidades se excluyen (y por qué)

| # | Funcionalidad excluida | Justificación |
|---|---|---|
| 1 | **Batch ingest** (`POST /memories/ingest`) | Pertenece a EP-007 (Should) — fuera del MVP. El mecanismo de ingesta batch está especificado pero su implementación se difiere. |
| 2 | **Migración v1→v2** | Pertenece a EP-008 (Could). No hay datos de v1 en producción que migrar. |
| 3 | **SDK Python** | Pertenece a EP-010 (Should) — diferido. La validación se hará exclusivamente mediante la API REST y el frontend. |
| 4 | **Benchmarks BEIR / LoCoMo** | Los benchmarks (CE-01, CE-02, CE-03) requieren datasets públicos externos y se ejecutan como suite automatizada separada, no como parte de UAT manual. |
| 5 | **Multi-hop graph traversal (profundidad > 2)** | HU-005.05 es Should. Se valida expansión de grafo básica (depth=1), no navegación multi-hop avanzada. |
| 6 | **Re-indexación masiva** | HU-005.08 es Should. Funcionalidad de administración avanzada diferida. |
| 7 | **Endpoints administrativos avanzados** (purga física, fusión automática) | Diferidos. Las operaciones de depuración en UAT se limitan a soft-delete, archive y merge manual básico. |

### 2.3 Features críticas para el negocio

Las siguientes capacidades son **bloqueantes** para la aceptación del producto. Si cualquiera de ellas falla en UAT, el producto no puede ser aceptado:

| ID | Feature crítica | Justificación de criticidad | Escenario UAT |
|---|---|---|---|
| **FC-01** | Búsqueda semántica funcional con filtros | Es el core del producto. Sin búsqueda, el motor de memoria no tiene valor. | UAT-S02, UAT-S10 |
| **FC-02** | Aislamiento multi-tenant | Un tenant no puede ver datos de otro. Es requisito de seguridad y compliance no negociable. | UAT-S07 |
| **FC-03** | Ciclo de vida con revisión humana | La gobernanza es el diferenciador competitivo. Sin aprobación/rechazo, no hay control de calidad. | UAT-S05 |
| **FC-04** | CRUD de memorias con los 8 kinds | Crear y recuperar memorias es la operación básica. Los 8 kinds universales son obligatorios. | UAT-S01 |
| **FC-05** | English-Only en identificadores internos | Restricción no negociable (R-04). Todos los endpoints, enums y códigos de error deben estar en inglés. | Todos los escenarios |
| **FC-06** | Trazabilidad de operaciones | Restricción R-08. Toda mutación debe generar registro de auditoría. | UAT-S06 |

---

## 3. Precondiciones y Ambiente

### 3.1 Ambiente UAT

| Elemento | Valor |
|---|---|
| **Ambiente** | UAT — `http://localhost:8080` (mismo despliegue que QA, validado) |
| **Base URL API** | `http://localhost:8080/api/v2` |
| **Base URL Frontend** | `http://localhost:3000` |
| **Base de datos** | PostgreSQL `localhost:5432/pmoadb` |
| **Motor vectorial** | Qdrant `localhost:6333` |
| **Autenticación** | Keycloak (proveedor OIDC) con 5 roles |
| **Motor de embeddings** | OpenAI `text-embedding-3-large` (3072-dim) |

### 3.2 Datos pre-cargados requeridos

Antes de iniciar UAT, el ambiente debe tener:

| Dato | Cantidad | Propósito |
|---|---|---|
| **Memorias de prueba (seed data)** | Mínimo 50 memorias distribuidas en 2 tenants | Base para búsquedas semánticas y pruebas de aislamiento |
| **Memorias en estado `pending`** | Mínimo 5 | Escenarios de revisión (UAT-S05) |
| **Memorias en estado `draft`** | Mínimo 3 | Verificación de regla de visibilidad BR-001 |
| **Memorias con relaciones** | Mínimo 10 memorias con al menos 15 relaciones entre ellas | Escenarios de grafo (UAT-S04) |
| **Perfil Ops configurado** | 1 perfil | Escenario de perfiles (UAT-S03) |
| **Perfil Agent configurado** | 1 perfil | Escenario de perfiles (UAT-S03) |
| **Usuarios Keycloak** | 6 usuarios (2 tenants × 3 roles) | Ver todos los roles |

### 3.3 Usuarios de prueba UAT

| Usuario | Tenant | Roles | Uso en escenarios |
|---|---|---|---|
| `operador@alpha.local` | `tenant-alpha` | `memory-operator` | UAT-S01, UAT-S02, UAT-S04, UAT-S08 |
| `revisor@alpha.local` | `tenant-alpha` | `memory-reviewer` | UAT-S05 |
| `admin@system.local` | `cross-tenant` | `memory-admin` | UAT-S03, UAT-S06, UAT-S07, UAT-S09 |
| `auditor@system.local` | `cross-tenant` | `memory-auditor` | UAT-S06 |
| `consumer@alpha.local` | `tenant-alpha` | `api-consumer` | UAT-S02, UAT-S10 |
| `operador@bravo.local` | `tenant-bravo` | `memory-operator` | UAT-S07 |

### 3.4 Defectos conocidos (Known Issues) al inicio de UAT

Los siguientes defectos están documentados y son conocidos por el usuario sponsor antes de iniciar UAT. No bloquean el inicio pero pueden afectar escenarios específicos:

| Defecto | Severidad | Escenarios afectados | Impacto en UAT | Plan de contingencia |
|---|---|---|---|---|
| **BUG-004** | 🔴 Crítica | UAT-S04 (Relaciones y grafo) | Bloquea la creación de relaciones. El escenario UAT-S04 no puede ejecutarse. | **Corregir antes de Sesión UAT-02.** Si no se corrige, UAT-S04 se marca como BLOCKED y se re-agenda. |
| **BUG-005** | 🟡 Media | UAT-S09 (Rate limiting) | El rate limiting no está implementado. UAT-S09 espera fallar. | Ejecutar UAT-S09 para confirmar la ausencia. Registrar como deficiencia a resolver en v2.0.1. |
| **BUG-006** | 🟡 Media | Escenarios que usan frontend | CORS no configurado puede impedir acceso desde el frontend en ciertas configuraciones de navegador. | Verificar acceso frontend antes de cada sesión. Si falla, usar la API directamente (curl/Postman) para los escenarios afectados. |
| **BUG-013** | 🟡 Media | UAT-S01 (CRUD) | Kind inválido retorna 500 en vez de 400 con mensaje descriptivo. | Documentar en el resultado de UAT-S01. No bloquea el flujo positivo. |
| **BUG-009** | 🟡 Media | Todos los escenarios con POST | Headers incorrectos causan 500. | Usar siempre `Content-Type: application/json` y `Accept: application/json`. |

---

## 4. Escenarios UAT

### UAT-S01: Registrar una decisión de infraestructura y recuperarla después

| Campo | Detalle |
|---|---|
| **ID** | UAT-S01 |
| **Título** | "Como operador, registro una decisión de infraestructura y la recupero después" |
| **Objetivo de negocio** | Validar que un Memory Operator puede crear una memoria de tipo `decision` con metadata específica de infraestructura, y recuperarla posteriormente mediante búsqueda por ID y por texto libre. |
| **Usuario / Rol** | Memory Operator (`operador@alpha.local`, tenant `tenant-alpha`) |
| **Épicas cubiertas** | EP-001 (Motor Genérico), EP-004 (API REST v2), EP-005 (Búsqueda) |
| **Criterios de éxito vinculados** | CE-05 (precisión top-1), CE-06 (cobertura de kinds), CE-010 (English-Only) |
| **HU relacionadas** | HU-001.01.1, HU-004.01.1, HU-004.01.2, HU-005.01.1 |

#### Precondiciones
- Usuario `operador@alpha.local` autenticado con token JWT válido.
- Al menos 20 memorias pre-cargadas en `tenant-alpha` de diversos kinds para validar que la búsqueda discrimina correctamente.
- Backend y Qdrant operativos.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v2/memories` con payload de decisión de infraestructura (ver Datos de prueba). | `HTTP 201`. Body contiene `id` con formato UUID, `kind: "decision"`, `lifecycle.status: "active"`. |
| 2 | Extraer el `id` retornado (ej: `dec-550e8400-...`). | ID capturado para pasos siguientes. |
| 3 | `GET /api/v2/memories/{id}` con el ID del paso 2. | `HTTP 200`. Mismo contenido, `kind`, `topics`, `entities` y `metadata` que los enviados. |
| 4 | `POST /api/v2/memories/search` con `query: "cambio de base de datos vectorial"`. | `HTTP 200`. La memoria creada aparece en los primeros 3 resultados (top-3). |
| 5 | `POST /api/v2/memories/search` con filtro `kinds: ["decision"]` y `query: "migración"`. | `HTTP 200`. La memoria creada aparece en resultados. Ningún resultado tiene `kind` distinto de `decision`. |
| 6 | `PUT /api/v2/memories/{id}` actualizando `lifecycle.confidence` de `0.85` a `0.90`. | `HTTP 200`. `confidence` actualizado a `0.90`. `updatedAt` es posterior al `createdAt` original. |
| 7 | `GET /api/v2/memories/{id}` para verificar la actualización. | `HTTP 200`. `confidence: 0.90`. |

#### Datos de prueba

```json
{
  "kind": "decision",
  "title": "Migración de Qdrant a Milvus para escalabilidad horizontal",
  "content": "Se decidió migrar el motor vectorial de Qdrant a Milvus a partir del Q3 2026. La decisión se basó en: (1) necesidad de escalado horizontal nativo, (2) mejor soporte para índices HNSW con >10M vectores, (3) integración más sencilla con el ecosistema Kubernetes. Riesgo identificado: curva de aprendizaje del equipo en Milvus. Plan de mitigación: PoC de 4 semanas antes de migración completa.",
  "summary": "Decisión: migrar vector DB a Milvus en Q3 2026",
  "topics": ["infrastructure", "vector-database", "migration", "scalability"],
  "entities": ["Qdrant", "Milvus", "Kubernetes"],
  "metadata": {
    "affectedService": "abax-memory-search",
    "decisionOwner": "architecture-team",
    "reviewDate": "2026-09-01",
    "riskLevel": "medium"
  },
  "source": {
    "type": "manual",
    "id": "arch-review-2026-05"
  },
  "scope": {
    "tenantId": "tenant-alpha",
    "userId": "operador@alpha.local",
    "namespace": "infrastructure-decisions"
  },
  "lifecycle": {
    "confidence": 0.85,
    "importance": 0.8,
    "sensitivity": "internal"
  }
}
```

#### Criterio de aceptación UAT
- **Given** un Memory Operator autenticado en `tenant-alpha`
- **When** crea una memoria `decision` con metadata de infraestructura, luego la busca por ID y por query semántica
- **Then**:
  - La creación retorna `HTTP 201` con `kind: "decision"` y estado `active`.
  - `GET /memories/{id}` retorna exactamente los mismos `title`, `content`, `topics`, `entities` y `metadata`.
  - `POST /memories/search` con query semántica ubica la memoria en el top-3 de resultados.
  - El filtro `kinds` restringe correctamente los resultados.
  - La actualización de `confidence` se persiste y es verificable.
  - Todos los endpoints, enums y códigos de error están en inglés.

---

### UAT-S02: Buscar información sobre un incidente pasado

| Campo | Detalle |
|---|---|
| **ID** | UAT-S02 |
| **Título** | "Como analista, busco información sobre un incidente pasado" |
| **Objetivo de negocio** | Validar que un Memory Consumer puede encontrar información relevante sobre incidentes pasados usando búsqueda semántica con filtros combinados, y que el motor retorna resultados con scores significativos y metadatos útiles para la toma de decisiones. |
| **Usuario / Rol** | Memory Consumer (`consumer@alpha.local`, tenant `tenant-alpha`) |
| **Épicas cubiertas** | EP-005 (Búsqueda Semántica + Graph) |
| **Criterios de éxito vinculados** | CE-04 (latencia < 500ms), CE-05 (precisión top-1 ≥ 0.92), CE-08 (visibilidad por estado) |
| **HU relacionadas** | HU-005.01.1, HU-005.02.1, HU-005.03.1, HU-005.04.1 |

#### Precondiciones
- 5+ memorias pre-cargadas en `tenant-alpha` con `kind: "event"`, `topics` incluyendo `["incident", "outage", "payment-api"]`, estados variados.
- Al menos 1 memoria en `draft` y 1 en `archived` sobre el mismo tema para validar BR-001 (visibilidad por defecto).
- Usuario `consumer@alpha.local` autenticado.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v2/memories/search` con `query: "caída del servicio de pagos en producción"` sin filtros adicionales. | `HTTP 200`. Resultados relevantes sobre incidentes de payment-api. **No** aparecen memorias en `draft`, `archived`, `rejected` ni `deleted` (BR-001). |
| 2 | Verificar que el score del primer resultado es ≥ 0.70. | El `score` del top-1 es numérico y ≥ 0.70 para una query bien relacionada. |
| 3 | `POST /api/v2/memories/search` con la misma query + filtros: `kinds: ["event"]`, `topics: ["incident", "outage"]`. | Resultados filtrados: solo `event`s con topics `incident` o `outage`. |
| 4 | Agregar filtro `createdAfter: "2026-01-01"` y `createdBefore: "2026-06-01"`. | Solo eventos dentro del rango de fechas. |
| 5 | Agregar filtro `importanceMin: 0.7`. | Solo memorias con `importance >= 0.7`. |
| 6 | Activar `expandGraph: true` en la búsqueda. | Cada resultado incluye sus relaciones directas (depth=1) si las tiene. |
| 7 | Medir tiempo de respuesta para el paso 1 (usando `curl -w` o herramienta de medición). | `time_total < 0.5s` (500ms) para la búsqueda semántica (CE-04). |
| 8 | Verificar que el frontend (si está disponible) muestra los resultados con: score, kind, status codificado por color, summary, topics como chips, y entities como badges. | Interfaz consistente con HU-009.02.1. |

#### Datos de prueba

**Query 1 (búsqueda libre)**:
```json
{
  "query": "caída del servicio de pagos en producción"
}
```

**Query 2 (con filtros)**:
```json
{
  "query": "caída del servicio de pagos en producción",
  "kinds": ["event"],
  "topics": ["incident", "outage"],
  "createdAfter": "2026-01-01T00:00:00Z",
  "createdBefore": "2026-06-01T00:00:00Z",
  "importanceMin": 0.7,
  "expandGraph": true
}
```

#### Criterio de aceptación UAT
- **Given** un Memory Consumer en `tenant-alpha` con 5+ eventos de incidentes pre-cargados (incluyendo algunos en `draft` y `archived`)
- **When** ejecuta búsqueda semántica con query "caída del servicio de pagos", con y sin filtros
- **Then**:
  - La búsqueda sin filtros retorna solo memorias `active` (BR-001).
  - Los resultados son semánticamente relevantes (score ≥ 0.70 para el top-1).
  - Los filtros combinados (`kinds`, `topics`, rango de fechas, `importanceMin`) reducen correctamente el conjunto de resultados.
  - `expandGraph: true` incluye relaciones directas en cada resultado.
  - El tiempo de respuesta p95 es < 500ms.
  - El frontend renderiza resultados con score, kind, status, summary, topics y entities.

---

### UAT-S03: Configurar un perfil de dominio para una industria

| Campo | Detalle |
|---|---|
| **ID** | UAT-S03 |
| **Título** | "Como administrador, configuro un perfil de dominio para mi industria" |
| **Objetivo de negocio** | Validar que un Memory Administrator puede configurar un perfil de dominio (ej. Ops Profile) que adapte el comportamiento del motor genérico —kinds recomendados, tags sugeridos, defaults de sensibilidad— y que un operador vea reflejada esa configuración al crear memorias. |
| **Usuario / Rol** | Memory Administrator (`admin@system.local`) configura el perfil; Memory Operator verifica la experiencia adaptada. |
| **Épicas cubiertas** | EP-002 (Perfiles de Dominio), EP-009 (Frontend Multi-Dominio) |
| **Criterios de éxito vinculados** | CE-06 (cobertura de kinds), CE-010 (English-Only) |
| **HU relacionadas** | HU-002.01.1, HU-002.02.1, HU-002.03.1, HU-009.01.1, HU-009.06.1 |

#### Precondiciones
- Usuario `admin@system.local` autenticado.
- Endpoint o mecanismo de configuración de perfiles disponible (ver Nota).
- Frontend accesible en `http://localhost:3000`.

> **Nota**: La configuración de perfiles de dominio en el MVP se realiza mediante archivos de configuración (JSON/YAML) o registros en base de datos. Si el endpoint administrativo de perfiles no está implementado, este escenario se adapta para usar configuración directa con verificación funcional del comportamiento resultante.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Como admin, crear/activar un perfil "Ops Profile" con: kinds recomendados `["event", "procedure", "decision", "fact"]`, tags sugeridos `["incident", "runbook", "deployment", "alert"]`, sensitivity default `internal`, campos metadata extra `["affectedService", "remediationSteps", "rootCause"]`. | Perfil creado/activado exitosamente. |
| 2 | Como operador (`operador@alpha.local`), abrir el frontend y seleccionar "Ops Profile" del selector de perfil. | La interfaz se reconfigura: el selector de `kind` destaca `event`, `procedure`, `decision`, `fact`. |
| 3 | Abrir el formulario de creación de memoria. Verificar que: (a) el campo `kind` muestra sugerencias priorizadas, (b) el campo `tags` sugiere `incident`, `runbook`, `deployment`, `alert`, (c) aparecen campos metadata extra `affectedService`, `remediationSteps`, `rootCause`. | Formulario adaptado al perfil Ops. |
| 4 | Crear una memoria sin especificar `sensitivity`. | `HTTP 201`. `lifecycle.sensitivity` es `internal` (default del perfil Ops). |
| 5 | Cambiar al perfil "Agent" (si está configurado) y verificar que: (a) kinds destacados son `fact`, `preference`, `event`, `decision`, (b) `scope.userId` y `scope.sessionId` son prominentes, (c) sensitivity default es `confidential`. | Interfaz reconfigurada para Agent. |
| 6 | Volver a "Sin perfil" (core genérico) y verificar que: (a) los 8 kinds aparecen sin priorización, (b) sin tags sugeridos, (c) defaults neutros (`importance: 0.5`, `sensitivity: internal`). | Formulario genérico sin especialización. |

#### Datos de prueba

**Configuración de perfil Ops** (`ops-profile.json`):
```json
{
  "profileId": "ops-profile",
  "name": "Ops Profile",
  "description": "IT Operations — incidentes, runbooks, deployments, postmortems",
  "recommendedKinds": ["event", "procedure", "decision", "fact"],
  "suggestedTags": ["incident", "runbook", "deployment", "alert", "postmortem"],
  "defaultSensitivity": "internal",
  "defaultImportance": 0.5,
  "extraMetadataFields": ["affectedService", "remediationSteps", "rootCause"]
}
```

#### Criterio de aceptación UAT
- **Given** un Memory Administrator configura un perfil de dominio (Ops) y un Memory Operator lo selecciona en el frontend
- **When** el operador crea memorias bajo ese perfil
- **Then**:
  - El formulario de creación refleja los kinds recomendados, tags sugeridos y campos metadata extra del perfil.
  - Los defaults del perfil (`sensitivity`, `importance`) se aplican automáticamente si el operador no los especifica.
  - El cambio entre perfiles (Ops → Agent → Sin perfil) reconfigura la interfaz sin recargar la página.
  - La selección de perfil se mantiene al recargar la página.

---

### UAT-S04: Relacionar dos fragmentos y navegar el grafo

| Campo | Detalle |
|---|---|
| **ID** | UAT-S04 |
| **Título** | "Como usuario, relaciono dos fragmentos y navego el grafo" |
| **Objetivo de negocio** | Validar que un Memory Operator puede establecer relaciones tipadas entre memorias, y que un Knowledge Searcher puede navegar el grafo de relaciones visualmente para descubrir conexiones de conocimiento. |
| **Usuario / Rol** | Memory Operator crea relaciones; Knowledge Searcher navega el grafo en el frontend. |
| **Épicas cubiertas** | EP-001 (Motor Genérico), EP-004 (API REST v2), EP-009 (Frontend) |
| **Criterios de éxito vinculados** | CE-011 (9/9 tipos de relación operativos), CE-05 (precisión) |
| **HU relacionadas** | HU-001.07.1, HU-001.08.1, HU-004.01.5, HU-009.05.1 |

#### Precondiciones
- ⚠️ **BLOQUEANTE**: BUG-004 debe estar corregido. `POST /api/v2/relations` debe retornar `HTTP 201`.
- Al menos 5 memorias pre-cargadas en `tenant-alpha` (o creadas en-session) para establecer relaciones.
- Usuario `operador@alpha.local` autenticado.
- Frontend con componente de grafo funcional.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Crear dos memorias: (a) `event` sobre un outage, (b) `procedure` sobre el runbook de recuperación. | Ambas retornan `HTTP 201` con sus respectivos IDs. |
| 2 | `POST /api/v2/relations` creando relación `caused_by` desde el `procedure` (source) hacia el `event` (target). | `HTTP 201`. Relación creada con `type: "caused_by"`. |
| 3 | `GET /api/v2/memories/{id_event}` con `expandGraph: true`. | `HTTP 200`. La respuesta incluye la relación inversa: el event tiene una relación `caused_by` desde el procedure. |
| 4 | `POST /api/v2/relations` creando relación `resolves` desde el `procedure` hacia el `event`. | `HTTP 201`. Ahora hay 2 relaciones entre las mismas memorias con tipos distintos. |
| 5 | Probar al menos 5 de los 9 tipos de relación: `related_to`, `depends_on`, `caused_by`, `resolves`, `supports`. | `HTTP 201` para cada tipo. |
| 6 | En el frontend, abrir el detalle de una de las memorias y activar la vista de grafo. | El grafo muestra: nodos (memorias con `kind`, `status` por color, `summary` truncado) y edges (líneas con etiqueta del tipo de relación y dirección de flecha). |
| 7 | Hacer clic en un nodo vecino para expandir sus relaciones. | El grafo se expande mostrando las relaciones del nodo clickeado (navegación progresiva). |
| 8 | `DELETE /api/v2/relations/{relationId}` para eliminar una relación. | `HTTP 204` o `HTTP 200`. La relación desaparece del grafo al refrescar. |

#### Datos de prueba

**Memoria A (evento)**:
```json
{
  "kind": "event",
  "title": "Outage: payment-api indisponible 45 minutos",
  "content": "El servicio payment-api estuvo indisponible el 2026-05-02 de 14:00 a 14:45 UTC. Causa raíz: saturación del pool de conexiones PostgreSQL por un deployment sin connection pooling adecuado.",
  "topics": ["incident", "outage", "payment-api"],
  "entities": ["payment-api", "PostgreSQL"],
  "scope": { "tenantId": "tenant-alpha" }
}
```

**Memoria B (procedimiento)**:
```json
{
  "kind": "procedure",
  "title": "Runbook: Recuperación de payment-api tras saturación de conexiones",
  "content": "Pasos: 1. Verificar métricas de conexiones en Grafana dashboard 'PG Pool'. 2. Si connections > 80% del max, ejecutar `kubectl rollout restart deploy/payment-api`. 3. Validar health endpoint. 4. Escalar horizontalmente si es recurrente.",
  "topics": ["runbook", "payment-api", "recovery"],
  "entities": ["payment-api", "Grafana", "Kubernetes"],
  "scope": { "tenantId": "tenant-alpha" }
}
```

**Relación 1**:
```json
{
  "sourceId": "<ID_MEMORIA_B>",
  "targetId": "<ID_MEMORIA_A>",
  "relationType": "caused_by"
}
```

**Relación 2**:
```json
{
  "sourceId": "<ID_MEMORIA_B>",
  "targetId": "<ID_MEMORIA_A>",
  "relationType": "resolves"
}
```

#### Criterio de aceptación UAT
- **Given** dos memorias existentes en el mismo tenant
- **When** un Memory Operator crea relaciones tipadas entre ellas y un Knowledge Searcher navega el grafo
- **Then**:
  - Se pueden crear relaciones de al menos 5 de los 9 tipos (`related_to`, `depends_on`, `caused_by`, `resolves`, `supports`).
  - `GET /memories/{id}` con `expandGraph` retorna las relaciones directas.
  - El frontend renderiza el grafo con nodos y edges dirigidos etiquetados.
  - La navegación progresiva (clic en nodo → expandir) funciona.
  - `DELETE /relations/{id}` elimina la relación correctamente.

---

### UAT-S05: Aprobar una memoria pendiente

| Campo | Detalle |
|---|---|
| **ID** | UAT-S05 |
| **Título** | "Como revisor, apruebo una memoria pendiente" |
| **Objetivo de negocio** | Validar el flujo completo del ciclo de vida con revisión humana: desde que un operador crea una memoria de alta importancia que requiere revisión, hasta que un revisor la aprueba (o rechaza con motivo). |
| **Usuario / Rol** | Memory Operator crea la memoria; Memory Reviewer (`revisor@alpha.local`) la revisa. |
| **Épicas cubiertas** | EP-001 (Motor Genérico), EP-006 (Gobernanza y Trazabilidad) |
| **Criterios de éxito vinculados** | CE-08 (visibilidad por estado), CE-09 (trazabilidad 100%) |
| **HU relacionadas** | HU-001.03.1, HU-001.04.1, HU-006.03.1, HU-006.04.1, HU-009.03.1 |

#### Precondiciones
- Usuario `operador@alpha.local` y `revisor@alpha.local` autenticados.
- Al menos 2 memorias pre-cargadas en estado `pending` para el flujo de rechazo.
- Frontend con panel de revisión funcional.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Como operador, `POST /api/v2/memories` con `lifecycle.importance: 0.8` y `lifecycle.sensitivity: "confidential"`. NO especificar `lifecycle.status`. | `HTTP 201`. El sistema asigna automáticamente `lifecycle.status: "draft"` (BR-006: alta importancia + sensibilidad elevada → no puede ser `active` directamente). |
| 2 | Como operador, enviar la memoria a revisión: `POST /api/v2/memories/{id}/review` con `action: "submit"`. | `HTTP 200`. `lifecycle.status` transiciona de `draft` a `pending`. |
| 3 | Como consumer, `POST /api/v2/memories/search` con query relacionada. | `HTTP 200`. La memoria `pending` **no aparece** en resultados (BR-001: solo `active` por defecto). |
| 4 | Como revisor, acceder al panel de revisión en el frontend. | Se listan las memorias `pending` en el scope del revisor, mostrando `kind`, `importance`, `sensitivity`, fecha de envío y operador solicitante. |
| 5 | Como revisor, abrir el detalle de la memoria creada en el paso 1. | Se ve contenido completo, historial de cambios (al menos la transición `draft → pending`), relaciones, botones de acción. |
| 6 | Como revisor, aprobar: `POST /api/v2/memories/{id}/review` con `action: "approve"`. | `HTTP 200`. `lifecycle.status` transiciona a `active`. La memoria desaparece de la bandeja de pendientes. |
| 7 | Como consumer, repetir la búsqueda del paso 3. | Ahora la memoria **sí aparece** en resultados (está `active`). |
| 8 | Con otra memoria `pending`, como revisor ejecutar `POST /api/v2/memories/{id}/review` con `action: "reject"` y `comment: "El contenido carece de fuentes verificables. Incluir referencias."`. | `HTTP 200`. `lifecycle.status` transiciona a `rejected`. El `comment` se registra. |
| 9 | Verificar en el frontend que la memoria rechazada muestra el motivo del rechazo. | El operador puede ver el `comment` del revisor. |

#### Datos de prueba

**Memoria de alta importancia**:
```json
{
  "kind": "decision",
  "title": "Aprobación de arquitectura: microservicios con event sourcing",
  "content": "Se aprueba la migración de la arquitectura monolítica a microservicios con event sourcing y CQRS para los módulos de billing y notificaciones. Esta decisión impacta a 4 equipos y requiere reentrenamiento.",
  "topics": ["architecture", "microservices", "event-sourcing", "cqrs"],
  "entities": ["billing-module", "notifications-module"],
  "scope": { "tenantId": "tenant-alpha", "userId": "operador@alpha.local" },
  "lifecycle": {
    "importance": 0.8,
    "sensitivity": "confidential",
    "confidence": 0.9
  }
}
```

#### Criterio de aceptación UAT
- **Given** un Memory Operator crea una memoria con `importance >= 0.7` y `sensitivity: "confidential"`
- **When** el operador la envía a revisión y un Memory Reviewer la evalúa
- **Then**:
  - La memoria se crea en `draft` (no `active`) automáticamente (BR-006).
  - `draft` y `pending` no son visibles en búsquedas sin filtro explícito (BR-001).
  - El revisor puede aprobar (`pending → active`) o rechazar (`pending → rejected`) con comentario.
  - Tras aprobación, la memoria es visible en búsquedas normales.
  - El historial de cambios registra cada transición (trazabilidad CE-09).
  - El frontend muestra la bandeja de pendientes, detalle con historial y botones de acción.

---

### UAT-S06: Revisar la trazabilidad de cambios

| Campo | Detalle |
|---|---|
| **ID** | UAT-S06 |
| **Título** | "Como auditor, reviso la trazabilidad de cambios" |
| **Objetivo de negocio** | Validar que toda mutación sobre memorias y relaciones genera registros de auditoría completos (timestamp, usuario, acción, diff antes/después), y que un auditor puede consultar, filtrar y verificar la integridad de la trazabilidad. |
| **Usuario / Rol** | Memory Auditor (`auditor@system.local`) |
| **Épicas cubiertas** | EP-006 (Gobernanza y Trazabilidad) |
| **Criterios de éxito vinculados** | CE-09 (trazabilidad 100%) |
| **HU relacionadas** | HU-006.01.1, HU-006.02.1, HU-006.08.1 |

#### Precondiciones
- Usuario `auditor@system.local` autenticado con rol `memory-auditor`.
- Al menos 10 mutaciones previas ejecutadas (creaciones, actualizaciones, cambios de estado, soft-deletes) para tener auditoría poblada.
- Endpoint de consulta de auditoría disponible (ver Nota).

> **Nota**: Si el endpoint dedicado de consulta de auditoría no está implementado, la verificación se realiza consultando directamente los registros en base de datos (tabla `audit_log`) o mediante el endpoint de estadísticas del tenant.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Como operador, ejecutar 3 mutaciones sobre una memoria existente: (a) actualizar `title`, (b) cambiar `lifecycle.status` de `active` a `archived`, (c) `DELETE` (soft-delete). | Las 3 operaciones se completan exitosamente. |
| 2 | Como auditor, consultar los registros de auditoría para la memoria modificada. | Se listan al menos 4 registros (creación original + 3 mutaciones). |
| 3 | Verificar que cada registro contiene: `timestamp`, `userId`, `action`, `resourceType` (`memory`/`relation`), `resourceId`, `diff` (antes/después). | Todos los campos están presentes y poblados. |
| 4 | Filtrar auditoría por `userId: "operador@alpha.local"`. | Solo aparecen registros del operador especificado. |
| 5 | Filtrar auditoría por `action: "status_change"`. | Solo aparecen registros de cambios de estado. |
| 6 | Verificar el `diff` de la operación de actualización de `title`. | El `diff` muestra `{ "before": { "title": "..." }, "after": { "title": "..." } }`. |
| 7 | Verificar que el soft-delete generó registro con `action: "soft_delete"` y `diff` mostrando el estado anterior (`active`) y el nuevo (`deleted`). | Registro de auditoría presente y correcto. |
| 8 | Como auditor, intentar crear una memoria (`POST /api/v2/memories`). | `HTTP 403 Forbidden`. El rol `memory-auditor` es solo lectura. |

#### Datos de prueba

No se requieren datos de prueba adicionales. Se utilizan las mutaciones ejecutadas en los escenarios previos (UAT-S01, UAT-S05) como fuente de registros de auditoría.

#### Criterio de aceptación UAT
- **Given** se han ejecutado múltiples mutaciones sobre memorias durante la sesión UAT
- **When** un Memory Auditor consulta los registros de auditoría
- **Then**:
  - El 100% de las mutaciones tienen registro de auditoría (CE-09).
  - Cada registro contiene `timestamp`, `userId`, `action`, `resourceType`, `resourceId`, `diff`.
  - El `diff` refleja correctamente el estado antes y después de cada mutación.
  - Los filtros por `userId` y `action` funcionan.
  - El rol `memory-auditor` no puede ejecutar mutaciones (HTTP 403).

---

### UAT-S07: Validar aislamiento multi-tenant

| Campo | Detalle |
|---|---|
| **ID** | UAT-S07 |
| **Título** | "Como usuario multi-tenant, no puedo ver datos de otro tenant" |
| **Objetivo de negocio** | Validar que el scoping multi-tenant es estricto: un usuario del Tenant A no puede ver, modificar ni acceder a memorias del Tenant B bajo ninguna circunstancia, a menos que tenga permisos cross-tenant explícitos (admin). |
| **Usuario / Rol** | Memory Operator (`operador@alpha.local` y `operador@bravo.local`), Memory Admin (`admin@system.local`) |
| **Épicas cubiertas** | EP-003 (Scoping Multi-Tenant) |
| **Criterios de éxito vinculados** | CE-07 (aislamiento 100%) |
| **HU relacionadas** | HU-003.01.1, HU-003.02.1, HU-003.03.1, HU-003.04.1 |

#### Precondiciones
- 10+ memorias en `tenant-alpha`, 10+ memorias en `tenant-bravo`.
- Usuarios `operador@alpha.local` y `operador@bravo.local` autenticados cada uno en su tenant respectivo.
- Usuario `admin@system.local` con permisos cross-tenant.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Como `operador@alpha.local`, `POST /api/v2/memories/search` con `query: "*"` o query genérica. | `HTTP 200`. **Solo** aparecen memorias de `tenant-alpha`. Ningún resultado de `tenant-bravo`. |
| 2 | Como `operador@alpha.local`, obtener el ID de una memoria de `tenant-bravo` (conocido por el tester) e intentar `GET /api/v2/memories/{id_bravo}`. | `HTTP 404 Not Found` o `HTTP 403 Forbidden`. No se revela la existencia de la memoria. |
| 3 | Como `operador@alpha.local`, intentar crear una memoria con `scope.tenantId: "tenant-bravo"`. | `HTTP 403 Forbidden` o `HTTP 400`. El `tenantId` del token no coincide con el del body. |
| 4 | Como `operador@alpha.local`, intentar actualizar una memoria de `tenant-bravo` vía `PUT`. | `HTTP 404` o `HTTP 403`. |
| 5 | Como `operador@bravo.local`, repetir los pasos 1 y 2 en dirección inversa. | Mismo resultado: solo ve datos de `tenant-bravo`. |
| 6 | Como `admin@system.local`, ejecutar búsqueda cross-tenant (sin filtro de `tenantId` o con filtro que abarque ambos). | `HTTP 200`. El admin puede ver memorias de ambos tenants si tiene permisos explícitos. |
| 7 | Como `admin@system.local`, `GET /api/v2/scopes/tenant-alpha/stats` y `GET /api/v2/scopes/tenant-bravo/stats`. | Estadísticas separadas y correctas por tenant. |

#### Datos de prueba

No se requieren datos específicos. Se utilizan las memorias pre-cargadas de ambos tenants.

#### Criterio de aceptación UAT
- **Given** dos tenants (`tenant-alpha`, `tenant-bravo`) con datos independientes
- **When** un operador del Tenant A intenta acceder a datos del Tenant B
- **Then**:
  - Las búsquedas solo retornan resultados del tenant del usuario autenticado (100% de aislamiento, CE-07).
  - `GET /memories/{id}` de otro tenant retorna 404 (no se revela existencia).
  - No se puede crear una memoria en un tenant distinto al del token.
  - El admin cross-tenant sí puede acceder a múltiples tenants con permisos explícitos.
  - Las estadísticas por tenant son correctas y están aisladas.

---

### UAT-S08: Extraer entidades de un texto

| Campo | Detalle |
|---|---|
| **ID** | UAT-S08 |
| **Título** | "Como usuario, extraigo entidades de un texto" |
| **Objetivo de negocio** | Validar que el endpoint de extracción de entidades identifica correctamente personas, organizaciones, tecnologías, ubicaciones y otros tipos de entidades desde texto libre, y que las entidades extraídas pueden usarse para vincular y buscar memorias. |
| **Usuario / Rol** | Memory Operator (`operador@alpha.local`) |
| **Épicas cubiertas** | EP-001 (Motor Genérico), EP-004 (API REST v2) |
| **Criterios de éxito vinculados** | CE-05 (precisión), CE-010 (English-Only) |
| **HU relacionadas** | HU-001.09.1, HU-004.01.7 |

#### Precondiciones
- Usuario `operador@alpha.local` autenticado.
- Endpoint `POST /api/v2/memories/extract` operativo.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v2/memories/extract` con un texto técnico sobre infraestructura (ver Datos de prueba). | `HTTP 200`. Body contiene un array de entidades detectadas con `name`, `type` y `confidence`. |
| 2 | Verificar que las entidades esperadas aparecen: `"Kubernetes"`, `"AWS"`, `"PostgreSQL"`, `"Terraform"`. | Entidades listadas con `type` apropiado (`technology`, `platform`, etc.) y `confidence > 0`. |
| 3 | Verificar que ninguna entidad es inventada (alucinación). | Solo entidades mencionadas en el texto. |
| 4 | Crear una memoria usando las entidades extraídas en el campo `entities`. | `HTTP 201`. Las entidades se asocian a la memoria. |
| 5 | `GET /api/v2/entities/{entityName}` (ej: `Kubernetes`). | `HTTP 200`. Retorna la entidad y las memorias vinculadas a ella. |
| 6 | `POST /api/v2/memories/search` filtrando por `entities: ["Kubernetes"]`. | Resultados incluyen memorias vinculadas a la entidad `Kubernetes`. |

#### Datos de prueba

**Texto para extracción**:
```json
{
  "text": "El equipo de SRE migró los workloads de Kubernetes desde GCP a AWS en Q1 2026. Utilizaron Terraform para la infraestructura como código y PostgreSQL en RDS para la capa de persistencia. El pipeline de CI/CD se implementó con GitHub Actions y ArgoCD. La latencia p95 se redujo de 320ms a 180ms tras la migración."
}
```

#### Criterio de aceptación UAT
- **Given** un texto con entidades nombradas (tecnologías, plataformas, servicios)
- **When** un Memory Operator invoca el endpoint de extracción de entidades
- **Then**:
  - El sistema retorna entidades detectadas con `name`, `type` y `confidence`.
  - Las entidades esperadas (`Kubernetes`, `AWS`, `PostgreSQL`, `Terraform`) aparecen en los resultados.
  - No se generan entidades ficticias no mencionadas en el texto.
  - Las entidades extraídas se pueden usar en el campo `entities` al crear una memoria.
  - `GET /entities/{name}` retorna memorias vinculadas.

---

### UAT-S09: Verificar que rate limiting funciona

| Campo | Detalle |
|---|---|
| **ID** | UAT-S09 |
| **Título** | "Como administrador, verifico que rate limiting funciona" |
| **Objetivo de negocio** | Validar que el sistema aplica rate limiting para proteger los endpoints contra abuso, o documentar formalmente su ausencia como riesgo aceptado para el MVP. |
| **Usuario / Rol** | Memory Administrator (`admin@system.local`) |
| **Épicas cubiertas** | EP-004 (API REST v2) — HU-004.13 (Should) |
| **Criterios de éxito vinculados** | CE-04 (latencia) — el rate limiting impacta indirectamente la latencia bajo carga |
| **HU relacionadas** | HU-004.13.1 (Should — no bloquea MVP) |

#### Precondiciones
- ⚠️ **KNOWN ISSUE**: BUG-005 documenta que rate limiting no está implementado (20/20 requests retornan HTTP 200).
- Este escenario se ejecuta con expectativa de **fallo controlado** (expected failure).

#### Flujo paso a paso

| Paso | Acción | Resultado esperado (ideal) | Resultado real (con BUG-005) |
|---|---|---|---|
| 1 | Enviar 30 requests `GET /api/v2/memories?query=test` en rápida sucesión (< 1 segundo entre requests) desde el mismo token. | Al menos 1 request retorna `HTTP 429 Too Many Requests` con header `Retry-After`. | Los 30 requests retornan `HTTP 200`. |
| 2 | Enviar 50 requests `POST /api/v2/memories/search` con body válido en < 2 segundos. | Rate limiting aplica. Requests excedentes retornan 429. | Todos retornan 200 (o error por carga, no por throttling). |
| 3 | Documentar el resultado en el acta de UAT. | Rate limiting operativo. | **Deficiencia documentada**: rate limiting no implementado (BUG-005). Se acepta como riesgo para MVP con plan de inclusión en v2.0.1. |

#### Datos de prueba

Script de prueba (bash):
```bash
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: tenant-alpha" \
    "http://localhost:8080/api/v2/memories?query=test" &
done
wait
```

#### Criterio de aceptación UAT
- **Given** un endpoint público de la API
- **When** se envían 30+ requests en rápida sucesión desde un mismo token
- **Then (ideal)**:
  - El sistema retorna `HTTP 429 Too Many Requests` para requests que exceden el límite.
  - La respuesta incluye header `Retry-After` con el tiempo de espera sugerido.
- **Then (realidad actual con BUG-005)**:
  - Se confirma la ausencia de rate limiting.
  - Se registra como deficiencia aceptada para MVP.
  - Se acuerda fecha target para implementación (v2.0.1 o v2.1.0).

---

### UAT-S10: Validar que el sistema responde en < 500ms

| Campo | Detalle |
|---|---|
| **ID** | UAT-S10 |
| **Título** | "Como usuario, valido que el sistema responde en < 500ms" |
| **Objetivo de negocio** | Validar que la latencia de búsqueda semántica cumple con el criterio de éxito CE-04 (p95 < 500ms) bajo condiciones de carga representativas. |
| **Usuario / Rol** | Memory Consumer (`consumer@alpha.local`) |
| **Épicas cubiertas** | EP-004 (API REST v2), EP-005 (Búsqueda Semántica) |
| **Criterios de éxito vinculados** | CE-04 (latencia p95 < 500ms) |
| **HU relacionadas** | HU-005.01.1, HU-005.03.1 |

#### Precondiciones
- 10,000+ memorias cargadas en `tenant-alpha` para tener un volumen representativo (o el volumen máximo disponible en el ambiente).
- 3 tenants activos con datos.
- Backend, PostgreSQL y Qdrant operativos y en condiciones equivalentes al entorno productivo previsto.

#### Flujo paso a paso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Ejecutar 50 búsquedas semánticas (`POST /api/v2/memories/search`) con queries variadas usando `curl -w` para medir `time_total`. | Se registran los tiempos de cada request. |
| 2 | Calcular p95 de los tiempos de respuesta. | p95 < 500ms. |
| 3 | Ejecutar 50 búsquedas con filtros adicionales (`kinds`, `topics`, `importanceMin`, `expandGraph: true`). | p95 de búsquedas con filtros < 800ms (métrica secundaria; la primaria es búsqueda simple). |
| 4 | Ejecutar 50 `GET /api/v2/memories/{id}` para medir latencia de lectura directa. | p95 < 200ms para lectura por ID. |
| 5 | Ejecutar 20 `POST /api/v2/memories` para medir latencia de escritura (incluye generación de embedding vía OpenAI). | p95 < 2s para creación de memoria (incluye llamada a API externa de embeddings). |
| 6 | Documentar todos los percentiles (p50, p90, p95, p99) en el acta de UAT. | Métricas registradas para comparación futura y aceptación formal. |

#### Datos de prueba

Queries de búsqueda variadas (seleccionar 10 queries distintas y ejecutar cada una 5 veces):

1. `"database connection pool exhaustion"`
2. `"kubernetes deployment rollout restart"`
3. `"incident response procedure for payment service"`
4. `"architecture decision about microservices"`
5. `"monitoring and observability best practices"`
6. `"security vulnerability patch management"`
7. `"customer data privacy compliance"`
8. `"CI/CD pipeline optimization"`
9. `"cloud migration strategy"`
10. `"error budget and SLO definition"`

#### Criterio de aceptación UAT
- **Given** un volumen de al menos 10,000 memorias en el repositorio y el ambiente UAT operativo
- **When** se ejecutan 50+ búsquedas semánticas y se mide la latencia
- **Then**:
  - El p95 de búsqueda semántica simple es < 500ms (CE-04).
  - El p95 de búsqueda con filtros y expansión de grafo es < 800ms.
  - El p95 de lectura por ID es < 200ms.
  - El p95 de creación de memoria (incluyendo embedding) es < 2s.
  - Si las métricas no alcanzan las metas, se documenta la desviación y se acuerda un plan de optimización o recalibración de metas con el Product Owner.

---

## 5. Criterios de Éxito UAT

### 5.1 Vinculación con los 13 Criterios de Éxito de la Visión

Cada CE de la Visión del Producto se verifica en UAT de la siguiente manera:

| CE Visión | Descripción | Meta | Verificación en UAT | Escenario UAT |
|---|---|---|---|---|
| **CE-01** | NDCG@10 en BEIR SciFact | ≥ 0.80 | ❏ No se verifica en UAT manual. Se ejecuta como suite automatizada separada. | — |
| **CE-02** | Recall@10 en BEIR SciFact | ≥ 0.90 | ❏ No se verifica en UAT manual. Suite automatizada separada. | — |
| **CE-03** | Recall en LoCoMo | ≥ 0.80 | ❏ No se verifica en UAT manual. Suite automatizada separada. | — |
| **CE-04** | Latencia p95 de búsqueda | < 500ms | ✅ Verificado en **UAT-S10**. Medición con 50+ búsquedas sobre 10K+ memorias. | UAT-S10 |
| **CE-05** | Precisión top-1 suite interna | ≥ 0.92 | ✅ Parcialmente verificado. **UAT-S01** y **UAT-S02** validan que búsquedas conocidas retornan el resultado esperado en top-3. | UAT-S01, UAT-S02 |
| **CE-06** | Cobertura de los 8 kinds | 100% (8/8) | ✅ Verificado en **UAT-S01** (usa `decision`). Los 8 kinds fueron validados en QA (CP-F-003). UAT confirma operatividad con al menos 4 kinds distintos (fact, event, decision, procedure). | UAT-S01, UAT-S04 |
| **CE-07** | Aislamiento multi-tenant | 100% (0 fugas) | ✅ Verificado en **UAT-S07**. Tests cross-tenant con 2 tenants y 3 roles. | UAT-S07 |
| **CE-08** | Visibilidad por estado | 100% (0 falsos positivos) | ✅ Verificado en **UAT-S02** (draft/archived no visibles) y **UAT-S05** (pending no visible sin filtro). | UAT-S02, UAT-S05 |
| **CE-09** | Trazabilidad de operaciones | 100% mutaciones con auditoría | ✅ Verificado en **UAT-S06**. 10+ mutaciones auditadas. | UAT-S06 |
| **CE-010** | English-Only compliance | 100% identificadores en inglés | ✅ Verificado transversalmente en todos los escenarios. Revisión de endpoints, enums, códigos de error. | Todos |
| **CE-011** | Operaciones sobre los 9 tipos de relación | 9/9 operativos | ✅ Parcialmente verificado. **UAT-S04** prueba 5 de 9 tipos. Los 4 restantes se validaron en QA (CP-F-026 a CP-F-032). ⚠️ Sujeto a corrección de BUG-004. | UAT-S04 |
| **CE-012** | Batch ingest | ≥ 99% éxito | ❏ Excluido de UAT. EP-007 (Should) diferido. | — |
| **CE-013** | Migración v1→v2 | 100% muestra de validación | ❏ Excluido de UAT. No hay datos v1 para migrar. | — |

### 5.2 Porcentaje mínimo de escenarios que deben pasar

| Indicador | Meta | Descripción |
|---|---|---|
| **Escenarios UAT aprobados** | ≥ 90% (9 de 10) | Al menos 9 de los 10 escenarios UAT deben ser aprobados. |
| **Features críticas aprobadas** | 100% (6 de 6) | Las 6 features críticas (FC-01 a FC-06) deben ser aprobadas sin excepción. |
| **Criterios de éxito verificables en UAT** | ≥ 80% (8 de 10 CE aplicables) | De los 10 CE verificables en UAT (excluyendo CE-01, CE-02, CE-03 que requieren benchmarks externos), al menos 8 deben cumplir su meta. |
| **Defectos críticos abiertos al cierre de UAT** | 0 | Ningún defecto de severidad crítica puede permanecer abierto al cierre de UAT. |

### 5.3 Criterios de aceptación final

El producto se considera **aceptado** cuando se cumplen TODAS las condiciones siguientes:

1. ✅ **9 de 10 escenarios UAT** han sido ejecutados y aprobados (firmados por el usuario sponsor).
2. ✅ **6 de 6 features críticas** (FC-01 a FC-06) operativas y verificadas.
3. ✅ **0 defectos críticos** abiertos. BUG-004 corregido y verificado.
4. ✅ **CE-07 (aislamiento multi-tenant)** verificado al 100% sin fugas.
5. ✅ **CE-09 (trazabilidad)** verificado al 100% de mutaciones con registro de auditoría.
6. ✅ **CE-04 (latencia)** verificado con p95 < 500ms, o desviación aceptada formalmente por el Product Owner.
7. ✅ **CE-010 (English-Only)** verificado en todos los endpoints, enums y códigos de error expuestos.
8. ✅ **Acta de UAT firmada** por el usuario sponsor y el business-analyst.

### 5.4 Condiciones de aceptación parcial

Si algún escenario no crítico falla (ej. UAT-S09 rate limiting, que es una funcionalidad Should), el producto puede ser aceptado con la condición de que la deficiencia se registre en el backlog para una versión posterior (v2.0.1). Esto requiere:

- Documentación explícita de la deficiencia en el acta de UAT.
- Aceptación formal del riesgo por el usuario sponsor.
- Fecha compromiso para la resolución (ej. "v2.0.1, Q3 2026").

---

## 6. Calendario UAT

### 6.1 Sesiones planificadas

El UAT se estructura en **4 sesiones** distribuidas en un máximo de **5 días hábiles**. Cada sesión tiene una duración estimada de 2 a 4 horas.

| Sesión | Día | Duración | Escenarios | Participantes requeridos |
|---|---|---|---|---|
| **Sesión UAT-01** | Día 1 | 3h | UAT-S01 (CRUD + decisión), UAT-S02 (Búsqueda semántica), UAT-S08 (Extracción entidades) | Sponsor, Memory Operator, Business Analyst |
| **Sesión UAT-02** | Día 2 | 3h | UAT-S04 (Relaciones y grafo) ⚠️ Requiere BUG-004 corregido, UAT-S05 (Ciclo de vida y revisión) | Sponsor, Memory Reviewer, Business Analyst |
| **Sesión UAT-03** | Día 3 | 3h | UAT-S03 (Perfiles de dominio), UAT-S06 (Trazabilidad), UAT-S07 (Aislamiento multi-tenant) | Sponsor, Memory Admin, Memory Auditor, Business Analyst |
| **Sesión UAT-04** | Día 4 | 4h | UAT-S09 (Rate limiting), UAT-S10 (Latencia y rendimiento) + Repaso de escenarios fallidos + Firma de acta | Sponsor, Memory Admin, Business Analyst, Tech Lead (opcional) |
| **Margen** | Día 5 | 3h | Re-ejecución de escenarios fallidos (si aplica) | Sponsor, Business Analyst |

### 6.2 Duración total estimada

| Concepto | Tiempo |
|---|---|
| Ejecución de escenarios | 10-13 horas (4 sesiones) |
| Preparación de datos y ambiente | 4 horas (pre-UAT, a cargo de DevOps/QA) |
| Documentación de resultados | 3 horas (durante y post sesiones, a cargo de BA) |
| Re-ejecución (si aplica) | 3 horas (día 5, solo si hay escenarios fallidos) |
| **Total** | **17-23 horas** |

### 6.3 Participantes requeridos

| Rol | Persona | Responsabilidad | Sesiones |
|---|---|---|---|
| **Usuario Sponsor** | **Pendiente de designación** (Owner: Product Owner. Deadline: 48h antes de Sesión UAT-01) | Ejecuta escenarios, valida cumplimiento de necesidades de negocio, firma aceptación. | Todas |
| **Business Analyst** | business-analyst (Abax) | Facilita sesiones, documenta resultados, registra desviaciones y acuerdos. | Todas |
| **Memory Operator** | Usuario de prueba `operador@alpha.local` (QA) o persona designada por el sponsor | Ejecuta flujos de creación, búsqueda, relaciones. | UAT-01, UAT-02 |
| **Memory Reviewer** | Usuario de prueba `revisor@alpha.local` (QA) o persona designada por el sponsor | Ejecuta flujos de revisión y aprobación. | UAT-02 |
| **Memory Admin** | Usuario de prueba `admin@system.local` (QA) o persona designada por el sponsor | Configura perfiles, verifica rate limiting, accede cross-tenant. | UAT-03, UAT-04 |
| **Memory Auditor** | Usuario de prueba `auditor@system.local` (QA) o persona designada por el sponsor | Verifica trazabilidad y registros de auditoría. | UAT-03 |
| **Tech Lead** (opcional) | **Pendiente de designación** (Owner: Tech Lead. Deadline: 48h antes de Sesión UAT-04) | Soporte técnico si se requieren ajustes o diagnóstico durante UAT. | UAT-04 |

---

## 7. Procedimiento de Aprobación

### 7.1 Quién firma

| Firmante | Rol | Tipo de firma |
|---|---|---|
| **Usuario Sponsor** | Representante del negocio / Product Owner | **Aprobación principal**. Valida que el producto cumple con las necesidades de negocio. |
| **Business Analyst** | Responsable del plan de UAT | **Conformidad**. Certifica que el plan se ejecutó según lo documentado y que los resultados son trazables. |
| **Tech Lead** (si aplica) | Responsable técnico | **Visto bueno técnico**. Confirma que los defectos remanentes son conocidos y aceptables. |

### 7.2 Criterios de firma

El usuario sponsor firma la aceptación cuando:

1. Los **10 escenarios UAT** han sido ejecutados.
2. Al menos **9 de 10 escenarios** tienen resultado **APROBADO**.
3. Las **6 features críticas** (FC-01 a FC-06) están operativas.
4. **0 defectos críticos** permanecen abiertos.
5. Las desviaciones detectadas (escenarios con resultado FALLIDO o PARCIAL) están documentadas con:
   - Descripción de la desviación.
   - Impacto en el negocio.
   - Plan de acción acordado (corregir antes de producción, diferir a v2.0.1, o aceptar como limitación conocida).
   - Fecha compromiso (si aplica).

### 7.3 Qué pasa si se rechaza

Si el usuario sponsor **no firma** la aceptación:

| Situación | Procedimiento |
|---|---|
| **1 escenario fallido (no crítico)** | Se documenta la deficiencia. Si el sponsor acepta el riesgo, se procede a aceptación parcial con condición de resolver en v2.0.1. |
| **1 feature crítica fallida** | El producto **no puede ser aceptado**. Se notifica al equipo de desarrollo. Se corrige el defecto. Se re-agenda UAT para el escenario afectado (máximo 3 días hábiles para corrección + 1 día para re-UAT). |
| **2+ escenarios fallidos** | UAT se suspende. Se emite un informe de no-conformidad. El equipo de desarrollo prioriza las correcciones. Se re-agenda UAT completo (no solo los escenarios fallidos, para prevenir regresiones). |
| **Defecto crítico sin corregir** | El producto **no puede ser aceptado** bajo ninguna circunstancia. Se escala al Project Manager y al sponsor para decidir: (a) extender plazo para corrección, o (b) reducir alcance (mover funcionalidad afectada a v2.1.0). |

### 7.4 Acta de UAT

Al finalizar la Sesión UAT-04, se genera un **Acta de UAT** que contiene:

1. Fecha de ejecución y participantes.
2. Tabla de resultados por escenario (APROBADO / FALLIDO / PARCIAL / BLOQUEADO / NO EJECUTADO).
3. Listado de defectos encontrados durante UAT (si los hay).
4. Desviaciones respecto a los criterios de éxito.
5. Acuerdos y condiciones de aceptación (si es aceptación parcial).
6. Firmas.

El acta se documenta en `docs/entregables/v2/fase-6-uat/acta-uat.md`.

---

## 8. Riesgos UAT

| ID | Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|---|
| **R-UAT-01** | BUG-004 no se corrige a tiempo para UAT-S04 | Alta | Alto — escenario UAT-S04 bloqueado | Plan de contingencia: re-agendar UAT-S04 para después de la corrección. Continuar con los demás escenarios. Si UAT-S04 es el único fallido, aceptar con condición de corrección previo a despliegue. |
| **R-UAT-02** | El ambiente UAT no está disponible o tiene datos insuficientes | Media | Alto — todos los escenarios bloqueados | Verificar disponibilidad del ambiente 24h antes del inicio de UAT. Pre-cargar datos de prueba (seed data) con anticipación. |
| **R-UAT-03** | OpenAI API tiene latencia elevada o está inaccesible durante UAT-S10 | Baja | Medio — afecta mediciones de latencia | Documentar la latencia de OpenAI como factor externo. Si la latencia excede el límite por causa de OpenAI, se excluye ese componente del cálculo y se reporta por separado. |
| **R-UAT-04** | El usuario sponsor no está disponible en las fechas planificadas | Media | Alto — UAT no puede ejecutarse sin el sponsor | Confirmar disponibilidad con 1 semana de anticipación. Tener fechas alternativas (semana siguiente). |
| **R-UAT-05** | Se descubren defectos críticos nuevos durante UAT | Media | Alto — puede bloquear la aceptación | Documentar inmediatamente. Si el defecto afecta una feature crítica, detener UAT y escalar. Si afecta feature no crítica, continuar y registrar. |
| **R-UAT-06** | BUG-006 (CORS) impide el uso del frontend | Media | Medio — escenarios que dependen del frontend se degradan | Plan de contingencia: ejecutar los escenarios vía API (curl/Postman) y validar el frontend por separado cuando CORS esté corregido. |
| **R-UAT-07** | El volumen de datos (< 10K memorias) no es suficiente para mediciones de latencia representativas (UAT-S10) | Alta | Medio — métricas de latencia poco representativas | Ejecutar UAT-S10 con el volumen disponible. Documentar el volumen usado. Si es < 2K, las métricas se consideran preliminares. Re-ejecutar con volumen representativo pre-producción. |

---

## 9. Matriz de Trazabilidad UAT → CE

| Escenario UAT | CE-04 | CE-05 | CE-06 | CE-07 | CE-08 | CE-09 | CE-010 | CE-011 | Features críticas |
|---|---|---|---|---|---|---|---|---|---|
| **UAT-S01** (CRUD decisión) | — | ✅ | ✅ | — | — | — | ✅ | — | FC-04, FC-05 |
| **UAT-S02** (Búsqueda incidente) | ✅ | ✅ | — | — | ✅ | — | ✅ | — | FC-01, FC-05 |
| **UAT-S03** (Perfil dominio) | — | — | ✅ | — | — | — | ✅ | — | FC-05 |
| **UAT-S04** (Relaciones y grafo) | — | ✅ | — | — | — | — | ✅ | ✅ | FC-05 |
| **UAT-S05** (Aprobación) | — | — | — | — | ✅ | ✅ | ✅ | — | FC-03, FC-06 |
| **UAT-S06** (Trazabilidad) | — | — | — | — | — | ✅ | ✅ | — | FC-06 |
| **UAT-S07** (Aislamiento tenant) | — | — | — | ✅ | — | — | ✅ | — | FC-02, FC-05 |
| **UAT-S08** (Extraer entidades) | — | ✅ | — | — | — | — | ✅ | — | FC-05 |
| **UAT-S09** (Rate limiting) | ✅ | — | — | — | — | — | — | — | — |
| **UAT-S10** (Latencia) | ✅ | — | — | — | — | — | — | — | FC-01 |

> **Nota**: CE-01, CE-02 y CE-03 (benchmarks) no se incluyen por requerir datasets externos. CE-012 (batch ingest) y CE-013 (migración) están fuera del alcance UAT (funcionalidades Should/Could diferidas).

---

## 10. Glosario

- **UAT**: User Acceptance Testing — fase de validación donde el usuario sponsor verifica que el producto satisface sus necesidades de negocio antes de la aceptación formal.
- **CE**: Criterio de Éxito — métrica medible definida en la Visión del Producto que determina si el producto cumple sus objetivos.
- **FC**: Feature Crítica — capacidad del sistema cuyo fallo impide la aceptación del producto.
- **p95**: Percentil 95 — métrica de latencia que indica que el 95% de las solicitudes se completan en un tiempo igual o menor al valor indicado.
- **BR**: Business Rule — regla de negocio documentada que gobierna el comportamiento del sistema.
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0 utilizado por Keycloak para gestionar identidad y claims de usuarios.
- **Qdrant**: Base de datos vectorial open-source que almacena embeddings y ejecuta búsqueda semántica por similitud de coseno.

---

*Documento generado por business-analyst el 2026-05-03. Cubre 10 escenarios UAT trazables a 13 criterios de éxito, 69 historias de usuario y 6 features críticas. Incluye alcance detallado, exclusiones justificadas, precondiciones, datos de prueba, criterios de aceptación en formato Given/When/Then, calendario con 4 sesiones, procedimiento de aprobación formal y matriz de riesgos.*
