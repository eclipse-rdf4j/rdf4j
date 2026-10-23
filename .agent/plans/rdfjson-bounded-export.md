# Bound default RDF/JSON export memory without changing grouped output

This ExecPlan is maintained under `.agent/PLANS.md`. Its progress, discoveries, decisions, and outcome must stay current as implementation proceeds.

## Purpose / Big Picture

The RDF/JSON writer normally groups every statement by subject, predicate, and object before writing JSON. It currently stores the entire graph in a `TreeModel`, so a large Server or Workbench export can exhaust heap. After this change, the default writer must preserve its grouped JSON, lexical order, duplicate elimination, context grouping, and settings while holding only a bounded sorting window in heap. Larger exports stage sorted runs in private temporary files with an aggregate disk budget and clean them up on success or failure. A small configured budget must fail clearly before filling a disk.

## Progress

- [x] (2026-10-23 09:56Z) Inspect writer and export reachability.
- [x] (2026-10-23 09:57Z) Create isolated main-based worktree.
- [x] (2026-10-23 09:59Z) Complete initial root quick install.
- [x] (2026-10-23 10:00Z) Add smallest failing resource-budget test.
- [x] (2026-10-23 10:01Z) Preserve red report before production changes.
- [x] (2026-10-23 10:25Z) Bound live run count during input.
- [x] (2026-10-23 10:15Z) Verify grouped output and cleanup variants.
- [x] (2026-10-23 10:29Z) Run focused and complete module gates.
- [x] (2026-10-23 10:52Z) Reproduce abandoned writer lifecycle leak.
- [x] (2026-10-23 11:00Z) Add abort cleanup across export paths.
- [x] (2026-10-23 11:11Z) Rerun affected tests and module gates.
- [x] (2026-10-23 11:19Z) Review candidate for draft publication.
- [ ] (in_progress) Publish separate draft PR after review.

## Surprises & Discoveries

- `core/rio/rdfjson/src/main/java/org/eclipse/rdf4j/rio/rdfjson/RDFJSONWriter.java` uses `TreeModel` by default. Its opt-in `ALLOW_MULTIPLE_OBJECT_VALUES=true` path writes immediately but can repeat JSON object keys, so enabling it by default changes semantics.
- `TreeModel` uses a lexical SPOG comparator; its subject, predicate, object, and graph order determines the writer's deterministic output and set-like duplicate elimination. The replacement needs the same comparator and adjacent-group output.
- `core/client/pom.xml` includes the RDF/JSON Rio module; Server and Workbench expose RDF writer factories through the registry. The Server export preflight scans at most 1,024 statements by default, but the actual export is not bounded.
- The initial root quick install succeeded in `.mvnf/workspaces/rdfjson-spill/logs/20261023T095427.967818Z-39392-43e2575f/maven.log`.
- The smallest budget test failed before production edits: `Expected org.eclipse.rdf4j.rio.RDFHandlerException to be thrown, but nothing was thrown` (`initial-evidence.rdfjson-spill.txt`; full log `.mvnf/workspaces/rdfjson-spill/logs/20261023T095942.744708Z-86989-eb83f962/verify.log`).
- The focused spill class passed 5/0/0/0 in `.mvnf/workspaces/rdfjson-spill/logs/20261023T101351.064209Z-62645-1e7fa41d/verify.log`; grouped output was byte-identical to the TreeModel oracle across compact/pretty mode, duplicates, contexts, directed language, a >64 KiB literal, and more than 32 sort runs.
- Review found that retaining one run path for every sorting window until `endRDF()` still grows heap with total input. Add an input-time compaction regression and use tiered merges so the live run count scales with the logarithm of input size.
- The 1,025-record run-count regression failed at `spill runs must compact while input is read` before tiered compaction (`initial-evidence.run-count.txt`), then passed 1/0/0/0 in `.mvnf/workspaces/rdfjson-spill/logs/20261023T102224.791920Z-43125-f2f696d4/verify.log`.
- Independent review identified a lifecycle gap: upstream failure can bypass `endRDF()` after a run is spilled, retaining its temporary file and aggregate reservation. `Rio.write` and repository export paths currently call `startRDF` and `endRDF` only on the success path. Reproduce this through public APIs before adding an abort contract and caller cleanup.
- Red abort evidence is retained in `initial-evidence.abort-rio.txt`, `initial-evidence.abort-wrapper.txt`, `initial-evidence.abort-sail.txt`, `initial-evidence.abort-server.txt`, and `initial-evidence.abort-preflight.txt`. Each showed either a leaked RDF/JSON disk reservation after a failed export or a missing repository abort callback.
- Rio, repository exports, and the Server export view now abort an incomplete handler after upstream failure. An abortable handler contract forwards through wrappers, while a Cleaner remains a fallback for callers that abandon a writer without invoking the lifecycle. A wrapper start failure also reaches abort, and the RDF/JSON generator leaves caller-owned output open (`AUTO_CLOSE_TARGET` disabled).
- `RDFFormat.RDFJSON` does not support TripleTerm natively. With default encoding disabled, a deep TripleTerm previously reached the grouped writer and produced an empty RDF/JSON object; `initial-evidence.triple-term.txt` records the failing writer-level test. The writer now rejects unsupported terms before spool sorting, and the private spool no longer has recursive TripleTerm codec paths. The same test covers grouped and opt-in streaming modes.
- An oversized literal that exceeds the configured disk budget now has a follow-up spill under that same budget in the focused test, proving the failed writer released its process-wide reservation.
- A direct writer caller can encounter an unsupported term after a prior spill without routing through Rio or repository export. The focused red report `initial-evidence.unsupported-term-cleanup.txt` showed the next export denied by the retained reservation. Unsupported-term rejection now invokes abort on the writer itself, preserving the original failure and freeing the spill immediately.

