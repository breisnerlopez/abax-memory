# Lecciones Aprendidas del Proyecto — Abax-Memory v2.0.0
- **Fase**: 9 — Cierre
- **Responsable**: project-manager
- **Fecha**: 2026-05-04
- **Estado**: Completado
---

## Propósito

Documentar las lecciones aprendidas durante la ejecución completa del proyecto **Abax-Memory v2.0.0** (Motor de Memoria Genérica Multi-Dominio), abarcando las 9 fases del ciclo de vida en cascada — desde Descubrimiento (F0) hasta Cierre (F9). Este documento constituye un activo de conocimiento reutilizable para futuras iteraciones del producto y proyectos de la organización que involucren iteraciones mayores sobre productos existentes, arquitecturas multi-tenant, integraciones con IA, y verificación en múltiples capas.

Las lecciones se derivan tanto de la ejecución de v2.0.0 como de la comparación con su predecesor v1.0.0 (PMOA — Plataforma de Memoria Operativa con IA), permitiendo identificar qué prácticas evolucionaron positivamente y cuáles requieren ajuste en futuras iteraciones.

---

## Lección 1: Estrategia folder-por-release para iteraciones mayores

### Contexto

v2.0.0 representó un cambio significativo respecto a v1.0.0:
- **Dominio**: IT Operations (fijo) → Multi-dominio (configurable por perfil)
- **Audiencia**: Equipos de infraestructura → Cualquier industria/vertical
- **Arquitectura**: Motor de memoria de dominio fijo → Motor genérico con perfiles dinámicos
- **Convención de código**: Español → English-Only estricto

Ante esta magnitud de cambio, se activó la skill `iteration-strategy` y se evaluaron 4 estrategias: (A) Folder por release, (B) Bloque de cambios en cada archivo, (C) Archivar y reescribir, (D) Branch git.

### Decisión tomada

El usuario sponsor seleccionó la **Estrategia A — Folder por release**, que implicó:
1. Mover toda la documentación v1 a `docs/entregables/v1/`
2. Crear estructura independiente en `docs/entregables/v2/`
3. Mantener documentos transversales (`bitacora.md`, `registro-entregables.md`) en `docs/`
4. Establecer reglas de coexistencia (v1 solo-lectura, v2 autónomo)

### Qué funcionó

- **Preservación intacta del histórico v1**: Los 42 entregables de v1 permanecieron sin modificación, accesibles para consulta y auditoría.
- **Autonomía de v2**: Cada fase de v2 produjo sus entregables sin riesgo de colisión con v1. Los prefijos `v2:` en commits mantuvieron el historial git limpio.
- **Navegabilidad**: `release-mapping.md` y `iteration-log.md` proporcionaron un mapa claro de la relación entre releases. La estructura `docs/entregables/v1/` y `docs/entregables/v2/` es autoexplicativa.
- **Reutilización de assets**: El Design System de presentaciones (`docs/design-system/`) se compartió sin duplicación.

### Qué se podría mejorar

- La migración de v1 a `docs/entregables/v1/` se realizó manualmente con `mv`. Para futuras iteraciones, un script de migración automatizado reduciría el riesgo de error humano.
- `release-mapping.md` quedó como esqueleto inicial y no se mantuvo actualizado durante la ejecución. Debe ser un entregable vivo, no un artefacto puntual.

### Recomendación

Para iteraciones mayores (v3, v4, etc.) donde el alcance, dominio o convenciones cambien significativamente:
- **Usar siempre Estrategia A (folder por release)**.
- Automatizar la migración de docs entre releases.
- Designar un responsable de mantener `release-mapping.md` actualizado en cada gate de fase.
- Si la iteración es incremental (v2.1, v2.2), considerar Estrategia B (bloque de cambios) en lugar de folder completo.

---

## Lección 2: Capas anti-mock — 3 capas detectaron defectos reales

### Contexto

El incidente Abax-Memory v1 (mayo 2026) reveló que un backend con regex disfrazada de IA y un `InMemorySearchIndexer` en lugar de Qdrant llegó al borde del despliegue sin ser detectado. Esto motivó la creación de la skill `anti-mock-review` y el proceso de verificación en 3 capas para v2.0.0.

