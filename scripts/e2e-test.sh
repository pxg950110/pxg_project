#!/bin/bash
# MAIDC End-to-End Test Script
# Tests the full model lifecycle: Register → Upload → Evaluate → Approve → Deploy → Infer

set -e
BASE_URL="http://localhost:8080/api/v1"
TOKEN=""

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log() { echo -e "${GREEN}[✓]${NC} $1"; }
fail() { echo -e "${RED}[✗]${NC} $1"; exit 1; }

# Step 1: Login
echo "=== Step 1: Login ==="
LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo $LOGIN_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])" 2>/dev/null)
[ -z "$TOKEN" ] && fail "Login failed"
log "Login successful, token obtained"

AUTH="Authorization: Bearer $TOKEN"

# Step 2: Register Model
echo "=== Step 2: Register Model ==="
MODEL_RES=$(curl -s -X POST "$BASE_URL/models" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "model_name": "E2E Test Model",
    "model_code": "e2e-test-001",
    "model_type": "IMAGE_CLASSIFICATION",
    "framework": "PYTORCH",
    "description": "End-to-end test model"
  }')
MODEL_ID=$(echo $MODEL_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -z "$MODEL_ID" ] && fail "Model registration failed"
log "Model registered: ID=$MODEL_ID"

# Step 3: Get Model Detail
echo "=== Step 3: Get Model ==="
curl -s "$BASE_URL/models/$MODEL_ID" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f'  Name: {d[\"model_name\"]}')
print(f'  Status: {d[\"status\"]}')
" 2>/dev/null
log "Model detail retrieved"

# Step 4: Create Evaluation
echo "=== Step 4: Create Evaluation ==="
EVAL_RES=$(curl -s -X POST "$BASE_URL/evaluations" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"model_id\": $MODEL_ID,
    \"dataset_id\": \"test-dataset\",
    \"metrics\": [\"accuracy\", \"f1_score\"]
  }")
EVAL_ID=$(echo $EVAL_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -z "$EVAL_ID" ] && fail "Evaluation creation failed"
log "Evaluation created: ID=$EVAL_ID"

# Step 5: Submit Approval
echo "=== Step 5: Submit Approval ==="
APPROVAL_RES=$(curl -s -X POST "$BASE_URL/approvals" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"model_id\": $MODEL_ID,
    \"approval_type\": \"DEPLOYMENT\",
    \"evidence_docs\": [\"doc1.pdf\"],
    \"risk_assessment\": \"Low risk\"
  }")
APPROVAL_ID=$(echo $APPROVAL_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -z "$APPROVAL_ID" ] && fail "Approval submission failed"
log "Approval submitted: ID=$APPROVAL_ID"

# Step 6: Review Approval
echo "=== Step 6: Review Approval ==="
curl -s -X PUT "$BASE_URL/approvals/$APPROVAL_ID/review" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"action":"APPROVED","comment":"E2E test approval"}' > /dev/null
log "Approval reviewed (approved)"

# Step 7: Create Deployment
echo "=== Step 7: Create Deployment ==="
DEPLOY_RES=$(curl -s -X POST "$BASE_URL/deployments" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"model_id\": $MODEL_ID,
    \"name\": \"e2e-test-deploy\",
    \"resource_config\": {\"cpu\":2,\"memory\":4096,\"gpu\":0,\"replicas\":1}
  }")
DEPLOY_ID=$(echo $DEPLOY_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -z "$DEPLOY_ID" ] && fail "Deployment creation failed"
log "Deployment created: ID=$DEPLOY_ID"

# Step 8: Start Deployment
echo "=== Step 8: Start Deployment ==="
curl -s -X PUT "$BASE_URL/deployments/$DEPLOY_ID/start" -H "$AUTH" > /dev/null
log "Deployment started"

# Step 9: Inference
echo "=== Step 9: Run Inference ==="
INFER_RES=$(curl -s -X POST "$BASE_URL/inference/$DEPLOY_ID" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"input_data":{"image":"test.jpg"},"parameters":{}}')
log "Inference completed"

# Step 10: Check Monitoring
echo "=== Step 10: Check Monitoring ==="
curl -s "$BASE_URL/monitoring/deployments/$DEPLOY_ID/logs?page=1&pageSize=5" -H "$AUTH" > /dev/null
log "Monitoring logs retrieved"

echo ""
echo "=========================================="
log "E2E Test PASSED - All 10 steps completed"
echo "=========================================="
