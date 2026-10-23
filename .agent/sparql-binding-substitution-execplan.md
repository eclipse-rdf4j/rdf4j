# Substitute SPARQL bindings only at variable tokens

This ExecPlan follows `.agent/PLANS.md` and is a living document. Keep its progress, discoveries, decisions, and evidence current as work proceeds.

## Purpose / Big Picture

A caller can bind a value to a SPARQL update or deprecated query, but the current regular-expression replacement also changes matching text inside string literals, IRI references, and comments. A binding name containing regular-expression punctuation can match a different variable, and a substituted literal can be rewritten a second time by a later binding. After this change, only actual SPARQL variable tokens in the original request receive substitutions. A parser-backed test and a full repository module test will demonstrate the result.

## Progress

- [x] [done] Inspect isolated checkout and source paths (2026-10-23 00:12Z).
- [x] [done] Install dependencies and capture smallest red test.
- [x] [done] Add lexical and nested regression variants.
- [x] [done] Implement token-aware substitution and integration.
- [x] [done] Verify focused and full repository module.
- [ ] [in_progress] Review scope and publish draft PR.
- [ ] [todo] Monitor exact-head CI to completion.

## Surprises & Discoveries

The current `core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/query/QueryStringUtil.java:getUpdateString` builds a regex from each binding name and calls `String.replaceAll` over the whole update. Deprecated `SPARQLOperation.getQueryString` does the same over a substring after the first raw `{`, while `getBindingNames` uses a regex over the preceding substring. The SPARQL JavaCC lexer already has distinct `VAR1` and `VAR2` tokens for `?name` and `$name`, plus `LBRACE` for `{`, so it can distinguish variables from lookalike text in literals, IRIs, and comments. No production files have been edited yet.

## Decision Log

- Decision: Start from detached main `3d292763558604affd89c7d4256e8df5720bd895` in the separate managed worktree `/Users/havardottestad/.codex/worktrees/security-sparql-binding-substitution/rdf4j` on branch `GH-0000-sparql-binding-substitution`. Rationale: this tokenization fix must not stack on draft term-rendering PR #6212. Date/Author: 2026-10-23, Codex.
- Decision: Use the existing SPARQL JavaCC token manager as the source of lexical truth, with a single source-order substitution pass over the original request. Rationale: it already distinguishes variable tokens from text in every valid lexical context, and a single pass prevents replacement text from being substituted again. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Pending red-first tests, implementation, and verification.

## Context and Orientation

`core/repository/sparql` builds remote SPARQL text. `QueryStringUtil.getUpdateString` is used by `SPARQLUpdate` before sending updates. Deprecated `SPARQLOperation` still has its own binding replacement and selection handling. A token is one indivisible unit recognized by the SPARQL lexer. A variable token begins with `?` or `$` and has a syntactically valid variable name. The same characters inside `<...>`, quoted strings, or `#` comments are ordinary source text and must stay unchanged. `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast` provides the generated JavaCC lexer and token kinds; it is already a dependency of the repository module.

## Plan of Work

First add a smallest in-repository failing test to `core/repository/sparql/src/test/java/org/eclipse/rdf4j/repository/sparql/query/QueryStringUtilTest.java`: bind `?value` while the update also contains `?value` inside an IRI, a quoted literal, and a comment. Run that one method without `-am` or `-q`; copy the Surefire failure into top-level `initial-evidence.txt` before editing any production code. Then add variants for `$` variables, comments, short and long strings with escapes, IRI references, line endings, Unicode variable names, binding names with regex metacharacters, nested update/query syntax, and two bindings whose replacement text mentions the other variable.

Implement a small internal helper in `core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/query` that scans the original text with `SyntaxTreeBuilderTokenManager`, identifies only `VAR1` and `VAR2` tokens, and replaces names present in the binding map. Preserve every non-replaced source character exactly, including comments and whitespace. Use the first lexical `LBRACE` token, rather than the first raw `{` character, to retain the deprecated operation's select-versus-body behavior. Route `getUpdateString`, `getQueryString`, and `getBindingNames` through that helper. Keep RDF value rendering in each existing caller and avoid editing the separate term-rendering PR.

