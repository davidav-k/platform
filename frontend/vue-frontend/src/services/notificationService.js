import { get, withQueryParams } from './apiClient'

const NOTIFICATIONS_PATH = '/api/notifications'

export function getNotifications(filters = {}) {
  return get(withQueryParams(NOTIFICATIONS_PATH, filters))
}

export function getNotification(notificationId) {
  return get(`${NOTIFICATIONS_PATH}/${encodeURIComponent(notificationId)}`)
}
