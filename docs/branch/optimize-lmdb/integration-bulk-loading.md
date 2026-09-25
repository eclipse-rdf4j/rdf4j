# Bulk-load CLI and distribution contract

This document covers the shared CLI, launchers, and SDK packaging introduced
in the historical comparison from merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Source descriptions were checked
against the current review revision recorded in the [branch README](README.md).
The staged loader's partition/codec/recovery internals and persistent effects are
documented in [storage bulk ingestion and recovery](storage-bulk-ingestion-recovery.md).
The CLI is an independent packaged module, `rdf4j-lmdb-bulk-load`, wired into
the SDK assembly.

## Entry points and packaging

The Java entry point is
[`LmdbBulkLoad`](../../../tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/LmdbBulkLoad.java).
The repo-level [`scripts/lmdb-bulk-load.sh`](../../../scripts/lmdb-bulk-load.sh)
looks for the executable tool jar under the module's `target`; if missing or
stale, its default path packages the module plus dependencies with Maven. Its
`--no-build` mode fails with usage exit 2 instead of building. It chooses
`$JAVA_HOME/bin/java` when available, otherwise `java`, and enables native
access for the packaged executable.

The SDK includes the LMDB bulk-load module and two launchers in `bin`:
POSIX [`lmdb-bulk-load.sh`](../../../assembly/src/main/dist/lmdb-bulk-load.sh)
and Windows [`lmdb-bulk-load.bat`](../../../assembly/src/main/dist/lmdb-bulk-load.bat).
They run the class from the SDK `lib/*` distribution and enable native access.
With no arguments the SDK launchers start the interactive flow. The shell
script beside the repository and the packaged SDK launcher differ: the
repository helper resolves/builds the executable jar, while the SDK script
uses already-assembled libraries. The SDK POM also makes API-doc and WAR source
paths configurable, so a custom workspace output tree can still be assembled.

## Input selection and CLI behavior

The noninteractive interface requires `--store PATH` and one or more
`--input PATH|-` values. Inputs may be regular RDF files or directories; the
CLI expands directories to regular descendants, normalizes and deduplicates
paths in sorted order, and rejects symbolic links. Format can be explicit by
format name, file extension, or MIME type. If not supplied for path input, the
CLI infers it from the path; stdin requires `--format` and uses
`urn:rdf4j:lmdb-bulk-load:stdin` as its default base URI. Stdin must be the only
input. For file input, the default base URI is the file URI; `--base-uri`
overrides it for each input.

This CLI uses its own loader path; it does not inherit the recursive compressed
and archived input support of the general RDF loader and Workbench
`RDFInputDispatcher`. The `rio` parser path recognizes gzip by the stream
signature and unwraps one gzip layer before parsing; the fast N-Triples parser
has the same gzip-only behavior. Neither path recursively visits ZIP/TAR
members or applies the general dispatcher's other compression codecs. This
boundary applies only to input parsing; intermediate bulk-load artifacts have
their own compression options below.

Options select parser mode (`auto`, `fast`, or `rio`), statement and triple-term
index specifications, inline literal IDs, value-hash generation, a byte-sized
memory budget (with KiB/MiB/GiB suffixes), power-of-two partition count,
maximum staging/merge files, `workers`, `queue-batches`, a temporary-directory
value, per-transaction record/byte caps, progress format, and intermediate
compression (`fastest`, `none`, or codec levels; more granular
run/staged/artifact choices are available). The exact current defaults and
legal values are printed by `--help` in source; the full storage consequences
belong in the storage guide. In the current implementation, `workers` and
`queue-batches` are captured into workspace/progress metadata but are not used
to schedule loader work or bound a work queue. Treat them as reported settings,
not active parallelism or memory controls. `memoryBudgetBytes`, partition
count, open-file limits, and writer transaction caps do flow into the builder;
the memory budget is not a process-wide heap cap. No throughput or
maximum-RAM number is asserted here.

The `temporary-directory` setting is recorded as the `spill.directory`
configuration value. The phase-artifact workspace itself remains the
target-adjacent `.<target>.lmdb-bulk-load` directory in the current engine;
the CLI disk monitor also watches the configured temporary directory, but that
does not make it the phase workspace location. See
[resource limits and workspace recovery](storage-bulk-ingestion-recovery.md)
for the source-level storage path.

