#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo " Installing Johan Vos Substrate (2024-july) to Local Maven"
echo "=========================================================="

TEMP_DIR=$(mktemp -d)
echo "Cloning johanvos/substrate (branch: 2024-july)..."
git clone -b 2024-july --depth 1 https://github.com/johanvos/substrate.git "$TEMP_DIR"

cd "$TEMP_DIR"
chmod +x gradlew
echo "Publishing Substrate 0.0.64-SNAPSHOT to ~/.m2/repository..."
./gradlew publishToMavenLocal -x test -x javadoc

rm -rf "$TEMP_DIR"

echo "=========================================================="
echo "✅ Substrate 0.0.64-SNAPSHOT successfully installed!"
echo "   It is now available for gluonfx-maven-plugin builds."
echo "=========================================================="
