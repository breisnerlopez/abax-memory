# Product Backlog Priorizado
- **Fase**: 0-Descubrimiento
- **Entregable**: Product Backlog Priorizado
- **Responsable**: business-analyst
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## Objetivo
Priorizar el backlog inicial de PMOA - Plataforma de Memoria Operativa para Agentes, ordenando historias por valor de negocio y esfuerzo para definir el MVP minimo de salida a produccion inicial.

## Criterios de priorizacion aplicados
- **Valor de negocio**: capacidad de habilitar memoria operativa reutilizable, auditable y explotable por API.
- **Esfuerzo**: complejidad funcional relativa estimada en **S / M / L**.
- **Dependencias**: historias habilitadoras para ingestion, consulta y gobierno operativo.
- **Riesgo**: prioridad mayor para capacidades que reducen riesgo operativo o de calidad de memoria.

## Alcance
### Incluye
- Flujo completo backend/API-first para crear, versionar, validar, buscar y depurar memorias operativas.
- Integracion con Git como interfaz operativa principal.
- Persistencia y consulta sobre PostgreSQL, Qdrant, Neo4j y Redis.
- Soporte de memorias en Markdown con frontmatter.

### No incluye
- UI dedicada para usuarios finales en el MVP.
- Modelo avanzado de visibilidad por roles o permisos granulares.
- Extraccion avanzada generalizada para todos los casos.
- Automatizacion total de aprobacion de memorias criticas sin revision humana.

