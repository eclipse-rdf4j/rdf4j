# Reproducing fast N-Quads parser and staging measurements

`scripts/benchmark-fast-nquads.py` compares two already-built JMH jars using the
existing `scripts/run-single-benchmark.sh --no-build` runner. It performs no
checkout or Maven build. Each input is a module directory containing exactly one
non-`original-*` JMH/benchmark jar under `target/` or one directory below it.

The helper snapshots both jars into a new output directory and verifies that the
parser and staging benchmark classes, their generated JMH classes, and their JMH
metadata are identical. This catches comparisons where a new corpus or benchmark
implementation was accidentally applied to only one side. It records jar and
class SHA-256 hashes, Java configuration, CPU/cgroup information, commands, and
caller-supplied revision labels in `manifest.json`. Revision labels are **not
verified source provenance**; the person producing the jars must establish which
sources they built.

## Run with existing, independent jars

Set `JAVA_HOME` and `PATH` to JDK 25 or newer. Bash 4 or newer must be on `PATH`
(the existing runner uses `mapfile`), and Python 3.11 or newer is required. The
helper uses only Python's standard library and existing repository scripts. Run
from a Linux or macOS host with a stable CPU allocation; macOS's bundled Bash 3
does not meet the runner's requirement.

```bash
python3 scripts/benchmark-fast-nquads.py \
  --baseline-module /path/to/baseline/core/sail/lmdb \
  --candidate-module /path/to/candidate/core/sail/lmdb \
  --baseline-label 2a843fff41f53a118ce92295be76a4af515a58a0 \
  --candidate-label "$(git rev-parse HEAD)" \
  --output-dir /path/to/new-results \
  --benchmark all
```

Parser defaults are three warmup iterations and four measurement iterations of
one second each, two forks, and a 512 MiB fixed heap. Staging defaults are two
warmup iterations of one second, four measurement iterations of two seconds,
two forks, and a 1 GiB fixed heap; longer measurements allow several complete
staging invocations within an iteration. Both use one JMH thread and the JMH GC
profiler. `--benchmark block`, `stream`, or `stage` selects one benchmark;
`all` runs each baseline/candidate pair in that order. Parameter defaults come
from the identical benchmark metadata in the jars, so newly added corpus cases
are included automatically. The staging benchmark currently declares workers
1/2/4/8, repeated/unique vocabularies, ASCII/Unicode labels, and no compression.

Use parameter overrides to select a smaller matrix, for example:

```bash
python3 scripts/benchmark-fast-nquads.py \
  --baseline-module /path/to/baseline/core/sail/lmdb \
  --candidate-module /path/to/candidate/core/sail/lmdb \
  --output-dir /path/to/new-staging-results \
  --benchmark stage --workers 1,2,4,8 --terms REPEATED --characters ASCII
```

`--parser-terms` selects parser corpora; `--compression fastest` includes LZ4 in
staging. Override timing with `--warmup-iterations`,
`--measurement-iterations`, `--forks`, and `--iteration-time` (the latter overrides
both warmup and measurement durations). `--heap 2g` selects a fixed heap for all
selected benchmarks; repeated `--jvm-arg=-XX:...` options append JVM arguments.
Each benchmark's settings are applied equally to both jars. `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`,
and `_JAVA_OPTIONS` also affect Java; their values are recorded in the manifest.

`--verify-only` copies the jars and verifies the harness without launching JMH.
`--dry-run` additionally prints the exact runner commands. Both require a new
output directory because the snapshots and manifest are retained. Jobs run
sequentially and retain JMH's own default benchmark locking; the helper neither
overrides nor deletes that lock. If another JMH process is running, JMH can
refuse the run and its failure log is preserved. Schedule the full comparison
when other benchmark or build processes are idle.

## Build a known baseline without changing the active checkout

The corrected baseline is commit
`2a843fff41f53a118ce92295be76a4af515a58a0`. The following prepares two isolated
worktrees and overlays **only the two benchmark sources** from the candidate onto
the baseline. Production sources remain those of the respective commits. Both
trees are built with the same JDK, Maven settings, and benchmark source files.

