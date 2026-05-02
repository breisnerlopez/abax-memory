# principios-de-arquitectura

- Separar la logica de negocio de la logica de infraestructura.
- No colocar logica de negocio en controladores; delegar al servicio.
- Usar transacciones de base de datos para operaciones que modifican multiples registros.
- Implementar logging estructurado en puntos criticos del flujo.
- Configurar valores externos mediante variables de entorno o archivos de configuracion.
- Evitar dependencias circulares entre servicios.
