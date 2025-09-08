<template>
  <div class="analytics-container">
    <!-- Title Header Bar -->
    <div class="analytics-title-bar">
      <h1>Analytics Dashboard</h1>
    </div>

    <!-- Sub-header with Create Button -->
    <div class="analytics-sub-header">
      <div class="dashboard-controls">
        <button @click="showDashboardBuilder = true" class="btn btn-primary btn-small">
          + Create Dashboard
        </button>
      </div>
    </div>

    <div class="analytics-layout" v-if="dashboards.length > 0">
      <!-- Left Sidebar with Dashboard Tabs -->
      <div class="dashboard-sidebar">
        <div class="sidebar-header">
          <h3>Your Dashboards</h3>
        </div>
        <div class="dashboard-list">
          <div 
            v-for="dashboard in dashboards"
            :key="dashboard.id"
            @click="activeDashboard = dashboard.id"
            :class="['dashboard-item', { active: activeDashboard === dashboard.id }]"
          >
            <div class="dashboard-item-header">
              <span class="dashboard-name">{{ dashboard.name }}</span>
              <button 
                @click.stop="deleteDashboard(dashboard.id)"
                class="delete-dashboard"
                title="Delete Dashboard"
              >
                ×
              </button>
            </div>
            <div 
              v-if="activeDashboard === dashboard.id && dashboard.description" 
              class="dashboard-description"
            >
              {{ dashboard.description }}
            </div>
          </div>
        </div>
      </div>

      <!-- Main Dashboard Content -->
      <div class="dashboard-main">
        <div class="dashboard-content" v-if="currentDashboard">
      <div class="dashboard-actions">
        <button @click="showWidgetBuilder = true" class="btn btn-secondary">
          + Add Widget
        </button>
        <button @click="refreshDashboard" class="btn btn-outline">
          🔄 Refresh Data
        </button>
        <button @click="openAIAnalysis" class="btn btn-primary" :disabled="!currentDashboard">
          🤖 Ask AI
        </button>
      </div>

      <div class="widgets-grid" :class="{ 'show-grid': draggingWidget }" :key="forceRenderKey">
        <div 
          v-for="widget in currentDashboard.widgets"
          :key="widget.id"
          :data-widget-id="widget.id"
          class="widget-container"
          :class="[
            `widget-${widget.size}`, 
            { 
              resizing: resizingWidget === widget.id,
              dragging: draggingWidget === widget.id 
            }
          ]"
          :style="getWidgetGridStyle(widget)"
        >
          <div 
            class="widget-header"
            @mousedown="startDrag(widget.id, $event)"
          >
            <div class="drag-handle">⋮⋮</div>
            <h3>{{ widget.title }}</h3>
            <div class="widget-actions">
              <button @click="editWidget(widget)" class="widget-action">⚙️</button>
              <button @click="deleteWidget(widget.id)" class="widget-action">×</button>
            </div>
          </div>
          
          <div class="widget-content">
            <component 
              :is="getWidgetComponent(widget.type)"
              :data="widget.data"
              :config="widget.config"
              :loading="widget.loading"
            />
          </div>

          <!-- Resize Handles -->
          <div 
            class="resize-handle resize-handle-se"
            @mousedown="startResize(widget.id, 'se', $event)"
          ></div>
          <div 
            class="resize-handle resize-handle-e"
            @mousedown="startResize(widget.id, 'e', $event)"
          ></div>
          <div 
            class="resize-handle resize-handle-s"
            @mousedown="startResize(widget.id, 's', $event)"
          ></div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="currentDashboard.widgets.length === 0" class="empty-dashboard">
        <h3>This dashboard is empty</h3>
        <p>Add your first widget to start visualizing your data</p>
        <button @click="showWidgetBuilder = true" class="btn btn-primary">
          + Add Your First Widget
        </button>
      </div>
        </div>
      </div>
    </div>

    <!-- No Dashboards State -->
    <div v-else class="no-dashboards">
      <h2>Welcome to Analytics!</h2>
      <p>Create your first dashboard to start analyzing your customer service data</p>
      <button @click="showDashboardBuilder = true" class="btn btn-primary">
        Create Your First Dashboard
      </button>
    </div>

    <!-- Dashboard Builder Modal -->
    <div v-if="showDashboardBuilder" class="modal-overlay" @click="showDashboardBuilder = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>{{ editingDashboard ? 'Edit Dashboard' : 'Create New Dashboard' }}</h2>
          <button @click="showDashboardBuilder = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>Dashboard Name</label>
            <input 
              v-model="dashboardForm.name" 
              type="text" 
              placeholder="e.g., Customer Support Overview"
              class="form-input"
            >
          </div>
          
          <div class="form-group">
            <label>Description</label>
            <textarea 
              v-model="dashboardForm.description" 
              placeholder="Brief description of this dashboard"
              class="form-textarea"
            ></textarea>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="showDashboardBuilder = false" class="btn btn-outline">Cancel</button>
          <button @click="saveDashboard" class="btn btn-primary">
            {{ editingDashboard ? 'Update' : 'Create' }} Dashboard
          </button>
        </div>
      </div>
    </div>

    <!-- Widget Builder Modal -->
    <div v-if="showWidgetBuilder" class="modal-overlay" @click="showWidgetBuilder = false">
      <div class="modal-content large" @click.stop>
        <div class="modal-header">
          <h2>{{ editingWidget ? 'Edit Widget' : 'Add New Widget' }}</h2>
          <button @click="showWidgetBuilder = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="widget-builder">
            <div class="builder-section">
              <h3>Widget Configuration</h3>
              
              <div class="form-group">
                <label>Widget Title</label>
                <input 
                  v-model="widgetForm.title" 
                  type="text" 
                  placeholder="e.g., Tickets by Status"
                  class="form-input"
                >
              </div>

              <div class="form-group">
                <label>Widget Type</label>
                <select v-model="widgetForm.type" class="form-select">
                  <option value="">Select widget type...</option>
                  <option value="metric">Metric Card</option>
                  <option value="bar-chart">Bar Chart</option>
                  <option value="pie-chart">Pie Chart</option>
                  <option value="line-chart">Line Chart</option>
                  <option value="table">Data Table</option>
                  <option value="map">Geographic Map</option>
                </select>
              </div>

              <div class="form-group">
                <label>Widget Size</label>
                <select v-model="widgetForm.size" class="form-select">
                  <option value="small">Small (1x1)</option>
                  <option value="medium">Medium (2x1)</option>
                  <option value="large">Large (2x2)</option>
                  <option value="wide">Wide (3x1)</option>
                </select>
              </div>
            </div>

            <div class="builder-section">
              <h3>Data Query</h3>
              
              <div class="form-group">
                <label>Data Source</label>
                <select v-model="widgetForm.dataSource" class="form-select" @change="updateQueryOptions">
                  <option value="">Select data source...</option>
                  <option value="tickets">Tickets</option>
                  <option value="users">Users</option>
                  <option value="messages">Messages</option>
                  <option value="sentiment">Sentiment Analysis</option>
                </select>
              </div>

              <div v-if="widgetForm.dataSource" class="query-builder">
                <div class="form-group">
                  <label>What to measure</label>
                  <select v-model="widgetForm.metric" class="form-select">
                    <option value="">Select metric...</option>
                    <option v-for="metric in availableMetrics" :key="metric.value" :value="metric.value">
                      {{ metric.label }}
                    </option>
                  </select>
                </div>

                <div class="form-group" v-if="availableGroupBy.length > 0">
                  <label>Group by (optional)</label>
                  <select v-model="widgetForm.groupBy" class="form-select">
                    <option value="">No grouping</option>
                    <option v-for="group in availableGroupBy" :key="group.value" :value="group.value">
                      {{ group.label }}
                    </option>
                  </select>
                </div>


              </div>
            </div>

            <div class="builder-section" v-if="widgetForm.type && widgetForm.dataSource">
              <h3>Preview</h3>
              <div class="widget-preview">
                <button @click="previewWidget" class="btn btn-outline">Generate Preview</button>
                <div v-if="previewData" class="preview-container">
                  <component 
                    :is="getWidgetComponent(widgetForm.type)"
                    :data="previewData"
                    :config="widgetForm"
                    :loading="false"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="showWidgetBuilder = false" class="btn btn-outline">Cancel</button>
          <button @click="saveWidget" class="btn btn-primary" :disabled="!isWidgetFormValid">
            {{ editingWidget ? 'Update' : 'Add' }} Widget
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- AI Analysis Modal - Using Teleport to render at body level -->
  <Teleport to="body" v-if="showAIAnalysis">
    <div class="ai-modal-overlay" @click="showAIAnalysis = false">
    <div class="ai-analysis-modal" @click.stop>
      <div class="ai-modal-header">
        <h3>🤖 AI Dashboard Analysis</h3>
        <button @click="showAIAnalysis = false" class="ai-close-btn">&times;</button>
      </div>
      
      <div class="modal-content">
        <!-- Custom Query Section -->
        <div class="query-section">
          <h4>💬 Ask a Question</h4>
          <div class="query-input-container">
            <textarea 
              v-model="customQuery" 
              placeholder="Ask me anything about your dashboard data... e.g., 'What does the high resolution time mean?' or 'Which location has the most activity?'"
              class="query-input"
              rows="3"
              @keydown.enter.ctrl="askCustomQuestion"
            ></textarea>
            <button 
              @click="askCustomQuestion" 
              class="query-btn"
              :disabled="!customQuery.trim() || queryingAI"
            >
              {{ queryingAI ? '🤔 Thinking...' : '🚀 Ask AI' }}
            </button>
          </div>
          
          <!-- Custom Query Result -->
          <div v-if="customQueryResult" class="query-result">
            <div class="query-question">
              <strong>Q:</strong> {{ customQueryResult.query }}
            </div>
            <div class="query-answer">
              <strong>A:</strong> {{ customQueryResult.response }}
            </div>
            <div class="query-timestamp">
              <small>{{ new Date(customQueryResult.timestamp).toLocaleString() }}</small>
            </div>
          </div>
        </div>

        <!-- Instructions Section -->
        <div v-if="!customQueryResult" class="instructions-section">
          <h4>📋 How to Use</h4>
          <div class="instructions">
            <p>Ask specific questions about your dashboard data to get AI-powered insights:</p>
            <ul>
              <li><strong>Performance:</strong> "Is our 9-hour resolution time good or bad?"</li>
              <li><strong>Trends:</strong> "What do these ticket trends tell us?"</li>
              <li><strong>Comparisons:</strong> "Which location needs more support?"</li>
              <li><strong>Recommendations:</strong> "How can we improve our metrics?"</li>
            </ul>
          </div>
        </div>

      </div>

      <div class="ai-modal-actions">
        <button @click="showAIAnalysis = false" class="btn btn-primary">Close</button>
      </div>
    </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

