---
name: deployment-plan
description: Elaboracion de planes de despliegue a produccion incluyendo checklist, ventana de pase, rollback, comunicacion y verificacion post-deploy.

---

# Plan de Despliegue

## Estructura del Plan de Despliegue
1. **Componentes**: que se despliega (version, artefactos).
2. **Orden de ejecucion**: secuencia de pasos.
3. **Ventana**: fecha, hora inicio/fin, duracion estimada.
4. **Responsables**: quien ejecuta cada paso.
5. **Prerequisitos**: aprobaciones, ambientes, datos.
6. **Rollback**: pasos para revertir cada componente.
7. **Verificacion**: smoke tests post-deploy.
8. **Comunicacion**: notificacion a usuarios y soporte.
9. **Contingencia**: equipo disponible, escalamiento.

## Checklist minimo
- [ ] Acta UAT aprobada.
- [ ] Scripts de BD revisados por DBA.
- [ ] Pipeline verde en staging.
- [ ] Plan de rollback probado.
- [ ] Soporte informado.
- [ ] Comunicacion a usuarios programada.
- [ ] Smoke tests definidos.

## Cuando usar esta habilidad
- Al preparar un pase a produccion.
- Al definir estrategia de rollback.
- Al coordinar un despliegue con multiples componentes.

## rollback-strategy
## Estrategias de rollback
- **Blue-green**: switch de ambiente.
- **Canary**: rollback del porcentaje desplegado.
- **DB rollback**: scripts DOWN de migracion.
- **Feature flags**: desactivar feature sin redeploy.
