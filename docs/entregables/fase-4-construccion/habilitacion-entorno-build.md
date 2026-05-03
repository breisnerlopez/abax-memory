# Habilitacion Entorno Build
- **Fase**: 4-Construccion
- **Entregable**: Habilitacion Entorno Build
- **Responsable**: devops
- **Fecha**: 2026-05-01
- **Estado**: Completado
---

## Resumen ejecutivo

Se tomo el estado actual del workspace posterior a las correcciones QA en `backend-quarkus/` y se reejecutaron las validaciones tecnicas solicitadas.

- `mvn test`: **PASA**
- `mvn clean verify`: **PASA**
- Resultado vigente: **workspace validado para build y tests locales**

## Evidencia de entorno

Entorno observado durante la validacion:

```text
$ java -version
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10+7-Ubuntu-124.04)
OpenJDK 64-Bit Server VM (build 21.0.10+7-Ubuntu-124.04, mixed mode, sharing)
```

```text
$ mvn -version
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java version: 21.0.10, vendor: Ubuntu, runtime: /usr/lib/jvm/java-21-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.8.0-58-generic", arch: "amd64", family: "unix"
```

## Validacion ejecutada

### 1) `mvn test`

Comando ejecutado:

```bash
mvn test
```

Evidencia relevante:

```text
[INFO] Results:

[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  56.219 s
[INFO] Finished at: 2026-05-02T07:02:48-05:00
```

Observaciones vigentes no bloqueantes:

- Warning de Quarkus: `quarkus.log.console.json` no reconocido.
- Warning de Flyway: H2 `2.3.230` es mas nuevo que la version validada por la libreria.
- Logs `ERROR` de `ProcessingWorkerServiceTest` y `NoSuchElementException` corresponden a escenarios negativos esperados; la suite finalizo con `Failures: 0` y `Errors: 0`.

Estado: **PASA**

### 2) `mvn clean verify`

Comando ejecutado:

```bash
mvn clean verify
```

Evidencia relevante:

```text
[INFO] Results:

[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0

[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ abax-memory-backend ---
[INFO] Building jar: /root/proyectos-personales/Abax-Memory/backend-quarkus/target/abax-memory-backend-1.0.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  35.877 s
[INFO] Finished at: 2026-05-02T07:03:30-05:00
```

Estado: **PASA**

## Estado final posterior a correcciones

| Verificacion | Resultado | Evidencia |
|---|---|---|
| Java disponible | PASA | `openjdk 21.0.10` |
| Maven disponible | PASA | `Apache Maven 3.8.7` |
| `mvn test` | PASA | `26` tests, `0` failures, `0` errors, `0` skipped |
| `mvn clean verify` | PASA | `BUILD SUCCESS` y jar generado en `target/` |

## Conclusion

No se identifican fallos bloqueantes vigentes en el estado actual del backend.
La evidencia de build/test queda actualizada con validacion exitosa posterior a correcciones QA.
