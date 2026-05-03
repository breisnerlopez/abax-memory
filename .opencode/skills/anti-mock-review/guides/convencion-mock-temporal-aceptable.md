# convencion-mock-temporal-aceptable

Hay casos legitimos para mock temporal: el equipo no tiene
credenciales del proveedor todavia, la API externa esta en mantenimiento,
la libreria oficial esta en beta y rompe builds, etc. En esos casos:

1. El mock OBLIGATORIAMENTE lleva la marca:

   ```
   // MOCK: <razon concreta + ticket de bloqueo> // REPLACE_BEFORE_PROD
   ```

2. El developer escala al orquestador la lista completa de mocks
   creados en su entregable, con su justificacion individual.

3. El technical-debt-management skill registra cada mock como deuda
   con prioridad alta y fecha de resolucion antes del deployment.

4. El deployment-plan (fase 7) NO se aprueba si quedan mocks sin
   resolver para features criticas. El sponsor decide si features
   no criticas pueden ir a prod con stub temporal.

Lo que NO es aceptable:
- Mock sin marca = mock silencioso = rechazo.
- Marca sin justificacion ni bloqueo concreto = rechazo.
- "Lo cambio despues" sin fecha ni responsable.
