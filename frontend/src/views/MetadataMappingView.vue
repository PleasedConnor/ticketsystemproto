<template>
  <div class="mapping-view-container">
    <div class="mapping-header">
      <div class="header-content">
        <button class="btn-back" @click="goBack">← Back</button>
        <h1>Field Mapping: {{ connectionName }}</h1>
        <p class="subtitle">Map external API fields to internal template variables</p>
      </div>
      <button class="btn btn-primary" @click="saveAllMappings" :disabled="saving">
        {{ saving ? 'Saving...' : 'Save All Mappings' }}
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <p>Loading connection data...</p>
    </div>

    <div v-else-if="!connectionName" class="error-state">
      <p>Failed to load connection. Please try again.</p>
      <button class="btn btn-secondary" @click="goBack">Go Back</button>
    </div>

    <div v-else class="mapping-content">
      <!-- Available Fields Section -->
      <div class="fields-section">
        <div class="section-header">
          <h2>Available Data Points from API</h2>
          <button class="btn-small" @click="testAndRefreshFields" :disabled="testing">
            {{ testing ? 'Testing...' : '🔄 Refresh Fields' }}
          </button>
        </div>
        
        <div v-if="availableFields.length === 0" class="empty-fields">
          <p>No fields available. Click "Refresh Fields" to test the connection and discover available data points.</p>
        </div>
        
        <div v-else class="fields-grid">
          <div 
            v-for="field in availableFields" 
            :key="field"
            class="field-card"
            :class="{ mapped: isFieldMapped(field) }"
            @click="selectField(field)"
          >
            <div class="field-name">{{ field }}</div>
            <div v-if="fieldTypes[field]" class="field-type">
              <span class="type-badge" :class="getTypeClass(fieldTypes[field])">
                {{ fieldTypes[field] }}
              </span>
            </div>
            <div v-if="isFieldMapped(field)" class="field-mapped-to">
              → {{ getMappingForField(field)?.internalVariable }}
            </div>
          </div>
        </div>
      </div>

      <!-- Mappings Section -->
      <div class="mappings-section">
        <div class="section-header">
          <h2>Current Mappings</h2>
          <button class="btn-small" @click="addNewMapping">
            + Add Mapping
          </button>
        </div>

        <div v-if="mappings.length === 0" class="empty-mappings">
          <p>No mappings configured. Click on a field above or "Add Mapping" to create one.</p>
        </div>

        <div v-else class="mappings-list">
          <div 
            v-for="(mapping, index) in mappings" 
            :key="index"
            class="mapping-card"
          >
            <div class="mapping-row">
              <div class="mapping-source">
                <label>External Field Path</label>
                <input
                  v-model="mapping.externalFieldPath"
                  type="text"
                  placeholder="user.name"
                  list="fields-datalist"
                  @focus="showFieldSuggestions = true"
                  @change="updateDataTypeFromField(mapping)"
                />
                <datalist id="fields-datalist">
                  <option v-for="field in availableFields" :key="field" :value="field" />
                </datalist>
                <div v-if="fieldTypes[mapping.externalFieldPath]" class="detected-type">
                  Detected: <span class="type-badge-small" :class="getTypeClass(fieldTypes[mapping.externalFieldPath])">
                    {{ fieldTypes[mapping.externalFieldPath] }}
                  </span>
                </div>
              </div>
              
              <div class="mapping-transform">
                <label>Data Type</label>
                <select v-model="mapping.dataType" class="data-type-select">
                  <option value="">Auto-detect</option>
                  <optgroup label="Primitive Types">
                    <option value="STRING">STRING</option>
                    <option value="INTEGER">INTEGER</option>
                    <option value="DECIMAL">DECIMAL</option>
                    <option value="BOOLEAN">BOOLEAN</option>
                  </optgroup>
                  <optgroup label="Specialized Types">
                    <option value="DATETIME">DATETIME</option>
                    <option value="DATE">DATE</option>
                    <option value="EMAIL">EMAIL</option>
                    <option value="URL">URL</option>
                  </optgroup>
                </select>
              </div>

              <div class="mapping-arrow">→</div>

              <div class="mapping-target">
                <label>Internal Template Variable</label>
                <div class="variable-input-group">
                  <select 
                    v-model="mapping.variableType"
                    @change="updateVariableFromType(mapping)"
                    class="variable-type-select"
                  >
                    <option value="existing">Use Existing Variable</option>
                    <option value="custom">Create Custom Variable</option>
                  </select>
                  
                  <select 
                    v-if="mapping.variableType === 'existing'"
                    v-model="mapping.internalVariable"
                    class="variable-select"
                  >
                    <option value="">Select variable...</option>
                    <optgroup label="User Variables">
                      <option :value="'{{user.name}}'" v-text="'{{user.name}}'"></option>
                      <option :value="'{{user.email}}'" v-text="'{{user.email}}'"></option>
                      <option :value="'{{user.location}}'" v-text="'{{user.location}}'"></option>
                      <option :value="'{{user.device}}'" v-text="'{{user.device}}'"></option>
                    </optgroup>
                    <optgroup label="Ticket Variables">
                      <option :value="'{{ticket.id}}'" v-text="'{{ticket.id}}'"></option>
                      <option :value="'{{ticket.status}}'" v-text="'{{ticket.status}}'"></option>
                      <option :value="'{{ticket.priority}}'" v-text="'{{ticket.priority}}'"></option>
                      <option :value="'{{ticket.category}}'" v-text="'{{ticket.category}}'"></option>
                      <option :value="'{{ticket.subject}}'" v-text="'{{ticket.subject}}'"></option>
                    </optgroup>
                    <optgroup label="Order Variables">
                      <option :value="'{{order.id}}'" v-text="'{{order.id}}'"></option>
                      <option :value="'{{order.status}}'" v-text="'{{order.status}}'"></option>
                      <option :value="'{{order.total}}'" v-text="'{{order.total}}'"></option>
                    </optgroup>
                  </select>
                  
                  <div v-else class="custom-variable-input">
                    <input
                      v-model="mapping.customEntity"
                      type="text"
                      placeholder="Entity (e.g., product)"
                      class="entity-input"
                    />
                    <span class="variable-separator">.</span>
                    <input
                      v-model="mapping.customField"
                      type="text"
                      placeholder="Field (e.g., name)"
                      class="field-input"
                    />
                    <div class="variable-preview">
                      {{ getCustomVariablePreview(mapping) }}
                    </div>
                  </div>
                </div>
              </div>

              <button class="btn-remove" @click="removeMapping(index)" title="Remove Mapping">
                ×
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBackendApi } from '@/composables/useBackendApi'

