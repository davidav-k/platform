<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import ConfirmDeleteButton from '../components/ConfirmDeleteButton.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import TaskAssignmentPanel from '../components/TaskAssignmentPanel.vue'
import TaskDetailsCard from '../components/TaskDetailsCard.vue'
import TaskStatusSelector from '../components/TaskStatusSelector.vue'
import { getUserFriendlyError } from '../services/apiClient'
import { assignTask, deleteTask, getTask, updateTaskStatus } from '../services/taskService'

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
const isUpdatingAssignment = ref(false)
const isDeleting = ref(false)
const error = ref(null)
const statusError = ref(null)
const assignmentError = ref(null)
const deleteError = ref(null)
let requestId = 0

function taskDetailsError(requestError) {
  return taskRequestError(requestError, 'Unable to load the task. Please try again.', {
    400: 'The task ID is invalid.',
    403: 'You do not have permission to view this task.',
    404: 'Task not found or not available to your account.',
  })
}

function taskRequestError(requestError, fallback, statusMessages = {}) {
  return getUserFriendlyError(requestError, {
    fallback,
    networkMessage: 'Unable to reach the task service. Check that the backend is running.',
    statusMessages: {
      ...statusMessages,
      500: 'The task service is unavailable. Please try again later.',
    },
  })
}

async function loadTask() {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null
  statusError.value = null
  assignmentError.value = null
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
  return taskRequestError(requestError, 'Unable to update the task status. Please try again.', {
    400: 'The selected status is invalid.',
    403: 'You do not have permission to change this task status.',
    404: 'Task not found or not available to your account.',
    409: 'This status change conflicts with the current task state.',
  })
}

async function handleStatusChange(status) {
  if (
    isUpdatingStatus.value
    || isUpdatingAssignment.value
    || isDeleting.value
    || status === task.value?.status
  ) {
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

function assignmentUpdateError(requestError) {
  return taskRequestError(requestError, 'Unable to update the task assignee. Please try again.', {
    400: 'The assignee user ID is invalid.',
    403: 'You do not have permission to assign this task.',
    404: 'Task not found or assignment is not available to your account.',
    409: 'This assignment conflicts with the current task state.',
  })
}

async function handleAssignmentChange(assigneeUserId) {
  if (isUpdatingAssignment.value || isUpdatingStatus.value || isDeleting.value) {
    return
  }

  isUpdatingAssignment.value = true
  assignmentError.value = null

  try {
    await assignTask(props.id, { assigneeUserId })
    await loadTask()
  } catch (requestError) {
    assignmentError.value = assignmentUpdateError(requestError)
  } finally {
    isUpdatingAssignment.value = false
  }
}

function deleteTaskError(requestError) {
  return taskRequestError(requestError, 'Unable to delete the task. Please try again.', {
    400: 'The task ID is invalid.',
    403: 'You do not have permission to delete this task.',
    404: 'Task not found, already deleted, or not available to your account.',
    409: 'The task cannot be deleted in its current state.',
  })
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
          v-if="!isUpdatingStatus && !isUpdatingAssignment && !isDeleting"
          class="button-link"
          :to="{ name: 'task-edit', params: { id } }"
        >
          Edit
        </RouterLink>
        <button
          type="button"
          :disabled="isLoading || isUpdatingStatus || isUpdatingAssignment || isDeleting"
          @click="loadTask"
        >
          {{ isLoading ? 'Refreshing...' : 'Refresh task' }}
        </button>
      </div>
    </div>

    <LoadingIndicator v-if="isLoading && !task" message="Loading task..." />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      retry-label="Retry"
      @retry="loadTask"
    />

    <template v-else-if="task">
      <TaskDetailsCard :task="task" />

      <section class="status-panel" aria-labelledby="task-status-heading">
        <h2 id="task-status-heading">Task Status</h2>
        <ErrorMessage v-if="statusError" :message="statusError" />
        <TaskStatusSelector
          :current-status="task.status"
          :is-submitting="isUpdatingStatus || isUpdatingAssignment || isDeleting"
          @submit="handleStatusChange"
        />
      </section>

      <div>
        <ErrorMessage v-if="assignmentError" :message="assignmentError" />
        <TaskAssignmentPanel
          :current-assignee-user-id="task.assigneeUserId"
          :is-submitting="isUpdatingAssignment"
          :disabled="isLoading || isUpdatingStatus || isDeleting"
          @submit="handleAssignmentChange"
        />
      </div>

      <section class="danger-panel" aria-labelledby="delete-task-heading">
        <h2 id="delete-task-heading">Delete Task</h2>
        <p>The backend retains the task record but removes it from normal task operations.</p>
        <ErrorMessage v-if="deleteError" :message="deleteError" />
        <ConfirmDeleteButton
          :is-deleting="isDeleting"
          :disabled="isLoading || isUpdatingStatus || isUpdatingAssignment"
          @confirm="handleDelete"
        />
      </section>
    </template>
  </section>
</template>
