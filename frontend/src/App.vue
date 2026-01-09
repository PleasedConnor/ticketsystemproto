<template>
  <div id="app">
    <header class="header">
      <nav class="nav">
        <div class="nav-brand">
          <RouterLink to="/" class="nav-link">AI Analytics</RouterLink>
        </div>
        <div class="nav-links">
          <RouterLink to="/" class="nav-link">Home</RouterLink>
          <RouterLink to="/tickets" class="nav-link">Tickets</RouterLink>
          <RouterLink to="/analytics" class="nav-link">Analytics</RouterLink>
          <RouterLink to="/simulation" class="nav-link">Simulation</RouterLink>
          <RouterLink to="/ai-configuration" class="nav-link">AI Config</RouterLink>
          <RouterLink to="/chatbot" class="nav-link">Chatbot</RouterLink>
          <RouterLink to="/about" class="nav-link">About</RouterLink>
        </div>
      </nav>
    </header>

    <main class="main">
      <RouterView />
    </main>

    <footer class="footer">
      <p>&copy; 2024 Prototype Framework</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.nav {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-brand .nav-link {
  font-size: 1.5rem;
  font-weight: bold;
  text-decoration: none;
  color: white;
}

.nav-links {
  display: flex;
  gap: 2rem;
}

.nav-link {
  color: white;
  text-decoration: none;
  transition: opacity 0.3s;
}

.nav-link:hover {
  opacity: 0.8;
}

.nav-link.router-link-active {
  font-weight: bold;
  border-bottom: 2px solid white;
  padding-bottom: 2px;
}

.main {
  min-height: calc(100vh - 140px);
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

.footer {
  background: #f8f9fa;
  text-align: center;
  padding: 1rem;
  border-top: 1px solid #e9ecef;
  color: #6c757d;
}
</style>