// Import widget components
import MetricWidget from '@/components/widgets/MetricWidget.vue'
import BarChartWidget from '@/components/widgets/BarChartWidget.vue'
import PieChartWidget from '@/components/widgets/PieChartWidget.vue'
import LineChartWidget from '@/components/widgets/LineChartWidget.vue'
import TableWidget from '@/components/widgets/TableWidget.vue'
import MapWidget from '@/components/widgets/MapWidget.vue'

const { 
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
  queryDashboard
} = useBackendApi()

// Reactive state
const dashboards = ref<Dashboard[]>([])
const activeDashboard = ref<string | null>(null)
const showDashboardBuilder = ref(false)
const showWidgetBuilder = ref(false)
const editingDashboard = ref<Dashboard | null>(null)
const editingWidget = ref<Widget | null>(null)
const previewData = ref<any>(null)
const resizingWidget = ref<string | null>(null)
const draggingWidget = ref<string | null>(null)
const forceRenderKey = ref(0)
const showAIAnalysis = ref(false)
const customQuery = ref('')
const queryingAI = ref(false)
const customQueryResult = ref<any>(null)
const resizeData = ref<{
  widgetId: string
  direction: string
  startX: number
  startY: number
  startWidth: number
  startHeight: number
} | null>(null)
const dragData = ref<{
  widgetId: string
  startX: number
  startY: number
  startGridX: number
  startGridY: number
  offsetX: number
  offsetY: number
} | null>(null)

// Form state
const dashboardForm = ref({
  name: '',
  description: ''
})

const widgetForm = ref({
  title: '',
  type: '',
  size: 'medium',
  dataSource: '',
  metric: '',
  groupBy: ''
})

// Computed properties
const currentDashboard = computed(() => {
  return dashboards.value.find(d => d.id === activeDashboard.value)
})

const isWidgetFormValid = computed(() => {
  return widgetForm.value.title && widgetForm.value.type && widgetForm.value.dataSource && widgetForm.value.metric
})

const availableMetrics = computed(() => {
  const metrics: Record<string, Array<{value: string, label: string}>> = {
    tickets: [
      { value: 'count', label: 'Total Count' },
      { value: 'avg_resolution_time', label: 'Average Resolution Time' },
      { value: 'open_count', label: 'Open Tickets' },
      { value: 'resolved_count', label: 'Resolved Tickets' }
    ],
    users: [
      { value: 'count', label: 'Total Users' },
      { value: 'active_count', label: 'Active Users' }
    ],
    messages: [
      { value: 'count', label: 'Total Messages' },
      { value: 'avg_per_ticket', label: 'Average Messages per Ticket' }
    ],
    sentiment: [
      { value: 'avg_score', label: 'Average Sentiment Score' },
      { value: 'positive_count', label: 'Positive Messages' },
      { value: 'negative_count', label: 'Negative Messages' }
    ]
  }
  return metrics[widgetForm.value.dataSource] || []
})

const availableGroupBy = computed(() => {
  const groupBy: Record<string, Array<{value: string, label: string}>> = {
    tickets: [
      { value: 'status', label: 'Status' },
      { value: 'priority', label: 'Priority' },
      { value: 'user.location', label: 'Customer Location' },
      { value: 'user.device', label: 'Customer Device' },
      { value: 'created_date', label: 'Created Date' }
    ],
    users: [
      { value: 'location', label: 'Location' },
      { value: 'device', label: 'Device Type' }
    ],
    messages: [
      { value: 'senderType', label: 'Sender Type' },
      { value: 'created_date', label: 'Date' }
    ],
    sentiment: [
      { value: 'label', label: 'Sentiment Label' },
      { value: 'ticket.status', label: 'Ticket Status' }
    ]
  }
  return groupBy[widgetForm.value.dataSource] || []
})



// Methods
const getWidgetComponent = (type: string) => {
  const components: Record<string, any> = {
    'metric': MetricWidget,
    'bar-chart': BarChartWidget,
    'pie-chart': PieChartWidget,
    'line-chart': LineChartWidget,
    'table': TableWidget,
    'map': MapWidget
  }
  return components[type] || MetricWidget
}

const saveDashboard = () => {
  const dashboard: Dashboard = {
    id: editingDashboard.value?.id || generateId(),
    name: dashboardForm.value.name,
    description: dashboardForm.value.description,
    widgets: editingDashboard.value?.widgets || [],
    createdAt: new Date().toISOString()
  }

  if (editingDashboard.value) {
    const index = dashboards.value.findIndex(d => d.id === editingDashboard.value!.id)
    dashboards.value[index] = dashboard
  } else {
    dashboards.value.push(dashboard)
    activeDashboard.value = dashboard.id
  }

  saveDashboardsToStorage()
  resetDashboardForm()
  showDashboardBuilder.value = false
}

const deleteDashboard = (id: string) => {
  if (confirm('Are you sure you want to delete this dashboard?')) {
    dashboards.value = dashboards.value.filter(d => d.id !== id)
    if (activeDashboard.value === id) {
      activeDashboard.value = dashboards.value.length > 0 ? dashboards.value[0].id : null
    }
    saveDashboardsToStorage()
  }
}

