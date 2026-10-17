# Repair graph explorer query, identity and RDF fidelity

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Work takes place in `/Users/havardottestad/Documents/Programming/rdf4j` on `GH-0000-workbench-graph-explorer`, initially at `bab9451e1e29538f3b6ef10b1a64beb83aa77f4e`. The original repair phase excluded publication; the user subsequently authorized commit and push and explicitly included all current Workbench/RDF4J code and tests, including the six preexisting project changes. Branch changes remain unauthorized. Preserve every untracked artifact; `.claude/` is unrelated and must not be inspected.

## Purpose / Big Picture


The graph explorer must select the same resources as its visible facets, count all loaded resources, keep annotations within the selected graph, and preserve distinct RDF terms during transport and export. Browsers connected to different RDF4J servers with the same repository ID must keep separate saved graphs, metadata and addition queues. Blank-node fixed-reference support is separate work: this change documents the future integration points without enabling syntax absent from this branch.

## Progress


- [done] (2026-10-16) snapshot existing changes and ownership
- [done] reproduce each behavioral defect before production
- [done] integrate three owned root cause implementations
- [done] confirm facets and scoped annotation greens
- [done] confirm RDF fidelity and identity greens
- [done] verify complete browser and module workflows
- [done] format and audit final cohesive changes
- [done] deliver final evidence and review handoff
- [in_progress] publish verified fixes and preserve work

Implementation and verification are complete. The subsequent authorized publication has exactly one active step; the coordinator updates this document at milestones.

## Surprises & Discoveries


The backend selects its repository manager using the `workbench-server` cookie, while the graph explorer originally names its workspace using only the Workbench path and repository ID. The cookie can differ between server requests without changing the graph route URL. Source control evidence is preserved under `logs/workbench-graph-review-fixes/preexisting-dirty-files.diff` and `preexisting-dirty-files.sha256.json`.

## Decision Log


Decision: use the same java.net.URL boundary as RemoteRepositoryManager for identity canonicalization. Rationale: the configured manager accepts underscore and Unicode hostnames that URI.getHost rejects; the in-repo compatibility test confirmed this regression before the helper was repaired. Date/Author: 2026-10-16, Codex coordinator.

Decision: use three owners with serialized Maven and frontend builds. Rationale: independent query and RDF-term work can proceed together while generated artifacts and shared files remain consistent. Date/Author: 2026-10-16, Codex coordinator.

Decision: retain legacy records whose server is unknown under their original keys and never silently import them into a resolved server. Rationale: repository names alone cannot prove data ownership. Date/Author: 2026-10-16, Codex coordinator.

Decision: require observed in-repository failures before every behavior-changing production group despite using a broad ExecPlan. Rationale: the task explicitly requires strict TDD and exact semantic evidence. Date/Author: 2026-10-16, Codex coordinator.

## Outcomes & Retrospective


All seven production fixes and the forward-looking blank-node comments are implemented. Final compiled frontend validation passes 309 tests; classic Workbench unit validation passes 765 tests; the complete Workbench Java suite passes 1,043 tests; current live facet evaluator validation passes 28 tests; and three server-boot packaging classes pass 54 tests. Immutable reports and full command logs are under `logs/workbench-graph-review-fixes/integration/`. The initial checkout and user edits still match the review snapshot at the last audit.

Final browser verification passes all 18 Chromium cases against the patched packaged JAR on two isolated local servers (8090 and 8091), with a temporary Vite proxy on 5174. The final JAR SHA-256 is `37f0889b5af801c42c54b0f9b6fff6b035265aef71256b2a45c572e6aa804579`. A dedicated inspection repository, `graph-review-inspection-mvbfppwx`, contains matching resource identifiers with different A/B labels, directional literals, blank nodes and named-graph annotation controls. The parent reviewer independently exercised generated facets, directional rows, actual Connection switching and retained Query results on the earlier package, and has the final graph URL for a short updated-UI inspection. Browser evidence is under `integration/browser-final-patched/`; independent observations are in `integration/independent-browser-review.md`.

Final parent packaged UI inspection is complete with no console errors. The final formatter passed without changing tracked sources, the eight touched/new Java files passed scoped exact-header/signature checks, and final `git diff --check` and user-WIP preservation audits passed. All required implementation, verification and handoff work is complete. The complete concise repair report is `logs/workbench-graph-review-fixes/final-report.md`.

The first browser run caught a description display regression: ordinary English comments had acquired a visible `@en` suffix. A focused authoritative in-repo red was retained before restoring that existing presentation while keeping directional descriptions explicit. The matching three-case selection and final full 18-case selection pass. The storage browser test was updated to derive the authoritative server tuple rather than inspect the old suffix key; actual compact persistence, reload, clear, undo and redo all pass. A standalone diagnostic initially inherited helper defaults to port8080, producing invalid reload evidence; it created and removed one own disposable in-memory repository there, without changing the foreign process or other data. The error is recorded and all subsequent commands explicitly assert isolated target URLs.

## Context and Orientation


