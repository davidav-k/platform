import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  improveTaskDescription,
  suggestPriority,
  suggestSubtasks,
  summarizeTask,
} from '../aiService'
import { post } from '../apiClient'

vi.mock('../apiClient', () => ({
  post: vi.fn(),
}))

describe('aiService', () => {
  const task = {
    title: 'Improve onboarding',
    description: 'Clarify the onboarding workflow.',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it.each([
    ['improve task description', improveTaskDescription, '/api/ai/tasks/description/improve'],
    ['suggest subtasks', suggestSubtasks, '/api/ai/tasks/subtasks/suggest'],
    ['summarize task', summarizeTask, '/api/ai/tasks/summary'],
    ['suggest priority', suggestPriority, '/api/ai/tasks/priority/suggest'],
  ])('uses the explicit Gateway route to %s', async (_operation, request, path) => {
    post.mockResolvedValue({ data: {} })

    await request(task)

    expect(post).toHaveBeenCalledWith(path, task)
  })
})
