import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import NotificationDetailsView from '../views/NotificationDetailsView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import ProfileView from '../views/ProfileView.vue'
import TaskCreateView from '../views/TaskCreateView.vue'
import TaskDetailsView from '../views/TaskDetailsView.vue'
import TaskEditView from '../views/TaskEditView.vue'
import TaskListView from '../views/TaskListView.vue'
import {
  hasTriedInitialProfileLoad,
  initializeAuth,
  isAuthenticated,
} from '../services/authState'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
    { path: '/profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
    { path: '/tasks', name: 'tasks', component: TaskListView, meta: { requiresAuth: true } },
    {
      path: '/tasks/create',
      name: 'task-create',
      component: TaskCreateView,
      meta: { requiresAuth: true },
    },
    {
      path: '/tasks/:id/edit',
      name: 'task-edit',
      component: TaskEditView,
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/tasks/:id',
      name: 'task-details',
      component: TaskDetailsView,
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: NotificationsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications/:id',
      name: 'notification-details',
      component: NotificationDetailsView,
      props: true,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  if (!hasTriedInitialProfileLoad.value) {
    await initializeAuth()
  }

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { name: 'login' }
  }

  if (to.meta.guestOnly && isAuthenticated.value) {
    return { name: 'profile' }
  }

  return true
})

export default router
