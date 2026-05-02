# estrategias-descomposicion

## Estrategias de Descomposicion segun Tipo de Requerimiento

### CRUD simple
1. Migracion de base de datos (crear tabla/campos).
2. Modelo de dominio y validaciones.
3. Repositorio / capa de acceso a datos.
4. Servicio con logica de negocio.
5. Endpoints de API (crear, leer, actualizar, eliminar).
6. Componentes de interfaz (listado, formulario, detalle).
7. Pruebas unitarias y de integracion.

### Integracion con sistema externo
1. Investigacion y prueba de concepto con la API externa.
2. Definicion del contrato y modelo de datos.
3. Cliente HTTP / adaptador para el servicio externo.
4. Manejo de errores, reintentos y circuit breaker.
5. Mapeo de datos entre el modelo externo y el interno.
6. Servicio orquestador del flujo de integracion.
7. Pruebas con mocks y pruebas de integracion reales.

### Cambio transversal (cross-cutting)
1. Analisis de impacto en todos los componentes afectados.
2. Creacion del componente compartido (middleware, libreria).
3. Migracion componente por componente (en orden de riesgo).
4. Pruebas de regresion por cada componente migrado.
5. Limpieza de codigo obsoleto.
