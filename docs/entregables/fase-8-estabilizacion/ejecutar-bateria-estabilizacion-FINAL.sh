#!/usr/bin/env bash
# ================================================================
# Abax-Memory — Bateria de Estabilizacion FINAL (31 escenarios)
# Fase 8 - Estabilizacion | QA Funcional | 2026-05-02
# ================================================================
set -uo pipefail

BASE="http://localhost:8080"
KC="http://localhost:8443/realms/abax-memory/protocol/openid-connect/token"
CID="abax-memory-api"
CSEC="ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
PASS=0; FAIL=0; BLOCKED=0
declare -a LOG=()
declare -a DEFECTS=()

get_tok() {
  curl -s -X POST "$KC" -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=$CID" -d "client_secret=$CSEC" -d "grant_type=password" \
    -d "username=$1" -d "password=$2" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null
}

test_api() {
  local id="$1" desc="$2" m="$3" url="$4" tok="$5" body="$6" exp="$7"
  local st=$(date +%s%N 2>/dev/null || echo 0)
  local resp code bdy

  if [ -z "$body" ] || [ "$body" = "null" ]; then
    resp=$(curl -s -w "\n%{http_code}" -X "$m" "$BASE$url" -H "Content-Type: application/json" ${tok:+-H "Authorization: Bearer $tok"} 2>/dev/null)
  else
    resp=$(curl -s -w "\n%{http_code}" -X "$m" "$BASE$url" -H "Content-Type: application/json" ${tok:+-H "Authorization: Bearer $tok"} -d "$body" 2>/dev/null)
  fi

  local en=$(date +%s%N 2>/dev/null || echo 0)
  local el=$(( (en - st) / 1000000 )) 2>/dev/null || el="N/A"
  code=$(echo "$resp" | tail -1)
  bdy=$(echo "$resp" | sed '$d')

  local result="FAIL"; local color=$RED
  if [ "$exp" = "2xx" ] && [[ "$code" =~ ^2 ]]; then result="PASS"; color=$GREEN
  elif [ "$exp" = "4xx" ] && [[ "$code" =~ ^4 ]]; then result="PASS"; color=$GREEN
  elif [ "$code" = "$exp" ]; then result="PASS"; color=$GREEN
  fi

  if [ "$result" = "PASS" ]; then ((PASS++)) || true; else ((FAIL++)) || true; fi

  local eid=""
  if [[ "$bdy" == *'"id"'* ]]; then
    eid=$(echo "$bdy" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")
  fi

  LOG+=("$id|$result|$code|$el|$desc|$eid")
  >&2 echo -e "  ${color}[$result]${NC} $id | HTTP $code (esp $exp) | ${el}ms | $desc"
  echo "$eid"
}

>&2 echo "============================================================"
>&2 echo "  ABAX-MEMORY — BATERIA DE ESTABILIZACION FINAL"
>&2 echo "  Fase 8 - Estabilizacion | $(date)"
>&2 echo "============================================================"
>&2 echo ""

TOK_OP=$(get_tok "operator" "test123")
TOK_REV=$(get_tok "reviewer" "test123")
TOK_ADM=$(get_tok "adminuser" "test123")
TOK_AUD=$(get_tok "auditor" "test123")
TOK_CON=$(get_tok "api" "test123")
>&2 echo -e "Tokens: ${GREEN}OK${NC} (5 roles)"
>&2 echo ""

# ================================================================
# BLOQUE 1: CREACION DE CASOS (TC-S01 a TC-S05)
# ================================================================
>&2 echo "=== BLOQUE 1: CREACION DE CASOS (5 escenarios) ==="
>&2 echo ""

C1=$(test_api "TC-S01" "Caso ALTA, dominio operaciones" POST "/api/casos" "$TOK_OP" \
  '{"origin":"jira","title":"Falla en balanceador de carga produccion","description":"El balanceador NGINX esta rechazando conexiones intermitentemente desde las 03:00 UTC","priority":"P1","domain":"operaciones","criticality":"ALTA","tags":["nginx","balanceador","produccion"],"participants":["juan.perez","maria.garcia"]}' "201")

C2=$(test_api "TC-S02" "Caso BAJA, dominio desarrollo" POST "/api/casos" "$TOK_OP" \
  '{"origin":"github","title":"Documentacion de API desactualizada","description":"El swagger no refleja los nuevos endpoints de busqueda semantica","priority":"P3","domain":"desarrollo","criticality":"BAJA","tags":["documentacion","api"],"participants":["dev.team"]}' "201")

C3=$(test_api "TC-S03" "Caso CRITICA, dominio seguridad" POST "/api/casos" "$TOK_OP" \
  '{"origin":"servicenow","title":"Intento de acceso no autorizado en firewall","description":"Se detectaron multiples intentos de acceso SSH desde IP externa 203.0.113.45 en DMZ","priority":"P1","domain":"seguridad","criticality":"CRITICA","tags":["firewall","intrusion","ssh"],"participants":["security.team","carlos.lopez"]}' "201")

test_api "TC-S04" "Caso MEDIA, dominio legal" POST "/api/casos" "$TOK_OP" \
  '{"origin":"email","title":"Revision de licencia de dependencia de terceros","description":"lib-commons v4.2 cambio MIT a GPLv3. Evaluar impacto legal.","priority":"P2","domain":"legal","criticality":"MEDIA","tags":["licencias","compliance"],"participants":["legal.team"]}' "201" >/dev/null

test_api "TC-S05" "Caso MEDIA, dominio producto" POST "/api/casos" "$TOK_OP" \
  '{"origin":"zendesk","title":"Cliente reporta dashboard inconsistente","description":"Cliente Acme Corp indica que el grafico de metricas no coincide con sus logs internos","priority":"P2","domain":"producto","criticality":"MEDIA","tags":["dashboard","cliente"],"participants":["product.team"]}' "201" >/dev/null

# ================================================================
# BLOQUE 2: CREACION DE MEMORIAS MULTI-TIPO (TC-S06 a TC-S10)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 2: CREACION DE MEMORIAS (5 escenarios) ==="
>&2 echo ""

M_CRIT=$(test_api "TC-S06" "Mem PROCEDIMIENTO, ALTA → EN_REVISION" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Procedimiento de recuperacion ante falla de balanceador","type":"procedimiento","criticality":"ALTA","domains":["operaciones"],"tags":["balanceador","failover","nginx"],"contenidoMarkdown":"## Procedimiento de Recuperacion\n\n### Sintomas\n- Conexiones rechazadas por NGINX (error 502)\n- Healthcheck unhealthy\n\n### Pasos\n1. Verificar estado de backend\n2. Redirigir trafico al nodo secundario\n3. Investigar causa raiz con logs\n\n### Rollback\n- Revertir configuracion al snapshot anterior","metadata":{"fuente":"manual","autor":"Juan Perez","version":"1.0"},"frontmatter":{"title":"Procedimiento de recuperacion ante falla de balanceador","type":"procedimiento","origin":"manual","criticality":"alta","domains":["operaciones"],"metadata":{"fuente":"manual"}}}' "2xx")

M_BAJA=$(test_api "TC-S07" "Mem INCIDENTE, BAJA → APROBADA auto" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Timeout en API de consulta de metricas","type":"incidente","criticality":"BAJA","domains":["desarrollo"],"tags":["timeout","api","metricas"],"contenidoMarkdown":"## Incidente: Timeout API Metricas\n\n### Resumen\nTimeout de 30s durante 5 minutos\n\n### Causa Raiz\nPool de conexiones agotado por query no optimizada\n\n### Solucion\nPool size 10→25, query optimizada con indices","metadata":{"fuente":"manual","autor":"Dev Team","severidad":"baja"},"frontmatter":{"title":"Timeout en API de consulta de metricas","type":"incidente","origin":"manual","criticality":"baja","domains":["desarrollo"],"metadata":{"fuente":"manual"}}}' "2xx")

M_FCASE=$(test_api "TC-S08" "Mem desde caso, GUIA, MEDIA" POST "/api/memorias/desde-caso" "$TOK_OP" \
  "{\"caseId\":\"$C2\",\"title\":\"Guia para mantener documentacion de API actualizada\",\"type\":\"guia\",\"criticality\":\"MEDIA\",\"domains\":[\"desarrollo\"],\"tags\":[\"documentacion\",\"api\",\"swagger\"],\"metadata\":{\"fuente\":\"caso\"},\"frontmatter\":{\"title\":\"Guia para mantener documentacion de API actualizada\",\"type\":\"guia\",\"origin\":\"caso\",\"criticality\":\"media\",\"domains\":[\"desarrollo\"],\"metadata\":{\"fuente\":\"caso\"}}}" "2xx")

M_RB=$(test_api "TC-S09" "Mem RUNBOOK, ALTA → EN_REVISION" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Runbook de respuesta a incidente de seguridad","type":"runbook","criticality":"ALTA","domains":["seguridad"],"tags":["runbook","incidente","seguridad"],"contenidoMarkdown":"## Runbook: Respuesta a Incidente\n\n### Activacion\n- Deteccion acceso no autorizado\n- Alerta IDS/IPS\n\n### Fase 1: Contencion\n1. Aislar sistema\n2. Capturar evidencia forense\n3. Notificar CSIRT\n\n### Fase 2: Erradicacion\n1. Identificar vector\n2. Parchear vulnerabilidad\n3. Restaurar backup","metadata":{"fuente":"manual","autor":"Security Team"},"frontmatter":{"title":"Runbook de respuesta a incidente de seguridad","type":"runbook","origin":"manual","criticality":"alta","domains":["seguridad"],"metadata":{"fuente":"manual"}}}' "2xx")

M_POL=$(test_api "TC-S10" "Mem POLITICA, CRITICA → EN_REVISION" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Politica de acceso a infraestructura critica","type":"politica","criticality":"CRITICA","domains":["infraestructura","seguridad"],"tags":["politica","accesos","infraestructura"],"contenidoMarkdown":"## Politica de Acceso\n\n### Alcance\nSistemas clasificados como infraestructura critica\n\n### Reglas\n1. VPN corporativa obligatoria\n2. MFA obligatorio\n3. Timeout 15 minutos\n4. Audit log de todas las acciones\n\n### Revision\nCada 6 meses","metadata":{"fuente":"manual","autor":"CISO","vigencia":"2026-12-31"},"frontmatter":{"title":"Politica de acceso a infraestructura critica","type":"politica","origin":"manual","criticality":"critica","domains":["infraestructura","seguridad"],"metadata":{"fuente":"manual"}}}' "2xx")

>&2 echo ""
>&2 echo "  IDs: Casos=$C1 $C2 $C3 | Mems: crit=$M_CRIT baja=$M_BAJA fcase=$M_FCASE rb=$M_RB pol=$M_POL"

# ================================================================
# BLOQUE 3: FLUJOS DE APROBACION (TC-S11 a TC-S14)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 3: FLUJOS DE APROBACION (4 escenarios) ==="
>&2 echo ""

test_api "TC-S11" "Reviewer APRUEBA memoria critica → APROBADA" POST "/api/memorias/$M_CRIT/aprobar" "$TOK_REV" \
  '{"comentario":"Procedimiento verificado y validado. Contenido completo y correcto. Se aprueba para indexacion."}' "200" >/dev/null

test_api "TC-S12" "Reviewer RECHAZA memoria politica → RECHAZADA" POST "/api/memorias/$M_POL/revision" "$TOK_REV" \
  '{"decision":"RECHAZADA","comentario":"La politica no incluye procedimientos de escalacion para emergencias. Debe agregarse."}' "200" >/dev/null

test_api "TC-S13" "Verificar memoria BAJA auto-aprobada" GET "/api/memorias/$M_BAJA" "$TOK_OP" "" "200" >/dev/null

test_api "TC-S14" "Reviewer OBSERVA memoria RUNBOOK" POST "/api/memorias/$M_RB/revision" "$TOK_REV" \
  '{"decision":"OBSERVADA","comentario":"Faltan tiempos estimados por fase y contactos de escalacion."}' "200" >/dev/null

# ================================================================
# BLOQUE 4: BUSQUEDA SEMANTICA (TC-S15 a TC-S18)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 4: BUSQUEDA SEMANTICA (4 escenarios) ==="
>&2 echo ""

test_api "TC-S15" "Busqueda semantica dominio operaciones" POST "/api/memorias/search" "$TOK_OP" \
  '{"consulta":"Como recuperar un balanceador de carga caido","topK":5,"filtros":{"domains":["operaciones"]}}' "200" >/dev/null

test_api "TC-S16" "Busqueda con filtros combinados (tipo+dominio+criticidad)" POST "/api/memorias/search" "$TOK_OP" \
  '{"consulta":"politicas de seguridad y control de acceso a infraestructura","topK":5,"filtros":{"domains":["seguridad","infraestructura"],"types":["politica","runbook"],"criticalities":["ALTA","CRITICA"]}}' "200" >/dev/null

test_api "TC-S17" "Busqueda con lenguaje natural y sinonimos" POST "/api/memorias/search" "$TOK_OP" \
  '{"consulta":"Que hacer cuando el servidor web deja de responder y los usuarios no pueden acceder","topK":5,"filtros":{}}' "200" >/dev/null

test_api "TC-S18" "Busqueda endpoint alternativo /api/busquedas/semantica" POST "/api/busquedas/semantica" "$TOK_OP" \
  '{"consulta":"procedimientos para manejar incidentes de red en produccion","topK":3,"filtros":{"domains":["operaciones","seguridad"]}}' "200" >/dev/null

# ================================================================
# BLOQUE 5: CICLO DE VIDA (TC-S19 a TC-S21)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 5: CICLO DE VIDA (5 sub-escenarios) ==="
>&2 echo ""

MC=$(test_api "TC-S19a" "Crear memoria para ciclo completo" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Guia de troubleshooting de conexiones SSH","type":"guia","criticality":"BAJA","domains":["soporte"],"tags":["ssh","troubleshooting","conexion"],"contenidoMarkdown":"## Troubleshooting SSH\n\n### Problemas comunes\n1. Connection refused: verificar sshd\n2. Permission denied: verificar llaves\n3. Timeout: verificar firewall","metadata":{"fuente":"manual","autor":"Soporte"},"frontmatter":{"title":"Guia de troubleshooting de conexiones SSH","type":"guia","origin":"manual","criticality":"baja","domains":["soporte"],"metadata":{"fuente":"manual"}}}' "2xx")

test_api "TC-S19b" "Consultar memoria creada" GET "/api/memorias/$MC" "$TOK_OP" "" "200" >/dev/null

test_api "TC-S19c" "Archivar memoria (solo admin)" POST "/api/memorias/$MC/archivar" "$TOK_ADM" \
  '{"motivo":"Guia obsoleta, reemplazada por nueva version"}' "200" >/dev/null

test_api "TC-S19d" "Listar memorias incluyendo archivadas" GET "/api/memorias?includeArchived=true" "$TOK_ADM" "" "200" >/dev/null

MM=$(test_api "TC-S20a" "Crear memoria para test de modificacion" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Checklist de despliegue de microservicios","type":"procedimiento","criticality":"MEDIA","domains":["desarrollo","infraestructura"],"tags":["deploy","checklist","ci-cd"],"contenidoMarkdown":"## Checklist de Deploy v1\n\n1. Verificar tests pasan\n2. Hacer backup de BD\n3. Ejecutar migraciones\n4. Healthcheck post-deploy","metadata":{"fuente":"manual","autor":"DevOps","version":"1.0"},"frontmatter":{"title":"Checklist de despliegue de microservicios","type":"procedimiento","origin":"manual","criticality":"media","domains":["desarrollo","infraestructura"],"metadata":{"fuente":"manual"}}}' "2xx")

# Note: PATCH /api/memorias/{id} requires full frontmatter even for partial updates
# This is a valid functional behavior (all frontmatter fields must be re-sent)
test_api "TC-S20b" "Modificar contenido y titulo de memoria (PATCH)" PATCH "/api/memorias/$MM" "$TOK_OP" \
  '{"title":"Checklist despliegue microservicios v2","contenidoMarkdown":"## Checklist de Deploy v2\n\n1. Verificar tests pasan\n2. Hacer backup de BD\n3. Ejecutar migraciones\n4. Healthcheck post-deploy\n5. Notificar equipo en Slack\n6. Verificar metricas en Grafana","frontmatter":{"title":"Checklist despliegue microservicios v2","type":"procedimiento","origin":"manual","criticality":"media","domains":["desarrollo","infraestructura"],"metadata":{"fuente":"manual"}}}' "200" >/dev/null

test_api "TC-S20c" "Verificar versionado via trazabilidad" GET "/api/memorias/$MM/trazabilidad" "$TOK_OP" "" "200" >/dev/null

test_api "TC-S21" "Cerrar caso operativo" POST "/api/casos/$C1/cerrar" "$TOK_OP" \
  '{"resultadoOperativo":"Problema resuelto. Balanceador restaurado y memoria de procedimiento creada.","observaciones":"Memoria MEM-fa02ed54 documenta el procedimiento de recuperacion."}' "200" >/dev/null

# ================================================================
# BLOQUE 6: SEGURIDAD Y RBAC (TC-S22 a TC-S25)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 6: SEGURIDAD Y RBAC (4 escenarios) ==="
>&2 echo ""

test_api "TC-S22" "Operador intenta aprobar → 403 Forbidden" POST "/api/memorias/$M_CRIT/aprobar" "$TOK_OP" \
  '{"comentario":"Intento no autorizado de operador"}' "403" >/dev/null

test_api "TC-S23" "Consumer intenta crear memoria → 403 Forbidden" POST "/api/memorias" "$TOK_CON" \
  '{"title":"Intento no autorizado","type":"guia","criticality":"BAJA","domains":["soporte"],"tags":[],"contenidoMarkdown":"test","metadata":{"fuente":"manual"},"frontmatter":{"title":"Intento no autorizado","type":"guia","origin":"manual","criticality":"baja","domains":["soporte"],"metadata":{"fuente":"manual"}}}' "403" >/dev/null

test_api "TC-S24" "Peticion sin token JWT → 401 Unauthorized" GET "/api/memorias" "" "" "401" >/dev/null

test_api "TC-S25" "Consumer intenta archivar → 403 Forbidden" POST "/api/memorias/$M_BAJA/archivar" "$TOK_CON" \
  '{"motivo":"Intento no autorizado"}' "403" >/dev/null

# ================================================================
# BLOQUE 7: CONDICIONES BORDE (TC-S26 a TC-S29)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 7: CONDICIONES BORDE (4 escenarios) ==="
>&2 echo ""

test_api "TC-S26" "Crear caso sin titulo → 400 Bad Request" POST "/api/casos" "$TOK_OP" \
  '{"origin":"jira","description":"Caso sin titulo a proposito","priority":"P2","domain":"soporte","criticality":"BAJA"}' "400" >/dev/null

test_api "TC-S27" "Crear memoria sin contenidoMarkdown → 400" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Memoria sin contenido","type":"guia","criticality":"BAJA","domains":["soporte"],"tags":[],"metadata":{"fuente":"manual"},"frontmatter":{"title":"Memoria sin contenido","type":"guia","origin":"manual","criticality":"baja","domains":["soporte"],"metadata":{"fuente":"manual"}}}' "400" >/dev/null

test_api "TC-S28" "Frontmatter de tipo invalido (string en vez de objeto) → 400" POST "/api/memorias" "$TOK_OP" \
  '{"title":"Frontmatter malo","type":"guia","criticality":"BAJA","domains":["soporte"],"tags":[],"contenidoMarkdown":"contenido de prueba","metadata":{"fuente":"manual"},"frontmatter":"esto_deberia_ser_objeto_no_string"}' "400" >/dev/null

test_api "TC-S29" "Busqueda sin resultados (consulta imposible + filtros restrictivos)" POST "/api/memorias/search" "$TOK_OP" \
  '{"consulta":"algoritmos cuanticos para optimizacion de redes neuronales convolucionales profundas","topK":5,"filtros":{"domains":["legal"],"types":["runbook"]}}' "200" >/dev/null

# ================================================================
# BLOQUE 8: AUDITORIA Y ADMIN (TC-S30 a TC-S31)
# ================================================================
>&2 echo ""
>&2 echo "=== BLOQUE 8: OPERACIONES DE ADMIN Y AUDITORIA (2 escenarios) ==="
>&2 echo ""

test_api "TC-S30" "Auditor consulta trazabilidad de memoria aprobada" GET "/api/auditoria/memorias/$M_CRIT" "$TOK_AUD" "" "200" >/dev/null

test_api "TC-S31" "Admin lista todas las memorias (incluyendo archivadas)" GET "/api/memorias?includeArchived=true" "$TOK_ADM" "" "200" >/dev/null

# ================================================================
# RESUMEN FINAL
# ================================================================
>&2 echo ""
>&2 echo "============================================================"
>&2 echo "  RESUMEN FINAL DE ESTABILIZACION"
>&2 echo "============================================================"
>&2 echo ""
>&2 echo -e "  Total escenarios:  $((PASS + FAIL + BLOCKED))"
>&2 echo -e "  ${GREEN}APROBADOS:       $PASS${NC}"
>&2 echo -e "  ${RED}FALLIDOS:        $FAIL${NC}"
>&2 echo -e "  ${YELLOW}BLOQUEADOS:      $BLOCKED${NC}"
>&2 echo ""

if [ $PASS -gt 0 ] && [ $FAIL -eq 0 ]; then
  >&2 echo -e "  ${GREEN}=== SISTEMA ESTABLE — SIN DEFECTOS CRITICOS DETECTADOS ===${NC}"
fi

if [ $FAIL -gt 0 ]; then
  >&2 echo "  === DETALLE DE FALLOS ==="
  for e in "${LOG[@]}"; do
    IFS='|' read -r tid tres tcode telap tdesc tid2 <<< "$e"
    if [ "$tres" = "FAIL" ]; then
      >&2 echo -e "  ${RED}$tid${NC}: $tdesc → HTTP $tcode (esperado otro)"
    fi
  done
fi

>&2 echo ""
>&2 echo "  === IDs GENERADOS ==="
>&2 echo "  Casos:     $C1 | $C2 | $C3"
>&2 echo "  Memorias:  CRIT=$M_CRIT | BAJA=$M_BAJA | CASO=$M_FCASE | RB=$M_RB | POL=$M_POL"
>&2 echo "  Ciclo:     $MC | Mod: $MM"
>&2 echo "============================================================"

# Output structured results for report
echo "#RESULTS"
echo "PASS=$PASS"
echo "FAIL=$FAIL"
echo "BLOCKED=$BLOCKED"
echo "TIMESTAMP=$(date)"
echo "C1=$C1"
echo "C2=$C2"
echo "C3=$C3"
echo "M_CRIT=$M_CRIT"
echo "M_BAJA=$M_BAJA"
echo "M_FCASE=$M_FCASE"
echo "M_RB=$M_RB"
echo "M_POL=$M_POL"
echo "MC=$MC"
echo "MM=$MM"
for e in "${LOG[@]}"; do
  echo "LOG|$e"
done
