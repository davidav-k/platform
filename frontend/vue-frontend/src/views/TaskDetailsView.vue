<script setup>
defineProps({
  id: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <section>
    <h1>Task Details</h1>
    <p>Details for task {{ id }} will appear here.</p>
  </section>
</template>