## Dashboard de backlog priorizado
| ID | Historia | Epica | Prioridad | Esfuerzo | Release |
|---|---|---|---|---|---|
| HU-001.1.1 | Como sistema, quiero registrar memorias en Markdown con frontmatter estandar para asegurar un formato reutilizable y auditable. | EP-001 Gestion de memorias | Must | M | R1-MVP |
| HU-001.1.2 | Como usuario operativo, quiero crear memorias desde un caso para capturar aprendizaje reutilizable desde la operacion. | EP-001 Gestion de memorias | Must | M | R1-MVP |
| HU-001.1.3 | Como usuario operativo, quiero crear memorias manuales para documentar conocimiento no originado en un caso. | EP-001 Gestion de memorias | Must | S | R1-MVP |
| HU-001.1.4 | Como sistema, quiero versionar cada memoria en Git para contar con trazabilidad y auditoria de cambios. | EP-001 Gestion de memorias | Must | M | R1-MVP |
| HU-002.1.1 | Como consumidor API, quiero publicar memorias mediante endpoints REST para operar el producto sin UI dedicada. | EP-002 API operativa | Must | M | R1-MVP |
| HU-002.1.2 | Como consumidor API, quiero consultar el detalle de una memoria por identificador para reutilizarla en flujos automatizados. | EP-002 API operativa | Must | S | R1-MVP |
| HU-002.1.3 | Como consumidor API, quiero listar memorias con filtros basicos por tipo, estado y origen para administrar el repositorio operativo. | EP-002 API operativa | Must | M | R1-MVP |
| HU-003.1.1 | Como sistema, quiero generar embeddings con text-embedding-3-large para habilitar busqueda semantica sobre memorias. | EP-003 Busqueda y recuperacion | Must | M | R1-MVP |
| HU-003.1.2 | Como usuario operativo, quiero buscar memorias semanticamente para encontrar antecedentes utiles aunque no coincidan exactamente las palabras. | EP-003 Busqueda y recuperacion | Must | M | R1-MVP |
| HU-003.1.3 | Como usuario operativo, quiero combinar busqueda semantica con filtros estructurados para acotar resultados relevantes. | EP-003 Busqueda y recuperacion | Must | M | R1-MVP |
| HU-004.1.1 | Como sistema, quiero almacenar metadatos estructurados de memorias en PostgreSQL para soportar consulta, auditoria y control operativo. | EP-004 Persistencia y metadatos | Must | M | R1-MVP |
| HU-004.1.2 | Como sistema, quiero indexar embeddings en Qdrant para soportar recuperacion vectorial de memorias. | EP-004 Persistencia y metadatos | Must | M | R1-MVP |
| HU-004.1.3 | Como sistema, quiero representar dominios y relaciones en Neo4j para soportar navegacion y clasificacion dinamica. | EP-004 Persistencia y metadatos | Should | L | R2 |
| HU-004.1.4 | Como sistema, quiero usar Redis para cachear consultas frecuentes y reducir latencia operativa. | EP-004 Persistencia y metadatos | Should | M | R2 |
| HU-005.1.1 | Como sistema, quiero clasificar memorias por tipos de memoria operativa reutilizable para mantener consistencia funcional. | EP-005 Gobierno de memoria | Must | M | R1-MVP |
| HU-005.1.2 | Como sistema, quiero ejecutar extraccion mini estructurada al crear una memoria para enriquecer metadatos de manera costo-eficiente. | EP-005 Gobierno de memoria | Must | M | R1-MVP |
| HU-005.1.3 | Como responsable de calidad, quiero marcar memorias criticas para que requieran validacion humana por PR manual antes de quedar disponibles. | EP-005 Gobierno de memoria | Must | M | R1-MVP |
| HU-005.1.4 | Como sistema, quiero mantener estados de memoria (borrador, en revision, aprobada, archivada) para controlar su ciclo de vida. | EP-005 Gobierno de memoria | Must | M | R1-MVP |
| HU-006.1.1 | Como usuario operativo, quiero archivar memorias obsoletas para retirarlas del uso activo sin perder trazabilidad. | EP-006 Depuracion y mantenimiento | Must | S | R1-MVP |
| HU-006.1.2 | Como usuario operativo, quiero marcar memorias duplicadas para reducir ruido y evitar reutilizacion incorrecta. | EP-006 Depuracion y mantenimiento | Should | S | R2 |
| HU-006.1.3 | Como usuario operativo, quiero fusionar memorias relacionadas para consolidar conocimiento operativo. | EP-006 Depuracion y mantenimiento | Should | M | R2 |
| HU-006.1.4 | Como usuario autorizado por acceso al repo, quiero eliminar memorias cuando deban retirarse definitivamente del conjunto operativo. | EP-006 Depuracion y mantenimiento | Could | M | R3 |
| HU-007.1.1 | Como equipo operativo, quiero que cualquier persona con acceso al repositorio vea todo el contenido para simplificar el modelo de visibilidad inicial. | EP-007 Acceso y visibilidad | Must | S | R1-MVP |
| HU-007.1.2 | Como equipo operativo, quiero registrar quien crea y modifica cada memoria para mantener responsabilidad auditable aun con visibilidad simple. | EP-007 Acceso y visibilidad | Must | S | R1-MVP |
| HU-008.1.1 | Como consumidor API, quiero disponer de documentacion de endpoints con metodo, path y payload JSON para integrar el backend desde otros sistemas. | EP-008 Contrato API | Must | S | R1-MVP |
| HU-008.1.2 | Como consumidor API, quiero recibir respuestas consistentes de error y validacion para operar el backend de manera predecible. | EP-008 Contrato API | Must | M | R1-MVP |
| HU-008.1.3 | Como operador de plataforma, quiero exponer endpoints de salud basicos para verificar disponibilidad de la API y sus dependencias principales. | EP-008 Contrato API | Should | S | R2 |
| HU-009.1.1 | Como usuario experto, quiero navegar dominios dinamicos y relaciones entre memorias para descubrir conocimiento conectado. | EP-009 Grafo de conocimiento | Should | L | R2 |
| HU-009.1.2 | Como sistema, quiero enriquecer automaticamente relaciones entre memorias y dominios para mejorar descubrimiento semantico. | EP-009 Grafo de conocimiento | Could | L | R3 |
| HU-010.1.1 | Como responsable de operacion, quiero aplicar extraccion avanzada solo en casos muy criticos para mejorar precision donde el costo se justifica. | EP-010 Enriquecimiento avanzado | Could | L | R3 |
| HU-010.1.2 | Como responsable de producto, quiero configurar criterios para considerar un caso como critico y activar procesamiento avanzado. | EP-010 Enriquecimiento avanzado | Could | M | R3 |

## Releases sugeridas

### R1-MVP
Objetivo: lanzar a produccion inicial un backend operativo de memoria con flujo completo de alta, versionado, validacion, busqueda y administracion basica sin UI dedicada.

