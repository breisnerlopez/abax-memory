# validacion-de-comandos

## Como validar comandos antes de documentarlos

Patron general:

1. ABRIR terminal en el directorio del proyecto.
2. COPIAR el comando exacto del doc al terminal.
3. EJECUTAR. Capturar salida.
4. Si el comando falla:
   - Si es por contexto faltante (ej. olvido `cd`, falta env var) → corregir el doc anadiendo el contexto.
   - Si es por bug en el sistema → reportar al developer/devops, NO documentar el comando roto.
5. ANOTAR en `docs/comandos-validados.md` (o equivalente) con fecha y resultado.

Para comandos contra ambientes no accesibles (prod, hardware
especifico), seguir las reglas:

- Marcar con comentario `# NOT VALIDATED — <razon>`.
- Solo aceptable si el operador del ambiente confirmo el comando
  contra el ambiente real (capturar el confirmador en el doc).
