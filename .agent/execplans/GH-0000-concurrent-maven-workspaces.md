# Build concurrent, isolated Maven agent workspaces

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md`. It is intentionally self-contained: a contributor should be able to implement and verify the feature using only this file and the current repository checkout.

## Purpose / Big Picture

Today two agents working in the same checkout can corrupt each other's evidence even when they edit unrelated files. Maven writes downloaded and installed artifacts into the same `.m2_repo`, writes every module's compiled classes and test reports into the same `target` directory, and the `mvnf` helper deletes stale reports before each run. One process can therefore clean or overwrite another process's classes, reports, logs, or locally installed snapshots. The existing PID guard only serializes `mvnf` processes and cannot isolate an ordinary Maven invocation.

After this change an agent can opt into a named build workspace. Different workspace names may run concurrently and write only below `.mvnf/workspaces/<id>/`; the same name remains exclusive. The command

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace concurrency-a ParsedIRITest#testCreate

must be able to overlap with the same selection in workspace `concurrency-b`. Ordinary lifecycle builds through `install` use

    python3 scripts/mvn-agent.py --workspace concurrency-a -- -Pquick clean install

Both commands read the existing `.m2_repo` as a shared cache while Maven writes downloads and locally installed artifacts into the workspace's private repository head. They also put all reactor build outputs, reports, temporary files, logs, and evidence in disjoint paths. Invoking either command twice with the same workspace must fail before Maven starts.

Workspace mode does not snapshot source files. Agents that edit concurrently still need separate Git worktrees for a stable source snapshot. Source-mutating operations such as formatting, release/deploy, and generators remain serialized and are not permitted in workspace mode.

## Progress

- [x] (2026-07-13 20:19Z) Read `.agent/PLANS.md` and official Maven repository, POM, Surefire, and Failsafe documentation.
- [x] (2026-07-13 20:19Z) Record the requested interfaces, isolation boundaries, test-first sequence, and acceptance commands in this ExecPlan.
- [x] (2026-07-13 20:34Z) Add failing fake-Maven and POM workspace contract tests; preserve both compact failures in `initial-evidence.concurrent-maven.txt` before production edits.
- [x] (2026-07-13 22:04Z) Implement and harden the shared Python workspace model, child-aware atomic run registry, workspace-aware `mvnf`, and `scripts/mvn-agent.py`; the complete fake-runner suites pass 34 and 5 tests.
- [x] (2026-07-13 20:44Z) Add red-first XML-model coverage and implement the property-activated full-GAV build root, per-GAV test temp/output propagation, unique Shade state, isolated assembly output, and parameterized WAR/API-doc inputs.
- [x] (2026-07-13 22:12Z) Parameterize project writers that bypass `${project.build.directory}` and cover query-render diagnostics, federation/Jetty/Lucene temp writers, server/compliance appdata, server-boot artifacts, plan capture, and LMDB benchmark/report/store defaults.
- [x] (2026-07-13 20:55Z) Update `.gitignore`, `AGENTS.md`, and `.codex/skills/mvnf/SKILL.md` with the opt-in interface, evidence locations, raw-Maven limitation, and serialized source-mutating operations.
- [x] (2026-07-13 22:36Z) Complete two-workspace unit, lifecycle, end-to-end overlap acceptance, formatter, hardened 39 + 5 + 27 contract suites, and independent final diff audit with no remaining findings.
- [x] (2026-07-14 05:26Z) Prune selector discovery before entering ignored repositories and build trees; `--module`
  now scopes the filesystem walk up front, and the 39 + 5 runner suites remain green.

## Surprises & Discoveries

- Observation: the current `mvnf` registry lives under the shared root `target/mvnf-runs`, and cleanup is hard-coded to a selected module's `target/surefire*` and `target/failsafe*` paths.
  Evidence: `.codex/skills/mvnf/scripts/mvnf.py` defines `_registry_dir` using `repo_root / "target" / "mvnf-runs"`; `_clean_test_artifacts` and `_report_dirs` also construct module-local `target` paths.

- Observation: this checkout currently has a second long-lived agent that alternates between direct Maven and `mvnf` runs. The existing `mvnf` guard cannot observe the direct Maven process, which has repeatedly invalidated focused optimizer evidence.
  Evidence: process inspection showed the other agent invoking a root `-Pquick install` before its `mvnf` selection while this task was capturing the same modules' reports.

- Observation: Maven's chained local-repository property is suitable for the requested two-tier cache without a new extension. `maven.repo.local.head` prepends a writable repository in front of the repository selected by `maven.repo.local`, and exists since Maven 3.9.10.
  Evidence: the official `DefaultRepositorySystemSessionFactory.MAVEN_REPO_LOCAL_HEAD` API documentation states that it is a user property for a chained local-repository head and marks it `Since: 3.9.10`.

- Observation: Maven itself warns that shared multi-process local-repository access requires coordination because readers may see incomplete or partially written data.
  Evidence: the official Maven Resolver local-repository guide's “Shared Access to Local Repository” section describes coordination as mandatory for multi-process filesystem access.

- Observation: setting `rdf4j.test.outputDirectory` on the Maven command line would collapse every reactor module onto one shared value because a user property overrides the POM expression `${project.build.directory}`.
  Evidence: the initial fake-runner test was corrected before production work; the runner now owns only `rdf4j.build.root` and `rdf4j.test.tmpRoot`, while Surefire/Failsafe receive the effective per-project `${project.build.directory}` from the POM.

- Observation: fixed mutable writers were concentrated in four families rather than being limited to Surefire reports: query-render diagnostics, server/compliance application data, federation/Jetty/Lucene temp data, and LMDB benchmark report/store roots.
  Evidence: targeted source inspection found direct `target` or CWD paths in the four query-render tests, `TestServer` and both compliance embedded servers, federation `EndpointFactoryTest`/`EmbeddedServer`, `LuceneGeoSPARQLTest`, and the LMDB benchmark helpers named in Plan of Work.

- Observation: process-liveness uncertainty and Python-runner death are distinct lock states. A transiently unavailable start-time probe is not proof that a PID is stale, and killing a runner can leave its Maven child writing after the recorded runner PID exits.
  Evidence: focused fake-process tests reproduced both failures: an unavailable second identity probe deleted a live record, and SIGKILL of the runner allowed a second same-ID run while the first Maven child remained alive.

- Observation: workspace path confinement cannot rely on checking only the final directory. A symlinked `.mvnf`, `tmp`, `logs`, or full-GAV ancestor can redirect writes and report cleanup outside the workspace even when the final path itself is not a symlink.
  Evidence: red sentinel tests followed each ancestor class and demonstrated both outside log/tmp creation and deletion of an outside `surefire-reports` directory.

- Observation: Maven option and plugin-goal syntax has multiple equivalent forms that a literal token blacklist misses. Lowercase `-t` is toolchains while uppercase `-T` is threads; plugin goals may include versions or full coordinates.
  Evidence: fake-Maven reds admitted `deploy:deploy`, `site:deploy`, version/full-coordinate Spotless goals, attached `-f`/`-rf`, `-am`, and `-q`, while incorrectly rejecting legitimate lowercase `-t`.

- Observation: the old compatibility marker under `target/mvnf-runs` can be erased by a root `clean` while a pre-workspace runner is still alive.
  Evidence: the externally running legacy Maven child remained visible in the process table after its marker disappeared. The compatibility bridge is therefore best effort for already-running old code; all newly launched runners need the persistent `.mvnf/runs` registry plus child tracking.

- Observation: globally overriding `${java.io.tmpdir}` through the shared Surefire/Failsafe configuration changed every ordinary test fork, not only the fixed writers being isolated.
  Evidence: a late compatibility contract failed until normal forks retained their prior JVM temp setting, the POM propagated a separate `rdf4j.test.tmpDirectory=${project.build.directory}/tmp`, and only the workspace profile overrode `java.io.tmpdir` with the run/full-GAV directory. The six fixed writers now prefer the dedicated property and retain ambient `java.io.tmpdir` as a non-Maven fallback.

- Observation: Maven's chained repository head does not redirect every plugin that receives the legacy `ArtifactRepository` object. Apache Felix's bundle install goal rewrote `.m2_repo/repository.xml` during the first real isolated smoke.
  Evidence: the name/size/nanosecond-mtime manifests differed only for `.m2_repo/repository.xml` and its parent directory. The bundle plugin bytecode obtains `localRepository.getBasedir()` directly. Workspace mode now configures `obrRepository=NONE`; the repeated smoke and all later acceptance runs left the shared manifest identical.

- Observation: Maven's CLI grammar includes no-argument flags, separate-value options, attached short options, full plugin coordinates, and `goal@executionId` suffixes; inherited `MAVEN_ARGS` bypasses ordinary argv validation unless inspected separately.
  Evidence: focused fake-Maven regressions covered `--raw-streams deploy`, log-file options, safe failure-strategy flags, deploy-file/full-coordinate goals, generator phases, inherited arguments, and project config. The final runner suite passes all 39 tests.

- Observation: marker-based plugin denylisting and explicit argv validation were not a closed isolation boundary.
  Evidence: fake Maven initially accepted direct Exec, Antrun, OpenRewrite, JAXB, CXF, and Help goals, while `.mvn/maven.config` could hide an owned property or deactivate `workspace-build-root`. Workspace mode now rejects all direct plugin goals and validates project config plus inherited arguments before any Maven process starts.

- Observation: retained private repositories and build trees can carry a nested symlink into a later run.
  Evidence: focused sentinels redirected nested `repository/` and `build/` children outside the workspace. A lock-held preflight now rejects only links that resolve outside the workspace root and retains legal internal links.

- Observation: repository-wide `Path.glob("**/src/test/**")` made ordinary selector inference recursively inspect every
  retained workspace repository and build tree. As `.mvnf/` accumulated, a new untracked test selector could spend
  minutes walking generated copies before Maven started and could report those copies as duplicate source classes.
  Evidence: the aggregate IR regression hung in `_find_test_files`; the focused red reproduced matches beneath
  `.mvnf/workspaces/**/build`, `.git`, and `target`. Discovery now uses a pruned walk and applies `--module` before
  traversing.

## Decision Log

- Decision: implement workspace behavior in one importable Python module shared by `mvnf.py` and `scripts/mvn-agent.py` rather than duplicating validation, path generation, locking, and Maven argument injection.
  Rationale: both runners must enforce byte-for-byte identical security and isolation rules. A shared implementation makes fake-Maven contract tests authoritative for both surfaces.
  Date/Author: 2026-07-13 / Codex.

- Decision: keep workspace mode opt-in and preserve workspace-less `mvnf` serialization.
  Rationale: legacy callers keep current paths and thread behavior. Isolation changes only commands that explicitly supply `--workspace` or `MVNF_WORKSPACE`.
  Date/Author: 2026-07-13 / Codex.

- Decision: use a repository-root registry outside every Maven build tree and protect registry updates with an atomic filesystem lock.
  Rationale: Maven `clean` must not erase registrations. The registry must arbitrate legacy/global and named-workspace scopes before a child Maven process starts. Stale records are removable only after verifying that their PID is no longer live.
  Date/Author: 2026-07-13 / Codex.

- Decision: workspace builds force `formatter.skip=true`, one Maven reactor thread by default, and a unique Java temporary directory per invocation.
  Rationale: Spotless `apply` mutates the shared source checkout, Maven reactor concurrency is a separate opt-in risk, and tests commonly write fixed names beneath `java.io.tmpdir`. The runner supplies `rdf4j.test.tmpRoot=<workspace>/tmp/<run-id>` and the active POM profile derives each fork's `java.io.tmpdir` by full GAV, preventing modules in one reactor from sharing a temp directory. These defaults make the isolation claim conservative; `--threads` is the explicit override for reactor parallelism.
  Date/Author: 2026-07-13 / Codex.

- Decision: do not pass `rdf4j.test.outputDirectory` or `java.io.tmpdir` as global Maven user properties, and do not override ordinary forks' `java.io.tmpdir` in shared plugin management.
  Rationale: workspace values must be evaluated per reactor project without changing legacy test semantics. The root model propagates `rdf4j.test.outputDirectory=${project.build.directory}` and a dedicated `rdf4j.test.tmpDirectory`; only the active workspace profile maps the latter to `java.io.tmpdir`. Fixed writers consume the dedicated property with an ambient fallback.
  Date/Author: 2026-07-13 / Codex.

- Decision: evidence for workspace `<id>` is persisted as `initial-evidence.<id>.txt`, while detailed logs and reports remain beneath the run-specific workspace directories.
  Rationale: this preserves the repository's evidence protocol without allowing two agents to overwrite one top-level file. Workspace IDs are already restricted to safe filename characters.
  Date/Author: 2026-07-13 / Codex.

- Decision: raw Maven remains outside the cooperative lock contract; the supported ordinary-lifecycle entry point is `scripts/mvn-agent.py`.
  Rationale: a Python registry cannot reliably intercept arbitrary `mvn` invocations. Documentation must make this limitation explicit instead of claiming isolation it cannot enforce.
  Date/Author: 2026-07-13 / Codex.

- Decision: registry ownership includes every live Maven child/process group, not only the Python runner.
  Rationale: normal cancellation forwards SIGINT/SIGTERM and waits for the child, while SIGKILL/crash leaves a durable child identity that keeps the workspace locked until the writer actually exits. Start-identity mismatch removes a record only when both recorded and current identities are known.
  Date/Author: 2026-07-13 / Codex.

- Decision: reactor-selection validation is runner-specific.
  Rationale: `mvnf` owns its module/test selection and must reject caller `-pl`, `-am`, `-rf`, and quiet test execution; `mvn-agent.py` is the ordinary lifecycle surface and may safely accept `-pl ... -am ... install` because the build-root and repository properties still isolate every selected project.
  Date/Author: 2026-07-13 / Codex.

- Decision: reject any existing symlink ancestor between the checkout and a workspace-owned repository/build/tmp/log or selected full-GAV path.
  Rationale: resolved containment alone cannot make destructive cleanup safe if an ancestor can redirect after lexical path construction. Conservative rejection is reversible and makes the isolation boundary auditable.
  Date/Author: 2026-07-13 / Codex.

- Decision: parse and validate inherited `MAVEN_ARGS` with the same contract as explicit runner arguments.
  Rationale: Maven prepends that channel outside the runner's displayed command. Safe presentation flags remain compatible, while repository/property overrides, owned-profile routing, direct plugins, and mutating goals fail before Maven starts.
  Date/Author: 2026-07-13 / Codex.

- Decision: make workspace commands lifecycle-only, validate `.mvn/maven.config`, and preflight reusable writable trees under the acquired workspace lock.
  Rationale: direct Maven plugins can perform arbitrary source or external writes regardless of their goal name; project config is another implicit argument channel; retained external symlinks can redirect otherwise isolated paths. These checks provide closed, reusable boundaries without deleting artifacts or blocking links whose targets remain inside the workspace.
  Date/Author: 2026-07-13 / Codex.

- Decision: distinguish the global legacy scope from a named workspace whose literal ID is `legacy`, and compare the device/inode identity of workspace roots when enforcing exclusivity.
  Rationale: mode must be explicit rather than encoded in a user-controlled string. Filesystem identity also prevents case aliases from opening the same physical directory concurrently on case-insensitive filesystems without rejecting distinct IDs on case-sensitive filesystems.
  Date/Author: 2026-07-13 / Codex.

- Decision: preserve module-local temporary data for normal builds and disable Apache Felix local OBR index updates only in workspace mode.
  Rationale: normal runs remain cleanable and checkout-local. The OBR index is ancillary repository metadata whose plugin bypasses Maven's chained head; disabling that update prevents the only observed shared-repository write while all Maven artifacts continue installing into the private head.
  Date/Author: 2026-07-13 / Codex.

## Outcomes & Retrospective

The implementation is operational. After the serialized formatter and final hardening, the fake runner suites pass 39 and 5 tests, and 27 POM/fixed-writer contracts pass. Red-first compatibility and isolation corrections preserve ordinary forks' existing `java.io.tmpdir`, prevent profile/config/plugin bypasses, confine reused symlinks, and keep selector discovery out of retained workspace/build repositories while keeping workspace forks and fixed writers isolated. Two concurrent `mvnf` workspaces each passed `ParsedIRITest#testCreate`; two concurrent `mvn-agent.py -Pquick clean install` reactors completed successfully; a real same-ID contender failed before its Maven executable was invoked; and representative server, plan-capture, query-render, federation, Lucene, and LMDB tests passed in isolated full-GAV report trees.

