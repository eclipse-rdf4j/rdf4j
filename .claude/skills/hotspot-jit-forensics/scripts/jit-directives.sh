#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: jit-directives.sh --method <pattern> [--out <file>]

Example:
  jit-directives.sh --method "com/foo/MyClass.myMethod()" --out c2-directives.json5
USAGE
}

method=""
out_file="c2-directives.json5"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --method)
      method="$2"
      shift 2
      ;;
    --out)
      out_file="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$method" ]]; then
  echo "--method is required" >&2
  usage
  exit 1
fi

python3 - "$out_file" "$method" <<'PY'
import sys

out_file = sys.argv[1]
method = sys.argv[2]

content = f"""[
  {{
    // Match the method(s) you care about. Wildcards are allowed.
    match: [\"{method}\"],

    c2: {{
      PrintInlining: true,
      PrintAssembly: true,
      PrintIntrinsics: true,
      IGVPrintLevel: 3,
      // MaxNodeLimit: 80000,
    }},

    c1: {{
      PrintInlining: false,
      PrintAssembly: false,
    }},
  }}
]
"""

with open(out_file, "w", encoding="utf-8") as handle:
    handle.write(content)
PY

echo "Wrote $out_file"
