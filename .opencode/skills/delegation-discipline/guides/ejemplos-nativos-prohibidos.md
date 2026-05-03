# ejemplos-nativos-prohibidos

## Casos donde nativos VIOLARIAN un veto y deben rechazarse

### Veto 1 violado — escritura en docs/

> "@general escribe el documento de vision del producto en
> `docs/entregables/fase-0-descubrimiento/vision-producto.md`"

MAL: escribe en docs/, sin `existing-docs-update-protocol` (puede
sobreescribir v1), sin frontmatter de procedencia
(`documentation-quality-bar`), sin approver RACI.

Correcto: `@business-analyst` con instruccion de seguir
`existing-docs-update-protocol` si el archivo preexiste.

### Veto 2 violado — git commit

> "@general haz commit del archivo nuevo y push a main"

MAL: no respeta rama `abax/<project>`, no usa `--author`, podria
hacer push a main directamente.

Correcto: `@devops` o el `@developer-backend` que lo escribio,
siguiendo `git-collaboration`.

### Veto 3 violado — decision formal

> "@plan decide cual es la mejor arquitectura entre microservicios
> y modular monolito y registra la decision como ADR."

MAL: ADR es decision formal con approver. `@plan` puede bosquejar
opciones pero NO firmar la decision.

Correcto: `@solution-architect` produce ADR-NNNN con approver
`@tech-lead`.

### Veto 4 violado — entregable formal

> "@explore lee la propuesta y produce el Documento de Vision."

MAL: Documento de Vision es entregable formal de fase 0 (esta en
`phase-deliverables.yaml`), responsible business-analyst.

Correcto: `@explore` puede leer la propuesta y devolver un resumen
al orquestador. El orquestador delega despues a `@business-analyst`
para producir el entregable formal.
