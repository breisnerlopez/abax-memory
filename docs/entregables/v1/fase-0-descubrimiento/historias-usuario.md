# Historias de Usuario
- **Fase**: 0-Descubrimiento
- **Entregable**: Historias de Usuario
- **Responsable**: business-analyst
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## Alcance considerado

### Incluye
- MVP backend/API-first para gestion de memorias operativas.
- Flujos operativos basados en Git/GitHub.
- Memorias en Markdown con frontmatter.
- Creacion manual y creacion desde caso.
- Busqueda semantica con filtros estructurados.
- Relaciones y dominios dinamicos.
- Trazabilidad, depuracion y validacion humana para casos criticos.

### No incluye
- UI dedicada en el MVP.
- Orquestacion general de agentes.
- Aprobacion automatica de memorias criticas sin intervencion humana.
- Reglas fijas de dominio cerradas desde el diseno inicial.

---

## Feature 001. Creacion manual de memorias

### HU-001.1.1: Crear una memoria manualmente
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** crear una memoria operativa manualmente  
**Para** registrar conocimiento reutilizable aunque no provenga de un caso previo

**Criterios de aceptacion**:
- Dado un usuario autorizado y un payload valido, cuando solicita la creacion manual de una memoria, entonces el sistema genera un archivo Markdown con frontmatter obligatorio y contenido persistido en el repositorio.
- Dado un payload sin campos obligatorios de frontmatter, cuando se intenta crear la memoria, entonces el sistema rechaza la solicitud e informa los campos faltantes.
- Dado una memoria creada manualmente, cuando la operacion finaliza correctamente, entonces el sistema devuelve un identificador unico y la referencia del commit o cambio generado.

### HU-001.1.2: Registrar metadata minima obligatoria
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** informar metadata minima estandarizada al crear una memoria  
**Para** asegurar clasificacion, busqueda y trazabilidad consistentes

**Criterios de aceptacion**:
- Dado un intento de alta de memoria, cuando falta al menos un atributo obligatorio definido para el frontmatter, entonces el sistema no crea el archivo y devuelve el detalle de validacion.
- Dado una memoria con metadata completa, cuando se persiste, entonces la metadata queda almacenada en el frontmatter del Markdown con formato valido.

---

## Feature 002. Creacion de memorias desde casos

### HU-002.1.1: Crear memoria a partir de un caso
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** generar una memoria a partir de un caso existente  
**Para** convertir experiencia operativa en conocimiento reutilizable y trazable

**Criterios de aceptacion**:
- Dado un caso con identificador valido, cuando se solicita crear una memoria desde ese caso, entonces el sistema genera una memoria vinculada al caso de origen.
- Dado un caso origen vinculado, cuando se consulta la memoria creada, entonces la memoria muestra la referencia trazable al caso que la origino.
- Dado un identificador de caso inexistente o invalido, cuando se solicita la creacion, entonces el sistema rechaza la operacion y notifica el error.

### HU-002.1.2: Extraer estructura inicial desde un caso
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** que el sistema proponga una estructura inicial de memoria a partir del caso  
**Para** reducir esfuerzo manual y mejorar consistencia documental

**Criterios de aceptacion**:
- Dado un caso con informacion suficiente, cuando se inicia la extraccion, entonces el sistema genera una salida estructurada con los campos configurados para la memoria.
- Dado un caso con informacion incompleta, cuando se genera la propuesta, entonces el sistema crea un borrador con campos faltantes claramente identificados para revision humana.

---

## Feature 003. Validacion estructural de memorias Markdown

### HU-003.1.1: Validar formato Markdown con frontmatter
**Prioridad**: Must

**Como** responsable de calidad funcional  
**Quiero** validar que toda memoria cumpla la estructura Markdown + frontmatter  
**Para** evitar inconsistencias que afecten indexacion, busqueda y reutilizacion

**Criterios de aceptacion**:
- Dado un archivo de memoria candidato a registro, cuando su frontmatter y cuerpo cumplen el formato definido, entonces el sistema lo acepta para procesamiento.
- Dado un archivo con frontmatter mal formado o ilegible, cuando se procesa, entonces el sistema lo marca como invalido y registra el motivo.
- Dado un archivo con contenido pero sin frontmatter, cuando se intenta registrar, entonces el sistema rechaza el alta.

