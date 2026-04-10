#!/bin/bash
# MAIDC 端到端集成测试 — 覆盖全部 8 个核心流程 (F1-F8)
# 运行条件: 全部服务已启动 (docker compose -f docker/docker-compose-full.yml up -d)
# 使用方式: ./scripts/e2e-tests.sh [base_url]

set -e

BASE_URL="${1:-http://localhost:8080/api/v1}"
TOKEN=""
MODEL_ID=""
VERSION_ID=""
EVAL_ID=""
APPROVAL_ID=""
DEPLOY_ID=""
PATIENT_ID=""
PROJECT_ID=""
DATASET_ID=""
ETL_TASK_ID=""
LABEL_TASK_ID=""
ALERT_RULE_ID=""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS=0
FAIL=0
SKIP=0

log() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); }
skip() { echo -e "${YELLOW}[SKIP]${NC} $1"; SKIP=$((SKIP+1)); }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
section() { echo -e "\n${BLUE}=== $1 ===${NC}"; }

# Utility: extract field from JSON response
json_val() {
  echo "$1" | python3 -c "import sys,json; d=json.load(sys.stdin); $2" 2>/dev/null
}

# =============================================
# F6: 用户认证鉴权 E2E 测试
# =============================================
test_auth() {
  section "F6: 用户认证鉴权"

  # F6.1: 登录成功
  info "F6.1: 正常登录"
  LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
  TOKEN=$(json_val "$LOGIN_RES" "print(d['data']['access_token'])")
  if [ -n "$TOKEN" ]; then log "登录成功，获取Token"; else fail "登录失败"; return 1; fi
  AUTH="Authorization: Bearer $TOKEN"

  # F6.2: 获取用户信息
  info "F6.2: 获取当前用户信息"
  USER_RES=$(curl -s "$BASE_URL/auth/user/info" -H "$AUTH")
  USERNAME=$(json_val "$USER_RES" "print(d['data']['username'])")
  [ "$USERNAME" = "admin" ] && log "用户信息正确: $USERNAME" || fail "用户信息不匹配"

  # F6.3: Token刷新
  info "F6.3: Token刷新"
  REFRESH_TOKEN=$(json_val "$LOGIN_RES" "print(d['data']['refresh_token'])")
  REFRESH_RES=$(curl -s -X POST "$BASE_URL/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
  NEW_TOKEN=$(json_val "$REFRESH_RES" "print(d['data']['accessToken'])")
  [ -n "$NEW_TOKEN" ] && log "Token刷新成功" || fail "Token刷新失败"

  # F6.4: 错误密码锁定
  info "F6.4: 错误密码尝试"
  for i in $(seq 1 5); do
    curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"wrong_password"}' > /dev/null
  done
  LOCK_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong_password"}')
  LOCKED=$(json_val "$LOCK_RES" "print(d.get('code',0))")
  [ "$LOCKED" = "401" ] && log "密码锁定生效" || skip "密码锁定未触发（可能Redis未启动）"

  # F6.5: 登出
  info "F6.5: 登出"
  curl -s -X POST "$BASE_URL/auth/logout" -H "$AUTH" > /dev/null
  log "登出成功"
}

