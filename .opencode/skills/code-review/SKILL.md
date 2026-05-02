---
name: code-review
description: Revision sistematica de codigo fuente para detectar errores, mejorar calidad, asegurar cumplimiento de estandares y compartir conocimiento en el equipo.

---

# Revision de Codigo

## Checklist de Revision de Codigo
1. **Correccion**: El codigo hace lo que dice el requerimiento?
2. **Legibilidad**: Es claro sin necesidad de comentarios excesivos?
3. **Estandares**: Sigue las convenciones del proyecto?
4. **Tests**: Tiene tests unitarios para la logica nueva?
5. **Seguridad**: No expone datos sensibles? Valida inputs?
6. **Performance**: No introduce N+1, loops innecesarios, memory leaks?
7. **Error handling**: Maneja errores de forma consistente?
8. **DRY/SOLID**: No duplica logica? Responsabilidades claras?

## Formato de comentarios
- [BLOQUEO] Debe corregirse antes de aprobar.
- [MEJORA] Recomendado pero no bloquea.
- [PREGUNTA] Necesito entender la intencion.
- [NITPICK] Detalle menor de estilo.

## Cuando usar esta habilidad
- Al recibir codigo nuevo o modificado para revision.
- Antes de mergear un PR/MR a la rama principal.
- Al detectar problemas de calidad en el codigo existente.

## common-issues
- Funciones de mas de 30 lineas (dividir).
- Nombres de variables no descriptivos.
- Try/catch vacios o que tragan excepciones.
- Console.log / System.out.println olvidados.
- Imports no utilizados.
- Datos hardcodeados que deberian ser configuracion.
