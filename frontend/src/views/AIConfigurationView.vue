<template>
  <div v-if="loading" class="loading-container">
    <p>Loading configuration...</p>
  </div>
  <div v-else class="ai-config-container">
    <div class="config-content">
      <!-- Header -->
      <header class="config-header">
        <div class="header-content">
          <h1>AI Customisation</h1>
          <p class="subtitle">Configure your AI customer service agent. Rules are applied in priority order.</p>
        </div>
        <button class="btn btn-primary" @click="saveAllRules" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save All Rules' }}
        </button>
      </header>

      <!-- Template Variables Help - Hoverable & Clickable -->
      <div class="variables-help-container">
        <span 
          class="variables-help-trigger"
          :class="{ pinned: isVariablesPinned }"
          @mouseenter="handleMouseEnter"
          @mouseleave="handleMouseLeave"
          @click="toggleVariablesPin"
        >
          📋 Available Template Variables
          <span v-if="isVariablesPinned" class="pin-indicator">📌</span>
        </span>
        
        <!-- Hoverable/Clickable Modal -->
        <div 
          v-if="showVariablesModal || isVariablesPinned"
          class="variables-modal"
          @mouseenter="showVariablesModal = true"
          @mouseleave="handleModalMouseLeave"
        >
          <h3>Available Template Variables</h3>
          <p class="modal-description">You can use these variables in your configuration rules:</p>
          
          <div class="variable-list">
            <div class="variable-group">
              <strong>User Variables:</strong>
              <div class="variable-items">
                <code v-pre>{{user.name}}</code>
                <code v-pre>{{user.email}}</code>
                <code v-pre>{{user.location}}</code>
                <code v-pre>{{user.device}}</code>
              </div>
            </div>
            
            <div class="variable-group">
              <strong>Ticket Variables:</strong>
              <div class="variable-items">
                <code v-pre>{{ticket.id}}</code>
                <code v-pre>{{ticket.status}}</code>
                <code v-pre>{{ticket.priority}}</code>
                <code v-pre>{{ticket.category}}</code>
                <code v-pre>{{ticket.subject}}</code>
              </div>
            </div>
            
            <div v-if="customVariables.length > 0" class="variable-group">
              <strong>Custom Variables (from Metadata Configuration):</strong>
              <div class="variable-items">
                <code 
                  v-for="variable in customVariables" 
                  :key="variable"
                  v-pre
                >{{ variable }}</code>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="tabs-container">
        <div class="tabs-header">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            :class="['tab-button', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
          >
            {{ tab.label }}
            <span :class="['tab-badge', tab.badgeClass]">{{ tab.badgeText }}</span>
          </button>
        </div>

        <!-- Tab Content -->
        <div class="tab-content">
          <!-- General AI Rules Tab -->
          <div v-show="activeTab === 'general'" class="tab-panel">
            <div class="section-header">
              <h2>General AI Rules</h2>
              <span class="priority-badge lowest">Lowest Priority (Always Active)</span>
            </div>
            <p class="section-description">
              These rules are always present and provide the foundation for all interactions. 
              They can be overridden by Escalation Rules or Additional AI Rules.
            </p>
            <div class="rules-list">
              <div v-for="(rule, index) in generalRules" :key="rule.id || `general-${index}`" class="rule-item">
                <div class="rule-header">
                  <input
                    v-model="rule.title"
                    class="rule-title-input"
                    placeholder="Rule Title"
                    @blur="saveRule(rule)"
                  />
                  <button class="btn-icon" @click="removeRule('GENERAL', index)" title="Remove Rule">
                    ×
                  </button>
                </div>
                <textarea
                  v-model="rule.content"
                  class="rule-content-textarea"
                  placeholder="Enter rule content here..."
                  rows="6"
                  @blur="saveRule(rule)"
                ></textarea>
              </div>
              <button class="btn-add-rule" @click="addRule('GENERAL')">
                + Add General Rule
              </button>
            </div>
          </div>

          <!-- Escalation Rules Tab -->
          <div v-show="activeTab === 'escalation'" class="tab-panel">
            <div class="section-header">
              <h2>Escalation Rules</h2>
              <span class="priority-badge medium">Medium Priority (Overrides General Rules)</span>
            </div>
            <p class="section-description">
              These rules override General AI Rules when escalation is needed. 
              They can be overridden by Additional AI Rules.
            </p>
            <div class="rules-list">
              <div v-for="(rule, index) in escalationRules" :key="rule.id || `escalation-${index}`" class="rule-item">
                <div class="rule-header">
                  <input
                    v-model="rule.title"
                    class="rule-title-input"
                    placeholder="Rule Title"
                    @blur="saveRule(rule)"
                  />
                  <button class="btn-icon" @click="removeRule('ESCALATION', index)" title="Remove Rule">
                    ×
                  </button>
                </div>
                <textarea
                  v-model="rule.content"
                  class="rule-content-textarea"
                  placeholder="Enter rule content here..."
                  rows="6"
                  @blur="saveRule(rule)"
                ></textarea>
              </div>
              <button class="btn-add-rule" @click="addRule('ESCALATION')">
                + Add Escalation Rule
              </button>
            </div>
          </div>

          <!-- Additional AI Rules Tab -->
          <div v-show="activeTab === 'additional'" class="tab-panel">
            <div class="section-header">
              <h2>Additional AI Rules</h2>
              <span class="priority-badge highest">Highest Priority (Overrides All)</span>
            </div>
            <p class="section-description">
              These rules have the highest priority and override both General Rules and Escalation Rules. 
              Use these for specific edge cases that require special handling.
            </p>
            <div class="rules-list">
              <div v-for="(rule, index) in additionalRules" :key="rule.id || `additional-${index}`" class="rule-item">
                <div class="rule-header">
                  <input
                    v-model="rule.title"
                    class="rule-title-input"
                    placeholder="Rule Title"
                    @blur="saveRule(rule)"
                  />
                  <button class="btn-icon" @click="removeRule('ADDITIONAL', index)" title="Remove Rule">
                    ×
                  </button>
                </div>
                <textarea
                  v-model="rule.content"
                  class="rule-content-textarea"
                  placeholder="Enter rule content here..."
                  rows="6"
                  @blur="saveRule(rule)"
                ></textarea>
              </div>
              <button class="btn-add-rule" @click="addRule('ADDITIONAL')">
                + Add Additional Rule
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Save Button (also at bottom) -->
      <div class="save-footer">
        <button class="btn btn-primary" @click="saveAllRules" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save All Rules' }}
        </button>
        <p class="save-hint">Changes are saved automatically when you blur from a field, or click Save All Rules.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { 
  getAllAIRules, 
  createAIRule, 
  updateAIRule, 
  deleteAIRule,
  getAllMetadataConnections,
  getMetadataMappings
} = useBackendApi()

