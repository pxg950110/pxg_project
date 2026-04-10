/**
 * 前端数据脱敏工具
 * 用于列表页面显示敏感字段
 */

/**
 * 姓名脱敏: 张三 → 张* / 欧阳修 → 欧阳*
 */
export function maskName(name: string): string {
  if (!name || name.length <= 1) return name
  if (name.length === 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

/**
 * 身份证号脱敏: 110101199001011234 → 110101****1234
 */
export function maskIdCard(id: string): string {
  if (!id || id.length < 8) return id
  return id.substring(0, 6) + '****' + id.substring(id.length - 4)
}

/**
 * 手机号脱敏: 13800138000 → 138****8000
 */
export function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

/**
 * 邮箱脱敏: admin@example.com → a***@example.com
 */
export function maskEmail(email: string): string {
  if (!email || !email.includes('@')) return email
  const [local, domain] = email.split('@', 2)
  if (local.length <= 1) return email
  return local[0] + '***@' + domain
}

/**
 * 地址脱敏: 北京市朝阳区xxx路 → 北京市朝阳区***
 */
export function maskAddress(addr: string): string {
  if (!addr || addr.length <= 6) return addr
  return addr.substring(0, 6) + '***'
}

/**
 * 通用脱敏: 保留前 prefixLen 后 suffixLen 位
 */
export function mask(value: string, prefixLen: number, suffixLen: number): string {
  if (!value) return value
  if (value.length <= prefixLen + suffixLen) return value
  const prefix = value.substring(0, prefixLen)
  const suffix = suffixLen > 0 ? value.substring(value.length - suffixLen) : ''
  return prefix + '*'.repeat(value.length - prefixLen - suffixLen) + suffix
}
