# Matriz de Riesgos Inicial — Abax-Memory v2.0.0
- **Fase**: 1 — Inicio (v2.0.0)
- **Responsable**: project-manager
- **Fecha**: 2026-05-03
- **Estado**: Completado
- **Release**: v2.0.0
- **Fuentes**:
  - `docs/entregables/v2/fase-0-descubrimiento/vision-producto.md` (alcance, supuestos, restricciones, criterios de éxito)
  - `docs/entregables/v2/fase-0-descubrimiento/epicas-features.md` (épicas y features)
  - `docs/iteration-log.md` (estrategia de iteración, lecciones de v1)
  - `project-manifest.yaml` (equipo, skills disponibles)
  - Lecciones aprendidas v1.0.0 (incidentes Abax-Memory: anti-mock review, role-boundaries, delegation-discipline)

---

## 1. Tabla de Riesgos Identificados

### Escala de Valoración

| Nivel | Probabilidad (P) | Impacto (I) |
|---|---|---|
| 1 | Muy Baja (< 10%) | Insignificante — no afecta cronograma ni calidad |
| 2 | Baja (10–25%) | Menor — retraso ≤ 1 día o afecta 1 criterio de éxito secundario |
| 3 | Media (25–50%) | Moderado — retraso 2–5 días o afecta 1 criterio de éxito primario |
| 4 | Alta (50–75%) | Significativo — retraso 1–2 semanas o afecta múltiples criterios de éxito |
| 5 | Muy Alta (> 75%) | Crítico — bloquea una fase completa o impide alcanzar objetivos del MVP |

**Score = Probabilidad × Impacto** (rango 1–25)

---

### 1.1 Riesgos Técnicos

