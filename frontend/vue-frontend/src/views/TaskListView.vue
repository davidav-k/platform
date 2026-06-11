<script setup>
import { computed, onMounted, ref } from 'vue'

import TaskTable from '../components/TaskTable.vue'
import { ApiError } from '../services/apiClient'
import { getTasks } from '../services/taskService'

const PAGE_SIZE = 20

const tasks = ref([])
const page = ref({
  number: 0,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
})
const statusFilter = ref('')
const priorityFilter = ref('')
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

const currentPage = computed(() => page.value.number + 1)
const displayedTotalPages = computed(() => Math.max(page.value.totalPages, 1))
const hasPreviousPage = computed(() => page.value.number > 0)
const hasNextPage = computed(() => page.value.number + 1 < page.value.totalPages)

function taskListError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 403) {
      return 'You do not have permission to view tasks.'
    }

    if (requestError.status >= 500) {
      return 'The task service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the task service. Check that the backend is running.'
  }

  return 'Unable to load tasks. Please try again.'
}

async function loadTasks(targetPage = page.value.number) {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await getTasks({
      page: targetPage,
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
      status: statusFilter.value,
      priority: priorityFilter.value,
    })

    if (activeRequestId !== requestId) {
      return
    }

    tasks.value = Array.isArray(response?.data?.items) ? response.data.items : []
    page.value = {
      number: response?.data?.page?.number ?? targetPage,
      size: response?.data?.page?.size ?? PAGE_SIZE,
      totalElements: response?.data?.page?.totalElements ?? tasks.value.length,
      totalPages: response?.data?.page?.totalPages ?? 0,
    }
  } catch (requestError) {
    if (activeRequestId === requestId) {
      tasks.value = []
      error.value = taskListError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

function applyFilters() {
  loadTasks(0)
}

function previousPage() {
  if (hasPreviousPage.value) {
    loadTasks(page.value.number - 1)
  }
}

function nextPage() {
  if (hasNextPage.value) {
    loadTasks(page.value.number + 1)
  }
}

onMounted(() => loadTasks(0))
</script>

<template>
  <section>
    <h1>Tasks</h1>

    <form class="task-filters" @submit.prevent="applyFilters">
      <label for="task-status">Status</label>
      <select id="task-status" v-model="statusFilter" :disabled="isLoading">
        <option value="">All statuses</option>
        <option value="NEW">New</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="DONE">Done</option>
        <option value="CANCELLED">Cancelled</option>
      </select>

      <label for="task-priority">Priority</label>
      <select id="task-priority" v-model="priorityFilter" :disabled="isLoading">
        <option value="">All priorities</option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
      </select>

      <button type="submit" :disabled="isLoading">Apply filters</button>
    </form>

    <p v-if="isLoading">Loading tasks...</p>

    <div v-else-if="error" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadTasks()">Retry</button>
    </div>

    <template v-else>
      <p v-if="tasks.length === 0">No tasks found</p>
      <TaskTable v-else :tasks="tasks" />

      <div class="pagination" aria-label="Task list pagination">
        <button type="button" :disabled="!hasPreviousPage" @click="previousPage">
          Previous
        </button>
        <span>
          Page {{ currentPage }} of {{ displayedTotalPages }}
          ({{ page.totalElements }} tasks)
        </span>
        <button type="button" :disabled="!hasNextPage" @click="nextPage">
          Next
        </button>
      </div>
    </template>
  </section>
</template>
