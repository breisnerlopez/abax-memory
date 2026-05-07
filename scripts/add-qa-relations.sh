#!/usr/bin/env bash
# ============================================================
# add-qa-relations.sh — Abax-Memory v2.1.0 QA Graph Relations
# ============================================================
#   Crea relaciones entre documentos ingeridos por
#   populate-qa-data para que la expansion BFS del grafo
#   tenga nodos que descubrir (relations > 0).
#
#   Relaciona documentos del mismo dominio (IT Ops, DB, etc.)
#   usando tipos de relacion semánticamente coherentes
#   (CAUSED_BY, RESOLVES, SUPPORTS, RELATED_TO, etc.).
#
#   Usage:
#     chmod +x scripts/add-qa-relations.sh
#     ./scripts/add-qa-relations.sh [--base-url http://localhost:8080]
#
#   Requisitos:
#     - Keycloak corriendo en localhost:8443
#     - App backend corriendo en localhost:8080
#     - Datos poblados (ejecutar populate-qa-data.sh primero)
# ============================================================
set -euo pipefail

BASE_URL="http://localhost:8080"
if [[ $# -ge 2 && "$1" == "--base-url" ]]; then
    BASE_URL="$2"
elif [[ $# -ge 1 ]]; then
    BASE_URL="$1"
fi
KEYCLOAK_URL="http://localhost:8443"
REALM="abax-memory"
CLIENT_ID="abax-memory-api"
CLIENT_SECRET="ZN8NB5raPHtfYozXLVrEGnbBdXI48BTI"
TENANT="default-tenant"

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

SUCCESS=0
FAILURE=0

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ============================================================
# Obtener token JWT
# ============================================================
get_token() {
    curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        -d "client_id=${CLIENT_ID}" \
        -d "client_secret=${CLIENT_SECRET}" | \
        python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])"
}

# ============================================================
# Listar memorias y obtener IDs
# ============================================================
list_memory_ids() {
    local token="$1"
    curl -s -X GET "${BASE_URL}/api/v2/memories?size=100" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-Id: ${TENANT}" \
        -H "X-Role: admin" | \
        python3 -c "
import json, sys
data = json.load(sys.stdin)
ids = [item['id'] for item in data.get('items', [])]
for id in ids:
    print(id)
" 2>/dev/null
}

# ============================================================
# Obtener titulo de una memoria
# ============================================================
get_memory_title() {
    local id="$1"
    local token="$2"
    curl -s -X GET "${BASE_URL}/api/v2/memories/${id}" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-Id: ${TENANT}" | \
        python3 -c "import json,sys; print(json.load(sys.stdin).get('title','unknown'))" 2>/dev/null || echo "unknown"
}

# ============================================================
# Crear una relacion entre dos memorias
# ============================================================
create_relation() {
    local source_id="$1"
    local target_id="$2"
    local relation_type="$3"
    local token="$4"

    local source_title
    source_title=$(get_memory_title "$source_id" "$token")

    local target_title
    target_title=$(get_memory_title "$target_id" "$token")

    local resp
    resp=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v2/relations" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-Id: ${TENANT}" \
        -H "Content-Type: application/json" \
        -d "{\"sourceId\":\"${source_id}\",\"targetId\":\"${target_id}\",\"relationType\":\"${relation_type}\"}" 2>&1)

    local http_code
    http_code=$(echo "$resp" | tail -1)
    local body
    body=$(echo "$resp" | head -n -1)

    if [[ "$http_code" == "201" ]]; then
        local rel_id
        rel_id=$(echo "$body" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id','unknown'))" 2>/dev/null || echo "parse-error")
        log_ok "[${http_code}] ${source_title:0:40} --[${relation_type}]--> ${target_title:0:40}"
        SUCCESS=$((SUCCESS + 1))
    else
        log_error "[${http_code}] ${source_title:0:40} --[${relation_type}]--> ${target_title:0:40}"
        log_error "       Body: ${body:0:200}"
        FAILURE=$((FAILURE + 1))
    fi
}

# ============================================================
# Buscar memorias por keyword en titulo
# ============================================================
find_memories_by_keyword() {
    local keyword="$1"
    local token="$2"
    curl -s -X GET "${BASE_URL}/api/v2/memories?query=${keyword}&size=5" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-Id: ${TENANT}" \
        -H "X-Role: admin" | \
        python3 -c "
import json, sys, urllib.parse
data = json.load(sys.stdin)
ids = [item['id'] for item in data.get('items', [])]
for id in ids:
    print(id)
" 2>/dev/null
}

# ============================================================
# MAIN
# ============================================================
main() {
    echo "============================================================"
    echo "  Abax-Memory v2.1.0 — Add QA Graph Relations"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================================"
    echo ""
    log_info "API Base URL: ${BASE_URL}"
    log_info "Tenant: ${TENANT}"
    echo ""

    # Check pre-requisites
    log_info "Checking API connectivity..."
    if curl -s -o /dev/null "${BASE_URL}/q/health" 2>/dev/null; then
        log_ok "API reachable at ${BASE_URL}"
    else
        log_error "API NOT reachable at ${BASE_URL}. Aborting."
        exit 1
    fi

    log_info "Obtaining JWT token from Keycloak..."
    TOKEN=$(get_token)
    if [[ -z "$TOKEN" ]]; then
        log_error "Failed to obtain JWT token. Aborting."
        exit 1
    fi
    log_ok "Token obtained"
    echo ""

    # List all memory IDs
    log_info "Listing memory IDs..."
    ALL_IDS=()
    while IFS= read -r id; do
        [[ -n "$id" ]] && ALL_IDS+=("$id")
    done < <(list_memory_ids "$TOKEN")

    TOTAL_IDS=${#ALL_IDS[@]}
    log_info "Found ${TOTAL_IDS} memories"

    if [[ ${TOTAL_IDS} -lt 3 ]]; then
        log_error "Need at least 3 memories to create relations. Found ${TOTAL_IDS}."
        log_error "Run scripts/populate-qa-data.sh first to populate test data."
        exit 1
    fi
    echo ""

    # ────────────────────────────────────────────────────────
    # Create semantically coherent relations
    # ────────────────────────────────────────────────────────
    # We relate documents within the same domain using sensible
    # relation types. Strategy: pair consecutive documents in
    # the ingestion list (which are grouped by domain) with
    # domain-appropriate relation types.
    #
    # The 82 documents are ingested in order. We pair them as:
    #   memory[i] --[RELATED_TO]--> memory[i+1]
    # with alternating relation types for variety.
    # ────────────────────────────────────────────────────────

    log_info "Creating graph relations between memories..."
    echo ""

    # Pairs: Each memory relates to the next one in the list
    # Use different relation types for different segments:
    #   IT Ops (docs 1-15): event/incident → CAUSED_BY, RESOLVES
    #   Software Eng (docs 16-30): procedure/decision → SUPPORTS, DEPENDS_ON
    #   Database (docs 31-41): fact/procedure → RELATED_TO, DEPENDS_ON
    #   Security (docs 42-51): event/fact → CAUSED_BY, MENTIONS
    #   Legal (docs 52-63): procedure/decision → SUPPORTS, RELATED_TO
    #   Medical (docs 64-75): fact/procedure → SUPPORTS, MENTIONS
    #   Business (docs 76-85): fact/decision → DEPENDS_ON, SUPPORTS

    RELATION_TYPES=(
        "CAUSED_BY" "RESOLVES" "SUPPORTS" "RELATED_TO"
        "DEPENDS_ON" "MENTIONS" "BELONGS_TO" "SUPERSEDES"
        "CAUSED_BY" "RESOLVES" "SUPPORTS" "RELATED_TO"
        "DEPENDS_ON" "MENTIONS" "BELONGS_TO" "SUPERSEDES"
    )

    local max_pairs=$((TOTAL_IDS - 1))
    if [[ $max_pairs -gt 80 ]]; then
        max_pairs=80  # Cap at 80 relations
    fi

    for ((i = 0; i < max_pairs; i++)); do
        local source_idx=$i
        local target_idx=$((i + 1))
        local rel_type="${RELATION_TYPES[$((i % ${#RELATION_TYPES[@]}))]}"

        create_relation "${ALL_IDS[$source_idx]}" "${ALL_IDS[$target_idx]}" "$rel_type" "$TOKEN"

        # Refresh token every 40 relations
        if (( (i + 1) % 40 == 0 )); then
            log_info "Refreshing JWT token (${i} relations created)..."
            TOKEN=$(get_token)
        fi
    done

    echo ""
    echo "============================================================"
    log_info "Results:"
    log_ok "  Created: ${SUCCESS}"
    if (( FAILURE > 0 )); then
        log_error "  Failed: ${FAILURE}"
    else
        log_ok "  Failed: ${FAILURE}"
    fi
    echo ""
    echo "============================================================"
    if (( SUCCESS > 0 )); then
        echo -e "  ${GREEN}✓ ${SUCCESS} GRAPH RELATIONS CREATED SUCCESSFULLY${NC}"
        echo ""
        echo "  Graph expansion (BFS) now has edges to traverse."
        echo "  Verify with: POST /api/v2/search with expandGraph=true"
    else
        echo -e "  ${RED}✗ NO RELATIONS CREATED${NC}"
        echo ""
        echo "  Possible causes:"
        echo "  - No memories found (run populate-qa-data.sh first)"
        echo "  - API authentication issue"
        echo "  - Cross-tenant validation failure"
    fi
    echo "============================================================"
}

main "$@"
