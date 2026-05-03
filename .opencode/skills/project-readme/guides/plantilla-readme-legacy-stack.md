# plantilla-readme-legacy-stack

## Plantilla README — stack legacy (PHP clasico, VB6, Java Swing, Cobol, Delphi)

Caracteristicas adicionales sobre la plantilla document:

- **Documentar las dependencias EOL** (end of life) explicitamente:

  ```markdown
  ## Dependencias y soporte

  | Componente | Version | Estado |
  |---|---|---|
  | PHP | 5.6.40 | EOL desde 2019-01 — sin patches de seguridad |
  | MySQL | 5.7 | EOL desde 2023-10 |
  | CentOS 7 | 7.9 | EOL desde 2024-06 |
  ```

  Esta tabla es informativa, NO una propuesta de upgrade. Las
  recomendaciones de mitigacion van en `docs/recommendations.md`.

- **Documentar acceso fisico/remoto al sistema** si aplica:

  ```markdown
  ## Acceso al sistema

  - **Codigo fuente**: VSS server `\\srv-vss\repos\<proyecto>` (lectura: grupo `dev-readonly`).
  - **Build**: VS6 IDE en VM Windows XP (`vm-vb6-build`). Compilacion manual: File > Make Project.exe.
  - **Despliegue**: copiar `.exe` resultante a `\\srv-prod\sistemas\<proyecto>\` (acceso: usuario `deploy`).
  - **Logs**: `\\srv-prod\sistemas\<proyecto>\logs\` rotados manualmente.
  ```

- **NO inventar comandos modernos** (ej. NO escribir `npm install` en un README de VB6).

- **Indicar que el sistema NO se construye con CI/CD moderno** si es el caso. Documentar el procedimiento manual real.

- Si el sistema usa formularios, screens, JFrames, .frm, .vbp, listar los principales con su proposito.