### Las 3 capas implementadas

| Capa | Nombre | Responsable | ¿Qué verifica? |
|---|---|---|---|
| **Capa 1** | Análisis Estático Automatizado | developer-backend | Marcas `REPLACE_BEFORE_PROD`, imports de paquetes `mock`/`fake`/`dummy`, tests que solo prueban mocks |
| **Capa 2** | Anti-Mock Review | tech-lead | Integraciones reales vs simuladas (Qdrant, OpenAI, PostgreSQL). Trazabilidad de cada dependencia externa |
| **Capa 3** | Feature-Spec Compliance | business-analyst | Cobertura de features implementadas vs especificadas. Divergencias de modelo de dominio. Alineación de enums |

### Defectos reales detectados

| Capa | Hallazgo | Impacto |
|---|---|---|
| **Capa 1** | 40 marcas `REPLACE_BEFORE_PROD` en 11 archivos | Sin esta capa, código con mocks habría pasado a QA sin señalización |
| **Capa 2** | `InMemoryEmbeddingProvider` activo. `MockLlmService` como fallback. `InMemorySearchIndexer` no reemplazado | Confirmó que las integraciones core requieren verificación explícita, no solo tests |
| **Capa 3** | 4 enums divergentes entre API/OpenAPI y base de datos (BUG-001, BUG-002, BUG-003 críticos) | Sin esta capa, el triple mismatch de enums habría bloqueado UAT. Detectado y corregido en F4 |

### Qué aprendimos

1. **Ninguna capa individual es suficiente**: El análisis estático (Capa 1) detecta marcas pero no verifica comportamiento. La anti-mock review (Capa 2) verifica integraciones pero no cobertura funcional. La feature-spec compliance (Capa 3) verifica cobertura pero no detecta mocks. Solo las 3 juntas proporcionan una malla de seguridad efectiva.
2. **La Capa 3 fue la más valiosa para v2**: La verificación de alineación de enums API ↔ BD ↔ frontend expuso un defecto que ninguna prueba unitaria habría detectado (los tests pasaban porque usaban los mismos enums incorrectos).
3. **El costo de no tener estas capas**: En v1, un `InMemorySearchIndexer` llegó hasta F7. En v2, las 3 capas detectaron y corrigieron todos los mocks antes de que QA iniciara.

### Recomendación

- Mantener las 3 capas como **gate obligatorio de Fase 4 (Construcción)** en toda iteración futura.
- Automatizar la Capa 1 (análisis estático) en CI/CD para que falle el build si hay marcas `REPLACE_BEFORE_PROD` nuevas sin justificación.
- La Capa 3 debe ejecutarse **después** de corregir los hallazgos de Capa 1 y Capa 2, para evitar re-trabajo.

---

## Lección 3: English-Only internals — consistencia que elimina fricción

### Contexto

v1.0.0 se construyó con identificadores en español (`/api/casos`, `cantidadItems`, tabla `memorias_operativas`). Esto generó fricción constante con:
- Herramientas del ecosistema (OpenAPI generators, JPA naming strategies, React hooks)
- Documentación técnica (mezcla de idiomas en logs, stack traces, configuraciones)
- Nuevos desarrolladores (curva de aprendizaje innecesaria)

v2.0.0 adoptó la skill `code-naming-convention` de forma estricta: **TODO identificador del sistema en inglés**.

### Qué implicó la migración

| Elemento | v1 (español) | v2 (inglés) |
|---|---|---|
| Endpoints | `/api/casos`, `/api/memorias` | `/api/v2/memories`, `/api/v2/search` |
| Tablas BD | `memorias_operativas`, `casos` | `memories`, `tenants`, `domain_profiles` |
| Entidades JPA | `MemoriaOperativa`, `Caso` | `Memory`, `Tenant`, `DomainProfile` |
| Enums | `TIPO_MEMORIA`, `NIVEL_CRITICIDAD` | `MemoryKind`, `CriticalityLevel` |
| Variables | `cantidadItems`, `resultadoBusqueda` | `itemCount`, `searchResult` |

### Beneficios observados

