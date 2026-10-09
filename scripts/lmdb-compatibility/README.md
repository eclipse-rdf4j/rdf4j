# LMDB binary-version round-trip investigation

This harness opens the same physical LMDB directories with three independently built, pinned RDF4J runtimes. It performs no export/import, repository rebuild, store metadata editing, or production-source changes. A failed check remains a failed check; its actual state, expected snapshots, journals, commands, logs, and binary provenance stay on disk.

The investigation defaults are actual release 6.1.0 (`46a2d41ab16fead609884ee655e26e6baa1ce325`), original optimize-lmdb 6.2.0-SNAPSHOT (`39650fd2a0324d06133578c1cee620e0e456a585`), and original maintenance 6.1.1-SNAPSHOT (`7c0fcac6094774aa96d45e29ad9e79080ddf3a14`). Implementation acceptance must override both mutable branch pins with the exact final local commits using `--source current=<maintenance-commit> --source optimize=<optimized-commit>`. The released pin remains immutable. The earlier checked-in fixture producer uses a different optimize revision and cannot substitute for these runtimes.

## Running

All output directories must be fresh; an existing path is rejected so earlier evidence cannot be overwritten. Java 25 or later, Python 3, and the repository Maven environment are required. No new dependency is added.

The packaging helper archives each exact source revision into a separate temporary checkout, builds it, copies runtime JARs into the evidence directory, and records each source and JAR SHA-256. The runtime classpaths contain these copied JARs, never a mutable Maven cache or `target/classes`. The harness compiles its Java sources separately against each runtime and checks loaded class locations and the actual LMDB JAR fingerprint at JVM startup.

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-full-20261004 \
      --prepare-runtimes

Reuse already packaged immutable runtime artifacts without repeating Maven builds:

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-full-20261004-reuse \
      --runtimes-json /absolute/path/to/runtimes/runtimes.json

For final implementation acceptance, package the final committed sources and keep the full default matrix:

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-implementation-full \
      --prepare-runtimes \
      --source current=<final-maintenance-commit> \
      --source optimize=<final-optimized-commit>

Use those same source overrides whenever reusing that final manifest. Manifest validation rejects a runtime whose revision differs from the selected source pin.

Two supplements establish paths that the default release → optimize → maintenance matrix cannot prove. The direct supplement creates authentic v2 stores with release, immediately opens and mutates them with maintenance, then reopens them with optimize. It includes profiles A/B/C, both inline settings, all selected seeds/scenarios and the normal/native/adjacency/overlay optimize sessions:

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-implementation-direct \
      --runtimes-json /absolute/path/to/final/runtimes/runtimes.json \
      --source current=<final-maintenance-commit> \
      --source optimize=<final-optimized-commit> \
      --direct-maintenance

The original-v6 supplement uses the latest completed optimize-first failure snapshot for each selected original v6 directory. It verifies the original runtime fingerprint and the confirmed independent expected snapshot against its manifest, records every physical input file hash, copies the bytes into the fresh output directory, and only opens the copy. Maintenance and final optimize then compare and mutate that copy. An uncertain/incomplete result or a modified oracle is rejected. Input inventories must still match after replay; output journals never use a store export as expected state.

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-implementation-original-v6 \
      --runtimes-json /absolute/path/to/final/runtimes/runtimes.json \
      --source current=<final-maintenance-commit> \
      --source optimize=<final-optimized-commit> \
      --original-v6-results logs/lmdb-version-roundtrip/20261004T094044Z/campaign-full-final/results.json

Both supplements support `--plan-only` and retain explicit selection artifacts. They are separate from the bounded `--writer-corpus` mode. All failures keep nonzero status; immutable release-only failures are reported separately from any patched-runtime failure, without treating a failed campaign as fully green.

An early, small authentic release-to-optimize smoke run includes metadata profiles A/B/C and fresh v6 repositories:

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-early-20261004 \
      --runtimes-json /absolute/path/to/runtimes/runtimes.json \
      --seeds 17011 --rounds 1 --statements 32 --payload-bytes 32 \
      --scenarios structural-control --optimize-modes normal \
      --stop-after-stage optimize-first

