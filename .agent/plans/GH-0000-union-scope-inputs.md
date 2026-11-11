# Preserve valid inputs while optimizing UNION joins

This ExecPlan follows `.agent/PLANS.md` and is a living document. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. The source baseline is main commit `eb9b76be580eed238009c7a359e9a928148bd368`. The local branch is `GH-0000-union-scope-inputs`; no issue number was supplied. The initial implementation was delivered without a commit; the user subsequently authorized committing and pushing this branch to the HASMAC fork on 2026-11-11.

## Purpose / Big Picture


Queries containing a property path followed by UNION inside OPTIONAL should use the path's bound endpoint to plan and execute connected UNION patterns when input propagation is semantically safe. Adding an unrelated mandatory triple outside OPTIONAL must not disable that proof. Rebuilding a join in one UNION arm must not make the other arm forget an already selected path operand. The work must preserve the complete multiset of result mappings, including duplicates, unbound variables, BIND failures, and independent subquery behavior.

The scope optimizer currently abandons candidate input collection whenever a non-Join ancestor has another Join above it. A distinct binding-analysis defect assigns a different identity to an unchanged lexical scope after cache invalidation. The implementation addresses both problems with separate regressions. More general scope removal must also account for how the containing OPTIONAL changes its input frames when its directly attached UNION stops requiring independent evaluation.

## Progress


- [x] (2026-11-11) Inspect current main and repository instructions.
- [x] (2026-11-11) Reproduce input and identity failures: model 5/5 failing; evaluation regressions and direct OPTIONAL result/effect failures captured.
- [x] (2026-11-11) Implement contextual UNION propagation proofs; all 47 focused evaluation tests pass.
- [x] (2026-11-11) Preserve stable lexical scope identities; derived caches still invalidate and all new/replacement boundaries allocate fresh tokens.
- [x] (2026-11-11) Verify focused and module regressions: 47 focused tests pass; complete suites report 4,841 tests, zero failures/errors, two existing skips.
- [x] (2026-11-11) Format changes and package evidence; patch applies to the pinned base and reconstructs every complete changed file.

## Surprises & Discoveries


The earlier scratch checkout is no longer present. A fresh clone is available at `/workspace/scratch/5c2cf927d55d/rdf4j-review`. JDK 25 and Maven are already installed in a shared toolchain. A separate copy of a populated Maven repository is being prepared for this checkout so installing these changes cannot overwrite another task's snapshot artifacts.

Previous executed diagnostic probes established the path/UNION failure and scope-identity failure, but repository instructions require new failing in-repository tests before production changes. The direct OPTIONAL RHS counterexamples are now executed: three change result bags, and one changes the number of RAND condition evaluations from two to one. The latter requires a repeatability guard in addition to binding-frame equivalence. A test assumption was corrected: ZeroLengthPath may legitimately issue a wildcard predicate/object request with a bound subject; the assertion now rejects fully unconstrained requests while preserving endpoint-specific input and plan assertions. The before-OPTIONAL control passes, and the after-OPTIONAL regression still fails.

## Decision Log


Decision: retain separate proofs for scope-removal safety and physical join-prefix facts. Rationale: possible incoming names authorize safety checks but cannot be treated as guaranteed runtime bindings. Date: 2026-11-11.

Decision: begin with an existing pure relational structural proof for a UNION within an ordinary Join, then improve directional candidate input analysis and prospective containing-operator checks. Rationale: statement patterns, stable VALUES, singleton, empty, joins and unions accept compatible mappings without scalar-expression dependencies; a directly enclosing OPTIONAL can nevertheless change the input to its other operand when a marker is cleared. Date: 2026-11-11.

Decision: preserve existing conservative operator exclusions for property paths inside UNION arms, SERVICE, projections, and unknown operators until their independent input contracts are proved. The path outside the UNION already has output facts and needs no additional branch-admission rule. Date: 2026-11-11.

Decision: serialize Maven builds and test executions while agents edit disjoint source files. Rationale: independent editing is useful, but shared target directories and Maven snapshot publication require controlled sequencing. Date: 2026-11-11.

## Outcomes & Retrospective


Initial failures are preserved in `initial-evidence.txt` and `../evidence/{model-before,evaluation-before,optional-condition-before}`. The model identity fix and contextual UNION proof are applied and verified. The full model (149 tests, one existing skip), SPARQL parser (502 tests), and evaluation (4,190 tests, one existing skip) suites pass with no failures or errors. The five new identity tests and 47 focused evaluation tests all pass. Spotless ran before those full suites, and git diff --check is clean.

## Context and Orientation


