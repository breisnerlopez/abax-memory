# Release Mapping — Abax-Memory

- **Responsable**: project-manager
- **Fecha**: 2026-05-04
- **Estado**: Completado — Ambos releases (v1.0.0 y v2.0.0) CERRADOS

---

## Mapeo de Releases

| Release | Carpeta                    | Descripción                                              | Estado    |
|---------|----------------------------|----------------------------------------------------------|-----------|
| v1.0.0  | `docs/entregables/v1/`     | Primera versión del sistema Abax-Memory (MVP inicial)    | Cerrado   |
| v2.0.0  | `docs/entregables/v2/`     | Segunda iteración mayor — Motor de memoria genérica multi-dominio | Cerrado   |

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
| Estado           | Documentación congelada       | **CERRADO** — 2026-05-04    |
| Lecciones        | 6 lecciones en v1 `fase-9-cierre/` | 11 lecciones en v2 `fase-9-cierre/` |

---

> **Nota**: Ambos releases (v1.0.0 y v2.0.0) están formalmente cerrados. v1.0.0 cerró el 2026-05-02. v2.0.0 cerró el 2026-05-04. La documentación de cada release reside en su respectivo folder bajo `docs/entregables/`. Para lecciones aprendidas de cada iteración, consultar `docs/entregables/v1/fase-9-cierre/` y `docs/entregables/v2/fase-9-cierre/`.
