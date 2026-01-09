<template>
  <div v-if="loading" class="loading-container">
    <p>Loading configuration...</p>
  </div>
  <div v-else class="ai-config-container">
    <div class="config-content">
      <!-- Header -->
      <header class="config-header">
        <div class="header-content">
          <h1>AI Configuration</h1>
          <p class="subtitle">Configure your AI customer service agent. Rules are applied in priority order.</p>
        </div>
        <button class="btn" @click="saveConfiguration" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save Configuration' }}
        </button>
      </header>

      <!-- Template Variables Help -->
      <div class="template-help">
        <h3>Available Template Variables</h3>
        <p>You can use these variables in your configuration rules:</p>
        <div class="variable-list">
          <div class="variable-group">
            <strong>User Variables:</strong>
            <code v-pre>{{user.name}}</code>, 
            <code v-pre>{{user.location}}</code>, 
            <code v-pre>{{user.email}}</code>, 
            <code v-pre>{{user.device}}</code>
          </div>
          <div class="variable-group">
            <strong>Ticket Variables:</strong>
            <code v-pre>{{ticket.id}}</code>, 
            <code v-pre>{{ticket.status}}</code>, 
            <code v-pre>{{ticket.priority}}</code>, 
            <code v-pre>{{ticket.category}}</code>, 
            <code v-pre>{{ticket.subject}}</code>
          </div>
        </div>
      </div>

      <!-- Configuration Sections -->
      <div class="config-sections">
        <!-- General AI Rules -->
        <div class="config-section">
          <div class="section-header">
            <h2>General AI Rules</h2>
            <span class="priority-badge lowest">Lowest Priority (Always Active)</span>
          </div>
          <p class="section-description">
            These rules are always present and provide the foundation for all interactions. 
            They can be overridden by Escalation Processes or Edge Case Rules.
          </p>
          <textarea
            v-model="configuration.generalRules"
            class="config-textarea"
            placeholder="Enter general AI rules here. These apply to all interactions unless overridden..."
            rows="12"
          ></textarea>
        </div>

        <!-- Escalation Processes -->
        <div class="config-section">
          <div class="section-header">
            <h2>Escalation Processes</h2>
            <span class="priority-badge medium">Medium Priority (Overrides General Rules)</span>
          </div>
          <p class="section-description">
            These rules override General AI Rules when escalation is needed. 
            They can be overridden by Edge Case Rules.
          </p>
          <textarea
            v-model="configuration.escalationProcesses"
            class="config-textarea"
            placeholder="Enter escalation processes here. These override General Rules when escalation is needed..."
            rows="12"
          ></textarea>
        </div>

        <!-- Edge Case AI Rules -->
        <div class="config-section">
          <div class="section-header">
            <h2>Edge Case AI Rules</h2>
            <span class="priority-badge highest">Highest Priority (Overrides All)</span>
          </div>
          <p class="section-description">
            These rules have the highest priority and override both General Rules and Escalation Processes. 
            Use these for specific edge cases that require special handling.
          </p>
          <textarea
            v-model="configuration.edgeCaseRules"
            class="config-textarea"
            placeholder="Enter edge case rules here. These override both General Rules and Escalation Processes..."
            rows="12"
          ></textarea>
        </div>
      </div>

      <!-- Save Button (also at bottom) -->
      <div class="save-footer">
        <button class="btn btn-primary" @click="saveConfiguration" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save Configuration' }}
        </button>
        <p class="save-hint">Changes are saved to the database and will persist across restarts.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { getAIConfiguration, updateAIConfiguration } = useBackendApi()

const configuration = ref({
  id: null as number | null,
  generalRules: '',
  escalationProcesses: '',
  edgeCaseRules: ''
})

const saving = ref(false)
const loading = ref(true)

const loadConfiguration = async () => {
  loading.value = true
  try {
    console.log('Loading AI configuration...')
    const response = await getAIConfiguration()
    console.log('Configuration response:', response)
    if (response && response.data) {
      configuration.value = {
        id: response.data.id || null,
        generalRules: response.data.generalRules ?? '',
        escalationProcesses: response.data.escalationProcesses ?? '',
        edgeCaseRules: response.data.edgeCaseRules ?? ''
      }
      console.log('Configuration loaded:', configuration.value)
    } else {
      // Initialize empty configuration if none exists
      console.log('No configuration found, initializing empty')
      configuration.value = {
        id: null,
        generalRules: '',
        escalationProcesses: '',
        edgeCaseRules: ''
      }
    }
  } catch (error: any) {
    console.error('Failed to load AI configuration:', error)
    console.error('Error details:', error.response || error.message)
    // Initialize empty configuration on error
    configuration.value = {
      id: null,
      generalRules: '',
      escalationProcesses: '',
      edgeCaseRules: ''
    }
  } finally {
    loading.value = false
  }
}

const saveConfiguration = async () => {
  saving.value = true
  try {
    await updateAIConfiguration(configuration.value)
    alert('Configuration saved successfully!')
  } catch (error) {
    console.error('Failed to save configuration:', error)
    alert('Failed to save configuration. Please try again.')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfiguration()
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

.template-help {
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.template-help h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  color: #333;
}

.template-help p {
  margin: 0 0 1rem 0;
  color: #666;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.variable-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.variable-group strong {
  color: #333;
  min-width: 120px;
}

.variable-group code {
  background: #fff;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #ddd;
  font-family: 'Courier New', monospace;
  color: #d63384;
}

.config-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.config-section {
  background: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  transition: border-color 0.2s;
}

.config-section:hover {
  border-color: #bbb;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
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

.config-textarea {
  width: 100%;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.95rem;
  line-height: 1.6;
  resize: vertical;
  min-height: 200px;
}

.config-textarea:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
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
