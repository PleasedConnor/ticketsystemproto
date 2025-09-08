<template>
  <div class="home">
    <div class="hero">
      <h1 class="hero-title">Welcome to Your Prototype</h1>
      <p class="hero-subtitle">
        A modern full-stack scaffold with Java 21 backend and Vue 3 frontend
      </p>
      
      <div class="status-cards">
        <div class="status-card">
          <h3>Backend Status</h3>
          <div v-if="backendStatus.loading" class="loading">Checking...</div>
          <div v-else-if="backendStatus.online" class="status online">
            ✅ Online - {{ backendStatus.data?.service }}
          </div>
          <div v-else class="status offline">❌ Offline</div>
        </div>
        
        <div class="status-card">
          <h3>Frontend Status</h3>
          <div class="status online">✅ Online - Vue 3 + TypeScript</div>
        </div>
      </div>

      <div class="tech-stack">
        <h3>Tech Stack</h3>
        <div class="tech-grid">
          <div class="tech-item">
            <strong>Backend:</strong> Java 21, Spring Boot 3, Maven
          </div>
          <div class="tech-item">
            <strong>Frontend:</strong> Vue 3, TypeScript, Vite
          </div>
          <div class="tech-item">
            <strong>Database:</strong> H2 (dev), PostgreSQL (prod)
          </div>
          <div class="tech-item">
            <strong>Tools:</strong> Docker, ESLint, Prettier
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

interface BackendStatus {
  loading: boolean
  online: boolean
  data?: {
    status: string
    service: string
    timestamp: string
  }
}

const backendStatus = ref<BackendStatus>({
  loading: true,
  online: false
})

const { checkHealth } = useBackendApi()

onMounted(async () => {
  try {
    const response = await checkHealth()
    backendStatus.value = {
      loading: false,
      online: true,
      data: response.data
    }
  } catch (error) {
    backendStatus.value = {
      loading: false,
      online: false
    }
  }
})
</script>

<style scoped>
.home {
  padding: 2rem 0;
}

.hero {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3rem;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.2rem;
  color: #6c757d;
  margin-bottom: 3rem;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
}

.status-card {
  background: white;
  border-radius: 10px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border: 1px solid #e9ecef;
}

.status-card h3 {
  margin: 0 0 1rem 0;
  color: #495057;
}

.status {
  font-weight: bold;
  padding: 0.5rem 1rem;
  border-radius: 5px;
}

.status.online {
  background: #d4edda;
  color: #155724;
}

.status.offline {
  background: #f8d7da;
  color: #721c24;
}

.loading {
  color: #6c757d;
  font-style: italic;
}

.tech-stack {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 2rem;
  margin-top: 2rem;
}

.tech-stack h3 {
  margin: 0 0 1.5rem 0;
  color: #495057;
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.tech-item {
  background: white;
  padding: 1rem;
  border-radius: 5px;
  border: 1px solid #dee2e6;
}

.tech-item strong {
  color: #495057;
}
</style>