interface AIRule {
  id?: number | null
  title: string
  content: string
  category: 'GENERAL' | 'ESCALATION' | 'ADDITIONAL'
  displayOrder?: number
}

const generalRules = ref<AIRule[]>([])
const escalationRules = ref<AIRule[]>([])
const additionalRules = ref<AIRule[]>([])

const activeTab = ref<'general' | 'escalation' | 'additional'>('general')

const tabs = [
  {
    id: 'general' as const,
    label: 'General AI Rules',
    badgeText: 'Lowest Priority',
    badgeClass: 'lowest'
  },
  {
    id: 'escalation' as const,
    label: 'Escalation Rules',
    badgeText: 'Medium Priority',
    badgeClass: 'medium'
  },
  {
    id: 'additional' as const,
    label: 'Additional AI Rules',
    badgeText: 'Highest Priority',
    badgeClass: 'highest'
  }
]

const saving = ref(false)
const loading = ref(true)
const showVariablesModal = ref(false)
const isVariablesPinned = ref(false)
const customVariables = ref<string[]>([])

const toggleVariablesPin = () => {
  isVariablesPinned.value = !isVariablesPinned.value
  if (isVariablesPinned.value) {
    showVariablesModal.value = true
  }
}

const handleMouseEnter = () => {
  if (!isVariablesPinned.value) {
    showVariablesModal.value = true
  }
}

const handleMouseLeave = () => {
  if (!isVariablesPinned.value) {
    showVariablesModal.value = false
  }
}

const handleModalMouseLeave = () => {
  if (!isVariablesPinned.value) {
    showVariablesModal.value = false
  }
}

