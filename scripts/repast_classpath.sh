#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LIB_DIR="${REPO_ROOT}/lib"

latest_cp_file="$(ls "${LIB_DIR}"/repast-*/classpath.txt 2>/dev/null | sort -V | tail -n 1 || true)"

if [[ -z "${latest_cp_file}" ]]; then
  echo "No Repast classpath file found. Run scripts/setup_repast_runtime.sh first." >&2
  exit 1
fi

grep -v '^#' "${latest_cp_file}" | paste -sd ':' -
