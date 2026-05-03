# ejemplos-nativos-permitidos

## Casos donde @explore / @general / @plan / @docs son OK

### `@explore`: busqueda eficiente sin context overhead

> "Busca en el codebase todas las llamadas a `OpenAI.completions.create`
> para ver donde se usa el modelo legacy."

OK porque: read-only, no produce entregable, herramienta optimizada
para grep masivo. Un developer-backend cargaria todo su contexto
(system_prompt, skills, stack overrides) para hacer un grep.

### `@docs`: lookup de libreria externa

> "Mira la doc oficial de Quarkus 3.20 sobre native-image y resume
> los breaking changes desde 3.10."

OK porque: research externo, no decision arquitectural, no escribe
en proyecto. Solo si despues va a producir un ADR (decision), eso si
va al solution-architect.

### `@plan`: bosquejo previo, NO ADR

> "Bosqueja como podria estructurarse el motor de perfiles de
> dominio (3 alternativas) antes de que el solution-architect tome
> la decision formal."

OK porque: explorar opciones, NO comprometer una decision. El
solution-architect leera el bosquejo y producira el ADR formal.

### `@general`: resumen multi-area sin entregable

> "Resume el estado actual de los 5 entregables de fase 4 que estan
> en revision para que pueda decidir si avanzo a fase 5."

OK porque: lectura multi-area + sintesis, no produce ningun documento,
no toma decision. El orquestador usa el resumen para decidir.
