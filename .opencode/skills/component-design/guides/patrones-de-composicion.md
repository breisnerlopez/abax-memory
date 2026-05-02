# patrones-de-composicion

## Patrones comunes

- **Compound Components**: Componentes que trabajan juntos compartiendo
  estado implicito (ej: Tabs, TabList, TabPanel).
- **Render Props**: Pasar una funcion como prop para delegar el renderizado.
- **Higher-Order Components (HOC)**: Envolver un componente para inyectar
  logica transversal (autenticacion, logging).
- **Hooks personalizados**: Extraer logica reutilizable en hooks
  (useForm, usePagination, useDebounce).
- **Slots / Children**: Usar props.children o slots con nombre para
  permitir insercion flexible de contenido.

## Antipatrones a evitar

- Componentes con mas de 300 lineas (dividir en subcomponentes).
- Props drilling de mas de 3 niveles (usar Context o estado global).
- Logica de negocio dentro de componentes de presentacion.
- Componentes que dependen de la estructura del DOM padre.