`--plan-only` generates journals and all expected snapshots before any JVM opens a repository. `--checker-only` retains source provenance, writes an empty repository plan, and directly compiles/runs the Java checker fault-injection suite against all three runtimes; it generates no repository journals or oracle trees. Both preserve their complete artifacts. The Python expected-state selftests require no Maven:

    python3 scripts/lmdb-compatibility/test_harness.py

Run the bounded supplemental writer investigation after a baseline campaign, using the same immutable runtime manifest:

    python3 scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-writer-corpus-20261004 \
      --runtimes-json /absolute/path/to/runtimes/runtimes.json \
      --writer-corpus

This creates six new repositories for a separate twenty-job campaign: two authentic release-created v2 repositories with inlining true/false, and four optimize-created v6 repositories with inlining true/false crossed with ordered numeric IDs true/false. Each repository keeps its same physical directory through creation, optimize, maintenance downgrade, and final optimize reopening; v6 creation starts at optimize. All jobs use normal evaluation and the same public profile C/matching optimized index configurations as the baseline. Its one seed (`17011`), 32 mutable rows, 32 Unicode payload repetitions and complete four-phase selection are fixed; combining `--writer-corpus` with workload selection flags is rejected. `--plan-only` also works for this mode. It does not repeat metadata-edge or optimizer-combination campaigns.

Every phase removes and commits every original 625-case direct, nested, shared, numeric-function and calendar-function reference, then re-adds and commits those fixed references. It separately writes phase-unique typed literal labels derived from the authoritative corpus categories. The fresh calendar inputs keep no-fraction, `.0`, `.00`, `.000`, nonzero and high-precision fraction spellings distinct while changing the minute for each phase; dateTime/dateTimeStamp, unusual zones, missing zones, negative/large years and invalid fallbacks remain explicit. Fresh integer subtype inputs respect their sign/range rules and include noncanonical/invalid labels; float/double and decimal inputs include lexical scales, signed zeros and large dictionary candidates. Compact `w0`/`w1`/`w2`/`w3` string, language and directional labels give actual runtimes opportunities to inline, and long Unicode variants exercise dictionary paths. Canonical booleans have only two word spellings and cannot be fresh in every phase, so their fixed owners are explicitly rewritten while phase-unique whitespace and invalid boolean labels are separate fresh inputs. These are expected input terms, with no duplicate codec or assumptions about which runtime should inline them.

The complete statement/query comparisons and commit confirmation rules apply unchanged. Supplemental queries compare exact STR/datatype/language results, sameTerm against nested dependencies, numeric/temporal sequences, and calendar functions with the independent MemoryStore. Fixed and fresh delete/re-add sequences each hold a materialized SNAPSHOT reader across SNAPSHOT writers; fresh creation uses NONE and remains uncertain until its commit returns. The mode also verifies SNAPSHOT rollback and namespace changes. The baseline corpus remains invariant in baseline campaigns; only this explicitly selected supplemental mode retires its owners.

`writer-corpus.json` records every phase's original lexical input and source case category. Each journal retains and hashes `writer-cases.tsv` along with its independent expected snapshots. `writer-ids.tsv` records before-write, after-commit, retirement/re-add and reopen observations by phase/category/case: actual `getId(value, false)`, `ValueIds.isInlined`, `ValueIds.getIdType`, exact decoded term, raw decoded language and direct-statement presence. Fresh dictionary-supported terms must be absent before their phase's writes and assigned afterward. Inline IDs can be computable before any statement is stored, so the checker permits those virtual IDs while requiring statement absence and exact pack/decode identity; it never mistakes them for a pre-existing dictionary assignment. `writer-id-observations.properties` summarizes those actual classifications without predicting codec behavior, and `writer-query-suite.tsv` retains the supplemental SPARQL text. Any missing ID, lexical mismatch, premature dictionary assignment or query/storage discrepancy remains a failed result. The Java checker injects faults into this observation comparator before any repository runs. Failed physical copies, confirmed independent oracle pointers and uncertain-commit blocking remain identical to the baseline failure/recovery flow.

The default seeds are `17011,29023,47017`, with twelve mutation rounds per phase. `--seeds`, `--rounds`, `--statements`, and `--payload-bytes` support reproduction and larger runs. Every eighth generated row has a long Unicode dictionary payload; the default is 2,048 repetitions of a two-byte character. The initial values/triples map settings are 1 MiB with automatic growth, so default workloads exercise real growth. The 3,000 mutable rows also have number, date, and edge companion statements, plus the fixed corpus and nested terms. The workload consequently has thousands of statements rather than count-only probes.

