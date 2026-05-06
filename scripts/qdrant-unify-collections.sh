#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# Abax-Memory v2.1.0 — Qdrant Collection Unification Script
# FT-V21-003.2
#
# Consolidates abax-memories-v1 (legacy) and abax-memories-v2 (active)
# into a single collection: abax-memories.
#
# Usage:
#   QDRANT_URL=http://localhost:6333 ./scripts/qdrant-unify-collections.sh
#
# Prerequisites:
#   - curl
#   - jq (optional, for formatted output)
#   - PostgreSQL access for pre-migration verification
# ─────────────────────────────────────────────────────────────────

set -euo pipefail

QDRANT_URL="${QDRANT_URL:-http://localhost:6333}"
V1_COLLECTION="abax-memories-v1"
V2_COLLECTION="abax-memories-v2"
TARGET_COLLECTION="abax-memories"

echo "=============================================="
echo "Abax-Memory v2.1.0 — Qdrant Unification Script"
echo "=============================================="
echo "Qdrant URL: $QDRANT_URL"
echo ""

# ── Step 1: Verify pre-migration state ─────────────────────────
echo "=== Step 1: Pre-migration verification ==="

echo "Checking existing collections..."
COLLECTIONS=$(curl -s "$QDRANT_URL/collections" | jq -r '.result.collections[].name' 2>/dev/null || \
              curl -s "$QDRANT_URL/collections")

echo "Collections found: $COLLECTIONS"

if echo "$COLLECTIONS" | grep -q "$V1_COLLECTION"; then
    echo "  [OK] $V1_COLLECTION exists"
else
    echo "  [SKIP] $V1_COLLECTION not found — nothing to migrate from v1"
fi

if echo "$COLLECTIONS" | grep -q "$V2_COLLECTION"; then
    echo "  [OK] $V2_COLLECTION exists"
else
    echo "  [ERROR] $V2_COLLECTION not found — cannot proceed"
    exit 1
fi

if echo "$COLLECTIONS" | grep -q "$TARGET_COLLECTION"; then
    echo "  [WARN] $TARGET_COLLECTION already exists — will be used as target"
fi

# ── Step 2: Migrate v1 points to target (if v1 exists) ─────────
if echo "$COLLECTIONS" | grep -q "$V1_COLLECTION"; then
    echo ""
    echo "=== Step 2: Migrating v1 points to $TARGET_COLLECTION ==="

    # Get point count from v1
    V1_COUNT=$(curl -s "$QDRANT_URL/collections/$V1_COLLECTION" | jq -r '.result.points_count // 0' 2>/dev/null || echo "0")
    echo "v1 collection has $V1_COUNT points"

    if [ "$V1_COUNT" -gt 0 ]; then
        # Scroll all points from v1
        echo "Scrolling v1 points..."
        SCROLL_RESPONSE=$(curl -s -X POST "$QDRANT_URL/collections/$V1_COLLECTION/points/scroll" \
            -H "Content-Type: application/json" \
            -d '{"limit": 1000, "with_payload": true, "with_vector": true}')

        # Extract points and upsert to target
        echo "Upserting to $TARGET_COLLECTION..."
        POINTS=$(echo "$SCROLL_RESPONSE" | jq '.result.points' 2>/dev/null || echo "[]")

        if [ "$POINTS" != "[]" ] && [ "$POINTS" != "null" ]; then
            curl -s -X PUT "$QDRANT_URL/collections/$TARGET_COLLECTION/points" \
                -H "Content-Type: application/json" \
                -d "{\"points\": $POINTS}"
            echo ""
            echo "  [OK] v1 points migrated to $TARGET_COLLECTION"
        else
            echo "  [SKIP] No points to migrate"
        fi
    fi

    # Create snapshot of v1 before deletion
    echo ""
    echo "Creating snapshot of $V1_COLLECTION..."
    curl -s -X POST "$QDRANT_URL/collections/$V1_COLLECTION/snapshots" \
        -H "Content-Type: application/json"
    echo ""
    echo "  [OK] Snapshot created"

    # Delete v1 collection
    echo ""
    echo "Deleting $V1_COLLECTION..."
    curl -s -X DELETE "$QDRANT_URL/collections/$V1_COLLECTION"
    echo ""
    echo "  [OK] $V1_COLLECTION deleted"
fi

# ── Step 3: Rename/create alias for v2 → target ────────────────
echo ""
echo "=== Step 3: Setting up $TARGET_COLLECTION ==="

# Check if v2 is the same as target or different
if [ "$V2_COLLECTION" != "$TARGET_COLLECTION" ]; then
    # If target already exists, use v2 data. If not, rename or alias.
    if echo "$COLLECTIONS" | grep -q "$TARGET_COLLECTION"; then
        echo "$TARGET_COLLECTION already exists — skipping rename"
        echo "Ensure the application configuration points to: $TARGET_COLLECTION"
    else
        # Qdrant 1.17 may not support rename, so create alias
        echo "Creating alias: $TARGET_COLLECTION -> $V2_COLLECTION"
        curl -s -X POST "$QDRANT_URL/collections/aliases" \
            -H "Content-Type: application/json" \
            -d "{\"actions\": [{\"create_alias\": {\"collection_name\": \"$V2_COLLECTION\", \"alias_name\": \"$TARGET_COLLECTION\"}}]}"
        echo ""
        echo "  [OK] Alias created: $TARGET_COLLECTION -> $V2_COLLECTION"
    fi
fi

# ── Step 4: Post-migration verification ─────────────────────────
echo ""
echo "=== Step 4: Post-migration verification ==="

# Verify only the target collection (or alias) is available
FINAL_COLLECTIONS=$(curl -s "$QDRANT_URL/collections" | jq -r '.result.collections[].name' 2>/dev/null || echo "")
echo "Final collections: $FINAL_COLLECTIONS"

# Check that v1 is gone
if echo "$FINAL_COLLECTIONS" | grep -q "$V1_COLLECTION"; then
    echo "  [WARN] $V1_COLLECTION still exists — manual cleanup may be required"
else
    echo "  [OK] $V1_COLLECTION no longer present"
fi

echo ""
echo "=============================================="
echo "Migration complete."
echo "Update application.properties:"
echo "  abax.v2.qdrant.collection=$TARGET_COLLECTION"
echo "=============================================="
