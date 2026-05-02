# patrones-integracion-empresarial

## Guia de Patrones de Integracion Empresarial (EIP)

### Patrones de mensajeria
- **Message Channel**: Canal dedicado para cada tipo de mensaje.
- **Message Router**: Enrutamiento condicional basado en contenido o cabeceras.
- **Message Translator**: Transformacion de formato entre sistemas.
- **Message Filter**: Descarte de mensajes que no cumplen criterios.

### Patrones de composicion
- **Pipes and Filters**: Cadena de procesamiento por etapas.
- **Scatter-Gather**: Envio paralelo y agregacion de respuestas.
- **Saga**: Transacciones distribuidas con compensacion.
- **Choreography vs Orchestration**: Coordinacion descentralizada vs centralizada.

### Cuando usar cada patron
- Alta disponibilidad requerida: Mensajeria asincrona con colas persistentes.
- Baja latencia requerida: Comunicacion sincrona con cache.
- Desacoplamiento entre dominios: Eventos de dominio con esquemas versionados.
- Procesamiento de grandes volumenes: Streaming con Kafka o procesamiento batch.
