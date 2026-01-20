<template>
  <div id="app">
    <header class="header">
      <div class="header-content">
        <RouterLink to="/" class="nav-brand">Demo Platform</RouterLink>
      </div>
    </header>

    <div class="app-layout">
      <aside class="sidebar">
        <nav class="sidebar-nav">
          <RouterLink to="/" class="nav-link">
            <span>Home</span>
          </RouterLink>
          <RouterLink to="/tickets" class="nav-link">
            <span>Tickets</span>
          </RouterLink>
          <RouterLink to="/analytics" class="nav-link">
            <span>Analytics</span>
          </RouterLink>
          <div class="nav-group">
            <div 
              class="nav-link nav-group-header" 
              :class="{ 'nav-group-active': isAIRoute(route.name as string) }"
              @click="aiExpanded = !aiExpanded"
            >
              <span>AI</span>
              <span class="nav-group-arrow" :class="{ 'expanded': aiExpanded }">▼</span>
            </div>
            <div v-if="aiExpanded" class="nav-submenu">
              <RouterLink to="/simulation" class="nav-link nav-submenu-link">
                <span>Simulation</span>
              </RouterLink>
              <div class="nav-subgroup">
                <div 
                  class="nav-link nav-submenu-link nav-subgroup-header"
                  :class="{ 'nav-group-active': route.name === 'ai-customisation' || route.name === 'ai-data-access' }"
                  @click.stop="aiConfigExpanded = !aiConfigExpanded"
                >
                  <span>AI Config</span>
                  <span class="nav-group-arrow" :class="{ 'expanded': aiConfigExpanded }">▼</span>
                </div>
                <div v-if="aiConfigExpanded" class="nav-submenu-level2">
                  <RouterLink to="/ai-customisation" class="nav-link nav-submenu-link nav-submenu-level2-link">
                    <span>AI Customisation</span>
                  </RouterLink>
                  <RouterLink to="/ai-data-access" class="nav-link nav-submenu-link nav-submenu-level2-link">
                    <span>AI Data Access</span>
                  </RouterLink>
                </div>
              </div>
              <RouterLink to="/chatbot" class="nav-link nav-submenu-link">
                <span>Chatbot</span>
              </RouterLink>
            </div>
          </div>
          <RouterLink to="/knowledge-base" class="nav-link">
            <span>Knowledge Base</span>
          </RouterLink>
          <div class="nav-group">
            <div 
              class="nav-link nav-group-header" 
              :class="{ 'nav-group-active': isMetadataRoute(route.name as string) }"
              @click="metadataExpanded = !metadataExpanded"
            >
              <span>Metadata Configuration</span>
              <span class="nav-group-arrow" :class="{ 'expanded': metadataExpanded }">▼</span>
            </div>
            <div v-if="metadataExpanded" class="nav-submenu">
              <RouterLink to="/metadata-configuration/inbound" class="nav-link nav-submenu-link">
                <span>Inbound Metadata</span>
              </RouterLink>
              <RouterLink to="/metadata-configuration/chat-fields" class="nav-link nav-submenu-link">
                <span>Chat Fields</span>
              </RouterLink>
              <RouterLink to="/metadata-configuration/ticket-fields" class="nav-link nav-submenu-link">
                <span>Ticket Fields</span>
              </RouterLink>
            </div>
          </div>
          <RouterLink to="/about" class="nav-link">
            <span>About</span>
          </RouterLink>
        </nav>
      </aside>

      <main class="main">
        <RouterView />
      </main>
    </div>

    <footer class="footer">
      <p>&copy; 2024 Prototype Framework</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const aiExpanded = ref(false)
const aiConfigExpanded = ref(false)
const metadataExpanded = ref(false)

// Check if current route is an AI route
const isAIRoute = (routeName: string | null): boolean => {
  if (!routeName) return false
  const aiRoutes = ['simulation', 'ai-customisation', 'ai-data-access', 'chatbot']
  return aiRoutes.includes(routeName)
}

// Check if current route is a Metadata route
const isMetadataRoute = (routeName: string | null): boolean => {
  if (!routeName) return false
  const metadataRoutes = ['inbound-metadata', 'chat-fields', 'ticket-fields', 'metadata-mapping']
  return metadataRoutes.includes(routeName)
}

// Auto-expand submenus if on one of their pages
const checkActiveMenus = () => {
  if (isAIRoute(route.name as string)) {
    aiExpanded.value = true
  }
  // Auto-expand AI Config submenu if on one of its pages
  const aiConfigRoutes = ['ai-customisation', 'ai-data-access']
  if (aiConfigRoutes.includes(route.name as string)) {
    aiConfigExpanded.value = true
  }
  // Auto-expand Metadata submenu if on one of its pages
  if (isMetadataRoute(route.name as string)) {
    metadataExpanded.value = true
  }
}

// Watch route changes
watch(() => route.name, () => {
  checkActiveMenus()
}, { immediate: true })
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 50%, #8b0000 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  padding: 0 2rem;
  display: flex;
  align-items: center;
}

.nav-brand {
  font-size: 1.5rem;
  font-weight: bold;
  text-decoration: none;
  color: white;
  transition: opacity 0.3s;
}

.nav-brand:hover {
  opacity: 0.8;
}

.app-layout {
  display: flex;
  min-height: calc(100vh - 140px);
}

.sidebar {
  width: 250px;
  background: #ffffff;
  border-right: 1px solid #e9ecef;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 60px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  padding: 1rem 0;
}

.sidebar-nav .nav-link {
  display: block;
  padding: 0.75rem 1.5rem;
  color: #495057;
  text-decoration: none;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.sidebar-nav .nav-link:hover {
  background: #f8f9fa;
  color: #dc3545;
  border-left-color: #dc3545;
}

.sidebar-nav .nav-link.router-link-active {
  background: #fff5f5;
  color: #dc3545;
  font-weight: 600;
  border-left-color: #dc3545;
}

/* Sub-menu styles */
.nav-group {
  display: flex;
  flex-direction: column;
}

.nav-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.nav-group-header.nav-group-active {
  background: #fff5f5;
  color: #dc3545;
  font-weight: 600;
  border-left-color: #dc3545;
}

.nav-group-arrow {
  font-size: 0.75rem;
  transition: transform 0.2s;
  color: #6c757d;
}

.nav-group-arrow.expanded {
  transform: rotate(180deg);
}

.nav-submenu {
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  border-left: 3px solid #dc3545;
  margin-left: 1.5rem;
}

.nav-submenu-link {
  padding-left: 2rem !important;
  font-size: 0.9rem;
}

.nav-submenu-link.router-link-active {
  background: #fff5f5;
  color: #dc3545;
  font-weight: 600;
}

/* Nested sub-menu (level 2) */
.nav-subgroup {
  display: flex;
  flex-direction: column;
}

.nav-subgroup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.nav-submenu-level2 {
  display: flex;
  flex-direction: column;
  background: #f0f0f0;
  border-left: 3px solid #dc3545;
  margin-left: 1rem;
}

.nav-submenu-level2-link {
  padding-left: 2.5rem !important;
  font-size: 0.85rem;
}

.main {
  flex: 1;
  min-height: calc(100vh - 140px);
  overflow-x: auto;
}

/* Default layout for non-analytics pages */
.main > :not(.analytics-container) {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* Full width for analytics */
.main > .analytics-container {
  width: 100%;
  padding: 0;
}

/* Responsive: Hide sidebar on mobile */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .main {
    width: 100%;
  }
}

.footer {
  background: #f8f9fa;
  text-align: center;
  padding: 1rem;
  border-top: 1px solid #e9ecef;
  color: #6c757d;
}
</style>
