<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isDeleting: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['confirm'])
const isConfirming = ref(false)

watch(
  () => props.isDeleting,
  (isDeleting) => {
    if (!isDeleting) {
      isConfirming.value = false
    }
  },
)

function requestConfirmation() {
  if (!props.disabled && !props.isDeleting) {
    isConfirming.value = true
  }
}

function cancelConfirmation() {
  if (!props.isDeleting) {
    isConfirming.value = false
  }
}

function confirmDelete() {
  if (!props.disabled && !props.isDeleting) {
    emit('confirm')
  }
}
</script>

<template>
  <div class="delete-control">
    <button
      v-if="!isConfirming"
      class="danger-button"
      type="button"
      :disabled="disabled || isDeleting"
      @click="requestConfirmation"
    >
      Delete task
    </button>

    <div v-else class="delete-confirmation" role="group" aria-label="Confirm task deletion">
      <p>This soft-deletes the task. Continue?</p>
      <button
        class="danger-button"
        type="button"
        :disabled="disabled || isDeleting"
        @click="confirmDelete"
      >
        {{ isDeleting ? 'Deleting...' : 'Confirm delete' }}
      </button>
      <button
        class="secondary-button"
        type="button"
        :disabled="isDeleting"
        @click="cancelConfirmation"
      >
        Cancel
      </button>
    </div>
  </div>
</template>
