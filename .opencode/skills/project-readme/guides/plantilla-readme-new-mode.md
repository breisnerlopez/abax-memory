# plantilla-readme-new-mode

## Plantilla README — modo new (proyecto desde cero)

Estructura minima ejecutable. Adaptar al stack:

```markdown
# <NombreProyecto>

[![CI](https://github.com/<org>/<repo>/actions/workflows/ci.yml/badge.svg)](...)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![tests](https://img.shields.io/badge/tests-XX%20passing-brightgreen)](...)

> **<Tagline en una frase: que hace y para quien.>**

<1 parrafo: que problema resuelve, principal beneficio, contexto minimo.>

## Quickstart

```bash
# Requisitos: Node.js >= 20, Docker
git clone <url> && cd <repo>
cp .env.example .env       # editar valores
pnpm install
pnpm dev                   # http://localhost:3000
```

## Tabla de contenidos

- [Caracteristicas](#caracteristicas)
- [Instalacion](#instalacion)
- [Uso](#uso)
- [Configuracion](#configuracion)
- [Desarrollo local](#desarrollo-local)
- [Tests](#tests)
- [Despliegue](#despliegue)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

## Caracteristicas

- Feature 1 con contexto.
- Feature 2 con contexto.
- Feature 3 con contexto.

## Instalacion

### Requisitos

- Node.js >= 20 ([nodejs.org](https://nodejs.org))
- Docker >= 24 (para BD local)
- pnpm (recomendado): `npm install -g pnpm`

### Pasos

```bash
git clone <url>
cd <repo>
pnpm install
```

## Uso

### Caso comun 1

```bash
pnpm dev
# Abre http://localhost:3000
```

### Caso comun 2

Codigo de ejemplo aqui...

## Configuracion

Variables de entorno en `.env`:

| Variable | Default | Descripcion |
|---|---|---|
| `DATABASE_URL` | - | URL de Postgres. Obligatoria. |
| `LOG_LEVEL` | `info` | `debug`, `info`, `warn`, `error` |

Secrets NUNCA en `.env` commiteado. Usar `.env.example` con valores
ficticios y `.env` ignorado por git.

## Desarrollo local

```bash
pnpm install
pnpm dev                   # arranca en watch mode
pnpm lint                  # ESLint + Prettier
pnpm typecheck             # tsc --noEmit
pnpm test                  # vitest
pnpm build                 # build de produccion
```

## Tests

```bash
pnpm test                  # XX tests
pnpm test:watch            # modo watch
pnpm test:coverage         # genera reporte
```

Cobertura actual: XX%. Target minimo del proyecto: YY%.

## Despliegue

Ver [docs/runbook.md](docs/runbook.md) para procedimiento completo
(rollback, monitoring, escalamiento).

Quick: tag `vX.Y.Z` sobre `main` dispara `release.yml` que builda,
publica imagen Docker y despliega a `<ambiente>`.

## Contribuir

Ver [CONTRIBUTING.md](CONTRIBUTING.md). Resumen:
- Trabajar en rama `feature/<nombre>` o `bugfix/<nombre>`.
- PR contra `main`. CI debe estar verde.
- Squash merge.

## Licencia

MIT — ver [LICENSE](LICENSE).
```
