# Confine managed FedX files to the repository directory

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Work in the isolated worktree `/Users/havardottestad/.codex/worktrees/security-fedx-config-paths/rdf4j`, branch `GH-0000-fedx-managed-paths`. No issue number was provided, so the branch uses the repository's `GH-0000` convention.

## Purpose / Big Picture

An RDF4J Server client can submit a FedX repository configuration through `PUT /repositories/{id}`. Today its member configuration filename, NativeStore member location, and prefix declarations filename can lead the Server process to read or create files outside the manager-assigned repository directory. After this change, a manager-created FedX repository will use only real files below its assigned data directory. Embedded `FedXFactory` users retain their explicit external-file behavior. A local manager test and, if practical, live Server HTTP test will demonstrate the boundary.

## Progress

- [done] create clean isolated branch and worktree (2026-10-23 04:14Z)
- [done] complete initial root quick install (2026-10-23 04:16Z)
- [done] capture smallest managed-path red test (2026-10-23 04:21Z)
- [done] reproduce three file input variants (2026-10-23 04:29Z)
- [done] implement managed path root containment (2026-10-23 04:34Z)
- [done] verify focused module and Server paths (2026-10-23 04:46Z)
- [done] preserve absolute NativeStore directory contract (2026-10-23 04:55Z)
- [done] verify live path and final diff (2026-10-23 05:03Z)
- [done] preserve managed NativeStore endpoint identity (2026-10-23 05:14Z)
- [in_progress] publish reviewed scoped draft

## Surprises & Discoveries

The wrapper is not Server-only: `FedXRepositoryFactory` creates it for any `RepositoryManager`. A manager assigns `FedXRepositoryWrapper.dataDir` in `LocalRepositoryManager.createRepository`, so the data directory is the natural trust boundary. `NativeStoreProvider` intentionally supports absolute locations for embedded use; a global provider restriction would break that documented API. `QueryManager` reads prefix declarations directly from `FedXConfig` during initialization, so the managed wrapper must pass a resolved safe path to the runtime without unexpectedly changing persisted configuration.

The initial quick clean install passed in the private Maven workspace. Its log is `.mvnf/workspaces/fedx-paths/logs/20261023T041447.442117Z-21288-3c8e3840/maven.log`.

On the current macOS/JDK, `new File(baseDir, absoluteChild)` in `FedXRepositoryWrapper` rendered the absolute child's text beneath `baseDir` and failed to find the file. The meaningful data-config escape is relative `..`: `../../../external-members.ttl` from `manager/repositories/fedx` reached an external Turtle file, and the repository initialized. The first genuine red Surefire report is preserved in `initial-evidence.txt`; it says `ManagedFedXPathTest.rejectsExternalDataConfig: Expecting code to raise a throwable`.

The expanded 13-case pre-fix selection has nine denial failures and one positive relative-prefix initialization error; three controls pass. `red-fedx-path-complete-evidence.txt` preserves the report, and `.mvnf/workspaces/fedx-paths/logs/20261023T042736.614678Z-99147-9f33f363/verify.log` is the full log. A relative prefix file inside the root currently fails because `QueryManager` interprets it against the process working directory.

## Decision Log

- Decision: Make the manager-created wrapper the entry point for confinement. Rationale: it has the manager-assigned repository directory, while direct `FedXFactory` calls intentionally support external files. Date/Author: 2026-10-23, Codex.
- Decision: Confirm file access with in-repository failing tests before changing production behavior. Rationale: absolute, relative `..`, and symlink spellings may behave differently at each sink. Date/Author: 2026-10-23, Codex.
- Decision: Use a managed-only real-path resolver and pass canonical files to the existing consumers. `EndpointFactory` will accept an optional managed root for NativeStore members, while direct `FedXFactory` calls keep the existing overload. A runtime copy of `FedXConfig` will hold a canonical prefix filename so the saved relative config remains portable. Rationale: this validates actual file paths before read/create without changing embedded semantics or duplicating Turtle parsing. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The worktree and baseline build are ready. Thirteen red-first/compatibility tests characterize all three file inputs; the same 13 tests now pass after a managed-only path resolver. An additional legitimate NativeStore alias case passes. The full federation module passed 610 tests, and the live Server IT rejects an externally located NativeStore with HTTP 500 while leaving that directory untouched. Formatting and final review remain.

## Context and Orientation

