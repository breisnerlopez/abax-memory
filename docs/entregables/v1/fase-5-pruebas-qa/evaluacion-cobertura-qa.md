# Evaluacion de Cobertura QA
- **Fase**: 5-Pruebas QA
- **Entregable**: Evaluacion de Cobertura QA
- **Responsable**: qa-functional
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Resumen ejecutivo

La cifra de **26 casos ejecutados** es **razonable como regresion tecnica automatizada de backend**, pero **insuficiente como evidencia de cobertura QA funcional del MVP completo**.

Motivos principales:
- el **alcance R1-MVP** aprobado cubre mas modulos que los efectivamente probados en los 26 tests ejecutados;
- existen **huecos en casos, consulta/listado, archivado por consulta directa, extraccion mini estructurada, contrato API y consistencia de errores**;
- la evidencia ejecutada cubre principalmente **backend tecnico y reglas internas**, no el **set funcional completo aprobado para QA gate**.

**Decision QA recomendada:** **ampliar antes del gate**. No aprobar QA final solo con los 26 ejecutados.

## 2. Base evaluada

| Artefacto | Fecha | Uso en la evaluacion |
|---|---|---|
| `historias-usuario.md` | 2026-05-01 | Validar alcance funcional esperado |
| `backlog-priorizado.md` | 2026-05-01 | Confirmar MVP R1-MVP y prioridades Must |
| `especificacion-funcional.md` | 2026-05-01 | Revisar modulos, flujos, RF, excepciones y contratos |
| `criterios-aceptacion.md` | 2026-05-01 | Medir cobertura contra criterios verificables |
| `codigo-fuente-implementado.md` | 2026-05-01 | Contrastar alcance implementado y limits tecnicos |
| `casos-de-prueba.md` | 2026-05-01 | Revisar cobertura disenada y huecos de QA |
| `reporte-ejecucion-pruebas.md` | 2026-05-02 | Validar evidencia fresca ejecutada |

## 3. Hallazgos objetivos

1. **Los 26 tests ejecutados no representan todo el MVP aprobado.** Cubren parte importante del backend Quarkus, pero no todo el alcance funcional R1-MVP.
2. **Hay inconsistencia entre artefactos de QA.** El reporte de ejecucion se baso en 26 automatizados y declaraba ausencia de `casos-de-prueba.md`; el set formal de casos ya existia y durante esta evaluacion fue normalizado a **49 casos disenados**.
3. **Existe desviacion de foco.** La evidencia ejecutada incluye al menos un escenario de relaciones/auditoria (`TC-QA-006`) mas cercano a alcance extendido, mientras faltaban pruebas explicitas de capacidades Must del MVP como contrato API, detalle de caso y extraccion mini estructurada.

## 4. Evaluacion de suficiencia por modulo

| Modulo / capacidad | Alcance aprobado MVP | Cobertura con 26 ejecutados | Evaluacion |
|---|---|---|---|
| Gestion de casos | Crear, consultar y cerrar caso; usar caso como origen de memoria | **Baja**: se prueba crear memoria desde caso inexistente/existente, pero no endpoints de caso ni cierre | Insuficiente |
| Memorias manuales | Alta manual, validaciones, detalle por ID | **Media**: alta valida/invalida cubierta; detalle por ID y archivada por ID no evidenciado | Parcial |
| API operativa | Crear, consultar, listar con filtros | **Media**: create y algunos errores si; GET/listado funcional completo no | Parcial |
| Frontmatter / modelo canonico | Markdown + frontmatter valido/invalido | **Media**: canonicalizacion y metadata si; frontmatter mal formado como rechazo funcional no queda completamente demostrado | Parcial |
| Clasificacion, criticidad y estados | tipo, criticidad, flujo revision, transiciones | **Media-Alta**: criticidad y aprobacion si; transiciones invalidas y estados completos no todos | Parcial |
| Extraccion mini estructurada | enriquecimiento minimo y manejo de faltantes | **Nula/Baja** | Insuficiente |
| Git / versionado / trazabilidad | commit/ref, falla Git, autor/modificador, historial | **Media**: commit/ref y auditoria parcial si; falla Git y creador/modificador no frescamente ejecutados | Parcial |
| Busqueda semantica + filtros | ranking, filtros, sin resultados, archivadas | **Alta** | Razonable |
| Archivado | archivar, excluir por defecto, consultar explicito | **Media**: exclusion por defecto si; autorizacion y consulta directa archivada no completas | Parcial |
| Seguridad / acceso | 401, 403, RBAC, visibilidad simple | **Alta** en autenticacion/autorizacion basica | Razonable |
| Contrato API y errores consistentes | documentacion por endpoint y formato estable de error | **Baja** | Insuficiente |

## 5. Huecos de cobertura identificados

### 5.1 Por modulo / flujo

| Area | Hueco detectado | Riesgo |
|---|---|---|
| Casos | No habia evidencia ejecutada de `POST /api/casos`, `GET /api/casos/{id}` y `POST /api/casos/{id}/cerrar` | Alto |
| Consulta API | Faltaba evidencia directa de consulta de memoria archivada por ID | Medio |
| Gobierno de memoria | No habia cobertura explicita de extraccion mini estructurada exitosa y fallida | Alto |
| Contrato API | No habia caso formal de verificacion documental por endpoint MVP | Alto |
| Errores consistentes | Faltaba comparacion repetida del formato de error | Medio |
| Archivado | Faltaba separar claramente exclusion por defecto vs recuperacion explicita | Medio |

### 5.2 Por riesgo funcional

