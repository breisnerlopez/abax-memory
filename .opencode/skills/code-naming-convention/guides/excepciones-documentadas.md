# excepciones-documentadas

## Como documentar una excepcion legitima

Algunas excepciones son legitimas (legacy DB, terminos de dominio sin
traduccion, APIs publicas con consumidores). Para cada una:

1. Crear `docs/decisions/NNNN-naming-exception-<slug>.md` con:

   ```markdown
   ---
   tipo: naming-exception
   ambito: <ej. database tables, public API endpoints, env vars>
   fecha: YYYY-MM-DD
   aprobado-por: <tech-lead | solution-architect | sponsor>
   ---

   # Excepcion a la convencion de nombres en ingles: <ambito>

   ## Contexto

   Por que esta excepcion existe (legacy, dominio, contrato externo).

   ## Alcance exacto de la excepcion

   Que identificadores especificamente quedan en espanol y por que
   no es viable migrarlos en el corto plazo.

   ## Que SIGUE en ingles a pesar de la excepcion

   Lo que SI se nombra en ingles aunque interactue con la parte
   legacy (ej. el repository en codigo nuevo, los DTOs).

   ## Plan de migracion (si aplica)

   Cronograma o condicion bajo la cual la excepcion se eliminaria.
   Si la excepcion es permanente, indicarlo y por que.
   ```

2. Anadir el slug al test guard como exencion documentada.

3. Mencionar la excepcion en el README del proyecto cliente
   (seccion "Convenciones") para que nuevos developers no se sorprendan.
