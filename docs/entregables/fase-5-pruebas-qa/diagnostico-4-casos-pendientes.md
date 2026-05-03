# Diagnostico 4 Casos Pendientes
- **Fase**: 5-Pruebas QA
- **Entregable**: Diagnostico 4 Casos Pendientes
- **Responsable**: qa-functional
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Resumen ejecutivo

Se analizaron los 4 casos QA aun marcados como **No ejecutados** contra la evidencia vigente (`mvn test` en verde, `MemoryResourceTest`, `MemoryServiceTest`, `SearchServiceTest`, reporte de ejecucion y reporte de defectos).

Conclusion: **ninguno de los 4 casos puede cerrarse todavia con la evidencia actual**. No hay defecto funcional abierto confirmado; el pendiente real es de **evidencia fresca directa del escenario exacto**.

## 2. Diagnostico puntual por caso

| ID | Precondicion | Pasos | Resultado esperado |
|---|---|---|---|
| TC-MEM-010 | Existe una memoria archivada y su identificador es conocido. | 1. Archivar una memoria.<br>2. Invocar `GET /api/memorias/{id}` sobre esa memoria.<br>3. Validar estado y metadatos. | La memoria archivada sigue visible por consulta directa y expone su estado real. |
| TC-APR-004 | Existe memoria critica pendiente; usuario sin rol revisor. | 1. Intentar aprobar o revisar con rol insuficiente.<br>2. Validar `403`.<br>3. Consultar estado final. | La operacion es denegada y el estado no cambia. |
| TC-AUD-004 | Existe memoria creada por usuario A y modificada por usuario B. | 1. Crear memoria con usuario A.<br>2. Modificar con usuario B.<br>3. Consultar auditoria/trazabilidad. | La auditoria separa creador y ultimo modificador de forma verificable. |
| TC-SRC-001 | Existen memorias indexadas relevantes para la misma consulta. | 1. Ejecutar busqueda semantica.<br>2. Revisar multiples resultados.<br>3. Validar orden relativo por score/relevancia. | Los resultados salen ordenados por relevancia. |

## 3. Analisis individual del bloqueo

| Caso | Evidencia actual revisada | Por que sigue no ejecutado | Tipo de bloqueo | ¿Puede cerrarse ya? | Accion minima para cerrarlo |
|---|---|---|---|---|---|
| TC-MEM-010 | `MemoryResourceTest` prueba archivado y exclusion de archivadas en busqueda/listados; `MemoryResource` expone `GET /api/memorias/{id}`. | La evidencia vigente demuestra archivado, pero **no muestra una corrida fresca de consulta directa por ID despues del archivado**. La cobertura es adyacente, no exacta. | **Falta de evidencia automatizada** | **No** | Ejecutar una prueba API puntual: crear memoria, archivarla y luego hacer `GET /api/memorias/{id}` validando `200`, `state=ARCHIVADA` y metadatos/trazabilidad visibles. |
| TC-APR-004 | `MemoryResource` restringe `/aprobar` y `/revision` a `memory-reviewer`/`memory-admin`; hay evidencia positiva de aprobacion/revision y `403` en otros endpoints. | Falta evidencia fresca del **mismo endpoint de aprobacion/revision** con un usuario sin rol revisor. No esta probado el rechazo RBAC del flujo exacto. | **Falta de evidencia automatizada** | **No** | Ejecutar una prueba API negativa con `memory-operator` sin rol revisor contra `/api/memorias/{id}/aprobar` o `/revision`, validar `403` y luego consultar la memoria para confirmar que sigue en `EN_REVISION` o estado equivalente. |
| TC-AUD-004 | La evidencia actual prueba `createdBy` y `lastModifiedBy`, pero en la corrida fresca ambos quedan en `operator-user`. | El escenario exacto exige **actor A creador y actor B modificador**. La suite actual no deja evidencia fresca de cambio real de identidad dentro del mismo flujo. | **Limitacion del entorno** | **No** | Ejecutar el flujo con dos identidades reales de prueba (dos JWT/tokens) o agregar una prueba dedicada capaz de cambiar de usuario entre alta y `PATCH`; luego consultar trazabilidad y validar `createdBy != lastModifiedBy`. |
| TC-SRC-001 | Hay evidencia de busqueda semantica, equivalencia semantica, filtros y exclusion de archivadas; la corrida observada devuelve un unico resultado relevante. | El caso exige validar **orden relativo**. Con un solo resultado no hay muestra ordenable. Ademas, la precondicion del caso queda corta para forzar un set multiresultado. | **Falta de datos/precondiciones** *(con inconsistencia documental menor en la precondicion)* | **No** | Sembrar al menos 2-3 memorias relevantes para la misma consulta, ejecutar la busqueda y validar orden descendente por `score`; actualizar la precondicion del caso para exigir explicitamente multiples resultados relevantes. |

