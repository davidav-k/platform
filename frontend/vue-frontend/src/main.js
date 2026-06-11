import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import { initializeAuth } from './services/authState'
import './assets/main.css'

createApp(App).use(router).mount('#app')

initializeAuth()
