<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import TaskDetailsCard from '../components/TaskDetailsCard.vue'
import { ApiError } from '../services/apiClient'
import { getTask } from '../services/taskService'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const task = ref(null)
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

function taskDetailsError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 400) {
      return 'The task ID is invalid.'
    }

    if (requestError.status === 403) {
      return 'You do not have permission to view this task.'
    }

    if (requestError.status === 404) {
      return 'Task not found or not available to your account.'
    }

    if (requestError.status >= 500) {
      return 'The task service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the task service. Check that the backend is running.'
  }

  return 'Unable to load the task. Please try again.'
}

async function loadTask() {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await getTask(props.id)

    if (activeRequestId !== requestId) {
      return
    }

    const responseTask = response?.data?.task

    if (!responseTask) {
      throw new Error('Task response does not contain a task.')
    }

    task.value = responseTask
  } catch (requestError) {
    if (activeRequestId === requestId) {
      task.value = null
      error.value = taskDetailsError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

watch(() => props.id, loadTask, { immediate: true })
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>Task Details</h1>
        <RouterLink to="/tasks">Back to Tasks</RouterLink>
      </div>

      <div v-if="task" class="page-actions">
        <RouterLink
          class="button-link"
          :to="{ name: 'task-edit', params: { id } }"
        >
          Edit
        </RouterLink>
        <button type="button" :disabled="isLoading" @click="loadTask">
          {{ isLoading ? 'Refreshing...' : 'Refresh task' }}
        </button>
      </div>
    </div>

    <p v-if="isLoading && !task">Loading task...</p>

    <div v-else-if="error" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadTask">Retry</button>
    </div>

    <TaskDetailsCard v-else-if="task" :task="task" />
  </section>
</template>
