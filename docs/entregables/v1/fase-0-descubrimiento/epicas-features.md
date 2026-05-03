# Mapa de Epicas y Features
- **Fase**: 0-Descubrimiento
- **Entregable**: Mapa de Epicas y Features
- **Responsable**: business-analyst
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## EP-001: Gestion de Casos Operativos (MVP)
Descripcion: Permite registrar el caso operativo que origina una memoria, darle contexto minimo y mantener su trazabilidad hasta el cierre.
### Features:
- FT-001.1: Creacion de caso — Alta de casos con identificador unico, origen, titulo, descripcion y estado inicial.
- FT-001.2: Metadatos operativos del caso — Registro de prioridad, dominio, criticidad, etiquetas y responsables participantes.
- FT-001.3: Casos nacidos sin memoria previa — Soporte para iniciar atencion aunque todavia no exista una memoria relacionada.
- FT-001.4: Cierre de caso con resultado — Registro del desenlace operativo y vinculacion con la memoria generada o reutilizada.

## EP-002: Modelo Canonico de Memoria (MVP)
Descripcion: Define la estructura estandar de una memoria operativa en Markdown + frontmatter para que pueda ser creada, leida, validada y reutilizada de forma consistente.
### Features:
- FT-002.1: Plantilla base de memoria — Estructura canonica con frontmatter obligatorio y secciones Markdown reutilizables.
- FT-002.2: Tipos de origen de memoria — Soporte para memorias creadas desde un caso y memorias creadas manualmente.
- FT-002.3: Metadatos obligatorios — Campos minimos para dominio, estado, fuentes, fecha, version, criticidad y relaciones.
- FT-002.4: Reglas de nomenclatura y ubicacion — Convenciones para nombre de archivo, rutas y organizacion en el repositorio unico.

## EP-003: Captura y Generacion de Memoria (MVP)
Descripcion: Permite convertir conocimiento operativo en una memoria persistible a partir de insumos estructurados o semiestructurados.
### Features:
- FT-003.1: Creacion manual de memoria — Alta directa de memorias sin depender de un caso previo.
- FT-003.2: Generacion de memoria desde caso — Construccion de un borrador de memoria usando el contexto y resultado del caso.
- FT-003.3: Borrador editable antes de persistir — Estado intermedio para revisar y ajustar contenido antes de publicarlo.
- FT-003.4: Version inicial controlada — Emision de la primera version formal de la memoria al aprobarse su publicacion.

## EP-004: Clasificacion y Extraccion Estructurada (MVP)
Descripcion: Organiza automaticamente el conocimiento capturado para que quede utilizable en busqueda, validacion y relacionamiento.
### Features:
- FT-004.1: Clasificacion por dominio y tipo — Asignacion de categoria funcional y naturaleza del conocimiento.
- FT-004.2: Extraccion estructurada minima — Obtencion de entidades, pasos, decisiones, evidencias y resultados mediante modelo mini estructurado.
- FT-004.3: Etiquetado operativo — Generacion o asignacion de tags para facilitar filtros posteriores.
- FT-004.4: Identificacion de criticidad — Marcado de memorias que requieren controles adicionales de validacion.

## EP-005: Recuperacion de Memoria y Reutilizacion (MVP)
Descripcion: Permite localizar memorias relevantes con precision para reutilizar conocimiento existente antes de generar nuevo contenido.
### Features:
- FT-005.1: Busqueda semantica — Recuperacion por similitud usando embeddings del contenido de memoria.
- FT-005.2: Filtros estructurados — Filtrado por dominio, etiquetas, estado, criticidad, fecha, origen y tipo.
- FT-005.3: Vista de coincidencias relevantes — Entrega de resultados con resumen, score y metadatos clave.
- FT-005.4: Reutilizacion sobre caso activo — Asociacion de memorias recuperadas a un caso en atencion para apoyar su resolucion.

## EP-006: Relacionamiento de Conocimiento y Grafo de Dominios (MVP)
Descripcion: Permite conectar memorias, casos, dominios y entidades para reflejar dependencias y reutilizacion cruzada.
### Features:
- FT-006.1: Relaciones entre memorias — Vinculos como relacionada-con, complementa, reemplaza o depende-de.
- FT-006.2: Dominios dinamicos en grafo — Alta y relacionamiento flexible de dominios sin catalogo rigido.
- FT-006.3: Vinculacion caso-memoria — Relacion entre el caso origen, memorias consultadas y memoria resultante.
- FT-006.4: Navegacion por contexto relacionado — Consulta de conexiones relevantes para ampliar el entendimiento operativo.

## EP-007: Validacion y Aprobacion de Memorias (MVP)
Descripcion: Asegura que una memoria cumpla criterios minimos de calidad antes de quedar disponible como conocimiento operativo reutilizable.
### Features:
- FT-007.1: Validacion estructural automatica — Verificacion de frontmatter, campos obligatorios y formato esperado.
- FT-007.2: Validacion funcional basica — Confirmacion de consistencia minima entre contenido, clasificacion y relaciones declaradas.
- FT-007.3: Flujo de aprobacion manual para memorias criticas — Requerimiento de revision humana mediante PR antes de publicar memorias criticas.
- FT-007.4: Estados de validacion — Manejo de borrador, validada, observada, aprobada, archivada o rechazada.

