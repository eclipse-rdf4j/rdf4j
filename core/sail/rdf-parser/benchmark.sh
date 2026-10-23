#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
module=core/sail/rdf-parser
baseline=${BASELINE_COMMIT:-}
offline=(-o)
if [[ "${MVN_OFFLINE:-true}" == false ]]; then offline=(); fi
if [[ -n "$baseline" ]]; then
  git rev-parse --verify "$baseline^{commit}" >/dev/null
  if ! git cat-file -e "$baseline:$module/src/main/java"; then
    echo "BASELINE_COMMIT must contain the RDF parser module production sources." >&2
    exit 1
  fi
fi
mkdir -p "$module/target/benchmarks"
output=$(mktemp -d "$PWD/$module/target/benchmarks/run.XXXXXX")
echo "Benchmark artifacts: $output"
mvn -B -ntp "${offline[@]}" -Dmaven.repo.local=.m2_repo -pl "$module" dependency:build-classpath \
  -Dmdep.includeScope=test -Dmdep.outputFile="$output/classpath.txt"
deps=$(cat "$output/classpath.txt")
variants=(current)
if [[ -n "$baseline" ]]; then
  mkdir -p "$output/baseline-src" "$output/baseline-classes"
  git archive "$baseline" "$module/src/main/java" | tar -x -C "$output/baseline-src"
  find "$output/baseline-src" -name '*.java' > "$output/sources.txt"
  javac --release 25 -proc:none -cp "$deps" -d "$output/baseline-classes" @"$output/sources.txt"
  variants=(baseline current)
fi
# Both variants use the same fixtures, harness, dependencies, JVM options and machine.
# A supplied baseline changes module classes only; shared Rio/evaluator dependencies remain current.
for variant in "${variants[@]}"; do
  if [[ "$variant" == baseline ]]; then classes="$output/baseline-classes"; else classes="$PWD/$module/target/classes"; fi
  classpath="$classes:$PWD/$module/target/test-classes:$deps"
  java -cp "$classpath" org.openjdk.jmh.Main 'RdfParserBenchmark.(tuple.*|service.*)' \
    -p statements=10,1000 -f 2 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -foe true \
    -rf json -rff "$output/$variant-documents.json" > "$output/$variant-documents.log" 2>&1
  java -cp "$classpath" org.openjdk.jmh.Main 'RdfParserBenchmark.rewrite.*' \
    -f 2 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -foe true \
    -rf json -rff "$output/$variant-rewrites.json" > "$output/$variant-rewrites.log" 2>&1
done
python3 - "$output" <<'PYRESULT'
import json, pathlib, sys
root = pathlib.Path(sys.argv[1])
for kind in ('documents', 'rewrites'):
    baseline_path = root / ('baseline-' + kind + '.json')
    baseline = {(r['benchmark'], tuple(sorted(r.get('params', {}).items()))): r
                for r in json.loads(baseline_path.read_text())} if baseline_path.exists() else {}
    for row in json.loads((root / ('current-' + kind + '.json')).read_text()):
        key = (row['benchmark'], tuple(sorted(row.get('params', {}).items())))
        result = {'benchmark': row['benchmark'].rsplit('.', 1)[-1], 'params': row.get('params', {}),
                  'current_us': row['primaryMetric']['score'],
                  'current_B_op': row['secondaryMetrics']['gc.alloc.rate.norm']['score']}
        if key in baseline:
            before = baseline[key]
            result.update(baseline_us=before['primaryMetric']['score'],
                          speedup=before['primaryMetric']['score'] / row['primaryMetric']['score'],
                          baseline_B_op=before['secondaryMetrics']['gc.alloc.rate.norm']['score'])
        print(json.dumps(result))
PYRESULT