`--source LABEL=REVISION` overrides an explicit source pin for future investigations; Git refs/tags are resolved to exact commit SHA before manifest comparison and the fresh rerun command pins those resolved SHA values. `--java` and `--javac` select the JDK binaries. `--jvm-timeout` defaults to 900 seconds for each owned JVM. A timeout preserves PID/argv, attempts `jcmd Thread.print -l`, retains the last confirmed-commit/completion evidence, and then terminates only that invocation's process group. It fails with an uncertain committed state and never silently retries the mutated store. A smoke-only mode list or stop stage narrows coverage visibly in `campaign-plan.json`; the complete campaign uses all default stages and modes.

## Matrix and public configurations

For each seed and named scenario, authentic 6.1.0 creates two v2 repositories with literal inlining true/false. Optimize opens those exact directories and creates four v6 repositories: inlining true/false crossed with ordered numeric IDs true/false. Maintenance opens all six, then optimize reopens all six. The complete matrix uses the same directories throughout, including writes at every phase.

The v2 full workload uses public metadata profile C: creation leaves triple indexes unset and sets triple-term indexes explicitly to `spoc,cspo`. Existing/reopen sessions explicitly request the matching public triple indexes `spoc,posc` and triple-term indexes `spoc,cspo`; public-API smoke proved this leaves the actual persisted properties unchanged across all three runtimes. Full optimize fresh-v6 and subsequent sessions also explicitly request those same matching indexes. Two separate short metadata-edge profiles preserve authentic older-writer behavior: A uses the public default indexes, and B sets both `spoc,posc,ospc` triple indexes and `spoc,cspo` triple-term indexes explicitly. Both A and B run with inlining true/false, retain those original configurations, create/close/reopen under release, and attempt optimize/current/final-optimize admission. No missing properties are synthesized. Numbered `effective-config-*.properties` preserve actual getters and whether native files already exist for every creation/reopen, with `effective-config.properties` retaining the latest view. `format-*.properties` records what the runtime actually persisted.

Optimize phases split their twelve mutation rounds across four sequential sessions on each same directory:

| Session | Native evaluation | Direct adjacency | Value overlay |
| --- | --- | --- | --- |
| normal | false | false | false |
| native | true | false | false |
| adjacency | true | true | false |
| overlay | true | true | true |

These dependency combinations use public configuration setters. Each session closes before the next JVM starts. Periodic close/reopen occurs after every third mutation round, in addition to phase boundaries and the initial seed reopen. Every job first compares phase-entry contents before its first mutation. Committed operations alternate NONE and SNAPSHOT transaction isolation. Rollback-only transactions always use SNAPSHOT: the project explicitly documents that NONE changes may not be rollbackable. Exact rollback assertions are unchanged. A held, materialized SNAPSHOT reader spans retired-owner deletion/re-addition and is compared with its separately persisted pre-mutation expected state. Optimize's public readiness APIs wait at most five seconds at open/seed/exit milestones and record readiness descriptions, planner-stat hit counts, effective strategy factory, and an executed join-plan JSON. The overlay observer takes a candidate from the independent expected MemoryStore, then proves the actual typed-overlay callback through `ValueStore.withData`; it records actual fallback/refusal/cancellation reasons and treats both v2 and v6 according to their observed capability. Configured flags, reported readiness, typed lookup hits, and observed planner use remain separate facts.

Backing-source snapshots must stay stable through ordinary commits and retirement. Actual concurrent native map growth may invalidate a pinned snapshot and require explicit retry; the snapshot/growth supplement records that fail-closed exception separately from normal snapshot results. The harness does not silently retry or reinterpret invalidation as an unchanged snapshot. This limitation must remain explicit in the final acceptance verdict.

Two visibly distinct scenarios run independently. `full-corpus` is the required primary coverage. `structural-control` uses four explicitly documented invariant values and the same generated graph workload so a pre-existing literal defect cannot prevent all independent storage transitions from being investigated. Controls never replace a failed full-corpus result. A failed admission stage does not automatically block later binaries: if the last committed expected state remains known, current and final optimize still attempt real work on that same physical directory. Only an uncertain commit or missing completion record blocks later mutations; subsequent runtimes then attempt explicit admission/entry probes and record the blocking ancestor. Any failed or blocked primary/control/metadata job makes the campaign nonzero.