### HU-003.1.2: Detectar campos fuera del estandar
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** conocer si una memoria incluye campos no estandarizados  
**Para** corregir desviaciones antes de afectar la calidad del repositorio

**Criterios de aceptacion**:
- Dado una memoria con campos adicionales no reconocidos, cuando se valida, entonces el sistema informa cuales campos no forman parte del estandar.
- Dado una politica configurada para permitir campos extra, cuando la memoria se procesa, entonces el sistema la acepta pero deja evidencia del hallazgo para revision.

---

## Feature 004. Versionado y operacion con Git/GitHub

### HU-004.1.1: Persistir memorias en Git como interfaz operativa
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** que las memorias queden versionadas en Git  
**Para** contar con historial auditable de cambios

**Criterios de aceptacion**:
- Dado una memoria creada o actualizada correctamente, cuando se confirma la operacion, entonces el sistema registra el cambio en el repositorio Git.
- Dado una memoria versionada, cuando se consulta su historial, entonces es posible identificar al menos la referencia de version asociada al cambio.

### HU-004.1.2: Someter memorias criticas a revision humana por PR
**Prioridad**: Must

**Como** revisor humano  
**Quiero** que las memorias marcadas como criticas requieran PR manual  
**Para** evitar publicacion directa de conocimiento sensible o de alto impacto

**Criterios de aceptacion**:
- Dado una memoria marcada como critica, cuando se solicita su publicacion, entonces el sistema la deja en estado pendiente de revision y no la publica como aprobada automaticamente.
- Dado una memoria critica en revision, cuando un revisor humano la aprueba por el flujo definido, entonces la memoria queda disponible segun el proceso operativo establecido.
- Dado una memoria no critica, cuando se publica, entonces el sistema no exige PR manual por criticidad.

---

## Feature 005. Busqueda semantica de memorias

### HU-005.1.1: Buscar memorias por similitud semantica
**Prioridad**: Must

**Como** agente autorizado u operador  
**Quiero** buscar memorias por significado y no solo por coincidencia textual  
**Para** encontrar conocimiento reutilizable aun cuando cambie la redaccion

**Criterios de aceptacion**:
- Dado una consulta en lenguaje natural, cuando se ejecuta la busqueda semantica, entonces el sistema devuelve memorias ordenadas por relevancia.
- Dado una consulta sin coincidencias relevantes por encima del umbral configurado, cuando se ejecuta la busqueda, entonces el sistema devuelve un resultado vacio o controlado sin errores tecnicos.
- Dado una memoria indexada, cuando una consulta resulta semanticamente relacionada, entonces la memoria puede aparecer en los resultados aunque no comparta las mismas palabras exactas.

### HU-005.1.2: Buscar memorias combinando semantica y texto
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** combinar busqueda semantica con terminos textuales  
**Para** afinar resultados en casos de alto volumen o alta ambiguedad

**Criterios de aceptacion**:
- Dado una consulta con texto libre y terminos precisos, cuando se ejecuta la busqueda combinada, entonces el sistema aplica ambos criterios segun la configuracion definida.
- Dado una consulta combinada, cuando no hay resultados que cumplan los filtros textuales, entonces el sistema informa ausencia de resultados sin devolver registros fuera del criterio indicado.

---

## Feature 006. Filtros estructurados de consulta

### HU-006.1.1: Filtrar resultados por metadata
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** filtrar memorias por atributos estructurados  
**Para** acotar la busqueda a un contexto operativo especifico

**Criterios de aceptacion**:
- Dado una consulta con filtros validos de metadata, cuando se ejecuta la busqueda, entonces el sistema solo devuelve memorias que cumplan los filtros solicitados.
- Dado multiples filtros simultaneos, cuando se ejecuta la consulta, entonces el sistema aplica la combinacion configurada de filtros de forma consistente.
- Dado un filtro con valor no valido para el campo, cuando se envia la solicitud, entonces el sistema rechaza la peticion o informa el error de validacion.

