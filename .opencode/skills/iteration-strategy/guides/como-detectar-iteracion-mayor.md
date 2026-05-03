# como-detectar-iteracion-mayor

## Como detectar que es iteracion mayor (vs fix menor)

Senales de iteracion MAYOR (activa esta skill):

- Usuario menciona "v2", "v3", "next gen", "evolucion".
- El propuesta cambia uno o mas de:
  - Dominio del producto (IT → Finance).
  - Stack tecnologico principal (Java → Go).
  - Audiencia objetivo (B2B → B2C).
  - Modelo de negocio (free → SaaS).
  - Arquitectura central (monolito → microservicios).
- El esfuerzo estimado es comparable a la fase 0-9 original.

Senales de fix MENOR (NO activa esta skill, usa `existing-docs-update-protocol`
directamente con estrategia B):

- Usuario menciona "bug", "fix", "ajuste", "correccion", "agregado pequeno".
- Cambio afecta < 20% de los entregables.
- Esfuerzo estimado es dias, no semanas.
- Stack tecnologico no cambia.
- Equipo del proyecto puede ser el mismo (no requiere onboarding).

En zona gris, **PREGUNTA al usuario directamente**: "¿Esto lo manejamos
como nueva iteracion mayor (v2) o como ajuste sobre v1?".
