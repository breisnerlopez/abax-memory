# Criterios de Aceptacion
- **Fase**: 2-Analisis Funcional
- **Entregable**: Criterios de Aceptacion
- **Responsable**: business-analyst
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Objetivo

Consolidar y refinar los criterios de aceptacion funcionales de **PMOA / Abax-Memory** para:
- **R1-MVP**
- **R2 (release siguiente)**

Los criterios se expresan en formato **Given / When / Then**, con foco en validacion **QA/UAT**, trazabilidad a historias de usuario y cobertura de escenarios **positivos, negativos y de borde**.

## 2. Alcance

### 2.1 Incluye
- Criterios funcionales para historias priorizadas de **R1-MVP**.
- Criterios funcionales para historias priorizadas de **R2**.
- Trazabilidad por modulo, epica e historia de usuario.
- Referencia funcional de endpoints REST ya identificados en Fase 0.

### 2.2 No incluye
- Historias **R3** o evolutivos posteriores.
- UI dedicada para usuarios finales.
- Definicion tecnica final de arquitectura, persistencia o integraciones internas.
- Normalizacion tecnica final de contratos REST cuando existan variantes de nomenclatura entre documentos de Fase 0.

## 3. Regla de trazabilidad usada

- **Fuente principal de release y prioridad**: `backlog-priorizado.md`
- **Fuente complementaria de detalle funcional**: `historias-usuario.md`
- Cuando ambas fuentes describen la misma capacidad con redaccion distinta, se consolida un unico criterio funcional verificable sin agregar alcance nuevo.

## 4. Mapa de trazabilidad consolidado

| Modulo | Epica | Historias trazadas | Release |
|---|---|---|---|
| M1. Gestion de memorias | EP-001 | HU-001.1.1, HU-001.1.2, HU-001.1.3, HU-001.1.4 | R1-MVP |
| M2. API operativa | EP-002 | HU-002.1.1, HU-002.1.2, HU-002.1.3 | R1-MVP |
| M3. Busqueda y recuperacion | EP-003 | HU-003.1.1, HU-003.1.2, HU-003.1.3 | R1-MVP |
| M4. Persistencia y metadatos | EP-004 | HU-004.1.1, HU-004.1.2 | R1-MVP |
| M5. Gobierno de memoria | EP-005 | HU-005.1.1, HU-005.1.2, HU-005.1.3, HU-005.1.4 | R1-MVP |
| M6. Depuracion y mantenimiento | EP-006 | HU-006.1.1 | R1-MVP |
| M7. Acceso y visibilidad | EP-007 | HU-007.1.1, HU-007.1.2 | R1-MVP |
| M8. Contrato API | EP-008 | HU-008.1.1, HU-008.1.2 | R1-MVP |
| M9. Persistencia extendida | EP-004 | HU-004.1.3, HU-004.1.4 | R2 |
| M10. Depuracion avanzada | EP-006 | HU-006.1.2, HU-006.1.3 | R2 |
| M11. Contrato operativo ampliado | EP-008 | HU-008.1.3 | R2 |
| M12. Grafo de conocimiento | EP-009 | HU-009.1.1 | R2 |

## 5. Referencia funcional de endpoints

> Nota: estos contratos reflejan necesidades funcionales ya identificadas en Fase 0. Su version definitiva debera alinearse en diseno tecnico sin alterar el alcance funcional aprobado.

| Release | Metodo | Path | Payload JSON de referencia | Trazabilidad |
|---|---|---|---|---|
| R1-MVP | POST | `/api/memorias` | `{ "titulo": "string", "tipo": "string", "origen": "manual", "contenidoMarkdown": "string", "metadata": {} }` | HU-001.1.1, HU-002.1.1 |
| R1-MVP | POST | `/api/memorias/desde-caso` | `{ "caseId": "string", "criticidad": "media", "dominios": ["string"] }` | HU-001.1.2, HU-002.1.1 |
| R1-MVP | GET | `/api/memorias/{id}` | N/A | HU-002.1.2 |
| R1-MVP | GET | `/api/memorias` | Query params de filtro | HU-002.1.3 |
| R1-MVP | POST | `/api/busquedas/semantica` | `{ "consulta": "string", "filtros": {} }` | HU-003.1.2, HU-003.1.3 |
| R1-MVP | POST | `/api/memorias/{id}/aprobar` | `{ "comentario": "string" }` | HU-005.1.3 |
| R1-MVP | POST | `/api/memorias/{id}/archivar` | `{ "motivo": "string" }` | HU-006.1.1 |
| R2 | GET | `/api/health` | N/A | HU-008.1.3 |
| R2 | POST | `/api/memorias/{id}/relaciones` | `{ "relaciones": [] }` | HU-009.1.1 |
| R2 | POST | `/api/memorias/{id}/fusion` | `{ "memoriasOrigen": ["id-1", "id-2"] }` | HU-006.1.3 |

