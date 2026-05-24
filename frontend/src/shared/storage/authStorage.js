const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const notifyAuthChange = () => {
  window.dispatchEvent(new Event('auth-change'))
}

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''

export const getStoredUser = () => {
  try {
    const rawUser = localStorage.getItem(USER_KEY)
    return rawUser ? JSON.parse(rawUser) : null
  } catch {
    return null
  }
}

export const saveSession = (token, user) => {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  notifyAuthChange()
}

export const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  notifyAuthChange()
}

export const updateStoredUser = (patch) => {
  const storedUser = getStoredUser()
  if (!storedUser) return null

  const nextUser = {
    ...storedUser,
    ...patch,
  }

  localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  notifyAuthChange()
  return nextUser
}
