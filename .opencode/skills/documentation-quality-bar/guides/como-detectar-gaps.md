# como-detectar-gaps

## Auditoria rapida de un doc existente

Para evaluar si un doc cumple la barra antes de heredarlo:

```bash
DOC=docs/mi-doc.md

# 1. Frontmatter
head -1 "$DOC" | grep -q '^---$' || echo "FALTA frontmatter"

# 2. TODO sueltos
grep -nE 'TODO|TBD|FIXME|XXX' "$DOC" && echo "Tiene marcadores sin owner"

# 3. Bloques de codigo sin lenguaje
awk '/^```$/{c++} END{if(c%2==1) print "Bloque sin cerrar"}' "$DOC"
grep -nE '^```$' "$DOC" | head -5  # bloques sin lenguaje

# 4. Links rotos
grep -oE '\[[^]]+\]\([^)]+\)' "$DOC" | grep -oE '\([^)]+\)' | tr -d '()' | while read url; do
  [[ "$url" == http* ]] && continue
  [[ -f "$(dirname "$DOC")/$url" ]] || echo "ROTO: $url"
done

# 5. Length check
wc -l "$DOC"
# Si > 200 lineas, debe tener TOC
```

Reportar los gaps al orquestador en formato matriz; NO arreglar
silenciosamente sin que el responsable original lo sepa.
