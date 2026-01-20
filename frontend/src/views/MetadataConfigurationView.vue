<template>
  <div class="metadata-config-container">
    <div class="content">
      <header class="page-header">
        <h1>Metadata Configuration</h1>
        <p class="subtitle">Manage metadata connections and field configurations</p>
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
          <!-- Inbound Metadata Tab -->
          <div v-show="activeTab === 'inbound'" class="tab-panel">
            <InboundMetadataView />
          </div>

          <!-- Chat Fields Tab -->
          <div v-show="activeTab === 'chat'" class="tab-panel">
            <ChatFieldsView />
          </div>

          <!-- Ticket Fields Tab -->
          <div v-show="activeTab === 'ticket'" class="tab-panel">
            <TicketFieldsView />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import InboundMetadataView from '@/components/metadata/InboundMetadataView.vue'
import ChatFieldsView from '@/components/metadata/ChatFieldsView.vue'
import TicketFieldsView from '@/components/metadata/TicketFieldsView.vue'

const activeTab = ref('inbound')

const tabs = [
  { id: 'inbound', label: 'Inbound Metadata' },
  { id: 'chat', label: 'Chat Fields' },
  { id: 'ticket', label: 'Ticket Fields' }
]
</script>

<style scoped>
.metadata-config-container {
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
  display: flex;
  flex-direction: column;
  min-height: 600px;
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
  color: #dc3545;
  border-bottom-color: #dc3545;
  background: white;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}

.tab-panel {
  height: 100%;
  overflow-y: auto;
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
</style>
