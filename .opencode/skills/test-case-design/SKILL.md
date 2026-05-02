---
name: test-case-design
description: Diseno sistematico de casos de prueba funcionales trazables a requerimientos, cubriendo caminos positivos, negativos, de borde y regresion.

---

# Diseno de Casos de Prueba

## Formato de Caso de Prueba
| Campo | Descripcion |
|---|---|
| ID | Identificador unico (TC-XXX) |
| Requerimiento | ID del requerimiento trazado |
| Precondicion | Estado necesario antes de ejecutar |
| Pasos | Secuencia numerada de acciones |
| Resultado esperado | Lo que debe ocurrir |
| Tipo | Positivo / Negativo / Borde / Regresion |
| Prioridad | Alta / Media / Baja |

## Tecnicas de diseno
1. Particion de equivalencia: agrupar inputs en clases.
2. Valores limite: probar en los bordes de cada particion.
3. Tabla de decision: combinaciones de condiciones.
4. Transicion de estados: flujos con estados multiples.

## Cuando usar esta habilidad
- Al recibir requerimientos funcionales aprobados.
- Al preparar un ciclo de pruebas.
- Al agregar cobertura para un defecto corregido.

## coverage-strategy
- Camino feliz: flujo principal exitoso.
- Validaciones: cada campo con datos invalidos.
- Permisos: acceso autorizado vs no autorizado.
- Concurrencia: operaciones simultaneas si aplica.
- Limites: maximos, minimos, vacios, nulos.
