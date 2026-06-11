<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import {
  authError,
  clearAuthError,
  isLoading,
  login,
} from '../services/authState'

const router = useRouter()
const email = ref('')
const password = ref('')

async function handleSubmit() {
  clearAuthError()

  try {
    const user = await login({
      email: email.value.trim(),
      password: password.value,
    })

    if (user) {
      await router.push('/profile')
    }
  } catch {
    // authState exposes a user-safe message.
  }
}
</script>

<template>
  <section class="form-panel">
    <h1>Login</h1>

    <form class="auth-form" @submit.prevent="handleSubmit">
      <label for="email">Email</label>
      <input
        id="email"
        v-model="email"
        name="email"
        type="email"
        autocomplete="email"
        required
        :disabled="isLoading"
      />

      <label for="password">Password</label>
      <input
        id="password"
        v-model="password"
        name="password"
        type="password"
        autocomplete="current-password"
        required
        :disabled="isLoading"
      />

      <ErrorMessage v-if="authError" :message="authError" />

      <LoadingIndicator v-if="isLoading" message="Signing in..." />

      <button type="submit" :disabled="isLoading">
        {{ isLoading ? 'Signing in...' : 'Sign in' }}
      </button>
    </form>
  </section>
</template>
