# Publicacion en GitHub
- **Fase**: 0-Descubrimiento
- **Entregable**: Publicacion en GitHub
- **Responsable**: general
- **Fecha**: 2026-05-01
- **Estado**: Completado

---

## Resultado

Se publico el estado actual del workspace en GitHub.

- **Owner**: `breisnerlopez`
- **Repositorio creado**: `abax-memory`
- **URL**: `https://github.com/breisnerlopez/abax-memory`
- **Visibilidad elegida**: `private`
- **Branch principal**: `main`
- **Commit publicado**: verificado en GitHub sobre `main` durante la ejecucion del entregable

## Criterio usado para el nombre

No habia nombre de repositorio explicitamente fijado. Se uso `abax-memory` por alineacion directa con el nombre del proyecto `Abax-Memory` y siguiendo una convencion comun de GitHub en minusculas con guiones.

## Validaciones ejecutadas

1. Se verifico que el directorio `/root/proyectos-personales/Abax-Memory` no tenia repositorio Git local.
2. Se verifico que `gh` inicialmente no estaba autenticado.
3. Se verifico el acceso SSH a GitHub y se detecto un bloqueo real por permisos demasiado abiertos en `/etc/ssh/devs-github/id_ed25519`.
4. Se corrigio el permiso de la clave a `0600` y luego se valido autenticacion SSH exitosa contra GitHub.
5. Se autentico `gh` usando el token disponible en `/etc/ssh/devs-github/gh-token`.
6. Se inicializo el repositorio local con branch `main`.
7. Se creo el commit inicial `chore: bootstrap project snapshot`.
8. Se creo el repositorio remoto `breisnerlopez/abax-memory` con `gh`.
9. Se hizo `push` inicial de `main` al remoto.
10. Se actualizo este entregable y se publico un segundo commit `docs: record GitHub publication`.
11. Se valido la existencia del remoto con `gh repo view` y se confirmo el commit publicado consultando `repos/breisnerlopez/abax-memory/commits/main`.

## Evidencia resumida

- `git rev-parse --is-inside-work-tree`: fallo inicialmente porque no existia `.git`.
- `gh auth status`: inicialmente sin sesion activa; luego autenticado como `breisnerlopez`.
- `ssh -T -i /etc/ssh/devs-github/id_ed25519 git@github.com`: autenticacion exitosa despues del ajuste de permisos.
- `gh repo create breisnerlopez/abax-memory --private --source=. --remote=origin --push`: repositorio creado y branch `main` publicado.
- `gh repo view breisnerlopez/abax-memory --json name,owner,url,visibility,defaultBranchRef`: confirmo repo `PRIVATE`, owner `breisnerlopez` y branch por defecto `main`.
- `gh api repos/breisnerlopez/abax-memory/commits/main --jq .sha`: se uso para validar que `main` quedo publicado en GitHub.

## Observaciones

- `gh repo create` configuro inicialmente `origin` en HTTPS. Para completar el push del commit final se cambio a `git@github.com:breisnerlopez/abax-memory.git`, que es la configuracion actual.
- Despues de autenticar `gh`, quedo un directorio local no versionado `.config/`. No formo parte del commit publicado porque fue generado despues del commit inicial.
