<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import TaskForm from '../components/TaskForm.vue'
import { ApiError, getUserFriendlyError } from '../services/apiClient'
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
  return getUserFriendlyError(requestError, {
    fallback: action === 'load'
      ? 'Unable to load the task. Please try again.'
      : 'Unable to update the task. Please try again.',
    networkMessage: 'Unable to reach the task service. Check that the backend is running.',
    statusMessages: {
      400: action === 'load'
        ? 'The task ID is invalid.'
        : 'Some task fields are invalid. Please review the form.',
      403: 'You do not have permission to update this task.',
      404: 'Task not found or not available to your account.',
      409: 'The task was changed by another operation. Refresh and try again.',
      500: 'The task service is unavailable. Please try again later.',
    },
  })
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

    <LoadingIndicator v-if="isLoading" message="Loading task..." />

    <ErrorMessage
      v-else-if="error && !task"
      :message="error"
      retry-label="Retry"
      @retry="loadTask"
    />

    <template v-else-if="task">
      <ErrorMessage v-if="error" :message="error" />
      <LoadingIndicator v-if="isSubmitting" message="Saving task..." />

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
