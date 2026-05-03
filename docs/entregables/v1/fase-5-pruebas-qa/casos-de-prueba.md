# Casos de Prueba
- **Fase**: 5-Pruebas QA
- **Entregable**: Casos de Prueba
- **Responsable**: qa-functional
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Objetivo

Consolidar el set funcional completo del MVP **PMOA / Abax-Memory** para dejarlo listo para ejecucion QA, decision de gate y auditoria, cerrando los huecos detectados en gestion de casos, consulta/listado, memoria archivada por ID, extraccion mini estructurada, contrato API, consistencia de errores y trazabilidad/auditoria.

## 2. Alcance cubierto

- Gestion de casos.
- Creacion manual de memorias y creacion desde caso.
- Consulta por ID y listado de memorias con filtros.
- Validacion estructural de Markdown + frontmatter.
- Clasificacion, criticidad, estados y extraccion mini estructurada.
- Aprobacion humana para memorias criticas.
- Persistencia Git, auditoria y trazabilidad.
- Procesamiento asincrono e indexacion semantica.
- Busqueda semantica con filtros estructurados.
- Archivado y consulta explicita de memorias archivadas.
- Seguridad OIDC/JWT/RBAC a nivel API.
- Contrato API y consistencia de errores.

## 3. Fuera de alcance

- UI dedicada.
- Kafka, Neo4j y Redis.
- Fusion avanzada, cache y grafo navegable de R2.
- Eliminacion definitiva R3.

## 4. Regla de trazabilidad y condiciones de ejecucion

- **Fuentes base**: `historias-usuario.md`, `especificacion-funcional.md`, `criterios-aceptacion.md`, `evaluacion-cobertura-qa.md`.
- Cuando un flujo de **casos** no dispone de HU/CA explicita en Fase 2, se traza de forma directa a **RF/excepciones funcionales** y al flujo consumidor relacionado.
- Version bajo prueba: **MVP backend aprobado en Fase 4**.
- Entorno QA esperado: **Quarkus + PostgreSQL + Git/GitHub + Qdrant + OIDC** operativos.
- Deben existir usuarios de prueba con roles `memory-operator`, `memory-reviewer`, `memory-admin`, `memory-auditor` y `api-consumer`.
- Toda ejecucion futura debe registrar evidencia con **fecha**, **version**, **request/response** y, cuando aplique, **commit/PR/job/correlation-id**.

## 5. Casos de prueba funcionales

## 5.1 Modulo M1 - Gestion de Casos

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-CASE-001 | Usuario con rol autorizado para casos y API disponible. | 1. Invocar `POST /api/casos` con titulo, descripcion, origen, prioridad, dominio y criticidad validos.<br>2. Revisar respuesta.<br>3. Consultar el caso creado. | El sistema crea el caso con identificador unico, estado inicial `Abierto` y metadata persistida de forma consistente. |
| TC-CASE-002 | Usuario autorizado. | 1. Invocar `POST /api/casos` omitiendo al menos un campo obligatorio.<br>2. Revisar codigo HTTP y cuerpo de error. | La solicitud es rechazada con error controlado de validacion; no se crea caso parcial. |
| TC-CASE-003 | Existe un caso abierto valido. | 1. Invocar `POST /api/casos/{id}/cerrar` informando resultado operativo y referencia de memoria reutilizada o generada.<br>2. Consultar nuevamente el caso. | El caso cambia a `Cerrado` y conserva trazabilidad del resultado y del vinculo con la memoria. |
| TC-CASE-004 | Existe un caso abierto valido. | 1. Invocar `POST /api/casos/{id}/cerrar` sin resultado minimo requerido.<br>2. Consultar el estado del caso. | El cierre se rechaza; el caso conserva su estado previo y se informa la causa de validacion. |
| TC-CASE-005 | Existe un caso previamente creado. | 1. Invocar `GET /api/casos/{id}` con identificador existente.<br>2. Revisar contenido funcional del caso. | El sistema devuelve detalle consistente del caso, incluyendo origen, descripcion, prioridad, dominio, criticidad y estado vigente. |
| TC-CASE-006 | No existe un caso con el identificador consultado. | 1. Invocar `GET /api/casos/{id}` con ID inexistente.<br>2. Revisar codigo HTTP y payload de error. | El sistema responde con error controlado, sin exponer fallas internas ni devolver datos ambiguos. |

