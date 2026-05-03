---
fase: 0 — Descubrimiento (v2.0.0)
entregable: Visión del Producto
responsable: business-analyst
fecha: 2026-05-03
release: v2.0.0
estado: Completado
fuentes:
  - docs/iteration-log.md (estrategia de iteración v2)
  - PROPuesta-ABAX-MEMORY-GENERICO.md (propuesta técnica)
  - docs/entregables/v1/fase-0-descubrimiento/vision-producto.md (visión v1 para referencia)
---

# Visión del Producto — Abax-Memory v2.0.0
## Motor de Memoria Genérica Multi-Dominio con IA

---

## 1. Propósito y Justificación

### 1.1 ¿Qué es Abax-Memory v2.0.0?

Abax-Memory v2.0.0 evoluciona desde un motor de memoria especializado en operaciones IT (PMOA v1.0.0) hacia un **motor de memoria genérica, multi-dominio y multi-tenant**, potenciado por inteligencia artificial. El producto permite a cualquier profesional —independientemente de su industria o dominio— capturar, estructurar, relacionar, recuperar, auditar y gobernar conocimiento mediante memorias interoperables con agentes, aplicaciones y flujos de trabajo.

### 1.2 ¿Por qué evolucionar de v1 a v2?

v1.0.0 (PMOA — Product Memory for Operations & Analysis) cumplió su propósito: demostró que el ranking semántico, la extracción de entidades, la auditoría y la gobernanza de memoria son viables y valiosos para equipos de IT Operations. Sin embargo, el producto quedó acotado a un solo dominio y una sola audiencia:

