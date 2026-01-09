<template>
  <div class="simulation-container">
    <div class="simulation-content">
      <!-- Header -->
      <header class="simulation-header">
        <div class="header-content">
          <h1>Conversation Simulation Test Suite</h1>
          <p class="subtitle">Simulate AI vs Human replies in bulk, then review per ticket.</p>
        </div>
        <div v-if="screen === 'results'" class="filter-badge">
          <span>Showing {{ filteredRows.length }} / {{ rows.length }}</span>
        </div>
      </header>

      <!-- Config Screen -->
      <div v-if="screen === 'config'" class="card">
        <div class="card-header">
          <h2>Configure simulation</h2>
        </div>
        <div class="card-body">
          <div class="form-section">
            <label class="form-label">Ticket selection method</label>
            <div class="tabs">
              <div class="tabs-list">
                <button 
                  :class="['tab-button', { active: selectionMode === 'specific' }]"
                  @click="selectionMode = 'specific'"
                >
                  Select specific tickets
                </button>
                <button 
                  :class="['tab-button', { active: selectionMode === 'category' }]"
                  @click="selectionMode = 'category'"
                >
                  Select tickets by category
                </button>
              </div>

              <!-- Specific Selection Tab -->
              <div v-if="selectionMode === 'specific'" class="tab-content">
                <div class="grid-2">
                  <div class="form-group">
                    <label class="form-label">Search</label>
                    <input
                      v-model="specificQuery"
                      type="text"
                      placeholder="Ticket ID, subject, category..."
                      class="form-control"
                    />
                    <div class="form-hint">Select up to 1000.</div>
                  </div>
                  <div class="selection-stats">
                    <div class="stat-label">Selected</div>
                    <div class="stat-value">{{ selectedTicketIds.length }}</div>
                    <div class="stat-actions">
                      <button class="btn btn-secondary" @click="clearSelection">Clear</button>
                      <button class="btn btn-secondary" @click="selectVisible">Select visible</button>
                    </div>
                  </div>
                </div>

                <div class="ticket-list-container">
                  <div class="ticket-list-header">
                    <span>Tickets</span>
                    <span class="count">Showing {{ specificResults.length }}</span>
                  </div>
                  <div class="ticket-list-scroll">
                    <label
                      v-for="ticket in specificResults"
                      :key="ticket.id"
                      class="ticket-item-checkbox"
                    >
                      <input
                        type="checkbox"
                        :checked="selectedTicketIds.includes(ticket.id)"
                        @change="toggleTicketSelection(ticket.id)"
                      />
                      <div class="ticket-info">
                        <div class="ticket-header-row">
                          <span class="ticket-id">#{{ ticket.id }}</span>
                          <span class="badge">{{ ticket.category || 'N/A' }}</span>
                        </div>
                        <div class="ticket-subject">{{ ticket.subject }}</div>
                      </div>
                    </label>
                  </div>
                </div>
              </div>

              <!-- Category Selection Tab -->
              <div v-if="selectionMode === 'category'" class="tab-content">
                <div class="grid-2">
                  <div class="form-group">
                    <label class="form-label">Categories</label>
                    <div class="category-checkboxes">
                      <label
                        v-for="category in categories"
                        :key="category"
                        class="category-checkbox"
                      >
                        <input
                          type="checkbox"
                          :checked="selectedCategories.includes(category)"
                          @change="toggleCategory(category)"
                        />
                        <span>{{ category }}</span>
                      </label>
                    </div>
                    <div class="form-hint">Randomly sampled from selected categories.</div>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Number of tickets</label>
                    <input
                      v-model.number="maxTickets"
                      type="number"
                      min="1"
                      max="1000"
                      class="form-control"
                    />
                    <div class="form-hint">
                      Candidate pool: <strong>{{ categoryCandidates.length }}</strong> · 
                      Selected: <strong>{{ selectedTickets.length }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">AI model (placeholder)</label>
              <select v-model="model" class="form-control">
                <option value="SportyAI Standard (placeholder)">SportyAI Standard</option>
                <option value="SportyAI Pro (placeholder)">SportyAI Pro</option>
                <option value="SportyAI Lite (placeholder)">SportyAI Lite</option>
              </select>
            </div>
            <div class="ready-stats">
              <div class="stat-label">Ready to simulate</div>
              <div class="stat-value">{{ selectedTickets.length }}</div>
              <div class="stat-hint">
                {{ selectionMode === 'specific' ? 'Manual selection' : 'Random sample' }}
              </div>
            </div>
          </div>

          <div class="action-bar">
            <div class="action-hint">
              Run will simulate <strong>{{ selectedTickets.length }}</strong> tickets
            </div>
            <button
              class="btn"
              :disabled="selectedTickets.length === 0"
              @click="runSimulation"
            >
              ▶ Run simulation
            </button>
          </div>
        </div>
      </div>

      <!-- Running Screen -->
      <div v-if="screen === 'running'" class="card">
        <div class="card-header">
          <h2>⏳ In process</h2>
        </div>
        <div class="card-body">
          <div class="grid-3">
            <div class="mini-metric">
              <div class="metric-label">Tickets</div>
              <div class="metric-value">{{ runningTotal }}</div>
            </div>
              <div class="mini-metric">
                <div class="metric-label">Progress</div>
                <div class="metric-value">{{ runningProgress }}%</div>
                <div class="metric-sub">{{ runningCompleted }} / {{ runningTotal }}</div>
              </div>
            <div class="mini-metric">
              <div class="metric-label">Runtime</div>
              <div class="metric-value">{{ formatRuntime(runtimeMs) }}</div>
            </div>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: runningProgress + '%' }"></div>
          </div>
          <div class="running-footer">
            <div class="model-info">
              Model: <strong>{{ model }}</strong>
            </div>
            <button class="btn btn-secondary" @click="stopSimulation">Cancel</button>
          </div>
        </div>
      </div>

      <!-- Results Screen -->
      <div v-if="screen === 'results'" class="results-container">
        <div class="grid-3">
          <div class="card grid-span-2">
            <div class="card-header">
              <h2>Overview</h2>
            </div>
            <div class="card-body">
              <div class="grid-3">
                <div class="mini-metric">
                  <div class="metric-label">AI correct reply score (self-reflection)</div>
                  <div class="metric-value">{{ selfScore }}%</div>
                  <div class="metric-sub">{{ correctBySelf }} / {{ rows.length }}</div>
                </div>
                <div class="mini-metric">
                  <div class="metric-label">Tickets tested</div>
                  <div class="metric-value">{{ rows.length }}</div>
                </div>
                <div class="mini-metric">
                  <div class="metric-label">Run time</div>
                  <div class="metric-value">{{ formatRuntime(runtimeMs) }}</div>
                </div>
              </div>
            </div>
          </div>
          <div class="card">
            <div class="card-header">
              <h2>Human review status</h2>
            </div>
            <div class="card-body">
              <div class="review-stats">
                <div class="review-stat-row">
                  <span>Marked correct</span>
                  <strong>{{ markedCorrect }}</strong>
                </div>
                <div class="review-stat-row">
                  <span>Marked incorrect</span>
                  <strong>{{ markedIncorrect }}</strong>
                </div>
                <div class="review-stat-row">
                  <span>Unreviewed</span>
                  <strong>{{ unreviewed }}</strong>
                </div>
              </div>
              <button class="btn btn-secondary" @click="screen = 'config'" style="width: 100%; margin-top: 1rem;">
                New run
              </button>
            </div>
          </div>
        </div>

        <!-- Results Table -->
        <div class="card">
          <div class="card-header">
            <div class="header-row">
              <h2>Results</h2>
              <span class="filter-badge">Showing {{ filteredRows.length }} shown</span>
            </div>
          </div>
          <div class="card-body" style="padding: 0;">
            <div class="table-container">
              <table class="results-table">
                <thead>
                  <tr>
                    <th>Ticket ID</th>
                    <th>Ticket Subject</th>
                    <th>Human Agent Reply</th>
                    <th>AI Agent Reply</th>
                    <th>AI Self-Reflection</th>
                    <th>AI Notes</th>
                    <th>Human Mark</th>
                  </tr>
                  <tr class="filter-row">
                    <th>
                      <input
                        v-model="filters.ticketId"
                        type="text"
                        placeholder="Filter"
                        class="filter-input"
                      />
                    </th>
                    <th>
                      <input
                        v-model="filters.ticketSubject"
                        type="text"
                        placeholder="Filter"
                        class="filter-input"
                      />
                    </th>
                    <th></th>
                    <th></th>
                    <th>
                      <select v-model="filters.aiSelfReflection" class="filter-select">
                        <option value="all">All</option>
                        <option value="correct">Correct</option>
                        <option value="improvement">Improvement</option>
                      </select>
                    </th>
                    <th>
                      <input
                        v-model="filters.aiNotes"
                        type="text"
                        placeholder="Filter"
                        class="filter-input"
                      />
                    </th>
                    <th>
                      <select v-model="filters.humanMark" class="filter-select">
                        <option value="all">All</option>
                        <option value="correct">Correct</option>
                        <option value="incorrect">Incorrect</option>
                        <option value="unreviewed">Unreviewed</option>
                      </select>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="row in filteredRows"
                    :key="row.id"
                    class="table-row"
                    @click="openReviewDialog(row)"
                  >
                    <td class="mono">#{{ row.ticketId }}</td>
                    <td>{{ row.ticketSubject }}</td>
                    <td class="muted">{{ row.humanReply }}</td>
                    <td class="muted">{{ row.aiReply }}</td>
                    <td>
                      <span :class="['badge', row.aiSelfReflectionCorrect ? 'badge-success' : 'badge-error']">
                        {{ row.aiSelfReflectionCorrect ? 'Correct' : 'Improvement' }}
                      </span>
                      <div class="confidence">{{ Math.round(row.aiSelfReflectionConfidence * 100) }}%</div>
                    </td>
                    <td class="muted">{{ row.aiNotes }}</td>
                    <td>
                      <span :class="['badge', getHumanMarkBadgeClass(row.humanMark)]">
                        {{ row.humanMark || 'Unreviewed' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Review Dialog -->
      <div v-if="showReviewDialog && selectedRow && selectedTicket" class="dialog-overlay" @click="closeReviewDialog">
        <div class="dialog-content" @click.stop>
          <div class="dialog-header">
            <h2>
              <span class="ticket-id">#{{ selectedTicket.id }}</span>
              <span class="separator">·</span>
              {{ selectedTicket.subject }}
            </h2>
            <span class="badge">{{ selectedTicket.category || 'N/A' }}</span>
          </div>

          <div class="dialog-body">
            <div class="grid-3">
              <div class="grid-span-2">
                <!-- Conversation -->
                <div class="card">
                  <div class="card-header">
                    <h3>Conversation</h3>
                  </div>
                  <div class="card-body">
                    <div class="conversation-scroll">
                      <div
                        v-for="(msg, idx) in conversationMessages"
                        :key="idx"
                        class="conversation-message"
                      >
                        <div class="message-meta">
                          {{ msg.from }} · {{ formatDate(msg.at) }}
                        </div>
                        <div :class="['message-bubble', msg.from === 'customer' ? 'customer' : 'agent']">
                          {{ msg.text }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- AI vs Human Agent Comparison -->
                <div class="card">
                  <div class="card-header">
                    <h3>AI Agent vs Human Agent</h3>
                  </div>
                  <div class="card-body">
                    <div class="grid-2">
                      <div class="reply-box">
                        <div class="reply-header">
                          <span>AI Agent Reply</span>
                          <span :class="['badge', selectedRow.aiSelfReflectionCorrect ? 'badge-success' : 'badge-error']">
                            {{ Math.round(selectedRow.aiSelfReflectionConfidence * 100) }}%
                          </span>
                        </div>
                        <div class="reply-content">
                          <div class="reply-text">{{ selectedRow.aiReply }}</div>
                        </div>
                        <div class="reply-notes">AI Notes: {{ selectedRow.aiNotes }}</div>
                      </div>
                      <div class="reply-box">
                        <div class="reply-header">
                          <span>Human Agent Reply</span>
                        </div>
                        <div class="reply-content">
                          <div class="reply-text">{{ selectedRow.humanReply }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Review Panel -->
              <div class="card">
                <div class="card-header">
                  <h3>Review</h3>
                </div>
                <div class="card-body">
                  <div class="review-panel">
                    <div class="review-label">Score the AI reply</div>
                    <div class="review-buttons">
                      <button
                        :class="['btn', selectedRow.humanMark === 'CORRECT' ? '' : 'btn-secondary']"
                        @click="markCorrect"
                      >
                        ✓ Correct
                      </button>
                      <button
                        :class="['btn', selectedRow.humanMark === 'INCORRECT' ? 'btn-danger' : 'btn-secondary']"
                        @click="markIncorrect"
                      >
                        ✗ Incorrect
                      </button>
                    </div>

                    <div v-if="selectedRow.humanMark !== 'CORRECT'" class="review-fields">
                      <div class="form-group">
                        <label class="form-label">Additional details</label>
                        <textarea
                          v-model="reviewNotes"
                          class="form-control"
                          rows="4"
                          placeholder="Why is the AI answer not suitable?"
                          @blur="saveReview"
                        ></textarea>
                      </div>
                      <div class="form-group">
                        <label class="form-label">Ideal Response</label>
                        <textarea
                          v-model="idealResponse"
                          class="form-control"
                          rows="5"
                          placeholder="Ideal response for this query…"
                          @blur="saveReview"
                        ></textarea>
                        <div class="form-hint">Required when marked incorrect.</div>
                      </div>
                    </div>
                    <div v-else class="review-success">
                      Marked correct — no further input needed.
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="dialog-footer">
            <button class="btn btn-secondary" @click="closeReviewDialog">Close</button>
            <div class="dialog-nav-buttons">
              <button
                class="btn btn-secondary"
                :disabled="!prevTicketId"
                @click="goToPrev"
              >
                ← Back
              </button>
              <button
                class="btn"
                :disabled="!nextTicketId"
                @click="goToNext"
              >
                Next →
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useBackendApi } from '../composables/useBackendApi'
import type { Ticket, TicketMessage } from '../composables/useBackendApi'

const {
  getAllTickets,
  getTicketMessages,
  getTicketsForSimulation,
  runSimulation: runSimulationApi,
  updateSimulationResult,
  simulateSingleTicket
} = useBackendApi()

// Types
type Screen = 'config' | 'running' | 'results'
type SelectionMode = 'specific' | 'category'
type HumanMark = 'CORRECT' | 'INCORRECT' | null

interface SimulationRow {
  id: number
  ticketId: number
  ticketSubject: string
  humanReply: string
  aiReply: string
  aiSelfReflectionCorrect: boolean
  aiSelfReflectionConfidence: number
  aiNotes: string
  humanMark?: HumanMark
  humanNotes?: string
  idealResponse?: string
}

interface Filters {
  ticketId: string
  ticketSubject: string
  aiSelfReflection: 'all' | 'correct' | 'improvement'
  aiNotes: string
  humanMark: 'all' | 'correct' | 'incorrect' | 'unreviewed'
}

// State
const screen = ref<Screen>('config')
const selectionMode = ref<SelectionMode>('category')
const model = ref('SportyAI Standard (placeholder)')
const specificQuery = ref('')
const selectedTicketIds = ref<number[]>([])
const categories = ref(['DEPOSITS', 'WITHDRAWALS', 'ACCOUNT', 'VERIFICATION', 'PAYMENTS', 'TECHNICAL', 'GENERAL'])
const selectedCategories = ref(['DEPOSITS', 'WITHDRAWALS', 'ACCOUNT', 'VERIFICATION', 'PAYMENTS', 'TECHNICAL', 'GENERAL'])
const maxTickets = ref(30)
const allTickets = ref<Ticket[]>([])
const rows = ref<SimulationRow[]>([])
const selectedRow = ref<SimulationRow | null>(null)
const selectedTicket = ref<Ticket | null>(null)
const conversationMessages = ref<Array<{from: string, text: string, at: string}>>([])
const showReviewDialog = ref(false)
const reviewNotes = ref('')
const idealResponse = ref('')
const filters = ref<Filters>({
  ticketId: '',
  ticketSubject: '',
  aiSelfReflection: 'all',
  aiNotes: '',
  humanMark: 'all'
})

// Running state
const runningStartedAt = ref(0)
const runningProgress = ref(0)
const runningTotal = ref(0)
const runningCompleted = ref(0)
const cancelled = ref(false)
const runtimeInterval = ref<number | null>(null)

// Computed
const specificResults = computed(() => {
  const q = specificQuery.value.trim().toLowerCase()
  if (!q) return allTickets.value.slice(0, 30)
  return allTickets.value
    .filter(t =>
      t.id.toString().includes(q) ||
      t.subject.toLowerCase().includes(q) ||
      (t.category && t.category.toLowerCase().includes(q))
    )
    .slice(0, 60)
})

const categoryCandidates = computed(() => {
  return allTickets.value.filter(t => t.category && selectedCategories.value.includes(t.category))
})

const selectedTickets = computed(() => {
  if (selectionMode.value === 'specific') {
    return allTickets.value.filter(t => selectedTicketIds.value.includes(t.id)).slice(0, 1000)
  }
  const n = Math.max(1, Math.min(1000, maxTickets.value))
  if (categoryCandidates.value.length <= n) return categoryCandidates.value
  // Simple shuffle
  const shuffled = [...categoryCandidates.value].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, n)
})

const runtimeMs = ref(0)

// Update runtime in real-time when running
const updateRuntime = () => {
  if (screen.value === 'running' || screen.value === 'results') {
    runtimeMs.value = Date.now() - runningStartedAt.value
  }
}

const correctBySelf = computed(() =>
  rows.value.filter(r => r.aiSelfReflectionCorrect).length
)

const selfScore = computed(() =>
  rows.value.length ? Math.round((correctBySelf.value / rows.value.length) * 100) : 0
)

const markedCorrect = computed(() =>
  rows.value.filter(r => r.humanMark === 'CORRECT').length
)

const markedIncorrect = computed(() =>
  rows.value.filter(r => r.humanMark === 'INCORRECT').length
)

const unreviewed = computed(() =>
  rows.value.filter(r => !r.humanMark).length
)

const filteredRows = computed(() => {
  const f = filters.value
  return rows.value.filter(r => {
    if (f.ticketId && !r.ticketId.toString().includes(f.ticketId)) return false
    if (f.ticketSubject && !r.ticketSubject.toLowerCase().includes(f.ticketSubject.toLowerCase())) return false
    if (f.aiNotes && !r.aiNotes.toLowerCase().includes(f.aiNotes.toLowerCase())) return false
    if (f.aiSelfReflection === 'correct' && !r.aiSelfReflectionCorrect) return false
    if (f.aiSelfReflection === 'improvement' && r.aiSelfReflectionCorrect) return false
    if (f.humanMark === 'correct' && r.humanMark !== 'CORRECT') return false
    if (f.humanMark === 'incorrect' && r.humanMark !== 'INCORRECT') return false
    if (f.humanMark === 'unreviewed' && r.humanMark) return false
    return true
  })
})

const nextTicketId = computed(() => {
  if (!selectedRow.value) return null
  const ids = filteredRows.value.map(r => r.ticketId)
  const idx = ids.indexOf(selectedRow.value.ticketId)
  if (idx === -1) return null
  return idx + 1 < ids.length ? ids[idx + 1] : null
})

const prevTicketId = computed(() => {
  if (!selectedRow.value) return null
  const ids = filteredRows.value.map(r => r.ticketId)
  const idx = ids.indexOf(selectedRow.value.ticketId)
  if (idx === -1) return null
  return idx > 0 ? ids[idx - 1] : null
})


// Methods
const toggleTicketSelection = (id: number) => {
  const idx = selectedTicketIds.value.indexOf(id)
  if (idx >= 0) {
    selectedTicketIds.value.splice(idx, 1)
  } else {
    selectedTicketIds.value.push(id)
  }
}

const clearSelection = () => {
  selectedTicketIds.value = []
}

const selectVisible = () => {
  selectedTicketIds.value = specificResults.value.map(t => t.id)
}

const toggleCategory = (category: string) => {
  const idx = selectedCategories.value.indexOf(category)
  if (idx >= 0) {
    selectedCategories.value.splice(idx, 1)
  } else {
    selectedCategories.value.push(category)
  }
}

const formatRuntime = (ms: number) => {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  const r = s % 60
  return m ? `${m}m ${r}s` : `${r}s`
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const getHumanMarkBadgeClass = (mark?: HumanMark) => {
  if (mark === 'INCORRECT') return 'badge-error'
  if (mark === 'CORRECT') return 'badge-success'
  return 'badge-outline'
}

const runSimulation = async () => {
  cancelled.value = false
  runningStartedAt.value = Date.now()
  runningProgress.value = 0
  runningCompleted.value = 0
  runtimeMs.value = 0
  
  const tickets = selectedTickets.value
  runningTotal.value = tickets.length
  
  screen.value = 'running'
  rows.value = []
  selectedRow.value = null
  filters.value = {
    ticketId: '',
    ticketSubject: '',
    aiSelfReflection: 'all',
    aiNotes: '',
    humanMark: 'all'
  }

  // Start runtime update interval
  runtimeInterval.value = window.setInterval(() => {
    updateRuntime()
  }, 100) // Update every 100ms for smooth display

  try {
    const runId = `run-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    const results: SimulationRow[] = []

    // Process tickets one by one to show accurate progress
    for (let i = 0; i < tickets.length; i++) {
      if (cancelled.value) {
        break
      }

      const ticket = tickets[i]
      
      try {
        const response = await simulateSingleTicket(ticket.id, model.value, runId)
        const result = response.data
        
        results.push({
          id: result.id,
          ticketId: result.ticketId,
          ticketSubject: result.ticketSubject,
          humanReply: result.humanReply || '',
          aiReply: result.aiReply || '',
          aiSelfReflectionCorrect: result.aiSelfReflectionCorrect,
          aiSelfReflectionConfidence: result.aiSelfReflectionConfidence,
          aiNotes: result.aiNotes || '',
          humanMark: result.humanMark ? (result.humanMark as HumanMark) : undefined,
          humanNotes: result.humanNotes,
          idealResponse: result.idealResponse
        })
        
        runningCompleted.value = i + 1
        runningProgress.value = Math.round(((i + 1) / tickets.length) * 100)
        rows.value = [...results] // Update rows in real-time
      } catch (error) {
        console.error(`Failed to simulate ticket ${ticket.id}:`, error)
        // Continue with next ticket even if one fails
      }
    }
    
    // Clear interval
    if (runtimeInterval.value) {
      clearInterval(runtimeInterval.value)
      runtimeInterval.value = null
    }
    
    runningProgress.value = 100
    updateRuntime() // Final update
    screen.value = 'results'
  } catch (error) {
    console.error('Simulation failed:', error)
    if (runtimeInterval.value) {
      clearInterval(runtimeInterval.value)
      runtimeInterval.value = null
    }
    screen.value = 'config'
  }
}

const stopSimulation = () => {
  cancelled.value = true
  if (runtimeInterval.value) {
    clearInterval(runtimeInterval.value)
    runtimeInterval.value = null
  }
  screen.value = 'config'
}

const openReviewDialog = async (row: SimulationRow) => {
  selectedRow.value = row
  reviewNotes.value = row.humanNotes || ''
  idealResponse.value = row.idealResponse || ''
  
  // Fetch ticket details
  try {
    const ticketResponse = await getAllTickets()
    selectedTicket.value = ticketResponse.data.find((t: Ticket) => t.id === row.ticketId) || null
    
    // Fetch messages
    if (selectedTicket.value) {
      const messagesResponse = await getTicketMessages(selectedTicket.value.id)
      conversationMessages.value = messagesResponse.data.map((msg: TicketMessage) => ({
        from: msg.senderType === 'USER' ? 'customer' : msg.senderType === 'AGENT' ? 'agent' : 'system',
        text: msg.message,
        at: msg.createdAt
      }))
    }
  } catch (error) {
    console.error('Failed to load ticket:', error)
  }
  
  showReviewDialog.value = true
}

const closeReviewDialog = () => {
  showReviewDialog.value = false
  selectedRow.value = null
  selectedTicket.value = null
  conversationMessages.value = []
}

const markCorrect = async () => {
  if (!selectedRow.value) return
  selectedRow.value.humanMark = 'CORRECT'
  await saveReview()
}

const markIncorrect = async () => {
  if (!selectedRow.value) return
  selectedRow.value.humanMark = 'INCORRECT'
  await saveReview()
}

const saveReview = async () => {
  if (!selectedRow.value) return
  
  try {
    await updateSimulationResult(selectedRow.value.id, {
      humanMark: selectedRow.value.humanMark,
      humanNotes: reviewNotes.value,
      idealResponse: idealResponse.value
    })
    
    if (selectedRow.value) {
      selectedRow.value.humanNotes = reviewNotes.value
      selectedRow.value.idealResponse = idealResponse.value
    }
  } catch (error) {
    console.error('Failed to save review:', error)
  }
}

const goToNext = () => {
  if (nextTicketId.value) {
    const nextRow = filteredRows.value.find(r => r.ticketId === nextTicketId.value)
    if (nextRow) {
      openReviewDialog(nextRow)
    }
  }
}

const goToPrev = () => {
  if (prevTicketId.value) {
    const prevRow = filteredRows.value.find(r => r.ticketId === prevTicketId.value)
    if (prevRow) {
      openReviewDialog(prevRow)
    }
  }
}

// Cleanup interval on unmount
onUnmounted(() => {
  if (runtimeInterval.value) {
    clearInterval(runtimeInterval.value)
    runtimeInterval.value = null
  }
})

// Load tickets on mount
onMounted(async () => {
  try {
    const response = await getAllTickets()
    allTickets.value = response.data
  } catch (error) {
    console.error('Failed to load tickets:', error)
  }
})
</script>

<style scoped>
.simulation-container {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 2rem;
}

.simulation-content {
  max-width: 1400px;
  margin: 0 auto;
}

.simulation-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
}

.header-content h1 {
  font-size: 2rem;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  color: #6c757d;
  margin: 0;
}

.filter-badge {
  background: #e9ecef;
  padding: 0.5rem 1rem;
  border-radius: 1rem;
  font-size: 0.875rem;
}

.card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.card-header h2 {
  margin: 0;
  font-size: 1.25rem;
}

.card-body {
  padding: 1.5rem;
}

.form-section {
  margin-bottom: 2rem;
}

.tabs-list {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tab-button {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #e9ecef;
  background: white;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-button.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.grid-span-2 {
  grid-column: span 2;
}

.selection-stats,
.ready-stats {
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  padding: 1rem;
}

.stat-label {
  font-size: 0.75rem;
  color: #6c757d;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0.5rem 0;
}

.stat-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.ticket-list-container {
  border: 1px solid #e9ecef;
  border-radius: 1rem;
  margin-top: 1rem;
}

.ticket-list-header {
  display: flex;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid #e9ecef;
  font-weight: 500;
}

.ticket-list-scroll {
  max-height: 300px;
  overflow-y: auto;
  padding: 0.5rem;
}

.ticket-item-checkbox {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  margin-bottom: 0.5rem;
  cursor: pointer;
  transition: background 0.2s;
}

.ticket-item-checkbox:hover {
  background: #f8f9fa;
}

.ticket-header-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.25rem;
}

.ticket-id {
  font-family: monospace;
  font-size: 0.75rem;
}

.ticket-subject {
  font-size: 0.875rem;
  color: #495057;
}

.category-checkboxes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.category-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  cursor: pointer;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.mini-metric {
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  padding: 1rem;
}

.metric-label {
  font-size: 0.75rem;
  color: #6c757d;
  margin-bottom: 0.5rem;
}

.metric-value {
  font-size: 1.25rem;
  font-weight: 600;
}

.metric-sub {
  font-size: 0.75rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.progress-bar {
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin: 1rem 0;
}

.progress-fill {
  height: 100%;
  background: #667eea;
  transition: width 0.3s;
}

.running-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.results-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.review-stats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.review-stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-container {
  overflow-x: auto;
}

.results-table {
  width: 100%;
  border-collapse: collapse;
}

.results-table th {
  text-align: left;
  padding: 0.75rem;
  font-weight: 500;
  border-bottom: 1px solid #e9ecef;
  background: #f8f9fa;
}

.filter-row {
  background: #f1f3f5 !important;
}

.filter-input,
.filter-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #e9ecef;
  border-radius: 0.5rem;
  font-size: 0.875rem;
}

.table-row {
  cursor: pointer;
  transition: background 0.2s;
}

.table-row:hover {
  background: #f8f9fa;
}

.table-row td {
  padding: 0.75rem;
  border-bottom: 1px solid #e9ecef;
}

.mono {
  font-family: monospace;
  font-size: 0.75rem;
}

.muted {
  color: #6c757d;
  font-size: 0.875rem;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-success {
  background: #d4edda;
  color: #155724;
}

.badge-error {
  background: #f8d7da;
  color: #721c24;
}

.badge-outline {
  background: #e9ecef;
  color: #495057;
}

.confidence {
  font-size: 0.75rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.dialog-overlay {
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
  padding: 2rem;
}

.dialog-content {
  background: white;
  border-radius: 1rem;
  max-width: 1200px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.dialog-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ticket-id {
  font-family: monospace;
  font-size: 0.875rem;
}

.separator {
  color: #6c757d;
}

.dialog-body {
  padding: 1.5rem;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.dialog-nav-buttons {
  display: flex;
  gap: 0.5rem;
}

.conversation-scroll {
  max-height: 300px;
  overflow-y: auto;
}

.conversation-message {
  margin-bottom: 1rem;
}

.message-meta {
  font-size: 0.75rem;
  color: #6c757d;
  margin-bottom: 0.25rem;
}

.message-bubble {
  padding: 0.75rem;
  border-radius: 0.75rem;
  font-size: 0.875rem;
}

.message-bubble.customer {
  background: #e9ecef;
}

.message-bubble.agent {
  background: white;
  border: 1px solid #e9ecef;
}

.reply-box {
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  padding: 1rem;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  font-weight: 500;
}

.reply-content {
  margin-bottom: 0.75rem;
}

.reply-text {
  font-size: 0.875rem;
  color: #495057;
}

.reply-notes {
  font-size: 0.75rem;
  color: #6c757d;
}

.review-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-label {
  font-size: 0.75rem;
  color: #6c757d;
}

.review-buttons {
  display: flex;
  gap: 0.5rem;
}

.review-fields {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-success {
  padding: 0.75rem;
  background: #d4edda;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  color: #155724;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

@media (max-width: 768px) {
  .grid-2,
  .grid-3 {
    grid-template-columns: 1fr;
  }
  
  .grid-span-2 {
    grid-column: span 1;
  }
  
  .simulation-container {
    padding: 1rem;
  }
}
</style>

