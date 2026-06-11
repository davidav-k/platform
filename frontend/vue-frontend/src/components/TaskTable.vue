<script setup>
import { RouterLink, useRouter } from 'vue-router'

import TaskStatusBadge from './TaskStatusBadge.vue'

const router = useRouter()

defineProps({
  tasks: {
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

function openTask(taskId) {
  router.push(`/tasks/${taskId}`)
}
</script>

<template>
  <div class="table-scroll">
    <table class="task-table">
      <thead>
        <tr>
          <th scope="col">Task ID</th>
          <th scope="col">Title</th>
          <th scope="col">Status</th>
          <th scope="col">Priority</th>
          <th scope="col">Assignee</th>
          <th scope="col">Created</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="task in tasks"
          :key="task.taskId"
          class="task-row"
          tabindex="0"
          :aria-label="`Open task ${task.title}`"
          @click="openTask(task.taskId)"
          @keydown.enter="openTask(task.taskId)"
          @keydown.space.prevent="openTask(task.taskId)"
        >
          <td class="task-id">
            <RouterLink :to="`/tasks/${task.taskId}`">{{ task.taskId }}</RouterLink>
          </td>
          <td>
            <RouterLink :to="`/tasks/${task.taskId}`">{{ task.title }}</RouterLink>
          </td>
          <td><TaskStatusBadge :status="task.status" /></td>
          <td>{{ formatValue(task.priority) }}</td>
          <td class="task-id">{{ task.assigneeUserId || 'Unassigned' }}</td>
          <td>{{ formatDate(task.createdAt) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