The parser applies optional statement indexes, triple-term indexes, inline
literal behavior, and value-hash population to `LmdbStoreConfig`, then builds a
`LmdbBulkLoader` with the selected streaming/spill settings. The CLI handles
one resumable recorded run through its interactive recovery flow; detailed
workspace eligibility and atomic publication/recovery contracts are in the
[storage guide](storage-bulk-ingestion-recovery.md).

## Disk safety and exit behavior

The top-level CLI owns the JVM and starts a daemon disk-space monitor before
parsing arguments. Once it knows the store and optional spill directory, it
watches those volumes, deduplicating paths that resolve to the same
`FileStore` and using the tightest readable free-space reading. It checks every
60 seconds at or above 100 GiB, every second between 50 and 100 GiB, and at
less than 50 GiB it logs the final reading and calls `Runtime.halt(3)`. The
monitor logs ordinary status no more than once per minute. If disk space
cannot be read, it reports the issue and continues at the relaxed cadence; it
does not mistake an unreadable volume for a full one. Before paths are known,
it probes shortly rather than guessing which volume to kill the process for.

The abrupt exit deliberately skips shutdown hooks because they might write to
the volume that has run out; the staged workspace is intended to remain
resumable. This is a safety trade-off: a caller embedding `LmdbBulkLoader`
should use its library contract rather than `LmdbBulkLoad.main`, which owns and
may halt its JVM. Exit codes in current source are 0 success/help, 1 load
failure, 2 usage/argument error, 3 disk exhausted, and 130 cancellation. A
non-success load prints diagnostics to stderr; completion counters including
parsed/stored statements, persisted/inline values, temporary bytes, elapsed
milliseconds, and selected compression are printed to stdout.

Sources: [CLI implementation](../../../tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/LmdbBulkLoad.java),
[bulk input engine](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/LmdbBulkLoaderEngine.java),
[general input dispatcher](../../../core/rio/api/src/main/java/org/eclipse/rdf4j/rio/helpers/RDFInputDispatcher.java),
[disk monitor](../../../tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/DiskSpaceMonitor.java),
[repository launcher](../../../scripts/lmdb-bulk-load.sh),
[SDK launchers and assembly](../../../assembly/src/main/assembly/sdk.xml).
[`LmdbBulkLoadTest`](../../../tools/lmdb-bulk-load/src/test/java/org/eclipse/rdf4j/tools/lmdb/bulk/LmdbBulkLoadTest.java)
and [`DiskSpaceMonitorTest`](../../../tools/lmdb-bulk-load/src/test/java/org/eclipse/rdf4j/tools/lmdb/bulk/DiskSpaceMonitorTest.java)
are source coverage references; they do not claim a passing result.

## Illustrative invocation

This is an illustrative invocation. Use `--help` from a CLI build matching
your checkout for the complete argument reference:

```text
rdf4j-lmdb-bulk-load --store /data/store --input /data/rdf --parser auto \
  --memory 8GiB --partitions 32 --progress json
```

The input directory is traversed as a set of regular files. Format inference
depends on each discovered file's suffix; supply `--format` if that inference
is ambiguous or unsupported. The memory/partition values above are examples,
not recommendations or evidence of a safe host configuration.

## FAQ

**Will `scripts/lmdb-bulk-load.sh` only launch an existing artifact?** No. By
default it checks whether the tool jar is missing or older than loader source
and may build it; `--no-build` prevents that behavior.

**Does `--input -` mix with directory or file input?** No. Stdin must be the
only input and needs an explicit `--format`.

**Do `--workers` and `--queue-batches` configure parallel loading here?** No.
The current engine records and reports those settings but does not use them to
schedule work or cap queued batches. Do not infer a concurrency or memory
effect from the option names.

**Does `--temporary-directory` contain the bulk phase files?** Not in this
implementation. It is recorded as `spill.directory`, while phase artifacts use
the target-adjacent resumable workspace. The CLI monitor watches the configured
directory separately.

**Why does low free disk cause a hard exit instead of cleanup?** The monitor's
source comment says shutdown hooks may write to the exhausted volume, while
the workspace left behind is resumable. See the storage guide for the actual
recovery contract.

**Is this a general RDF4J repository API change?** The Sail API adds a default
bulk-add hook so stores can preserve a bulk sequence across the connection
boundary; that shared contract is described in
[Sail source lifetimes](integration-sail-source-lifecycle.md). The packaged
CLI itself targets LMDB.
