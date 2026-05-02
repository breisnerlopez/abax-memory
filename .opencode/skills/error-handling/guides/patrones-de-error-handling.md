# patrones-de-error-handling

- Fallar rapido (fail fast): validar entradas al inicio del metodo.
- Lanzar excepciones especificas, nunca Exception generica.
- Capturar excepciones en el nivel correcto; no atrapar lo que no se puede manejar.
- Usar bloques try-catch solo cuando se tiene una estrategia de recuperacion.
- Propagar excepciones con contexto adicional cuando sea necesario (wrap).
- Documentar las excepciones que un metodo puede lanzar.
