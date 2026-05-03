# skeleton-script

## Script para generar el esqueleto inicial

Ejecutar UNA VEZ al inicio de la fase relevante. Crea las carpetas
vacias con README.md placeholder en cada una:

```bash
# Adapta segun el modo del proyecto
ROOT="docs"
mkdir -p "$ROOT"/{architecture/adrs,api,runbooks,user-guides/screenshots,functional,deliverables,decisions,presentations,design-system}

for dir in architecture api runbooks user-guides functional deliverables decisions; do
  if [ ! -f "$ROOT/$dir/README.md" ]; then
    cat > "$ROOT/$dir/README.md" <<EOF
# $(echo "$dir" | sed 's/-/ /g; s/\b\(.\)/\u\1/g')

Indice de documentos en \`$dir/\`. Llenar a medida que se producen
entregables.

| Documento | Para que |
|---|---|
| _(pendiente)_ | _(pendiente)_ |
EOF
  fi
done

# Indice raiz de docs/
if [ ! -f "$ROOT/README.md" ]; then
  cat > "$ROOT/README.md" <<EOF
# Documentacion del proyecto

Indice navegable de toda la documentacion del proyecto. Cada
subcarpeta tiene su propio README.md como indice secundario.

| Tema | Carpeta |
|---|---|
| Arquitectura tecnica | [architecture/](architecture/) |
| API y contratos | [api/](api/) |
| Procedimientos operativos | [runbooks/](runbooks/) |
| Manuales de usuario | [user-guides/](user-guides/) |
| Especificacion funcional | [functional/](functional/) |
| Entregables por fase | [deliverables/](deliverables/) |
| Decisiones no arquitecturales | [decisions/](decisions/) |
| Presentaciones HTML | [presentations/](presentations/) |
| Glosario de terminos | [glossary.md](glossary.md) |
EOF
fi
```
