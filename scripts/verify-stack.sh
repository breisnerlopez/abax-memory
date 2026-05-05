#!/usr/bin/env bash
# =============================================================================
# Abax-Memory v2.0.0 — Stack Verification Script
# =============================================================================
# Uso: ./scripts/verify-stack.sh
#
# Verifica el estado de los 4 servicios core del stack:
#   1. PostgreSQL  (puerto 5432)
#   2. Qdrant      (puerto 6333)
#   3. Keycloak    (puerto 8443)
#   4. OpenAI      (API key)
#
# Salida: reporte de estado + exit code (0 = todo UP, 1 = algo DOWN)
# =============================================================================

set -o pipefail

# ── Colores ──────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0
TOTAL=4

echo ""
echo "══════════════════════════════════════════════════════════"
echo "  Abax-Memory v2.0.0 — Stack Verification"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "══════════════════════════════════════════════════════════"
echo ""

# ── 1. PostgreSQL ───────────────────────────────────────────────────────
echo -n "  [1/4] PostgreSQL (localhost:5432) ... "
if pg_isready -h localhost -p 5432 -q 2>/dev/null; then
    echo -e "${GREEN}UP${NC}"
    ((PASS++))
else
    echo -e "${RED}DOWN${NC}"
    echo "        → Verifica: docker compose up -d postgres"
    ((FAIL++))
fi

# ── 2. Qdrant ───────────────────────────────────────────────────────────
echo -n "  [2/4] Qdrant (localhost:6333) ...... "
QDRANT_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:6333/healthz 2>/dev/null)
if [ "$QDRANT_RESPONSE" = "200" ]; then
    QDRANT_VERSION=$(curl -s http://localhost:6333/ | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','unknown'))" 2>/dev/null)
    echo -e "${GREEN}UP${NC} (v${QDRANT_VERSION})"
    ((PASS++))
else
    echo -e "${RED}DOWN${NC} (HTTP ${QDRANT_RESPONSE})"
    echo "        → Verifica: docker compose up -d qdrant"
    ((FAIL++))
fi

# ── 3. Keycloak ─────────────────────────────────────────────────────────
echo -n "  [3/4] Keycloak (localhost:8443) .... "
KEYCLOAK_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8443/realms/abax-memory 2>/dev/null)
if [ "$KEYCLOAK_CODE" = "200" ]; then
    echo -e "${GREEN}UP${NC} (realm: abax-memory)"
    ((PASS++))
else
    echo -e "${RED}DOWN${NC} (HTTP ${KEYCLOAK_CODE})"
    echo "        → Verifica: docker compose up -d keycloak"
    echo "        → Keycloak puede tardar 30-60s en iniciar"
    ((FAIL++))
fi

# ── 4. OpenAI ───────────────────────────────────────────────────────────
echo -n "  [4/4] OpenAI API Key ............... "
if [ -n "${OPENAI_API_KEY}" ]; then
    KEY_LENGTH=${#OPENAI_API_KEY}
    if [ "$KEY_LENGTH" -ge 50 ]; then
        echo -e "${GREEN}SET${NC} (${KEY_LENGTH} chars)"
        ((PASS++))
    else
        echo -e "${YELLOW}SET but short${NC} (${KEY_LENGTH} chars — expected ~164)"
        ((PASS++))
    fi
else
    echo -e "${RED}NOT SET${NC}"
    echo "        → export OPENAI_API_KEY=\"sk-proj-...\""
    echo "        → La app usara InMemoryEmbeddingProvider (solo tests)"
    ((FAIL++))
fi

# ── Resumen ─────────────────────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────"
echo -n "  Resultado: ${PASS}/${TOTAL} servicios UP  "
if [ "$FAIL" -eq 0 ]; then
    echo -e "${GREEN}✓ STACK HEALTHY${NC}"
    exit 0
elif [ "$PASS" -ge 3 ]; then
    echo -e "${YELLOW}⚠ STACK DEGRADED${NC} (${FAIL} servicio(s) down)"
    exit 1
else
    echo -e "${RED}✗ STACK DOWN${NC} (${FAIL} servicio(s) down)"
    exit 1
fi
echo "══════════════════════════════════════════════════════════"
echo ""
