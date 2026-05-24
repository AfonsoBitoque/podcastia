import { API_BASE_URL } from '../config/env'

export const resolveProfilePicture = (path, userId) => {
  const safePath = String(path || '').trim()
  if (!safePath) return ''
  if (/^(https?:|blob:|data:)/i.test(safePath)) return safePath
  if (userId) {
    return `${API_BASE_URL}/users/${userId}/profile-image`
  }

  const normalizedPath = safePath.replace(/^\/+/, '')
  return `${API_BASE_URL}/${normalizedPath}`
}

export const resolveAssetUrl = (path) => {
  const safePath = String(path || '').trim()
  if (!safePath) return ''
  if (/^(https?:|blob:|data:)/i.test(safePath)) return safePath
  return `${API_BASE_URL}${safePath.startsWith('/') ? safePath : `/${safePath}`}`
}

export const getInitial = (name) =>
  String(name || '?')
    .trim()
    .charAt(0)
    .toUpperCase()