const loadRules = async () => {
  loading.value = true
  try {
    console.log('Loading AI rules...')
    const response = await getAllAIRules()
    console.log('Rules response:', response)
    
    if (response && response.data) {
      const rules: AIRule[] = response.data
      generalRules.value = rules.filter(r => r.category === 'GENERAL')
      escalationRules.value = rules.filter(r => r.category === 'ESCALATION')
      additionalRules.value = rules.filter(r => r.category === 'ADDITIONAL')
      
      // Sort by displayOrder
      generalRules.value.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
      escalationRules.value.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
      additionalRules.value.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
      
      console.log('Rules loaded:', { generalRules: generalRules.value, escalationRules: escalationRules.value, additionalRules: additionalRules.value })
    } else {
      // Initialize empty rules
      generalRules.value = []
      escalationRules.value = []
      additionalRules.value = []
    }
  } catch (error: any) {
    console.error('Failed to load AI rules:', error)
    // Initialize empty rules on error
    generalRules.value = []
    escalationRules.value = []
    additionalRules.value = []
  } finally {
    loading.value = false
  }
}

const addRule = (category: 'GENERAL' | 'ESCALATION' | 'ADDITIONAL') => {
  const newRule: AIRule = {
    id: null,
    title: '',
    content: '',
    category: category,
    displayOrder: 0
  }
  
  if (category === 'GENERAL') {
    newRule.displayOrder = generalRules.value.length
    generalRules.value.push(newRule)
  } else if (category === 'ESCALATION') {
    newRule.displayOrder = escalationRules.value.length
    escalationRules.value.push(newRule)
  } else {
    newRule.displayOrder = additionalRules.value.length
    additionalRules.value.push(newRule)
  }
}

const removeRule = async (category: 'GENERAL' | 'ESCALATION' | 'ADDITIONAL', index: number) => {
  const rules = category === 'GENERAL' ? generalRules.value : 
                category === 'ESCALATION' ? escalationRules.value : 
                additionalRules.value
  
  const rule = rules[index]
  
  if (rule.id) {
    // Delete from backend
    try {
      await deleteAIRule(rule.id)
    } catch (error) {
      console.error('Failed to delete rule:', error)
      alert('Failed to delete rule. Please try again.')
      return
    }
  }
  
  // Remove from local array
  rules.splice(index, 1)
  
  // Update display orders
  rules.forEach((r, i) => {
    r.displayOrder = i
  })
}

const saveRule = async (rule: AIRule) => {
  if (!rule.title.trim() && !rule.content.trim()) {
    // Don't save empty rules
    return
  }
  
  saving.value = true
  try {
    if (rule.id) {
      // Update existing rule
      await updateAIRule(rule.id, rule)
      console.log('Rule updated:', rule)
    } else {
      // Create new rule
      const response = await createAIRule(rule)
      if (response && response.data) {
        rule.id = response.data.id
        rule.displayOrder = response.data.displayOrder
        console.log('Rule created:', rule)
      }
    }
  } catch (error) {
    console.error('Failed to save rule:', error)
    alert('Failed to save rule. Please try again.')
  } finally {
    saving.value = false
  }
}

const saveAllRules = async () => {
  saving.value = true
  try {
    // Save all rules that have content
    const allRules = [...generalRules.value, ...escalationRules.value, ...additionalRules.value]
    const rulesToSave = allRules.filter(r => r.title.trim() || r.content.trim())
    
    for (const rule of rulesToSave) {
      await saveRule(rule)
    }
    
    alert('All rules saved successfully!')
  } catch (error) {
    console.error('Failed to save all rules:', error)
    alert('Failed to save some rules. Please check and try again.')
  } finally {
    saving.value = false
  }
}

