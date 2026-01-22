<template>
  <div class="page-container">
    <div class="page-header">
      <h1>Chatbot Actions</h1>
      <p>Configure interactive actions that the AI can display in the chatbot</p>
    </div>

    <div class="actions-list">
      <div class="actions-header">
        <button class="btn btn-primary" @click="openAddDialog">
          + Add Action
        </button>
      </div>

      <div v-if="loading" class="loading-state">Loading actions...</div>
      <div v-else-if="actions.length === 0" class="empty-state">
        <p>No actions configured. Click "Add Action" to create one.</p>
      </div>
      <div v-else class="actions-grid">
        <div v-for="action in actions" :key="action.id" class="action-card">
          <div class="action-header">
            <h3>{{ action.name }}</h3>
            <div class="action-badge" :class="action.isActive ? 'active' : 'inactive'">
              {{ action.isActive ? 'Active' : 'Inactive' }}
            </div>
          </div>
          <div class="action-body">
            <p class="action-key">Key: <code>{action_{{ action.actionKey }}}</code></p>
            <p class="action-type">Type: {{ formatComponentType(action.componentType) }}</p>
            <p v-if="action.description" class="action-description">{{ action.description }}</p>
          </div>
          <div class="action-actions">
            <button class="btn btn-secondary" @click="editAction(action)">Edit</button>
            <button class="btn btn-danger" @click="deleteAction(action.id)">Delete</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ editingAction ? 'Edit Action' : 'Add Action' }}</h3>
          <button class="dialog-close" @click="closeDialog">×</button>
        </div>
        <div class="dialog-form">
          <div class="step-indicator">
            <div class="step" :class="{ active: currentStep === 1, completed: currentStep === 2 }">
              <span class="step-number">1</span>
              <span class="step-label">Basic Configuration</span>
            </div>
            <div class="step-connector"></div>
            <div class="step" :class="{ active: currentStep === 2 }">
              <span class="step-number">2</span>
              <span class="step-label">Action Behavior</span>
            </div>
          </div>
          
          <div v-if="currentStep === 1">
          <div class="form-group">
            <label>Name <span class="required">*</span></label>
            <input v-model="formData.name" type="text" placeholder="e.g., Order Status Selector" required />
          </div>
          <div class="form-group">
            <label>Action Key <span class="required">*</span></label>
            <input v-model="formData.actionKey" type="text" placeholder="e.g., order_selector" required />
            <small>Use this key in AI config: {action_{{ formData.actionKey }}}</small>
          </div>
          <div class="form-group">
            <label>Component Type <span class="required">*</span></label>
            <select v-model="formData.componentType" required @change="onComponentTypeChange">
              <option value="">Select type...</option>
              <option value="ORDER_SELECTOR">Order Selector</option>
              <option value="DATE_PICKER">Date Picker</option>
              <option value="FILE_UPLOADER">File Uploader</option>
              <option value="FORM">Form</option>
              <option value="BUTTON">Button</option>
              <option value="CUSTOM">Custom</option>
            </select>
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="formData.description" rows="3" placeholder="Describe what this action does..."></textarea>
          </div>
          
          <!-- Component-Specific Configuration -->
          <div v-if="formData.componentType" class="component-config-section">
            <h4>Configuration</h4>
            
            <!-- Order Selector Config -->
            <div v-if="formData.componentType === 'ORDER_SELECTOR'" class="component-config">
              <div class="form-group">
                <label>Label Text</label>
                <input v-model="componentConfig.label" type="text" placeholder="e.g., Select Order" />
              </div>
              <div class="form-group">
                <label>Placeholder Text</label>
                <input v-model="componentConfig.placeholder" type="text" placeholder="e.g., -- Select an order --" />
              </div>
              <div class="form-group">
                <label>Metadata Variable for Orders</label>
                <input v-model="componentConfig.ordersVariable" type="text" placeholder="e.g., {{Order.list}}" />
                <small>Variable that contains the list of orders</small>
              </div>
            </div>
            
            <!-- Date Picker Config -->
            <div v-else-if="formData.componentType === 'DATE_PICKER'" class="component-config">
              <div class="form-group">
                <label>Label Text</label>
                <input v-model="componentConfig.label" type="text" placeholder="e.g., Select Date" />
              </div>
              <div class="form-group">
                <label>Min Date</label>
                <input v-model="componentConfig.minDate" type="date" />
              </div>
              <div class="form-group">
                <label>Max Date</label>
                <input v-model="componentConfig.maxDate" type="date" />
              </div>
            </div>
            
            <!-- File Uploader Config -->
            <div v-else-if="formData.componentType === 'FILE_UPLOADER'" class="component-config">
              <div class="form-group">
                <label>Label Text</label>
                <input v-model="componentConfig.label" type="text" placeholder="e.g., Upload File" />
              </div>
              <div class="form-group">
                <label>Accepted File Types</label>
                <input v-model="componentConfig.accept" type="text" placeholder="e.g., .pdf,.jpg,.png" />
                <small>Comma-separated list of file extensions</small>
              </div>
              <div class="form-group">
                <label>Max File Size (MB)</label>
                <input v-model.number="componentConfig.maxSize" type="number" min="1" />
              </div>
            </div>
            
            <!-- Form Config -->
            <div v-else-if="formData.componentType === 'FORM'" class="component-config">
              <div class="form-group">
                <label>Form Fields</label>
                <div v-for="(field, index) in componentConfig.fields" :key="index" class="form-field-item">
                  <div class="form-field-header">
                    <strong>Field {{ index + 1 }}</strong>
                    <button type="button" class="btn btn-sm btn-danger" @click="removeFormField(index)">Remove</button>
                  </div>
                  <div class="form-field-config">
                    <div class="form-group-inline">
                      <div>
                        <label>Field Name</label>
                        <input v-model="field.name" type="text" placeholder="e.g., email" />
                      </div>
                      <div>
                        <label>Label</label>
                        <input v-model="field.label" type="text" placeholder="e.g., Email Address" />
                      </div>
                    </div>
                    <div class="form-group-inline">
                      <div>
                        <label>Type</label>
                        <select v-model="field.type">
                          <option value="text">Text</option>
                          <option value="email">Email</option>
                          <option value="number">Number</option>
                          <option value="textarea">Textarea</option>
                          <option value="select">Select</option>
                        </select>
                      </div>
                      <div>
                        <label>
                          <input type="checkbox" v-model="field.required" />
                          Required
                        </label>
                      </div>
                    </div>
                    <div v-if="field.type === 'select'" class="form-group">
                      <label>Options (one per line)</label>
                      <textarea v-model="field.options" rows="3" placeholder="Option 1&#10;Option 2&#10;Option 3"></textarea>
                    </div>
                  </div>
                </div>
                <button type="button" class="btn btn-secondary" @click="addFormField">+ Add Field</button>
              </div>
            </div>
            
            <!-- Button Config -->
            <div v-else-if="formData.componentType === 'BUTTON'" class="component-config">
              <div class="form-group">
                <label>Button Text</label>
                <input v-model="componentConfig.buttonText" type="text" placeholder="e.g., Submit Order" />
              </div>
              <div class="form-group">
                <label>Button Style</label>
                <select v-model="componentConfig.buttonStyle">
                  <option value="primary">Primary</option>
                  <option value="secondary">Secondary</option>
                  <option value="success">Success</option>
                  <option value="danger">Danger</option>
                </select>
              </div>
            </div>
            
            <!-- Custom Config -->
            <div v-else-if="formData.componentType === 'CUSTOM'" class="component-config">
              <div class="form-group">
                <label>Custom HTML</label>
                <textarea v-model="componentConfig.html" rows="8" placeholder="Enter custom HTML here..."></textarea>
                <small>You can use HTML to create custom components</small>
              </div>
            </div>
          </div>
          
          <!-- Live Preview -->
          <div v-if="formData.componentType" class="preview-section">
            <h4>Preview</h4>
            <div class="preview-container">
              <div class="preview-message">
                <div class="preview-message-content">
                  This is how the action will appear in the chatbot:
                </div>
                <div class="preview-action">
                  <ActionPreview 
                    :component-type="formData.componentType"
                    :config="componentConfig"
                  />
                </div>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>
              <input type="checkbox" v-model="formData.isActive" />
              Active
            </label>
          </div>
          </div>
          
          <!-- Step 2: Action Behavior -->
          <div v-if="currentStep === 2" class="dialog-form-step2">
          <div class="step-header">
            <h3>Configure Action Behavior</h3>
            <p>Define what happens when a user interacts with this action</p>
          </div>
          
          <div class="form-group">
            <label>When user interacts with this action:</label>
            <select v-model="actionBehavior.type" @change="onBehaviorTypeChange">
              <option value="">Select action...</option>
              <option value="ENGAGE_AI">Engage AI with new context</option>
              <option value="SEND_MESSAGE">Send message to AI</option>
              <option value="UPDATE_METADATA">Update metadata variable</option>
              <option value="TRIGGER_WEBHOOK">Trigger webhook/API call</option>
              <option value="CUSTOM_SCRIPT">Run custom script</option>
            </select>
          </div>
          
          <!-- Engage AI Configuration -->
          <div v-if="actionBehavior.type === 'ENGAGE_AI'" class="behavior-config">
            <div class="form-group">
              <label>Context Variable</label>
              <input 
                v-model="actionBehavior.contextVariable" 
                type="text" 
                placeholder="e.g., {{Order.selected}}"
              />
              <small>The selected value will be stored in this variable for AI context</small>
            </div>
            <div class="form-group">
              <label>AI Prompt</label>
              <textarea 
                v-model="actionBehavior.aiPrompt" 
                rows="4"
                placeholder="e.g., The user has selected order {{Order.selected}}. Please provide information about this order."
              ></textarea>
              <small>This message will be sent to the AI with the selected value</small>
            </div>
            <div class="form-group">
              <label>
                <input type="checkbox" v-model="actionBehavior.autoSend" />
                Automatically send to AI (don't wait for user confirmation)
              </label>
            </div>
          </div>
          
          <!-- Send Message Configuration -->
          <div v-else-if="actionBehavior.type === 'SEND_MESSAGE'" class="behavior-config">
            <div class="form-group">
              <label>Message Template</label>
              <textarea 
                v-model="actionBehavior.messageTemplate" 
                rows="4"
                placeholder="e.g., I selected order {{Order.selected}}"
              ></textarea>
              <small>Use {{variable}} to include selected values</small>
            </div>
            <div class="form-group">
              <label>
                <input type="checkbox" v-model="actionBehavior.autoSend" />
                Automatically send message
              </label>
            </div>
          </div>
          
          <!-- Update Metadata Configuration -->
          <div v-else-if="actionBehavior.type === 'UPDATE_METADATA'" class="behavior-config">
            <div class="form-group">
              <label>Metadata Variable</label>
              <input 
                v-model="actionBehavior.metadataVariable" 
                type="text" 
                placeholder="e.g., {{Order.selected}}"
              />
            </div>
            <div class="form-group">
              <label>Value Source</label>
              <select v-model="actionBehavior.valueSource">
                <option value="selected">Selected value from action</option>
                <option value="custom">Custom value</option>
              </select>
            </div>
            <div v-if="actionBehavior.valueSource === 'custom'" class="form-group">
              <label>Custom Value</label>
              <input v-model="actionBehavior.customValue" type="text" />
            </div>
          </div>
          
          <!-- Trigger Webhook Configuration -->
          <div v-else-if="actionBehavior.type === 'TRIGGER_WEBHOOK'" class="behavior-config">
            <div class="form-group">
              <label>Webhook URL</label>
              <input v-model="actionBehavior.webhookUrl" type="url" placeholder="https://api.example.com/webhook" />
            </div>
            <div class="form-group">
              <label>HTTP Method</label>
              <select v-model="actionBehavior.httpMethod">
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="PATCH">PATCH</option>
              </select>
            </div>
            <div class="form-group">
              <label>Request Body (JSON)</label>
              <textarea 
                v-model="actionBehavior.requestBody" 
                rows="6"
                placeholder='{"orderId": "{{selected}}", "action": "order_selected"}'
              ></textarea>
              <small>Use {{selected}} to include the selected value</small>
            </div>
          </div>
          
          <!-- Custom Script Configuration -->
          <div v-else-if="actionBehavior.type === 'CUSTOM_SCRIPT'" class="behavior-config">
            <div class="form-group">
              <label>JavaScript Code</label>
              <textarea 
                v-model="actionBehavior.scriptCode" 
                rows="8"
                placeholder="// Custom JavaScript code&#10;const selected = actionValue;&#10;// Your code here"
              ></textarea>
              <small>Write custom JavaScript to handle the action. Use 'actionValue' variable for selected value.</small>
            </div>
          </div>
          
          <!-- Step Navigation -->
          <div class="dialog-actions">
            <button class="btn" @click="closeDialog">Cancel</button>
            <button class="btn btn-secondary" @click="currentStep = 1">Back</button>
            <button class="btn btn-primary" @click="saveAction" :disabled="saving || !actionBehavior.type">
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
          </div>
          
          <!-- Step Navigation for Step 1 -->
          <div v-if="currentStep === 1" class="dialog-actions">
            <button class="btn" @click="closeDialog">Cancel</button>
            <button class="btn btn-primary" @click="nextStep" :disabled="saving || !canProceed">
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'
import ActionPreview from '@/components/chatbot/ActionPreview.vue'

const { 
  getAllChatbotActions, 
  createChatbotAction, 
  updateChatbotAction, 
  deleteChatbotAction 
} = useBackendApi()

const actions = ref<any[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editingAction = ref<any>(null)
const saving = ref(false)
const currentStep = ref(1)

const formData = ref({
  name: '',
  actionKey: '',
  componentType: '',
  description: '',
  config: '',
  isActive: true
})

const componentConfig = ref<any>({})
const actionBehavior = ref<any>({
  type: '',
  contextVariable: '',
  aiPrompt: '',
  autoSend: true,
  messageTemplate: '',
  metadataVariable: '',
  valueSource: 'selected',
  customValue: '',
  webhookUrl: '',
  httpMethod: 'POST',
  requestBody: '',
  scriptCode: ''
})

// Watch component config changes and update JSON
watch(componentConfig, (newConfig) => {
  if (formData.value.componentType) {
    updateConfigJSON()
  }
}, { deep: true })

// Watch action behavior changes
watch(actionBehavior, () => {
  updateConfigJSON()
}, { deep: true })

const updateConfigJSON = () => {
  const fullConfig = {
    component: componentConfig.value,
    behavior: actionBehavior.value
  }
  formData.value.config = JSON.stringify(fullConfig, null, 2)
}

// Watch component type changes
watch(() => formData.value.componentType, (newType) => {
  if (newType) {
    initializeComponentConfig(newType)
  }
})

onMounted(() => {
  loadActions()
})

const loadActions = async () => {
  loading.value = true
  try {
    const response = await getAllChatbotActions()
    actions.value = response.data || []
  } catch (error) {
    console.error('Failed to load actions:', error)
    alert('Failed to load actions. Please try again.')
  } finally {
    loading.value = false
  }
}

const initializeComponentConfig = (componentType: string) => {
  switch (componentType) {
    case 'ORDER_SELECTOR':
      componentConfig.value = {
        label: 'Select Order',
        placeholder: '-- Select an order --',
        ordersVariable: '{{Order.list}}'
      }
      break
    case 'DATE_PICKER':
      componentConfig.value = {
        label: 'Select Date',
        minDate: '',
        maxDate: ''
      }
      break
    case 'FILE_UPLOADER':
      componentConfig.value = {
        label: 'Upload File',
        accept: '.pdf,.jpg,.png',
        maxSize: 10
      }
      break
    case 'FORM':
      componentConfig.value = {
        fields: [
          { name: 'email', label: 'Email', type: 'email', required: true, options: '' }
        ]
      }
      break
    case 'BUTTON':
      componentConfig.value = {
        buttonText: 'Submit',
        buttonStyle: 'primary'
      }
      break
    case 'CUSTOM':
      componentConfig.value = {
        html: '<div>Custom Component</div>'
      }
      break
    default:
      componentConfig.value = {}
  }
}

const onComponentTypeChange = () => {
  initializeComponentConfig(formData.value.componentType)
  // Reset behavior when component type changes
  actionBehavior.value = {
    type: '',
    contextVariable: '',
    aiPrompt: '',
    autoSend: true,
    messageTemplate: '',
    metadataVariable: '',
    valueSource: 'selected',
    customValue: '',
    webhookUrl: '',
    httpMethod: 'POST',
    requestBody: '',
    scriptCode: ''
  }
}

const onBehaviorTypeChange = () => {
  // Initialize default values based on behavior type
  if (actionBehavior.value.type === 'ENGAGE_AI') {
    if (!actionBehavior.value.contextVariable) {
      // Suggest context variable based on component type
      if (formData.value.componentType === 'ORDER_SELECTOR') {
        actionBehavior.value.contextVariable = '{{Order.selected}}'
        actionBehavior.value.aiPrompt = 'The user has selected order {{Order.selected}}. Please provide information about this order.'
      }
    }
  }
}

const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return formData.value.name && formData.value.actionKey && formData.value.componentType
  }
  return true
})

