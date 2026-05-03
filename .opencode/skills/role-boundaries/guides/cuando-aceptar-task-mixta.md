# cuando-aceptar-task-mixta

Hay zonas grises legitimas. Algunos ejemplos:

- **Devops corriendo `curl /health`**: SI es tuyo. Es smoke test
  operacional, no validacion de feature.
- **Qa-functional consultando logs del deploy para reportar bug**:
  SI es tuyo. Necesitas ver logs para entender el fallo.
- **Tech-lead escribiendo un POC pequeno antes del code review**:
  SI es tuyo. POC es para evaluar diseño, no implementacion final.
- **Developer documentando en el codigo (javadoc/jsdoc) y en
  README de su modulo**: SI es tuyo. Documentacion del API que
  escribiste, no manual de usuario.

La regla de oro: **si tu rol es el principal aportante de valor a
esa actividad, hazla. Si es accesoria a otro rol que es el principal,
delega**.