`tools/workbench/src/main/frontend/graph-explorer` contains the React graph page and pure TypeScript query, RDF-term, export, storage and queue helpers. `GraphExplorerApp.tsx` joins these helpers to user interaction. `rdf-explorer.ts` caches metadata and executes graph queries. `tools/workbench/src/main/webapp/scripts/ts` contains classic pages and their graph-addition producer. Java servlet code under `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench` selects the effective RDF4J server, produces initial HTML, and sends a structured page model to browser navigation. A graph scope means the default graph, a named graph, or all graphs selected in the explorer; a storage scope means the exact server and repository owning browser data.

Sol facet worker owns `facet-queries.ts`, `statement-details.ts`, `graph-facets.ts`, `link-counts.ts` and separate query regressions. Luna term worker owns `rdf-terms.ts`, graph term types, `graph-export.ts`, directional persistence validation and term/export regressions. The coordinator owns Java server identity propagation, `main.tsx`, `GraphExplorerApp.tsx`, classic `workbenchViews.ts`, identity/storage scope helpers and integration/browser tests. Shared-file edits require an explicit handoff.

## Plan of Work


First reproduce all reported defects with focused automated tests inside the repository. Query regressions must execute queries generated by the current TypeScript code against RDF4J, and export regressions must parse generated Turtle with Rio and compare RDF models. Preserve initial red summaries immediately in `initial-evidence.txt` and immutable task-specific copies under `logs/workbench-graph-review-fixes` before later verification overwrites Maven reports.

Repair generated facet focus by respecting SPARQL variable scope across both statement endpoints. Keep intrinsic and property facet composition correct for AND/OR, ranges, types and untyped resources. Replace the 120-resource truncation with bounded requests that aggregate every distinct source, shared value counts and global label/top-100 selection while honoring cancellation. Make numeric and date facet expressions follow the same lexical and datatype rules as local matching. Scope optional annotation property triples through the existing graph-scope abstraction.

Preserve `its:dir` from SPARQL JSON through literal term types, node IDs, metadata, workspace validation, queue reconstruction, SPARQL rendering and Turtle export. Reject invalid direction/language/datatype combinations without altering ordinary literals. Allocate valid, collision-free export-local blank labels consistently through edges and embedded statement links rather than sanitizing original identifiers.

Resolve server identity at the backend boundary that chooses the repository manager, remove credentials from the public identity, and carry the authoritative value in HTML/page models and classic route context to the React mount. Use delimiter-safe versioned storage scopes for workspaces, queues and metadata. Retain old unknown-server records without loading them into a known-server scope. Test default/relative server resolution, local file managers, equivalent canonical server spellings, same repository IDs across servers, switch-back restoration and cross-tab queues.

Add comments to shared blank lookup boundaries explaining that ordinary `_:label` is not a fixed existing-node lookup and that pending Jena-style `<_:id>` references should replace IRI-only restrictions after parser/evaluator support merges, preserving server/repository identity. Do not implement the omitted blank-neighbor pagination behavior.

## Concrete Steps


Run commands from the repository root unless stated otherwise. The initial required install is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Keep full output in `maven-build.log`; if offline dependency resolution fails retry once without `-o`, otherwise retry a non-dependency build failure without parallel Maven. Tests must never use `-am` or `-q`. Prefer:

    python3 .codex/skills/mvnf/scripts/mvnf.py GraphExplorerFacetRegressionTest --retain-logs -- -Dformatter.skip=true -Dimpsort.skip=true -DskipFrontend=true
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs -- -Dformatter.skip=true -Dimpsort.skip=true

Use source-level Node tests via `tools/workbench/target/node/node --experimental-strip-types --test <test files>` where practical. Run the full frontend tests and build with existing local npm dependencies, then classic `e2e` unit tests. All Maven installs/verifies, formatter invocations and npm builds acquire the coordinator's single build lock. Source tests can run independently when generated outputs are stable.

Before final formatting, run `scripts/checkCopyrightPresent.sh` from `scripts`; touched Java must have `// Some portions generated by Codex` below the existing header. Audit formatter scope and preserve unrelated changes.

## Validation and Acceptance


A generated query for a property facet must select exactly the expected statement endpoints under source/object variants, AND/OR, intrinsic/type/untyped/range combinations. Named-graph annotation queries must never return annotation properties from another graph, and default/all-graph controls must match their selected scope. A facet requested for 121 or many more resources must include later-only values and globally correct shared counts, labels and top 100.

Rio parsing of exported graphs containing blank identifiers `a.b`, `a_b`, empty, punctuation, Unicode and leading digit/hyphen forms must yield a model isomorphic to the original, including statement links. Directional literals with equal text/language and opposite directions must have distinct IDs and reconstruct exactly through persistence, queues and export; ordinary literals remain compatible and invalid combinations fail predictably.

Browser acceptance is the actual app flow: open Graph on server A/repository X, add and save a graph, switch to server B/repository X and observe an isolated graph and queue, then switch back to A and recover its graph. Verify cross-tab queue isolation, meaningful rendered content, no framework overlay or console error, and relevant filter/export interaction. Browser plugin is not listed, so the repository's existing Playwright workflow is the permitted fallback. Capture screenshots outside the repository and give the parent milestone URL and flow for independent inspection.