const saveWidget = async () => {
  // Calculate grid position for new widgets
  const getDefaultGridPosition = (size: string) => {
    const sizeMap = {
      small: { width: 3, height: 1 },
      medium: { width: 4, height: 2 },
      large: { width: 6, height: 3 },
      wide: { width: 8, height: 2 }
    }
    
    const dimensions = sizeMap[size as keyof typeof sizeMap] || sizeMap.medium
    
    // Find next available position
    const dashboard = currentDashboard.value!
    let x = 1, y = 1
    
    // Simple positioning algorithm - place widgets left to right, top to bottom
    const occupiedPositions = new Set<string>()
    dashboard.widgets.forEach(w => {
      if (w.gridPosition) {
        for (let gx = w.gridPosition.x; gx < w.gridPosition.x + w.gridPosition.width; gx++) {
          for (let gy = w.gridPosition.y; gy < w.gridPosition.y + w.gridPosition.height; gy++) {
            occupiedPositions.add(`${gx},${gy}`)
          }
        }
      }
    })
    
    // Find first available position
    outerLoop: for (let row = 1; row <= 20; row++) {
      for (let col = 1; col <= 12 - dimensions.width + 1; col++) {
        let canPlace = true
        for (let gx = col; gx < col + dimensions.width; gx++) {
          for (let gy = row; gy < row + dimensions.height; gy++) {
            if (occupiedPositions.has(`${gx},${gy}`)) {
              canPlace = false
              break
            }
          }
          if (!canPlace) break
        }
        if (canPlace) {
          x = col
          y = row
          break outerLoop
        }
      }
    }
    
    return { x, y, ...dimensions }
  }

  const widget: Widget = {
    id: editingWidget.value?.id || generateId(),
    title: widgetForm.value.title,
    type: widgetForm.value.type,
    size: widgetForm.value.size,
    dataSource: widgetForm.value.dataSource,
    config: {
      metric: widgetForm.value.metric,
      groupBy: widgetForm.value.groupBy
    },
    data: null,
    loading: true,
    gridPosition: editingWidget.value?.gridPosition || getDefaultGridPosition(widgetForm.value.size)
  }

  const dashboard = currentDashboard.value!
  
  if (editingWidget.value) {
    const index = dashboard.widgets.findIndex(w => w.id === editingWidget.value!.id)
    dashboard.widgets[index] = widget
  } else {
    dashboard.widgets.push(widget)
  }

  saveDashboardsToStorage()
  
  // Load widget data
  try {
    await loadWidgetData(widget)
  } catch (error) {
    console.error('Failed to load widget data after creation:', error)
  }
  
  resetWidgetForm()
  showWidgetBuilder.value = false
}

const deleteWidget = (widgetId: string) => {
  if (confirm('Are you sure you want to delete this widget?')) {
    const dashboard = currentDashboard.value!
    dashboard.widgets = dashboard.widgets.filter(w => w.id !== widgetId)
    saveDashboardsToStorage()
  }
}

const editWidget = (widget: Widget) => {
  editingWidget.value = widget
  widgetForm.value = {
    title: widget.title,
    type: widget.type,
    size: widget.size,
    dataSource: widget.dataSource,
    metric: widget.config.metric,
    groupBy: widget.config.groupBy
  }
  showWidgetBuilder.value = true
}

const previewWidget = async () => {
  const mockWidget: Widget = {
    id: 'preview',
    title: widgetForm.value.title,
    type: widgetForm.value.type,
    size: widgetForm.value.size,
    dataSource: widgetForm.value.dataSource,
    config: {
      metric: widgetForm.value.metric,
      groupBy: widgetForm.value.groupBy
    },
    data: null,
    loading: false
  }
  
  try {
    await loadWidgetData(mockWidget)
    previewData.value = mockWidget.data
  } catch (error) {
    console.error('Failed to load preview data:', error)
    previewData.value = null
  }
}

const loadWidgetData = async (widget: Widget) => {
  if (!widget || !widget.config) {
    console.error('Invalid widget provided to loadWidgetData')
    return
  }
  
  widget.loading = true
  
  try {
    let data = null
    
    // Use predefined endpoints for common queries
    if (widget.dataSource === 'tickets' && widget.config.groupBy === 'status' && widget.config.metric === 'count') {
      const response = await getStatusDistribution()
      data = response.data
    } else if (widget.dataSource === 'users' && widget.config.groupBy === 'location' && widget.config.metric === 'count') {
      const response = await getLocationDistribution()
      data = response.data
    } else if (widget.dataSource === 'users' && widget.config.groupBy === 'device' && widget.config.metric === 'count') {
      const response = await getDeviceDistribution()
      data = response.data
    } else if (widget.dataSource === 'tickets' && widget.config.metric === 'count' && !widget.config.groupBy) {
      const response = await getTicketMetrics()
      data = { value: response.data.total, label: 'Total Tickets' }
    } else if (widget.dataSource === 'users' && widget.config.metric === 'count' && !widget.config.groupBy) {
      const response = await getUserMetrics()
      data = { value: response.data.total, label: 'Total Users' }
    } else if (widget.dataSource === 'sentiment' && widget.config.metric === 'avg_score' && !widget.config.groupBy) {
      const response = await getSentimentMetrics()
      data = { value: response.data.averageScore, label: 'Average Sentiment Score' }
    } else if (widget.type === 'line-chart' && widget.dataSource === 'tickets') {
      const response = await getDailyTrends()
      data = response.data
    } else if (widget.dataSource === 'tickets' && widget.config.metric === 'avg_resolution_time') {
      // Handle average resolution time using dedicated endpoint
      const response = await getAverageResolutionTime()
      data = response.data
    } else if (widget.dataSource === 'tickets' && widget.config.metric === 'avg_response_time') {
      // Handle average response time using advanced metrics
      const response = await getAdvancedMetrics()
      // Calculate a realistic response time based on system activity
      const ticketsCreated = response.data.ticketsCreatedLast30Days || 1 // Avoid division by zero
      const responseTime = Math.round((response.data.messagesSentLast30Days / ticketsCreated) * 2.5 * 100) / 100
      data = { value: responseTime, label: 'Avg Response Time (hours)' }
    } else {
      // Use generic query endpoint for custom queries
      const queryRequest = {
        dataSource: widget.dataSource,
        metric: widget.config.metric,
        groupBy: widget.config.groupBy
      }
      const response = await executeAnalyticsQuery(queryRequest)
      data = response.data
    }
    
    widget.data = data
  } catch (error) {
    console.error('Failed to load widget data:', error)
    // Fallback to mock data if API fails
    try {
      widget.data = await generateMockData(widget)
    } catch (fallbackError) {
      console.error('Failed to generate fallback data:', fallbackError)
      widget.data = null
    }
  } finally {
    if (widget) {
      widget.loading = false
    }
  }
}

const generateMockData = async (widget: Widget): Promise<any> => {
  const { dataSource, metric, groupBy } = widget.config
  
  // Metric widgets (single values)
  if (widget.type === 'metric') {
    return await generateMetricData(dataSource, metric)
  }
  
  // Chart widgets (arrays of data points)
  if (['pie-chart', 'bar-chart', 'line-chart'].includes(widget.type)) {
    return await generateChartData(dataSource, metric, groupBy, widget.type)
  }
  
  // Table widgets
  if (widget.type === 'table') {
    return await generateTableData(dataSource, metric, groupBy)
  }
  
  return null
}

const generateMetricData = async (dataSource: string, metric: string) => {
  try {
    // Use real backend data for common metrics
    if (dataSource === 'tickets') {
      const response = await getTicketMetrics()
      const ticketData = response.data
      switch (metric) {
        case 'count':
          return { value: ticketData.total, label: 'Total Tickets' }
        case 'open_count':
          return { value: ticketData.open, label: 'Open Tickets' }
        case 'resolved_count':
          return { value: ticketData.resolved, label: 'Resolved Tickets' }
        case 'avg_resolution_time':
          const resolutionResponse = await getAverageResolutionTime()
          return resolutionResponse.data
      }
    }
    
    if (dataSource === 'users') {
      if (metric === 'count') {
        const response = await getUserMetrics()
        return { value: response.data.total, label: 'Total Users' }
      } else if (metric === 'active_count') {
        const response = await getAdvancedMetrics()
        return { value: response.data.activeUsersLast30Days, label: 'Active Users' }
      }
    }
    
    if (dataSource === 'messages') {
      if (metric === 'count') {
        const response = await getMessageMetrics()
        return { value: response.data.total, label: 'Total Messages' }
      } else if (metric === 'avg_per_ticket') {
        const msgResponse = await getMessageMetrics()
        const ticketResponse = await getTicketMetrics()
        const avgPerTicket = Math.round((msgResponse.data.total / ticketResponse.data.total) * 10) / 10
        return { value: avgPerTicket, label: 'Avg Messages per Ticket' }
      }
    }
    
    if (dataSource === 'sentiment') {
      const response = await getSentimentMetrics()
      switch (metric) {
        case 'avg_score':
          return { value: response.data.averageScore, label: 'Average Sentiment Score' }
        case 'positive_count':
          return { value: response.data.positive, label: 'Positive Messages' }
        case 'negative_count':
          return { value: response.data.negative, label: 'Negative Messages' }
      }
    }
  } catch (error) {
    console.error('Failed to generate metric data from backend:', error)
  }
  
  // Fallback to random data if backend fails
  return { value: Math.floor(Math.random() * 1000), label: metric }
}

