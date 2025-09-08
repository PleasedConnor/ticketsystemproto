<template>
  <div class="bar-chart-widget">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading...</span>
    </div>
    
    <div v-else-if="data && data.length > 0" class="chart-container">
      <div class="bars">
        <div 
          v-for="(item, index) in data" 
          :key="index"
          class="bar-group"
        >
          <div 
            class="bar"
            :style="{ height: getBarHeight(item.value) + '%' }"
            :title="`${item.label}: ${item.value}`"
          ></div>
          <div class="bar-label">{{ item.label }}</div>
          <div class="bar-value">{{ item.value }}</div>
        </div>
      </div>
    </div>
    
    <div v-else class="no-data">
      <span>No data available</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from 'vue'

const props = defineProps<{
  data: Array<{ label: string, value: number }> | null
  config: any
  loading: boolean
}>()

const maxValue = computed(() => {
  if (!props.data || props.data.length === 0) return 0
  return Math.max(...props.data.map(item => item.value))
})

const getBarHeight = (value: number): number => {
  if (maxValue.value === 0) return 0
  return (value / maxValue.value) * 100
}
</script>

<style scoped>
.bar-chart-widget {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  color: #6c757d;
  height: 100%;
  justify-content: center;
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

.chart-container {
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 1rem 0;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  width: 100%;
  height: 100%;
}

.bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar {
  width: 100%;
  max-width: 60px;
  background: linear-gradient(135deg, #007bff, #0056b3);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.bar:hover {
  background: linear-gradient(135deg, #0056b3, #004085);
  transform: translateY(-2px);
}

.bar-label {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #6c757d;
  text-align: center;
  word-break: break-word;
}

.bar-value {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #495057;
}

.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6c757d;
  font-style: italic;
}
</style>
