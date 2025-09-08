#!/bin/bash

# Development startup script
# This script starts both backend and frontend in development mode

set -e

echo "🚀 Starting development servers..."

# Function to cleanup processes on exit
cleanup() {
    echo ""
    echo "🛑 Shutting down development servers..."
    kill $(jobs -p) 2>/dev/null || true
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

# Start database if Docker is available
if command -v docker >/dev/null 2>&1 && command -v docker-compose >/dev/null 2>&1; then
    echo "🐳 Starting development database..."
    docker-compose -f docker-compose.dev.yml up -d
    echo "✅ Database started on localhost:5432"
    echo "   Database: prototype"
    echo "   Username: prototype"
    echo "   Password: password"
    echo ""
fi

# Start backend
echo "☕ Starting Java backend on port 8080..."
cd backend
./mvnw spring-boot:run &
BACKEND_PID=$!
cd ..

# Wait a moment for backend to start
sleep 3

# Start frontend
echo "🌐 Starting Vue frontend on port 3000..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ Development servers started!"
echo ""
echo "🌐 Frontend: http://localhost:3000"
echo "☕ Backend:  http://localhost:8080/api"
echo "💾 Database: http://localhost:8080/h2-console (if using H2)"
echo "🔍 Health:   http://localhost:8080/api/public/health"
echo ""
echo "Press Ctrl+C to stop all servers"

# Wait for processes
wait $BACKEND_PID $FRONTEND_PID
