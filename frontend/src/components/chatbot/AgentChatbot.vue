<template>
  <div class="agent-chatbot-container">
    <!-- Main Chat Area -->
    <div class="chat-area">
      <div class="chat-window">
        <div class="chat-messages" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message', message.senderType === 'USER' ? 'user-message' : 'agent-message']"
          >
            <div class="message-header">
              <span class="message-sender">{{ message.senderName }}</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-content">{{ message.text }}</div>
          </div>
          <div v-if="isLoading" class="message agent-message">
            <div class="message-content">
              <span class="typing-indicator">AI is typing...</span>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input-area">
          <div class="input-container">
            <input
              v-model="inputMessage"
              type="text"
              class="chat-input"
              placeholder="Type your message..."
              @keyup.enter="sendMessage"
              :disabled="isLoading || chatEnded"
            />
            <button
              class="btn send-btn"
              @click="sendMessage"
              :disabled="!inputMessage.trim() || isLoading || chatEnded"
            >
              Send
            </button>
          </div>
          <div class="chat-actions">
            <button
              class="btn btn-secondary end-chat-btn"
              @click="endChat"
              :disabled="messages.length === 0 || chatEnded"
            >
              End Chat
            </button>
            <button
              v-if="chatEnded"
              class="btn btn-secondary"
              @click="startNewChat"
            >
              Start New Chat
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Customer Profile Panel -->
    <div class="customer-profile-panel">
      <div class="profile-header">
        <h2>Customer Profile</h2>
        <button class="btn-icon" @click="refreshProfile" title="Refresh">
          🔄
        </button>
      </div>
      
      <div v-if="loadingProfile" class="profile-loading">
        <p>Loading customer information...</p>
      </div>
      
      <div v-else class="profile-content">
        <div 
          v-for="field in profileFields" 
          :key="field.fieldKey"
          class="profile-field"
        >
          <div class="field-label">
            <span v-if="field.icon" class="field-icon">{{ field.icon }}</span>
            {{ field.fieldLabel }}
          </div>
          <div class="field-value">
            <input
              v-if="field.isEditable"
              v-model="field.value"
              :type="getInputType(field.fieldType)"
              class="field-input"
              @blur="updateField(field)"
            />
            <span v-else class="field-text">{{ field.value || 'N/A' }}</span>
          </div>
          <div v-if="field.description" class="field-description">
            {{ field.description }}
          </div>
        </div>
        
        <div v-if="profileFields.length === 0" class="profile-empty">
          <p>No customer profile fields configured.</p>
          <p class="hint">Configure fields in Customer Profile Settings.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { generateChatbotResponse, createTicketFromChat, getCustomerProfileFields } = useBackendApi()

