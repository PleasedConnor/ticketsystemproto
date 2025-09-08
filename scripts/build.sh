#!/bin/bash

# Build script for production deployment
# This script builds both backend and frontend for production

set -e

echo "🏗️  Building prototype framework for production..."

# Build backend
echo "☕ Building Java backend..."
cd backend
./mvnw clean package -DskipTests
echo "✅ Backend built successfully"
cd ..

# Build frontend
echo "🌐 Building Vue frontend..."
cd frontend
npm run build
echo "✅ Frontend built successfully"
cd ..

echo ""
echo "🎉 Build complete!"
echo ""
echo "📦 Artifacts:"
echo "   Backend JAR: backend/target/*.jar"
echo "   Frontend:    frontend/dist/"
echo ""
echo "🐳 To build Docker images:"
echo "   docker-compose build"
echo ""
echo "🚀 To deploy:"
echo "   docker-compose up -d"
