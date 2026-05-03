# nombre-de-rama

El nombre de rama sigue el patron `abax/<project-name-kebab-case>`.

Reglas de sanitizacion del nombre del proyecto:
- lowercase
- espacios y caracteres especiales -> `-`
- colapsar `-` consecutivos
- quitar `-` al inicio o final
- max 60 caracteres (truncar)

Ejemplos:
- "Sistema de Gestión de Pagos" -> `abax/sistema-de-gestion-de-pagos`
- "API_v2.0" -> `abax/api-v2-0`
- "MiProyecto" -> `abax/miproyecto`

Si la rama ya existe en local o remoto, hacer checkout (no recrear).
