import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000, // Increased to 30 seconds for AI operations
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token here if needed
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      // router.push('/login')
    }
    return Promise.reject(error)
  }
)

export interface User {
  uid: string
  name: string
  email: string
  location: string
  device: string
  phoneNumber: string
  createdAt: string
}

export interface TicketMessage {
  id: number
  message: string
  senderType: 'USER' | 'AGENT' | 'SYSTEM'
  senderName: string
  createdAt: string
}

export interface Ticket {
  id: number
  subject: string
  description: string
  status: 'OPEN' | 'IN_PROGRESS' | 'PENDING' | 'RESOLVED' | 'CLOSED'
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  category?: 'DEPOSITS' | 'WITHDRAWALS' | 'ACCOUNT' | 'VERIFICATION' | 'PAYMENTS' | 'TECHNICAL' | 'GENERAL'
  user: User
  createdAt: string
  updatedAt: string
  messages?: TicketMessage[]
}

export function useBackendApi() {
  const checkHealth = () => {
    return api.get('/public/health')
  }

  const getVersion = () => {
    return api.get('/public/version')
  }

  // Ticket API methods
  const getAllTickets = () => {
    return api.get<Ticket[]>('/tickets')
  }

  const getTicketById = (id: number) => {
    return api.get<Ticket>(`/tickets/${id}`)
  }

  const getTicketMessages = (ticketId: number) => {
    return api.get<TicketMessage[]>(`/tickets/${ticketId}/messages`)
  }

  const addTicketMessage = (ticketId: number, message: Partial<TicketMessage>) => {
    return api.post<TicketMessage>(`/tickets/${ticketId}/messages`, message)
  }

  const updateTicketStatus = (ticketId: number, status: string, priority?: string) => {
    return api.put<Ticket>(`/tickets/${ticketId}`, { status, priority })
  }

  // AI API methods
  const generateAIResponse = (ticketId: number, message: string, addToTicket: boolean = true) => {
    return api.post<{response: string, message: TicketMessage | null}>(`/ai/tickets/${ticketId}/generate-response`, {
      message,
      addToTicket
    })
  }

  const getConversationSentiment = (ticketId: number) => {
    return api.get<{score: number, label: string}>(`/ai/tickets/${ticketId}/sentiment`)
  }

  const analyzeMessageSentiment = (message: string) => {
    return api.post<{score: number, label: string}>('/ai/sentiment', { text: message })
  }

  const generateChatbotResponse = (message: string, conversationHistory: string) => {
    return api.post<{response: string}>('/ai/chatbot/response', { message, conversationHistory })
  }

  // User API methods
  const getAllUsers = () => {
    return api.get<User[]>('/users')
  }

  const getUserById = (uid: string) => {
    return api.get<User>(`/users/${uid}`)
  }

  const getUserTickets = (uid: string) => {
    return api.get<Ticket[]>(`/users/${uid}/tickets`)
  }

  // Analytics API methods
  const executeAnalyticsQuery = (query: any) => {
    return api.post<any[]>('/analytics/query', query)
  }

  const getTicketMetrics = () => {
    return api.get<any>('/analytics/metrics/tickets')
  }

  const getUserMetrics = () => {
    return api.get<any>('/analytics/metrics/users')
  }

  const getMessageMetrics = () => {
    return api.get<any>('/analytics/metrics/messages')
  }

  const getSentimentMetrics = () => {
    return api.get<any>('/analytics/metrics/sentiment')
  }

  const getLocationDistribution = () => {
    return api.get<any[]>('/analytics/distribution/location')
  }

  const getDeviceDistribution = () => {
    return api.get<any[]>('/analytics/distribution/device')
  }

  const getStatusDistribution = () => {
    return api.get<any[]>('/analytics/distribution/status')
  }

  const getDailyTrends = () => {
    return api.get<any[]>('/analytics/trends/daily')
  }

  const getAdvancedMetrics = () => {
    return api.get<any>('/analytics/metrics/advanced')
  }

  const getPerformanceMetrics = () => {
    return api.get<any[]>('/analytics/metrics/performance')
  }

  const getHourlyActivity = () => {
    return api.get<any[]>('/analytics/trends/hourly')
  }

  const getSentimentTrends = () => {
    return api.get<any[]>('/analytics/trends/sentiment')
  }

  const getAverageResolutionTime = () => {
    return api.get('/analytics/metrics/resolution-time')
  }

  const analyzeDashboard = (widgets: any[]) => {
    return api.post('/analytics/dashboard/analyze', { widgets })
  }

  const queryDashboard = (widgets: any[], query: string) => {
    return api.post('/analytics/dashboard/query', { widgets, query })
  }

  // Simulation API methods
  const getTicketsForSimulation = (request: any) => {
    return api.post('/simulation/tickets', request)
  }

  const runSimulation = (request: any) => {
    return api.post('/simulation/run', request)
  }

  const getSimulationResults = (runId: string) => {
    return api.get(`/simulation/results/${runId}`)
  }

  const updateSimulationResult = (resultId: number, request: any) => {
    return api.put(`/simulation/results/${resultId}`, request)
  }

  const simulateSingleTicket = (ticketId: number, modelName: string, runId: string) => {
    return api.post('/simulation/simulate-ticket', { ticketId, modelName, runId })
  }

  // AI Configuration API methods
  const getAIConfiguration = () => {
    return api.get('/ai-configuration')
  }

  const updateAIConfiguration = (configuration: any) => {
    return api.put('/ai-configuration', configuration)
  }

  // Chatbot API methods
  const createTicketFromChat = (request: any) => {
    return api.post('/tickets/from-chat', request)
  }

  return {
    checkHealth,
    getVersion,
    // Ticket methods
    getAllTickets,
    getTicketById,
    getTicketMessages,
    addTicketMessage,
    updateTicketStatus,
    // AI methods
    generateAIResponse,
    getConversationSentiment,
    analyzeMessageSentiment,
    generateChatbotResponse,
    // User methods
    getAllUsers,
    getUserById,
    getUserTickets,
    // Analytics methods
    executeAnalyticsQuery,
    getTicketMetrics,
    getUserMetrics,
    getMessageMetrics,
    getSentimentMetrics,
    getLocationDistribution,
    getDeviceDistribution,
    getStatusDistribution,
    getDailyTrends,
    getAdvancedMetrics,
    getPerformanceMetrics,
    getHourlyActivity,
    getSentimentTrends,
    getAverageResolutionTime,
    analyzeDashboard,
    queryDashboard,
    // Simulation methods
    getTicketsForSimulation,
    runSimulation,
    getSimulationResults,
    updateSimulationResult,
    simulateSingleTicket,
    // AI Configuration methods
    getAIConfiguration,
    updateAIConfiguration,
    // Chatbot methods
    createTicketFromChat
  }
}
