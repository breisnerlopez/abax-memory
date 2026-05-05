# Reporte de Soporte — Fase 8 Estabilizacion v2.0.0
- **Fase**: 8-Estabilizacion
- **Responsable**: project-manager
- **Fecha**: 2026-05-04
- **Estado**: Completado
- **Sistema**: Abax-Memory v2.0.0
- **Entorno**: Desarrollo/Produccion unificado (localhost)

---

## 1. Procedimientos de Monitoreo

### 1.1 Health Check Periodico

Verificar el estado del backend cada 5 minutos durante la ventana de estabilizacion:

```bash
# Health check basico
curl -s http://localhost:8080/q/health

# Health check detallado (incluye DB, Qdrant)
curl -s http://localhost:8080/q/health/ready

# Liveness (si el proceso vive)
curl -s http://localhost:8080/q/health/live
```

**Criterio de alerta**: Si `/q/health` no responde 200 en 3 intentos consecutivos → escalar.

### 1.2 Verificacion de Componentes

```bash
# PostgreSQL
pg_isready -h localhost -p 5432 -U pmoa -d pmoadb

# Qdrant
curl -s http://localhost:6333/healthz
curl -s http://localhost:6333/readyz

# Proceso del backend
ps -p $(pgrep -f "quarkus-run.jar") -o pid,etime,cmd
```

### 1.3 Verificacion Funcional

```bash
# CRUD — Crear
ID=$(curl -s -X POST http://localhost:8080/api/v2/memories \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{"title":"Health check memory","content":"Automatic monitoring check","kind":"FACT","sensitivityLevel":"PUBLIC"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('id','ERR'))")
echo "Created: $ID"

# CRUD — Leer
curl -s http://localhost:8080/api/v2/memories/$ID \
  -H "X-Tenant-Id: tenant-alpha" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"Title: {d.get('title','ERR')}, Status: {d.get('status','ERR')}\")"

# Busqueda semantica
curl -s -X POST http://localhost:8080/api/v2/search/semantic \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{"query":"database migration","limit":3}'
```

### 1.4 Monitoreo de Logs

```bash
# Buscar errores criticos
grep -c "ERROR\|FATAL" /tmp/abax-deploy.log 2>/dev/null

# Ver ultimas lineas del log
tail -20 /tmp/abax-deploy.log

# Monitoreo continuo (durante ventana de estabilizacion)
tail -f /tmp/abax-deploy.log | grep --color -E "ERROR|WARN|FATAL"
```

---

## 2. Runbook de Incidentes Comunes

### INC-001: Backend no responde (Health check DOWN)

**Sintomas**:
- `curl http://localhost:8080/q/health` retorna error de conexion o timeout
- `ps -p $(pgrep -f "quarkus-run.jar")` no encuentra el proceso

**Diagnostico**:
```bash
# 1. Verificar si el proceso esta corriendo
pgrep -fa "quarkus-run.jar"

# 2. Verificar logs en busca de causa de caida
tail -50 /tmp/abax-deploy.log | grep -E "ERROR|FATAL|OutOfMemory|killed"

# 3. Verificar espacio en disco
df -h /tmp
```

**Procedimiento de recuperacion**:
```bash
# Opcion A: Reiniciar el backend
cd /ruta/al/proyecto/backend-quarkus
nohup java -jar target/quarkus-app/quarkus-run.jar > /tmp/abax-deploy.log 2>&1 &

# Opcion B: Usar Docker Compose
docker compose up -d
```

**RTO esperado**: < 2 minutos

---

### INC-002: PostgreSQL no accesible

**Sintomas**:
- `pg_isready` retorna "no response"
- Backend reporta errores de conexion JDBC en logs

**Diagnostico**:
```bash
# 1. Verificar estado del contenedor
docker ps | grep postgres

# 2. Verificar logs de PostgreSQL
docker logs abax-postgres 2>&1 | tail -20

# 3. Verificar puerto
ss -tlnp | grep 5432
```

