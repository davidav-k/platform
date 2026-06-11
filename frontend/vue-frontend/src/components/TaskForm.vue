<script setup>
import { reactive, ref } from 'vue'

const props = defineProps({
  isSubmitting: {
    type: Boolean,
    default: false,
  },
  serverErrors: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['submit'])

const form = reactive({
  title: '',
  description: '',
  priority: '',
  assigneeUserId: '',
})
const validationErrors = ref({})

function isUuid(value) {
  return /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(value)
}

function validate() {
  const errors = {}
  const title = form.title.trim()
  const assigneeUserId = form.assigneeUserId.trim()

  if (!title) {
    errors.title = 'Title is required.'
  } else if (title.length > 200) {
    errors.title = 'Title must not exceed 200 characters.'
  }

  if (form.description.length > 5000) {
    errors.description = 'Description must not exceed 5000 characters.'
  }

  if (assigneeUserId && !isUuid(assigneeUserId)) {
    errors.assigneeUserId = 'Assignee user ID must be a valid UUID.'
  }

  validationErrors.value = errors
  return Object.keys(errors).length === 0
}

function fieldError(field) {
  return validationErrors.value[field] || props.serverErrors[field] || null
}

function handleSubmit() {
  if (props.isSubmitting || !validate()) {
    return
  }

  emit('submit', {
    title: form.title.trim(),
    description: form.description || undefined,
    priority: form.priority || undefined,
    assigneeUserId: form.assigneeUserId.trim() || undefined,
  })
}
</script>

<template>
  <form class="task-form" novalidate @submit.prevent="handleSubmit">
    <div class="form-field">
      <label for="task-title">Title</label>
      <input
        id="task-title"
        v-model="form.title"
        name="title"
        type="text"
        maxlength="200"
        required
        :disabled="isSubmitting"
        :aria-invalid="Boolean(fieldError('title'))"
        aria-describedby="task-title-error"
      />
      <p v-if="fieldError('title')" id="task-title-error" class="field-error">
        {{ fieldError('title') }}
      </p>
    </div>

    <div class="form-field">
      <label for="task-description">Description</label>
      <textarea
        id="task-description"
        v-model="form.description"
        name="description"
        rows="6"
        maxlength="5000"
        :disabled="isSubmitting"
        :aria-invalid="Boolean(fieldError('description'))"
        aria-describedby="task-description-error"
      />
      <p v-if="fieldError('description')" id="task-description-error" class="field-error">
        {{ fieldError('description') }}
      </p>
    </div>

    <div class="form-field">
      <label for="task-priority">Priority</label>
      <select id="task-priority" v-model="form.priority" name="priority" :disabled="isSubmitting">
        <option value="">Medium (default)</option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
      </select>
      <p v-if="fieldError('priority')" class="field-error">{{ fieldError('priority') }}</p>
    </div>

    <div class="form-field">
      <label for="task-assignee">Assignee user ID</label>
      <input
        id="task-assignee"
        v-model="form.assigneeUserId"
        name="assigneeUserId"
        type="text"
        placeholder="Optional UUID"
        :disabled="isSubmitting"
        :aria-invalid="Boolean(fieldError('assigneeUserId'))"
        aria-describedby="task-assignee-error"
      />
      <p v-if="fieldError('assigneeUserId')" id="task-assignee-error" class="field-error">
        {{ fieldError('assigneeUserId') }}
      </p>
    </div>

    <button type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? 'Creating task...' : 'Create task' }}
    </button>
  </form>
</template>
