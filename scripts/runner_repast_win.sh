#!/usr/bin/env bash
set -euo pipefail

# === Config ===
MODEL_NAME="test250930"
SRC_DIR="src/$MODEL_NAME"
BIN_DIR="bin/$MODEL_NAME"
PARAMS_FILE="$(pwd -W)\\${MODEL_NAME}.rs\\batch_params.xml"
SCENARIO_DIR="$(pwd -W)\\${MODEL_NAME}.rs"

# === 1. Classpath generation ===
echo "[INFO] Generating Repast classpath..."
CLASSPATH=$(./scripts/repast_classpath.sh)
CLASSPATH_WIN=$(echo "$CLASSPATH" | tr ':' ';')

# === 2. Compile sources ===
echo "[INFO] Compiling sources into $BIN_DIR ..."
mkdir -p "$BIN_DIR"
find "$SRC_DIR" -name "*.java" > sources.txt
javac -cp "$CLASSPATH_WIN" -d "$BIN_DIR" @sources.txt

# === 3. Run batch simulation ===
echo "[INFO] Launching Repast batch simulation..."
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  -cp "bin;$CLASSPATH_WIN" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$PARAMS_FILE" \
  "$SCENARIO_DIR"