### HU-006.1.2: Filtrar por estado de ciclo de vida
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** filtrar memorias por estado operativo  
**Para** excluir registros archivados, duplicados o eliminados de mis consultas normales

**Criterios de aceptacion**:
- Dado una consulta de memorias activas, cuando existen registros archivados o marcados como duplicados, entonces esos registros no aparecen salvo que se soliciten explicitamente.
- Dado una consulta filtrada por estado especifico, cuando se ejecuta, entonces todos los resultados devueltos pertenecen a ese estado.

---

## Feature 007. Dominios dinamicos

### HU-007.1.1: Asignar una memoria a dominios dinamicos
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** asociar una memoria a uno o varios dominios dinamicos  
**Para** clasificar conocimiento sin depender de una taxonomia fija cerrada

**Criterios de aceptacion**:
- Dado una memoria valida, cuando se la asocia a uno o varios dominios permitidos, entonces el sistema guarda la relacion para consulta posterior.
- Dado una memoria sin dominio asignado cuando el dominio es obligatorio por politica, cuando se intenta publicar, entonces el sistema impide continuar e informa la validacion.

### HU-007.1.2: Incorporar nuevos dominios sin redisenar el modelo funcional
**Prioridad**: Should

**Como** administrador funcional  
**Quiero** registrar nuevos dominios de negocio  
**Para** adaptar la clasificacion a la evolucion operativa

**Criterios de aceptacion**:
- Dado un nuevo dominio validado por negocio, cuando se registra, entonces puede usarse para clasificar nuevas memorias sin alterar las memorias existentes.
- Dado un dominio nuevo, cuando se utiliza en filtros y relaciones, entonces el sistema lo reconoce como un valor valido.

---

## Feature 008. Relaciones entre memorias y entidades

### HU-008.1.1: Relacionar memorias entre si
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** vincular memorias relacionadas  
**Para** navegar dependencias, contexto y reutilizacion de conocimiento

**Criterios de aceptacion**:
- Dado dos memorias existentes, cuando se registra una relacion valida entre ellas, entonces la relacion queda disponible para consulta.
- Dado una memoria relacionada con otras, cuando se consulta su detalle, entonces el sistema muestra las referencias relacionadas registradas.

### HU-008.1.2: Consultar relaciones en grafo
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** recuperar las relaciones de una memoria en forma navegable  
**Para** entender rapidamente su contexto operativo

**Criterios de aceptacion**:
- Dado una memoria con relaciones registradas, cuando se consulta su red de relaciones, entonces el sistema devuelve al menos los nodos y vinculos asociados.
- Dado una memoria sin relaciones, cuando se consulta su grafo, entonces el sistema responde sin error indicando ausencia de relaciones.

---

## Feature 009. Trazabilidad de origen y cambios

### HU-009.1.1: Consultar la trazabilidad completa de una memoria
**Prioridad**: Must

**Como** auditor funcional o revisor  
**Quiero** ver el origen y los cambios principales de una memoria  
**Para** validar su confiabilidad y contexto

**Criterios de aceptacion**:
- Dado una memoria creada desde un caso, cuando se consulta su trazabilidad, entonces el sistema muestra la referencia al caso origen.
- Dado una memoria modificada y versionada, cuando se consulta su trazabilidad, entonces el sistema muestra el historial de cambios disponibles en la interfaz operativa definida.
- Dado una memoria fusionada o archivada, cuando se consulta su trazabilidad, entonces el sistema conserva la evidencia de la accion realizada.

---

## Feature 010. Deteccion y gestion de duplicados

### HU-010.1.1: Detectar posibles memorias duplicadas
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** identificar memorias posiblemente duplicadas  
**Para** evitar dispersion e inconsistencias del conocimiento

**Criterios de aceptacion**:
- Dado una memoria nueva o actualizada, cuando el sistema detecta similitud por encima del umbral definido para duplicidad, entonces marca coincidencias potenciales para revision.
- Dado coincidencias por debajo del umbral definido, cuando se procesa la memoria, entonces el sistema no la marca como posible duplicada.

