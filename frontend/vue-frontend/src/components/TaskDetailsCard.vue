<script setup>
import TaskStatusBadge from './TaskStatusBadge.vue'

defineProps({
  task: {
    type: Object,
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
  <article class="task-details-card">
    <h2>{{ task.title }}</h2>

    <dl class="task-details-grid">
      <dt>Task ID</dt>
      <dd class="task-id">{{ task.taskId }}</dd>

      <dt>Description</dt>
      <dd class="task-description">{{ task.description || 'No description' }}</dd>

      <dt>Status</dt>
      <dd><TaskStatusBadge :status="task.status" /></dd>

      <dt>Priority</dt>
      <dd>{{ formatValue(task.priority) }}</dd>

      <dt>Assignee</dt>
      <dd class="task-id">{{ task.assigneeUserId || 'Unassigned' }}</dd>

      <dt>Created by</dt>
      <dd class="task-id">{{ task.createdByUserId || 'Not provided' }}</dd>

      <dt>Created</dt>
      <dd>{{ formatDate(task.createdAt) }}</dd>

      <dt>Updated</dt>
      <dd>{{ formatDate(task.updatedAt) }}</dd>
    </dl>
  </article>
</template>