| v1.0.0 (IT Ops) | v2.0.0 (Multi-dominio) |
|---|---|
| Dominio fijo: incidentes, runbooks, casos | Dominios dinámicos: legal, CRM, finanzas, agentes, IT ops, etc. |
| Tipos rígidos (`incidente`, `runbook`, `procedimiento`, `caso`) | Tipos genéricos (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`) |
| Sin scoping multi-tenant nativo | `scope` con `tenantId`, `userId`, `sessionId`, `namespace` |
| Sin perfiles de dominio configurables | Perfiles de dominio que heredan del core genérico |
| Internals en español (`EN_REVISION`, `APROBADA`, `criticality`) | English-Only internals (`draft`, `active`, `importance`) |
| Solo API v1 (`/api/v1/memorias`) | Solo API v2 (`/api/v2/memories`) — **v1 se descarta por completo** |
| Audiencia: operadores IT, revisores, administradores | Audiencia: cualquier profesional con necesidad de memoria aumentada |
| Sin SDK ni integraciones con ecosistemas de IA | Diseñado para ser consumido por agentes, LangChain, LlamaIndex y SDKs |

### 1.3 Justificación de negocio

1. **Ampliación de mercado**: pasar de un nicho (IT ops) a cualquier dominio que requiera memoria aumentada (legal, CRM, finanzas, agentes conversacionales, salud, educación, etc.).
2. **Competitividad directa**: posicionarse contra Mem0, Zep y Letta en el mercado emergente de motores de memoria como producto, no como feature.
3. **Diferenciación por gobernanza**: ningún competidor ofrece el nivel de auditoría, estados de ciclo de vida, revisión humana y relaciones estructuradas que Abax-Memory ya tiene implementados.
4. **Benchmarks comparables**: ser medible contra LoCoMo, LongMemEval y BEIR sin adaptaciones forzadas, gracias a la estandarización English-Only.
5. **Aprovechamiento de inversión existente**: la infraestructura de v1 (Qdrant 3072-dim, OpenAI embeddings, PostgreSQL, Keycloak RBAC, Caddy/HTTPS, Docker) es sólida y reutilizable.

> **Decisión del sponsor (2026-05-03)**: v1.0.0 está cerrado. Nadie usa v1 en producción. No hay que mantener backward compatibility. API v1 se descarta completamente. Solo `/api/v2`.

---

## 2. Usuarios Objetivo

### 2.1 Principio: Usuario Universal con IA

v2.0.0 se diseña para un **usuario universal**: cualquier profesional que necesite memoria aumentada, independientemente del dominio. El core genérico se complementa con **perfiles de dominio** que adaptan la semántica sin modificar la API base.

### 2.2 Roles del Sistema

Los roles de v1 se heredan y se generalizan para un contexto multi-dominio:

| Rol | Descripción | Necesidades principales |
|---|---|---|
| **Memory Operator** (`memory-operator`) | Usuario que crea, clasifica y relaciona memorias en cualquier dominio | Registrar conocimiento con estructura mínima y trazabilidad. Cada operador trabaja dentro de su `scope` (tenant, usuario, sesión). |
| **Memory Reviewer** (`memory-reviewer`) | Usuario responsable de aprobar o rechazar memorias que requieren revisión humana | Ver origen, cambios, nivel de importancia/sensibilidad y decidir aprobación o rechazo mediante el ciclo de vida (`lifecycle.status`). |
| **Memory Consumer** (`api-consumer`) | Usuario, aplicación o agente que consulta memoria para resolver problemas o tomar decisiones | Encontrar memoria relevante, confiable y actualizada mediante búsqueda semántica y filtros estructurados. |
| **Memory Administrator** (`memory-admin`) | Responsable de depuración, gobierno y calidad del repositorio multi-tenant | Archivar, fusionar duplicadas, soft-delete, gestionar tenants y mantener calidad global del repositorio. |
| **Memory Auditor** (`memory-auditor`) | Responsable de revisar cumplimiento, trazabilidad y calidad por tenant o dominio | Ver historial completo de cambios, responsables, estados de validación y cobertura de conocimiento. |

> **Nota**: En el MVP un mismo usuario puede cumplir más de un rol, según permisos asignados en Keycloak y el alcance (`scope`) en el que opere.

### 2.3 Perfiles de Dominio (usuarios indirectos)

Los perfiles de dominio no son roles del sistema, sino **configuraciones que adaptan el motor genérico** a las necesidades de distintos verticales:

| Perfil | Vertical | Usuarios representativos | Ejemplos de uso |
|---|---|---|---|
| **Ops Profile** | IT Operations | SRE, DevOps, soporte L2/L3 | Incidentes, runbooks, procedimientos, postmortems |
| **Agent Profile** | Memoria conversacional | Agentes IA, chatbots, asistentes | Hechos sobre el usuario, preferencias, decisiones previas, contexto de sesión |
| **Business Profile** | CRM, Legal, Finanzas, Producto | Abogados, vendedores, analistas, PMs | Clientes, contratos, oportunidades, acuerdos, minutas de reunión |

Cada perfil define:
- **Kinds recomendados**: qué tipos de memoria (`kind`) son más relevantes para ese dominio.
- **Tags y topics sugeridos**: vocabulario controlado para clasificación.
- **Campos extra en `metadata`**: `affectedService`, `remediationSteps`, `clientName`, `contractId`, etc.
- **Reglas de sensibilidad**: qué nivel de `lifecycle.sensitivity` aplicar por defecto.

---

## 3. Alcance de Alto Nivel

### 3.1 DENTRO del alcance (v2.0.0 MVP completo)

| # | Ítem | Descripción |
|---|---|---|
| 1 | **Motor de memoria genérico** | Core con tipos universales (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`), estados de ciclo de vida (`draft`, `pending`, `active`, `archived`, `rejected`, `deleted`), relaciones tipadas (9 tipos), niveles de sensibilidad (`public`, `internal`, `confidential`, `secret`). |
| 2 | **Perfiles de dominio** | Mecanismo para definir configuraciones de dominio (Ops, Agent, Business) que heredan del core genérico sin modificar la API base. Los perfiles incluyen kinds recomendados, tags, metadatos extra y reglas de sensibilidad. |
| 3 | **Scoping multi-tenant nativo** | `scope.tenantId` + `scope.userId` + `scope.sessionId` + `scope.namespace` permiten aislamiento multi-tenant y búsquedas por usuario o sesión sin perfiles extra. |
| 4 | **API REST v2** | Nueva API bajo `/api/v2/` con endpoints para CRUD de memorias, búsqueda semántica con filtros, expansión de grafo, gestión de relaciones, entidades, revisión de estados, ingesta batch, extracción de entidades, estadísticas por tenant, health check y métricas. |
| 5 | **English-Only Internals** | Todos los identificadores internos del sistema (kinds, estados, tipos de relación, niveles de sensibilidad, paths de API, códigos de error, enums, columnas de BD, endpoints) estandarizados en inglés. El contenido de las memorias puede estar en cualquier idioma. |
| 6 | **Búsqueda semántica + graph expand** | Búsqueda por texto libre con filtros estructurados (scopes, kinds, statuses, topics, entities, importance, confidence, sensitivities, rango de fechas), expansión de subgrafo (`expandGraph`) y re-ranking (`rerank`). |
| 7 | **Gobernanza y ciclo de vida** | Regla de visibilidad por estado: búsqueda por defecto solo devuelve `active`. `draft` y `pending` requieren filtro explícito o permisos elevados. `archived`, `rejected` y `deleted` no son visibles sin acción explícita. |
| 8 | **Relaciones estructuradas** | 9 tipos de relación (`related_to`, `depends_on`, `caused_by`, `resolves`, `contradicts`, `supports`, `mentions`, `belongs_to`, `supersedes`) materializadas como edges dirigidas o bidireccionales, con endpoints CRUD y expansión de grafo. |
| 9 | **Extracción de entidades** | Endpoint `/api/v2/memories/extract` para extraer entidades de texto sin persistir. Endpoint `/api/v2/entities` para buscar entidades por nombre y obtener detalle con memorias vinculadas. |
| 10 | **Batch ingest** | Endpoint `/api/v2/memories/ingest` para ingesta batch de conversaciones o documentos que generan múltiples memorias en una sola llamada. |
| 11 | **Migración de datos v1→v2 (opcional)** | Script de migración que lee memorias v1, mapea tipos y estados al nuevo modelo v2, y reingesta. No es obligatorio — solo se ejecuta si el usuario tiene datos en v1 que desea preservar. |
| 12 | **Frontend** | Interfaz de usuario para interactuar con el motor de memoria v2 (creación, búsqueda, revisión, administración). |
| 13 | **Stack flexible** | Se permite cambiar componentes del stack si se justifica técnicamente (ej. cambiar motor de embeddings, base vectorial, framework backend). La decisión debe documentarse mediante ADR. |

