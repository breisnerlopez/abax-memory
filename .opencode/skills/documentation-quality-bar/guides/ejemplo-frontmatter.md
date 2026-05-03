# ejemplo-frontmatter

## Ejemplos de frontmatter por tipo de doc

### Especificacion funcional (BA, fase 2)

```yaml
---
fase: 2-analisis-funcional
entregable: functional-spec
responsable: business-analyst
aprobado-por: product-owner
fecha: 2026-05-10
estado: aprobado
version: 1.0
---
```

### ADR (Solution Architect, fase 3)

```yaml
---
fase: 3-diseno-tecnico
entregable: architecture-doc
tipo: ADR-007
responsable: solution-architect
aprobado-por: tech-lead
fecha: 2026-05-15
estado: aprobado
decision: usar-postgres-en-lugar-de-mongo
---
```

### Runbook (tech-writer, fase 7)

```yaml
---
fase: 7-despliegue
entregable: runbook
servicio: api-pagos
responsable: tech-writer
aprobado-por: tech-lead
fecha: 2026-05-20
estado: en-revision
oncall: equipo-pagos
---
```
