# como-detectar-task-cruzada

Senales de que la Task no es tuya:

1. **El verbo no encaja con tu rol**:
   - devops: deploy, configurar, monitorear, rotar, restaurar
   - qa-functional: validar, certificar, ejecutar caso de prueba, verificar criterio de aceptacion
   - developer: implementar, refactorizar, optimizar, escribir tests
   - tech-lead: revisar, aprobar, decidir patron, mentorizar
   - dba: modelar, migrar, indexar, optimizar query

2. **El entregable no es de tu fase principal**:
   - devops trabaja principalmente en fase 4 (env), 7 (deploy), 8 (operate)
   - qa-functional en fase 5 (QA), 6 (UAT), 7 (smoke post-deploy)
   - developer-* en fase 4 (construction)
   - business-analyst en fase 0/1/2 + verification de spec en fase 4

3. **El criterio de aceptacion menciona otro rol**:
   - "Aprobar el plan con product-owner" → si tu no eres PO, escala.
   - "Validar contra criterios de aceptacion" → si tu no eres BA o qa, escala.

Cuando 2 de las 3 senales estan presentes, RECHAZA.