const route = useRoute()
const router = useRouter()
const { 
  getMetadataConnection,
  getMetadataMappings,
  saveMetadataMappings,
  testMetadataConnection
} = useBackendApi()

interface Mapping {
  id?: number
  externalFieldPath: string
  internalVariable: string
  variableType: 'existing' | 'custom'
  customEntity?: string
  customField?: string
  dataType?: string
  isActive?: boolean
  connection?: any
}

const connectionId = ref<number | null>(null)
const connectionName = ref('')
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const availableFields = ref<string[]>([])
const fieldTypes = ref<Record<string, string>>({})
const mappings = ref<Mapping[]>([])

const loadConnection = async () => {
  const id = route.params.id as string
  if (!id) {
    router.push('/metadata-configuration')
    return
  }
  
  connectionId.value = parseInt(id)
  loading.value = true
  
  try {
    const connResponse = await getMetadataConnection(connectionId.value)
    connectionName.value = connResponse.data.name
    
    const mappingsResponse = await getMetadataMappings(connectionId.value)
    const existingMappings = mappingsResponse.data || []
    
    console.log('Loaded mappings:', existingMappings)
    
    // Convert existing mappings to our format
    mappings.value = existingMappings.map((m: any) => {
      try {
        const isCustom = m.internalVariable && !isStandardVariable(m.internalVariable)
        return {
          id: m.id,
          externalFieldPath: m.externalFieldPath || '',
          internalVariable: m.internalVariable || '',
          variableType: isCustom ? 'custom' : 'existing',
          customEntity: isCustom ? extractEntityFromVariable(m.internalVariable) : '',
          customField: isCustom ? extractFieldFromVariable(m.internalVariable) : '',
          dataType: m.dataType || '',
          isActive: m.isActive !== false,
          connection: m.connection
        }
      } catch (e) {
        console.error('Error processing mapping:', m, e)
        // Return a safe default
        return {
          id: m.id,
          externalFieldPath: m.externalFieldPath || '',
          internalVariable: m.internalVariable || '',
          variableType: 'existing' as const,
          isActive: m.isActive !== false
        }
      }
    })
    
    // If no mappings, add one empty
    if (mappings.value.length === 0) {
      mappings.value.push({
        externalFieldPath: '',
        internalVariable: '',
        variableType: 'existing',
        dataType: '',
        isActive: true
      })
    }
    
    // Test connection to get available fields (don't fail if this errors)
    try {
      await testAndRefreshFields()
    } catch (testError) {
      console.warn('Failed to test connection for fields, continuing anyway:', testError)
      // Don't block the page if field discovery fails
    }
    
    console.log('Connection loaded successfully:', {
      connectionName: connectionName.value,
      mappingsCount: mappings.value.length,
      availableFieldsCount: availableFields.value.length
    })
  } catch (error: any) {
    console.error('Failed to load connection:', error)
    console.error('Error details:', error.response?.data || error.message)
    alert('Failed to load connection: ' + (error.response?.data?.message || error.message || 'Unknown error'))
    router.push('/metadata-configuration')
  } finally {
    loading.value = false
  }
}

