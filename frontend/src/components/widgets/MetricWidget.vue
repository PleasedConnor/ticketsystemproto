<template>
  <div class="metric-widget">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading...</span>
    </div>
    
    <div v-else-if="data" class="metric-content">
      <div class="metric-value">{{ formatValue(data.value) }}</div>
      <div class="metric-label">{{ data.label || config.metric }}</div>
      <div v-if="data.change" class="metric-change" :class="data.change > 0 ? 'positive' : 'negative'">
        {{ data.change > 0 ? '+' : '' }}{{ data.change }}%
      </div>
    </div>
    
    <div v-else class="no-data">
      <span>No data available</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'

defineProps<{
  data: any
  config: any
  loading: boolean
}>()

const formatValue = (value: number): string => {
  if (value >= 1000000) {
    return (value / 1000000).toFixed(1) + 'M'
  } else if (value >= 1000) {
    return (value / 1000).toFixed(1) + 'K'
  }
  return value.toString()
}
</script>

<style scoped>
.metric-widget {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  color: #6c757d;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 2px solid #e9ecef;
  border-top: 2px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.metric-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.metric-value {
  font-size: 2.5rem;
  font-weight: bold;
  color: #007bff;
  line-height: 1;
}

.metric-label {
  font-size: 0.9rem;
  color: #6c757d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metric-change {
  font-size: 0.8rem;
  font-weight: 500;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
}

.metric-change.positive {
  background-color: #d4edda;
  color: #155724;
}

.metric-change.negative {
  background-color: #f8d7da;
  color: #721c24;
}

.no-data {
  color: #6c757d;
  font-style: italic;
}
</style>