## Idempotence and Recovery


Never reset, restore, clean, broadly stage, remove artifacts or overwrite the user's changes. Preserve previous logs before replacing shared log names. Re-run focused failures only after diagnosing them; missing dependencies receive the one authorized online retry, then return offline. Maven output and tests are reproducible using the workspace-local `.m2_repo`. If scope identity is unavailable, browser artifacts must remain unclaimed rather than borrowing a server from a cookie or URL guess.

## Artifacts and Notes


Initial user changes are captured in `logs/workbench-graph-review-fixes/preexisting-dirty-files.diff` and `preexisting-dirty-files.sha256.json`. Existing `initial-evidence.txt`, when present, was copied into the same directory before new task evidence. Each worker stores immutable red and green artifacts in its own subdirectory. Final evidence includes exact commands, report filenames, totals and limitations without treating a quick build as runtime proof.

## Interfaces and Dependencies


Use only existing Java/RDF4J, React, TypeScript, Node and Playwright dependencies. Do not add dependencies. The backend public page context must expose a credential-free canonical server identity distinct from the repository ID. The frontend identity helper must serialize a versioned tuple of Workbench path, server identity and repository ID so delimiter characters cannot collide. Existing storage APIs can retain arbitrary string scope keys; they must not migrate unknown-server records into the new tuple automatically.

Revision note (2026-10-16): created the complete repair plan and ownership map before production edits; strict TDD, legacy retention and serialized builds are explicit acceptance requirements.

Revision note (2026-10-16): live RDF4J initial failures are captured (facets20 tests17failures; identity3tests3failures). Added identity regressions and fixes for cross-tab transport guards, kept Query authoritative revalidation, server-scoped shelved controllers/activity, standalone identity resolution, workspace/queue legacy retention and metadata isolation. All new red logs are immutable under the task log directory; production groups are now implementing.

Revision note (2026-10-16): initial implementation groups are green (facet evaluator26/26, RDF/export frontend64/64, Rio32/32, identity3/3). Broader validation added a configured-host compatibility regression, observed its URI rejection, and repaired the helper with the existing URL boundary. Final module and real-browser verification is now active.

Revision note (2026-10-16): explicit empty-default dataset exposed two all-scope annotation failures; independent graph scan variables repair them (whole evaluator28/28). Long-IRI request-size regressions then exposed three HTTP413 handling failures; count requests now split rejected batches without losing sources, and cancellation/staleness remains exact (9/9 matching unit cases). The broad pre-final checks were frontend302, classic765 and Java1041; final verification reruns the changed frontend/evaluator paths and current packaged runtime.

Revision note (2026-10-16): final source freeze passed frontend309, classic765, Workbench Java1043, live evaluator28 and boot packaging54. The browser workflow is delegated to the same Luna owner with five existing/new Chromium specs, unique output paths and routine recovery ownership. Two isolated servers and the standalone Vite route are live for independent parent inspection; no production edits are planned unless actual runtime evidence requires them.

Revision note (2026-10-16): browser red exposed a nondirectional description presentation regression; preserved failing assertion preceded the final render correction. Frontend309 and typecheck/build passed again, the root quick install repackaged current sources, focused browser3/3 passed, and full browser18/18 passed on ports8090/8091/5174. Complete module verification is done; final formatting and preservation audit are the only active plan step.

Revision note (2026-10-16): parent independently inspected the final packaged UI and actual earlier Turtle download. Final scoped header check, formatter (no tracked changes), diff check and six-file WIP preservation audit pass. All implementation and verification requirements are complete; final evidence/report handoff is the only active step. Runtime remains available as requested.

Revision note (2026-10-16): the user subsequently authorized commit and push. The 66 repair source/test files are staged; all six original WIP deltas remain unstaged. A temporary copy with the original baseline patch reversed produced repair-only blobs for the two overlapping files, and an independent Luna review confirmed there is no semantic dependency on the excluded Cancel-delay or Graph-menu changes. Every staged blob matches the verified repair delta; no retest is necessary. Logs, local ExecPlan and unrelated untracked artifacts are excluded from publication. The sole active step is non-force publication and local/remote SHA verification.

Revision note (2026-10-16): the user broadened publication to all current Workbench/RDF4J code/tests after the first repair commit. Two local commits now contain all70 distinct source/test files: d481a1f0de6c76a44f5922cf244b3a92dc94b065 (repairs) and e84956087ebfc8f7f5094b6b0b9fec1599cf3495 (six original project changes). No amend occurred. The two exact Cancel-delay browser cases passed2/2 on the existing final8090 package; all tracked changes are committed and the index/worktree are clean. Logs, local plan and all task artifacts remain excluded. Automatic approval review rejected the push twice, including the one allowed retry after official public-repository and live branch provenance verification. The parent has presented explicit public-destination/payload confirmation and is awaiting the human response; no further push is permitted before that response. Remote remains bab9451e1e29538f3b6ef10b1a64beb83aa77f4e. Publication is the sole active step, with final evidence at logs/workbench-graph-review-fixes/publication/publication-report.md.
