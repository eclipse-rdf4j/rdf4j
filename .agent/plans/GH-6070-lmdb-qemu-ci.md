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
- [done] Move hosted guest to x86 KVM.
- [done] Commit and push x86 KVM gate.
- [done] Diagnose hosted ACL verifier mismatch.
- [done] Capture hosted QEMU access evidence.
- [done] Test bounded QEMU probe controller.
- [done] Validate refactored preflight gate.
- [done] Verify QEMU process-group cleanup.
- [done] Test guest cut process ordering.
- [done] Verify wrapped QEMU teardown failures.
- [done] Run complete crashlab Python suite.
- [done] Commit and push CI fix.
- [done] Fix hosted KVM credential drop.
- [done] Pass 66 crashlab Python checks.
- [done] Reproduce hosted shell syntax failure.
- [done] Correct probe and rerun tests.
- [done] Reach hosted crash-cut recovery.
- [done] Reproduce long control-socket path failure.
- [done] Fix socket path and retest.
- [done] Validate actual namespace FLUSH event.
- [done] Add cut-contract and packaging tests.
- [done] Add compact artifact staging.
- [done] Pass seventy-five harness checks.
- [in_progress] Commit and rerun hosted Actions.
- [todo] Update PR evidence and plan.

## Surprises & Discoveries

- Observation: initial runner inspection found seed ISO and QEMU defaults tied to macOS. The runner now accepts Linux `genisoimage`, QEMU machine/accelerator selection, and a guest native-library classifier while preserving local ARM64 use.
  Evidence: the `hdiutil`/`genisoimage` contract tests, `guest_qemu_arguments`, and the x86_64/Linux classifier tests in `test_runner_common.py`.
- Observation: `commit-returned-before-ack` currently accepts either old A or new A+B even when its witness says the native commit returned successfully. That makes the oracle unable to detect loss of a force-synced committed transaction.
  Evidence: `run_powercut_campaign.py:SCENARIOS` and the existing five contract tests in `test_powercut_campaign.py`.
- Observation: existing LMDB suite skips are largely explicitly disabled theme/benchmark cases; one test uses a platform assumption for an oversized native map probe. Recovery tests and lifecycle isolation tests have direct selectors and should be run explicitly with zero skipped reports in the new job.
  Evidence: `rg -n '@Disabled|Assumptions\.' core/sail/lmdb/src/test --glob '*.java'`, `LmdbCrashRecoveryTest`, and `LmdbStoreModelLifecycleIT`.
- Observation: the default crashlab Python suite runs 55 checks; its NBD wire tests require local loopback binding.
  Evidence: `scripts/lmdb-crashlab/run-tests.sh` passed 55 tests with test-owned loopback binding; the restricted first attempt failed only with `PermissionError` at `bind(('127.0.0.1', 0))`.
- Observation: the selected no-skip Java inventory must include the snapshot lifetime, retirement recovery, and mutation journal suites alongside the crash and map-growth suites.
  Evidence: `ci_gate.py` lists nine Surefire classes and one Failsafe class; matching test sources are present in `core/sail/lmdb/src/test`.
- Observation: the first hosted ARM64 run exposed that the 10,000-statement replay cut did not reach a spilled-journal map-growth replay there; the child completed and raised its existing protocol assertion rather than being silently skipped.
  Evidence: [GitHub Actions run 36333236346](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36333236346), Surefire reported 15 `LmdbCrashRecoveryTest` cases with one failure, zero errors, and zero skips.
- Observation: after raising that case to 20,000 statements, the hosted Java gate passed all 86 selected tests with zero skips, but ARM guest boot stopped because `virtio-net-pci` attempted to load a missing UEFI option ROM.
  Evidence: [GitHub Actions run 36334103137](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36334103137), `guest/provision-qemu.log` says `failed to find romfile "efi-virtio.rom"`.
- Observation: the initial always-uploaded artifact was 688,376,851 bytes, mostly the 620,224,512-byte Ubuntu base image and 67,108,864-byte UEFI variables copy.
  Evidence: the artifact manifest for run 36334103137 included both regenerable inputs and small test reports/logs.
- Observation: the ARM TCG guest installed Temurin 25.0.4.1, emitted `CRASHLAB_PROVISIONED`, and reached systemd poweroff; QEMU stayed alive until the 1800-second timeout.
  Evidence: [GitHub Actions run 36335095831](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36335095831), archived `guest/provision-serial.log` and `guest/provision-qemu.log`; the marker was at guest uptime 1732.4s and Java gate reports had zero skips.
