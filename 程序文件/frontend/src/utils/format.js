const pad = value => String(value).padStart(2, '0')

export const formatDateTime = value => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export const formatDate = value => value ? String(value).slice(0, 10) : '—'
export const formatMoney = value => `¥ ${Number(value || 0).toFixed(2)}`
export const formatRate = value => `${Number(value || 0).toFixed(2)}%`