---

## 6. Criterios de aceptacion — R1-MVP

## M1. Gestion de memorias

### HU-001.1.1 — Registrar memorias en Markdown con frontmatter estandar

**CA-001**  
**Given** un consumidor autorizado y un payload con frontmatter obligatorio completo  
**When** solicita registrar una memoria  
**Then** el sistema crea la memoria en formato Markdown con frontmatter valido.

**CA-002**  
**Given** una solicitud de registro sin uno o mas campos obligatorios del frontmatter  
**When** se procesa la solicitud  
**Then** el sistema rechaza la operacion e informa los campos faltantes.

**CA-003**  
**Given** una memoria con frontmatter mal formado  
**When** el sistema intenta validarla  
**Then** la memoria no pasa a estado procesable y se registra el motivo de rechazo.

### HU-001.1.2 — Crear memorias desde un caso

**CA-004**  
**Given** un caso existente con identificador valido  
**When** se solicita crear una memoria desde caso  
**Then** el sistema genera una memoria vinculada al caso origen.

**CA-005**  
**Given** una memoria creada desde caso  
**When** se consulta su detalle  
**Then** el sistema muestra la referencia trazable al caso que la origino.

**CA-006**  
**Given** un identificador de caso inexistente, vacio o invalido  
**When** se intenta crear la memoria  
**Then** el sistema rechaza la solicitud sin generar memoria parcial.

### HU-001.1.3 — Crear memorias manuales

**CA-007**  
**Given** un usuario autorizado con contenido y metadata minima validos  
**When** solicita crear una memoria manual  
**Then** el sistema registra la memoria sin requerir un caso previo.

**CA-008**  
**Given** una memoria manual creada correctamente  
**When** finaliza la operacion  
**Then** el sistema devuelve un identificador unico de memoria.

**CA-009**  
**Given** una solicitud manual con contenido vacio o metadata incompleta  
**When** se valida la solicitud  
**Then** el sistema la rechaza y no genera persistencia.

### HU-001.1.4 — Versionar cada memoria en Git

**CA-010**  
**Given** una memoria creada o actualizada correctamente  
**When** se confirma la operacion  
**Then** el sistema registra el cambio en el repositorio Git asociado.

**CA-011**  
**Given** una memoria versionada  
**When** se consulta su trazabilidad  
**Then** el sistema expone al menos la referencia de version o commit generado.

**CA-012**  
**Given** una falla en la persistencia Git  
**When** el sistema no puede completar el versionado  
**Then** la operacion queda informada como fallida con resultado verificable para QA/UAT.

## M2. API operativa

### HU-002.1.1 — Publicar memorias mediante endpoints REST

**CA-013**  
**Given** un consumidor autorizado y un endpoint valido  
**When** invoca la operacion de alta de memoria  
**Then** el sistema responde con codigo HTTP y payload JSON consistentes con el contrato funcional.

**CA-014**  
**Given** una solicitud con estructura JSON invalida o semantica incompleta  
**When** se procesa el endpoint  
**Then** el sistema devuelve un error de validacion verificable.

**CA-015**  
**Given** una operacion exitosa de alta  
**When** concluye la llamada REST  
**Then** la respuesta incluye el identificador de la memoria y su estado inicial.

### HU-002.1.2 — Consultar detalle de una memoria por identificador

**CA-016**  
**Given** un identificador de memoria existente  
**When** el consumidor consulta el detalle  
**Then** el sistema devuelve contenido, metadata y estado de la memoria.

**CA-017**  
**Given** un identificador inexistente  
**When** se consulta el detalle  
**Then** el sistema responde con error controlado sin exponer fallas tecnicas internas.

**CA-018**  
**Given** una memoria archivada  
**When** se consulta por identificador directo  
**Then** el sistema la devuelve con su estado real y trazabilidad disponible.

### HU-002.1.3 — Listar memorias con filtros basicos

**CA-019**  
**Given** una consulta con filtros validos por tipo, estado u origen  
**When** el consumidor lista memorias  
**Then** el sistema devuelve solo registros que cumplan los filtros solicitados.

**CA-020**  
**Given** multiples filtros simultaneos  
**When** se ejecuta la consulta  
**Then** el sistema aplica los filtros de manera consistente sobre el mismo conjunto de resultados.

