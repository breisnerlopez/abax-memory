# Cronograma Preliminar
- **Fase**: 1-Inicio
- **Entregable**: Cronograma Preliminar
- **Responsable**: project-manager
- **Fecha**: 2026-05-01
- **Estado**: Completado

---

## Objetivo

Definir un cronograma preliminar en enfoque cascada para llevar PMOA / Abax-Memory desde Fase 1 hasta Fase 9, considerando un backend listo para produccion inicial con documentacion minima suficiente.

## Supuestos de planificacion

- Fase 0 aprobada y publicacion inicial en GitHub completada.
- Alcance inicial controlado, sin integraciones mayores no declaradas.
- Equipo base disponible: product-owner, business-analyst, tech-lead, backend-dev, qa-lead y devops.
- No se considera ampliacion de alcance sin control de cambios formal.
- QA completo es condicion obligatoria antes de liberar.

## Cronograma preliminar consolidado

| Fase | Nombre | Duracion estimada | Entregables principales | Dependencia principal |
|---|---|---:|---|---|
| 1 | Inicio | 1 semana | acta de inicio, cronograma base, registro inicial de riesgos | Fase 0 aprobada |
| 2 | Analisis funcional | 2 semanas | requerimientos, alcance detallado, criterios de aceptacion | Fase 1 |
| 3 | Diseno tecnico | 2 semanas | diseno tecnico, arquitectura, contratos API, modelo de datos | Fase 2 |
| 4 | Construccion | 5 semanas | backend implementado, pruebas unitarias, documentacion tecnica minima | Fase 3 |
| 5 | QA testing | 2 semanas | evidencia QA, defectos, reporte de regresion | Fase 4 |
| 6 | UAT | 1 semana | validacion negocio, observaciones y aprobacion UAT | Fase 5 |
| 7 | Deployment | 1 semana | plan de despliegue, pase a produccion, checklist y rollback | Fase 6 |
| 8 | Stabilization | 2 semanas | monitoreo, correccion post-produccion, cierre de incidentes iniciales | Fase 7 |
| 9 | Closure | 1 semana | cierre formal, lecciones aprendidas, acta de cierre | Fase 8 |

**Duracion total estimada:** 17 semanas calendario de trabajo.

## Cronograma con hitos y dependencias

```mermaid
gantt
    title PMOA / Abax-Memory - Cronograma preliminar Fase 1 a Fase 9
    dateFormat  YYYY-MM-DD
    axisFormat  %d-%b

    section Fase 1 - Inicio
    Plan de inicio y gobierno               :a1, 2026-05-04, 5d
    Hito: Inicio aprobado                   :milestone, m1, after a1, 0d

    section Fase 2 - Analisis funcional
    Levantamiento y alcance detallado       :a2, after m1, 10d
    Hito: Requerimientos aprobados          :milestone, m2, after a2, 0d

    section Fase 3 - Diseno tecnico
    Arquitectura y contratos API            :a3, after m2, 10d
    Hito: Diseno tecnico aprobado           :milestone, m3, after a3, 0d

    section Fase 4 - Construccion
    Implementacion backend MVP productivo   :a4, after m3, 25d
    Hito: Build candidata a QA              :milestone, m4, after a4, 0d

    section Fase 5 - QA testing
    QA funcional y regresion                :a5, after m4, 10d
    Hito: QA aprobado                       :milestone, m5, after a5, 0d

    section Fase 6 - UAT
    Validacion usuario/negocio              :a6, after m5, 5d
    Hito: UAT aprobado                      :milestone, m6, after a6, 0d

    section Fase 7 - Deployment
    Preparacion y despliegue a produccion   :a7, after m6, 5d
    Hito: Go-live inicial                   :milestone, m7, after a7, 0d

    section Fase 8 - Stabilization
    Monitoreo y soporte hiper care          :a8, after m7, 10d
    Hito: Operacion estabilizada            :milestone, m8, after a8, 0d

    section Fase 9 - Closure
    Cierre formal del proyecto              :a9, after m8, 5d
    Hito: Proyecto cerrado                  :milestone, m9, after a9, 0d
```

## Secuencia recomendada

1. Cerrar definicion de gobierno, supuestos y linea base en Fase 1.
2. Congelar requerimientos priorizados en Fase 2 antes de diseno tecnico.
3. Aprobar arquitectura, APIs y datos en Fase 3 antes de construir.
4. Ejecutar construccion completa del backend con pruebas unitarias en Fase 4.
5. Completar ciclo QA formal en Fase 5 sin saltar defectos criticos.
6. Obtener aprobacion de negocio en UAT antes de producir.
7. Desplegar con rollback definido y ventana controlada.
8. Mantener estabilizacion controlada antes del cierre administrativo.

## Dependencias clave

| ID | Dependencia | Tipo |
|---|---|---|
| D1 | Fase 2 inicia despues de aprobacion de Fase 1 | FS |
| D2 | Fase 3 inicia despues de aprobacion funcional | FS |
| D3 | Fase 4 inicia despues de diseno tecnico aprobado | FS |
| D4 | Fase 5 inicia con build candidata estable | FS |
| D5 | Fase 6 inicia solo con QA completado | FS |
| D6 | Fase 7 inicia solo con aprobacion UAT | FS |
| D7 | Fase 8 inicia luego de go-live | FS |
| D8 | Fase 9 inicia al cerrar incidencias criticas de estabilizacion | FS |

## Ruta critica preliminar

La ruta critica preliminar es:

**Fase 1 Inicio -> Fase 2 Analisis funcional -> Fase 3 Diseno tecnico -> Fase 4 Construccion -> Fase 5 QA testing -> Fase 6 UAT -> Fase 7 Deployment -> Fase 8 Stabilization -> Fase 9 Closure**

Actividades con menor holgura:

- aprobacion de requerimientos
- aprobacion del diseno tecnico
- cierre de construccion MVP
- cierre de defectos criticos en QA
- aprobacion UAT
- validacion post-deploy sin incidentes severos

## Hitos de control

| Hito | Fecha objetivo preliminar |
|---|---|
| Inicio aprobado | 2026-05-08 |
| Requerimientos aprobados | 2026-05-22 |
| Diseno tecnico aprobado | 2026-06-05 |
| Build candidata a QA | 2026-07-10 |
| QA aprobado | 2026-07-24 |
| UAT aprobado | 2026-07-31 |
| Go-live inicial | 2026-08-07 |
| Operacion estabilizada | 2026-08-21 |
| Proyecto cerrado | 2026-08-28 |

## Observaciones de gestion

- Este cronograma es preliminar y debe convertirse en linea base al cierre de Fase 1.
- Cualquier cambio de alcance posterior requiere evaluacion formal de impacto en tiempo, costo, calidad y riesgo.
- No se recomienda solapar QA, UAT y deployment de forma agresiva en esta etapa por tratarse de una salida inicial a produccion.
