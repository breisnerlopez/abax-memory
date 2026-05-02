# manejo-de-errores

## Clasificacion de errores HTTP

- **400 Bad Request**: Datos enviados invalidos. Mostrar errores de
  validacion en el formulario.
- **401 Unauthorized**: Token expirado o ausente. Intentar refresh
  del token; si falla, redirigir a login.
- **403 Forbidden**: Sin permisos. Mostrar mensaje de acceso denegado.
- **404 Not Found**: Recurso no encontrado. Mostrar estado vacio
  o redirigir.
- **409 Conflict**: Conflicto de datos (ej: registro duplicado).
  Informar al usuario y sugerir accion.
- **422 Unprocessable Entity**: Error de validacion del servidor.
  Mapear errores a los campos del formulario.
- **429 Too Many Requests**: Rate limit. Implementar backoff exponencial.
- **500+ Server Error**: Error del servidor. Mostrar mensaje generico
  y opcion de reintento.

## Patron de reintentos

- Reintentar solo errores de red y 5xx (nunca 4xx).
- Usar backoff exponencial: 1s, 2s, 4s (maximo 3 intentos).
- Mostrar indicador de reintento al usuario.
- Permitir cancelacion manual del reintento.
