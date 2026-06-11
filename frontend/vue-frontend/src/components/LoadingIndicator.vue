<script setup>
defineProps({
  message: {
    type: String,
    default: 'Loading...',
  },
})
</script>

<template>
  <div class="loading-indicator" role="status" aria-live="polite">
    <span class="loading-spinner" aria-hidden="true"></span>
    <span>{{ message }}</span>
  </div>
</template>
