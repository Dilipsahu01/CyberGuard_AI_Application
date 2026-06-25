#!/usr/bin/env bash
# Makefile-style convenience scripts for the CyberGuard AI project

set -e

TARGET=${1:-help}

case "$TARGET" in
  server)
    echo "[server] Starting Go Swarm Server on :8080..."
    cd server && go run ./main.go
    ;;
  test-server)
    echo "[test] Running Go tests..."
    cd server && go test ./... -v -count=1
    ;;
  build-server)
    echo "[build] Building Go server binary..."
    cd server && go build -o cyberguard-server ./...
    echo "[build] Done: server/cyberguard-server"
    ;;
  tidy)
    echo "[tidy] Running go mod tidy..."
    cd server && go mod tidy
    ;;
  android-debug)
    echo "[android] Building debug APK..."
    ./gradlew assembleDebug
    ;;
  android-test)
    echo "[android] Running connected Android tests..."
    ./gradlew connectedAndroidTest
    ;;
  clean)
    echo "[clean] Cleaning build artifacts..."
    ./gradlew clean
    cd server && rm -f cyberguard-server
    ;;
  help|*)
    echo ""
    echo "Usage: ./scripts/dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  server          Start the Go Swarm Server locally"
    echo "  test-server     Run Go unit tests"
    echo "  build-server    Build Go server binary"
    echo "  tidy            Run go mod tidy"
    echo "  android-debug   Build debug Android APK"
    echo "  android-test    Run instrumented Android tests"
    echo "  clean           Clean all build artifacts"
    echo ""
    ;;
esac