## Decision Log

- Decision: Keep the existing opt-in duplicate-key streaming mode as-is and change only the default grouped mode. Rationale: RDF/JSON consumers may discard duplicate JSON keys; default grouped output is a compatibility contract. Date/Author: 2026-10-23, Codex.
- Decision: Use the JDK and project model comparator for bounded external sorting. Rationale: sorted runs plus a bounded fan-in merge preserve lexical order and duplicate elimination without a new external dependency. Date/Author: 2026-10-23, Codex.
- Decision: Expose a writer-configured memory window and aggregate temporary disk budget. Rationale: heap and temporary disk are separate finite resources; a clear budget error is safer than moving exhaustion from heap to disk. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The red budget, live-run, abort, and unsupported-term reports were retained before their respective production edits. The bounded sorter, private run files, tiered k-way merge, grouped emitter, and abort lifecycle are implemented. Final affected-module verification passed 783 tests, 0 failures, 0 errors, and 10 skips across seven Rio, repository, and Server modules (`.mvnf/workspaces/rdfjson-spill/logs/rdfjson-final-cross-module-verify.log`). Independent review requested one more mixed-term byte parity test against the TreeModel oracle; the focused class passed 15/0 and the full RDF/JSON module passed 218/0/0/2 (`.mvnf/workspaces/rdfjson-spill/logs/rdfjson-final-module-verify.log`). Copyright, formatting, and `git diff --check` passed. Publication is next.

## Context and Orientation

`RDFJSONWriter.startRDF()` currently chooses between opt-in direct streaming and the default `TreeModel`. `consumeStatement()` adds default-mode statements to that model. `endRDF()` walks its subject/predicate/object/context sets to write grouped JSON. `RDFJSONWriterSettings` holds format-specific Rio settings. The writer module is `core/rio/rdfjson`; its tests live in the corresponding `src/test/java/org/eclipse/rdf4j/rio/rdfjson` directory. `TreeModel.comparator()` supplies exactly the existing lexical SPOG order. A sorted run is a private file containing a sorted sequence of statements; a merge reads one current statement from each run and emits the least one next.