const nextStep = () => {
  if (currentStep.value === 1) {
    // Validate step 1
    if (!formData.value.name || !formData.value.actionKey || !formData.value.componentType) {
      alert('Please fill in all required fields')
      return
    }
    currentStep.value = 2
  }
}

const addFormField = () => {
  if (!componentConfig.value.fields) {
    componentConfig.value.fields = []
  }
  componentConfig.value.fields.push({
    name: '',
    label: '',
    type: 'text',
    required: false,
    options: ''
  })
}

const removeFormField = (index: number) => {
  if (componentConfig.value.fields) {
    componentConfig.value.fields.splice(index, 1)
  }
}

const openAddDialog = () => {
  editingAction.value = null
  currentStep.value = 1
  formData.value = {
    name: '',
    actionKey: '',
    componentType: '',
    description: '',
    config: '',
    isActive: true
  }
  componentConfig.value = {}
  actionBehavior.value = {
    type: '',
    contextVariable: '',
    aiPrompt: '',
    autoSend: true,
    messageTemplate: '',
    metadataVariable: '',
    valueSource: 'selected',
    customValue: '',
    webhookUrl: '',
    httpMethod: 'POST',
    requestBody: '',
    scriptCode: ''
  }
  showDialog.value = true
}

const editAction = (action: any) => {
  editingAction.value = action
  currentStep.value = 1
  formData.value = {
    name: action.name,
    actionKey: action.actionKey,
    componentType: action.componentType,
    description: action.description || '',
    config: action.config || '',
    isActive: action.isActive
  }
  
  // Parse existing config
  if (action.config) {
    try {
      const parsed = JSON.parse(action.config)
      // Handle both old format (just component config) and new format (with behavior)
      if (parsed.component) {
        componentConfig.value = parsed.component
        actionBehavior.value = parsed.behavior || {
          type: '',
          contextVariable: '',
          aiPrompt: '',
          autoSend: true,
          messageTemplate: '',
          metadataVariable: '',
          valueSource: 'selected',
          customValue: '',
          webhookUrl: '',
          httpMethod: 'POST',
          requestBody: '',
          scriptCode: ''
        }
      } else {
        // Old format - just component config
        componentConfig.value = parsed
        actionBehavior.value = {
          type: '',
          contextVariable: '',
          aiPrompt: '',
          autoSend: true,
          messageTemplate: '',
          metadataVariable: '',
          valueSource: 'selected',
          customValue: '',
          webhookUrl: '',
          httpMethod: 'POST',
          requestBody: '',
          scriptCode: ''
        }
      }
    } catch (e) {
      console.error('Failed to parse config:', e)
      initializeComponentConfig(action.componentType)
    }
  } else {
    initializeComponentConfig(action.componentType)
  }
  
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
  editingAction.value = null
  currentStep.value = 1
}

