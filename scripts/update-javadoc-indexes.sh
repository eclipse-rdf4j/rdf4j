#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAVADOC_DIR="${REPO_ROOT}/site/static/javadoc"
OLD_DIR="${JAVADOC_DIR}/old"

if [[ ! -d "${JAVADOC_DIR}" ]]; then
	echo "Javadoc directory '${JAVADOC_DIR}' does not exist." >&2
	exit 1
fi

if [[ ! -d "${OLD_DIR}" ]]; then
	echo "Legacy javadoc directory '${OLD_DIR}' does not exist." >&2
	exit 1
fi

export JAVADOC_DIR OLD_DIR

python3 <<'PY'
import datetime
import json
import os
import re
from pathlib import Path

javadoc_dir = Path(os.environ["JAVADOC_DIR"])
old_dir = Path(os.environ["OLD_DIR"])

def isoformat(timestamp: float) -> str:
	return datetime.datetime.fromtimestamp(timestamp, tz=datetime.timezone.utc).isoformat().replace("+00:00", "Z")

def natural_key(value: str):
	return [int(part) if part.isdigit() else part.lower() for part in re.split(r"(\d+)", value)]

directories = [
	{
		"name": path.name,
		"href": f"{path.name}/",
		"lastModified": isoformat(path.stat().st_mtime),
	}
	for path in javadoc_dir.iterdir()
	if path.is_dir() and not path.name.startswith(".")
]

directories.sort(key=lambda item: natural_key(item["name"]), reverse=True)

files = [
	{
		"name": path.name,
		"href": path.name,
		"size": path.stat().st_size,
		"lastModified": isoformat(path.stat().st_mtime),
	}
	for path in old_dir.iterdir()
	if path.is_file() and not path.name.startswith(".")
]

files.sort(key=lambda item: natural_key(item["name"]), reverse=True)

(javadoc_dir / "manifest.json").write_text(
	json.dumps(directories, indent=2) + "\n", encoding="utf-8"
)

(old_dir / "files.json").write_text(
	json.dumps(files, indent=2) + "\n", encoding="utf-8"
)
PY

echo "Updated ${JAVADOC_DIR}/manifest.json"
echo "Updated ${OLD_DIR}/files.json"