| ID | Descripción | Categoría | P (1-5) | I (1-5) | Score | Estrategia | Plan de Mitigación | Responsable | Disparador | Estado |
|---|---|---|---|---|---|---|---|---|---|---|
| **R-001** | **Stack flexible mal administrado**: La libertad de cambiar componentes del stack (Restricción R-01) provoca cambios tardíos sin justificación suficiente, generando retrabajo, inestabilidad o bloqueos en fases avanzadas. | Técnico | 3 | 4 | **12** | Mitigar | (1) Todo cambio de stack requiere ADR aprobado antes de F3 (Diseño Técnico). (2) Congelar stack al finalizar F3 — no se permiten cambios post-F3 sin evaluación de impacto completa y plan de migración. (3) Si un cambio implica reindexar embeddings (ej. cambiar de OpenAI a otro proveedor), medir desviación de scores en colección de validación antes de aprobar. | Tech Lead + Solution Architect | Propuesta de cambio de stack sin ADR | Abierto |
| **R-002** | **English-Only non-compliance**: Identificadores en español se filtran en código fuente, endpoints, enums, columnas de BD o documentación OpenAPI, violando la convención no negociable (Restricción R-04, BR-010) y generando retrabajo de corrección. | Técnico | 4 | 3 | **12** | Mitigar | (1) Linter custom que detecte identificadores en español en código fuente (variables, funciones, endpoints, enums, columnas SQL). (2) Gate de code review: todo PR debe pasar el linter English-Only antes de merge. (3) Script de validación sobre el spec OpenAPI para detectar paths/params/schemas con español. (4) Lista de palabras prohibidas en identificadores: `usuario`, `memoria`, `relacion`, `entidad`, `estado`, `ambito`, etc. | Tech Lead + Developer Backend | PR con identificadores en español no detectado | Abierto |
| **R-003** | **Data leakage multi-tenant**: El aislamiento entre tenants falla — un tenant accede a memorias de otro tenant por error en los filtros de `scope.tenantId`, índice mal particionado o bug en la lógica de autorización. | Técnico | 2 | 5 | **10** | Mitigar | (1) Tests automatizados de seguridad que verifican cross-tenant isolation con tokens de distintos tenants (Criterio CE-07: 100% queries cross-tenant retornan 0 resultados). (2) Índices compuestos en PostgreSQL con `tenantId` como primera columna. (3) Regla de autorización en capa de servicios: toda query se filtra automáticamente por `tenantId` del token, nunca por parámetro de request. (4) Test de penetración antes de F7 (Deployment). | Solution Architect + Developer Backend | Falla en test de cross-tenant isolation | Abierto |
| **R-004** | **Latencia de búsqueda excede meta**: El endpoint `POST /memories/search` supera los 500ms en p95 (CE-04), degradando la experiencia del Memory Consumer y haciendo el producto no competitivo frente a Mem0/Zep. | Técnico | 3 | 4 | **12** | Mitigar | (1) Pruebas de carga tempranas en F4 (Construcción) con 10K+ memorias y 3 tenants simulados. (2) Optimizar pipeline de búsqueda: filtros estructurados antes del ranking vectorial cuando sea posible (pre-filtrado en Qdrant). (3) Cache de embeddings frecuentes en Redis/memoria para consultas repetidas. (4) Si la latencia objetivo no se alcanza con el stack actual, evaluar cambio a motor de embeddings más rápido mediante ADR. | Tech Lead + DevOps | Primera prueba de carga > 500ms p95 | Abierto |
| **R-005** | **Regresión de funcionalidad v1**: Capacidades que funcionaban en v1.0.0 (búsqueda semántica, extracción de entidades, auditoría, ciclo de vida) no se replican correctamente en la nueva arquitectura v2, resultando en pérdida de calidad respecto al baseline. | Técnico | 3 | 3 | **9** | Mitigar | (1) Suite de regresión que ejecuta los 54 tests de v1 adaptados al modelo v2. (2) Comparación de métricas de búsqueda (NDCG, Recall) entre v1 y v2 sobre el mismo dataset de prueba. (3) Las lecciones aprendidas de v1 (anti-mock review, role boundaries) se aplican como gates obligatorios en v2. | QA Functional + Tech Lead | Regresión en métricas vs baseline v1 | Abierto |
| **R-006** | **Dependencia de OpenAI**: Cambios en la API de embeddings de OpenAI (deprecación de `text-embedding-3-large`, cambio de dimensionalidad, aumento de latencia o pricing) afectan la viabilidad del producto en producción. | Externo | 2 | 4 | **8** | Mitigar | (1) Arquitectura que abstraiga el motor de embeddings detrás de una interfaz (`EmbeddingProvider`) para permitir swap sin cambios en el core. (2) Evaluar y documentar al menos un proveedor alternativo (ej. Cohere, Voyage AI, modelo local) como fallback. (3) Medir equivalencia de scores entre proveedores en el dataset de validación interno. | Solution Architect + Tech Lead | Anuncio de deprecación o cambio de pricing de OpenAI | Abierto |
| **R-007** | **Qdrant no escala con multi-tenancy**: La colección unificada de Qdrant con filtros por `tenantId` muestra degradación cuando múltiples tenants con miles de memorias cada uno ejecutan búsquedas concurrentes. | Técnico | 2 | 4 | **8** | Mitigar | (1) Evaluar arquitectura de colecciones por tenant vs. colección unificada con filtros — decidir mediante ADR en F3. (2) Pruebas de estrés simulando 10 tenants con 1K+ memorias cada uno y 50 búsquedas concurrentes. (3) Monitorear uso de memoria y disco de Qdrant por tenant. (4) Documentar límites operativos y recomendar particionamiento si se exceden. | Solution Architect + DevOps | Degradación > 20% en búsqueda con múltiples tenants | Abierto |

---

### 1.2 Riesgos Funcionales