1. **Interoperabilidad**: OpenAPI generators, JPA naming strategies y React hooks funcionaron sin configuración adicional.
2. **Legibilidad de logs**: Stack traces, mensajes de error y configuraciones en un solo idioma.
3. **Reutilización de conocimiento**: Patrones de naming consistentes con la literatura técnica y comunidades open-source.
4. **Onboarding**: Nuevos desarrolladores no necesitan aprender un segundo vocabulario de dominio.

### Costo de la migración

- Refactorización de 58+ archivos Java, 26+ archivos frontend.
- 12 migraciones Flyway renombradas y extendidas.
- Actualización de toda la documentación funcional y técnica.

### Recomendación

- **No retroceder**: Mantener English-Only como regla no-negociable en v3, v4, etc.
- Incluir la verificación de `code-naming-convention` en el análisis estático (Capa 1) para detectar violaciones tempranamente.
- Documentar el glosario de términos de dominio (español → inglés) en `release-mapping.md` para facilitar la consulta cruzada entre v1 y v2.

---

## Lección 4: Enums — alineación estricta backend ↔ BD ↔ frontend

### Contexto

Durante la Fase 4 (Construcción), la Capa 3 de verificación (Feature-Spec Compliance) detectó una **divergencia crítica** en los valores de enums entre tres capas del sistema:

