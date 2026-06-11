<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  mode: {
    type: String,
    default: 'create',
    validator: (value) => ['create', 'edit'].includes(value),
  },
  initialValues: {
    type: Object,
    default: () => ({}),
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
  serverErrors: {
    type: Object,
    default: () => ({}),
  },
  showAssignee: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['cancel', 'submit'])

const form = reactive({
  title: '',
  description: '',
  priority: '',
  assigneeUserId: '',
})
const validationErrors = ref({})

watch(
  () => props.initialValues,
  (values) => {
    form.title = values.title ?? ''
    form.description = values.description ?? ''
    form.priority = values.priority ?? ''
    form.assigneeUserId = values.assigneeUserId ?? ''
    validationErrors.value = {}
  },
  { immediate: true },
)

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

  if (props.mode === 'edit' && !form.priority) {
    errors.priority = 'Priority is required.'
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

  const task = {
    title: form.title.trim(),
    description: form.description || (props.mode === 'edit' ? null : undefined),
    priority: form.priority || undefined,
  }

  if (props.showAssignee) {
    task.assigneeUserId = form.assigneeUserId.trim() || undefined
  }

  emit('submit', task)
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
        <option value="" :disabled="mode === 'edit'">
          {{ mode === 'edit' ? 'Select priority' : 'Medium (default)' }}
        </option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
      </select>
      <p v-if="fieldError('priority')" class="field-error">{{ fieldError('priority') }}</p>
    </div>

    <div v-if="showAssignee" class="form-field">
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

    <div class="form-actions">
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? (mode === 'edit' ? 'Saving changes...' : 'Creating task...') : (mode === 'edit' ? 'Save changes' : 'Create task') }}
      </button>
      <button
        v-if="mode === 'edit'"
        class="secondary-button"
        type="button"
        :disabled="isSubmitting"
        @click="emit('cancel')"
      >
        Cancel
      </button>
    </div>
  </form>
</template>
