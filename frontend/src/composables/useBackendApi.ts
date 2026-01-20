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

  // AI Rule API methods
  const getAllAIRules = () => {
    return api.get('/ai/rules')
  }

  const getAIRulesByCategory = (category: string) => {
    return api.get(`/ai/rules/category/${category}`)
  }

  const getAIRule = (id: number) => {
    return api.get(`/ai/rules/${id}`)
  }

  const createAIRule = (rule: any) => {
    return api.post('/ai/rules', rule)
  }

  const updateAIRule = (id: number, rule: any) => {
    return api.put(`/ai/rules/${id}`, rule)
  }

  const deleteAIRule = (id: number) => {
    return api.delete(`/ai/rules/${id}`)
  }

  const reorderAIRules = (rules: any[]) => {
    return api.post('/ai/rules/reorder', rules)
  }

  // Knowledge Base API methods
  const getAllCategories = () => {
    return api.get('/knowledge-base/categories')
  }

  const getCategoryById = (id: number) => {
    return api.get(`/knowledge-base/categories/${id}`)
  }

  const createCategory = (category: any) => {
    return api.post('/knowledge-base/categories', category)
  }

  const updateCategory = (id: number, category: any) => {
    return api.put(`/knowledge-base/categories/${id}`, category)
  }

  const deleteCategory = (id: number) => {
    return api.delete(`/knowledge-base/categories/${id}`)
  }

  const getAllArticles = () => {
    return api.get('/knowledge-base/articles')
  }

  const getActiveArticles = () => {
    return api.get('/knowledge-base/articles/active')
  }

  const getArticlesByCategory = (categoryId: number) => {
    return api.get(`/knowledge-base/articles/category/${categoryId}`)
  }

  const getArticleById = (id: number) => {
    return api.get(`/knowledge-base/articles/${id}`)
  }

  const createArticle = (article: any) => {
    return api.post('/knowledge-base/articles', article)
  }

  const updateArticle = (id: number, article: any) => {
    return api.put(`/knowledge-base/articles/${id}`, article)
  }

  const deleteArticle = (id: number) => {
    return api.delete(`/knowledge-base/articles/${id}`)
  }

  const searchArticles = (query: string) => {
    return api.post('/knowledge-base/search', { query })
  }

  // Chatbot API methods
  const createTicketFromChat = (request: any) => {
    return api.post('/tickets/from-chat', request)
  }

  // Metadata API methods
  const getAllMetadataConnections = () => {
    return api.get('/metadata/connections')
  }

  const getActiveMetadataConnections = () => {
    return api.get('/metadata/connections/active')
  }

  const getMetadataConnection = (id: number) => {
    return api.get(`/metadata/connections/${id}`)
  }

  const createMetadataConnection = (connection: any) => {
    return api.post('/metadata/connections', connection)
  }

  const updateMetadataConnection = (id: number, connection: any) => {
    return api.put(`/metadata/connections/${id}`, connection)
  }

  const deleteMetadataConnection = (id: number) => {
    return api.delete(`/metadata/connections/${id}`)
  }

  const testMetadataConnection = (id: number) => {
    return api.post(`/metadata/connections/${id}/test`)
  }

  const getMetadataMappings = (connectionId: number) => {
    return api.get(`/metadata/connections/${connectionId}/mappings`)
  }

  const saveMetadataMappings = (connectionId: number, mappings: any[]) => {
    return api.post(`/metadata/connections/${connectionId}/mappings`, mappings)
  }

  const createMetadataMapping = (mapping: any) => {
    return api.post('/metadata/mappings', mapping)
  }

  const updateMetadataMapping = (id: number, mapping: any) => {
    return api.put(`/metadata/mappings/${id}`, mapping)
  }

  const getAllMappingsForAIAccess = () => {
    return api.get('/metadata/mappings/ai-access')
  }

  const updateAIAccess = (mappingId: number, aiAccessible: boolean) => {
    return api.put(`/metadata/mappings/${mappingId}/ai-access`, { aiAccessible })
  }

  const bulkUpdateAIAccess = (accessMap: Record<number, boolean>) => {
    return api.put('/metadata/mappings/ai-access/bulk', accessMap)
  }

  const testMappingValue = (mappingId: number, ticketId?: number) => {
    const params = ticketId ? `?ticketId=${ticketId}` : ''
    return api.get(`/metadata/mappings/${mappingId}/test-value${params}`)
  }

  // Customer Profile methods
  const getCustomerProfileFields = () => {
    return api.get('/customer-profile/fields')
  }

  const getAllCustomerProfileFields = () => {
    return api.get('/customer-profile/fields/all')
  }

  const getCustomerProfileData = (ticketId: number) => {
    return api.get(`/customer-profile/data/${ticketId}`)
  }

  const createCustomerProfileField = (field: any) => {
    return api.post('/customer-profile/fields', field)
  }

  const updateCustomerProfileField = (id: number, field: any) => {
    return api.put(`/customer-profile/fields/${id}`, field)
  }

  const deleteCustomerProfileField = (id: number) => {
    return api.delete(`/customer-profile/fields/${id}`)
  }

  // Ticket Fields methods
  const getAllTicketFields = () => {
    return api.get('/ticket-fields/fields/all')
  }

  const getTicketFieldsByLocation = (location: string) => {
    return api.get(`/ticket-fields/fields/location/${location}`)
  }

  const getTicketFieldData = (ticketId: number, location: string) => {
    return api.get(`/ticket-fields/data/${ticketId}/${location}`)
  }

  const createTicketField = (field: any) => {
    return api.post('/ticket-fields/fields', field)
  }

  const updateTicketField = (id: number, field: any) => {
    return api.put(`/ticket-fields/fields/${id}`, field)
  }

  const deleteTicketField = (id: number) => {
    return api.delete(`/ticket-fields/fields/${id}`)
  }

  const deleteMetadataMapping = (id: number) => {
    return api.delete(`/metadata/mappings/${id}`)
  }

  const getAvailableMetadataVariables = (excludeVariable?: string) => {
    const params = excludeVariable ? { excludeVariable } : {}
    return api.get('/metadata/variables/available', { params })
  }

  // Third Party Integration methods
  const getAllThirdPartyIntegrations = () => {
    return api.get('/third-party-integrations')
  }

  const getActiveThirdPartyIntegrations = () => {
    return api.get('/third-party-integrations/active')
  }

  const getThirdPartyIntegration = (id: number) => {
    return api.get(`/third-party-integrations/${id}`)
  }

  const createThirdPartyIntegration = (integration: any) => {
    return api.post('/third-party-integrations', integration)
  }

  const updateThirdPartyIntegration = (id: number, integration: any) => {
    return api.put(`/third-party-integrations/${id}`, integration)
  }

  const deleteThirdPartyIntegration = (id: number) => {
    return api.delete(`/third-party-integrations/${id}`)
  }

  const updateThirdPartyAIAccess = (id: number, aiAccessible: boolean) => {
    return api.put(`/third-party-integrations/${id}/ai-access`, { aiAccessible })
  }

  const testThirdPartyConnection = (id: number) => {
    return api.post(`/third-party-integrations/${id}/test-connection`)
  }

  const fetchThirdPartyItems = (id: number, itemType: string = 'PAGE') => {
    return api.get(`/third-party-integrations/${id}/items`, { params: { itemType } })
  }

  const getBlacklistedItems = (id: number) => {
    return api.get(`/third-party-integrations/${id}/blacklist`)
  }

  const addToBlacklist = (id: number, item: any) => {
    return api.post(`/third-party-integrations/${id}/blacklist`, item)
  }

  const bulkAddToBlacklist = (id: number, items: any[]) => {
    return api.post(`/third-party-integrations/${id}/blacklist/bulk`, items)
  }

  const removeFromBlacklist = (id: number, itemId: string) => {
    return api.delete(`/third-party-integrations/${id}/blacklist/${itemId}`)
  }

  const generateOAuthUrl = (request: any) => {
    return api.post('/third-party-integrations/oauth/authorize', request)
  }

  const exchangeOAuthToken = (request: any) => {
    return api.post('/third-party-integrations/oauth/token', request)
  }

  const getAvailableIntegrations = () => {
    return api.get('/third-party-integrations/available')
  }

  const getOAuthAppConfig = (integrationType: string) => {
    return api.get(`/third-party-integrations/oauth-config/${integrationType}`)
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
    // AI Rule methods
    getAllAIRules,
    getAIRulesByCategory,
    getAIRule,
    createAIRule,
    updateAIRule,
    deleteAIRule,
    reorderAIRules,
    // Knowledge Base methods
    getAllCategories,
    getCategoryById,
    createCategory,
    updateCategory,
    deleteCategory,
    getAllArticles,
    getActiveArticles,
    getArticlesByCategory,
    getArticleById,
    createArticle,
    updateArticle,
    deleteArticle,
    searchArticles,
    // Chatbot methods
    createTicketFromChat,
    // Metadata methods
    getAllMetadataConnections,
    getActiveMetadataConnections,
    getMetadataConnection,
    createMetadataConnection,
    updateMetadataConnection,
    deleteMetadataConnection,
    testMetadataConnection,
    getMetadataMappings,
    saveMetadataMappings,
    createMetadataMapping,
    updateMetadataMapping,
    deleteMetadataMapping,
    getAllMappingsForAIAccess,
    updateAIAccess,
    bulkUpdateAIAccess,
    testMappingValue,
    // Customer Profile methods
    getCustomerProfileFields,
    getAllCustomerProfileFields,
    getCustomerProfileData,
    createCustomerProfileField,
    updateCustomerProfileField,
    deleteCustomerProfileField,
    // Ticket Fields methods
    getAllTicketFields,
    getTicketFieldsByLocation,
    getTicketFieldData,
    createTicketField,
    updateTicketField,
    deleteTicketField,
    getAvailableMetadataVariables,
    // Third Party Integration methods
    getAllThirdPartyIntegrations,
    getActiveThirdPartyIntegrations,
    getThirdPartyIntegration,
    createThirdPartyIntegration,
    updateThirdPartyIntegration,
    deleteThirdPartyIntegration,
    updateThirdPartyAIAccess,
    testThirdPartyConnection,
    fetchThirdPartyItems,
    getBlacklistedItems,
    addToBlacklist,
    bulkAddToBlacklist,
    removeFromBlacklist,
    generateOAuthUrl,
    exchangeOAuthToken,
    getAvailableIntegrations,
    getOAuthAppConfig
  }
}