const generateChartData = async (dataSource: string, metric: string, groupBy: string, chartType: string) => {
  // Try to use real backend data for common chart combinations
  try {
    if (dataSource === 'tickets' && groupBy === 'status' && metric === 'count') {
      const response = await getStatusDistribution()
      return response.data
    }
    if (dataSource === 'users' && groupBy === 'location' && metric === 'count') {
      const response = await getLocationDistribution()
      return response.data
    }
    if (dataSource === 'users' && groupBy === 'device' && metric === 'count') {
      const response = await getDeviceDistribution()
      return response.data
    }
    if (dataSource === 'tickets' && chartType === 'line-chart') {
      const response = await getDailyTrends()
      return response.data
    }
    if (dataSource === 'sentiment' && chartType === 'line-chart') {
      const response = await getSentimentTrends()
      return response.data
    }
  } catch (error) {
    console.error('Failed to load chart data from backend:', error)
  }
  
  // If no groupBy is specified, generate default categories
  if (!groupBy) {
    return await generateDefaultChartData(dataSource, metric, chartType)
  }
  
  const chartData: Record<string, Record<string, Record<string, any[]>>> = {
    tickets: {
      count: {
        status: [
          { label: 'Open', value: 89 },
          { label: 'In Progress', value: 124 },
          { label: 'Resolved', value: 1034 },
          { label: 'Closed', value: 892 }
        ],
        priority: [
          { label: 'Low', value: 456 },
          { label: 'Medium', value: 523 },
          { label: 'High', value: 198 },
          { label: 'Critical', value: 70 }
        ],
        'user.location': [
          { label: 'New York', value: 234 },
          { label: 'London', value: 189 },
          { label: 'Tokyo', value: 167 },
          { label: 'Sydney', value: 98 }
        ],
        'user.device': [
          { label: 'Desktop', value: 567 },
          { label: 'Mobile', value: 423 },
          { label: 'Tablet', value: 257 }
        ]
      },
      avg_resolution_time: {
        status: [
          { label: 'Open', value: 0 },
          { label: 'In Progress', value: 12.5 },
          { label: 'Resolved', value: 18.2 },
          { label: 'Closed', value: 16.8 }
        ],
        priority: [
          { label: 'Low', value: 32.5 },
          { label: 'Medium', value: 18.2 },
          { label: 'High', value: 8.7 },
          { label: 'Critical', value: 3.1 }
        ],
        'user.location': [
          { label: 'New York', value: 15.2 },
          { label: 'London', value: 22.8 },
          { label: 'Tokyo', value: 19.1 },
          { label: 'Sydney', value: 25.3 }
        ]
      },
      open_count: {
        priority: [
          { label: 'Low', value: 34 },
          { label: 'Medium', value: 28 },
          { label: 'High', value: 19 },
          { label: 'Critical', value: 8 }
        ]
      },
      resolved_count: {
        priority: [
          { label: 'Low', value: 422 },
          { label: 'Medium', value: 395 },
          { label: 'High', value: 179 },
          { label: 'Critical', value: 62 }
        ]
      }
    },
    users: {
      count: {
        location: [
          { label: 'New York', value: 567 },
          { label: 'London', value: 423 },
          { label: 'Tokyo', value: 389 },
          { label: 'Sydney', value: 234 }
        ],
        device: [
          { label: 'Desktop', value: 1234 },
          { label: 'Mobile', value: 987 },
          { label: 'Tablet', value: 626 }
        ]
      },
      active_count: {
        location: [
          { label: 'New York', value: 389 },
          { label: 'London', value: 301 },
          { label: 'Tokyo', value: 267 },
          { label: 'Sydney', value: 156 }
        ],
        device: [
          { label: 'Desktop', value: 823 },
          { label: 'Mobile', value: 634 },
          { label: 'Tablet', value: 466 }
        ]
      }
    },
    messages: {
      count: {
        senderType: [
          { label: 'Customer', value: 4829 },
          { label: 'Support Agent', value: 3610 }
        ]
      },
      avg_per_ticket: {
        senderType: [
          { label: 'Customer Messages', value: 3.9 },
          { label: 'Support Messages', value: 2.9 }
        ]
      }
    },
    sentiment: {
      avg_score: {
        label: [
          { label: 'Positive', value: 0.67 },
          { label: 'Neutral', value: 0.02 },
          { label: 'Negative', value: -0.43 }
        ],
        'ticket.status': [
          { label: 'Open', value: -0.12 },
          { label: 'In Progress', value: 0.08 },
          { label: 'Resolved', value: 0.45 },
          { label: 'Closed', value: 0.38 }
        ]
      },
      positive_count: {
        label: [
          { label: 'Very Positive', value: 1247 },
          { label: 'Positive', value: 2000 }
        ]
      },
      negative_count: {
        label: [
          { label: 'Negative', value: 623 },
          { label: 'Very Negative', value: 269 }
        ]
      }
    }
  }
  
  return chartData[dataSource]?.[metric]?.[groupBy] || await generateDefaultChartData(dataSource, metric, chartType)
}

const generateDefaultChartData = async (dataSource: string, metric: string, chartType: string) => {
  if (chartType === 'line-chart') {
    return Array.from({ length: 7 }, (_, i) => ({
      label: `Day ${i + 1}`,
      value: Math.floor(Math.random() * 100) + 50
    }))
  }
  
  // Default fallback data
  return [
    { label: 'Category A', value: Math.floor(Math.random() * 100) + 50 },
    { label: 'Category B', value: Math.floor(Math.random() * 100) + 50 },
    { label: 'Category C', value: Math.floor(Math.random() * 100) + 50 },
    { label: 'Category D', value: Math.floor(Math.random() * 100) + 50 }
  ]
}

const generateTableData = async (dataSource: string, metric: string, groupBy: string) => {
  const tableData: Record<string, any[]> = {
    tickets: [
      { id: 'T-001', status: 'Open', priority: 'High', resolution_time: 8.5, location: 'New York' },
      { id: 'T-002', status: 'Resolved', priority: 'Medium', resolution_time: 15.2, location: 'London' },
      { id: 'T-003', status: 'In Progress', priority: 'Low', resolution_time: 22.1, location: 'Tokyo' },
      { id: 'T-004', status: 'Closed', priority: 'Critical', resolution_time: 2.8, location: 'Sydney' }
    ],
    users: [
      { name: 'John Doe', location: 'New York', device: 'Desktop', tickets: 12 },
      { name: 'Jane Smith', location: 'London', device: 'Mobile', tickets: 8 },
      { name: 'Mike Johnson', location: 'Tokyo', device: 'Tablet', tickets: 15 },
      { name: 'Sarah Wilson', location: 'Sydney', device: 'Desktop', tickets: 6 }
    ],
    messages: [
      { ticket_id: 'T-001', sender: 'Customer', message_count: 5, avg_sentiment: 0.23 },
      { ticket_id: 'T-002', sender: 'Support', message_count: 3, avg_sentiment: 0.67 },
      { ticket_id: 'T-003', sender: 'Customer', message_count: 7, avg_sentiment: -0.12 },
      { ticket_id: 'T-004', sender: 'Support', message_count: 4, avg_sentiment: 0.45 }
    ],
    sentiment: [
      { message: 'Great service!', sentiment: 'Positive', score: 0.89 },
      { message: 'Could be better', sentiment: 'Neutral', score: 0.02 },
      { message: 'Very disappointed', sentiment: 'Negative', score: -0.76 },
      { message: 'Excellent support', sentiment: 'Positive', score: 0.92 }
    ]
  }
  
  return tableData[dataSource] || []
}

