# rest-vs-graphql

## REST - Cuando usarlo

- APIs publicas o de terceros que solo ofrecen REST.
- Operaciones CRUD simples con recursos bien definidos.
- Cuando se necesita cache HTTP nativo (ETags, Cache-Control).
- Endpoints con respuestas predecibles y estables.

## GraphQL - Cuando usarlo

- Cuando el frontend necesita datos de multiples recursos en una sola peticion.
- Para evitar over-fetching (recibir mas datos de los necesarios)
  y under-fetching (necesitar multiples llamadas).
- Cuando los requerimientos de datos cambian frecuentemente.
- Con Apollo Client o urql para cache normalizado automatico.

## Buenas practicas de GraphQL en el cliente

- Definir queries y mutations en archivos .graphql separados.
- Usar fragments para reutilizar selecciones de campos.
- Generar tipos automaticamente con GraphQL Code Generator.
- Implementar politicas de cache (cache-first, network-only) por query.
- Usar subscriptions solo cuando se necesite tiempo real (WebSocket).
