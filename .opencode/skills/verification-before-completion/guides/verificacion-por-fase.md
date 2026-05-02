# verificacion-por-fase

## Guia de Verificacion por Fase del Proyecto

### Fase de Desarrollo
- Ejecutar tests unitarios antes de cada commit.
- Verificar que el build local pasa antes de pushear.
- Correr el linter y corregir todos los errores.
- Hacer una prueba manual rapida del flujo modificado.
- Revisar el diff completo antes de crear el PR.

### Fase de QA
- Verificar cada criterio de aceptacion individualmente con evidencia.
- Ejecutar la suite completa de tests automatizados.
- Realizar pruebas exploratorias en areas adyacentes al cambio.
- Documentar cada verificacion con capturas o logs.
- Confirmar que los tests de regresion pasan al 100%.

### Fase de Deploy
- Verificar que el artefacto a desplegar corresponde al commit aprobado.
- Ejecutar el deploy en un entorno de staging primero.
- Correr health checks automaticos post-deploy.
- Verificar manualmente los flujos criticos del negocio.
- Monitorear metricas y logs durante los primeros 15 minutos.
- Tener plan de rollback listo y verificado antes de desplegar.

### Fase de Post-Deploy
- Monitorear alertas durante las primeras 2 horas.
- Verificar que no hay aumento en tasas de error.
- Confirmar que los tiempos de respuesta se mantienen estables.
- Validar que los reportes y dashboards reflejan datos correctos.