| ID | Descripción | Categoría | P (1-5) | I (1-5) | Score | Estrategia | Plan de Mitigación | Responsable | Disparador | Estado |
|---|---|---|---|---|---|---|---|---|---|---|
| **R-008** | **Perfiles de dominio insuficientes**: Los 3 perfiles iniciales (Ops, Agent, Business) no logran el balance entre generalidad y utilidad — son demasiado genéricos para aportar valor real al Domain Curator, o demasiado rígidos y requieren lógica custom. | Funcional | 4 | 3 | **12** | Mitigar | (1) Validar cada perfil con al menos un escenario real de uso (no sintético) antes de F4. Para Ops: un incidente real y su resolución. Para Agent: un historial de conversación de 20 turnos. Para Business: un caso de CRM con 5 entidades relacionadas. (2) Los perfiles se definen como configuraciones JSON/YAML — si un escenario requiere lógica que no puede expresarse como configuración, se escala a Product Owner para decidir si se ajusta el alcance. (3) Incluir a Domain Curator como revisor de los perfiles en F2 (Análisis Funcional). | Business Analyst + Product Owner | Perfil no cubre un escenario real de validación | Abierto |
| **R-009** | **Scope creep por multi-dominio**: La promesa de "cualquier dominio" genera expectativas de features verticales específicas (dashboard legal, consola de CRM, workflow de aprobación por industria) que están fuera del alcance del MVP, causando fricción con stakeholders o desviación de esfuerzo. | Funcional | 4 | 3 | **12** | Mitigar | (1) Comunicar explícitamente en cada gate y demo que el entregable es un motor genérico + perfiles configurables, no una solución vertical completa. (2) Toda solicitud de feature específica de dominio se registra como cambio de alcance (Control de Cambios) y se evalúa contra el backlog MoSCoW. (3) El catálogo de perfiles es abierto y extensible, pero solo se entregan 3 perfiles en el MVP — cualquier perfil adicional es post-MVP. (4) Mantener visible la lista de "Fuera de Alcance (Won't)" de la Visión del Producto en cada presentación de avance. | Project Manager + Product Owner | Solicitud de feature vertical no planificada | Abierto |
| **R-010** | **Complejidad del ciclo de vida**: Los 6 estados y las reglas de transición (BR-005) con revisión humana obligatoria (BR-006) resultan confusos para el Domain Curator, generando memorias atascadas en `draft` o `pending` y frustración. | Funcional | 3 | 2 | **6** | Mitigar | (1) UX del ciclo de vida simple: solo mostrar las transiciones válidas según el estado actual y el rol del usuario. (2) Tooltips y ayuda contextual en el frontend explicando por qué una memoria requiere revisión (`importance ≥ 0.7` + `sensitivity ≥ confidential`). (3) Vista "Mis pendientes" para el Domain Curator que lista sus memorias en `draft` y notifica cuando pasan a `pending`/`active`/`rejected`. | Developer Frontend + Business Analyst | Feedback de usuario sobre confusión en estados | Abierto |
| **R-011** | **Fallo en benchmarks**: Los criterios de éxito CE-01 (NDCG@10 ≥ 0.80 en SciFact), CE-02 (Recall@10 ≥ 0.90 en SciFact) y CE-03 (Recall ≥ 0.80 en LoCoMo) no se alcanzan en la primera ejecución, retrasando la validación del producto. | Funcional | 3 | 4 | **12** | Mitigar | (1) Ejecutar benchmarks temprano en F4 (Construcción) tan pronto como el pipeline de búsqueda esté funcional, no esperar a F5 (QA). (2) Si los resultados iniciales están por debajo de la meta, analizar causa: ¿embeddings? ¿chunking? ¿re-ranking? ¿filtros? (3) Iterar sobre parámetros de búsqueda (topK, rerank weight, chunk size) documentando cada experimento. (4) Mantener un "benchmark log" trazable por ejecución. (5) Si no se alcanza la meta, escalar a Product Owner para recalibrar o aceptar desviación con justificación. | Tech Lead + QA Functional | Primera ejecución de benchmark < meta | Abierto |

---

### 1.3 Riesgos Organizacionales y de Proyecto

