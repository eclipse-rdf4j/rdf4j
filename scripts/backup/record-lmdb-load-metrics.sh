#!/usr/bin/env bash
set -euo pipefail

usage() {
	echo "Usage: $0 --manifest PATH --store-dir PATH --load-start-epoch SECONDS"
	echo "          --triple-map-size BYTES --value-map-size BYTES [--output PATH]"
}

manifest=
store_dir=
load_start_epoch=
triple_map_size=
value_map_size=
output=lmdb-load-metrics.json

while [[ $# -gt 0 ]]; do
	case "$1" in
	--manifest) manifest=$2; shift 2 ;;
	--store-dir) store_dir=$2; shift 2 ;;
	--load-start-epoch) load_start_epoch=$2; shift 2 ;;
	--triple-map-size) triple_map_size=$2; shift 2 ;;
	--value-map-size) value_map_size=$2; shift 2 ;;
	--output) output=$2; shift 2 ;;
	*) usage >&2; exit 2 ;;
	esac
done

[[ -f "$manifest" && -d "$store_dir" && -n "$load_start_epoch" && -n "$triple_map_size" && -n "$value_map_size" ]] ||
	{ usage >&2; exit 2; }

actual_map_size() {
	local environment=$1
	if command -v mdb_stat >/dev/null 2>&1; then
		mdb_stat -e "$environment" | awk -F: '/Map size/ { gsub(/ /, "", $2); print $2; exit }'
	else
		echo null
	fi
}

now=$(date +%s)
python3 - "$manifest" "$store_dir" "$load_start_epoch" "$now" "$triple_map_size" "$value_map_size" \
	"$(du -sb "$store_dir/triples" | awk '{print $1}')" \
	"$(du -sb "$store_dir/values" | awk '{print $1}')" \
	"$(actual_map_size "$store_dir/triples")" "$(actual_map_size "$store_dir/values")" <<'PY' > "$output"
import json
import sys

manifest, store_dir, started, finished, triple_configured, value_configured, triple_bytes, value_bytes, triple_actual, value_actual = sys.argv[1:]
with open(manifest, encoding="ascii") as source:
    dataset = json.load(source)
metrics = {
    "dataset": dataset["total"],
    "store_directory": store_dir,
    "load_seconds": int(finished) - int(started),
    "configured_map_sizes": {
        "triples_bytes": int(triple_configured),
        "values_bytes": int(value_configured),
    },
    "directory_sizes": {
        "triples_bytes": int(triple_bytes),
        "values_bytes": int(value_bytes),
    },
    "actual_map_sizes": {
        "triples_bytes": None if triple_actual == "null" else int(triple_actual),
        "values_bytes": None if value_actual == "null" else int(value_actual),
    },
}
print(json.dumps(metrics, indent=2))
PY

echo "Metrics: $output"
