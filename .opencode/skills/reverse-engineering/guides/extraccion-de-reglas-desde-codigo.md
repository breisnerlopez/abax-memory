# extraccion-de-reglas-desde-codigo

Patrones comunes para encontrar reglas de negocio escondidas:

- **Validaciones de input**: Zod schemas, Joi schemas, Pydantic models, class-validator decorators, validators Django.
- **Condiciones de flujo**: branches if/else con literales del dominio (estados, tipos, categorias).
- **Queries SQL**: WHERE con calculos, CASE WHEN, vistas, stored procedures, triggers.
- **Calculos**: funciones puras que combinan inputs (precios, descuentos, impuestos, comisiones).
- **Configuracion declarativa**: feature flags, archivos JSON/YAML que parametrizan comportamiento.
- **Tests existentes**: cada test "it(...)" describe un comportamiento esperado - es regla documentada implicitamente.

Para cada regla extraida, capturar siempre: condicion (cuando aplica), accion (que hace), excepciones (cuando NO aplica), evidencia (archivo:linea).
