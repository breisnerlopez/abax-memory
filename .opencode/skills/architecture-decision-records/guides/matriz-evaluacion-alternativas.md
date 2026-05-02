# matriz-evaluacion-alternativas

## Guia para Evaluar Alternativas Arquitectonicas

### Definir criterios de evaluacion
Seleccionar los atributos de calidad relevantes:
- **Rendimiento**: latencia, throughput, uso de recursos.
- **Escalabilidad**: horizontal, vertical, elastica.
- **Disponibilidad**: SLA objetivo, tolerancia a fallos.
- **Mantenibilidad**: complejidad, curva de aprendizaje, documentacion.
- **Seguridad**: autenticacion, cifrado, cumplimiento normativo.
- **Costo**: licencias, infraestructura, esfuerzo de desarrollo.
- **Madurez**: comunidad, soporte, adopcion en la industria.

### Matriz de decision ponderada
| Criterio | Peso | Alternativa A | Alternativa B | Alternativa C |
|----------|------|--------------|--------------|--------------|
| Rendimiento | 25% | 8 (2.0) | 6 (1.5) | 9 (2.25) |
| Escalabilidad | 20% | 7 (1.4) | 9 (1.8) | 7 (1.4) |
| Costo | 20% | 9 (1.8) | 5 (1.0) | 6 (1.2) |
| Mantenibilidad | 15% | 7 (1.05) | 8 (1.2) | 5 (0.75) |
| **Total** | | **6.25** | **5.50** | **5.60** |

### Documentar supuestos y restricciones
- Indicar el horizonte temporal de la decision (corto, mediano, largo plazo).
- Registrar las restricciones que condicionan la eleccion.
- Documentar los supuestos sobre carga, crecimiento y evolucion.
