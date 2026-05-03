# Acta de Aprobacion — Gate Fase 7: Despliegue
- **Fase**: 7-Despliegue
- **Responsable**: project-manager
- **Fecha**: 2026-05-02
- **Estado**: APROBADA CON CONDICIONES
- **Version**: v1.0
---

## 1. Decision del Gate

El Project Manager, en uso de sus atribuciones y habiendo revisado la documentacion completa de Fase 7 — Despliegue, declara:

**El Gate de Fase 7 queda APROBADO CON CONDICIONES.**

El proyecto esta habilitado para avanzar a Fase 8 — Estabilizacion una vez que las 10 condiciones enunciadas en este acta hayan sido verificadas y cumplidas satisfactoriamente antes de la ventana de despliegue.

---

## 2. Fundamentacion de la Decision

| Criterio | Evaluacion |
|---|---|
| Completitud documental | 3/3 entregables completados (100%) |
| Plan de Despliegue (F7-DEL-001) | v1.0. 17 secciones. Estrategia greenfield. Checklist go/no-go de 25 items. |
| Plan de Rollback (F7-DEL-002) | v1.0. 14 secciones. SRP definido. 7 escenarios de falla. RTO ≤30 min, RPO 0. |
| Presentacion Go-Live Readiness (F7-DEL-003) | 25 slides. Consistente con los otros 2 entregables. |
| Checklist pre-go-live documentada | 28 items (10 pre-deploy + 8 ventana + 10 post-deploy) |
| Riesgos identificados | 10 riesgos con probabilidad, impacto y mitigacion documentados |
| Ventana de despliegue | Planificada: Lunes 2026-05-04, 06:00-09:00 COT |
| Fase 6 (UAT) aprobada | 61/61 CA R1-MVP (100%), 0 defectos abiertos. Producto apto. |

---

## 3. Condiciones para Aprobacion Plena

Las siguientes 10 condiciones deben cumplirse **antes del inicio de la ventana de despliegue** (2026-05-04, 06:00 COT). Su verificacion corresponde al equipo de DevOps con supervision del Tech Lead. El Project Manager debe recibir confirmacion de cumplimiento de cada condicion.

### Condiciones Pre-Deploy

| # | ID | Condicion | Responsable | Verificacion |
|---|---|---|---|---|
| 1 | F7-C01 | **Dockerfile multi-stage validado** — Build local exitoso sin errores. Imagen generada con tag `v1.0.0-release`. | devops | Log de build exitoso. Imagen en registry local. |
| 2 | F7-C02 | **K8s manifests validados** — `kubectl --dry-run=client` ejecutado exitosamente sobre todos los manifests del namespace `abax-memory-prod`. | devops | Salida de `--dry-run=client` sin errores. |
| 3 | F7-C03 | **Despliegue en staging exitoso** — Imagen desplegada en entorno de staging. Smoke tests automatizados pasan (todos los endpoints health check UP). | devops + qa-functional | Evidencia de smoke tests en staging: BUILD SUCCESS. |
| 4 | F7-C04 | **PostgreSQL prod creado y accesible** — Base de datos productiva provisionada. Conexion verificada desde el cluster K8s. | infra + devops | `psql` o health check de conexion exitoso. |
| 5 | F7-C05 | **Qdrant prod desplegado con health check UP** — Servicio de vector store productivo corriendo. Endpoint `/health` responde 200. | infra + devops | `curl /health` → 200 OK. |
| 6 | F7-C06 | **Keycloak realm `abax-memory` configurado** — Realm productivo con clientes, roles y usuarios configurados. Emision de JWT verificada. | infra + devops | Token JWT valido emitido por Keycloak prod. |
| 7 | F7-C07 | **Secretos K8s cargados en namespace prod** — Todos los secrets requeridos (DB, Qdrant, Keycloak, GitHub token) presentes en el namespace `abax-memory-prod`. | devops | `kubectl get secrets -n abax-memory-prod` muestra todos los secrets requeridos. |
| 8 | F7-C08 | **Registry de imagenes accesible desde cluster K8s** — El cluster puede hacer pull de imagenes del registry sin errores de autenticacion o red. | devops | Pull de imagen de prueba exitoso desde un pod en el cluster. |
| 9 | F7-C09 | **Tag `v1.0.0-release` aplicado al commit aprobado** — El commit exacto que paso UAT (Fase 6) esta taggeado con `v1.0.0-release` en el repositorio Git. | devops + tech-lead | `git tag -l v1.0.0-release` muestra el tag. Coincide con el commit de UAT. |
| 10 | F7-C10 | **GitHub deploy token generado y almacenado en Secret** — Token con permisos de lectura de registry generado. Almacenado como Secret en K8s. Expiracion > 90 dias. | devops | Secret presente en namespace. Token funcional verificado. |

---

## 4. Regimen de Verificacion

- **Fecha limite de cumplimiento**: Domingo 2026-05-03, 23:59 COT (vispera de la ventana de despliegue).
- **Formato de verificacion**: Cada responsable debe reportar el cumplimiento de sus condiciones asignadas mediante el canal `#abax-memory-deploy`.
- **Escalamiento**: Si alguna condicion no puede cumplirse antes de la fecha limite, el Tech Lead debe escalar inmediatamente al Project Manager para evaluar si la ventana de despliegue se mantiene, se reprograma o se cancela.
- **Evidencia**: Cada condicion cumplida debe tener evidencia documental (captura de pantalla, log, salida de comando) disponible para auditoria.

---

## 5. Consecuencias del Incumplimiento

- Si **1 o mas condiciones** no se cumplen antes de la fecha limite, la ventana de despliegue **no podra iniciar**.
- El Project Manager convocara una sesion extraordinaria con el equipo completo para decidir:
  - Reprogramar la ventana de despliegue.
  - Reducir el alcance de condiciones (requiere aprobacion formal del Product Owner).
  - Cancelar la ventana y regresar el gate a estado "No Aprobado".
- **No se autoriza el inicio de la ventana de despliegue sin el cumplimiento del 100% de las condiciones.**

---

## 6. Efecto de la Aprobacion Condicionada

- **Fase 7 — Despliegue** queda en estado **APROBADA CON CONDICIONES**.
- **Fase 8 — Estabilizacion** queda **habilitada condicionalmente**. Su inicio efectivo ocurrira tras:
  1. Verificacion del cumplimiento de las 10 condiciones (Seccion 3).
  2. Ejecucion exitosa de la ventana de despliegue (2026-05-04, 06:00-09:00 COT).
  3. Smoke tests post-deploy aprobados.
  4. Firma del acta de despliegue por devops y tech-lead.
- La aprobacion plena de Fase 7 se registrara en una adenda a esta acta tras el despliegue exitoso.

---

## 7. Firmas

| Rol | Nombre | Firma | Fecha |
|---|---|---|---|
| Project Manager | project-manager | **APROBADO CON CONDICIONES** | 2026-05-02 |
| Tech Lead | tech-lead | Pendiente | — |
| DevOps Lead | devops | Pendiente | — |
| Product Owner | product-owner | Pendiente | — |

---

## 8. Referencias

- [Plan de Despliegue v1.0](./plan-despliegue.md) — F7-DEL-001
- [Plan de Rollback v1.0](./plan-rollback.md) — F7-DEL-002
- [Presentacion Go-Live Readiness](./presentacion-go-live.html) — F7-DEL-003
- [Bitacora del Proyecto](../../bitacora.md)
- [Registro de Entregables](../../registro-entregables.md)
- [Acta de Aceptacion UAT](../fase-6-uat/acta-aceptacion-uat.md) — Fase 6 APROBADA
