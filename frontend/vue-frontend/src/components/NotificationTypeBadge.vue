<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    required: true,
  },
})

const label = computed(() => props.type.replaceAll('_', ' '))
const typeClass = computed(() => `notification-type-${props.type.toLowerCase().replaceAll('_', '-')}`)
</script>

<template>
  <span class="notification-type-badge" :class="typeClass">{{ label }}</span>
</template>
