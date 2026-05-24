import { API_BASE_URL } from '../config/env'
import { clearSession, getToken } from '../storage/authStorage'

export class ApiError extends Error {
  constructor(message, { status, data, response } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
    this.response = response
  }
}

const buildUrl = (path) => {
  if (/^https?:\/\//i.test(path)) return path
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

const parseResponseBody = async (response) => {
  if (response.status === 204) return null

  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }

  return response.text()
}

const getErrorMessage = (body, fallback) => {
  if (!body) return fallback
  if (typeof body === 'string') return body || fallback
  return body.error || body.message || fallback
}

export const apiRequest = async (path, options = {}) => {
  const {
    auth = true,
    logoutOnUnauthorized = false,
    headers,
    body,
    ...fetchOptions
  } = options

  const requestHeaders = new Headers(headers || {})
  const token = getToken()

  if (auth && token && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${token}`)
  }

  if (
    body !== undefined &&
    !(body instanceof FormData) &&
    !requestHeaders.has('Content-Type')
  ) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  const response = await fetch(buildUrl(path), {
    ...fetchOptions,
    headers: requestHeaders,
    body: body instanceof FormData || typeof body === 'string' ? body : JSON.stringify(body),
  })

  const parsedBody = await parseResponseBody(response)

  if (!response.ok) {
    if (response.status === 401 && logoutOnUnauthorized) {
      clearSession()
    }

    throw new ApiError(getErrorMessage(parsedBody, `HTTP ${response.status}`), {
      status: response.status,
      data: parsedBody,
      response,
    })
  }

  return parsedBody
}

export const apiGet = (path, options) => apiRequest(path, { ...options, method: 'GET' })

export const apiPost = (path, body, options) =>
  apiRequest(path, { ...options, method: 'POST', body })

export const apiPut = (path, body, options) =>
  apiRequest(path, { ...options, method: 'PUT', body })

export const apiPatch = (path, body, options) =>
  apiRequest(path, { ...options, method: 'PATCH', body })

export const apiDelete = (path, options) => apiRequest(path, { ...options, method: 'DELETE' })
