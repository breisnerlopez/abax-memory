# ejemplo-decision-bloque-cambios

## Ejemplo completo — Estrategia B aplicada a un refinamiento v1.1.0

### Contexto

Proyecto en produccion v1.0.0. Usuario reporta que necesita agregar
notificaciones via Slack (antes solo email + Teams). Es un agregado,
no cambio de alcance.

### Decision

Estrategia B — Bloque "## Cambios v1.1.0" al final de los archivos
afectados.

### Aplicacion

Archivos a actualizar (NO sobreescribir):
- `docs/entregables/fase-2-analisis/especificacion-funcional.md`
- `docs/entregables/fase-3-diseno-tecnico/integration-design-doc.md`
- `docs/entregables/fase-4-construccion/source-code/` (referenciar codigo nuevo)
- `docs/entregables/fase-7-despliegue/runbook.md` (agregar comando de config Slack)

En cada uno, agregar al final:

```markdown
---

## Cambios v1.1.0 — 2026-06-15

### Que agregamos
- Integracion con Slack para notificaciones operativas (canal `#alerts-prod`).
- Webhook configurado en variable de entorno `SLACK_WEBHOOK_URL`.

### Que NO cambia
- Email y Microsoft Teams (canales originales) siguen activos.
- Logica de filtrado de severidades (sin cambios).

### Migracion
Sin migracion necesaria. Si la variable `SLACK_WEBHOOK_URL` no esta
seteada, el canal Slack queda desactivado (default seguro).
```

Y actualizar el header del archivo:

```yaml
---
fase: 2-analisis
entregable: especificacion-funcional
iteraciones:
  - version: 1.0.0
    fecha: 2026-05-01
    responsable: business-analyst
  - version: 1.1.0
    fecha: 2026-06-15
    responsable: business-analyst
    cambios: integracion Slack notifications
---
```
