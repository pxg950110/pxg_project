#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EMBULK_JAR="$SCRIPT_DIR/embulk.jar"

if [ -f "$EMBULK_JAR" ]; then
    echo "Embulk already installed at $EMBULK_JAR"
    java -jar "$EMBULK_JAR" --version
    exit 0
fi

echo "Downloading Embulk v0.11.5..."
curl -L https://github.com/embulk/embulk/releases/download/v0.11.5/embulk-0.11.5.jar -o "$EMBULK_JAR"

echo "Installing PostgreSQL output plugin..."
java -jar "$EMBULK_JAR" gem install embulk-output-postgresql

echo "Embulk installation complete."
java -jar "$EMBULK_JAR" --version
