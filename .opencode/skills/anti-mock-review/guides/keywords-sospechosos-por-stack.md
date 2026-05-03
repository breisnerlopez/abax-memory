# keywords-sospechosos-por-stack

### Java / Spring / Quarkus
- Clases: `InMemory*`, `Mock*`, `Fake*`, `Dummy*`, `Test*` (en src/main/)
- `Pattern.compile` / `.matches` en servicios que dicen extraer entidades
- `Arrays.asList(...)` o `List.of(...)` con datos hardcoded retornados como respuesta
- `@ConditionalOnProperty` con condiciones siempre falsas

### TypeScript / React / Node
- `MSW` (mock service worker) usado en codigo de produccion
- Funciones que retornan `Promise.resolve({...})` con valores fijos
- `// @ts-ignore` o `as any` cerca de calls a APIs externas
- Fixtures importados en componentes en lugar de fetch real

### Python
- `def fake_*`, `def mock_*` en modulos de produccion
- `return [...]` con datos hardcoded en endpoints
- `unittest.mock` importado fuera de tests/

### Go / Rust
- Tipos con sufijo `Stub`, `Fake`, `Dummy` en paquetes no-test
- Funciones que retornan `nil` o estructuras con valores zero sin llamar al servicio real