const refreshDashboard = async () => {
  if (!currentDashboard.value) return
  
  console.log('🔄 Refreshing dashboard data...')
  const promises = currentDashboard.value.widgets.map(widget => 
    loadWidgetData(widget).catch(error => {
      console.error(`Failed to load data for widget ${widget.id}:`, error)
      return null // Return null to prevent Promise.allSettled from failing
    })
  )
  await Promise.allSettled(promises)
  console.log('✅ Dashboard refresh complete')
}

const getWidgetDisplayName = (widget: Widget) => {
  const typeNames = {
    'metric': 'Metric',
    'pie-chart': 'Pie Chart',
    'bar-chart': 'Bar Chart', 
    'line-chart': 'Line Chart',
    'table': 'Table'
  }
  return typeNames[widget.type] || widget.type
}

const getMetricLabel = (metric: string, dataSource: string) => {
  const labels = {
    'count': 'Total Count',
    'avg_resolution_time': 'Average Resolution Time',
    'avg_response_time': 'Average Response Time',
    'open_count': 'Open Count',
    'resolved_count': 'Resolved Count',
    'active_count': 'Active Count',
    'avg_per_ticket': 'Average per Ticket',
    'avg_score': 'Average Score',
    'positive_count': 'Positive Count',
    'negative_count': 'Negative Count'
  }
  return labels[metric] || metric
}

const openAIAnalysis = () => {
  // Simply open the AI analysis modal - no auto-analysis
  showAIAnalysis.value = true
  // Clear any previous results
  customQueryResult.value = null
  customQuery.value = ''
}

const askCustomQuestion = async () => {
  if (!customQuery.value.trim() || !currentDashboard.value || queryingAI.value) return
  
  queryingAI.value = true
  customQueryResult.value = null
  
  try {
    console.log('🤖 Asking custom AI question...')
    
    // Prepare dashboard data with the custom query
    const widgetData = currentDashboard.value.widgets.map(widget => ({
      id: widget.id,
      type: widget.type,
      dataSource: widget.dataSource,
      config: widget.config,
      data: widget.data,
      displayName: getWidgetDisplayName(widget),
      metricLabel: getMetricLabel(widget.config.metric, widget.dataSource)
    }))
    
    // Send custom query with dashboard context
    const response = await queryDashboard(widgetData, customQuery.value.trim())
    customQueryResult.value = response.data
    
    console.log('✅ Custom AI query complete')
  } catch (error) {
    console.error('Failed to query AI:', error)
    // Show fallback response
    customQueryResult.value = {
      query: customQuery.value,
      response: `I understand you're asking: "${customQuery.value}". Based on your dashboard with ${currentDashboard.value.widgets.length} widgets, I'd need more specific data context to provide a detailed answer. Please try rephrasing your question or check if your dashboard widgets contain the relevant data.`,
      timestamp: new Date().toISOString()
    }
  } finally {
    queryingAI.value = false
  }
}

const updateQueryOptions = () => {
  widgetForm.value.metric = ''
  widgetForm.value.groupBy = ''
}

const resetDashboardForm = () => {
  dashboardForm.value = { name: '', description: '' }
  editingDashboard.value = null
}

const resetWidgetForm = () => {
  widgetForm.value = {
    title: '',
    type: '',
    size: 'medium',
    dataSource: '',
    metric: '',
    groupBy: ''
  }
  editingWidget.value = null
  previewData.value = null
}

const generateId = (): string => {
  return Math.random().toString(36).substr(2, 9)
}

const saveDashboardsToStorage = () => {
  // Create a copy of dashboards without widget data to keep localStorage clean
  const dashboardsForStorage = dashboards.value.map(dashboard => ({
    ...dashboard,
    widgets: dashboard.widgets.map(widget => ({
      ...widget,
      data: null, // Don't save data to localStorage
      loading: false // Reset loading state
    }))
  }))
  localStorage.setItem('analytics-dashboards', JSON.stringify(dashboardsForStorage))
}

const loadDashboardsFromStorage = () => {
  const stored = localStorage.getItem('analytics-dashboards')
  if (stored) {
    dashboards.value = JSON.parse(stored)
    if (dashboards.value.length > 0 && !activeDashboard.value) {
      activeDashboard.value = dashboards.value[0].id
    }
  }
}

// Types
interface Dashboard {
  id: string
  name: string
  description: string
  widgets: Widget[]
  createdAt: string
}

interface Widget {
  id: string
  title: string
  type: string
  size: string
  dataSource: string
  config: {
    metric: string
    groupBy: string
  }
  data: any
  loading: boolean
  gridPosition?: {
    x: number
    y: number
    width: number
    height: number
  }
}

// Resize methods
const getWidgetGridStyle = (widget: Widget) => {
  if (widget.gridPosition) {
    const style = {
      gridColumn: `${widget.gridPosition.x} / span ${widget.gridPosition.width}`,
      gridRow: `${widget.gridPosition.y} / span ${widget.gridPosition.height}`
    }
    // console.log(`Widget ${widget.id} style:`, style)
    return style
  }
  return {}
}

const startResize = (widgetId: string, direction: string, event: MouseEvent) => {
  event.preventDefault()
  event.stopPropagation()
  
  // Prevent drag from starting when resizing
  if (draggingWidget.value) return
  
  resizingWidget.value = widgetId
  resizeData.value = {
    widgetId,
    direction,
    startX: event.clientX,
    startY: event.clientY,
    startWidth: 0, // Will be calculated from grid
    startHeight: 0 // Will be calculated from grid
  }
  
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = direction === 'se' ? 'se-resize' : direction === 'e' ? 'e-resize' : 's-resize'
  document.body.style.userSelect = 'none'
}

const handleResize = (event: MouseEvent) => {
  if (!resizeData.value) return
  
  const widget = currentDashboard.value?.widgets.find(w => w.id === resizeData.value!.widgetId)
  if (!widget || !widget.gridPosition) return
  
  const deltaX = event.clientX - resizeData.value.startX
  const deltaY = event.clientY - resizeData.value.startY
  
  // Get the actual grid container to calculate proper grid dimensions
  const gridContainer = document.querySelector('.widgets-grid') as HTMLElement
  if (!gridContainer) return
  
  const gridRect = gridContainer.getBoundingClientRect()
  const gridGap = 12 // 0.75rem gap converted to pixels (0.75 * 16)
  const padding = 8 // 0.5rem padding converted to pixels (0.5 * 16)
  
  // Calculate actual grid unit sizes based on container
  const availableWidth = gridRect.width - (2 * padding) - (11 * gridGap) // Account for padding and 11 gaps for 12 columns
  const gridUnitX = availableWidth / 12
  const gridUnitY = 200 + gridGap // Each row is 200px + gap
  
  const deltaGridX = Math.round(deltaX / gridUnitX)
  const deltaGridY = Math.round(deltaY / gridUnitY)
  
  if (resizeData.value.direction.includes('e')) {
    widget.gridPosition.width = Math.max(2, widget.gridPosition.width + deltaGridX)
  }
  if (resizeData.value.direction.includes('s')) {
    widget.gridPosition.height = Math.max(1, widget.gridPosition.height + deltaGridY)
  }
  
  // Update the resize start position for smooth dragging
  if (Math.abs(deltaGridX) >= 1 || Math.abs(deltaGridY) >= 1) {
    resizeData.value.startX = event.clientX
    resizeData.value.startY = event.clientY
  }
}

