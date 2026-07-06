<script setup>
import { computed, reactive, ref } from 'vue'

import { getUserFriendlyError } from '../services/apiClient'
import {
  improveTaskDescription,
  suggestPriority,
  suggestSubtasks,
  summarizeTask,
} from '../services/aiService'

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  priority: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:description', 'update:priority'])

const loadingActions = reactive({})
const error = ref(null)
const improvedDescription = ref('')
const suggestedSubtasks = ref([])
const summary = ref('')
const suggestedPriority = ref(null)

const taskInput = computed(() => ({
  title: props.title.trim(),
  description: props.description.trim(),
}))

function isActionLoading(action) {
  return Boolean(loadingActions[action])
}

function validateAiInput() {
  if (!taskInput.value.title) {
    error.value = 'Enter a task title before using AI assistance.'
    return false
  }

  if (!taskInput.value.description) {
    error.value = 'Enter a task description before using AI assistance.'
    return false
  }

  if (taskInput.value.title.length > 200) {
    error.value = 'Task title must not exceed 200 characters.'
    return false
  }

  if (taskInput.value.description.length > 5000) {
    error.value = 'Task description must not exceed 5000 characters.'
    return false
  }

  return true
}

function aiError(requestError) {
  return getUserFriendlyError(requestError, {
    fallback: 'Unable to complete the AI request. Please try again.',
    networkMessage: 'Unable to reach the AI service. Check that the backend is running.',
    statusMessages: {
      400: 'The task title or description is invalid for AI assistance.',
      401: 'Your session has expired. Please sign in again.',
      403: 'You do not have permission to use AI assistance.',
      502: 'The AI provider failed to process the request. Please try again later.',
      503: 'The AI provider is unavailable. Please try again later.',
      504: 'The AI provider timed out. Please try again.',
    },
  })
}

async function runAiAction(action, request) {
  if (loadingActions[action] || !validateAiInput()) {
    return null
  }

  loadingActions[action] = true
  error.value = null

  try {
    return await request(taskInput.value)
  } catch (requestError) {
    error.value = aiError(requestError)
    return null
  } finally {
    loadingActions[action] = false
  }
}

async function handleImproveDescription() {
  const response = await runAiAction('improve-description', improveTaskDescription)
  const value = response?.data?.result?.improvedDescription

  if (value) {
    improvedDescription.value = value
  }
}

async function handleSuggestSubtasks() {
  const response = await runAiAction('suggest-subtasks', suggestSubtasks)
  const subtasks = response?.data?.result?.subtasks

  if (Array.isArray(subtasks)) {
    suggestedSubtasks.value = subtasks
  }
}

async function handleSummarizeTask() {
  const response = await runAiAction('summarize-task', summarizeTask)
  const value = response?.data?.result?.summary

  if (value) {
    summary.value = value
  }
}

async function handleSuggestPriority() {
  const response = await runAiAction('suggest-priority', suggestPriority)
  const result = response?.data?.result

  if (result?.priority) {
    suggestedPriority.value = {
      priority: result.priority,
      reason: result.reason || '',
    }
  }
}

function applyImprovedDescription() {
  emit('update:description', improvedDescription.value)
}

function formattedSubtasks() {
  return suggestedSubtasks.value.map((subtask) => `- ${subtask}`).join('\n')
}

async function copySubtasks() {
  if (!suggestedSubtasks.value.length || typeof navigator === 'undefined' || !navigator.clipboard) {
    return
  }

  await navigator.clipboard.writeText(formattedSubtasks())
}

function insertSubtasks() {
  const subtasksText = formattedSubtasks()

  if (!subtasksText) {
    return
  }

  const separator = props.description.trim() ? '\n\n' : ''
  emit('update:description', `${props.description}${separator}Subtasks:\n${subtasksText}`)
}

function applyPriority() {
  if (suggestedPriority.value?.priority) {
    emit('update:priority', suggestedPriority.value.priority)
  }
}
</script>

<template>
  <section class="ai-task-tools" aria-labelledby="ai-task-tools-heading">
    <div class="ai-task-tools-heading">
      <h2 id="ai-task-tools-heading">AI Assistance</h2>
    </div>

    <div class="ai-task-actions">
      <button
        class="secondary-button"
        type="button"
        :disabled="isActionLoading('improve-description')"
        @click="handleImproveDescription"
      >
        {{ isActionLoading('improve-description') ? 'Improving...' : 'Improve Description' }}
      </button>
      <button
        class="secondary-button"
        type="button"
        :disabled="isActionLoading('suggest-subtasks')"
        @click="handleSuggestSubtasks"
      >
        {{ isActionLoading('suggest-subtasks') ? 'Suggesting...' : 'Suggest Subtasks' }}
      </button>
      <button
        class="secondary-button"
        type="button"
        :disabled="isActionLoading('summarize-task')"
        @click="handleSummarizeTask"
      >
        {{ isActionLoading('summarize-task') ? 'Summarizing...' : 'Summarize Task' }}
      </button>
      <button
        class="secondary-button"
        type="button"
        :disabled="isActionLoading('suggest-priority')"
        @click="handleSuggestPriority"
      >
        {{ isActionLoading('suggest-priority') ? 'Checking...' : 'Suggest Priority' }}
      </button>
    </div>

    <p v-if="error" class="field-error" role="alert">{{ error }}</p>

    <div v-if="improvedDescription" class="ai-result">
      <h3>Improved Description</h3>
      <p>{{ improvedDescription }}</p>
      <button class="secondary-button" type="button" @click="applyImprovedDescription">
        Apply improved description
      </button>
    </div>

    <div v-if="suggestedSubtasks.length" class="ai-result">
      <h3>Suggested Subtasks</h3>
      <ul>
        <li v-for="subtask in suggestedSubtasks" :key="subtask">{{ subtask }}</li>
      </ul>
      <div class="ai-result-actions">
        <button class="secondary-button" type="button" @click="copySubtasks">
          Copy subtasks
        </button>
        <button class="secondary-button" type="button" @click="insertSubtasks">
          Insert into description
        </button>
      </div>
    </div>

    <div v-if="summary" class="ai-result">
      <h3>Summary</h3>
      <p>{{ summary }}</p>
    </div>

    <div v-if="suggestedPriority" class="ai-result">
      <h3>Suggested Priority</h3>
      <p>
        <strong>{{ suggestedPriority.priority }}</strong>
        <span v-if="suggestedPriority.reason"> - {{ suggestedPriority.reason }}</span>
      </p>
      <button class="secondary-button" type="button" @click="applyPriority">
        Apply priority
      </button>
    </div>
  </section>
</template>