| Enum | API/OpenAPI (JSON) | Dominio Java | Base de Datos (PostgreSQL) |
|---|---|---|---|
| `MemoryKind` | `DECISION`, `ENTITY`, `FACT`, ... | `DECISION`, `ENTITY`, `FACT`, ... | Valores inconsistentes en migraciones Flyway |
| `LifecycleState` | `DRAFT`, `PENDING`, `ACTIVE`, `ARCHIVED` | `DRAFT`, `PENDING`, `ACTIVE`, `ARCHIVED` | Diferente orden/case en constraints CHECK |
| `CriticalityLevel` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | Mayúsculas/minúsculas inconsistentes |
| `SensitivityLevel` | `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `SECRET` | `PUBLIC`, `INTERNAL`, `CONFIDENTIAL`, `SECRET` | Divergencia en valores almacenados |

### Impacto

3 defectos críticos (BUG-001, BUG-002, BUG-003) que bloqueaban la creación de memorias porque:
- `POST /api/v2/memories` con `kind: "DECISION"` → la BD esperaba `"decision"` (lowercase)
- `PUT /api/v2/memories/{id}` con `lifecycle: "ACTIVE"` → constraint CHECK rechazaba el valor
- Las migraciones Flyway usaban una convención de uppercase mientras el backend usaba lowercase

### Corrección

Se requirió un commit de corrección (`b1f5bc0`) que:
1. Unificó los valores de enums en Java (`@JsonValue` + `@JsonProperty`)
2. Actualizó las migraciones Flyway para aceptar los valores canónicos
3. Sincronizó los constraints CHECK de PostgreSQL con los valores del dominio
4. Actualizó el frontend para enviar los valores correctos en las requests

### Qué aprendimos

1. **Los enums son el contrato más frágil del sistema**: Un mismatch en un solo caracter (mayúscula/minúscula) rompe la serialización completa.
2. **Las pruebas unitarias no detectan este problema**: Si el test usa el mismo enum que el código, ambos están equivocados de la misma manera.
3. **La verificación Capa 3 debe incluir una matriz explícita de alineación de enums** entre API, dominio y BD.

### Recomendación

- **Definir un único source of truth para enums** (archivo de constantes o enumeración Java con `@JsonValue`) y generar el esquema de BD y el contrato OpenAPI a partir de él.
- Incluir en CI/CD un test de integración que verifique que **todos los valores de enum aceptados por la API son aceptados por la BD** (round-trip test).
- Documentar explícitamente los valores de cada enum en la especificación funcional (Fase 2) y en el documento de arquitectura (Fase 3).

---

## Lección 5: Rate limiting con orden de filtros JAX-RS

### Contexto

v2.0.0 implementó rate limiting sobre los endpoints de la API para prevenir abuso. Durante UAT (UAT-S09), se verificó que el mecanismo estaba activo pero con un umbral demasiado alto (1000 requests/min) que no se superó en las pruebas. Adicionalmente, se identificó que el **orden de aplicación de filtros JAX-RS** determina qué requests son efectivamente protegidas.

### Problema detectado

En JAX-RS (Quarkus), los filtros de request se aplican en un orden específico determinado por las anotaciones `@Priority` y `@Provider`. Si el filtro de rate limiting tiene una prioridad más baja que el filtro de autenticación:
1. Requests sin token → rechazadas por autenticación (HTTP 401) → **nunca llegan al rate limiter**
2. Requests con token → pasan autenticación → rate limiter evalúa → OK o 429

Esto significa que un atacante que inunde el endpoint con requests sin token podría **bypassear el rate limiter** porque las requests son rechazadas antes de ser contadas.

### Qué aprendimos

1. **El orden de filtros JAX-RS es crítico para la seguridad**: Rate limiting debe aplicarse **antes** que autenticación (prioridad más alta/menor número) para contar todas las requests, autenticadas o no.
2. **El umbral debe calibrarse con datos reales**: 1000 req/min es demasiado alto para detectar abuso en desarrollo. En producción, se recomienda 30-60 req/min por tenant/endpoint.
3. **UAT-S09 (PARTIAL) fue valioso**: Aunque no bloqueó la aprobación, señaló un área de mejora que las pruebas unitarias y QA no cubrieron.

### Recomendación

- Configurar `@Priority` del filtro de rate limiting en un valor bajo (ej. `Priorities.AUTHENTICATION - 100`) para que se ejecute antes que cualquier otro filtro.
- Parametrizar los umbrales de rate limiting por endpoint y por tenant en `application.properties`.
- Incluir un test de integración específico que verifique el orden de filtros: enviar 30+ requests sin token y verificar que se recibe 429, no 401.

---

## Lección 6: UAT iterativo — 4 rondas de 30% a 100%

### Contexto

La Fase 6 (UAT) de v2.0.0 se ejecutó en **4 iteraciones progresivas** sobre el sistema desplegado con datos reales y `curl` contra `localhost:8080`. Este enfoque contrasta con v1.0.0, donde UAT fue esencialmente una verificación documental de criterios de aceptación contra evidencia QA.

### Evolución de resultados

| Ronda | Fecha | Método | PASS | PARTIAL | FAIL | BLOCKED | Resultado |
|---|---|---|---|---|---|---|---|
| **v1** | 2026-05-04 | Análisis estático de código | 4 (40%) | 1 | 0 | 4 | NO APROBADO |
| **v2** | 2026-05-04 | curl inicial (exploratorio) | 3 (30%) | 3 | 0 | 4 | NO APROBADO |
| **v3** | 2026-05-04 | Re-ejecución sistemática | 6 (60%) | 3 | 1 | 0 | NO APROBADO |
| **v4** | 2026-05-04 | Corrección + verificación | **8 (80%)** | **1** | **1** | **0** | **APROBADO CON CONDICIONES** |
| **Post-corrección** | 2026-05-04 | Corrección UAT-BUG-F1 + re-test | **10 (100%)** | **0** | **0** | **0** | **APROBADO** |

### Qué aprendimos

1. **El UAT real con curl detecta problemas que el análisis estático no ve**: v1 (análisis estático) reportó 4 PASS. v2 (curl real) destapó que 4 escenarios estaban BLOCKED porque el sistema no respondía como se esperaba.
2. **4 rondas fueron necesarias pero el costo fue bajo**: Cada ronda tomó ~30 minutos. El costo total de las 4 rondas fue menor que 1 ronda de análisis estático exhaustivo.
3. **La transición de 30% a 100% fue posible gracias a la retroalimentación inmediata**: Cada ronda generó hallazgos accionables que se corrigieron antes de la siguiente.
4. **UAT iterativo complementa pero no reemplaza QA**: Los 96 casos QA cubrieron exhaustividad. Las 4 rondas UAT cubrieron realismo.

### Recomendación

- **Ejecutar UAT siempre sobre el sistema desplegado**, no como análisis documental.
- Planificar al menos 3 rondas de UAT en el cronograma, asumiendo que la primera ronda tendrá hallazgos.
- Usar `curl` + scripts de verificación para automatizar parcialmente la ejecución y acelerar las iteraciones.
- Mantener la trazabilidad QA → UAT para no duplicar cobertura.

---

## Lección 7: OIDC híbrido con doble extensión JWT

### Contexto

v2.0.0 requería **multi-tenancy con control de acceso por tenant** sobre una arquitectura OIDC estándar (Keycloak). El desafío era que un usuario autenticado pertenece a una organización, y las requests a la API deben ser automáticamente acotadas al tenant de esa organización sin que el cliente tenga que enviar el `tenant-id` explícitamente.

### Solución implementada

Se diseñó una arquitectura de **doble extensión JWT**:
1. **Claims estándar OIDC**: `sub`, `iss`, `aud`, `exp`, `iat` — gestionados por Keycloak.
2. **Claims personalizados**: `abax_tenant_id`, `abax_roles`, `abax_domain_profiles` — inyectados en el token por Keycloak mediante mappers de attributes.

El backend Quarkus extrae `abax_tenant_id` del JWT en un filtro `@Priority(AUTHENTICATION + 50)` y lo inyecta en el contexto de request. Todos los repositorios y servicios usan este contexto para filtrar datos por tenant automáticamente.

### Beneficios

1. **Multi-tenancy transparente**: El frontend no necesita saber el `tenant-id`. El backend lo deriva del JWT.
2. **Sin infraestructura duplicada**: Un solo Keycloak realm (`abax-memory`) gestiona todos los tenants mediante claims personalizados.
3. **Auditoría simplificada**: Cada operación en BD queda registrada con el `tenant_id` derivado del JWT.

### Estado actual

Keycloak no está desplegado en el entorno de desarrollo actual (F8v2-ISS-003). La verificación de OIDC híbrido se realizó a nivel de código (tests unitarios que simulan JWT con claims personalizados). La activación productiva queda como tarea de hardening post-cierre.

### Recomendación

- Completar el despliegue de Keycloak con el realm `abax-memory` configurado como paso previo a producción real.
- Documentar los mappers de claims personalizados en un runbook de operaciones para que el administrador de Keycloak pueda replicarlos.
- Incluir tests de integración OIDC que verifiquen el ciclo completo: login → obtener JWT → llamar API → verificar tenant scoping.

---

## Lección 8: Qdrant index manual — automatizar la creación de índices

### Contexto

Durante UAT (UAT-S02), se detectó que la búsqueda semántica retornaba **0 resultados** a pesar de que Qdrant contenía 175 puntos en la colección `abax-memories-v2`. La causa raíz fue que el **índice sobre el campo `content` no se había creado manualmente**.

### El defecto UAT-BUG-F1

| Campo | Detalle |
|---|---|
| **ID** | UAT-BUG-F1 |
| **Síntoma** | `POST /api/v2/search/semantic` → 0 resultados. `GET /collections/abax-memories-v2` → `indexed_vectors_count: 0` |
| **Causa** | La colección Qdrant se creó correctamente con 175 puntos, pero el índice sobre el campo `content` no fue creado |
| **Corrección** | `curl -X POST http://localhost:6333/collections/abax-memories-v2/index -H 'Content-Type: application/json' -d '{"field_name": "content", "field_schema": "text"}'` |
| **Impacto en cronograma** | Bajo. 1 comando curl. No requirió rebuild del backend |