const loadCustomVariables = async () => {
  try {
    const connectionsResponse = await getAllMetadataConnections()
    const connections = connectionsResponse.data || []
    
    const allVariables = new Set<string>()
    
    // Load mappings from all active connections
    for (const connection of connections) {
      if (connection.isActive && connection.id) {
        try {
          const mappingsResponse = await getMetadataMappings(connection.id)
          const mappings = mappingsResponse.data || []
          
          mappings.forEach((m: any) => {
            if (m.internalVariable && m.isActive) {
              allVariables.add(m.internalVariable)
            }
          })
        } catch (e) {
          console.warn('Failed to load mappings for connection', connection.id, e)
        }
      }
    }
    
    customVariables.value = Array.from(allVariables).sort()
  } catch (error) {
    console.error('Failed to load custom variables:', error)
  }
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const container = document.querySelector('.variables-help-container')
  const modal = document.querySelector('.variables-modal')
  
  if (isVariablesPinned.value && 
      container && 
      !container.contains(target) && 
      modal && 
      !modal.contains(target)) {
    isVariablesPinned.value = false
    showVariablesModal.value = false
  }
}

onMounted(() => {
  loadRules()
  loadCustomVariables()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.ai-config-container {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.config-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}

.header-content h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #333;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 1rem;
}

.variables-help-container {
  position: relative;
  margin-bottom: 1rem;
}

.variables-help-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #666;
  transition: all 0.2s;
  user-select: none;
}

.variables-help-trigger:hover {
  background: #e0e0e0;
  border-color: #1976d2;
  color: #1976d2;
}

.variables-help-trigger.pinned {
  background: #e3f2fd;
  border-color: #1976d2;
  color: #1976d2;
  font-weight: 600;
}

.pin-indicator {
  font-size: 0.8rem;
  margin-left: 0.25rem;
}

.variables-modal {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: white;
  border: 2px solid #1976d2;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 1.5rem;
  z-index: 1000;
  min-width: 500px;
  max-width: 700px;
}

.variables-modal h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: #333;
}

.modal-description {
  margin: 0 0 1rem 0;
  color: #666;
  font-size: 0.9rem;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.variable-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.variable-group strong {
  color: #333;
  font-size: 0.95rem;
  margin-bottom: 0.25rem;
}

.variable-items {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.variable-group code {
  background: #f8f9fa;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  color: #d63384;
  white-space: nowrap;
}

.tabs-container {
  background: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.tabs-header {
  display: flex;
  background: #f5f5f5;
  border-bottom: 2px solid #e0e0e0;
  gap: 0;
}

.tab-button {
  flex: 1;
  padding: 1rem 1.5rem;
  border: none;
  background: transparent;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
  position: relative;
}

.tab-button:hover {
  background: #f0f0f0;
  color: #333;
}

.tab-button.active {
  background: #fff;
  color: #1976d2;
  border-bottom-color: #1976d2;
}

.tab-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.tab-badge.lowest {
  background: #e3f2fd;
  color: #1976d2;
}

.tab-badge.medium {
  background: #fff3e0;
  color: #f57c00;
}

.tab-badge.highest {
  background: #fce4ec;
  color: #c2185b;
}

.tab-content {
  padding: 1.5rem;
}

.tab-panel {
  animation: fadeIn 0.2s ease-in;
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  margin-top: 0;
}

.section-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.priority-badge {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.priority-badge.lowest {
  background: #e3f2fd;
  color: #1976d2;
}

.priority-badge.medium {
  background: #fff3e0;
  color: #f57c00;
}

.priority-badge.highest {
  background: #fce4ec;
  color: #c2185b;
}

.section-description {
  margin: 0 0 1rem 0;
  color: #666;
  font-size: 0.95rem;
  line-height: 1.5;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.rule-item {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1rem;
  background: #fafafa;
}

.rule-header {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  align-items: center;
}

.rule-title-input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
}

.rule-title-input:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
}

.btn-icon {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: #dc3545;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-icon:hover {
  background: #c82333;
}

.rule-content-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.95rem;
  line-height: 1.6;
  resize: vertical;
  min-height: 120px;
}

.rule-content-textarea:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
}

.btn-add-rule {
  padding: 0.75rem 1rem;
  border: 2px dashed #ddd;
  border-radius: 6px;
  background: #fafafa;
  color: #666;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-rule:hover {
  border-color: #1976d2;
  background: #e3f2fd;
  color: #1976d2;
}

.save-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding-top: 2rem;
  border-top: 2px solid #e0e0e0;
}

.save-hint {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #1976d2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #1565c0;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  font-size: 1.2rem;
  color: #666;
}

@media (max-width: 768px) {
  .ai-config-container {
    padding: 1rem;
  }

  .config-header {
    flex-direction: column;
    gap: 1rem;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style>