## EP-008: Persistencia Git y Versionado Operativo (MVP)
Descripcion: Usa Git/GitHub como interfaz operativa inicial para almacenar, versionar y auditar el conocimiento del repositorio unico de memorias.
### Features:
- FT-008.1: Repositorio unico de memorias — Gestion centralizada de todo el conocimiento operativo del MVP.
- FT-008.2: Commit de altas y cambios — Persistencia versionada de creaciones, actualizaciones, fusiones y archivados.
- FT-008.3: Flujo Git completo sin UI dedicada — Operacion backend para crear, actualizar y promover memorias usando flujos Git.
- FT-008.4: Compatibilidad inicial con GitHub — Integracion operativa sobre el proveedor inicial definido para el MVP.

## EP-009: Atencion Operativa Multiagente (MVP)
Descripcion: Registra el trabajo colaborativo de multiples agentes sobre un caso sin incluir una capa completa de orquestacion.
### Features:
- FT-009.1: Registro de agentes participantes — Identificacion de los agentes que intervienen en un caso.
- FT-009.2: Trazabilidad de aportes por agente — Historial de contribuciones, hallazgos o acciones relevantes durante la atencion.
- FT-009.3: Consolidacion de resolucion — Integracion de aportes para emitir una memoria final o actualizar una existente.
- FT-009.4: Asociacion de memorias consultadas y generadas — Relacion entre agentes, caso y conocimiento reutilizado o creado.

## EP-010: Depuracion y Ciclo de Vida de Memorias (MVP)
Descripcion: Permite mantener la calidad del repositorio mediante controles de archivado, deteccion de redundancias y saneamiento del conocimiento.
### Features:
- FT-010.1: Archivado de memorias — Retiro controlado de memorias sin eliminarlas del historial.
- FT-010.2: Deteccion de posibles duplicadas — Identificacion de memorias candidatas a fusion o revision.
- FT-010.3: Fusion de memorias — Consolidacion de contenido manteniendo trazabilidad de origen.
- FT-010.4: Eliminacion controlada — Baja excepcional de memorias bajo reglas de auditoria y autorizacion.

## EP-011: Auditoria y Trazabilidad de Conocimiento (MVP)
Descripcion: Hace verificable el ciclo completo del conocimiento desde su origen hasta su reutilizacion, validacion y depuracion.
### Features:
- FT-011.1: Historial de cambios de memoria — Registro de quien, cuando y que se modifico.
- FT-011.2: Trazabilidad extremo a extremo — Relacion entre caso, clasificacion, validacion, persistencia y cierre.
- FT-011.3: Evidencias de validacion y aprobacion — Conservacion de observaciones, decisiones y aprobaciones realizadas.
- FT-011.4: Registro de acciones de depuracion — Trazas de archivado, fusion, duplicidad y eliminacion.

## EP-012: Plataforma API-First y Servicios de Nucleo (MVP)
Descripcion: Expone capacidades del nucleo de memoria mediante APIs backend consumibles por flujos operativos presentes y futuros.
### Features:
- FT-012.1: API de casos — Endpoints para crear, consultar, actualizar y cerrar casos.
- FT-012.2: API de memorias — Endpoints para crear, consultar, actualizar, validar, archivar y relacionar memorias.
- FT-012.3: API de busqueda y recuperacion — Endpoints para busqueda semantica, filtros y consulta de relaciones.
- FT-012.4: API de auditoria y depuracion — Endpoints para consultar historial y ejecutar acciones controladas de saneamiento.

## EP-013: Portabilidad Git y Abstraccion de Proveedor (Post-MVP)
Descripcion: Reduce dependencia operativa de GitHub y prepara la evolucion hacia otros proveedores o despliegues equivalentes.
### Features:
- FT-013.1: Capa abstracta de proveedor Git — Separacion entre el nucleo de memoria y la implementacion especifica del proveedor.
- FT-013.2: Compatibilidad con multiples proveedores — Soporte futuro para variantes Git compatibles ademas de GitHub.
- FT-013.3: Configuracion portable por entorno — Parametrizacion de credenciales, repositorios y politicas por ambiente.

## EP-014: Validacion Avanzada para Casos Criticos (Post-MVP)
Descripcion: Incorpora controles mas profundos solo donde el riesgo operativo justifica mayor costo de validacion.
### Features:
- FT-014.1: Reglas reforzadas por criticidad — Criterios diferenciados para memorias de alto impacto.
- FT-014.2: Segunda validacion asistida — Revision adicional sobre consistencia, completitud y evidencia.
- FT-014.3: Bloqueos de publicacion por riesgo — Impedimento de liberacion cuando una memoria critica no cumple umbrales definidos.

## EP-015: Experiencia Operativa con UI Dedicada (Post-MVP)
Descripcion: Agrega una interfaz especializada para operar la plataforma sin depender exclusivamente de flujos Git y consumo directo de API.
### Features:
- FT-015.1: Consola de casos y memorias — Vista unificada para operar el ciclo de vida del conocimiento.
- FT-015.2: Exploracion visual del grafo — Navegacion grafica de relaciones entre dominios, casos y memorias.
- FT-015.3: Bandeja de validacion y depuracion — Gestion visual de PR, observaciones, duplicadas y archivados.

## EP-016: Escalamiento de Repositorios y Gobierno Avanzado (Post-MVP)
Descripcion: Extiende el modelo operativo cuando el crecimiento del conocimiento requiera segmentacion y controles adicionales.
### Features:
- FT-016.1: Multiples repositorios de memorias — Soporte para particionar el conocimiento por dominio, negocio o region.
- FT-016.2: Politicas de gobierno por segmento — Reglas diferenciadas de aprobacion, retencion y acceso.
- FT-016.3: Federacion de busqueda — Consulta transversal sobre varios repositorios manteniendo trazabilidad.
