<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { isAuthenticated, isLoading, logout } from './services/authState'

const router = useRouter()

async function handleLogout() {
  await logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/">Task Management Platform</RouterLink>

      <nav aria-label="Main navigation">
        <RouterLink to="/">Home</RouterLink>
        <RouterLink v-if="!isLoading && !isAuthenticated" to="/login">Login</RouterLink>
        <template v-if="isAuthenticated">
          <RouterLink to="/profile">Profile</RouterLink>
          <RouterLink to="/tasks">Tasks</RouterLink>
          <RouterLink to="/notifications">Notifications</RouterLink>
          <button class="nav-button" type="button" :disabled="isLoading" @click="handleLogout">
            Logout
          </button>
        </template>
      </nav>
    </header>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>