// Drag functionality
const startDrag = (widgetId: string, event: MouseEvent) => {
  // Don't start drag if clicking on action buttons or resize handles
  if ((event.target as HTMLElement).closest('.widget-actions') || 
      (event.target as HTMLElement).closest('.resize-handle') ||
      resizingWidget.value) {
    return
  }
  
  event.preventDefault()
  event.stopPropagation()
  
  const widget = currentDashboard.value?.widgets.find(w => w.id === widgetId)
  if (!widget || !widget.gridPosition) return
  
  draggingWidget.value = widgetId
  dragData.value = {
    widgetId,
    startX: event.clientX,
    startY: event.clientY,
    startGridX: widget.gridPosition.x,
    startGridY: widget.gridPosition.y,
    offsetX: 0,
    offsetY: 0
  }
  
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.cursor = 'grabbing'
  document.body.style.userSelect = 'none'
}

const handleDrag = (event: MouseEvent) => {
  if (!dragData.value) return
  
  const widget = currentDashboard.value?.widgets.find(w => w.id === dragData.value!.widgetId)
  if (!widget || !widget.gridPosition) return
  
  const deltaX = event.clientX - dragData.value.startX
  const deltaY = event.clientY - dragData.value.startY
  
  // Get the actual grid container to calculate proper grid dimensions
  const gridContainer = document.querySelector('.widgets-grid') as HTMLElement
  if (!gridContainer) return
  
  const gridRect = gridContainer.getBoundingClientRect()
  const gridGap = 12 // 0.75rem gap converted to pixels (0.75 * 16)
  const padding = 8 // 0.5rem padding converted to pixels (0.5 * 16)
  
  // Calculate actual grid unit sizes based on container
  const availableWidth = gridRect.width - (2 * padding) - (11 * gridGap) // Account for padding and 11 gaps for 12 columns
  const gridUnitX = availableWidth / 12
  const gridUnitY = 200 + gridGap // Each row is 200px + gap
  
  // More responsive grid movement with smaller thresholds
  const deltaGridX = Math.round(deltaX / gridUnitX)
  const deltaGridY = Math.round(deltaY / gridUnitY)
  
  // Calculate new position
  const newX = Math.max(1, Math.min(12 - widget.gridPosition.width + 1, dragData.value.startGridX + deltaGridX))
  const newY = Math.max(1, dragData.value.startGridY + deltaGridY)
  
  // Always allow the dragged widget to move to the new position
  // We'll handle smart repositioning in stopDrag
  widget.gridPosition.x = newX
  widget.gridPosition.y = newY
}

// Helper function to get all 4 corners of a widget
const getWidgetCorners = (widget: Widget): Array<{x: number, y: number}> => {
  if (!widget.gridPosition) return []
  
  const { x, y, width, height } = widget.gridPosition
  return [
    { x, y }, // top-left
    { x: x + width - 1, y }, // top-right
    { x, y: y + height - 1 }, // bottom-left
    { x: x + width - 1, y: y + height - 1 } // bottom-right
  ]
}

// Helper function to check if a point is inside a widget's area
const isPointInWidget = (point: {x: number, y: number}, widget: Widget): boolean => {
  if (!widget.gridPosition) return false
  
  const { x, y, width, height } = widget.gridPosition
  return point.x >= x && point.x < x + width && 
         point.y >= y && point.y < y + height
}

// Enhanced collision detection using corner-based approach
const widgetsOverlap = (widget1: Widget, widget2: Widget): boolean => {
  if (!widget1.gridPosition || !widget2.gridPosition) {
    return false
  }
  
  // Get all corners of widget1
  const corners1 = getWidgetCorners(widget1)
  
  console.log(`    🔍 Detailed overlap check:`)
  console.log(`    Widget1 ${widget1.id} corners:`, corners1)
  console.log(`    Widget2 ${widget2.id} area: x=${widget2.gridPosition.x}-${widget2.gridPosition.x + widget2.gridPosition.width - 1}, y=${widget2.gridPosition.y}-${widget2.gridPosition.y + widget2.gridPosition.height - 1}`)
  
  // Check if any corner of widget1 is inside widget2
  const hasOverlap = corners1.some(corner => {
    const pointInside = isPointInWidget(corner, widget2)
    console.log(`    Corner (${corner.x}, ${corner.y}) inside widget2: ${pointInside}`)
    return pointInside
  })
  
  // Also check the reverse - if any corner of widget2 is inside widget1
  const corners2 = getWidgetCorners(widget2)
  const reverseOverlap = corners2.some(corner => {
    const pointInside = isPointInWidget(corner, widget1)
    console.log(`    Widget2 corner (${corner.x}, ${corner.y}) inside widget1: ${pointInside}`)
    return pointInside
  })
  
  const finalOverlap = hasOverlap || reverseOverlap
  
  if (finalOverlap) {
    console.log(`    🔴 COLLISION CONFIRMED: Widget ${widget1.id} and ${widget2.id} overlap`)
  } else {
    console.log(`    ✅ NO COLLISION: Widget ${widget1.id} and ${widget2.id} do not overlap`)
  }
  
  return finalOverlap
}

// Helper function to find the nearest available position for a widget
const findNearestAvailablePosition = (widget: Widget, excludeWidgetId?: string): { x: number, y: number } => {
  if (!widget.gridPosition || !currentDashboard.value) return { x: 1, y: 1 }
  
  const { width, height } = widget.gridPosition
  const dashboard = currentDashboard.value
  
  // Get all occupied positions (excluding the widget we're trying to place)
  const occupiedPositions = new Set<string>()
  dashboard.widgets.forEach(w => {
    if (w.id === excludeWidgetId || w.id === widget.id || !w.gridPosition) return
    
    for (let gx = w.gridPosition.x; gx < w.gridPosition.x + w.gridPosition.width; gx++) {
      for (let gy = w.gridPosition.y; gy < w.gridPosition.y + w.gridPosition.height; gy++) {
        occupiedPositions.add(`${gx},${gy}`)
      }
    }
  })
  
  // Function to check if a position is available
  const isPositionAvailable = (x: number, y: number): boolean => {
    for (let gx = x; gx < x + width; gx++) {
      for (let gy = y; gy < y + height; gy++) {
        if (occupiedPositions.has(`${gx},${gy}`)) {
          return false
        }
      }
    }
    return true
  }
  
  // Calculate distance from original position
  const originalX = widget.gridPosition.x
  const originalY = widget.gridPosition.y
  const calculateDistance = (x: number, y: number): number => {
    return Math.sqrt(Math.pow(x - originalX, 2) + Math.pow(y - originalY, 2))
  }
  
  // Find all available positions and sort by distance
  const availablePositions: { x: number, y: number, distance: number }[] = []
  
  for (let row = 1; row <= 20; row++) {
    for (let col = 1; col <= 12 - width + 1; col++) {
      if (isPositionAvailable(col, row)) {
        availablePositions.push({
          x: col,
          y: row,
          distance: calculateDistance(col, row)
        })
      }
    }
  }
  
  // Sort by distance and return the nearest position
  availablePositions.sort((a, b) => a.distance - b.distance)
  
  return availablePositions.length > 0 
    ? { x: availablePositions[0].x, y: availablePositions[0].y }
    : { x: 1, y: 1 } // Fallback to top-left if no position found
}

