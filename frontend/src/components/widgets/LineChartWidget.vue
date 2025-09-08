<template>
  <div class="line-chart-widget">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading...</span>
    </div>
    
    <div v-else-if="data && data.length > 0" class="chart-container">
      <svg viewBox="0 0 400 200" class="line-svg">
        <!-- Grid lines -->
        <defs>
          <pattern id="grid" width="40" height="20" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 20" fill="none" stroke="#e9ecef" stroke-width="1"/>
          </pattern>
        </defs>
        <rect width="400" height="200" fill="url(#grid)" />
        
        <!-- Line path -->
        <path 
          :d="linePath"
          fill="none"
          stroke="#007bff"
          stroke-width="3"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        
        <!-- Data points -->
        <circle
          v-for="(point, index) in dataPoints"
          :key="index"
          :cx="point.x"
          :cy="point.y"
          r="4"
          fill="#007bff"
          stroke="white"
          stroke-width="2"
          class="data-point"
          :title="`${data[index].label}: ${data[index].value}`"
        />
      </svg>
      
      <!-- X-axis labels -->
      <div class="x-axis">
        <span 
          v-for="(item, index) in data"
          :key="index"
          class="x-label"
        >
          {{ item.label }}
        </span>
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

const minValue = computed(() => {
  if (!props.data || props.data.length === 0) return 0
  return Math.min(...props.data.map(item => item.value))
})

const dataPoints = computed(() => {
  if (!props.data || props.data.length === 0) return []
  
  const padding = 20
  const width = 400 - (padding * 2)
  const height = 200 - (padding * 2)
  
  return props.data.map((item, index) => {
    const x = padding + (index / (props.data!.length - 1)) * width
    const y = padding + height - ((item.value - minValue.value) / (maxValue.value - minValue.value)) * height
    
    return { x, y }
  })
})

const linePath = computed(() => {
  if (dataPoints.value.length === 0) return ''
  
  let path = `M ${dataPoints.value[0].x} ${dataPoints.value[0].y}`
  
  for (let i = 1; i < dataPoints.value.length; i++) {
    path += ` L ${dataPoints.value[i].x} ${dataPoints.value[i].y}`
  }
  
  return path
})
</script>

<style scoped>
.line-chart-widget {
  height: 100%;
  display: flex;
  flex-direction: column;
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
  flex-direction: column;
}

.line-svg {
  flex: 1;
  width: 100%;
}

.data-point {
  cursor: pointer;
  transition: all 0.2s ease;
}

.data-point:hover {
  r: 6;
  stroke-width: 3;
}

.x-axis {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  font-size: 0.75rem;
  color: #6c757d;
}

.x-label {
  text-align: center;
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