const saveAction = async () => {
  if (!formData.value.name || !formData.value.actionKey || !formData.value.componentType) {
    alert('Please fill in all required fields')
    return
  }
  
  if (!actionBehavior.value.type) {
    alert('Please select an action behavior')
    return
  }

  // Update config JSON before saving
  updateConfigJSON()

  // Log what we're sending
  console.log('=== SAVING ACTION ===')
  console.log('Form Data:', formData.value)
  console.log('Component Config:', componentConfig.value)
  console.log('Action Behavior:', actionBehavior.value)
  console.log('Config JSON:', formData.value.config)
  console.log('=== END SAVE DATA ===')

  saving.value = true
  try {
    if (editingAction.value) {
      console.log('Updating action:', editingAction.value.id)
      await updateChatbotAction(editingAction.value.id, formData.value)
    } else {
      console.log('Creating new action')
      await createChatbotAction(formData.value)
    }
    await loadActions()
    closeDialog()
    alert('Action saved successfully!')
  } catch (error: any) {
    console.error('=== ERROR SAVING ACTION ===')
    console.error('Error:', error)
    console.error('Response:', error.response)
    console.error('Response Data:', error.response?.data)
    console.error('=== END ERROR ===')
    alert(error.response?.data?.error || error.message || 'Failed to save action. Please try again.')
  } finally {
    saving.value = false
  }
}