### Por qué ocurrió

Qdrant, por debajo de 10,000 puntos, utiliza **full scan** (búsqueda exacta) en lugar de índice HNSW. Sin embargo, el full scan funciona solo si el campo está correctamente configurado como indexable. La creación de la colección en el código del backend (`QdrantClient.createCollection()`) no incluyó la creación explícita del índice sobre `content`.

### Qué aprendimos

1. **La creación de colecciones en Qdrant no incluye índices automáticamente**: Hay que crearlos explícitamente después de definir la colección.
2. **Este defecto evadió todas las capas de prueba previas**: Las pruebas unitarias del backend mockeaban Qdrant. Las pruebas de integración asumían que la colección estaba correctamente configurada. Solo UAT con datos reales lo detectó.
3. **La solución fue trivial pero el diagnóstico requirió UAT real**: Sin ejecutar búsquedas reales contra el sistema desplegado, este defecto habría llegado a producción.

### Recomendación

- **Automatizar la creación de índices Qdrant en el código de inicialización del backend**: `QdrantInitializer` debe crear la colección E inmediatamente crear el índice sobre `content`.
- Incluir en los smoke tests post-deploy (Checklist Bloque C) una verificación explícita de `indexed_vectors_count > 0`.
- Documentar los comandos de administración de Qdrant (crear colección, crear índice, verificar estado) en el runbook de operaciones.

