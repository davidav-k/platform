<script setup>
import { RouterLink } from 'vue-router'

import NotificationTypeBadge from './NotificationTypeBadge.vue'

defineProps({
  notifications: {
    type: Array,
    required: true,
  },
})

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
</script>

<template>
  <div class="notification-list">
    <article
      v-for="notification in notifications"
      :key="notification.notificationId"
      class="notification-card"
    >
      <div class="notification-card-heading">
        <NotificationTypeBadge :type="notification.type" />
        <span>{{ formatDate(notification.createdAt) }}</span>
      </div>

      <h2>
        <RouterLink :to="`/notifications/${notification.notificationId}`">
          {{ notification.subject || 'Notification' }}
        </RouterLink>
      </h2>
      <p>{{ notification.body }}</p>

      <dl class="notification-summary">
        <dt>Channel</dt>
        <dd>{{ formatValue(notification.channel) }}</dd>
        <dt>Status</dt>
        <dd>{{ formatValue(notification.status) }}</dd>
      </dl>
    </article>
  </div>
</template>