### 3.2 FUERA del alcance (v2.0.0)

| # | Ítem | Justificación de exclusión |
|---|---|---|
| 1 | **API v1 (`/api/v1/memorias`)** | Descartada por completo por decisión del sponsor. No se mantiene, no coexiste, no se migra progresivamente. v2 es un nuevo producto. |
| 2 | **Backward compatibility con v1** | v1.0.0 está cerrado y nadie lo usa en producción. No hay consumidores que proteger. |
| 3 | **Tipos de memoria fijos por dominio** | El core es genérico. Los tipos (`kind`) son universales. Cualquier especialización se logra mediante perfiles de dominio y metadatos. |
| 4 | **Soporte multi-repositorio** | El MVP opera sobre un repositorio unificado (PostgreSQL + Qdrant). La federación multi-repositorio queda diferida. |
| 5 | **UI dedicada para usuarios finales (versión pre-v2)** | Se incluye un frontend, pero no se desarrollan interfaces especializadas por vertical (ej. dashboard legal, consola de CRM). |
| 6 | **Orquestación multi-agente** | La plataforma es un motor de memoria, no un orquestador de agentes. La integración con frameworks de agentes (LangChain, CrewAI, AutoGen) se logra mediante la API y SDKs, no dentro del core. |
| 7 | **SDKs multi-lenguaje** | El MVP incluye SDK Python básico. SDKs para Node.js, Java, Go quedan diferidos a releases posteriores. |
| 8 | **Benchmarks públicos completos** | Se ejecutarán benchmarks internos (SciFact, LoCoMo, suite de ~100 test cases) para validación. La publicación formal de resultados comparativos queda para una fase posterior. |
| 9 | **Automatización completa de revisión humana** | Las memorias con `importance` alta o `sensitivity` elevada requieren revisión humana. No se automatiza la decisión de aprobar/rechazar. |
| 10 | **Localización / i18n de la API** | La API y los identificadores internos son English-Only. Los mensajes de error visibles al usuario final pueden localizarse, pero no es parte del MVP. |
| 11 | **Modelado cerrado y definitivo de dominios** | Los perfiles de dominio son configurables y evolutivos. No se entrega un catálogo cerrado de dominios, sino un mecanismo para definirlos. |

