<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
  },
})

const label = computed(() => props.status.replaceAll('_', ' '))
const statusClass = computed(() => `status-${props.status.toLowerCase().replaceAll('_', '-')}`)
</script>

<template>
  <span class="status-badge" :class="statusClass">{{ label }}</span>
</template>
