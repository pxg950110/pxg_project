# Task 13: Update EtlConfigGenerator for Graph Topology

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlConfigGenerator.java`

- [ ] **Step 1: Add graph-based generation method**

In `EtlConfigGenerator.java`, add a new method that generates Embulk YAML from the graph structure:

```java
public String generateFromGraph(Map<String, Object> graph) {
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.get("nodes");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");

    if (nodes == null || nodes.isEmpty()) {
        return "# No steps configured\n";
    }

    // Build adjacency: find input nodes (no incoming edges)
    Set<String> nodesWithIncoming = new HashSet<>();
    for (Map<String, Object> edge : edges) {
        nodesWithIncoming.add((String) edge.get("target"));
    }

    // Find root nodes (no incoming edges = input sources)
    List<Map<String, Object>> rootNodes = nodes.stream()
        .filter(n -> !nodesWithIncoming.contains(n.get("id")))
        .collect(Collectors.toList());

    StringBuilder yaml = new StringBuilder();

    // Generate config for each root node chain
    for (Map<String, Object> rootNode : rootNodes) {
        generateChainYaml(rootNode, nodes, edges, yaml, 0);
    }

    return yaml.toString();
}

private void generateChainYaml(
        Map<String, Object> node,
        List<Map<String, Object>> allNodes,
        List<Map<String, Object>> edges,
        StringBuilder yaml,
        int depth) {

    Map<String, Object> data = (Map<String, Object>) node.get("data");
    String nodeType = (String) data.get("nodeType");
    Map<String, Object> config = (Map<String, Object>) data.get("config");

    if (config == null) config = Map.of();

    switch (nodeType) {
        case "TABLE_INPUT":
            yaml.append("in:\n");
            yaml.append("  type: postgresql\n");
            yaml.append("  host: ${env:DB_HOST:localhost}\n");
            yaml.append("  user: ${env:DB_USER:maidc}\n");
            yaml.append("  password: ${env:DB_PASS:maidc123}\n");
            yaml.append("  database: ${env:DB_NAME:maidc}\n");
            yaml.append("  schema: ").append(config.getOrDefault("schema", "public")).append("\n");
            yaml.append("  table: ").append(config.getOrDefault("table", "")).append("\n");
            if (config.get("where") != null && !config.get("where").toString().isEmpty()) {
                yaml.append("  where: ").append(config.get("where")).append("\n");
            }
            yaml.append("\n");
            break;

        case "CSV_INPUT":
            yaml.append("in:\n");
            yaml.append("  type: file\n");
            yaml.append("  path_prefix: ").append(config.getOrDefault("filePath", "")).append("\n");
            yaml.append("  parser:\n");
            yaml.append("    type: csv\n");
            yaml.append("    delimiter: ").append(config.getOrDefault("delimiter", ",")).append("\n");
            yaml.append("    charset: ").append(config.getOrDefault("encoding", "UTF-8")).append("\n");
            yaml.append("\n");
            break;

        case "FILTER":
            yaml.append("filters:\n");
            yaml.append("  - type: row\n");
            yaml.append("    where: ").append(config.getOrDefault("condition", "1=1")).append("\n");
            yaml.append("\n");
            break;

        case "TABLE_OUTPUT":
            yaml.append("out:\n");
            yaml.append("  type: postgresql\n");
            yaml.append("  host: ${env:DB_HOST:localhost}\n");
            yaml.append("  user: ${env:DB_USER:maidc}\n");
            yaml.append("  password: ${env:DB_PASS:maidc123}\n");
            yaml.append("  database: ${env:DB_NAME:maidc}\n");
            yaml.append("  schema: ").append(config.getOrDefault("schema", "public")).append("\n");
            yaml.append("  table: ").append(config.getOrDefault("table", "")).append("\n");
            String mode = (String) config.getOrDefault("writeMode", "insert");
            if ("truncate".equals(mode)) {
                yaml.append("  mode: truncate_insert\n");
            } else if ("upsert".equals(mode)) {
                yaml.append("  mode: merge\n");
            } else {
                yaml.append("  mode: insert\n");
            }
            yaml.append("\n");
            break;

        case "CSV_OUTPUT":
            yaml.append("out:\n");
            yaml.append("  type: file\n");
            yaml.append("  path_prefix: ").append(config.getOrDefault("filePath", "")).append("\n");
            yaml.append("  formatter:\n");
            yaml.append("    type: csv\n");
            yaml.append("    delimiter: ").append(config.getOrDefault("delimiter", ",")).append("\n");
            yaml.append("    charset: ").append(config.getOrDefault("encoding", "UTF-8")).append("\n");
            yaml.append("\n");
            break;

        case "JOIN":
        case "AGGREGATE":
        case "VALUE_MAP":
        case "EXPRESSION":
        case "DATE_FMT":
        case "CONSTANT":
        case "LOOKUP":
            // These nodes generate SQL pre/post processing
            yaml.append("# ").append(nodeType).append(" node - requires SQL preprocessing\n");
            yaml.append("# Config: ").append(config).append("\n\n");
            break;
    }

    // Follow edges to child nodes
    String nodeId = (String) node.get("id");
    for (Map<String, Object> edge : edges) {
        if (nodeId.equals(edge.get("source"))) {
            String targetId = (String) edge.get("target");
            allNodes.stream()
                .filter(n -> targetId.equals(n.get("id")))
                .findFirst()
                .ifPresent(child -> generateChainYaml(child, allNodes, edges, yaml, depth + 1));
        }
    }
}
```

- [ ] **Step 2: Add required imports**

Ensure the file has:
```java
import java.util.*;
import java.util.stream.*;
```

- [ ] **Step 3: Verify backend compiles**

```bash
cd E:/pxg_project/maidc-portal/../maidc-parent
mvn compile -pl maidc-data -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlConfigGenerator.java
git commit -m "feat(etl): add graph-based Embulk YAML generation with topology traversal"
```