## Expected state and checks

Python constructs an explicit independent term model before actual writes. `journal.json` records the operation sequence, seed, repository configuration, runtime phase, session, and transaction isolation. `operations.tsv` is the exact JVM input. Every `expected-*.tsv.gz` is produced from that independent model and has a pre-execution SHA-256 in `expected-manifest.json`; the orchestrator checks those hashes after execution. Each commit operation points to its independently prepared post-commit snapshot. Java records that pointer only after commit returns successfully, and Python advances the active committed oracle only to confirmed pointers. Conditional recovery journals start from the last confirmed independent snapshot before their writes; initial prepared journals are retained unchanged. A raw physical failure snapshot is retained before a later runtime attempts recovery.

IRI strings, stable blank-node identifiers, literal labels, datatype IRIs, original language-tag spelling, direction, and recursively nested triple terms are explicit. Literal labels and datatypes are compared exactly: `.0`, `.00`, `.000`, noncanonical integers, decimal scales, and signed zeros cannot be silently normalized into a future expectation. RDF language tag identity is case-insensitive; the comparator applies that RDF-wide rule while raw input spelling stays in the corpus/journal and actual decoded spelling is recorded in ID evidence. There is no codec clone, reflection, private-state mutation, or use of actual exported contents as later expected state.

Java compares complete explicit statement bags, named-context bags, and namespace bags. It then populates an independent MemoryStore solely from the expected snapshot and compares every query result bag, ASK boolean, and CONSTRUCT statement bag. Before MemoryStore population, format-6 expected terms recursively use the declared lowercase language-tag contract; format-2 inputs retain original tag spelling. Literal labels, datatypes and directions stay exact, and the comparator never blanket-normalizes output strings. Raw journals/snapshots remain unchanged. Ordered numeric/temporal queries also compare the entire returned sequence; `STR(?s)` breaks numeric/temporal value ties deterministically. No generated CONSTRUCT blank nodes introduce accidental identifier nondeterminism.

The Java checker injects a missing statement, extra statement, and lexically changed same-number statement, then proves the actual comparison detects each. It also checks result multiplicity and a MemoryStore `sameTerm` query difference. All three runtime checker suites must pass before a campaign opens any LMDB repository. This validates the checker, not the pinned product behavior.

The workload exercises duplicate adds, exact deletes, deletion/re-addition with shared components, nested terms, default/named/blank-node contexts, namespace updates and removals, statement/namespace rollback, and genuine committed retired-owner delete/re-add sequences while a snapshot reader holds the earlier state. Writers spanning that held reader always use SNAPSHOT because NONE does not guarantee isolation. Ordinary committed mutation rounds still alternate NONE/SNAPSHOT. The execution state becomes uncertain from BEGIN NONE until its successful commit, so interruption during NONE mutations blocks subsequent writes rather than pretending a pre-BEGIN oracle is still the committed state. It does not create synthetic retirement metadata.

## Literal corpus

`corpus.json` is the authoritative case-by-case coverage table: each entry has a stable category ID, label, datatype, original language, direction, and input term. Every case is stored both directly and in shared/nested triple terms, with invariant statements that remain through all later mutation rounds. At the default pins it currently has 625 cases.

| Category | Coverage |
| --- | --- |
| boolean | true/false, 1/0, case/whitespace/invalid fallbacks |
| integer subtypes | byte/short/int/long, all unsigned forms, integer, positive/nonnegative/nonpositive/negative; min/max and adjacent outside values, zero/negative/large values, signs/leading zeros/whitespace/invalid labels |
| float/double | zero/negative zero, exponent forms, small/large values, NaN, INF/-INF/+INF, invalid labels |
| decimal | lexical scales, signed zero, leading zeros, large precision, invalid labels |
| strings/language/direction | empty/short/long Unicode, composed/decomposed text, control characters, language case/long tags, LTR/RTL |
| custom/core fallback | custom short/long datatype IRIs, all remaining core XSD types, RDF HTML/JSON/XMLLiteral, GeoSPARQL WKT |
| date/dateTime/dateTimeStamp | no fraction, .0/.00/.000, nonzero milliseconds, higher precision, Z/no timezone/+00:01/+00:15/±14:00, negative/large years, 24:00:00, invalid lexical forms |