## 5.2 Modulo M2 - Memorias y modelo canonico

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-MEM-001 | Usuario con rol `memory-operator` o equivalente. | 1. Invocar `POST /api/memorias` con payload valido para memoria manual.<br>2. Revisar respuesta.<br>3. Consultar la memoria por ID. | La memoria se registra sin requerir caso previo, devuelve ID unico y conserva contenido/metadata coherentes con la solicitud. |
| TC-MEM-002 | Usuario autorizado. | 1. Invocar `POST /api/memorias` con contenido vacio o metadata obligatoria incompleta.<br>2. Revisar respuesta.<br>3. Verificar que el ID no exista. | La operacion se rechaza con error de validacion y no genera persistencia funcional. |
| TC-MEM-003 | Usuario autorizado. | 1. Invocar alta de memoria con frontmatter ausente, mal formado o ilegible.<br>2. Revisar respuesta y detalle del error. | La memoria no pasa a estado procesable/persistible; el sistema informa el motivo de rechazo. |
| TC-MEM-004 | Existe un caso valido previamente creado. | 1. Invocar `POST /api/memorias/desde-caso` con `caseId` valido.<br>2. Consultar la memoria generada.<br>3. Consultar la trazabilidad. | Se genera memoria vinculada al caso origen y la referencia al `caseId` queda disponible en detalle/trazabilidad. |
| TC-MEM-005 | Usuario autorizado. | 1. Invocar `POST /api/memorias/desde-caso` con `caseId` inexistente, vacio o invalido.<br>2. Revisar respuesta.<br>3. Verificar ausencia de memoria creada. | La solicitud se rechaza sin crear memoria parcial y sin romper consistencia operativa. |
| TC-MEM-006 | Existe una memoria persistida. | 1. Invocar `GET /api/memorias/{id}` con ID existente.<br>2. Validar contenido, metadata y estado. | El detalle devuelve la memoria correcta con contenido Markdown, metadata, origen y estado vigente. |
| TC-MEM-007 | No existe una memoria con el ID consultado. | 1. Invocar `GET /api/memorias/{id}` con ID inexistente.<br>2. Revisar codigo y payload. | El sistema responde con error controlado, sin exponer fallas internas ni datos ambiguos. |
| TC-MEM-008 | Existen memorias de distintos tipos/origenes/estados. | 1. Invocar `GET /api/memorias` con filtros validos.<br>2. Comparar resultados con los datos sembrados. | Solo se devuelven memorias que cumplen todos los filtros enviados. |
| TC-MEM-009 | API disponible. | 1. Invocar `GET /api/memorias` con filtro de estado o tipo no soportado.<br>2. Revisar respuesta. | El sistema rechaza el filtro como invalido y mantiene formato consistente de error. |
| TC-MEM-010 | Existe una memoria archivada y su identificador es conocido. | 1. Invocar `GET /api/memorias/{id}` sobre la memoria archivada.<br>2. Revisar estado y metadatos devueltos. | El sistema devuelve la memoria archivada por consulta directa, mostrando su estado real y manteniendo trazabilidad disponible. |

