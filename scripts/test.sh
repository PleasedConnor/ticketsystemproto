#!/bin/bash

# Test script to run all tests
# This script runs tests for both backend and frontend

set -e

echo "🧪 Running tests for prototype framework..."

# Test backend
echo "☕ Running Java tests..."
cd backend
./mvnw test
echo "✅ Backend tests passed"
cd ..

# Test frontend
echo "🌐 Running frontend linting..."
cd frontend
npm run lint
echo "✅ Frontend linting passed"

echo "🔍 Type checking..."
npm run type-check
echo "✅ Type checking passed"
cd ..

echo ""
echo "🎉 All tests passed!"
