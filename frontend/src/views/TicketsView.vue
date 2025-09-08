<template>
  <div class="tickets-container">
    <!-- Header -->
    <div class="header">
      <h1>Customer Service Tickets</h1>
      <div class="stats">
        <div class="stat-card">
          <span class="stat-number">{{ tickets.length }}</span>
          <span class="stat-label">Total Tickets</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ openTickets }}</span>
          <span class="stat-label">Open</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ inProgressTickets }}</span>
          <span class="stat-label">In Progress</span>
        </div>
      </div>
    </div>

    <div class="content">
      <!-- Ticket List Sidebar -->
      <div class="sidebar">
        <div class="search-box">
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="Search tickets..."
            class="search-input"
          >
        </div>
        
        <div class="ticket-list">
          <div 
            v-for="ticket in filteredTickets" 
            :key="ticket.id"
            :class="['ticket-item', { active: selectedTicket?.id === ticket.id }]"
            @click="selectTicket(ticket)"
          >
            <div class="ticket-header">
              <span class="ticket-id">#{{ ticket.id }}</span>
              <span :class="['status', ticket.status.toLowerCase().replace('_', '-')]">
                {{ ticket.status.replace('_', ' ') }}
              </span>
            </div>
            <div class="ticket-subject">{{ ticket.subject }}</div>
            <div class="ticket-user">{{ ticket.user.name }}</div>
            <div class="ticket-date">{{ formatDate(ticket.createdAt) }}</div>
          </div>
        </div>
      </div>

      <!-- Ticket Detail View -->
      <div class="main-content">
        <div v-if="selectedTicket" class="ticket-detail">
          <!-- Customer Info -->
          <div class="customer-info">
            <h2>Customer's Profile</h2>
            <div class="customer-details">
              <div class="detail-row">
                <span class="label">Name:</span>
                <span class="value">{{ selectedTicket.user.name }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Email:</span>
                <span class="value">{{ selectedTicket.user.email }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Phone:</span>
                <span class="value">{{ selectedTicket.user.phoneNumber }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Location:</span>
                <span class="value">{{ selectedTicket.user.location }}</span>
              </div>
              <div class="detail-row">
                <span class="label">Device:</span>
                <span class="value">{{ selectedTicket.user.device }}</span>
              </div>
              <div class="detail-row">
                <span class="label">UID:</span>
                <span class="value">{{ selectedTicket.user.uid }}</span>
              </div>
            </div>
          </div>

          <!-- Ticket Status Controls -->
          <div class="ticket-controls">
            <div class="control-group">
              <label>Status:</label>
              <select v-model="selectedTicket.status" @change="updateTicketStatus">
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="PENDING">Pending</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>
            <div class="control-group">
              <label>Priority:</label>
              <select v-model="selectedTicket.priority" @change="updateTicketStatus">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          <!-- Chat Messages -->
                  <div class="chat-container">
          <div class="chat-header">
            <div class="header-content">
              <h3>Conversation</h3>
              <div class="sentiment-indicator">
                <span class="sentiment-label">Conversation sentiment:</span>
                <span class="sentiment-value" :class="sentimentClass">{{ currentSentiment }}</span>
              </div>
            </div>
          </div>
          
          <!-- Scrollable Messages Area -->
          <div class="messages-wrapper">
            <div class="messages" ref="messagesContainer">
              <div 
                v-for="message in messages" 
                :key="message.id"
                :class="['message', message.senderType.toLowerCase()]"
              >
                <div class="message-content">
                  <div class="message-header">
                    <div class="message-text">{{ message.message }}</div>
                    <div class="message-actions">
                      <div class="dropdown" :class="{ active: activeDropdown === message.id }">
                        <button 
                          class="dropdown-toggle"
                          @click="toggleDropdown(message.id)"
                          aria-label="Message actions"
                        >
                          ⋮
                        </button>
                        <div class="dropdown-menu" v-if="activeDropdown === message.id">
                          <button 
                            class="dropdown-item"
                            @click="checkMessageSentiment(message)"
                          >
                            Check sentiment
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="message-meta">
                    <span class="sender">{{ message.senderName }}</span>
                    <span class="time">{{ formatTime(message.createdAt) }}</span>
                  </div>
                  
                  <!-- Individual Message Sentiment Display -->
                  <div v-if="messageSentiments[message.id]">
                    <!-- Collapsed State: Just the dropdown arrow -->
                    <div 
                      v-if="collapsedSentiments[message.id]"
                      class="sentiment-toggle-collapsed"
                      @click="toggleSentimentCollapse(message.id)"
                    >
                      <span class="sentiment-toggle-text">Sentiment</span>
                      <span class="sentiment-arrow">▼</span>
                    </div>
                    
                    <!-- Expanded State: Full sentiment analysis -->
                    <div 
                      v-else
                      class="message-sentiment-analysis"
                      :class="getSentimentClass(messageSentiments[message.id].label)"
                    >
                      <div 
                        class="sentiment-header" 
                        @click="toggleSentimentCollapse(message.id)"
                      >
                        <span class="sentiment-title">Message Sentiment Analysis</span>
                        <span class="sentiment-arrow">▲</span>
                      </div>
                      <div class="sentiment-details">
                        <span class="sentiment-label">{{ messageSentiments[message.id].label }}</span>
                        <span class="sentiment-score">(Score: {{ messageSentiments[message.id].score.toFixed(2) }})</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- AI Generating Indicator (inside messages area) -->
              <div v-if="isGeneratingAI" class="message ai-generating-message">
                <div class="message-content ai-thinking">
                  <div class="ai-indicator">
                    <span class="ai-icon">🤖</span>
                    <span class="ai-text">AI is typing...</span>
                    <div class="loading-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Fixed Message Input -->
          <div class="message-input-fixed">
            <div class="message-input">
              <input 
                v-model="newMessage"
                type="text"
                placeholder="Type your message..."
                @keyup.enter="sendMessage"
                class="message-field"
              >
              <button @click="sendMessage" class="send-button">Send</button>
            </div>
          </div>
        </div>
        </div>
        
        <div v-else class="no-selection">
          <h3>Select a ticket to view details</h3>
          <p>Choose a ticket from the list to see customer information and conversation history.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useBackendApi, type Ticket, type TicketMessage } from '@/composables/useBackendApi'

const { getAllTickets, getTicketMessages, addTicketMessage, updateTicketStatus: updateStatus, generateAIResponse, getConversationSentiment, analyzeMessageSentiment } = useBackendApi()

const tickets = ref<Ticket[]>([])
const selectedTicket = ref<Ticket | null>(null)
const messages = ref<TicketMessage[]>([])
const searchQuery = ref('')
const newMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const isGeneratingAI = ref(false)
const currentSentiment = ref('NEUTRAL') // Will be dynamically updated later
const activeDropdown = ref<number | null>(null)
const messageSentiments = ref<Record<number, {label: string, score: number}>>({})
const collapsedSentiments = ref<Record<number, boolean>>({})

const filteredTickets = computed(() => {
  if (!searchQuery.value) return tickets.value
  const query = searchQuery.value.toLowerCase()
  return tickets.value.filter(ticket => 
    ticket.subject.toLowerCase().includes(query) ||
    ticket.user.name.toLowerCase().includes(query) ||
    ticket.id.toString().includes(query)
  )
})

const openTickets = computed(() => 
  tickets.value.filter(t => t.status === 'OPEN').length
)

const inProgressTickets = computed(() => 
  tickets.value.filter(t => t.status === 'IN_PROGRESS').length
)

const sentimentClass = computed(() => {
  switch (currentSentiment.value) {
    case 'POSITIVE': return 'sentiment-positive'
    case 'NEGATIVE': return 'sentiment-negative'
    case 'NEUTRAL':
    default: return 'sentiment-neutral'
  }
})

const getSentimentClass = (label: string) => {
  switch (label) {
    case 'POSITIVE': return 'sentiment-positive'
    case 'NEGATIVE': return 'sentiment-negative'
    case 'NEUTRAL':
    default: return 'sentiment-neutral'
  }
}

const loadTickets = async () => {
  try {
    const response = await getAllTickets()
    tickets.value = response.data
    if (tickets.value.length > 0 && !selectedTicket.value) {
      selectTicket(tickets.value[0])
    }
  } catch (error) {
    console.error('Failed to load tickets:', error)
  }
}

const selectTicket = async (ticket: Ticket) => {
  selectedTicket.value = ticket
  await loadMessages(ticket.id)
}

const loadMessages = async (ticketId: number) => {
  try {
    const response = await getTicketMessages(ticketId)
    messages.value = response.data
    await nextTick()
    scrollToBottom()
    
    // Load sentiment after messages are loaded
    await loadSentiment(ticketId)
  } catch (error) {
    console.error('Failed to load messages:', error)
  }
}

const loadSentiment = async (ticketId: number) => {
  try {
    const response = await getConversationSentiment(ticketId)
    currentSentiment.value = response.data.label
    console.log('Loaded sentiment for ticket', ticketId, ':', response.data.label, '(score:', response.data.score, ')')
  } catch (error) {
    console.error('Failed to load sentiment:', error)
    currentSentiment.value = 'NEUTRAL'
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim() || !selectedTicket.value) return
  
  const userMessage = newMessage.value
  const ticketId = selectedTicket.value.id
  
  try {
    // Send the user's message
    const messageData = {
      message: userMessage,
      senderType: 'AGENT' as const,
      senderName: 'Support Agent'
    }
    
    await addTicketMessage(ticketId, messageData)
    newMessage.value = ''
    
    // Reload messages to show the user's message
    await loadMessages(ticketId)
    
    // Generate AI response automatically with realistic delay
    try {
      console.log('Generating AI response for message:', userMessage)
      isGeneratingAI.value = true
      
      // Add realistic delay (1.5-3 seconds) to simulate thinking
      const delay = 1500 + Math.random() * 1500 // Random delay between 1.5-3 seconds
      await new Promise(resolve => setTimeout(resolve, delay))
      
      await generateAIResponse(ticketId, userMessage, true) // addToTicket = true
      
      // Reload messages again to show the AI response
      await loadMessages(ticketId)
      console.log('AI response generated and added successfully')
      
      // Ensure scroll to bottom after AI response
      await nextTick()
      scrollToBottom()
    } catch (aiError) {
      console.error('Failed to generate AI response:', aiError)
      // Don't fail the whole operation if AI fails
    } finally {
      isGeneratingAI.value = false
    }
    
  } catch (error) {
    console.error('Failed to send message:', error)
  }
}

const updateTicketStatus = async () => {
  if (!selectedTicket.value) return
  
  try {
    await updateStatus(
      selectedTicket.value.id, 
      selectedTicket.value.status, 
      selectedTicket.value.priority
    )
    await loadTickets() // Refresh the ticket list
  } catch (error) {
    console.error('Failed to update ticket:', error)
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const toggleDropdown = (messageId: number) => {
  activeDropdown.value = activeDropdown.value === messageId ? null : messageId
}

const toggleSentimentCollapse = (messageId: number) => {
  collapsedSentiments.value[messageId] = !collapsedSentiments.value[messageId]
}

const checkMessageSentiment = async (message: TicketMessage) => {
  try {
    console.log('Analyzing sentiment for message:', message.id, message.message)
    
    // Call the sentiment analysis API using the composable
    const response = await analyzeMessageSentiment(message.message)
    
    // Store the sentiment result for this message
    messageSentiments.value[message.id] = {
      label: response.data.label,
      score: response.data.score
    }
    
    // Start collapsed by default
    collapsedSentiments.value[message.id] = true
    
    console.log('Sentiment analysis result:', response.data)
    
    // Close the dropdown
    activeDropdown.value = null
    
  } catch (error) {
    console.error('Error analyzing message sentiment:', error)
    activeDropdown.value = null
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadTickets()
  
  // Close dropdown when clicking outside
  document.addEventListener('click', (event) => {
    if (!event.target || !(event.target as Element).closest('.dropdown')) {
      activeDropdown.value = null
    }
  })
})
</script>

<style scoped>
.tickets-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.header {
  background: white;
  padding: 1rem 2rem;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  margin: 0;
  color: #333;
}

.stats {
  display: flex;
  gap: 1rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  min-width: 80px;
}

.stat-number {
  font-size: 1.5rem;
  font-weight: bold;
  color: #007bff;
}

.stat-label {
  font-size: 0.8rem;
  color: #666;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 350px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.search-box {
  padding: 1rem;
  border-bottom: 1px solid #e0e0e0;
}

.search-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.ticket-list {
  flex: 1;
  overflow-y: auto;
}

.ticket-item {
  padding: 1rem;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.ticket-item:hover {
  background: #f8f9fa;
}

.ticket-item.active {
  background: #e3f2fd;
  border-left: 3px solid #007bff;
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.ticket-id {
  font-weight: bold;
  color: #007bff;
}

.status {
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: bold;
  text-transform: uppercase;
}

.status.open { background: #fff3cd; color: #856404; }
.status.in-progress { background: #d1ecf1; color: #0c5460; }
.status.pending { background: #f8d7da; color: #721c24; }
.status.resolved { background: #d4edda; color: #155724; }
.status.closed { background: #e2e3e5; color: #383d41; }

.ticket-subject {
  font-weight: 500;
  margin-bottom: 0.3rem;
  color: #333;
}

.ticket-user {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.3rem;
}

.ticket-date {
  color: #999;
  font-size: 0.8rem;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ticket-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.customer-info {
  background: white;
  padding: 1rem 2rem;
  border-bottom: 1px solid #e0e0e0;
}

.customer-info h2 {
  margin: 0 0 1rem 0;
  color: #333;
}

.customer-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.detail-row {
  display: flex;
  flex-direction: column;
}

.label {
  font-weight: bold;
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.2rem;
}

.value {
  color: #333;
}

.ticket-controls {
  background: white;
  padding: 1rem 2rem;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  gap: 2rem;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.control-group label {
  font-weight: bold;
  color: #666;
}

.control-group select {
  padding: 0.3rem 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  margin: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  height: calc(100vh - 160px);
  min-height: 500px;
}

.chat-header {
  padding: 1rem;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  color: #333;
}

.sentiment-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sentiment-label {
  font-size: 0.9rem;
  color: #666;
  font-weight: 500;
}

.sentiment-value {
  font-size: 0.9rem;
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  text-transform: uppercase;
  font-size: 0.8rem;
}

.sentiment-positive {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.sentiment-negative {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.sentiment-neutral {
  background-color: #e2e3e5;
  color: #383d41;
  border: 1px solid #d6d8db;
}

/* Individual Message Sentiment Analysis Styles */
.message-sentiment-analysis {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.85rem;
  border-left: 4px solid;
}

.message-sentiment-analysis.sentiment-positive {
  background-color: #d4edda;
  color: #155724;
  border-left-color: #28a745;
}

.message-sentiment-analysis.sentiment-negative {
  background-color: #f8d7da;
  color: #721c24;
  border-left-color: #dc3545;
}

.message-sentiment-analysis.sentiment-neutral {
  background-color: #e2e3e5;
  color: #383d41;
  border-left-color: #6c757d;
}

.sentiment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  margin-bottom: 0.25rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.sentiment-header:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* Collapsed sentiment toggle - minimal design */
.sentiment-toggle-collapsed {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.5rem;
  padding: 0.4rem 0.6rem;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
  color: #6c757d;
  transition: all 0.2s ease;
}

.sentiment-toggle-collapsed:hover {
  background-color: #e9ecef;
  border-color: #dee2e6;
  color: #495057;
}

.sentiment-toggle-text {
  font-weight: 500;
}

.sentiment-arrow {
  font-size: 0.7rem;
  transition: transform 0.2s ease;
}

.sentiment-title {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sentiment-details {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-top: 0.25rem;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sentiment-label {
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.75rem;
}

.sentiment-score {
  font-family: monospace;
  font-size: 0.8rem;
  opacity: 0.8;
}

.messages-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0; /* Important for flex child to shrink */
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  scroll-behavior: smooth;
  min-height: 0; /* Important for scrolling */
}

.message {
  display: flex;
  position: relative;
}

.message.user .message-content {
  background: #e3f2fd;
  margin-left: auto;
  border-radius: 18px 18px 4px 18px;
}

.message.agent .message-content {
  background: #f5f5f5;
  margin-right: auto;
  border-radius: 18px 18px 18px 4px;
}

.message.system .message-content {
  background: #e8f4fd;
  margin-right: auto;
  border-radius: 18px 18px 18px 4px;
  border-left: 3px solid #007bff;
}

.message-content {
  max-width: 70%;
  padding: 0.8rem 1.2rem;
  word-wrap: break-word;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.message-actions {
  flex-shrink: 0;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-toggle {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #666;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  line-height: 1;
  transition: all 0.2s ease;
}

.dropdown-toggle:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: #333;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 160px;
  z-index: 1000;
  margin-top: 0.25rem;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  font-size: 0.9rem;
  color: #333;
  transition: background-color 0.2s ease;
}

.dropdown-item:hover {
  background-color: #f8f9fa;
}

.dropdown-item:first-child {
  border-radius: 8px 8px 0 0;
}

.dropdown-item:last-child {
  border-radius: 0 0 8px 8px;
}

.message-text {
  margin-bottom: 0.3rem;
  line-height: 1.4;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: #666;
}

.message-input-fixed {
  position: sticky;
  bottom: 0;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 0;
  flex-shrink: 0;
  z-index: 10;
}

.message-input {
  display: flex;
  padding: 1rem;
  gap: 0.5rem;
}

.message-field {
  flex: 1;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
}

.send-button {
  padding: 0.8rem 1.5rem;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: bold;
}

.send-button:hover {
  background: #0056b3;
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.ai-generating-message .message-content.ai-thinking {
  background: #f0f8ff;
  border: 2px dashed #007bff;
  margin-right: auto;
  border-radius: 18px 18px 18px 4px;
  animation: pulse 1.5s infinite;
}

.ai-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #007bff;
  font-size: 0.9rem;
}

@keyframes pulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

.ai-icon {
  font-size: 1.2rem;
}

.ai-text {
  font-style: italic;
}

.loading-dots {
  display: flex;
  gap: 0.2rem;
}

.loading-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #007bff;
  animation: loading 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.no-selection {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #666;
}

.no-selection h3 {
  margin-bottom: 0.5rem;
}
</style>
