<template>
  <div class="page-container">
    <div class="inbound-metadata-view">
      <div class="view-header">
        <div class="header-content">
          <h1>Inbound Metadata</h1>
          <p class="subtitle">Connect external APIs and map client data to internal template variables</p>
        </div>
        <button class="btn btn-primary" @click="showConnectionDialog = true">
          + Add API Connection
        </button>
      </div>

    <!-- Connections List -->
    <div v-if="connections.length === 0 && !loading" class="empty-state">
      <p>No API connections configured. Click "Add API Connection" to get started.</p>
    </div>

    <div v-else class="connections-list">
      <div 
        v-for="connection in connections" 
        :key="connection.id"
        class="connection-card"
        :class="{ active: connection.isActive, inactive: !connection.isActive }"
      >
        <div class="connection-header">
          <div class="connection-info">
            <h3>{{ connection.name }}</h3>
            <span class="connection-status" :class="{ active: connection.isActive }">
              {{ connection.isActive ? 'Active' : 'Inactive' }}
            </span>
          </div>
          <div class="connection-actions">
            <button class="btn-icon" @click="editConnection(connection)" title="Edit">
              ✏️
            </button>
            <button class="btn-icon" @click="deleteConnection(connection.id)" title="Delete">
              🗑️
            </button>
          </div>
        </div>
        
        <div class="connection-details">
          <div class="detail-item">
            <strong>Endpoint:</strong> <code>{{ connection.apiEndpoint }}</code>
          </div>
          <div class="detail-item">
            <strong>Method:</strong> {{ connection.requestMethod }}
          </div>
          <div class="detail-item">
            <strong>Auth Type:</strong> {{ connection.authType }}
          </div>
        </div>

        <!-- Mappings Section -->
        <div class="mappings-section">
          <div class="mappings-header">
            <h4>Field Mappings</h4>
            <button class="btn-small" @click="openMappingEditor(connection.id!)">
              {{ getMappingsCount(connection.id!) > 0 ? 'Edit Mappings' : 'Add Mappings' }}
            </button>
          </div>
          
          <div v-if="getMappingsCount(connection.id) > 0" class="mappings-list">
            <div 
              v-for="mapping in getMappingsForConnection(connection.id)" 
              :key="mapping.id"
              class="mapping-item"
            >
              <span class="external-field">{{ mapping.externalFieldPath }}</span>
              <span class="mapping-arrow">→</span>
              <span class="internal-variable">{{ mapping.internalVariable }}</span>
            </div>
          </div>
          <div v-else class="no-mappings">
            <p>No field mappings configured. Click "Add Mappings" to map external fields to template variables.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Connection Dialog (same as original) -->
    <Teleport to="body" v-if="showConnectionDialog">
      <div class="modal-overlay" @click="showConnectionDialog = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h2>{{ editingConnection ? 'Edit Connection' : 'Add API Connection' }}</h2>
            <button class="btn-close" @click="showConnectionDialog = false">×</button>
          </div>
          
          <div class="modal-body">
            <div class="form-group">
              <label>Connection Name *</label>
              <input v-model="connectionForm.name" type="text" placeholder="e.g., Client CRM API" />
            </div>
            
            <div class="form-group">
              <label>API Endpoint *</label>
              <input v-model="connectionForm.apiEndpoint" type="url" placeholder="https://api.example.com/data" />
            </div>
            
            <div class="form-group">
              <label>Request Method *</label>
              <select v-model="connectionForm.requestMethod">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="PATCH">PATCH</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>Authentication Type</label>
              <select v-model="connectionForm.authType" @change="updateAuthFields">
                <option value="NONE">None</option>
                <option value="API_KEY">API Key</option>
                <option value="BEARER_TOKEN">Bearer Token</option>
                <option value="BASIC_AUTH">Basic Auth</option>
              </select>
            </div>
            
            <!-- Auth Fields -->
            <div v-if="connectionForm.authType === 'API_KEY'" class="form-group">
              <label>API Key Header</label>
              <input v-model="authFields.apiKeyHeader" type="text" placeholder="X-API-Key" />
            </div>
            <div v-if="connectionForm.authType === 'API_KEY'" class="form-group">
              <label>API Key</label>
              <input v-model="authFields.apiKey" type="password" placeholder="Your API key" />
            </div>
            <div v-if="connectionForm.authType === 'BEARER_TOKEN'" class="form-group">
              <label>Bearer Token</label>
              <input v-model="authFields.token" type="password" placeholder="Your bearer token" />
            </div>
            <div v-if="connectionForm.authType === 'BASIC_AUTH'" class="form-group">
              <label>Username</label>
              <input v-model="authFields.username" type="text" placeholder="Username" />
            </div>
            <div v-if="connectionForm.authType === 'BASIC_AUTH'" class="form-group">
              <label>Password</label>
              <input v-model="authFields.password" type="password" placeholder="Password" />
            </div>
            
            <div class="form-group checkbox-group">
              <div class="checkbox-label">
                <input v-model="connectionForm.isActive" type="checkbox" id="isActive" />
                <label for="isActive">Active</label>
              </div>
            </div>
          </div>
          
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showConnectionDialog = false">Cancel</button>
            <button class="btn btn-primary" @click="saveConnection">Save</button>
          </div>
        </div>
      </div>
    </Teleport>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBackendApi } from '@/composables/useBackendApi'

