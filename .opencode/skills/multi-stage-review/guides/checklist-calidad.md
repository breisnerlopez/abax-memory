# checklist-calidad

## Checklist Detallado de Calidad Tecnica

### Codigo Limpio
- Funciones cortas con responsabilidad unica (idealmente < 30 lineas).
- Maximo 3 niveles de indentacion (evitar arrow code).
- Sin codigo comentado (el control de versiones es para eso).
- Sin variables no utilizadas.
- Sin imports no utilizados.
- Constantes con nombres descriptivos en lugar de numeros magicos.

### Manejo de Errores
- Cada operacion que puede fallar tiene manejo de error.
- Los errores se propagan de forma consistente (excepciones, Result types, etc.).
- Los mensajes de error son utiles para diagnostico (incluyen contexto).
- No se atrapan excepciones genericas sin razon (catch Exception/catch all).
- Los recursos se liberan correctamente en caso de error (finally, using, defer).

### Tests
- Cada funcion publica tiene al menos un test.
- Tests para casos exitosos y casos de error.
- Tests para validaciones de entrada.
- Mocks y stubs son minimos y justificados.
- No hay logica compleja dentro de los tests.
- Los tests fallan por la razon correcta cuando fallan.

### Seguridad (Checklist Rapido)
- Input validation en todo dato que viene del exterior.
- Parametros en queries SQL (nunca concatenacion de strings).
- Autenticacion verificada en endpoints protegidos.
- Autorizacion verificada (el usuario tiene permiso para esta accion?).
- No hay logging de datos sensibles (passwords, tokens, PII).
- Las dependencias nuevas se verificaron contra bases de vulnerabilidades.

### Performance (Checklist Rapido)
- No hay queries dentro de loops (N+1).
- Las colecciones grandes se procesan con paginacion.
- Las operaciones costosas tienen cache si se ejecutan frecuentemente.
- Los indices de BD cubren las consultas nuevas.
- No hay lecturas de archivos grandes en memoria completa sin necesidad.