### HU-010.1.2: Marcar una memoria como duplicada
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** marcar una memoria como duplicada de otra memoria canonica  
**Para** mantener una unica referencia principal reutilizable

**Criterios de aceptacion**:
- Dado dos memorias existentes, cuando una se marca como duplicada de la otra, entonces la memoria duplicada queda asociada a la memoria canonica.
- Dado una memoria marcada como duplicada, cuando se realiza una consulta estandar de memorias activas, entonces la memoria duplicada no aparece salvo solicitud explicita.

---

## Feature 011. Fusion de memorias

### HU-011.1.1: Fusionar memorias relacionadas
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** fusionar memorias complementarias o redundantes  
**Para** consolidar conocimiento sin perder trazabilidad

**Criterios de aceptacion**:
- Dado dos o mas memorias elegibles para fusion, cuando se ejecuta la fusion, entonces el sistema genera o actualiza una memoria consolidada segun la politica definida.
- Dado una fusion completada, cuando se consulta la trazabilidad de las memorias origen, entonces el sistema conserva la referencia hacia la memoria resultante.
- Dado una solicitud de fusion con memorias inexistentes o no elegibles, cuando se procesa, entonces el sistema rechaza la operacion informando la causa.

---

## Feature 012. Archivado y eliminacion

### HU-012.1.1: Archivar una memoria
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** archivar memorias que ya no deben usarse operativamente  
**Para** depurar el repositorio sin perder el historial

**Criterios de aceptacion**:
- Dado una memoria existente, cuando se archiva, entonces cambia a estado archivado y deja de aparecer en consultas activas por defecto.
- Dado una memoria archivada, cuando se consulta con filtros que incluyan archivados, entonces la memoria sigue siendo recuperable.

### HU-012.1.2: Eliminar una memoria bajo control
**Prioridad**: Could

**Como** administrador autorizado  
**Quiero** eliminar una memoria de forma controlada  
**Para** retirar contenido invalido o no permitido

**Criterios de aceptacion**:
- Dado una memoria con permisos suficientes para eliminacion, cuando se confirma la accion, entonces el sistema la marca o retira segun la politica definida y deja evidencia auditable.
- Dado un usuario sin permisos suficientes, cuando intenta eliminar una memoria, entonces el sistema deniega la operacion.

---

## Feature 013. Criticidad y validacion reforzada

### HU-013.1.1: Marcar una memoria como critica
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** clasificar una memoria como critica  
**Para** activar controles adicionales antes de su uso operativo

**Criterios de aceptacion**:
- Dado una memoria que cumple criterios de criticidad definidos por negocio, cuando se la clasifica como critica, entonces el sistema registra dicho estado.
- Dado una memoria critica, cuando se consulta su detalle, entonces el sistema informa de forma visible su condicion de criticidad.

### HU-013.1.2: Aplicar validacion avanzada solo a memorias muy criticas
**Prioridad**: Should

**Como** revisor humano  
**Quiero** que la validacion avanzada se aplique solo a memorias muy criticas  
**Para** concentrar esfuerzo de control en los casos de mayor riesgo

**Criterios de aceptacion**:
- Dado una memoria clasificada como muy critica segun la regla de negocio vigente, cuando entra al flujo de validacion, entonces el sistema activa la validacion avanzada correspondiente.
- Dado una memoria no clasificada como muy critica, cuando se procesa, entonces el sistema no exige validacion avanzada por defecto.

---

## Feature 014. API-first para operacion del repositorio de memorias

### HU-014.1.1: Operar memorias via API REST
**Prioridad**: Must

**Como** sistema consumidor o automatizacion autorizada  
**Quiero** crear, consultar, actualizar y cambiar el estado de memorias mediante API  
**Para** integrar la plataforma con flujos operativos sin depender de una UI dedicada

**Criterios de aceptacion**:
- Dado un consumidor autorizado, cuando invoca un endpoint valido, entonces el sistema responde con el codigo HTTP y payload JSON acordados.
- Dado una solicitud invalida, cuando se invoca el endpoint correspondiente, entonces el sistema devuelve un error de validacion consistente y verificable.