`tools/federation/src/main/java/org/eclipse/rdf4j/federated/repository/FedXRepositoryFactory.java` creates a lazy `FedXRepositoryWrapper` from a submitted configuration. `core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManager.java` assigns a data directory to that wrapper. In `FedXRepositoryWrapper.createFactory`, the untrusted `dataConfig` string becomes a `File` passed to `EndpointFactory.loadFederationMembers`. Its Turtle member model may name a NativeStore path, which `NativeStoreProvider.loadEndpoint` opens or creates under `baseDir/repositories` for relative paths and opens directly for absolute paths. Separately, `FedXRepositoryConfig.parseFedXConfigInternal` accepts `prefixDeclarations`, and `QueryManager.init` opens that filename as Java properties. A symlink is a file-system alias to another path, so lexical `startsWith` checks alone cannot enforce the real directory boundary.

## Plan of Work

First extend the existing manager test area in `tools/federation/src/test/java/org/eclipse/rdf4j/federated/` with one smallest failing test: a manager-created FedX config referencing a valid member file outside the manager data directory must fail before opening it. Run that method with `mvnf --workspace fedx-paths --retain-logs`, and immediately save its Surefire report in top-level `initial-evidence.txt`. Keep that test unchanged while adding variants for absolute and `../` paths, symlink aliases, NativeStore member locations in both direct model and data config, prefix declarations, and legitimate in-directory files. A direct `FedXFactory` external-file control must stay green.

Then implement a single managed-path check based on the real/canonical manager data directory. Resolve each candidate before opening or creating it, reject escapes with a clear `RepositoryException`, and pass the verified canonical path to the file-consuming code. For a missing NativeStore member directory, resolve its existing ancestor and remainder safely before creation, and recheck the created real path. Keep the library's existing external-file behavior when no manager boundary was supplied. Avoid changing repository configuration serialization merely because a relative path was resolved at runtime.

Finally run focused tests, the full `tools/federation` module, affected Server Spring/Server integration tests, copyright check, non-quiet formatter, and `git diff --check`. Review the candidate with the parent before a scoped commit, push, and draft PR. Describe any existing managed FedX deployment using external files as a compatibility migration.

## Concrete Steps

All commands run from this worktree root. `.m2_repo` is a read-through link to the main checkout's dependency cache; workspace `fedx-paths` is the only writable Maven repository/build layer.

    python3 .codex/skills/mvnf/scripts/mvnf.py ManagedFedXPathTest#rejectsExternalDataConfig --workspace fedx-paths --retain-logs
    python3 scripts/agent-evidence.py --command 'mvnf ManagedFedXPathTest#rejectsExternalDataConfig --workspace fedx-paths --retain-logs' .mvnf/workspaces/fedx-paths/build/org.eclipse.rdf4j/rdf4j-tools-federation/6.2.0-SNAPSHOT/surefire-reports > initial-evidence.txt

Do not use Maven `-am` or `-q` for tests. `mvnf` performs the required quick root install before each test selection. If offline dependencies are missing, retry the exact selection once with `--no-offline` and return offline afterward.

## Validation and Acceptance

A manager-created FedX repository must refuse a `dataConfig`, NativeStore member location, or prefix declarations file that resolves outside its assigned data directory, including an absolute path, `..`, or an in-directory symlink to an outside target. It must still initialize when all files are inside that directory, including a symlink to an in-directory target. An embedded `FedXFactory` with an intentionally external member configuration or NativeStore directory must still work. The red pre-fix and green post-fix Surefire reports must show these outcomes, and live Server HTTP coverage should prove the submitted config path is actually reachable through the WAR.

## Idempotence and Recovery

Do not reset, clean, or delete another worktree's files. Preserve untracked evidence and full Maven logs, because later `mvnf` runs replace report directories. Keep Server and embedded tests in separate temporary directories so a failed test cannot modify real repositories. If a failure is in test setup, correct the test and rerun before any production edit.

## Artifacts and Notes

The first build log path is recorded above. `initial-evidence.txt` preserves the first meaningful Surefire failure before any fix. Its selected command was `python3 .codex/skills/mvnf/scripts/mvnf.py ManagedFedXPathTest#rejectsExternalDataConfig --workspace fedx-paths --retain-logs`, and its report is `.mvnf/workspaces/fedx-paths/build/org.eclipse.rdf4j/rdf4j-tools-federation/6.2.0-SNAPSHOT/surefire-reports/org.eclipse.rdf4j.federated.repository.ManagedFedXPathTest.txt`.

Revision note, 2026-10-23 04:22Z: Corrected the initial test case from an absolute `dataConfig` spelling, which did not escape on this JDK, to a relative traversal. The corrected test now fails for the intended missing validation, and the unedited first red report has been preserved.

