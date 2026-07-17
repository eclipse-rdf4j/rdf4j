# Make SPARQL AST Regeneration Reproducible

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`,
`Decision Log`, and `Outcomes & Retrospective` must be kept current as work proceeds.

This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J checks generated SPARQL parser sources into Git and then carries two substantial,
hand-maintained optimizations as Git patches. Today the documented regeneration procedure is
manual, uses an unpinned JavaCC source checkout, formats too broadly, and no longer produces a
baseline to which the checked-in patches apply. A contributor therefore cannot safely tell
whether a grammar regeneration lost an optimization or merely used a different tool or order.

After this work, a contributor can run one standard-library Python command to verify the
checked-in result, regenerate it, or record deliberate edits. The reproducible pipeline is:

    JavaCC 7.0.12 generation
      -> restore exact existing RDF4J headers
      -> Spotless-format only files touched by JavaCC
      -> apply the two ordered file-scoped patches

The observable acceptance criterion is byte equality: replaying the pipeline must produce every
JavaCC-touched Java source exactly as it is checked in. The current Java sources are canonical;
this work refreshes the patch representation without changing parser output or behavior.

## Progress

- [x] (2026-07-17 10:22Z) Read repository instructions and `.agent/PLANS.md`.
- [x] (2026-07-17 10:22Z) Ran the mandatory root offline quick clean install; all modules passed.
- [x] (2026-07-17 10:22Z) Inspected JavaCC history, generated-file scope, patch failures, and CI.
- [x] (2026-07-17 10:45Z) Added ten Python tests for helpers, preflights, rollback, and CLI results.
- [x] (2026-07-17 10:45Z) Implemented `check`, `regenerate`, and `record` with pinned JavaCC resolution.
- [x] (2026-07-17 10:45Z) Refreshed and renamed both deterministic optimization patches.
- [x] (2026-07-17 10:45Z) Replaced manual README guidance and added the CI drift check.
- [x] (2026-07-17 10:45Z) Proved record/regenerate/check round trips and working-tree restoration.
- [x] (2026-07-17 10:45Z) Ran copyright, formatting, focused parser tests, and the module suite.
- [x] (2026-07-17 10:45Z) Audited the final diff while preserving unrelated working-tree changes.

## Surprises & Discoveries

- Observation: the first patch is stored as
  `JavaCC/patches/01-optimize-SyntaxTreeBuilder..diff`, with two dots, although the README refers
  to the intended one-dot name.
  Evidence: `git ls-files core/queryparser/sparql/JavaCC/patches` and the README disagree.

- Observation: neither existing patch reverses cleanly from the current checked-in Java sources,
  and neither applies cleanly to a freshly generated, header-restored, Spotless-formatted baseline.
  Evidence: isolated `git apply --reverse --check` and fresh-baseline replay both fail; the first
  failure is around `SyntaxTreeBuilder.java`, and the second is in
  `SyntaxTreeBuilderTokenManager.java`.

- Observation: JavaCC generation currently changes exactly seven support Java files and creates
  the temporary `sparql.jj`; it does not rewrite the many RDF4J-specific `AST*.java` classes.
  Evidence: byte snapshots around `jjtree` followed by `javacc` identified
  `JJTSyntaxTreeBuilderState.java`, `SyntaxTreeBuilder.java`, `SyntaxTreeBuilderConstants.java`,
  `SyntaxTreeBuilderDefaultVisitor.java`, `SyntaxTreeBuilderTokenManager.java`,
  `SyntaxTreeBuilderTreeConstants.java`, and `SyntaxTreeBuilderVisitor.java`.

- Observation: the official JavaCC 7.0.12 Maven Central binary identifies itself as 7.0.12 and
  has SHA-256 `067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13`.
  Building the 7.0.12 source tag with its Ant build instead reports 7.0.11 in generated output.
  Evidence: isolated generation with both tool forms. This is why the workflow must use the
  official binary and verify its digest.

- Observation: Maven Spotless accepts `-DspotlessFiles` as a comma-separated set of regular
  expressions over absolute paths, allowing only JavaCC-touched sources to be formatted.
  Evidence: an isolated invocation targeting `SyntaxTreeBuilder.java` changed only that file.

- Observation: commits `e93dee29cc`, `6f932b730c`, and `b3719a2381` advanced the checked-in parser
  and lexer implementation after the last patch refresh. In particular, the later lexer commit
  adds fast-stop logic, whitespace/comment paths, and extracted NFA helpers.
  Evidence: `git show` and path-limited history for the two patched Java files and patch directory.

- Observation: the first real official-jar replay exposed three differences outside the patch
  targets, each solely in JavaCC's trailing `OriginalChecksum` comment. The formatted Java bodies
  were byte-identical. JavaCC computes this footer from its unformatted template output, so a
  template-whitespace difference can survive as metadata even after Spotless normalizes the body.
  Evidence: the failed `record` transaction reported only the footer in
  `SyntaxTreeBuilderDefaultVisitor.java`, `SyntaxTreeBuilderTreeConstants.java`, and
  `SyntaxTreeBuilderVisitor.java`, then restored the AST exactly.

- Observation: Git's whitespace checker treats a unified-diff context marker followed by a Java
  tab as a space-before-tab error in the patch file, even though the whitespace is literal patch
  syntax and the underlying Java source is Spotless-clean.
  Evidence: `git diff --check` initially reported patch context lines; the scoped Git attribute
  `core/queryparser/sparql/JavaCC/patches/*.diff -whitespace` removes those false positives while
  leaving source whitespace checking enabled everywhere else.

## Decision Log

- Decision: use Routine D and this ExecPlan even though the intended Java output is unchanged.
  Rationale: this is a cross-cutting regeneration transaction involving external tooling, patch
  serialization, failure recovery, CI, documentation, and end-to-end reproducibility.
  Date/Author: 2026-07-17 / Codex.

- Decision: treat the current checked-in seven generated support files as the desired byte-level
  result, not as inputs to be reformatted or cleaned up.
  Rationale: the user explicitly requires zero Java output changes; refreshed patches must encode
  all accumulated customizations instead.
  Date/Author: 2026-07-17 / Codex.

- Decision: discover JavaCC-touched Java files from before/after byte snapshots on every run, then
  require all resulting custom differences to be represented. Only the two named patch targets
  may differ after generation and formatting.
  Rationale: a hard-coded omission could silently discard a new generated file or customization.
  Date/Author: 2026-07-17 / Codex.

- Decision: restore the exact header bytes from each pre-generation file before Spotless rather
  than synthesize or normalize them.
  Rationale: existing copyright years and spacing are source history and must remain byte-stable.
  New generated Java files receive the repository's current standard header only when no previous
  header exists.
  Date/Author: 2026-07-17 / Codex.

- Decision: make every mode transactional over the AST scope. `check` and `record` always restore
  source bytes; `regenerate` restores them on every failure but intentionally leaves the successful
  replay in place.
  Rationale: generator, formatter, checksum, and patch failures must not strand partial output.
  Date/Author: 2026-07-17 / Codex.

- Decision: run Spotless through the module POM with exact absolute-path regular expressions and
  try Maven offline first. Only a dependency-resolution failure may be retried online when the user
  did not pass `--offline`.
  Rationale: this matches repository dependency policy and prevents unrelated source formatting.
  Date/Author: 2026-07-17 / Codex.

- Decision: after targeted formatting, retain an existing JavaCC `OriginalChecksum` footer only
  when replacing that one footer makes the entire file byte-identical to the formatted result.
  Any body difference remains visible and causes `record` to fail outside the two patch targets.
  Rationale: the checksum describes pre-Spotless template bytes rather than the checked-in source;
  this narrow normalization reproduces current metadata without masking generated-code drift.
  Date/Author: 2026-07-17 / Codex.

- Decision: unset Git whitespace classification only for the JavaCC patch directory.
  Rationale: unified diffs must retain source indentation and blank context verbatim; treating the
  serialized patch as Java-like source makes an otherwise clean repository diff unauditable.
  Date/Author: 2026-07-17 / Codex.

## Outcomes & Retrospective

The reproducible workflow is complete. `record` generated the intended one-dot first patch and
refreshed the token-manager patch from an official JavaCC 7.0.12 baseline. A second `record` kept
both patch SHA-256 values unchanged:

    9da26397f2bfdd2c10b03dfa8083606e96bc6b4a831ecea938623da79789ccbc  01-optimize-SyntaxTreeBuilder.diff
    b35a0e3fb00fdfe50ebd2870141b756cbc937107330b897042606e80ab92f743  02-optimize-SyntaxTreeBuilderTokenManager.diff

Default `check` downloaded and cached the official jar, whose digest matched the required
`067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13`. The final complete Git-status
stream hashed to `83636d38e14c330815a6eb03269af5028f137ae7c917f365e24eeb997ce97a51`
both before and after `check`. Offline `regenerate` then changed no checked-in AST source and left no
`sparql.jj`.

Ten Python unit tests pass. The copyright checker and root `process-resources` formatting pass.
`SPARQLParserTest` passes 75 tests, and the complete `core/queryparser/sparql` module passes 131
tests, with zero failures, errors, or skips. Logs are retained at
`logs/mvnf/20260717-104158-verify.log` and `logs/mvnf/20260717-104241-verify.log`.

There were no deviations in the requested CLI. The only additional normalization is the narrowly
guarded retention of JavaCC checksum footer metadata when it is provably the sole formatted-file
difference. No generated Java source is changed in the final working tree, and the pre-existing
LMDB benchmark result modification remains outside this work.

## Context and Orientation

The Maven module is `core/queryparser/sparql`. Its JavaCC inputs and checked-in AST sources live at
`core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast`. The JJTree grammar
is `sparql.jjt`; `jjtree` produces temporary `sparql.jj`, and `javacc` consumes that file. The
optimization patches live in `core/queryparser/sparql/JavaCC/patches` and are applied from the
repository root because their headers use repository-relative paths.

The first patch represents all desired differences in `SyntaxTreeBuilder.java`. The second
represents all desired differences in `SyntaxTreeBuilderTokenManager.java`. Other support files are
generated but are expected to equal their formatted generated baselines exactly. The existing
`AST*.java`, `Node.java`, and `SimpleNode.java` files include RDF4J-owned behavior that JavaCC does
not regenerate; they remain outside the transaction unless a future JavaCC invocation actually
touches or creates them.

The new entry point is `scripts/manage-sparql-ast.py`. Unit tests are in
`scripts/test_manage_sparql_ast.py` and import the hyphenated script with `importlib`. The PR workflow
is `.github/workflows/pr-verify.yml`, and contributor documentation is
`core/queryparser/sparql/JavaCC/README.md`.

One pre-existing unrelated tracked modification is present at
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/temp.txt`.
It is not part of this work and must remain byte-for-byte untouched.

## Plan of Work

First, expose small pure helpers for exact RDF4J header extraction/restoration, checksum validation,
stable unified-diff construction, Git status parsing, and byte snapshots. Add Python unit tests that
make these contracts executable, including deterministic labels with no timestamps or machine paths.

Next, implement a coordinator that resolves the repository root, verifies the JavaCC jar, and wraps
the complete AST operation in a byte-restoring transaction. It snapshots every Java file in the AST
directory plus the temporary grammar path before invoking `jjtree` and `javacc`. It removes only the
temporary grammar it created, derives the set of touched/created Java files from snapshots, restores
headers, and calls Spotless with exact `spotlessFiles` expressions.

Implement modes on the shared baseline operation. `regenerate` rejects tracked modifications in its
generated output scope, runs the pipeline, applies patches in lexical order, and commits the result
to the working tree only on success. `check` performs the same replay, compares all touched files to
the pre-run snapshot, reports concise stable diffs on drift, and restores the working tree even when
the comparison fails. `record` preserves current customized outputs, recreates the formatted
baseline, refuses any unrepresented difference outside the two patch target files, creates both
candidate patches in temporary storage, replays them, and accepts them only after exact comparison.
Patch replacement uses atomic same-directory temporary files followed by `os.replace`.

Then run `record` against the canonical files using the verified 7.0.12 jar. Remove the malformed
double-dot patch name and retain the two intended one-dot, file-scoped patches. Update the README to
teach only the three commands, the pinned cache/digest behavior, the format-before-patch ordering,
transactional recovery, and the rule that generated-code customizations are edited before `record`.
Add `python3 scripts/manage-sparql-ast.py check` to the `formatting-and-quick-compile` PR job.

Finally, demonstrate record idempotence, successful regenerate with no AST diff, and check with an
unchanged complete Git status. Run the Python unit tests, copyright checker, repository formatter,
focused `SPARQLParserTest`, and all tests in `core/queryparser/sparql`. Audit patches for stable paths,
file scope, and the expected later fast paths.

## Concrete Steps

Commands are run from `/Users/havardottestad/Documents/Programming/rdf4j` unless a different working
directory is stated.

1. Run fast Python tests while implementing:

       python3 -m unittest scripts/test_manage_sparql_ast.py

2. Record and test the patch round trip with the pinned tool:

       python3 scripts/manage-sparql-ast.py record
       python3 scripts/manage-sparql-ast.py record
       python3 scripts/manage-sparql-ast.py regenerate
       git diff --exit-code -- core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast
       python3 scripts/manage-sparql-ast.py check

3. Validate headers and formatting:

       (working directory: scripts) ./checkCopyrightPresent.sh
       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

4. Run parser tests through the repository `mvnf` skill, retaining full logs:

       python3 .codex/skills/mvnf/scripts/mvnf.py SPARQLParserTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py core/queryparser/sparql --retain-logs

   These are test commands: neither `-am` nor `-q` may be added.

5. Audit final state:

       git diff --check
       git status --short
       git diff -- core/queryparser/sparql/JavaCC scripts/manage-sparql-ast.py scripts/test_manage_sparql_ast.py .github/workflows/pr-verify.yml

## Validation and Acceptance

The Python test suite must cover exact header preservation, deterministic patch bytes, rejection of
the wrong jar checksum, dirty generated-output preflights, restoration after an injected failure,
and public CLI exit codes. These tests may use temporary repositories and injected command runners;
they must not require Maven, JavaCC, Git network access, or third-party Python packages.

The real workflow is accepted only when:

- `record` run twice leaves both patch files unchanged on the second invocation.
- `regenerate` leaves no diff in any AST Java file.
- `check` exits zero and the complete `git status --porcelain=v1 -z` byte stream is identical before
  and after, including the unrelated LMDB benchmark result modification.
- Both patches apply in lexical order to a newly generated, header-restored, formatted baseline.
- Patch 1 names only `SyntaxTreeBuilder.java`; patch 2 names only
  `SyntaxTreeBuilderTokenManager.java`; neither contains timestamps or absolute paths.
- The checked-in Java output does not change from the initial snapshot.
- The copyright checker and repository formatting complete successfully.
- `SPARQLParserTest` and the complete `core/queryparser/sparql` module pass via `mvnf` with retained
  logs.
- The CI workflow calls `check` in its `formatting-and-quick-compile` job.

## Idempotence and Recovery

Jar download goes to a temporary sibling of the ignored target cache and becomes visible only after
the expected SHA-256 is verified. Patch recording likewise prepares both candidate files before
atomically replacing either checked-in patch. A failed `record` must leave both patches and all AST
sources as they were.

The AST transaction records both file contents and file absence. Restoration writes original bytes
atomically and deletes only files proven to have been created by the current transaction. It never
uses `git reset`, `git checkout`, `git clean`, or a broad deletion. A pre-existing `sparql.jj` is a
hard preflight failure because ownership would be ambiguous.

It is safe to repeat `check`; it is read-only from the user's point of view. It is safe to repeat
`regenerate` after a successful clean replay because the bytes are identical. It is safe to repeat
`record`; a second run must synthesize identical patches. If a command is interrupted, rerunning
`check` identifies any drift; tracked user edits remain protected by command-specific dirty-file
preflights and byte restoration.

## Artifacts and Notes

The JavaCC jar cache is
`core/queryparser/sparql/target/javacc/javacc-7.0.12.jar`. The module's `target/` directory is already
ignored. No new runtime or Python dependency is introduced.

Full Maven parser-test logs are retained under `logs/mvnf/` by `--retain-logs`. The mandatory initial
quick install output is retained in `maven-build.log`.

## Interfaces and Dependencies

`scripts/manage-sparql-ast.py` exposes:

    python3 scripts/manage-sparql-ast.py [--javacc-jar PATH] [--offline] check
    python3 scripts/manage-sparql-ast.py [--javacc-jar PATH] [--offline] regenerate
    python3 scripts/manage-sparql-ast.py [--javacc-jar PATH] [--offline] record

For contributor convenience, the parser also accepts the two options after the subcommand. Exit zero
means success; exit one means drift or an operational/preflight failure; argparse retains exit two
for invalid command syntax.

The implementation uses only Python's standard library. Its external process dependencies are the
repository's JDK `java`, Maven `mvn`, Git `git`, and the verified JavaCC jar. It invokes JavaCC using
the jar's `jjtree` and `javacc` entry points, invokes Maven Spotless with exact `spotlessFiles`
targeting, and invokes `git apply --check` before actual patch replay.
