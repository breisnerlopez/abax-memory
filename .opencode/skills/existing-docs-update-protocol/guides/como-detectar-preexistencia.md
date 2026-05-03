# como-detectar-preexistencia

## Como detectar si un archivo preexiste antes de escribir

Las herramientas a usar (sin tocar disco):

```bash
# 1. Existe?
test -f "<ruta>" && echo EXISTE || echo NUEVO

# 2. Cuanta historia tiene?
wc -l "<ruta>"

# 3. Que dice el frontmatter?
head -15 "<ruta>"

# 4. Cuando se modifico la ultima vez?
stat -c '%y  %n' "<ruta>"

# 5. Tiene historia git?
git log --oneline --follow "<ruta>" | head -3
```

Reporta los 5 datos al orquestador en el escalamiento (ver Plantilla en
seccion "3. Si EXISTE → escala primero al orquestador").
