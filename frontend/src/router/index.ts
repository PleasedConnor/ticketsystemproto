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
      path: '/ai-customisation',
      name: 'ai-customisation',
      component: () => import('../views/AIConfigurationView.vue')
    },
    {
      path: '/ai-data-access',
      name: 'ai-data-access',
      component: () => import('../views/AIDataAccessView.vue')
    },
    {
      path: '/chatbot',
      name: 'chatbot',
      component: () => import('../views/ChatbotView.vue')
    },
    {
      path: '/knowledge-base',
      name: 'knowledge-base',
      component: () => import('../views/KnowledgeBaseView.vue')
    },
    {
      path: '/metadata-configuration',
      redirect: '/metadata-configuration/inbound'
    },
    {
      path: '/metadata-configuration/inbound',
      name: 'inbound-metadata',
      component: () => import('../components/metadata/InboundMetadataView.vue')
    },
    {
      path: '/metadata-configuration/chat-fields',
      name: 'chat-fields',
      component: () => import('../components/metadata/ChatFieldsView.vue')
    },
    {
      path: '/metadata-configuration/ticket-fields',
      name: 'ticket-fields',
      component: () => import('../components/metadata/TicketFieldsView.vue')
    },
    {
      path: '/metadata-configuration/mapping/:id',
      name: 'metadata-mapping',
      component: () => import('../views/MetadataMappingView.vue')
    }
  ]
})

export default router