| ID | Descripción | Categoría | P (1-5) | I (1-5) | Score | Estrategia | Plan de Mitigación | Responsable | Disparador | Estado |
|---|---|---|---|---|---|---|---|---|---|---|
| **R-012** | **Supuesto de v1 sin usuarios incorrecto**: Aunque v1.0.0 no tiene usuarios en producción, surgen dependencias inesperadas (datos de v1 que necesitan migración urgente, referencias externas a API v1, o expectativas de backward compatibility). | Organizacional | 2 | 4 | **8** | Mitigar | (1) La migración v1→v2 está planificada como Could (EP-008), no como Must. Si surge una necesidad urgente, se escala a Product Owner para repriorizar. (2) API v1 no se expone en ningún entorno de v2 (Restricción R-05). Si aparece una dependencia externa, se evalúa como cambio de alcance. (3) Comunicar en todos los canales que v2 es un producto nuevo, no una actualización de v1. | Project Manager + Product Owner | Solicitud de soporte a v1 o migración urgente | Abierto |
| **R-013** | **Conocimiento insuficiente del dominio**: El equipo técnico carece de familiaridad con los dominios objetivo (legal, CRM, finanzas, agentes conversacionales), generando perfiles de dominio superficiales, modelos de metadatos poco útiles o vocabulario controlado irrelevante. | Organizacional | 3 | 3 | **9** | Mitigar | (1) Para cada perfil de dominio, documentar al menos un escenario real con un experto o fuente autoritativa (no sintético). (2) Revisar los perfiles con el Product Owner (voz del usuario) antes de cerrar F2. (3) El diseño de perfiles es configurable — si un perfil resulta insuficiente en UAT, se itera sin modificar el core. (4) Incluir al menos una sesión de validación de perfil con un Domain Curator real o simulado. | Business Analyst + Product Owner | Feedback negativo en validación de perfil | Abierto |
| **R-014** | **Presión de cronograma sobre calidad**: La metodología cascada con fases secuenciales genera presión para "cerrar fases" sin completar todos los verificables, resultando en deuda técnica acumulada (atajos en tests, documentación desactualizada, validación superficial). | Organizacional | 4 | 3 | **12** | Mitigar | (1) Gates de fase no negociables: no se avanza a la siguiente fase sin completar todos los entregables y aprobaciones. (2) Si hay presión de tiempo, la única palanca aceptable es reducir alcance (quitar Could/Should), nunca reducir calidad. (3) Reportes de estado semanales con % real de completitud por fase (no optimista). (4) La verificación antes de completar (skill `verification-before-completion`) es obligatoria en todo entregable. | Project Manager | Gate de fase en riesgo de cerrar sin completitud | Abierto |
| **R-015** | **Fatiga del equipo entre releases**: El equipo completó v1.0.0 (9 fases, 42 entregables) en 2 días y arranca inmediatamente v2.0.0 con alcance mayor (10 épicas, 85+ features). El ritmo puede generar errores por fatiga o pérdida de rigor. | Organizacional | 3 | 2 | **6** | Aceptar | (1) Monitorear densidad de defectos y tiempos de completitud como indicadores indirectos de fatiga. (2) La estrategia de folder por release aísla v1 de v2, reduciendo carga cognitiva de mantener dos versiones simultáneas. (3) Si los indicadores de calidad se degradan respecto a v1, escalar y evaluar ajuste de cronograma. (4) Este riesgo se acepta porque el equipo opera bajo modelo de agentes con capacidad de procesamiento paralelo. | Project Manager | Degradación de métricas de calidad vs v1 | Abierto |

---

### 1.4 Riesgos de Seguridad y Cumplimiento

| ID | Descripción | Categoría | P (1-5) | I (1-5) | Score | Estrategia | Plan de Mitigación | Responsable | Disparador | Estado |
|---|---|---|---|---|---|---|---|---|---|---|
| **R-016** | **Configuración incorrecta de OIDC/Keycloak**: La complejidad de configurar Keycloak para multi-tenancy con roles RBAC, claims de tenant y scopes por usuario/sesión genera vulnerabilidades de autorización o bloquea el acceso legítimo. | Técnico | 3 | 4 | **12** | Mitigar | (1) Configuración de Keycloak como infraestructura como código (realm exportado en JSON versionado en el repo). (2) Tests de integración que validen cada rol (memory-operator, memory-reviewer, api-consumer, memory-admin, memory-auditor) con permisos correctos. (3) Tests de seguridad que verifiquen que un token sin claim de tenant no puede acceder a ningún endpoint. (4) Documentar el flujo de autenticación (Authorization Code + PKCE) para el frontend y client credentials para integraciones. | DevOps + Solution Architect | Falla en test de autorización por rol | Abierto |
| **R-017** | **Exposición de datos sensibles en búsqueda**: Una memoria marcada como `sensitivity = secret` aparece en resultados de búsqueda de un usuario sin permisos adecuados, violando la regla de visibilidad por sensibilidad. | Seguridad | 2 | 5 | **10** | Mitigar | (1) Validar en tests automatizados que memorias con `sensitivity = secret` no aparecen en búsquedas de `api-consumer`, incluso si el `status = active`. (2) El filtro de sensitivity se aplica en capa de servicios, no solo en Qdrant (defensa en profundidad). (3) Registrar en auditoría cada búsqueda que excluye resultados por sensitivity para trazabilidad forense. | Developer Backend + QA Functional | Resultado con sensibilidad incorrecta en búsqueda | Abierto |