---

## 4. Flujo Conceptual del Producto

```mermaid
flowchart TB
    subgraph Sources["Fuentes de Conocimiento"]
        CONV[Conversación / Chat]
        DOC[Documento / Archivo]
        API_EXT[API Externa / Workflow]
        MANUAL[Entrada Manual / Frontend]
    end

    subgraph Ingestion["Ingesta (API v2)"]
        CREATE[POST /memories]
        BATCH[POST /memories/ingest]
        EXTRACT[POST /memories/extract]
    end

    subgraph Core["Motor Genérico de Memoria"]
        CLASSIFY[Clasificación por kind + topics]
        EMBED[Embedding semántico]
        RELATE[Relaciones estructuradas]
        ENTITIES[Extracción de entidades]
        LIFECYCLE[Ciclo de vida: draft → pending → active]
        SCOPE[Aislamiento por scope: tenant + user + session]
    end

    subgraph Profiles["Perfiles de Dominio"]
        OPS[Ops Profile]
        AGENT[Agent Profile]
        BUSINESS[Business Profile]
    end

    subgraph Consumption["Consumo"]
        SEARCH[POST /memories/search + graph expand]
        READ[GET /memories/{id}]
        REVIEW[POST /memories/{id}/review]
        STATS[GET /scopes/{tenantId}/stats]
    end

    subgraph Governance["Gobernanza"]
        AUDIT[Auditoría completa]
        APPROVAL[Revisión humana]
        DEPURATION[Depuración: archive/merge/soft-delete]
    end

    Sources --> Ingestion
    Ingestion --> Core
    Core --> Profiles
    Profiles --> Consumption
    Consumption --> Governance
    Governance --> Core
```

---

## 5. Reglas de Negocio Fundamentales

Las siguientes reglas de negocio aplican al core genérico y son independientes del perfil de dominio:

| ID | Regla | Condición | Acción | Excepciones |
|---|---|---|---|---|
| **BR-001** | Visibilidad por defecto en búsqueda | El consumidor ejecuta `POST /memories/search` sin filtro explícito de `statuses` | Solo se devuelven memorias con `lifecycle.status = active` | `memory-reviewer` y `memory-admin` pueden ver `pending` y `draft` si lo solicitan explícitamente |
| **BR-002** | Soft-delete | Se ejecuta `DELETE /memories/{id}` | El registro se marca como `lifecycle.status = deleted`. No se elimina físicamente. | `memory-admin` puede purgar físicamente con un endpoint administrativo (futuro). |
| **BR-003** | Scoping obligatorio en escritura | Se crea una memoria mediante `POST /memories` | El `scope` es obligatorio. Debe contener al menos `tenantId`. `userId` y `sessionId` son opcionales pero recomendados. | Ninguna en MVP. |
| **BR-004** | Scoping en lectura | Se ejecuta una búsqueda o consulta | Los resultados se filtran automáticamente por el `scope.tenantId` del token de autenticación. Un tenant no puede ver memorias de otro tenant. | `memory-admin` puede consultar cross-tenant si tiene permisos explícitos. |
| **BR-005** | Transición de estados | Una memoria cambia de estado mediante `POST /memories/{id}/review` | Las transiciones permitidas son: `draft → pending`, `pending → active`, `pending → rejected`, `active → archived`, cualquier estado → `deleted`. | `active → draft` está prohibido. Si se necesita reabrir, se crea una nueva versión y se usa `supersedes`. |
| **BR-006** | `importance` mínima para revisión humana | Una memoria se crea con `lifecycle.importance >= 0.7` y `lifecycle.sensitivity IN (confidential, secret)` | La memoria debe ser creada en estado `draft` o `pending`, nunca directamente en `active`. Requiere revisión humana antes de pasar a `active`. | `memory-admin` puede saltar esta regla con justificación de auditoría registrada. |
| **BR-007** | Relaciones con target existente | Se crea una relación `POST /memories/{id}/relations` con `targetId` | El `targetId` debe corresponder a una memoria existente y no `deleted`. | Si `targetId` no existe, se rechaza con error `TARGET_NOT_FOUND`. |
| **BR-008** | Ingesta batch atómica | Se ejecuta `POST /memories/ingest` con un array de memorias | Todas las memorias del batch se crean en una transacción. Si una falla, ninguna se persiste (todo o nada). | El batch tiene un límite máximo de 100 memorias por llamada. |
| **BR-009** | Importancia y sensibilidad heredadas del perfil | Se crea una memoria sin especificar `lifecycle.importance` o `lifecycle.sensitivity` | El sistema asigna valores por defecto según el perfil de dominio activo. Si no hay perfil, `importance = 0.5` y `sensitivity = internal`. | El usuario puede sobrescribir estos valores explícitamente. |
| **BR-010** | English-Only en identificadores | Cualquier identificador interno del sistema (kind, status, relation type, endpoint, enum, columna) | Debe estar en inglés (`UPPER_SNAKE_CASE` o `lower_snake_case` según convención del lenguaje). | Contenido de memorias (`content`, `summary`, `metadata` libre), tags y topics definidos por el usuario, y mensajes de error visibles al usuario final (si se decide localizar). |

