import { API_BASE_URL } from '../config/env'

export const resolveProfilePicture = (path, userId) => {
  const safePath = String(path || '').trim()
  if (!safePath) return ''
  if (/^https?:\/\//i.test(safePath)) return safePath
  if (userId) {
    return `${API_BASE_URL}/users/${userId}/profile-image?t=${Date.now()}`
  }

  const normalizedPath = safePath.replace(/^\/+/, '')
  const separator = normalizedPath.includes('?') ? '&' : '?'
  return `${API_BASE_URL}/${normalizedPath}${separator}t=${Date.now()}`
}

export const resolveAssetUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

export const getInitial = (name) =>
  String(name || '?')
    .trim()
    .charAt(0)
    .toUpperCase()
