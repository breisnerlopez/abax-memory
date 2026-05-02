# evidencia-reproducible

## Guia de Evidencia Reproducible

### Que es Evidencia Reproducible
Evidencia reproducible es aquella que un tercero puede verificar de forma
independiente siguiendo los mismos pasos. No depende de la interpretacion
del autor ni de condiciones especiales de su entorno.

### Tipos de Evidencia Valida
- **Salida de terminal:** Copiar el comando ejecutado y su salida completa.
- **Capturas de pantalla:** Mostrar el estado del sistema con timestamp visible.
- **Logs del sistema:** Extractos relevantes con contexto suficiente.
- **Resultados de tests:** Reporte completo de la suite ejecutada.
- **URLs verificables:** Links a endpoints que demuestren el comportamiento.
- **Grabaciones de pantalla:** Para flujos complejos de interfaz de usuario.

### Tipos de Evidencia NO Valida
- "Lo probe y funciona" (sin detalle de que se probo ni como).
- "Los tests pasan" (sin mostrar cuales tests ni su salida).
- "Se ve bien" (sin criterio especifico de evaluacion).
- Capturas recortadas que no muestran contexto suficiente.
- Referencias a pruebas realizadas en el pasado ("la semana pasada funciono").

### Como Documentar Evidencia
1. Indicar el entorno donde se ejecuto la prueba (local, staging, produccion).
2. Listar los pasos exactos para reproducir la verificacion.
3. Incluir la salida completa sin editar.
4. Indicar la fecha y hora de la verificacion.
5. Si la evidencia tiene fecha de expiracion (ej: URL temporal), indicarlo.
