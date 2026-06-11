<script setup>
defineProps({
  message: {
    type: String,
    required: true,
  },
  retryLabel: {
    type: String,
    default: '',
  },
})

defineEmits(['retry'])
</script>

<template>
  <div class="error-panel" role="alert">
    <p class="error-message">{{ message }}</p>
    <button v-if="retryLabel" type="button" @click="$emit('retry')">
      {{ retryLabel }}
    </button>
  </div>
</template>
