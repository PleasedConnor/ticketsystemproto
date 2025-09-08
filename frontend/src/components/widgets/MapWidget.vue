<template>
  <div class="map-widget">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading...</span>
    </div>
    
    <div v-else-if="data && data.length > 0" class="map-container">
      <div class="world-map">
        <!-- Simplified world map visualization -->
        <div class="map-regions">
          <div 
            v-for="(region, index) in mapRegions"
            :key="index"
            class="map-region"
            :style="{ 
              left: region.x + '%', 
              top: region.y + '%',
              transform: `scale(${region.scale})`
            }"
            :title="`${region.name}: ${region.value}`"
          >
            <div class="region-marker">
              <div class="marker-dot" :style="{ backgroundColor: getIntensityColor(region.value) }"></div>
              <div class="marker-label">{{ region.value }}</div>
            </div>
          </div>
        </div>
        
        <!-- Simple world outline -->
        <svg viewBox="0 0 800 400" class="world-outline">
          <!-- Simplified continents -->
          <path d="M150,100 Q200,80 250,100 Q300,90 350,110 L350,200 Q300,220 250,210 Q200,230 150,200 Z" 
                fill="#f8f9fa" stroke="#e9ecef" stroke-width="1"/>
          <path d="M400,120 Q450,100 500,120 Q550,110 600,130 L600,230 Q550,250 500,240 Q450,260 400,230 Z" 
                fill="#f8f9fa" stroke="#e9ecef" stroke-width="1"/>
          <path d="M100,250 Q150,230 200,250 Q250,240 300,260 L300,350 Q250,370 200,360 Q150,380 100,350 Z" 
                fill="#f8f9fa" stroke="#e9ecef" stroke-width="1"/>
          <path d="M650,150 Q700,130 750,150 Q780,140 800,160 L800,250 Q780,270 750,260 Q700,280 650,250 Z" 
                fill="#f8f9fa" stroke="#e9ecef" stroke-width="1"/>
        </svg>
      </div>
      
      <div class="map-legend">
        <h4>Geographic Distribution</h4>
        <div class="legend-items">
          <div 
            v-for="(item, index) in sortedData"
            :key="index"
            class="legend-item"
          >
            <div 
              class="legend-color"
              :style="{ backgroundColor: getIntensityColor(item.value) }"
            ></div>
            <span class="legend-label">{{ item.location || item.label }}</span>
            <span class="legend-value">{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="no-data">
      <span>No geographic data available</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from 'vue'

const props = defineProps<{
  data: Array<{ location?: string, label?: string, value: number }> | null
  config: any
  loading: boolean
}>()

// Simplified coordinates for major locations
const locationCoordinates: Record<string, { x: number, y: number }> = {
  'New York': { x: 25, y: 35 },
  'London': { x: 50, y: 25 },
  'Tokyo': { x: 85, y: 40 },
  'Sydney': { x: 90, y: 75 },
  'Berlin': { x: 52, y: 28 },
  'Paris': { x: 48, y: 30 },
  'Los Angeles': { x: 15, y: 40 },
  'Singapore': { x: 80, y: 60 },
  'Dubai': { x: 60, y: 45 },
  'Toronto': { x: 22, y: 30 }
}

const maxValue = computed(() => {
  if (!props.data || props.data.length === 0) return 0
  return Math.max(...props.data.map(item => item.value))
})

const sortedData = computed(() => {
  if (!props.data) return []
  return [...props.data].sort((a, b) => b.value - a.value)
})

const mapRegions = computed(() => {
  if (!props.data) return []
  
  return props.data.map(item => {
    const locationName = item.location || item.label || 'Unknown'
    const coords = locationCoordinates[locationName] || { x: 50, y: 50 }
    const scale = Math.max(0.5, (item.value / maxValue.value))
    
    return {
      name: locationName,
      value: item.value,
      x: coords.x,
      y: coords.y,
      scale
    }
  })
})

const getIntensityColor = (value: number): string => {
  if (maxValue.value === 0) return '#e9ecef'
  
  const intensity = value / maxValue.value
  const colors = [
    '#e3f2fd', // Very light blue
    '#bbdefb', // Light blue  
    '#90caf9', // Medium light blue
    '#64b5f6', // Medium blue
    '#42a5f5', // Medium dark blue
    '#2196f3', // Blue
    '#1e88e5', // Dark blue
    '#1976d2'  // Very dark blue
  ]
  
  const colorIndex = Math.min(Math.floor(intensity * colors.length), colors.length - 1)
  return colors[colorIndex]
}
</script>

<style scoped>
.map-widget {
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

.map-container {
  height: 100%;
  display: flex;
  gap: 1rem;
}

.world-map {
  flex: 2;
  position: relative;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  overflow: hidden;
}

.world-outline {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

.map-regions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.map-region {
  position: absolute;
  transform-origin: center;
}

.region-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

.marker-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.region-marker:hover .marker-dot {
  transform: scale(1.3);
}

.marker-label {
  margin-top: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  color: #495057;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 6px;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.map-legend {
  flex: 1;
  background: white;
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid #e9ecef;
  overflow-y: auto;
}

.map-legend h4 {
  margin: 0 0 1rem 0;
  font-size: 0.9rem;
  color: #495057;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
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
  border: 1px solid #e9ecef;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  color: #495057;
}

.legend-value {
  font-weight: 600;
  color: #007bff;
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