The final post-acceptance manifests are identical to the post-fix baseline: shared `.m2_repo` SHA-256 `05382ed7a398e4a4ed9ac170be3f378d9a205ce9cf6205f4aaf79fc80b05df2c` and ordinary `target` SHA-256 `ba46c341b4461be85c216f8659a01adab8f3372adf0fc7dd8bf19505ec9f47ef`. An online recovery downloaded JUnit Platform 6.0.3 only into `lifecycle-a/repository`. No active registry records remained. The serialized formatter completed successfully, `git diff --check` is clean, and an independent refreshed diff audit found no remaining side-quest issue.

## Context and Orientation

The repository is a large Maven reactor rooted at `pom.xml`. A reactor is one Maven invocation that builds the root project and many modules. Each module normally writes under its own `target` directory. `${project.build.directory}` is Maven's model value for that directory, and well-behaved plugins derive compiled classes, generated sources, packages, and reports from it. Surefire runs unit tests and Failsafe runs integration tests; their default report directories are under `${project.build.directory}`.

`.codex/skills/mvnf/scripts/mvnf.py` is the preferred focused-test runner. It resolves a target string to a Maven module/test selector, removes stale reports, performs a root quick install so the workspace-local repository contains current reactor artifacts, runs a module verify without `-am`, and summarizes Surefire/Failsafe XML and text reports. It currently serializes all invocations with PID records under `target/mvnf-runs` and offers `--allow-concurrent` as an unsafe bypass.

