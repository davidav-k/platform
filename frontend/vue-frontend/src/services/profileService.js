import { get } from './apiClient'

export function getProfile() {
  return get('/api/users/profile')
}