- Observation: the first x86_64 hosted run reached the KVM preflight, applied the correct job-user ACL, then stopped because `getfacl` printed the username while the check expected a numeric UID.
  Evidence: [GitHub Actions run 36339315491](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36339315491), job log shows `user:runner:rw-` followed by a failed `grep` for `user:$UID:rw-`; Maven and guest steps were skipped.
- Observation: after the ACL representation fix, the job user's direct KVM API/VM creation succeeded but the QEMU process still reported `EACCES`; this does not identify the cause.
  Evidence: [GitHub Actions run 36339519128](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36339519128), uploaded `kvm-preflight` artifact shows `user:1001:rw-`, `KVM API 12; created test VM fd 4`, and QEMU's `failed to initialize kvm: Permission denied`.
- Observation: the new open trace confirmed the direct QEMU process gets `EACCES` from `openat("/dev/kvm", O_RDWR|O_CLOEXEC)`, but the step stopped before the same-UID group probe.
  Evidence: [GitHub Actions run 36341154827](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36341154827), `kvm-preflight/direct-user/open.trace` records `= -1 EACCES`; the artifact has no primary-group attempt or denial-log files, so it provides no group-ownership result and the immediate shell exit remains under investigation.
- Observation: `probe_state()` used the exit status of `ps | tr` inside command substitutions while the workflow enabled `set -euo pipefail`; a vanished child could abort the shell before the missing-process branch ran. The workflow now delegates child/QMP lifecycle to a Python controller that checks `Popen.poll()` and tests exits before socket creation, after socket creation but before the QMP greeting, and optional direct-probe failure followed by required group-probe success.
  Evidence: `scripts/lmdb-crashlab/test_qemu_kvm_preflight.py` passed four tests; the first workflow contract run failed because the helper was absent, the focused regression run passed six tests, and `bash scripts/lmdb-crashlab/run-tests.sh` passed all 59 tests.
- Observation: the prior campaign runners stopped QEMU before requesting the NBD volatile-sector cut. Earlier guest campaign reports therefore do not validate the newly enforced fence-first ordering. Current runners cut/disconnect the backend while the guest is still alive, then stop and reap its process group; a backend-cut exception also stops the guest and re-raises the original failure.
  Evidence: `test_process_lifecycle.py` covers the fence-before-stop order, a wrapper exiting before a TERM-ignoring child, bounded group escalation/quiescence, and cleanup after backend-cut failure; `bash scripts/lmdb-crashlab/run-tests.sh` passed all 64 tests. No historical GitHub Actions run reached the real campaign tier. Earlier completed local externally orchestrated campaigns used the old order, so the next hosted run must execute calibration and all four campaigns with this ordering.
- Observation: the first hosted attempt on commit `ac97814ea3` stopped before Maven because the required primary-group command used `sudo -n -u runner -g kvm`, which returned `sudo: a password is required`; the QEMU wrapper therefore exited without starting QEMU. The direct-user strace probe still recorded `openat("/dev/kvm", O_RDWR|O_CLOEXEC) = -1 EACCES`, and the direct-user context snapshot did not contain the earlier per-user ACL entry, so the effective access cause is still unproven.
  Evidence: [GitHub Actions run 36343615627](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36343615627), artifact `lmdb-qemu-durability-36343615627-1` under `kvm-preflight/{result.txt,direct-user/open.trace,primary-group/process-context.txt}`. Replace the password-prompting same-user group switch with a bounded explicit credential drop, record its effective uid/gid/groups, and require a real QEMU/QMP pass before any guest work.
- Observation: run 36344640588 stopped before Maven or QEMU because the inline `runner_groups` Python command lacked the final parenthesis for `print`; a workflow contract test now compiles every inline Python snippet and executes this one with duplicate-group input.
  Evidence: [GitHub Actions run 36344640588](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36344640588); the test failed before the workflow fix, both focused KVM tests passed after it, and the full crashlab Python suite passed 67 tests.
