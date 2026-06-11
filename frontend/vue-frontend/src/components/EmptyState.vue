<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <div class="empty-state" role="status">
    <h2>{{ title }}</h2>
    <p v-if="message">{{ message }}</p>
    <slot></slot>
  </div>
</template>
