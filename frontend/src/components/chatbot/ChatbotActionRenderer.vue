<template>
  <div class="chatbot-action">
    <div v-if="loading" class="action-loading">Loading action...</div>
    <div v-else-if="error" class="action-error">{{ error }}</div>
    <div v-else-if="action" class="action-component">
      <!-- Order Selector -->
      <div v-if="action.componentType === 'ORDER_SELECTOR'" class="order-selector">
        <label>Select Order:</label>
        <select v-model="selectedOrder" @change="handleOrderSelect">
          <option value="">-- Select an order --</option>
          <option v-for="order in orders" :key="order.id" :value="order.id">
            Order #{{ order.id }} - {{ order.total }} - {{ order.status }}
          </option>
        </select>
      </div>
      
      <!-- Date Picker -->
      <div v-else-if="action.componentType === 'DATE_PICKER'" class="date-picker">
        <label>{{ action.name }}:</label>
        <input type="date" v-model="selectedDate" @change="handleDateSelect" />
      </div>
      
      <!-- File Uploader -->
      <div v-else-if="action.componentType === 'FILE_UPLOADER'" class="file-uploader">
        <label>{{ action.name }}:</label>
        <input type="file" @change="handleFileSelect" />
      </div>
      
      <!-- Button -->
      <div v-else-if="action.componentType === 'BUTTON'" class="action-button">
        <button class="btn btn-primary" @click="handleButtonClick">
          {{ action.name }}
        </button>
      </div>
      
      <!-- Custom Component -->
      <div v-else-if="action.componentType === 'CUSTOM'" class="custom-action">
        <div v-html="action.config"></div>
      </div>
      
      <!-- Form -->
      <div v-else-if="action.componentType === 'FORM'" class="action-form">
        <form @submit.prevent="handleFormSubmit">
          <div v-for="field in formFields" :key="field.name" class="form-field">
            <label>{{ field.label }}:</label>
            <input 
              v-if="field.type === 'text'" 
              type="text" 
              v-model="formData[field.name]" 
              :required="field.required"
            />
            <input 
              v-else-if="field.type === 'email'" 
              type="email" 
              v-model="formData[field.name]" 
              :required="field.required"
            />
            <textarea 
              v-else-if="field.type === 'textarea'" 
              v-model="formData[field.name]" 
              :required="field.required"
            />
          </div>
          <button type="submit" class="btn btn-primary">Submit</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const props = defineProps<{
  actionKey: string
  param?: string
}>()

const emit = defineEmits<{
  actionTriggered: [data: { type: string; message?: string; context?: any; metadata?: Record<string, any> }]
}>()

const { getActiveChatbotActions } = useBackendApi()

const action = ref<any>(null)
const loading = ref(true)
const error = ref<string | null>(null)

// Component-specific state
const selectedOrder = ref('')
const orders = ref<any[]>([])
const selectedDate = ref('')
const formData = ref<Record<string, any>>({})
const formFields = ref<any[]>([])
const behavior = ref<any>(null)

onMounted(async () => {
  try {
    const response = await getActiveChatbotActions()
    const actions = response.data || []
    // Case-insensitive lookup to handle variations like Confirmation vs confirmation
    const foundAction = actions.find((a: any) => 
      a.actionKey.toLowerCase() === props.actionKey.toLowerCase()
    )
    
    if (foundAction) {
      action.value = foundAction
      
      // Parse config if it's JSON
      if (foundAction.config) {
        try {
          const config = JSON.parse(foundAction.config)
          if (foundAction.componentType === 'FORM' && config.component?.fields) {
            formFields.value = config.component.fields
            config.component.fields.forEach((field: any) => {
              formData.value[field.name] = ''
            })
          } else if (foundAction.componentType === 'ORDER_SELECTOR') {
            // Load orders from metadata or mock data
            loadOrders()
          }
          // Store behavior config for later use
          if (config.behavior) {
            action.value.behaviorConfig = config.behavior
            behavior.value = config.behavior
          }
        } catch (e: any) {
          console.error('Failed to parse action config:', e)
          console.error('Config string:', foundAction.config)
          console.error('Error details:', e.message, e.stack)
        }
      }
    } else {
      error.value = `Action "${props.actionKey}" not found`
    }
  } catch (e: any) {
    error.value = e.message || 'Failed to load action'
  } finally {
    loading.value = false
  }
})

const loadOrders = async () => {
  // TODO: Load orders from metadata API
  // For now, use mock data
  orders.value = [
    { id: 'ORD-001', total: '$99.99', status: 'Processing' },
    { id: 'ORD-002', total: '$149.50', status: 'Shipped' },
    { id: 'ORD-003', total: '$79.99', status: 'Delivered' }
  ]
}