---

## 6. Supuestos y Restricciones

### 6.1 Supuestos

| # | Supuesto | Impacto si no se cumple |
|---|---|---|
| **S-01** | La infraestructura de v1 (PostgreSQL, Qdrant, Keycloak, OpenAI, Docker, Caddy) es reutilizable para v2 sin cambios estructurales mayores. | Se requeriría migrar componentes, lo que impacta el cronograma. |
| **S-02** | Los perfiles de dominio pueden definirse como configuraciones (JSON/YAML o registros en BD) sin requerir código custom por perfil. | Si un perfil requiere lógica de negocio específica, se necesitaría desarrollo adicional por perfil. |
| **S-03** | Los usuarios objetivo tienen acceso a un cliente HTTP o al frontend para interactuar con la API v2 (no se requiere interfaz Git/GitHub como en v1). | Si los usuarios requieren interacción vía Git, se necesitaría una capa de adaptación no contemplada. |
| **S-04** | El motor de embeddings (OpenAI `text-embedding-3-large`) y Qdrant continúan siendo adecuados para los benchmarks objetivo. | Si se requiere cambiar de motor de embeddings, el impacto se limita a reindexar y validar equivalencia de scores. |
| **S-05** | Los datos de v1 (si existen) se migran mediante el script de mapeo documentado en la propuesta. No se requiere migración online ni zero-downtime. | Si se requiere migración online, se necesita un plan adicional con ventana de corte y sincronización. |
| **S-06** | Un mismo usuario puede pertenecer a múltiples tenants, y el `scope.tenantId` se determina por el token de autenticación o por parámetro explícito. | Si el modelo de tenants requiere jerarquías (parent/child), se necesita extender el modelo de scopes. |

### 6.2 Restricciones

