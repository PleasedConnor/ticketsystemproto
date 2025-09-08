# Prototype Framework

A modern, full-stack scaffold for rapid prototyping with **Java 21** backend and **Vue 3** frontend.

## 🚀 Quick Start

```bash
# Clone or download this scaffold
git clone <your-repo> my-new-project
cd my-new-project

# Setup the development environment
./scripts/setup.sh

# Start development servers
./scripts/dev.sh
```

Visit [http://localhost:3000](http://localhost:3000) to see your application running!

## 📋 Prerequisites

- **Java 21+** - [Download from Adoptium](https://adoptium.net/)
- **Node.js 18+** - [Download from nodejs.org](https://nodejs.org/)
- **Docker** (optional) - [Download from docker.com](https://docker.com/)

## 🏗️ Architecture

### Backend (Java 21 + Spring Boot)
- **Spring Boot 3.2+** - Modern Java framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database operations
- **H2 Database** - In-memory database for development
- **PostgreSQL** - Production database
- **Maven** - Dependency management
- **Java 21** - Latest LTS Java version

### Frontend (Vue 3 + TypeScript)
- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool and dev server
- **Vue Router** - Client-side routing
- **Pinia** - State management
- **Axios** - HTTP client
- **ESLint + Prettier** - Code quality tools

## 📁 Project Structure

```
prototype-framework/
├── backend/                    # Java Spring Boot application
│   ├── src/main/java/         # Java source code
│   │   └── com/prototype/     # Main package
│   │       ├── config/        # Configuration classes
│   │       └── controller/    # REST controllers
│   ├── src/main/resources/    # Configuration files
│   ├── pom.xml               # Maven dependencies
│   └── Dockerfile            # Docker configuration
├── frontend/                  # Vue 3 application
│   ├── src/                  # Vue source code
│   │   ├── components/       # Reusable components
│   │   ├── views/            # Page components
│   │   ├── router/           # Route definitions
│   │   ├── composables/      # Composition API logic
│   │   └── assets/           # Static assets
│   ├── package.json          # NPM dependencies
│   ├── vite.config.ts        # Vite configuration
│   ├── Dockerfile            # Docker configuration
│   └── nginx.conf            # Nginx configuration
├── database/                  # Database scripts
│   └── init.sql              # Initial database setup
├── scripts/                   # Development scripts
│   ├── setup.sh              # Initial setup
│   ├── dev.sh                # Start development servers
│   ├── build.sh              # Build for production
│   ├── clean.sh              # Clean build artifacts
│   └── test.sh               # Run tests
├── docker-compose.yml         # Full production stack
├── docker-compose.dev.yml     # Development database only
└── README.md                  # This file
```

## 🛠️ Development

### Starting Development Servers

The easiest way to start development is using the provided script:

```bash
./scripts/dev.sh
```

This will:
1. Start a PostgreSQL database (if Docker is available)
2. Start the Java backend on port 8080
3. Start the Vue frontend on port 3000

### Manual Development Setup

If you prefer to start services manually:

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm run dev
```

#### Database (Optional)
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### Available URLs

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/api/public/health
- **H2 Console**: http://localhost:8080/h2-console (dev mode only)
- **API Documentation**: http://localhost:8080/swagger-ui.html (if enabled)

## 🔧 Configuration

### Backend Configuration

The backend uses Spring profiles for different environments:

- **dev** (default): Uses H2 in-memory database
- **prod**: Uses PostgreSQL database

Configuration files:
- `backend/src/main/resources/application.yml` - Main configuration
- `backend/.env` - Environment variables (created by setup script)

### Frontend Configuration

The frontend uses Vite for configuration:

- `frontend/vite.config.ts` - Vite configuration
- `frontend/.env` - Environment variables (created by setup script)

## 🐳 Docker Deployment

### Development (Database Only)
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### Full Production Stack
```bash
# Build and start all services
docker-compose up -d

# Or build first, then start
./scripts/build.sh
docker-compose up -d
```

### Individual Services
```bash
# Build backend image
docker build -t prototype-backend ./backend

# Build frontend image
docker build -t prototype-frontend ./frontend
```

## 🧪 Testing

Run all tests:
```bash
./scripts/test.sh
```

Or run individually:

### Backend Tests
```bash
cd backend
./mvnw test
```

### Frontend Linting & Type Checking
```bash
cd frontend
npm run lint
npm run type-check
```

## 🏗️ Building for Production

```bash
./scripts/build.sh
```

This creates:
- Backend JAR: `backend/target/*.jar`
- Frontend assets: `frontend/dist/`

## 🧹 Cleaning

To clean all build artifacts and dependencies:
```bash
./scripts/clean.sh
```

## 🔒 Security Features

### Backend
- Spring Security configuration
- CORS enabled for development
- JWT ready (uncomment in `SecurityConfig.java`)
- SQL injection prevention via JPA
- Input validation with Bean Validation

### Frontend
- TypeScript for type safety
- ESLint for code quality
- Content Security Policy headers (in nginx.conf)
- XSS protection headers

## 🌍 Environment Variables

### Backend (.env)
```bash
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://localhost:5432/prototype
DATABASE_USERNAME=prototype
DATABASE_PASSWORD=password
```

### Frontend (.env)
```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## 📚 Adding Features

### Backend (Java)
1. Add dependencies to `pom.xml`
2. Create entities in `com.prototype.entity`
3. Create repositories in `com.prototype.repository`
4. Create services in `com.prototype.service`
5. Create controllers in `com.prototype.controller`

### Frontend (Vue)
1. Add dependencies: `npm install <package>`
2. Create components in `src/components/`
3. Create views in `src/views/`
4. Add routes in `src/router/index.ts`
5. Add API calls in `src/composables/useBackendApi.ts`

## 🔧 Customization

### Changing Project Name
1. Update `pom.xml` - change `<artifactId>` and `<name>`
2. Update `package.json` - change `name`
3. Update `docker-compose.yml` - change container names
4. Update Java package names and folder structure

### Database Migration
1. For PostgreSQL: Update connection details in `application.yml`
2. For other databases: Add driver dependency to `pom.xml`
3. Update Docker configuration if needed

### Adding Authentication
1. Uncomment JWT configuration in `SecurityConfig.java`
2. Create user entity and repository
3. Implement authentication endpoints
4. Add token handling in frontend API client

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./scripts/test.sh`
5. Submit a pull request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🆘 Troubleshooting

### Common Issues

**Java version error**
- Ensure Java 21+ is installed and `JAVA_HOME` is set correctly

**Port already in use**
- Backend (8080): `lsof -ti:8080 | xargs kill -9`
- Frontend (3000): `lsof -ti:3000 | xargs kill -9`

**Database connection failed**
- Check if PostgreSQL is running: `docker-compose -f docker-compose.dev.yml ps`
- Restart database: `docker-compose -f docker-compose.dev.yml restart`

**Frontend build fails**
- Clear node modules: `rm -rf frontend/node_modules && cd frontend && npm install`

**Docker issues**
- Clean Docker resources: `./scripts/clean.sh`

### Getting Help

1. Check the logs: `docker-compose logs <service-name>`
2. Verify health endpoints:
   - Backend: http://localhost:8080/api/public/health
   - Frontend: http://localhost:3000/health
3. Check if all prerequisites are installed: `./scripts/setup.sh`

---

**Happy Prototyping! 🚀**
