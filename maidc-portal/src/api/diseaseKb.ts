import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ==================== 专病知识库 API ====================
// 后端: DiseaseKnowledgeController (/api/v1/cdr/disease-kb)

// ---- 知识空间 ----

export function getDiseaseKbSpaces(params: {
  page?: number
  page_size?: number
  keyword?: string
  status?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/disease-kb/spaces', { params })
}

export function getDiseaseKbSpace(id: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/disease-kb/spaces/${id}`)
}

export function createDiseaseKbSpace(data: any) {
  return request.post<ApiResponse<any>>('/cdr/disease-kb/spaces', data)
}

export function updateDiseaseKbSpace(id: number | string, data: any) {
  return request.put<ApiResponse<any>>(`/cdr/disease-kb/spaces/${id}`, data)
}

export function deleteDiseaseKbSpace(id: number | string) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-kb/spaces/${id}`)
}

// 按队列查知识空间（专病详情页跳转用；未关联 data 为 null）
export function getSpaceByCohort(cohortId: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/disease-kb/cohorts/${cohortId}/space`)
}

// ---- 知识条目 ----

export function getDiseaseKbItems(spaceId: number | string, params: {
  page?: number
  page_size?: number
  item_type?: string
  status?: string
  keyword?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/disease-kb/spaces/${spaceId}/items`, { params })
}

export function getDiseaseKbItem(id: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/disease-kb/items/${id}`)
}

export function createDiseaseKbItem(spaceId: number | string, data: any) {
  return request.post<ApiResponse<any>>(`/cdr/disease-kb/spaces/${spaceId}/items`, data)
}

export function updateDiseaseKbItem(id: number | string, data: any) {
  return request.put<ApiResponse<any>>(`/cdr/disease-kb/items/${id}`, data)
}

export function deleteDiseaseKbItem(id: number | string) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-kb/items/${id}`)
}

export function publishDiseaseKbItem(id: number | string, action: 'PUBLISH' | 'ARCHIVE') {
  return request.post<ApiResponse<void>>(`/cdr/disease-kb/items/${id}/publish`, { action })
}

export function recomputeDiseaseKbItem(id: number | string) {
  return request.post<ApiResponse<void>>(`/cdr/disease-kb/items/${id}/recompute`)
}

// ---- 跨空间检索 ----

export function searchDiseaseKb(params: {
  keyword: string
  space_id?: number | string
  page?: number
  page_size?: number
}) {
  return request.get<ApiResponse<any[]>>('/cdr/disease-kb/search', { params })
}

// ---- AI 问答 ----

export function getQaSessions(spaceId: number | string) {
  return request.get<ApiResponse<any[]>>('/cdr/disease-kb/qa/sessions', { params: { space_id: spaceId } })
}

export function createQaSession(spaceId: number | string) {
  return request.post<ApiResponse<any>>('/cdr/disease-kb/qa/sessions', { space_id: spaceId })
}

export function getQaMessages(sessionId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/disease-kb/qa/sessions/${sessionId}/messages`)
}

export function deleteQaSession(sessionId: number | string) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-kb/qa/sessions/${sessionId}`)
}

/**
 * SSE 流式提问：POST /qa/ask，服务端以 text/event-stream 返回
 * 事件: delta(token 片段) / citations(引用列表) / done / error
 * 当前 AI 切片未接入：后端返回 5034，走 onError 提示
 */
export async function askDiseaseKb(
  sessionId: number | string,
  question: string,
  handlers: {
    onDelta?: (t: string) => void
    onCitations?: (c: any[]) => void
    onDone?: () => void
    onError?: (msg: string) => void
  },
) {
  let resp: Response
  try {
    resp = await fetch(`/api/v1/cdr/disease-kb/qa/ask`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ session_id: sessionId, question }),
    })
  } catch {
    handlers.onError?.('AI 服务连接失败，请稍后重试')
    return
  }
  if (!resp.ok || !resp.body) {
    // 非 SSE 响应（如 5034 降级 / 403），尽力解析错误信息
    let msg = `AI 服务暂不可用 (${resp.status})`
    try {
      const body = await resp.json()
      if (body?.message) msg = body.message
    } catch { /* 非 JSON 响应，用默认文案 */ }
    handlers.onError?.(msg)
    return
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    const events = buf.split('\n\n')
    buf = events.pop() ?? ''
    for (const ev of events) {
      const dataLine = ev.split('\n').find(l => l.startsWith('data:'))
      if (!dataLine) continue
      try {
        const payload = JSON.parse(dataLine.slice(5))
        if (payload.type === 'delta') handlers.onDelta?.(payload.text)
        else if (payload.type === 'citations') handlers.onCitations?.(payload.citations)
        else if (payload.type === 'done') handlers.onDone?.()
        else if (payload.type === 'error') handlers.onError?.(payload.message)
      } catch { /* 忽略半包 */ }
    }
  }
  handlers.onDone?.()
}
