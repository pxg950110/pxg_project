#!/bin/bash
# MAIDC - Nacos Configuration Import Script
# Usage: bash init-nacos.sh

set -e

NACOS_URL="http://localhost:8848/nacos"
NAMESPACE="dev"
NACOS_USER="${NACOS_USER:-nacos}"
NACOS_PASS="${NACOS_PASS:-nacos}"

echo "=== MAIDC Nacos Configuration Import ==="

# Wait for Nacos to be ready
echo "Waiting for Nacos to be ready..."
for i in $(seq 1 30); do
    if curl -sf "${NACOS_URL}/" > /dev/null 2>&1; then
        echo "Nacos is ready"
        break
    fi
    echo "  Waiting... ($i/30)"
    sleep 3
done

# Login to get access token
echo "Logging in to Nacos..."
TOKEN=$(curl -s -X POST "${NACOS_URL}/v1/auth/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}" | \
    grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "ERROR: Failed to login to Nacos"
    exit 1
fi
echo "  Token acquired"

import_config() {
  local data_id=$1
  local group=$2
  local file=$3

  if [ ! -f "$file" ]; then
    echo "  SKIP: $file not found"
    return
  fi

  local content=$(cat "$file")

  echo "Importing: $data_id ($group)"
  local result=$(curl -s -X POST "${NACOS_URL}/v1/cs/configs?accessToken=${TOKEN}" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$group" \
    --data-urlencode "content=$content" \
    --data-urlencode "type=yaml" \
    --data-urlencode "tenant=$NAMESPACE")

  if [ "$result" = "true" ]; then
    echo "  OK"
  else
    echo "  FAILED: $result"
  fi
}

# Import shared config
import_config "maidc-shared.yaml" "DEFAULT_GROUP" "nacos-config/maidc-shared.yaml"

# Import service configs
import_config "maidc-gateway-dev.yaml" "DEFAULT_GROUP" "nacos-config/maidc-gateway-dev.yaml"
import_config "maidc-auth-dev.yaml" "DEFAULT_GROUP" "nacos-config/maidc-auth-dev.yaml"
import_config "maidc-model-dev.yaml" "DEFAULT_GROUP" "nacos-config/maidc-model-dev.yaml"
import_config "maidc-aiworker-dev.yaml" "DEFAULT_GROUP" "nacos-config/maidc-aiworker-dev.yaml"

echo ""
echo "=== Import Complete ==="
