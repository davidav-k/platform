<script setup>
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

defineProps({
  records: {
    type: Array,
    required: true,
  },
})

function formatDate(value) {
  if (!value) {
    return 'Not provided'
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function formatValue(value, fallback = 'Not provided') {
  return value ? String(value).replaceAll('_', ' ') : fallback
}

function openAuditRecord(auditId) {
  router.push(`/audit/${auditId}`)
}
</script>

<template>
  <div class="table-scroll">
    <table class="task-table audit-table">
      <thead>
        <tr>
          <th scope="col">Occurred</th>
          <th scope="col">Source service</th>
          <th scope="col">Event type</th>
          <th scope="col">Action</th>
          <th scope="col">Aggregate type</th>
          <th scope="col">Aggregate ID</th>
          <th scope="col">Actor user ID</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="record in records"
          :key="record.auditId"
          class="task-row"
          tabindex="0"
          :aria-label="`Open audit record ${record.auditId}`"
          @click="openAuditRecord(record.auditId)"
          @keydown.enter="openAuditRecord(record.auditId)"
          @keydown.space.prevent="openAuditRecord(record.auditId)"
        >
          <td>
            <RouterLink :to="`/audit/${record.auditId}`">
              {{ formatDate(record.occurredAt) }}
            </RouterLink>
          </td>
          <td>{{ record.sourceService }}</td>
          <td>{{ formatValue(record.eventType) }}</td>
          <td>{{ formatValue(record.action) }}</td>
          <td>{{ formatValue(record.aggregateType) }}</td>
          <td class="task-id">{{ record.aggregateId }}</td>
          <td class="task-id">{{ record.actorUserId || 'System' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