const deleteAction = async (id: number) => {
  if (!confirm('Are you sure you want to delete this action?')) {
    return
  }

  try {
    await deleteChatbotAction(id)
    await loadActions()
  } catch (error) {
    console.error('Failed to delete action:', error)
    alert('Failed to delete action. Please try again.')
  }
}

const formatComponentType = (type: string) => {
  return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}
</script>

<style scoped>
.page-container {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  color: #333;
}

.page-header p {
  color: #666;
  margin: 0;
}

.actions-header {
  margin-bottom: 1.5rem;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.action-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.action-header h3 {
  margin: 0;
  color: #333;
}

.action-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 500;
}

.action-badge.active {
  background: #d4edda;
  color: #155724;
}

.action-badge.inactive {
  background: #f8d7da;
  color: #721c24;
}

.action-body {
  margin-bottom: 1rem;
}

.action-key {
  font-size: 0.9rem;
  color: #666;
  margin: 0.5rem 0;
}

.action-key code {
  background: #f5f5f5;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: monospace;
}

.action-type {
  font-size: 0.9rem;
  color: #666;
  margin: 0.5rem 0;
}

.action-description {
  color: #666;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.action-actions {
  display: flex;
  gap: 0.5rem;
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
}

.dialog {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #ddd;
}

.dialog-header h3 {
  margin: 0;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
}

