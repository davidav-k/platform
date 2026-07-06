import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AiTaskTools from '../AiTaskTools.vue'
import {
  improveTaskDescription,
  suggestPriority,
  suggestSubtasks,
  summarizeTask,
} from '../../services/aiService'
import { ApiError } from '../../services/apiClient'

vi.mock('../../services/aiService', () => ({
  improveTaskDescription: vi.fn(),
  suggestPriority: vi.fn(),
  suggestSubtasks: vi.fn(),
  summarizeTask: vi.fn(),
}))

function mountTools(props = {}) {
  return mount(AiTaskTools, {
    props: {
      title: 'Improve onboarding',
      description: 'Make onboarding clearer for new users.',
      priority: '',
      ...props,
    },
  })
}

function buttonByText(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('AiTaskTools', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('validates required task input before calling AI endpoints', async () => {
    const wrapper = mountTools({ title: '   ' })

    await buttonByText(wrapper, 'Improve Description').trigger('click')

    expect(improveTaskDescription).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Enter a task title before using AI assistance.')
  })

  it('shows and applies an improved description without resetting other form state', async () => {
    improveTaskDescription.mockResolvedValue({
      data: {
        result: {
          improvedDescription: 'Add acceptance criteria and rollout notes.',
        },
      },
    })
    const wrapper = mountTools()

    await buttonByText(wrapper, 'Improve Description').trigger('click')
    await flushPromises()

    expect(improveTaskDescription).toHaveBeenCalledWith({
      title: 'Improve onboarding',
      description: 'Make onboarding clearer for new users.',
    })
    expect(wrapper.text()).toContain('Add acceptance criteria and rollout notes.')

    await buttonByText(wrapper, 'Apply improved description').trigger('click')

    expect(wrapper.emitted('update:description')).toEqual([
      ['Add acceptance criteria and rollout notes.'],
    ])
    expect(wrapper.emitted('update:priority')).toBeUndefined()
  })

  it('disables only the active AI action while a request is running', async () => {
    let resolveRequest
    improveTaskDescription.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )
    const wrapper = mountTools()

    await buttonByText(wrapper, 'Improve Description').trigger('click')
    await wrapper.vm.$nextTick()

    expect(buttonByText(wrapper, 'Improving...').attributes('disabled')).toBeDefined()
    expect(buttonByText(wrapper, 'Suggest Subtasks').attributes('disabled')).toBeUndefined()

    await buttonByText(wrapper, 'Improving...').trigger('click')
    expect(improveTaskDescription).toHaveBeenCalledTimes(1)

    resolveRequest({ data: { result: { improvedDescription: 'Improved text' } } })
    await flushPromises()
  })

  it('displays suggested subtasks and emits an insert description update', async () => {
    suggestSubtasks.mockResolvedValue({
      data: {
        result: {
          subtasks: ['Draft acceptance criteria', 'Review with product'],
        },
      },
    })
    const wrapper = mountTools({ description: 'Current description' })

    await buttonByText(wrapper, 'Suggest Subtasks').trigger('click')
    await flushPromises()
    await buttonByText(wrapper, 'Insert into description').trigger('click')

    expect(wrapper.text()).toContain('Draft acceptance criteria')
    expect(wrapper.emitted('update:description')).toEqual([
      ['Current description\n\nSubtasks:\n- Draft acceptance criteria\n- Review with product'],
    ])
  })

  it('displays a generated summary', async () => {
    summarizeTask.mockResolvedValue({
      data: {
        result: {
          summary: 'Improve onboarding clarity.',
        },
      },
    })
    const wrapper = mountTools()

    await buttonByText(wrapper, 'Summarize Task').trigger('click')
    await flushPromises()

    expect(summarizeTask).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Improve onboarding clarity.')
  })

  it('displays and applies a suggested priority', async () => {
    suggestPriority.mockResolvedValue({
      data: {
        result: {
          priority: 'HIGH',
          reason: 'Blocks onboarding release.',
        },
      },
    })
    const wrapper = mountTools()

    await buttonByText(wrapper, 'Suggest Priority').trigger('click')
    await flushPromises()
    await buttonByText(wrapper, 'Apply priority').trigger('click')

    expect(wrapper.text()).toContain('Blocks onboarding release.')
    expect(wrapper.emitted('update:priority')).toEqual([['HIGH']])
  })

  it('shows friendly provider errors', async () => {
    const response = new Response('{}', {
      status: 503,
      statusText: 'Service Unavailable',
      headers: { 'Content-Type': 'application/json' },
    })
    suggestPriority.mockRejectedValue(new ApiError('provider stack trace', response, {}))
    const wrapper = mountTools()

    await buttonByText(wrapper, 'Suggest Priority').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('The AI provider is unavailable. Please try again later.')
    expect(wrapper.text()).not.toContain('provider stack trace')
  })
})