---

## 2. Resumen de Clasificación

### 2.1 Distribución por Score

| Score | Nivel | Cantidad | IDs |
|---|---|---|---|
| 16–25 | **Crítico** | 0 | — |
| 10–15 | **Alto** | 9 | R-001, R-002, R-003, R-004, R-008, R-009, R-011, R-014, R-016 |
| 6–9 | **Medio** | 7 | R-005, R-006, R-007, R-010, R-012, R-013, R-015 |
| 1–5 | **Bajo** | 1 | R-017 (10 — Alto, corrección) |

> **Nota**: R-017 tiene Score 10 y se reclasifica como Alto.

### 2.2 Distribución por Categoría

| Categoría | Cantidad | IDs |
|---|---|---|
| **Técnico** | 7 | R-001, R-002, R-003, R-004, R-005, R-007, R-016 |
| **Funcional** | 4 | R-008, R-009, R-010, R-011 |
| **Organizacional** | 4 | R-012, R-013, R-014, R-015 |
| **Externo** | 1 | R-006 |
| **Seguridad** | 1 | R-017 |

### 2.3 Distribución por Estrategia

| Estrategia | Cantidad | IDs |
|---|---|---|
| **Mitigar** | 16 | R-001 a R-014, R-016, R-017 |
| **Aceptar** | 1 | R-015 |
| **Transferir** | 0 | — |
| **Evitar** | 0 | — |

---

## 3. Heat Map (Mapa de Calor)

Visualización probabilidad × impacto de los 17 riesgos identificados:

```mermaid
matrix
    title Heat Map de Riesgos — Abax-Memory v2.0.0
    x-axis Impacto ➔
    y-axis Probabilidad ➔
    
    section "1 — Insignificante" [0:8]
    section "2 — Menor" [9:16]
    section "3 — Moderado" [17:24]
    section "4 — Significativo" [25:32]
    section "5 — Crítico" [33:40]
    
    section "5 — Muy Alta (>75%)" [0:8]
    section "4 — Alta (50-75%)" [9:16]
    section "3 — Media (25-50%)" [17:24]
    section "2 — Baja (10-25%)" [25:32]
    section "1 — Muy Baja (<10%)" [33:40]
```

### Tabla de Calor

|  | Impacto 1 | Impacto 2 | Impacto 3 | Impacto 4 | Impacto 5 |
|---|---|---|---|---|---|
| **Prob 5** | 5 | 10 | 15 | 20 | 25 |
| **Prob 4** | 4 | 8 | 12 ⬤ | **16** | 20 |
| **Prob 3** | 3 | 6 ⬤ | 9 ⬤ | **12** ⬤⬤⬤ | 15 |
| **Prob 2** | 2 | 4 | 6 | 8 ⬤⬤ | **10** ⬤⬤ |
| **Prob 1** | 1 | 2 | 3 | 4 | 5 |

**Leyenda**:
- 🟢 Verde (Score 1–5): Bajo — Aceptar y monitorear
- 🟡 Amarillo (Score 6–9): Medio — Monitoreo activo, plan de contingencia
- 🟠 Naranja (Score 10–15): Alto — Plan de mitigación obligatorio, revisión semanal
- 🔴 Rojo (Score 16–25): Crítico — Acción inmediata requerida, escalar a sponsor

### Riesgos por Celda