.dialog-form {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.form-group .required {
  color: #e74c3c;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.85rem;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 2rem;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.component-config-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #ddd;
}

.component-config-section h4 {
  margin: 0 0 1rem 0;
  color: #333;
}

.component-config {
  background: #f9f9f9;
  padding: 1rem;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.form-group-inline {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field-item {
  background: white;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.form-field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.form-field-config {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.85rem;
}

.preview-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #ddd;
}

.preview-section h4 {
  margin: 0 0 1rem 0;
  color: #333;
}

.preview-container {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
}

.preview-message {
  padding: 1rem;
}

.preview-message-content {
  margin-bottom: 1rem;
  color: #666;
  font-size: 0.9rem;
}

.preview-action {
  margin-top: 0.5rem;
}

.step-indicator {
  display: flex;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #ddd;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ddd;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-bottom: 0.5rem;
  transition: all 0.3s;
}

.step.active .step-number {
  background: #e74c3c;
  color: white;
}

.step.completed .step-number {
  background: #27ae60;
  color: white;
}

.step-label {
  font-size: 0.85rem;
  color: #666;
  text-align: center;
}

.step.active .step-label {
  color: #e74c3c;
  font-weight: 500;
}

.step-connector {
  flex: 1;
  height: 2px;
  background: #ddd;
  margin: 0 1rem;
  margin-top: -20px;
}

.step.completed + .step-connector {
  background: #27ae60;
}

.dialog-form-step2 {
  padding: 1.5rem;
}

.step-header {
  margin-bottom: 1.5rem;
}

.step-header h3 {
  margin: 0 0 0.5rem 0;
  color: #333;
}

.step-header p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.behavior-config {
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 4px;
  border: 1px solid #ddd;
  margin-top: 1rem;
}
</style>

