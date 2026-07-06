import { post } from './apiClient'

const AI_TASKS_PATH = '/api/ai/tasks'

export function improveTaskDescription(task) {
  return post(`${AI_TASKS_PATH}/description/improve`, task)
}

export function suggestSubtasks(task) {
  return post(`${AI_TASKS_PATH}/subtasks/suggest`, task)
}

export function summarizeTask(task) {
  return post(`${AI_TASKS_PATH}/summary`, task)
}

export function suggestPriority(task) {
  return post(`${AI_TASKS_PATH}/priority/suggest`, task)
}
