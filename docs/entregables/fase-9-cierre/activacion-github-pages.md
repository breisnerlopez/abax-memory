# Activacion de GitHub Pages via API
- **Fase**: Fase 9 - Cierre
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-02
- **Estado**: Completado

---

## Objetivo

Activar GitHub Pages para el repositorio `breisnerlopez/abax-memory` configurando
la fuente desde `main` branch, path `/docs`, donde reside todo el contenido
estatico del proyecto (8 presentaciones + index.html).

## Procedimiento ejecutado

### 1. Autenticacion de gh CLI

```bash
echo "<TOKEN>" | gh auth login --with-token
```

Resultado: autenticacion exitosa como `breisnerlopez`.

```
github.com
  ✓ Logged in to github.com account breisnerlopez
  - Token scopes: repo, workflow, admin:repo_hook, ...
```

### 2. Activacion de Pages

Primer intento con `POST`:

```bash
gh api repos/breisnerlopez/abax-memory/pages \
  -X POST \
  -f "source[branch]=main" \
  -f "source[path]=/docs"
```

**Respuesta**: HTTP 409 — `GitHub Pages is already enabled.`

→ Ya existia una configuracion previa de Pages. Se procede con `PUT` para actualizar.

### 3. Actualizacion de configuracion (PUT)

```bash
gh api repos/breisnerlopez/abax-memory/pages \
  -X PUT \
  -f "source[branch]=main" \
  -f "source[path]=/docs"
```

**Respuesta**: HTTP 204 No Content (exito).

### 4. Verificacion de configuracion final

```bash
gh api repos/breisnerlopez/abax-memory/pages
```

```json
{
  "url": "https://api.github.com/repos/breisnerlopez/abax-memory/pages",
  "status": "built",
  "html_url": "https://breisnerlopez.github.io/abax-memory/",
  "source": {
    "branch": "main",
    "path": "/docs"
  },
  "public": true,
  "https_enforced": true
}
```

### 5. Verificacion del ultimo build

```bash
gh api repos/breisnerlopez/abax-memory/pages/builds/latest
```

| Campo | Valor |
|---|---|
| **Build ID** | `982384984` |
| **Status** | `built` |
| **Commit** | `9e641f670225bdc73d7ea651b063084ad18f1432` |
| **Duration** | ~45 segundos |
| **Error** | `null` (sin errores) |
| **Fecha** | 2026-05-03T03:30:50Z |

## Resultado final

| Indicador | Valor |
|---|---|
| **URL publica** | https://breisnerlopez.github.io/abax-memory/ |
| **Source branch** | `main` |
| **Source path** | `/docs` |
| **HTTPS** | Enforced |
| **Repositorio** | Publico |
| **¿Requiere GitHub Pro?** | No (repositorio publico) |

## Notas

- El repositorio es **publico**, por lo que GitHub Pages esta disponible sin necesidad de GitHub Pro.
- El contenido servido desde `/docs` incluye `docs/index.html` como landing page y las 8 presentaciones del proyecto.
- El build se dispara automaticamente ante cada push a `main` que afecte la carpeta `docs/`.
- El token utilizado fue rotado inmediatamente despues de esta operacion (no persiste en el ambiente).
