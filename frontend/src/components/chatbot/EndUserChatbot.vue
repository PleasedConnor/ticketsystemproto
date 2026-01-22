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
        <div class="message-content" v-html="formatMessageWithActions(message.text, message.actions)"></div>
        <div v-if="message.actions && Object.keys(message.actions).length > 0" class="message-actions">
          <ChatbotActionRenderer 
            v-for="(param, actionKey) in message.actions" 
            :key="actionKey"
            :action-key="actionKey"
            :param="param"
            @action-triggered="handleActionTriggered"
          />
        </div>
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
import ChatbotActionRenderer from './ChatbotActionRenderer.vue'

const { generateChatbotResponse, createTicketFromChat } = useBackendApi()

// State
const messages = ref<Array<{ 
  text: string; 
  senderType: 'USER' | 'AGENT'; 
  senderName: string; 
  timestamp: Date;
  actions?: Record<string, string>;
}>>([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatEnded = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// Session variables storage (for this chat session only)
const sessionVariables = ref<Record<string, any>>({})

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
    // Build conversation history from previous messages in THIS chat session only
    // Exclude the current user message (which was just added) - only include messages BEFORE it
    // This helps the AI understand what has already happened vs what is happening now
    const conversationHistory = messages.value
      .slice(0, -1) // Exclude the current user message that was just added
      .slice(-4) // Last 4 messages for context (before the current message)
      .map(msg => `${msg.senderType === 'USER' ? 'User' : 'Agent'}: ${msg.text}`)
      .join('\n')
    
    console.log('Sending message with conversation history:', conversationHistory.substring(0, 200))
    const response = await generateChatbotResponse(messageText, conversationHistory, false)
    console.log('Received response:', response.data)
    
    // Add AI response with actions
    if (response.data && response.data.response) {
      console.log('Response actions:', response.data.actions)
      console.log('Response text:', response.data.response)
      messages.value.push({
        text: response.data.response,
        senderType: 'AGENT',
        senderName: 'AI Agent',
        timestamp: new Date(),
        actions: response.data.actions || {}
      })
    } else {
      messages.value.push({
        text: 'I apologize, but I encountered an error processing your message. Please try again.',
        senderType: 'AGENT',
        senderName: 'AI Agent',
        timestamp: new Date(),
        actions: {}
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

// Send a message silently (without showing it to the user) - used for action-triggered messages
const sendMessageSilently = async (messageText: string) => {
  if (!messageText || isLoading.value || chatEnded.value) return

  // Don't add user message to the chat - this is a background action
  // Just get the AI response directly
  
  isLoading.value = true
  try {
    // Build conversation history from visible messages (excluding the silent action message)
    const conversationHistory = messages.value
      .slice(-4)
      .map(msg => `${msg.senderName}: ${msg.text}`)
      .join('\n')
    
    console.log('Sending silent action message:', messageText)
    const response = await generateChatbotResponse(messageText, conversationHistory, true) // Pass flag for action-triggered
    
    // Add AI response with actions (but actions will be suppressed by backend)
    if (response.data && response.data.response) {
      console.log('Silent action response:', response.data)
      messages.value.push({
        text: response.data.response,
        senderType: 'AGENT',
        senderName: 'AI Agent',
        timestamp: new Date(),
        actions: response.data.actions || {} // Should be empty due to suppression
      })
    }
  } catch (error) {
    console.error('Failed to get AI response for action:', error)
    messages.value.push({
      text: 'I encountered an error processing that action. Please try again.',
      senderType: 'AGENT',
      senderName: 'AI Agent',
      timestamp: new Date()
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const handleActionTriggered = async (data: { type: string; message?: string; context?: any; metadata?: Record<string, any> }) => {
  console.log('Action triggered:', data)
  
  switch (data.type) {
    case 'ENGAGE_AI':
      // Send the AI prompt silently in the background without showing it to the user
      if (data.message) {
        await sendMessageSilently(data.message)
      }
      break
      
    case 'SEND_MESSAGE':
      // Send a predefined message
      if (data.message) {
        inputMessage.value = data.message
        await sendMessage()
      }
      break
      
    case 'CONFIRM_NAME':
      // Extract name from conversation history and save it
      const nameFromConversation = extractNameFromConversation()
      if (nameFromConversation) {
        sessionVariables.value['user.name'] = nameFromConversation
        console.log(`Saved name from conversation: ${nameFromConversation}`)
        messages.value.push({
          text: `✓ Name confirmed and saved: ${nameFromConversation}`,
          senderType: 'AGENT',
          senderName: 'System',
          timestamp: new Date()
        })
        scrollToBottom()
      } else {
        messages.value.push({
          text: `⚠ Could not find name in conversation. Please provide your name again.`,
          senderType: 'AGENT',
          senderName: 'System',
          timestamp: new Date()
        })
        scrollToBottom()
      }
      break
      
    case 'UPDATE_METADATA':
      // Save metadata to session variables (for this chat session only)
      console.log('Updating metadata:', data.metadata)
      if (data.metadata) {
        // Store each metadata variable in session storage
        Object.entries(data.metadata).forEach(([key, value]) => {
          // Handle variable format like "{{user.name}}" or just "user.name"
          let varKey = key
          if (varKey.startsWith('{{') && varKey.endsWith('}}')) {
            varKey = varKey.slice(2, -2) // Remove {{ and }}
          }
          sessionVariables.value[varKey] = value
          console.log(`Saved session variable: ${varKey} = ${value}`)
        })
        
        // Show confirmation message
        const metadataStr = Object.entries(data.metadata)
          .map(([key, value]) => {
            let varKey = key
            if (varKey.startsWith('{{') && varKey.endsWith('}}')) {
              varKey = varKey.slice(2, -2)
            }
            return `${varKey}: ${value}`
          })
          .join(', ')
        messages.value.push({
          text: `✓ Saved: ${metadataStr}`,
          senderType: 'AGENT',
          senderName: 'System',
          timestamp: new Date()
        })
        scrollToBottom()
      }
      break
      
    case 'TRIGGER_WEBHOOK':
      // TODO: Make webhook API call
      console.log('Triggering webhook:', data.context)
      // For now, just show a message
      messages.value.push({
        text: `Webhook triggered: ${data.context?.url || 'Unknown URL'}`,
        senderType: 'AGENT',
        senderName: 'System',
        timestamp: new Date()
      })
      scrollToBottom()
      break
      
    case 'CUSTOM_SCRIPT':
      // TODO: Execute custom script
      console.log('Executing custom script:', data.context)
      messages.value.push({
        text: 'Custom script executed',
        senderType: 'AGENT',
        senderName: 'System',
        timestamp: new Date()
      })
      scrollToBottom()
      break
      
    default:
      console.warn('Unknown action type:', data.type)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const extractNameFromConversation = (): string | null => {
  // Look through recent user messages to find a name
  // Check last 5 user messages
  const userMessages = messages.value
    .filter(msg => msg.senderType === 'USER')
    .slice(-5)
    .reverse()
  
  for (const msg of userMessages) {
    const text = msg.text.trim()
    // Try to match name patterns
    const nameMatch = text.match(/^(?:my name is|i'm|i am|this is|it's|it is)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)?)/i)
    if (nameMatch) {
      return nameMatch[1]
    }
    // If message is short and looks like a name (2-3 words, capitalized)
    if (text.length < 30 && /^[A-Z][a-z]+(?:\s+[A-Z][a-z]+)?$/.test(text)) {
      return text
    }
  }
  
  // Also check if we already extracted and stored it
  if (sessionVariables.value['user.name']) {
    return sessionVariables.value['user.name']
  }
  
  return null
}

const formatMessageWithActions = (text: string, actions: Record<string, string> | undefined) => {
  if (!text) return ''
  if (!actions || Object.keys(actions).length === 0) return text
  
  // Replace action placeholders with empty string in the text (they're rendered separately)
  let formatted = text
  for (const actionKey in actions) {
    const placeholder = `{action_${actionKey}}`
    formatted = formatted.replace(new RegExp(placeholder.replace(/[{}]/g, '\\$&'), 'g'), '')
  }
  return formatted.trim()
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
  console.log('Started new chat - conversation history cleared')
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

