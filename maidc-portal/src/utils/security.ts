/**
 * XSS 防护工具
 * 前端输入净化和输出转义
 */

const XSS_REGEX = /<script[^>]*>[\s\S]*?<\/script>|javascript\s*:|on\w+\s*=|<iframe[^>]*>[\s\S]*?<\/iframe>/gi

/**
 * HTML 转义
 */
export function escapeHtml(str: string): string {
  if (!str) return str
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;'
  }
  return str.replace(/[&<>"'/]/g, (char) => map[char] || char)
}

/**
 * 检测是否包含 XSS 攻击向量
 */
export function containsXss(str: string): boolean {
  if (!str) return false
  return XSS_REGEX.test(str)
}

/**
 * 净化输入：移除危险标签
 */
export function sanitize(str: string): string {
  if (!str) return str
  return str.replace(XSS_REGEX, '').trim()
}

/**
 * 净化 URL（防止 javascript: 协议）
 */
export function sanitizeUrl(url: string): string {
  if (!url) return url
  const trimmed = url.trim().toLowerCase()
  if (trimmed.startsWith('javascript:') || trimmed.startsWith('data:text/html')) {
    return ''
  }
  return url
}