The fixed corpus is informed by the existing codec and interoperability fixture sources, but is generated independently and is not an export of a fixture repository.

## Queries and retained evidence

Every JVM writes `query-suite.tsv`: default/named SELECT, joins and filters, VALUES/sameTerm, OPTIONAL, nested UNION, MINUS, EXISTS/NOT EXISTS, group/count, subquery+OPTIONAL, numeric/temporal ranges and order, nested triple-term functions, ASK, and CONSTRUCT. A fixed numeric predicate includes known valid integer subtypes, float/double, and decimal forms; its range query orders by value then deterministic subject. A fixed calendar predicate includes independently generated valid 2026 dateTime/dateTimeStamp variants, including zero/nonzero/high-precision fractions and unusual timezones. Its query compares YEAR/MONTH/DAY/HOURS/MINUTES/SECONDS/TIMEZONE results to MemoryStore; invalid lexical forms, missing-zone dateTimeStamp, and 24-hour edge cases remain in the complete identity corpus but are excluded from this explicitly valid function subset. Result comparison respects duplicate SELECT rows and exact RDF term identity.

`*-ids.tsv` records actual `ValueStore.getId(value, false)` IDs, the runtime's `ValueIds.isInlined` classification, and `ValueStore.getValue(id)` decoded terms/raw language tags at seed and session exit. These are read-only public/package-appropriate observations, not expected-state inputs. Nested dependency terms, IRIs, blank nodes, literals, contexts, and owners are inventoried. `format-before`, `format-reopen-*`, and `format-after` capture outer properties, marker, value/triple map sizes, and the native gc_meta format key through a separate read-only native environment while the owning store is closed. Raw database/key presence is recorded as `native.format.probe`; `native.format` compares the actual fence value, treating a missing database and a database without the fence key as the same absent format fence. Existing format and capability keys must remain stable, and v6 must retain its native format-6 marker. A genuine v2 runtime recovery may author previously absent version/index descriptors: the resulting version must be 2 and index sets must match the public configuration (or pinned default). Only those three absent descriptors may be completed; existing descriptors, all capabilities, markers, and native fences remain strict. Before/after inventories and `legacy_metadata_completion` in each result explicitly record this runtime-authored recovery. Java checker fault injections verify completion acceptance and rejection of wrong versions, wrong indexes, new capabilities, changed fences, and removed existing descriptors.

The output root contains `campaign-plan.json`, `corpus.json`, `runtime-provenance.json`, `harness-source-provenance.json`, `checker-results.json`, `results.json`, `summary.json`, and `invocation.json`. Startup copies retain and hash the Python entry point, packaging helper, and all executed Java source bytes. Compilation uses the retained Java copies, and source packaging uses the retained helper copy. Each job has exact argv/shell command, run log, effective config, loaded runtime class sources/JAR hashes, completion state, complete mismatch TSVs, and query exceptions. Failures accumulate without weakening assertions or changing expectations. Completed operations permit subsequent real transitions even after comparison failures. Known-state admission failures permit later recovery work, while an uncertain commit instead produces explicit ancestor probes. A recovered stage cannot turn its earlier failed stage into a complete four-stage pass.

`invocation.json` contains an exact fresh-directory rerun command. Run that command to reproduce from the same seed and pinned binaries; it never resumes a partially mutated repository as though it were clean. A result's `command`/`probe_command` and retained `operations.tsv` allow inspecting a single JVM invocation, but replaying writes against an already mutated failure directory changes the experiment. Preserve the original directory and use the fresh rerun command.

Exit status 0 means every selected job and checker passed. Status 1 means a pinned-runtime comparison, transition, or blocked-chain failure. Status 2 means harness/build/configuration setup failed; `harness-error.json` names the error. A structural-control success is explicitly partial coverage and cannot turn a primary full-corpus failure into campaign success.

## Completed 2026-10-04 investigation