### HU-014.1.2: Consultar contratos REST documentados
**Prioridad**: Must

**Como** consumidor de la API  
**Quiero** disponer de contratos claros de endpoints, metodos y payloads  
**Para** integrar la solucion con menor ambiguedad

**Criterios de aceptacion**:
- Dado el conjunto de operaciones del MVP, cuando se consulta la documentacion funcional, entonces existe al menos un contrato por endpoint con metodo HTTP, path, request y response.
- Dado un cambio aprobado en un endpoint, cuando se actualiza el contrato, entonces la documentacion refleja el comportamiento vigente.

**Endpoints funcionales esperados para el MVP**:
- `POST /api/memorias` — crear memoria manual.
- `POST /api/memorias/desde-caso` — crear memoria desde caso.
- `GET /api/memorias/{id}` — consultar memoria.
- `POST /api/memorias/busqueda` — busqueda semantica con filtros.
- `PATCH /api/memorias/{id}` — actualizar contenido o metadata.
- `PATCH /api/memorias/{id}/estado` — archivar, marcar duplicada o eliminar segun politica.
- `POST /api/memorias/{id}/relaciones` — registrar relaciones.
- `POST /api/memorias/{id}/fusion` — fusionar memorias.

**Payloads JSON funcionales de referencia**:

```json
{
  "titulo": "Procedimiento de regularizacion operativa",
  "dominios": ["operaciones", "regularizacion"],
  "criticidad": "alta",
  "contenidoMarkdown": "# Resumen\n...",
  "metadata": {
    "fuente": "manual",
    "autor": "usuario.operativo"
  }
}
```

```json
{
  "caseId": "CASO-12345",
  "criticidad": "media",
  "dominios": ["cobranzas"],
  "forzarRevisionHumana": false
}
```

```json
{
  "query": "como regularizar una incidencia de cobranza",
  "topK": 10,
  "filtros": {
    "dominios": ["cobranzas"],
    "estado": ["activa"],
    "criticidad": ["media", "alta"]
  }
}
```

---

## Feature 015. Estado de procesamiento e indexacion

### HU-015.1.1: Conocer si una memoria ya esta disponible para consulta
**Prioridad**: Must

**Como** operador de conocimiento  
**Quiero** conocer el estado de procesamiento e indexacion de una memoria  
**Para** saber cuando ya puede ser reutilizada en busquedas

**Criterios de aceptacion**:
- Dado una memoria recien creada, cuando aun no termino su procesamiento, entonces el sistema informa un estado transitorio verificable.
- Dado una memoria ya indexada, cuando se consulta su estado, entonces el sistema informa que esta disponible para busqueda.
- Dado una falla de procesamiento, cuando se consulta el estado, entonces el sistema informa que la memoria no quedo disponible e indica la causa registrada.

---

## Feature 016. Reprocesamiento cercano al MVP

### HU-016.1.1: Reindexar una memoria modificada
**Prioridad**: Should

**Como** operador de conocimiento  
**Quiero** reprocesar una memoria luego de una correccion relevante  
**Para** asegurar que su version vigente sea la usada por la busqueda y trazabilidad

**Criterios de aceptacion**:
- Dado una memoria actualizada, cuando se solicita su reprocesamiento, entonces el sistema vuelve a indexar la version vigente.
- Dado un reprocesamiento exitoso, cuando se realiza una nueva consulta, entonces los resultados reflejan la ultima version disponible.

---

## Historias fuera de alcance del MVP inmediato

### HU-017.1.1: Administrar memorias desde una UI dedicada
**Prioridad**: Won't

**Como** usuario operativo  
**Quiero** gestionar memorias desde una interfaz web dedicada  
**Para** no depender de APIs ni flujos Git

**Criterios de aceptacion**:
- Dado el alcance actual del MVP, cuando se revisa el plan de entrega, entonces esta historia se mantiene fuera de implementacion inicial.
- Dado una evaluacion futura aprobada, cuando el negocio priorice una UI dedicada, entonces esta historia podra reconsiderarse como nuevo alcance.