| # | Restricción | Tipo | Descripción |
|---|---|---|---|
| **R-01** | **Stack flexible con justificación técnica** | Técnica | Se permite cambiar componentes del stack base (Quarkus, PostgreSQL, Qdrant, Keycloak, OpenAI) si el cambio se justifica mediante un ADR. No hay dependencia rígida de ningún componente heredado de v1. |
| **R-02** | **Sin dependencia de v1** | Arquitectónica | v2.0.0 se desarrolla como un producto independiente. No comparte base de código, esquemas de BD ni contratos de API con v1.0.0. La migración de datos v1→v2 es un script externo opcional. |
| **R-03** | **Sin restricción de dominio** | Funcional | El core no puede tener lógica específica de ningún dominio. Toda especialización debe estar en perfiles de dominio configurables. |
| **R-04** | **English-Only en internals** | Convención | Todos los identificadores del sistema (kinds, estados, endpoints, columnas, enums, códigos de error, paths de API) deben estar en inglés. Esta restricción es no negociable. |
| **R-05** | **API v1 no existe en v2** | Producto | No se expone ningún endpoint bajo `/api/v1/`. Solo `/api/v2/`. No hay período de coexistencia. |
| **R-06** | **API-first** | Arquitectónica | El producto se expone como API REST. El frontend es un consumidor más de la API, no un monolito acoplado. |
| **R-07** | **Autenticación centralizada** | Seguridad | Keycloak (o el proveedor OIDC que lo reemplace) es el único mecanismo de autenticación y autorización. No se aceptan API keys estáticas como mecanismo primario. |
| **R-08** | **Trazabilidad completa** | Gobernanza | Toda operación sobre memorias (creación, modificación, cambio de estado, soft-delete) debe generar un registro de auditoría con timestamp, usuario, acción y diff (antes/después). |

---

## 7. Criterios de Éxito Medibles

v2.0.0 se considerará exitoso si cumple los siguientes criterios, verificables antes del cierre de la fase UAT (Fase 6):

| ID | Criterio de Éxito | Métrica | Meta | Método de Verificación |
|---|---|---|---|---|
| **CE-01** | Rendimiento semántico en benchmark estándar | NDCG@10 en BEIR SciFact (5,183 docs) | ≥ 0.80 | Ejecución del benchmark con datos públicos y medición automatizada |
| **CE-02** | Recall en benchmark estándar | Recall@10 en BEIR SciFact (5,183 docs) | ≥ 0.90 | Ejecución del benchmark con datos públicos y medición automatizada |
| **CE-03** | Recall en memoria conversacional | Recall en dataset LoCoMo | ≥ 0.80 | Ejecución del benchmark con adaptación para el perfil Agent |
| **CE-04** | Latencia de búsqueda | p95 del endpoint `POST /memories/search` | < 500ms | Pruebas de carga con volumen representativo (10K+ memorias, 3 tenants) |
| **CE-05** | Precisión top-1 en suite interna | Suite de ~100 test cases multi-dominio | ≥ 0.92 | Suite de tests automatizados con ground truth conocido |
| **CE-06** | Cobertura de tipos de memoria | Porcentaje de los 8 kinds soportados con al menos 10 memorias de prueba | 100% (8/8) | Conteo en dataset de pruebas multi-dominio |
| **CE-07** | Aislamiento multi-tenant | Porcentaje de queries cross-tenant que retornan 0 resultados | 100% | Tests de seguridad automatizados con tokens de distintos tenants |
| **CE-08** | Visibilidad por estado | Búsqueda sin filtro de status solo retorna memorias `active` | 100% (0 falsos positivos) | Tests automatizados con memorias en todos los estados |
| **CE-09** | Trazabilidad de operaciones | Porcentaje de mutaciones con registro de auditoría completo (timestamp, user, action, diff) | 100% | Auditoría de logs tras ejecutar suite completa de operaciones CRUD |
| **CE-010** | English-Only compliance | Porcentaje de identificadores internos (endpoints, enums, columnas, códigos de error) en inglés | 100% | Revisión automatizada de código (linter custom) + revisión manual de documentación OpenAPI |
| **CE-011** | Operaciones sobre relaciones | CRUD de relaciones funcional para los 9 tipos (`related_to` … `supersedes`) | 9/9 tipos operativos | Tests de integración que crean, consultan, expanden y eliminan cada tipo de relación |
| **CE-012** | Batch ingest | Ingesta de 100 memorias en una llamada con atomicidad | Tasa de éxito ≥ 99% sin inconsistencias | Tests de carga con batches de tamaño máximo y fallos simulados en memorias individuales |
| **CE-013** | Migración v1→v2 (si aplica) | Porcentaje de memorias v1 correctamente migradas al modelo v2 sin pérdida semántica | 100% de las memorias de una muestra de validación | Muestreo manual: comparar contenido, tipo mapeado y estado de al menos 20 memorias migradas |