`.codex/skills/mvnf/scripts/test_mvnf.py` and `.codex/skills/mvnf/test_mvnf.py` contain runner tests. They create temporary fake repositories and fake `mvn` executables so argument injection, process overlap, report cleanup, and summaries can be tested quickly without invoking the real reactor. Prefer consolidating new workspace behavior in the test location that already exercises subprocess behavior, while keeping any compatibility test suite green.

Create a shared module at `.codex/skills/mvnf/scripts/maven_workspace.py`. It owns workspace ID validation, Maven version probing, path construction, run IDs, forbidden-argument checking, Maven property injection, the cooperative process registry, and child-process lifecycle cleanup. `mvnf.py` imports it directly because both files share a directory. `scripts/mvn-agent.py` adds that scripts directory to `sys.path` using its repository-relative absolute path before importing the same module; it must not duplicate the implementation.

The workspace root is `.mvnf/workspaces/<id>`. Its `repository` directory is Maven's private writable local-repository head. Its `build` directory is the base for every reactor project's full GAV, meaning `<groupId>/<artifactId>/<version>`; GAV means Maven group ID, artifact ID, and version. Its `tmp/<run-id>` directory is the invocation's temporary root and each test fork receives a full-GAV child below it as `java.io.tmpdir`. Its `logs/<run-id>` directory contains command output and invocation metadata. A run ID combines a UTC timestamp with microseconds, the runner PID, and a UUID suffix so concurrent process creation cannot collide.