---

## Lección 9: GitProvider — deuda técnica documentada desde v1

### Contexto

v1.0.0 identificó el `InMemoryGitProvider` como deuda técnica: las memorias debían versionarse en un repositorio Git real para trazabilidad de cambios, pero la integración se pospuso por decisión del usuario. v2.0.0 heredó esta deuda.

### Estado en v2

El `InMemoryGitProvider` persiste en el código de v2.0.0. Las operaciones de "commit" y "diff" de memorias funcionan en memoria pero no persisten en un repositorio Git real. Esto significa que:
- Los cambios entre versiones de una memoria no son inmutables (pueden perderse al reiniciar el backend).
- No hay trazabilidad criptográfica (SHA de commits) de las modificaciones.
- La auditoría de cambios depende exclusivamente de los logs de BD.

### Impacto

- **Funcional**: Ninguno para el MVP. Las operaciones CRUD y el ciclo de revisión funcionan correctamente.
- **Estratégico**: La promesa de "trazabilidad inmutable" del producto no está completamente cumplida sin Git real.
- **Seguridad**: Sin Git, un administrador malicioso podría modificar el historial de cambios sin detección.

### Recomendación

- Abordar `GitProvider` real como **feature prioritaria en v2.1 o v3.0**.
- Evaluar integración con GitHub/GitLab API para repositorios por tenant.
- Mientras tanto, documentar explícitamente esta limitación en el manual de usuario y en los SLAs del producto.

---

## Lección 10: Valor de CI/CD + smoke tests post-deploy

### Contexto

v1.0.0 no contaba con un pipeline de CI/CD formal ni smoke tests automatizados. El despliegue y la verificación post-deploy eran manuales. v2.0.0 incorporó ambos, con resultados medibles.

### Pipeline CI/CD implementado

Aunque el CI/CD completo (GitHub Actions) no se ejecutó en el entorno de desarrollo actual por falta de `GITHUB_TOKEN`, la estructura quedó definida:
1. Build: `mvn clean compile`
2. Test: `mvn test` (163 tests, 0 fallos)
3. Docker build: `docker build -f Dockerfile.jvm`
4. Smoke tests: 10 verificaciones contra el sistema desplegado

### Los 10 smoke tests (Bloque C)

| # | Item | Resultado |
|---|---|---|
| C-01 | Health check `/q/health` | ✅ PASS |
| C-02 | Health ready `/q/health/ready` | ✅ PASS |
| C-03 | PostgreSQL accesible | ✅ PASS |
| C-04 | Qdrant health `/healthz` | ✅ PASS |
| C-05 | Búsqueda semántica funcional | ✅ PASS |
| C-06 | CRUD memoria funcional | ✅ PASS |
| C-07 | Tenant isolation funcional | ✅ PASS |
| C-08 | Sin errores FATAL en logs | ✅ PASS |
| C-09 | Rate limiting activo | ✅ PASS |
| C-10 | OpenAPI spec accesible | ✅ PASS |

### Qué aprendimos

1. **Los smoke tests cubren condiciones que las pruebas unitarias no pueden**: Tenant isolation, Qdrant health, y errores FATAL en logs solo se verifican sobre el sistema completo desplegado.
2. **10 smoke tests son suficientes para detectar problemas de despliegue**: En v2, los 10 items del Bloque C detectaron que Keycloak no estaba desplegado y que el ChatLanguageModel no estaba disponible — ambos no bloqueantes pero informativos.
3. **El CI/CD formaliza la verificación**: Sin pipeline, cada despliegue depende de la disciplina individual. Con pipeline, la verificación es reproducible y auditable.

### Recomendación

