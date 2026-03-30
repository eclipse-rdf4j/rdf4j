# Query Plan Snapshot Guide

Complete guide for query-plan capture, persistence, and comparison.

Scope:
- Any tuple query (not only themed query benchmark).
- MemoryStore and LMDB Store.
- Unoptimized, optimized, executed explanations.
- TupleExpr JSON dump + load-back.
- Metadata + feature-flag state for reproducibility.
- Smart semantic diff modes.

Status:
- Experimental CLI and capture APIs (`@Experimental`).

## What You Get

Each snapshot stores:
- Query identity (`queryId`, query text, unoptimized fingerprint).
- Three explanation levels:
  - `unoptimized`
  - `optimized`
  - `executed`
- For each level:
  - explanation JSON
  - tuple expression JSON
  - IR-rendered query (optimized/executed by default)
- Metadata (git commit, benchmark/store/theme context, custom keys).
- Feature flags (system properties, direct values, reflection probes).
- Execution verification summary printed by CLI:
  - repeated query executions
  - dynamic run count with a soft 60-second cap per query

## Where Things Live

- CLI entrypoint:
  - `testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotCli.java`
- CLI options:
  - `testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotCliOptions.java`
- Compare + semantic diff:
  - `testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotComparator.java`
- Snapshot capture:
  - `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCapture.java`
- TupleExpr codec:
  - `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/TupleExprJsonCodec.java`
- Feature-flag collector:
  - `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/FeatureFlagCollector.java`

## Quick Start

From repo root.

Run interactive mode:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot
```

Interactive menu behavior:
- Arrow keys (`Up`/`Down`) + `Enter` select menu options.
- Number or exact value input also works.
- No-argument startup begins with `Action`:
  - `run query`
  - `compare existing runs`
  - `list themes`
  - `list queries`
  - `help`
- Run mode prompt order starts with: `store` -> `query source` (`themed`, `manual`, `file`, or `all-themed`) -> remaining fields.
- No-argument run wizard also prompts optional CLI flags: persist, compare-latest, diff-mode, query-id, output-dir, lmdb-data-dir (for LMDB).

Show help:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot -Dexec.args="--help"
```

## Core CLI Workflows

### 1) Run themed query and persist snapshot

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot \
  -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0"
```

Equivalent shorthand:

```bash
... -Dexec.args="--store memory --theme-query MEDICAL_RECORDS:0"
```

### 2) Run direct query text

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot \
  -Dexec.args="--store lmdb --theme SOCIAL_MEDIA --query 'SELECT * WHERE { ?s ?p ?o } LIMIT 50'"
```

### 3) Run query from file

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-file /tmp/query.rq"
```

### 4) Run and compare to latest previous run for same query

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0 --query-id my-q0 --compare-latest"
```

Matching order for `--compare-latest`:
1. Same unoptimized fingerprint.
2. Fallback to same `queryId`.

### 5) Compare existing runs (without running a query)

Interactive selection (browse/view/compare):

```bash
... -Dexec.args="--compare-existing --query-id my-q0"
```

Interactive run browser actions:
- `view run`: pick a run and print full details, original query, and full per-level explanation blocks (including IR-rendered query text when present).
- `compare runs`: pick left/right runs and print semantic diff.
- `quit`: exit browser.

No-argument compare wizard also supports:
- compare filter (`query-id` or `fingerprint`)
- optional output directory
- when `query-id` filter is chosen: shows all available query ids in that directory (with run counts) and lets you select one with arrows/numbers, or choose `<manual entry>`
- diff mode
- selection mode (`browse runs` or `enter indices`)

Non-interactive selection:

```bash
... -Dexec.args="--compare-existing --query-id my-q0 --compare-indices 0,1 --no-interactive"
```

Filter by fingerprint instead of query id:

```bash
... -Dexec.args="--compare-existing --fingerprint <sha256> --compare-indices 0,1 --no-interactive"
```

### 6) Capture without persistence

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0 --persist false"
```

or

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0 --no-persist"
```