- Observation: run 36345043610 passed the KVM preflight, selected zero-skip Java gates, Python harness, guest provisioning, calibration, and namespace campaign. The mixed-replay writer reached its deterministic pre-authoritative-commit cut, but recovery's NBD control socket path was too long for AF_UNIX; recovery did not start, so this run provides no LMDB recovery verdict for that cut.
  Evidence: the uploaded artifact's `campaigns/mixed-replay-before-native-commit/results/actual-recovery-1-backend.log` records `OSError: AF_UNIX path too long`. A real-backend regression reproduced this before the path mapping and passed afterward together with ordinary-path and failed-startup cleanup cases; the full crashlab suite passed 70 tests.
- Observation: run 36346986710 completed the namespace public recovery oracle, then failed because the CI gate interpreted the LMDB commit's final `FLUSH_DONE` as an unwanted post-write sync.
  Evidence: the range-extracted report has `PUBLIC_ORACLE_PASS`, 80 successful flushes, and `last_data_write_followed_by_no_flush_or_fua=false`; the event trace ends with `WRITE` at event 369 followed by `FLUSH_DONE` at event 372. The namespace commit has `forceSync=true`, so that flush is expected. New report checks validate the live-guest NBD-cut-before-SIGKILL sequence instead.
- Observation: the previous evidence upload was 2,240,743,172 bytes because it included mutable campaign data images, recovery copies, and QEMU overlays in addition to preserved crash evidence.
  Evidence: the artifact ZIP index showed repeated 134,217,728-byte `data.raw` files under `campaigns/*/data/`, `working-recovery-*`, and `results/pre-recovery-image/`, plus regenerable QEMU overlays. The compact packager retains reports, witnesses, NBD events, Maven/KVM/provision logs, and gzip-encodes immutable pre-recovery images byte-for-byte while excluding mutable images and overlays.
- Decision: run the required group-context QEMU through root-launched `setpriv`, then verify it has the runner's real/effective/saved/filesystem UID, the KVM primary GID, no Linux capabilities, and `no_new_privs` before starting QEMU.
  Rationale: the hosted runner allowed `sudo -n` system ACL changes but rejected `sudo -n -u runner -g kvm`; this makes the credentials explicit without running QEMU as root or relying on group membership inherited by the job shell. The controller also records available udev device information, rules, and journal entries while investigating the ACL snapshot difference.
  Date/Author: 2026-09-27 / Codex.
- Decision: raise the replay-cut fixture to 20,000 statements, matching the seeded replay fixture, so the native writer must spill the transaction journal and reach an actual post-growth replay on the hosted ARM64 runner.
  Evidence: after the fixture adjustment, the full `LmdbCrashRecoveryTest` selector passed locally with 15 tests and zero skips; see `initial-evidence.txt` and `logs/mvnf/20260927-163858-verify.log`.
- Decision: disable only the unused QEMU UEFI network option ROM with `romfile=`; retain the virtio NIC and user-mode network needed by guest package provisioning.
  Evidence: the same QEMU binary stayed alive with that argument in a bounded local TCG startup check.
- Decision: exclude regenerable guest OS base/overlay, UEFI variable copy, and cloud-init ISO from the uploaded artifact while keeping checksums, serial/QEMU logs, Maven reports, independent witnesses, and preserved crash images.
  Rationale: preserve reproducible failure evidence without uploading hundreds of megabytes of OS provisioning inputs on every pull request.
- Decision: use an x86_64 hosted Linux runner with a measured `/dev/kvm` QEMU preflight for CI; preserve ARM64 local runs and observe guest poweroff before terminating a still-live QEMU host process.
  Rationale: ARM TCG required nearly 30 minutes just to provision the Java guest, so it is not a practical campaign path; KVM must be verified rather than assumed or silently skipped.
- Decision: grant `/dev/kvm` read/write access only to the current job user, exercise `KVM_GET_API_VERSION` and `KVM_CREATE_VM`, then require a paused QEMU process configured with KVM.
  Rationale: device ownership may deny the runner user even when KVM is available; a bounded API and forced-accelerator probe fails before Maven or guest provisioning without introducing a TCG fallback.
- Decision: request numeric ACL output from `getfacl` when validating the user-specific entry.
  Rationale: names are rendered by default even when the ACL was set by numeric UID; `--numeric` makes the output stable against account-name formatting.
- Decision: treat the QEMU `EACCES` as unresolved until both process/device context and a same-UID primary-device-group probe are captured.
  Rationale: direct KVM ioctl success shows the ACL applies to one process, but does not establish why QEMU fails. Collect QEMU identity/capabilities and available AppArmor denials, trace its `/dev/kvm` open, then test a same-UID invocation with `/dev/kvm`'s primary group without disabling host security.