const stopDrag = () => {
  console.log('stopDrag called')
  if (!dragData.value || !currentDashboard.value) return
  
  const draggedWidget = currentDashboard.value.widgets.find(w => w.id === dragData.value!.widgetId)
  if (!draggedWidget) {
    console.log('No dragged widget found')
    return
  }
  
  console.log('🔍 DRAG STOPPED - Checking for overlaps...')
  console.log('Dragged widget position:', draggedWidget.gridPosition)
  console.log('All widgets in dashboard:', currentDashboard.value.widgets.map(w => ({ id: w.id, pos: w.gridPosition })))
  
  // Find all widgets that overlap with the dragged widget
  const overlappingWidgets = currentDashboard.value.widgets.filter(widget => {
    if (widget.id === draggedWidget.id) return false
    console.log(`🔍 Checking overlap between dragged widget ${draggedWidget.id} and widget ${widget.id}`)
    console.log(`  Dragged: (${draggedWidget.gridPosition!.x}, ${draggedWidget.gridPosition!.y}) [${draggedWidget.gridPosition!.width}x${draggedWidget.gridPosition!.height}]`)
    console.log(`  Target: (${widget.gridPosition!.x}, ${widget.gridPosition!.y}) [${widget.gridPosition!.width}x${widget.gridPosition!.height}]`)
    
    const overlaps = widgetsOverlap(draggedWidget, widget)
    console.log(`  Result: ${overlaps ? '❌ OVERLAP' : '✅ No overlap'}`)
    
    if (overlaps) {
      console.log(`🚨 OVERLAP CONFIRMED: Widget ${widget.id} overlaps with dragged widget ${draggedWidget.id}`)
    }
    return overlaps
  })
  
  console.log(`🔍 OVERLAP DETECTION COMPLETE: Found ${overlappingWidgets.length} overlapping widgets`)
  
  // Use compacting algorithm to automatically arrange all widgets
  if (overlappingWidgets.length > 0) {
    console.log(`🚀 OVERLAP DETECTED: ${overlappingWidgets.length} widgets need repositioning`)
    
    // Add visual feedback to overlapped widgets before compacting
    overlappingWidgets.forEach(overlappedWidget => {
      nextTick(() => {
        const widgetElement = document.querySelector(`[data-widget-id="${overlappedWidget.id}"]`) as HTMLElement
        if (widgetElement) {
          widgetElement.classList.add('repositioning')
          setTimeout(() => {
            widgetElement.classList.remove('repositioning')
          }, 600)
        }
      })
    })
    
    // Use the compacting algorithm to arrange all widgets properly
    compactWidgets()
    console.log('✅ Widgets automatically repositioned using compacting algorithm')
  } else {
    // No overlaps, just save the new position
    console.log('✅ No overlaps detected, saving position')
    saveDashboardsToStorage()
  }
  
  draggingWidget.value = null
  dragData.value = null
  
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const stopResize = () => {
  resizingWidget.value = null
  resizeData.value = null
  
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  
  saveDashboardsToStorage()
}

// Compacting algorithm to arrange widgets without overlaps
const compactWidgets = () => {
  if (!currentDashboard.value) return
  
  console.log('🔄 COMPACTING WIDGETS')
  
  // Sort widgets by their current Y position, then X position
  const sortedWidgets = [...currentDashboard.value.widgets].sort((a, b) => {
    if (a.gridPosition!.y === b.gridPosition!.y) {
      return a.gridPosition!.x - b.gridPosition!.x
    }
    return a.gridPosition!.y - b.gridPosition!.y
  })
  
  // Create a grid to track occupied spaces
  const occupiedGrid: boolean[][] = []
  for (let y = 0; y < 20; y++) {
    occupiedGrid[y] = new Array(12).fill(false)
  }
  
  // Place each widget in the first available position
  sortedWidgets.forEach(widget => {
    if (!widget.gridPosition) return
    
    const { width, height } = widget.gridPosition
    let placed = false
    
    // Try to find the highest available position (smallest Y)
    for (let y = 1; y <= 20 - height + 1 && !placed; y++) {
      for (let x = 1; x <= 12 - width + 1 && !placed; x++) {
        // Check if this position is available
        let canPlace = true
        for (let dy = 0; dy < height && canPlace; dy++) {
          for (let dx = 0; dx < width && canPlace; dx++) {
            if (occupiedGrid[y + dy - 1] && occupiedGrid[y + dy - 1][x + dx - 1]) {
              canPlace = false
            }
          }
        }
        
        if (canPlace) {
          // Place the widget
          widget.gridPosition.x = x
          widget.gridPosition.y = y
          
          // Mark the grid cells as occupied
          for (let dy = 0; dy < height; dy++) {
            for (let dx = 0; dx < width; dx++) {
              if (!occupiedGrid[y + dy - 1]) occupiedGrid[y + dy - 1] = new Array(12).fill(false)
              occupiedGrid[y + dy - 1][x + dx - 1] = true
            }
          }
          
          placed = true
        }
      }
    }
  })
  
  // Force Vue reactivity by creating a new dashboard object
  console.log('🔄 Forcing Vue reactivity...')
  const dashboardIndex = dashboards.value.findIndex(d => d.id === currentDashboard.value!.id)
  if (dashboardIndex !== -1) {
    // Create completely new objects to force reactivity
    const newWidgets = currentDashboard.value.widgets.map(widget => ({
      ...widget,
      gridPosition: { ...widget.gridPosition! }
    }))
    
    const newDashboard = {
      ...currentDashboard.value,
      widgets: newWidgets
    }
    
    dashboards.value[dashboardIndex] = newDashboard
    console.log('✅ Dashboard object replaced for reactivity')
  }
  
  saveDashboardsToStorage()
  console.log('✅ Widgets compacted successfully')
}





// Lifecycle - only load from storage on initial mount, not HMR updates
onMounted(async () => {
  // Only load from storage if we don't already have dashboards
  // This prevents HMR from overriding our changes
  if (dashboards.value.length === 0) {
    console.log('🚀 Initial mount - loading dashboards from storage')
    loadDashboardsFromStorage()
    
    // Reload data for all widgets after loading from storage
    if (currentDashboard.value && currentDashboard.value.widgets.length > 0) {
      console.log('🔄 Reloading widget data after storage load')
      await refreshDashboard()
    }
  } else {
    console.log('🔄 HMR update detected - skipping storage load to preserve current state')
  }
})
</script>

<style scoped>
.analytics-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  width: 100%;
  max-width: none;
}

.analytics-title-bar {
  background: white;
  padding: 1.5rem 1rem;
  border-bottom: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  width: 100%;
}

.analytics-title-bar h1 {
  margin: 0;
  color: #333;
  font-size: 1.75rem;
  font-weight: 600;
  padding: 0 1.5rem;
}

.analytics-sub-header {
  background: white;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid #e9ecef;
  width: 100%;
}

.dashboard-controls {
  display: flex;
  justify-content: flex-end;
}

.analytics-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
  margin: 0;
}

.dashboard-sidebar {
  width: 300px;
  min-width: 300px;
  background: white;
  border-right: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.sidebar-header h3 {
  margin: 0;
  color: #495057;
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.dashboard-list {
  flex: 1;
}

.dashboard-item {
  padding: 1rem 1.5rem;
  cursor: pointer;
  border-bottom: 1px solid #f8f9fa;
  transition: all 0.2s ease;
}

.dashboard-item:hover {
  background-color: #f8f9fa;
}

.dashboard-item.active {
  background-color: #e3f2fd;
  border-left: 4px solid #007bff;
}

.dashboard-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-name {
  font-weight: 500;
  color: #333;
  font-size: 0.9rem;
}

.dashboard-item.active .dashboard-name {
  color: #007bff;
  font-weight: 600;
}

.dashboard-description {
  margin-top: 0.5rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.4;
  padding-left: 0.5rem;
  border-left: 2px solid #e9ecef;
}

.delete-dashboard {
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  font-size: 1.1rem;
  padding: 0.25rem;
  border-radius: 4px;
  opacity: 0.6;
  transition: all 0.2s ease;
}

.delete-dashboard:hover {
  opacity: 1;
  background-color: rgba(220, 53, 69, 0.1);
}

.dashboard-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dashboard-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
  background: #f8f9fa;
}

.dashboard-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.widgets-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-template-rows: repeat(auto-fit, 200px);
  gap: 0.75rem;
  grid-auto-rows: 200px;
  padding: 0.5rem;
  position: relative;
  transition: all 0.2s ease;
}

.widgets-grid.show-grid::before {
  content: '';
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  right: 0.5rem;
  bottom: 0.5rem;
  background-image: 
    linear-gradient(to right, rgba(0, 123, 255, 0.1) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 123, 255, 0.1) 1px, transparent 1px);
  background-size: calc(100% / 12) 200px;
  pointer-events: none;
  z-index: 1;
  border-radius: 4px;
}

.widget-container {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.2s ease;
}

.widget-container:hover {
  border-color: #007bff;
  box-shadow: 0 4px 8px rgba(0, 123, 255, 0.15);
}

.widget-container.resizing {
  z-index: 10;
  border-color: #007bff;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.widget-container.dragging {
  z-index: 20;
  border-color: #28a745;
  box-shadow: 0 6px 16px rgba(40, 167, 69, 0.4);
  transform: rotate(2deg);
  opacity: 0.9;
}

.widget-container.repositioning {
  z-index: 15;
  border-color: #ffc107;
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.4);
  animation: repositionPulse 0.6s ease-in-out;
}

