#!/usr/bin/env bash
set -euo pipefail

HUGO_BIN="${HUGO_BIN:-hugo}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
TEMP_CONTENT_PATH="$REPO_ROOT/site/content/_alert-shortcode-test.md"

cleanup() {
  local exit_code=$?
  rm -f "$TEMP_CONTENT_PATH"
  exit $exit_code
}
trap cleanup EXIT

cat <<'CONTENT' > "$TEMP_CONTENT_PATH"
---
title: "Alert Shortcode Test"
date: 2025-01-01
---

{{< alert type="info" >}}
This is a shortcode validation test.
{{< /alert >}}
CONTENT

"$HUGO_BIN" --source "$REPO_ROOT/site" --minify --baseURL "https://example.com/" >/tmp/hugo-alert-test.log

trap - EXIT
rm -f "$TEMP_CONTENT_PATH"
echo "Hugo alert shortcode test completed successfully."
