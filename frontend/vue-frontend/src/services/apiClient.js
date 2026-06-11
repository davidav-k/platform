const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export function apiRequest(path, options = {}) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  return fetch(`${apiBaseUrl}${normalizedPath}`, {
    ...options,
    credentials: 'include',
  })
}

export { apiBaseUrl }