- Decision: optional host-denial evidence must not prevent the required same-UID group probe from running.
  Rationale: run 36341154827 preserved the direct `EACCES` trace but stopped before the candidate QEMU invocation; optional kernel metadata collection is now explicitly best-effort while the final KVM process check remains required.
- Decision: move process/QMP lifecycle supervision into a small Python helper with subprocess return-code handling, while keeping the workflow's ACL setup and the existing forced-KVM requirement.
  Rationale: absent child PIDs are normal diagnostic outcomes for the optional direct probe and must be recorded before continuing; shell `set -e`/pipeline behavior currently couples them to whole-step failure. Both optional-direct continuation and required-group failure must be tested deterministically.
  Date/Author: 2026-09-27 / Codex.

## Decision Log

- Decision: place the gate in an unconditional pull-request workflow with `contents: read`, no path, draft, label, or secret-based gate.
  Rationale: the QEMU result must be present for all pull requests that could change this behavior, including fork-originated PRs, and no repository settings are changed here.
  Date/Author: 2026-09-27 / Codex.
- Initial decision (superseded): use a public `ubuntu-24.04-arm` runner and explicit QEMU TCG acceleration.
  Rationale at the time: avoid assuming hosted KVM availability. Run 36335095831 showed ARM TCG did provision correctly but took 1732 seconds to install Java and then kept QEMU alive through poweroff, so the hosted design moved to measured x86_64/KVM.
  Date/Author: 2026-09-27 / Codex.
- Decision: replace the hosted ARM TCG configuration with Ubuntu x86_64, QEMU x86_64, OVMF, and required KVM preflight.
  Rationale: the ARM guest did finish provisioning, but TCG made it consume the full 30-minute bound; hosted x86 KVM needs a measured preflight and a fast fail if unavailable.
  Date/Author: 2026-09-27 / Codex.
- Decision: make a single driver execute calibration, acknowledged namespace recovery, and all three deterministic cuts, then validate their complete reports.
  Rationale: one manifest of required cases prevents a workflow edit from silently omitting a campaign; strict report checks prove two stable recoveries and public index/context reads rather than only command exit status.
  Date/Author: 2026-09-27 / Codex.
- Decision: require A+B after `commit-returned-before-ack` because the native commit returned under `forceSync=true`.
  Rationale: absence of the separate external acknowledgment makes the witness uncertain to the parent process, but the writer's observed successful commit return is enough to require durable new state.
  Date/Author: 2026-09-27 / Codex.
- Decision: preserve `last_data_write_followed_by_no_flush_or_fua` as a diagnostic field, but validate that the guest remained live until the NBD cut and was reaped afterward.
  Rationale: a correct forceSync LMDB transaction can issue its final FLUSH after its last data WRITE; that event is required durability behavior, not an extra harness sync. The checked-in cut helper and new action-order report still reject a guest sync or shutdown before cutting the volatile device.
  Date/Author: 2026-09-27 / Codex.
- Decision: upload a curated evidence tree with gzip-compressed immutable crash images instead of the full scratch directory.
  Rationale: the prior artifact exceeded 2.2 GB because it repeated working NBD images and OS overlays; these are regenerable, while reports, independent witnesses, event logs, test reports, and pre-recovery image contents remain available.
  Date/Author: 2026-09-27 / Codex.

## Outcomes & Retrospective