These commands assume the candidate changes have been committed and that the
workspace's `.m2_repo` already has the required dependencies. Builds run
sequentially; do not share this Maven repository with another simultaneous build.

```bash
set -euo pipefail
repo_root="$(git rev-parse --show-toplevel)"
candidate_commit="$(git rev-parse HEAD)"
base_commit=2a843fff41f53a118ce92295be76a4af515a58a0
benchmark_work="$(mktemp -d)"

git worktree add --detach "$benchmark_work/baseline" "$base_commit"
git worktree add --detach "$benchmark_work/candidate" "$candidate_commit"

for benchmark_source in \
  core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/bulk/FastNQuadsParserBenchmark.java \
  core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/bulk/FastNQuadsStagingBenchmark.java
do
  git show "$candidate_commit:$benchmark_source" > "$benchmark_work/baseline/$benchmark_source"
done

for variant in baseline candidate
do
  ln -s "$repo_root/.m2_repo" "$benchmark_work/$variant/.m2_repo"
  (
    cd "$benchmark_work/$variant"
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -T 1C -Pquick \
      clean install > maven-build.log 2>&1
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am \
      -Pbenchmarks,quick clean package >> maven-build.log 2>&1
  )
done

python3 "$repo_root/scripts/benchmark-fast-nquads.py" \
  --baseline-module "$benchmark_work/baseline/core/sail/lmdb" \
  --candidate-module "$benchmark_work/candidate/core/sail/lmdb" \
  --baseline-label "$base_commit" --candidate-label "$candidate_commit" \
  --output-dir "$benchmark_work/comparison" --benchmark all
```

If dependencies are missing, populate the local Maven cache with the same build
without `-o`, then return to offline builds. Preserve each worktree's
`maven-build.log`. A clean package rebuilds production and generated JMH classes;
the helper runs copied jars rather than a classpath containing mutable
`target/classes` directories. To verify the source origin separately, record
`git -C <worktree> rev-parse HEAD` and `git -C <worktree> diff --name-only`.
Only the benchmark overlay should appear in the baseline diff.

## Evidence and interpretation

Each result directory contains:

- `manifest.json`: input/snapshot jar paths and hashes, benchmark/class hashes,
  environment, unverified revision labels, settings, commands, and completion or
  failure status.
- `baseline/target/jmh.jar` and `candidate/target/jmh.jar`: the exact runtime jars.
- `<side>/<benchmark>.console.log`: complete runner output, including failures.
- `<side>/<benchmark>.jmh.log`: the runner's unfiltered JMH log, when produced,
  copied before `benchmark-output.log` can be overwritten by the next benchmark.
  With `RDF4J_BENCHMARK_PLAN_GUARD=false`, the runner writes directly to the
  console log instead; preflight failures can also leave no separate JMH log.
- `<side>/<benchmark>.json`: raw JMH results, including per-iteration data and
  allocation metrics. Staging's `gc.alloc.rate.norm` may include its
  `Level.Invocation` setup and teardown fixtures; it does not isolate staging
  hot-path allocation. Parser allocation excludes its trial-level setup.
- `comparison.csv` and `comparison.json`: results joined by benchmark and exact
  parameter set, throughput ratio, JMH score error, and allocated bytes per
  operation. Both parameter sets and runtime settings are checked before joining.

The parser benchmark measures parsing and canonical byte construction. The
staging benchmark includes parsing, validation, routing, de-duplication,
predicate counting, and staging output writes and closes; it does not build LMDB
indexes. Staging uses the filesystem selected by `java.io.tmpdir`, buffered
writes, fresh workspaces, and an in-memory input corpus. Interpret its results
for that workload, including worker startup and buffer allocation, rather than
as a durable database import or cold-storage throughput measurement.

The CSV ratio is candidate throughput divided by baseline throughput. Compare
the error intervals and repeat measurements when differences are small. An
identical production binary is allowed and produces a warning; this is useful
for measuring run-to-run noise. Stage workers share one process, while the
parser benchmark uses one parsing thread. Record actual CPU quotas and memory
limits when discussing multicore scaling.
