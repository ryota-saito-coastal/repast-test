#!/usr/bin/env bash
set -euo pipefail

MODEL_NAME="test250930"
SRC_DIR="src/$MODEL_NAME"
BIN_DIR="bin/$MODEL_NAME"

to_windows_path() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "$1"
  elif command -v pwd >/dev/null 2>&1 && [[ "$1" == "$PWD"* ]]; then
    # Fallback for shells without cygpath; best-effort by reusing pwd output.
    printf '%s\n' "$(pwd -W 2>/dev/null || pwd)${1#"$PWD"}"
  else
    printf '%s\n' "$1"
  fi
}

PARAMS_FILE=$(to_windows_path "$PWD/${MODEL_NAME}.rs/batch_params.xml")
SCENARIO_DIR=$(to_windows_path "$PWD/${MODEL_NAME}.rs")
BIN_DIR_WIN=$(to_windows_path "$PWD/$BIN_DIR")
LOG_DIR_POSIX="${MODEL_NAME}.rs/logs"
LOG_DIR_WIN=$(to_windows_path "$PWD/$LOG_DIR_POSIX")

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
IFS=':' read -r -a CP_ARRAY <<< "$CLASSPATH"
CONVERTED_CP=()
for entry in "${CP_ARRAY[@]}"; do
  [[ -z "$entry" ]] && continue
  CONVERTED_CP+=("$(to_windows_path "$entry")")
done
CLASSPATH_WIN=$(IFS=';'; printf '%s' "${CONVERTED_CP[*]}")
if [[ -n "$CLASSPATH_WIN" ]]; then
  JAVA_CLASSPATH="${BIN_DIR_WIN};${CLASSPATH_WIN}"
else
  JAVA_CLASSPATH="$BIN_DIR_WIN"
fi

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Classpath generated!"
# === 2. Compile sources ===
echo "[INFO] Compiling sources into $BIN_DIR ..."
spinner &   # start spinner in background
SPIN_PID=$!
mkdir -p "$BIN_DIR" "$LOG_DIR_POSIX"
find "$SRC_DIR" -name "*.java" > sources.txt
javac -cp "$CLASSPATH_WIN" -d "$BIN_DIR" @sources.txt

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Sources compiled!"

# === 3. Run batch simulation ===
echo "[INFO] Launching Repast batch simulation..."
echo "[INFO] Log output directory: $LOG_DIR_POSIX"

export MSYS2_ARG_CONV_EXCL='*'

spinner &   # start spinner in background
SPIN_PID=$!

# Run the Java command
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  -cp "$JAVA_CLASSPATH" \
  -Dtest250930.logging.directory="$LOG_DIR_WIN" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$PARAMS_FILE" \
  "$SCENARIO_DIR"

kill $SPIN_PID >/dev/null 2>&1 || true
wait $SPIN_PID 2>/dev/null || true
echo -e "\r Simulation finished!"
