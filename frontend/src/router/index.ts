import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue')
    },
    {
      path: '/tickets',
      name: 'tickets',
      component: () => import('../views/TicketsView.vue')
    },
    {
      path: '/analytics',
      name: 'analytics',
      component: () => import('../views/AnalyticsView.vue')
    },
    {
      path: '/simulation',
      name: 'simulation',
      component: () => import('../views/SimulationView.vue')
    },
    {
      path: '/ai-configuration',
      name: 'ai-configuration',
      component: () => import('../views/AIConfigurationView.vue')
    },
    {
      path: '/chatbot',
      name: 'chatbot',
      component: () => import('../views/ChatbotView.vue')
    }
  ]
})

export default router
