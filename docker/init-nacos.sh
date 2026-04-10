#!/bin/bash
# MAIDC - Nacos Configuration Import Script
# Usage: bash init-nacos.sh

NACOS_URL="http://localhost:8848/nacos"
NAMESPACE="dev"

echo "=== MAIDC Nacos Configuration Import ==="

import_config() {
  local data_id=$1
  local group=$2
  local file=$3
  local content=$(cat "$file")

  echo "Importing: $data_id ($group)"
  curl -s -X POST "$NACOS_URL/v1/cs/configs" \
    -d "dataId=$data_id&group=$group&content=$content&type=yaml&namespace=$NAMESPACE" \
    | grep -q "true" && echo "  ✓ OK" || echo "  ✗ FAILED"
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