Maven 3.9.10 introduced `maven.repo.local.head`. In workspace mode the runner supplies `-Dmaven.repo.local=<absolute repository root>/.m2_repo` as the read-through tail and `-Dmaven.repo.local.head=<absolute workspace>/repository` as the private writable head. The runner first executes `mvn --version`, extracts the Apache Maven semantic version, and rejects versions older than 3.9.10 before doing build work. The currently recorded project Maven is 3.9.15.

The root `pom.xml` needs an inactive-by-default profile activated by the user property `rdf4j.build.root`. Its build directory must be `${rdf4j.build.root}/${project.groupId}/${project.artifactId}/${project.version}`. Normal Maven runs without this property must retain every module's existing `target` path. The root model also supplies `rdf4j.test.outputDirectory`, defaulting to `${project.build.directory}`, so test helpers can move fixed writers without changing normal behavior. Workspace runs supply the isolated build root and a unique Java temporary directory; tests and plugins use these properties rather than guessing module paths.

Some repository code and plugins bypass `${project.build.directory}`. Inspection must locate and parameterize query-render diagnostics, server and compliance application-data directories, federation databases and Jetty temporary directories, Lucene test data, LMDB benchmark reports, and LMDB persistent-store defaults. An explicit user-supplied persistent-store root wins over the default. Maven assembly output must derive from `${project.build.directory}`; the workbench-to-server WAR input must be a property whose workspace value points at the isolated producer; Maven Shade must use unique dependency-reduced POM filenames; and `.gitignore` must ignore `dependency-reduced-pom-*.xml`.

