<template>
  <div class="page-container">
    <div class="ticket-fields-view">
      <div class="view-header">
        <div class="header-content">
          <h1>Ticket Fields</h1>
          <p class="subtitle">Configure which metadata fields appear on the Tickets page</p>
        </div>
        <button class="btn btn-primary" @click="openAddDialog">
          + Add Field
        </button>
      </div>

    <div v-if="loading" class="loading-state">
      <p>Loading fields...</p>
    </div>

    <div v-else-if="fields.length === 0" class="empty-state">
      <p>No fields configured. Click "Add Field" to create a ticket field.</p>
    </div>

    <div v-else class="fields-list">
      <div 
        v-for="field in sortedFields" 
        :key="field.id"
        class="field-card"
      >
        <div class="field-header">
          <div class="field-info">
            <h3>
              <span v-if="field.icon" class="field-icon">{{ field.icon }}</span>
              {{ field.fieldLabel }}
            </h3>
            <span class="field-key">{{ field.fieldKey }}</span>
          </div>
          <div class="field-actions">
            <button class="btn-icon" @click="editField(field)" title="Edit">
              ✏️
            </button>
            <button class="btn-icon" @click="deleteField(field.id!)" title="Delete">
              🗑️
            </button>
          </div>
        </div>

        <div class="field-details">
          <div class="detail-row">
            <strong>Metadata Variable:</strong>
            <code>{{ field.metadataVariable || 'Not mapped' }}</code>
          </div>
          <div class="detail-row">
            <strong>Type:</strong> {{ field.fieldType }}
          </div>
          <div class="detail-row">
            <strong>Display Order:</strong> {{ field.displayOrder }}
          </div>
          <div class="detail-row">
            <strong>Status:</strong>
            <span :class="['status-badge', field.isVisible ? 'visible' : 'hidden']">
              {{ field.isVisible ? 'Visible' : 'Hidden' }}
            </span>
          </div>
          <div class="detail-row">
            <strong>Editable:</strong>
            <span :class="['status-badge', field.isEditable ? 'editable' : 'readonly']">
              {{ field.isEditable ? 'Yes' : 'No' }}
            </span>
          </div>
        </div>

        <div class="field-controls">
          <button 
            class="btn-small" 
            @click="moveField(field.id!, 'up')"
            :disabled="!canMoveUp(field)"
          >
            ↑ Move Up
          </button>
          <button 
            class="btn-small" 
            @click="moveField(field.id!, 'down')"
            :disabled="!canMoveDown(field)"
          >
            ↓ Move Down
          </button>
        </div>
      </div>
    </div>

    <!-- Field Dialog -->
    <Teleport to="body" v-if="showFieldDialog">
      <div class="modal-overlay" @click="handleOverlayClick">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h2>{{ editingField ? 'Edit Field' : 'Add Field' }}</h2>
            <button class="btn-close" @click="showFieldDialog = false">×</button>
          </div>
          
          <div class="modal-body">
            <div class="form-group">
              <label>Field Label *</label>
              <input v-model="fieldForm.fieldLabel" type="text" placeholder="e.g., Customer Name" />
            </div>
            
            <div class="form-group">
              <label>Field Key *</label>
              <input v-model="fieldForm.fieldKey" type="text" placeholder="e.g., customer_name" />
              <small>Internal identifier (lowercase, underscores)</small>
            </div>
            
            <div class="form-group">
              <label>Metadata Variable</label>
              <select v-model="fieldForm.metadataVariable" :disabled="loadingVariables">
                <option value="">-- Select a variable (optional) --</option>
                <option v-for="variable in availableVariables" :key="variable" :value="variable">
                  {{ variable }}
                </option>
              </select>
              <small>Map to a metadata variable (optional)</small>
            </div>
            
            <div class="form-group">
              <label>Field Type *</label>
              <select v-model="fieldForm.fieldType">
                <option value="TEXT">Text</option>
                <option value="EMAIL">Email</option>
                <option value="PHONE">Phone</option>
                <option value="NUMBER">Number</option>
                <option value="DATE">Date</option>
                <option value="BOOLEAN">Boolean</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>Icon (Emoji)</label>
              <div class="icon-input-group">
                <input v-model="fieldForm.icon" type="text" placeholder="e.g., 👤" maxlength="2" />
                <button type="button" class="btn-emoji-picker" @click="showEmojiPicker = !showEmojiPicker">
                  😀
                </button>
                <div v-if="showEmojiPicker" class="emoji-picker">
                  <div class="emoji-category">
                    <div class="emoji-category-title">Common</div>
                    <div class="emoji-grid">
                      <button 
                        v-for="emoji in commonEmojis" 
                        :key="emoji"
                        type="button"
                        class="emoji-btn"
                        @click="selectEmoji(emoji)"
                      >
                        {{ emoji }}
                      </button>
                    </div>
                  </div>
                  <div class="emoji-category">
                    <div class="emoji-category-title">People</div>
                    <div class="emoji-grid">
                      <button 
                        v-for="emoji in peopleEmojis" 
                        :key="emoji"
                        type="button"
                        class="emoji-btn"
                        @click="selectEmoji(emoji)"
                      >
                        {{ emoji }}
                      </button>
                    </div>
                  </div>
                  <div class="emoji-category">
                    <div class="emoji-category-title">Objects</div>
                    <div class="emoji-grid">
                      <button 
                        v-for="emoji in objectEmojis" 
                        :key="emoji"
                        type="button"
                        class="emoji-btn"
                        @click="selectEmoji(emoji)"
                      >
                        {{ emoji }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="form-group">
              <label>Description</label>
              <textarea v-model="fieldForm.description" rows="2" placeholder="Optional description"></textarea>
            </div>
            
            <div class="form-group">
              <label>Display Order</label>
              <input v-model.number="fieldForm.displayOrder" type="number" min="0" />
            </div>
            
            <div class="form-group checkbox-group">
              <div class="checkbox-label">
                <input v-model="fieldForm.isVisible" type="checkbox" id="isVisible" />
                <label for="isVisible">Visible</label>
              </div>
            </div>
            
            <div class="form-group checkbox-group">
              <div class="checkbox-label">
                <input v-model="fieldForm.isEditable" type="checkbox" id="isEditable" />
                <label for="isEditable">Editable</label>
              </div>
            </div>
          </div>
          
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showFieldDialog = false">Cancel</button>
            <button class="btn btn-primary" @click="saveField" :disabled="saving">
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const { 
  getAllTicketFields,
  createTicketField,
  updateTicketField,
  deleteTicketField,
  getAvailableMetadataVariables
} = useBackendApi()

interface TicketField {
  id?: number
  fieldLabel: string
  fieldKey: string
  metadataVariable?: string
  fieldType: string
  displayOrder: number
  isVisible: boolean
  isEditable: boolean
  icon?: string
  description?: string
}

const fields = ref<TicketField[]>([])
const loading = ref(true)
const saving = ref(false)
const showFieldDialog = ref(false)
const editingField = ref<TicketField | null>(null)
const availableVariables = ref<string[]>([])
const loadingVariables = ref(false)
const showEmojiPicker = ref(false)

const commonEmojis = ['👤', '📧', '📞', '📍', '💻', '🆔', '💰', '📦', '✅', '❌', '⭐', '🔔']
const peopleEmojis = ['👤', '👥', '👨', '👩', '🧑', '👶', '👴', '👵']
const objectEmojis = ['📧', '📞', '💻', '📱', '⌚', '🖥️', '📦', '📄', '📋', '📊', '💰', '💳', '🏠', '📍', '🌐', '🔒', '🔑', '📝', '✏️', '🗂️']

const selectEmoji = (emoji: string) => {
  fieldForm.value.icon = emoji
  showEmojiPicker.value = false
}

const handleOverlayClick = () => {
  showEmojiPicker.value = false
  showFieldDialog.value = false
}

const fieldForm = ref<TicketField>({
  fieldLabel: '',
  fieldKey: '',
  metadataVariable: '',
  fieldType: 'TEXT',
  displayOrder: 0,
  isVisible: true,
  isEditable: false,
  icon: '',
  description: ''
})

const sortedFields = computed(() => {
  return [...fields.value].sort((a, b) => a.displayOrder - b.displayOrder)
})

const canMoveUp = (field: TicketField) => {
  const currentIndex = sortedFields.value.findIndex(f => f.id === field.id)
  return currentIndex > 0
}

const canMoveDown = (field: TicketField) => {
  const currentIndex = sortedFields.value.findIndex(f => f.id === field.id)
  return currentIndex < sortedFields.value.length - 1 && currentIndex >= 0
}

const loadFields = async () => {
  loading.value = true
  try {
    const response = await getAllTicketFields()
    fields.value = response.data || []
  } catch (error) {
    console.error('Failed to load fields:', error)
  } finally {
    loading.value = false
  }
}

const saveField = async () => {
  saving.value = true
  try {
    if (editingField.value?.id) {
      await updateTicketField(editingField.value.id, fieldForm.value)
    } else {
      await createTicketField(fieldForm.value)
    }
    await loadFields()
    showFieldDialog.value = false
    resetForm()
  } catch (error) {
    console.error('Failed to save field:', error)
    alert('Failed to save field. Please try again.')
  } finally {
    saving.value = false
  }
}

const loadAvailableVariables = async () => {
  loadingVariables.value = true
  try {
    const excludeVar = editingField.value?.metadataVariable || null
    const response = await getAvailableMetadataVariables(excludeVar || undefined)
    availableVariables.value = response.data || []
  } catch (error) {
    console.error('Failed to load available variables:', error)
    availableVariables.value = []
  } finally {
    loadingVariables.value = false
  }
}

const editField = async (field: TicketField) => {
  editingField.value = field
  fieldForm.value = { ...field }
  await loadAvailableVariables()
  showFieldDialog.value = true
}

const openAddDialog = async () => {
  editingField.value = null
  resetForm()
  await loadAvailableVariables()
  showFieldDialog.value = true
}

const deleteField = async (id: number) => {
  if (!confirm('Are you sure you want to delete this field?')) return
  
  try {
    await deleteTicketField(id)
    await loadFields()
  } catch (error) {
    console.error('Failed to delete field:', error)
    alert('Failed to delete field. Please try again.')
  }
}

const moveField = async (id: number, direction: 'up' | 'down') => {
  const field = fields.value.find(f => f.id === id)
  if (!field) return
  
  const currentIndex = sortedFields.value.findIndex(f => f.id === id)
  if (currentIndex === -1) return
  
  const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1
  if (newIndex < 0 || newIndex >= sortedFields.value.length) return
  
  const targetField = sortedFields.value[newIndex]
  const tempOrder = field.displayOrder
  field.displayOrder = targetField.displayOrder
  targetField.displayOrder = tempOrder
  
  try {
    await updateTicketField(field.id!, field)
    await updateTicketField(targetField.id!, targetField)
    await loadFields()
  } catch (error) {
    console.error('Failed to reorder field:', error)
    alert('Failed to reorder field. Please try again.')
  }
}

const resetForm = () => {
  editingField.value = null
  fieldForm.value = {
    fieldLabel: '',
    fieldKey: '',
    metadataVariable: '',
    fieldType: 'TEXT',
    displayOrder: fields.value.length,
    isVisible: true,
    isEditable: false,
    icon: '',
    description: ''
  }
  showEmojiPicker.value = false
}

onMounted(() => {
  loadFields()
})
</script>

<style scoped>
.page-container {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.ticket-fields-view {
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

.loading-state,
.empty-state {
  padding: 3rem;
  text-align: center;
  color: #999;
}

.fields-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
}

.field-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f0f0f0;
}

.field-info h3 {
  margin: 0 0 0.25rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field-icon {
  font-size: 1.2rem;
}

.field-key {
  font-size: 0.85rem;
  color: #999;
  font-family: monospace;
}

.field-actions {
  display: flex;
  gap: 0.5rem;
}

.field-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.detail-row strong {
  min-width: 140px;
  color: #666;
}

.detail-row code {
  background: #f8f9fa;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  color: #d63384;
}

.location-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  background: #e3f2fd;
  color: #1976d2;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-badge.visible {
  background: #d4edda;
  color: #155724;
}

.status-badge.hidden {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.editable {
  background: #d1ecf1;
  color: #0c5460;
}

.status-badge.readonly {
  background: #e2e3e5;
  color: #383d41;
}

.field-controls {
  display: flex;
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

.btn-primary {
  background: #dc3545;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c82333;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  background: #f0f0f0;
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

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.25rem;
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
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.85rem;
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

.icon-input-group {
  position: relative;
  display: flex;
  gap: 0.5rem;
}

.icon-input-group input {
  flex: 1;
}

.btn-emoji-picker {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.2s;
}

.btn-emoji-picker:hover {
  background: #f8f9fa;
  border-color: #dc3545;
}

.emoji-picker {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 1rem;
  z-index: 1001;
  max-width: 300px;
  max-height: 400px;
  overflow-y: auto;
}

.emoji-category {
  margin-bottom: 1rem;
}

.emoji-category:last-child {
  margin-bottom: 0;
}

.emoji-category-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.25rem;
}

.emoji-btn {
  padding: 0.5rem;
  border: 1px solid transparent;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-btn:hover {
  background: #f8f9fa;
  border-color: #dc3545;
  transform: scale(1.1);
}
</style>