## Concrete Steps

Run commands from `/Users/havardottestad/.codex/worktrees/security-sparql-binding-substitution/rdf4j`. The required first clean install is running with:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

After writing the first regression, select it with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-binding-substitution --retain-logs --module core/repository/sparql QueryStringUtilTest#<method>

Persist the first failure with `python3 scripts/agent-evidence.py ... > initial-evidence.txt`. Then run focused test classes and the full module using `mvnf --workspace sparql-binding-substitution --retain-logs`. Run `scripts/checkCopyrightPresent.sh` from `scripts`, Spotless `process-resources` without `-q`, and `git diff --check` before committing. The managed worktree links `.m2_repo` read-through to the existing cache. If an offline plugin is missing, retry the exact command once online, then return offline.

## Validation and Acceptance

The first new test must fail before production changes and pass unchanged afterward. Each binding should replace only complete variable tokens in the original source. Literal/IRI/comment lookalikes must remain byte-for-byte identical; regex punctuation in a binding name must never alter another variable; replacement text must not be substituted a second time. Parser-backed assertions should confirm the update/query still has the intended operations and terms. The full `core/repository/sparql` module must pass with no new failures.

## Idempotence and Recovery

The worktree is isolated; retain all untracked logs and Surefire reports. `mvnf` can overwrite test reports in its isolated build tree, so preserve each first red report immediately before any green run. Never use `-am` or `-q` when tests run. Stage only reviewed source, tests, and this plan; do not copy or cherry-pick the term-rendering branch.

## Artifacts and Notes

The initial root clean install writes `maven-build.log`. The first red report is `.mvnf/workspaces/sparql-binding-substitution/build/org.eclipse.rdf4j/rdf4j-repository-sparql/6.2.0-SNAPSHOT/surefire-reports/org.eclipse.rdf4j.repository.sparql.query.QueryStringUtilTest.txt`; its compact result is preserved at `initial-evidence.txt` and the full Maven log is `.mvnf/workspaces/sparql-binding-substitution/logs/20261022T221626.247417Z-38014-37499314/verify.log`. The separate term-rendering PR is #6212; it addresses unsafe RDF term serialization but does not change the regex binding replacement in the main-tree code used here.

## Interfaces and Dependencies

The token helper should be package-private and used by both affected repository classes. It should accept source text and a mapping from complete variable names to already-rendered replacement strings, and should expose enough lexical information for the deprecated operation to remove bound variables from its select head without matching comments, IRIs, or literals. Use only existing Java and RDF4J parser dependencies. Do not change public repository interfaces or add external libraries.

Plan revision note (2026-10-23): Established the independent plan after clean worktree inspection and source review. The required initial install is running; no production code has been edited.
Plan revision note (2026-10-23): Root install passed. The first focused Surefire test failed with replacements inside an IRI, literal, and comment; preserved the result in `initial-evidence.txt` before any production edit. Next add related regression variants.
Plan revision note (2026-10-23): The expanded pre-fix class failed with 6 assertion failures and 2 errors across Unicode names, nested graph IRIs, comments, long strings, cascading substitutions, regex names, and the deprecated operation. Preserved reports in `variant-evidence.txt` and `expanded-variant-evidence.txt`. Next implement one source-order lexical pass using the existing parser stream and token kinds.
Plan revision note (2026-10-23): Shared token-aware helper, update integration, and deprecated operation integration compile. The same focused class is green with 16 tests and zero failures/errors; report is in the isolated Surefire path and Maven log `.mvnf/workspaces/sparql-binding-substitution/logs/20261022T222528.758152Z-196-ad42b68f/verify.log`. Run the whole module and review scope next.
Plan revision note (2026-10-23): Full SPARQL repository module passed 96 tests; root formatter succeeded, copyright and diff checks passed. Parent review approved the candidate for a separate draft PR and requested an explicit overlap note with #6212. Proceed with scoped publication.
