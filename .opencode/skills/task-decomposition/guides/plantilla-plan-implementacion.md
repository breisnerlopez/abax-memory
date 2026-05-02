# plantilla-plan-implementacion

## Plantilla de Plan de Implementacion

Copiar y completar esta plantilla para cada plan nuevo:

```markdown
# Plan de Implementacion: [Nombre del Requisito]

**Requisito:** [Descripcion breve del requisito]
**Fuente:** [Ticket/Issue/Historia de usuario]
**Total de tareas:** [N]
**Tiempo estimado total:** [suma de estimaciones]
**Paralelismo maximo:** [cuantas tareas pueden ejecutarse simultaneamente]

## Dependencias Externas
- [Listar cualquier dependencia externa: APIs, servicios, datos, accesos]

## Diagrama de Dependencias
```
T-01 ──> T-02 ──> T-04
               ──> T-05
T-03 ──────────> T-05
T-05 ──> T-06
```

---

### T-01: [Nombre del Componente] [~N min]

- **Descripcion:** [Que hacer, especifico y completo]
- **Archivos afectados:**
  - Crear: [ruta absoluta]
  - Modificar: [ruta absoluta]
- **Dependencias:** [IDs o "ninguna"]
- **Verificacion:** [comando ejecutable]
- [ ] Completada

### T-02: [Nombre del Componente] [~N min]

(... repetir para cada tarea ...)

---

## Resumen de Progreso
- [ ] T-01: [descripcion breve]
- [ ] T-02: [descripcion breve]
- [ ] ...
```

### Notas sobre la Plantilla
- El diagrama de dependencias es obligatorio cuando hay mas de 4 tareas.
- El resumen de progreso al final permite seguimiento rapido.
- Las dependencias externas se listan por separado para visibilidad.
- El paralelismo maximo indica cuantos ejecutores pueden trabajar a la vez.
