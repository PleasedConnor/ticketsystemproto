#!/bin/bash

# Prototype Framework Setup Script
# This script sets up the development environment for the prototype framework

set -e

echo "🚀 Setting up Prototype Framework..."

# Check if Java 21 is installed
echo "📋 Checking Java version..."
if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo "✅ Java $JAVA_VERSION found"
    else
        echo "❌ Java 21 or higher is required. Current version: $JAVA_VERSION"
        echo "Please install Java 21 from: https://adoptium.net/"
        exit 1
    fi
else
    echo "❌ Java not found. Please install Java 21 from: https://adoptium.net/"
    exit 1
fi

# Check if Node.js is installed
echo "📋 Checking Node.js version..."
if command -v node >/dev/null 2>&1; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        echo "✅ Node.js v$(node -v | cut -d'v' -f2) found"
    else
        echo "❌ Node.js 18 or higher is required. Current version: v$(node -v | cut -d'v' -f2)"
        echo "Please install Node.js 18+ from: https://nodejs.org/"
        exit 1
    fi
else
    echo "❌ Node.js not found. Please install Node.js 18+ from: https://nodejs.org/"
    exit 1
fi

# Check if Docker is installed
echo "📋 Checking Docker..."
if command -v docker >/dev/null 2>&1; then
    echo "✅ Docker found"
else
    echo "⚠️  Docker not found. Install Docker from: https://docker.com/"
    echo "Docker is optional but recommended for production deployment"
fi

# Setup backend
echo "🔧 Setting up backend..."
cd backend

# Make mvnw executable
chmod +x mvnw

# Download dependencies
echo "📦 Downloading Maven dependencies..."
./mvnw dependency:go-offline -q

echo "✅ Backend setup complete"

# Setup frontend
echo "🔧 Setting up frontend..."
cd ../frontend

# Install npm dependencies
echo "📦 Installing npm dependencies..."
npm install

echo "✅ Frontend setup complete"

# Go back to root
cd ..

# Create .env files if they don't exist
echo "📝 Creating environment files..."

if [ ! -f backend/.env ]; then
    cat > backend/.env << EOF
# Backend Environment Variables
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:h2:mem:testdb
DATABASE_USERNAME=sa
DATABASE_PASSWORD=password
EOF
    echo "✅ Created backend/.env"
fi

if [ ! -f frontend/.env ]; then
    cat > frontend/.env << EOF
# Frontend Environment Variables
VITE_API_BASE_URL=http://localhost:8080/api
EOF
    echo "✅ Created frontend/.env"
fi

# Make scripts executable
chmod +x scripts/*.sh

echo ""
echo "🎉 Setup complete!"
echo ""
echo "🚀 To start development:"
echo "   ./scripts/dev.sh"
echo ""
echo "🐳 To start with Docker:"
echo "   docker-compose up -d"
echo ""
echo "📚 Check README.md for more information"
