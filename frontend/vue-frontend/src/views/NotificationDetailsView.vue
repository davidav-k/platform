<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import NotificationTypeBadge from '../components/NotificationTypeBadge.vue'
import { getUserFriendlyError } from '../services/apiClient'
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
  return getUserFriendlyError(requestError, {
    fallback: 'Unable to load the notification. Please try again.',
    networkMessage: 'Unable to reach the notification service. Check that the backend is running.',
    statusMessages: {
      400: 'The notification ID is invalid.',
      403: 'You do not have permission to view this notification.',
      404: 'Notification not found.',
      500: 'The notification service is unavailable. Please try again later.',
    },
  })
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

    <LoadingIndicator
      v-if="isLoading && !notification"
      message="Loading notification..."
    />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      retry-label="Retry"
      @retry="loadNotification"
    />

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
