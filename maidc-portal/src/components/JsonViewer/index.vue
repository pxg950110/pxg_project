<template>
  <div class="json-viewer">
    <JsonNode :data="data" :depth="0" :max-depth="maxDepth" :default-collapsed="collapsed" />
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, ref, computed, type PropType } from 'vue'

interface Props {
  data: any
  collapsed?: boolean
  maxDepth?: number
}

withDefaults(defineProps<Props>(), {
  collapsed: false,
  maxDepth: 10,
})

/* Inner recursive component */
const JsonNode = defineComponent({
  name: 'JsonNode',
  props: {
    data: { type: null as any as PropType<any>, required: true },
    depth: { type: Number, default: 0 },
    maxDepth: { type: Number, default: 10 },
    defaultCollapsed: { type: Boolean, default: false },
    keyName: { type: String, default: '' },
  },
  setup(props) {
    const isCollapsed = ref(props.defaultCollapsed && props.depth > 1)

    const dataType = computed(() => {
      if (props.data === null) return 'null'
      if (props.data === undefined) return 'undefined'
      if (Array.isArray(props.data)) return 'array'
      return typeof props.data
    })

    const isExpandable = computed(() => dataType.value === 'object' || dataType.value === 'array')

    const itemCount = computed(() => {
      if (dataType.value === 'array') return props.data.length
      if (dataType.value === 'object') return Object.keys(props.data).length
      return 0
    })

    function toggle() {
      if (isExpandable.value) isCollapsed.value = !isCollapsed.value
    }

    return () => {
      const indent = { paddingLeft: `${props.depth * 16}px` }
      const children: any[] = []

      // Key prefix
      if (props.keyName) {
        children.push(h('span', { class: 'json-key' }, `"${props.keyName}": `))
      }

      if (!isExpandable.value) {
        // Primitive rendering
        children.push(h('span', { class: `json-${dataType.value}` }, formatPrimitive(props.data)))
        return h('div', { class: 'json-line', style: indent }, children)
      }

      // Expandable toggle
      const bracket = dataType.value === 'array' ? ['[', ']'] : ['{', '}']

      children.push(
        h('span', {
          class: 'json-toggle',
          onClick: toggle,
        }, isCollapsed.value ? '\u25B6' : '\u25BC'),
        h('span', { class: 'json-bracket' }, bracket[0]),
      )

      if (isCollapsed.value) {
        children.push(
          h('span', { class: 'json-ellipsis' }, `...${itemCount.value} items`),
          h('span', { class: 'json-bracket' }, bracket[1]),
        )
        return h('div', { class: 'json-line', style: indent }, children)
      }

      // Expanded: children on new lines
      const childNodes: any[] = []
      const entries =
        dataType.value === 'array'
          ? props.data.map((v: any, i: number) => [String(i), v])
          : Object.entries(props.data)

      entries.forEach(([key, value]: [string, any], idx: number) => {
        const showKey = dataType.value === 'object'
        childNodes.push(
          h(JsonNode, {
            data: value,
            depth: props.depth + 1,
            maxDepth: props.maxDepth,
            defaultCollapsed: props.defaultCollapsed,
            keyName: showKey ? key : '',
            key: `${props.depth}-${key}-${idx}`,
          }),
        )
      })

      // Close bracket
      childNodes.push(
        h('div', { class: 'json-line', style: indent }, [
          h('span', { class: 'json-bracket' }, bracket[1]),
        ]),
      )

      return h('div', null, [h('div', { class: 'json-line', style: indent }, children), ...childNodes])
    }
  },
})

function formatPrimitive(value: any): string {
  if (value === null) return 'null'
  if (value === undefined) return 'undefined'
  if (typeof value === 'string') return `"${value}"`
  if (typeof value === 'boolean') return String(value)
  return String(value)
}
</script>

<style scoped>
.json-viewer {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #f6f8fa;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  overflow: auto;
  max-height: 600px;
}
.json-viewer :deep(.json-line) {
  white-space: nowrap;
}
.json-viewer :deep(.json-toggle) {
  cursor: pointer;
  user-select: none;
  display: inline-block;
  width: 16px;
  font-size: 10px;
  color: #999;
}
.json-viewer :deep(.json-toggle:hover) {
  color: #1677ff;
}
.json-viewer :deep(.json-key) {
  color: #0550ae;
}
.json-viewer :deep(.json-string) {
  color: #0a3069;
}
.json-viewer :deep(.json-number) {
  color: #0550ae;
}
.json-viewer :deep(.json-boolean) {
  color: #cf222e;
}
.json-viewer :deep(.json-null) {
  color: #6e7781;
}
.json-viewer :deep(.json-bracket) {
  color: #24292f;
  font-weight: 600;
}
.json-viewer :deep(.json-ellipsis) {
  color: #6e7781;
  margin: 0 4px;
}
</style>
