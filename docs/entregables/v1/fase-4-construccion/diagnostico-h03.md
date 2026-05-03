# Diagnostico H-03
- **Fase**: 4-Construccion
- **Entregable**: Diagnostico H-03
- **Responsable**: tech-lead
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Resumen ejecutivo

H-03 sigue abierto por una **combinacion de problemas de herramientas/build y pruebas**, no por una falla funcional ya demostrada en la logica productiva.

Estado real luego del diagnostico:

1. **El problema de entorno original ya no es el bloqueo principal**: se instalaron **JDK 21** y **Maven 3.8.7** en el entorno para eliminar la limitacion de ejecucion.
2. **El build continua bloqueado** porque `backend-quarkus/pom.xml` declara `org.assertj:assertj-core` **sin version** (`pom.xml:96-99`). Por eso `mvn test` falla antes de compilar tests.
3. **Ademas existe una inconsistencia real en la suite**: `ProcessingJobServiceTest` invoca `markInProgress(...)` (`src/test/.../ProcessingJobServiceTest.java:30`), pero `ProcessingJobService` no expone ese metodo (`src/main/.../ProcessingJobService.java:29-85`).

Conclusión: **H-03 es una combinacion de problema de pruebas + problema de herramientas/configuracion de build**. La limitacion de entorno existio, pero ya no explica por si sola el gate pendiente.

## 2. Evidencia levantada

### 2.1 Entorno

- `java -version` => disponible despues de instalar JDK 21
- `mvn -version` => disponible despues de instalar Maven 3.8.7

### 2.2 Ejecucion reproducible actual

Comando ejecutado desde `backend-quarkus/`:

```bash
mvn test
```

Resultado observado:

```text
[ERROR] 'dependencies.dependency.version' for org.assertj:assertj-core:jar is missing. @ line 96, column 21
```

Esto confirma que hoy la suite **ni siquiera llega a la fase de compilacion de tests**; el primer bloqueo real es de build/configuracion.

### 2.3 Inconsistencia estatica de suite

- Test: `backend-quarkus/src/test/java/com/btl/administrador/api/service/ProcessingJobServiceTest.java:30`
- Servicio: `backend-quarkus/src/main/java/com/btl/administrador/api/service/ProcessingJobService.java:29-85`

El test llama:

```java
support.processingJobService.markInProgress(job);
```

Pero el servicio solo expone:

- `createIfAbsent(...)`
- `pendingJobs()`
- `claimPendingJobs(...)`
- `markCompleted(...)`
- `markFailed(...)`

No existe `markInProgress(...)`.

## 3. Causa raiz real de H-03

### 3.1 Problema de codigo

**No hay evidencia suficiente de un bug funcional en la logica productiva asociado a H-03.**

La ausencia de `markInProgress(...)` en `ProcessingJobService` no es, por si sola, un defecto de negocio. El flujo actual sugiere que la transicion a `IN_PROGRESS` fue movida al mecanismo de claim:

- `ProcessingJobService.claimPendingJobs(...)`
- `ProcessingJobRepository.claimPendingJobs(...)`
- implementacion in-memory/postgres que marcan el job como `IN_PROGRESS` al reclamarlo.

Por lo tanto, el desalineamiento visible apunta mas a una **suite desactualizada** que a una falta funcional del servicio.

### 3.2 Problema de pruebas

**Si, existe.**

`ProcessingJobServiceTest` quedo acoplado a una API anterior o a una expectativa vieja del lifecycle. El test sigue esperando una transicion directa `markInProgress(...)`, mientras que la implementacion actual hace esa transicion mediante `claimPendingJobs(...)` en repositorio/worker.

Raiz probable: **refactor del lifecycle no reflejado en tests**.

### 3.3 Problema de entorno / herramientas

**Si, existe, pero cambió de naturaleza.**

- Antes: faltaban herramientas base (`java`, `mvn`) y eso impedía verificar.
- Ahora: esas herramientas ya fueron instaladas, pero surgio el bloqueo real de build: **POM invalido por dependencia sin version**.

Entonces, el problema de entorno inicial ya fue removido, pero persiste un **problema de tooling/configuracion del proyecto** que sigue impidiendo la evidencia ejecutable.

### 3.4 Diagnostico consolidado

**H-03 = combinacion de:**