## Plan of Work

Start with Routine D's test-first milestone even though this ExecPlan governs the full refactor. Extend fake-Maven tests before runner production code. Cover CLI `--workspace` taking precedence over `MVNF_WORKSPACE`, the exact workspace ID grammar `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`, invalid traversal/separator rejection without normalization, Maven 3.9.10 minimum parsing, exact absolute isolation properties, and default `-T 1`. Make fake Maven expose a synchronization barrier so two distinct workspaces demonstrably overlap. Assert that the same workspace and a legacy/workspace pair conflict before the fake Maven build marker is written. Assert unique run logs, cleanup/report sentinels, forbidden property overrides, and unchanged serialized legacy behavior. Capture the smallest failing unittest transcript in `initial-evidence.concurrent-maven.txt` before editing runner production code.

Then add `maven_workspace.py`. Represent the validated paths in an immutable data object. Validation must accept the provided identifier exactly or reject it; never trim, lowercase, resolve separators, or silently replace characters. Parse both `-Dname=value` and Maven's split `-Dname value` forms when checking forbidden overrides. Reject overrides for `maven.repo.local`, `maven.repo.local.head`, `rdf4j.build.root`, `rdf4j.test.outputDirectory`, `java.io.tmpdir`, and `formatter.skip`, including an attempted false value. Reject release, deploy, formatter-apply, and known generator invocations in workspace mode with migration guidance.

