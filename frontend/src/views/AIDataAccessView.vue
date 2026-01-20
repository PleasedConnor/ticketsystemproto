<template>
  <div class="ai-data-access-container">
    <div class="content">
      <header class="page-header">
        <h1>AI Data Access</h1>
        <p class="subtitle">Manage AI data access and integration methods</p>
      </header>

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
          </button>
        </div>

        <!-- Tab Content -->
        <div class="tab-content">
          <!-- 3rd Party Integrations Tab -->
          <div v-show="activeTab === 'third-party'" class="tab-panel">
            <div class="section-header">
              <h2>3rd Party Integrations</h2>
            </div>
            <p class="section-description">
              Connect third-party services and control which data the AI can access. Click "Connect" to authenticate and link your accounts.
            </p>
            
            <!-- Available Integrations Grid -->
            <div v-if="loadingAvailableIntegrations" class="loading-container">
              <p>Loading available integrations...</p>
            </div>
            
            <div v-else class="available-integrations">
              <div v-for="category in integrationCategories" :key="category.name" class="integration-category">
                <h3 class="category-header">{{ category.name }}</h3>
                <div class="integrations-grid">
                  <div 
                    v-for="integration in category.integrations" 
                    :key="integration.type"
                    class="integration-card-available"
                    :class="{ connected: isIntegrationConnected(integration.type) }"
                  >
                    <div class="integration-icon">{{ integration.icon }}</div>
                    <div class="integration-details">
                      <h4>{{ integration.name }}</h4>
                      <p>{{ integration.description }}</p>
                    </div>
                    <div class="integration-action">
                      <button 
                        v-if="!isIntegrationConnected(integration.type)"
                        class="btn btn-connect" 
                        @click="connectIntegration(integration.type)"
                        :disabled="connectingIntegration === integration.type"
                      >
                        {{ connectingIntegration === integration.type ? 'Connecting...' : 'Connect' }}
                      </button>
                      <div v-else class="connected-badge">
                        <span>✓ Connected</span>
                        <button class="btn-link" @click="viewConnectedIntegration(integration.type)">Manage</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- Connected Integrations List -->
            <div v-if="integrations.length > 0" class="connected-integrations-section">
              <h3 class="section-subheader">Connected Integrations</h3>
              <div class="integrations-container">
              <!-- Atlassian Group -->
              <div v-if="getIntegrationsByProvider('ATLASSIAN').length > 0" class="integration-group">
                <h3 class="group-header">Atlassian</h3>
                <div class="integrations-list">
                  <div v-for="integration in getIntegrationsByProvider('ATLASSIAN')" :key="integration.id" class="integration-card">
                    <div class="integration-header">
                      <div class="integration-info">
                        <h4>{{ integration.name }}</h4>
                        <span class="integration-type">{{ formatIntegrationType(integration.integrationType) }}</span>
                      </div>
                      <div class="integration-actions">
                        <label class="toggle-switch">
                          <input type="checkbox" v-model="integration.aiAccessible" @change="updateIntegrationAIAccess(integration)" />
                          <span class="toggle-slider"></span>
                        </label>
                        <span class="access-status" :class="{ allowed: integration.aiAccessible, denied: !integration.aiAccessible }">
                          {{ integration.aiAccessible ? 'AI Allowed' : 'AI Denied' }}
                        </span>
                        <button class="btn-icon" @click="viewBlacklist(integration)" title="Manage Blacklist">
                          🚫
                        </button>
                        <button class="btn-icon" @click="editIntegration(integration)" title="Edit">
                          ✏️
                        </button>
                        <button class="btn-icon" @click="deleteIntegration(integration)" title="Delete">
                          🗑️
                        </button>
                      </div>
                    </div>
                    <div class="integration-status">
                      <span :class="['status-badge', integration.isActive ? 'active' : 'inactive']">
                        {{ integration.isActive ? 'Active' : 'Inactive' }}
                      </span>
                      <span v-if="integration.lastSyncAt" class="sync-info">
                        Last synced: {{ formatDate(integration.lastSyncAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Salesforce Group -->
              <div v-if="getIntegrationsByProvider('SALESFORCE').length > 0" class="integration-group">
                <h3 class="group-header">Salesforce</h3>
                <div class="integrations-list">
                  <div v-for="integration in getIntegrationsByProvider('SALESFORCE')" :key="integration.id" class="integration-card">
                    <div class="integration-header">
                      <div class="integration-info">
                        <h4>{{ integration.name }}</h4>
                        <span class="integration-type">{{ formatIntegrationType(integration.integrationType) }}</span>
                      </div>
                      <div class="integration-actions">
                        <label class="toggle-switch">
                          <input type="checkbox" v-model="integration.aiAccessible" @change="updateIntegrationAIAccess(integration)" />
                          <span class="toggle-slider"></span>
                        </label>
                        <span class="access-status" :class="{ allowed: integration.aiAccessible, denied: !integration.aiAccessible }">
                          {{ integration.aiAccessible ? 'AI Allowed' : 'AI Denied' }}
                        </span>
                        <button class="btn-icon" @click="viewBlacklist(integration)" title="Manage Blacklist">
                          🚫
                        </button>
                        <button class="btn-icon" @click="editIntegration(integration)" title="Edit">
                          ✏️
                        </button>
                        <button class="btn-icon" @click="deleteIntegration(integration)" title="Delete">
                          🗑️
                        </button>
                      </div>
                    </div>
                    <div class="integration-status">
                      <span :class="['status-badge', integration.isActive ? 'active' : 'inactive']">
                        {{ integration.isActive ? 'Active' : 'Inactive' }}
                      </span>
                      <span v-if="integration.lastSyncAt" class="sync-info">
                        Last synced: {{ formatDate(integration.lastSyncAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- GitHub/GitLab Group -->
              <div v-if="getIntegrationsByProvider('GIT').length > 0" class="integration-group">
                <h3 class="group-header">GitHub / GitLab</h3>
                <div class="integrations-list">
                  <div v-for="integration in getIntegrationsByProvider('GIT')" :key="integration.id" class="integration-card">
                    <div class="integration-header">
                      <div class="integration-info">
                        <h4>{{ integration.name }}</h4>
                        <span class="integration-type">{{ formatIntegrationType(integration.integrationType) }}</span>
                      </div>
                      <div class="integration-actions">
                        <label class="toggle-switch">
                          <input type="checkbox" v-model="integration.aiAccessible" @change="updateIntegrationAIAccess(integration)" />
                          <span class="toggle-slider"></span>
                        </label>
                        <span class="access-status" :class="{ allowed: integration.aiAccessible, denied: !integration.aiAccessible }">
                          {{ integration.aiAccessible ? 'AI Allowed' : 'AI Denied' }}
                        </span>
                        <button class="btn-icon" @click="viewBlacklist(integration)" title="Manage Blacklist">
                          🚫
                        </button>
                        <button class="btn-icon" @click="editIntegration(integration)" title="Edit">
                          ✏️
                        </button>
                        <button class="btn-icon" @click="deleteIntegration(integration)" title="Delete">
                          🗑️
                        </button>
                      </div>
                    </div>
                    <div class="integration-status">
                      <span :class="['status-badge', integration.isActive ? 'active' : 'inactive']">
                        {{ integration.isActive ? 'Active' : 'Inactive' }}
                      </span>
                      <span v-if="integration.lastSyncAt" class="sync-info">
                        Last synced: {{ formatDate(integration.lastSyncAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Microsoft Group -->
              <div v-if="getIntegrationsByProvider('MICROSOFT').length > 0" class="integration-group">
                <h3 class="group-header">Microsoft</h3>
                <div class="integrations-list">
                  <div v-for="integration in getIntegrationsByProvider('MICROSOFT')" :key="integration.id" class="integration-card">
                    <div class="integration-header">
                      <div class="integration-info">
                        <h4>{{ integration.name }}</h4>
                        <span class="integration-type">{{ formatIntegrationType(integration.integrationType) }}</span>
                      </div>
                      <div class="integration-actions">
                        <label class="toggle-switch">
                          <input type="checkbox" v-model="integration.aiAccessible" @change="updateIntegrationAIAccess(integration)" />
                          <span class="toggle-slider"></span>
                        </label>
                        <span class="access-status" :class="{ allowed: integration.aiAccessible, denied: !integration.aiAccessible }">
                          {{ integration.aiAccessible ? 'AI Allowed' : 'AI Denied' }}
                        </span>
                        <button class="btn-icon" @click="viewBlacklist(integration)" title="Manage Blacklist">
                          🚫
                        </button>
                        <button class="btn-icon" @click="editIntegration(integration)" title="Edit">
                          ✏️
                        </button>
                        <button class="btn-icon" @click="deleteIntegration(integration)" title="Delete">
                          🗑️
                        </button>
                      </div>
                    </div>
                    <div class="integration-status">
                      <span :class="['status-badge', integration.isActive ? 'active' : 'inactive']">
                        {{ integration.isActive ? 'Active' : 'Inactive' }}
                      </span>
                      <span v-if="integration.lastSyncAt" class="sync-info">
                        Last synced: {{ formatDate(integration.lastSyncAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Google Workspace Group -->
              <div v-if="getIntegrationsByProvider('GOOGLE').length > 0" class="integration-group">
                <h3 class="group-header">Google Workspace</h3>
                <div class="integrations-list">
                  <div v-for="integration in getIntegrationsByProvider('GOOGLE')" :key="integration.id" class="integration-card">
                    <div class="integration-header">
                      <div class="integration-info">
                        <h4>{{ integration.name }}</h4>
                        <span class="integration-type">{{ formatIntegrationType(integration.integrationType) }}</span>
                      </div>
                      <div class="integration-actions">
                        <label class="toggle-switch">
                          <input type="checkbox" v-model="integration.aiAccessible" @change="updateIntegrationAIAccess(integration)" />
                          <span class="toggle-slider"></span>
                        </label>
                        <span class="access-status" :class="{ allowed: integration.aiAccessible, denied: !integration.aiAccessible }">
                          {{ integration.aiAccessible ? 'AI Allowed' : 'AI Denied' }}
                        </span>
                        <button class="btn-icon" @click="viewBlacklist(integration)" title="Manage Blacklist">
                          🚫
                        </button>
                        <button class="btn-icon" @click="editIntegration(integration)" title="Edit">
                          ✏️
                        </button>
                        <button class="btn-icon" @click="deleteIntegration(integration)" title="Delete">
                          🗑️
                        </button>
                      </div>
                    </div>
                    <div class="integration-status">
                      <span :class="['status-badge', integration.isActive ? 'active' : 'inactive']">
                        {{ integration.isActive ? 'Active' : 'Inactive' }}
                      </span>
                      <span v-if="integration.lastSyncAt" class="sync-info">
                        Last synced: {{ formatDate(integration.lastSyncAt) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              </div>
            </div>
          </div>

          <!-- Direct Integrations Tab -->
          <div v-show="activeTab === 'direct'" class="tab-panel">
            <div class="section-header">
              <h2>Direct Integrations</h2>
              <button 
                class="btn btn-primary" 
                @click="saveAIAccessSettings" 
                :disabled="saving"
              >
                {{ saving ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
            <p class="section-description">
              Control which data points from Metadata Configuration the AI can access. 
              Toggle access for individual variables to provide granular control.
            </p>
            
            <div v-if="loading" class="loading-container">
              <p>Loading data points...</p>
            </div>
            
            <div v-else-if="dataPoints.length === 0" class="empty-state">
              <p>No data points found. Create mappings in Metadata Configuration first.</p>
            </div>
            
            <div v-else class="data-points-list">
              <div class="list-header">
                <div class="header-item variable-col">Variable</div>
                <div class="header-item path-col">External Path</div>
                <div class="header-item type-col">Data Type</div>
                <div class="header-item connection-col">Connection</div>
                <div class="header-item value-col">Test Value</div>
                <div class="header-item access-col">AI Access</div>
              </div>
              
              <div 
                v-for="point in dataPoints" 
                :key="point.id" 
                class="data-point-item"
              >
                <div class="item-cell variable-col">
                  <code class="variable-code">{{ point.internalVariable }}</code>
                </div>
                <div class="item-cell path-col">
                  <span class="path-text">{{ point.externalFieldPath }}</span>
                </div>
                <div class="item-cell type-col">
                  <span class="type-badge" :class="getTypeClass(point.dataType)">
                    {{ point.dataType || 'STRING' }}
                  </span>
                </div>
                <div class="item-cell connection-col">
                  <span class="connection-name">{{ point.connectionName }}</span>
                </div>
                <div class="item-cell value-col">
                  <button 
                    v-if="point.connectionName === 'Demo Client API'"
                    class="btn-test" 
                    @click="testValue(point)"
                    :disabled="testingValue === point.id"
                  >
                    {{ testingValue === point.id ? 'Testing...' : 'Test' }}
                  </button>
                  <span v-if="point.testValue" class="test-value" :title="point.testValue">
                    {{ point.testValue }}
                  </span>
                  <span v-else-if="point.connectionName !== 'Demo Client API'" class="test-value-placeholder">
                    N/A
                  </span>
                </div>
                <div class="item-cell access-col">
                  <label class="toggle-switch">
                    <input 
                      type="checkbox" 
                      v-model="point.aiAccessible"
                      @change="onAccessChange(point)"
                    />
                    <span class="toggle-slider"></span>
                  </label>
                  <span class="access-status" :class="{ allowed: point.aiAccessible, denied: !point.aiAccessible }">
                    {{ point.aiAccessible ? 'Allowed' : 'Denied' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Integration Dialog -->
    <div v-if="showAddDialog || showEditDialog" class="dialog-overlay" @click.self="showAddDialog = false; showEditDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ currentIntegration?.id ? 'Edit Integration' : 'Add Integration' }}</h3>
          <button class="dialog-close" @click="showAddDialog = false; showEditDialog = false">×</button>
        </div>
        <div class="dialog-form" v-if="currentIntegration">
          <div class="form-group">
            <label>Integration Name</label>
            <input v-model="currentIntegration.name" type="text" placeholder="e.g., Production Confluence" />
          </div>
          <div class="form-group">
            <label>Integration Type</label>
            <select v-model="currentIntegration.integrationType">
              <optgroup label="Atlassian">
                <option value="JIRA">Jira</option>
                <option value="CONFLUENCE">Confluence</option>
                <option value="SLACK">Slack</option>
              </optgroup>
              <optgroup label="Salesforce">
                <option value="SALESFORCE">Salesforce</option>
                <option value="SERVICE_CLOUD">Service Cloud</option>
              </optgroup>
              <optgroup label="Git">
                <option value="GITHUB">GitHub</option>
                <option value="GITLAB">GitLab</option>
              </optgroup>
              <optgroup label="Microsoft">
                <option value="TEAMS">Teams</option>
                <option value="ONEDRIVE">OneDrive</option>
                <option value="WORD">Word</option>
                <option value="EXCEL">Excel</option>
                <option value="OUTLOOK">Outlook</option>
              </optgroup>
              <optgroup label="Google Workspace">
                <option value="GOOGLE_DRIVE">Google Drive</option>
                <option value="GOOGLE_DOCS">Google Docs</option>
                <option value="GOOGLE_SHEETS">Google Sheets</option>
                <option value="GMAIL">Gmail</option>
              </optgroup>
            </select>
          </div>
          <div class="form-group">
            <label>API Endpoint</label>
            <input v-model="currentIntegration.apiEndpoint" type="text" placeholder="https://your-instance.atlassian.net" />
          </div>
          <div class="form-group">
            <label>Authentication Type</label>
            <select v-model="currentIntegration.authType">
              <option value="OAUTH2">OAuth 2.0</option>
              <option value="BEARER">Bearer Token</option>
              <option value="BASIC">Basic Auth</option>
              <option value="API_KEY">API Key</option>
            </select>
          </div>
          <div class="form-group" v-if="currentIntegration.authType !== 'OAUTH2'">
            <label>Access Token / API Key</label>
            <input v-model="currentIntegration.accessToken" type="password" placeholder="Enter access token or API key" />
          </div>
          <div class="form-group" v-if="currentIntegration.authType === 'OAUTH2'">
            <label>Client ID</label>
            <input v-model="currentIntegration.clientId" type="text" placeholder="Enter OAuth Client ID" required />
          </div>
          <div class="form-group" v-if="currentIntegration.authType === 'OAUTH2'">
            <label>Client Secret</label>
            <input v-model="currentIntegration.clientSecret" type="password" placeholder="Enter OAuth Client Secret" required />
          </div>
          <div class="form-group">
            <label>Workspace ID (if applicable)</label>
            <input v-model="currentIntegration.workspaceId" type="text" placeholder="e.g., Slack workspace ID" />
          </div>
          <div class="form-group">
            <label>
              <input type="checkbox" v-model="currentIntegration.isActive" />
              Active
            </label>
          </div>
          <div class="form-group">
            <label>
              <input type="checkbox" v-model="currentIntegration.aiAccessible" />
              AI Can Access Data
            </label>
          </div>
          <div class="form-group">
            <label>
              <input type="checkbox" v-model="currentIntegration.syncEnabled" />
              Enable Auto Sync
            </label>
          </div>
          <div class="dialog-actions">
            <button class="btn" @click="showAddDialog = false; showEditDialog = false">Cancel</button>
            <button 
              v-if="currentIntegration.authType === 'OAUTH2' && !currentIntegration.id" 
              class="btn btn-primary" 
              @click="startOAuthFlow"
              :disabled="!currentIntegration.name || !currentIntegration.clientId || !currentIntegration.clientSecret"
            >
              Connect via OAuth
            </button>
            <button 
              v-else 
              class="btn btn-primary" 
              @click="saveIntegration"
            >
              {{ currentIntegration.id ? 'Update' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- OAuth Configuration Dialog -->
    <div v-if="showOAuthConfigDialog" class="dialog-overlay" @click.self="showOAuthConfigDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>Configure OAuth - {{ oauthConfig.integrationName }}</h3>
          <button class="dialog-close" @click="showOAuthConfigDialog = false">×</button>
        </div>
        <div class="dialog-form">
          <p style="margin-bottom: 1rem; color: #666; font-size: 0.9rem;">
            Enter your OAuth credentials to connect this integration. You can get these from the service's developer portal.
          </p>
          <div class="form-group">
            <label>Integration Name</label>
            <input v-model="oauthConfig.integrationName" type="text" placeholder="e.g., Production Slack Workspace" />
          </div>
          <div class="form-group">
            <label>Client ID <span style="color: red;">*</span></label>
            <input v-model="oauthConfig.clientId" type="text" placeholder="Enter OAuth Client ID" required />
            <small style="color: #666; font-size: 0.85rem;">Get this from the service's developer/API settings</small>
          </div>
          <div class="form-group">
            <label>Client Secret <span style="color: red;">*</span></label>
            <input v-model="oauthConfig.clientSecret" type="password" placeholder="Enter OAuth Client Secret" required />
            <small style="color: #666; font-size: 0.85rem;">Keep this secure - it will be stored encrypted</small>
          </div>
          <div class="form-group" v-if="needsWorkspaceId(oauthConfig.integrationType)">
            <label>Workspace/Site ID (if applicable)</label>
            <input v-model="oauthConfig.workspaceId" type="text" placeholder="e.g., your-site.atlassian.net" />
            <small style="color: #666; font-size: 0.85rem;">Required for Atlassian and some other services</small>
          </div>
          <div class="form-group">
            <label>API Endpoint</label>
            <input v-model="oauthConfig.apiEndpoint" type="text" placeholder="Base API URL" />
            <small style="color: #666; font-size: 0.85rem;">Usually auto-detected, but you can override if needed</small>
          </div>
          <div class="dialog-actions">
            <button class="btn" @click="showOAuthConfigDialog = false">Cancel</button>
            <button 
              class="btn btn-primary" 
              @click="startOAuthFlow"
              :disabled="!oauthConfig.clientId || !oauthConfig.clientSecret"
            >
              Connect via OAuth
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Blacklist Management Dialog -->
    <div v-if="showBlacklistDialog && currentIntegration" class="dialog-overlay" @click.self="showBlacklistDialog = false">
      <div class="dialog blacklist-dialog">
        <div class="dialog-header">
          <h3>Manage Blacklist - {{ currentIntegration.name }}</h3>
          <button class="dialog-close" @click="showBlacklistDialog = false">×</button>
        </div>
        <div class="blacklist-content">
          <div class="blacklist-section">
            <h4>Available Items</h4>
            <div v-if="loadingItems" class="loading-container">
              <p>Loading items...</p>
            </div>
            <div v-else-if="availableItems.length === 0" class="empty-state">
              <p>No items available</p>
            </div>
            <div v-else class="items-list">
              <div v-for="item in availableItems" :key="item.id" class="item-card">
                <div class="item-info">
                  <h5>{{ item.title || item.name }}</h5>
                  <p>{{ item.id }}</p>
                </div>
                <div class="item-actions">
                  <button class="btn btn-primary" @click="addItemToBlacklist(item)">Blacklist</button>
                </div>
              </div>
            </div>
          </div>
          <div class="blacklist-section">
            <h4>Blacklisted Items</h4>
            <div v-if="blacklistedItems.length === 0" class="empty-state">
              <p>No items blacklisted</p>
            </div>
            <div v-else class="items-list">
              <div v-for="item in blacklistedItems" :key="item.id" class="item-card">
                <div class="item-info">
                  <h5>{{ item.itemTitle }}</h5>
                  <p>{{ item.itemId }} - {{ item.itemType }}</p>
                  <p v-if="item.reason" style="font-size: 0.7rem; color: #999;">{{ item.reason }}</p>
                </div>
                <div class="item-actions">
                  <button class="btn" @click="removeItemFromBlacklist(item)">Remove</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn btn-primary" @click="showBlacklistDialog = false">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { 
  getAllMappingsForAIAccess, 
  updateAIAccess, 
  bulkUpdateAIAccess, 
  testMappingValue,
  getAllThirdPartyIntegrations,
  createThirdPartyIntegration,
  updateThirdPartyIntegration,
  deleteThirdPartyIntegration,
  updateThirdPartyAIAccess,
  testThirdPartyConnection,
  fetchThirdPartyItems,
  getBlacklistedItems,
  addToBlacklist,
  bulkAddToBlacklist,
  removeFromBlacklist,
  generateOAuthUrl,
  exchangeOAuthToken,
  getAvailableIntegrations,
  getOAuthAppConfig
} = useBackendApi()

const activeTab = ref('third-party')
const loading = ref(false)
const saving = ref(false)
const testingValue = ref<number | null>(null)

// Third Party Integrations
const loadingIntegrations = ref(false)
const integrations = ref<any[]>([])
const showAddDialog = ref(false)
const showBlacklistDialog = ref(false)
const showEditDialog = ref(false)
const currentIntegration = ref<any>(null)
const availableItems = ref<any[]>([])
const blacklistedItems = ref<any[]>([])
const loadingItems = ref(false)

const tabs = [
  { id: 'third-party', label: '3rd Party Integrations' },
  { id: 'direct', label: 'Direct Integrations' }
]

interface DataPoint {
  id: number
  internalVariable: string
  externalFieldPath: string
  dataType: string
  connectionName: string
  aiAccessible: boolean
  testValue?: string
}

const dataPoints = ref<DataPoint[]>([])

const loadDataPoints = async () => {
  loading.value = true
  try {
    const response = await getAllMappingsForAIAccess()
    const mappings = response.data || []
    
    dataPoints.value = mappings.map((m: any) => ({
      id: m.id,
      internalVariable: m.internalVariable || '',
      externalFieldPath: m.externalFieldPath || '',
      dataType: m.dataType || 'STRING',
      connectionName: m.connectionName || m.connection?.name || 'Unknown',
      aiAccessible: m.aiAccessible !== false, // Default to true if not set
      testValue: undefined
    }))
  } catch (error) {
    console.error('Failed to load data points:', error)
    alert('Failed to load data points. Please try again.')
  } finally {
    loading.value = false
  }
}

const onAccessChange = async (point: DataPoint) => {
  try {
    await updateAIAccess(point.id, point.aiAccessible)
  } catch (error) {
    console.error('Failed to update AI access:', error)
    // Revert the change
    point.aiAccessible = !point.aiAccessible
    alert('Failed to update AI access. Please try again.')
  }
}

const saveAIAccessSettings = async () => {
  saving.value = true
  try {
    const accessMap: Record<number, boolean> = {}
    dataPoints.value.forEach(point => {
      accessMap[point.id] = point.aiAccessible
    })
    
    await bulkUpdateAIAccess(accessMap)
    alert('AI access settings saved successfully!')
  } catch (error) {
    console.error('Failed to save AI access settings:', error)
    alert('Failed to save settings. Please try again.')
  } finally {
    saving.value = false
  }
}

const testValue = async (point: DataPoint) => {
  testingValue.value = point.id
  try {
    const response = await testMappingValue(point.id)
    const result = response.data
    
    if (result.success && result.testValue) {
      point.testValue = result.testValue
    } else {
      point.testValue = result.error || 'No value available'
    }
  } catch (error: any) {
    console.error('Failed to test value:', error)
    point.testValue = 'Error: ' + (error.response?.data?.error || error.message || 'Failed to test')
  } finally {
    testingValue.value = null
  }
}

const getTypeClass = (dataType: string) => {
  const type = (dataType || 'STRING').toUpperCase()
  if (type.includes('INT') || type.includes('NUMBER') || type.includes('DECIMAL')) {
    return 'type-number'
  } else if (type.includes('BOOL')) {
    return 'type-boolean'
  } else if (type.includes('DATE') || type.includes('TIME')) {
    return 'type-date'
  } else if (type.includes('EMAIL')) {
    return 'type-email'
  } else if (type.includes('URL')) {
    return 'type-url'
  }
  return 'type-string'
}

// Third Party Integration methods
const loadIntegrations = async () => {
  loadingIntegrations.value = true
  try {
    const response = await getAllThirdPartyIntegrations()
    integrations.value = response.data || []
  } catch (error) {
    console.error('Failed to load integrations:', error)
    alert('Failed to load integrations. Please try again.')
  } finally {
    loadingIntegrations.value = false
  }
}

const getIntegrationsByProvider = (provider: string) => {
  const providerMap: Record<string, string[]> = {
    ATLASSIAN: ['JIRA', 'CONFLUENCE', 'SLACK'],
    SALESFORCE: ['SALESFORCE', 'SERVICE_CLOUD'],
    GIT: ['GITHUB', 'GITLAB'],
    MICROSOFT: ['TEAMS', 'ONEDRIVE', 'WORD', 'EXCEL', 'OUTLOOK'],
    GOOGLE: ['GOOGLE_DRIVE', 'GOOGLE_DOCS', 'GOOGLE_SHEETS', 'GMAIL']
  }
  
  const types = providerMap[provider] || []
  return integrations.value.filter(i => types.includes(i.integrationType))
}

const formatIntegrationType = (type: string) => {
  return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const formatDate = (dateString: string) => {
  if (!dateString) return 'Never'
  const date = new Date(dateString)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

// Available integrations
const loadingAvailableIntegrations = ref(false)
const availableIntegrations = ref<Record<string, any>>({})
const connectingIntegration = ref<string | null>(null)
const showOAuthConfigDialog = ref(false)
const oauthConfig = ref({
  integrationType: '',
  integrationName: '',
  clientId: '',
  clientSecret: '',
  workspaceId: '',
  apiEndpoint: ''
})

const integrationCategories = computed(() => {
  const categories: Record<string, any[]> = {}
  
  Object.entries(availableIntegrations.value).forEach(([key, integration]: [string, any]) => {
    const category = integration.category || 'Other'
    if (!categories[category]) {
      categories[category] = []
    }
    categories[category].push({
      type: key,
      ...integration
    })
  })
  
  return Object.entries(categories).map(([name, integrations]) => ({
    name,
    integrations
  }))
})

const loadAvailableIntegrations = async () => {
  loadingAvailableIntegrations.value = true
  try {
    const response = await getAvailableIntegrations()
    availableIntegrations.value = response.data || {}
  } catch (error) {
    console.error('Failed to load available integrations:', error)
  } finally {
    loadingAvailableIntegrations.value = false
  }
}

const isIntegrationConnected = (integrationType: string) => {
  return integrations.value.some(i => i.integrationType === integrationType)
}

const viewConnectedIntegration = (integrationType: string) => {
  const integration = integrations.value.find(i => i.integrationType === integrationType)
  if (integration) {
    editIntegration(integration)
  }
}

const connectIntegration = async (integrationType: string) => {
  const integrationInfo = availableIntegrations.value[integrationType]
  
  try {
    // Check if OAuth app credentials are already configured
    const configResponse = await getOAuthAppConfig(integrationType)
    
    if (configResponse.data.configured) {
      // OAuth app is configured - automatically start OAuth flow
      const config = configResponse.data
      connectingIntegration.value = integrationType
      
      const response = await generateOAuthUrl({
        integrationType: integrationType,
        clientId: config.clientId,
        workspaceId: config.workspaceId || undefined
      })
      
      if (response.data.error) {
        alert(response.data.error)
        connectingIntegration.value = null
        return
      }
      
      const { authUrl, state, integrationType: type } = response.data
      
      // Store state for callback verification
      sessionStorage.setItem('oauth_state', state)
      sessionStorage.setItem('oauth_integration_type', type)
      // Store that we're using existing config (no clientSecret needed - backend will retrieve it)
      sessionStorage.setItem('oauth_use_existing_config', 'true')
      
      // Immediately redirect to OAuth provider
      window.location.href = authUrl
    } else {
      // No OAuth app configured - show setup dialog
      oauthConfig.value = {
        integrationType: integrationType,
        integrationName: integrationInfo?.name || integrationType,
        clientId: '',
        clientSecret: '',
        workspaceId: '',
        apiEndpoint: getDefaultApiEndpoint(integrationType)
      }
      showOAuthConfigDialog.value = true
    }
  } catch (error: any) {
    console.error('Failed to check OAuth config:', error)
    // On error, show setup dialog as fallback
    oauthConfig.value = {
      integrationType: integrationType,
      integrationName: integrationInfo?.name || integrationType,
      clientId: '',
      clientSecret: '',
      workspaceId: '',
      apiEndpoint: getDefaultApiEndpoint(integrationType)
    }
    showOAuthConfigDialog.value = true
  }
}

const getDefaultApiEndpoint = (integrationType: string) => {
  const endpoints: Record<string, string> = {
    JIRA: 'https://api.atlassian.com',
    CONFLUENCE: 'https://api.atlassian.com',
    SLACK: 'https://slack.com/api',
    GITHUB: 'https://api.github.com',
    GITLAB: 'https://gitlab.com/api/v4',
    GOOGLE_DRIVE: 'https://www.googleapis.com',
    GOOGLE_DOCS: 'https://www.googleapis.com',
    GOOGLE_SHEETS: 'https://www.googleapis.com',
    GMAIL: 'https://www.googleapis.com',
    TEAMS: 'https://graph.microsoft.com',
    ONEDRIVE: 'https://graph.microsoft.com',
    OUTLOOK: 'https://graph.microsoft.com',
    SALESFORCE: 'https://login.salesforce.com',
    SERVICE_CLOUD: 'https://login.salesforce.com'
  }
  return endpoints[integrationType] || ''
}

const needsWorkspaceId = (integrationType: string) => {
  return ['JIRA', 'CONFLUENCE', 'SALESFORCE', 'SERVICE_CLOUD'].includes(integrationType)
}

const startOAuthFlow = async () => {
  if (!oauthConfig.value.clientId || !oauthConfig.value.clientSecret) {
    alert('Please enter Client ID and Client Secret')
    return
  }
  
  connectingIntegration.value = oauthConfig.value.integrationType
  
  try {
    const response = await generateOAuthUrl({
      integrationType: oauthConfig.value.integrationType,
      clientId: oauthConfig.value.clientId,
      workspaceId: oauthConfig.value.workspaceId || undefined
    })
    
    if (response.data.error) {
      alert(response.data.error)
      connectingIntegration.value = null
      return
    }
    
    const { authUrl, state, integrationType: type } = response.data
    
    // Store OAuth config and state for callback
    sessionStorage.setItem('oauth_state', state)
    sessionStorage.setItem('oauth_integration_type', type)
    sessionStorage.setItem('oauth_config', JSON.stringify({
      clientId: oauthConfig.value.clientId,
      clientSecret: oauthConfig.value.clientSecret,
      workspaceId: oauthConfig.value.workspaceId,
      apiEndpoint: oauthConfig.value.apiEndpoint,
      name: oauthConfig.value.integrationName
    }))
    
    // Close dialog and redirect
    showOAuthConfigDialog.value = false
    window.location.href = authUrl
  } catch (error: any) {
    console.error('Failed to start OAuth flow:', error)
    alert('Failed to connect integration: ' + (error.response?.data?.error || error.message))
    connectingIntegration.value = null
  }
}

const handleOAuthCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('oauth_code')
  const state = urlParams.get('oauth_state')
  const error = urlParams.get('oauth_error')
  
  if (error) {
    alert('OAuth error: ' + error)
    window.history.replaceState({}, document.title, window.location.pathname)
    return
  }
  
  if (!code || !state) {
    return
  }
  
  // Verify state matches
  const storedState = sessionStorage.getItem('oauth_state')
  const integrationType = sessionStorage.getItem('oauth_integration_type')
  const useExistingConfig = sessionStorage.getItem('oauth_use_existing_config') === 'true'
  const storedConfig = sessionStorage.getItem('oauth_config')
  
  if (storedState !== state || !integrationType) {
    alert('OAuth state mismatch. Please try again.')
    window.history.replaceState({}, document.title, window.location.pathname)
    return
  }
  
  try {
    let tokenRequest: any = {
      code,
      state,
      integrationType: integrationType,
      name: (availableIntegrations.value[integrationType]?.name || integrationType) + ' Integration'
    }
    
    if (useExistingConfig) {
      // Using existing OAuth app config - backend will retrieve credentials
      // Just pass the integration type, backend will find the config
    } else if (storedConfig) {
      // Using credentials from dialog
      const config = JSON.parse(storedConfig)
      tokenRequest.clientId = config.clientId
      tokenRequest.clientSecret = config.clientSecret
      tokenRequest.workspaceId = config.workspaceId
      tokenRequest.apiEndpoint = config.apiEndpoint
      tokenRequest.name = config.name || tokenRequest.name
    } else {
      alert('OAuth configuration not found. Please try connecting again.')
      window.history.replaceState({}, document.title, window.location.pathname)
      return
    }
    
    const response = await exchangeOAuthToken(tokenRequest)
    
    if (response.data.success) {
      // Clean up
      sessionStorage.removeItem('oauth_state')
      sessionStorage.removeItem('oauth_integration_type')
      sessionStorage.removeItem('oauth_config')
      sessionStorage.removeItem('oauth_use_existing_config')
      window.history.replaceState({}, document.title, window.location.pathname)
      
      // Reload integrations
      await loadIntegrations()
      await loadAvailableIntegrations()
      
      // Show success message
      alert('✅ Integration connected successfully!\n\nYou can now manage this integration and control AI access to its data.')
    } else {
      alert('Failed to connect integration: ' + (response.data.error || 'Unknown error'))
    }
  } catch (error: any) {
    console.error('Failed to exchange OAuth token:', error)
    alert('Failed to complete OAuth flow: ' + (error.response?.data?.error || error.message))
  }
}

const editIntegration = (integration: any) => {
  currentIntegration.value = { ...integration }
  showEditDialog.value = true
}

const saveIntegration = async () => {
  try {
    if (currentIntegration.value.id) {
      await updateThirdPartyIntegration(currentIntegration.value.id, currentIntegration.value)
    } else {
      await createThirdPartyIntegration(currentIntegration.value)
    }
    await loadIntegrations()
    showAddDialog.value = false
    showEditDialog.value = false
    currentIntegration.value = null
    alert('Integration saved successfully!')
  } catch (error) {
    console.error('Failed to save integration:', error)
    alert('Failed to save integration. Please try again.')
  }
}

const deleteIntegration = async (integration: any) => {
  if (!confirm(`Are you sure you want to delete "${integration.name}"?`)) {
    return
  }
  
  try {
    await deleteThirdPartyIntegration(integration.id)
    await loadIntegrations()
    alert('Integration deleted successfully!')
  } catch (error) {
    console.error('Failed to delete integration:', error)
    alert('Failed to delete integration. Please try again.')
  }
}

const updateIntegrationAIAccess = async (integration: any) => {
  try {
    await updateThirdPartyAIAccess(integration.id, integration.aiAccessible)
  } catch (error) {
    console.error('Failed to update AI access:', error)
    integration.aiAccessible = !integration.aiAccessible
    alert('Failed to update AI access. Please try again.')
  }
}

const viewBlacklist = async (integration: any) => {
  currentIntegration.value = integration
  loadingItems.value = true
  
  try {
    // Load blacklisted items
    const blacklistResponse = await getBlacklistedItems(integration.id)
    blacklistedItems.value = blacklistResponse.data || []
    
    // Load available items
    const itemType = getItemTypeForIntegration(integration.integrationType)
    const itemsResponse = await fetchThirdPartyItems(integration.id, itemType)
    availableItems.value = itemsResponse.data || []
    
    showBlacklistDialog.value = true
  } catch (error) {
    console.error('Failed to load blacklist:', error)
    alert('Failed to load blacklist. Please try again.')
  } finally {
    loadingItems.value = false
  }
}

const getItemTypeForIntegration = (integrationType: string) => {
  const typeMap: Record<string, string> = {
    CONFLUENCE: 'PAGE',
    JIRA: 'ISSUE',
    SLACK: 'CHANNEL',
    GITHUB: 'REPOSITORY',
    GOOGLE_DRIVE: 'FILE',
    GOOGLE_DOCS: 'DOCUMENT',
    GOOGLE_SHEETS: 'SPREADSHEET',
    GMAIL: 'EMAIL'
  }
  return typeMap[integrationType] || 'ITEM'
}

const addItemToBlacklist = async (item: any) => {
  if (!currentIntegration.value) return
  
  try {
    await addToBlacklist(currentIntegration.value.id, {
      itemType: item.type || 'ITEM',
      itemId: item.id,
      itemTitle: item.title || item.name,
      itemUrl: item.url || '',
      reason: ''
    })
    
    // Reload blacklist and available items
    await viewBlacklist(currentIntegration.value)
    alert('Item added to blacklist')
  } catch (error) {
    console.error('Failed to add to blacklist:', error)
    alert('Failed to add item to blacklist. Please try again.')
  }
}

const removeItemFromBlacklist = async (item: any) => {
  if (!currentIntegration.value) return
  
  try {
    await removeFromBlacklist(currentIntegration.value.id, item.itemId)
    
    // Reload blacklist and available items
    await viewBlacklist(currentIntegration.value)
    alert('Item removed from blacklist')
  } catch (error) {
    console.error('Failed to remove from blacklist:', error)
    alert('Failed to remove item from blacklist. Please try again.')
  }
}

onMounted(() => {
  loadDataPoints()
  loadIntegrations()
  loadAvailableIntegrations()
  
  // Check for OAuth callback
  handleOAuthCallback()
})
</script>

<style scoped>
.ai-data-access-container {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.page-header {
  margin-bottom: 1rem;
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #333;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 1rem;
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
}

.tab-button {
  flex: 1;
  padding: 1rem 1.5rem;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  transition: all 0.2s;
  position: relative;
}

.tab-button:hover {
  background: #f0f0f0;
  color: #333;
}

.tab-button.active {
  color: #1976d2;
  border-bottom-color: #1976d2;
  background: white;
}

.tab-content {
  padding: 2rem;
}

.tab-panel {
  animation: fadeIn 0.2s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
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
  margin-bottom: 1rem;
}

.section-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.section-description {
  margin: 0 0 2rem 0;
  color: #666;
  font-size: 1rem;
}

.placeholder-content {
  padding: 3rem;
  text-align: center;
  background: #f8f9fa;
  border: 2px dashed #ddd;
  border-radius: 8px;
  color: #999;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
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
  padding: 3rem;
  text-align: center;
  color: #666;
}

.empty-state {
  padding: 3rem;
  text-align: center;
  background: #f8f9fa;
  border: 2px dashed #ddd;
  border-radius: 8px;
  color: #999;
}

.data-points-list {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.list-header {
  display: grid;
  grid-template-columns: 2fr 2fr 1fr 1.5fr 1.5fr 1.5fr;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: #f5f5f5;
  border-bottom: 2px solid #e0e0e0;
  font-weight: 600;
  font-size: 0.9rem;
  color: #333;
}

.data-point-item {
  display: grid;
  grid-template-columns: 2fr 2fr 1fr 1.5fr 1.5fr 1.5fr;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
  transition: background 0.2s;
}

.data-point-item:hover {
  background: #fafafa;
}

.data-point-item:last-child {
  border-bottom: none;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.variable-code {
  background: #f8f9fa;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  color: #d63384;
  border: 1px solid #e0e0e0;
}

.path-text {
  font-size: 0.9rem;
  color: #666;
  word-break: break-all;
}

.type-badge {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.type-string {
  background: #e3f2fd;
  color: #1976d2;
}

.type-number {
  background: #f3e5f5;
  color: #7b1fa2;
}

.type-boolean {
  background: #fff3e0;
  color: #e65100;
}

.type-date {
  background: #e8f5e9;
  color: #2e7d32;
}

.type-email {
  background: #fce4ec;
  color: #c2185b;
}

.type-url {
  background: #e0f2f1;
  color: #00695c;
}

.connection-name {
  font-size: 0.9rem;
  color: #333;
}

.access-col {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

.toggle-switch input:checked + .toggle-slider {
  background-color: #1976d2;
}

.toggle-switch input:checked + .toggle-slider:before {
  transform: translateX(26px);
}

.access-status {
  font-size: 0.85rem;
  font-weight: 600;
}

.access-status.allowed {
  color: #2e7d32;
}

.access-status.denied {
  color: #d32f2f;
}

.btn-test {
  padding: 0.25rem 0.75rem;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  color: #333;
  transition: all 0.2s;
}

.btn-test:hover:not(:disabled) {
  background: #e0e0e0;
  border-color: #1976d2;
  color: #1976d2;
}

.btn-test:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.test-value {
  font-size: 0.85rem;
  color: #2e7d32;
  font-weight: 500;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.test-value-placeholder {
  font-size: 0.85rem;
  color: #999;
  font-style: italic;
}

/* Third Party Integrations Styles */
.integrations-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.integration-group {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
}

.group-header {
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  color: #333;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0.5rem;
}

.integrations-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.integration-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 1rem;
  transition: all 0.2s;
}

.integration-card:hover {
  border-color: #1976d2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.integration-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.integration-info h4 {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
  color: #333;
}

.integration-type {
  font-size: 0.85rem;
  color: #666;
  text-transform: capitalize;
}

.integration-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-icon:hover {
  background: #e0e0e0;
}

.integration-status {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.85rem;
}

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.75rem;
}

.status-badge.active {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.inactive {
  background: #ffebee;
  color: #c62828;
}

.sync-info {
  color: #666;
}

/* Dialog Styles */
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
  padding: 2rem;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.dialog-header h3 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
}

.dialog-close:hover {
  color: #333;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

.form-group input,
.form-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #1976d2;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}

/* Blacklist Dialog */
.blacklist-dialog {
  max-width: 900px;
}

.blacklist-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  margin-top: 1rem;
}

.blacklist-section {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 1rem;
}

.blacklist-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  color: #333;
}

.items-list {
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.item-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  flex: 1;
}

.item-info h5 {
  margin: 0 0 0.25rem 0;
  font-size: 0.9rem;
  color: #333;
}

.item-info p {
  margin: 0;
  font-size: 0.75rem;
  color: #666;
}

.item-actions {
  display: flex;
  gap: 0.5rem;
}

/* Available Integrations Grid */
.available-integrations {
  margin-top: 2rem;
}

.integration-category {
  margin-bottom: 3rem;
}

.category-header {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 1.5rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e0e0e0;
}

.integrations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.integration-card-available {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: all 0.2s;
  position: relative;
}

.integration-card-available:hover {
  border-color: #1976d2;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.integration-card-available.connected {
  border-color: #4caf50;
  background: #f1f8f4;
}

.integration-icon {
  font-size: 3rem;
  text-align: center;
  margin-bottom: 0.5rem;
}

.integration-details {
  flex: 1;
}

.integration-details h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  color: #333;
  font-weight: 600;
}

.integration-details p {
  margin: 0;
  font-size: 0.9rem;
  color: #666;
  line-height: 1.4;
}

.integration-action {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.btn-connect {
  width: 100%;
  padding: 0.75rem 1.5rem;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-connect:hover:not(:disabled) {
  background: #1565c0;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.3);
}

.btn-connect:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.7;
}

.connected-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem;
  background: #e8f5e9;
  border-radius: 6px;
  color: #2e7d32;
  font-weight: 600;
  font-size: 0.9rem;
}

.btn-link {
  background: none;
  border: none;
  color: #1976d2;
  cursor: pointer;
  font-size: 0.85rem;
  text-decoration: underline;
  padding: 0;
}

.btn-link:hover {
  color: #1565c0;
}

.connected-integrations-section {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 2px solid #e0e0e0;
}

.section-subheader {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 1.5rem 0;
}
</style>