`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/UnionScopeChangeOptimizer.java` removes flags that currently make a UNION independently evaluated. Its `candidateInputs` collects names that might arrive after later join reordering. Its branch proof checks supported tuple operators, repeatability, expression inputs, and BIND target collisions. It currently clones only the candidate UNION, so direct OPTIONAL parent behavior needs explicit consideration. A UNION's other arm is never an input supplier.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/AlgebraEvaluationSafety.java` already contains `isPureRelational`, a recursive proof limited to standard positive relations. Positive here means statement matches, VALUES, singleton/empty, Join, Union and QueryRoot, with no OPTIONAL, MINUS, scalar expressions, projections, or paths. For these relations, applying an incoming mapping is equivalent to compatibility-joining independently produced mappings; duplicate multiplicities are retained.

`core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/helpers/QueryAlgebraBindingAnalysis.java` models evaluator entry bindings and operator outputs. It tracks possible names, guaranteed names, overwritten names, retained inputs, exact values and lexical scope identities separately. Its `invalidate` currently clears the identity map as well as derived facts, while QJO still holds the previous identity token in its active physical prefix. Physical prefix means the guaranteed bindings from operands already selected earlier in the chosen join order.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java` costs and reorders joins. `visitAt` imports an active prefix only when its scope identity matches the current child input. The implementation should retain that check and stabilize identity ownership, rather than permit bindings across genuinely independent scopes.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/JoinQueryEvaluationStep.java` chooses independent or bound evaluation. `LeftJoinQueryEvaluationStep.java` and `iterator/BadlyDesignedLeftJoinIterator.java` also withhold certain inherited optional-only names. Changing a direct OPTIONAL RHS scope can change the left input, which a branch-only proof cannot see.

## Plan of Work


First add focused model tests and evaluation tests. Model tests exercise identity across ordinary invalidation, fresh scope creation, real movement into a different boundary, and replacement. Evaluation tests parse the user's complete path-plus-UNION query with an unrelated triple before or after OPTIONAL, compare full result bags, record actual inputs/storage requests, and test the second UNION arm inside a subselect after QJO selects a path first. Separate algebra-level tests exercise direct OPTIONAL RHS BIND visibility and OPTIONAL-left BIND input retention.

After observing the failures, preserve per-node lexical identity tokens during ordinary fact invalidation. New boundary nodes must still obtain unique identities, and node relocation must recompute the actual containing frame. Do not reset the identity counter or blindly import all physical-prefix names.

Implement an invocation-local scope analysis in UnionScopeChangeOptimizer. Prove pure regions compositionally and clear only authorized flags. For the general path, replace the generic ancestor rejection with directional treatment of modeled parents, collecting nullable possible names and overwritten names without claiming they are bound. Keep both current evaluator context and prospective changes caused by scope removal. For direct OPTIONAL parents, compare relevant left/right/condition input frames or conservatively retain the marker when the transformation would change unrelated expression inputs. Reuse the shared binding and safety facts; avoid a new divergent operator whitelist.

Handle nested pure UNIONs as a jointly proven region. Reconsider failed candidates only after a relevant successful change, with a bounded worklist or equivalent monotone pass. Avoid repeatedly scanning or cloning the entire query for every nested node. Add internal rejection diagnostics containing a reason and responsible node/name without changing the public query API.

Run focused classes, the model and evaluation suites, and the relevant parser/scope integration selections. Inspect any failure and broaden only when a concrete risk remains. Run the repository formatter and copyright checks, recheck the focused tests, and create a verified patch plus complete changed files and evidence.

## Concrete Steps


Use the repository root `/workspace/scratch/5c2cf927d55d/rdf4j-review` for all Maven invocations. The toolchain JDK is `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8` and Maven is `/workspace/scratch/d0a0e51b9386/toolchain/maven-mvnd-1.0.6-linux-amd64/mvn/bin/mvn`. An external task wrapper in `../build_support` will set JAVA_HOME, PATH, the proxy trust store if needed, and the local repository flag without modifying project configuration.

First run the prescribed root quick clean install with tests skipped, saving output to `maven-build.log`. Start offline and retry the exact command online only for missing dependencies. Use `-Dmaven.repo.local=.m2_repo` for every Maven command. Do not use `-am` or `-q` when tests run.

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For tests, prefer the repository runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py QueryAlgebraBindingAnalysisScopeIdentityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py UnionScopeInputPropagationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py UnionScopeOptionalContextTest --retain-logs