**Procedimiento de recuperacion**:
```bash
# Reiniciar contenedor PostgreSQL
docker restart abax-postgres

# Esperar a que este listo
until pg_isready -h localhost -p 5432; do sleep 2; done
echo "PostgreSQL listo"

# Verificar migraciones Flyway
# El backend las aplica automaticamente al iniciar
```

**RTO esperado**: < 3 minutos

---

### INC-003: Qdrant no responde

**Sintomas**:
- `curl http://localhost:6333/healthz` retorna error
- Busqueda semantica retorna error 500

**Diagnostico**:
```bash
# 1. Verificar si el proceso Qdrant esta corriendo
pgrep -fa "qdrant"

# 2. Intentar health check
curl -v http://localhost:6333/healthz
curl -v http://localhost:6333/readyz

# 3. Verificar espacio en disco (Qdrant requiere espacio para indices)
df -h /var/lib/qdrant 2>/dev/null || df -h .
```

**Procedimiento de recuperacion**:
```bash
# Reiniciar Qdrant
./qdrant &

# Verificar que este listo
until curl -s http://localhost:6333/healthz | grep -q "healthz check passed"; do sleep 1; done
echo "Qdrant listo"
```

**RTO esperado**: < 2 minutos

---

### INC-004: Busqueda semantica sin resultados

**Sintomas**:
- `POST /api/v2/search/semantic` retorna 200 pero con resultados vacios
- Endpoints CRUD funcionan normalmente

**Diagnostico**:
```bash
# 1. Verificar puntos en Qdrant
curl -s http://localhost:6333/collections/abax-memories-v2 | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"Points: {d.get('result',{}).get('points_count',0)}, Indexed: {d.get('result',{}).get('config',{}).get('params',{}).get('vectors',{}).get('size',0)}\")" 2>/dev/null

# Alternativa simple
curl -s http://localhost:6333/collections/abax-memories-v2 | python3 -m json.tool | head -20

# 2. Verificar que la coleccion existe
curl -s http://localhost:6333/collections | python3 -c "import sys,json; cols=json.load(sys.stdin).get('result',{}).get('collections',[]); print([c['name'] for c in cols])"
```

**Procedimiento de recuperacion**:
- Si `points_count=0`: Re-poblar Qdrant reiniciando el backend (re-indexa las memorias existentes)
- Si `indexed_vectors_count=0` con puntos presentes: Comportamiento normal para <10k puntos (full scan activo)
- Si la coleccion no existe: Reiniciar el backend — Flyway + startup listeners la crean automaticamente

**RTO esperado**: < 5 minutos (incluyendo reinicio y re-indexacion)

---

### INC-005: Tenant isolation falla (datos visibles cross-tenant)

**Sintomas**:
- Una memoria creada en `tenant-alpha` es visible desde `tenant-bravo`
- Las verificaciones de aislamiento retornan 200 en lugar de 404

**Diagnostico**:
```bash
# 1. Crear memoria en tenant-alpha
ID=$(curl -s -X POST http://localhost:8080/api/v2/memories \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-alpha" \
  -d '{"title":"Isolation test alpha","content":"Test","kind":"FACT","sensitivityLevel":"PUBLIC"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('id','ERR'))")

# 2. Intentar acceder desde tenant-bravo (debe retornar 404)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v2/memories/$ID \
  -H "X-Tenant-Id: tenant-bravo")
echo "Cross-tenant HTTP: $HTTP (esperado: 404)"
```

**Procedimiento de recuperacion**:
```bash
# Este escenario indica un bug en la capa de persistencia.
# 1. Detener el backend
pkill -f "quarkus-run.jar"

# 2. Verificar el estado de la base de datos
psql -h localhost -U pmoa -d pmoadb -c "SELECT tenant_id, COUNT(*) FROM memories GROUP BY tenant_id;"

# 3. Escalar al tech-lead para investigacion de causa raiz
```

**Severidad**: Critica — requiere intervencion inmediata del tech-lead.

---

## 3. Contactos y Escalamiento

### Equipo de Soporte — Abax-Memory v2.0.0