**CA-021**  
**Given** un filtro con valor no valido  
**When** el consumidor ejecuta la consulta  
**Then** el sistema informa el error de validacion sin devolver resultados ambiguos.

## M3. Busqueda y recuperacion

### HU-003.1.1 — Generar embeddings para habilitar busqueda semantica

**CA-022**  
**Given** una memoria aprobada o disponible para indexacion  
**When** finaliza su procesamiento  
**Then** el sistema genera su representacion semantica para consulta posterior.

**CA-023**  
**Given** una memoria con contenido no procesable o invalido  
**When** el sistema intenta generar su embedding  
**Then** la memoria queda con estado de procesamiento fallido y causa registrada.

**CA-024**  
**Given** una memoria recien creada  
**When** aun no completo el procesamiento semantico  
**Then** el sistema informa un estado transitorio verificable antes de habilitar la busqueda.

### HU-003.1.2 — Buscar memorias semanticamente

**CA-025**  
**Given** una consulta en lenguaje natural  
**When** se ejecuta la busqueda semantica  
**Then** el sistema devuelve memorias ordenadas por relevancia.

**CA-026**  
**Given** una consulta sin coincidencias relevantes sobre el umbral configurado  
**When** se ejecuta la busqueda  
**Then** el sistema responde con resultado vacio o controlado sin error tecnico.

**CA-027**  
**Given** una memoria semanticamente relacionada pero sin coincidencia textual exacta  
**When** se realiza la consulta  
**Then** la memoria puede aparecer entre los resultados relevantes.

### HU-003.1.3 — Combinar busqueda semantica con filtros estructurados

**CA-028**  
**Given** una consulta con texto libre y filtros validos  
**When** se ejecuta la busqueda combinada  
**Then** el sistema aplica simultaneamente relevancia semantica y restricciones estructuradas.

**CA-029**  
**Given** resultados semanticamente relevantes que no cumplen los filtros enviados  
**When** se devuelve la respuesta  
**Then** esos resultados no deben incluirse.

**CA-030**  
**Given** una busqueda combinada sin resultados que cumplan todos los criterios  
**When** finaliza la consulta  
**Then** el sistema informa ausencia de resultados sin relajar filtros de manera implicita.

## M4. Persistencia y metadatos

### HU-004.1.1 — Almacenar metadatos estructurados de memorias

**CA-031**  
**Given** una memoria creada correctamente  
**When** se persiste su informacion estructurada  
**Then** el sistema registra al menos identificador, tipo, origen, estado y metadata funcional requerida.

**CA-032**  
**Given** una memoria con metadata obligatoria incompleta  
**When** se intenta persistir  
**Then** el sistema rechaza la operacion o deja la memoria en estado no publicable.

**CA-033**  
**Given** una consulta posterior sobre la memoria  
**When** se recuperan sus metadatos  
**Then** los valores devueltos deben ser consistentes con la version vigente registrada.

### HU-004.1.2 — Indexar embeddings en Qdrant

**CA-034**  
**Given** una memoria con embedding generado correctamente  
**When** finaliza la indexacion vectorial  
**Then** la memoria queda disponible para recuperacion semantica.

**CA-035**  
**Given** una falla de indexacion en el indice vectorial  
**When** el sistema no puede completar el alta  
**Then** la memoria no debe informarse como disponible para busqueda.

**CA-036**  
**Given** una memoria ya indexada  
**When** se consulta su estado de procesamiento  
**Then** el sistema indica que esta disponible para busqueda.

## M5. Gobierno de memoria

### HU-005.1.1 — Clasificar memorias por tipos de memoria operativa reutilizable

**CA-037**  
**Given** una nueva memoria  
**When** se registra con un tipo de memoria permitido  
**Then** el sistema almacena dicha clasificacion para consulta y filtros.

**CA-038**  
**Given** una memoria sin tipo cuando el tipo es obligatorio  
**When** se intenta publicarla  
**Then** el sistema impide continuar e informa la validacion.

**CA-039**  
**Given** un valor de tipo fuera del catalogo funcional aprobado  
**When** se procesa la solicitud  
**Then** el sistema rechaza el valor como no valido.

### HU-005.1.2 — Ejecutar extraccion mini estructurada

**CA-040**  
**Given** una memoria con contenido suficiente  
**When** se ejecuta la extraccion mini estructurada  
**Then** el sistema obtiene al menos elementos minimos reutilizables definidos por el negocio.

**CA-041**  
**Given** una memoria con informacion incompleta  
**When** se realiza la extraccion  
**Then** el sistema conserva evidencia de campos faltantes para revision humana.