Persist initial failure report snippets in `initial-evidence.txt` and complete reports under `../evidence`. Before downstream tests, reinstall changed model artifacts so tests execute current source. After the focused mvnf run installs the current complete reactor, a serialized multi-module Maven verify may reuse that successful installation to run the complete model, SPARQL parser, and evaluation suites together. The runner accepts one selected module; this combined verify avoids repeating the same 125-module installation three times. Keep exact commands and report totals in the evidence.

Run copyright verification from `scripts`, then format touched modules using Maven process-resources without the quick profile. Ensure new/edited Java files have the existing license header and the Codex attribution comment. Commit and push only with user authorization; the 2026-11-11 follow-up supplies that authorization.

## Validation and Acceptance


The path regression must fail before the scope fix and pass afterward: an unrelated mandatory triple after OPTIONAL no longer prevents safe UNION branch parameterization. The runtime storage request for the endpoint-connected pattern must receive the endpoint value. Complete result multisets must match unoptimized evaluation across order variants, duplicate input rows, and UNDEF cases. Controlled statistics must show the local connected operand benefiting from the valid binding; avoid asserting that connectivity always overrides cost.

The scope identity regression must fail before the identity fix and pass afterward. In the full parsed subselect case, both UNION arms must retain the selected path endpoint during optimization, and actual runtime evaluation must retain the same results. Newly created or genuinely different boundaries must remain distinct.

Direct OPTIONAL cases must preserve original BIND visibility and OPTIONAL-left computations, retaining scopes where the proposed containing-frame change is not proven equivalent. Existing rejection cases for BIND overwrite, expression visibility, subqueries, projection fallback, unknown domains, and nonrepeatable evaluation must continue to pass. All affected module tests must either pass or have an explicitly investigated, independently reproduced baseline failure.

## Idempotence and Recovery


All builds and tests can be rerun from this checkout. Evidence is copied before report cleanup. Scope removal is monotone within one optimizer invocation: a marker is only removed after proof, and rejected candidates remain unchanged. A failed proof must not partially mutate the source tree. Preserve all unrelated and untracked files. Local artifacts are isolated from other tasks' Maven repositories.

## Artifacts and Notes


The final deliverable will contain complete changed Java sources and tests, a git-compatible diff including new files, the executable plan, and concise pre/post evidence. Validate that the patch applies to the pinned base and that every listed file is present before sharing. The source repository remains the authoritative code workspace.

## Interfaces and Dependencies


No new external dependency or public API is needed. Keep binding facts immutable across shared consumers. Any rejection-reporting interface should remain internal/package-private or use existing logging. Cache keys must include the appropriate invocation/context and be invalidated when facts change; lexical identity tokens must not be regenerated for unchanged boundary nodes during routine invalidation.

Revision 2026-11-11: Created before production edits, with explicit test-first gates and independent ownership of model, runtime regression, and OPTIONAL safety work.

Revision 2026-11-11: Added direct OPTIONAL condition-repeatability protection after an automated pre-fix RAND call-count failure. Applied source identity preservation and started contextual scope-removal implementation; no production edit preceded its in-repository failure evidence.

Decision 2026-11-11: Reuse the successful focused mvnf root installation for a serial three-module verify (model, parser, evaluation), with no -am/-q and no test selection. Reason: all changed dependencies are installed and this runs the broader suites without three redundant full-reactor builds.

Verification 2026-11-11: First post-fix focused evaluation run: tests=47, failures=0, errors=0, skipped=0. Copyright check completed with all headers/SPDX valid. Two independent reviews found no blocking issue. Structural purity cache and worklist scheduling are bounded per invocation, but the whole pass is not claimed linear: non-pure nested candidates can still clone/rescan overlapping regions.

Verification 2026-11-11: Serial `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model,core/queryparser/sparql,core/queryalgebra/evaluation verify` completed BUILD SUCCESS. The existing skips are VarProviderSecurityTest on JRE 25 and the disabled randomized sketch tombstone test. All 18 newly added test invocations pass. No source changes followed this full run.

Delivery 2026-11-11: Complete changed sources, tests, ExecPlan, git patch, focused pre/post reports, and per-class module totals are packaged in the deliverable. The patch was applied to a temporary index at the pinned base and every resulting blob compared byte-for-byte with the current source. At the initial delivery, the actual index and commit history were unchanged. Archive entries and CRCs are verified by the packaging script.

Publication 2026-11-11: The user requested a branch, a push, and a link to create the PR. Publish the verified eight-file change on `HASMAC-AS/rdf4j:GH-0000-union-scope-inputs`, targeting `eclipse-rdf4j/rdf4j:main`. Re-run the formatter before creating the commit and verify that all Java files still match the fully tested implementation. Leave PR creation to the user via the compare link.
