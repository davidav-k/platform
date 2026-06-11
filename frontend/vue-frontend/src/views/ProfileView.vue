<script setup>
import { onMounted } from 'vue'

import EmptyState from '../components/EmptyState.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import LoadingIndicator from '../components/LoadingIndicator.vue'
import {
  authError,
  currentUser,
  isLoading,
  loadCurrentUser,
} from '../services/authState'

function loadProfile() {
  return loadCurrentUser().catch(() => null)
}

onMounted(() => {
  if (!currentUser.value) {
    loadProfile()
  }
})
</script>

<template>
  <section>
    <h1>Profile</h1>

    <LoadingIndicator v-if="isLoading" message="Loading profile..." />
    <ErrorMessage
      v-else-if="authError"
      :message="authError"
      retry-label="Retry"
      @retry="loadProfile"
    />

    <dl v-else-if="currentUser" class="profile-details">
      <dt>Email</dt>
      <dd>{{ currentUser.email || 'Not provided' }}</dd>

      <dt>First name</dt>
      <dd>{{ currentUser.firstName || 'Not provided' }}</dd>

      <dt>Last name</dt>
      <dd>{{ currentUser.lastName || 'Not provided' }}</dd>

      <dt>Role</dt>
      <dd>{{ currentUser.role || currentUser.roles || 'Not provided' }}</dd>

      <template v-if="currentUser.authorities">
        <dt>Authorities</dt>
        <dd>{{ currentUser.authorities }}</dd>
      </template>
    </dl>

    <EmptyState
      v-else
      title="Profile unavailable"
      message="You are not authenticated. Please sign in to view your profile."
    />
  </section>
</template>
