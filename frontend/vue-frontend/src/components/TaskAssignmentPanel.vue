<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  currentAssigneeUserId: {
    type: String,
    default: null,
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['submit'])
const assigneeUserId = ref(props.currentAssigneeUserId || '')
const error = ref(null)

const normalizedAssignee = computed(() => assigneeUserId.value.trim())
const hasChanged = computed(() => normalizedAssignee.value !== (props.currentAssigneeUserId || ''))

watch(
  () => props.currentAssigneeUserId,
  (value) => {
    assigneeUserId.value = value || ''
    error.value = null
  },
)

function isUuid(value) {
  return /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(value)
}

function submitAssignment() {
  if (props.disabled || props.isSubmitting || !hasChanged.value) {
    return
  }

  if (normalizedAssignee.value && !isUuid(normalizedAssignee.value)) {
    error.value = 'Assignee user ID must be a valid UUID.'
    return
  }

  error.value = null
  emit('submit', normalizedAssignee.value || null)
}

function unassign() {
  if (props.disabled || props.isSubmitting || !props.currentAssigneeUserId) {
    return
  }

  assigneeUserId.value = ''
  error.value = null
  emit('submit', null)
}
</script>

<template>
  <section class="assignment-panel" aria-labelledby="task-assignment-heading">
    <h2 id="task-assignment-heading">Task Assignment</h2>
    <p>
      Current assignee:
      <strong class="task-id">{{ currentAssigneeUserId || 'Unassigned' }}</strong>
    </p>

    <form class="assignment-form" @submit.prevent="submitAssignment">
      <label for="task-assignee-update">Assignee user ID</label>
      <input
        id="task-assignee-update"
        v-model="assigneeUserId"
        type="text"
        placeholder="Public user UUID"
        :disabled="disabled || isSubmitting"
        :aria-invalid="Boolean(error)"
        aria-describedby="task-assignee-update-error"
      />
      <button type="submit" :disabled="disabled || isSubmitting || !hasChanged">
        {{ isSubmitting ? 'Updating...' : 'Assign task' }}
      </button>
      <button
        class="secondary-button"
        type="button"
        :disabled="disabled || isSubmitting || !currentAssigneeUserId"
        @click="unassign"
      >
        Unassign
      </button>
      <p v-if="error" id="task-assignee-update-error" class="field-error" role="alert">
        {{ error }}
      </p>
    </form>

    <p class="help-text">
      No assignment-candidate lookup endpoint is currently available. Enter a public user UUID.
    </p>
  </section>
</template>
