import { ref, onUnmounted, readonly } from 'vue'

interface WebSocketOptions {
  /** Auto-reconnect on disconnect (default: true) */
  autoReconnect?: boolean
  /** Max reconnection attempts before giving up (default: 5) */
  maxRetries?: number
  /** Delay in ms between reconnection attempts (default: 3000) */
  reconnectInterval?: number
  /** Protocols to pass to WebSocket constructor */
  protocols?: string | string[]
}

export function useWebSocket(url: string, options: WebSocketOptions = {}) {
  const {
    autoReconnect = true,
    maxRetries = 5,
    reconnectInterval = 3000,
    protocols,
  } = options

  const status = ref<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected')
  const lastMessage = ref<MessageEvent | null>(null)
  const retryCount = ref(0)

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  function connect() {
    if (ws) {
      ws.close()
    }

    // Build WebSocket URL - if relative, build from current location
    const wsUrl = url.startsWith('ws') ? url : buildWsUrl(url)

    status.value = 'connecting'

    try {
      ws = protocols ? new WebSocket(wsUrl, protocols) : new WebSocket(wsUrl)
    } catch (err) {
      status.value = 'error'
      return
    }

    ws.onopen = () => {
      status.value = 'connected'
      retryCount.value = 0
    }

    ws.onmessage = (event: MessageEvent) => {
      lastMessage.value = event
    }

    ws.onclose = () => {
      status.value = 'disconnected'

      if (autoReconnect && retryCount.value < maxRetries) {
        retryCount.value++
        reconnectTimer = setTimeout(() => {
          connect()
        }, reconnectInterval)
      }
    }

    ws.onerror = () => {
      status.value = 'error'
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    retryCount.value = maxRetries // Prevent auto-reconnect
    if (ws) {
      ws.close()
      ws = null
    }
    status.value = 'disconnected'
  }

  function send(data: string | ArrayBuffer | Blob | ArrayBufferView) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(data)
      return true
    }
    return false
  }

  function sendJson(data: any) {
    return send(JSON.stringify(data))
  }

  function buildWsUrl(path: string): string {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${proto}//${window.location.host}${path}`
  }

  // Auto-connect
  connect()

  // Cleanup on unmount
  onUnmounted(() => {
    disconnect()
  })

  return {
    status: readonly(status),
    lastMessage: readonly(lastMessage),
    retryCount: readonly(retryCount),
    connect,
    disconnect,
    send,
    sendJson,
  }
}
