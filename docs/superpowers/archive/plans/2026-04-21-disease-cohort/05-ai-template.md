# 05 — AI 推荐 + 模板集成

**Goal:** 实现疾病名称 AutoComplete 匹配模板库、AI 推荐纳入条件接口、前端集成。

**Files:**
- Create: `maidc-data/src/main/java/com/maidc/data/controller/DiseaseTemplateController.java`
- Create: `maidc-data/src/main/java/com/maidc/data/service/DiseaseAiService.java`
- Modify: `maidc-portal/src/views/data-cdr/DiseaseList.vue`（弹窗集成 AutoComplete + AI 按钮）
- Modify: `maidc-portal/src/api/data.ts`（追加 AI 推荐 API）

---

## Task 5.1: 模板搜索后端

- [ ] **Step 1: 创建 DiseaseTemplateController**

```java
@RestController
@RequestMapping("/api/v1/dict/disease-templates")
@RequiredArgsConstructor
public class DiseaseTemplateController {
    private final DiseaseTemplateRepository repository;

    @GetMapping
    public R<List<DiseaseTemplateEntity>> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return R.ok(repository.findByIsDeletedFalse(PageRequest.of(0, 20)).getContent());
        }
        return R.ok(repository.findByDiseaseNameContainingAndIsDeletedFalse(q));
    }
}
```

## Task 5.2: AI 推荐服务

- [ ] **Step 2: 创建 DiseaseAiService**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseAiService {

    private final RestTemplate restTemplate;

    public Map<String, Object> suggestRules(String diseaseName) {
        // 调用 MAIDC 推理服务或外部 LLM API
        // Prompt: 根据疾病名称返回 ICD-10 编码范围、相关检验、用药、手术的结构化 JSON
        // 解析响应为 inclusion_rules groups 结构
        // 异常时返回空 groups + confidence 0
        try {
            // TODO: 接入实际 AI 服务，当前返回空结果
            return Map.of("groups", List.of(), "confidence", 0, "source", "AI");
        } catch (Exception e) {
            log.warn("AI suggest failed for: {}", diseaseName, e);
            return Map.of("groups", List.of(), "confidence", 0, "source", "AI");
        }
    }
}
```

在 DiseaseCohortController 中追加：

```java
@PostMapping("/ai-suggest")
public R<Map<String, Object>> aiSuggest(@RequestBody Map<String, String> req) {
    return R.ok(diseaseAiService.suggestRules(req.get("disease_name")));
}
```

## Task 5.3: 前端集成

- [ ] **Step 3: 在 `api/data.ts` 追加**

```typescript
export function aiSuggestDiseaseRules(diseaseName: string) {
  return request.post<ApiResponse<{ groups: any[]; confidence: number; source: string }>>('/cdr/disease-cohorts/ai-suggest', { disease_name: diseaseName })
}
```

- [ ] **Step 4: 修改 DiseaseList.vue 弹窗**

将专病名称 input 改为 `a-auto-complete`：
- 数据源绑定 searchDiseaseTemplates API 搜索结果
- 选中模板时自动填充 inclusion_rules 到 ConditionBuilder
- 未匹配时显示"AI 推荐"按钮
- 点击 AI 按钮调用 aiSuggestDiseaseRules，loading 状态，展示推荐结果 + 置信度标签
- 管理员可一键采纳或手动调整后确认

- [ ] **Step 5: 浏览器验证**

- 输入"糖尿" → AutoComplete 下拉出现模板 → 选中后条件自动填充
- 输入"罕见病XYZ" → 无匹配 → 点击 AI 推荐 → loading → 展示推荐结果

- [ ] **Step 6: Commit**

```bash
git add maidc-data/src/main/java/com/maidc/data/controller/DiseaseTemplateController.java
git add maidc-data/src/main/java/com/maidc/data/service/DiseaseAiService.java
git add maidc-portal/src/api/data.ts
git add maidc-portal/src/views/data-cdr/DiseaseList.vue
git commit -m "feat(disease): integrate template autocomplete and AI rule suggestion"
```
