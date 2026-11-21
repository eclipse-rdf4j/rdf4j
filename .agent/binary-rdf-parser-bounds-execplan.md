# Bound Binary RDF parsing to received data and declared values

This ExecPlan follows `.agent/PLANS.md` and is updated as the work proceeds.

## Purpose / Big Picture

Binary RDF documents can claim enormous string lengths and value IDs before supplying corresponding bytes or declarations. The parser currently doubles a claimed string length when allocating, shifts version 1 lengths without overflow checks, indexes value declarations without range/defined checks, and keeps buffers and values across parses. Version 2 integer decoding also accepts overlong and overflowed encodings. After this change, malformed streams fail with parse or I/O errors, memory grows with actual input and declarations, valid large strings and sparse IDs remain readable, and a reused parser has a fresh document scope.

## Progress

- [x] [done] Inspect isolated checkout and parser contracts.
- [x] [done] Add smallest malformed input red tests.
- [x] [done] Add sparse, reuse, and streaming variants.
- [x] [done] Implement bounded decoding and lifecycle fixes.
- [x] [done] Verify focused and full affected modules.
- [x] [done] Add negative-ID red regression.
- [x] [done] Reject negative IDs and reverify.
- [x] [done] Review scope and publish draft PR.
- [x] [done] Reproduce nested triple stack exhaustion.
- [x] [done] Implement iterative value term parsing.
- [x] [done] Reverify nested triple follow-up.
- [x] [done] Obtain follow-up scope review.
- [ ] [in_progress] Commit and push approved follow-up.
- [ ] [todo] Monitor exact-head CI to completion.

## Surprises & Discoveries

`BinaryRDFParser.readBytes(int)` allocates `new byte[length << 1]` before reading and retains the result in a parser field. Version 1 length-to-byte conversion also shifts without overflow checks. `IOUtil.readVarInt` has no five-byte or canonical-encoding check and can wrap its Java shift count. A value declaration grows an array only once, regardless of the ID; references index the array without checking declaration state. `AbstractRDFParser.clear()` does not clear the binary parser's arrays. The format documentation explicitly illustrates declaration ID 42 and permits reassignment, so sparse and repeated IDs should remain supported. The built-in writer emits nonnegative sequential IDs with optional reuse.

## Decision Log

- Decision: Use a separate clean managed worktree from main `3d292763558604affd89c7d4256e8df5720bd895` on branch `GH-0000-binary-rdf-parser-bounds`. Rationale: this parser fix must not stack on other security PRs. Date/Author: 2026-10-23, Codex.
- Decision: Preserve nonnegative sparse value IDs using a dense array only while preceding slots are populated, backed by a map for gaps. Rationale: the format shows a sparse ID as valid; one attacker-controlled integer must not force a huge array, while normal sequential writer output should keep array speed. Date/Author: 2026-10-23, Codex.
- Decision: Reject negative version 1 IDs at the shared ID reader. Rationale: the format describes signed storage but does not grant negative reference IDs; writer output is nonnegative and the old parser could not resolve negative IDs. Date/Author: 2026-10-23, Codex.
- Decision: Grow a string byte array only after the corresponding prior chunk has been read, and avoid retaining large arrays after parsing. Rationale: truncated claimed lengths should consume memory proportional to bytes actually received; valid large strings still decode. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The initial version 1 overflow regression failed with an unchecked range exception. The expanded parser class failed in 8 of 9 cases before the fix, and the malformed varint test failed in `IOUtilTest`. Negative-ID tests then failed in two cases against the candidate before their fix. After implementation, the full common IO module passed 103 tests with one pre-existing skip, and the full Binary RDF module passed 182 tests with four pre-existing skips. No failures or errors remain. Copyright validation and the root formatter passed. Publication and exact-head CI remain.

## Context and Orientation

`core/rio/binary` contains `BinaryRDFParser` and `BinaryRDFWriter`. Version 1 stores UTF-16BE strings with a 32-bit count of code units; version 2 stores the encoded byte count as an unsigned variable integer. `core/common/io/IOUtil` implements the version 2 integer decoder, and its production read callers are in this parser. The parser's `Value[] declaredValues` caches reference IDs for the current document. Format documentation is `site/content/documentation/reference/rdf4j-binary.md`, especially the value-declaration section.

## Plan of Work

First create the smallest in-repo failing tests for a version 1 string length that overflows when doubled and a version 2 overlong integer. Run their individual methods without `-am` or `-q`, and preserve the initial Surefire report in top-level `initial-evidence.txt` before touching production. Add focused variants for truncated large claims, canonical boundary integers, sparse/reassigned/undefined reference IDs, negative version 1 IDs, parser reuse after success/failure, retained buffer capacity, and valid large round trips. Then make the parser consume bytes incrementally and validate arithmetic before reading. Tighten `IOUtil.readVarInt` to the canonical five-byte nonnegative encoding. Keep the normal dense ID path fast and use sparse storage for gaps. Reset document-specific state at every parse boundary. Run both affected modules, formatting, copyright, and scoped diff checks.