const router = useRouter()
const { 
  getAllMetadataConnections, 
  createMetadataConnection, 
  updateMetadataConnection, 
  deleteMetadataConnection,
  getMetadataMappings
} = useBackendApi()

interface MetadataConnection {
  id?: number
  name: string
  apiEndpoint: string
  authType: string
  requestMethod: string
  isActive: boolean
  authCredentials?: string
  requestHeaders?: string
  requestBody?: string
}

interface MetadataFieldMapping {
  id?: number
  connectionId?: number
  externalFieldPath: string
  internalVariable: string
  dataType?: string
  description?: string
  isActive: boolean
}

const connections = ref<MetadataConnection[]>([])
const mappings = ref<Map<number, MetadataFieldMapping[]>>(new Map())
const loading = ref(true)
const saving = ref(false)

const showConnectionDialog = ref(false)
const editingConnection = ref<MetadataConnection | null>(null)

const connectionForm = ref<MetadataConnection>({
  name: '',
  apiEndpoint: '',
  authType: 'NONE',
  requestMethod: 'GET',
  isActive: true
})

const authFields = ref({
  apiKeyHeader: 'X-API-Key',
  apiKey: '',
  token: '',
  username: '',
  password: ''
})

const loadConnections = async () => {
  loading.value = true
  try {
    const response = await getAllMetadataConnections()
    connections.value = response.data || []
    
    for (const conn of connections.value) {
      if (conn.id) {
        await loadMappings(conn.id)
      }
    }
  } catch (error) {
    console.error('Failed to load connections:', error)
  } finally {
    loading.value = false
  }
}

const loadMappings = async (connectionId: number) => {
  try {
    const response = await getMetadataMappings(connectionId)
    mappings.value.set(connectionId, response.data || [])
  } catch (error) {
    console.error('Failed to load mappings:', error)
  }
}

const getMappingsForConnection = (connectionId: number): MetadataFieldMapping[] => {
  return mappings.value.get(connectionId) || []
}

const getMappingsCount = (connectionId: number): number => {
  return getMappingsForConnection(connectionId).length
}

const updateAuthFields = () => {
  if (connectionForm.value.authType === 'API_KEY') {
    connectionForm.value.authCredentials = JSON.stringify({
      header: authFields.value.apiKeyHeader,
      apiKey: authFields.value.apiKey
    })
  } else if (connectionForm.value.authType === 'BEARER_TOKEN') {
    connectionForm.value.authCredentials = JSON.stringify({
      token: authFields.value.token
    })
  } else if (connectionForm.value.authType === 'BASIC_AUTH') {
    connectionForm.value.authCredentials = JSON.stringify({
      username: authFields.value.username,
      password: authFields.value.password
    })
  } else {
    connectionForm.value.authCredentials = undefined
  }
}

