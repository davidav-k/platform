<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/">Task Management Platform</RouterLink>

      <nav aria-label="Main navigation">
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/login">Login</RouterLink>
        <RouterLink to="/profile">Profile</RouterLink>
        <RouterLink to="/tasks">Tasks</RouterLink>
        <RouterLink to="/notifications">Notifications</RouterLink>
      </nav>
    </header>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>
