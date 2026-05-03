# ejemplos-frontmatter-iterado

## Frontmatter para archivos con multiples iteraciones

### v1 original (creado en proyecto inicial)

```yaml
---
fase: 0-descubrimiento
entregable: vision-producto
responsable: business-analyst
aprobado-por: product-owner
fecha: 2026-05-01
version: 1.0.0
estado: aprobado
---
```

### v2 actualizado en sitio (estrategia A)

```yaml
---
fase: 0-descubrimiento
entregable: vision-producto
version: 2.0.0
estado: aprobado
iteraciones:
  - version: 1.0.0
    fecha: 2026-05-01
    responsable: business-analyst
    aprobado-por: product-owner
  - version: 2.0.0
    fecha: 2026-05-03
    responsable: business-analyst
    aprobado-por: product-owner
    cambios: |
      Evoluciona de PMOA (IT ops) a motor generico con perfiles de dominio.
      Ver bloque "## Cambios v2.0.0" al final del documento.
---
```

### v2 paralelo (estrategia C)

Header del archivo `vision-producto.v2.md` (o en folder `v2/`):

```yaml
---
fase: 0-descubrimiento
entregable: vision-producto
version: 2.0.0
relacion-iteracion-anterior: docs/entregables/v1/fase-0-descubrimiento/vision-producto.md
responsable: business-analyst
aprobado-por: product-owner
fecha: 2026-05-03
estado: borrador
---
```