Revision note, 2026-10-23 04:30Z: Added NativeStore absolute/relative/symlink and data-config member-model cases, prefix absolute/symlink cases, and in-root and embedded controls. Their complete pre-fix result is preserved before any production edit. The managed-only resolver design uses `Path.toRealPath()` and verified existing ancestors before creating a new NativeStore directory; passing canonical targets to readers reduces alias reinterpretation after validation.

Revision note, 2026-10-23 04:35Z: `ManagedFedXPaths` now resolves existing member/prefix files and NativeStore directories beneath the real assigned data directory. `EndpointFactory` has an optional managed-path overload used only by the manager wrapper, and `FedXConfig.copy()` lets the runtime use a canonical prefix filename without changing the saved configuration. Focused `ManagedFedXPathTest` is green: 13 tests, zero failures/errors/skips (`.mvnf/workspaces/fedx-paths/logs/20261023T043210.734427Z-59395-63daaa2f/verify.log`).

Revision note, 2026-10-23 04:46Z: The full federation module passed 610 tests (`.mvnf/workspaces/fedx-paths/logs/20261023T043434.756469Z-61114-baa0904c/verify.log`). The expanded focused class passed 14 tests (`.mvnf/workspaces/fedx-paths/logs/20261023T044240.104794Z-43089-b7832e24/verify.log`). The live Server IT passed with an exact HTTP 500 assertion (`.mvnf/workspaces/fedx-paths/logs/20261023T044440.734566Z-74679-4b3a5f5d/verify.log`). Federation documentation now explains managed paths and migration.

Revision note, 2026-10-23 04:50Z: Final review found that the managed helper creates a missing absolute NativeStore path inside the root, whereas `NativeStoreProvider` requires an absolute directory to already exist. Add a focused failing regression before correcting this compatibility contract, then reverify.

Revision note, 2026-10-23 04:55Z: The focused regression revealed a second issue: on macOS an absolute `/var/...` location inside the managed directory has a `/private/var/...` real path, so a lexical precheck rejected it as external. `red-fedx-absolute-inroot-evidence.txt` preserves the 1-test Surefire failure. The helper now validates the real existing ancestor before comparing with the real root, creates missing relative locations only, and passes the canonical location to the provider. The focused class passed 16 tests and the full federation module passed 613 tests (52 skipped) in `.mvnf/workspaces/fedx-paths/logs/20261023T045508.163493Z-43218-ccbf7433/verify.log`. The live Server test remains to be rerun on this exact version.

Revision note, 2026-10-23 05:03Z: The exact-code live Server IT passed in `.mvnf/workspaces/fedx-paths/logs/20261023T050026.740743Z-66751-8374ca9f/verify.log`. Final non-quiet formatter passed (`formatter-fedx-final.log`), copyright passed (`copyright-fedx-final.log`), and `git diff --check` is clean. The candidate was sent to the parent for independent review before publication.

Revision note, 2026-10-23 05:07Z: Parent review found that rewriting `NativeRepositoryInformation.location` to the canonical path also changes the endpoint's configured identity and monitoring value. Add a red regression for the member alias, then pass the validated canonical `File` separately to `NativeStoreProvider` while leaving repository information unchanged.

Revision note, 2026-10-23 05:24Z: `red-fedx-identity-evidence.txt` records the alias endpoint changing from `alias` to a canonical absolute path. `NativeStoreProvider` now accepts a separately resolved `File`; the configured `RepositoryInformation` retains its alias and ID. The 16-case focused class passed; the full federation module passed 613 tests with 52 skips (`.mvnf/workspaces/fedx-paths/logs/20261023T051344.208639Z-13054-b206396d/verify.log`); live Server IT passed 1/1 (`20261023T051921.865502Z-15570-57d1ee86/verify.log`). Final copyright `copyright-fedx-identity-final.log`, formatter `formatter-fedx-identity-final.log`, and `git diff --check` passed. Awaiting parent final review before publication.

## Interfaces and Dependencies

Prefer the existing `FedXRepositoryWrapper`, `EndpointFactory`, and `NativeStoreProvider` abstractions. A new path-confinement helper, if needed, should remain internal to `tools/federation` and take an explicit managed root; the absence of that root keeps direct `FedXFactory` semantics. Do not add an external dependency for canonical path handling; JDK `Path`, `Files`, and `File` are sufficient.

Revision note, 2026-10-23 04:17Z: Created this plan after tracing the three untrusted file inputs and completing the isolated baseline build. Exact implementation choices will be revised after the red tests establish how each sink behaves.
