# metricas-y-formulas

# Guia de Metricas y Formulas

Guia detallada para calcular, interpretar y actuar sobre cada metrica
del sistema de seguimiento de proyecto.

## 1. Avance vs Plan

**Formula**: (Tareas completadas / Tareas planificadas) x 100
**Frecuencia**: Semanal
**Target**: >= 90%

**Como calcular**:
- Contar tareas completadas en el periodo (semana o fase).
- Dividir entre tareas que debian completarse segun el plan.
- Multiplicar por 100 para obtener porcentaje.

**Interpretacion**:
- >= 90%: El proyecto avanza segun lo planificado.
- 70-89%: Hay retraso moderado, investigar causas.
- < 70%: Retraso critico, requiere accion inmediata.

**Acciones correctivas**:
- Revisar si las tareas incompletas tienen bloqueos.
- Evaluar si las estimaciones fueron realistas.
- Considerar reasignacion de recursos si hay sobrecarga.
- Si es sistematico, replanificar con estimaciones ajustadas.

## 2. Lead Time

**Formula**: Fecha completado - Fecha creacion de la tarea
**Frecuencia**: Por tarea (agregado semanal)
**Target**: <= dias estimados para la tarea

**Como calcular**:
- Registrar fecha en que la tarea entra al backlog.
- Registrar fecha en que la tarea se marca como completada.
- La diferencia en dias es el lead time.

**Interpretacion**:
- Menor o igual al estimado: buena planificacion.
- Hasta 20% mayor: aceptable, monitorear tendencia.
- Mas de 20% mayor: problema en el flujo o estimacion.

**Acciones correctivas**:
- Analizar en que columna se acumula mas tiempo.
- Reducir tareas en espera (backlog excesivo).
- Mejorar la priorizacion para reducir tiempo de espera.

## 3. Cycle Time

**Formula**: Fecha completado - Fecha inicio de trabajo activo
**Frecuencia**: Por tarea (agregado semanal)
**Target**: Tendencia decreciente semana a semana

**Como calcular**:
- Registrar fecha en que la tarea pasa a "En Progreso".
- Registrar fecha en que la tarea se completa.
- La diferencia en dias es el cycle time.

**Interpretacion**:
- Cycle time decreciente: el equipo mejora su eficiencia.
- Cycle time estable: flujo predecible (bueno).
- Cycle time creciente: problemas de complejidad o bloqueos.

**Acciones correctivas**:
- Dividir tareas grandes en subtareas mas pequenas.
- Reducir contexto switching (respetar WIP limit).
- Eliminar pasos innecesarios en el flujo de trabajo.

## 4. WIP (Work In Progress)

**Formula**: Conteo de tareas en estado "En Progreso"
**Frecuencia**: Diario
**Target**: <= WIP Limit (5 por equipo)

**Como calcular**:
- Contar tareas en la columna "En Progreso" del tablero.

**Interpretacion**:
- Dentro del limite: flujo saludable.
- En el limite: equipo a capacidad maxima, no agregar mas.
- Sobre el limite: sobrecarga, riesgo de baja calidad.

**Acciones correctivas**:
- Completar tareas en progreso antes de iniciar nuevas.
- Identificar y resolver bloqueos que impiden completar.
- Si WIP > limite por 3+ dias, reunion de capacidad.

## 5. Tasa de Bloqueo

**Formula**: (Tareas bloqueadas / Total tareas en progreso) x 100
**Frecuencia**: Semanal
**Target**: <= 10%

**Como calcular**:
- Contar tareas marcadas como bloqueadas durante la semana.
- Dividir entre total de tareas que estuvieron en progreso.
- Multiplicar por 100.

**Interpretacion**:
- <= 10%: Nivel normal de impedimentos.
- 10-25%: Problemas frecuentes, revisar dependencias.
- > 25%: Problema sistematico, accion urgente requerida.

**Acciones correctivas**:
- Categorizar bloqueos por tipo (dependencia, recurso, tecnico).
- Atacar la categoria mas frecuente.
- Mejorar la identificacion temprana de dependencias.
- Escalar bloqueos segun la tabla de escalamiento.

## 6. Defect Density

**Formula**: Defectos encontrados / Tareas completadas en el periodo
**Frecuencia**: Por ciclo de QA
**Target**: Tendencia decreciente

**Como calcular**:
- Contar defectos reportados en el ciclo de QA.
- Dividir entre tareas que pasaron a QA en ese ciclo.

**Interpretacion**:
- Decreciente: la calidad mejora con el tiempo.
- Estable bajo: calidad consistente y aceptable.
- Creciente: problemas de calidad, revisar proceso.

**Acciones correctivas**:
- Reforzar criterios de "Definition of Done".
- Agregar revisiones de codigo mas rigurosas.
- Identificar patrones en los tipos de defectos.
- Capacitar al equipo en las areas con mas defectos.

## 7. Desvio de Cronograma

**Formula**: (Fecha real - Fecha planificada) / Duracion total de la fase x 100
**Frecuencia**: Por fase del proyecto
**Target**: <= 5%

**Como calcular**:
- Comparar la fecha real de fin de fase vs la planificada.
- Dividir la diferencia entre la duracion total de la fase.
- Multiplicar por 100 para obtener porcentaje.

**Interpretacion**:
- <= 5%: Desvio aceptable, dentro de margenes normales.
- 5-10%: Desvio moderado, ajustar plan si es posible.
- > 10%: Desvio critico, requiere reunion de riesgo.

**Acciones correctivas**:
- Analizar causas raiz del desvio.
- Evaluar impacto en fases posteriores.
- Proponer plan de recuperacion o ajuste de alcance.
- Comunicar a stakeholders si afecta fecha de entrega.

## Resumen de Acciones por Rango

| Metrica | Normal | Alerta | Critico |
|---|---|---|---|
| Avance vs Plan | >= 90% | 70-89% | < 70% |
| Lead Time | <= estimado | hasta +20% | > +20% |
| Cycle Time | decreciente | estable | creciente |
| WIP | < limite | = limite | > limite |
| Tasa Bloqueo | <= 10% | 10-25% | > 25% |
| Defect Density | decreciente | estable | creciente |
| Desvio Cronograma | <= 5% | 5-10% | > 10% |