## 5.3 Modulo M3 - Clasificacion, estados y gobierno

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-GOV-001 | Usuario autorizado. | 1. Crear memoria con tipo permitido, dominio informado y criticidad valida.<br>2. Consultar detalle y metadata. | El sistema persiste la clasificacion funcional para uso en consulta y filtros. |
| TC-GOV-002 | Usuario autorizado. | 1. Intentar publicar memoria sin tipo cuando el tipo sea obligatorio o con valor fuera de catalogo.<br>2. Revisar respuesta. | El sistema impide la publicacion y devuelve validacion consistente del campo invalido/faltante. |
| TC-GOV-003 | Existe memoria nueva creada correctamente. | 1. Crear memoria nueva.<br>2. Consultar estado inicial.<br>3. Ejecutar una transicion valida del flujo. | La memoria recibe un estado inicial verificable y las transiciones validas actualizan el estado con trazabilidad. |
| TC-GOV-004 | Existe memoria en un estado que no admite la transicion solicitada. | 1. Intentar cambio de estado no permitido.<br>2. Consultar estado final. | La accion se rechaza y el estado previo permanece intacto. |
| TC-GOV-005 | Existe memoria de baja/mediana criticidad validada. | 1. Publicar memoria no critica completa.<br>2. Revisar estado y disponibilidad. | La memoria sigue flujo automatizado sin exigir PR manual por criticidad. |
| TC-GOV-006 | Existe memoria con contenido suficiente para extraccion mini estructurada. | 1. Crear/publicar memoria con pasos, decisiones y evidencias identificables.<br>2. Consultar metadata enriquecida o resultado de extraccion. | El sistema obtiene al menos elementos minimos reutilizables definidos por negocio y deja evidencia del enriquecimiento. |
| TC-GOV-007 | Existe memoria con contenido parcial o incompleto. | 1. Ejecutar alta/procesamiento de memoria con informacion insuficiente para extraccion completa.<br>2. Revisar estado y evidencias generadas. | El sistema conserva evidencia de campos faltantes o hallazgos pendientes, sin marcar la extraccion como exitosamente completa si no corresponde. |

## 5.4 Modulo M4 - Validacion y aprobacion humana

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-APR-001 | Usuario operador autorizado; politica de criticidad vigente. | 1. Crear memoria clasificada como `alta` o `critica`.<br>2. Revisar estado posterior a la validacion inicial. | La memoria queda en `En revision` o estado equivalente y no aparece como aprobada automaticamente. |
| TC-APR-002 | Existe memoria critica en revision; usuario con rol `memory-reviewer`. | 1. Ejecutar flujo de aprobacion humana/PR establecido.<br>2. Confirmar aprobacion.<br>3. Consultar detalle y trazabilidad. | La memoria cambia a `Aprobada`, conserva evidencia de aprobacion humana y queda disponible segun politica operativa. |
| TC-APR-003 | Existe memoria critica en revision. | 1. Rechazar u observar la memoria mediante el flujo definido.<br>2. Consultar estado y disponibilidad de consulta. | La memoria permanece no aprobada para reutilizacion operativa general y conserva observaciones/rechazo auditables. |
| TC-APR-004 | Existe memoria critica pendiente; usuario sin rol revisor. | 1. Intentar aprobar la memoria con rol insuficiente.<br>2. Revisar respuesta.<br>3. Consultar estado final. | La operacion es denegada por autorizacion y el estado de la memoria no cambia. |

## 5.5 Modulo M5 - Persistencia Git, auditoria y trazabilidad

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-AUD-001 | Git/GitHub operativo; usuario autorizado. | 1. Crear o actualizar una memoria valida.<br>2. Consultar respuesta de alta/actualizacion.<br>3. Consultar trazabilidad. | La operacion deja referencia de version/commit y evidencia de persistencia versionada en Git. |
| TC-AUD-002 | Git/GitHub temporalmente no disponible o simulacion de falla controlada. | 1. Ejecutar alta/actualizacion de memoria valida.<br>2. Forzar falla de persistencia Git.<br>3. Revisar estado final de la operacion. | La operacion no se considera completada; el sistema informa falla verificable y evita falso positivo funcional. |
| TC-AUD-003 | Existe memoria creada, versionada y eventualmente aprobada. | 1. Invocar `GET /api/memorias/{id}/trazabilidad`.<br>2. Revisar origen, versionado, estado, actor y eventos. | La trazabilidad permite identificar origen, cambios, validaciones, aprobaciones y acciones operativas relevantes. |
| TC-AUD-004 | Existe memoria creada y luego modificada por otro usuario autorizado. | 1. Crear memoria con usuario A.<br>2. Modificar memoria con usuario B.<br>3. Consultar auditoria. | La auditoria expone creador, ultimo modificador y fechas correspondientes de forma verificable. |

