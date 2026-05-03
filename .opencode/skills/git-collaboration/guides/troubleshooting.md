# troubleshooting

### "fatal: not a git repository"
El detector de Abax marco hasGit=true pero el directorio no tiene .git.
Quizas se elimino. Reporta al usuario.

### "fatal: refusing to merge unrelated histories" en push
El remoto tiene commits no relacionados (otra rama o repo distinto).
Verifica el remote: `git remote -v`. Si es incorrecto, reporta al usuario.

### "remote: Repository not found"
El usuario no tiene acceso al repo o el URL es incorrecto. Reporta.

### Multiple agentes commitearon a la vez
Esto es raro porque cada agente commitea archivos distintos. Si pasa,
el segundo agente ve "non-fast-forward" en push (pero recuerda: solo
devops hace push). Para los commits locales no hay conflict salvo que
editen el mismo archivo (escalar al orquestador en ese caso).
