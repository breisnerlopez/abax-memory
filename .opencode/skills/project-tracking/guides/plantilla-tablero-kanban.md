# plantilla-tablero-kanban

# Plantilla de Tablero Kanban

Esta plantilla esta lista para copiar y adaptar a cualquier proyecto.
Reemplaza los valores entre corchetes con los datos reales del proyecto.

## Tablero de Tareas - [Nombre del Proyecto] - [Fecha Actualizacion]

**Equipo**: [nombre del equipo]
**Sprint/Semana**: [numero]
**WIP Limit**: 5 tareas en progreso

---

### Backlog (Priorizado)

| ID | Tarea | Responsable | Prioridad | Fase | Estimado (h) | Dependencia |
|---|---|---|---|---|---|---|
| T-001 | [descripcion tarea] | [nombre] | Alta/Media/Baja | [fase] | [horas] | [T-XXX o -] |
| T-002 | [descripcion tarea] | [nombre] | Alta/Media/Baja | [fase] | [horas] | [T-XXX o -] |

**Total backlog**: [N] tareas | **Estimado total**: [X] horas

---

### En Progreso (WIP Limit: 5)

| ID | Tarea | Responsable | Inicio | Dias | Bloqueante | Notas |
|---|---|---|---|---|---|---|
| T-003 | [descripcion] | [nombre] | [YYYY-MM-DD] | [N] | [Si/No: detalle] | [contexto] |

**WIP actual**: [N]/5

---

### En Revision

| ID | Tarea | Revisor | Fecha Envio | Dias en Revision | Resultado |
|---|---|---|---|---|---|
| T-004 | [descripcion] | [revisor] | [YYYY-MM-DD] | [N] | Pendiente/Aprobado/Devuelto |

**Regla**: Maximo 2 dias en revision. Despues se escala.

---

### Completado (Semana Actual)

| ID | Tarea | Responsable | Completado | Dias Total | Estimado (h) | Real (h) |
|---|---|---|---|---|---|---|
| T-005 | [descripcion] | [nombre] | [YYYY-MM-DD] | [N] | [X] | [Y] |

**Completadas esta semana**: [N] tareas

---

### Bloqueadas

| ID | Tarea | Responsable | Bloqueado Desde | Motivo | Escalado a | Estado |
|---|---|---|---|---|---|---|
| T-006 | [descripcion] | [nombre] | [YYYY-MM-DD] | [motivo] | [rol] | Abierto/Resolviendo |

---

### Reglas del Tablero

1. **WIP Limit**: Maximo 5 tareas en "En Progreso" por equipo.
2. **Dependencias**: No iniciar tarea si su dependencia no esta completada.
3. **Revision**: Maximo 2 dias. Pasado ese tiempo, escalar.
4. **Bloqueos**: Registrar inmediatamente con motivo y escalar si > 2 dias.
5. **Actualizacion**: El tablero se actualiza al menos una vez al dia.
6. **Prioridad**: Backlog ordenado por prioridad (Alta primero).
7. **Movimiento**: Solo mover cuando se cumple el criterio de la columna destino.
