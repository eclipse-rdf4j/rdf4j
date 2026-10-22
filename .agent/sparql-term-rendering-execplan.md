# Contain RDF terms when writing remote SPARQL text

This ExecPlan follows `.agent/PLANS.md`. Keep its progress, discoveries, decisions, and evidence current while implementing it.

## Purpose / Big Picture

A client can supply an RDF term containing SPARQL punctuation to SPARQLRepository or FedX. Several renderers currently place that term directly into a remote query or update. A crafted IRI, literal, datatype, language tag, or blank-node identifier can then change the parsed operation or break its syntax. After this change, each term either retains its exact RDF value in a single SPARQL term or is rejected before sending remote text. A reader can verify the behavior with parser-backed regression tests and the module test suites below.

## Progress

- [x] [done] Inspect isolated branch and install dependencies (2026-10-22 21:28Z).
- [x] [done] Capture smallest in-repo red rendering regression (2026-10-22 21:33Z).
- [x] [done] Extend regressions across term renderers and nesting (2026-10-22 21:38Z).
- [x] [done] Implement shared lexical rules at rendering boundaries (2026-10-22 21:50Z).
- [x] [done] Run focused and affected module verification (2026-10-22 22:09Z).
- [x] [done] Review design before scoped draft publication (2026-10-23 00:10Z).
- [ ] [in_progress] Publish scoped draft and verify exact head.
- [ ] [todo] Monitor exact-head checks to completion.

## Surprises & Discoveries

- `core/query/src/main/java/org/eclipse/rdf4j/query/impl/BlankNodeReferences.java` already has `toIRIRef`, which escapes SPARQL IRIREF punctuation and rejects invalid Unicode surrogates. The SPARQL parser grammar accepts `UCHAR` inside `Q_IRI_REF` in `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt`.
- `core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/query/SPARQLOperation.java` has a separate legacy renderer from `QueryStringUtil`; FedX also has separate value, filter, and `FROM NAMED` paths. The previous PR #6163 explicitly left `SPARQLOperation` untouched, so this plan tests it directly.
- `SimpleLiteral.setLanguage` only rejects null and empty strings. A malformed language suffix can therefore reach a renderer through normal model objects.
- The first offline `mvnf` run needed `maven-dependency-plugin:3.11.0`. The single online retry fetched it and produced the intended Surefire red: `QueryStringUtilTest.delimiterBearingIriRemainsOneStatementTerm` could not parse the injected `;` outside the IRIREF.
- `Values.iri` and `Values.getValueFactory` validate inputs early; `SimpleValueFactory` is needed in regression tests to represent the permissive RDF values already accepted by normal model code. Repository variant tests were red at 29 tests, 4 failures, 5 errors. FedX variant tests were red at 14 tests, 3 failures, 3 errors. Notably, `FROM NAMED` split one supplied graph IRI into two parsed graphs.
- SPARQL `INSERT DATA` uses the Turtle data-block parser, which applies `ParsedIRI` syntax validation after IRIREF decoding. Query expressions can round-trip an escaped delimiter-bearing IRI, but update data blocks cannot. The connection now rejects such malformed update IRIs before remote send. A separate focused red test showed the deprecated operation's raw `BASE` IRI insertion accepted a malformed base; it now validates that base first. Another focused red showed FedX filter constants lacked recursive triple-term support; the filter renderer now recurses.
- A focused Surefire test showed `SPARQLQueries.escape` leaves NUL raw. The shared renderer now rejects NUL; new tests show other C0 characters at U+0001 and U+001F round-trip through the parser, while standard escaped controls retain their existing behavior. A separate focused red showed the deprecated operation's `setBinding` assertion excluded nested triple terms even though its renderer handled them; it now admits them, and a parser-algebra assertion verifies both nested levels.

## Decision Log

