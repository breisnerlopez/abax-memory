# patrones-aria-comunes

## Roles y atributos ARIA mas utilizados

- **role="button"**: Solo si no se puede usar <button>. Requiere
  manejo de Enter y Space con keydown.
- **role="dialog"** + aria-modal="true": Para modales. Atrapar
  el foco dentro del dialogo.
- **role="alert"**: Para mensajes urgentes que deben anunciarse
  inmediatamente por el lector de pantalla.
- **role="navigation"** + aria-label: Para regiones de navegacion.
  Usar aria-label si hay multiples navs.
- **aria-expanded**: En botones que controlan paneles colapsables
  (acordeones, menus desplegables).
- **aria-live="polite"**: Para regiones cuyo contenido se actualiza
  dinamicamente (notificaciones, contadores).
- **aria-describedby**: Para asociar texto de ayuda o errores
  a un campo de formulario.

## Reglas fundamentales de ARIA

1. No usar ARIA si existe un elemento HTML nativo equivalente.
2. No cambiar la semantica nativa (no poner role="button" en un <a>
   si es realmente un enlace).
3. Todos los controles interactivos con ARIA deben ser operables
   por teclado.
4. No usar aria-hidden="true" en elementos enfocables.
5. Todos los elementos interactivos deben tener un nombre accesible.
