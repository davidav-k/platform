<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import TaskForm from '../components/TaskForm.vue'
import { ApiError } from '../services/apiClient'
import { getTask, updateTask } from '../services/taskService'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const task = ref(null)
const isLoading = ref(false)
const isSubmitting = ref(false)
const error = ref(null)
const serverErrors = ref({})
let requestId = 0

function taskError(requestError, action) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 400) {
      return action === 'load'
        ? 'The task ID is invalid.'
        : 'Some task fields are invalid. Please review the form.'
    }

    if (requestError.status === 403) {
      return 'You do not have permission to update this task.'
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

  return action === 'load'
    ? 'Unable to load the task. Please try again.'
    : 'Unable to update the task. Please try again.'
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
      error.value = taskError(requestError, 'load')
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

async function handleSubmit(updates) {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  error.value = null
  serverErrors.value = {}

  try {
    await updateTask(props.id, updates)
    await router.push({ name: 'task-details', params: { id: props.id } })
  } catch (requestError) {
    if (requestError instanceof ApiError && requestError.status === 400) {
      const responseData = requestError.data?.data
      serverErrors.value = responseData && typeof responseData === 'object' ? responseData : {}
    }

    error.value = taskError(requestError, 'update')
  } finally {
    isSubmitting.value = false
  }
}

function cancelEdit() {
  router.push({ name: 'task-details', params: { id: props.id } })
}

watch(() => props.id, loadTask, { immediate: true })
</script>

<template>
  <section class="form-panel task-form-panel">
    <div class="page-heading">
      <div>
        <h1>Edit Task</h1>
        <RouterLink :to="{ name: 'task-details', params: { id } }">Back to Task</RouterLink>
      </div>
    </div>

    <p v-if="isLoading">Loading task...</p>

    <div v-else-if="error && !task" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadTask">Retry</button>
    </div>

    <template v-else-if="task">
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>

      <TaskForm
        mode="edit"
        :initial-values="task"
        :is-submitting="isSubmitting"
        :server-errors="serverErrors"
        :show-assignee="false"
        @cancel="cancelEdit"
        @submit="handleSubmit"
      />
    </template>
  </section>
</template>
