# heuristicas-rapidas

## Heuristicas rapidas para decidir en 5 segundos

Pregunta 1: ¿La Task escribira algo en `docs/` o `src/`?
- SI → rol del proyecto (no negociable, Veto 1)
- NO → continua

Pregunta 2: ¿La Task hara `git commit` o `push`?
- SI → rol del proyecto con bash + git-collaboration (Veto 2)
- NO → continua

Pregunta 3: ¿La Task producira un entregable de phase-deliverables.yaml?
- SI → rol del proyecto definido como `responsible` (Veto 4)
- NO → continua

Pregunta 4: ¿La Task tomara una decision formal con approver RACI?
- SI → rol del proyecto (Veto 3)
- NO → continua

Pregunta 5: ¿La Task es exploracion/lookup/research read-only?
- SI → nativo OK (`@explore` para grep, `@docs` para lookup externo,
  `@plan` para bosquejo, `@general` para sintesis multi-area)
- NO → rol del proyecto por default (caso ambiguo, default seguro)

Pregunta 6 (atajo): ¿Necesitas info del proyecto en general?
- LEE `project-manifest.yaml`, `docs/bitacora.md`, `CHANGELOG.md`
  DIRECTAMENTE sin delegar. Es el camino mas corto.
