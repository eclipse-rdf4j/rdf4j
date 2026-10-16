# Preserve and verify SPARQL generated sources

This ExecPlan is maintained under `.agent/PLANS.md` and `PLANS.md`.

## Purpose / Big Picture

Developers and CI will rebuild the parser and its customized AST classes from an empty directory, apply tracked Git patches, format the result with the project's formatter, and fail on any byte or file-set difference. Current Java sources and the grammar must remain byte-identical throughout this task. The default command is a source-read-only check; patch maintenance is an explicit separate command.

## Progress

- [done] inspect audit and verify generator provenance
- [done] capture failing patch replay regression
- [done] implement independent generation and patch maintenance
- [done] verify real roundtrip and negative controls
- [done] add documentation and continuous integration
- [done] prove final Java source byte integrity
- [done] review final implementation and evidence handoff

## Surprises & Discoveries

Fresh JavaCC 7.0.12 emits 170 Java files. This repository contains 173 marked generated Java files plus nine handwritten Java support files in the AST directory. Three marked files are not emitted by today's grammar: ASTAnnotation.java, ASTPathNegatedPropertySet.java, and JavaCharStream.java. They must be added by a tracked compatibility patch, rather than seeded from checked-in Java during checks. The old patches fail even at the merge-base. The current branch's lookahead layout is semantically consistent but differs from fresh generation, so the refreshed parser patch must preserve it.

## Decision Log

Use the published Maven Central artifact net.java.dev.javacc:javacc:7.0.12, verified by a fresh download and the published SHA-1, and pin SHA-256 067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13. This prevents local jar provenance from becoming an assumption. Use Python standard library tests and real Git application; no new runtime dependency is needed. Keep the existing double-dot patch filename to avoid unnecessary renaming, but correct all documentation to match it. Checks never derive patches, headers, checksums, or generated contents from the expected Java files. A manifest declares generator output, patched output, and handwritten exclusions, making missing/extra files observable. Date/author: 2026-10-16, Codex.

## Outcomes & Retrospective

The feature is implemented and independently validated on Java 25. Empty-directory generation, Git patch replay, forced project formatting, exact comparison, explicit scratch export, and patch recording preserve all 173 generated files. All 21 Python tests passed in 73.148 seconds, including real negative controls and real formatter normalization when the POM inherits formatter.skip=true. SPARQLParserTest passed the same selection before and after the non-Java infrastructure changes: 70 tests each, zero failures, errors, or skips. A cold-cache online replay passed from an isolated root that initially had no .m2_repo; it populated 73 MB of dependencies and verified the pinned jar. No current Java source or grammar bytes changed: all 4,641 frozen hashes match. The dirty theme and pre-existing untracked work remain intact; staged diff is empty. At the validation handoff, no commit, push, or branch change had occurred. The GitHub Actions definition was present, but remote CI had not run because nothing had been pushed at that time.

The initial real regression failed before patch refresh, and three later boundary regressions failed before their narrow fixes. The final pipeline applies patches before formatting; format skipping is explicitly overridden, and expected Java contents do not enter generation, replay, or formatting. Record mode may read reviewed Java deliberately, verifies its candidate patches through an independent replay, and never updates Java itself. Parent final review and handoff are complete; implementation and independent verification are complete.

## Context and Orientation

The grammar is core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt. JavaCC is the parser generator; JJTree is its tree-generation preprocessor. The generated-source directory also contains customized AST classes that inherit from handwritten base classes. Existing patches live in core/queryparser/sparql/JavaCC/patches. New tooling lives in scripts/manage-sparql-ast.py and scripts/test_sparql_ast.py. A new GitHub Actions workflow runs all checker tests and the exact same real replay command without path filters. The existing dirty Hugo theme submodule and untracked user notes must be preserved.

## Plan of Work

First create an automated regression that generates into an empty temporary directory and checks the existing patches with real Git; record its failure before changing tooling or patches. Implement a CLI with default --check, explicit --record-patches, a checksum-verified jar cache, a declared source manifest, exact file-set and byte comparison, actionable residual diffs, and retained failure evidence. Generation and formatting run in scratch, with only grammar, build configuration, and patches copied in; expected Java sources are read only during final comparison. Patch maintenance intentionally computes Git diffs against current Java sources and verifies the resulting replay before publishing patch changes. Refresh the two existing patches and add one compatibility patch for remaining AST classes, support files, and generated additions. Test real success and isolated source, grammar, and patch failures. Document local offline behavior and CI's explicit network access.