const saveConnection = async () => {
  updateAuthFields()
  saving.value = true
  try {
    if (editingConnection.value?.id) {
      await updateMetadataConnection(editingConnection.value.id, connectionForm.value)
    } else {
      await createMetadataConnection(connectionForm.value)
    }
    await loadConnections()
    showConnectionDialog.value = false
    resetForm()
  } catch (error) {
    console.error('Failed to save connection:', error)
    alert('Failed to save connection. Please try again.')
  } finally {
    saving.value = false
  }
}

const editConnection = (connection: MetadataConnection) => {
  editingConnection.value = connection
  connectionForm.value = { ...connection }
  
  // Parse auth credentials
  if (connection.authCredentials) {
    try {
      const auth = JSON.parse(connection.authCredentials)
      if (connection.authType === 'API_KEY') {
        authFields.value.apiKeyHeader = auth.header || 'X-API-Key'
        authFields.value.apiKey = auth.apiKey || ''
      } else if (connection.authType === 'BEARER_TOKEN') {
        authFields.value.token = auth.token || ''
      } else if (connection.authType === 'BASIC_AUTH') {
        authFields.value.username = auth.username || ''
        authFields.value.password = auth.password || ''
      }
    } catch (e) {
      console.error('Failed to parse auth credentials:', e)
    }
  }
  
  showConnectionDialog.value = true
}

const deleteConnection = async (id: number) => {
  if (!confirm('Are you sure you want to delete this connection?')) return
  
  try {
    await deleteMetadataConnection(id)
    await loadConnections()
  } catch (error) {
    console.error('Failed to delete connection:', error)
    alert('Failed to delete connection. Please try again.')
  }
}

const openMappingEditor = (connectionId: number) => {
  router.push(`/metadata-configuration/mapping/${connectionId}`)
}

const resetForm = () => {
  editingConnection.value = null
  connectionForm.value = {
    name: '',
    apiEndpoint: '',
    authType: 'NONE',
    requestMethod: 'GET',
    isActive: true
  }
  authFields.value = {
    apiKeyHeader: 'X-API-Key',
    apiKey: '',
    token: '',
    username: '',
    password: ''
  }
}

onMounted(() => {
  loadConnections()
})
</script>

<style scoped>
.page-container {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.inbound-metadata-view {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}

.header-content {
  flex: 1;
}

.view-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #333;
}

.subtitle {
  color: #666;
  margin: 0 0 1rem 0;
}

.empty-state {
  padding: 3rem;
  text-align: center;
  color: #999;
  background: #f8f9fa;
  border-radius: 8px;
}

.connections-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.connection-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  transition: all 0.2s;
}

.connection-card.active {
  border-color: #4caf50;
}

.connection-card.inactive {
  border-color: #ff9800;
  opacity: 0.8;
}

.connection-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f0f0f0;
}

.connection-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  color: #333;
}

.connection-status {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
}

.connection-status.active {
  background: #d4edda;
  color: #155724;
}

.connection-actions {
  display: flex;
  gap: 0.5rem;
}

.connection-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.detail-item {
  display: flex;
  gap: 0.5rem;
}

.detail-item strong {
  min-width: 100px;
  color: #666;
}

.detail-item code {
  background: #f8f9fa;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  color: #d63384;
}

.mappings-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.mappings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.mappings-header h4 {
  margin: 0;
  font-size: 1rem;
  color: #333;
}

.mappings-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mapping-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 0.9rem;
}

.external-field {
  font-family: monospace;
  color: #666;
}

.mapping-arrow {
  color: #999;
}

.internal-variable {
  font-family: monospace;
  color: #d63384;
  font-weight: 600;
}

.no-mappings {
  padding: 1rem;
  text-align: center;
  color: #999;
  font-size: 0.9rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.modal-overlay {
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

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.checkbox-group {
  margin-top: 1rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.btn-primary {
  background: #dc3545;
  color: white;
}

.btn-primary:hover {
  background: #c82333;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn-small {
  padding: 0.25rem 0.75rem;
  font-size: 0.85rem;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.25rem;
}
</style>

