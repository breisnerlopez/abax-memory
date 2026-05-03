# como-construir-url

## Como construir la URL publica desde un path local

### Caso A — proyecto con Pages legacy (source: main:/docs)

Local: `docs/entregables/v2/fase-0-descubrimiento/presentacion.html`
Remoto: `git@github.com:breisnerlopez/abax-memory.git`

URL: `https://breisnerlopez.github.io/abax-memory/entregables/v2/fase-0-descubrimiento/presentacion.html`

Pasos:
1. `git remote get-url origin` -> extraer owner y repo
2. URL base = `https://<owner>.github.io/<repo>/`
3. Append path quitando el prefijo `docs/`

### Caso B — proyecto con Pages workflow (GitHub Actions + MkDocs)

Si existe `mkdocs.yml`, los `.md` se procesan a HTML. La URL pierde
el `.md` y se queda con la ruta de carpeta:

Local: `docs/entregables/v2/fase-0-descubrimiento/vision-producto.md`
URL: `https://<owner>.github.io/<repo>/entregables/v2/fase-0-descubrimiento/vision-producto/`
(con slash final, MkDocs usa pretty URLs por default)

Si NO existe `mkdocs.yml` y Pages esta en modo Actions, los `.md` no
se sirven (devuelven 404). En ese caso, solo reportar URLs de archivos
`.html`.

### Caso C — repo privado

Si el repo es privado, GitHub Pages requiere plan Pro/Team/Enterprise.
Si Pages no esta disponible, reportar al usuario:

> Entregable disponible en `<path-local>`. Pages no esta activo en
> este repo (privado en plan free). Para visualizar, abrir el HTML
> local con un navegador.
