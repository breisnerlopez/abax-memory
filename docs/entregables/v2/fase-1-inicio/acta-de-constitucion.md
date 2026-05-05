# Acta de Constitución del Proyecto — Abax-Memory v2.0.0

- **Fase**: 1 — Inicio (v2.0.0)
- **Responsable**: project-manager
- **Fecha**: 2026-05-03
- **Release**: v2.0.0
- **Estado**: Completado
- **Fuentes**:
  - `docs/entregables/v2/fase-0-descubrimiento/vision-producto.md`
  - `docs/entregables/v2/fase-0-descubrimiento/backlog-priorizado.md`
  - `docs/iteration-log.md`

---

## Índice

- [1. Información General del Proyecto](#1-informacion-general-del-proyecto)
- [2. Propósito y Justificación de Negocio](#2-proposito-y-justificacion-de-negocio)
- [3. Objetivos del Proyecto](#3-objetivos-del-proyecto)
- [4. Criterios de Éxito Medibles](#4-criterios-de-exito-medibles)
- [5. Alcance de Alto Nivel](#5-alcance-de-alto-nivel)
- [6. Fuera del Alcance](#6-fuera-del-alcance)
- [7. Supuestos](#7-supuestos)
- [8. Restricciones](#8-restricciones)
- [9. Interesados Clave](#9-interesados-clave)
- [10. Hitos del Proyecto](#10-hitos-del-proyecto)
- [11. Estructura de Gobernanza](#11-estructura-de-gobernanza)
- [12. Matriz de Riesgos de Alto Nivel](#12-matriz-de-riesgos-de-alto-nivel)
- [13. Dependencias Externas](#13-dependencias-externas)
- [14. Aprobaciones](#14-aprobaciones)
- [15. Glosario](#15-glosario)

---

## 1. Información General del Proyecto

| Campo | Valor |
|---|---|
| **Nombre del Proyecto** | Abax-Memory v2.0.0 — Motor de Memoria Genérica Multi-Dominio con IA |
| **Versión** | 2.0.0 |
| **Tipo de Proyecto** | Evolución de producto (v1.0.0 → v2.0.0) — Iteración mayor |
| **Sponsor / Product Owner** | Usuario (sponsor del proyecto) |
| **Project Manager** | project-manager (orquestador Abax) |
| **Metodología** | Cascada con fases formales (F0–F9) y releases incrementales (R1, R2, R3) |
| **Producto Predecesor** | Abax-Memory v1.0.0 (PMOA — IT Operations) — **CERRADO** (2026-05-02) |
| **Estrategia de Iteración** | A — Folder por release (`docs/entregables/v2/`) |
| **Fecha de Constitución** | 2026-05-03 |
| **Stack Base** | Quarkus + PostgreSQL + Qdrant + Keycloak + OpenAI (sujeto a ADR) |

---

## 2. Propósito y Justificación de Negocio

### 2.1 Propósito

Abax-Memory v2.0.0 evoluciona desde un motor de memoria especializado en operaciones IT (PMOA v1.0.0) hacia un **motor de memoria genérica, multi-dominio y multi-tenant**, potenciado por inteligencia artificial. El producto permite a cualquier profesional —independientemente de su industria o dominio— capturar, estructurar, relacionar, recuperar, auditar y gobernar conocimiento mediante memorias interoperables con agentes, aplicaciones y flujos de trabajo.

### 2.2 Justificación de Negocio

| # | Justificación | Descripción |
|---|---|---|
| **J-01** | Ampliación de mercado | Pasar de un nicho (IT ops) a cualquier dominio que requiera memoria aumentada (legal, CRM, finanzas, agentes conversacionales, salud, educación, etc.). |
| **J-02** | Competitividad directa | Posicionarse contra Mem0, Zep y Letta en el mercado emergente de motores de memoria como producto, no como feature. |
| **J-03** | Diferenciación por gobernanza | Ningún competidor ofrece el nivel de auditoría, estados de ciclo de vida, revisión humana y relaciones estructuradas que Abax-Memory ya tiene implementados. |
| **J-04** | Benchmarks comparables | Ser medible contra LoCoMo, LongMemEval y BEIR sin adaptaciones forzadas, gracias a la estandarización English-Only. |
| **J-05** | Aprovechamiento de inversión existente | La infraestructura de v1 (Qdrant 3072-dim, OpenAI embeddings, PostgreSQL, Keycloak RBAC, Caddy/HTTPS, Docker) es sólida y reutilizable. |

### 2.3 Decisión Estratégica del Sponsor

> **Decisión del sponsor (2026-05-03)**: v1.0.0 está cerrado. Nadie usa v1 en producción. No hay que mantener backward compatibility. API v1 se descarta completamente. Solo `/api/v2`. v2 se construye como producto independiente, sin compartir base de código, esquemas de BD ni contratos de API con v1.0.0.

---

## 3. Objetivos del Proyecto

| ID | Objetivo | Descripción | Medible por |
|---|---|---|---|
| **OBJ-01** | Motor de memoria genérico funcional | Core con 8 tipos universales de memoria, 6 estados de ciclo de vida, 9 tipos de relaciones y modelo de sensibilidad. | CE-06, CE-11 |
| **OBJ-02** | Perfiles de dominio extensibles | Mecanismo de perfiles que heredan del core genérico, con al menos 3 perfiles funcionales (Ops, Agent, Business). | CE-03, CE-05 |
| **OBJ-03** | Aislamiento multi-tenant completo | `scope.tenantId` + `userId` + `sessionId` + `namespace` con aislamiento estricto entre tenants. | CE-07 |
| **OBJ-04** | API REST v2 segura y documentada | CRUD completo de memorias, búsqueda semántica, entidades, revisión, estadísticas, health. Autenticación JWT vía OIDC. Documentación OpenAPI viva. | CE-10, CE-04 |
| **OBJ-05** | Búsqueda semántica de alto rendimiento | Búsqueda por texto libre con filtros multidimensionales, expansión de grafo, re-ranking y scoring transparente. | CE-01, CE-02, CE-04 |
| **OBJ-06** | Gobernanza y trazabilidad completas | Auditoría de toda mutación, ciclo de vida con revisión humana, visibilidad gobernada por rol y estado, RBAC con 5 roles. | CE-08, CE-09 |
| **OBJ-07** | Frontend operativo multi-dominio | Interfaz de usuario para creación, búsqueda, revisión, administración y visualización de métricas, con login OIDC integrado. | Cobertura de historias EP-009 |

---

## 4. Criterios de Éxito Medibles

v2.0.0 se considerará exitoso si cumple los siguientes 13 criterios, verificables antes del cierre de la fase UAT (Fase 6):

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
| **CE-10** | English-Only compliance | Porcentaje de identificadores internos (endpoints, enums, columnas, códigos de error) en inglés | 100% | Revisión automatizada de código (linter custom) + revisión manual de documentación OpenAPI |
| **CE-11** | Operaciones sobre relaciones | CRUD de relaciones funcional para los 9 tipos (`related_to` … `supersedes`) | 9/9 tipos operativos | Tests de integración que crean, consultan, expanden y eliminan cada tipo de relación |
| **CE-12** | Batch ingest | Ingesta de 100 memorias en una llamada con atomicidad | Tasa de éxito ≥ 99% sin inconsistencias | Tests de carga con batches de tamaño máximo y fallos simulados en memorias individuales |
| **CE-13** | Migración v1→v2 (si aplica) | Porcentaje de memorias v1 correctamente migradas al modelo v2 sin pérdida semántica | 100% de las memorias de una muestra de validación | Muestreo manual: comparar contenido, tipo mapeado y estado de al menos 20 memorias migradas |

> **Nota**: CE-12 y CE-13 corresponden a épicas Should/Could (EP-007, EP-008) diferidas a v2.1+. Para el MVP de v2.0.0, los criterios vinculantes son CE-01 a CE-11 (11 criterios Must). CE-12 y CE-13 se incluyen como referencia para releases posteriores.

---

## 5. Alcance de Alto Nivel

### 5.1 Resumen de Épicas

v2.0.0 comprende **10 épicas** definidas, de las cuales **7 son Must** (MVP) y **3 son Should/Could** (diferidas a v2.1+):

| Épica | Nombre | Prioridad | Historias | Release | Descripción |
|---|---|---|---|---|---|
| **EP-001** | Motor de Memoria Genérico | Must | 12 | R1 + R2 | Core con 8 kinds, 6 estados, 9 relaciones, metadatos, soft-delete, versionado, confidence. |
| **EP-002** | Perfiles de Dominio | Must | 8 | R1 + R2 | Mecanismo de perfiles configurables (Ops, Agent, Business) que heredan del core. |
| **EP-003** | Scoping Multi-Tenant | Must | 7 | R1 + R2 | Aislamiento por `tenantId`, `userId`, `sessionId`, `namespace`. |
| **EP-004** | API REST v2 | Must | 15 | R1 + R2 + R3 | CRUD `/api/v2/memories`, autenticación JWT, OpenAPI, validación, errores estándar. |
| **EP-005** | Búsqueda Semántica + Graph | Must | 10 | R1 + R2 + R3 | Embedding, búsqueda semántica, filtros, graph expand, re-ranking. |
| **EP-006** | Gobernanza y Trazabilidad | Must | 9 | R1 + R2 | Auditoría, ciclo de vida, visibilidad, RBAC 5 roles, linaje de decisiones. |
| **EP-009** | Frontend Multi-Dominio | Must | 8 | R1 + R2 | UI para creación, búsqueda, revisión, administración, métricas. |
| **EP-007** | Batch Ingestion | Should | — | Diferido v2.1+ | Fuera del alcance de v2.0.0. |
| **EP-008** | Migración v1→v2 | Could | — | Diferido v2.1+ | Fuera del alcance de v2.0.0. |
| **EP-010** | SDK Python | Should | — | Diferido v2.1+ | Fuera del alcance de v2.0.0. |

### 5.2 Distribución de Historias por Release

| Release | Historias | % del Total | Tipo | Descripción |
|---|---|---|---|---|
| **R1 — MVP Core** | 43 | 62.3% | Must | Motor funcional, API completa, seguridad, gobernanza básica, UI mínima |
| **R2 — MVP Completo** | 23 | 33.3% | Must | Perfiles avanzados, búsqueda avanzada, gobernanza completa, frontend completo |
| **R3 — Enhancements** | 3 | 4.3% | Should | Rate limiting, multi-hop traversal, re-indexación masiva |
| **MVP Total (R1+R2)** | **66** | 95.7% | Must | Producto completo con todos los criterios de éxito Must |

### 5.3 DENTRO del alcance (v2.0.0 MVP completo)

| # | Ítem | Épica | Release |
|---|---|---|---|
| 1 | Motor de memoria genérico con 8 kinds universales (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`) | EP-001 | R1 |
| 2 | Ciclo de vida con 6 estados (`draft`, `pending`, `active`, `archived`, `rejected`, `deleted`) | EP-001 | R1 |
| 3 | Relaciones estructuradas con 9 tipos (`related_to`, `depends_on`, `caused_by`, `resolves`, `contradicts`, `supports`, `mentions`, `belongs_to`, `supersedes`) | EP-001 | R1 |
| 4 | Niveles de sensibilidad (`public`, `internal`, `confidential`, `secret`) y confidence (0.0–1.0) | EP-001 | R1 |
| 5 | Mecanismo de perfiles de dominio configurables que heredan del core genérico | EP-002 | R1 |
| 6 | Tres perfiles de referencia: Ops, Agent, Business | EP-002 | R1 + R2 |
| 7 | Scoping multi-tenant con `tenantId`, `userId`, `sessionId`, `namespace` | EP-003 | R1 + R2 |
| 8 | API REST v2 bajo `/api/v2/` con CRUD de memorias, relaciones y entidades | EP-004 | R1 + R2 |
| 9 | Autenticación JWT vía OIDC (Keycloak) con RBAC de 5 roles | EP-004, EP-006 | R1 |
| 10 | Documentación OpenAPI viva y errores estandarizados HTTP | EP-004 | R1 |
| 11 | Búsqueda semántica con embedding automático (OpenAI) y filtros multidimensionales | EP-005 | R1 |
| 12 | Expansión de subgrafo (`expandGraph`) y re-ranking (`rerank`) | EP-005 | R2 |
| 13 | Gobernanza: visibilidad por estado, auditoría de mutaciones, revisión humana | EP-006 | R1 + R2 |
| 14 | Trazabilidad completa con linaje de decisiones | EP-006 | R2 |
| 15 | Frontend con login OIDC, formulario de creación, panel de búsqueda, revisión y administración | EP-009 | R1 + R2 |
| 16 | English-Only en todos los identificadores internos del sistema | Global | R1 |
| 17 | Stack flexible con justificación técnica mediante ADR | Global | R1 (precondición) |

---

## 6. Fuera del Alcance

Los siguientes ítems quedan **explícitamente excluidos** de v2.0.0:

| # | Ítem | Épica | Justificación de exclusión |
|---|---|---|---|
| **1** | **API v1 (`/api/v1/memorias`)** | N/A | Descartada por completo por decisión del sponsor. No se mantiene, no coexiste, no se migra progresivamente. v2 es un nuevo producto. |
| **2** | **Backward compatibility con v1** | N/A | v1.0.0 está cerrado y nadie lo usa en producción. No hay consumidores que proteger. |
| **3** | **Batch Ingestion** | EP-007 | Should — diferido a v2.1+. No bloquea la operación core del motor. |
| **4** | **Migración de datos v1→v2** | EP-008 | Could — diferido a v2.1+. Script opcional solo si el usuario tiene datos en v1 que desea preservar. |
| **5** | **SDK Python** | EP-010 | Should — diferido a v2.1+. El MVP se consume vía API REST directamente. |
| **6** | **Rate limiting** | EP-004 (HU-004.13.1) | Should — diferido a R3. El MVP opera con carga controlada. |
| **7** | **Multi-hop traversal** | EP-005 (HU-005.05.1) | Should — diferido a R3. El 90% de casos se cubren con depth=1. |
| **8** | **Re-indexación masiva** | EP-005 (HU-005.08.1) | Should — diferido a R3. Solo necesario ante cambio de motor de embeddings. |
| **9** | **Tipos de memoria fijos por dominio** | N/A | El core es genérico. Cualquier especialización se logra mediante perfiles y metadatos. |
| **10** | **Soporte multi-repositorio** | N/A | El MVP opera sobre un repositorio unificado (PostgreSQL + Qdrant). |
| **11** | **UI especializadas por vertical** | N/A | Se incluye frontend genérico, no dashboards por industria. |
| **12** | **Orquestación multi-agente** | N/A | La plataforma es un motor de memoria, no un orquestador de agentes. |
| **13** | **SDKs multi-lenguaje** | N/A | Solo se contempla SDK Python en v2.1+. |
| **14** | **Benchmarks públicos completos** | N/A | Se ejecutarán benchmarks internos para validación. Publicación formal diferida. |
| **15** | **Automatización completa de revisión humana** | N/A | Memorias de alta criticidad requieren revisión humana. No se automatiza. |
| **16** | **Localización / i18n de la API** | N/A | Identificadores English-Only. Mensajes de error al usuario final pueden localizarse, fuera del MVP. |
| **17** | **Catálogo cerrado de dominios** | N/A | Los perfiles de dominio son configurables y evolutivos. |

---

## 7. Supuestos

Los siguientes supuestos son considerados válidos para la planificación de v2.0.0. Si un supuesto se invalida, debe escalarse al Project Manager para reevaluar impacto en alcance, cronograma y costo.

| # | Supuesto | Impacto si no se cumple | Validación |
|---|---|---|---|
| **S-01** | La infraestructura de v1 (PostgreSQL, Qdrant, Keycloak, OpenAI, Docker, Caddy) es reutilizable para v2 sin cambios estructurales mayores. | Se requeriría migrar componentes, lo que impacta el cronograma. | Fase 3 (Diseño Técnico) |
| **S-02** | Los perfiles de dominio pueden definirse como configuraciones (JSON/YAML o registros en BD) sin requerir código custom por perfil. | Si un perfil requiere lógica de negocio específica, se necesitaría desarrollo adicional por perfil. | Prototipo con perfil Ops en R1 |
| **S-03** | Los usuarios objetivo tienen acceso a un cliente HTTP o al frontend para interactuar con la API v2. No se requiere interfaz Git/GitHub como en v1. | Si los usuarios requieren interacción vía Git, se necesitaría una capa de adaptación no contemplada. | Fase 2 (Análisis Funcional) |
| **S-04** | El motor de embeddings (OpenAI `text-embedding-3-large`) y Qdrant continúan siendo adecuados para los benchmarks objetivo. | Si se requiere cambiar de motor de embeddings, el impacto se limita a reindexar y validar equivalencia de scores. | Fase 3 (Diseño Técnico) |
| **S-05** | Los datos de v1 (si existen) se migran mediante el script de mapeo documentado. No se requiere migración online ni zero-downtime. | Si se requiere migración online, se necesita un plan adicional con ventana de corte y sincronización. | Solo si aplica (opcional) |
| **S-06** | Un mismo usuario puede pertenecer a múltiples tenants, y el `scope.tenantId` se determina por el token de autenticación o por parámetro explícito. Modelo de tenants plano, sin jerarquías parent/child. | Si el modelo de tenants requiere jerarquías, se necesita extender el modelo de scopes. | Fase 2 (Análisis Funcional) |

---

## 8. Restricciones

Las siguientes restricciones son **no negociables** para v2.0.0:

| # | Restricción | Tipo | Descripción |
|---|---|---|---|
| **R-01** | **Stack flexible con justificación técnica** | Técnica | Se permite cambiar componentes del stack base (Quarkus, PostgreSQL, Qdrant, Keycloak, OpenAI) si el cambio se justifica mediante un ADR. No hay dependencia rígida de ningún componente heredado de v1. |
| **R-02** | **Sin dependencia de v1** | Arquitectónica | v2.0.0 se desarrolla como un producto independiente. No comparte base de código, esquemas de BD ni contratos de API con v1.0.0. La migración de datos v1→v2 es un script externo opcional. |
| **R-03** | **Sin restricción de dominio en el core** | Funcional | El core no puede tener lógica específica de ningún dominio. Toda especialización debe estar en perfiles de dominio configurables. |
| **R-04** | **English-Only en internals** | Convención | Todos los identificadores del sistema (kinds, estados, endpoints, columnas, enums, códigos de error, paths de API) deben estar en inglés. Esta restricción es no negociable. El contenido de las memorias y mensajes al usuario final pueden estar en cualquier idioma. |
| **R-05** | **API v1 no existe en v2** | Producto | No se expone ningún endpoint bajo `/api/v1/`. Solo `/api/v2/`. No hay período de coexistencia. |
| **R-06** | **API-first** | Arquitectónica | El producto se expone como API REST. El frontend es un consumidor más de la API, no un monolito acoplado. |
| **R-07** | **Autenticación centralizada** | Seguridad | Keycloak (o el proveedor OIDC que lo reemplace vía ADR) es el único mecanismo de autenticación y autorización. No se aceptan API keys estáticas como mecanismo primario. |
| **R-08** | **Trazabilidad completa** | Gobernanza | Toda operación sobre memorias (creación, modificación, cambio de estado, soft-delete) debe generar un registro de auditoría con timestamp, usuario, acción y diff (antes/después). |
| **R-09** | **Separación de releases** | Documental | Todo entregable de v2 se escribe exclusivamente en `docs/entregables/v2/`. Los archivos bajo `docs/entregables/v1/` son solo-lectura. |
| **R-10** | **Ciclo de QA obligatorio** | Proceso | Ningún entregable se libera sin el ciclo QA completado y aprobado. No se admiten atajos por presión de tiempo. |

---

## 9. Interesados Clave

### 9.1 Sponsor y Dirección

| Interesado | Rol | Responsabilidad | Expectativas |
|---|---|---|---|
| **Usuario Sponsor** | Product Owner / Sponsor | Aprobar alcance, presupuesto, criterios de éxito y gates de fase. Decidir estrategia de iteración. | Producto funcional, medible, gobernable. Sin lastre de v1. |
| **Project Manager** | Orquestador del proyecto | Gestionar alcance, tiempo, costo, calidad, riesgos y comunicaciones. Facilitar gates de fase. | Proyecto entregado dentro de lo planificado, con trazabilidad de decisiones. |

### 9.2 Roles del Sistema (Usuarios Finales)

| Rol | Descripción | Necesidades Principales |
|---|---|---|
| **Memory Operator** (`memory-operator`) | Usuario que crea, clasifica y relaciona memorias en cualquier dominio. Opera dentro de su `scope`. | Registrar conocimiento con estructura mínima y trazabilidad. |
| **Memory Reviewer** (`memory-reviewer`) | Usuario responsable de aprobar o rechazar memorias con revisión humana. | Ver origen, cambios, nivel de importancia/sensibilidad y decidir aprobación o rechazo. |
| **Memory Consumer** (`api-consumer`) | Usuario, aplicación o agente que consulta memoria para resolver problemas o tomar decisiones. | Encontrar memoria relevante, confiable y actualizada mediante búsqueda semántica y filtros. |
| **Memory Administrator** (`memory-admin`) | Responsable de depuración, gobierno y calidad del repositorio multi-tenant. | Archivar, fusionar duplicadas, soft-delete, gestionar tenants y mantener calidad global. |
| **Memory Auditor** (`memory-auditor`) | Responsable de revisar cumplimiento, trazabilidad y calidad por tenant o dominio. | Ver historial completo de cambios, responsables, estados y cobertura de conocimiento. |

### 9.3 Perfiles de Dominio (Usuarios Indirectos)

| Perfil | Vertical | Usuarios Representativos |
|---|---|---|
| **Ops Profile** | IT Operations | SRE, DevOps, soporte L2/L3 |
| **Agent Profile** | Memoria conversacional | Agentes IA, chatbots, asistentes |
| **Business Profile** | CRM, Legal, Finanzas, Producto | Abogados, vendedores, analistas, PMs |

### 9.4 Equipo de Proyecto (Agentes Abax)

| Rol | Responsabilidad Principal | Fases Activas |
|---|---|---|
| **business-analyst** | Visión de producto, épicas, historias de usuario, backlog priorizado, análisis funcional | F0, F2, F6 |
| **tech-lead** | Diseño técnico, ADRs, arquitectura, revisión de código, anti-mock review | F3, F4, F5 |
| **developer-backend** | Implementación del core, API, búsqueda, gobernanza | F4 |
| **developer-frontend** | Implementación del frontend multi-dominio | F4 |
| **qa-lead** | Plan de pruebas, casos de prueba, ejecución QA, reporting de defectos | F5 |
| **devops** | CI/CD, contenedorización, despliegue, environment management | F4, F7 |
| **change-manager** | Control de cambios, impacto, aprobaciones | F1–F9 |
| **project-manager** | Planificación, cronograma, hitos, riesgos, reportes de estado, gates | F1–F9 |

---

## 10. Hitos del Proyecto

### 10.1 Cronograma de Fases

```mermaid
gantt
    title Abax-Memory v2.0.0 — Cronograma de Fases
    dateFormat  YYYY-MM-DD
    axisFormat  %d %b

    section Fase 0 — Descubrimiento
    Visión de Producto            :f0a, 2026-05-03, 1d
    Épicas y Features             :f0b, 2026-05-03, 1d
    Historias de Usuario          :f0c, 2026-05-03, 1d
    Backlog Priorizado            :f0d, 2026-05-03, 1d
    Gate F0: Backlog aprobado     :milestone, f0g, 2026-05-03, 0d

    section Fase 1 — Inicio
    Acta de Constitución          :f1a, 2026-05-03, 1d
    Gate F1: Charter aprobado     :milestone, f1g, 2026-05-03, 0d

    section Fase 2 — Análisis Funcional
    Especificación Funcional      :f2a, after f1g, 3d
    Reglas de Negocio             :f2b, after f1g, 3d
    Gate F2: Spec aprobada        :milestone, f2g, after f2a, 0d

    section Fase 3 — Diseño Técnico
    ADR de Stack                  :f3a, after f2g, 2d
    Diseño Técnico Detallado      :f3b, after f3a, 3d
    Gate F3: Diseño aprobado      :milestone, f3g, after f3b, 0d

    section Fase 4 — Construcción
    R1 — MVP Core (43 historias)  :f4a, after f3g, 21d
    Anti-Mock Review              :f4b, after f4a, 2d
    R2 — MVP Completo (23 hist.)  :f4c, after f4b, 16d
    Gate F4: Código entregado     :milestone, f4g, after f4c, 0d

    section Fase 5 — Pruebas QA
    QA Funcional + Regresión      :f5a, after f4g, 5d
    Gate F5: QA aprobado          :milestone, f5g, after f5a, 0d

    section Fase 6 — UAT
    Validación con Sponsor        :f6a, after f5g, 3d
    Gate F6: UAT aprobado         :milestone, f6g, after f6a, 0d

    section Fase 7 — Despliegue
    Deploy a Producción           :f7a, after f6g, 2d
    Gate F7: Prod operativo       :milestone, f7g, after f7a, 0d

    section Fase 8 — Estabilización
    Monitoreo + Fixes             :f8a, after f7g, 3d
    Gate F8: Sistema estable      :milestone, f8g, after f8a, 0d

    section Fase 9 — Cierre
    Informe Final + Lecciones     :f9a, after f8g, 1d
    Gate F9: Proyecto cerrado     :milestone, f9g, after f9a, 0d
```

> **Nota sobre duraciones**: Las estimaciones son preliminares y serán refinadas en Fase 3 (Diseño Técnico) una vez que el stack se fije mediante ADR. Las duraciones de construcción (F4) asumen equipo de desarrollo concurrente.

### 10.2 Lista de Hitos (Milestones)

| Hito | Fase | Descripción | Criterio de Salida | Aprobador |
|---|---|---|---|---|
| **M0** | F0 | Backlog de producto aprobado | Visión de producto, épicas, historias y backlog priorizado completados y aprobados. | Sponsor |
| **M1** | F1 | Proyecto formalmente autorizado | Acta de constitución firmada. | Sponsor |
| **M2** | F2 | Especificación funcional aprobada | Reglas de negocio, criterios de aceptación y perfiles de dominio documentados. | Sponsor + BA |
| **M3** | F3 | Diseño técnico aprobado | ADR de stack aprobado. Arquitectura, modelo de datos, contratos API documentados. | Tech Lead |
| **M4** | F4 | Construcción completada (R1+R2) | 66 historias Must implementadas. Anti-mock review superado. Código en repositorio. | Tech Lead |
| **M5** | F5 | QA funcional aprobado | Suite de pruebas ejecutada. Defectos críticos = 0. CE-07, CE-08, CE-10, CE-11 verificados. | QA Lead |
| **M6** | F6 | UAT aprobado | Criterios de éxito CE-01 a CE-11 validados. Sponsor da conformidad. | Sponsor |
| **M7** | F7 | Despliegue a producción completado | Imagen publicada en GHCR. Contenedor operativo. Health check verde. | DevOps |
| **M8** | F8 | Estabilización completada | Sin incidentes críticos durante ventana de estabilización. Métricas dentro de metas. | DevOps + QA |
| **M9** | F9 | Proyecto cerrado | Informe de cierre, lecciones aprendidas y documentación final archivada. | Sponsor |

---

## 11. Estructura de Gobernanza

### 11.1 Principios de Gobernanza

v2.0.0 adopta una estructura de gobernanza **lightweight** que prioriza decisiones rápidas con trazabilidad completa:

| Principio | Descripción |
|---|---|
| **Decisiones técnicas por ADR** | Cualquier cambio en el stack, arquitectura o contratos de API debe documentarse mediante un Architecture Decision Record. |
| **Control de cambios formal** | Todo cambio de alcance, tiempo o costo requiere evaluación de impacto documentada y aprobación del sponsor. |
| **Gates de fase vinculantes** | No se avanza a la siguiente fase sin aprobación explícita del gate actual. |
| **English-Only compliance** | Verificación automatizada (linter) en CI. Violaciones bloquean el merge. |
| **Rol del Project Manager** | Facilita, no controla. Escala bloqueos, mantiene trazabilidad, reporta estado. No aprueba cambios de alcance — eso es prerrogativa del sponsor. |
| **Rol del Sponsor** | Aprueba alcance, presupuesto y criterios de éxito. Decide en controversias de priorización. Aprueba gates F0, F1, F2, F6, F9. |

### 11.2 Comité de Control de Cambios (CCB)

| Miembro | Rol | Responsabilidad |
|---|---|---|
| **Usuario Sponsor** | Presidente del CCB | Aprobación o rechazo final de cambios de alcance. |
| **Project Manager** | Secretario del CCB | Registro de solicitudes, evaluación de impacto, comunicación de decisiones. |
| **Tech Lead** | Asesor técnico | Evaluación de impacto técnico de cambios propuestos. |
| **Business Analyst** | Asesor funcional | Evaluación de impacto funcional y en criterios de éxito. |

### 11.3 Proceso de Escalamiento

```
Bloqueo detectado
    ↓
¿Puede resolverlo el rol responsable?
    ├── Sí → Resolver. Registrar en bitácora.
    └── No → Escalar a Project Manager.
              ↓
              ¿Puede resolverlo el PM?
              ├── Sí → Decidir. Informar a afectados.
              └── No → Escalar a Sponsor con análisis de impacto y opciones.
```

### 11.4 Frecuencia de Reportes

| Reporte | Frecuencia | Audiencia | Responsable |
|---|---|---|---|
| **Reporte de avance** | Diario (durante F4) / Semanal (resto) | Sponsor, equipo | Project Manager |
| **Matriz de riesgos actualizada** | En cada gate de fase | Sponsor, equipo | Project Manager |
| **Dashboard de entregables** | En cada gate de fase | Sponsor | Project Manager |
| **Acta de reunión de gate** | Al cierre de cada fase | Sponsor, equipo | Project Manager |

---

## 12. Matriz de Riesgos de Alto Nivel

| ID | Riesgo | Categoría | Probabilidad | Impacto | Nivel | Mitigación | Responsable | Estado |
|---|---|---|---|---|---|---|---|---|
| **R-01** | Stack no definido al iniciar F4 — La decisión de stack (Quarkus vs. alternativa) impacta estimaciones y arquitectura. | Técnico | Alta | Alto | **Crítico** | ADR de stack como precondición de F4. Estimaciones usan rangos amplios hasta fijar stack. Prototipo de validación en F3. | Tech Lead | Abierto |
| **R-02** | OpenAI como dependencia externa — Latencia, costos o indisponibilidad del servicio de embeddings afectan búsqueda semántica. | Externo | Media | Alto | **Alto** | Generación asíncrona de embeddings (HU-005.07.1). Health check monitorea disponibilidad (HU-004.07.1). ADR para evaluar motores alternativos si es necesario. | Tech Lead + DevOps | Abierto |
| **R-03** | Keycloak como dependencia de seguridad — Sin autenticación OIDC funcional, la API no es segura y no se pueden exponer endpoints. | Técnico | Baja | Crítico | **Alto** | Auth JWT (HU-004.10.1) priorizado en R1 temprano. Keycloak es componente maduro con amplia adopción. Health check incluye verificación de OIDC. | Tech Lead + DevOps | Abierto |
| **R-04** | Complejidad del modelo de datos — 8 kinds × 6 estados × 9 relaciones = 432 combinaciones posibles. Riesgo de bugs por combinatoria. | Técnico | Media | Medio | **Medio** | EP-001 se implementa completo en R1 para forzar validación temprana del modelo. Tests de integración cubren los 9 tipos de relación (CE-11). | Tech Lead + QA Lead | Abierto |
| **R-05** | Los perfiles de dominio requieren lógica custom — Si un perfil no puede definirse solo con configuración, HU-002.04.1 y HU-002.05.1 suben de M a L. | Funcional | Media | Alto | **Alto** | Prototipar mecanismo de perfiles con Ops (HU-002.03.1) en R1 para validar el supuesto S-02 tempranamente. | Tech Lead | Abierto |
| **R-06** | Benchmarks no alcanzan metas (CE-01/02/03) — NDCG@10 o Recall no llegan a las metas definidas. | Técnico | Media | Alto | **Alto** | Suite de ~100 test cases interna (CE-05) como red de seguridad. Re-ranking (HU-005.04.1) mejora precisión. Evaluar cambio de motor de embeddings si los resultados son insuficientes. | Tech Lead + QA Lead | Abierto |
| **R-07** | Adopción del frontend — Si el frontend llega tarde, no hay validación con usuarios no técnicos. | Organizacional | Media | Medio | **Medio** | Formulario de creación (HU-009.01.1) y auth (HU-009.08.1) en R1 para tener UI operativa mínima desde el inicio. | Developer Frontend | Abierto |
| **R-08** | Scope creep por nuevos dominios — Stakeholders solicitan soporte para dominios no contemplados en MVP durante el desarrollo. | Organizacional | Media | Medio | **Medio** | Control de cambios formal. Perfiles de dominio son configurables (R-03). Nuevos perfiles se agregan sin modificar el core. | Project Manager | Abierto |

> **Estado de la matriz al inicio del proyecto**: 8 riesgos identificados. 1 crítico, 4 altos, 3 medios. El riesgo crítico (R-01) tiene mitigación directa con el ADR de stack en Fase 3.

---

## 13. Dependencias Externas

| # | Dependencia | Tipo | Impacto si no está disponible | Acción |
|---|---|---|---|---|
| **D-01** | Stack de infraestructura (PostgreSQL, Qdrant, Keycloak, OpenAI) | Técnica | Bloquea F4 (Construcción). | ADR de stack en F3. Si se cambia algún componente, documentar y aprobar antes de iniciar F4. |
| **D-02** | Propuesta técnica aprobada (`PROPUESTA-ABAX-MEMORY-GENERICO.md`) | Documental | Sin base para modelo de datos API y perfiles. | Cualquier desviación debe documentarse y aprobarse. |
| **D-03** | Definición de perfiles de dominio (Ops, Agent, Business) | Funcional | Los perfiles requieren refinamiento con expertos de cada dominio. | Refinar en Fase 2 (Análisis Funcional). |
| **D-04** | Disponibilidad de datasets de benchmark (BEIR SciFact, LoCoMo) | Validación | CE-01, CE-02 y CE-03 no se pueden verificar. | Descargar y almacenar localmente al inicio de F5. |
| **D-05** | Keycloak o proveedor OIDC equivalente | Seguridad | Autenticación y autorización multi-tenant no funcionan. | Configurar temprano en F4. Health check OIDC en HU-004.07.1. |

---

## 14. Aprobaciones

| Rol | Nombre | Fecha | Firma |
|---|---|---|---|
| **Sponsor / Product Owner** | Usuario | 2026-05-03 | ✅ Aprobado |
| **Project Manager** | project-manager (orquestador Abax) | 2026-05-03 | ✅ Emitido |

> **Nota**: Esta acta de constitución formaliza el inicio del proyecto v2.0.0. Una vez aprobada por el sponsor, el proyecto queda autorizado para proceder a la Fase 2 — Análisis Funcional. Cualquier cambio al contenido de esta acta debe seguir el proceso de control de cambios documentado en la sección 11.2.

---

## 15. Glosario

- **ADR**: Architecture Decision Record — documento que registra una decisión arquitectónica, su contexto, alternativas evaluadas y consecuencias.
- **BEIR**: Benchmarking Information Retrieval — conjunto de datasets estándar para evaluar sistemas de búsqueda semántica.
- **GHCR**: GitHub Container Registry — registro de imágenes de contenedor donde se publica la imagen Docker del producto.
- **LoCoMo**: Long-Context Memory — benchmark para evaluación de memoria conversacional de largo plazo en agentes de IA.
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que mide la calidad de los resultados en las primeras 10 posiciones.
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0 para verificar identidad de usuarios y obtener claims.
- **Qdrant**: Base de datos vectorial open-source utilizada para almacenar embeddings y ejecutar búsqueda semántica por similitud de coseno.
