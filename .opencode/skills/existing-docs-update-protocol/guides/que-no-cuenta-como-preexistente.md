# que-no-cuenta-como-preexistente

## Que NO requiere escalamiento (puedes escribir directamente)

Estos casos son escritura legitima sin protocolo de actualizacion:

- **Archivos creados por TI MISMO en esta misma sesion** (estas iterando
  sobre tu propio borrador). El protocolo solo aplica a docs de iteraciones/
  sesiones previas.
- **Archivos generados automaticamente por tooling** (mkdocs.yml, .github/
  workflows generados por Abax). Estos se regeneran consistentemente.
- **Archivos en `docs/.archive/`**. Por convencion no se modifican; si
  hay que cambiarlos es un caso muy especial.
- **Archivos en carpetas `node_modules/`, `target/`, `dist/`, `.opencode/`,
  `.claude/`, `vendor/`** — son artefactos generados.
- **Comments / strings en codigo fuente** — esos van por code review,
  no son entregables de docs.

Si tienes duda, escala. Mejor pregunta extra que overwrite silencioso.
