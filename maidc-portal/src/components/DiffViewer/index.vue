<template>
  <div class="diff-viewer">
    <div class="diff-header">
      <div class="diff-col-header">
        <span>原始内容</span>
        <span v-if="language" class="diff-lang">{{ language }}</span>
      </div>
      <div class="diff-col-header">
        <span>新内容</span>
      </div>
    </div>
    <div class="diff-body">
      <div class="diff-column">
        <div
          v-for="(line, idx) in oldLines"
          :key="'o-' + idx"
          :class="['diff-line', getOldLineClass(idx)]"
        >
          <span class="line-num">{{ idx + 1 }}</span>
          <span class="line-prefix">{{ getOldPrefix(idx) }}</span>
          <span class="line-text">{{ line }}</span>
        </div>
      </div>
      <div class="diff-column">
        <div
          v-for="(line, idx) in newLines"
          :key="'n-' + idx"
          :class="['diff-line', getNewLineClass(idx)]"
        >
          <span class="line-num">{{ idx + 1 }}</span>
          <span class="line-prefix">{{ getNewPrefix(idx) }}</span>
          <span class="line-text">{{ line }}</span>
        </div>
      </div>
    </div>
    <div class="diff-stats">
      <span class="stat-added">+{{ addedCount }} 添加</span>
      <span class="stat-removed">-{{ removedCount }} 删除</span>
      <span class="stat-unchanged">~{{ unchangedCount }} 未变</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface Props {
  oldContent: string
  newContent: string
  language?: string
}

const props = defineProps<Props>()

const oldLines = computed(() => props.oldContent.split('\n'))
const newLines = computed(() => props.newContent.split('\n'))

// Simple LCS-based diff computation
interface DiffResult {
  oldType: ('unchanged' | 'removed' | 'empty')[]
  newType: ('unchanged' | 'added' | 'empty')[]
}

const diff = computed<DiffResult>(() => {
  const oLines = oldLines.value
  const nLines = newLines.value

  // Build LCS table
  const m = oLines.length
  const n = nLines.length
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (oLines[i - 1] === nLines[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  // Backtrack to find diff
  const oldType: ('unchanged' | 'removed' | 'empty')[] = []
  const newType: ('unchanged' | 'added' | 'empty')[] = []

  let i = m, j = n
  const ops: Array<{ op: string; oi: number; ni: number }> = []

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && oLines[i - 1] === nLines[j - 1]) {
      ops.push({ op: 'eq', oi: i - 1, ni: j - 1 })
      i--; j--
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      ops.push({ op: 'add', oi: -1, ni: j - 1 })
      j--
    } else {
      ops.push({ op: 'del', oi: i - 1, ni: -1 })
      i--
    }
  }

  ops.reverse()

  // Reconstruct paired lines
  for (const op of ops) {
    if (op.op === 'eq') {
      oldType.push('unchanged')
      newType.push('unchanged')
    } else if (op.op === 'add') {
      oldType.push('empty')
      newType.push('added')
    } else {
      oldType.push('removed')
      newType.push('empty')
    }
  }

  return { oldType, newType }
})

// Track the diff index per visual line in each column
const oldLineMapping = computed(() => {
  const map: number[] = []
  let dIdx = 0
  for (const t of diff.value.oldType) {
    if (t !== 'empty') {
      map.push(dIdx)
    } else {
      map.push(-1)
    }
    dIdx++
  }
  return map
})

const newLineMapping = computed(() => {
  const map: number[] = []
  let dIdx = 0
  for (const t of diff.value.newType) {
    if (t !== 'empty') {
      map.push(dIdx)
    } else {
      map.push(-1)
    }
    dIdx++
  }
  return map
})

function getOldLineClass(idx: number): string {
  const di = oldLineMapping.value[idx]
  if (di < 0) return ''
  return diff.value.oldType[di] === 'removed' ? 'line-removed' : ''
}

function getNewLineClass(idx: number): string {
  const di = newLineMapping.value[idx]
  if (di < 0) return ''
  return diff.value.newType[di] === 'added' ? 'line-added' : ''
}

function getOldPrefix(idx: number): string {
  const di = oldLineMapping.value[idx]
  if (di < 0) return ' '
  return diff.value.oldType[di] === 'removed' ? '-' : ' '
}

function getNewPrefix(idx: number): string {
  const di = newLineMapping.value[idx]
  if (di < 0) return ' '
  return diff.value.newType[di] === 'added' ? '+' : ' '
}

const addedCount = computed(() => diff.value.newType.filter((t) => t === 'added').length)
const removedCount = computed(() => diff.value.oldType.filter((t) => t === 'removed').length)
const unchangedCount = computed(() => diff.value.oldType.filter((t) => t === 'unchanged').length)
</script>

<style scoped>
.diff-viewer {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
}
.diff-header {
  display: flex;
}
.diff-col-header {
  flex: 1;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  gap: 8px;
}
.diff-col-header + .diff-col-header {
  border-left: 1px solid #f0f0f0;
}
.diff-lang {
  font-size: 10px;
  padding: 1px 4px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
}
.diff-body {
  display: flex;
  max-height: 500px;
  overflow: auto;
}
.diff-column {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}
.diff-column + .diff-column {
  border-left: 1px solid #f0f0f0;
}
.diff-line {
  display: flex;
  line-height: 1.6;
  border-bottom: 1px solid #f5f5f5;
}
.line-num {
  width: 40px;
  text-align: right;
  padding: 0 8px;
  color: rgba(0, 0, 0, 0.25);
  user-select: none;
  flex-shrink: 0;
  font-size: 12px;
}
.line-prefix {
  width: 16px;
  text-align: center;
  flex-shrink: 0;
  font-weight: 600;
}
.line-text {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
  min-width: 0;
}
.line-removed {
  background: #fff1f0;
}
.line-removed .line-prefix {
  color: #ff4d4f;
}
.line-added {
  background: #f6ffed;
}
.line-added .line-prefix {
  color: #52c41a;
}
.diff-stats {
  display: flex;
  gap: 16px;
  padding: 6px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
}
.stat-added {
  color: #52c41a;
}
.stat-removed {
  color: #ff4d4f;
}
.stat-unchanged {
  color: rgba(0, 0, 0, 0.45);
}
</style>
