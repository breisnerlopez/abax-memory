# diseno-de-apis

- Usar verbos HTTP semanticamente correctos: GET (consulta), POST (creacion), PUT (actualizacion completa), PATCH (actualizacion parcial), DELETE (eliminacion).
- Nombrar recursos en plural y en minusculas: /api/v1/usuarios, /api/v1/ordenes.
- Retornar codigos de estado HTTP apropiados: 200, 201, 204, 400, 401, 403, 404, 500.
- Implementar paginacion para endpoints que retornan listas.
- Versionar las APIs desde el inicio (v1, v2).
- Incluir mensajes de error descriptivos en el cuerpo de respuesta.
