import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

// Global error handler for unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
  // Prevent the default behavior (which logs the error to console)
  event.preventDefault()
})

const app = createApp(App)

// Global Vue error handler
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err)
  console.error('Component instance:', instance)
  console.error('Error info:', info)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
