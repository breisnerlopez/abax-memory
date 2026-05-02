# estrategias-paralelizacion

## Guia de Estrategias de Paralelizacion

### Principio
Maximizar el trabajo que puede hacerse en paralelo reduce el tiempo total
de implementacion. Pero la paralelizacion solo es segura cuando las tareas
son verdaderamente independientes.

### Identificar Tareas Paralelizables

Dos tareas pueden ejecutarse en paralelo si y solo si:
1. No modifican los mismos archivos.
2. Ninguna depende del output de la otra.
3. No crean conflictos de merge al integrarse.

### Patrones Comunes de Paralelizacion

#### Patron 1: Modelo + Servicio en paralelo con Frontend
```
T-01 (Modelo) ──> T-03 (Servicio) ──> T-05 (Controller)
T-02 (UI mockup) ──> T-04 (Componentes UI) ──> T-06 (Integracion)
```
El backend y el frontend pueden avanzar en paralelo si se acuerda
el contrato de la API de antemano.

#### Patron 2: Tests en paralelo con implementacion
Un agente/desarrollador escribe los tests (basados en la spec) mientras
otro implementa el codigo. Requiere que la interfaz (firmas de funciones,
tipos) se defina primero en una tarea previa.

#### Patron 3: Componentes independientes
Si el sistema tiene componentes que no interactuan entre si (ej: modulo de
notificaciones y modulo de reportes), pueden desarrollarse completamente
en paralelo.

### Anti-Patrones de Paralelizacion

| Anti-Patron                              | Riesgo                                           |
|------------------------------------------|--------------------------------------------------|
| Dos tareas editan el mismo archivo        | Conflictos de merge, una sobrescribe a la otra.  |
| Tarea depende de interfaz no definida     | El ejecutor inventa la interfaz, genera retrabajo.|
| Paralelizar sin definir contratos          | Las piezas no encajan al integrar.               |
| Asumir que "se resuelve en el merge"       | Los merges complejos introducen bugs sutiles.     |

### Regla de Oro
Antes de paralelizar, crear una tarea inicial (T-00) que defina todos
los contratos, interfaces y tipos compartidos. Esta tarea se ejecuta
primero y es dependencia de todas las demas. Esto asegura que las
piezas paralelas seran compatibles al integrarse.