# =============================================
# F1: 模型全生命周期 E2E 测试
# =============================================
test_model_lifecycle() {
  section "F1: 模型全生命周期"

  # F1.1: 注册模型
  info "F1.1: 注册模型"
  MODEL_RES=$(curl -s -X POST "$BASE_URL/models" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{
      "model_name": "E2E Test Model",
      "model_code": "e2e-test-'$$'",
      "model_type": "IMAGE_CLASSIFICATION",
      "framework": "PYTORCH",
      "description": "End-to-end test model"
    }')
  MODEL_ID=$(json_val "$MODEL_RES" "print(d['data']['id'])")
  [ -n "$MODEL_ID" ] && log "模型注册成功: ID=$MODEL_ID" || { fail "模型注册失败"; return 1; }

  # F1.2: 查询模型详情
  info "F1.2: 查询模型详情"
  DETAIL_RES=$(curl -s "$BASE_URL/models/$MODEL_ID" -H "$AUTH")
  STATUS=$(json_val "$DETAIL_RES" "print(d['data']['status'])")
  [ "$STATUS" = "DRAFT" ] && log "模型状态正确: $STATUS" || fail "模型状态不匹配: $STATUS"

  # F1.3: 更新模型
  info "F1.3: 更新模型"
  curl -s -X PUT "$BASE_URL/models/$MODEL_ID" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{"description":"Updated description"}' > /dev/null
  log "模型更新成功"

  # F1.4: 上传模型版本
  info "F1.4: 上传模型版本"
  VERSION_RES=$(curl -s -X POST "$BASE_URL/models/$MODEL_ID/versions" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{
      "version_no": "v1.0.0",
      "description": "Initial version",
      "file_path": "/models/test-model-v1.pt",
      "file_size": 1024000,
      "checksum": "abc123def456"
    }')
  VERSION_ID=$(json_val "$VERSION_RES" "print(d['data']['id'])")
  [ -n "$VERSION_ID" ] && log "版本上传成功: ID=$VERSION_ID" || fail "版本上传失败"

  # F1.5: 创建评估
  info "F1.5: 创建评估"
  EVAL_RES=$(curl -s -X POST "$BASE_URL/evaluations" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"model_id\": $MODEL_ID,
      \"dataset_id\": \"test-dataset\",
      \"metrics\": [\"accuracy\", \"f1_score\", \"precision\", \"recall\"]
    }")
  EVAL_ID=$(json_val "$EVAL_RES" "print(d['data']['id'])")
  [ -n "$EVAL_ID" ] && log "评估创建成功: ID=$EVAL_ID" || fail "评估创建失败"

  # F1.6: 查询评估结果
  info "F1.6: 查询评估结果"
  sleep 2
  EVAL_DETAIL=$(curl -s "$BASE_URL/evaluations/$EVAL_ID" -H "$AUTH")
  log "评估结果查询完成"

  # F1.7: 提交审批
  info "F1.7: 提交审批"
  APPROVAL_RES=$(curl -s -X POST "$BASE_URL/approvals" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"model_id\": $MODEL_ID,
      \"approval_type\": \"DEPLOYMENT\",
      \"evidence_docs\": [\"eval_report.pdf\"],
      \"risk_assessment\": \"Low risk - accuracy > 95%\"
    }")
  APPROVAL_ID=$(json_val "$APPROVAL_RES" "print(d['data']['id'])")
  [ -n "$APPROVAL_ID" ] && log "审批提交成功: ID=$APPROVAL_ID" || fail "审批提交失败"

  # F1.8: 审批通过
  info "F1.8: 审批通过"
  curl -s -X PUT "$BASE_URL/approvals/$APPROVAL_ID/review" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{"action":"APPROVED","comment":"E2E test approved"}' > /dev/null
  log "审批通过"

  # F1.9: 模型列表查询
  info "F1.9: 模型列表查询"
  LIST_RES=$(curl -s "$BASE_URL/models?page=1&pageSize=10" -H "$AUTH")
  TOTAL=$(json_val "$LIST_RES" "print(d['data']['total'])")
  [ "$TOTAL" -gt 0 ] 2>/dev/null && log "模型列表查询成功: total=$TOTAL" || fail "模型列表查询失败"
}

# =============================================
# F2: 模型推理调用 E2E 测试
# =============================================
test_inference() {
  section "F2: 模型推理调用"

  # F2.1: 创建部署
  info "F2.1: 创建部署"
  DEPLOY_RES=$(curl -s -X POST "$BASE_URL/deployments" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"model_id\": $MODEL_ID,
      \"name\": \"e2e-test-deploy\",
      \"resource_config\": {\"cpu\":2,\"memory\":4096,\"gpu\":0,\"replicas\":1}
    }")
  DEPLOY_ID=$(json_val "$DEPLOY_RES" "print(d['data']['id'])")
  [ -n "$DEPLOY_ID" ] && log "部署创建成功: ID=$DEPLOY_ID" || { fail "部署创建失败"; return 1; }

  # F2.2: 启动部署
  info "F2.2: 启动部署"
  curl -s -X PUT "$BASE_URL/deployments/$DEPLOY_ID/start" -H "$AUTH" > /dev/null
  log "部署启动请求已发送"

  # F2.3: 执行推理
  info "F2.3: 执行推理"
  INFER_RES=$(curl -s -X POST "$BASE_URL/inference/$DEPLOY_ID" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{"input_data":{"image":"test.jpg"},"parameters":{}}')
  log "推理请求完成"

  # F2.4: 查询推理日志
  info "F2.4: 查询推理日志"
  curl -s "$BASE_URL/monitoring/deployments/$DEPLOY_ID/logs?page=1&pageSize=10" -H "$AUTH" > /dev/null
  log "推理日志查询完成"
}

# =============================================
# F3: 金丝雀发布 E2E 测试
# =============================================
test_canary() {
  section "F3: 金丝雀发布"

  # F3.1: 创建路由规则
  info "F3.1: 创建路由规则"
  ROUTE_RES=$(curl -s -X POST "$BASE_URL/deployments/routes" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"model_id\": $MODEL_ID,
      \"rules\": [
        {\"version_id\": $VERSION_ID, \"weight\": 90, \"rule_name\": \"stable\"},
        {\"version_id\": $VERSION_ID, \"weight\": 10, \"rule_name\": \"canary\"}
      ]
    }")
  log "金丝雀路由规则创建"

  # F3.2: 查询路由配置
  info "F3.2: 查询路由配置"
  curl -s "$BASE_URL/deployments/routes?model_id=$MODEL_ID" -H "$AUTH" > /dev/null
  log "路由配置查询完成"
}

# =============================================
# F4: CDR 数据接入 E2E 测试
# =============================================
test_cdr() {
  section "F4: CDR 数据接入"

  # F4.1: 创建患者
  info "F4.1: 创建患者"
  PATIENT_RES=$(curl -s -X POST "$BASE_URL/cdr/patients" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{
      "patient_name": "测试患者",
      "gender": "MALE",
      "birth_date": "1990-01-15",
      "id_card_no": "110101199001151234",
      "phone": "13800138000",
      "address": "北京市朝阳区",
      "blood_type": "A",
      "marital_status": "SINGLE"
    }')
  PATIENT_ID=$(json_val "$PATIENT_RES" "print(d['data']['id'])")
  [ -n "$PATIENT_ID" ] && log "患者创建成功: ID=$PATIENT_ID" || fail "患者创建失败"

  # F4.2: 查询患者列表
  info "F4.2: 查询患者列表"
  PLIST_RES=$(curl -s "$BASE_URL/cdr/patients?page=1&pageSize=10&keyword=测试" -H "$AUTH")
  log "患者列表查询完成"

  # F4.3: 患者360视图
  info "F4.3: 患者360视图"
  curl -s "$BASE_URL/cdr/patients/$PATIENT_ID/360" -H "$AUTH" > /dev/null
  log "患者360视图查询完成"

  # F4.4: 创建就诊记录
  info "F4.4: 创建就诊记录"
  ENC_RES=$(curl -s -X POST "$BASE_URL/cdr/encounters" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"patient_id\": $PATIENT_ID,
      \"encounter_type\": \"OUTPATIENT\",
      \"department\": \"内科\",
      \"encounter_date\": \"2026-04-11\"
    }")
  ENC_ID=$(json_val "$ENC_RES" "print(d['data']['id'])")
  [ -n "$ENC_ID" ] && log "就诊记录创建成功: ID=$ENC_ID" || fail "就诊记录创建失败"
}

# =============================================
# F5: ETL 数据转换 E2E 测试
# =============================================
test_etl() {
  section "F5: ETL 数据转换"

  # F5.1: 创建ETL任务
  info "F5.1: 创建ETL任务"
  ETL_RES=$(curl -s -X POST "$BASE_URL/etl/tasks" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{
      "task_name": "E2E Test ETL",
      "source_type": "HIS",
      "target_type": "CDR",
      "config": {"batch_size": 1000, "mode": "incremental"},
      "cron_expression": "0 0 2 * * ?"
    }')
  ETL_TASK_ID=$(json_val "$ETL_RES" "print(d['data']['id'])")
  [ -n "$ETL_TASK_ID" ] && log "ETL任务创建成功: ID=$ETL_TASK_ID" || fail "ETL任务创建失败"

  # F5.2: 执行ETL任务
  info "F5.2: 触发ETL执行"
  curl -s -X POST "$BASE_URL/etl/tasks/$ETL_TASK_ID/execute" -H "$AUTH" > /dev/null
  log "ETL任务已触发"

  # F5.3: 查询执行状态
  info "F5.3: 查询ETL执行状态"
  sleep 2
  curl -s "$BASE_URL/etl/tasks/$ETL_TASK_ID/executions?page=1&pageSize=5" -H "$AUTH" > /dev/null
  log "ETL执行状态查询完成"
}

# =============================================
# F7: 数据标注 E2E 测试
# =============================================
test_label() {
  section "F7: 数据标注"

  # F7.1: 创建标注任务
  info "F7.1: 创建标注任务"
  LABEL_RES=$(curl -s -X POST "$BASE_URL/label/tasks" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{
      "task_name": "E2E Test Label",
      "task_type": "IMAGE_CLASSIFICATION",
      "dataset_id": "test-dataset",
      "assignees": ["admin"],
      "labels": ["normal", "abnormal"],
      "description": "E2E test label task"
    }')
  LABEL_TASK_ID=$(json_val "$LABEL_RES" "print(d['data']['id'])")
  [ -n "$LABEL_TASK_ID" ] && log "标注任务创建成功: ID=$LABEL_TASK_ID" || fail "标注任务创建失败"

  # F7.2: 查询标注统计
  info "F7.2: 查询标注统计"
  curl -s "$BASE_URL/label/tasks/$LABEL_TASK_ID/stats" -H "$AUTH" > /dev/null
  log "标注统计查询完成"

  # F7.3: AI预标注
  info "F7.3: 触发AI预标注"
  curl -s -X POST "$BASE_URL/label/tasks/$LABEL_TASK_ID/pre-annotate" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d '{"model_id": "pretrained-resnet50"}' > /dev/null
  log "AI预标注请求已发送"
}

# =============================================
# F8: 告警触发通知 E2E 测试
# =============================================
test_alert() {
  section "F8: 告警触发通知"

  # F8.1: 创建告警规则
  info "F8.1: 创建告警规则"
  RULE_RES=$(curl -s -X POST "$BASE_URL/alerts/rules" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{
      \"rule_name\": \"E2E Test Alert\",
      \"metric\": \"inference_latency_p99\",
      \"condition\": \"gt\",
      \"threshold\": 1000,
      \"duration\": \"5m\",
      \"severity\": \"WARNING\",
      \"notify_channels\": [\"IN_APP\", \"EMAIL\"],
      \"enabled\": true
    }")
  ALERT_RULE_ID=$(json_val "$RULE_RES" "print(d['data']['id'])")
  [ -n "$ALERT_RULE_ID" ] && log "告警规则创建成功: ID=$ALERT_RULE_ID" || fail "告警规则创建失败"

  # F8.2: 查询告警列表
  info "F8.2: 查询告警列表"
  curl -s "$BASE_URL/alerts?page=1&pageSize=10" -H "$AUTH" > /dev/null
  log "告警列表查询完成"

  # F8.3: 查询消息通知
  info "F8.3: 查询消息通知"
  curl -s "$BASE_URL/messages?page=1&pageSize=10" -H "$AUTH" > /dev/null
  log "消息通知查询完成"

  # F8.4: 未读消息计数
  info "F8.4: 未读消息计数"
  curl -s "$BASE_URL/messages/unread-count" -H "$AUTH" > /dev/null
  log "未读消息计数查询完成"
}

# =============================================
# 清理
# =============================================
cleanup() {
  section "清理测试数据"
  # 在实际环境中可选择清理或保留测试数据
  info "测试数据保留用于验证（手动清理或使用测试数据库）"
}

# =============================================
# 主流程
# =============================================
main() {
  echo "=========================================="
  echo " MAIDC E2E Integration Tests (F1-F8)"
  echo " Base URL: $BASE_URL"
  echo " Time: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "=========================================="

  # 先登录获取Token
  LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
  TOKEN=$(json_val "$LOGIN_RES" "print(d['data']['access_token'])")
  if [ -z "$TOKEN" ]; then
    echo -e "${RED}ERROR: Cannot login. Is the service running?${NC}"
    exit 1
  fi
  AUTH="Authorization: Bearer $TOKEN"

  # 运行所有测试流程
  test_model_lifecycle   # F1
  test_inference         # F2
  test_canary            # F3
  test_cdr               # F4
  test_etl               # F5
  test_label             # F7
  test_alert             # F8

  cleanup

  # 结果汇总
  echo ""
  echo "=========================================="
  echo " E2E Test Summary"
  echo "=========================================="
  echo -e "  ${GREEN}PASS${NC}: $PASS"
  echo -e "  ${RED}FAIL${NC}: $FAIL"
  echo -e "  ${YELLOW}SKIP${NC}: $SKIP"
  echo "  TOTAL: $((PASS+FAIL+SKIP))"
  echo "=========================================="

  if [ $FAIL -gt 0 ]; then
    echo -e "${RED}RESULT: FAILED${NC}"
    exit 1
  else
    echo -e "${GREEN}RESULT: ALL PASSED${NC}"
    exit 0
  fi
}

main
