<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import NotificationTypeBadge from '../components/NotificationTypeBadge.vue'
import { ApiError } from '../services/apiClient'
import { getNotification } from '../services/notificationService'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const notification = ref(null)
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

function formatValue(value) {
  return value ? String(value).replaceAll('_', ' ') : 'Not provided'
}

function formatDate(value) {
  if (!value) {
    return 'Not provided'
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function notificationDetailsError(requestError) {
  if (requestError instanceof ApiError) {
    if (requestError.status === 400) {
      return 'The notification ID is invalid.'
    }

    if (requestError.status === 403) {
      return 'You do not have permission to view this notification.'
    }

    if (requestError.status === 404) {
      return 'Notification not found.'
    }

    if (requestError.status >= 500) {
      return 'The notification service is unavailable. Please try again later.'
    }
  }

  if (requestError instanceof TypeError) {
    return 'Unable to reach the notification service. Check that the backend is running.'
  }

  return 'Unable to load the notification. Please try again.'
}

async function loadNotification() {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await getNotification(props.id)

    if (activeRequestId !== requestId) {
      return
    }

    const responseNotification = response?.data?.notification

    if (!responseNotification) {
      throw new Error('Notification response does not contain a notification.')
    }

    notification.value = responseNotification
  } catch (requestError) {
    if (activeRequestId === requestId) {
      notification.value = null
      error.value = notificationDetailsError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

watch(() => props.id, loadNotification, { immediate: true })
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>Notification Details</h1>
        <RouterLink to="/notifications">Back to Notifications</RouterLink>
      </div>
      <button type="button" :disabled="isLoading" @click="loadNotification">
        {{ isLoading ? 'Refreshing...' : 'Refresh notification' }}
      </button>
    </div>

    <p v-if="isLoading && !notification">Loading notification...</p>

    <div v-else-if="error" class="error-panel" role="alert">
      <p class="error-message">{{ error }}</p>
      <button type="button" @click="loadNotification">Retry</button>
    </div>

    <article v-else-if="notification" class="notification-details-card">
      <div class="notification-card-heading">
        <NotificationTypeBadge :type="notification.type" />
        <span>{{ formatDate(notification.createdAt) }}</span>
      </div>
      <h2>{{ notification.subject || 'Notification' }}</h2>
      <p class="notification-body">{{ notification.body }}</p>

      <dl class="task-details-grid">
        <dt>Notification ID</dt>
        <dd class="task-id">{{ notification.notificationId }}</dd>
        <dt>Recipient user ID</dt>
        <dd class="task-id">{{ notification.recipientUserId }}</dd>
        <dt>Type</dt>
        <dd>{{ formatValue(notification.type) }}</dd>
        <dt>Channel</dt>
        <dd>{{ formatValue(notification.channel) }}</dd>
        <dt>Status</dt>
        <dd>{{ formatValue(notification.status) }}</dd>
        <dt>Created</dt>
        <dd>{{ formatDate(notification.createdAt) }}</dd>
        <dt>Updated</dt>
        <dd>{{ formatDate(notification.updatedAt) }}</dd>
        <dt>Sent</dt>
        <dd>{{ formatDate(notification.sentAt) }}</dd>
        <template v-if="notification.failureReason">
          <dt>Failure reason</dt>
          <dd>{{ notification.failureReason }}</dd>
        </template>
      </dl>
    </article>
  </section>
</template>