- Decision: Implement against detached main at `3d292763558604affd89c7d4256e8df5720bd895` on isolated branch `GH-0000-sparql-term-rendering`; use open PR #6163 only as threat context, not as a patch source. Rationale: current tree includes blank-node reference and graph identity semantics the older PR did not address. Date/Author: 2026-10-22, Codex.
- Decision: Test parser-visible term identity and operation containment, as well as exact ordinary term spelling. Rationale: a string that merely lacks an obvious payload could still parse as a different term. Date/Author: 2026-10-22, Codex.
- Decision: Keep token-aware variable substitution as a separate follow-up. Rationale: `getUpdateString` and deprecated `getQueryString` replace regex matches across comments, literals, and IRIs and interpolate unquoted variable names; that is a distinct query-tokenization defect from term serialization and needs its own failing tests. Date/Author: 2026-10-22, Codex.
- Decision: Grade user compatibility impact medium. Rationale: ordinary SPARQL term spelling is unchanged, and malformed update IRI spellings previously failed at the remote parser but now fail locally. The connection formerly converted blank-node graph contexts into `urn:nodeid:` graph IRIs; those writes now fail explicitly because their identity can collide with a real IRI graph. Any deployment using those contexts must migrate them to real graph IRIs. Invalid language tags, blank-node labels, NUL literals, and IRI spellings beginning `_:` are also rejected at the rendering boundary. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Pending implementation and verification.

## Context and Orientation

`core/repository/sparql` sends SPARQL queries and updates to a remote endpoint. `query/QueryStringUtil.java` and `query/SPARQLOperation.java` inline bound terms. `SPARQLConnection.java` assembles graph size, clear, insert, delete, and pattern updates. `tools/federation` (FedX) composes remote queries in `util/QueryStringUtil.java`, including dataset `FROM` clauses and recursive triple terms, and pushes comparison filters through `util/FilterUtils.java`. An IRIREF is the `<...>` syntax for one IRI. A language tag follows a literal after `@`. A blank-node identifier follows `_:`; unlike an IRIREF, it cannot be freely escaped without changing its identity. A triple term contains subject, predicate, and object RDF terms and may nest.

The worktree root is `/Users/havardottestad/.codex/worktrees/security-sparql-term-rendering/rdf4j`. `.m2_repo` links to the existing Maven cache. For tests use `.codex/skills/mvnf/scripts/mvnf.py --workspace sparql-term-rendering --retain-logs`, which installs dependencies before each verify and retains separate logs. Never use Maven `-am` or `-q` with tests.

## Plan of Work

First add a parser-backed regression to `core/repository/sparql/src/test/java/org/eclipse/rdf4j/repository/sparql/query/QueryStringUtilTest.java` for an IRI that closes an IRIREF and injects an update. Run that method alone, copy the Surefire failure into root `initial-evidence.txt`, and leave its full log intact before touching production sources. Add focused tests for connection graph contexts and statement terms, legacy operation bindings, FedX value and nested triple terms, FedX dataset graph sources, filter constants, and benign syntax. Tests should cover delimiters, controls, backslashes and quotes, datatype IRIs, malformed language tags, and blank-node identity/collision.

Then place reusable lexical validation or escaping in an existing lower-level SPARQL abstraction, reusing `BlankNodeReferences.toIRIRef` and `SPARQLQueries.escape` where their contracts fit. Route every audited output boundary through the same rules while preserving ordinary spelling. Reject unrepresentable blank-node identities before remote text is emitted; retain FedX's trusted graph binding path. Avoid new dependencies and avoid changing unrelated query construction.

The implementation is acceptable only if a malicious term cannot add an operation, clause, graph, or triple pattern; parseable terms round-trip to the original RDF value; nested triple terms recurse safely; and existing valid syntax remains stable.

## Concrete Steps

From the worktree root, the initial prerequisite has run:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

It reported `BUILD SUCCESS` and wrote `maven-build.log`. Next run the smallest selected regression using:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-term-rendering --retain-logs QueryStringUtilTest#<method>

After recording the initial failure, use matching focused selections, then verify both modules:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-term-rendering --retain-logs core/repository/sparql
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-term-rendering --retain-logs tools/federation

Run `scripts/checkCopyrightPresent.sh` from its own directory, Spotless `process-resources` for touched modules with `-Dmaven.repo.local=.m2_repo`, and `git diff --check`. Update this plan with actual commands and reports before review.

## Validation and Acceptance

