# Build, Despliegue y Verificación — ISSUES #5, #9, #10
- **Fase**: R2 Corrección
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-03
- **Estado**: Completado

---

## Resumen Ejecutivo

Build, despliegue y verificación de los fixes para los issues #5, #9, #10 del proyecto Abax-Memory. Release v1.0.3 generado y publicado en GitHub Container Registry.

---

## Plan de Despliegue

| Componente | Versión | Artefacto |
|---|---|---|
| Backend Quarkus | 1.0.0-SNAPSHOT (fast-jar) | `backend-quarkus/target/quarkus-app/` |
| Docker Image | v1.0.3 | `ghcr.io/breisnerlopez/abax-memory:v1.0.3` |
| Docker Image (latest) | v1.0.3 | `ghcr.io/breisnerlopez/abax-memory:latest` |
| GitHub Release | v1.0.3 | https://github.com/breisnerlopez/abax-memory/releases/tag/v1.0.3 |

### Prerrequisitos
- [x] PostgreSQL 16 (host: localhost:5432, db: pmoadb, user: pmoa)
- [x] Qdrant (host: localhost:6333)
- [x] Keycloak 26.1 (host: localhost:8443, realm: abax-memory)
- [x] OpenAI API Key configurada
- [x] Usuarios de prueba: operator/test123, api/test123

---

## Resultados de Verificación

### PASO 1: Kill procesos anteriores
```bash
pkill -f quarkus-run.jar
```
**OK** ✅ — PID 3767986 terminado.

### PASO 2: Build Maven
```bash
cd backend-quarkus && mvn quarkus:build -DskipTests -q
```
**OK** ✅ — Build completado en ~30s, sin errores.

### PASO 3: Verificación de artefacto
```bash
ls target/quarkus-app/quarkus-run.jar
```
**OK** ✅ — quarkus-run.jar generado (fast-jar packaging).

### PASO 4-5: Inicio del servicio
```bash
export OPENAI_API_KEY="sk-proj-..."
export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/pmoadb"
export QUARKUS_DATASOURCE_USERNAME="pmoa"
export QUARKUS_DATASOURCE_PASSWORD="pmoa"
setsid java -jar backend-quarkus/target/quarkus-app/quarkus-run.jar &
```
**OK** ✅ — App iniciada, health check UP. Profile: prod. Flyway: schema up to date.

### PASO 6: TEST RELACIONES (#10)
**"Crear 2 memorias: segunda menciona a la primera. Verificar relations no vacío."**

1. Memoria A creada: `MEM-6b58c17e` "Jenkins Pipeline Config" (APROBADA)
2. Memoria B creada: `MEM-344e26ca` "Kubernetes Deploy con Jenkins" (APROBADA)
3. IA generó `extractedRelaciones: "Jenkins - Kubernetes"`
4. `materializeExtractedRelations()` encontró match por título → 2 relaciones creadas

**OK** ✅ — Relations count: 2, tipo RELACIONADO.

### PASO 7: TEST FILTRO LIST (#9)
**"Consumer NO ve memorias EN_REVISION en GET /api/memorias"**

1. Memoria ALTA creada con operator: `MEM-93ea4c55` state=EN_REVISION
2. GET con consumer (api): 6 memorias visibles (todas APROBADA)
3. `MEM-93ea4c55` NO aparece en resultados

**OK** ✅ — Filtro `buildRoleFilter()` funcionando en `list()`.

### PASO 8: TEST FILTRO GET (#9)
**"Consumer recibe 403 al acceder a EN_REVISION por ID"**

```bash
curl -H "Authorization: Bearer $TOKEN_CON" \
  http://localhost:8080/api/memorias/MEM-93ea4c55
→ HTTP 403 FORBIDDEN
```

**OK** ✅ — `getById()` bloquea acceso para api-consumer-only a memorias no APROBADA.

### PASO 9: TEST PRIORITY (#5)
**"Priority=INVALIDA debe devolver 400"**

```bash
curl -X POST http://localhost:8080/api/memorias \
  -d '{"criticality": "INVALIDA", ...}'
→ HTTP 400 BAD REQUEST
```

**OK** ✅ — Defense-in-depth: validación de enum Criticality.

---

## Commit y Release

### PASO 10: Commit
```
git add [fuentes modificadas] docs/
git commit -m "fix: ISSUE #5 (defense-in-depth), #9 (api-consumer filter), #10 (relations materialized)"
git push origin main
```
**OK** ✅ — Commit `3af7c06`, push a `origin/main`.

### PASO 11: Cierre de Issues
```
gh issue close 5 --repo breisnerlopez/abax-memory
gh issue close 9 --repo breisnerlopez/abax-memory
gh issue close 10 --repo breisnerlopez/abax-memory
```
**OK** ✅ — Issues #5, #9, #10 cerrados como "completed".

### PASO 12: Docker Build y Push
```
docker build -t ghcr.io/breisnerlopez/abax-memory:v1.0.3 .
docker push ghcr.io/breisnerlopez/abax-memory:v1.0.3
docker push ghcr.io/breisnerlopez/abax-memory:latest
```
**OK** ✅ — Imagen v1.0.3 y latest publicadas.
Digest: `sha256:0f9138f25229a10e751ea57593176d0083004303454c099f6af9dc0e7e2d6a40`

### PASO 13: GitHub Release
```
gh release create v1.0.3 --title "v1.0.3 - ISSUE #5 #9 #10"
```
**OK** ✅ — Release publicado: https://github.com/breisnerlopez/abax-memory/releases/tag/v1.0.3

---

## Matriz de Trazabilidad

| Issue | Descripción | Verificación | Estado |
|---|---|---|---|
| #5 | Priority validation defense-in-depth | PASO 9: INVALIDA → 400 | ✅ CERADO |
| #9 | api-consumer filter en GET/list | PASO 7: list filtrado, PASO 8: 403 en getById | ✅ CERADO |
| #10 | Relations materialized | PASO 6: 2 relaciones creadas vía IA | ✅ CERADO |

---

## Rollback

En caso de necesitar revertir:

```bash
# Revertir a v1.0.2
docker pull ghcr.io/breisnerlopez/abax-memory:v1.0.2
docker tag ghcr.io/breisnerlopez/abax-memory:v1.0.2 ghcr.io/breisnerlopez/abax-memory:latest
docker push ghcr.io/breisnerlopez/abax-memory:latest

# O revertir código
git revert 3af7c06
git push origin main
```

---

## Notas Operativas

1. **Profile**: La app se ejecuta con profile `prod` por defecto. Requiere PostgreSQL disponible.
2. **Variables de entorno requeridas**:
   - `OPENAI_API_KEY`: API key de OpenAI
   - `QUARKUS_DATASOURCE_JDBC_URL`: JDBC URL de PostgreSQL
   - `QUARKUS_DATASOURCE_USERNAME`: Usuario PostgreSQL
   - `QUARKUS_DATASOURCE_PASSWORD`: Password PostgreSQL
3. **Health Check**: `GET /q/health` verifica conexión a BD.
4. **Advertencia**: La imagen Docker expone `OPENAI_API_KEY` como ENV vacío (se debe pasar en runtime).
5. **IA**: La extracción de relaciones depende de que OpenAI genere el campo `relaciones` en la respuesta JSON. Si no lo genera, `extractedRelaciones` queda vacío y no se materializan relaciones automáticas.
