# logging-y-observabilidad

- Registrar errores con nivel apropiado: WARN para recuperables, ERROR para criticos.
- Incluir contexto en cada log: ID de usuario, ID de transaccion, operacion.
- Usar identificadores de correlacion (correlation ID) para rastrear flujos distribuidos.
- Configurar alertas automaticas para errores criticos recurrentes.
- No registrar informacion sensible en logs (contrasenas, tokens, datos personales).
- Centralizar los logs en una plataforma de monitoreo para facilitar el diagnostico.