## 5.6 Modulo M6 - Procesamiento asincrono e indexacion

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-ASY-001 | Existe memoria nueva elegible para indexacion. | 1. Crear memoria valida.<br>2. Consultar inmediatamente su estado de procesamiento antes de completarse el job. | El sistema informa un estado transitorio verificable (`Pendiente`, `PENDING_INDEX` o equivalente) antes de quedar disponible en busqueda. |
| TC-ASY-002 | Worker y Qdrant operativos. | 1. Crear/publicar memoria elegible.<br>2. Esperar finalizacion del job.<br>3. Consultar estado de procesamiento y ejecutar busqueda relacionada. | La memoria queda `Disponible/AVAILABLE` para recuperacion semantica cuando la indexacion concluye correctamente. |
| TC-ASY-003 | Existe memoria con falla inducida de indexacion o contenido no procesable. | 1. Ejecutar procesamiento asincrono.<br>2. Revisar estado de memoria/job.<br>3. Buscar la memoria semanticamente. | La memoria queda en estado fallido controlado (`INDEX_FAILED` o equivalente), con causa registrada y sin exponerse como disponible en busqueda. |

## 5.7 Modulo M7 - Busqueda semantica y filtros

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-SRC-001 | Existen memorias indexadas sobre el dominio consultado. | 1. Invocar endpoint de busqueda semantica con consulta en lenguaje natural.<br>2. Revisar orden de resultados. | El sistema devuelve memorias ordenadas por relevancia, con score/resumen/metadatos clave. |
| TC-SRC-002 | Existen memorias semanticamente relacionadas pero sin coincidencia textual exacta. | 1. Ejecutar busqueda con una consulta conceptualmente equivalente.<br>2. Revisar resultados. | El sistema puede devolver memorias relevantes aunque no exista match textual exacto. |
| TC-SRC-003 | No existen coincidencias relevantes para la consulta. | 1. Ejecutar busqueda semantica con consulta sin match relevante.<br>2. Revisar respuesta. | El sistema responde vacio o controlado, sin error tecnico ni resultados espurios. |
| TC-SRC-004 | Existen memorias indexadas de varios dominios/estados/criticidades. | 1. Ejecutar busqueda con texto libre y filtros estructurados validos.<br>2. Verificar cada resultado. | Solo se devuelven memorias que cumplen simultaneamente la relevancia semantica y los filtros aplicados. |
| TC-SRC-005 | Existen resultados semanticamente relevantes que no cumplen los filtros. | 1. Ejecutar busqueda con filtros restrictivos.<br>2. Revisar resultados devueltos. | Los resultados que no cumplen filtros quedan excluidos; el sistema no relaja filtros implicitamente. |
| TC-SRC-006 | API disponible. | 1. Ejecutar busqueda con `topK` o filtro estructurado invalido.<br>2. Revisar payload de error. | La solicitud se rechaza con error funcional consistente y trazable. |
| TC-SRC-007 | Existe al menos una memoria archivada y una activa relacionadas con la consulta. | 1. Ejecutar consulta/busqueda estandar sin pedir archivadas.<br>2. Revisar lista de resultados. | Las memorias archivadas no aparecen por defecto en consultas activas. |
| TC-SRC-008 | Existe memoria archivada. | 1. Ejecutar consulta que incluya explicitamente archivadas o consultar por ID directo.<br>2. Revisar detalle/trazabilidad. | La memoria archivada sigue siendo consultable bajo solicitud explicita y conserva su historial. |

## 5.8 Modulo M8 - Archivado

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-ARC-001 | Existe memoria elegible para archivado; usuario con rol autorizado. | 1. Invocar accion de archivado con motivo.<br>2. Consultar detalle y listados activos. | La memoria cambia a `Archivada`, conserva historial y deja de figurar en listados activos por defecto. |
| TC-ARC-002 | Existe memoria elegible para archivado; usuario sin permisos suficientes. | 1. Intentar archivar la memoria.<br>2. Revisar respuesta y estado posterior. | La operacion es denegada y se conserva el estado previo de la memoria. |