The 20,000-statement replay fixture passed the hosted Java gate (86 selected tests, zero skips). Run 36335095831 also passed that gate; the ARM guest installed Java 25 and reached poweroff, but TCG took 1732 seconds to provision and its QEMU process did not exit before the 1800-second bound. The first x86_64/KVM run, 36339315491, failed at ACL-output parsing before the KVM API, Maven, or guest checks. Run 36339519128 confirmed the direct runner user can query KVM API 12 and create a VM, but QEMU's own KVM initialization still failed with permission denied. Run 36341154827 traced that error to QEMU's `/dev/kvm` open returning `EACCES`; the step stopped before the same-UID group probe, so the access cause remains unresolved. Run 36343615627 captured the ACL entry at the KVM API probe, then showed it absent in the direct-user context; the required group wrapper stopped at a password prompt before launching QEMU. Run 36344640588 then stopped at a missing parenthesis in the inline group-list Python, before Maven or QEMU. The in-repository test reproduced the syntax error and passed after the fix; the complete local crashlab suite passed 67 tests. Run 36345043610 passed KVM, Java, harness, guest provisioning, calibration, and namespace, and reached the mixed-replay cut, but its recovery backend could not bind an overlong Unix socket. That failed launch is not an LMDB recovery finding; the real-backend path regression now passes and the full local crashlab suite passes 70 tests. No hosted run has yet validated all recovery campaigns end to end. The previous inline supervisor's `ps | tr` command substitution could exit under `set -euo pipefail` when the child vanished; focused tests now prove that an optional direct-process exit is recorded and the required same-UID group probe still runs, while a required-process exit fails. Earlier completed local externally orchestrated guest campaigns used stop-QEMU-then-cut ordering and do not validate the new fence-first boundary. The local process lifecycle suite now checks NBD-cut failure cleanup and wrapper/child reaping. The latest hosted run 36346986710 at head `1ee091953aa97bc33367cb29ab864e988b34fc9a` passed KVM, the selected Java gate, the Python harness, guest provisioning, calibration, and campaign execution, but the post-run gate rejected the namespace report's final commit FLUSH. The actual report/event trace confirms an oracle bug: public namespace checks passed, and the final `FLUSH_DONE` followed the final data write as part of the forceSync commit. The validator now preserves that diagnostic while requiring the status/cut/SIGKILL sequence and a live writer guest at cut time. The same change adds curated artifact staging with exact-byte gzip preservation for immutable crash images. `bash scripts/lmdb-crashlab/run-tests.sh` passed 74 tests in 9.944 seconds. A fresh hosted run is required to validate the corrected report contract, all four campaigns, and the reduced artifact while retaining the selected Java zero-skip gate.

Current validation (2026-09-27): the curated package preserves early-failure campaign JSON, runner/QEMU setup logs, and guest checksum manifests while excluding binary guest images. Package tests cover a runner failure before campaigns or results exist and verify those diagnostics survive. `bash scripts/lmdb-crashlab/run-tests.sh` passed 75 tests in 9.714 seconds, and changed Python files pass `py_compile`; this supersedes the earlier 74-test local result. The remaining acceptance is a pushed-head GitHub Actions run that completes calibration, the namespace ACK campaign, and all three recovery cuts, then uploads the curated artifact.

## Context and Orientation

`.github/workflows/pr-verify.yml` runs ordinary Maven tests on Ubuntu, but it does not boot the crashlab guest. `scripts/lmdb-crashlab/run_calibration.py` checks that guest filesystem writes reach the volatile NBD backend through FLUSH/FUA. `run_campaign.py` verifies acknowledged namespace-only commits. `run_powercut_campaign.py` runs the three deterministic LMDB commit cutpoints and reopens the same preserved image twice. The runners support macOS `hdiutil` and Linux `genisoimage`, ARM64/TCG for local use, and x86_64/KVM for CI. They use the actual compiled RDF4J classes, the matching LWJGL native, a writable ext4 data disk, and an independent result witness. The guest provisioning script selects the serial console for the QEMU machine and waits for a guest-originated QMP shutdown before exiting.

The workflow keeps source, classpath dependencies, QEMU guest image, faulted data image, and external witness separate. The cloud guest is prepared once for Java 25 and ext4 tools, then each campaign uses its own overlay and NBD data image. The CI job grants a per-UID `/dev/kvm` ACL for direct API validation, captures QEMU/device access diagnostics, and then requires forced-KVM QEMU under the same UID with the device's primary group; there is no root QEMU, TCG fallback, or guest-tier skip. The repository's existing unit workflow also runs `LmdbCrashRecoveryTest`; the new job explicitly selects recovery and isolation classes and rejects missing, failing, or skipped Surefire/Failsafe reports.

## Plan of Work

First add a failing Python contract test showing that an A-only recovery is rejected after the connection's native commit-returned marker. Tighten the scenario contract and document that guarantee. Add a CI campaign driver plus tests that require calibration, the namespace ACK result, and each power-cut scenario exactly once; it must reject failed or skipped outcomes, wrong scenario names, missing report fields, missing public index checks, and inconsistent recovery hashes.