> **Nota**: Las metas de latencia (CE-04) asumen infraestructura equivalente al entorno de v1. Si se cambia el motor de embeddings o la base vectorial, las metas deben recalibrarse con Product Owner.

---

## 8. Resumen Ejecutivo

Abax-Memory v2.0.0 representa la evolución natural del producto: de una herramienta especializada en memoria operativa para equipos de IT a un **motor de memoria genérico, multi-dominio y multi-tenant** que puede ser utilizado por cualquier profesional o aplicación con necesidades de memoria aumentada.

La v1 demostró que los fundamentos —ranking semántico, extracción de entidades, relaciones, auditoría y gobernanza— son sólidos. Ahora v2 los lleva al siguiente nivel:

- **Core genérico** con 8 tipos universales de memoria, 6 estados de ciclo de vida y 9 tipos de relaciones.
- **Perfiles de dominio** que adaptan el core a cualquier vertical sin modificar la API.
- **Scoping multi-tenant** que permite aislar inquilinos, usuarios y sesiones en un solo despliegue.
- **English-Only internals** que hacen el producto interoperable con ecosistemas internacionales de IA.
- **API v2** diseñada desde cero para ser consumida por agentes, aplicaciones y flujos de trabajo.
- **Sin lastre de v1**: API v1 descartada, sin backward compatibility, libertad total de stack.

El producto se posiciona para competir directamente con Mem0, Zep y Letta en el mercado de motores de memoria, diferenciándose por su gobernanza, auditoría y control de ciclo de vida que ningún competidor ofrece al mismo nivel.

---

## 9. Dependencias Relevantes

| # | Dependencia | Tipo | Impacto |
|---|---|---|---|
| **D-01** | Stack de infraestructura (PostgreSQL, Qdrant, Keycloak, OpenAI) | Técnica | v2 requiere que estos servicios estén disponibles. Si se decide cambiar alguno, el ADR correspondiente debe aprobarse antes de iniciar Fase 3 (Diseño Técnico). |
| **D-02** | Propuesta técnica aprobada (PROPUESTA-ABAX-MEMORY-GENERICO.md) | Documental | El modelo de datos, la API y los perfiles de dominio se derivan de esta propuesta. Cualquier desviación debe documentarse y aprobarse. |
| **D-03** | Definición de perfiles de dominio (Ops, Agent, Business) | Funcional | Los perfiles requieren refinamiento con expertos de cada dominio durante la Fase 1 (Inicio) y Fase 2 (Análisis Funcional). |
| **D-04** | Disponibilidad de datasets de benchmark (BEIR SciFact, LoCoMo) | Validación | Los criterios de éxito CE-01, CE-02 y CE-03 dependen de la disponibilidad de estos datasets públicos. |
| **D-05** | Keycloak o proveedor OIDC equivalente | Seguridad | La autenticación y autorización multi-tenant depende de un proveedor OIDC con soporte para roles y claims de tenant. |

---

## Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que mide la calidad de los resultados en las primeras 10 posiciones, penalizando resultados relevantes en posiciones bajas.
- **BEIR**: Benchmarking Information Retrieval — conjunto de datasets estándar para evaluar sistemas de búsqueda semántica. SciFact es uno de sus subconjuntos (5,183 documentos científicos).
- **Qdrant**: Base de datos vectorial open-source utilizada para almacenar embeddings y ejecutar búsqueda semántica por similitud de coseno.
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0 que permite verificar la identidad de usuarios y obtener claims (roles, tenants) desde un proveedor centralizado como Keycloak.
- **LoCoMo**: Long-Context Memory — benchmark para evaluación de memoria conversacional de largo plazo en agentes de IA.
- **ADR**: Architecture Decision Record — documento que registra una decisión arquitectónica, su contexto, alternativas evaluadas y consecuencias.
- **p95**: Percentil 95 — métrica de latencia que indica que el 95% de las solicitudes se completan en un tiempo igual o menor al valor indicado.