Implement registration as a lock-protected JSON or per-record directory beneath `.mvnf/runs`, not beneath `target`. A record contains PID, process start identity when the platform exposes it, workspace ID or `legacy`, command, and creation time. Under the atomic lock, discard only records proven stale. A legacy registration conflicts with every live record; a workspace registration conflicts with live legacy and the same workspace, but not another workspace. Register before version probing or invoking Maven and unregister in a `finally` block. Never recursively delete workspace artifacts.

Extend `mvnf.py` with `--workspace`, `MVNF_WORKSPACE`, and `--threads`. CLI wins when both workspace sources exist. Workspace-less behavior retains its current root install, report paths, cleanup, logs, and global serialization. `--allow-concurrent` without a workspace now fails with a message directing the caller to `--workspace`; with a workspace it prints one deprecation warning and has no semantic effect. For workspace mode, derive the selected module's GAV build directory for cleanup and report summarization. This may be obtained from a deterministic reactor map built from POM models or a Maven help/effective-model query; it must not assume artifact ID equals module folder. Both the root quick install and module verify receive the same workspace properties and default reactor thread count. Each child process writes to the current run log.

Add `scripts/mvn-agent.py`. It requires a workspace, accepts `--threads`, requires a literal `--` delimiter, and forwards Maven arguments only through the shared validator. It supports ordinary lifecycle builds through `install`; reject `deploy`, release goals/profiles, formatting, and source generators. It uses the same version check, lock, run paths, repository chain, build root, temporary directory, formatter skip, and logging contracts as `mvnf`.

Before changing `pom.xml`, add a focused model test that evaluates normal and workspace-effective models in a small safe selection or verifies the profile structure precisely. Capture the red. Add the property-activated profile and prove that normal builds retain module `target` while workspace models place output beneath the full GAV. Ensure the path contract covers generated sources, classes, packages, Surefire reports, Failsafe summaries, temporary directories, assembly output, workbench WAR input, and Shade state. Prefer behavior assertions against Maven's evaluated model over brittle string-only assertions.

Search for fixed writers using targeted `rg` queries for `target/`, `surefire-reports`, `java.io.tmpdir`, `Files.createTempDirectory`, application data, Jetty temp, Lucene directories, LMDB benchmark output, and persistent store defaults. For every relevant writer, first add or identify focused coverage proving an explicit `rdf4j.test.outputDirectory` or existing user property is honored. Change defaults to derive from `rdf4j.test.outputDirectory` or `project.build.directory` without changing normal runs. Do not redirect explicitly configured persistent data.

Update `.gitignore` for `.mvnf/`, `initial-evidence.*.txt`, and `dependency-reduced-pom-*.xml`. Update `AGENTS.md` and `.codex/skills/mvnf/SKILL.md` so concurrent examples use workspace commands; document private repositories, build/report/log/evidence locations, one-thread default, `--threads`, raw Maven's inability to cooperate, no automatic cleanup, and the continued exclusivity of formatting/release/generator operations. Keep legacy evidence instructions for workspace-less runs and add the workspace variant beside them.

Finally run focused Python tests, POM/model tests, and affected Java module tests. Perform end-to-end overlap with two `ParsedIRITest#testCreate` selections and two root `-Pquick clean install` processes through `mvn-agent.py`. Record pre/post manifests of `.m2_repo` and ordinary module `target` trees using relative name, size, and nanosecond modification time; their manifests must be identical. Confirm workspace repositories, build roots, reports, temporary directories, logs, and evidence are disjoint. Attempt same-workspace overlap and prove the losing command exits before fake or real Maven starts.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Never use `-am` or `-q` when tests are enabled.

Run the fake-Maven unit selection before runner implementation and retain the failure:

    python3 -m unittest .codex/skills/mvnf/scripts/test_mvnf.py
    python3 scripts/agent-evidence.py --command "python3 -m unittest .codex/skills/mvnf/scripts/test_mvnf.py" --log <captured-log> > initial-evidence.concurrent-maven.txt

If unittest module-path discovery rejects the dotted path because `.codex` is not a package, invoke the test file directly with `python3 .codex/skills/mvnf/scripts/test_mvnf.py` and record that exact command instead. Python tests do not invoke the real Maven reactor unless the fake executable explicitly delegates, which it must not.