Then add an ISO builder path using `genisoimage` alongside the existing `hdiutil` path. Provision a disposable Ubuntu 24.04 x86_64 cloud image with Temurin Java 25 and ext4 utilities using cloud-init. The hosted QEMU invocation uses a `q35` machine and verified KVM, OVMF with a writable variables copy, no `-snapshot` or unsafe cache flags, a read-only OS source, and the existing volatile NBD data device with writeback and flush support. Keep the ARM64/HVF local runner path working.

Add an unconditional PR workflow with read-only contents permission and a bounded job timeout. It installs QEMU/OVMF/ISO tools, measures `/dev/kvm` with a bounded QEMU startup probe, builds the exact LMDB test classes and Linux x86_64 dependency classpath, runs selected recovery and lifecycle tests with report verification that requires zero skips, provisions the guest, runs all four durability campaigns, validates their reports, and uploads logs/reports/crash images under `always()` without masking earlier failures. On success the job summary names the QEMU KVM model, each required case, the complete recovery outcomes, and the simulated fault boundary.

## Concrete Steps

From the repository root, run the required offline root quick clean install before Maven tests. Run the Python contract tests directly from `scripts/lmdb-crashlab`; they do not require `-am` or a Maven test invocation. For a focused Java check use `mvnf` or the documented Maven test runner without `-am` or `-q`. The final acceptance is a GitHub Actions run on the updated PR head where the x86_64/KVM QEMU job succeeds, every required case has a report, all selected Java tests have zero skips, and the artifact contains the preserved recovery evidence.

## Validation and Acceptance

The report validator must reject a missing calibration, missing namespace campaign, missing/duplicated/wrong power-cut scenario, any reported skip/failure, absent native commit/cutpoint witness, absent or empty state hash, public index check not marked `COMPLETED`, unequal recovery outcomes, or unequal full-state hashes. For commit-returned-before-ack it requires exactly A+B in both recoveries. For the two pre-native-commit cuts it requires exactly A in both recoveries. The namespace campaign must report `PUBLIC_ORACLE_PASS` with no findings, and calibration must report success after observing the expected synced/unflushed device behavior.

The selected Java classes are `LmdbCrashRecoveryTest`, `LmdbStoreFlushReproductionTest`, `TripleStoreAutoGrowTest`, `ValueStoreTermIndexRecoveryTest`, `LmdbConstructorCleanupTest`, `PersistentSetFactoryTransactionOwnershipTest`, `LmdbSnapshotValueLifetimeTest`, `LmdbValueRetirementRecoveryTest`, and `TxnMutationJournalTest`; `LmdbStoreModelLifecycleIT` is the required Failsafe class. Each must appear in its Surefire/Failsafe report with zero failures, errors, and skipped tests. Other explicitly disabled theme/benchmark and sketch-placement cases remain out of this gate, along with disabled `SailSourceModelTest` and `LongMultithreadedTransactions`, property-gated `LmdbRegressionPlanCaptureTest`, and a platform-assumed oversized map probe in `TripleStoreInitializationTest`; do not claim the entire LMDB module has zero skips. The checked-in QEMU workflow itself must have no `continue-on-error`, conditional skip, secrets dependency, or `paths` filter.

## Idempotence and Recovery

Every job uses a fresh runner-owned temporary directory, newly downloaded and checksum-verified cloud image, copied UEFI variables, per-boot guest overlay, and distinct NBD image. If a guest or campaign fails, retain the external witness, pre-recovery image, QEMU serial logs, NBD events, and JSON reports as an Actions artifact. Uploading evidence is an `always()` step and does not change the prior job result. The only mutable Git operations are the explicitly authorized branch commits and push; do not change repository branch protection or settings.

## Artifacts and Notes

The required cases are the one device calibration, one acknowledged namespace-only campaign, and `mixed-replay-before-native-commit`, `dictionary-before-triple-commit`, and `commit-returned-before-ack`. Each power-cut report must show two recovery IDs from separate copies of the same immutable crash image.

## Interfaces and Dependencies

The CI driver must consume only the checked-in runner scripts and generated report files. Host dependencies are Python 3, Maven/JDK 25, `qemu-system-x86_64`, `qemu-img`, OVMF, working `/dev/kvm`, and `genisoimage`; guest provisioning uses official Ubuntu x86_64 cloud image checksums and the Eclipse Adoptium package signing key. All external prerequisites are required inputs: a missing tool, image, Java version, KVM, report, or artifact fails the workflow rather than skipping a case.
