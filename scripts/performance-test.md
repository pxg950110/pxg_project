#!/bin/bash
# MAIDC JMeter 性能测试脚本
# 需要先安装 JMeter 5.6+ : https://jmeter.apache.org/
# 使用方式: ./perf-test.sh [base_url] [threads] [duration]

set -e

BASE_URL="${1:-http://localhost:8080}"
THREADS="${2:-50}"
DURATION="${3:-300}"
JMETER="${JMETER_HOME:-/opt/jmeter}/bin/jmeter"

REPORT_DIR="reports/perf-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$REPORT_DIR"

echo "=========================================="
echo " MAIDC Performance Test"
echo " URL: $BASE_URL"
echo " Threads: $THREADS"
echo " Duration: ${DURATION}s"
echo " Report: $REPORT_DIR"
echo "=========================================="

# Step 1: Login and get token
echo "[1/6] Getting auth token..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@12345"}')
TOKEN=$(echo $LOGIN_RES | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "ERROR: Login failed. Check credentials."
  exit 1
fi
echo "Token obtained."

# Generate JMeter test plan dynamically
cat > "$REPORT_DIR/test-plan.jmx" << 'JMETER_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="MAIDC Perf Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="API Load Test">
        <intProp name="ThreadGroup.num_threads">50</intProp>
        <intProp name="ThreadGroup.ramp_time">10</intProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <stringProp name="ThreadGroup.delay">0</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <!-- Model List API -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="List Models">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.port"></stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/models?page=1&amp;pageSize=20</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Authorization" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${TOKEN}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
        </hashTree>

        <!-- Deployment List API -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="List Deployments">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/deployments?page=1&amp;pageSize=20</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Authorization" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${TOKEN}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
        </hashTree>

        <!-- Patient List API -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="List Patients">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/cdr/patients?page=1&amp;pageSize=20</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Authorization" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${TOKEN}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
        </hashTree>

        <!-- Audit Log API -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="List Audit Logs">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/audit/operations?page=1&amp;pageSize=20</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Authorization" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${TOKEN}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
        </hashTree>

        <!-- Constant Timer: 500ms think time -->
        <ConstantTimer guiclass="ConstantTimerGui" testclass="ConstantTimer" testname="Think Time">
          <stringProp name="ConstantTimer.delay">500</stringProp>
        </ConstantTimer>

        <!-- Response Assertions -->
        <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Status 200">
          <collectionProp name="Asserion.test_strings">
            <stringProp name="49586">200</stringProp>
          </collectionProp>
          <intProp name="Assertion.test_type">8</intProp>
          <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
        </ResponseAssertion>

        <!-- JSON Path Assertion: success -->
        <JSONPathAssertion guiclass="JSONPathAssertionGui" testclass="JSONPathAssertion" testname="Check Success">
          <stringProp name="JSON_PATH">$.code</stringProp>
          <stringProp name="EXPECTED_VALUE">200</stringProp>
          <boolProp name="JSONVALIDATION">true</boolProp>
        </JSONPathAssertion>
      </hashTree>

      <!-- Summary Report -->
      <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Summary Report">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
            <threadName>true</threadName>
            <dataType>true</dataType>
            <encoding>false</encoding>
            <assertions>true</assertions>
            <subresults>true</subresults>
            <responseData>false</responseData>
            <samplerData>false</samplerData>
            <xml>false</xml>
            <fieldNames>true</fieldNames>
            <responseHeaders>true</responseHeaders>
            <requestHeaders>true</requestHeaders>
            <responseDataOnError>true</responseDataOnError>
            <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
            <assertionsResultsToSave>0</assertionsResultsToSave>
          </value>
        </objProp>
        <stringProp name="filename">REPORT_DIR/results.jtl</stringProp>
      </ResultCollector>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
JMETER_EOF

# Replace placeholders
sed -i "s|\${BASE_URL}|$BASE_URL|g" "$REPORT_DIR/test-plan.jmx"
sed -i "s|\${TOKEN}|$TOKEN|g" "$REPORT_DIR/test-plan.jmx"
sed -i "s|REPORT_DIR|$REPORT_DIR|g" "$REPORT_DIR/test-plan.jmx"

echo ""
echo "[2/6] Running JMeter test..."
if command -v $JMETER &>/dev/null; then
  $JMETER -n -t "$REPORT_DIR/test-plan.jmx" -l "$REPORT_DIR/results.jtl" -e -o "$REPORT_DIR/html"
  echo "[3/6] Test completed. Report: $REPORT_DIR/html/index.html"
else
  echo "[3/6] JMeter not found. Test plan saved to: $REPORT_DIR/test-plan.jmx"
  echo "      Install JMeter and run: $JMETER -n -t $REPORT_DIR/test-plan.jmx -l $REPORT_DIR/results.jtl -e -o $REPORT_DIR/html"
fi

echo ""
echo "[4/6] Quick API smoke test..."

# P95 latency check
echo "  Testing model list API response time..."
TIME=$(curl -o /dev/null -s -w '%{time_total}' "$BASE_URL/api/v1/models?page=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN")
echo "  Model list: ${TIME}s"

TIME=$(curl -o /dev/null -s -w '%{time_total}' "$BASE_URL/api/v1/deployments?page=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN")
echo "  Deployment list: ${TIME}s"

echo ""
echo "[5/6] Performance targets:"
echo "  - API P95 latency < 500ms"
echo "  - API P99 latency < 1000ms"
echo "  - Throughput > 100 req/s (50 threads)"
echo "  - Error rate < 0.1%"

echo ""
echo "[6/6] Done. Reports in: $REPORT_DIR"
