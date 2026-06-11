import { post } from './apiClient'

export function login(credentials) {
  return post('/api/users/login', credentials)
}

export function refreshSession() {
  return post('/api/users/refresh')
}

// The backend has no public logout endpoint yet. This keeps the UI contract
// explicit while authState clears only the in-memory frontend session.
export function logout() {
  return Promise.resolve()
}
