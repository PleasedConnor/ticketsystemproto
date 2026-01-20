<template>
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
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { generateChatbotResponse, createTicketFromChat } = useBackendApi()

// State
const messages = ref<Array<{ text: string; senderType: 'USER' | 'AGENT'; senderName: string; timestamp: Date }>>([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatEnded = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// Methods
const sendMessage = async () => {
  const messageText = inputMessage.value.trim()
  if (!messageText || isLoading.value || chatEnded.value) return

  // Add user message
  messages.value.push({
    text: messageText,
    senderType: 'USER',
    senderName: 'You',
    timestamp: new Date()
  })

  inputMessage.value = ''
  scrollToBottom()

  // Get AI response
  isLoading.value = true
  try {
    // Build conversation history from previous messages
    // Reduced to last 4 messages to prevent context confusion
    const conversationHistory = messages.value
      .slice(-4) // Last 4 messages for context (reduced to prevent confusion)
      .map(msg => `${msg.senderName}: ${msg.text}`)
      .join('\n')
    
    const response = await generateChatbotResponse(messageText, conversationHistory)
    
    // Add AI response
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
    // Generate subject from first user message
    const firstUserMessage = messages.value.find(m => m.senderType === 'USER')
    const subject = firstUserMessage 
      ? firstUserMessage.text.substring(0, 50) + (firstUserMessage.text.length > 50 ? '...' : '')
      : 'Chatbot Conversation'

    // Generate description from conversation summary
    const description = `Conversation started via chatbot. Total messages: ${messages.value.length}`

    // Convert messages to chat format
    const chatMessages = messages.value.map(msg => ({
      message: msg.text,
      senderType: msg.senderType,
      senderName: msg.senderName || (msg.senderType === 'USER' ? 'You' : 'AI Agent')
    }))

    console.log('Saving chat as ticket:', { subject, description, messageCount: chatMessages.length })

    const response = await createTicketFromChat({
      subject,
      description,
      messages: chatMessages
    })

    console.log('Ticket created:', response.data)

    chatEnded.value = true
    alert(`Chat saved as ticket #${response.data.id}. You can review it in the Tickets section.`)
  } catch (error: any) {
    console.error('Failed to save chat as ticket:', error)
    console.error('Error details:', error.response?.data || error.message)
    const errorMessage = error.response?.data?.message || error.message || 'Unknown error'
    alert(`Failed to save chat as ticket: ${errorMessage}. Please check the console for details.`)
  }
}

const startNewChat = () => {
  messages.value = []
  inputMessage.value = ''
  chatEnded.value = false
}

// Auto-scroll on new messages
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
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
  white-space: pre-line; /* Preserve newlines from backend formatting */
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

/* Scrollbar styling */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>