## Concrete Steps

Run from repository root:

    python3 -m unittest discover -s scripts -p test_sparql_ast.py -v
    python3 scripts/manage-sparql-ast.py --check --jar /path/to/javacc-7.0.12.jar
    python3 scripts/manage-sparql-ast.py --record-patches --jar /path/to/javacc-7.0.12.jar

The checker defaults to offline dependency resolution. CI passes --online to fetch the pinned jar and missing Maven formatter dependencies. Python tests use SPARQL_AST_JAVACC_JAR to require actual JavaCC integration controls. Maven commands always use the workspace-local .m2_repo. Focused parser pre/post verification uses the same SPARQLParserTest selection through mvnf in an independent HEAD scratch checkout, because the source checkout must not be formatted or modified.

## Validation and Acceptance

The initial real patch replay regression must fail with a Git patch-application error. After implementation, the same regression and unit tests must pass. The real checker must report 173 exact source matches, and report nonzero status for isolated source drift, grammar drift, missing or corrupt patches, missing or extra generated files, generator/formatter failure, and a wrong jar checksum. Source drift tests must mutate only temporary fixtures. Before and after the task, hash all tracked Java sources and the grammar; every hash must match. Git diff must contain only intended tooling, patch, documentation, CI, plan, and evidence changes. At the validation handoff, no commit, push, or branch change had been authorized. The user later explicitly authorized committing and pushing this cohesive scope on the current branch.

## Idempotence and Recovery

Repeated checks must preserve source and patch bytes. Failure logs and residual diffs stay in a reported evidence directory. Patch recording is explicit, stages candidate patches in scratch, verifies their full replay and formatting, and only then updates the owned patch files. Missing offline dependencies produce a clear instruction to use --online once. A checksum mismatch always fails rather than silently replacing an unexpected jar. No generated source is overwritten by either command; regeneration output remains in scratch for review.

## Artifacts and Notes

Evidence directory: logs/gh-514-generated-fix-20261016. Generator provenance: freshly downloaded Maven Central jar SHA-256 067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13, published and locally computed SHA-1 both 0dc6b0336863fb33c8ec7ce2d0c77cd22fe11bd7. The initial root build and earlier exact source inventory remain under the audit evidence directory.

## Interfaces and Dependencies

Use Python 3.10+, Git, Java 25+, and Maven. The checker exposes an internal WorkflowError for concise command failures, validates manifest paths before use, verifies the generator's SHA-256, generates into an empty AST directory, applies every manifest-listed patch with git apply --check followed by git apply, invokes project formatting in scratch, and compares final source bytes. Tests may use temporary repositories and real Git rather than requiring mocks for basic correctness.

Revision 2026-10-16: created for the newly authorized compliance repair and CI feature, preserving the frozen Java-source constraint.

Revision 2026-10-16: explicit reviewed-directory patch recording enables scratch-only customization adoption. Full 18-test suite and exact 173-source replay passed independent validation. Added three failing boundary controls before fixes: nested patches, unsafe output directories, and inherited formatter skipping. The formatter now forces formatter.skip=false and a real normalization control verifies it changes unformatted patched code. Independent Luna validation owns the final 21-test suite, matching parser pre/post selection, cold-cache CI probe, and integrity checks; do not duplicate its test execution.

Revision 2026-10-16: independent final validation passed the exact Java 25 CI-style replay, 21 Python tests, parser 70/70 pre and post, and a cold-cache online run. Evidence is retained under logs/gh-514-generated-validator-20261016T195637Z; all 4,641 frozen hashes match. Progress now leaves only final parent review/handoff in progress.

Revision 2026-10-16: parent final review inspected the implementation, real red/green evidence, CI wiring, final handoff, and diff checks with no remaining findings. All plan steps are complete. Evidence remains retained; no code changes or test reruns were needed.
