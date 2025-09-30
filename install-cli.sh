#!/bin/bash

# URL Scanner CLI Installer

set -e

echo "🚀 Installing URL Scanner CLI..."

# Check if running as root for system-wide install
if [ "$EUID" -eq 0 ]; then
    INSTALL_DIR="/usr/local/bin"
    echo "Installing system-wide to $INSTALL_DIR"
else
    INSTALL_DIR="$HOME/.local/bin"
    echo "Installing for current user to $INSTALL_DIR"
    mkdir -p "$INSTALL_DIR"
fi

# Copy the CLI script
cp urlscanner-cli.sh "$INSTALL_DIR/urlscanner"
chmod +x "$INSTALL_DIR/urlscanner"

echo "✅ CLI installed successfully!"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo ""
    echo "⚠️  Warning: jq is not installed (required for JSON parsing)"
    echo ""
    echo "Install it with:"
    echo "  macOS:         brew install jq"
    echo "  Ubuntu/Debian: sudo apt-get install jq"
    echo "  CentOS/RHEL:   sudo yum install jq"
    echo ""
fi

# Check if install directory is in PATH
if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
    echo ""
    echo "⚠️  Warning: $INSTALL_DIR is not in your PATH"
    echo ""
    echo "Add it to your PATH by adding this line to your ~/.bashrc or ~/.zshrc:"
    echo "  export PATH=\"\$PATH:$INSTALL_DIR\""
    echo ""
    echo "Then run: source ~/.bashrc  (or ~/.zshrc)"
else
    echo ""
    echo "✅ All set! Try it out:"
    echo "  urlscanner help"
fi