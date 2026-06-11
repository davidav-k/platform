const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message, response, data) {
    super(message)
    this.name = 'ApiError'
    this.status = response.status
    this.statusText = response.statusText
    this.data = data
  }
}

async function parseResponse(response) {
  if (response.status === 204 || response.status === 205) {
    return null
  }

  const text = await response.text()

  if (!text) {
    return null
  }

  const contentType = response.headers.get('content-type') || ''

  if (!contentType.includes('json')) {
    return text
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function errorMessage(response, data) {
  if (data && typeof data === 'object') {
    return data.message || data.error || `API request failed with status ${response.status}`
  }

  return data || response.statusText || `API request failed with status ${response.status}`
}

export async function apiRequest(path, options = {}) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const response = await fetch(`${apiBaseUrl}${normalizedPath}`, {
    ...options,
    credentials: 'include',
  })
  const data = await parseResponse(response)

  if (!response.ok) {
    throw new ApiError(errorMessage(response, data), response, data)
  }

  return data
}

function requestWithBody(method, path, body) {
  const options = { method }

  if (body !== undefined && body !== null) {
    options.headers = { 'Content-Type': 'application/json' }
    options.body = JSON.stringify(body)
  }

  return apiRequest(path, options)
}

export function get(path) {
  return apiRequest(path, { method: 'GET' })
}

export function post(path, body) {
  return requestWithBody('POST', path, body)
}

export function put(path, body) {
  return requestWithBody('PUT', path, body)
}

export function patch(path, body) {
  return requestWithBody('PATCH', path, body)
}

export function del(path) {
  return apiRequest(path, { method: 'DELETE' })
}

export function withQueryParams(path, params = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value))
    }
  })

  const query = searchParams.toString()
  return query ? `${path}?${query}` : path
}

export { apiBaseUrl }
