const clean = value => String(value || '').trim()

export const maskPhone = value => {
  const text = clean(value)
  return /^\d{11}$/.test(text) ? `${text.slice(0, 3)}****${text.slice(-4)}` : text || '—'
}

export const maskIdCard = value => {
  const text = clean(value)
  if (text.length < 8) return text || '—'
  return `${text.slice(0, 3)}${'*'.repeat(Math.max(4, text.length - 7))}${text.slice(-4)}`
}
