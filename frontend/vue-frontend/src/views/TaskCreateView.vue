<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import TaskForm from '../components/TaskForm.vue'
import { ApiError } from '../services/apiClient'
import { createTask } from '../services/taskService'

const router = useRouter()
const isSubmitting = ref(false)
const error = ref(null)
const serverErrors = ref({})

function createTaskError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 403) {
      return 'You do not have permission to create tasks.'
    }

    if (requestError.status === 409) {
      return 'The task could not be created because its data conflicts with existing data.'
    }

    if (requestError.status >= 500) {
      return 'The task service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the task service. Check that the backend is running.'
  }

  return 'Unable to create the task. Please review the form and try again.'
}

async function handleSubmit(task) {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  error.value = null
  serverErrors.value = {}

  try {
    const response = await createTask(task)
    const taskId = response?.data?.task?.taskId

    if (!taskId) {
      throw new Error('Create task response does not contain a task ID.')
    }

    await router.push({ name: 'task-details', params: { id: taskId } })
  } catch (requestError) {
    if (requestError instanceof ApiError && requestError.status === 400) {
      const responseData = requestError.data?.data
      serverErrors.value = responseData && typeof responseData === 'object' ? responseData : {}
    }

    error.value = createTaskError(requestError)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="form-panel task-form-panel">
    <div class="page-heading">
      <div>
        <h1>Create Task</h1>
        <RouterLink to="/tasks">Back to Tasks</RouterLink>
      </div>
    </div>

    <p v-if="error" class="error-message" role="alert">{{ error }}</p>

    <TaskForm
      :is-submitting="isSubmitting"
      :server-errors="serverErrors"
      @submit="handleSubmit"
    />
  </section>
</template>