## Plan of Work

First add a focused JUnit test that configures very small memory and temporary disk budgets, sends one or more larger statements, and expects a clear `RDFHandlerException`; the current writer ignores the settings and must fail this test. Save its Surefire report in `initial-evidence.rdfjson-spill.txt` before editing production code. Add output parity tests for interleaved subjects and predicates, duplicate statements, multiple contexts including the default graph, language/datatype literals, blank nodes, empty graphs, pretty printing, and the existing duplicate-key opt-in path.

Replace default-mode `TreeModel` accumulation with a bounded sorted window using the same comparator. Spill sorted, deduplicated windows to private files, then merge with a bounded number of open files. Keep at most one current statement per run and a fixed-size window in memory. Encode RDF terms in a private binary record format with checked lengths; one exceptionally large term may still occupy memory because the incoming RDF `Value` already does. While merging, group adjacent SPO values and stream the contexts into one JSON object without retaining all contexts. Remove files after success and in nested failure paths, including generator/output exceptions. Reserve temporary bytes before writes, release them only after file deletion, and bound aggregate use across concurrent writers.

## Concrete Steps

From `/Users/havardottestad/.codex/worktrees/security-rdfjson-spill/rdf4j`, use `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace rdfjson-spill RDFJSONSpillWriterTest#rejectsConfiguredDiskBudget --retain-logs` for the red test. Use `python3 scripts/agent-evidence.py` against the printed workspace Surefire path and log immediately afterward. Repeat the focused selection after the fix, then run the test class and `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace rdfjson-spill core/rio/rdfjson --retain-logs`. Never run tests with `-am` or `-q`. Run `scripts/checkCopyrightPresent.sh` from `scripts`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` for formatting, and check `git diff --check`.

## Validation and Acceptance

The red budget test must fail before production edits and pass afterward, with retained Surefire snippets. Existing RDF/JSON writer and parser tests must pass. The new parity tests must show the same canonical JSON bytes as the pre-change writer for both compact and pretty output across unsorted input and duplicates; the direct streaming setting must remain unchanged. A forced spill with a small window must produce the same grouped output. Failure paths must leave no run files and must release their aggregate reservation, so a later small export can complete. Full module Surefire/Failsafe totals and the exact PR head will be reported separately.

## Idempotence and Recovery

The worktree is separate from main and other branches. Test logs and `initial-evidence.rdfjson-spill.txt` are intentionally retained and must not be staged. If a test run fails because a Maven dependency is absent offline, retry the same selection once online and return offline. If a run fails for another reason, inspect the focused report before changing implementation. Temporary run files belong to one writer and are deleted by that writer only.

## Artifacts and Notes

Initial install: `.mvnf/workspaces/rdfjson-spill/logs/20261023T095427.967818Z-39392-43e2575f/maven.log` ended `BUILD SUCCESS` after 125 modules. Red reports: `initial-evidence.rdfjson-spill.txt`, 1 test / 1 failure / 0 errors / 0 skips; `initial-evidence.run-count.txt`, 1 failure on 1,025 live runs. Final module report: `.mvnf/workspaces/rdfjson-spill/logs/20261023T102551.065523Z-46007-096dd05f/verify.log`, 211 tests / 0 failures / 0 errors / 2 skips.

## Interfaces and Dependencies

Use `RDFJSONWriterSettings` for the two new `LongRioSetting` controls. Keep them specific to RDF/JSON, without modifying global Rio APIs. The implementation may add a package-private spool helper in `core/rio/rdfjson/src/main/java/org/eclipse/rdf4j/rio/rdfjson`. Use JDK temporary files, `DataInputStream`/`DataOutputStream`, and the existing `TreeModel` comparator. Do not add a new Maven dependency. The existing `RDFJSONWriter` public constructors and `ALLOW_MULTIPLE_OBJECT_VALUES` behavior remain available.

Revision note: 2026-10-23 initial plan after source and reachability audit. The red budget test is now preserved and implementation can begin.
