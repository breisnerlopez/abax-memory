# por-stack

## Convenciones por stack tecnologico

### TypeScript / JavaScript (React, Next.js, NestJS, Node)

- Identificadores: `camelCase` (variables, funciones), `PascalCase` (clases, types).
- Archivos: `kebab-case.ts` para utilidades, `PascalCase.tsx` para componentes React.
- Endpoints REST: `kebab-case` plural (`/users`, `/order-items`).
- Env vars: `UPPER_SNAKE_CASE`. Prefijos: `NEXT_PUBLIC_*`, `VITE_*` para client-exposed.

### Java (Spring Boot, Quarkus)

- Identificadores: `camelCase` (vars/methods), `PascalCase` (classes).
- Packages: `com.acme.product.module`. Todos en ingles.
- Endpoints REST: `@RequestMapping("/users")`. Path params: `{userId}`.
- Env vars: `UPPER_SNAKE_CASE`. Prefijo `SPRING_*` para Spring config override.

### Python (FastAPI, Django)

- Identificadores: `snake_case` (vars/methods), `PascalCase` (classes).
- Modulos: `snake_case.py`.
- Endpoints: FastAPI usa el path tal cual; Django convierte URL patterns. Usar `kebab-case` o `snake_case` segun convencion del proyecto.
- Env vars: `UPPER_SNAKE_CASE`. Django settings: `SECRET_KEY`, `DEBUG`, `ALLOWED_HOSTS`.

### Go (Fiber)

- Identificadores: `camelCase` (privados), `PascalCase` (exported).
- Archivos: `snake_case.go`.
- Packages: `lowercase` corto (`auth`, `user`, `order`).
- Endpoints: `kebab-case` plural.

### Rust (Axum)

- Identificadores: `snake_case` (vars/functions), `PascalCase` (structs/enums/traits).
- Crates/modulos: `snake_case`.
- Endpoints: `kebab-case` plural.
- Env vars: `UPPER_SNAKE_CASE`.

### Bases de datos (Postgres, MySQL, etc.)

- Tablas: `snake_case` plural (`users`, `order_items`).
- Columnas: `snake_case` (`first_name`, `created_at`).
- Indices: `idx_<table>_<columns>` (`idx_orders_user_id_created_at`).
- Foreign keys: `fk_<table>_<ref_table>` (`fk_orders_users`).
- Enums: valores en `UPPER_SNAKE_CASE` o `snake_case` segun consenso del equipo, **siempre en ingles**.

### YAML / JSON

- Claves: `camelCase` para JSON consumido por JS/TS, `snake_case` para Python/Ruby, `kebab-case` para configs CLI.
- **Siempre en ingles**, sin importar el idioma del proyecto.
