<script setup>
import { computed, ref, watch } from 'vue'

const TASK_STATUSES = [
  { value: 'NEW', label: 'New' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'DONE', label: 'Done' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const props = defineProps({
  currentStatus: {
    type: String,
    required: true,
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['submit'])
const selectedStatus = ref(props.currentStatus)
const error = ref(null)

const canSubmit = computed(() => (
  TASK_STATUSES.some(({ value }) => value === selectedStatus.value)
  && selectedStatus.value !== props.currentStatus
  && !props.isSubmitting
))

watch(
  () => props.currentStatus,
  (status) => {
    selectedStatus.value = status
    error.value = null
  },
)

function handleSubmit() {
  if (!TASK_STATUSES.some(({ value }) => value === selectedStatus.value)) {
    error.value = 'Select a valid task status.'
    return
  }

  if (!canSubmit.value) {
    return
  }

  error.value = null
  emit('submit', selectedStatus.value)
}
</script>

<template>
  <form class="status-form" @submit.prevent="handleSubmit">
    <label for="task-status-update">Change status</label>
    <select
      id="task-status-update"
      v-model="selectedStatus"
      :disabled="isSubmitting"
    >
      <option v-for="status in TASK_STATUSES" :key="status.value" :value="status.value">
        {{ status.label }}
      </option>
    </select>
    <button type="submit" :disabled="!canSubmit">
      {{ isSubmitting ? 'Updating...' : 'Apply status' }}
    </button>
    <p v-if="error" class="field-error" role="alert">{{ error }}</p>
  </form>
</template>