## 4. Trazabilidad del diagnostico

| Requerimiento / flujo | Caso de prueba | Diagnostico | Resultado |
|---|---|---|---|
| HU-002.1.2 Consultar detalle de memoria por ID + HU-006.1.2 estados visibles | TC-MEM-010 | Falta evidencia directa post-archivado por `GET /api/memorias/{id}` | Pendiente de evidencia |
| HU-004.1.2 / HU-005.1.3 revision humana + HU-007.1.1 acceso y visibilidad | TC-APR-004 | Falta evidencia RBAC negativa sobre el endpoint exacto de aprobacion/revision | Pendiente de evidencia |
| HU-007.1.2 registrar creador y modificador + MF-10 auditoria | TC-AUD-004 | Falta corrida A/B con dos identidades distintas | Pendiente de evidencia |
| HU-005.1.1 buscar memorias semanticamente | TC-SRC-001 | Falta set multiresultado para validar orden por relevancia | Pendiente de evidencia |

## 5. Defectos / observaciones

| Severidad | Prioridad | Pasos | Evidencia |
|---|---|---|---|
| Media | Media | `TC-MEM-010`: archivar y consultar por ID. Brecha de evidencia, no defecto confirmado. | `reporte-defectos.md` -> OBS-QA-002; `MemoryResourceTest` vigente. |
| Media | Media | `TC-APR-004`: aprobar/revisar con rol insuficiente. Brecha de evidencia, no defecto confirmado. | `reporte-defectos.md` -> OBS-QA-003; `MemoryResource` y pruebas RBAC vigentes. |
| Media | Media | `TC-AUD-004`: crear con usuario A y modificar con usuario B. Brecha de evidencia, no defecto confirmado. | `reporte-defectos.md` -> OBS-QA-004; trazabilidad vigente con mismo actor. |
| Media | Media | `TC-SRC-001`: ejecutar busqueda con varios resultados relevantes. Brecha de evidencia, no defecto confirmado. | `reporte-defectos.md` -> OBS-QA-005; `SearchServiceTest` y `MemoryResourceTest` vigentes. |

## 6. Informe de ejecucion del diagnostico

| Total analizados | Ya cerrables con evidencia actual | Pendientes por evidencia | Defectos funcionales abiertos | Observacion |
|---:|---:|---:|---:|---|
| 4 | 0 | 4 | 0 | Todos los pendientes son brechas de evidencia fresca del escenario exacto. |

## 7. Recomendacion ejecutiva para cerrar Fase 5

1. **No aprobar aun Fase 5** con el baseline formal actual, porque siguen faltando 4 evidencias directas.
2. Ejecutar un **microciclo QA focalizado** solo sobre estos 4 casos, sin reabrir el ciclo completo:
   - **TC-MEM-010** y **TC-APR-004**: cerrar con pruebas API automatizadas puntuales.
   - **TC-AUD-004**: cerrar con evidencia de dos identidades reales o prueba automatizada con cambio de actor.
   - **TC-SRC-001**: cerrar con siembra controlada de multiples memorias relevantes y validacion explicita de orden por score.
3. Ajustar la documentacion de **TC-SRC-001** para que la precondicion exija multiples resultados relevantes y evitar ambiguedad futura.

Recomendacion final: **Fase 5 puede cerrarse rapidamente** si el equipo genera estas 4 evidencias frescas en una corrida dirigida. Mientras eso no ocurra, la decision QA correcta sigue siendo **No Aprobado por evidencia incompleta**, no por defecto funcional abierto.
