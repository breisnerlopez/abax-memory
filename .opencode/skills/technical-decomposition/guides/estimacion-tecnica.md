# estimacion-tecnica

## Guia de Estimacion Tecnica

### Factores que afectan la estimacion
- **Complejidad tecnica**: algoritmos, concurrencia, integraciones.
- **Incertidumbre**: tecnologia nueva, requisitos ambiguos, API externa.
- **Deuda tecnica**: refactoring necesario para implementar el cambio.
- **Pruebas**: cobertura requerida y complejidad de los escenarios.
- **Coordinacion**: dependencia de otros equipos o revisiones.

### Tecnica de estimacion por comparacion
1. Identificar una tarea de referencia ya completada por el equipo.
2. Comparar la nueva tarea en complejidad, volumen y riesgo.
3. Ajustar la estimacion proporcionalmente.

### Margen de seguridad por nivel de incertidumbre
- Baja incertidumbre: estimacion x 1.2 (20% margen).
- Media incertidumbre: estimacion x 1.5 (50% margen).
- Alta incertidumbre: estimacion x 2.0 (100% margen) o spike previo.

### Senales de que la tarea necesita mas descomposicion
- Estimacion superior a 3 dias de trabajo.
- La descripcion contiene multiples "y" (hacer X y Y y Z).
- No se puede explicar el alcance en 2-3 oraciones.
- Diferentes miembros del equipo dan estimaciones muy dispares.