const testAndRefreshFields = async () => {
  if (!connectionId.value) return
  
  testing.value = true
  try {
    const response = await testMetadataConnection(connectionId.value)
    if (response.data.availableFields) {
      availableFields.value = response.data.availableFields
      // Store field types if available
      if (response.data.fieldTypes) {
        fieldTypes.value = response.data.fieldTypes
      }
    } else if (response.data.success && response.data.response) {
      // Try to extract fields from the response
      try {
        const jsonResponse = typeof response.data.response === 'string' 
          ? JSON.parse(response.data.response) 
          : response.data.response
        const fields: string[] = []
        extractFieldPaths(jsonResponse, '', fields)
        availableFields.value = fields
      } catch (e) {
        console.warn('Could not extract fields from response')
        availableFields.value = []
      }
    } else {
      availableFields.value = []
    }
  } catch (error) {
    console.error('Failed to test connection:', error)
    availableFields.value = []
  } finally {
    testing.value = false
  }
}

const extractFieldPaths = (obj: any, prefix: string, paths: string[]) => {
  if (obj === null || obj === undefined) return
  if (typeof obj !== 'object') return
  
  if (Array.isArray(obj)) {
    if (obj.length > 0) {
      extractFieldPaths(obj[0], prefix, paths)
    }
  } else {
    Object.keys(obj).forEach(key => {
      const newPath = prefix ? `${prefix}.${key}` : key
      if (typeof obj[key] === 'object' && obj[key] !== null && !Array.isArray(obj[key])) {
        extractFieldPaths(obj[key], newPath, paths)
      } else {
        paths.push(newPath)
      }
    })
  }
}

const isStandardVariable = (variable: string): boolean => {
  const standardVars = [
    '{{user.name}}', '{{user.email}}', '{{user.location}}', '{{user.device}}',
    '{{ticket.id}}', '{{ticket.status}}', '{{ticket.priority}}', '{{ticket.category}}', '{{ticket.subject}}',
    '{{order.id}}', '{{order.status}}', '{{order.total}}'
  ]
  return standardVars.includes(variable)
}

const extractEntityFromVariable = (variable: string): string => {
  if (!variable || !variable.startsWith('{{') || !variable.endsWith('}}')) return ''
  const content = variable.slice(2, -2)
  const parts = content.split('.')
  return parts[0] || ''
}

const extractFieldFromVariable = (variable: string): string => {
  if (!variable || !variable.startsWith('{{') || !variable.endsWith('}}')) return ''
  const content = variable.slice(2, -2)
  const parts = content.split('.')
  return parts[1] || ''
}

const updateVariableFromType = (mapping: Mapping) => {
  if (mapping.variableType === 'custom') {
    mapping.internalVariable = ''
  } else {
    mapping.customEntity = ''
    mapping.customField = ''
  }
}

const updateDataTypeFromField = (mapping: Mapping) => {
  if (mapping.externalFieldPath && fieldTypes.value[mapping.externalFieldPath]) {
    // Auto-set data type if not already set
    if (!mapping.dataType) {
      mapping.dataType = fieldTypes.value[mapping.externalFieldPath]
    }
  }
}

const getTypeClass = (type: string): string => {
  const typeLower = type.toLowerCase()
  if (['integer', 'decimal', 'float', 'double'].includes(typeLower)) {
    return 'type-numeric'
  } else if (['boolean'].includes(typeLower)) {
    return 'type-boolean'
  } else if (['datetime', 'date'].includes(typeLower)) {
    return 'type-datetime'
  } else if (['email', 'url'].includes(typeLower)) {
    return 'type-special'
  }
  return 'type-string'
}

const selectField = (field: string) => {
  // Check if already mapped
  const existing = mappings.value.find(m => m.externalFieldPath === field)
  if (existing) {
    // Scroll to existing mapping
    return
  }
  
  // Add new mapping with this field
  const newMapping: Mapping = {
    externalFieldPath: field,
    internalVariable: '',
    variableType: 'existing',
    isActive: true
  }
  mappings.value.push(newMapping)
}

const isFieldMapped = (field: string): boolean => {
  return mappings.value.some(m => m.externalFieldPath === field && m.internalVariable)
}

const getMappingForField = (field: string): Mapping | undefined => {
  return mappings.value.find(m => m.externalFieldPath === field)
}

const addNewMapping = () => {
  mappings.value.push({
    externalFieldPath: '',
    internalVariable: '',
    variableType: 'existing',
    isActive: true
  })
}

const removeMapping = (index: number) => {
  mappings.value.splice(index, 1)
}

