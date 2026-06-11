<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import ConfirmDeleteButton from '../components/ConfirmDeleteButton.vue'
import TaskDetailsCard from '../components/TaskDetailsCard.vue'
import TaskStatusSelector from '../components/TaskStatusSelector.vue'
import { ApiError } from '../services/apiClient'
import { deleteTask, getTask, updateTaskStatus } from '../services/taskService'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const task = ref(null)
const isLoading = ref(false)
const isUpdatingStatus = ref(false)
const isDeleting = ref(false)
const error = ref(null)
const statusError = ref(null)
const deleteError = ref(null)
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
  statusError.value = null
  deleteError.value = null

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

function statusUpdateError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 400) {
      return 'The selected status is invalid.'
    }

    if (requestError.status === 403) {
      return 'You do not have permission to change this task status.'
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

  return 'Unable to update the task status. Please try again.'
}

async function handleStatusChange(status) {
  if (isUpdatingStatus.value || isDeleting.value || status === task.value?.status) {
    return
  }

  isUpdatingStatus.value = true
  statusError.value = null

  try {
    await updateTaskStatus(props.id, { status })
    await loadTask()
  } catch (requestError) {
    statusError.value = statusUpdateError(requestError)
  } finally {
    isUpdatingStatus.value = false
  }
}

function deleteTaskError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 400) {
      return 'The task ID is invalid.'
    }

    if (requestError.status === 403) {
      return 'You do not have permission to delete this task.'
    }

    if (requestError.status === 404) {
      return 'Task not found, already deleted, or not available to your account.'
    }

    if (requestError.status >= 500) {
      return 'The task service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the task service. Check that the backend is running.'
  }

  return 'Unable to delete the task. Please try again.'
}

async function handleDelete() {
  if (isDeleting.value) {
    return
  }

  isDeleting.value = true
  deleteError.value = null

  try {
    await deleteTask(props.id)
    await router.push({ name: 'tasks' })
  } catch (requestError) {
    deleteError.value = deleteTaskError(requestError)
  } finally {
    isDeleting.value = false
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
          v-if="!isDeleting"
          class="button-link"
          :to="{ name: 'task-edit', params: { id } }"
        >
          Edit
        </RouterLink>
        <button type="button" :disabled="isLoading || isDeleting" @click="loadTask">
          {{ isLoading ? 'Refreshing...' : 'Refresh task' }}
        </button>
      </div>
    </div>

    <p v-if="isLoading && !task">Loading task...</p>

    <div v-else-if="error" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadTask">Retry</button>
    </div>

    <template v-else-if="task">
      <TaskDetailsCard :task="task" />

      <section class="status-panel" aria-labelledby="task-status-heading">
        <h2 id="task-status-heading">Task Status</h2>
        <p v-if="statusError" class="error-message" role="alert">{{ statusError }}</p>
        <TaskStatusSelector
          :current-status="task.status"
          :is-submitting="isUpdatingStatus || isDeleting"
          @submit="handleStatusChange"
        />
      </section>

      <section class="danger-panel" aria-labelledby="delete-task-heading">
        <h2 id="delete-task-heading">Delete Task</h2>
        <p>The backend retains the task record but removes it from normal task operations.</p>
        <p v-if="deleteError" class="error-message" role="alert">{{ deleteError }}</p>
        <ConfirmDeleteButton
          :is-deleting="isDeleting"
          :disabled="isLoading || isUpdatingStatus"
          @confirm="handleDelete"
        />
      </section>
    </template>
  </section>
</template>
