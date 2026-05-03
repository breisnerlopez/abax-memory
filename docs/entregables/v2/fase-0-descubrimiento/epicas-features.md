---
fase: 0 — Descubrimiento (v2.0.0)
entregable: Mapa de Épicas y Features
responsable: business-analyst
fecha: 2026-05-03
release: v2.0.0
estado: Completado
fuentes:
  - docs/entregables/v2/fase-0-descubrimiento/vision-producto.md
  - /root/proyectos-personales/administrador/PROPUESTA-ABAX-MEMORY-GENERICO.md
---

# Mapa de Épicas y Features — Abax-Memory v2.0.0

## Tabla de Contenidos

- [Resumen Ejecutivo](#resumen-ejecutivo)
- [EP-001: Motor de Memoria Genérico](#ep-001-motor-de-memoria-genérico)
- [EP-002: Perfiles de Dominio](#ep-002-perfiles-de-dominio)
- [EP-003: Scoping Multi-Tenant](#ep-003-scoping-multi-tenant)
- [EP-004: API REST v2](#ep-004-api-rest-v2)
- [EP-005: Búsqueda Semántica + Graph](#ep-005-búsqueda-semántica--graph)
- [EP-006: Gobernanza y Trazabilidad](#ep-006-gobernanza-y-trazabilidad)
- [EP-007: Batch Ingestion](#ep-007-batch-ingestion)
- [EP-008: Migración v1→v2](#ep-008-migración-v1v2)
- [EP-009: Frontend Multi-Dominio](#ep-009-frontend-multi-dominio)
- [EP-010: SDK Python Básico](#ep-010-sdk-python-básico)
- [Resumen MoSCoW](#resumen-moscow)
- [Fuera de Alcance (Won't)](#fuera-de-alcance-wont)
- [Trazabilidad con Criterios de Éxito](#trazabilidad-con-criterios-de-éxito)
- [Glosario](#glosario)

---

## Resumen Ejecutivo

Este documento descompone la Visión del Producto y la Propuesta Técnica de Abax-Memory v2.0.0 en **10 épicas** y **85+ features** organizadas con priorización MoSCoW. Cada épica agrupa funcionalidades cohesivas desde la perspectiva del usuario final y los consumidores de la API.

La priorización se basa en los criterios de éxito definidos en la Visión del Producto, las reglas de negocio fundamentales (BR-001 a BR-010) y la decisión del sponsor de descartar v1 por completo.

---

## EP-001: Motor de Memoria Genérico

**Descripción**: Establece el núcleo del producto — el modelo de datos universal que permite a cualquier profesional, independientemente de su dominio, capturar, estructurar y relacionar conocimiento mediante tipos de memoria, estados de ciclo de vida, relaciones tipadas y extracción de entidades. Es el fundamento sobre el que se construyen todas las demás capacidades.

**Prioridad**: Must

**Features**:

- **FT-001.01: Ocho Kinds Universales** — Implementación de los 8 tipos de memoria (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`) como clasificación primaria de toda memoria en el sistema, con validación estricta del enum al crear o modificar.

- **FT-001.02: Ciclo de Vida con Seis Estados** — Máquina de estados con 6 valores (`draft`, `pending`, `active`, `archived`, `rejected`, `deleted`), reglas de transición explícitas (BR-005: `draft → pending`, `pending → active | rejected`, `active → archived`, cualquier estado → `deleted`), y prohibición de la transición `active → draft`.

- **FT-001.03: Nueve Tipos de Relación Estructurada** — Implementación de los 9 tipos de relación (`related_to`, `depends_on`, `caused_by`, `resolves`, `contradicts`, `supports`, `mentions`, `belongs_to`, `supersedes`) con direccionalidad explícita (dirigida vs. bidireccional) y validación de target existente no `deleted` (BR-007).

- **FT-001.04: Extracción de Entidades desde Texto** — Capacidad de extraer entidades nombradas de texto libre sin persistir (`POST /memories/extract`), con búsqueda posterior de entidades por nombre y detalle de entidades con sus memorias vinculadas.

- **FT-001.05: Modelo de Metadatos Extensibles** — Campo `metadata` de tipo libre (key-value) que permite a cada perfil de dominio y a cada usuario enriquecer las memorias con atributos específicos sin modificar el schema del core.

- **FT-001.06: Modelo de Source Tipado** — Atributo `source` en cada memoria con `type` controlado (`conversation`, `document`, `api`, `workflow`, `manual`, `case`) e `id` externo para trazabilidad del origen del conocimiento.

- **FT-001.07: Soft-Delete y Purgado** — Eliminación lógica que marca `lifecycle.status = deleted` sin borrado físico (BR-002), preservando trazabilidad. El purgado físico queda reservado para `memory-admin` mediante endpoint administrativo futuro.

- **FT-001.08: Versionado de Memorias con Supersedes** — Mecánica de versionado donde una memoria nueva puede declarar que reemplaza una versión anterior mediante la relación `supersedes`, permitiendo evolución del conocimiento sin perder el historial ni violar la regla `active → draft` prohibida (BR-005).

- **FT-001.09: Modelo de Confidence** — Atributo `lifecycle.confidence` (0.0 a 1.0) que refleja el nivel de certeza sobre la corrección de una memoria, permitiendo filtrar búsquedas por umbral mínimo de confianza y señalando memorias que requieren verificación humana.

---

## EP-002: Perfiles de Dominio

**Descripción**: Proporciona un mecanismo de configuración que adapta el motor de memoria genérico a las necesidades de distintos verticales (IT Operations, memoria conversacional para agentes, CRM/Legal/Finanzas) sin modificar el core ni la API base. Cada perfil define kinds recomendados, vocabulario controlado, metadatos extra y reglas de sensibilidad.

**Prioridad**: Must

**Features**:

- **FT-002.01: Mecanismo de Definición de Perfiles** — Infraestructura para definir, almacenar y versionar perfiles de dominio como configuraciones (JSON/YAML o registros en base de datos), garantizando que ningún perfil requiera código custom ni modificación del core (Restricción R-03).

- **FT-002.02: Herencia del Core Genérico** — Cada perfil hereda automáticamente los 8 kinds, 6 estados, 9 tipos de relación y 4 niveles de sensibilidad del core. El perfil solo especializa recomendaciones, defaults y metadatos sin restringir las capacidades base.

- **FT-002.03: Perfil Ops (IT Operations)** — Configuración predefinida para equipos de SRE, DevOps y soporte: kinds clave (`event` como incidentes, `procedure` como runbooks), tags (`incident`, `runbook`, `alert`, `maintenance`), y metadatos extra (`affectedService`, `remediationSteps`, `rootCause`). Mapea `lifecycle.importance` al concepto de `criticality`.

- **FT-002.04: Perfil Agent (Conversational Memory)** — Configuración para agentes IA y chatbots: kinds clave (`fact` como hechos del usuario, `preference` como preferencias, `event` como interacciones pasadas, `decision` como decisiones previas), scoping intensivo por `scope.userId` y `scope.sessionId`, y priorización por `lifecycle.importance` para ventana de contexto.

- **FT-002.05: Perfil Business (CRM/Legal/Finanzas)** — Configuración para entornos corporativos: kinds clave (`entity` como cliente/empresa/producto, `decision` como acuerdo/contrato, `note` como minutas de reunión, `task` como acción/compromiso), relaciones dominantes (`belongs_to` cliente, `related_to` oportunidad), y metadatos extra (`clientName`, `contractId`, `opportunityValue`).

- **FT-002.06: Reglas de Sensibilidad por Defecto según Perfil** — Cada perfil define valores por defecto de `lifecycle.sensitivity` y `lifecycle.importance` (BR-009). Si el usuario no los especifica, el sistema aplica el default del perfil activo. Sin perfil, el default es `importance = 0.5` y `sensitivity = internal`.

- **FT-002.07: Vocabulario Controlado por Perfil** — Cada perfil sugiere tags y topics predefinidos para clasificación consistente dentro de un dominio, facilitando la adopción por parte de usuarios no técnicos y mejorando la calidad de búsquedas y filtros posteriores.

- **FT-002.08: Extensibilidad para Nuevos Perfiles** — La arquitectura de perfiles permite que nuevos dominios (salud, educación, logística, etc.) se agreguen como configuraciones adicionales sin cambios en el core ni en la API, cumpliendo la restricción de no entregar un catálogo cerrado de dominios.

---

## EP-003: Scoping Multi-Tenant

**Descripción**: Implementa el aislamiento de datos entre tenants, usuarios y sesiones mediante el modelo `scope` (`tenantId`, `userId`, `sessionId`, `namespace`). Garantiza que un inquilino nunca acceda a datos de otro, permite búsquedas por usuario o sesión, y provee capacidades de cross-tenant access para administradores autorizados.

**Prioridad**: Must

**Features**:

- **FT-003.01: Aislamiento por tenantId** — Toda memoria pertenece a un `scope.tenantId`. Las búsquedas y consultas filtran automáticamente por el `tenantId` derivado del token OIDC/Keycloak. Un tenant no puede ver memorias de otro tenant (BR-004). Los índices en PostgreSQL se particionan por `scope.tenantId` para garantizar rendimiento.

- **FT-003.02: Scoping por userId** — Las memorias pueden asociarse opcionalmente a un `scope.userId`, permitiendo búsquedas acotadas a un usuario específico. Esencial para el perfil Agent (memoria conversacional por usuario) y para auditorías por operador.

- **FT-003.03: Scoping por sessionId** — Las memorias pueden asociarse a un `scope.sessionId`, permitiendo aislar el contexto de una sesión conversacional específica. Fundamental para agentes que manejan múltiples sesiones simultáneas sin mezclar contextos.

- **FT-003.04: Namespace como Subdivisión Adicional** — Campo `scope.namespace` que permite subdivisiones arbitrarias dentro de un tenant (ej. por proyecto, por departamento, por cliente final) sin crear tenants separados. Útil para organizaciones grandes con múltiples unidades de negocio.

- **FT-003.05: Cross-Tenant Access para memory-admin** — Los usuarios con rol `memory-admin` y permisos explícitos pueden consultar y operar cross-tenant para tareas de depuración, consolidación y gobierno global del repositorio. Toda operación cross-tenant queda registrada en auditoría.

- **FT-003.06: Scope Obligatorio en Escritura** — La creación de cualquier memoria requiere `scope` con al menos `tenantId` (BR-003). `userId` y `sessionId` son opcionales pero recomendados. Las requests sin `scope` son rechazadas con error de validación.

- **FT-003.07: Filtrado Automático en Lectura** — Toda operación de lectura (GET, search) aplica automáticamente el filtro de scope derivado del token de autenticación, sin requerir que el consumidor lo especifique explícitamente. El filtro explícito en la request solo puede restringir, nunca ampliar, el alcance del token.

---

## EP-004: API REST v2

**Descripción**: Expone todas las capacidades del motor de memoria mediante una API REST bajo `/api/v2/`, diseñada como contrato público y estable. Incluye endpoints para CRUD de memorias, relaciones, revisión, extracción de entidades, estadísticas, health check, métricas y batch ingestion. Cumple el principio English-Only en todos los identificadores internos.

**Prioridad**: Must

**Features**:

- **FT-004.01: CRUD de Memorias** — Endpoints `POST /memories`, `GET /memories/{id}`, `PATCH /memories/{id}`, `DELETE /memories/{id}` con validación completa del modelo de datos (kinds, lifecycle, scope, relations, metadata, source) y códigos de error estandarizados.

- **FT-004.02: Gestión de Relaciones** — Endpoints `POST /memories/{id}/relations` para crear relaciones tipadas y `DELETE /memories/{id}/relations/{relId}` para eliminarlas. Validación de que el `targetId` existe y no está `deleted` (BR-007), con código de error `TARGET_NOT_FOUND`.

- **FT-004.03: Expansión de Grafo** — Endpoint `GET /memories/{id}/graph?depth=2` que devuelve el subgrafo de relaciones alrededor de una memoria, con profundidad configurable y filtro opcional por `includeKinds` para limitar los tipos de nodos expandidos.

- **FT-004.04: Revisión de Estados** — Endpoint `POST /memories/{id}/review` que permite transicionar el `lifecycle.status` de una memoria. Soporta las acciones: aprobar (`pending → active`), rechazar (`pending → rejected`), archivar (`active → archived`). Registra `reviewedBy` y `reviewedAt`. Respeta las reglas de transición (BR-005) y el umbral de revisión obligatoria (BR-006).

- **FT-004.05: Búsqueda y Detalle de Entidades** — Endpoints `GET /entities?q=...` para buscar entidades por nombre y `GET /entities/{name}` para obtener detalle de una entidad con todas las memorias vinculadas. Complementa la extracción del motor (FT-001.04).

- **FT-004.06: Estadísticas por Tenant** — Endpoint `GET /scopes/{tenantId}/stats` que devuelve métricas agregadas: total de memorias, distribución por kind, por status, por sensitivity, tasas de revisión, crecimiento en el tiempo. Requiere permisos de `memory-admin` o `memory-auditor`.

- **FT-004.07: Health Check y Métricas Operativas** — Endpoints `GET /health` para verificar disponibilidad de todos los servicios dependientes (Qdrant, PostgreSQL, OpenAI) y `GET /metrics` para exponer métricas de latencia, throughput, tasas de error y uso de recursos en formato compatible con Prometheus.

- **FT-004.08: English-Only en Identificadores de API** — Todos los paths, query params, códigos de error, enums y respuestas de la API están estandarizados en inglés (`UPPER_SNAKE_CASE` para enums y códigos, `lower_snake_case` para campos). No existe rastro de español en ningún identificador interno (BR-010, Restricción R-04).

- **FT-004.09: Documentación OpenAPI 3.x** — Especificación OpenAPI completa de todos los endpoints v2, incluyendo schemas de request/response, códigos de error, ejemplos de uso y autenticación requerida. Sirve como contrato para el frontend, SDKs y consumidores externos. Accesible desde `/api/v2/openapi.json`.

- **FT-004.10: Autenticación y Autorización OIDC/Keycloak** — Todas las requests a `/api/v2/` requieren Bearer token JWT. Keycloak (o el proveedor OIDC configurado) es la única fuente de identidad y roles (Restricción R-07). Los claims del token determinan `scope.tenantId`, `userId` y los roles RBAC del usuario.

- **FT-004.11: Estándares de Códigos de Error HTTP y Cuerpos de Error** — Respuestas de error estandarizadas con código HTTP apropiado, `errorCode` machine-readable (`INVALID_JSON`, `VALIDATION_ERROR`, `TARGET_NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `DATABASE_UNAVAILABLE`, `UNSUPPORTED_MEDIA_TYPE`, `INVALID_REQUEST_BODY`, `BATCH_SIZE_EXCEEDED`) y `message` human-readable.

- **FT-004.12: Validación de Request Bodies** — Validación estricta de payloads JSON contra el schema del modelo, rechazando campos desconocidos, tipos incorrectos y valores de enum inválidos antes de cualquier procesamiento de negocio. Errores de validación incluyen el campo específico que falló.

- **FT-004.13: Rate Limiting por Tenant y Usuario** — Limitación de tasa de requests por `tenantId` y `userId` para proteger el sistema de abusos y garantizar calidad de servicio equitativa entre tenants. Configurable por perfil de tenant.

---

## EP-005: Búsqueda Semántica + Graph

**Descripción**: Implementa la búsqueda semántica híbrida que combina embeddings vectoriales (Qdrant + OpenAI) con filtros estructurados multidimensionales, expansión de subgrafo para navegación de relaciones, re-ranking para mejorar precisión top-K, y multi-hop traversal para consultas complejas. Es la capacidad central de consumo del motor de memoria.

**Prioridad**: Must

**Features**:

- **FT-005.01: Búsqueda por Texto Libre con Qdrant + OpenAI Embeddings** — Búsqueda semántica que recibe `query` en texto libre, genera embedding con OpenAI `text-embedding-3-large` (3072 dimensiones) y recupera los top-K resultados más similares por similitud de coseno en Qdrant. Soporta contenido en cualquier idioma.

- **FT-005.02: Filtros Estructurados Multidimensionales** — Capacidad de refinar búsquedas mediante filtros simultáneos en hasta 8 dimensiones: `scopes` (tenant, user, session), `kinds`, `statuses`, `topics`, `entities`, `importance` (rango gte/lte), `confidence` (rango gte/lte), `sensitivities`, y rango de fechas (`createdAfter`, `createdBefore`). Los filtros se aplican antes, durante o después del ranking vectorial según optimización.

- **FT-005.03: Expansión de Subgrafo en Resultados** — Parámetro `expandGraph` en la búsqueda que, para cada memoria recuperada, devuelve sus vecinos inmediatos (depth configurable) filtrados opcionalmente por `includeKinds`. Permite al consumidor navegar el contexto de relaciones sin llamadas adicionales.

- **FT-005.04: Re-Ranking de Resultados** — Parámetro `rerank` que activa una segunda pasada de scoring sobre los resultados crudos de Qdrant, combinando score semántico con señales de importancia, confianza, frescura (`updatedAt`) y riqueza de relaciones para mejorar la precisión en el top-K.

- **FT-005.05: Multi-Hop Traversal** — Soporte para consultas que requieren seguir múltiples saltos de relaciones (ej. "encuentra todas las decisiones que dependen de eventos causados por incidentes del servicio X"). Implementado mediante expansión recursiva de grafo con límite de profundidad y filtros por tipo de relación.

- **FT-005.06: Top-K Configurable** — Parámetro `topK` en la búsqueda que permite al consumidor controlar cuántos resultados retorna el sistema. Límite superior configurable por tenant para prevenir abusos. Default: 10.

- **FT-005.07: Embedding de Nuevas Memorias** — Toda memoria creada o actualizada (cuyo `content` cambió) dispara automáticamente la generación de un nuevo embedding vectorial mediante OpenAI y su indexación en Qdrant. El embedding se almacena en Qdrant vinculado al `memoryId` para recuperación posterior.

- **FT-005.08: Re-Indexación Masiva** — Capacidad administrativa de regenerar embeddings y reindexar en Qdrant todas las memorias de un tenant o del repositorio completo. Necesaria tras cambios de motor de embeddings o migración de datos. Requiere rol `memory-admin`.

- **FT-005.09: Filtrado por lifecycle.status Gobernado** — La búsqueda por defecto (sin filtro explícito de `statuses`) solo retorna memorias con `lifecycle.status = active` (BR-001). `draft` y `pending` requieren filtro explícito y permisos de `memory-reviewer` o `memory-admin`. `archived`, `rejected` y `deleted` nunca aparecen sin acción explícita.

- **FT-005.10: Scoring Transparente** — Cada resultado de búsqueda incluye `score` numérico (0.0 a 1.0) que representa la relevancia semántica calculada. El score es trazable y reproducible para auditoría de calidad de búsqueda.

---

## EP-006: Gobernanza y Trazabilidad

**Descripción**: Garantiza que toda operación sobre el repositorio de memoria sea trazable, auditable y sujeta a reglas de visibilidad y revisión humana. Implementa el diferenciador competitivo de Abax-Memory: auditoría completa con diff antes/después, ciclo de vida con revisión humana, reglas de visibilidad por estado, umbrales de revisión obligatoria y control de acceso RBAC con 5 roles.

**Prioridad**: Must

**Features**:

- **FT-006.01: Auditoría Completa de Mutaciones** — Toda operación de escritura (creación, modificación, cambio de estado, soft-delete, creación/eliminación de relación) genera un registro de auditoría inmutable con: `timestamp`, `userId`, `action`, `memoryId`, `diff` (antes/después en formato estructurado), `ipAddress` y `userAgent`. Cumple Restricción R-08 (trazabilidad completa) y Criterio de Éxito CE-09 (100% de mutaciones auditadas).

- **FT-006.02: Flujo de Revisión Humana** — Workflow completo de revisión: el operador crea una memoria en `draft`, solicita revisión (`draft → pending`), el revisor evalúa y aprueba (`pending → active`) o rechaza (`pending → rejected`). El revisor debe registrar `reviewedBy`. El operador puede iterar sobre `draft` antes de enviar a `pending`.

- **FT-006.03: Visibilidad Gobernada por Estado** — Regla de negocio BR-001 implementada en el endpoint de búsqueda: sin filtro de `statuses`, solo se devuelven memorias `active`. `draft` y `pending` requieren filtro explícito y permisos `memory-reviewer` o superiores. `archived` y `rejected` son invisibles para `api-consumer`. `deleted` solo visible para `memory-admin`.

- **FT-006.04: Umbral de Revisión Obligatoria** — Cuando una memoria se crea con `lifecycle.importance >= 0.7` Y `lifecycle.sensitivity IN (confidential, secret)`, el sistema fuerza el estado inicial a `draft` o `pending`, nunca `active`. La memoria no puede transicionar a `active` sin revisión humana explícita (BR-006). Solo `memory-admin` puede saltar esta regla, dejando justificación en auditoría.

- **FT-006.05: Linaje de Decisiones** — Trazabilidad completa de qué memorias influyeron en qué decisiones. Permite responder preguntas como "¿qué hechos y eventos llevaron a esta decisión?" y "¿qué decisiones se tomaron basadas en este procedimiento?" mediante consultas de grafo navegando relaciones `supports`, `caused_by`, `depends_on` y `supersedes`.

- **FT-006.06: Depuración de Repositorio** — Herramientas para `memory-admin` y `memory-auditor`: archivar memorias obsoletas (`active → archived`), fusionar duplicadas (merge de contenido y relaciones con trazabilidad), soft-delete de memorias inválidas, y consulta de métricas de calidad del repositorio (proporción de drafts huérfanos, memorias sin relaciones, etc.).

- **FT-006.07: Control de Acceso RBAC con Cinco Roles** — Implementación de los 5 roles definidos en la Visión: `memory-operator` (crear, editar, relacionar, scope propio), `memory-reviewer` (revisar, aprobar, rechazar en su scope), `api-consumer` (solo lectura de `active`), `memory-admin` (depuración, cross-tenant, purgado), `memory-auditor` (lectura de todo, acceso a audit logs, sin capacidad de escritura).

- **FT-006.08: Registro de Cambios en Relaciones** — Las relaciones también generan registros de auditoría: creación de relación (qué memoria estableció qué conexión con cuál otra), eliminación de relación (quién la eliminó y cuándo). Permite reconstruir la evolución completa del grafo de conocimiento.

---

## EP-007: Batch Ingestion

**Descripción**: Permite la ingesta masiva de conocimiento desde conversaciones, documentos y otras fuentes externas en una sola llamada atómica, transformando múltiples fragmentos de información en memorias estructuradas con garantía de consistencia transaccional (todo o nada).

**Prioridad**: Should

**Features**:

- **FT-007.01: Ingesta Batch Atómica Transaccional** — Endpoint `POST /memories/ingest` que recibe un array de memorias y las persiste en una única transacción de base de datos. Si cualquier memoria del batch falla (validación, constraints, error de embedding), ninguna se persiste (BR-008: todo o nada). Garantiza consistencia del repositorio.

- **FT-007.02: Límite de 100 Memorias por Batch** — Cada llamada a `/memories/ingest` acepta un máximo de 100 memorias. Si el array excede este límite, se rechaza con código de error `BATCH_SIZE_EXCEEDED` antes de cualquier procesamiento, protegiendo los recursos del sistema.

- **FT-007.03: Ingesta desde Conversaciones** — Soporte específico para transformar logs de conversación (chat, transcripciones, historial de agente) en múltiples memorias estructuradas: cada turno o intercambio significativo se convierte en una memoria con su `kind`, `topics`, `entities` extraídas y `source.type = conversation`.

- **FT-007.04: Ingesta desde Documentos** — Soporte para procesar documentos estructurados o semi-estructurados (JSON, logs, reportes) y generar múltiples memorias. Cada sección o fragmento extraído se clasifica con el `kind` correspondiente y `source.type = document`.

- **FT-007.05: Validación Pre-Ingesta de Cada Memoria** — Antes de iniciar la transacción, el sistema valida individualmente cada memoria del batch: schema correcto, `scope` obligatorio presente, `kind` válido, `lifecycle.status` válido, `confidence` en rango, referencias a targets de relaciones existentes (o dentro del mismo batch). Los errores de validación se reportan con índice de la memoria que falló.

- **FT-007.06: Respuesta de Resultado del Batch** — La respuesta de `/memories/ingest` incluye un resumen: `totalIngested`, `memoryIds` generados, `entitiesExtracted` totales, y para cada memoria, `id` asignado y `embeddingStatus` (success/failed). Si el batch completo falla, se retorna `success: false` con el detalle del error que causó el rollback.

---

## EP-008: Migración v1→v2

**Descripción**: Script opcional y autónomo (fuera del runtime de v2) que permite a usuarios con datos en la versión 1.0.0 (PMOA) migrar sus memorias al nuevo modelo v2. Mapea tipos, estados y criticality al esquema genérico, re-ingesta con embeddings actualizados, y valida equivalencia semántica post-migración.

**Prioridad**: Could

**Features**:

- **FT-008.01: Script de Mapeo de Kinds v1→v2** — Transforma los tipos fijos de v1 al modelo genérico: `incidente → kind: event + tag incident`, `runbook → kind: procedure + tag runbook`, `procedimiento → kind: procedure`, `politica → kind: decision + tag policy`, `guia → kind: note + tag guide`, `caso → source.type: case`. El mapeo es configurable para ajustes por tenant.

- **FT-008.02: Script de Mapeo de Estados v1→v2** — Transforma estados en español al modelo en inglés: `EN_REVISION → lifecycle.status: pending`, `APROBADA → active`, `RECHAZADA → rejected`. Memorias sin estado definido en v1 se crean como `draft`.

- **FT-008.03: Mapeo de criticality → importance + sensitivity** — Descompone el campo unidimensional `criticality` de v1 en los dos ejes del nuevo modelo: `importance` (qué tan relevante es el conocimiento) y `sensitivity` (qué tan restringido debe estar). Usa una tabla de mapeo configurable por perfil de dominio.

- **FT-008.04: Re-Ingesta con Embeddings Actualizados** — Cada memoria migrada se re-procesa para generar un nuevo embedding con el motor configurado en v2 (OpenAI `text-embedding-3-large` o el que esté activo) e indexar en la colección Qdrant de v2, garantizando compatibilidad con las búsquedas del nuevo sistema.

- **FT-008.05: Validación de Equivalencia Semántica Post-Migración** — Suite de validación que compara una muestra de memorias migradas contra sus originales en v1: contenido preservado, tipo mapeado correctamente, estado equivalente, relaciones mantenidas. Requiere aprobación manual para muestras mayores a 20 memorias (Criterio de Éxito CE-013).

- **FT-008.06: Reporte de Migración Detallado** — El script genera un reporte con: total de memorias procesadas, éxitos, fallos (con causa), advertencias (mapeos que requirieron interpretación, pérdida potencial de información), y recomendaciones post-migración. El usuario decide si acepta o revierte basado en este reporte.

---

## EP-009: Frontend Multi-Dominio

**Descripción**: Interfaz de usuario web que consume la API v2 y permite a los usuarios interactuar con el motor de memoria genérico: crear y clasificar memorias, buscar con filtros avanzados, revisar y aprobar contenido, administrar tenants, visualizar el grafo de relaciones y monitorear estadísticas. Se adapta visual y semánticamente según el perfil de dominio seleccionado.

**Prioridad**: Must

**Features**:

- **FT-009.01: Creación de Memorias con Formulario Multi-Dominio** — Interfaz de creación de memorias que se adapta según el perfil de dominio activo: muestra los kinds relevantes como opciones principales, sugiere tags del vocabulario controlado del perfil, expone los campos de metadatos extra pertinentes, y permite configurar `importance`, `sensitivity` y `scope` con valores por defecto contextuales.

- **FT-009.02: Búsqueda Avanzada con Filtros Visuales** — Panel de búsqueda que expone todos los filtros del modelo `SearchRequest`: campo de texto libre, selectores de kinds, statuses, topics, entities, sliders de `importance` y `confidence`, selector de `sensitivity`, rango de fechas con date pickers, y toggle para `expandGraph` y `rerank`. Los resultados se presentan con score, kind, status y entidades destacadas.

- **FT-009.03: Panel de Revisión (Approve/Reject/Archive)** — Vista dedicada para `memory-reviewer` y `memory-admin` que lista memorias en estado `pending` (y opcionalmente `draft`), permite abrir el detalle completo, ver historial de cambios, y ejecutar acciones de revisión: aprobar, rechazar (con motivo obligatorio) o solicitar cambios (devuelve a `draft`).

- **FT-009.04: Panel de Administración Multi-Tenant** — Interfaz para `memory-admin`: gestión de tenants (crear, configurar, suspender), depuración de repositorio (archivar, fusionar duplicadas, soft-delete), vista de auditoría completa con filtros por usuario, acción y rango de fechas, y acceso cross-tenant para gobierno global.

- **FT-009.05: Visualización de Grafo de Relaciones** — Componente interactivo que renderiza el subgrafo de relaciones alrededor de una memoria (nodos como tarjetas con kind y status, edges con tipo de relación y dirección). Permite navegación clickeable: clic en un nodo expande sus vecinos, clic en un edge muestra detalle de la relación.

- **FT-009.06: Selección y Cambio de Perfil de Dominio** — Selector de perfil de dominio en la interfaz (Ops, Agent, Business) que reconfigura dinámicamente los kinds sugeridos, tags, campos de metadatos y reglas de sensibilidad sin recargar la página. El perfil seleccionado se persiste como preferencia de usuario.

- **FT-009.07: Dashboard de Estadísticas por Tenant** — Vista de dashboard con gráficos y métricas: total de memorias, distribución por kind (pie chart), evolución temporal (line chart de creaciones/semana), proporción por status (bar chart), top entities, top topics, tasa de revisión (aprobadas vs. rechazadas). Requiere permisos `memory-admin` o `memory-auditor`.

- **FT-009.08: Autenticación Integrada con Keycloak** — Flujo de login/logout integrado con Keycloak vía OIDC (Authorization Code Flow + PKCE), renovación automática de token, y adaptación de la UI según los roles del usuario (ocultar/mostrar paneles de admin, revisión, etc.). El `tenantId` y `userId` se derivan de los claims del token.

---

## EP-010: SDK Python Básico

**Descripción**: Librería Python que encapsula la API REST v2 para que desarrolladores y data scientists puedan integrar Abax-Memory en sus aplicaciones, notebooks y pipelines de datos sin preocuparse por detalles HTTP, autenticación o serialización. Es el primer paso hacia un ecosistema de SDKs multi-lenguaje.

**Prioridad**: Should

**Features**:

- **FT-010.01: Cliente HTTP con Autenticación OIDC** — Clase `AbaxMemoryClient` que maneja autenticación contra Keycloak (client credentials o resource owner password), renueva tokens automáticamente, y adjunta Bearer token a toda request. Soporta configuración por variables de entorno, archivo `.env` o parámetros de constructor.

- **FT-010.02: Métodos CRUD de Alto Nivel** — API idiomática en Python: `client.create_memory(...)`, `client.get_memory(id)`, `client.update_memory(id, ...)`, `client.delete_memory(id)`. Cada método acepta objetos Python nativos (`dict`, `list`, `Enum`) y los serializa correctamente al modelo JSON de la API.

- **FT-010.03: Búsqueda Semántica con Filtros Tipados** — Método `client.search(query=..., filters=SearchFilters(...), top_k=10, expand_graph=True)` donde `SearchFilters` es una dataclass con campos tipados (`kinds: List[MemoryKind]`, `importance_gte: float`, etc.) que se serializan al `SearchRequest` JSON. Los resultados se devuelven como objetos `SearchResult` con campos accesibles por atributo.

- **FT-010.04: Gestión de Relaciones desde SDK** — Métodos `client.add_relation(memory_id, target_id, relation_type)` y `client.remove_relation(memory_id, relation_id)` con validación local de tipos de relación (enum `RelationType`) y mensajes de error descriptivos si la API rechaza la operación.

- **FT-010.05: Documentación y Ejemplos de Uso** — README con quickstart (≤ 2 minutos para crear la primera memoria), docstrings en todas las clases y métodos públicos, y ejemplos ejecutables en un directorio `examples/` cubriendo: CRUD básico, búsqueda semántica, ingesta batch, extracción de entidades, y uso del perfil Agent para memoria conversacional.

- **FT-010.06: Manejo de Errores Tipado** — Jerarquía de excepciones Python (`AbaxMemoryError`, `ValidationError`, `AuthenticationError`, `NotFoundError`, `BatchIngestError`) que envuelven los códigos de error de la API, preservando el `errorCode` original y el mensaje human-readable para facilitar debugging.

---

## Resumen MoSCoW

| Prioridad | Épicas | Features | Justificación |
|---|---|---|---|
| **Must** | EP-001 a EP-006, EP-009 | ~65 | Core del motor, API, búsqueda, gobernanza y frontend son indispensables para el MVP. Sin ellos no hay producto. |
| **Should** | EP-007 y EP-010 | ~12 | Batch ingestion y SDK Python son importantes para adopción y productividad, pero el producto funciona sin ellos. |
| **Could** | EP-008 | ~6 | Migración v1→v2 solo aplica si existen datos en v1 que preservar. Es opcional y externa al runtime. |
| **Won't** | — | — | Ver sección _Fuera de Alcance_. |

---

## Fuera de Alcance (Won't)

Los siguientes ítems fueron evaluados y **excluidos explícitamente** de v2.0.0 según la Visión del Producto. No se descomponen en features ni se asignan a ninguna épica:

| # | Ítem | Justificación |
|---|---|---|
| **W-01** | API v1 (`/api/v1/memorias`) | Descartada por decisión del sponsor. v1 no existe en v2 (Restricción R-05). |
| **W-02** | Backward compatibility con v1 | v1.0.0 está cerrado y sin usuarios en producción. |
| **W-03** | Tipos de memoria fijos por dominio | El core es genérico. Especialización vía perfiles (Restricción R-03). |
| **W-04** | Federación multi-repositorio | MVP opera sobre un solo repositorio PostgreSQL + Qdrant. |
| **W-05** | UI especializadas por vertical | El frontend es multi-dominio genérico, no dashboards por industria. |
| **W-06** | Orquestación multi-agente | Abax-Memory es motor de memoria, no orquestador. Integración vía API/SDK. |
| **W-07** | SDKs multi-lenguaje (Node, Java, Go) | Solo SDK Python básico en MVP. El resto diferido. |
| **W-08** | Benchmarks públicos completos | Solo benchmarks internos. Publicación formal diferida. |
| **W-09** | Automatización completa de revisión humana | Memorias de alta importancia/sensibilidad siempre requieren revisor humano (BR-006). |
| **W-10** | Localización / i18n de la API | API es English-Only. Solo mensajes al usuario potencialmente localizables. |
| **W-11** | Catálogo cerrado de dominios | Perfiles son configurables y evolutivos, no un catálogo fijo. |

---

## Trazabilidad con Criterios de Éxito

Cada épica contribuye directamente a uno o más criterios de éxito definidos en la Visión del Producto:

| Épica | Criterios de Éxito Cubiertos |
|---|---|
| EP-001: Motor de Memoria Genérico | CE-06 (8/8 kinds), CE-010 (English-Only), CE-011 (9/9 relaciones) |
| EP-002: Perfiles de Dominio | CE-05 (suite multi-dominio), CE-06 (kinds por perfil) |
| EP-003: Scoping Multi-Tenant | CE-07 (aislamiento cross-tenant), CE-10 (English-Only) |
| EP-004: API REST v2 | CE-04 (latencia p95 < 500ms), CE-10 (English-Only), CE-11 (CRUD relaciones) |
| EP-005: Búsqueda Semántica + Graph | CE-01 (NDCG@10 ≥ 0.80), CE-02 (Recall@10 ≥ 0.90), CE-03 (LoCoMo ≥ 0.80), CE-04 (latencia), CE-05 (top-1 ≥ 0.92), CE-08 (visibilidad por estado) |
| EP-006: Gobernanza y Trazabilidad | CE-08 (visibilidad por estado), CE-09 (100% mutaciones auditadas) |
| EP-007: Batch Ingestion | CE-12 (batch 100 memorias, atomicidad) |
| EP-008: Migración v1→v2 | CE-13 (100% memorias migradas sin pérdida semántica) |
| EP-009: Frontend Multi-Dominio | CE-05 (tests funcionales end-to-end) |
| EP-010: SDK Python Básico | CE-05 (facilita ejecución de suite de tests) |

---

## Glosario

- **MoSCoW**: Método de priorización con cuatro niveles: Must (debe estar), Should (debería estar), Could (podría estar), Won't (no se incluirá).
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0 que permite verificar identidad de usuarios y obtener claims (roles, tenants) desde un proveedor centralizado como Keycloak.
- **RBAC**: Role-Based Access Control — modelo de control de acceso donde los permisos se asignan a roles y los usuarios heredan los permisos del rol que se les asigna.
- **Qdrant**: Base de datos vectorial open-source utilizada para almacenar embeddings y ejecutar búsqueda semántica por similitud de coseno.
- **CRUD**: Create, Read, Update, Delete — las cuatro operaciones fundamentales de gestión de datos.
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que mide la calidad de los resultados en las primeras 10 posiciones.
- **PKCE**: Proof Key for Code Exchange — extensión de OAuth 2.0 que protege contra ataques de interceptación de código de autorización en aplicaciones públicas.