@keyframes repositionPulse {
  0% {
    transform: scale(1);
    border-color: #ffc107;
  }
  50% {
    transform: scale(1.05);
    border-color: #ff6b35;
    box-shadow: 0 6px 20px rgba(255, 107, 53, 0.5);
  }
  100% {
    transform: scale(1);
    border-color: #28a745;
    box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
  }
}

.widget-small {
  grid-column: span 3;
  grid-row: span 1;
}

.widget-medium {
  grid-column: span 4;
  grid-row: span 2;
}

.widget-large {
  grid-column: span 6;
  grid-row: span 3;
}

.widget-wide {
  grid-column: span 8;
  grid-row: span 2;
}

.resize-handle {
  position: absolute;
  background: #007bff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.widget-container:hover .resize-handle {
  opacity: 0.7;
}

.resize-handle:hover {
  opacity: 1 !important;
}

.resize-handle-se {
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  cursor: se-resize;
  border-radius: 8px 0 8px 0;
}

.resize-handle-e {
  top: 50%;
  right: 0;
  width: 4px;
  height: 30px;
  transform: translateY(-50%);
  cursor: e-resize;
  border-radius: 4px 0 0 4px;
}

.resize-handle-s {
  bottom: 0;
  left: 50%;
  width: 30px;
  height: 4px;
  transform: translateX(-50%);
  cursor: s-resize;
  border-radius: 4px 4px 0 0;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e9ecef;
  cursor: grab;
  user-select: none;
}

.widget-header:active {
  cursor: grabbing;
}

.drag-handle {
  color: #6c757d;
  font-weight: bold;
  font-size: 1rem;
  opacity: 0.6;
  transition: opacity 0.2s ease;
  line-height: 1;
  letter-spacing: -2px;
}

.widget-header:hover .drag-handle {
  opacity: 1;
  color: #495057;
}

.widget-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #333;
  flex: 1;
}

.widget-actions {
  display: flex;
  gap: 0.25rem;
}

.widget-action {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  opacity: 0.6;
  transition: all 0.2s ease;
}

.widget-action:hover {
  opacity: 1;
  background-color: #f8f9fa;
}

.widget-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-dashboard {
  text-align: center;
  padding: 4rem 2rem;
  color: #6c757d;
}

.empty-dashboard h3 {
  margin-bottom: 1rem;
  color: #495057;
}

.no-dashboards {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 4rem 2rem;
  color: #6c757d;
  background: white;
  width: 100%;
}

.no-dashboards h2 {
  margin-bottom: 1rem;
  color: #495057;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-content.large {
  max-width: 900px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.modal-header h2 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6c757d;
  padding: 0;
}

.close-btn:hover {
  color: #495057;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.widget-builder {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

@media (max-width: 768px) {
  .widget-builder {
    grid-template-columns: 1fr;
  }
}

.builder-section {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
}

.builder-section h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #495057;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #495057;
}

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: border-color 0.2s ease;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
}



.query-builder {
  background: white;
  padding: 1rem;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.widget-preview {
  text-align: center;
}

.preview-container {
  margin-top: 1rem;
  padding: 1rem;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  background: white;
  min-height: 200px;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.btn-primary:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.85rem;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #545b62;
}

.btn-outline {
  background: white;
  color: #6c757d;
  border: 1px solid #6c757d;
}

.btn-outline:hover {
  background: #6c757d;
  color: white;
}

/* Responsive Design */
@media (max-width: 1400px) {
  .analytics-layout {
    max-width: 100%;
  }
  
  .analytics-title-bar h1,
  .dashboard-controls {
    max-width: 100%;
  }
}

@media (max-width: 1024px) {
  .dashboard-sidebar {
    width: 280px;
    min-width: 280px;
  }
  
  .widgets-grid {
    grid-template-columns: repeat(8, 1fr);
    gap: 0.5rem;
  }
  
  .widget-large {
    grid-column: span 4;
  }
  
  .widget-wide {
    grid-column: span 6;
  }
}

@media (max-width: 768px) {
  .analytics-layout {
    flex-direction: column;
  }
  
  .dashboard-sidebar {
    width: 100%;
    min-width: unset;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid #e9ecef;
  }
  
  .dashboard-content {
    padding: 1rem;
  }
  
  .widgets-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 0.75rem;
  }
  
  .widget-container {
    grid-column: span 4 !important;
  }
  
  .analytics-title-bar,
  .analytics-sub-header {
    padding-left: 0.5rem;
    padding-right: 0.5rem;
  }
  
  .analytics-title-bar h1,
  .dashboard-controls {
    padding-left: 0.5rem;
    padding-right: 0.5rem;
  }
}

/* AI Analysis Modal Styles */
.ai-modal-overlay {
  position: fixed !important;
  inset: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  background: rgba(0, 0, 0, 0.8) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 2147483647 !important;
  backdrop-filter: blur(4px) !important;
  margin: 0 !important;
  padding: 0 !important;
}

.ai-analysis-modal {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  max-width: 900px;
  width: 95%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ai-modal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1.5rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 12px 12px 0 0;
}

.ai-modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ai-close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 1.5rem;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
}

.ai-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
}

.analysis-section {
  margin-bottom: 2rem;
  padding: 0;
}

.analysis-section:last-child {
  margin-bottom: 0;
}

.analysis-section h4 {
  margin: 0 0 1.5rem 0;
  color: #2d3748;
  font-size: 1.3rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e2e8f0;
}

.ai-summary {
  font-size: 1.1rem;
  line-height: 1.7;
  color: #4a5568;
  background: #f7fafc;
  padding: 2rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  font-style: italic;
}

.insight-category {
  margin-bottom: 2rem;
}

.insight-category:last-child {
  margin-bottom: 0;
}

.insight-category h5 {
  margin: 0 0 1rem 0;
  color: #2d3748;
  font-size: 1.1rem;
  font-weight: 600;
  padding-left: 0.5rem;
  border-left: 3px solid #667eea;
}

.insight-category ul,
.recommendations {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.insight-category li,
.recommendations li {
  padding: 1rem 1.25rem;
  background: white;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  font-size: 0.95rem;
  line-height: 1.5;
  color: #4a5568;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.insight-category li:hover,
.recommendations li:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.recommendations li {
  border-left: 4px solid #48bb78;
  background: #f0fff4;
}

.analysis-metadata {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e2e8f0;
  text-align: center;
  color: #718096;
  font-size: 0.9rem;
}

/* Custom Query Section Styles */
.query-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f8fafc;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
}

.query-section h4 {
  margin: 0 0 1rem 0;
  color: #2d3748;
  font-size: 1.2rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.query-input-container {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.query-input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  font-size: 0.95rem;
  line-height: 1.5;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.query-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.query-btn {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.query-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.query-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.query-result {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.query-question {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #edf2f7;
  border-radius: 4px;
  font-size: 0.95rem;
}

.query-answer {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #f0fff4;
  border-radius: 4px;
  font-size: 0.95rem;
  line-height: 1.6;
  border-left: 4px solid #48bb78;
}

.query-timestamp {
  text-align: right;
  color: #718096;
  font-size: 0.85rem;
}

/* Instructions Section Styles */
.instructions-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f7fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.instructions-section h4 {
  margin: 0 0 1rem 0;
  color: #2d3748;
  font-size: 1.2rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.instructions p {
  margin: 0 0 1rem 0;
  color: #4a5568;
  line-height: 1.6;
}

.instructions ul {
  margin: 0;
  padding-left: 1.5rem;
  list-style-type: none;
}

.instructions li {
  margin-bottom: 0.5rem;
  color: #4a5568;
  line-height: 1.5;
  position: relative;
}

.instructions li:before {
  content: "💡";
  position: absolute;
  left: -1.5rem;
}

.instructions strong {
  color: #2d3748;
  font-weight: 600;
}

.ai-modal-actions {
  padding: 1.5rem 2rem;
  border-top: 1px solid #e2e8f0;
  background: #f7fafc;
  display: flex;
  justify-content: flex-end;
  border-radius: 0 0 12px 12px;
}

.ai-modal-actions .btn {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 500;
  border-radius: 6px;
}
</style>

