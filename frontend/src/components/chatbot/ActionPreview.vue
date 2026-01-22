<template>
  <div class="action-preview">
    <!-- Order Selector Preview -->
    <div v-if="componentType === 'ORDER_SELECTOR'" class="preview-order-selector">
      <label>{{ config.label || 'Select Order' }}:</label>
      <select>
        <option>{{ config.placeholder || '-- Select an order --' }}</option>
        <option>Order #ORD-001 - $99.99 - Processing</option>
        <option>Order #ORD-002 - $149.50 - Shipped</option>
      </select>
    </div>
    
    <!-- Date Picker Preview -->
    <div v-else-if="componentType === 'DATE_PICKER'" class="preview-date-picker">
      <label>{{ config.label || 'Select Date' }}:</label>
      <input type="date" />
    </div>
    
    <!-- File Uploader Preview -->
    <div v-else-if="componentType === 'FILE_UPLOADER'" class="preview-file-uploader">
      <label>{{ config.label || 'Upload File' }}:</label>
      <input type="file" :accept="config.accept" />
      <small v-if="config.maxSize">Max size: {{ config.maxSize }}MB</small>
    </div>
    
    <!-- Form Preview -->
    <div v-else-if="componentType === 'FORM'" class="preview-form">
      <div v-for="(field, index) in (config.fields || [])" :key="index" class="preview-form-field">
        <label>
          {{ field.label || 'Field ' + (index + 1) }}
          <span v-if="field.required" class="required">*</span>
        </label>
        <input 
          v-if="field.type === 'text' || field.type === 'email' || field.type === 'number'"
          :type="field.type"
          :placeholder="field.label"
        />
        <textarea 
          v-else-if="field.type === 'textarea'"
          :placeholder="field.label"
        />
        <select v-else-if="field.type === 'select'">
          <option v-for="option in getSelectOptions(field.options)" :key="option">
            {{ option }}
          </option>
        </select>
      </div>
      <button class="btn btn-primary">Submit</button>
    </div>
    
    <!-- Button Preview -->
    <div v-else-if="componentType === 'BUTTON'" class="preview-button">
      <button :class="'btn btn-' + (config.buttonStyle || 'primary')">
        {{ config.buttonText || 'Submit' }}
      </button>
    </div>
    
    <!-- Custom Preview -->
    <div v-else-if="componentType === 'CUSTOM'" class="preview-custom" v-html="config.html || '<div>Custom Component</div>'"></div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  componentType: string
  config: any
}>()

const getSelectOptions = (optionsText: string) => {
  if (!optionsText) return ['Option 1', 'Option 2']
  return optionsText.split('\n').filter(o => o.trim())
}
</script>

<style scoped>
.action-preview {
  padding: 1rem;
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.preview-order-selector,
.preview-date-picker,
.preview-file-uploader {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.preview-order-selector label,
.preview-date-picker label,
.preview-file-uploader label {
  font-weight: 500;
  font-size: 0.9rem;
}

.preview-order-selector select,
.preview-date-picker input,
.preview-file-uploader input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.preview-file-uploader small {
  font-size: 0.85rem;
  color: #666;
}

.preview-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.preview-form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.preview-form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}

.preview-form-field .required {
  color: #e74c3c;
}

.preview-form-field input,
.preview-form-field textarea,
.preview-form-field select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.preview-button {
  margin-top: 0.5rem;
}

.preview-custom {
  padding: 0.5rem;
}
</style>

