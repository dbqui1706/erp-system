#!/usr/bin/env bash
# ==============================================================================
# Helper Script: Configure Git to use versioned .githooks directory
# ==============================================================================

set -eo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.githooks"
GIT_DIR="$REPO_ROOT/.git"

echo "🔧 Configuring Git hooks path for repository..."

if [ ! -d "$GIT_DIR" ]; then
    echo "⚠️ Warning: .git directory not found in $REPO_ROOT."
    echo "Run 'git init' first if you intend to enable local repository hooks."
    exit 0
fi

if [ ! -d "$HOOKS_DIR" ]; then
    echo "❌ Error: .githooks directory not found at $HOOKS_DIR."
    exit 1
fi

chmod +x "$HOOKS_DIR"/*

git -C "$REPO_ROOT" config core.hooksPath .githooks

echo "✅ Git configured successfully: core.hooksPath = .githooks"
echo "   Active hooks: $(ls "$HOOKS_DIR" | tr '\n' ' ')"
