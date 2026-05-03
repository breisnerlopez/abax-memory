# Matriz de Riesgos Inicial
- **Fase**: 1-Inicio
- **Entregable**: Matriz de Riesgos Inicial
- **Responsable**: project-manager
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## Objetivo

Identificar y priorizar los riesgos iniciales del proyecto PMOA / Abax-Memory para establecer acciones preventivas, responsables y mecanismos de monitoreo desde la fase de inicio.

## Criterios de evaluacion

| Valor | Probabilidad | Impacto |
|---|---|---|
| Alta | Alta posibilidad de ocurrencia en el corto plazo | Afecta alcance, tiempo, costo, calidad o salida a produccion |
| Media | Posible ocurrencia con condiciones controlables | Afecta parcialmente entregables o hitos |
| Baja | Baja posibilidad de ocurrencia | Impacto acotado y recuperable |

| Probabilidad | Impacto | Severidad |
|---|---|---|
| Alta | Alta | Critica |
| Alta | Media / Media | Alta |
| Alta | Baja / Baja | Media |
| Media | Alta | Alta |
| Media | Media | Media |
| Media | Baja / Baja | Baja |
| Baja | Alta | Media |
| Baja | Media / Baja | Baja |

## Matriz de Riesgos Inicial

| ID | Categoria | Descripcion del riesgo | Causa | Probabilidad | Impacto | Severidad | Plan de mitigacion | Contingencia | Responsable | Disparadores / indicadores |
|---|---|---|---|---|---|---|---|---|---|---|
| R-001 | Integracion | Fallas o latencia en integraciones con GitHub afectan flujos de validacion humana por PR | Dependencia de APIs externas, rate limits, cambios de permisos o configuracion de webhooks | Media | Alta | Alta | Definir contrato de integracion, validar autenticacion, monitorear rate limits, documentar reintentos y alertas | Operar aprobaciones manuales temporales y reprogramar automatizaciones no criticas | Tech Lead / DevOps | Errores 401/403/429, webhooks no recibidos, caida de sincronizacion de PR |
| R-002 | Tecnico | Degradacion de rendimiento o indisponibilidad de Qdrant impacta consultas vectoriales | Infraestructura insuficiente, mala configuracion de indices, crecimiento no estimado de embeddings | Media | Alta | Alta | Ejecutar pruebas de capacidad, definir sizing inicial, monitorear latencia y uso de recursos, establecer politicas de indexacion | Deshabilitar funciones no criticas de busqueda semantica y usar fallback transaccional | Tech Lead / DevOps | Latencia por encima de umbral, timeouts, aumento sostenido de CPU/RAM |
| R-003 | Integracion | Inestabilidad o errores de conexion con Neo4j afectan relaciones semanticas y trazabilidad | Configuracion incompleta, versionamiento no compatible, credenciales o red inestables | Media | Alta | Alta | Validar conectividad temprana, estandarizar drivers, definir pruebas smoke y observabilidad | Operar con capacidades parciales sin grafo y registrar reproceso posterior | Tech Lead | Fallas de conexion, errores de driver, consultas Cypher con timeout |
| R-004 | Datos | Inconsistencias entre PostgreSQL, Redis, Qdrant y Neo4j generan divergencia de informacion | Multiples fuentes con sincronizacion asincronica y ausencia de estrategia de consistencia | Alta | Alta | Critica | Definir fuente maestra por dominio, reglas de sincronizacion, trazabilidad de eventos y reconciliacion periodica | Ejecutar reconciliacion manual/automatizada y congelar cargas afectadas | Arquitectura / Tech Lead | Diferencias entre repositorios, registros huerfanos, lecturas contradictorias |
| R-005 | Operativo | Redis pierde cache critica o presenta configuracion deficiente afectando tiempos de respuesta | Evicciones, falta de persistencia requerida, configuracion de memoria no validada | Media | Media | Media | Definir estrategia de cache, TTLs, limites de memoria y pruebas de recuperacion | Invalidar cache completa y operar temporalmente con lectura directa a origen | Tech Lead / DevOps | Aumento de misses, evicciones elevadas, reinicios no planificados |
| R-006 | Seguridad | Exposicion de secretos, tokens o credenciales de servicios externos | Manejo inseguro de variables, repositorios, pipelines o accesos compartidos | Media | Alta | Alta | Gestionar secretos en vault o variables seguras, rotacion, minimo privilegio, escaneo de secretos | Revocar y rotar credenciales comprometidas, auditoria de accesos e incidente de seguridad | DevOps / Security | Hallazgo de secretos en repositorios, accesos no autorizados, alertas de escaneo |
| R-007 | Seguridad | Controles de acceso insuficientes permiten acceso indebido a memoria operativa o datos sensibles | Reglas RBAC incompletas, endpoints sin validacion consistente, pruebas de seguridad insuficientes | Media | Alta | Alta | Definir matriz de roles, pruebas de autorizacion, revisiones de seguridad y hardening previo a despliegue | Bloquear endpoints afectados y aplicar restriccion temporal por red o rol | Tech Lead / Security | Accesos inesperados, findings en pentest, tickets de autorizacion incorrecta |
| R-008 | Modelo IA | Baja calidad de embeddings o extraccion reduce precision de recuperacion y utilidad del backend | Seleccion inadecuada de modelos, datos de entrada ruidosos, falta de evaluacion objetiva | Alta | Alta | Critica | Definir metricas de calidad, dataset de evaluacion, pruebas comparativas de modelos y criterio de aceptacion | Revertir a modelo previo o desactivar experiencias IA de alto riesgo | Product Owner / Tech Lead | Caida de precision, feedback negativo de usuarios, baja relevancia en resultados |
| R-009 | Modelo IA | Drift del modelo o cambio de comportamiento por actualizaciones externas | Dependencia de proveedores/modelos versionados externamente y ausencia de baseline congelado | Media | Alta | Alta | Versionar modelos y prompts, congelar baseline, ejecutar pruebas de regresion IA antes de promover cambios | Rollback a version validada y bloqueo temporal de nuevas promociones | Tech Lead / MLOps | Diferencias relevantes contra baseline, aumento de falsos positivos/negativos |
| R-010 | Funcional | Requerimientos funcionales de memoria operativa, trazabilidad y validacion humana quedan ambiguos o incompletos | Fase inicial con definiciones parciales y dependencias entre negocio y tecnologia | Alta | Alta | Critica | Cerrar alcance funcional con BA/PO, criterios de aceptacion, casos de uso y exclusiones explicitas | Elevar decision a comite y congelar construccion de items ambiguos | Project Manager / Business Analyst | Historias contradictorias, dudas recurrentes, retrabajo en definiciones |
| R-011 | Adopcion | Baja adopcion del flujo de validacion humana por PR por friccion operativa del equipo | Proceso percibido como lento, sin guias, sin SLAs ni capacitacion | Media | Media | Media | Definir proceso simple, roles, tiempos objetivo, checklist y capacitacion inicial | Habilitar esquema escalonado de validacion priorizando cambios criticos | Project Manager / Change Manager | PRs sin validar, tiempos altos de aprobacion, rechazo del proceso por usuarios internos |
| R-012 | Operativo | Cuello de botella en revisiones humanas retrasa liberaciones y mantenimiento | Capacidad limitada de revisores y dependencia de personas clave | Alta | Media | Alta | Definir backup de revisores, calendario, priorizacion por criticidad y SLAs de revision | Reasignar capacidad y aplicar ventana de cambio controlada | Project Manager | Cola creciente de PRs, SLAs vencidos, dependencia reiterada de un unico aprobador |
| R-013 | Despliegue | Despliegue inicial a produccion falla por diferencias entre ambientes | Falta de paridad entre desarrollo, QA y produccion; configuraciones manuales | Media | Alta | Alta | Inventariar configuraciones, checklist de despliegue, smoke tests y gestion formal de ambientes | Rollback inmediato y extension de ventana de pase | DevOps | Errores solo en produccion, variables faltantes, incompatibilidad de versiones |
| R-014 | Datos | Carga inicial de datos incompleta, duplicada o de baja calidad afecta utilidad del sistema | Fuentes heterogeneas, reglas de limpieza no definidas, validaciones insuficientes | Alta | Alta | Critica | Definir reglas de calidad, validaciones previas, muestreo y trazabilidad de origen | Reprocesar lote, aislar dataset defectuoso y restringir consumo | Data Lead / Tech Lead | Duplicados, campos nulos criticos, rechazo de usuarios por calidad de resultados |
| R-015 | Seguridad | Incumplimiento de requisitos de auditoria y trazabilidad sobre cambios y accesos | Logs insuficientes, eventos no normalizados, retencion no definida | Media | Alta | Alta | Definir eventos auditables, retencion, correlacion por usuario/PR y revisiones periodicas | Implementar logging reforzado y controles compensatorios manuales | Tech Lead / Security | Imposibilidad de reconstruir acciones, hallazgos de auditoria, logs incompletos |
| R-016 | Tecnico | Dependencias open source o librerias de GitHub presentan vulnerabilidades o cambios incompatibles | Actualizaciones no controladas, paquetes sin version pinneada, CVEs activos | Alta | Alta | Critica | Establecer versionado controlado, escaneo de dependencias, politica de actualizacion y aprobacion | Congelar version segura y aplicar parche/rollback de emergencia | Tech Lead / DevOps | Alertas CVE, builds rotos por nuevas versiones, incompatibilidades en integracion |
| R-017 | Integracion | Fallo en orquestacion entre servicios de embeddings, extraccion y backend genera resultados parciales | Interfaces inestables, contratos no formalizados, manejo deficiente de errores | Media | Alta | Alta | Documentar contratos, timeouts, reintentos, circuit breakers y pruebas E2E tempranas | Reprocesar solicitudes fallidas y aislar modulo defectuoso | Tech Lead | Respuestas incompletas, errores intermitentes, colas de reproceso crecientes |
| R-018 | Operativo | Monitoreo y alertamiento insuficiente impide detectar fallas tempranamente | Observabilidad no definida desde el inicio, falta de umbrales y ownership | Media | Alta | Alta | Definir tablero minimo, logs, metricas, alertas y responsables por servicio | Activar monitoreo manual reforzado y guardias temporales | DevOps | Incidentes detectados por usuarios, ausencia de alertas, MTTR elevado |
| R-019 | Funcional | Criterios de exito para salida a produccion inicial no estan formalmente acordados | Presion por liberar rapido y ausencia de gate de aprobacion comun | Media | Alta | Alta | Definir checklist Go/No-Go con negocio, QA, seguridad y operaciones | Posponer pase hasta completar evidencias requeridas | Project Manager | Discusiones sobre si “ya esta listo”, aprobaciones verbales sin evidencia |
| R-020 | Adopcion | Stakeholders no tecnicos perciben baja confiabilidad del componente IA y frenan su uso | Resultados poco explicables, expectativas no alineadas y falta de comunicacion | Media | Media | Media | Gestionar expectativas, presentar casos de uso, limites, metricas y ejemplos de valor | Restringir uso a escenarios controlados mientras madura el modelo | Product Owner / Change Manager | Feedback de desconfianza, solicitudes de controles extra, baja demanda de uso |

## Riesgos prioritarios de atencion inmediata

1. **R-004**: Inconsistencias entre almacenes de datos.
2. **R-008**: Baja calidad de embeddings o extraccion.
3. **R-010**: Ambiguedad funcional en alcance inicial.
4. **R-014**: Calidad deficiente de carga inicial de datos.
5. **R-016**: Vulnerabilidades o incompatibilidades en dependencias.

## Recomendaciones iniciales de gestion

- Formalizar un comite de riesgos semanal durante Fase 1.
- Definir umbrales de severidad y mecanismo de escalamiento.
- Alinear responsables por categoria de riesgo con seguimiento quincenal.
- No autorizar salida a QA o produccion sin evidencia de mitigacion para riesgos criticos y altos.
- Mantener trazabilidad de cambios de alcance mediante control formal de cambios.

## Estado general

- **Total de riesgos identificados**: 20
- **Criticos**: 5
- **Altos**: 11
- **Medios**: 4
- **Bajos**: 0
- **Conclusión**: El proyecto es viable en Fase 1, pero requiere control temprano de integraciones, calidad de datos, gobierno de modelos IA, seguridad y definicion funcional para reducir riesgo de retrabajo y fallas en la salida inicial.
