<template>
  <div class="pie-chart-widget">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading...</span>
    </div>
    
    <div v-else-if="data && data.length > 0" class="chart-container">
      <div class="pie-chart">
        <svg viewBox="0 0 200 200" class="pie-svg">
          <path
            v-for="(segment, index) in pieSegments"
            :key="index"
            :d="segment.path"
            :fill="segment.color"
            :stroke="'white'"
            :stroke-width="2"
            class="pie-segment"
            :title="`${segment.label}: ${segment.value} (${segment.percentage}%)`"
          />
        </svg>
      </div>
      
      <div class="legend">
        <div 
          v-for="(item, index) in data"
          :key="index"
          class="legend-item"
        >
          <div 
            class="legend-color"
            :style="{ backgroundColor: getColor(index) }"
          ></div>
          <div class="legend-text">
            <span class="legend-label">{{ item.label }}</span>
            <span class="legend-value">{{ item.value }} ({{ getPercentage(item.value) }}%)</span>
          </div>
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

const colors = ['#007bff', '#28a745', '#ffc107', '#dc3545', '#6f42c1', '#fd7e14', '#20c997', '#e83e8c']

const totalValue = computed(() => {
  if (!props.data) return 0
  return props.data.reduce((sum, item) => sum + item.value, 0)
})

const pieSegments = computed(() => {
  if (!props.data || totalValue.value === 0) return []
  
  let cumulativeAngle = 0
  const centerX = 100
  const centerY = 100
  const radius = 80
  
  return props.data.map((item, index) => {
    const percentage = (item.value / totalValue.value) * 100
    const angle = (percentage / 100) * 360 // Convert to degrees
    const angleRad = (angle * Math.PI) / 180 // Convert to radians
    
    // Calculate start and end points
    const startAngle = (cumulativeAngle * Math.PI) / 180
    const endAngle = ((cumulativeAngle + angle) * Math.PI) / 180
    
    const x1 = centerX + radius * Math.cos(startAngle)
    const y1 = centerY + radius * Math.sin(startAngle)
    const x2 = centerX + radius * Math.cos(endAngle)
    const y2 = centerY + radius * Math.sin(endAngle)
    
    // Large arc flag for arcs > 180 degrees
    const largeArcFlag = angle > 180 ? 1 : 0
    
    // Create SVG path for pie slice
    const path = [
      `M ${centerX} ${centerY}`, // Move to center
      `L ${x1} ${y1}`, // Line to start point
      `A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2}`, // Arc to end point
      'Z' // Close path back to center
    ].join(' ')
    
    cumulativeAngle += angle
    
    return {
      ...item,
      color: getColor(index),
      percentage: Math.round(percentage * 10) / 10,
      path
    }
  })
})

const getColor = (index: number): string => {
  return colors[index % colors.length]
}

const getPercentage = (value: number): string => {
  if (totalValue.value === 0) return '0'
  return Math.round((value / totalValue.value) * 100 * 10) / 10 + ''
}
</script>

<style scoped>
.pie-chart-widget {
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
  gap: 1rem;
  align-items: center;
}

.pie-chart {
  flex: 1;
  max-width: 200px;
}

.pie-svg {
  width: 100%;
  height: auto;
}

.pie-segment {
  cursor: pointer;
  transition: all 0.3s ease;
}

.pie-segment:hover {
  transform: scale(1.05);
}

.legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 100%;
  overflow-y: auto;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.legend-label {
  font-weight: 500;
  color: #495057;
  word-break: break-word;
}

.legend-value {
  color: #6c757d;
  font-size: 0.75rem;
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
