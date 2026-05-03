# Pruebas Unitarias
- **Fase**: 4-Construccion
- **Entregable**: Pruebas Unitarias
- **Responsable**: developer-backend
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## 1. Objetivo de la iteracion correctiva

Ajustar y ampliar la baseline de pruebas backend para respaldar los defectos corregidos por QA, priorizando cobertura automatizada sobre flujos fallidos o bloqueados en validaciones, seguridad de endpoints, filtros estructurados y lifecycle de `processing_jobs`.

## 2. Insumos usados

- `docs/entregables/fase-4-construccion/reporte-revision-codigo.md`
- codigo fuente y tests en `backend-quarkus/`
- configuracion de test Quarkus/H2/Flyway del workspace actual

## 3. Resultado de la correccion

La baseline queda en **28 pruebas automatizadas**:

- `MemoryServiceTest`: 6
- `SearchServiceTest`: 5
- `ProcessingWorkerServiceTest`: 4
- `ProcessingJobServiceTest`: 2
- `MarkdownCanonicalServiceTest`: 1
- `MemoryResourceTest` (`@QuarkusTest`): 10

## 4. Cobertura agregada o ajustada

### 4.1 Endpoints y seguridad

Archivo: `backend-quarkus/src/test/java/com/btl/administrador/api/resource/MemoryResourceTest.java`

Cobertura relevante:

- `createManual_invalidPayload_returnsValidationErrorWithCorrelationId`
- `list_invalidStateFilter_returnsControlledError`
- `semanticSearch_invalidTopK_returnsControlledError`
- `createManualMemory_withoutToken_returnsUnauthorized`
- `createManualMemory_withWrongRole_returnsForbidden`
- `createCriticalMemory_thenApprove_returnsApprovedContract`

Esto deja evidencia automatizada de:

- rechazo controlado por payload invalido;
- propagacion de `X-Correlation-Id`;
- enforcement de `401` y `403` por seguridad;
- flujo de memoria critica `EN_REVISION -> APROBADA` con rol revisor.

### 4.2 Filtros estructurados de busqueda

Archivo: `backend-quarkus/src/test/java/com/btl/administrador/api/service/SearchServiceTest.java`

Cobertura agregada:

- `search_invalidStructuredStateFilter_throwsValidationError`

Valida que filtros estructurados invalidos fallen con `INVALID_FILTER` antes de consultar resultados, alineando la suite con un flujo bloqueado de QA.

### 4.3 Lifecycle de `processing_jobs`

Archivos:

- `backend-quarkus/src/test/java/com/btl/administrador/api/service/ProcessingJobServiceTest.java`
- `backend-quarkus/src/test/java/com/btl/administrador/api/service/ProcessingWorkerServiceTest.java`

Cobertura mantenida y verificada:

- deduplicacion por `memoryId + versionId + jobType`;
- claim de jobs y paso a `IN_PROGRESS`;
- reintentos, `retryCount`, `lastError`, `nextAttemptAt`;
- paso a `COMPLETED` y `FAILED`;
- indexacion exitosa y fallida;
- version inexistente;
- job no soportado sin efectos colaterales funcionales.

### 4.4 Regresion funcional adyacente

Se mantiene cobertura sobre:

- creacion manual y desde caso;
- metadata obligatoria;
- aprobacion de memorias en revision;
- exclusion de archivadas en busqueda;
- relaciones entre memorias;
- auditoria de eventos;
- render canonico Markdown.

## 5. Archivos actualizados

- `backend-quarkus/src/test/java/com/btl/administrador/api/resource/MemoryResourceTest.java`
- `backend-quarkus/src/test/java/com/btl/administrador/api/service/SearchServiceTest.java`
- `docs/entregables/fase-4-construccion/pruebas-unitarias.md`

## 6. Ejecucion y evidencia

Entorno validado:

- Java: `openjdk 21.0.10`
- Maven: `3.8.7`

Comandos ejecutados desde `backend-quarkus/`:

```bash
mvn test -Dtest=SearchServiceTest,MemoryResourceTest
mvn test
mvn clean verify
```

Resultados observados:

- suite focalizada: **15 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**
- suite completa: **28 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**
- verificacion extendida: **28 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**

Warnings no bloqueantes:

- `quarkus.log.console.json` no reconocido en el perfil actual;
- warning informativo de compatibilidad Flyway/H2;
- logs `ERROR` esperados en `ProcessingWorkerServiceTest` por escenarios negativos controlados de retry/falla.

## 7. Comandos reproducibles

```bash
mvn test
mvn clean verify
```

Ejecucion focalizada:

```bash
mvn test -Dtest=MemoryResourceTest
mvn test -Dtest=SearchServiceTest
```

## 8. Brechas fuera de alcance de esta iteracion

- la validacion de seguridad queda cubierta con `@TestSecurity`, pero no sustituye una prueba OIDC end-to-end contra proveedor real;
- la integracion automatizada corre sobre H2 + Flyway en test, no sobre un ambiente formal PostgreSQL de certificacion;
- no se cubren escenarios de concurrencia distribuida de infraestructura real.

## 9. Estado final

Entregable actualizado con evidencia ejecutable y trazable. La suite backend queda alineada con las correcciones QA y en verde con **28 pruebas automatizadas**.
