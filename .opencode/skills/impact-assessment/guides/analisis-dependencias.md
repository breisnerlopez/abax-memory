# analisis-dependencias

## Guia para Mapeo de Dependencias

### Tecnicas de descubrimiento
- Revisar imports y referencias en el codigo fuente.
- Analizar configuraciones de API gateway y service mesh.
- Consultar registros de service discovery (Consul, Eureka).
- Revisar logs de comunicacion entre servicios.
- Analizar tracing distribuido (Jaeger, Zipkin) para flujos reales.

### Matriz de dependencias
| Servicio afectado | Tipo de dependencia | Tipo de cambio requerido | Esfuerzo | Equipo responsable |
|-------------------|--------------------|--------------------------|---------|--------------------|

### Clasificacion de dependencias
- **Critica**: El servicio no funciona sin esta dependencia.
- **Importante**: Funcionalidad degradada sin esta dependencia.
- **Opcional**: Funcionalidad menor o alternativa disponible.

### Estrategias de mitigacion por tipo
- Dependencia critica: Despliegue coordinado, feature flags, blue-green.
- Dependencia importante: Versionado de API, periodo de transicion.
- Dependencia opcional: Comunicacion y timeline de deprecacion.
