---
fase: 1-Inicio
entregable: Registro de Interesados (Stakeholder Register)
version: 2.1.0
responsable: project-manager
fecha: 2026-05-05
estado: Completado
aprobado-por: pendiente (gate F1)
dependencias:
  - docs/entregables/v2.1/fase-1-inicio/acta-de-constitucion.md
relacion-release-anterior: docs/entregables/v2/fase-1-inicio/
---

# Registro de Interesados — Abax-Memory v2.1.0
## "Hardening & Optimización del Motor de Memoria Multi-Dominio"

---

## Tabla de Contenidos

- [1. Introducción y Propósito](#1-introducción-y-propósito)
- [2. Contexto del Proyecto v2.1.0](#2-contexto-del-proyecto-v210)
- [3. Identificación de Interesados](#3-identificación-de-interesados)
  - [3.1 Sponsor / Product Owner (Usuario)](#31-sponsor--product-owner-usuario)
  - [3.2 Project Manager](#32-project-manager)
  - [3.3 Business Analyst](#33-business-analyst)
  - [3.4 Solution Architect](#34-solution-architect)
  - [3.5 Tech Lead](#35-tech-lead)
  - [3.6 Developer Backend](#36-developer-backend)
  - [3.7 Developer Frontend](#37-developer-frontend)
  - [3.8 QA Lead / QA Functional](#38-qa-lead--qa-functional)
  - [3.9 DevOps](#39-devops)
- [4. Matriz de Poder/Interés](#4-matriz-de-poderinterés)
- [5. Plan de Comunicación](#5-plan-de-comunicación)
- [6. Matriz de Participación por Fase](#6-matriz-de-participación-por-fase)
- [7. Estrategias de Gestión por Cuadrante](#7-estrategias-de-gestión-por-cuadrante)
- [8. Escalamiento](#8-escalamiento)
- [9. Control de Cambios en el Registro](#9-control-de-cambios-en-el-registro)
- [Glosario](#glosario)

---

## 1. Introducción y Propósito

El **Registro de Interesados** documenta formalmente a todas las personas y roles que influyen, son afectados o tienen interés en el proyecto **Abax-Memory v2.1.0**. Este registro es un instrumento vivo de gobernanza que:

- Identifica a cada interesado con claridad inequívoca (rol, tipo, influencia, interés).
- Define expectativas explícitas y responsabilidades para prevenir ambigüedades.
- Establece una estrategia de comunicación adaptada a cada perfil.
- Sirve como referencia única para la ejecución del plan de comunicación y la gestión de expectativas a lo largo de las 10 fases del ciclo cascada (F0–F9).

Este documento complementa y expande la sección 6 de la [Acta de Constitución](./acta-de-constitucion.md), que presenta un resumen tabular. Aquí se despliega el perfil completo de cada interesado.

---

## 2. Contexto del Proyecto v2.1.0

| Campo | Valor |
|---|---|
| **Versión** | v2.1.0 |
| **Tipo de iteración** | Hardening y optimización (no re-arquitectura) |
| **Predecesor** | v2.0.9 (cerrado 2026-05-05) |
| **Alcance** | 13 features en 4 épicas: Precisión, Velocidad, Eficiencia, API/DX |
| **Equipo** | 9 agentes |
| **Metodología** | Cascada completa (F0 → F9) con gates formales |
| **Fase actual** | F1 — Inicio / Análisis Funcional (en curso) |
| **Stack** | Quarkus 3.15.3, Java 21, PostgreSQL 16.13, Qdrant 1.17.1, Keycloak 26.1.0, OpenAI |

> **Principio rector**: v2.1.0 no cambia dominio, audiencia ni arquitectura general. Refina componentes existentes con evidencia cuantitativa de 7 benchmarks ejecutados durante la estabilización de v2.0.0. Esto influye directamente en las expectativas de cada interesado: no se esperan sorpresas arquitectónicas, sino mejoras incrementales trazables.

---

## 3. Identificación de Interesados

### 3.1 Sponsor / Product Owner (Usuario)

| Atributo | Valor |
|---|---|
| **Rol** | Sponsor / Product Owner |
| **Agente** | `product-owner` |
| **Tipo** | **Externo** (dueño del producto, no pertenece al equipo de ejecución) |
| **Influencia** | **Alta** — Define prioridades, aprueba gates, autoriza presupuesto y cambios de alcance |
| **Interés** | **Alto** — Es el principal beneficiario del éxito del producto |
| **Cuadrante** | Gestionar de Cerca (Alto Poder, Alto Interés) |

#### Expectativas

1. Que las 13 features eleven la precisión, velocidad, eficiencia y DX del motor a niveles competitivos, verificables con los **10 Criterios de Éxito (CE-01 a CE-10)** definidos en el Acta de Constitución.
2. Que el proyecto se ejecute dentro del alcance, cronograma (2026-05-05 a 2026-05-10) y sin desviaciones no aprobadas.
3. Que toda decisión de cambio de alcance pase por el proceso formal de **Control de Cambios** con evaluación de impacto antes de su aprobación.
4. Que la API mantenga **backward compatibility** con v2.0.x — los endpoints existentes no deben romperse.
5. Que las mejoras sean **trazables a benchmarks**: cada feature debe demostrar su impacto con datos antes/después.
6. Que el producto final pase la **UAT (Fase 6)** con los 10 criterios de éxito satisfechos.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Definir prioridades** | Decide el orden de implementación de las 13 features, validando las prioridades establecidas en el Backlog (Prioridad 1 a 13) |
| **Aprobar gates de fase** | Aprueba formalmente F0 (Descubrimiento), F6 (UAT) y F9 (Cierre). Su firma es bloqueante para avanzar |
| **Autorizar cambios de alcance** | Única autoridad para aprobar solicitudes de cambio que afecten el scope, cronograma o criterios de éxito |
| **Validar producto final** | Ejecuta la UAT con los 10 criterios de éxito. Solo el Sponsor puede declarar "Aceptado" |
| **Resolver conflictos de prioridad** | Si surge tensión entre velocidad y calidad, o entre features, el Sponsor tiene la última palabra |
| **Aprobar release** | Firma el cierre formal (F9) autorizando la publicación de v2.1.0 |

#### Estrategia de Comunicación

- **Frecuencia**: Al cierre de cada fase (reporte de avance). Adicionalmente, en cada gate que requiera su aprobación (F0, F6, F9) y en cualquier escalamiento de riesgos críticos.
- **Canal**: Reportes formales escritos (Markdown en `docs/entregables/v2.1/`). Comunicación síncrona (chat) para decisiones urgentes.
- **Formato**: Reportes ejecutivos con semáforos de estado, desviaciones vs. línea base, y decisiones pendientes. Sin detalles técnicos innecesarios.
- **Responsable de comunicación**: Project Manager.

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Accountable (aprueba visión y alcance) | Alta |
| **F1 — Inicio** | Informed (recibe el Acta para aprobación) | Media |
| **F2 — Diseño Técnico** | Informed | Baja |
| **F3 — Planificación** | Accountable (aprueba plan y cronograma) | Alta |
| **F4 — Construcción** | Informed | Baja |
| **F5 — QA Testing** | Informed (recibe resultados de benchmarks) | Media |
| **F6 — UAT** | **Responsible + Accountable** (ejecuta y aprueba UAT) | **Máxima** |
| **F7 — Despliegue** | Accountable (aprueba deploy a producción) | Alta |
| **F8 — Estabilización** | Informed | Baja |
| **F9 — Cierre** | Accountable (aprueba cierre formal) | Alta |

---

### 3.2 Project Manager

| Atributo | Valor |
|---|---|
| **Rol** | Project Manager |
| **Agente** | `project-manager` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Alta** — Controla alcance, cronograma, recursos y comunicación |
| **Interés** | **Alto** — Responsable del éxito global del proyecto |
| **Cuadrante** | Gestionar de Cerca (Alto Poder, Alto Interés) |

#### Expectativas

1. Que el proyecto se complete dentro del alcance, tiempo (5 días, 2026-05-05 a 2026-05-10) y con la calidad definida en los 10 Criterios de Éxito.
2. Que todos los entregables de cada fase se produzcan con la calidad requerida y sean aprobados en sus gates correspondientes.
3. Que los riesgos identificados (RSK-01 a RSK-08) se gestionen proactivamente y se escalen a tiempo.
4. Que la comunicación con el Sponsor y el equipo sea clara, oportuna y basada en evidencia.
5. Que no se aprueben cambios de alcance sin el proceso formal de Control de Cambios.
6. Que el proyecto deje lecciones aprendidas documentadas para futuras iteraciones.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Planificación** | Define el cronograma, hitos, dependencias y asignaciones. Mantiene la línea base actualizada |
| **Control de alcance** | Monitorea que el trabajo ejecutado corresponda al scope aprobado. Activa Control de Cambios ante desviaciones |
| **Gestión de riesgos** | Mantiene la Matriz de Riesgos actualizada. Revisa al inicio de cada fase. Escala riesgos críticos al Sponsor |
| **Seguimiento** | Monitorea avance real vs. planificado. Produce reportes de estado al cierre de cada fase |
| **Comunicación** | Punto único de contacto con el Sponsor. Convoca y facilita los gates de fase |
| **Gestión de recursos** | Asegura que cada agente tenga claridad de sus tareas y capacidad. Resuelve bloqueos entre agentes |
| **Calidad de entregables** | Verifica que cada entregable cumpla con el `documentation-quality-bar` antes de someterlo a aprobación |
| **Cierre** | Produce el informe de cierre, consolida lecciones aprendidas y actualiza documentos transversales |

#### Estrategia de Comunicación

- **Frecuencia**: Comunicación continua con el equipo. Reporte formal al Sponsor al cierre de cada fase.
- **Canal**: Documentación en repositorio (`docs/entregables/v2.1/`), reportes de estado, actas de reunión, bitácora (`docs/bitacora.md`).
- **Formato**: Reportes estructurados con: % completado, hitos cumplidos, riesgos activos, bloqueantes, próximos pasos.
- **Audiencia**: Sponsor (reportes ejecutivos), equipo (instrucciones detalladas), documentos transversales (histórico).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Responsible + Accountable (coordina y aprueba) | Alta |
| **F1 — Inicio** | Accountable (aprueba Acta) + Responsible | **Máxima** |
| **F2 — Diseño Técnico** | Accountable (aprueba diseño) | Alta |
| **F3 — Planificación** | **Responsible + Accountable** (planifica y aprueba) | **Máxima** |
| **F4 — Construcción** | Accountable (monitorea avance, gestiona riesgos) | Alta |
| **F5 — QA Testing** | Accountable (aprueba resultados QA) | Alta |
| **F6 — UAT** | Accountable (facilita, coordina, documenta) | Alta |
| **F7 — Despliegue** | Responsible (coordina deploy) | Alta |
| **F8 — Estabilización** | Responsible (monitorea, coordina ajustes) | Alta |
| **F9 — Cierre** | **Responsible** (produce informe, consolida lecciones) | **Máxima** |

---

### 3.3 Business Analyst

| Atributo | Valor |
|---|---|
| **Rol** | Business Analyst |
| **Agente** | `business-analyst` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Media** — Define requerimientos y criterios de aceptación; no aprueba gates |
| **Interés** | **Alto** — Responsable de que las especificaciones sean completas y verificables |
| **Cuadrante** | Mantener Informado y Escuchar Feedback (Bajo Poder, Alto Interés) |

> **Nota**: Aunque su poder formal es medio, su influencia técnica es alta porque la calidad de las especificaciones funcionales determina la calidad de la construcción y las pruebas.

#### Expectativas

1. Que las 13 features estén sustentadas en los gaps documentados de los 7 benchmarks de estabilización de v2.0.0.
2. Que cada feature tenga criterios de aceptación claros en formato **Given/When/Then**, verificables por QA.
3. Que la trazabilidad requerimiento → feature → criterio de éxito → benchmark sea completa y sin eslabones rotos.
4. Que las reglas de negocio de v2.0.x que no cambian estén explícitamente documentadas como "sin modificación".
5. Que los entregables de F0 y F1 (visión, épicas, backlog, historias, especificaciones funcionales) sean aprobados sin retrabajo mayor.
6. Que el `documentation-quality-bar` se cumpla en todos sus entregables.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Levantamiento de requerimientos** | Documenta las 13 features con especificación funcional detallada, derivada de los gaps de benchmarks |
| **Criterios de aceptación** | Redacta criterios en formato Given/When/Then para cada feature, asegurando que sean medibles y verificables |
| **Historias de usuario** | Produce las historias de usuario para las 4 épicas (EP-V21-001 a EP-V21-004) |
| **Backlog priorizado** | Mantiene el backlog con prioridades alineadas a las definidas por el Sponsor |
| **Matriz de trazabilidad** | Asegura que cada requerimiento trace a: gap de benchmark → ID de feature → criterio de aceptación → criterio de éxito |
| **Validación de reglas de negocio** | Documenta explícitamente qué reglas de v2.0.x se mantienen sin cambio (restricción R-04: sin cambios en modelo de datos) |

#### Estrategia de Comunicación

- **Frecuencia**: Alta durante F0 y F1 (entregables principales). Puntual en F2–F6 como consultado.
- **Canal**: Documentación en repositorio (`docs/entregables/v2.1/fase-0-descubrimiento/`, `fase-1-inicio/`).
- **Formato**: Especificaciones funcionales estructuradas, criterios Given/When/Then, tablas de trazabilidad.
- **Audiencia**: Tech Lead y equipo de desarrollo (consumen sus especificaciones), QA Lead (consume criterios de aceptación), PM (aprueba entregables).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Responsible (visión, épicas, backlog, historias) | **Máxima** |
| **F1 — Inicio** | **Responsible** (especificaciones funcionales, criterios de aceptación) | **Máxima** |
| **F2 — Diseño Técnico** | Consulted | Media |
| **F3 — Planificación** | Consulted | Baja |
| **F4 — Construcción** | Consulted (clarifica requerimientos) | Media |
| **F5 — QA Testing** | Consulted (clarifica criterios de aceptación) | Media |
| **F6 — UAT** | Consulted | Baja |
| **F7 — Despliegue** | Informed | Baja |
| **F8 — Estabilización** | Informed | Baja |
| **F9 — Cierre** | Consulted | Baja |

---

### 3.4 Solution Architect

| Atributo | Valor |
|---|---|
| **Rol** | Solution Architect |
| **Agente** | `solution-architect` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Alta** — Define la arquitectura de solución, caching, seguridad y despliegue |
| **Interés** | **Alto** — Responsable de que el diseño técnico sea viable, escalable y seguro |
| **Cuadrante** | Gestionar de Cerca (Alto Poder, Alto Interés) |

#### Expectativas

1. Que el diseño técnico de las 13 features sea compatible con la arquitectura existente de v2.0.x (stack inalterado por R-01).
2. Que las decisiones arquitectónicas estén documentadas mediante **ADR** (Architecture Decision Records) con contexto, alternativas evaluadas y justificación.
3. Que las mejoras de caching (JWT, grafo), unificación de colecciones Qdrant, y estrategia de cold start estén diseñadas con sustento técnico sólido.
4. Que la seguridad no se degrade: el cache JWT debe mantener la integridad de la validación criptográfica (R-03), y el `MockLlmService` debe ser reemplazado sin exponer datos a terceros no autorizados.
5. Que el diseño respete la restricción de backward compatibility de la API v2 (R-02).
6. Que el diseño habilite la verificación de los 10 Criterios de Éxito sin requerir cambios posteriores.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Diseño de arquitectura** | Define la arquitectura de solución para las 4 épicas, incluyendo diagramas de componentes, integración y despliegue |
| **ADR** | Redacta y mantiene los Architecture Decision Records para decisiones clave (cross-encoder, cache JWT, unificación Qdrant, estrategia de grafo) |
| **Estrategia de caching** | Diseña la arquitectura de caché para JWT y grafo de conocimiento (Caffeine, TTL, invalidación) |
| **Seguridad** | Verifica que el diseño no introduzca vulnerabilidades. El cache JWT debe cumplir R-03 (cliente JWT opaco) |
| **Modelo de despliegue** | Define cómo las mejoras se despliegan en el ambiente existente sin disrupción |
| **Revisión de diseño** | Revisa y aprueba el diseño técnico (F2) junto con el Tech Lead |

#### Estrategia de Comunicación

- **Frecuencia**: Alta en F2 (diseño técnico). Consultas puntuales en F4 y F7.
- **Canal**: Documentos de diseño en repositorio (`docs/entregables/v2.1/fase-2-diseno-tecnico/`), ADRs.
- **Formato**: Diagramas de arquitectura (Mermaid), ADRs estructurados, especificaciones de integración.
- **Audiencia**: Tech Lead (co-aprobador), Developer Backend (consumidor del diseño), DevOps (ejecutor del despliegue).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Consulted | Baja |
| **F1 — Inicio** | Consulted | Baja |
| **F2 — Diseño Técnico** | **Responsible** (diseño y ADRs) + Accountable (aprueba) | **Máxima** |
| **F3 — Planificación** | Consulted | Media |
| **F4 — Construcción** | Consulted (clarifica decisiones de diseño) | Media |
| **F5 — QA Testing** | Informed | Baja |
| **F6 — UAT** | Informed | Baja |
| **F7 — Despliegue** | Consulted (verifica despliegue según diseño) | Media |
| **F8 — Estabilización** | Consulted | Baja |
| **F9 — Cierre** | Consulted | Baja |

---

### 3.5 Tech Lead

| Atributo | Valor |
|---|---|
| **Rol** | Tech Lead |
| **Agente** | `tech-lead` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Alta** — Aprueba diseño técnico, revisa código (anti-mock review), coordina desarrollo |
| **Interés** | **Alto** — Responsable de la calidad técnica del producto construido |
| **Cuadrante** | Gestionar de Cerca (Alto Poder, Alto Interés) |

#### Expectativas

1. Que el código implementado cumpla fielmente el diseño técnico aprobado en F2.
2. Que no se introduzcan mocks, stubs o implementaciones falsas en producción (**anti-mock review** obligatoria antes del gate F4).
3. Que los estándares de código (English-Only, coding-standards, patrones de diseño) se respeten en todas las 13 features.
4. Que la deuda técnica no crezca: las 13 mejoras deben implementarse limpiamente sin atajos.
5. Que la cobertura de tests unitarios sea adecuada para todas las features nuevas.
6. Que el pipeline CI/CD (herencia de v2.0.x) se mantenga verde durante toda la construcción.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Co-diseño técnico** | Trabaja con el Solution Architect en el diseño técnico. Responsable de la viabilidad de implementación |
| **Aprobación de diseño** | Co-aprueba el documento de diseño técnico (F2) junto con el Solution Architect |
| **Revisión de código** | Ejecuta code review de todas las 13 features. Aplica `multi-stage-review` (cumplimiento de especificación + calidad de código) |
| **Anti-mock review** | Ejecuta la auditoría `anti-mock-review` antes del gate F4 para detectar implementaciones falsas |
| **Coordinación de desarrollo** | Resuelve dudas técnicas del Developer Backend durante F4. Prioriza issues técnicos |
| **Estándares de código** | Hace cumplir `code-naming-convention` (English-Only), `coding-standards` y patrones de diseño |
| **Gate F4** | Aprueba formalmente la fase de Construcción tras pasar anti-mock review y code review |

#### Estrategia de Comunicación

- **Frecuencia**: Alta en F2 (diseño) y F4 (construcción). Consultas puntuales en F1, F3, F5.
- **Canal**: Documentos de diseño, ADRs, code review comments en PRs, reportes de anti-mock review.
- **Formato**: Reportes técnicos de hallazgos, clasificación de severidad (Crítico/Alto/Medio/Bajo), recomendaciones.
- **Audiencia**: Solution Architect (co-diseño), Developer Backend (dirección técnica), PM (aprobación de gate).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Consulted | Baja |
| **F1 — Inicio** | Consulted | Media |
| **F2 — Diseño Técnico** | **Responsible** (co-diseña y aprueba) | **Máxima** |
| **F3 — Planificación** | Consulted | Media |
| **F4 — Construcción** | **Responsible** (revisa código, anti-mock review) + Accountable (gate F4) | **Máxima** |
| **F5 — QA Testing** | Consulted (apoya diagnóstico de defectos) | Media |
| **F6 — UAT** | Consulted | Baja |
| **F7 — Despliegue** | Consulted | Baja |
| **F8 — Estabilización** | Consulted (apoya diagnóstico de issues) | Media |
| **F9 — Cierre** | Consulted | Baja |

---

### 3.6 Developer Backend

| Atributo | Valor |
|---|---|
| **Rol** | Developer Backend |
| **Agente** | `developer-backend` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Media** — Ejecuta la implementación; no toma decisiones de arquitectura ni aprueba gates |
| **Interés** | **Alto** — Responsable de que el código funcione correctamente y pase las pruebas |
| **Cuadrante** | Mantener Informado y Escuchar Feedback (Bajo Poder, Alto Interés) |

#### Expectativas

1. Que las especificaciones funcionales (F1) y el diseño técnico (F2) sean lo suficientemente claros para implementar sin ambigüedad.
2. Que las 13 features puedan implementarse dentro del tiempo asignado sin comprometer calidad.
3. Que el stack tecnológico (Quarkus, Qdrant, PostgreSQL, Keycloak, OpenAI) se comporte de forma predecible y sin sorpresas.
4. Que el entorno de desarrollo esté correctamente configurado (dependency-management) para un arranque rápido.
5. Que el pipeline CI/CD valide automáticamente tests unitarios en cada commit.
6. Que la anti-mock review no encuentre problemas mayores en su código — el trabajo debe ser genuino, no simulado.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Implementación** | Codifica las 13 features del backend Quarkus según diseño técnico y criterios de aceptación |
| **Tests unitarios** | Escribe y ejecuta tests unitarios para todas las features nuevas |
| **Code review** | Recibe y atiende las observaciones del Tech Lead en el proceso de code review |
| **Corrección de defectos** | Corrige defectos encontrados en QA (F5) y UAT (F6) |
| **English-Only** | Cumple estrictamente la convención de identificadores en inglés (`code-naming-convention`) |
| **Documentación técnica** | Documenta decisiones de implementación relevantes en el código (comentarios, Javadoc) |

#### Estrategia de Comunicación

- **Frecuencia**: Alta durante F4 (construcción). Puntual en F5 (corrección de defectos).
- **Canal**: Repositorio de código (commits, PRs), código fuente, tests.
- **Formato**: Código documentado, mensajes de commit descriptivos con prefijo `v2.1:`, PRs con descripción de cambios.
- **Audiencia**: Tech Lead (revisa su código), QA Lead (consume el build para pruebas), DevOps (despliega el artefacto).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Informed | Baja |
| **F1 — Inicio** | Informed | Baja |
| **F2 — Diseño Técnico** | Consulted | Media |
| **F3 — Planificación** | Informed | Baja |
| **F4 — Construcción** | **Responsible** (implementa las 13 features) | **Máxima** |
| **F5 — QA Testing** | Consulted (corrige defectos) | Media |
| **F6 — UAT** | Informed | Baja |
| **F7 — Despliegue** | Consulted (soporte durante deploy) | Media |
| **F8 — Estabilización** | Consulted (corrige issues post-deploy) | Media |
| **F9 — Cierre** | Informed | Baja |

---

### 3.7 Developer Frontend

| Atributo | Valor |
|---|---|
| **Rol** | Developer Frontend |
| **Agente** | `developer-frontend` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Baja** — En v2.1.0 no hay cambios funcionales en el frontend React |
| **Interés** | **Medio** — Debe verificar que la API no rompa el frontend existente, pero no construye features nuevas |
| **Cuadrante** | Monitorear con Mínimo Esfuerzo (Bajo Poder, Bajo Interés) |

#### Expectativas

1. Que los cambios en la API v2 (`search`/`hybrid`, `DELETE /admin/namespaces`, `X-Graph-Strategy`) no rompan la interfaz de usuario React existente.
2. Que la backward compatibility de la API (R-02) se cumpla para que el frontend siga funcionando sin modificaciones.
3. Que cualquier cambio que afecte al frontend (aunque no previsto) sea comunicado con anticipación.
4. Que su participación se limite a verificación de no-regresión, sin requerir desarrollo nuevo.
5. Que el esfuerzo requerido sea mínimo y no distraiga de otras responsabilidades.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Verificación de no-regresión** | Prueba las 6 pantallas y 7 componentes React contra la API v2 modificada para confirmar que no hay breaking changes |
| **Reporte de issues** | Si detecta que un cambio de API rompe el frontend, reporta inmediatamente al PM y Tech Lead |
| **Validación de headers** | Verifica que headers nuevos (`X-Graph-Strategy`, `Deprecation`) no causen errores en el frontend |
| **Consulta técnica** | Disponible como Consulted si el Tech Lead o el Developer Backend necesitan clarificar el comportamiento esperado del frontend |

#### Estrategia de Comunicación

- **Frecuencia**: Baja. Solo en F5–F6 para verificación de no-regresión.
- **Canal**: Reportes de issues, canal de equipo.
- **Formato**: Lista de verificación de pantallas y componentes, issues si los hay.
- **Audiencia**: PM y Tech Lead (reciben reportes de no-regresión).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Informed | Baja |
| **F1 — Inicio** | Informed | Baja |
| **F2 — Diseño Técnico** | Informed | Baja |
| **F3 — Planificación** | Informed | Baja |
| **F4 — Construcción** | Informed | Baja |
| **F5 — QA Testing** | Consulted (verifica no-regresión del frontend) | Media |
| **F6 — UAT** | Consulted | Baja |
| **F7 — Despliegue** | Informed | Baja |
| **F8 — Estabilización** | Informed | Baja |
| **F9 — Cierre** | Informed | Baja |

---

### 3.8 QA Lead / QA Functional

| Atributo | Valor |
|---|---|
| **Rol** | QA Lead / QA Functional |
| **Agente** | `qa-lead` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Media** — Diseña y ejecuta pruebas; sus hallazgos pueden bloquear el avance a producción |
| **Interés** | **Alto** — Responsable de que el producto cumpla los criterios de aceptación y benchmarks |
| **Cuadrante** | Mantener Informado y Escuchar Feedback (Bajo Poder, Alto Interés) |

> **Nota**: Su poder formal es medio, pero su influencia es alta en la práctica: un defecto crítico no resuelto bloquea el gate F5 y por tanto todo el despliegue.

#### Expectativas

1. Que las especificaciones funcionales y los criterios de aceptación (F1) sean lo suficientemente precisos para diseñar casos de prueba sin ambigüedad.
2. Que el producto construido (F4) sea testeable y que el entorno de QA esté correctamente configurado.
3. Que los 10 Criterios de Éxito (CE-01 a CE-10) sean verificables con datos reales y benchmarks reproducibles.
4. Que los benchmarks de precisión (SciFact, suite multi-dominio) muestren mejora medible respecto a v2.0.9.
5. Que los defectos encontrados sean corregidos por el Developer Backend dentro de la misma fase (F5) sin arrastrarse a fases posteriores.
6. Que las pruebas de carga confirmen que la latencia p95 (CE-02) cumple la meta de ≤ 500ms.

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Diseño de casos de prueba** | Diseña casos de prueba funcionales trazables a cada uno de los 13 features y a los 10 CE |
| **Ejecución de pruebas** | Ejecuta pruebas funcionales, de integración y de regresión en ambiente QA |
| **Benchmarks** | Ejecuta los benchmarks de precisión (SciFact, suite multi-dominio) y de latencia (pruebas de carga) |
| **Reporte de defectos** | Registra, clasifica y da seguimiento a defectos encontrados usando `defect-reporting` |
| **Matriz de trazabilidad** | Mantiene la `traceability-matrix` vinculando requerimientos → casos de prueba → defectos → resultados |
| **Aprobación de QA** | Aprueba formalmente la fase QA (F5) cuando todos los criterios de aceptación y CE están verificados |
| **Pruebas de regresión** | Ejecuta `regression-testing` para verificar que las features existentes de v2.0.x no se degradan |

#### Estrategia de Comunicación

- **Frecuencia**: Alta en F5 (QA Testing). Puntual en F4 (revisión de criterios) y F6 (soporte a UAT).
- **Canal**: Plan de pruebas, reporte de defectos, resultados de benchmarks, matriz de trazabilidad — todo en repositorio.
- **Formato**: Reportes estructurados con: casos ejecutados, pasaron/fallaron, defectos abiertos por severidad, benchmarks antes/después.
- **Audiencia**: PM y Tech Lead (resultados y bloqueos), Developer Backend (defectos a corregir), Sponsor (resultados de benchmarks en F6).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Informed | Baja |
| **F1 — Inicio** | Informed | Baja |
| **F2 — Diseño Técnico** | Informed | Baja |
| **F3 — Planificación** | Informed | Baja |
| **F4 — Construcción** | Consulted (revisa criterios de aceptación) | Media |
| **F5 — QA Testing** | **Responsible** (diseña, ejecuta, reporta) | **Máxima** |
| **F6 — UAT** | Consulted (soporta al Sponsor en validación) | Media |
| **F7 — Despliegue** | Informed | Baja |
| **F8 — Estabilización** | Consulted (verifica benchmarks post-deploy) | Media |
| **F9 — Cierre** | Consulted | Baja |

---

### 3.9 DevOps

| Atributo | Valor |
|---|---|
| **Rol** | DevOps |
| **Agente** | `devops` |
| **Tipo** | **Interno** (miembro del equipo de ejecución) |
| **Influencia** | **Media** — Controla infraestructura, CI/CD y despliegue; sus decisiones afectan la estabilidad operativa |
| **Interés** | **Alto** — Responsable de que el despliegue sea seguro, reversible y monitoreado |
| **Cuadrante** | Mantener Informado y Escuchar Feedback (Bajo Poder, Alto Interés) |

> **Nota**: Su influencia es media en la estructura de gobierno, pero crítica en la práctica: un error de despliegue puede causar downtime. La unificación de colecciones Qdrant (FT-V21-003.2) y el diagnóstico del worker (FT-V21-003.1) son tareas que requieren su expertise.

#### Expectativas

1. Que el despliegue de v2.1.0 sea seguro, planificado, con ventana de pase definida y plan de rollback documentado.
2. Que la unificación de colecciones Qdrant (FT-V21-003.2) no cause pérdida de datos ni downtime.
3. Que el diagnóstico del worker inactivo (FT-V21-003.1) resulte en su eliminación o reparación sin afectar la ingesta de memorias.
4. Que los ambientes de desarrollo, QA, staging y producción estén correctamente configurados y con paridad razonable.
5. Que el pipeline CI/CD herede la configuración de v2.0.x y valide build + tests automáticamente.
6. Que el monitoreo post-deploy (F8) detecte anomalías tempranamente (cold start, latencia, errores).

#### Responsabilidades en el Proyecto

| Responsabilidad | Detalle |
|---|---|
| **Gestión de ambientes** | Administra los ambientes de dev, QA, staging y producción con `environment-management` |
| **CI/CD** | Mantiene y opera el pipeline de integración continua y despliegue (`ci-cd-pipeline`) |
| **Infraestructura Qdrant** | Ejecuta la unificación de colecciones Qdrant (FT-V21-003.2) con migración segura |
| **Diagnóstico de worker** | Diagnostica y resuelve el estado del worker asíncrono (`Claimed=0`) |
| **Plan de despliegue** | Elabora el `deployment-plan` con checklist, ventana, rollback y comunicación |
| **Ejecución de despliegue** | Ejecuta el despliegue a producción siguiendo el plan aprobado |
| **Monitoreo post-deploy** | Monitorea el sistema tras el despliegue (F8) y reporta métricas de salud |
| **Containerización** | Mantiene las imágenes Docker y la configuración de contenedores |

#### Estrategia de Comunicación

- **Frecuencia**: Alta en F7 (despliegue) y F8 (estabilización). Puntual en F4 (soporte de infraestructura).
- **Canal**: Plan de despliegue documentado, logs de CI/CD, reportes de monitoreo post-deploy.
- **Formato**: Checklist pre-deploy/post-deploy, runbooks, dashboards de monitoreo.
- **Audiencia**: PM (aprobación de despliegue), Tech Lead (coordinación técnica), Sponsor (confirmación de deploy exitoso).

#### Fases donde Participa

| Fase | Rol | Intensidad |
|---|---|---|
| **F0 — Descubrimiento** | Informed | Baja |
| **F1 — Inicio** | Informed | Baja |
| **F2 — Diseño Técnico** | Consulted (viabilidad de infraestructura) | Media |
| **F3 — Planificación** | Consulted (estimaciones de infraestructura) | Media |
| **F4 — Construcción** | Consulted (soporte de ambientes) | Media |
| **F5 — QA Testing** | Consulted (ambiente QA) | Media |
| **F6 — UAT** | Informed | Baja |
| **F7 — Despliegue** | **Responsible** (ejecuta el deploy) | **Máxima** |
| **F8 — Estabilización** | **Responsible** (monitorea, ajusta) | **Máxima** |
| **F9 — Cierre** | Consulted | Baja |

---

## 4. Matriz de Poder/Interés

### 4.1 Clasificación por Cuadrante

La matriz clasifica a los 9 interesados según su nivel de **influencia** (poder de decisión sobre el proyecto) y su nivel de **interés** (cuánto les afecta el resultado).

| Cuadrante | Estrategia | Interesados |
|---|---|---|
| **I — Alto Poder, Alto Interés** | **Gestionar de Cerca** — Comunicación frecuente, involucramiento en decisiones clave, consulta proactiva | Sponsor, Project Manager, Solution Architect, Tech Lead |
| **II — Alto Poder, Bajo Interés** | **Mantener Satisfechos** — Reportes periódicos para que se sientan considerados, sin saturarlos | *(Ninguno en v2.1.0)* |
| **III — Bajo Poder, Alto Interés** | **Mantener Informados** — Comunicación regular, escuchar su feedback, involucrarlos en decisiones que les afectan | Business Analyst, Developer Backend, QA Lead, DevOps |
| **IV — Bajo Poder, Bajo Interés** | **Monitorear** — Mínimo esfuerzo, informar solo si hay cambios que les afecten directamente | Developer Frontend |

### 4.2 Diagrama de la Matriz

```
                          INTERÉS
                    
                    Bajo              Alto
           ┌──────────────────┬──────────────────┐
           │                  │                  │
           │  II. MANTENER    │  I. GESTIONAR    │
   A       │  SATISFECHOS     │  DE CERCA        │
   L       │                  │                  │
   T       │  (vacío)         │  Sponsor         │
   O       │                  │  Project Manager │
           │                  │  Solution Arch.  │
   P       │                  │  Tech Lead       │
   O       ├──────────────────┼──────────────────┤
   D       │                  │                  │
   E       │  IV. MONITOREAR  │  III. MANTENER   │
   R       │                  │  INFORMADOS      │
           │                  │                  │
   B       │  Dev Frontend    │  Business Analyst│
   A       │                  │  Dev Backend     │
   J       │                  │  QA Lead         │
   O       │                  │  DevOps          │
           │                  │                  │
           └──────────────────┴──────────────────┘
```

### 4.3 Tabla de Clasificación

| # | Interesado | Tipo | Influencia | Interés | Cuadrante | Estrategia |
|---|---|---|---|---|---|---|
| 1 | Sponsor (Usuario) | Externo | Alta | Alto | I | Gestionar de Cerca |
| 2 | Project Manager | Interno | Alta | Alto | I | Gestionar de Cerca |
| 3 | Solution Architect | Interno | Alta | Alto | I | Gestionar de Cerca |
| 4 | Tech Lead | Interno | Alta | Alto | I | Gestionar de Cerca |
| 5 | Business Analyst | Interno | Media | Alto | III | Mantener Informado |
| 6 | Developer Backend | Interno | Media | Alto | III | Mantener Informado |
| 7 | QA Lead | Interno | Media | Alto | III | Mantener Informado |
| 8 | DevOps | Interno | Media | Alto | III | Mantener Informado |
| 9 | Developer Frontend | Interno | Baja | Medio | IV | Monitorear |

---

## 5. Plan de Comunicación

### 5.1 Canales y Frecuencias por Interesado

| # | Interesado | Canal Principal | Frecuencia | Formato | Responsable de Comunicar |
|---|---|---|---|---|---|
| 1 | **Sponsor** | Reporte de avance (Markdown en repo) + Chat para decisiones urgentes | Al cierre de cada fase + en gates F0, F6, F9 + escalamientos | Reporte ejecutivo: semáforos, desviaciones, decisiones pendientes | Project Manager |
| 2 | **Project Manager** | Bitácora (`docs/bitacora.md`), registro de entregables, reportes de agentes | Continuo (recibe de todos los agentes) | Reportes estructurados por fase, actas de reunión, matriz de riesgos | Todos los agentes → PM |
| 3 | **Solution Architect** | Documento de diseño técnico + ADRs (F2). Consultas puntuales vía chat | Alta en F2. Puntual en F4, F7 | Diagramas Mermaid, ADRs estructurados | Tech Lead, Developer Backend |
| 4 | **Tech Lead** | ADRs, code review en PRs, anti-mock review report | Alta en F2 y F4. Puntual en F1, F5 | Reportes técnicos con clasificación de severidad | Solution Architect, Developer Backend, QA Lead |
| 5 | **Business Analyst** | Especificaciones funcionales, criterios de aceptación, historias de usuario (repo) | Alta en F0–F1. Puntual en F2–F6 | Documentos estructurados Given/When/Then, tablas de trazabilidad | PM (aprobación), Tech Lead y QA (consumo) |
| 6 | **Developer Backend** | Código fuente, PRs, tests (repo) | Alta en F4. Puntual en F5, F8 | Commits con prefijo `v2.1:`, PRs con descripción de cambios | Tech Lead (revisión), QA Lead (pruebas) |
| 7 | **QA Lead** | Plan de pruebas, casos de prueba, reporte de defectos, benchmarks (repo) | Alta en F5. Puntual en F4, F6, F8 | Reportes con casos ejecutados, defectos por severidad, benchmarks antes/después | PM, Tech Lead, Developer Backend |
| 8 | **DevOps** | Plan de despliegue, logs CI/CD, dashboards de monitoreo (repo) | Alta en F7–F8. Puntual en F2, F4 | Checklist, runbooks, gráficos de monitoreo | PM (aprobación), Tech Lead (coordinación) |
| 9 | **Developer Frontend** | Issues, canal de equipo | Baja. Solo en F5–F6 para verificación | Lista de verificación de pantallas/componentes | PM, Tech Lead |

### 5.2 Eventos de Comunicación Obligatorios

| Evento | Gatillador | Audiencia | Canal | Responsable |
|---|---|---|---|---|
| **Reporte de Avance de Fase** | Al cierre de cada fase (F0–F9) | Sponsor, todos los stakeholders | Documento Markdown en `docs/entregables/v2.1/` | Project Manager |
| **Acta de Gate** | Al completar cada gate de fase (H0–H10) | Participantes del gate + Sponsor | Documento Markdown en la carpeta de la fase | Project Manager |
| **Escalamiento de Riesgo Crítico** | Al activarse el disparador de un riesgo con impacto Crítico | Sponsor, PM | Chat (urgente) + documento formal | Project Manager |
| **Control de Cambios** | Ante cualquier solicitud de cambio de alcance | Sponsor, PM, Tech Lead | Documento formal con evaluación de impacto | Project Manager |
| **Actualización de Matriz de Riesgos** | Al inicio de cada fase | PM, Sponsor (si hay cambios Críticos) | Documento Markdown | Project Manager |
| **Actualización de Bitácora** | Cada agente al completar su tarea | Histórico para futuras iteraciones | `docs/bitacora.md` | Todos los agentes |
| **Notificación de Bloqueo** | Cuando un bloqueo persiste > 2h sin resolución | PM | Chat | Agente bloqueado → PM |
| **Verificación Post-Deploy** | Inmediatamente después del despliegue (F7) | Sponsor, PM, DevOps, QA | Documento de verificación | DevOps |

### 5.3 Protocolo de Escalamiento en la Comunicación

```
Agente bloqueado
  └─→ Notifica al PM (inmediato, chat)
       ├─ PM resuelve en ≤ 2h → Fin
       └─ PM no resuelve en > 2h
            └─→ PM escala al Sponsor con:
                 - Descripción del bloqueo
                 - Impacto (fase, cronograma, riesgo)
                 - Alternativas evaluadas
                 - Recomendación del PM
```

---

## 6. Matriz de Participación por Fase

### 6.1 RACI por Fase (Matriz de Responsabilidades)

> **Leyenda**: **R** = Responsible (ejecuta), **A** = Accountable (aprueba/responde), **C** = Consulted, **I** = Informed, **—** = No participa

| Fase | Sponsor | PM | BA | Sol. Arch. | Tech Lead | Dev Backend | Dev Frontend | QA Lead | DevOps |
|---|---|---|---|---|---|---|---|---|---|
| **F0 — Descubrimiento** | A | R/A | R | C | C | I | I | I | I |
| **F1 — Inicio / Análisis Funcional** | I | A | R | C | C | I | I | I | I |
| **F2 — Diseño Técnico** | I | A | C | R/A | R/A | C | I | I | C |
| **F3 — Planificación** | A | R/A | C | C | C | I | I | I | C |
| **F4 — Construcción** | I | A | C | C | R/A | R | I | C | C |
| **F5 — QA Testing** | I | A | C | I | C | C | C | R/A | C |
| **F6 — UAT** | R/A | A | C | I | C | I | C | C | I |
| **F7 — Despliegue** | A | R | I | C | C | C | I | I | R/A |
| **F8 — Estabilización** | I | R | I | C | C | C | I | C | R |
| **F9 — Cierre** | A | R | C | C | C | I | I | I | C |

### 6.2 Intensidad de Participación por Fase (Mapa de Calor)

| Fase | Sponsor | PM | BA | Sol. Arch. | Tech Lead | Dev Backend | Dev Frontend | QA Lead | DevOps |
|---|---|---|---|---|---|---|---|---|---|
| **F0** | ███ | ███ | ███ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ |
| **F1** | ░░░ | ███ | ███ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ |
| **F2** | ░░░ | ███ | ██░ | ███ | ███ | ██░ | ░░░ | ░░░ | ██░ |
| **F3** | ██░ | ███ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ |
| **F4** | ░░░ | ██░ | ░░░ | ██░ | ███ | ███ | ░░░ | ██░ | ██░ |
| **F5** | ░░░ | ██░ | ░░░ | ░░░ | ██░ | ██░ | ██░ | ███ | ██░ |
| **F6** | ███ | ██░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ██░ | ░░░ |
| **F7** | ██░ | ██░ | ░░░ | ██░ | ░░░ | ░░░ | ░░░ | ░░░ | ███ |
| **F8** | ░░░ | ██░ | ░░░ | ░░░ | ░░░ | ██░ | ░░░ | ██░ | ███ |
| **F9** | ██░ | ███ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ | ░░░ |

> **Leyenda**: ███ = Máxima / Responsable directo | ██░ = Alta / Consultado frecuente | ░░░ = Baja / Informed

---

## 7. Estrategias de Gestión por Cuadrante

### Cuadrante I — Gestionar de Cerca (Sponsor, PM, Solution Architect, Tech Lead)

| Estrategia | Acciones Concretas |
|---|---|
| **Comunicación frecuente y proactiva** | Reportes al cierre de cada fase. Involucramiento en todos los gates. Consulta antes de decisiones que les afecten. |
| **Transparencia total** | Compartir riesgos, bloqueos y desviaciones sin filtro ni demora. El Sponsor no debe recibir sorpresas. |
| **Gestión de expectativas** | Cada reporte destaca: ¿vamos según lo planeado? ¿Qué cambió? ¿Qué necesita decisión? |
| **Participación en decisiones** | Los 4 deben estar alineados en las decisiones de arquitectura, diseño y construcción. Los desacuerdos se escalan al Sponsor. |
| **Monitoreo de satisfacción** | Al cierre de F4, F6 y F9: verificar explícitamente que cada uno está satisfecho con el progreso y resultado. |

### Cuadrante III — Mantener Informados (BA, Dev Backend, QA Lead, DevOps)

| Estrategia | Acciones Concretas |
|---|---|
| **Comunicación regular** | Incluirlos en los reportes de avance. Compartir decisiones que afectan su trabajo. |
| **Escuchar feedback activamente** | Sus observaciones sobre especificaciones, diseño, pruebas e infraestructura son valiosas. El PM debe asegurarse de que su voz es escuchada. |
| **Involucramiento en su dominio** | Aunque no aprueban gates, son responsables de la calidad de su trabajo. Darles autonomía en su área. |
| **Reconocimiento** | Visibilizar sus contribuciones en los reportes de avance. |
| **Remover bloqueos** | Si un miembro de este cuadrante está bloqueado, el PM debe priorizar la resolución — su trabajo es el motor del proyecto. |

### Cuadrante IV — Monitorear (Dev Frontend)

| Estrategia | Acciones Concretas |
|---|---|
| **Mínimo esfuerzo** | No saturar con comunicaciones innecesarias. Incluir en reportes de avance solo como cortesía. |
| **Alerta temprana** | Si surge un cambio que podría afectar al frontend (aunque no previsto), notificar inmediatamente. |
| **Verificación puntual** | Solicitar su participación solo en F5–F6 para verificación de no-regresión. |

---

## 8. Escalamiento

### 8.1 Niveles de Escalamiento

| Nivel | Quién escala | A quién escala | Cuándo |
|---|---|---|---|
| **N1 — Bloqueo de agente** | Cualquier agente | Project Manager | Cuando un bloqueo impide continuar con una tarea asignada |
| **N2 — Bloqueo no resuelto** | Project Manager | Sponsor | Si un bloqueo persiste > 4h sin resolución en N1 |
| **N3 — Riesgo materializado** | Project Manager | Sponsor | Cuando un riesgo con impacto Crítico o Alto se materializa |
| **N4 — Desviación de alcance** | Project Manager | Sponsor | Ante cualquier cambio que requiera modificar el scope, cronograma o criterios de éxito |
| **N5 — Conflicto entre roles** | Cualquier agente | Project Manager → Sponsor | Si dos agentes con igual autoridad (ej. Tech Lead vs Solution Architect) no llegan a acuerdo |

### 8.2 Datos de Contacto para Escalamiento

| Rol | Canal Primario | Tiempo de Respuesta Esperado |
|---|---|---|
| **Project Manager** | Chat del proyecto | ≤ 30 min en horario hábil |
| **Sponsor** | Chat directo (solo para N2–N5) | ≤ 2h |

---

## 9. Control de Cambios en el Registro

Este registro es un documento vivo. Cualquier cambio en la identificación, clasificación o estrategia de un interesado debe seguir el siguiente proceso:

1. **Solicitud de cambio**: cualquier agente puede proponer una actualización (nuevo interesado, cambio de influencia/interés, ajuste de estrategia).
2. **Evaluación**: el Project Manager evalúa la solicitud y su impacto en el plan de comunicación.
3. **Aprobación**: el Sponsor aprueba cambios que afecten la estructura de gobernanza (RACI, escalamiento).
4. **Actualización**: se actualiza este documento, se registra en `docs/bitacora.md` y se notifica a los afectados.
5. **Versión**: se incrementa el minor version en el frontmatter (ej. 2.1.0 → 2.1.1) para cambios en el registro.

---

## Glosario

- **RACI**: Matriz de asignación de responsabilidades: Responsible (ejecuta), Accountable (aprueba/responde), Consulted (consultado), Informed (informado).
- **ADR**: Architecture Decision Record — documento estructurado que registra una decisión arquitectónica, su contexto, alternativas evaluadas y justificación.
- **UAT**: User Acceptance Testing — fase 6 del ciclo cascada donde el Sponsor valida que el producto cumple los criterios de aceptación.
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que penaliza documentos relevantes en posiciones bajas del top-10.
- **Qdrant**: Base de datos vectorial open-source utilizada para almacenar embeddings y ejecutar búsqueda semántica por similitud de coseno.
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación; Abax-Memory valida JWTs contra Keycloak en cada request.
- **CI/CD**: Continuous Integration / Continuous Deployment — pipeline automatizado para build, test y deploy de software.