## 5.9 Modulo M9 - Seguridad y acceso API

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-SEC-001 | Token JWT valido con rol habilitado para la operacion. | 1. Invocar un endpoint protegido del MVP.<br>2. Revisar respuesta. | La API permite la operacion segun RBAC y devuelve respuesta consistente. |
| TC-SEC-002 | No se envia token o el token es invalido/expirado. | 1. Invocar endpoint protegido sin autenticacion valida.<br>2. Revisar codigo HTTP y body. | El sistema responde `401` o equivalente de autenticacion, sin exponer detalle interno sensible. |
| TC-SEC-003 | Token valido pero con rol insuficiente para la operacion. | 1. Invocar endpoint protegido restringido a otro rol.<br>2. Revisar respuesta. | El sistema responde `403` o equivalente de autorizacion y bloquea el acceso. |

## 5.10 Modulo M10 - Contrato API y consistencia de errores

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-API-001 | Documentacion funcional/API del MVP disponible. | 1. Revisar cada endpoint aprobado del MVP.<br>2. Verificar metodo, path, request y response documentados.<br>3. Comparar contra backlog y especificacion funcional. | Existe un contrato funcional por endpoint del MVP y su documentacion es consistente con el comportamiento aprobado. |
| TC-API-002 | Endpoint del MVP con validaciones conocidas. | 1. Ejecutar dos solicitudes invalidas del mismo tipo sobre el mismo endpoint.<br>2. Comparar codigo, estructura y campos del error.<br>3. Repetir la validacion en al menos un segundo endpoint critico. | El formato de error se mantiene consistente y permite identificar la causa funcional sin depender de mensajes tecnicos internos. |

## 6. Cobertura de huecos identificados

| Hueco detectado | Cobertura consolidada |
|---|---|
| Gestion de casos | TC-CASE-001 a TC-CASE-006 |
| Consulta/listado de memorias | TC-MEM-006 a TC-MEM-010 |
| Memoria archivada por ID | TC-MEM-010, TC-SRC-008 |
| Extraccion mini estructurada | TC-GOV-006, TC-GOV-007 |
| Contrato API | TC-API-001 |
| Consistencia de errores | TC-CASE-002, TC-MEM-009, TC-SRC-006, TC-API-002 |
| Trazabilidad y auditoria | TC-AUD-001 a TC-AUD-004 |

## 7. Reporte de defectos

Este entregable corresponde a **diseno/consolidacion del set** y no a ejecucion. No se registran defectos funcionales porque aun no existe evidencia fresca de corrida para estos 49 casos dentro de este mismo artefacto.

| Severidad | Prioridad | Pasos | Evidencia |
|---|---|---|---|
| N/A | N/A | No aplica en fase de diseno/consolidacion. | Sin ejecucion funcional en este documento al 2026-05-01. |

## 8. Matriz de trazabilidad

