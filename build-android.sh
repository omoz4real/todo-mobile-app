#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo " Building TaskFlow Android Native App with GraalVM & Gluon"
echo "=========================================================="

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker is not installed or not in PATH."
    echo "Gluon Substrate requires a Linux environment to cross-compile Android native binaries."
    echo "You can either:"
    echo "  1. Start Docker Desktop and run this script again."
    echo "  2. Push your project to GitHub and let the included GitHub Actions workflow build it automatically."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "⚠️ Docker is installed but Docker Desktop is not currently running."
    echo "Starting Docker Desktop..."
    open -a Docker || true
    echo "Waiting for Docker daemon to become responsive..."
    for i in {1..30}; do
        if docker info &> /dev/null; then
            echo "✅ Docker daemon is ready!"
            break
        fi
        sleep 2
    done
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker daemon did not start. Please open Docker Desktop manually or use the GitHub Actions CI workflow."
    exit 1
fi

echo "📦 Step 1: Building Linux GraalVM Builder Image..."
docker build -t taskflow-gluon-builder -f Dockerfile.android .

echo "⚙️ Step 2: Running Ahead-of-Time Native Android Compilation..."
docker run --rm \
    -v "$SCRIPT_DIR":/workspace \
    -v "$HOME/.m2":/root/.m2 \
    taskflow-gluon-builder

echo ""
echo "=========================================================="
echo "✅ Android Native Build Completed!"
echo "APK Output location:"
ls -lh target/gluonfx/aarch64-android/gvm/*.apk 2>/dev/null || echo "Check target/gluonfx/aarch64-android/gvm/"
echo "=========================================================="
echo "To deploy to your phone, run: ./deploy-to-phone.sh"
