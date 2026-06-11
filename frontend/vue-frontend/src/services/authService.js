import { post } from './apiClient'

export function login(credentials) {
  return post('/api/users/login', credentials)
}

export function refreshSession() {
  return post('/api/users/refresh')
}
