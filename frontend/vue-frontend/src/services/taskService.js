import { del, get, patch, post, withQueryParams } from './apiClient'

const TASKS_PATH = '/api/tasks'

export function getTasks(filters = {}) {
  return get(withQueryParams(TASKS_PATH, filters))
}

export function createTask(task) {
  return post(TASKS_PATH, task)
}

export function getTask(taskId) {
  return get(`${TASKS_PATH}/${encodeURIComponent(taskId)}`)
}

export function updateTask(taskId, updates) {
  return patch(`${TASKS_PATH}/${encodeURIComponent(taskId)}`, updates)
}

export function updateTaskStatus(taskId, statusUpdate) {
  return patch(`${TASKS_PATH}/${encodeURIComponent(taskId)}/status`, statusUpdate)
}

export function assignTask(taskId, assignment) {
  return patch(`${TASKS_PATH}/${encodeURIComponent(taskId)}/assignee`, assignment)
}

export function deleteTask(taskId) {
  return del(`${TASKS_PATH}/${encodeURIComponent(taskId)}`)
}
