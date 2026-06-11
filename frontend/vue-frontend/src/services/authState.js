import { computed, reactive } from 'vue'

import { ApiError, getUserFriendlyError } from './apiClient'
import { login as loginRequest, logout as logoutRequest } from './authService'
import { getProfile } from './profileService'

const state = reactive({
  currentUser: null,
  isLoading: true,
  error: null,
  hasTriedInitialProfileLoad: false,
})

let profileRequest = null

export const currentUser = computed(() => state.currentUser)
export const isAuthenticated = computed(() => Boolean(state.currentUser))
export const isLoading = computed(() => state.isLoading)
export const authError = computed(() => state.error)
export const hasTriedInitialProfileLoad = computed(() => state.hasTriedInitialProfileLoad)

function responseUser(response) {
  return response?.data?.user || response?.user || null
}

function userMessage(error, fallback, unauthorizedMessage = 'Your session is not authorized.') {
  return getUserFriendlyError(error, {
    fallback,
    statusMessages: {
      400: 'The submitted authentication data is invalid.',
      401: unauthorizedMessage,
      403: 'Access is not allowed for this account.',
      409: 'The account state does not allow this operation.',
      500: 'The server is unavailable. Please try again later.',
    },
  })
}

export function clearAuthError() {
  state.error = null
}

export async function loadCurrentUser({ silentUnauthorized = false } = {}) {
  if (profileRequest) {
    return profileRequest
  }

  state.isLoading = true
  state.error = null

  profileRequest = getProfile()
    .then((response) => {
      const user = responseUser(response)

      if (!user) {
        throw new Error('Profile response does not contain a user.')
      }

      state.currentUser = user
      return user
    })
    .catch((error) => {
      state.currentUser = null

      if (!(silentUnauthorized && error instanceof ApiError && [401, 403].includes(error.status))) {
        state.error = userMessage(
          error,
          'Unable to load the user profile.',
          'Your session has expired. Please sign in again.',
        )
      }

      throw error
    })
    .finally(() => {
      state.isLoading = false
      profileRequest = null
    })

  return profileRequest
}

export async function login(credentials) {
  state.isLoading = true
  state.error = null

  try {
    const response = await loginRequest(credentials)
    const loginUser = responseUser(response)

    if (loginUser?.mfa) {
      state.currentUser = null
      state.error = 'MFA verification is required. MFA support will be added in a later step.'
      return null
    }

    return await loadCurrentUser()
  } catch (error) {
    if (!state.error) {
      state.error = userMessage(
        error,
        'Login failed. Please try again.',
        'The email or password is incorrect.',
      )
    }

    throw error
  } finally {
    state.isLoading = false
  }
}

export async function logout() {
  state.isLoading = true
  state.error = null

  try {
    await logoutRequest()
  } finally {
    state.currentUser = null
    state.isLoading = false
  }
}

export function initializeAuth() {
  if (state.hasTriedInitialProfileLoad) {
    return Promise.resolve(state.currentUser)
  }

  return loadCurrentUser({ silentUnauthorized: true })
    .catch(() => null)
    .finally(() => {
      state.hasTriedInitialProfileLoad = true
    })
}