The selected new test must fail before production edits with a Surefire report snippet retained in `initial-evidence.txt`, then pass unchanged afterward. Both affected modules must complete their test suites without new failures. Parser-backed tests must show that crafted delimiter text remains one RDF term and cannot become `DROP ALL` or a second pattern; ordinary IRIs, language literals including direction, typed literals, graph contexts, and nested triple terms must still parse to their original values. Where a term has no safe SPARQL representation, the API must fail before issuing remote text.

## Idempotence and Recovery

`mvnf` may clean its isolated test reports, so persist the first failure immediately and retain its timestamped log. Repeat a failed offline Maven dependency resolution once online, then return offline. Keep evidence files and unrelated untracked data. Stage only reviewed source, tests, and this plan after parent design review; do not transplant commits from PR #6163.

## Artifacts and Notes

Initial red evidence is retained in root `initial-evidence.txt`; the report says `Tests run: 1, Failures: 0, Errors: 1, Skipped: 0`, with `MalformedQueryException` at the injected `;`. Full log: `.mvnf/workspaces/sparql-term-rendering/logs/20261022T213108.132760Z-77723-89c540fa/verify.log`. Candidate green evidence will be added later. Root `maven-build.log` records the initial quick install.

Expanded pre-fix results are retained in `variant-evidence-repository.txt`, `variant-evidence-federation.txt`, `variant-red-repository.log`, and `variant-red-federation.log`. The repository selection had 29 tests, 4 failures, 5 errors; the federation selection had 14 tests, 3 failures, 3 errors. Additional red evidence for legacy `BASE` and nested FedX filter terms is retained in `base-initial-evidence.txt`, `base-red.log`, `filter-nested-initial-evidence.txt`, and `filter-nested-red.log`.

The focused selections passed: `core/repository/sparql` 30 tests, zero failures or errors in `focused-repository-green.log`; FedX 15 selected tests, zero failures or errors in each of three Surefire passes in `focused-federation-green.log`. The additional parser-backed nested-filter selection passes in `filter-nested-green.log`, and the nested legacy binding passes in `legacy-nested-green.log`. The complete `core/repository/sparql` module passed 102 tests, zero failures/errors, in `.mvnf/workspaces/sparql-term-rendering/logs/20261022T215914.850353Z-51580-329b1cb4/verify.log`. The complete FedX module passed 604 tests, zero failures/errors, 52 skipped, in `.mvnf/workspaces/sparql-term-rendering/logs/20261022T220111.028116Z-52835-9a00fc85/verify.log`. The complete parser module passed 387 tests, zero failures/errors or skips, in `.mvnf/workspaces/sparql-term-rendering/logs/20261022T220757.180329Z-88877-ee5fee11/verify.log`. Surefire/Failsafe reports are in the corresponding isolated build directories.

New failures are preserved in `nul-initial-evidence.txt`/`nul-red.log` and `legacy-nested-initial-evidence.txt`/`legacy-nested-red.log` before their production fixes.

## Interfaces and Dependencies

Use existing `core/query` IRIREF encoding and existing `core/queryparser/sparql` literal escaping. Both affected modules already consume these libraries. Any shared helper must be internal use only and should expose narrow methods for one IRIREF, one literal, and, if needed, a syntactically valid blank-node label. Do not add third-party dependencies or change the public repository API.

Plan revision note (2026-10-22): Initial plan established after source-path inspection and before writing the first regression or editing production code.

Plan revision note (2026-10-22): Recorded the first in-repo Surefire failure and advanced to variant regressions; no production code has been edited.

Plan revision note (2026-10-22): Added and observed repository/FedX variants, corrected test fixture factories, and advanced to the shared rendering implementation.

Plan revision note (2026-10-22): Focused repository/FedX selections pass after shared lexical containment, update URI syntax validation, legacy base validation, and recursive filter triple terms. Advanced to module verification and review.

Plan revision note (2026-10-23): Parent review requested parser-algebra identity assertions and control-character behavior. Added those tests, observed NUL and legacy nested-binding reds, fixed them, and confirmed the full repository module at 102 passing tests. FedX and parser modules remain in verification.

Plan revision note (2026-10-23): All three affected module suites passed. Formatting and copyright checks passed. Advanced to final scoped review and parent approval before authorized publication.

Plan revision note (2026-10-23): Parent final review found no blocker and authorized the scoped commit and draft PR. Advanced to publication and exact-head verification.