// Chat state
const messages = ref<Array<{ text: string; senderType: 'USER' | 'AGENT'; senderName: string; timestamp: Date }>>([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatEnded = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// Profile state
const loadingProfile = ref(false)
const profileFields = ref<Array<{
  fieldKey: string
  fieldLabel: string
  metadataVariable: string
  fieldType: string
  value: string
  isEditable: boolean
  icon?: string
  description?: string
}>>([])

// Mock customer data for now - will be replaced with actual metadata resolution
const mockCustomerData: Record<string, string> = {
  'customer_name': 'John Doe',
  'customer_email': 'john.doe@example.com',
  'customer_phone': '+1 (555) 123-4567',
  'order_total': '$299.99',
  'order_status': 'Pending',
  'customer_location': 'New York, NY'
}

// Load customer profile fields
const loadProfileFields = async () => {
  loadingProfile.value = true
  try {
    // Load visible fields from API
    const response = await getCustomerProfileFields()
    const fields = response.data || []
    
    // Sort by display order
    const sortedFields = [...fields].sort((a, b) => a.displayOrder - b.displayOrder)
    
    // Populate with mock values for now (in production, these would come from metadata resolution via ticket context)
    profileFields.value = sortedFields.map(field => ({
      fieldKey: field.fieldKey,
      fieldLabel: field.fieldLabel,
      metadataVariable: field.metadataVariable || '',
      fieldType: field.fieldType,
      isEditable: field.isEditable || false,
      icon: field.icon || '',
      description: field.description || '',
      value: mockCustomerData[field.fieldKey] || ''
    }))
  } catch (error) {
    console.error('Failed to load profile fields:', error)
    profileFields.value = []
  } finally {
    loadingProfile.value = false
  }
}

const refreshProfile = () => {
  loadProfileFields()
}

const updateField = async (field: any) => {
  // TODO: Implement API call to update field value
  console.log('Updating field:', field.fieldKey, 'to:', field.value)
}

const getInputType = (fieldType: string) => {
  switch (fieldType) {
    case 'EMAIL':
      return 'email'
    case 'PHONE':
      return 'tel'
    case 'NUMBER':
      return 'number'
    case 'DATE':
      return 'date'
    case 'BOOLEAN':
      return 'checkbox'
    default:
      return 'text'
  }
}

// Chat methods (same as EndUserChatbot)
const sendMessage = async () => {
  const messageText = inputMessage.value.trim()
  if (!messageText || isLoading.value || chatEnded.value) return

  messages.value.push({
    text: messageText,
    senderType: 'USER',
    senderName: 'Customer',
    timestamp: new Date()
  })

  inputMessage.value = ''
  scrollToBottom()

  isLoading.value = true
  try {
    const conversationHistory = messages.value
      .slice(-10)
      .map(msg => `${msg.senderName}: ${msg.text}`)
      .join('\n')
    
    const response = await generateChatbotResponse(messageText, conversationHistory)
    
    if (response.data && response.data.response) {
      messages.value.push({
        text: response.data.response,
        senderType: 'AGENT',
        senderName: 'AI Agent',
        timestamp: new Date()
      })
    } else {
      messages.value.push({
        text: 'I apologize, but I encountered an error processing your message. Please try again.',
        senderType: 'AGENT',
        senderName: 'AI Agent',
        timestamp: new Date()
      })
    }
  } catch (error) {
    console.error('Failed to get AI response:', error)
    messages.value.push({
      text: 'I apologize, but I encountered an error. Please try again.',
      senderType: 'AGENT',
      senderName: 'AI Agent',
      timestamp: new Date()
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatTime = (date: Date) => {
  return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const endChat = async () => {
  if (messages.value.length === 0 || chatEnded.value) return
  if (!confirm('End this chat and save it as a ticket?')) return

  try {
    const firstUserMessage = messages.value.find(m => m.senderType === 'USER')
    const subject = firstUserMessage 
      ? firstUserMessage.text.substring(0, 50) + (firstUserMessage.text.length > 50 ? '...' : '')
      : 'Chatbot Conversation'

    const description = `Conversation started via agent chatbot. Total messages: ${messages.value.length}`

    const chatMessages = messages.value.map(msg => ({
      message: msg.text,
      senderType: msg.senderType,
      senderName: msg.senderName || (msg.senderType === 'USER' ? 'Customer' : 'AI Agent')
    }))

    const response = await createTicketFromChat({
      subject,
      description,
      messages: chatMessages
    })

    chatEnded.value = true
    alert(`Chat saved as ticket #${response.data.id}. You can review it in the Tickets section.`)
  } catch (error: any) {
    console.error('Failed to save chat as ticket:', error)
    const errorMessage = error.response?.data?.message || error.message || 'Unknown error'
    alert(`Failed to save chat as ticket: ${errorMessage}. Please check the console for details.`)
  }
}

const startNewChat = () => {
  messages.value = []
  inputMessage.value = ''
  chatEnded.value = false
}

onMounted(() => {
  scrollToBottom()
  loadProfileFields()
})
</script>

<style scoped>
.agent-chatbot-container {
  flex: 1;
  display: flex;
  gap: 1rem;
  overflow: hidden;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 0.5rem;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  align-self: flex-end;
}

.agent-message {
  align-self: flex-start;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.25rem;
  font-size: 0.75rem;
  color: #6c757d;
}

.message-sender {
  font-weight: 500;
}

.message-time {
  margin-left: 0.5rem;
}

.message-content {
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  word-wrap: break-word;
  line-height: 1.5;
}

.user-message .message-content {
  background: linear-gradient(135deg, #dc3545 0%, #c0392b 100%);
  color: white;
  border-bottom-right-radius: 0.25rem;
}

.agent-message .message-content {
  background: #e9ecef;
  color: #495057;
  border-bottom-left-radius: 0.25rem;
}

.typing-indicator {
  font-style: italic;
  color: #6c757d;
}

.chat-input-area {
  border-top: 1px solid #e9ecef;
  padding: 1rem 1.5rem;
  background: #f8f9fa;
}

.input-container {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.chat-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #e9ecef;
  border-radius: 2rem;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1);
}

.chat-input:disabled {
  background: #e9ecef;
  cursor: not-allowed;
}

.send-btn {
  padding: 0.75rem 2rem;
  border-radius: 2rem;
  white-space: nowrap;
  background: #dc3545;
  color: white;
  border: none;
  cursor: pointer;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #5a6268;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.end-chat-btn {
  padding: 0.5rem 1.5rem;
}

/* Customer Profile Panel */
.customer-profile-panel {
  width: 350px;
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.profile-header {
  padding: 1rem 1.5rem;
  border-bottom: 2px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.profile-header h2 {
  margin: 0;
  font-size: 1.25rem;
  color: #333;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-icon:hover {
  background: #e9ecef;
}

.profile-loading {
  padding: 2rem;
  text-align: center;
  color: #6c757d;
}

.profile-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.profile-field {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f0f0f0;
}

.profile-field:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
}

.field-icon {
  font-size: 1rem;
}

.field-value {
  margin-bottom: 0.25rem;
}

.field-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: border-color 0.2s;
}

.field-input:focus {
  outline: none;
  border-color: #dc3545;
  box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.1);
}

.field-text {
  font-size: 0.95rem;
  color: #333;
  word-break: break-word;
}

.field-description {
  font-size: 0.75rem;
  color: #999;
  margin-top: 0.25rem;
  font-style: italic;
}

.profile-empty {
  padding: 2rem;
  text-align: center;
  color: #999;
}

.profile-empty .hint {
  font-size: 0.85rem;
  margin-top: 0.5rem;
  color: #ccc;
}

/* Scrollbar styling */
.chat-messages::-webkit-scrollbar,
.profile-content::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track,
.profile-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb,
.profile-content::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.profile-content::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>

