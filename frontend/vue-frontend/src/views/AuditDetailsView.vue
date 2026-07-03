<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import { getUserFriendlyError } from '../services/apiClient'
import { getAuditRecord } from '../services/auditService'

const props = defineProps({
  auditId: {
    type: String,
    required: true,
  },
})

const auditRecord = ref(null)
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

function formatValue(value, fallback = 'Not provided') {
  return value ? String(value).replaceAll('_', ' ') : fallback
}

function formatDate(value) {
  if (!value) {
    return 'Not provided'
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function auditDetailsError(requestError) {
  return getUserFriendlyError(requestError, {
    fallback: 'Unable to load the audit record. Please try again.',
    networkMessage: 'Unable to reach the audit service. Check that the backend is running.',
    statusMessages: {
      400: 'The audit record ID is invalid.',
      401: 'Your session has expired. Please sign in again.',
      403: 'Audit records are available only to administrators.',
      404: 'Audit record not found.',
      500: 'The audit service is unavailable. Please try again later.',
    },
  })
}

async function loadAuditRecord() {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await getAuditRecord(props.auditId)

    if (activeRequestId !== requestId) {
      return
    }

    const responseRecord = response?.data?.audit
    if (!responseRecord) {
      throw new Error('Audit response does not contain an audit record.')
    }

    auditRecord.value = responseRecord
  } catch (requestError) {
    if (activeRequestId === requestId) {
      auditRecord.value = null
      error.value = auditDetailsError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

watch(() => props.auditId, loadAuditRecord, { immediate: true })
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>Audit Details</h1>
        <RouterLink to="/audit">Back to Audit Log</RouterLink>
      </div>
      <button type="button" :disabled="isLoading" @click="loadAuditRecord">
        {{ isLoading ? 'Refreshing...' : 'Refresh audit record' }}
      </button>
    </div>

    <LoadingIndicator v-if="isLoading && !auditRecord" message="Loading audit record..." />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      retry-label="Retry"
      @retry="loadAuditRecord"
    />

    <article v-else-if="auditRecord" class="task-details-card">
      <dl class="task-details-grid">
        <dt>Audit ID</dt>
        <dd class="task-id">{{ auditRecord.auditId }}</dd>
        <dt>Event ID</dt>
        <dd class="task-id">{{ auditRecord.eventId }}</dd>
        <dt>Occurred</dt>
        <dd>{{ formatDate(auditRecord.occurredAt) }}</dd>
        <dt>Stored</dt>
        <dd>{{ formatDate(auditRecord.createdAt) }}</dd>
        <dt>Source service</dt>
        <dd>{{ auditRecord.sourceService }}</dd>
        <dt>Event type</dt>
        <dd>{{ formatValue(auditRecord.eventType) }}</dd>
        <dt>Action</dt>
        <dd>{{ formatValue(auditRecord.action) }}</dd>
        <dt>Aggregate type</dt>
        <dd>{{ formatValue(auditRecord.aggregateType) }}</dd>
        <dt>Aggregate ID</dt>
        <dd class="task-id">{{ auditRecord.aggregateId }}</dd>
        <dt>Actor user ID</dt>
        <dd class="task-id">{{ auditRecord.actorUserId || 'System' }}</dd>
        <dt>Actor email</dt>
        <dd>{{ auditRecord.actorEmail || 'Not provided' }}</dd>
      </dl>

      <p class="help-text audit-payload-note">
        Event payload is not returned by the current read-only Audit API.
      </p>
    </article>
  </section>
</template>
