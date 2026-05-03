# priorizacion-de-areas-a-documentar

No toda parte del sistema merece el mismo nivel de detalle. Prioriza por:

- **Riesgo**: lo que si falla, falla caro (pagos, autenticacion, datos personales).
- **Frecuencia de cambio**: lo que se toca a menudo necesita docs vivas.
- **Onboarding**: lo que un nuevo desarrollador toca el primer dia.
- **Integraciones externas**: contratos con sistemas que no controlamos - cambios cuestan.
- **Reglas de negocio criticas**: las que el negocio consultaria si las pierde.

Documenta primero el 20% de los componentes que cubren el 80% del riesgo y la actividad.