Useful for ad-hoc compare-only checks:

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0 --no-persist --compare-latest"
```

Every CLI run also prints:

- `=== Execution Verification ===`
- `runs`, `totalMillis`, `averageMillis`, `resultCount`
- `verificationStatus` and, when applicable, failure diagnostics (`failureClass`, `failureMessage`, root-cause fields)
- plan-stability diagnostics (`optimizedPlanHashTransitionCount`, `optimizedPlanHashSequence`)
- `softLimitMillis` (currently `60000`)
- whether stopping hit the soft-limit projection or max repeat-run cap

Verification failures during repeated execution are persisted as snapshot metadata so plan capture/comparison can proceed.

### 8) Configure query timeout

Set a per-query timeout in seconds (`0` disables timeout):

```bash
... -Dexec.args="--store memory --theme MEDICAL_RECORDS --query-index 0 --query-timeout-seconds 30"
```

### 7) Run all themed queries for one store (all themes or one theme)

Memory store:

```bash
... -Dexec.args="--store memory --all-theme-queries"
```

LMDB store:

```bash
... -Dexec.args="--store lmdb --all-theme-queries"
```

Scope run-all to one theme:

```bash
... -Dexec.args="--store lmdb --all-theme-queries --theme HIGHLY_CONNECTED"
```

With in-memory-only capture:

```bash
... -Dexec.args="--store memory --all-theme-queries --no-persist"
```

With compare-latest per query run:

```bash
... -Dexec.args="--store lmdb --all-theme-queries --compare-latest --diff-mode structure+estimates"
```

Notes:
- `--all-theme-queries` is run mode only (not compare mode).
- You may combine `--all-theme-queries` with `--theme` to run all 11 queries for that theme only.
- Do not combine `--all-theme-queries` with `--theme-query`, `--query-index`, `--query`, or `--query-file`.
- In interactive mode this is available via query source `all-themed`.
- Batch runs print historical ETA at startup and emit ETA updates every 10 seconds while queries are running.
- Batch run CSV output now also includes determinism/perf-debug fields (`execution.verificationStatus`, failure details,
  plan-hash transition/sequence, `planDeterminism.*` fingerprints, and runtime metadata fields).

## Smart Diff Modes

Set with `--diff-mode`:

- `structure` (default):
  - compares plan structure
  - compares join algorithm annotations
  - compares actual result sizes
  - ignores estimates
- `structure+estimates`:
  - same as above
  - also compares estimates (`costEstimate`, `resultSizeEstimate`)

Examples:

```bash
... -Dexec.args="--compare-existing --query-id my-q0 --compare-indices 0,1 --diff-mode structure --no-interactive"
... -Dexec.args="--compare-existing --query-id my-q0 --compare-indices 0,1 --diff-mode structure+estimates --no-interactive"
```

Diff output per level (`unoptimized`, `optimized`, `executed`) includes:
- `structure`
- `joinAlgorithms`
- `actualResultSizes`
- `estimates`

If explanation JSON cannot be parsed, comparator prints `unavailable(<reason>)` for semantic fields.

## Metadata and Feature Flags

### CLI-level custom metadata

```bash
... -Dexec.args="... --metadata runTag=exp1 --metadata note=join-order-check"
```

### CLI-level system properties (applied before run)

```bash
... -Dexec.args="... --property rdf4j.optimizer.someFlag=true --property my.prop=123"
```

Also supported:

```bash
... -Dexec.args="... -Drdf4j.optimizer.someFlag=true -Dmy.prop=123"
```

### Built-in reproducibility metadata

Capture automatically tries, in order:
1. `rdf4j.query.plan.capture.gitCommit` system property
2. `GIT_COMMIT` env var
3. `git rev-parse --verify HEAD`
4. fallback `unknown`

Also stores `javaVersion`.

### Metadata from JVM properties by prefix

Any JVM property with prefix:

- `rdf4j.query.plan.capture.metadata.`

is copied into snapshot metadata.

Example:

```bash
... -Dexec.args="... --property rdf4j.query.plan.capture.metadata.branch=main"
```

### Feature flag collectors from JVM properties

`QueryPlanCapture` supports:

- CSV list property key:
  - `rdf4j.query.plan.capture.featureProperties`
- Prefix property:
  - `rdf4j.query.plan.capture.featurePropertyPrefix`

Example:

```bash
... -Dexec.args="... --property rdf4j.query.plan.capture.featureProperties=rdf4j.a,rdf4j.b"
... -Dexec.args="... --property rdf4j.query.plan.capture.featurePropertyPrefix=rdf4j.optimizer."
```

Store-specific reflective probes are included automatically in CLI and themed benchmark capture.

## Output Layout

Global default root:

- `testsuites/benchmark/src/main/resources/plan`

CLI default:

- `testsuites/benchmark/src/main/resources/plan/cli/<store>/`

Themed benchmark capture default:

- `testsuites/benchmark/src/main/resources/plan/<store>/`

Override output dir:

```bash
... -Dexec.args="... --output-dir /tmp/query-plan-snapshots"
```

Filename pattern:

- `<queryId>-<unoptimizedFingerprint>-<utcTimestamp>-<random8>.json`

## Snapshot JSON Shape

Top-level keys:
- `formatVersion`
- `capturedAt`
- `queryId`
- `queryString`
- `unoptimizedFingerprint`
- `metadata`
- `featureFlags`
- `explanations`

`explanations` map keys:
- `unoptimized`
- `optimized`
- `executed`

Per explanation:
- `level`
- `explanationText`
- `explanationJson`
- `tupleExprTree`
- `tupleExprJson`
- `irRenderedQuery`
- `irRenderingError` (only when IR rendering fails)

## TupleExpr JSON Round-Trip

`tupleExprJson` stores:
- format marker
- class/tree/fingerprint
- Base64 Java-serialized TupleExpr payload

Load back:

```java
QueryPlanCapture capture = new QueryPlanCapture();
QueryPlanSnapshot snapshot = capture.readSnapshot(path);
QueryPlanExplanation optimized = snapshot.getExplanations().get("optimized");
TupleExpr tupleExpr = new TupleExprJsonCodec().fromJson(optimized.getTupleExprJson());
```

Note: Java-serialized payload requires compatible RDF4J classes/version.

## Query Retrieval Strategy

To find "same query across runs":

1. Use `unoptimizedFingerprint` (most stable for structural identity).
2. Use `queryId` as secondary grouping.

`--compare-latest` already follows this strategy.

## Themed Benchmark Integration

Memory and LMDB `ThemeQueryBenchmark` capture snapshots during setup when enabled:

- property: `rdf4j.query.plan.capture.enabled=true`

Optional global output override:

- `rdf4j.query.plan.capture.outputDir=/path`

This path stores themed benchmark artifacts without CLI wrapper.

## Full Option Reference

- `--store <memory|lmdb>`
- `--theme <THEME>`
- `--query-index <0-10>`
- `--theme-query <THEME:INDEX>`
- `--query <SPARQL>`
- `--query-file <path>`
- `--persist <true|false>`
- `--no-persist`
- `--compare-latest`
- `--compare-existing`
- `--all-theme-queries`
  - optional with `--theme` to scope to one theme
- `--query-id <id>`
- `--fingerprint <hash>`
- `--compare-indices <i,j>`
- `--diff-mode <structure|structure+estimates>`
- `--property <key=value>`
- `-Dk=v` (inside `exec.args`)
- `--metadata <key=value>`
- `--output-dir <path>`
- `--lmdb-data-dir <path>`
- `--list-themes`
- `--list-queries <THEME>`
- `--no-interactive`
- `--help`

## Troubleshooting

- No interactive stdin (CI):
  - use `--no-interactive` and pass full args.
- No runs found in compare mode:
  - check `--output-dir`, `--query-id`, or `--fingerprint`.
- Only one run matched:
  - need at least two runs to compare.
- LMDB temp dir issues:
  - set `--lmdb-data-dir` explicitly.
- Want deterministic reproduction:
  - set `--query-id`, `--metadata`, and explicit system properties.
