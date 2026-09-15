const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
})
const dayFormatter = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
const moneyFormatter = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 0, maximumFractionDigits: 2 })
const rateFormatter = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })

const parseDate = value => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export const formatDateTime = value => {
  const date = parseDate(value)
  return date ? dateFormatter.format(date).replaceAll('/', '-') : value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

export const formatDate = value => {
  const date = parseDate(value)
  return date ? dayFormatter.format(date).replaceAll('/', '-') : value ? String(value).slice(0, 10) : '—'
}
export const formatMoney = value => moneyFormatter.format(Number(value || 0)).replace('CN¥', '¥')
export const formatRate = value => `${rateFormatter.format(Number(value || 0))}%`
