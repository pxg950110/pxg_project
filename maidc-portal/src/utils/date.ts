import dayjs from 'dayjs'

export function formatDateTime(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDate(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD')
}

export function fromNow(date: string | Date): string {
  return dayjs(date).fromNow()
}