## Concrete Steps

The required initial root quick clean install passed in this worktree:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace binary-rdf-parser --retain-logs --module core/rio/binary <Class#method>` and the analogous `core/common/io` selection for tests. Keep Surefire reports under `.mvnf/workspaces/binary-rdf-parser/build/.../surefire-reports`; persist first red output with `scripts/agent-evidence.py`. Run full modules with `mvnf`, without `-am` or `-q`. Format with root Maven `process-resources` without `-q`, run `scripts/checkCopyrightPresent.sh`, and stage only reviewed files and this plan.

## Validation and Acceptance

The first tests fail before production changes and pass unchanged afterward. Invalid, overflowed, and noncanonical lengths/IDs produce checked parse or I/O failures, never unchecked indexing/allocation errors. Sparse and repeated IDs retain documented behavior; an undefined reference fails explicitly. Successful or failed parse state cannot leak into a later parse on the same parser. A short body claiming a huge string does not trigger a huge pre-read allocation. Writer-produced large strings and both format versions still round-trip. Full affected module tests pass.

## Idempotence and Recovery

Keep the worktree, Maven logs, and all untracked artifacts. Preserve red evidence before later tests overwrite Surefire reports. Never use `-am` or `-q` when testing. The branch is separate from the SPARQL and other security branches. If offline dependencies are missing, retry the same Maven selection once online, then return offline.

## Artifacts and Notes

The initial root install writes `maven-build.log`. Record red and green report paths here after execution. Draft PR publication and exact-head CI are later steps.

## Interfaces and Dependencies

Keep `BinaryRDFParser`'s public API and protected `readBytes(int)` contract. No new dependency is needed. `IOUtil.readVarInt` remains the same public signature, but rejects values the writer cannot produce. Sparse IDs can be held in a standard Java map while sequential IDs use the existing array. Do not impose an arbitrary string-length cap; bound allocation to the bytes read and reject lengths Java cannot represent.

Plan revision note (2026-10-23): Created this independent plan after branch/source inspection. The initial clean install is in progress; no production changes have been made.
Plan revision note (2026-10-23): Root quick install passed. The smallest version 1 overflow test failed with `IndexOutOfBoundsException` instead of checked `IOException`; preserved the Surefire report in `initial-evidence.txt`. Add related malformed/valid format cases and the `IOUtil` decoder regression before editing production.
Plan revision note (2026-10-23): Expanded parser test ran red (9 tests, 7 failures, 1 error) and was preserved in `binary-variants-evidence.txt`. The IOUtil varint regression ran red (1 failure), preserved in `varint-initial-evidence.txt`. Begin production implementation.
Plan revision note (2026-10-23): Bounded byte growth, dense plus sparse declarations, explicit undefined-reference errors, parse-state cleanup, and strict varint decoding are implemented. Focused IOUtil class passed 4 tests and parser class passed 9 tests. Added two final related variants before full-module verification.
Plan revision note (2026-10-23): Full common IO and Binary RDF module suites passed (103 and 180 tests respectively). The expanded BinaryRDFParserMalformedTest has 10 passing tests. Copyright and root process-resources formatter passed; begin scoped final review and request parent candidate review before publication.
Plan revision note (2026-10-23): Parent review identified that negative version 1 reference IDs are not established as valid by the format despite their signed storage type. Add new red tests for negative declarations and references, then reject them as malformed before publication.
Plan revision note (2026-10-23): Negative declaration and reference tests ran red in the candidate: 12 tests, 2 failures. Preserved report as `negative-id-initial-evidence.txt` before changing production.
Plan revision note (2026-10-23): Shared ID validation now rejects negative declarations and references before value parsing or table lookup. The focused parser class passed 12 tests; the full Binary RDF module passed 182 tests with four skips. The final formatter passed.
Plan revision note (2026-10-23): Published draft PR #6215 at `bf196608f2a66121f7b6d4a9c7fa3889eaf83ca2`. Parent review then identified unbounded recursion in nested TRIPLE_VALUE parsing. Add a red-first stack regression and compatible iterative fix as a separate follow-up commit on this same PR; do not amend.
Plan revision note (2026-10-23): A 50,000-level nested triple statement failed in Surefire with StackOverflowError through `readTripleTerm/readValue`; preserved `triple-stack-initial-evidence.txt` before implementation.
Plan revision note (2026-10-23): `readValue` now uses pending frames for nested triples and a direct path for ordinary values. The focused stack test and full malformed-parser class pass (16 tests, 0 failures/errors); added coverage for nested declarations/references and invalid subject/predicate types. Run the full module, format, and review before a follow-up commit.
Plan revision note (2026-10-23): Full Binary RDF module passed 186 tests, 0 failures/errors, 4 skips. Copyright, root formatter, and diff check passed. Sent the three-file follow-up candidate to parent for review before pushing a new head.
Plan revision note (2026-10-23): Parent approved the iterative nested-triple candidate for a new commit on draft PR #6215. Preserve the separate preexisting NULL_VALUE triple-object factory exception for later assessment; do not delay the stack fix.
