#!/usr/bin/env bash
set -euo pipefail

MODEL_NAME="test250930"
SRC_DIR="src/$MODEL_NAME"
BIN_DIR="bin/$MODEL_NAME"
PARAMS_FILE="$(pwd -W)\\${MODEL_NAME}.rs\\batch_params.xml"
SCENARIO_DIR="$(pwd -W)\\${MODEL_NAME}.rs"

# === Spinner ===
spinner() {
  local spin_chars=("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
  local i=0
  while :; do
    printf "\r${spin_chars[i]} Running..."
    i=$(((i + 1) % 4))
    sleep 0.1
  done
}

# === 1. Classpath generation ===
echo "[INFO] Generating Repast classpath..."
spinner &   # start spinner in background
SPIN_PID=$!
CLASSPATH=$(./scripts/repast_classpath.sh)
CLASSPATH_WIN=$(echo "$CLASSPATH" | tr ':' ';')

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Classpath generated!"
# === 2. Compile sources ===
echo "[INFO] Compiling sources into $BIN_DIR ..."
spinner &   # start spinner in background
SPIN_PID=$!
mkdir -p "$BIN_DIR"
find "$SRC_DIR" -name "*.java" > sources.txt
javac -cp "$CLASSPATH_WIN" -d "$BIN_DIR" @sources.txt

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Sources compiled!"

# === 3. Run batch simulation ===
echo "[INFO] Launching Repast batch simulation..."

spinner &   # start spinner in background
SPIN_PID=$!

# Run the Java command
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  -cp "bin;$CLASSPATH_WIN" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$PARAMS_FILE" \
  "$SCENARIO_DIR"

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Simulation finished!"
