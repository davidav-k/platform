<script setup>
import { onMounted } from 'vue'

import {
  authError,
  currentUser,
  isLoading,
  loadCurrentUser,
} from '../services/authState'

onMounted(() => {
  loadCurrentUser().catch(() => null)
})
</script>

<template>
  <section>
    <h1>Profile</h1>

    <p v-if="isLoading">Loading profile...</p>
    <p v-else-if="authError" class="error-message" role="alert">{{ authError }}</p>

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

    <p v-else>You are not authenticated. Please sign in to view your profile.</p>
  </section>
</template>