**CA-042**  
**Given** una extraccion fallida  
**When** concluye el procesamiento  
**Then** la memoria no debe marcarse como enriquecida exitosamente.

### HU-005.1.3 — Requerir validacion humana por PR para memorias criticas

**CA-043**  
**Given** una memoria clasificada como alta o critica segun la politica vigente  
**When** se solicita su publicacion  
**Then** el sistema la deja en estado de revision humana y no la publica como aprobada automaticamente.

**CA-044**  
**Given** una memoria critica en revision  
**When** el revisor humano la aprueba por el flujo establecido  
**Then** la memoria cambia a estado aprobada y queda disponible segun la politica operativa.

**CA-045**  
**Given** una memoria no critica  
**When** se publica correctamente  
**Then** el sistema no exige PR manual por criticidad.

### HU-005.1.4 — Mantener estados de memoria

**CA-046**  
**Given** una memoria nueva  
**When** es creada  
**Then** el sistema le asigna un estado inicial verificable.

**CA-047**  
**Given** una memoria que atraviesa validacion, aprobacion o archivado  
**When** ocurre una transicion valida  
**Then** el sistema actualiza el estado y conserva trazabilidad del cambio.

**CA-048**  
**Given** una solicitud de cambio de estado no permitida por el flujo vigente  
**When** se procesa la accion  
**Then** el sistema la rechaza sin alterar el estado actual.

## M6. Depuracion y mantenimiento

### HU-006.1.1 — Archivar memorias obsoletas

**CA-049**  
**Given** una memoria existente y elegible para archivado  
**When** un usuario autorizado ejecuta la accion  
**Then** la memoria cambia a estado archivado.

**CA-050**  
**Given** una memoria archivada  
**When** se ejecuta una consulta estandar de memorias activas  
**Then** la memoria no aparece por defecto en los resultados.

**CA-051**  
**Given** una consulta que incluye archivados  
**When** se recupera la informacion  
**Then** la memoria archivada sigue siendo consultable con su trazabilidad.

## M7. Acceso y visibilidad

### HU-007.1.1 — Visibilidad simple para personas con acceso al repositorio

**CA-052**  
**Given** un usuario con acceso autorizado al repositorio operativo  
**When** consulta memorias dentro del alcance permitido del MVP  
**Then** puede visualizar el contenido disponible sin segmentacion granular adicional.

**CA-053**  
**Given** un usuario sin acceso autorizado al repositorio o API  
**When** intenta consultar memorias  
**Then** el sistema deniega la operacion.

### HU-007.1.2 — Registrar creador y modificador de cada memoria

**CA-054**  
**Given** una memoria creada por un usuario autorizado  
**When** se registra el alta  
**Then** el sistema conserva la identidad del creador.

**CA-055**  
**Given** una memoria modificada posteriormente  
**When** se registra el cambio  
**Then** el sistema conserva la identidad del ultimo modificador y la fecha del cambio.

**CA-056**  
**Given** una consulta de auditoria sobre una memoria  
**When** se revisa su trazabilidad  
**Then** es posible verificar quien la creo y quien la modifico.

## M8. Contrato API

### HU-008.1.1 — Disponer de documentacion de endpoints

**CA-057**  
**Given** el conjunto de operaciones del MVP  
**When** QA o un consumidor revisan la documentacion funcional  
**Then** existe un contrato funcional por endpoint con metodo, path, request y response.

**CA-058**  
**Given** un endpoint del MVP aprobado para uso  
**When** se revisa su documentacion  
**Then** la descripcion funcional es consistente con el comportamiento esperado del backlog.

### HU-008.1.2 — Recibir respuestas consistentes de error y validacion

**CA-059**  
**Given** una solicitud invalida a cualquier endpoint cubierto por el MVP  
**When** el backend detecta error funcional o de validacion  
**Then** devuelve una respuesta consistente y verificable.

**CA-060**  
**Given** dos solicitudes invalidas del mismo tipo sobre el mismo endpoint  
**When** se comparan las respuestas  
**Then** el formato de error debe mantenerse consistente.

**CA-061**  
**Given** un error funcional controlado  
**When** el consumidor recibe la respuesta  
**Then** puede identificar la causa sin depender de mensajes tecnicos internos.

---

## 7. Criterios de aceptacion — R2 (release siguiente)

## M9. Persistencia extendida

### HU-004.1.3 — Representar dominios y relaciones en Neo4j

**CA-062**  
**Given** una memoria con dominio y relaciones funcionales registradas  
**When** se persiste su contexto relacional  
**Then** el sistema conserva nodos y vinculos consultables para navegacion posterior.