The main campaign retained 352 job results at `logs/lmdb-version-roundtrip/20261004T094044Z/campaign-full-final`: 66 passed, 286 failed, 342 completed their operations, and ten stopped on authentic metadata-edge admission errors. No ancestor blocked a later stage. The separate writer supplement retained twenty completed jobs at `logs/lmdb-version-roundtrip/20261004T094044Z/writer-corpus-final/campaign`; all twenty remain failed under strict comparison. Both commands exit 1. These job counts do not count distinct defects.

No stored statement or namespace difference was observed in either campaign. Independently reviewed failures include older public metadata configurations that cannot reopen/admit, duplicate context IDs from a live LMDB writer after retirement/re-addition, and optimized format-6 accelerated query errors already present before downgrade. The accelerated errors include an unsigned-long value incorrectly passing a numeric range filter, missing YEAR/MONTH/DAY/HOURS/MINUTES bindings, and CONSTRUCT returning 3,000 numeric triples when only ninety satisfy its filter. The maintenance binary has no corresponding accelerated-query failure in this matrix. Original failed stores and comparisons remain preserved.

The separate authentic A/B/C metadata smoke is retained at `/private/tmp/rdf4j-lmdb-metadata-smoke-20261004T1153Z/`. Default profile A omitted triple-term index metadata. Both-explicit profile B left `store.properties` absent and optimize rejected the nonempty directory. Profile C naturally saved complete metadata, but its original unset-index reopening configuration failed under release/optimize. Untouched C copies reopened with the matching persisted public indexes passed under all three binaries without changing the properties SHA256. `summary.json` preserves the original failures; `explicit-match-results.json` preserves the matched-profile success. Full workloads use that proven public configuration path while the independent metadata-edge failures remain visible.

Supplemental `LANG(?value)` string-case differences reflect the persisted format-6 `canonical-language-tags=lowercase-v1` capability, observed during optimize creation before downgrade. They remain strict oracle differences and preserve nonzero status, but do not show corruption or a new downgrade regression. All 720 fresh maintenance-phase typed inputs have exact post-write decoding and present statements: 619 newly assigned dictionary IDs and 101 inline IDs. Inline IDs can already be computed before storage; all fresh statements were absent before writing. The two inline-enabled v6 stores each assign 41 inline and 79 dictionary cases, including exact fresh `.0`, `.00` and `.000` dateTime/dateTimeStamp spellings through inline IDs. Inline-disabled stores use dictionary IDs throughout.

Actual native executed plans and active direct adjacency were observed. Value overlay was configured but did not become ready during any captured five-second readiness wait; this run does not establish overlay execution coverage. Per-session readiness, effective configuration, executed plans and planner-stat observations retain the exact stage/sample evidence.

The main executed Java source SHA256 is `a530128b778279877013e453e9b8ffee943a1208a0187b59216ee839df112d98`; the supplemental Java source SHA256 is `bdc9f068138ddf21fed817595d712c079d9370624ad4d84fa15ce42dcd3818de`. Each campaign's `harness-source-provenance.json` hashes all copied Python and Java sources, and `runtime-provenance.json` identifies the actual immutable bundles. Ten final Python selftests passed and the final sixteen Java checker assertions passed under all three pinned binaries. These are harness/checker validation, not a green product compatibility result.

For byte-for-byte harness reproduction, invoke the retained entry point rather than a later edited workspace script. From the repository root, use a fresh output path:

    python3 logs/lmdb-version-roundtrip/20261004T094044Z/campaign-full-final/harness-sources/scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-frozen-main-replay \
      --runtimes-json logs/lmdb-version-roundtrip/20261004T094044Z/runtimes/runtimes.json

The retained supplemental entry point likewise carries its exact source bytes and embedded version pins:

    python3 logs/lmdb-version-roundtrip/20261004T094044Z/writer-corpus-final/campaign/harness-sources/scripts/run-lmdb-version-roundtrip.py \
      --output-dir /tmp/lmdb-roundtrip-frozen-writer-replay \
      --runtimes-json logs/lmdb-version-roundtrip/20261004T094044Z/runtimes/runtimes.json \
      --writer-corpus

The original `invocation.json` also retains the command that was issued and a fresh rerun using the workspace entry point. Reusing an existing failed data directory for another write changes the experiment; preserve it and use a fresh campaign directory.
