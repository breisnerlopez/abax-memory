# diseno-de-apis

- Usar verbos HTTP semanticamente correctos: GET (consulta), POST (creacion), PUT (actualizacion completa), PATCH (actualizacion parcial), DELETE (eliminacion).
- Nombrar recursos en plural, minusculas y SIEMPRE en INGLES: /api/v1/users, /api/v1/orders, /api/v1/order-items. Path params en ingles tambien (`/users/{userId}/orders/{orderId}`). Ver skill `code-naming-convention`.
- Retornar codigos de estado HTTP apropiados: 200, 201, 204, 400, 401, 403, 404, 500.
- Implementar paginacion para endpoints que retornan listas.
- Versionar las APIs desde el inicio (v1, v2).
- Incluir mensajes de error descriptivos en el cuerpo de respuesta.
