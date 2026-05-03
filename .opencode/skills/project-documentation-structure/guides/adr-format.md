# adr-format

## Formato estandar para ADR (Architecture Decision Record)

Cada ADR vive en `docs/architecture/adrs/NNNN-<verbo>-<sustantivo>.md`.
Numero correlativo, padding a 4 digitos.

```markdown
---
fase: 3-diseno-tecnico
entregable: architecture-doc
tipo: ADR-NNNN
responsable: solution-architect
aprobado-por: tech-lead
fecha: YYYY-MM-DD
estado: propuesto | aceptado | superado | obsoleto
---

# ADR-NNNN: <Decision en imperativo, ej. "Usar Postgres en lugar de MongoDB">

## Contexto

Que problema enfrentamos. Una vez explicado, por que necesita
decision formal (no es trivial, hay tradeoffs reales).

## Decision

Que vamos a hacer. Una frase clara.

## Consecuencias

### Positivas
- Lo que ganamos.

### Negativas
- Lo que cedemos / costo asumido.

### Neutras
- Cambios necesarios para implementar.

## Alternativas consideradas

Las que evaluamos y descartamos, con la razon principal.

## Referencias

- Issue / PR / Linear ticket que la motivo.
- Documentos externos (RFCs, blog posts, benchmarks) que la sustentan.
```
