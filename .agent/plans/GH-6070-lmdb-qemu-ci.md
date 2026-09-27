# Require QEMU LMDB Recovery in CI

This is a living ExecPlan governed by `.agent/PLANS.md`. Keep its `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current.

## Purpose / Big Picture

Every pull request should run real LMDB crash-recovery checks inside a Linux guest booted by QEMU. The gate must exercise the checked-in FLUSH/FUA calibration, the acknowledged namespace campaign, and each of the three transaction cutpoints; it must fail if a prerequisite, scenario, report, public lookup, or repeated recovery is missing or skipped. The check uses `forceSync=true` and reports its limited simulated device-loss boundary rather than claiming physical host power-loss proof.

## Progress

- [done] Add strict commit-returned red.
- [done] Validate campaign reports and skips.
- [done] Add Linux guest provisioning workflow.
- [done] Make hosted growth replay cut deterministic.
- [done] Fix hosted guest and artifact rules.
- [in_progress] Boot guest and run campaigns.
- [todo] Commit fixes and update PR evidence.

## Surprises & Discoveries

- Observation: the current shell runners only create seed ISOs with macOS `hdiutil` and default to the macOS `hvf` accelerator and host CPU. A workflow that simply invokes them on Linux cannot run the campaigns.
  Evidence: `runner_common.py:create_seed_iso`, `guest_qemu_arguments`, and the required `--hdiutil` arguments in `run_campaign.py`, `run_calibration.py`, and `run_powercut_campaign.py`.
- Observation: `commit-returned-before-ack` currently accepts either old A or new A+B even when its witness says the native commit returned successfully. That makes the oracle unable to detect loss of a force-synced committed transaction.
  Evidence: `run_powercut_campaign.py:SCENARIOS` and the existing five contract tests in `test_powercut_campaign.py`.
- Observation: existing LMDB suite skips are largely explicitly disabled theme/benchmark cases; one test uses a platform assumption for an oversized native map probe. Recovery tests and lifecycle isolation tests have direct selectors and should be run explicitly with zero skipped reports in the new job.
  Evidence: `rg -n '@Disabled|Assumptions\.' core/sail/lmdb/src/test --glob '*.java'`, `LmdbCrashRecoveryTest`, and `LmdbStoreModelLifecycleIT`.
- Observation: the default crashlab Python suite now runs 50 checks; its NBD wire tests require local loopback binding.
  Evidence: `scripts/lmdb-crashlab/run-tests.sh` passed 50 tests with loopback-bind permission; the sandboxed first attempt failed only with `PermissionError` at `bind(('127.0.0.1', 0))`.
- Observation: the selected no-skip Java inventory must include the snapshot lifetime, retirement recovery, and mutation journal suites alongside the crash and map-growth suites.
  Evidence: `ci_gate.py` lists nine Surefire classes and one Failsafe class; matching test sources are present in `core/sail/lmdb/src/test`.
- Observation: the first hosted ARM64 run exposed that the 10,000-statement replay cut did not reach a spilled-journal map-growth replay there; the child completed and raised its existing protocol assertion rather than being silently skipped.
  Evidence: [GitHub Actions run 36333236346](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36333236346), Surefire reported 15 `LmdbCrashRecoveryTest` cases with one failure, zero errors, and zero skips.
- Observation: after raising that case to 20,000 statements, the hosted Java gate passed all 86 selected tests with zero skips, but ARM guest boot stopped because `virtio-net-pci` attempted to load a missing UEFI option ROM.
  Evidence: [GitHub Actions run 36334103137](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36334103137), `guest/provision-qemu.log` says `failed to find romfile "efi-virtio.rom"`.
- Observation: the initial always-uploaded artifact was 688,376,851 bytes, mostly the 620,224,512-byte Ubuntu base image and 67,108,864-byte UEFI variables copy.
  Evidence: the artifact manifest for run 36334103137 included both regenerable inputs and small test reports/logs.
- Decision: raise the replay-cut fixture to 20,000 statements, matching the seeded replay fixture, so the native writer must spill the transaction journal and reach an actual post-growth replay on the hosted ARM64 runner.
  Evidence: after the fixture adjustment, the full `LmdbCrashRecoveryTest` selector passed locally with 15 tests and zero skips; see `initial-evidence.txt` and `logs/mvnf/20260927-163858-verify.log`.
- Decision: disable only the unused QEMU UEFI network option ROM with `romfile=`; retain the virtio NIC and user-mode network needed by guest package provisioning.
  Evidence: the same QEMU binary stayed alive with that argument in a bounded local TCG startup check.
- Decision: exclude regenerable guest OS base/overlay, UEFI variable copy, and cloud-init ISO from the uploaded artifact while keeping checksums, serial/QEMU logs, Maven reports, independent witnesses, and preserved crash images.
  Rationale: preserve reproducible failure evidence without uploading hundreds of megabytes of OS provisioning inputs on every pull request.

## Decision Log

- Decision: place the gate in an unconditional pull-request workflow with `contents: read`, no path, draft, label, or secret-based gate.
  Rationale: the QEMU result must be present for all pull requests that could change this behavior, including fork-originated PRs, and no repository settings are changed here.
  Date/Author: 2026-09-27 / Codex.
- Decision: use a public `ubuntu-24.04-arm` runner and explicit QEMU TCG acceleration.
  Rationale: the guest and LWJGL native artifact are ARM64 Linux. GitHub lists ARM64 public runners; nested virtualization is not an officially supported dependency, so TCG makes the job portable without relying on `/dev/kvm`.
  Date/Author: 2026-09-27 / Codex.
- Decision: make a single driver execute calibration, acknowledged namespace recovery, and all three deterministic cuts, then validate their complete reports.
  Rationale: one manifest of required cases prevents a workflow edit from silently omitting a campaign; strict report checks prove two stable recoveries and public index/context reads rather than only command exit status.
  Date/Author: 2026-09-27 / Codex.
- Decision: require A+B after `commit-returned-before-ack` because the native commit returned under `forceSync=true`.
  Rationale: absence of the separate external acknowledgment makes the witness uncertain to the parent process, but the writer's observed successful commit return is enough to require durable new state.
  Date/Author: 2026-09-27 / Codex.

## Outcomes & Retrospective

The 20,000-statement replay fixture passed the hosted Java gate (86 selected tests, zero skips). The next hosted run must validate guest provisioning and all calibrated campaigns; its predecessor stopped before provisioning because QEMU requested a missing UEFI NIC ROM. Completion requires a hosted workflow run to execute calibration and every required campaign, publish evidence, and retain zero skips for the selected Java tests. Local Python contracts or Maven results alone do not satisfy this outcome.

## Context and Orientation

`.github/workflows/pr-verify.yml` runs ordinary Maven tests on Ubuntu, but it does not boot the crashlab guest. `scripts/lmdb-crashlab/run_calibration.py` checks that guest filesystem writes reach the volatile NBD backend through FLUSH/FUA. `run_campaign.py` verifies acknowledged namespace-only commits. `run_powercut_campaign.py` runs the three deterministic LMDB commit cutpoints and reopens the same preserved image twice. The runners now support both macOS `hdiutil` and Linux `genisoimage`; they launch a provisioned Ubuntu ARM64 guest and use the actual compiled RDF4J classes, ARM64 LWJGL native, a writable ext4 data disk, and an independent result witness. The guest provisioning script writes its completion marker through the `virt` machine's PL011 serial console.

The new workflow must keep source, classpath dependencies, QEMU guest image, faulted data image, and external witness separate. The cloud guest is prepared once for Java 25 and ext4 tools, then each campaign uses its own overlay and NBD data image. QEMU runs with `-accel tcg`; no host KVM, workflow secret, disk mount, or mutable shared data image is assumed. The repository's existing unit workflow also runs `LmdbCrashRecoveryTest`; the new job explicitly selects recovery and isolation classes and rejects missing, failing, or skipped Surefire/Failsafe reports.

## Plan of Work

First add a failing Python contract test showing that an A-only recovery is rejected after the connection's native commit-returned marker. Tighten the scenario contract and document that guarantee. Add a CI campaign driver plus tests that require calibration, the namespace ACK result, and each power-cut scenario exactly once; it must reject failed or skipped outcomes, wrong scenario names, missing report fields, missing public index checks, and inconsistent recovery hashes.

Then add an ISO builder path using `genisoimage` alongside the existing `hdiutil` path. Provision a disposable Ubuntu 24.04 ARM64 cloud image with Temurin Java 25 and ext4 utilities using cloud-init. The QEMU invocation uses a TCG `virt` machine, a writable UEFI variables copy, no `-snapshot` or unsafe cache flags, a read-only OS source, and the existing volatile NBD data device with writeback and flush support.

Add an unconditional PR workflow with read-only contents permission and a bounded job timeout. It installs QEMU/UEFI/ISO tools, builds the exact LMDB test classes and Linux ARM64 dependency classpath, runs selected recovery and lifecycle tests with report verification that requires zero skips, provisions the guest, runs all four durability campaigns, validates their reports, and uploads logs/reports/crash images under `always()` without masking earlier failures. On success the job summary names the QEMU TCG model, each required case, the complete recovery outcomes, and the simulated fault boundary.

## Concrete Steps

From the repository root, run the required offline root quick clean install before Maven tests. Run the Python contract tests directly from `scripts/lmdb-crashlab`; they do not require `-am` or a Maven test invocation. For a focused Java check use `mvnf` or the documented Maven test runner without `-am` or `-q`. The final acceptance is a GitHub Actions run on the updated PR head where the QEMU job succeeds, every required case has a report, all selected Java tests have zero skips, and the artifact contains the preserved recovery evidence.

## Validation and Acceptance

The report validator must reject a missing calibration, missing namespace campaign, missing/duplicated/wrong power-cut scenario, any reported skip/failure, absent native commit/cutpoint witness, absent or empty state hash, public index check not marked `COMPLETED`, unequal recovery outcomes, or unequal full-state hashes. For commit-returned-before-ack it requires exactly A+B in both recoveries. For the two pre-native-commit cuts it requires exactly A in both recoveries. The namespace campaign must report `PUBLIC_ORACLE_PASS` with no findings, and calibration must report success after observing the expected synced/unflushed device behavior.

The selected Java classes are `LmdbCrashRecoveryTest`, `LmdbStoreFlushReproductionTest`, `TripleStoreAutoGrowTest`, `ValueStoreTermIndexRecoveryTest`, `LmdbConstructorCleanupTest`, `PersistentSetFactoryTransactionOwnershipTest`, `LmdbSnapshotValueLifetimeTest`, `LmdbValueRetirementRecoveryTest`, and `TxnMutationJournalTest`; `LmdbStoreModelLifecycleIT` is the required Failsafe class. Each must appear in its Surefire/Failsafe report with zero failures, errors, and skipped tests. Other explicitly disabled theme/benchmark and sketch-placement cases remain out of this gate, along with disabled `SailSourceModelTest` and `LongMultithreadedTransactions`, property-gated `LmdbRegressionPlanCaptureTest`, and a platform-assumed oversized map probe in `TripleStoreInitializationTest`; do not claim the entire LMDB module has zero skips. The checked-in QEMU workflow itself must have no `continue-on-error`, conditional skip, secrets dependency, or `paths` filter.

## Idempotence and Recovery

Every job uses a fresh runner-owned temporary directory, newly downloaded and checksum-verified cloud image, copied UEFI variables, per-boot guest overlay, and distinct NBD image. If a guest or campaign fails, retain the external witness, pre-recovery image, QEMU serial logs, NBD events, and JSON reports as an Actions artifact. Uploading evidence is an `always()` step and does not change the prior job result. The only mutable Git operations are the explicitly authorized branch commits and push; do not change repository branch protection or settings.

## Artifacts and Notes

The required cases are the one device calibration, one acknowledged namespace-only campaign, and `mixed-replay-before-native-commit`, `dictionary-before-triple-commit`, and `commit-returned-before-ack`. Each power-cut report must show two recovery IDs from separate copies of the same immutable crash image.

## Interfaces and Dependencies

The CI driver must consume only the checked-in runner scripts and generated report files. Host dependencies are Python 3, Maven/JDK 25, `qemu-system-aarch64`, `qemu-img`, AArch64 UEFI firmware, and `genisoimage`; guest provisioning uses official Ubuntu ARM64 cloud image checksums and the Eclipse Adoptium package signing key. All external prerequisites are required inputs: a missing tool, image, Java version, report, or artifact fails the workflow rather than skipping a case.
