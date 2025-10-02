#!/usr/bin/env bash
set -euo pipefail

REPAST_VERSION="2.11.0"
UPDATE_SITE_ARCHIVE="repast.simphony.updatesite.${REPAST_VERSION}.zip"
UPDATE_SITE_URL="https://github.com/Repast/repast.simphony/releases/download/v.${REPAST_VERSION}/${UPDATE_SITE_ARCHIVE}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_DIR="${REPO_ROOT}/lib/repast-${REPAST_VERSION}"
PLUGINS_SUBDIR="repast.simphony.updatesite"
EXTRACT_DIR="${TARGET_DIR}/extracted"

if [[ -d "${TARGET_DIR}" && -f "${TARGET_DIR}/classpath.txt" ]]; then
  echo "Repast runtime already installed at ${TARGET_DIR}" >&2
  exit 0
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "${tmpdir}"' EXIT

mkdir -p "${REPO_ROOT}/lib"

echo "Downloading Repast Simphony update site ${REPAST_VERSION}..." >&2
curl -L --fail --show-error --progress-bar "${UPDATE_SITE_URL}" -o "${tmpdir}/${UPDATE_SITE_ARCHIVE}"

echo "Unpacking archive..." >&2
unzip -q "${tmpdir}/${UPDATE_SITE_ARCHIVE}" -d "${tmpdir}"

if [[ ! -d "${tmpdir}/${PLUGINS_SUBDIR}/plugins" ]]; then
  echo "Expected plugins directory not found in archive" >&2
  exit 1
fi

mkdir -p "${TARGET_DIR}" "${EXTRACT_DIR}"
cp "${tmpdir}/${PLUGINS_SUBDIR}/plugins"/*.jar "${TARGET_DIR}"

echo "Extracting plugin bundles (this may take a minute)..." >&2
for plugin in "${TARGET_DIR}"/*.jar; do
  bundle_name="$(basename "${plugin}" .jar)"
  dest="${EXTRACT_DIR}/${bundle_name}"
  if [[ ! -d "${dest}" ]]; then
    unzip -q "${plugin}" -d "${dest}"
  fi
done

echo "Generating classpath metadata..." >&2

# Use python3 if available, otherwise fallback to python
PYTHON_CMD=$(command -v python3 || command -v python)

if [[ -z "$PYTHON_CMD" ]]; then
  echo "Error: Python is not installed." >&2
  exit 1
fi

"$PYTHON_CMD" - "$REPO_ROOT" "$TARGET_DIR" <<'PY'
import sys
from pathlib import Path
repo_root = Path(sys.argv[1])
target_dir = Path(sys.argv[2])
entries = []
for path in sorted(target_dir.rglob('*')):
    if path.is_file() and path.suffix == '.jar':
        entries.append(path.relative_to(repo_root).as_posix())
    elif path.is_dir() and path.name in {'bin', 'classes'}:
        entries.append(path.relative_to(repo_root).as_posix())
with open(target_dir / 'classpath.txt', 'w') as f:
    f.write('# Generated classpath entries\n')
    for entry in entries:
        f.write(entry + '\n')
PY

echo "Repast runtime extracted to ${TARGET_DIR}" >&2