1. **Problema de pruebas**: test desalineado (`markInProgress(...)` inexistente).
2. **Problema de herramientas/configuracion**: `pom.xml` no construye por `assertj-core` sin version.
3. **Problema historico de entorno**: ya mitigado al instalar JDK/Maven, por lo que ya no es la excusa principal.

## 4. Que bloquea hoy la aprobacion tecnica

La aprobacion tecnica esta bloqueada hoy por dos razones objetivas y verificables:

### Bloqueo A — El build no arranca de forma valida

`mvn test` falla por definicion de dependencias en `pom.xml`:

- archivo: `backend-quarkus/pom.xml:96-99`
- causa: `org.assertj:assertj-core` sin version declarada ni gestionada por el BOM efectivo

Mientras eso no se corrija, **no existe evidencia reproducible de compilacion ni de ejecucion de suite**.

### Bloqueo B — La suite contiene al menos un test incoherente con la API actual

Aunque el POM se corrigiera, la revision estatica sigue mostrando que `ProcessingJobServiceTest` llama una API inexistente. Eso significa que la suite no es aprobable como evidencia tecnica hasta alinearla con la implementacion real.

## 5. Correccion minima requerida para cerrar la inconsistencia de `markInProgress(...)`

La correccion minima recomendada es **ajustar el test, no ampliar la API productiva**.

### Recomendacion

Reemplazar en `ProcessingJobServiceTest` la llamada:

```java
support.processingJobService.markInProgress(job);
```

por una transicion consistente con el flujo actual, por ejemplo:

```java
var claimed = support.processingJobService.claimPendingJobs("worker-test", 1);
assertThat(claimed).singleElement().isSameAs(job);
```

y luego mantener las validaciones sobre:

- `job.status == IN_PROGRESS`
- `job.updatedAt` actualizado
- opcionalmente `job.lockedBy` y `job.lockedAt`

### Justificacion tecnica

- La implementacion vigente marca `IN_PROGRESS` al **reclamar** jobs, no mediante un metodo publico independiente.
- Reintroducir `markInProgress(...)` solo para satisfacer el test agregaria una API productiva artificial y aumentaria superficie sin necesidad funcional demostrada.

## 6. Evidencia tecnica faltante para aprobar completamente despues de esa correccion

Despues de corregir:

1. **el POM (`assertj-core` con version valida o dependencia gestionada correctamente)**
2. **el test `ProcessingJobServiceTest` alineado con `claimPendingJobs(...)`**

seguira faltando la evidencia formal de aprobacion:

### Evidencia minima requerida

1. **Build reproducible exitoso**
   - `mvn test`
   - exit code 0
   - salida completa adjunta

2. **Verificacion extendida recomendada**
   - `mvn verify`
   - salida completa adjunta

3. **Trazabilidad de entorno**
   - version de Java
   - version de Maven
   - fecha/hora de ejecucion
   - directorio/proyecto desde donde se corrio

4. **Artefacto o log adjunto a la entrega/PR**
   - no alcanza con decir “los tests pasan”
   - debe adjuntarse la salida reproducible del comando

## 7. Plan de cierre accionable

### Paso 1 — Corregir build
- Ajustar `backend-quarkus/pom.xml` para resolver `assertj-core` con version valida.

### Paso 2 — Corregir suite
- Actualizar `backend-quarkus/src/test/java/com/btl/administrador/api/service/ProcessingJobServiceTest.java` para usar `claimPendingJobs(...)` en lugar de `markInProgress(...)`.

### Paso 3 — Reejecutar evidencia
- Ejecutar `mvn test`
- Ejecutar `mvn verify` (recomendado)
- Adjuntar salida completa

### Paso 4 — Cierre de gate
- Si build y suite quedan en verde con evidencia adjunta, **H-03 puede cerrarse**.

## 8. Resumen final para decision

- **No aprobado hoy**.
- **Motivo real**: H-03 no es solo “falta de entorno”; hoy esta bloqueado por **POM invalido + suite desalineada**.
- **Correccion minima funcional**: alinear `ProcessingJobServiceTest` con `claimPendingJobs(...)`.
- **Correccion minima operativa adicional**: arreglar `assertj-core` en `pom.xml`.
- **Para aprobar completamente**: evidencia reproducible de `mvn test`/`mvn verify` en verde, con logs adjuntos.
