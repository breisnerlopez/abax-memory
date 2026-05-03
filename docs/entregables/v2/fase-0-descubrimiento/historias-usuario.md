---
fase: 0 — Descubrimiento (v2.0.0)
entregable: Historias de Usuario
responsable: business-analyst
fecha: 2026-05-03
release: v2.0.0
estado: Completado
fuentes:
  - docs/entregables/v2/fase-0-descubrimiento/vision-producto.md
  - docs/entregables/v2/fase-0-descubrimiento/epicas-features.md
  - /root/proyectos-personales/administrador/PROPUESTA-ABAX-MEMORY-GENERICO.md
---

# Historias de Usuario — Abax-Memory v2.0.0

## Índice

- [Introducción](#introducción)
- [EP-001: Motor de Memoria Genérico](#ep-001-motor-de-memoria-genérico)
- [EP-002: Perfiles de Dominio](#ep-002-perfiles-de-dominio)
- [EP-003: Scoping Multi-Tenant](#ep-003-scoping-multi-tenant)
- [EP-004: API REST v2](#ep-004-api-rest-v2)
- [EP-005: Búsqueda Semántica + Graph](#ep-005-búsqueda-semántica--graph)
- [EP-006: Gobernanza y Trazabilidad](#ep-006-gobernanza-y-trazabilidad)
- [EP-009: Frontend Multi-Dominio](#ep-009-frontend-multi-dominio)
- [Fuera del MVP (Should/Could)](#fuera-del-mvp-shouldcould)
- [Tabla Resumen](#tabla-resumen)
- [Glosario](#glosario)

---

## Introducción

Este documento contiene las historias de usuario para **Abax-Memory v2.0.0 — Motor de Memoria Genérica Multi-Dominio con IA**. Cada historia se deriva de las features definidas en el Mapa de Épicas y Features, de las reglas de negocio de la Visión del Producto, y de la especificación técnica de la propuesta.

### Roles utilizados

Los roles provienen de la Visión del Producto, reinterpretados para el contexto de usuario universal:

| Rol | Descripción | Equivalencia técnica |
|---|---|---|
| **Memory Consumer** | Usuario que busca y consume conocimiento del repositorio para resolver problemas o tomar decisiones. | `api-consumer` |
| **Domain Curator** | Usuario que crea, clasifica, relaciona y mantiene memorias dentro de un dominio. Puede operar con permisos de revisión. | `memory-operator` / `memory-reviewer` |
| **System Operator** | Usuario responsable de la administración del sistema: tenants, depuración, gobierno, monitoreo. | `memory-admin` |
| **Integration Builder** | Desarrollador que integra sistemas externos, agentes o flujos de trabajo con la API v2. | `api-consumer` (developer) |
| **Knowledge Searcher** | Usuario que realiza búsquedas avanzadas, auditorías de conocimiento y análisis de cobertura. | `memory-auditor` |

### Formato

Cada historia sigue el formato estándar con criterios de aceptación en Given/When/Then. La numeración es `HU-XXX.Y.Z` donde `XXX` = número de épica, `Y` = número de feature, `Z` = número secuencial dentro de la feature.

### Alcance

Este documento cubre **exclusivamente las épicas Must** (EP-001 a EP-006, EP-009). Las épicas Should (EP-007, EP-010) y Could (EP-008) se documentan con una nota en la sección _Fuera del MVP_.

---

## EP-001: Motor de Memoria Genérico

### FT-001.01: Ocho Kinds Universales

#### HU-001.01.1: Clasificar una memoria con kind universal

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.01 Ocho Kinds Universales
**Prioridad**: Must

**Como** Domain Curator
**Quiero** crear una memoria y clasificarla con uno de los 8 kinds universales (`fact`, `preference`, `event`, `decision`, `task`, `procedure`, `note`, `entity`)
**Para** que cualquier dominio pueda estructurar su conocimiento usando categorías estándar interoperables.

**Criterios de aceptación**:

- Dado que soy un Domain Curator autenticado, cuando creo una memoria con `kind = "fact"` y contenido válido, entonces el sistema persiste la memoria con `kind = fact` y me retorna `HTTP 201` con el `memoryId` asignado.
- Dado que soy un Domain Curator autenticado, cuando intento crear una memoria con `kind = "bug"` (valor no válido), entonces el sistema rechaza la petición con `HTTP 400` y código de error `VALIDATION_ERROR` indicando que `kind` debe ser uno de los 8 valores permitidos.
- Dado que existen memorias de distintos kinds en el repositorio, cuando un Memory Consumer ejecuta una búsqueda con filtro `kinds: ["fact", "decision"]`, entonces solo se retornan memorias cuyo `kind` sea `fact` o `decision`.

---

### FT-001.02: Ciclo de Vida con Seis Estados

#### HU-001.02.1: Gestionar el ciclo de vida de una memoria

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.02 Ciclo de Vida con Seis Estados
**Prioridad**: Must

**Como** Domain Curator
**Quiero** que una memoria transite por estados de ciclo de vida (`draft`, `pending`, `active`, `archived`, `rejected`, `deleted`) según reglas predefinidas
**Para** controlar la madurez y visibilidad del conocimiento antes de que sea consumido.

**Criterios de aceptación**:

- Dado que existe una memoria en estado `draft`, cuando un Domain Curator solicita revisión, entonces el sistema permite la transición `draft → pending` mediante `POST /memories/{id}/review`.
- Dado que existe una memoria en estado `pending`, cuando un Domain Curator con permisos de revisión la aprueba, entonces el sistema ejecuta la transición `pending → active` y la memoria se vuelve visible en búsquedas por defecto.
- Dado que existe una memoria en estado `active`, cuando se intenta la transición `active → draft`, entonces el sistema la rechaza con `HTTP 422` indicando que esa transición está prohibida por la regla BR-005.
- Dado que existe una memoria en cualquier estado, cuando se ejecuta soft-delete, entonces el sistema permite la transición a `deleted` desde cualquier estado origen.

#### HU-001.02.2: Rechazar una memoria en revisión

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.02 Ciclo de Vida con Seis Estados
**Prioridad**: Must

**Como** Domain Curator con permisos de revisión
**Quiero** rechazar una memoria en estado `pending` y registrar el motivo del rechazo
**Para** que el creador pueda iterar sobre el contenido antes de reenviarla a revisión.

**Criterios de aceptación**:

- Dado que existe una memoria en estado `pending`, cuando un revisor ejecuta `POST /memories/{id}/review` con acción `reject` y un motivo en el campo `reviewComment`, entonces el sistema transiciona la memoria a `rejected`, registra `reviewedBy`, `reviewedAt` y el comentario de rechazo.
- Dado que una memoria está en estado `rejected`, cuando el creador original la consulta, entonces puede ver el motivo del rechazo en el campo `reviewComment` del ciclo de vida.

---

### FT-001.03: Nueve Tipos de Relación Estructurada

#### HU-001.03.1: Crear relaciones tipadas entre memorias

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.03 Nueve Tipos de Relación Estructurada
**Prioridad**: Must

**Como** Domain Curator
**Quiero** establecer relaciones tipadas entre memorias usando cualquiera de los 9 tipos (`related_to`, `depends_on`, `caused_by`, `resolves`, `contradicts`, `supports`, `mentions`, `belongs_to`, `supersedes`)
**Para** construir un grafo de conocimiento navegable que capture la semántica de las conexiones.

**Criterios de aceptación**:

- Dado que existen dos memorias activas `MEM-001` y `MEM-002`, cuando un Domain Curator crea una relación desde `MEM-001` hacia `MEM-002` con `type = "caused_by"`, entonces el sistema persiste la relación con direccionalidad explícita y retorna `HTTP 201` con el `relationId`.
- Dado que existe la memoria `MEM-001`, cuando se intenta crear una relación hacia un `targetId` inexistente, entonces el sistema rechaza con `HTTP 404` y código de error `TARGET_NOT_FOUND`.
- Dado que existe la memoria `MEM-001`, cuando se intenta crear una relación hacia `MEM-003` cuyo `lifecycle.status = deleted`, entonces el sistema rechaza con `HTTP 422` indicando que el target no puede estar en estado `deleted`.
- Dado que existen relaciones de los 9 tipos en el repositorio, cuando se consulta el grafo de una memoria, entonces cada edge muestra su `type` y dirección (dirigida o bidireccional según el tipo).

#### HU-001.03.2: Eliminar relaciones entre memorias

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.03 Nueve Tipos de Relación Estructurada
**Prioridad**: Must

**Como** Domain Curator
**Quiero** eliminar una relación existente entre dos memorias
**Para** corregir conexiones erróneas o reflejar que el vínculo ya no es válido.

**Criterios de aceptación**:

- Dado que existe la relación `REL-001` entre `MEM-001` y `MEM-002`, cuando un Domain Curator ejecuta `DELETE /memories/MEM-001/relations/REL-001`, entonces el sistema elimina la relación y retorna `HTTP 204`.
- Dado que la relación `REL-001` fue eliminada, cuando se consulta el grafo de `MEM-001`, entonces la relación `REL-001` ya no aparece en la respuesta.
- Dado que la relación `REL-001` fue eliminada, cuando un Knowledge Searcher consulta los registros de auditoría, entonces existe un registro que documenta quién eliminó la relación, cuándo y cuál era el vínculo.

---

### FT-001.04: Extracción de Entidades desde Texto

#### HU-001.04.1: Extraer entidades de texto sin persistir

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.04 Extracción de Entidades desde Texto
**Prioridad**: Must

**Como** Domain Curator
**Quiero** enviar un texto libre al endpoint de extracción y recibir las entidades nombradas detectadas
**Para** conocer qué entidades contiene un texto antes de decidir si crear una memoria con ellas.

**Criterios de aceptación**:

- Dado que soy un Domain Curator autenticado, cuando envío `POST /memories/extract` con `{"content": "El servicio Kubernetes en AWS falló tras el despliegue de Jenkins"}` , entonces el sistema retorna un array de entidades detectadas como `["Kubernetes", "AWS", "Jenkins"]` con el tipo de cada entidad (tecnología, plataforma, herramienta).
- Dado que envío un texto sin entidades reconocibles, cuando llamo a `POST /memories/extract`, entonces el sistema retorna un array vacío `[]` con `HTTP 200`.
- Dado que envío un `content` vacío o nulo, cuando llamo a `POST /memories/extract`, entonces el sistema rechaza con `HTTP 400` y código `VALIDATION_ERROR`.

#### HU-001.04.2: Buscar entidades y ver sus memorias vinculadas

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.04 Extracción de Entidades desde Texto
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** buscar entidades por nombre y obtener todas las memorias vinculadas a una entidad específica
**Para** comprender todo el conocimiento del repositorio relacionado con una entidad concreta.

**Criterios de aceptación**:

- Dado que existen 5 memorias que mencionan la entidad `Kubernetes`, cuando busco `GET /entities?q=Kubernetes`, entonces el sistema retorna la entidad `Kubernetes` con su conteo de memorias vinculadas (`memoryCount: 5`).
- Dado que existe la entidad `Kubernetes`, cuando consulto `GET /entities/Kubernetes`, entonces el sistema retorna el detalle de la entidad con la lista de `memoryIds` vinculados y sus respectivos `kind` y `summary`.
- Dado que busco una entidad que no existe en el repositorio, cuando ejecuto `GET /entities?q=NonExistentEntity`, entonces el sistema retorna un array vacío con `HTTP 200`.

---

### FT-001.05: Modelo de Metadatos Extensibles

#### HU-001.05.1: Enriquecer memorias con metadatos de dominio

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.05 Modelo de Metadatos Extensibles
**Prioridad**: Must

**Como** Domain Curator
**Quiero** adjuntar un objeto `metadata` de tipo key-value al crear o actualizar una memoria
**Para** enriquecer el conocimiento con atributos específicos de mi dominio sin modificar el schema del core.

**Criterios de aceptación**:

- Dado que soy un Domain Curator del perfil Ops, cuando creo una memoria con `metadata: {"affectedService": "payment-api", "remediationSteps": "Rollback to v1.4.2", "rootCause": "Null pointer in PaymentProcessor"}`, entonces el sistema persiste la memoria con los metadatos intactos y los retorna en `GET /memories/{id}`.
- Dado que existe una memoria con metadatos, cuando un Memory Consumer la busca por filtros de contenido, entonces los metadatos NO afectan el ranking semántico (solo se almacenan y se retornan).
- Dado que actualizo el `metadata` de una memoria existente mediante `PATCH /memories/{id}`, entonces el sistema reemplaza completamente el objeto `metadata` anterior por el nuevo (no hace merge parcial).

---

### FT-001.06: Modelo de Source Tipado

#### HU-001.06.1: Registrar el origen de una memoria

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.06 Modelo de Source Tipado
**Prioridad**: Must

**Como** Domain Curator
**Quiero** especificar el `source` de cada memoria con su `type` (`conversation`, `document`, `api`, `workflow`, `manual`, `case`) e `id` externo
**Para** mantener trazabilidad completa del origen del conocimiento.

**Criterios de aceptación**:

- Dado que creo una memoria originada en un chat, cuando incluyo `source: {type: "conversation", id: "chat-4423"}`, entonces el sistema persiste el source y lo retorna en las consultas de detalle.
- Dado que creo una memoria sin especificar `source`, entonces el sistema la acepta porque `source` es opcional, dejando el campo `null`.
- Dado que intento crear una memoria con `source.type = "email"` (valor no permitido), entonces el sistema rechaza con `HTTP 400` y código `VALIDATION_ERROR` indicando los 6 valores permitidos.

---

### FT-001.07: Soft-Delete y Purgado

#### HU-001.07.1: Eliminar lógicamente una memoria

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.07 Soft-Delete y Purgado
**Prioridad**: Must

**Como** Domain Curator
**Quiero** ejecutar un soft-delete sobre una memoria que ya no es relevante
**Para** que desaparezca de las búsquedas sin perder su trazabilidad histórica.

**Criterios de aceptación**:

- Dado que existe la memoria `MEM-001` en estado `active`, cuando un Domain Curator ejecuta `DELETE /memories/MEM-001`, entonces el sistema marca `lifecycle.status = deleted`, registra la acción en auditoría, y retorna `HTTP 204`. La memoria NO se elimina físicamente de PostgreSQL ni de Qdrant.
- Dado que `MEM-001` está en estado `deleted`, cuando un Memory Consumer ejecuta una búsqueda sin filtro de `statuses`, entonces `MEM-001` NO aparece en los resultados.
- Dado que `MEM-001` está en estado `deleted`, cuando un System Operator consulta la auditoría, entonces existe un registro con `action = "delete"`, `userId`, `timestamp` y el diff del estado anterior.

---

### FT-001.08: Versionado de Memorias con Supersedes

#### HU-001.08.1: Crear una nueva versión que reemplaza una anterior

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.08 Versionado de Memorias con Supersedes
**Prioridad**: Must

**Como** Domain Curator
**Quiero** crear una nueva memoria que declare que reemplaza (`supersedes`) una versión anterior
**Para** evolucionar el conocimiento sin perder el historial y sin violar la regla que prohíbe `active → draft`.

**Criterios de aceptación**:

- Dado que existe `MEM-001` en estado `active` con un procedimiento desactualizado, cuando creo `MEM-002` y establezco una relación `supersedes` hacia `MEM-001`, entonces el sistema persiste `MEM-002` y la relación. `MEM-001` permanece en `active` (no cambia de estado automáticamente).
- Dado que `MEM-002` supersede a `MEM-001`, cuando un Memory Consumer busca el procedimiento, entonces el re-ranking favorece `MEM-002` sobre `MEM-001` por ser más reciente.
- Dado que `MEM-002` supersede a `MEM-001`, cuando un Domain Curator archiva manualmente `MEM-001` (`active → archived`), entonces `MEM-002` queda como la única versión activa visible por defecto.

---

### FT-001.09: Modelo de Confidence

#### HU-001.09.1: Asignar nivel de confianza a una memoria

**Épica**: EP-001 Motor de Memoria Genérico
**Feature**: FT-001.09 Modelo de Confidence
**Prioridad**: Must

**Como** Domain Curator
**Quiero** establecer un nivel de `confidence` (0.0 a 1.0) al crear una memoria
**Para** que los consumidores puedan filtrar por umbral mínimo de certeza y distinguir hechos verificados de información preliminar.

**Criterios de aceptación**:

- Dado que creo una memoria con `lifecycle.confidence = 0.95`, cuando se persiste, entonces el campo `confidence` se almacena con el valor `0.95`.
- Dado que intento crear una memoria con `lifecycle.confidence = 1.5` (fuera de rango), entonces el sistema rechaza con `HTTP 400` y código `VALIDATION_ERROR` indicando que el rango válido es `[0.0, 1.0]`.
- Dado que existen 20 memorias con distintos niveles de confianza, cuando un Knowledge Searcher busca con filtro `confidence: {gte: 0.9}`, entonces solo se retornan memorias con `confidence >= 0.9`.
- Dado que creo una memoria sin especificar `confidence`, entonces el sistema asigna el valor por defecto según el perfil activo (o `0.5` si no hay perfil).

---

## EP-002: Perfiles de Dominio

### FT-002.01: Mecanismo de Definición de Perfiles

#### HU-002.01.1: Definir un perfil de dominio como configuración

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.01 Mecanismo de Definición de Perfiles
**Prioridad**: Must

**Como** System Operator
**Quiero** definir un perfil de dominio como una configuración (JSON/YAML o registro en base de datos) que especifique kinds recomendados, tags sugeridos, campos de metadatos extra, y reglas de sensibilidad por defecto
**Para** adaptar el motor genérico a distintos verticales sin modificar el core.

**Criterios de aceptación**:

- Dado que soy un System Operator, cuando creo un perfil con `name: "legal"`, `recommendedKinds: ["entity", "decision", "note"]`, `defaultSensitivity: "confidential"`, y `extraMetadataFields: ["clientName", "contractId"]`, entonces el sistema persiste el perfil y lo deja disponible para activación.
- Dado que un perfil existe, cuando un Domain Curator activa ese perfil, entonces la interfaz y la API reflejan los kinds recomendados y los campos de metadatos extra del perfil.
- Dado que intento definir un perfil sin `name`, entonces el sistema rechaza con `HTTP 400` indicando que `name` es obligatorio.

---

### FT-002.02: Herencia del Core Genérico

#### HU-002.02.1: Garantizar que todo perfil hereda la base genérica

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.02 Herencia del Core Genérico
**Prioridad**: Must

**Como** System Operator
**Quiero** que cualquier perfil de dominio herede automáticamente los 8 kinds, 6 estados, 9 tipos de relación y 4 niveles de sensibilidad del core
**Para** que ningún perfil restrinja las capacidades base del motor.

**Criterios de aceptación**:

- Dado que existe el perfil "legal" que define `recommendedKinds: ["entity", "decision", "note"]`, cuando un Domain Curator crea una memoria con `kind = "procedure"` (no recomendado por el perfil), entonces el sistema la acepta porque los 8 kinds del core siempre están disponibles.
- Dado que el perfil "legal" define `defaultSensitivity: "confidential"`, cuando un Domain Curator especifica explícitamente `sensitivity: "public"`, entonces el sistema respeta el valor explícito del usuario, no el default del perfil.
- Dado que el perfil "ops" no menciona el tipo de relación `contradicts`, cuando un Domain Curator crea una relación con `type = "contradicts"`, entonces el sistema la acepta porque los 9 tipos son universales.

---

### FT-002.03: Perfil Ops (IT Operations)

#### HU-002.03.1: Utilizar el perfil Ops para gestionar conocimiento de operaciones IT

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.03 Perfil Ops (IT Operations)
**Prioridad**: Must

**Como** Domain Curator en un equipo de SRE/DevOps
**Quiero** activar el perfil Ops y crear memorias con kinds orientados a operaciones (`event` como incidentes, `procedure` como runbooks) y tags predefinidos (`incident`, `runbook`, `alert`, `maintenance`)
**Para** gestionar incidentes, procedimientos y postmortems con la semántica adecuada a mi dominio.

**Criterios de aceptación**:

- Dado que activé el perfil Ops, cuando creo una memoria sobre un incidente, entonces el sistema me sugiere `kind = "event"` y el tag `incident` como opciones principales, y expone los campos `affectedService`, `remediationSteps` y `rootCause` en `metadata`.
- Dado que creo una memoria en el perfil Ops sin especificar `importance`, entonces el sistema asigna `importance = 0.7` (default del perfil Ops que mapea el concepto de `criticality`).
- Dado que activé el perfil Ops, cuando busco memorias, entonces el selector de kinds muestra `event` y `procedure` como opciones principales, seguidas de las demás.

---

### FT-002.04: Perfil Agent (Conversational Memory)

#### HU-002.04.1: Utilizar el perfil Agent para memoria conversacional

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.04 Perfil Agent (Conversational Memory)
**Prioridad**: Must

**Como** Integration Builder que desarrolla un agente IA conversacional
**Quiero** activar el perfil Agent y crear memorias con kinds conversacionales (`fact`, `preference`, `event`, `decision`) con scoping intensivo por `userId` y `sessionId`
**Para** que el agente recuerde hechos sobre el usuario, sus preferencias y decisiones previas en el contexto correcto de sesión.

**Criterios de aceptación**:

- Dado que activé el perfil Agent, cuando creo una memoria con `kind = "preference"`, `scope.userId = "user-42"`, y `scope.sessionId = "sess-abc"`, entonces el sistema la persiste correctamente y permite recuperarla filtrando por `userId` y `sessionId`.
- Dado que existen 50 memorias del usuario `user-42` en distintas sesiones, cuando busco con `scope.userId = "user-42"`, entonces el sistema retorna solo las memorias de ese usuario, ordenadas por `importance` descendente para priorizar las más relevantes en la ventana de contexto del agente.
- Dado que el perfil Agent está activo, cuando creo una memoria sin especificar `scope.sessionId`, entonces el sistema la acepta porque `sessionId` es recomendado pero no obligatorio.

---

### FT-002.05: Perfil Business (CRM/Legal/Finanzas)

#### HU-002.05.1: Utilizar el perfil Business para conocimiento corporativo

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.05 Perfil Business (CRM/Legal/Finanzas)
**Prioridad**: Must

**Como** Domain Curator en un entorno corporativo (CRM, legal, finanzas)
**Quiero** activar el perfil Business y usar kinds como `entity` para clientes/empresas, `decision` para acuerdos/contratos, y `note` para minutas de reunión
**Para** gestionar relaciones con clientes, contratos y compromisos con la semántica corporativa.

**Criterios de aceptación**:

- Dado que activé el perfil Business, cuando creo una memoria con `kind = "entity"` y `metadata: {clientName: "Acme Corp", contractId: "CTR-2026-001"}`, entonces el sistema me sugiere los campos de metadatos `clientName`, `contractId` y `opportunityValue`.
- Dado que creo una memoria sobre una reunión con `kind = "note"`, cuando establezco una relación `belongs_to` hacia la entidad del cliente, entonces el grafo refleja que la nota de reunión pertenece al cliente `Acme Corp`.
- Dado que activé el perfil Business, cuando creo una memoria sin especificar `sensitivity`, entonces el sistema asigna `sensitivity = "internal"` (default del perfil Business).

---

### FT-002.06: Reglas de Sensibilidad por Defecto según Perfil

#### HU-002.06.1: Aplicación automática de defaults según perfil

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.06 Reglas de Sensibilidad por Defecto según Perfil
**Prioridad**: Must

**Como** Domain Curator
**Quiero** que al crear una memoria sin especificar `lifecycle.importance` o `lifecycle.sensitivity`, el sistema aplique los valores por defecto del perfil de dominio activo
**Para** no tener que configurar manualmente estos valores en cada creación y asegurar consistencia dentro del dominio.

**Criterios de aceptación**:

- Dado que el perfil Ops está activo y define `defaultImportance: 0.7` y `defaultSensitivity: "internal"`, cuando creo una memoria sin especificar `importance` ni `sensitivity`, entonces la memoria se crea con `importance = 0.7` y `sensitivity = "internal"`.
- Dado que NO hay un perfil activo (core genérico puro), cuando creo una memoria sin especificar `importance` ni `sensitivity`, entonces la memoria se crea con `importance = 0.5` y `sensitivity = "internal"` (BR-009).
- Dado que el perfil Business está activo con `defaultSensitivity: "internal"`, cuando creo una memoria especificando explícitamente `sensitivity: "public"`, entonces el sistema respeta `public` y no aplica el default del perfil.

---

### FT-002.07: Vocabulario Controlado por Perfil

#### HU-002.07.1: Usar tags y topics sugeridos por el perfil

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.07 Vocabulario Controlado por Perfil
**Prioridad**: Must

**Como** Domain Curator
**Quiero** que el perfil de dominio activo me sugiera tags y topics predefinidos al clasificar una memoria
**Para** mantener consistencia en la clasificación y facilitar búsquedas posteriores dentro del dominio.

**Criterios de aceptación**:

- Dado que el perfil Ops está activo, cuando abro el formulario de creación, entonces el sistema me sugiere tags como `incident`, `runbook`, `alert`, `maintenance`, `postmortem`.
- Dado que el perfil Agent está activo, cuando clasifico una memoria, entonces el sistema me sugiere topics como `user-preference`, `user-fact`, `session-context`, `decision-history`.
- Dado que uso un tag sugerido por el perfil, cuando busco memorias filtrando por ese tag, entonces el sistema retorna todas las memorias clasificadas con ese tag, independientemente del perfil activo al momento de la búsqueda.

---

### FT-002.08: Extensibilidad para Nuevos Perfiles

#### HU-002.08.1: Agregar un nuevo perfil sin modificar el core

**Épica**: EP-002 Perfiles de Dominio
**Feature**: FT-002.08 Extensibilidad para Nuevos Perfiles
**Prioridad**: Must

**Como** System Operator
**Quiero** agregar un nuevo perfil de dominio (ej. "healthcare") como una configuración adicional sin modificar el código del core ni la API base
**Para** que el producto pueda expandirse a nuevos verticales sin releases de la plataforma.

**Criterios de aceptación**:

- Dado que soy un System Operator, cuando defino un nuevo perfil `healthcare` mediante una configuración JSON con `recommendedKinds: ["entity", "procedure", "decision"]` y `extraMetadataFields: ["patientId", "diagnosisCode"]`, entonces el sistema lo registra y queda disponible para los usuarios sin requerir un deploy.
- Dado que el nuevo perfil `healthcare` existe, cuando un Domain Curator lo activa, entonces la API base (`/api/v2/memories`) funciona exactamente igual: no hay nuevos endpoints ni cambios en los existentes.
- Dado que el perfil `healthcare` solo define 3 kinds recomendados, cuando un Domain Curator crea una memoria con `kind = "fact"`, entonces el sistema lo acepta porque el core no restringe los kinds por perfil.

---

## EP-003: Scoping Multi-Tenant

### FT-003.01: Aislamiento por tenantId

#### HU-003.01.1: Garantizar que un tenant no accede a datos de otro

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.01 Aislamiento por tenantId
**Prioridad**: Must

**Como** System Operator
**Quiero** que cada tenant tenga sus datos completamente aislados y que un usuario del tenant A nunca pueda ver, modificar ni buscar memorias del tenant B
**Para** garantizar la seguridad y privacidad de los datos en un despliegue multi-tenant.

**Criterios de aceptación**:

- Dado que existen memorias en el tenant `tenant-A` y en el tenant `tenant-B`, cuando un usuario autenticado con token que porta `tenantId = "tenant-A"` ejecuta una búsqueda sin filtros adicionales, entonces el sistema retorna ÚNICAMENTE memorias de `tenant-A`.
- Dado que un usuario de `tenant-A` conoce el `memoryId` de una memoria de `tenant-B`, cuando intenta acceder a `GET /memories/{id-de-tenant-B}`, entonces el sistema retorna `HTTP 404` (no revela la existencia de la memoria).
- Dado que existen 10,000 memorias en 3 tenants distintos, cuando ejecuto pruebas de aislamiento con tokens de cada tenant, entonces el 100% de las queries cross-tenant retornan 0 resultados del tenant equivocado.

---

### FT-003.02: Scoping por userId

#### HU-003.02.1: Acotar memorias a un usuario específico

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.02 Scoping por userId
**Prioridad**: Must

**Como** Domain Curator
**Quiero** asociar memorias a un `scope.userId` y poder buscar por usuario
**Para** aislar conocimiento personal (preferencias, decisiones individuales) dentro de un tenant.

**Criterios de aceptación**:

- Dado que creo una memoria con `scope.userId = "user-42"`, cuando busco con filtro `scopes.userId = "user-42"`, entonces el sistema retorna todas las memorias de ese usuario.
- Dado que creo una memoria sin `scope.userId`, cuando busco con filtro `scopes.userId = "user-42"`, entonces esa memoria NO aparece en los resultados (solo se retornan las que tienen `userId` explícito).
- Dado que el token del usuario porta `userId = "user-42"`, cuando crea una memoria sin especificar `scope.userId`, entonces el sistema puede opcionalmente inferir `userId` del token para trazabilidad.

---

### FT-003.03: Scoping por sessionId

#### HU-003.03.1: Aislar contexto de sesión conversacional

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.03 Scoping por sessionId
**Prioridad**: Must

**Como** Integration Builder que desarrolla un agente con múltiples sesiones simultáneas
**Quiero** asociar memorias a un `scope.sessionId` y recuperar solo las de la sesión activa
**Para** que el agente no mezcle contextos de distintas conversaciones simultáneas.

**Criterios de aceptación**:

- Dado que un agente crea 10 memorias en la sesión `sess-abc` y 5 en `sess-xyz`, cuando busca con `scope.sessionId = "sess-abc"`, entonces solo retorna las 10 memorias de `sess-abc`.
- Dado que creo una memoria con `scope.sessionId = "sess-abc"`, cuando esa sesión finaliza, entonces las memorias permanecen en el repositorio y pueden recuperarse posteriormente por `sessionId` o por otros criterios (userId, búsqueda semántica).
- Dado que especifico `scope.sessionId` al crear una memoria, cuando la busco más tarde, entonces el `sessionId` se preserva exactamente como fue registrado.

---

### FT-003.04: Namespace como Subdivisión Adicional

#### HU-003.04.1: Organizar memorias por namespace dentro de un tenant

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.04 Namespace como Subdivisión Adicional
**Prioridad**: Must

**Como** Domain Curator en una organización grande
**Quiero** subdividir las memorias de mi tenant usando `scope.namespace` (ej. por proyecto, departamento o cliente final)
**Para** organizar el conocimiento sin crear tenants separados para cada unidad de negocio.

**Criterios de aceptación**:

- Dado que creo memorias con `scope.namespace = "project-alpha"`, cuando busco con filtro `scopes.namespace = "project-alpha"`, entonces solo se retornan memorias de ese namespace.
- Dado que creo memorias sin `namespace`, cuando busco sin filtro de namespace, entonces todas las memorias del tenant son elegibles, independientemente de si tienen namespace o no.
- Dado que `namespace` es un campo de texto libre, cuando creo memorias con `namespace = "finance-Q1"` y `namespace = "finance-Q2"`, entonces puedo buscar independientemente por cada uno.

---

### FT-003.05: Cross-Tenant Access para memory-admin

#### HU-003.05.1: Consultar y operar cross-tenant como administrador

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.05 Cross-Tenant Access para memory-admin
**Prioridad**: Must

**Como** System Operator
**Quiero** consultar y operar sobre múltiples tenants simultáneamente para tareas de depuración y gobierno global
**Para** mantener la calidad del repositorio sin tener que autenticarme tenant por tenant.

**Criterios de aceptación**:

- Dado que soy un System Operator con permisos cross-tenant, cuando ejecuto una búsqueda sin restricción de `tenantId`, entonces el sistema retorna memorias de todos los tenants (o de un subconjunto especificado).
- Dado que realizo una operación cross-tenant (archivar, fusionar, soft-delete), entonces el sistema registra en auditoría que la operación fue cross-tenant, incluyendo los `tenantIds` afectados.
- Dado que un usuario sin rol `memory-admin` intenta una búsqueda cross-tenant, entonces el sistema ignora cualquier intento de ampliar el alcance del token y solo retorna resultados de su propio tenant.

---

### FT-003.06: Scope Obligatorio en Escritura

#### HU-003.06.1: Validar que toda memoria nueva tiene scope con tenantId

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.06 Scope Obligatorio en Escritura
**Prioridad**: Must

**Como** System Operator
**Quiero** que el sistema rechace cualquier intento de crear una memoria sin `scope.tenantId`
**Para** garantizar que ninguna memoria quede sin tenant asignado, lo que rompería el aislamiento.

**Criterios de aceptación**:

- Dado que intento crear una memoria sin el campo `scope`, entonces el sistema rechaza con `HTTP 400` y código `VALIDATION_ERROR` indicando que `scope` es obligatorio.
- Dado que intento crear una memoria con `scope: {}` (objeto vacío, sin `tenantId`), entonces el sistema rechaza con `HTTP 400` indicando que `scope.tenantId` es obligatorio (BR-003).
- Dado que creo una memoria con `scope: {tenantId: "tenant-A"}` (mínimo requerido), entonces el sistema acepta la creación y persiste la memoria correctamente.

---

### FT-003.07: Filtrado Automático en Lectura

#### HU-003.07.1: Filtrar automáticamente resultados por el tenant del token

**Épica**: EP-003 Scoping Multi-Tenant
**Feature**: FT-003.07 Filtrado Automático en Lectura
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** que todas mis consultas de lectura se filtren automáticamente por mi `tenantId`
**Para** no tener que especificar manualmente mi tenant en cada request y evitar fugas accidentales de datos.

**Criterios de aceptación**:

- Dado que mi token porta `tenantId = "tenant-A"`, cuando ejecuto `GET /memories/{id}` sin ningún filtro, entonces el sistema automáticamente restringe la consulta a `tenant-A` (BR-004).
- Dado que mi token porta `tenantId = "tenant-A"`, cuando intento especificar `scopes.tenantId = "tenant-B"` en un search, entonces el sistema ignora la ampliación y solo busca en `tenant-A`, o rechaza con error si el filtro explícito intenta ampliar el scope del token.
- Dado que mi token porta `tenantId = "tenant-A"`, cuando especifico `scopes.tenantId = "tenant-A"` en un search, entonces el sistema acepta porque el filtro explícito coincide con el del token.

---

## EP-004: API REST v2

### FT-004.01: CRUD de Memorias

#### HU-004.01.1: Crear una memoria vía API v2

**Épica**: EP-004 API REST v2
**Feature**: FT-004.01 CRUD de Memorias
**Prioridad**: Must

**Como** Integration Builder
**Quiero** crear una memoria mediante `POST /api/v2/memories` con el payload JSON del modelo v2
**Para** integrar mi aplicación o agente con el motor de memoria de forma programática.

**Criterios de aceptación**:

- Dado que envío un payload JSON válido con `kind`, `content`, `scope.tenantId`, y resto de campos opcionales, cuando ejecuto `POST /api/v2/memories`, entonces el sistema crea la memoria, genera embedding, indexa en Qdrant, y retorna `HTTP 201` con el `memoryId` asignado y el embedding generado.
- Dado que envío `POST /api/v2/memories` con `content` vacío, entonces el sistema rechaza con `HTTP 400` porque `content` es obligatorio.
- Dado que envío un payload con un campo desconocido (ej. `color`), entonces el sistema rechaza con `HTTP 400` y código `VALIDATION_ERROR` indicando que el campo no es reconocido por el schema.

#### HU-004.01.2: Consultar detalle de una memoria

**Épica**: EP-004 API REST v2
**Feature**: FT-004.01 CRUD de Memorias
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** consultar el detalle completo de una memoria mediante `GET /api/v2/memories/{id}`
**Para** ver todo el contenido, metadatos, relaciones y ciclo de vida de una memoria específica.

**Criterios de aceptación**:

- Dado que `MEM-001` existe y está en estado `active` en mi tenant, cuando ejecuto `GET /api/v2/memories/MEM-001`, entonces el sistema retorna `HTTP 200` con el objeto completo de la memoria incluyendo `kind`, `content`, `summary`, `topics`, `entities`, `relations`, `metadata`, `source`, `scope`, `lifecycle`, `createdAt` y `updatedAt`.
- Dado que `MEM-001` no existe, cuando ejecuto `GET /api/v2/memories/MEM-999`, entonces el sistema retorna `HTTP 404`.
- Dado que `MEM-001` existe en otro tenant, cuando ejecuto `GET /api/v2/memories/MEM-001` desde mi tenant, entonces el sistema retorna `HTTP 404` (sin revelar que existe en otro tenant).

#### HU-004.01.3: Actualizar contenido y metadatos de una memoria

**Épica**: EP-004 API REST v2
**Feature**: FT-004.01 CRUD de Memorias
**Prioridad**: Must

**Como** Domain Curator
**Quiero** actualizar el `content`, `summary`, `topics` o `metadata` de una memoria existente mediante `PATCH /api/v2/memories/{id}`
**Para** corregir, enriquecer o refinar el conocimiento sin crear una nueva memoria.

**Criterios de aceptación**:

- Dado que `MEM-001` existe, cuando ejecuto `PATCH /api/v2/memories/MEM-001` con un nuevo `content`, entonces el sistema actualiza el contenido, regenera el embedding, reindexa en Qdrant, registra la auditoría con el diff, y retorna el objeto actualizado.
- Dado que `MEM-001` existe, cuando intento cambiar su `kind` mediante `PATCH`, entonces el sistema rechaza el cambio porque `kind` es inmutable una vez creada la memoria.
- Dado que actualizo solo `metadata` (sin cambiar `content`), cuando ejecuto `PATCH`, entonces el sistema NO regenera el embedding porque el contenido semántico no cambió.

---

### FT-004.02: Gestión de Relaciones

#### HU-004.02.1: API para crear y eliminar relaciones

**Épica**: EP-004 API REST v2
**Feature**: FT-004.02 Gestión de Relaciones
**Prioridad**: Must

**Como** Integration Builder
**Quiero** crear relaciones tipadas mediante `POST /api/v2/memories/{id}/relations` y eliminarlas mediante `DELETE /api/v2/memories/{id}/relations/{relId}`
**Para** construir y mantener el grafo de conocimiento desde aplicaciones externas.

**Criterios de aceptación**:

- Dado que `MEM-001` y `MEM-002` existen y están activas, cuando ejecuto `POST /api/v2/memories/MEM-001/relations` con `targetId: "MEM-002"` y `type: "depends_on"`, entonces el sistema crea la relación dirigida y retorna `HTTP 201`.
- Dado que la relación `REL-001` existe, cuando ejecuto `DELETE /api/v2/memories/MEM-001/relations/REL-001`, entonces el sistema elimina la relación y registra el cambio en auditoría (FT-006.08).
- Dado que intento crear una relación con `type = "colaborates_with"` (valor no permitido), entonces el sistema rechaza con `HTTP 400` indicando los 9 tipos válidos.

---

### FT-004.03: Expansión de Grafo

#### HU-004.03.1: Expandir el subgrafo alrededor de una memoria

**Épica**: EP-004 API REST v2
**Feature**: FT-004.03 Expansión de Grafo
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** consultar `GET /api/v2/memories/{id}/graph?depth=2` y recibir el subgrafo de relaciones alrededor de una memoria
**Para** navegar el contexto de conocimiento conectado sin múltiples llamadas individuales.

**Criterios de aceptación**:

- Dado que `MEM-001` tiene 3 relaciones directas, y cada vecino tiene 2 relaciones adicionales, cuando ejecuto `GET /api/v2/memories/MEM-001/graph?depth=2`, entonces el sistema retorna `MEM-001`, sus 3 vecinos directos (depth=1), y los vecinos de esos vecinos (depth=2), con todos los edges intermedios.
- Dado que ejecuto `GET /api/v2/memories/MEM-001/graph?depth=1&includeKinds=entity`, entonces el sistema retorna `MEM-001` y solo los vecinos directos cuyo `kind` sea `entity`.
- Dado que `MEM-001` no tiene relaciones, cuando ejecuto `GET /api/v2/memories/MEM-001/graph`, entonces el sistema retorna solo `MEM-001` con un array de relaciones vacío.

---

### FT-004.04: Revisión de Estados

#### HU-004.04.1: Ejecutar acciones de revisión vía API

**Épica**: EP-004 API REST v2
**Feature**: FT-004.04 Revisión de Estados
**Prioridad**: Must

**Como** Domain Curator con permisos de revisión
**Quiero** ejecutar `POST /api/v2/memories/{id}/review` con acciones `approve`, `reject` o `archive`
**Para** controlar el ciclo de vida de las memorias mediante la API sin depender del frontend.

**Criterios de aceptación**:

- Dado que `MEM-001` está en `pending` y soy un revisor autorizado, cuando ejecuto `POST /api/v2/memories/MEM-001/review` con `action: "approve"`, entonces el sistema transiciona a `active`, registra `reviewedBy` y `reviewedAt`, y retorna `HTTP 200`.
- Dado que `MEM-001` está en `active` y soy un revisor, cuando ejecuto `POST /api/v2/memories/MEM-001/review` con `action: "archive"`, entonces el sistema transiciona a `archived`.
- Dado que `MEM-001` está en `draft` y ejecuto `review` con `action: "approve"`, entonces el sistema rechaza porque la transición `draft → active` no es válida (debe pasar por `pending` primero).
- Dado que un Memory Consumer sin permisos de revisión intenta ejecutar review, entonces el sistema rechaza con `HTTP 403`.

---

### FT-004.05: Búsqueda y Detalle de Entidades

#### HU-004.05.1: API de búsqueda y consulta de entidades

**Épica**: EP-004 API REST v2
**Feature**: FT-004.05 Búsqueda y Detalle de Entidades
**Prioridad**: Must

**Como** Integration Builder
**Quiero** buscar entidades por nombre mediante `GET /api/v2/entities?q=...` y obtener el detalle de una entidad con `GET /api/v2/entities/{name}`
**Para** construir funcionalidades de exploración de entidades en aplicaciones consumidoras.

**Criterios de aceptación**:

- Dado que existen entidades `Kubernetes`, `Docker` y `AWS` en el repositorio, cuando busco `GET /api/v2/entities?q=Kube`, entonces el sistema retorna `Kubernetes` (coincidencia parcial).
- Dado que existe la entidad `AWS` vinculada a 15 memorias, cuando ejecuto `GET /api/v2/entities/AWS`, entonces el sistema retorna la entidad con `memoryCount: 15` y la lista de `memoryIds` vinculados.
- Dado que busco una entidad inexistente, cuando ejecuto `GET /api/v2/entities?q=NonExistent`, entonces el sistema retorna `HTTP 200` con array vacío.

---

### FT-004.06: Estadísticas por Tenant

#### HU-004.06.1: Consultar métricas agregadas de un tenant

**Épica**: EP-004 API REST v2
**Feature**: FT-004.06 Estadísticas por Tenant
**Prioridad**: Must

**Como** System Operator
**Quiero** consultar `GET /api/v2/scopes/{tenantId}/stats` para obtener métricas agregadas del tenant
**Para** monitorear la salud del repositorio, el crecimiento y la distribución del conocimiento.

**Criterios de aceptación**:

- Dado que `tenant-A` tiene 500 memorias, cuando ejecuto `GET /api/v2/scopes/tenant-A/stats`, entonces el sistema retorna: `totalMemories: 500`, distribución por `kind`, distribución por `status`, distribución por `sensitivity`, tasa de revisión, y crecimiento en el tiempo.
- Dado que un Memory Consumer sin rol `memory-admin` o `memory-auditor` intenta acceder a stats, entonces el sistema rechaza con `HTTP 403`.
- Dado que `tenant-A` no tiene memorias, cuando ejecuto stats, entonces el sistema retorna `totalMemories: 0` y distribuciones vacías (no error).

---

### FT-004.07: Health Check y Métricas Operativas

#### HU-004.07.1: Verificar disponibilidad del sistema

**Épica**: EP-004 API REST v2
**Feature**: FT-004.07 Health Check y Métricas Operativas
**Prioridad**: Must

**Como** System Operator
**Quiero** consultar `GET /api/v2/health` para verificar que todos los servicios dependientes (Qdrant, PostgreSQL, OpenAI) están disponibles
**Para** monitorear la salud operativa del sistema y detectar degradaciones tempranamente.

**Criterios de aceptación**:

- Dado que todos los servicios están operativos, cuando ejecuto `GET /api/v2/health`, entonces el sistema retorna `HTTP 200` con `status: "healthy"` y el detalle de cada dependencia: `qdrant: {status: "up"}`, `postgresql: {status: "up"}`, `openai: {status: "up"}`.
- Dado que Qdrant está inaccesible, cuando ejecuto `GET /api/v2/health`, entonces el sistema retorna `HTTP 503` con `status: "unhealthy"` y `qdrant: {status: "down", error: "..."}`.
- Dado que consulto `GET /api/v2/metrics`, entonces el sistema retorna métricas en formato Prometheus con latencia p95, throughput, tasas de error y uso de recursos.

---

### FT-004.08: English-Only en Identificadores de API

#### HU-004.08.1: Garantizar que todos los identificadores de API están en inglés

**Épica**: EP-004 API REST v2
**Feature**: FT-004.08 English-Only en Identificadores de API
**Prioridad**: Must

**Como** Integration Builder
**Quiero** que todos los paths, query params, enums, códigos de error y nombres de campos de la API estén en inglés estándar
**Para** integrar el motor de memoria con herramientas y SDKs internacionales sin fricción por diferencias de idioma.

**Criterios de aceptación**:

- Dado que la especificación OpenAPI está publicada en `/api/v2/openapi.json`, cuando la reviso, entonces el 100% de los paths (ej. `/memories`, `/entities`, `/review`), enums (ej. `fact`, `active`, `related_to`) y códigos de error (ej. `VALIDATION_ERROR`, `TARGET_NOT_FOUND`) están en inglés.
- Dado que existe una memoria creada, cuando consulto `GET /api/v2/memories/{id}`, entonces la respuesta contiene campos como `kind`, `lifecycle`, `scope`, `confidence` — nunca `tipo`, `cicloVida`, `alcance`, `confianza`.
- Dado que se produce un error de validación, cuando la API retorna `HTTP 400`, entonces el `errorCode` es `VALIDATION_ERROR` (no `ERROR_VALIDACION`).

---

### FT-004.09: Documentación OpenAPI 3.x

#### HU-004.09.1: Acceder a la especificación OpenAPI completa

**Épica**: EP-004 API REST v2
**Feature**: FT-004.09 Documentación OpenAPI 3.x
**Prioridad**: Must

**Como** Integration Builder
**Quiero** acceder a `/api/v2/openapi.json` y obtener la especificación OpenAPI 3.x completa de todos los endpoints
**Para** generar clientes automáticamente, validar requests, y entender el contrato de la API sin leer documentación separada.

**Criterios de aceptación**:

- Dado que el servidor está corriendo, cuando accedo a `GET /api/v2/openapi.json`, entonces el sistema retorna un JSON válido según el estándar OpenAPI 3.x con todos los endpoints, schemas de request/response, códigos de error, y ejemplos.
- Dado que reviso la especificación, entonces cada endpoint documenta: método HTTP, path, parámetros requeridos, body schema, response schema para éxitos y errores, y autenticación requerida (Bearer JWT).
- Dado que se modifica un endpoint, cuando accedo a `openapi.json`, entonces la especificación refleja los cambios en tiempo real (no es un archivo estático desactualizado).

---

### FT-004.10: Autenticación y Autorización OIDC/Keycloak

#### HU-004.10.1: Autenticar todas las requests con Bearer token JWT

**Épica**: EP-004 API REST v2
**Feature**: FT-004.10 Autenticación y Autorización OIDC/Keycloak
**Prioridad**: Must

**Como** System Operator
**Quiero** que toda request a `/api/v2/` requiera un Bearer token JWT válido emitido por Keycloak (o el proveedor OIDC configurado)
**Para** garantizar que solo usuarios autenticados accedan al motor de memoria.

**Criterios de aceptación**:

- Dado que un usuario se autentica contra Keycloak y obtiene un JWT, cuando ejecuta cualquier endpoint de `/api/v2/` con el header `Authorization: Bearer <token>`, entonces el sistema valida la firma, expiración y claims del token, y permite el acceso según los roles.
- Dado que una request llega sin header `Authorization`, cuando intenta acceder a cualquier endpoint, entonces el sistema retorna `HTTP 401` con código `UNAUTHORIZED`.
- Dado que un token expiró, cuando se usa en una request, entonces el sistema retorna `HTTP 401` indicando que el token expiró.
- Dado que un usuario tiene rol `api-consumer`, cuando intenta crear una memoria, entonces el sistema retorna `HTTP 403` porque `api-consumer` solo tiene permisos de lectura.

---

### FT-004.11: Estándares de Códigos de Error HTTP y Cuerpos de Error

#### HU-004.11.1: Recibir errores estandarizados y machine-readable

**Épica**: EP-004 API REST v2
**Feature**: FT-004.11 Estándares de Códigos de Error HTTP y Cuerpos de Error
**Prioridad**: Must

**Como** Integration Builder
**Quiero** que todos los errores de la API sigan un formato estándar con código HTTP apropiado, `errorCode` machine-readable y `message` human-readable
**Para** que mi aplicación pueda manejar errores programáticamente sin parsear mensajes de texto libre.

**Criterios de aceptación**:

- Dado que envío un JSON malformado, cuando la API responde, entonces retorna `HTTP 400` con `errorCode: "INVALID_JSON"` y `message` descriptivo.
- Dado que excedo el límite de rate, cuando la API responde, entonces retorna `HTTP 429` con `errorCode: "RATE_LIMIT_EXCEEDED"`.
- Dado que la base de datos no está disponible, cuando la API responde, entonces retorna `HTTP 503` con `errorCode: "DATABASE_UNAVAILABLE"`.
- Dado que cualquier error ocurre, cuando la API responde, entonces el cuerpo incluye siempre `errorCode`, `message`, y opcionalmente `details` con información adicional (ej. qué campo falló la validación).

---

### FT-004.12: Validación de Request Bodies

#### HU-004.12.1: Validación estricta de payloads JSON

**Épica**: EP-004 API REST v2
**Feature**: FT-004.12 Validación de Request Bodies
**Prioridad**: Must

**Como** Integration Builder
**Quiero** que la API valide estrictamente los payloads JSON contra el schema del modelo, rechazando campos desconocidos y valores inválidos
**Para** detectar errores de integración tempranamente, antes de que datos malformados lleguen al core.

**Criterios de aceptación**:

- Dado que envío un campo `kind: "bug"` (valor no válido), cuando la API procesa la request, entonces retorna `HTTP 400` con `errorCode: "VALIDATION_ERROR"` y `details: [{field: "kind", error: "must be one of: fact, preference, event, decision, task, procedure, note, entity"}]`.
- Dado que envío un campo desconocido `priority: "high"`, cuando la API procesa la request, entonces retorna `HTTP 400` indicando que el campo no es reconocido (strict mode).
- Dado que envío `importance: "high"` (string en vez de float), cuando la API procesa la request, entonces retorna `HTTP 400` con `details` indicando que `importance` debe ser un número entre 0.0 y 1.0.

---

### FT-004.13: Rate Limiting por Tenant y Usuario

#### HU-004.13.1: Protección contra abuso mediante rate limiting

**Épica**: EP-004 API REST v2
**Feature**: FT-004.13 Rate Limiting por Tenant y Usuario
**Prioridad**: Should

**Como** System Operator
**Quiero** que el sistema limite la tasa de requests por `tenantId` y `userId`
**Para** proteger los recursos de abusos y garantizar calidad de servicio equitativa entre todos los tenants.

**Criterios de aceptación**:

- Dado que un usuario excede el límite de requests por minuto configurado para su tenant, cuando ejecuta una nueva request, entonces el sistema retorna `HTTP 429` con `Retry-After` header indicando cuándo puede reintentar.
- Dado que dos tenants distintos tienen límites diferentes configurados, cuando cada uno consume requests, entonces el rate limiter aplica el límite específico de cada tenant.
- Dado que un System Operator configura un nuevo límite para un tenant, cuando el tenant consume requests, entonces el nuevo límite se aplica sin requerir reinicio del servicio.

---

## EP-005: Búsqueda Semántica + Graph

### FT-005.01: Búsqueda por Texto Libre con Qdrant + OpenAI Embeddings

#### HU-005.01.1: Buscar memorias por texto libre semántico

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.01 Búsqueda por Texto Libre con Qdrant + OpenAI Embeddings
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** enviar una query en texto libre a `POST /api/v2/memories/search` y recibir las memorias semánticamente más relevantes
**Para** encontrar conocimiento relevante sin necesidad de conocer los términos exactos con que fue registrado.

**Criterios de aceptación**:

- Dado que existen 500 memorias en el repositorio, cuando busco con `query: "¿cómo restaurar la base de datos después de una caída?"`, entonces el sistema retorna las top-K memorias con mayor similitud semántica, cada una con su `score` numérico (0.0 a 1.0) y ordenadas de mayor a menor relevancia.
- Dado que envío una query en español, cuando existen memorias en español que responden semánticamente a la query, entonces el sistema las retorna correctamente (el embedding es multilingüe).
- Dado que envío una query vacía (`query: ""`), entonces el sistema rechaza con `HTTP 400` indicando que `query` es obligatorio.

---

### FT-005.02: Filtros Estructurados Multidimensionales

#### HU-005.02.1: Refinar búsqueda con filtros simultáneos

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.02 Filtros Estructurados Multidimensionales
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** combinar la búsqueda semántica con filtros estructurados en hasta 8 dimensiones (kinds, statuses, topics, entities, importance, confidence, sensitivity, rango de fechas)
**Para** encontrar exactamente el conocimiento que necesito sin ruido de resultados irrelevantes.

**Criterios de aceptación**:

- Dado que busco `query: "despliegue fallido"` con filtros `kinds: ["event"]`, `importance: {gte: 0.7}`, `createdAfter: "2026-01-01"`, cuando ejecuto la búsqueda, entonces los resultados cumplen simultáneamente todos los filtros aplicados.
- Dado que aplico filtros que ningún resultado cumple (ej. `kinds: ["fact"]` y `entities: ["NonExistentEntity"]`), cuando ejecuto la búsqueda, entonces el sistema retorna array vacío (no error).
- Dado que no especifico filtros, cuando ejecuto la búsqueda, entonces solo se aplica el filtro implícito de `statuses: ["active"]` (BR-001) y el `tenantId` del token.

---

### FT-005.03: Expansión de Subgrafo en Resultados

#### HU-005.03.1: Obtener vecinos de cada resultado en la misma búsqueda

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.03 Expansión de Subgrafo en Resultados
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** activar `expandGraph` en mi búsqueda para que cada resultado incluya sus vecinos inmediatos
**Para** navegar el contexto de relaciones sin llamadas adicionales a la API.

**Criterios de aceptación**:

- Dado que busco con `expandGraph: {depth: 1, includeKinds: ["entity"]}`, cuando el sistema retorna resultados, entonces cada memoria incluye un array `relations` con sus vecinos directos que sean de kind `entity`.
- Dado que busco sin `expandGraph`, cuando el sistema retorna resultados, entonces cada memoria solo incluye su información básica sin expandir vecinos (respuesta más ligera).
- Dado que una memoria en los resultados no tiene relaciones, cuando `expandGraph` está activo, entonces la memoria se retorna con `relations: []` (array vacío, sin error).

---

### FT-005.04: Re-Ranking de Resultados

#### HU-005.04.1: Activar re-ranking para mejorar precisión top-K

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.04 Re-Ranking de Resultados
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** activar el parámetro `rerank: true` en mi búsqueda
**Para** que el sistema refine el orden de resultados combinando score semántico con importancia, confianza, frescura y riqueza de relaciones.

**Criterios de aceptación**:

- Dado que busco con `rerank: true`, cuando el sistema retorna resultados, entonces el orden refleja un scoring combinado que considera el score semántico, `importance`, `confidence`, y `updatedAt` — no solo la similitud de coseno cruda.
- Dado que busco con `rerank: false` (o sin el parámetro), cuando el sistema retorna resultados, entonces el orden se basa exclusivamente en el score de similitud semántica.
- Dado que dos memorias tienen score semántico similar pero una tiene `importance = 0.9` y la otra `importance = 0.3`, cuando busco con `rerank: true`, entonces la de mayor importancia aparece primero.

---

### FT-005.05: Multi-Hop Traversal

#### HU-005.05.1: Navegar múltiples saltos de relaciones en una consulta

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.05 Multi-Hop Traversal
**Prioridad**: Should

**Como** Knowledge Searcher
**Quiero** realizar consultas que sigan múltiples saltos de relaciones (ej. "todas las decisiones que dependen de eventos causados por incidentes del servicio X")
**Para** responder preguntas complejas que requieren atravesar el grafo de conocimiento.

**Criterios de aceptación**:

- Dado que configuro `expandGraph` con `depth: 3` y filtros por tipo de relación, cuando ejecuto la búsqueda, entonces el sistema retorna los nodos alcanzables hasta profundidad 3 siguiendo las relaciones que cumplen los filtros.
- Dado que configuro `depth: 10` (valor excesivo), cuando ejecuto la búsqueda, entonces el sistema limita la profundidad a un máximo configurable (ej. 5) para proteger el rendimiento.
- Dado que un nodo intermedio está en estado `deleted`, cuando el traversal lo encuentra, entonces el sistema omite ese nodo y no sigue expandiendo a través de él.

---

### FT-005.06: Top-K Configurable

#### HU-005.06.1: Controlar cuántos resultados retorna la búsqueda

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.06 Top-K Configurable
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** especificar `topK` en mi búsqueda para controlar cuántos resultados recibo
**Para** ajustar el consumo de recursos y la precisión según mi caso de uso (pocos resultados precisos vs. exploración amplia).

**Criterios de aceptación**:

- Dado que especifico `topK: 5`, cuando el sistema retorna resultados, entonces obtengo exactamente 5 memorias (o menos si no hay suficientes que cumplan los filtros).
- Dado que no especifico `topK`, cuando el sistema retorna resultados, entonces aplica el valor por defecto: `topK = 10`.
- Dado que especifico `topK: 500`, cuando el sistema procesa la request, entonces aplica el límite máximo configurado para el tenant (ej. 100) y retorna como máximo ese número.

---

### FT-005.07: Embedding de Nuevas Memorias

#### HU-005.07.1: Generar y almacenar embedding automáticamente al crear o actualizar

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.07 Embedding de Nuevas Memorias
**Prioridad**: Must

**Como** System Operator
**Quiero** que toda memoria creada o cuyo `content` se actualizó dispare automáticamente la generación de un nuevo embedding y su indexación en Qdrant
**Para** que ninguna memoria quede fuera del índice de búsqueda semántica.

**Criterios de aceptación**:

- Dado que creo una memoria con `POST /api/v2/memories`, cuando la respuesta es `HTTP 201`, entonces el embedding vectorial ya fue generado por OpenAI e indexado en Qdrant vinculado al `memoryId`.
- Dado que actualizo solo `metadata` de una memoria (sin cambiar `content`), cuando ejecuto `PATCH`, entonces NO se regenera el embedding porque el contenido semántico no cambió.
- Dado que actualizo el `content` de una memoria, cuando ejecuto `PATCH`, entonces el sistema regenera el embedding, actualiza el vector en Qdrant, y la memoria es inmediatamente buscable con el nuevo contenido.

---

### FT-005.08: Re-Indexación Masiva

#### HU-005.08.1: Regenerar todos los embeddings de un tenant o del repositorio

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.08 Re-Indexación Masiva
**Prioridad**: Should

**Como** System Operator
**Quiero** disparar una re-indexación masiva de todas las memorias de un tenant (o del repositorio completo)
**Para** regenerar embeddings tras un cambio de motor de embeddings, migración de datos, o corrección de un lote de índices corruptos.

**Criterios de aceptación**:

- Dado que soy un System Operator, cuando ejecuto el comando de re-indexación para `tenant-A`, entonces el sistema regenera embeddings para todas las memorias de ese tenant y actualiza Qdrant.
- Dado que la re-indexación está en progreso, cuando un Memory Consumer busca, entonces el sistema sigue respondiendo con los embeddings anteriores hasta que la re-indexación del tenant se complete (no hay downtime).
- Dado que un usuario sin rol `memory-admin` intenta disparar una re-indexación, entonces el sistema rechaza con `HTTP 403`.

---

### FT-005.09: Filtrado por lifecycle.status Gobernado

#### HU-005.09.1: Visibilidad gobernada por estado en búsqueda

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.09 Filtrado por lifecycle.status Gobernado
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** que las búsquedas sin filtro explícito de `statuses` solo me devuelvan memorias en estado `active`
**Para** consumir solo conocimiento aprobado y verificado, sin ver borradores ni contenido rechazado.

**Criterios de aceptación**:

- Dado que existen memorias en `draft`, `pending`, `active`, `archived` y `rejected`, cuando ejecuto una búsqueda sin especificar `statuses`, entonces el sistema solo retorna memorias con `lifecycle.status = active` (BR-001).
- Dado que soy un Domain Curator con permisos de revisión, cuando especifico explícitamente `statuses: ["pending", "active"]`, entonces el sistema retorna memorias en ambos estados.
- Dado que soy un Memory Consumer sin permisos elevados, cuando intento especificar `statuses: ["pending"]`, entonces el sistema rechaza o ignora el filtro de estados no permitidos.
- Dado que una memoria está en estado `deleted`, cuando ejecuto CUALQUIER búsqueda (incluso como admin), entonces NUNCA aparece a menos que use un endpoint administrativo específico.

---

### FT-005.10: Scoring Transparente

#### HU-005.10.1: Ver el score de relevancia en cada resultado

**Épica**: EP-005 Búsqueda Semántica + Graph
**Feature**: FT-005.10 Scoring Transparente
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** que cada resultado de búsqueda incluya un `score` numérico entre 0.0 y 1.0
**Para** entender qué tan relevante es cada resultado y poder auditar la calidad del ranking.

**Criterios de aceptación**:

- Dado que ejecuto una búsqueda, cuando el sistema retorna resultados, entonces cada item incluye el campo `score` con un valor entre 0.0 y 1.0.
- Dado que dos búsquedas idénticas se ejecutan en momentos diferentes sobre los mismos datos, cuando comparo los scores, entonces los valores son reproducibles (mismo query + mismos datos = mismos scores).
- Dado que busco con `rerank: true`, cuando veo los scores, entonces el campo `score` refleja el score post-re-ranking, no el score crudo de Qdrant.

---

## EP-006: Gobernanza y Trazabilidad

### FT-006.01: Auditoría Completa de Mutaciones

#### HU-006.01.1: Registrar toda operación de escritura en el log de auditoría

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.01 Auditoría Completa de Mutaciones
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** que toda mutación (creación, modificación, cambio de estado, soft-delete, relaciones) genere un registro de auditoría inmutable
**Para** poder reconstruir la historia completa de cualquier memoria y demostrar cumplimiento normativo.

**Criterios de aceptación**:

- Dado que un Domain Curator crea una memoria, cuando se completa la operación, entonces existe un registro de auditoría con: `timestamp`, `userId`, `action: "create"`, `memoryId`, `diff` (contenido creado), `ipAddress`, `userAgent`.
- Dado que un revisor aprueba una memoria (`pending → active`), cuando se completa la operación, entonces existe un registro de auditoría con `action: "review_approve"`, `userId` del revisor, y `diff` mostrando el cambio de estado.
- Dado que se ejecutan 100 mutaciones en una hora, cuando un Knowledge Searcher consulta los registros de auditoría, entonces el 100% de las mutaciones tiene su registro correspondiente (Criterio de Éxito CE-09).

#### HU-006.01.2: Consultar el historial de auditoría de una memoria

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.01 Auditoría Completa de Mutaciones
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** consultar el historial completo de cambios de una memoria específica
**Para** auditar quién hizo qué cambios, cuándo y por qué.

**Criterios de aceptación**:

- Dado que `MEM-001` fue creada, modificada 3 veces y finalmente archivada, cuando consulto su historial de auditoría, entonces obtengo 5 registros cronológicos con todos los campos de auditoría.
- Dado que consulto el historial de una memoria que no tiene cambios (solo creación), cuando obtengo los registros, entonces aparece un único registro con `action: "create"`.
- Dado que un Memory Consumer sin rol de auditoría intenta acceder a los logs, entonces el sistema rechaza con `HTTP 403`.

---

### FT-006.02: Flujo de Revisión Humana

#### HU-006.02.1: Completar el workflow de revisión: draft → pending → active

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.02 Flujo de Revisión Humana
**Prioridad**: Must

**Como** Domain Curator (operador) y Domain Curator (revisor)
**Quiero** que una memoria pase por el flujo completo: un operador la crea en `draft`, la envía a `pending`, y un revisor la evalúa hasta `active` o `rejected`
**Para** garantizar que el conocimiento publicado pasó por validación humana cuando corresponde.

**Criterios de aceptación**:

- Dado que un operador crea una memoria en `draft`, cuando la edita y considera que está lista, entonces ejecuta `review` con acción `submit` y la memoria transiciona a `pending`.
- Dado que la memoria está en `pending`, cuando un revisor la evalúa, encuentra el contenido correcto, y ejecuta `review` con `action: "approve"`, entonces transiciona a `active`.
- Dado que la memoria está en `pending`, cuando el revisor encuentra problemas y ejecuta `review` con `action: "reject"` y un comentario, entonces transiciona a `rejected` y el operador puede ver el motivo.
- Dado que la memoria está en `draft`, cuando el operador la edita 5 veces antes de enviarla a revisión, entonces cada edición queda registrada en auditoría pero la memoria permanece en `draft` hasta que se envíe explícitamente.

---

### FT-006.03: Visibilidad Gobernada por Estado

#### HU-006.03.1: Control de visibilidad según estado y rol

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.03 Visibilidad Gobernada por Estado
**Prioridad**: Must

**Como** System Operator
**Quiero** que cada estado de ciclo de vida tenga reglas de visibilidad específicas según el rol del usuario
**Para** que los consumidores solo vean contenido aprobado, los revisores vean contenido pendiente, y los administradores tengan visibilidad completa.

**Criterios de aceptación**:

- Dado que existen memorias en todos los estados, cuando un Memory Consumer busca sin filtro, entonces solo ve `active`.
- Dado que un Domain Curator con rol de revisor busca con filtro `statuses: ["pending"]`, entonces ve memorias `pending` en su scope.
- Dado que un System Operator busca sin restricciones, entonces puede ver memorias en cualquier estado, incluyendo `deleted` mediante un endpoint administrativo.
- Dado que una memoria está `archived`, cuando un Memory Consumer busca, entonces NUNCA aparece (ni siquiera con filtro de `statuses` — los estados `archived`, `rejected`, `deleted` requieren endpoints o permisos administrativos).

---

### FT-006.04: Umbral de Revisión Obligatoria

#### HU-006.04.1: Forzar revisión humana para memorias de alta criticidad

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.04 Umbral de Revisión Obligatoria
**Prioridad**: Must

**Como** System Operator
**Quiero** que las memorias creadas con `importance >= 0.7` Y `sensitivity IN (confidential, secret)` nunca se creen directamente en `active`
**Para** garantizar que el conocimiento crítico o sensible siempre pase por revisión humana antes de publicarse.

**Criterios de aceptación**:

- Dado que un Domain Curator intenta crear una memoria con `importance = 0.8` y `sensitivity = "confidential"` especificando `status = "active"`, entonces el sistema automáticamente asigna `status = "draft"` (BR-006) y advierte que requiere revisión.
- Dado que una memoria fue forzada a `draft` por el umbral, cuando un revisor intenta aprobarla (`pending → active`), entonces el sistema permite la transición porque ya pasó por revisión humana.
- Dado que un System Operator con justificación registrada intenta saltar el umbral, cuando crea la memoria con un flag administrativo, entonces el sistema permite `active` directo pero registra la excepción en auditoría con la justificación.
- Dado que creo una memoria con `importance = 0.6` y `sensitivity = "confidential"`, entonces el sistema permite crearla en `active` porque no se cumple la condición compuesta (ambas condiciones deben ser verdaderas).

---

### FT-006.05: Linaje de Decisiones

#### HU-006.05.1: Trazabilidad de qué memorias influyeron en qué decisiones

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.05 Linaje de Decisiones
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** consultar el linaje de una decisión: qué hechos, eventos y procedimientos la sustentaron
**Para** entender el contexto completo detrás de cada decisión registrada en el sistema.

**Criterios de aceptación**:

- Dado que `MEM-010` es una `decision` vinculada mediante `supports` y `caused_by` a varias memorias, cuando consulto el grafo de `MEM-010` con depth adecuado, entonces puedo identificar todos los hechos y eventos que respaldan o causaron la decisión.
- Dado que `MEM-005` es un `procedure` referenciado por varias decisiones, cuando consulto el grafo inverso (qué memorias apuntan a `MEM-005`), entonces el sistema puede responder "qué decisiones se basaron en este procedimiento".
- Dado que una relación `supersedes` existe entre dos versiones de una decisión, cuando consulto el linaje, entonces la versión más reciente se identifica claramente y se preserva la trazabilidad hacia la versión anterior.

---

### FT-006.06: Depuración de Repositorio

#### HU-006.06.1: Archivar, fusionar y soft-delete como administrador

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.06 Depuración de Repositorio
**Prioridad**: Must

**Como** System Operator
**Quiero** ejecutar operaciones de depuración: archivar memorias obsoletas, fusionar duplicadas, y aplicar soft-delete a contenido inválido
**Para** mantener la calidad, consistencia y relevancia del repositorio a largo plazo.

**Criterios de aceptación**:

- Dado que `MEM-020` contiene información desactualizada, cuando la archivo (`active → archived`), entonces desaparece de las búsquedas por defecto pero permanece trazable en auditoría.
- Dado que `MEM-030` y `MEM-031` son duplicadas, cuando ejecuto una fusión, entonces el sistema consolida el contenido, unifica las relaciones de ambas en la memoria sobreviviente, marca la otra como `deleted`, y registra la fusión en auditoría.
- Dado que `MEM-040` contiene información inválida que no debe ser archivada ni fusionada, cuando ejecuto soft-delete, entonces pasa a `deleted` y se registra el motivo en auditoría.
- Dado que consulto métricas de calidad del repositorio, cuando ejecuto stats, entonces obtengo indicadores como: proporción de drafts huérfanos (>30 días sin modificar), memorias sin relaciones, y tasa de revisión.

---

### FT-006.07: Control de Acceso RBAC con Cinco Roles

#### HU-006.07.1: Asignar y verificar permisos según los 5 roles

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.07 Control de Acceso RBAC con Cinco Roles
**Prioridad**: Must

**Como** System Operator
**Quiero** que Keycloak gestione los 5 roles del sistema y que cada endpoint valide los permisos según el rol del usuario
**Para** garantizar el principio de mínimo privilegio en todas las operaciones.

**Criterios de aceptación**:

- Dado que un usuario tiene rol `api-consumer`, cuando intenta crear una memoria (`POST /memories`), entonces el sistema retorna `HTTP 403`.
- Dado que un usuario tiene rol `memory-operator`, cuando intenta acceder a estadísticas del tenant (`GET /scopes/{tenantId}/stats`), entonces el sistema retorna `HTTP 403` (ese endpoint requiere `memory-admin` o `memory-auditor`).
- Dado que un usuario tiene rol `memory-admin`, cuando intenta acceder a logs de auditoría y ejecutar depuración, entonces el sistema permite ambas operaciones.
- Dado que un usuario tiene rol `memory-auditor`, cuando intenta crear o modificar memorias, entonces el sistema retorna `HTTP 403` (auditor es solo lectura).
- Dado que un usuario tiene rol `memory-reviewer`, cuando intenta aprobar una memoria fuera de su scope, entonces el sistema retorna `HTTP 403`.

---

### FT-006.08: Registro de Cambios en Relaciones

#### HU-006.08.1: Auditar la creación y eliminación de relaciones

**Épica**: EP-006 Gobernanza y Trazabilidad
**Feature**: FT-006.08 Registro de Cambios en Relaciones
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** que la creación y eliminación de relaciones también generen registros de auditoría
**Para** poder reconstruir la evolución completa del grafo de conocimiento.

**Criterios de aceptación**:

- Dado que un Domain Curator crea una relación `caused_by` de `MEM-001` a `MEM-002`, cuando se completa, entonces existe un registro de auditoría con `action: "create_relation"`, `userId`, `sourceId: "MEM-001"`, `targetId: "MEM-002"`, `type: "caused_by"`.
- Dado que un Domain Curator elimina una relación, cuando se completa, entonces existe un registro con `action: "delete_relation"` y los detalles de la relación eliminada.
- Dado que una memoria con relaciones es soft-deleteada, cuando se elimina, entonces el sistema registra en auditoría qué relaciones quedaron huérfanas como consecuencia.

---

## EP-009: Frontend Multi-Dominio

### FT-009.01: Creación de Memorias con Formulario Multi-Dominio

#### HU-009.01.1: Formulario de creación adaptado al perfil activo

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.01 Creación de Memorias con Formulario Multi-Dominio
**Prioridad**: Must

**Como** Domain Curator
**Quiero** que el formulario de creación de memorias en el frontend se adapte automáticamente según el perfil de dominio activo
**Para** ver los kinds relevantes, tags sugeridos y campos de metadatos apropiados para mi dominio sin configuración manual.

**Criterios de aceptación**:

- Dado que tengo el perfil Ops activo, cuando abro el formulario de creación, entonces el selector de `kind` muestra `event` y `procedure` como opciones principales, el campo de tags sugiere `incident`, `runbook`, `alert`, y los campos de metadatos extra (`affectedService`, `remediationSteps`, `rootCause`) aparecen automáticamente.
- Dado que cambio al perfil Agent, cuando abro el formulario, entonces el selector de `kind` muestra `fact`, `preference`, `event`, `decision` como principales, y los campos `scope.userId` y `scope.sessionId` se despliegan prominentemente.
- Dado que no tengo ningún perfil activo, cuando abro el formulario, entonces veo los 8 kinds sin priorización, tags sin sugerencias, y solo los campos base del modelo.

---

### FT-009.02: Búsqueda Avanzada con Filtros Visuales

#### HU-009.02.1: Panel de búsqueda con todos los filtros del modelo

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.02 Búsqueda Avanzada con Filtros Visuales
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** un panel de búsqueda visual que exponga todos los filtros del modelo `SearchRequest`
**Para** refinar búsquedas complejas sin tener que escribir JSON manualmente.

**Criterios de aceptación**:

- Dado que abro el panel de búsqueda, entonces veo: campo de texto libre, selectores de kinds (multi-select), statuses, topics, entities, sliders de rango para `importance` y `confidence`, selector de `sensitivity`, date pickers para `createdAfter`/`createdBefore`, y toggles para `expandGraph` y `rerank`.
- Dado que aplico filtros y ejecuto la búsqueda, cuando se retornan resultados, entonces cada item muestra: `score` (con barra visual), `kind` (con ícono), `status` (con color), `summary`, `topics` (chips), y `entities` (badges).
- Dado que busco sin filtros, cuando se retornan resultados, entonces se aplica el filtro implícito de `statuses: ["active"]` y los resultados lo reflejan.

---

### FT-009.03: Panel de Revisión (Approve/Reject/Archive)

#### HU-009.03.1: Vista dedicada para revisar y decidir sobre memorias pendientes

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.03 Panel de Revisión (Approve/Reject/Archive)
**Prioridad**: Must

**Como** Domain Curator con permisos de revisión
**Quiero** una vista dedicada donde se listen las memorias en `pending` (y opcionalmente `draft`), pueda abrir su detalle, ver historial de cambios, y ejecutar acciones de revisión
**Para** gestionar eficientemente el flujo de aprobación sin navegar entre múltiples pantallas.

**Criterios de aceptación**:

- Dado que soy un revisor, cuando accedo al panel de revisión, entonces veo una lista de memorias `pending` en mi scope, con `kind`, `importance`, `sensitivity`, fecha de envío y solicitante.
- Dado que selecciono una memoria, cuando abro su detalle, entonces veo el contenido completo, historial de cambios, relaciones, y los botones de acción: "Approve", "Reject" (requiere motivo), "Request Changes" (devuelve a draft con comentario).
- Dado que apruebo una memoria, cuando confirmo, entonces la memoria transiciona a `active` y desaparece de mi bandeja de pendientes.
- Dado que rechazo una memoria, cuando escribo el motivo y confirmo, entonces la memoria transiciona a `rejected` y el operador recibe el motivo del rechazo.

---

### FT-009.04: Panel de Administración Multi-Tenant

#### HU-009.04.1: Interfaz de administración para System Operator

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.04 Panel de Administración Multi-Tenant
**Prioridad**: Must

**Como** System Operator
**Quiero** un panel de administración que me permita gestionar tenants, depurar el repositorio (archivar, fusionar, soft-delete), ver auditoría completa y acceder cross-tenant
**Para** gobernar el sistema completo desde una interfaz unificada.

**Criterios de aceptación**:

- Dado que soy un System Operator, cuando accedo al panel de administración, entonces veo la lista de tenants con métricas básicas (total memorias, crecimiento, estado) y puedo crear, configurar o suspender tenants.
- Dado que selecciono la herramienta de depuración, cuando busco memorias duplicadas, entonces el sistema sugiere candidatas a fusión basadas en similitud semántica y el operador puede ejecutar merge.
- Dado que accedo a la vista de auditoría, cuando aplico filtros (por usuario, acción, rango de fechas), entonces veo los registros de auditoría correspondientes con detalle de cada mutación.
- Dado que activo el modo cross-tenant, cuando navego el repositorio, entonces puedo ver y operar sobre memorias de múltiples tenants simultáneamente.

---

### FT-009.05: Visualización de Grafo de Relaciones

#### HU-009.05.1: Componente interactivo de visualización de grafo

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.05 Visualización de Grafo de Relaciones
**Prioridad**: Must

**Como** Knowledge Searcher
**Quiero** un componente visual interactivo que renderice el subgrafo de relaciones alrededor de una memoria
**Para** explorar visualmente las conexiones de conocimiento y navegar el grafo con clics.

**Criterios de aceptación**:

- Dado que abro el detalle de una memoria con 5 relaciones, cuando activo la vista de grafo, entonces veo nodos (tarjetas con `kind`, `status` codificado por color, y `summary` truncado) y edges (líneas con etiqueta del tipo de relación y dirección).
- Dado que hago clic en un nodo vecino, cuando el grafo se actualiza, entonces ese nodo se expande mostrando sus propias relaciones (navegación progresiva).
- Dado que hago clic en un edge, cuando se muestra el detalle, entonces veo el tipo de relación, dirección, fecha de creación y las memorias conectadas.
- Dado que una memoria tiene 20 relaciones, cuando el grafo se renderiza, entonces el layout es legible con zoom y paneo, y los nodos no se solapan excesivamente.

---

### FT-009.06: Selección y Cambio de Perfil de Dominio

#### HU-009.06.1: Cambiar de perfil y ver la interfaz reconfigurarse

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.06 Selección y Cambio de Perfil de Dominio
**Prioridad**: Must

**Como** Domain Curator
**Quiero** seleccionar un perfil de dominio (Ops, Agent, Business) desde un selector y que la interfaz se reconfigure dinámicamente sin recargar la página
**Para** trabajar en múltiples dominios durante la misma sesión sin fricción.

**Criterios de aceptación**:

- Dado que estoy en el perfil Ops, cuando cambio al perfil Agent mediante el selector, entonces el formulario de creación, los tags sugeridos, los kinds destacados y los defaults de sensibilidad se actualizan inmediatamente.
- Dado que cambio de perfil, cuando la interfaz se reconfigura, entonces mi selección se persiste como preferencia de usuario y al recargar la página el perfil seleccionado se mantiene.
- Dado que selecciono "Sin perfil" (core genérico), cuando la interfaz se reconfigura, entonces veo los 8 kinds sin priorización, sin tags sugeridos, y con defaults neutros.

---

### FT-009.07: Dashboard de Estadísticas por Tenant

#### HU-009.07.1: Visualizar gráficos y métricas del tenant

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.07 Dashboard de Estadísticas por Tenant
**Prioridad**: Must

**Como** System Operator
**Quiero** un dashboard con gráficos y métricas que muestren: total de memorias, distribución por kind (pie chart), evolución temporal (line chart), proporción por status (bar chart), top entities, top topics, y tasa de revisión
**Para** monitorear la salud y evolución del repositorio de un vistazo.

**Criterios de aceptación**:

- Dado que accedo al dashboard de `tenant-A`, cuando cargo la página, entonces veo gráficos interactivos con: pie chart de kinds, line chart de creaciones por semana, bar chart de statuses, y listas de top entities y topics.
- Dado que selecciono un rango de fechas en el dashboard, cuando los gráficos se actualizan, entonces reflejan solo los datos dentro del rango seleccionado.
- Dado que un Memory Consumer sin permisos intenta acceder al dashboard, entonces la interfaz oculta el enlace y si accede por URL el sistema muestra un error de permisos.

---

### FT-009.08: Autenticación Integrada con Keycloak

#### HU-009.08.1: Login/logout con Keycloak OIDC y UI adaptada a roles

**Épica**: EP-009 Frontend Multi-Dominio
**Feature**: FT-009.08 Autenticación Integrada con Keycloak
**Prioridad**: Must

**Como** Memory Consumer
**Quiero** autenticarme en el frontend mediante un flujo OIDC con Keycloak (Authorization Code Flow + PKCE) y que la interfaz se adapte a mis roles
**Para** acceder al motor de memoria de forma segura y ver solo las funcionalidades que mis permisos me permiten.

**Criterios de aceptación**:

- Dado que no estoy autenticado, cuando accedo al frontend, entonces soy redirigido a la página de login de Keycloak. Tras autenticarme exitosamente, soy redirigido de vuelta al frontend con un token JWT válido.
- Dado que mi token está por expirar, cuando el frontend detecta la proximidad de expiración, entonces renueva el token silenciosamente usando refresh token sin interrumpir mi sesión.
- Dado que tengo rol `api-consumer`, cuando navego el frontend, entonces NO veo el panel de revisión, el panel de administración, ni el dashboard de estadísticas — solo veo búsqueda y consulta.
- Dado que tengo rol `memory-admin`, cuando navego el frontend, entonces veo todas las secciones: búsqueda, creación, revisión, administración, dashboard y auditoría.
- Dado que cierro sesión, cuando hago clic en logout, entonces el frontend invalida el token local y me redirige a la página de logout de Keycloak.

---

## Fuera del MVP (Should/Could)

Las siguientes épicas no se descomponen en historias de usuario para el MVP v2.0.0. Se documentan aquí como referencia para releases posteriores.

### EP-007: Batch Ingestion (Should)

**Nota**: La ingesta batch es importante para escenarios de adopción con alto volumen (migración de conversaciones, procesamiento de documentos), pero el MVP puede operar con creación individual de memorias. El endpoint `POST /api/v2/memories/ingest` se documenta en la API pero su implementación completa se difiere.

**Features diferidas**:
- FT-007.01: Ingesta Batch Atómica Transaccional
- FT-007.02: Límite de 100 Memorias por Batch
- FT-007.03: Ingesta desde Conversaciones
- FT-007.04: Ingesta desde Documentos
- FT-007.05: Validación Pre-Ingesta de Cada Memoria
- FT-007.06: Respuesta de Resultado del Batch

### EP-008: Migración v1→v2 (Could)

**Nota**: La migración solo aplica si el usuario tiene datos en v1.0.0 que desea preservar. Es un script externo opcional que no forma parte del runtime de v2. No se incluye en el MVP.

**Features diferidas**:
- FT-008.01 a FT-008.06

### EP-010: SDK Python Básico (Should)

**Nota**: El SDK Python es deseable para adopción por parte de desarrolladores y data scientists, pero el producto es plenamente funcional mediante la API REST v2. Se difiere a un release posterior al MVP.

**Features diferidas**:
- FT-010.01 a FT-010.06

---

## Tabla Resumen

### Total de historias

| Métrica | Valor |
|---|---|
| **Total de historias de usuario** | **69** |
| **Historias en épicas Must (EP-001 a EP-006, EP-009)** | 69 |
| **Features Must con cobertura completa** | 63/63 (100%) |
| **Historias en épicas Should/Could** | 0 (documentadas como Fuera del MVP) |

### Distribución por épica

| Épica | Prioridad | Features | Historias |
|---|---|---|---|
| EP-001: Motor de Memoria Genérico | Must | 9 | 12 |
| EP-002: Perfiles de Dominio | Must | 8 | 8 |
| EP-003: Scoping Multi-Tenant | Must | 7 | 7 |
| EP-004: API REST v2 | Must | 13 | 15 |
| EP-005: Búsqueda Semántica + Graph | Must | 10 | 10 |
| EP-006: Gobernanza y Trazabilidad | Must | 8 | 9 |
| EP-009: Frontend Multi-Dominio | Must | 8 | 8 |
| **Subtotal Must** | | **63** | **69** |
| EP-007: Batch Ingestion | Should | 6 | Fuera del MVP |
| EP-008: Migración v1→v2 | Could | 6 | Fuera del MVP |
| EP-010: SDK Python Básico | Should | 6 | Fuera del MVP |
| **Total** | | **81** | **69 + 18 diferidas** |

### Distribución por prioridad (dentro de las 69 historias Must)

| Prioridad | Historias | % |
|---|---|---|
| **Must** | 66 | 95.7% |
| **Should** | 3 | 4.3% |
| **Could** | 0 | 0% |
| **Fuera del MVP** | 18 features (EP-007, EP-008, EP-010) | — |

> **Nota**: Las 3 historias Should (HU-004.13 Rate Limiting, HU-005.05 Multi-Hop Traversal, HU-005.08 Re-Indexación Masiva) se clasifican como Should porque, aunque pertenecen a épicas Must, son capacidades de optimización o administración avanzada que no bloquean la operación básica del MVP.

### Cobertura de roles

| Rol | Historias donde es protagonista |
|---|---|
| **Domain Curator** | 22 |
| **Memory Consumer** | 8 |
| **System Operator** | 16 |
| **Integration Builder** | 9 |
| **Knowledge Searcher** | 14 |

---

## Glosario

- **MoSCoW**: Método de priorización con cuatro niveles: Must (debe estar), Should (debería estar), Could (podría estar), Won't (no se incluirá).
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0 que permite verificar identidad de usuarios y obtener claims (roles, tenants) desde un proveedor centralizado como Keycloak.
- **Qdrant**: Base de datos vectorial open-source utilizada para almacenar embeddings y ejecutar búsqueda semántica por similitud de coseno.
- **JWT**: JSON Web Token — formato de token de acceso que transporta claims del usuario (identidad, roles, tenantId) firmados digitalmente.
- **Embedding**: Representación vectorial densa de un texto, generada por un modelo de IA (ej. OpenAI text-embedding-3-large), que permite comparar similitud semántica entre textos mediante distancia de coseno.
- **PKCE**: Proof Key for Code Exchange — extensión de OAuth 2.0 que protege contra ataques de interceptación de código de autorización en aplicaciones públicas.
- **p95**: Percentil 95 — métrica de latencia que indica que el 95% de las solicitudes se completan en un tiempo igual o menor al valor indicado.