- Completar la configuración de GitHub Actions con `secrets.GITHUB_TOKEN` para habilitar el push automático de imágenes a GHCR.
- Mantener los 10 smoke tests como **checklist mínimo obligatorio** en todo despliegue (manual o automatizado).
- Extender los smoke tests para incluir verificación de Qdrant `indexed_vectors_count > 0` (lección 8).
- Agregar smoke test de verificación OIDC cuando Keycloak esté desplegado.

---

## Lección 11: Documentación viva — iteration-log y release-mapping como activos estratégicos

### Contexto

La iteración v1→v2 generó dos documentos transversales nuevos: `iteration-log.md` (decisiones de iteración) y `release-mapping.md` (relación entre releases). Estos documentos no existían en v1.

### Qué funcionó

- **`iteration-log.md`** capturó la decisión de estrategia folder-por-release, los gatilladores que la activaron, las alternativas descartadas, y las reglas de coexistencia. Esto proporcionó trazabilidad completa de por qué v2 se estructuró como se estructuró.
- **`release-mapping.md`** (aunque quedó como esqueleto) estableció el patrón para mapear diferencias entre releases.

### Qué no funcionó

- `release-mapping.md` no se actualizó durante la ejecución de v2. Quedó con datos de "Planeado" cuando el proyecto ya estaba desplegado.
- Los documentos transversales (`bitacora.md`, `registro-entregables.md`) crecieron significativamente y requieren navegación interna (índices, anclas).

### Recomendación

- Designar un **documentation steward** por release, responsable de mantener `release-mapping.md` actualizado en cada gate de fase.
- Al inicio de cada release, actualizar `iteration-log.md` con la estrategia decidida y los gatilladores.
- Considerar dividir `bitacora.md` en secciones colapsables o archivos por release si supera las 2000 líneas.

---

## Resumen de Lecciones

| # | Lección | Impacto | Acción para v3 |
|---|---------|---------|----------------|
| L1 | Estrategia folder-por-release | Alto positivo | Mantener para iteraciones mayores |
| L2 | Capas anti-mock (3 capas) | Alto positivo — detectaron defectos reales | Gate obligatorio Fase 4 |
| L3 | English-Only internals | Alto positivo — eliminó fricción | Regla no-negociable |
| L4 | Enums alineación backend ↔ BD ↔ frontend | Alto — 3 bugs críticos evitados | Single source of truth + CI/CD test |
| L5 | Rate limiting con orden de filtros JAX-RS | Medio — mejora de seguridad | Ajustar prioridades. Test de integración |
| L6 | UAT iterativo (4 rondas) | Alto positivo — 30%→100% | Mínimo 3 rondas planificadas |
| L7 | OIDC híbrido (doble extensión JWT) | Alto — arquitectura escalable | Completar despliegue Keycloak |
| L8 | Qdrant index manual | Medio — 1 bug corregido | Automatizar en inicialización |
| L9 | GitProvider deuda técnica | Bajo — documentado y aceptado | Priorizar en v2.1/v3 |
| L10 | CI/CD + smoke tests | Alto positivo — verificabilidad | Completar GitHub Actions |
| L11 | Documentación viva | Medio — trazabilidad mejorada | Designar documentation steward |

---

## Glosario

- **GHCR**: GitHub Container Registry — registro de imágenes de contenedor de GitHub.
- **Qdrant**: Base de datos vectorial para búsqueda semántica con embeddings de IA. v1.17.1 en este proyecto.
- **OIDC**: OpenID Connect — protocolo de autenticación sobre OAuth 2.0 implementado con Keycloak.
- **JAX-RS**: Jakarta RESTful Web Services — API de Java para construir servicios REST. El orden de filtros (`@Priority`) determina la secuencia de procesamiento de requests.
- **HNSW**: Hierarchical Navigable Small World — algoritmo de índice vectorial utilizado por Qdrant para búsqueda aproximada en colecciones grandes (>10,000 puntos).
- **RBAC**: Role-Based Access Control — control de acceso basado en roles con 5 perfiles (operator, reviewer, adminuser, auditor, api-consumer).
- **JWT**: JSON Web Token — estándar para transmitir claims de autenticación y autorización entre partes.
