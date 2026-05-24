export const isPlainObject = (value) =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value)

export const asArray = (value) => {
  if (Array.isArray(value)) return value
  if (!isPlainObject(value)) return []

  const nestedKeys = ['content', 'items', 'data', 'results', 'messages', 'logs', 'users', 'podcasts']
  for (const key of nestedKeys) {
    if (Array.isArray(value[key])) return value[key]
  }

  return []
}

export const safeText = (value, fallback = '') => {
  const text = String(value ?? '').trim()
  return text || fallback
}

export const toFiniteNumber = (value, fallback = 0) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}
