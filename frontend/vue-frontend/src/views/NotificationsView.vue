<script setup>
import { computed, onMounted, ref } from 'vue'

import NotificationList from '../components/NotificationList.vue'
import { ApiError } from '../services/apiClient'
import { getNotifications } from '../services/notificationService'

const PAGE_SIZE = 20

const notifications = ref([])
const page = ref({ number: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 })
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

const currentPage = computed(() => page.value.number + 1)
const displayedTotalPages = computed(() => Math.max(page.value.totalPages, 1))
const hasPreviousPage = computed(() => page.value.number > 0)
const hasNextPage = computed(() => page.value.number + 1 < page.value.totalPages)

function notificationListError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 403) {
      return 'You do not have permission to view notifications.'
    }

    if (requestError.status >= 500) {
      return 'The notification service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the notification service. Check that the backend is running.'
  }

  return 'Unable to load notifications. Please try again.'
}

async function loadNotifications(targetPage = page.value.number) {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await getNotifications({
      page: targetPage,
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })

    if (activeRequestId !== requestId) {
      return
    }

    notifications.value = Array.isArray(response?.data?.items) ? response.data.items : []
    page.value = {
      number: response?.data?.page?.number ?? targetPage,
      size: response?.data?.page?.size ?? PAGE_SIZE,
      totalElements: response?.data?.page?.totalElements ?? notifications.value.length,
      totalPages: response?.data?.page?.totalPages ?? 0,
    }
  } catch (requestError) {
    if (activeRequestId === requestId) {
      notifications.value = []
      error.value = notificationListError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

function previousPage() {
  if (hasPreviousPage.value) {
    loadNotifications(page.value.number - 1)
  }
}

function nextPage() {
  if (hasNextPage.value) {
    loadNotifications(page.value.number + 1)
  }
}

onMounted(() => loadNotifications(0))
</script>

<template>
  <section>
    <h1>Notifications</h1>

    <p v-if="isLoading">Loading notifications...</p>

    <div v-else-if="error" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadNotifications()">Retry</button>
    </div>

    <template v-else>
      <p v-if="notifications.length === 0">No notifications</p>
      <NotificationList v-else :notifications="notifications" />

      <div class="pagination" aria-label="Notification list pagination">
        <button type="button" :disabled="!hasPreviousPage" @click="previousPage">
          Previous
        </button>
        <span>
          Page {{ currentPage }} of {{ displayedTotalPages }}
          ({{ page.totalElements }} notifications)
        </span>
        <button type="button" :disabled="!hasNextPage" @click="nextPage">
          Next
        </button>
      </div>
    </template>
  </section>
</template>
