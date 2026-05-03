# Lecciones Aprendidas de Despliegue y Validacion - DevOps
- **Fase**: R2
- **Responsable**: project-manager
- **Fecha**: 2026-05-02
- **Estado**: Completado

---

## Resumen Ejecutivo

Durante la fase R2 del proyecto Abax-Memory, el equipo DevOps enfrento multiples incidencias recurrentes que provocaron redespliegues fallidos y retrabajo significativo. Este documento consolida los 12 patrones de falla identificados, su causa raiz, sintomas observados, solucion aplicada y el checklist preventivo correspondiente. El objetivo es que estos aprendizajes se incorporen al proceso estandar de despliegue para evitar su repeticion en fases futuras y proyectos posteriores.

---

## 1. Puerto 8080 ocupado por instancia previa

- **Problema**: El backend no arranca porque el puerto 8080 ya esta en uso por una instancia anterior no detenida correctamente.

- **Causa raiz**: El script de despliegue no incluye un paso de limpieza de procesos previos. Si el proceso anterior termino de forma anomala (kill -9, crash, o timeout del script), el puerto queda ocupado y el nuevo despliegue falla silenciosamente o con error de bind.

- **Sintomas**:
  - Error `Address already in use` o `bind failed` en los logs de arranque del backend.
  - El nuevo proceso de backend parece arrancar pero no responde en el puerto 8080.
  - `curl localhost:8080/health` retorna `Connection refused`.

- **Solucion**: Ejecutar `pkill -f` sobre el proceso del backend antes de cada despliegue. Como alternativa mas precisa, verificar el puerto con `ss -tlnp | grep 8080` y matar el proceso especifico que lo ocupa.

- **Comando/checklist preventivo**:
  ```bash
  # Opcion A - Pkill preventivo
  pkill -f "abax-memory" 2>/dev/null || true
  sleep 2

  # Opcion B - Verificacion precisa del puerto
  PORT_PID=$(ss -tlnp | grep ':8080' | awk '{print $NF}' | grep -oP 'pid=\K[0-9]+')
  if [ -n "$PORT_PID" ]; then
    echo "[WARN] Puerto 8080 ocupado por PID $PORT_PID. Matando..."
    kill -9 "$PORT_PID"
    sleep 1
  fi
  ```

---

## 2. Procesos zombies bloqueando recursos

- **Problema**: Backends anteriores no se matan correctamente y quedan como procesos zombies que bloquean el puerto, impidiendo el arranque de nuevas instancias.

- **Causa raiz**: El script de parada usa `kill` sin senal `-9` (SIGKILL) y el proceso no responde a SIGTERM. Ademas, no se verifica que el proceso realmente haya terminado antes de continuar con el despliegue.

- **Sintomas**:
  - `ps aux | grep abax-memory` muestra procesos con estado `Z` (zombie) o `D` (uninterruptible sleep).
  - El puerto sigue ocupado incluso despues de ejecutar `pkill` con la senal por defecto.
  - El sistema reporta memoria consumida por procesos que no responden.