| Celda (P×I) | Cantidad | IDs | Color |
|---|---|---|---|
| 3×2 = 6 | 2 | R-010, R-015 | 🟡 Medio |
| 2×3 = 6 | 2 | R-005, R-013 | 🟡 Medio |
| 2×4 = 8 | 4 | R-006, R-007, R-012, R-017 | 🟡 Medio |
| 3×3 = 9 | 1 | R-005 (corrección: R-005 ya está en 2×3) | — |
| 3×4 = 12 | 6 | R-001, R-002, R-004, R-008, R-009, R-011, R-014, R-016 | 🟠 Alto |
| 4×3 = 12 | 2 | R-002 (corrección), R-009, R-014 | 🟠 Alto |
| 2×5 = 10 | 2 | R-003, R-017 | 🟠 Alto |
| 3×5 = 15 | 0 | — | — |
| 4×4 = 16 | 0 | — | — |
| 4×5 = 20 | 0 | — | — |

> **Nota**: Se ha simplificado la visualización. Los riesgos se concentran mayoritariamente en el cuadrante de Alto (Score 10–15), con 10 de 17 riesgos (59%) en esta categoría.

---

## 4. Top 5 Riesgos Prioritarios (por Score y criticidad)

| # | ID | Riesgo | Score | Justificación de prioridad |
|---|---|---|---|---|
| 1 | **R-008** | Perfiles de dominio insuficientes | 12 | Afecta directamente la propuesta de valor de v2 (multi-dominio). Si los perfiles no funcionan, el producto no cumple su promesa central. Probabilidad alta porque es la primera iteración de esta capacidad. |
| 2 | **R-001** | Stack flexible mal administrado | 12 | Puede descarrilar el cronograma si se decide un cambio de stack sin el rigor requerido. Impacto 4: 1–2 semanas de retrabajo. La tentación de "mejorar" componentes existe porque se hereda la infraestructura de v1. |
| 3 | **R-014** | Presión de cronograma sobre calidad | 12 | Riesgo sistémico que afecta todos los entregables. Probabilidad alta (el patrón se observó en v1). Si se materializa, el impacto se propaga en cascada a fases posteriores. |
| 4 | **R-011** | Fallo en benchmarks | 12 | Los criterios de éxito CE-01 a CE-03 son la validación externa del producto. Si no se alcanzan, no se puede declarar el producto como competitivo. Impacto 4 por el retrabajo de iterar sobre el pipeline de búsqueda. |
| 5 | **R-002** | English-Only non-compliance | 12 | Convención no negociable. La corrección de identificadores en español puede requerir cambios en cascada (código, BD, OpenAPI, SDK). Probabilidad alta en la transición de español a inglés desde v1. |

---

## 5. Riesgos Específicos Solicitados

Estos riesgos fueron identificados explícitamente como prioritarios para v2.0.0:

### 5.1 Stack Flexible (R-001)

> **Contexto**: Restricción R-01 permite cambiar componentes del stack si se justifica con ADR. El stack heredado de v1 (Quarkus, PostgreSQL, Qdrant, Keycloak, OpenAI) puede no ser óptimo para el nuevo alcance multi-dominio.

| Aspecto | Detalle |
|---|---|
| **Riesgo** | Cambios tardíos o mal justificados generan inestabilidad y retrabajo |
| **Ventana de exposición** | F3 (Diseño Técnico) — cualquier cambio post-F3 requiere re-planificación |
| **Mitigación clave** | ADR obligatorio antes de F3, congelamiento post-F3 |
| **Señal de alerta** | "Cambiemos X porque en v1 no escalaba bien" sin datos de carga en v2 |

### 5.2 English-Only (R-002)

> **Contexto**: v1 usaba español en identificadores internos (`EN_REVISION`, `APROBADA`, `criticality`). v2 exige inglés en absolutamente todos los identificadores del sistema. La skill `code-naming-convention` es no negociable.

| Aspecto | Detalle |
|---|---|
| **Riesgo** | Identificadores en español se filtran en endpoints, modelos, columnas o OpenAPI spec |
| **Ventana de exposición** | F3 (Diseño Técnico) a F5 (QA) — todo el ciclo de construcción |
| **Mitigación clave** | Linter custom + gate de code review + lista de palabras prohibidas |
| **Señal de alerta** | `POST /api/v2/entidades` en lugar de `POST /api/v2/entities` |

