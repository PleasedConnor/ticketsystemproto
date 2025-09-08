#!/bin/bash

# Clean script to remove build artifacts and dependencies
# Use this to reset the project to a clean state

set -e

echo "🧹 Cleaning prototype framework..."

# Clean backend
echo "☕ Cleaning Java backend..."
cd backend
if [ -f mvnw ]; then
    ./mvnw clean
fi
rm -rf target/
echo "✅ Backend cleaned"
cd ..

# Clean frontend
echo "🌐 Cleaning Vue frontend..."
cd frontend
rm -rf node_modules/
rm -rf dist/
rm -f package-lock.json
echo "✅ Frontend cleaned"
cd ..

# Clean Docker
if command -v docker >/dev/null 2>&1; then
    echo "🐳 Cleaning Docker resources..."
    docker-compose down --volumes --remove-orphans 2>/dev/null || true
    docker-compose -f docker-compose.dev.yml down --volumes --remove-orphans 2>/dev/null || true
    
    # Remove project-specific images (optional)
    read -p "Remove Docker images for this project? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker image rm prototype-framework-backend 2>/dev/null || true
        docker image rm prototype-framework-frontend 2>/dev/null || true
        echo "✅ Docker images removed"
    fi
fi

echo ""
echo "🎉 Project cleaned!"
echo ""
echo "To set up again, run:"
echo "   ./scripts/setup.sh"
