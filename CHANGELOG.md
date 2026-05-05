# Changelog — Abax-Memory

## [2.0.3] — 2026-05-05

### Fixed
- #12: Agregada extensión quarkus-langchain4j-openai. OpenAI ahora se cablea correctamente en producción. Ya no cae silenciosamente en MockLlmService.
- #11: Corregido mismatch de columna next_attempt_at → next_retry_at en ProcessingJob.

### Changed
- Imagen base: UBI8/JDK → UBI9/JRE (menor superficie de ataque).

### Docker
- ghcr.io/breisnerlopez/abax-memory:v2.0.3
- ghcr.io/breisnerlopez/abax-memory:latest

## v1.0.0 — MVP (2026-05-02)

### 🚀 Release Inicial — MVP funcional con IA real

**Stack operativo completo**:
- **Backend**: Quarkus 3.15.3 + Java 21
- **Base de datos**: PostgreSQL 16 (Alpine)
- **Vector DB**: Qdrant 1.17.1 — busqueda semantica funcional
- **Identity Provider**: Keycloak 26.1 — OIDC/JWT/RBAC
- **Migraciones**: Flyway v1 (baseline operational store)

### Capacidades IA (OpenAI)

| Capacidad | Modelo | Estado |
|---|---|---|
| Generacion de embeddings | `text-embedding-3-large` (3072 dims) | ✅ Funcional |
| Extraccion de entidades | `gpt-4o-mini` (structured outputs) | ✅ Funcional |
| Validacion semantica | `gpt-4o` | ✅ Funcional |

### Modulos R1-MVP implementados (61/61 CA aprobados)

1. **M1 — Gestion de memorias**: CRUD completo con validacion semantica
2. **M2 — API operativa**: 13 endpoints REST documentados (OpenAPI 3.0.3)
3. **M3 — Busqueda y recuperacion**: Busqueda semantica via Qdrant + embeddings OpenAI
4. **M4 — Persistencia y metadatos**: PostgreSQL + Flyway migrations + metadatos enriquecidos
5. **M5 — Gobierno de memoria**: Reglas de validacion, RBAC, ciclo de vida
6. **M6 — Depuracion y mantenimiento**: Health checks, processing jobs
7. **M7 — Acceso y visibilidad**: OIDC via Keycloak, control de acceso
8. **M8 — Contrato API**: OpenAPI 3.0.3 expuesta en `/q/openapi`

### Calidad

- Suite automatizada: **54 tests, BUILD SUCCESS, 0 failures**
- QA: **49/49 casos de prueba aprobados (100%)**
- UAT: **61/61 criterios de aceptacion R1-MVP aprobados (100%)**
- **0 defectos abiertos** en todas las fases

### Seguridad

- API key de OpenAI gestionada **exclusivamente via variable de entorno** (`OPENAI_API_KEY`)
- Sin secretos hardcodeados en codigo fuente ni configuracion
- Rotacion de API key recomendada post-desarrollo

### Docker

- Imagen publicada en **GitHub Container Registry**: `ghcr.io/breisnerlopez/abax-memory`
- Tags disponibles: `latest`, `v1.0.0`
- `docker-compose.yml` incluido para despliegue local completo
