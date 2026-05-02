# checklist-gate-de-fase

# Checklist: Gate de Fase

## Informacion del Gate

- **Proyecto**: [Nombre del Proyecto]
- **Fase evaluada**: [Nombre de la Fase]
- **Fecha del gate**: [Fecha]
- **Responsable del gate**: [Nombre/Rol]
- **Participantes**: [Lista de participantes]

---

## Paso 1: Inventario de Entregables

Confirmar que todos los entregables de la fase estan listados en el registro:

- [ ] Se revisaron todos los entregables obligatorios de la fase.
- [ ] Se revisaron todos los entregables opcionales de la fase.
- [ ] No hay entregables faltantes que deberian haberse incluido.

**Total entregables de la fase**: [N]
**Obligatorios**: [N]
**Opcionales**: [N]

## Paso 2: Validacion de Entregables Obligatorios

Para cada entregable obligatorio, verificar:

- [ ] Tiene responsable asignado.
- [ ] Tiene aprobador asignado.
- [ ] Estado actual es "Aprobado".
- [ ] Fecha real de entrega esta registrada.
- [ ] El aprobador confirmo la calidad del entregable.

**Obligatorios aprobados**: [X] de [Y]

## Paso 3: Validacion de Entregables Opcionales

Para cada entregable opcional, verificar:

- [ ] Estado es "Aprobado" o "No Aplica".
- [ ] Si es "No Aplica", la justificacion esta documentada.
- [ ] Si es "Aprobado", el aprobador confirmo la calidad.

**Opcionales aprobados**: [X] de [Y]
**Opcionales no aplican**: [Z] de [Y]

## Paso 4: Calculo de Completitud

- Completitud obligatorios: ([Aprobados] / [Total obligatorios]) x 100 = [%]
- Completitud opcionales: ([Aprobados + No Aplica] / [Total opcionales]) x 100 = [%]
- Completitud general: ([Total resueltos] / [Total entregables]) x 100 = [%]

## Paso 5: Preguntas de Validacion

Responder cada pregunta antes de tomar la decision:

1. Todos los entregables obligatorios estan aprobados?
   - [ ] Si → Continuar
   - [ ] No → Gate RECHAZADO (documentar pendientes)

2. Existen riesgos abiertos asociados a entregables de esta fase?
   - [ ] No → Continuar
   - [ ] Si → Documentar riesgos y plan de mitigacion

3. Las dependencias de la siguiente fase estan satisfechas?
   - [ ] Si → Continuar
   - [ ] No → Documentar dependencias bloqueantes

4. El equipo confirma que no hay deuda tecnica critica sin documentar?
   - [ ] Si → Continuar
   - [ ] No → Documentar y evaluar impacto

5. Los stakeholders relevantes fueron informados del estado?
   - [ ] Si → Continuar
   - [ ] No → Completar comunicacion antes de cerrar gate

## Paso 6: Decision del Gate

```markdown
### Gate de Fase: [Nombre Fase]
- **Fecha**: [Fecha]
- **Aprobador**: [Nombre/Rol]
- **Obligatorios**: [X/Y] aprobados
- **Opcionales**: [X/Y] aprobados, [Z] no aplican
- **Completitud**: [%]
- **Decision**: APROBADO / RECHAZADO
- **Observaciones**: [Notas]
```

## Paso 7: Criterios de Escalamiento

Escalar al nivel superior si:

- Un entregable obligatorio lleva mas de 2 ciclos de rechazo.
- La completitud de obligatorios es menor al 70% en la fecha limite del gate.
- Existe un bloqueo externo que impide completar un entregable critico.
- El responsable de un entregable no esta disponible y no hay sustituto asignado.

**Ruta de escalamiento**:
1. Primer nivel: Project Manager.
2. Segundo nivel: Sponsor del proyecto.
3. Tercer nivel: Comite de gobernanza / PMO.

---

**Resultado del gate**: [APROBADO / RECHAZADO]
**Firma del responsable**: ___________________
**Fecha**: [Fecha]