- **Solucion**: Usar `pkill -9 -f "abax-memory"` (SIGKILL forzado) y verificar con `ps aux | grep abax-memory` que no quede ningun proceso residual antes de arrancar el nuevo backend.

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Matar con SIGKILL
  pkill -9 -f "abax-memory" 2>/dev/null || true
  sleep 2

  # Paso 2 - Verificar que no queden procesos
  REMAINING=$(ps aux | grep -v grep | grep "abax-memory" | wc -l)
  if [ "$REMAINING" -gt 0 ]; then
    echo "[ERROR] Aun quedan $REMAINING procesos abax-memory vivos."
    ps aux | grep -v grep | grep "abax-memory"
    exit 1
  fi
  echo "[OK] Todos los procesos abax-memory han sido detenidos."
  ```

---

## 3. Inconsistencia en nombre del JAR segun comando de build

- **Problema**: Segun como se compile el proyecto (`mvn package` vs `mvn quarkus:build`), el JAR generado tiene nombre distinto, lo que rompe el script de despliegue que espera un nombre fijo para ejecutar el artefacto.

- **Causa raiz**: `mvn package` genera `target/quarkus-app/quarkus-run.jar` (fast-jar), mientras que `mvn quarkus:build` puede generar `target/abax-memory-1.0.0-SNAPSHOT-runner.jar` (uber-jar) dependiendo de la configuracion. El script de despliegue no validaba cual JAR existia realmente en el target.

- **Sintomas**:
  - Error `java -jar target/abax-memory-runner.jar` → `Error: Unable to access jarfile`.
  - El pipeline de CI/CD falla en el paso de ejecucion post-build.
  - El equipo pierde tiempo debuggeando por que "no se genero el JAR" cuando en realidad si se genero, pero con otro nombre.

- **Solucion**: Estandarizar el comando de build a `mvn package -Dquarkus.package.type=uber-jar` para generar siempre el uber-jar con nombre predecible. En el script de despliegue, usar `ls target/*-runner.jar` para verificar el nombre real del JAR generado antes de ejecutarlo.

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Build estandarizado (uber-jar)
  mvn clean package -Dquarkus.package.type=uber-jar -DskipTests

  # Paso 2 - Verificar JAR generado (debe haber exactamente uno)
  JAR_COUNT=$(ls target/*-runner.jar 2>/dev/null | wc -l)
  if [ "$JAR_COUNT" -eq 0 ]; then
    echo "[ERROR] No se encontro ningun JAR runner en target/. Verificar build."
    exit 1
  fi
  if [ "$JAR_COUNT" -gt 1 ]; then
    echo "[WARN] Multiples JARs encontrados. Usando el mas reciente."
  fi
  JAR_FILE=$(ls -t target/*-runner.jar | head -1)
  echo "[OK] JAR detectado: $JAR_FILE"

  # Paso 3 - Ejecutar con el JAR detectado
  java -jar "$JAR_FILE" &
  ```

---

## 4. CDI proxy y acceso directo a campos (field access)

- **Problema**: Al acceder campos directamente en beans CDI mediante `bean.field` en lugar de `bean.getField()`, el proxy de CDI no intercepta el acceso y se obtienen valores 0 (para tipos primitivos) o `null` (para objetos), causando comportamientos erraticos en produccion.

- **Causa raiz**: CDI utiliza proxies que interceptan las llamadas a metodos (getters/setters) pero no pueden interceptar el acceso directo a campos (field access). Si el codigo accede a `obj.campo` en lugar de `obj.getCampo()`, el proxy devuelve el valor por defecto del tipo (0, null, false) porque el campo real no ha sido inicializado en el proxy.

- **Sintomas**:
  - Valores `0` o `null` donde se esperaban datos validos de configuracion.
  - Comportamiento diferente entre entorno de desarrollo (sin proxy CDI) y produccion (con proxy CDI).
  - `NullPointerException` en puntos donde los datos "deberian estar inicializados".
  - El problema aparece intermitentemente, dependiendo del scope del bean.

- **Solucion**: Usar siempre getters publicos (`getX()`) para acceder a campos de beans CDI, nunca acceso directo a campos. Configurar el IDE y las reglas de linting para marcar el acceso directo a campos en beans anotados con CDI como warning o error.

- **Comando/checklist preventivo**:
  ```bash
  # Checklist de revision de codigo para beans CDI:
  # [ ] Todos los campos son private
  # [ ] Existen getters publicos para todos los campos accedidos externamente
  # [ ] No hay field access (obj.campo) fuera de la propia clase
  # [ ] Si es necesario acceso a campo, usar @Inject directo en lugar de proxy
  ```

---

## 5. Coleccion Qdrant incompatible por parametros cambiados

- **Problema**: Si la coleccion existente en Qdrant se creo con parametros diferentes a los actuales (por ejemplo `vectorSize=0` de una version anterior del codigo), el redeploy falla al intentar usar la coleccion con los nuevos parametros de indexacion.

- **Causa raiz**: El codigo de inicializacion asume que la coleccion ya existe y es compatible. Qdrant no permite modificar parametros estructurales de una coleccion existente (como `vectorSize` o la distancia). Al cambiar estos parametros en el codigo, la coleccion vieja se vuelve incompatible.

- **Sintomas**:
  - Error de Qdrant: `Wrong input: Vector dimension error: expected dim: 1536, got 0`.
  - El backend arranca pero falla al intentar indexar o consultar documentos.
  - Los logs muestran `Collection 'abax-memory' already exists` seguido de error de dimensiones.

- **Solucion**: Eliminar la coleccion vieja antes de redesplegar si se cambiaron parametros de Qdrant. Agregar al script de despliegue una verificacion de compatibilidad de la coleccion existente, y si los parametros no coinciden, eliminarla y recrearla.

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Verificar si la coleccion existe y sus parametros
  curl -s http://localhost:6333/collections/abax-memory | jq '.result.config.params'

  # Paso 2 - Si los parametros no coinciden, eliminar la coleccion
  curl -X DELETE http://localhost:6333/collections/abax-memory

  # Paso 3 - Recrear con los parametros correctos (se hara desde el backend)
  # Alternativa: script de reset completo de Qdrant
  curl -X DELETE http://localhost:6333/collections/abax-memory && \
  echo "[OK] Coleccion eliminada. Se recreara en el arranque del backend."
  ```

---

## 6. Token de GitHub CLI expira en medio del flujo

- **Problema**: El token de autenticacion de GitHub CLI (`gh`) expira en medio del flujo de trabajo de despliegue, interrumpiendo operaciones que dependen de GitHub (push, release, Pages).

- **Causa raiz**: El token se obtuvo via variable de entorno (`GITHUB_TOKEN`) que expira al cerrar la sesion, o se genero con un scope/duracion insuficiente. No se verifico el estado de autenticacion al inicio del script de despliegue.

- **Sintomas**:
  - Error `gh auth status` muestra `not logged in` en medio del pipeline.
  - Comandos `gh release create`, `gh pr create` fallan con `HTTP 401: Bad credentials`.
  - El despliegue de GitHub Pages falla en el ultimo paso (push a `gh-pages`).

- **Solucion**: Verificar autenticacion al inicio del script con `gh auth status`. Usar `gh auth login --with-token` para persistir el token mas alla de la sesion actual. Implementar un mecanismo de reautenticacion rapido (token almacenado en variable de entorno segura con fallback a archivo).

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Verificar autenticacion al inicio del script
  if ! gh auth status >/dev/null 2>&1; then
    echo "[ERROR] GH CLI no autenticado."
    if [ -n "$GH_TOKEN" ]; then
      echo "[INFO] Intentando autenticar con GH_TOKEN..."
      echo "$GH_TOKEN" | gh auth login --with-token
    else
      echo "[FATAL] No hay GH_TOKEN disponible. Ejecuta gh auth login manualmente."
      exit 1
    fi
  fi

  # Paso 2 - Verificar scope del token (necesita repo, workflow)
  gh auth status --show-token 2>&1 | head -5
  echo "[OK] GH CLI autenticado correctamente."

  # Paso 3 - Refrescar token si fue generado con fecha de expiracion
  if [ -n "$GH_TOKEN_EXPIRY" ] && [ "$(date +%s)" -gt "$GH_TOKEN_EXPIRY" ]; then
    echo "[WARN] Token por expirar. Solicitar nuevo token."
  fi
  ```

---

## 7. Formato de vectores Qdrant v1.17.1 (5 iteraciones de prueba y error)

- **Problema**: Se requirieron 5 iteraciones para encontrar el formato correcto de envio de vectores a la API de Qdrant v1.17.1. El formato de named vectors `{"": [...]}` no funciona en esta version; el formato correcto es un array directo `[...]`.

- **Causa raiz**: La documentacion y ejemplos en linea de Qdrant muestran el formato de named vectors como el estandar, pero en la version 1.17.1 el endpoint de upsert espera vectores en formato de array directo cuando la coleccion no fue creada con named vectors explicitos. No se consulto la documentacion especifica de la version desplegada (`/openapi.json`).

- **Sintomas**:
  - Error `Validation error in body: [points[0].vector: invalid type: map, expected a sequence]`.
  - El backend falla al indexar documentos con error 422 de Qdrant.
  - Los tests de integracion pasan en una version de Qdrant y fallan en otra.
  - El payload enviado era `{"vector": {"": [0.1, 0.2, ...]}}` cuando debia ser `{"vector": [0.1, 0.2, ...]}`.

- **Solucion**: Usar array directo `[0.1, 0.2, ...]` para el campo `vector` en llamadas a la API de Qdrant v1.17.1. Documentar la version exacta de Qdrant en uso y verificar el esquema esperado consultando `GET /openapi.json` del servidor Qdrant desplegado.

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Verificar version de Qdrant
  curl -s http://localhost:6333/ | jq '.version'
  # Debe mostrar: "1.17.1" o similar

  # Paso 2 - Verificar el esquema esperado para upsert
  curl -s http://localhost:6333/openapi.json | \
    jq '.paths."/collections/{collection_name}/points".put.requestBody.content."application/json".schema'

  # Paso 3 - Formato correcto para v1.17.1 sin named vectors:
  # {
  #   "points": [
  #     {
  #       "id": 1,
  #       "vector": [0.1, 0.2, 0.3, ...],   // Array directo, NO {"": [...]}
  #       "payload": { ... }
  #     }
  #   ]
  # }

  # Paso 4 - Validar formato antes de enviar
  python3 -c "
  import json, sys
  data = json.load(sys.stdin)
  for pt in data.get('points', []):
      v = pt.get('vector')
      if isinstance(v, dict):
          print(f'ERROR: Point {pt[\"id\"]} usa named vector (dict). Debe ser array.')
          sys.exit(1)
  print('Formato de vectores validado OK')
  " < /tmp/payload.json
  ```

---

## 8. Jekyll secuestra GitHub Pages sin `.nojekyll`

- **Problema**: GitHub Pages intenta procesar el sitio con Jekyll por defecto. Sin un archivo `.nojekyll` en la raiz, GitHub Pages ignora `docs/index.html` (archivos que comienzan con underscore o punto) y no sirve el contenido estatico correctamente.

- **Causa raiz**: GitHub Pages asume que todo sitio es Jekyll a menos que se indique lo contrario. Jekyll ignora archivos y directorios que comienzan con `.` o `_` (como `_static/`, `.nojekyll` mismo si no existe aun). Esto causa que recursos CSS, JS e incluso el `index.html` no se sirvan si el build de Jekyll falla.

- **Sintomas**:
  - El sitio desplegado muestra un error 404 o pagina en blanco.
  - Los assets (`css/`, `js/`) no se cargan (404).
  - En Settings > Pages de GitHub, aparece "Your site is ready to be published at..." pero el contenido no se renderiza.
  - El build log de GitHub Pages muestra errores de Jekyll procesando archivos HTML estaticos.

- **Solucion**: Incluir siempre un archivo `.nojekyll` vacio en la raiz del directorio publicado en GitHub Pages. Esto le indica a GitHub Pages que el sitio es estatico puro y no debe ser procesado por Jekyll.

- **Comando/checklist preventivo**:
  ```bash
  # Paso 1 - Crear .nojekyll si no existe
  if [ ! -f docs/.nojekyll ]; then
    touch docs/.nojekyll
    echo "[OK] Archivo .nojekyll creado en docs/"
  else
    echo "[OK] Archivo .nojekyll ya existe."
  fi

  # Paso 2 - Verificar que se incluye en el deploy
  git add docs/.nojekyll
  git commit -m "chore: agregar .nojekyll para GitHub Pages"

  # Paso 3 - Checklist de GitHub Pages:
  # [ ] .nojekyll existe en la raiz del directorio de publicacion
  # [ ] Settings > Pages > Source esta configurado como "Deploy from a branch"
  # [ ] Branch correcto (main/gh-pages) y carpeta correcta (/docs o /root)
  # [ ] Build log en Actions no muestra errores de Jekyll
  ```

---

## 9. API key expuesta en logs de comandos

- **Problema**: El token de OpenAI aparecio en texto plano en los logs de comandos del pipeline de CI/CD y en el historial de bash, creando un riesgo de seguridad por exposicion de credenciales.

- **Causa raiz**: El token se pasaba como argumento visible en linea de comandos (ej: `--api-key sk-abc123...`) en lugar de usar una variable de entorno. Los argumentos de linea de comandos son visibles en `ps aux`, logs de CI/CD, y en `.bash_history`.

- **Sintomas**:
  - El token aparece en los logs publicos del pipeline de GitHub Actions.
  - `ps aux | grep java` muestra el token como argumento del proceso.
  - `.bash_history` contiene el token en texto plano.
  - (En el peor caso) El token podria ser extraido por cualquier persona con acceso a los logs.

- **Solucion**: Usar `export OPENAI_API_KEY=sk-...` y leer la variable de entorno en el codigo. Nunca pasar el token como argumento visible en linea de comandos. Usar GitHub Secrets para CI/CD y variables de entorno para ejecucion local.

- **Comando/checklist preventivo**:
  ```bash
  # NUNCA HACER ESTO:
  # java -jar app.jar --api-key sk-abc123...     # PELIGROSO

  # SIEMPRE HACER ESTO:
  export OPENAI_API_KEY="sk-abc123..."
  java -jar app.jar    # El backend lee de System.getenv("OPENAI_API_KEY")

  # Paso 1 - Verificar que no hay tokens en el historial
  grep -r "sk-" ~/.bash_history 2>/dev/null && echo "[WARN] Token en bash_history!"

  # Paso 2 - Para CI/CD, usar GitHub Secrets
  # Settings > Secrets and variables > Actions > New repository secret
  # Nombre: OPENAI_API_KEY
  # En el workflow:
  # env:
  #   OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}

  # Paso 3 - Verificar que el token no aparece en ps
  ps aux | grep java | grep -oP '(sk-[a-zA-Z0-9]+)' && echo "[CRITICAL] Token visible en procesos!"

  # Paso 4 - Agregar patrones al .gitignore / reglas de escaneo
  echo "sk-[a-zA-Z0-9]{20,}" >> .gitleaks.toml
  ```

---

## 10. NullPointerException por body vacio en RESTEasy

- **Problema**: Cuando el cliente envia un request POST con body vacio o `Content-Type` incorrecto, RESTEasy asigna `null` al parametro del endpoint en lugar de lanzar una excepcion de validacion. Esto causa `NullPointerException` aguas abajo en el codigo de negocio.

- **Causa raiz**: RESTEasy (implementacion JAX-RS de Quarkus) no realiza validacion automatica de `@NotNull` en el body del request si no se configura explicitamente un `ExceptionMapper` para manejar el caso de body nulo. El comportamiento por defecto es pasar `null` al metodo del recurso.

- **Sintomas**:
  - El cliente recibe `HTTP 500 Internal Server Error` con `NullPointerException` en el stack trace.
  - El mensaje de error generico no ayuda al cliente a entender que envio un body vacio.
  - El problema no se detecta en desarrollo porque el frontend siempre envia un body valido.

- **Solucion**: Agregar un `ExceptionMapper<NullPointerException>` especifico que capture los NPE causados por body nulo y los convierta en una respuesta `HTTP 400 Bad Request` con un mensaje claro: `"Request body is required but was empty or null"`.

- **Comando/checklist preventivo**:
  ```java
  // ExceptionMapper para NPE por body nulo
  @Provider
  public class NullBodyExceptionMapper implements ExceptionMapper<NullPointerException> {
      @Override
      public Response toResponse(NullPointerException exception) {
          // Verificar si el NPE es por body nulo (stack trace contiene ResourceMethodInvoker)
          for (StackTraceElement ste : exception.getStackTrace()) {
              if (ste.getClassName().contains("ResourceMethodInvoker")) {
                  return Response.status(Response.Status.BAD_REQUEST)
                      .entity(Map.of("error", "Request body is required but was empty or null"))
                      .build();
              }
          }
          // Si no es por body nulo, devolver 500 generico
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(Map.of("error", "Internal server error"))
              .build();
      }
  }
  ```

  ```bash
  # Checklist:
  # [ ] Existe ExceptionMapper para NullPointerException
  # [ ] El mapper diferencia entre NPE de body nulo y otros NPE
  # [ ] La respuesta es 400 (Bad Request), no 500
  # [ ] El mensaje indica claramente que el body es requerido
  ```

---

## 11. Docker login sin credential helper

- **Problema**: Al ejecutar `docker login`, las credenciales se almacenan en texto plano en `~/.docker/config.json` (campo `auth` codificado en base64, que es reversible). Esto representa un riesgo de seguridad si el archivo es accedido por terceros o incluido accidentalmente en un respaldo/repositorio.

- **Causa raiz**: Docker Desktop configura automaticamente un credential helper (`osxkeychain`, `wincred`, etc.), pero en entornos Linux sin Docker Desktop, el comportamiento por defecto es almacenar credenciales en el archivo `config.json`. No se configuro `docker-credential-pass` ni otro helper.

- **Sintomas**:
  - `cat ~/.docker/config.json` muestra el campo `auth` con un string base64 que decodifica a `usuario:password`.
  - Auditoria de seguridad detecta credenciales en texto plano en el filesystem.
  - Riesgo de exposicion si el archivo se incluye en backups o se comparte inadvertidamente.

- **Solucion**: Configurar `docker-credential-pass` en Linux para almacenar credenciales en el keyring del sistema (`pass`). Como alternativa en entornos CI/CD, usar variables de entorno temporales (`DOCKER_USERNAME`, `DOCKER_PASSWORD`) con `docker login --password-stdin` para evitar persistencia en disco.

- **Comando/checklist preventivo**:
  ```bash
  # Opcion A - Configurar credential helper (recomendado para entornos locales)
  sudo apt-get install -y pass docker-credential-pass 2>/dev/null
  # Agregar al ~/.docker/config.json:
  # { "credsStore": "pass" }
  docker login

  # Opcion B - Login sin persistencia (recomendado para CI/CD)
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

  # Paso 1 - Verificar que no hay credenciales en texto plano
  if grep -q '"auth":' ~/.docker/config.json 2>/dev/null; then
    echo "[WARN] Credenciales Docker en texto plano en ~/.docker/config.json"
    echo "[INFO] Decodificando para verificacion..."
    cat ~/.docker/config.json | jq -r '.auths[].auth' | base64 -d
  fi

  # Paso 2 - Limpiar credenciales existentes
  docker logout
  rm -f ~/.docker/config.json

  # Paso 3 - Checklist post-deploy
  # [ ] ~/.docker/config.json no contiene campo "auth" (solo "credsStore")
  # [ ] docker-credential-pass esta instalado y configurado
  # [ ] Las variables de entorno de credenciales no persisten post-deploy
  ```

---

## 12. `gh auth login --with-token` vs variable de entorno

- **Problema**: Usar el token de GitHub como variable de entorno (`GITHUB_TOKEN` o `GH_TOKEN`) hace que la autenticacion expire al cerrar la sesion de terminal. Al abrir una nueva sesion, los comandos `gh` fallan con error de autenticacion.

- **Causa raiz**: La variable de entorno se establece en la sesion actual de shell pero no persiste entre sesiones. `gh auth login --with-token` escribe el token en `~/.config/gh/hosts.yml`, lo que garantiza persistencia independientemente de la sesion.

- **Sintomas**:
  - `gh auth status` muestra `not logged into github.com` al abrir una nueva terminal.
  - Comandos `gh` que funcionaban en la sesion anterior ahora fallan con `401`.
  - El equipo pierde tiempo reautenticando repetidamente.
  - Scripts automatizados que dependen de `gh` fallan si la sesion se cerro.

- **Solucion**: Usar `gh auth login --with-token` para persistir el token en el archivo de configuracion de `gh` (`~/.config/gh/hosts.yml`). Esto asegura que la autenticacion sobreviva al cierre de sesion y este disponible para scripts y sesiones futuras.

- **Comando/checklist preventivo**:
  ```bash
  # En lugar de esto (NO PERSISTE):
  export GH_TOKEN="ghp_abc123..."

  # Usar esto (SI PERSISTE):
  echo "$GH_TOKEN_VALUE" | gh auth login --with-token

  # O interactivamente:
  gh auth login --with-token < /path/to/token-file

  # Paso 1 - Verificar autenticacion persistente
  gh auth status
  # Debe mostrar: "Logged in to github.com as <usuario>"

  # Paso 2 - Verificar archivo de hosts
  cat ~/.config/gh/hosts.yml
  # Debe contener la entrada de github.com con el token

  # Paso 3 - Verificar que funciona en nueva sesion
  bash -c 'gh auth status'
  # Debe mostrar autenticado sin necesidad de exportar variables

  # Paso 4 - Limpiar variables de entorno redundantes
  unset GH_TOKEN
  unset GITHUB_TOKEN
  # La autenticacion debe seguir funcionando via archivo hosts.yml
  ```

---

## Checklist Consolidada de Pre-deploy

Ejecutar este checklist antes de cada despliegue para prevenir los 12 problemas documentados:

```bash
#!/bin/bash
# checklist-pre-deploy.sh - Ejecutar antes de cada despliegue
set -e

echo "============================================"
echo "CHECKLIST PRE-DEPLOY - Abax-Memory"
echo "============================================"

# 1. Limpiar procesos anteriores
echo "[1/12] Limpiando procesos anteriores..."
pkill -9 -f "abax-memory" 2>/dev/null || true
sleep 2
REMAINING=$(ps aux | grep -v grep | grep "abax-memory" | wc -l)
[ "$REMAINING" -eq 0 ] || { echo "ERROR: Procesos residuales"; exit 1; }

# 2. Verificar puerto 8080
echo "[2/12] Verificando puerto 8080..."
PORT_PID=$(ss -tlnp | grep ':8080' | grep -oP 'pid=\K[0-9]+' || true)
[ -z "$PORT_PID" ] || { echo "ERROR: Puerto 8080 ocupado por PID $PORT_PID"; exit 1; }

# 3. Build estandarizado
echo "[3/12] Compilando..."
mvn clean package -Dquarkus.package.type=uber-jar -DskipTests
JAR_FILE=$(ls -t target/*-runner.jar 2>/dev/null | head -1)
[ -n "$JAR_FILE" ] || { echo "ERROR: No se genero JAR"; exit 1; }
echo "JAR: $JAR_FILE"

# 4. Verificar GH auth
echo "[4/12] Verificando GH auth..."
gh auth status >/dev/null 2>&1 || {
  echo "Reautenticando GH..."
  [ -n "$GH_TOKEN_VALUE" ] && echo "$GH_TOKEN_VALUE" | gh auth login --with-token
}

# 5. Verificar .nojekyll
echo "[5/12] Verificando .nojekyll..."
[ -f docs/.nojekyll ] || touch docs/.nojekyll

# 6. Verificar credenciales Docker
echo "[6/12] Verificando credenciales Docker..."
docker logout 2>/dev/null || true
[ -f ~/.docker/config.json ] && grep -q '"auth":' ~/.docker/config.json && {
  echo "WARN: Credenciales Docker en texto plano. Usar credential helper."
}

# 7. Verificar token OpenAI
echo "[7/12] Verificando token OpenAI no expuesto..."
ps aux | grep -v grep | grep -oP 'sk-[a-zA-Z0-9]{20,}' && {
  echo "CRITICAL: Token OpenAI visible en procesos!"
}

# 8. Verificar coleccion Qdrant
echo "[8/12] Verificando Qdrant..."
curl -s http://localhost:6333/collections/abax-memory >/dev/null 2>&1 && {
  echo "WARN: Coleccion Qdrant existe. Verificar compatibilidad de parametros."
}

echo "============================================"
echo "CHECKLIST COMPLETADO - OK para desplegar"
echo "============================================"
```

---

## Resumen de Impacto

| # | Problema | Frecuencia | Severidad | Tiempo perdido estimado |
|---|----------|------------|-----------|------------------------|
| 1 | Puerto 8080 ocupado | Alta | Alta | 15-30 min |
| 2 | Procesos zombies | Media | Alta | 20-45 min |
| 3 | Nombre de JAR inconsistente | Alta | Media | 15-30 min |
| 4 | CDI proxy field access | Baja | Critica | 2-4 horas |
| 5 | Coleccion Qdrant incompatible | Media | Alta | 30-60 min |
| 6 | Token gh CLI expira | Alta | Media | 10-20 min |
| 7 | Formato vectores Qdrant | Unica (5 iteraciones) | Alta | 3-5 horas |
| 8 | Jekyll secuestra Pages | Alta | Alta | 15-30 min |
| 9 | API key en logs | Media | Critica | Riesgo de seguridad |
| 10 | NPE por body vacio | Baja | Media | 1-2 horas |
| 11 | Docker creds texto plano | Unica | Alta | Riesgo de seguridad |
| 12 | gh auth no persiste | Alta | Media | 5-10 min |

**Tiempo total estimado perdido por estos problemas**: 12-18 horas en la fase R2.
