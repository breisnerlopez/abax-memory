# validar-status-url

## Como validar status de la URL antes de reportar

Si tienes `bash: allow|ask` y curl disponible:

```bash
URL="https://breisnerlopez.github.io/abax-memory/entregables/v2/fase-0-descubrimiento/presentacion.html"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$URL"
```

Codigos esperados:
- 200 = publicado y accesible
- 404 = aun no se ha hecho deploy (esperar al proximo push a main)
- 403 = repo privado sin Pages
- 5xx = problema temporal de GitHub

Si 404, marca status como "Publicada (deploy pendiente — siguiente push a main lo activa)".
