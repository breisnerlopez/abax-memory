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
