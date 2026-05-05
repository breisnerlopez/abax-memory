# Plan de Rollback — Abax-Memory v2.0.0

- **Fase**: 7 — Despliegue
- **Responsable**: project-manager
- **Fecha**: 2026-05-04
- **Estado**: Completado
- **Ejecutor del rollback**: devops
- **Referencia al plan de despliegue**: `00-plan-despliegue.md` (Seccion 8)

---

## 1. Objetivo

Este documento define el procedimiento exacto de rollback para revertir el despliegue de Abax-Memory v2.0.0 a la version anterior (v1.0.0) en caso de que se materialice alguna de las condiciones de fallo definidas en la Seccion 4. El plan esta diseniado para un **RTO ≤ 5 minutos** y es **bloqueante**: ningun go-live se autoriza sin que este plan este revisado y aprobado.

---

## 2. Comando Exacto de Rollback

Para entorno Docker Compose (localhost), el procedimiento de rollback rapido se ejecuta con los siguientes comandos. Si la imagen v1.0.0 esta disponible en cache local, se usa `docker tag` para evitar descarga. Si no, se hace `docker pull` desde GHCR.

### Opcion A — Rollback rapido con imagen en cache local (recomendado, ≤ 2 min)

```bash
# 1. Detener stack v2.0.0
docker compose down

# 2. Etiquetar imagen v1.0.0 como la activa (sobrescribe el tag v2.0.0 local)
docker tag ghcr.io/breisnerlopez/abax-memory:v1.0.0 ghcr.io/breisnerlopez/abax-memory:v2.0.0

# 3. Levantar stack con la imagen etiquetada
docker compose up -d

# 4. Verificar health
curl -s http://localhost:8080/q/health | jq .status
# Esperado: "UP"
```

### Opcion B — Rollback con pull desde GHCR (si la imagen v1.0.0 no esta en cache, ≤ 5 min)

```bash
# 1. Detener stack v2.0.0
docker compose down

# 2. Pull de imagen v1.0.0 desde GHCR
docker pull ghcr.io/breisnerlopez/abax-memory:v1.0.0

# 3. Etiquetar y levantar
docker tag ghcr.io/breisnerlopez/abax-memory:v1.0.0 ghcr.io/breisnerlopez/abax-memory:v2.0.0
docker compose up -d

# 4. Verificar health
curl -s http://localhost:8080/q/health | jq .status
```

### Opcion C — Rollback con `docker run` directo (contingencia si docker-compose.yml no esta disponible)

```bash
# 1. Detener contenedor v2.0.0
docker stop abax-memory-backend && docker rm abax-memory-backend

# 2. Ejecutar v1.0.0 con las mismas variables de entorno
docker run -d \
  --name abax-memory-backend \
  --network abax-memory_default \
  -p 8080:8080 \
  --env-file .env \
  ghcr.io/breisnerlopez/abax-memory:v1.0.0

# 3. Verificar health
curl -s http://localhost:8080/q/health | jq .status
```

---

## 3. Procedimiento — 5 Pasos

| Paso | Accion | Responsable | Duracion estimada | Verificacion |
|---|---|---|---|---|
| **P1** | **Detener v2.0.0**: ejecutar `docker compose down`. Verificar que todos los contenedores del stack (backend, postgres, qdrant, keycloak) estan detenidos con `docker compose ps`. | devops | 30 s | `docker compose ps` muestra estado `exited` o sin contenedores corriendo |
| **P2** | **Verificar v1 disponible**: confirmar que la imagen `ghcr.io/breisnerlopez/abax-memory:v1.0.0` existe localmente (`docker images`) o en GHCR (`docker pull --dry-run`). Si no esta disponible, abortar rollback y escalar. | devops | 30 s | `docker images \| grep abax-memory` muestra v1.0.0 |
| **P3** | **Desplegar v1.0.0**: ejecutar rollback segun Opcion A (cache local) o B (pull desde GHCR). Los volumenes persistentes (`postgres_data`, `qdrant_data`) no se eliminan. | devops | 2-3 min | Contenedores UP en `docker compose ps` |
| **P4** | **Verificar post-rollback**: ejecutar checklist completo de la Seccion 5. Si algun item falla, escalar inmediatamente y NO declarar rollback exitoso. | devops | 1-2 min | Todos los items del checklist en ✅ |
| **P5** | **Notificar**: informar al sponsor que el rollback se completo, indicando version restaurada (v1.0.0), hora de inicio y fin, y resultado de verificaciones. Si se detectaron anomalias en datos, notificarlas explicitamente. | devops | 1 min | Mensaje enviado al sponsor por el canal acordado |

**Tiempo total estimado**: ≤ 5 minutos (RTO).

---

## 4. Triggers de Rollback — Condiciones que Disparan Rollback

El rollback debe ejecutarse **inmediatamente** si cualquiera de las siguientes condiciones se detecta dentro de los primeros **15 minutos** post-deploy de v2.0.0:

