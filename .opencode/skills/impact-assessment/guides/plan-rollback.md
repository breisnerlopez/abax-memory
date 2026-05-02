# plan-rollback

## Guia para Diseno de Plan de Rollback

### Principios del rollback
- Todo cambio debe tener un plan de rollback documentado antes de ejecutarse.
- El rollback debe poder ejecutarse en menos tiempo que el cambio original.
- Probar el rollback en entornos no productivos antes del despliegue.

### Estrategias de rollback segun el tipo de cambio
- **Cambio de codigo**: Revertir despliegue a version anterior (blue-green, canary).
- **Cambio de esquema de BD**: Scripts de rollback para migraciones (down migrations).
- **Cambio de configuracion**: Restaurar configuracion anterior desde version control.
- **Cambio de infraestructura**: Terraform/IaC para revertir a estado anterior.

### Checklist de rollback
- [ ] Script o procedimiento de rollback documentado paso a paso.
- [ ] Tiempo estimado de ejecucion del rollback.
- [ ] Criterios que activan el rollback (metricas, errores, timeout).
- [ ] Datos creados durante el cambio: como se manejan en rollback.
- [ ] Comunicacion: a quien notificar antes y despues del rollback.