After implementing the shared runner, rerun both compatibility suites:

    python3 .codex/skills/mvnf/scripts/test_mvnf.py
    python3 .codex/skills/mvnf/test_mvnf.py

Run the smallest Maven/POM test selection through an isolated workspace once workspace mode itself is green. The exact selector is to be recorded here when the test location is chosen. A typical form is:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace concurrent-pom <PomWorkspaceIsolationTest>

Run representative affected module selections through distinct workspaces. Record their workspace-local Surefire/Failsafe report paths and summaries in this document as they become known.

For focused end-to-end test overlap, start both commands without waiting for the first one to finish and require both exits to be zero:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace concurrency-a ParsedIRITest#testCreate
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace concurrency-b ParsedIRITest#testCreate

For lifecycle overlap, run:

    python3 scripts/mvn-agent.py --workspace concurrency-a -- -B -ntp -o -Pquick clean install
    python3 scripts/mvn-agent.py --workspace concurrency-b -- -B -ntp -o -Pquick clean install

The runner injects `.m2_repo`, the private head, build root, formatter skip, temporary directory, and default `-T 1`; callers must not repeat those properties.

Before final handoff run the repository copyright check for any touched Java files, the prescribed formatter in serialized legacy mode, focused runner tests, affected module tests, and a full diff audit. Formatting itself must not run in workspace mode because it intentionally mutates shared source files.

## Validation and Acceptance

The fake-Maven suite is accepted when all of the following are observable: CLI workspace beats the environment; every invalid ID is rejected before child execution; Maven 3.9.9 is rejected and 3.9.10 is accepted; injected absolute properties exactly match the workspace layout; two different IDs cross a synchronization barrier concurrently; same-ID and legacy/workspace overlap fail before a Maven marker appears; every run has a unique log/tmp run ID; cleanup and report summary touch only the selected GAV build path; forbidden overrides fail; and workspace-less serialized behavior remains unchanged.

The POM contract is accepted when a normal evaluated project still reports `<module>/target`, while a workspace property reports `.mvnf/workspaces/<id>/build/<groupId>/<artifactId>/<version>`. Compiled classes, generated sources, archives, Surefire reports, Failsafe summaries, plugin state, test temp data, WAR input, assembly output, and unique Shade dependency-reduced POMs must follow that isolated path or another explicitly unique path.

The end-to-end focused pair is accepted when both `ParsedIRITest#testCreate` runs succeed and their report files are distinct. The lifecycle pair is accepted when both root quick installs succeed and install snapshots only into their private repository heads. There must be no differences in the before/after manifests of shared `.m2_repo` or pre-existing ordinary `target` files. There must be no active registry records after clean exits. A same-workspace overlap must produce a concise owner/PID error before Maven starts.

The documentation contract is accepted when a new contributor can find one copy-paste command for focused tests, one for lifecycle builds, the exact artifact/evidence locations, the raw-Maven limitation, and the list of operations that remain exclusive.

## Idempotence and Recovery

All workspace directories are additive and ignored. Rerunning with a workspace creates a new `tmp/<run-id>` and `logs/<run-id>` while reusing that workspace's private repository and GAV build tree. The implementation must not automatically delete these artifacts. Maven `clean` only removes the current workspace's GAV build directories because the build root is isolated.

If a runner is killed, its registry record may remain. The next registration acquires the registry lock and removes the record only if the recorded PID/process identity is no longer live. If it is live, the new run fails rather than stealing the workspace. A corrupt registry record is quarantined or reported with recovery guidance; it is not treated as permission to overlap.

If Maven model interpolation cannot reliably use `${project.groupId}`, `${project.artifactId}`, or `${project.version}` in an inherited build directory, stop before weakening the full-GAV contract. Add a toy effective-model test and choose a Maven-supported expression or generated user property map that remains collision-free across duplicate artifact IDs. Record the discovery and decision here.

If Maven 3.9.10 cannot resolve an artifact through the `.m2_repo` tail while writing metadata only to the head, preserve the fake contract, reproduce with a toy offline artifact, and document the Maven version-specific behavior before choosing a fallback. Do not silently make the shared repository writable.

The working tree contains unrelated optimizer migration work. Never reset, restore, clean, stash, or format unrelated files. Keep side-quest patches file-scoped and inspect `git diff -- <paths>` before each handoff.

## Artifacts and Notes

