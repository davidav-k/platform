<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="not-found-page">
    <p class="not-found-code">404</p>
    <h1>Page Not Found</h1>
    <p>The page you requested does not exist.</p>
    <RouterLink class="button-link" to="/">Return Home</RouterLink>
  </section>
</template>