const saveAllMappings = async () => {
  if (!connectionId.value) return
  
  saving.value = true
  try {
    // Build final mappings
    const finalMappings = mappings.value
      .filter(m => m.externalFieldPath.trim() && (m.internalVariable.trim() || (m.customEntity && m.customField)))
      .map(m => {
        let internalVar = m.internalVariable
        if (m.variableType === 'custom' && m.customEntity && m.customField) {
          internalVar = `{{${m.customEntity}.${m.customField}}}`
        }
        
        return {
          id: m.id,
          externalFieldPath: m.externalFieldPath.trim(),
          internalVariable: internalVar,
          dataType: m.dataType || '',
          isActive: m.isActive !== false
        }
      })
    
    await saveMetadataMappings(connectionId.value, finalMappings)
    alert('Mappings saved successfully!')
    router.push('/metadata-configuration')
  } catch (error) {
    console.error('Failed to save mappings:', error)
    alert('Failed to save mappings. Please try again.')
  } finally {
    saving.value = false
  }
}

const getCustomVariablePreview = (mapping: Mapping): string => {
  const entity = mapping.customEntity || 'entity'
  const field = mapping.customField || 'field'
  return `{{${entity}.${field}}}`
}

const goBack = () => {
  router.push('/metadata-configuration')
}

onMounted(() => {
  loadConnection()
})
</script>

<style scoped>
.mapping-view-container {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
}

.mapping-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}

.header-content {
  flex: 1;
}

.btn-back {
  padding: 0.5rem 1rem;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #e0e0e0;
}

.mapping-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #333;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 1rem;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 4rem;
  color: #666;
}

.error-state {
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 8px;
  margin: 2rem;
}

.error-state p {
  margin-bottom: 1rem;
  color: #856404;
}

.mapping-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

.fields-section,
.mappings-section {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.empty-fields,
.empty-mappings {
  padding: 2rem;
  text-align: center;
  color: #999;
  background: #f8f9fa;
  border-radius: 4px;
}

.fields-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  max-height: 600px;
  overflow-y: auto;
  padding: 0.5rem;
}

.field-card {
  padding: 1rem;
  background: #f8f9fa;
  border: 2px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.field-card:hover {
  background: #e3f2fd;
  border-color: #1976d2;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.field-card.mapped {
  background: #e8f5e9;
  border-color: #4caf50;
}

.field-name {
  font-family: monospace;
  font-size: 0.9rem;
  color: #333;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.field-type {
  margin-top: 0.25rem;
}

.type-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.type-badge-small {
  display: inline-block;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.type-string {
  background: #e3f2fd;
  color: #1976d2;
}

.type-numeric {
  background: #fff3e0;
  color: #f57c00;
}

.type-boolean {
  background: #f3e5f5;
  color: #7b1fa2;
}

.type-datetime {
  background: #e8f5e9;
  color: #388e3c;
}

.type-special {
  background: #fce4ec;
  color: #c2185b;
}

.detected-type {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: #666;
}

.mapping-transform {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mapping-transform label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #666;
}

.data-type-select {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.field-mapped-to {
  font-size: 0.85rem;
  color: #4caf50;
  font-weight: 600;
}

.mappings-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-height: 600px;
  overflow-y: auto;
  padding: 0.5rem;
}

.mapping-card {
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1rem;
}

.mapping-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr auto 2fr auto;
  gap: 1rem;
  align-items: end;
}

.mapping-source,
.mapping-target {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mapping-source label,
.mapping-target label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #666;
}

.mapping-source input,
.mapping-target select {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-family: monospace;
}

.mapping-arrow {
  font-size: 1.5rem;
  color: #999;
  align-self: center;
  padding: 0 0.5rem;
}

.variable-input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.variable-type-select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.variable-select {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.custom-variable-input {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.entity-input,
.field-input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.variable-separator {
  font-size: 1.2rem;
  color: #999;
  font-weight: bold;
}

.variable-preview {
  padding: 0.5rem;
  background: #e3f2fd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9rem;
  color: #1976d2;
  font-weight: 600;
  min-width: 150px;
  text-align: center;
}

.btn-remove {
  width: 36px;
  height: 36px;
  border: none;
  background: #dc3545;
  color: white;
  border-radius: 4px;
  font-size: 1.5rem;
  cursor: pointer;
  transition: background 0.2s;
  align-self: end;
}

.btn-remove:hover {
  background: #c82333;
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

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  background: #f0f0f0;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.btn-small:hover:not(:disabled) {
  background: #e0e0e0;
}

.btn-small:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 1200px) {
  .mapping-content {
    grid-template-columns: 1fr;
  }
  
  .mapping-row {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .mapping-arrow {
    transform: rotate(90deg);
  }
}
</style>

