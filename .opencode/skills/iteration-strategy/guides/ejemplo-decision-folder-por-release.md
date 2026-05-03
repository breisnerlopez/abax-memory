# ejemplo-decision-folder-por-release

## Ejemplo completo — Estrategia A aplicada a Abax-Memory v2

### Contexto

Proyecto Abax-Memory v1.0.0 cerrado el 2026-05-02. v1 era PMOA
(memoria operativa para IT). Usuario solicita evolucion a v2 como
motor de memoria generica para cualquier dominio.

### Decision

Estrategia A — Folder por release. Justificacion: cambio mayor de
alcance (dominio especifico → multi-dominio), audiencia distinta
(IT ops → cualquier industria), arquitectura interna evoluciona
(perfiles de dominio nuevos).

### Estructura aplicada

```
docs/
  entregables/
    v1/                                  <- mover lo existente aqui
      fase-0-descubrimiento/
      fase-1-inicio/
      ...
      fase-9-cierre/
    v2/                                  <- NUEVO, donde van los Tasks v2
      fase-0-descubrimiento/
        vision-producto.md
        epicas-features.md
        historias-usuario.md
        backlog-priorizado.md
        presentacion-descubrimiento.html
      (mas fases conforme avanzan)
  release-mapping.md                     <- relacion v1 <-> v2
  iteration-log.md                       <- bitacora de iteracion
```

### Comandos de migracion (una sola vez)

```bash
cd <proyecto>
mkdir -p docs/entregables/v1
# Mover fases existentes (no las archives, las renombras a v1/)
for f in docs/entregables/fase-*; do
  mv "$f" docs/entregables/v1/
done
# Crear placeholder de v2 con seed README
mkdir -p docs/entregables/v2/fase-0-descubrimiento/
echo "# v2 — En curso" > docs/entregables/v2/README.md
# Documentar
cat > docs/release-mapping.md << EOF
# Release Mapping

## v1.0.0 → v2.0.0

| v1 | v2 | Cambia? |
|---|---|---|
| PMOA (IT operations memory) | Motor generico multi-dominio | SI: alcance |
| Backend Quarkus + PG + Qdrant + Keycloak + OpenAI | Mismo stack, perfiles de dominio | NO: stack |
| API REST en español | API REST en ingles (English-Only Internals) | SI: convencion |

EOF
```

Cada Task delegado de fase 0 v2 escribe en
`docs/entregables/v2/fase-0-descubrimiento/<entregable>.md`.

### Tarea OBLIGATORIA tras el rename — actualizar docs/index.html

Despues de mover las carpetas a `entregables/v1/`, **TODAS las
referencias en `docs/index.html` quedan rotas** porque apuntan a
las rutas viejas (`entregables/fase-X/...`). El sitio publico de
GitHub Pages devuelve 404 en cada link de presentaciones.

Esto paso en el incidente 2026-05-03 sobre Abax-Memory v2: el
rename a v1/ lo hizo `@devops` correctamente, pero nadie actualizo
el `index.html` y el sitio quedo con 16 links rotos.

**El orquestador DEBE delegar al `@devops` (o `@tech-writer` si no
hay devops) inmediatamente despues del rename:**

```
agent: devops
description: Actualizar docs/index.html tras rename a v1/v2
prompt: |
  El rename a `docs/entregables/v1/` y la creacion de
  `docs/entregables/v2/` rompio los links en `docs/index.html`.
  Actualiza el archivo:

  1. Reescribe TODOS los `href="entregables/fase-X/..."` a
     `href="entregables/v1/fase-X/..."`. Verifica con grep que no
     quedan referencias sin prefijo `v1/`.

  2. Agrega seccion nueva al inicio del listado de presentaciones:
     "## v2.0.0 — En curso" con enlaces a los HTMLs de
     `docs/entregables/v2/`. Actualizable conforme avancen las fases.

  3. Actualiza el header/banner para indicar "Iteracion v2.0.0 en
     curso" sin borrar la mencion al cierre v1.0.0.

  4. Carga la skill `publication-notification` y reporta al
     orquestador la URL publica del index actualizado.

  NO sobreescribir el index entero — preserva el design system
  existente (CSS, layout, branding). Solo modifica las secciones
  de links y header. Sigue `existing-docs-update-protocol`.
```

Sin esta Task, los links del home quedan rotos y la siguiente
notificacion al usuario apunta a 404.

### Validacion post-rename

Despues del rename + actualizacion del index, validar:

```bash
# 1. No deben quedar referencias a rutas viejas
grep -E 'href="entregables/fase-[0-9]' docs/index.html && echo "ROTO: ajustar prefijo a v1/"

# 2. Las URLs publicas previstas deben dar 200 tras el proximo deploy
# (skill publication-notification valida con curl si tienes bash)
```