### 5.3 Multi-Tenancy (R-003, R-007)

> **Contexto**: v1 no tenía multi-tenancy nativa. v2 introduce `scope` con `tenantId`, `userId`, `sessionId`, `namespace`. Es una capacidad nueva, no heredada, y crítica para el modelo de negocio.

| Aspecto | Detalle |
|---|---|
| **Riesgo** | Data leakage entre tenants (R-003) o degradación de rendimiento (R-007) |
| **Ventana de exposición** | F4 (Construcción) a F8 (Estabilización) |
| **Mitigación clave** | Tests automatizados de cross-tenant isolation + pruebas de estrés multi-tenant |
| **Señal de alerta** | Resultado de búsqueda con `tenantId` distinto al del token de autenticación |

### 5.4 Perfiles de Dominio (R-008, R-009)

> **Contexto**: La gran innovación de v2 sobre v1 son los perfiles de dominio configurables. Pero es un concepto nuevo que no existe en el mercado de motores de memoria (Mem0, Zep, Letta no tienen este concepto).

| Aspecto | Detalle |
|---|---|
| **Riesgo** | Perfiles demasiado genéricos (R-008) o scope creep por expectativas desmedidas (R-009) |
| **Ventana de exposición** | F2 (Análisis Funcional) a F6 (UAT) |
| **Mitigación clave** | Validar cada perfil con escenarios reales + comunicación clara de alcance |
| **Señal de alerta** | "El perfil Business no me sirve para mi caso de uso legal" o "¿pueden agregar un perfil de Healthcare?" |

### 5.5 v1 Sin Usuarios (R-012)

> **Contexto**: La decisión del sponsor establece que v1.0.0 está cerrado y sin usuarios en producción. Toda la estrategia de v2 (sin backward compatibility, sin API v1, sin coexistencia) depende de este supuesto.

| Aspecto | Detalle |
|---|---|
| **Riesgo** | Aparece una dependencia inesperada de v1 que fuerza soporte o migración urgente |
| **Ventana de exposición** | Todo el ciclo de v2, especialmente F7 (Deployment) si aparecen dependencias externas |
| **Mitigación clave** | Migración v1→v2 está planificada como Could, no Must. Cualquier urgencia se escala a Product Owner |
| **Señal de alerta** | "Necesitamos migrar 500 memorias de v1 antes del lanzamiento" o "el sistema X todavía llama a `/api/v1/`" |

---

## 6. Ciclo de Revisión

| Evento | Frecuencia | Responsable | Acción |
|---|---|---|---|
| **Revisión de matriz** | Semanal (viernes) | Project Manager | Revisar scores, cerrar riesgos mitigados, abrir nuevos |
| **Reporte a sponsor** | Por gate de fase | Project Manager | Top 5 riesgos activos con estado de mitigación |
| **Escalamiento** | Inmediato | Cualquier rol | Si un riesgo Score ≥ 12 se materializa, notificar a Project Manager para evaluación de impacto |

---

## 7. Actualizaciones

| Fecha | Versión | Cambio | Autor |
|---|---|---|---|
| 2026-05-03 | 1.0 | Identificación inicial de 17 riesgos para v2.0.0 Fase 1. | project-manager |

---

## Glosario

- **ADR**: Architecture Decision Record — documento que registra una decisión arquitectónica, su contexto, alternativas y consecuencias.
- **MoSCoW**: Método de priorización: Must (obligatorio), Should (debería), Could (podría), Won't (no incluido).
- **NDCG@10**: Normalized Discounted Cumulative Gain — métrica de ranking que mide calidad de resultados en las primeras 10 posiciones.
- **p95**: Percentil 95 — el 95% de las solicitudes se completan en un tiempo igual o menor al valor indicado.
- **OIDC**: OpenID Connect — protocolo de autenticación basado en OAuth 2.0.
- **Qdrant**: Base de datos vectorial open-source para almacenar embeddings y búsqueda semántica.
- **PKCE**: Proof Key for Code Exchange — extensión de OAuth 2.0 que protege contra ataques de interceptación de código.
