<template>
  <div class="about">
    <div class="container">
      <h1>About This Prototype Scaffold</h1>
      
      <div class="content">
        <section class="section">
          <h2>🚀 Purpose</h2>
          <p>
            This scaffold provides a solid foundation for rapid prototyping of full-stack applications.
            It combines modern Java backend development with a reactive Vue.js frontend, giving you
            everything you need to build and deploy web applications quickly.
          </p>
        </section>

        <section class="section">
          <h2>🏗️ Architecture</h2>
          <div class="architecture">
            <div class="arch-item">
              <h3>Backend (Java 21)</h3>
              <ul>
                <li>Spring Boot 3.2+ for rapid development</li>
                <li>Spring Security for authentication</li>
                <li>Spring Data JPA for database operations</li>
                <li>H2 database for development</li>
                <li>PostgreSQL support for production</li>
                <li>Maven for dependency management</li>
              </ul>
            </div>
            
            <div class="arch-item">
              <h3>Frontend (Vue 3)</h3>
              <ul>
                <li>Vue 3 with Composition API</li>
                <li>TypeScript for type safety</li>
                <li>Vite for fast development builds</li>
                <li>Vue Router for navigation</li>
                <li>Pinia for state management</li>
                <li>Axios for API communication</li>
              </ul>
            </div>
          </div>
        </section>

        <section class="section">
          <h2>🛠️ Development Features</h2>
          <ul class="features-list">
            <li>Hot reload for both frontend and backend</li>
            <li>CORS configured for local development</li>
            <li>Proxy setup for API calls</li>
            <li>ESLint and Prettier for code quality</li>
            <li>Docker support for containerization</li>
            <li>Health check endpoints</li>
            <li>Actuator endpoints for monitoring</li>
          </ul>
        </section>

        <section class="section">
          <h2>📁 Project Structure</h2>
          <pre class="code-block">
prototype-framework/
├── backend/                 # Java Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration files
│   └── pom.xml             # Maven configuration
├── frontend/               # Vue 3 application
│   ├── src/                # Vue source code
│   ├── public/             # Static assets
│   └── package.json        # NPM configuration
├── docker/                 # Docker configurations
└── scripts/                # Development scripts
          </pre>
        </section>

        <section class="section">
          <h2>🚦 Getting Started</h2>
          <p>
            Check the README.md file for detailed setup instructions. The scaffold includes
            scripts to help you get up and running quickly with both the backend and frontend.
          </p>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.about {
  padding: 2rem 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  margin-bottom: 3rem;
  color: #495057;
}

.section {
  margin-bottom: 3rem;
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.section h2 {
  color: #495057;
  margin-bottom: 1rem;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
}

.architecture {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-top: 1rem;
}

.arch-item {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.arch-item h3 {
  color: #495057;
  margin-bottom: 1rem;
}

.arch-item ul {
  margin: 0;
  padding-left: 1.5rem;
}

.arch-item li {
  margin-bottom: 0.5rem;
  color: #6c757d;
}

.features-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 0.5rem;
  list-style: none;
  padding: 0;
}

.features-list li {
  background: #f8f9fa;
  padding: 0.75rem;
  border-radius: 5px;
  border-left: 3px solid #667eea;
}

.code-block {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 5px;
  border: 1px solid #dee2e6;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.4;
  color: #495057;
}
</style>