const executeBehavior = (value: any) => {
  if (!behavior.value || !behavior.value.type) {
    console.warn('No behavior config found for action', { behavior: behavior.value, action: action.value })
    return
  }

  const behaviorConfig = behavior.value
  console.log('Executing behavior:', behaviorConfig.type, 'with value:', value, 'full config:', behaviorConfig)

  switch (behaviorConfig.type) {
    case 'ENGAGE_AI':
      // Replace variables in AI prompt with actual value
      let aiPrompt = behaviorConfig.aiPrompt || ''
      if (behaviorConfig.contextVariable && value) {
        // Replace context variable with actual value
        aiPrompt = aiPrompt.replace(behaviorConfig.contextVariable, value)
      }
      
      emit('actionTriggered', {
        type: 'ENGAGE_AI',
        message: aiPrompt,
        context: {
          variable: behaviorConfig.contextVariable,
          value: value
        }
      })
      break

    case 'SEND_MESSAGE':
      let message = behaviorConfig.messageTemplate || ''
      if (value) {
        message = message.replace('{{value}}', value)
      }
      emit('actionTriggered', {
        type: 'SEND_MESSAGE',
        message: message
      })
      break

    case 'UPDATE_METADATA':
      // For UPDATE_METADATA, if valueSource is "selected" but value is null/empty,
      // and metadataVariable is {{user.name}}, emit CONFIRM_NAME to extract from conversation
      if (behaviorConfig.metadataVariable && behaviorConfig.metadataVariable.includes('user.name')) {
        if (!value || (behaviorConfig.valueSource === 'selected' && !value)) {
          // Emit special event to extract name from conversation
          emit('actionTriggered', {
            type: 'CONFIRM_NAME',
            extractFromConversation: true
          })
          return
        }
      }
      
      emit('actionTriggered', {
        type: 'UPDATE_METADATA',
        metadata: {
          [behaviorConfig.metadataVariable || '']: value
        }
      })
      break

    case 'TRIGGER_WEBHOOK':
      // TODO: Implement webhook call
      console.log('Webhook triggered:', behaviorConfig.webhookUrl, value)
      emit('actionTriggered', {
        type: 'TRIGGER_WEBHOOK',
        context: {
          url: behaviorConfig.webhookUrl,
          method: behaviorConfig.httpMethod,
          body: behaviorConfig.requestBody,
          value: value
        }
      })
      break

    case 'CUSTOM_SCRIPT':
      // TODO: Execute custom script
      console.log('Custom script:', behaviorConfig.scriptCode)
      emit('actionTriggered', {
        type: 'CUSTOM_SCRIPT',
        context: {
          script: behaviorConfig.scriptCode,
          value: value
        }
      })
      break

    default:
      console.warn('Unknown behavior type:', behaviorConfig.type)
  }
}

const handleOrderSelect = () => {
  if (!selectedOrder.value) {
    return
  }
  
  if (!behavior.value) {
    console.warn('Behavior config not loaded yet for action')
    return
  }
  
  const order = orders.value.find(o => o.id === selectedOrder.value)
  const value = behavior.value.valueSource === 'selected' ? selectedOrder.value : 
                behavior.value.valueSource === 'custom' ? behavior.value.customValue : 
                JSON.stringify(order)
  executeBehavior(value)
}

const handleDateSelect = () => {
  if (selectedDate.value) {
    executeBehavior(selectedDate.value)
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    // For file uploads, we might want to upload first, then execute behavior
    executeBehavior(target.files[0].name)
  }
}

const handleButtonClick = () => {
  // For confirmation buttons, try to extract the name from recent conversation
  // This is a workaround - ideally the behavior config would specify what value to use
  let value = null
  
  // If this is a confirmation action and we need to get the name from conversation
  // We'll emit a special event that the parent can handle
  if (action.value?.name?.toLowerCase().includes('confirm')) {
    // Emit with a flag to extract name from conversation
    emit('actionTriggered', {
      type: 'CONFIRM_NAME',
      extractFromConversation: true
    })
    return
  }
  
  executeBehavior(value)
}

const handleFormSubmit = () => {
  executeBehavior(formData.value)
}
</script>

<style scoped>
.chatbot-action {
  margin-top: 1rem;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.action-loading, .action-error {
  padding: 0.5rem;
  text-align: center;
}

.action-error {
  color: #e74c3c;
}

.order-selector, .date-picker, .file-uploader {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.order-selector select, .date-picker input, .file-uploader input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.action-button {
  margin-top: 0.5rem;
}

.action-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}

.form-field input, .form-field textarea {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>