| Historia / Fuente funcional | RF / CA trazados | Caso(s) de prueba | Resultado |
|---|---|---|---|
| Gestion de casos (cobertura derivada de especificacion funcional) | RF-001, RF-002, RF-003, RF-004, RF-042; excepciones de caso inexistente y cierre sin resultado | TC-CASE-001, TC-CASE-002, TC-CASE-003, TC-CASE-004, TC-CASE-005, TC-CASE-006 | Disenado |
| HU-001.1.1 Registrar memorias en Markdown con frontmatter estandar | RF-005, RF-007, RF-009, CA-001, CA-002, CA-003 | TC-MEM-001, TC-MEM-002, TC-MEM-003 | Disenado |
| HU-001.1.2 Crear memorias desde un caso | RF-010, RF-023, CA-004, CA-005, CA-006 | TC-MEM-004, TC-MEM-005 | Disenado |
| HU-001.1.3 Crear memorias manuales | RF-009, CA-007, CA-008, CA-009 | TC-MEM-001, TC-MEM-002 | Disenado |
| HU-002.1.2 Consultar detalle de memoria por ID | RF-043, CA-016, CA-017, CA-018, CA-033 | TC-MEM-006, TC-MEM-007, TC-MEM-010 | Disenado |
| HU-002.1.3 Listar memorias con filtros basicos | RF-018, RF-043, CA-019, CA-020, CA-021 | TC-MEM-008, TC-MEM-009 | Disenado |
| HU-005.1.1 Clasificar memorias por tipo | RF-013, RF-016, CA-037, CA-038, CA-039 | TC-GOV-001, TC-GOV-002 | Disenado |
| HU-005.1.2 Ejecutar extraccion mini estructurada | RF-014, RF-015, CA-040, CA-041, CA-042 | TC-GOV-006, TC-GOV-007 | Disenado |
| HU-005.1.4 Mantener estados de memoria | RF-028, CA-046, CA-047, CA-048 | TC-GOV-003, TC-GOV-004 | Disenado |
| HU-004.1.2 / HU-005.1.3 Revision humana para memorias criticas | RF-016, RF-027, RF-028, CA-043, CA-044, CA-045 | TC-GOV-005, TC-APR-001, TC-APR-002, TC-APR-003, TC-APR-004 | Disenado |
| HU-004.1.1 Persistir memorias en Git | RF-029, RF-030, RF-031, RF-032, CA-010, CA-011, CA-012 | TC-AUD-001, TC-AUD-002 | Disenado |
| MF-10 Auditoria y trazabilidad extremo a extremo | RF-038, RF-039, RF-040, RF-041, CA-054, CA-055, CA-056 | TC-AUD-003, TC-AUD-004, TC-ARC-001 | Disenado |
| HU-003.1.1 Generar embeddings / indexacion | CA-022, CA-023, CA-024, CA-034, CA-035, CA-036 | TC-ASY-001, TC-ASY-002, TC-ASY-003 | Disenado |
| HU-005.1.1 Buscar memorias semanticamente | RF-017, RF-019, CA-025, CA-026, CA-027 | TC-SRC-001, TC-SRC-002, TC-SRC-003 | Disenado |
| HU-003.1.3 Combinar busqueda semantica con filtros estructurados | RF-018, RF-019, RF-020, CA-028, CA-029, CA-030 | TC-SRC-004, TC-SRC-005 | Disenado |
| HU-006.1.1 / HU-006.1.2 Filtros estructurados y estados visibles | RF-018, RF-033, CA-021, CA-050, CA-051 | TC-SRC-006, TC-SRC-007, TC-SRC-008, TC-MEM-010 | Disenado |
| HU-006.1.1 Archivar memorias obsoletas | RF-033, RF-045, CA-049, CA-050, CA-051 | TC-ARC-001, TC-ARC-002 | Disenado |
| HU-007.1.1 Acceso y visibilidad | CA-052, CA-053 | TC-SEC-001, TC-SEC-002, TC-SEC-003, TC-APR-004 | Disenado |
| HU-007.1.2 Registrar creador y modificador | CA-054, CA-055, CA-056 | TC-AUD-004 | Disenado |
| HU-008.1.1 Documentacion de endpoints | RF-046, CA-057, CA-058 | TC-API-001 | Disenado |
| HU-008.1.2 Respuestas consistentes de error y validacion | CA-059, CA-060, CA-061 | TC-CASE-002, TC-MEM-009, TC-SRC-006, TC-API-002 | Disenado |

## 9. Informe de ejecucion

> Estado actual del artefacto: **set consolidado y listo para ejecucion**. La actualizacion a Pasado/Fallido/Bloqueado debe realizarse con evidencia fresca al correr el ciclo QA formal.

| Total | Pasados | Fallidos | Bloqueados | Observacion |
|---|---:|---:|---:|---|
| 49 | 0 | 0 | 49 | Set formal consolidado, completo y apto para ejecucion/gate; pendiente corrida QA con evidencia fresca. |

## 10. Criterios de readiness para ejecucion y auditoria

1. El set mantiene el baseline formal de **49 casos**.
2. Los huecos detectados por la evaluacion de cobertura quedaron cubiertos explicitamente.
3. La trazabilidad queda visible hacia **historias, RF y criterios de aceptacion**.
4. El artefacto queda listo para usarse como base de ejecucion, reporte de resultados y evidencia de auditoria QA.