**CA-063**  
**Given** una memoria sin relaciones declaradas  
**When** se registra en el modelo relacional  
**Then** el sistema la conserva sin error y sin crear relaciones ficticias.

### HU-004.1.4 — Usar Redis para cachear consultas frecuentes

**CA-064**  
**Given** una consulta frecuente elegible para cache funcional  
**When** se repite dentro de la politica vigente  
**Then** el sistema puede responder reutilizando el resultado almacenado sin alterar su contenido funcional.

**CA-065**  
**Given** una memoria modificada o archivada  
**When** una consulta dependiente de cache vuelve a ejecutarse  
**Then** el resultado no debe exponer informacion obsoleta fuera de la politica definida.

## M10. Depuracion avanzada

### HU-006.1.2 — Marcar memorias duplicadas

**CA-066**  
**Given** dos memorias existentes  
**When** una se marca como duplicada de otra memoria canonica  
**Then** el sistema conserva la asociacion entre duplicada y canonica.

**CA-067**  
**Given** una memoria marcada como duplicada  
**When** se ejecuta una consulta estandar de memorias activas  
**Then** la memoria duplicada no aparece salvo solicitud explicita.

**CA-068**  
**Given** una solicitud para marcar duplicidad con identificadores inexistentes o iguales entre si  
**When** se procesa la accion  
**Then** el sistema rechaza la operacion.

### HU-006.1.3 — Fusionar memorias relacionadas

**CA-069**  
**Given** dos o mas memorias elegibles para fusion  
**When** un usuario autorizado ejecuta la fusion  
**Then** el sistema genera o actualiza una memoria consolidada segun la politica vigente.

**CA-070**  
**Given** una fusion completada  
**When** se consulta la trazabilidad de las memorias origen  
**Then** el sistema conserva referencia a la memoria resultante.

**CA-071**  
**Given** una solicitud de fusion con memorias inexistentes, archivadas no elegibles o no compatibles  
**When** se procesa la accion  
**Then** el sistema la rechaza informando la causa.

## M11. Contrato operativo ampliado

### HU-008.1.3 — Exponer endpoints de salud basicos

**CA-072**  
**Given** la API operativa disponible  
**When** un consumidor autorizado consulta el endpoint de salud  
**Then** el sistema informa un estado verificable de disponibilidad.

**CA-073**  
**Given** una degradacion o indisponibilidad de una dependencia principal monitoreada por la politica aprobada  
**When** se consulta el endpoint de salud  
**Then** la respuesta refleja que existe una condicion no saludable.

## M12. Grafo de conocimiento

### HU-009.1.1 — Navegar dominios dinamicos y relaciones entre memorias

**CA-074**  
**Given** una memoria con relaciones y dominios registrados  
**When** se consulta su contexto relacional  
**Then** el sistema devuelve al menos nodos y vinculos asociados.

**CA-075**  
**Given** una memoria sin relaciones registradas  
**When** se consulta su grafo  
**Then** el sistema responde sin error indicando ausencia de relaciones.

**CA-076**  
**Given** un dominio nuevo validado por negocio y ya registrado  
**When** se utiliza en relaciones o filtros del grafo  
**Then** el sistema lo reconoce como valor valido sin requerir redisenar el modelo funcional.

---

## 8. Cobertura funcional por tipo de escenario

| Tipo de escenario | Cobertura incluida |
|---|---|
| Positivos | Altas validas, consulta exitosa, busqueda con resultados, aprobacion, archivado, fusion, navegacion de grafo |
| Negativos | IDs inexistentes, payload invalido, campos obligatorios faltantes, transiciones no permitidas, errores de validacion, consultas sin acceso |
| Borde | Busqueda sin resultados, memoria sin relaciones, memoria archivada consultable por filtro, memoria en estado transitorio, cache con datos actualizados |

## 9. Fuera de alcance explicito

- Eliminacion definitiva de memorias (**R3**).
- Enriquecimiento automatico avanzado de relaciones (**R3**).
- Extraccion avanzada solo para casos criticos (**R3**).
- Configuracion avanzada de criterios de criticidad (**R3**).
- UI dedicada de operacion.
- Modelo granular de permisos por memoria.

## 10. Observaciones para QA/UAT

1. Los criterios anteriores estan redactados para permitir diseno directo de casos de prueba funcionales.
2. Toda prueba de aprobacion debe validar:
   - resultado observable,
   - estado final de la memoria,
   - trazabilidad registrada,
   - consistencia de respuesta API.
3. Cuando exista discrepancia de nomenclatura entre documentos de Fase 0, QA/UAT debe validar primero el **comportamiento funcional esperado**, no la forma tecnica final del endpoint.