| Riesgo | Estado |
|---|---|
| Seguridad 401/403/RBAC | Cubierto razonablemente |
| Errores de validacion | Parcialmente cubierto |
| Persistencia Git fallida | Cobertura disenada, no evidenciada en ejecucion fresca del reporte |
| Trazabilidad y auditoria completa | Parcial |
| Estados y transiciones invalidas | Parcial |
| Casos borde de metadata/faltantes | Parcial |

## 6. Casos adicionales incorporados al set QA

Se actualizo `docs/entregables/fase-5-pruebas-qa/casos-de-prueba.md` para cerrar huecos y normalizar el conteo total.

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-CASE-005 | Existe un caso previamente creado. | 1. Invocar `GET /api/casos/{id}`.<br>2. Revisar el detalle. | Devuelve origen, descripcion, prioridad, dominio, criticidad y estado vigente. |
| TC-CASE-006 | No existe el caso consultado. | 1. Invocar `GET /api/casos/{id}` inexistente.<br>2. Revisar respuesta. | Error controlado sin fallas internas expuestas. |
| TC-MEM-010 | Existe una memoria archivada. | 1. Invocar `GET /api/memorias/{id}`.<br>2. Revisar estado y metadata. | La memoria archivada sigue siendo consultable por ID directo. |
| TC-GOV-006 | Existe memoria con contenido suficiente. | 1. Crear/procesar memoria con pasos, decisiones y evidencias.<br>2. Consultar enriquecimiento. | La extraccion mini estructurada genera elementos minimos reutilizables. |
| TC-GOV-007 | Existe memoria con contenido incompleto. | 1. Procesar memoria incompleta.<br>2. Revisar hallazgos. | Se conservan faltantes/evidencias y no se marca enriquecimiento exitoso indebidamente. |
| TC-API-001 | Documentacion funcional disponible. | 1. Revisar endpoints MVP.<br>2. Contrastar metodo/path/request/response. | Existe contrato funcional consistente por endpoint. |
| TC-API-002 | Endpoint con validaciones conocidas. | 1. Repetir solicitudes invalidas comparables.<br>2. Comparar respuestas. | El error mantiene formato consistente y causa entendible. |

## 7. Reporte de hallazgos de cobertura

| Severidad | Prioridad | Pasos | Evidencia |
|---|---|---|---|
| Alta | Alta | Comparar alcance R1-MVP vs `reporte-ejecucion-pruebas.md` | 26 ejecutados no cubren todos los modulos Must del MVP |
| Alta | Alta | Revisar `casos-de-prueba.md` y `reporte-ejecucion-pruebas.md` | Existia desalineacion entre set disenado y set ejecutado |
| Media | Alta | Revisar criterios CA-040 a CA-042 | No habia casos explicitos para extraccion mini estructurada |
| Media | Media | Revisar criterios CA-057 a CA-061 | No habia caso formal de contrato API y consistencia de errores |

## 8. Matriz de trazabilidad resumida

| Requerimiento / area | Caso(s) de prueba | Resultado actual |
|---|---|---|
| Gestion de casos | TC-CASE-001 a TC-CASE-006 | Disenado; sin evidencia completa de ejecucion en reporte de 26 |
| Alta manual y validaciones | TC-MEM-001 a TC-MEM-003 | Parcialmente ejecutado |
| Alta desde caso | TC-MEM-004 a TC-MEM-005 | Parcialmente ejecutado |
| Consulta/listado de memorias | TC-MEM-006 a TC-MEM-010 | Disenado; cobertura ejecutada parcial |
| Clasificacion, extraccion y estados | TC-GOV-001 a TC-GOV-007 | Disenado; cobertura ejecutada parcial |
| Aprobacion humana por criticidad | TC-APR-001 a TC-APR-004 | Ejecutado parcial con evidencia razonable |
| Git, auditoria y trazabilidad | TC-AUD-001 a TC-AUD-004 | Disenado; evidencia ejecutada parcial |
| Indexacion y jobs | TC-ASY-001 a TC-ASY-003 | Ejecutado razonablemente |
| Busqueda y filtros | TC-SRC-001 a TC-SRC-008 | Ejecutado razonablemente |
| Archivado | TC-ARC-001 a TC-ARC-002 | Disenado; ejecucion parcial |
| Seguridad API | TC-SEC-001 a TC-SEC-003 | Ejecutado razonablemente |
| Contrato API y errores | TC-API-001 a TC-API-002 | Agregado; pendiente ejecucion |

## 9. Informe de ejecucion para decision de gate

| Total requerido para gate funcional | Pasados | Fallidos | Bloqueados/Pendientes | Observacion |
|---|---:|---:|---:|---|
| 49 | 26 | 0 | 23 | Los 26 pasados son valiosos, pero no alcanzan para declarar cobertura QA funcional completa del MVP. |

## 10. Conclusion y recomendacion

### Veredicto
**26 casos no son suficientes para aprobar QA final del MVP tal como esta.**

### Interpretacion correcta
- **Si la pregunta es:** “¿26 tests sirven como smoke/regresion tecnica del backend?” → **Si, son razonables.**
- **Si la pregunta es:** “¿26 tests alcanzan para cerrar el gate QA funcional del MVP aprobado?” → **No, son insuficientes.**

### Recomendacion obligatoria
**Ampliar antes del gate.**

### Minimo accionable
1. Tomar como baseline funcional el set actualizado de **49 casos**.
2. Ejecutar primero los **23 pendientes** con prioridad en:
   - gestion de casos,
   - consulta/listado de memorias,
   - extraccion mini estructurada,
   - contrato API,
   - consistencia de errores,
   - archivado por consulta directa,
   - trazabilidad Git/auditoria faltante.
3. Actualizar `reporte-ejecucion-pruebas.md` para alinear evidencia ejecutada con el set vigente.

### Decision QA final
**No aprobar QA tal como esta. Ampliar cobertura y ejecutar el set pendiente antes del gate.**
