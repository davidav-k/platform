import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import ProfileView from '../views/ProfileView.vue'
import TaskDetailsView from '../views/TaskDetailsView.vue'
import TaskListView from '../views/TaskListView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/tasks', name: 'tasks', component: TaskListView },
    { path: '/tasks/:id', name: 'task-details', component: TaskDetailsView, props: true },
    { path: '/notifications', name: 'notifications', component: NotificationsView },
  ],
})

export default router