Incluye:
- Gestion de memorias en Markdown + frontmatter.
- Alta desde caso y alta manual.
- Versionado y auditoria en Git.
- API REST para crear, consultar y listar.
- Metadatos en PostgreSQL.
- Embeddings y busqueda semantica en Qdrant.
- Filtros estructurados combinados.
- Tipificacion de memoria operativa.
- Extraccion mini estructurada.
- Validacion humana por PR para memorias criticas.
- Estados de ciclo de vida.
- Archivado.
- Visibilidad simple y trazabilidad de autor/modificador.
- Documentacion de contratos API y manejo consistente de errores.

### R2
Objetivo: optimizar recuperacion, gobierno y navegacion del conocimiento.

Incluye:
- Dominios dinamicos y relaciones en Neo4j.
- Cache con Redis.
- Marcado de duplicadas.
- Fusion de memorias.
- Endpoints de salud.
- Navegacion por grafo.

### R3
Objetivo: ampliar capacidades avanzadas y operacion selectiva de alto valor.

Incluye:
- Eliminacion definitiva.
- Enriquecimiento automatico avanzado de relaciones.
- Extraccion avanzada solo para casos criticos.
- Configuracion de criterios de criticidad.

## MVP minimo para produccion inicial
El **MVP** queda conformado por todas las historias **Must** de **R1-MVP**.

### Por que este conjunto es el MVP
Este conjunto es el minimo viable porque habilita el ciclo funcional extremo a extremo requerido por el negocio:
1. **Crear memoria** desde caso o manualmente.
2. **Persistirla y versionarla** de forma auditable en Git y metadatos estructurados.
3. **Validarla** cuando sea critica mediante PR manual.
4. **Recuperarla** por API mediante busqueda semantica y filtros.
5. **Administrarla** con estados y archivado sin depender de una UI dedicada.

Sin estas capacidades no existe una plataforma operativa usable en produccion inicial. En cambio, capacidades como grafo avanzado, cache, fusion de memorias o extraccion avanzada mejoran eficiencia y sofisticacion, pero no son imprescindibles para el primer lanzamiento.

## MoSCoW consolidado

### Must
- Registro de memorias en Markdown con frontmatter.
- Alta desde caso y alta manual.
- Versionado en Git.
- API REST de alta, consulta y listado.
- Embeddings y busqueda semantica con filtros estructurados.
- Persistencia de metadatos en PostgreSQL e indice vectorial en Qdrant.
- Tipificacion de memorias.
- Extraccion mini estructurada.
- Validacion humana por PR para memorias criticas.
- Estados de ciclo de vida.
- Archivado.
- Visibilidad simple.
- Trazabilidad de creador/modificador.
- Documentacion API y errores consistentes.

### Should
- Dominios dinamicos y relaciones en Neo4j.
- Cache con Redis.
- Marcado de duplicadas.
- Fusion de memorias.
- Endpoints de salud.
- Navegacion por dominios y grafo.

### Could
- Eliminacion definitiva.
- Enriquecimiento automatico avanzado de relaciones.
- Extraccion avanzada para casos criticos.
- Configuracion de criticidad para procesamiento avanzado.

### Won't en MVP
- UI dedicada.
- Visibilidad granular por roles/permisos.
- Extraccion avanzada generalizada para todos los casos.
- Aprobacion automatica de memorias criticas sin revision humana.

## Endpoints API funcionales esperados para el MVP
| Metodo | Path | Payload JSON | Proposito |
|---|---|---|---|
| POST | /api/memorias | `{ "titulo": "string", "tipo": "string", "origen": "caso|manual", "contenidoMarkdown": "string", "metadata": {} }` | Crear una memoria operativa |
| GET | /api/memorias/{id} | N/A | Consultar detalle de memoria |
| GET | /api/memorias | N/A | Listar memorias con filtros por tipo, estado y origen |
| POST | /api/memorias/{id}/aprobar | `{ "comentario": "string" }` | Aprobar memoria critica tras revision humana |
| POST | /api/memorias/{id}/archivar | `{ "motivo": "string" }` | Archivar memoria |
| POST | /api/busquedas/semantica | `{ "consulta": "string", "filtros": { "tipo": "string", "estado": "string", "origen": "string" } }` | Buscar memorias semanticamente con filtros |

> Nota: los endpoints anteriores reflejan necesidades funcionales del backlog y deben validarse con Product Owner antes de formalizar el contrato definitivo.
