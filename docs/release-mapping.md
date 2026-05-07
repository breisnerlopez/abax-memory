# Release Mapping — Abax-Memory

- **Responsable**: project-manager
- **Fecha**: 2026-05-07
- **Estado**: Completado — Tres releases (v1.0.0, v2.0.0, v2.1.0) CERRADOS

---

## Mapeo de Releases

| Release | Carpeta                    | Descripción                                              | Estado    |
|---------|----------------------------|----------------------------------------------------------|-----------|
| v1.0.0  | `docs/entregables/v1/`     | Primera versión del sistema Abax-Memory (MVP inicial)    | Cerrado   |
| v2.0.0  | `docs/entregables/v2/`     | Segunda iteración mayor — Motor de memoria genérica multi-dominio | Cerrado   |
| v2.1.0  | `docs/entregables/v2.1/`   | Tercera iteración — Hardening & Producción Real (13 features, 96.67% top-1) | **Cerrado** |

---

## Migraciones y Cambios de v1 → v2

| Aspecto          | v1.0.0                        | v2.0.0                        |
|------------------|-------------------------------|-------------------------------|
| Estrategia docs  | Carpeta plana `entregables/`  | `entregables/v2/` por release |
| Dominio          | IT Operations (fijo)          | Multi-dominio (perfiles configurables) |
| Audiencia        | Operadores IT, revisores      | Cualquier industria/vertical  |
| Convención código| Español                       | English-Only estricto         |
| Endpoints        | `/api/casos`, `/api/memorias` | `/api/v2/memories`, `/api/v2/search` |
| Entidades        | `Caso`, `MemoriaOperativa`    | `Memory`, `Tenant`, `DomainProfile` |
| Frontend         | No incluido                   | 6 pantallas React + 7 componentes |
| Multi-tenancy    | No                            | Sí (tenant isolation, cross-tenant 404) |
| QA               | 49 casos                      | 96 casos                      |
| UAT              | 61/61 CA (análisis documental)| 10/10 escenarios (curl real, 4 rondas iterativas) |
| Tests            | 54                            | 163 (115 backend + 48 frontend) |
| Anti-mock        | No (InMemorySearchIndexer llegó a F7) | 3 capas (40 marcas detectadas, 0 mocks en prod) |
| v2.1.0  | Hardening: cross-encoder, cache JWT, DELETE namespace, unificación Qdrant | 13 features, 56/56 QA, 14/14 UAT, 228 tests |
| Estado           | Documentación congelada       | **CERRADO** — 2026-05-04    | **CERRADO** — 2026-05-07 |
| Lecciones        | 6 lecciones en v1 `fase-9-cierre/` | 11 lecciones en v2 `fase-9-cierre/` | Lecciones en v2.1 `fase-9-cierre/` |

---

> **Nota**: Los tres releases (v1.0.0, v2.0.0 y v2.1.0) están formalmente cerrados. v1.0.0 cerró el 2026-05-02. v2.0.0 cerró el 2026-05-04. v2.1.0 cerró el 2026-05-07 con 13 features, 56/56 CPs QA, 14/14 UAT, y 96.67% precisión top-1. La documentación de cada release reside en su respectivo folder bajo `docs/entregables/`. Para lecciones aprendidas de cada iteración, consultar `docs/entregables/v1/fase-9-cierre/`, `docs/entregables/v2/fase-9-cierre/`, y `docs/entregables/v2.1/fase-9-cierre/`.