The initial failing runner transcript will be stored at `initial-evidence.concurrent-maven.txt`. Workspace-specific Java evidence will use `initial-evidence.<workspace>.txt`. Detailed logs and invocation metadata live at `.mvnf/workspaces/<id>/logs/<run-id>/`; test reports live under the matching full-GAV build directory.

The first persisted POM-model failure is:

    Command: PYTHONDONTWRITEBYTECODE=1 python3 scripts/test_maven_workspace_pom.py
    Result: 4 tests, 4 failures
    Missing contracts: workspace-build-root profile, rdf4j.test.outputDirectory,
    generateUniqueDependencyReducedPom=true, and dependency-reduced-pom-*.xml ignore.

The first persisted runner failure is:

    Command: PYTHONDONTWRITEBYTECODE=1 python3 .codex/skills/mvnf/scripts/test_mvnf.py MvnfWorkspaceTest.test_cli_workspace_precedence_injects_isolated_paths_and_uses_gav_reports
    Result: 1 test, 1 failure
    Missing contract: mvnf rejected the new --workspace interface as an unrecognized argument.

Authoritative Maven facts used by this plan are embedded here: concurrent writes to a filesystem local repository require coordination; `maven.repo.local.head` prepends a chained local repository and is available since Maven 3.9.10; Maven's build directory is the root from which standard output directories derive; Surefire and Failsafe report locations can follow the project build directory.

## Interfaces and Dependencies

No third-party dependency and no production Java API is added.

`.codex/skills/mvnf/scripts/maven_workspace.py` must expose internal interfaces equivalent to:

    WORKSPACE_ID_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}\Z")

    @dataclass(frozen=True)
    class WorkspacePaths:
        workspace_id: str
        root: Path
        repository: Path
        build_root: Path
        tmp_dir: Path
        log_dir: Path
        run_id: str

    def resolve_workspace(cli_value: str | None, environment: Mapping[str, str]) -> str | None: ...
    def validate_workspace_id(value: str) -> str: ...
    def create_workspace_paths(repo_root: Path, workspace_id: str, *, pid: int | None = None) -> WorkspacePaths: ...
    def validate_maven_version(output: str, minimum: tuple[int, int, int] = (3, 9, 10)) -> tuple[int, int, int]: ...
    def validate_forwarded_arguments(arguments: Sequence[str], *, workspace_mode: bool) -> None: ...
    def isolation_arguments(repo_root: Path, paths: WorkspacePaths, threads: str) -> list[str]: ...
    @contextmanager
    def registered_run(repo_root: Path, workspace_id: str | None, command: Sequence[str]): ...

Exact function names may follow existing `mvnf.py` conventions, but the responsibilities and single-source-of-truth requirement are fixed.

`mvnf.py` adds:

    --workspace ID
    --threads COUNT

and reads `MVNF_WORKSPACE` if the CLI option is absent. `scripts/mvn-agent.py` requires the same workspace/thread options and `--` followed by Maven arguments. Workspace IDs use the exact regex above. Workspace mode requires Maven 3.9.10 or newer.

The Maven properties are `rdf4j.build.root` and `rdf4j.test.outputDirectory`. The runner additionally supplies the internal `rdf4j.test.tmpRoot`; the POM derives `rdf4j.test.tmpDirectory` for each project. Workspace mode also owns `maven.repo.local`, `maven.repo.local.head`, both test-temp properties, `java.io.tmpdir`, and `formatter.skip`. A caller cannot override any of them.

Revision note (2026-07-13, Codex): created this ExecPlan before side-quest production edits, incorporating the requested runner, Maven model, fixed-writer, documentation, TDD, and end-to-end isolation contracts. The optimizer ExecPlan remains separate and resumes after this prerequisite is usable.

Revision note (2026-07-13, Codex): recorded the first pre-change POM-model failure in Progress and Artifacts so the living plan retains the observable TDD state even after later green runs.

Revision note (2026-07-13, Codex): recorded the focused fake-Maven runner red and marked the initial contract-test milestone complete; runner and lock implementation may now proceed.

Revision note (2026-07-13, Codex): recorded the per-run/per-GAV temp-root decision, lifecycle-path model work, fixed-writer inventory, and current partial implementation state so global Maven user properties cannot accidentally reintroduce cross-module collisions.

Revision note (2026-07-13, Codex): recorded the red-first normal-fork compatibility correction, final formatted contract results, real overlap acceptance, and remaining independent diff audit.

Revision note (2026-07-13, Codex): closed the side quest after profile/config/plugin/symlink hardening, 71 final contract tests, the real isolated temp-path recheck, and an independent no-findings diff audit.
