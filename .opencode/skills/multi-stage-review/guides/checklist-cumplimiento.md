# checklist-cumplimiento

## Checklist Detallado de Cumplimiento Funcional

### Preparacion
1. Obtener la especificacion/requisitos completos del ticket o historia.
2. Leer la especificacion completa antes de abrir el codigo.
3. Listar cada requisito funcional como item individual.
4. Identificar requisitos implicitos (manejo de errores, validaciones).

### Verificacion por Requisito
Para cada requisito, completar:

| # | Requisito                    | Implementado | Funciona | Evidencia        |
|---|------------------------------|-------------|----------|------------------|
| 1 | [Descripcion del requisito]  | Si/No       | Si/No    | [Test o prueba]  |
| 2 | ...                          | ...         | ...      | ...              |

### Verificacion de Alcance
- Revisar que no se implemento funcionalidad no solicitada.
- Verificar que los cambios se limitan a los archivos necesarios.
- Confirmar que no se modificaron comportamientos existentes sin justificacion.

### Verificacion de Integracion
- Los endpoints nuevos siguen la convencion existente de la API.
- Los cambios en modelos de datos son compatibles con el esquema existente.
- Las migraciones (si las hay) son reversibles.
- Los cambios en interfaces publicas son backward-compatible (o se documento el breaking change).
