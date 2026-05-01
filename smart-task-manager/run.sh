#!/bin/bash

echo ""
echo "=================================="
echo "  Smart Task Manager - Starting"
echo "=================================="
echo ""

# Check Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+:"
    echo "   https://adoptium.net/"
    exit 1
fi

if ! command -v javac &> /dev/null; then
    echo "❌ javac not found. Please install a JDK (not just JRE):"
    echo "   https://adoptium.net/"
    exit 1
fi

echo "✅ Java found: $(java -version 2>&1 | head -1)"
echo ""

# Compile
echo "🔨 Compiling..."
mkdir -p out
javac -d out src/com/tecnomate/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Compilation failed. See errors above."
    exit 1
fi

echo "✅ Compilation successful!"
echo ""

# API key reminder
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "⚠️  ANTHROPIC_API_KEY not set."
    echo "   AI priority suggestions will default to 'medium'."
    echo "   To enable AI: export ANTHROPIC_API_KEY=sk-ant-your-key-here"
else
    echo "✅ Anthropic API key detected — AI priority suggestions enabled!"
fi

echo ""
echo "🚀 Server starting on http://localhost:8080"
echo ""
echo "   Try these in a new terminal:"
echo "   curl -X POST http://localhost:8080/tasks -H 'Content-Type: application/json' -d '{\"title\":\"Fix login bug\"}'"
echo "   curl http://localhost:8080/tasks"
echo ""
echo "   Press Ctrl+C to stop."
echo "=================================="
echo ""

java -cp out com.tecnomate.TaskManagerApp