| ID | Condicion | Umbral | Como se detecta | Severidad |
|---|---|---|---|---|
| T-01 | **Error rate elevado** | > 5% de requests retornan HTTP 5xx en ventana de 1 minuto | `/q/metrics` → `http_server_requests_seconds_count{status="500",...}` | Critico |
| T-02 | **Latencia p95 excedida** | p95 > 2 segundos en `POST /api/v2/search/semantic` o `POST /api/v2/memories` | `/q/metrics` → `http_server_requests_seconds{quantile="0.95",...}` | Critico |
| T-03 | **Health check en DOWN** | `/q/health` retorna `{"status": "DOWN"}` o no responde | `curl -s http://localhost:8080/q/health \| jq .status` | Critico |
| T-04 | **Readiness check en DOWN** | `/q/health/ready` no retorna `UP` (indica falla en BD, Qdrant u OpenAI) | `curl -s http://localhost:8080/q/health/ready \| jq .status` | Critico |
| T-05 | **Busqueda semantica sin resultados** | `POST /api/v2/search/semantic` retorna 0 resultados para query que en UAT retornaba > 0 | Smoke test C-05 del plan de despliegue | Alto |
| T-06 | **Error fatal en logs** | Cualquier log con nivel `FATAL` o `ERROR` no esperado en los primeros 2 minutos | `docker compose logs --tail=50 backend \| grep -E "FATAL\|ERROR"` | Alto |

> **Regla de decision**: Un solo trigger de severidad **Critico** (T-01 a T-04) → rollback inmediato. Dos o mas triggers de severidad **Alto** (T-05, T-06) simultaneos → rollback inmediato. Un solo trigger Alto → evaluar con sponsor antes de decidir.

---

## 5. Verificacion Post-Rollback — Checklist

Ejecutar en orden. Si un item falla, **no** declarar el rollback como exitoso.

| # | Item | Comando / Accion | Resultado esperado | Estado |
|---|---|---|---|---|
| VR-01 | Health check del backend | `curl -s http://localhost:8080/q/health \| jq .status` | `"UP"` | ⬜ |
| VR-02 | Readiness check (BD + Qdrant + OpenAI) | `curl -s http://localhost:8080/q/health/ready \| jq .status` | `"UP"` | ⬜ |
| VR-03 | PostgreSQL accesible | `docker compose exec postgres pg_isready` | `accepting connections` | ⬜ |
| VR-04 | Qdrant healthy | `curl -s http://localhost:6333/healthz` | `healthz check passed` | ⬜ |
| VR-05 | CRUD de memorias funcional | `POST /api/v1/memories` → 201, `GET /api/v1/memories/{id}` → 200 | Respuesta HTTP esperada | ⬜ |
| VR-06 | Sin errores en logs (ultimos 2 min) | `docker compose logs --tail=30 backend \| grep -cE "FATAL\|ERROR"` | `0` | ⬜ |
| VR-07 | Volumenes persistentes conservados | `docker compose exec postgres psql -U pmoa -d pmoadb -c "SELECT count(*) FROM memories;"` | El count debe coincidir con el esperado pre-deploy | ⬜ |

---

## 6. Roles y Responsabilidades

| Rol | Responsabilidad durante rollback |
|---|---|
| **devops** (ejecutor) | Ejecutar los 5 pasos del procedimiento (Seccion 3). Verificar checklist (Seccion 5). Notificar al sponsor (Paso 5). |
| **project-manager** (supervisor) | Autorizar el inicio del rollback ante triggers. Verificar que el RTO se cumple. Escalar si el rollback falla. |
| **sponsor** (aprobador) | Recibir notificacion de inicio y finalizacion del rollback. Confirmar recepcion. Decidir siguiente paso (investigar causa raiz, re-planificar go-live). |

---

## 7. Escalamiento

Si el rollback **falla** (alguna verificacion VR-01 a VR-07 no pasa):

1. **No reintentar automaticamente**. Detener y diagnosticar.
2. devops escala al project-manager en un maximo de 5 minutos.
3. project-manager escala al developer-backend si se requiere diagnostico tecnico profundo.
4. Si PostgreSQL o Qdrant estan corruptos (VR-07 falla), ejecutar restore desde backup (ver `00-plan-despliegue.md`, Seccion 9).
5. El sponsor debe ser notificado del fallo de rollback con:
   - Que fallo (item VR especifico)
   - Que se esta haciendo para resolverlo
   - ETA estimado de resolucion

---

## 8. Prueba de Rollback Pre-Go-Live

> **Recomendacion**: Ejecutar una simulacion de rollback (Opcion A) **antes** del go-live para validar:
> - Que la imagen v1.0.0 esta disponible en cache local o en GHCR.
> - Que el procedimiento completo toma ≤ 5 minutos.
> - Que la verificacion post-rollback (VR-01 a VR-07) pasa sin errores.

Registrar el resultado aqui:

| Fecha de simulacion | Resultado | Duracion real | Observaciones |
|---|---|---|---|
| `___________` | ⬜ Exitoso / ⬜ Fallo | `___` min |  |

---

## 9. Aprobacion

- [ ] devops confirma que los comandos de rollback son ejecutables en el entorno local.
- [ ] project-manager aprueba los triggers de rollback y el RTO de 5 minutos.
- [ ] sponsor comprende que el rollback restaura v1.0.0 (sin busqueda semantica funcional) y acepta el riesgo.
- [ ] Simulacion de rollback pre-go-live completada (Seccion 8).

---

## Glosario

- **RTO**: Recovery Time Objective — tiempo maximo aceptable para restaurar el servicio tras un fallo (≤ 5 min).
- **GHCR**: GitHub Container Registry — repositorio de imagenes Docker del proyecto.
- **p95**: Percentil 95 — el 95% de las requests se completan en menos de este tiempo.
- **Smoke test**: Prueba superficial post-deploy que verifica que las funcionalidades criticas no estan rotas.
