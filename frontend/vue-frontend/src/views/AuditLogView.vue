<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import AuditTable from '../components/AuditTable.vue'
import EmptyState from '../components/EmptyState.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import { getUserFriendlyError } from '../services/apiClient'
import { listAuditRecords } from '../services/auditService'

const PAGE_SIZE = 20

const records = ref([])
const page = ref({ number: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 })
const filters = reactive({
  eventType: '',
  aggregateType: '',
  sourceService: '',
  action: '',
  sort: 'occurredAt,desc',
})
const isLoading = ref(false)
const error = ref(null)
let requestId = 0

const currentPage = computed(() => page.value.number + 1)
const displayedTotalPages = computed(() => Math.max(page.value.totalPages, 1))
const hasPreviousPage = computed(() => page.value.number > 0)
const hasNextPage = computed(() => page.value.number + 1 < page.value.totalPages)

function auditListError(requestError) {
  return getUserFriendlyError(requestError, {
    fallback: 'Unable to load audit records. Please try again.',
    networkMessage: 'Unable to reach the audit service. Check that the backend is running.',
    statusMessages: {
      401: 'Your session has expired. Please sign in again.',
      403: 'Audit records are available only to administrators.',
      500: 'The audit service is unavailable. Please try again later.',
    },
  })
}

async function loadAuditRecords(targetPage = page.value.number) {
  const activeRequestId = ++requestId
  isLoading.value = true
  error.value = null

  try {
    const response = await listAuditRecords({
      page: targetPage,
      size: PAGE_SIZE,
      sort: filters.sort,
      eventType: filters.eventType,
      aggregateType: filters.aggregateType,
      sourceService: filters.sourceService,
      action: filters.action,
    })

    if (activeRequestId !== requestId) {
      return
    }

    records.value = Array.isArray(response?.data?.items) ? response.data.items : []
    page.value = {
      number: response?.data?.page?.number ?? targetPage,
      size: response?.data?.page?.size ?? PAGE_SIZE,
      totalElements: response?.data?.page?.totalElements ?? records.value.length,
      totalPages: response?.data?.page?.totalPages ?? 0,
    }
  } catch (requestError) {
    if (activeRequestId === requestId) {
      records.value = []
      error.value = auditListError(requestError)
    }
  } finally {
    if (activeRequestId === requestId) {
      isLoading.value = false
    }
  }
}

function applyFilters() {
  loadAuditRecords(0)
}

function clearFilters() {
  filters.eventType = ''
  filters.aggregateType = ''
  filters.sourceService = ''
  filters.action = ''
  filters.sort = 'occurredAt,desc'
  loadAuditRecords(0)
}

function previousPage() {
  if (hasPreviousPage.value) {
    loadAuditRecords(page.value.number - 1)
  }
}

function nextPage() {
  if (hasNextPage.value) {
    loadAuditRecords(page.value.number + 1)
  }
}

onMounted(() => loadAuditRecords(0))
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>Audit Log</h1>
        <p class="help-text">Read-only platform activity recorded from service events.</p>
      </div>
    </div>

    <form class="audit-filters" @submit.prevent="applyFilters">
      <div class="form-field">
        <label for="audit-event-type">Event type</label>
        <input
          id="audit-event-type"
          v-model.trim="filters.eventType"
          type="text"
          placeholder="TASK_CREATED"
          :disabled="isLoading"
        />
      </div>

      <div class="form-field">
        <label for="audit-aggregate-type">Aggregate type</label>
        <input
          id="audit-aggregate-type"
          v-model.trim="filters.aggregateType"
          type="text"
          placeholder="TASK"
          :disabled="isLoading"
        />
      </div>

      <div class="form-field">
        <label for="audit-source-service">Source service</label>
        <select id="audit-source-service" v-model="filters.sourceService" :disabled="isLoading">
          <option value="">All services</option>
          <option value="task-service">Task Service</option>
          <option value="user-service">User Service</option>
          <option value="notification-service">Notification Service</option>
        </select>
      </div>

      <div class="form-field">
        <label for="audit-action">Action</label>
        <input
          id="audit-action"
          v-model.trim="filters.action"
          type="text"
          placeholder="CREATE_TASK"
          :disabled="isLoading"
        />
      </div>

      <div class="form-field">
        <label for="audit-sort">Sort</label>
        <select id="audit-sort" v-model="filters.sort" :disabled="isLoading">
          <option value="occurredAt,desc">Newest first</option>
          <option value="occurredAt,asc">Oldest first</option>
          <option value="createdAt,desc">Recently stored first</option>
        </select>
      </div>

      <div class="audit-filter-actions">
        <button type="submit" :disabled="isLoading">Apply filters</button>
        <button class="secondary-button" type="button" :disabled="isLoading" @click="clearFilters">
          Clear
        </button>
      </div>
    </form>

    <LoadingIndicator v-if="isLoading" message="Loading audit records..." />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      retry-label="Retry"
      @retry="loadAuditRecords()"
    />

    <template v-else>
      <EmptyState
        v-if="records.length === 0"
        title="No audit records found"
        message="Try changing the filters or generate new platform activity."
      />
      <AuditTable v-else :records="records" />

      <div class="pagination" aria-label="Audit log pagination">
        <button type="button" :disabled="!hasPreviousPage" @click="previousPage">
          Previous
        </button>
        <span>
          Page {{ currentPage }} of {{ displayedTotalPages }}
          ({{ page.totalElements }} audit records)
        </span>
        <button type="button" :disabled="!hasNextPage" @click="nextPage">
          Next
        </button>
      </div>
    </template>
  </section>
</template>