| Rol | Responsable | Responsabilidades |
|---|---|---|
| **Tech Lead** | tech-lead | Escalamiento tecnico. Decisiones de arquitectura. Correccion de defectos criticos. |
| **DevOps** | devops | Infraestructura. Despliegue. Health checks. Logs. Rollback. |
| **QA Functional** | qa-functional | Verificacion funcional. Smoke tests. Reporte de defectos. |
| **Project Manager** | project-manager | Coordinacion. Comunicacion a stakeholders. Aprobacion de gates. |
| **Product Owner** | product-owner | Decisiones de producto. Aceptacion funcional. |

### Matriz de Escalamiento

| Nivel | Gatillo | Accion | Responsable |
|---|---|---|---|
| **N1 — Monitoreo** | Alerta de health check | Verificar y documentar | devops |
| **N2 — Soporte** | Health check DOWN > 3 intentos | Ejecutar runbook INC-001 | devops |
| **N3 — Escalamiento** | Recuperacion fallida (>10 min) | Escalar a tech-lead | devops → tech-lead |
| **N4 — Critico** | Perdida de datos o breach de seguridad | Escalar a PM + PO. Evaluar rollback | tech-lead → project-manager |

---

## 4. Procedimientos de Rollback

Si un incidente en Fase 8 requiere volver a la version anterior (v1.0.0):

```bash
# 1. Detener backend v2.0.0
pkill -f "quarkus-run.jar"

# 2. Verificar que el proceso murio
pgrep -f "quarkus-run.jar" || echo "Backend detenido"

# 3. Rollback de base de datos (si Flyway v2 migro esquema)
# Flyway v1 es baseline. Las migraciones v2 son incrementales.
# Si no hubo migraciones destructivas, la DB es compatible con v1.

# 4. Iniciar backend v1.0.0
# (requiere JAR de v1.0.0 disponible)

# 5. Verificar health check v1
curl -s http://localhost:8080/q/health
```

**Nota**: El plan de rollback completo esta documentado en `docs/entregables/v2/fase-7-despliegue/plan-de-rollback.md` con RTO ≤ 5 minutos y 7 escenarios de falla cubiertos.

---

## 5. Mantenimiento Programado

### Tareas de mantenimiento recomendadas

| Frecuencia | Tarea | Responsable |
|---|---|---|
| Diaria | Verificar health checks y logs | devops |
| Semanal | Rotar API key de OpenAI (si aplica) | devops |
| Semanal | Verificar espacio en disco para Qdrant y PostgreSQL | devops |
| Mensual | Backup de base de datos PostgreSQL | devops |
| Por release | Actualizar dependencias (Quarkus, Qdrant, etc.) | tech-lead |

### Comando de backup PostgreSQL

```bash
pg_dump -h localhost -U pmoa -d pmoadb -F c -f "abax-memory-backup-$(date +%Y%m%d).dump"
```

---

## 6. Estado de la Documentacion de Soporte

| Documento | Ruta | Estado |
|---|---|---|
| Plan de Despliegue | `docs/entregables/v2/fase-7-despliegue/00-plan-despliegue.md` | ✅ Vigente |
| Plan de Rollback | `docs/entregables/v2/fase-7-despliegue/plan-de-rollback.md` | ✅ Vigente |
| Reporte de Incidentes | `docs/entregables/v2/fase-8-estabilizacion/reporte-incidentes.md` | ✅ Vigente |
| Reporte de Soporte (este) | `docs/entregables/v2/fase-8-estabilizacion/reporte-soporte.md` | ✅ Vigente |

---

## Glosario

- **RTO**: Recovery Time Objective — tiempo maximo aceptable para restaurar un servicio tras una interrupcion.
- **Flyway**: Herramienta de migracion de base de datos que aplica cambios de esquema de forma versionada y repetible.
- **Tenant**: Cliente o inquilino en una arquitectura multi-tenant. Cada tenant tiene datos aislados de los demas.
- **Smoke test**: Prueba superficial que verifica que las funcionalidades criticas del sistema funcionan tras un despliegue o recuperacion.
- **JDBC**: Java Database Connectivity — API de Java para conectarse y ejecutar consultas contra bases de datos relacionales.
- **OIDC**: OpenID Connect — protocolo de autenticacion basado en OAuth 2.0.
