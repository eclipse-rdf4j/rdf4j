# Exercise live LMDB transactions during device power cuts

This living ExecPlan follows `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current. Source edits and all Maven invocations are serialized through the existing execution worker. The user authorized one scoped commit and push on the existing branch; do not switch branches, force-push, send GitHub messages, or clean unrelated artifacts.

## Purpose / Big Picture

The QEMU durability job must test actual running LMDB transactions as well as its three deliberately paused publication boundaries. A paused boundary gives a precise old/new-state assertion, but cannot sample races and interruptions while the writer continues making progress. The added campaigns run a continuous stream of numbered mixed transactions without waiting at a pause hook. A seeded device fault selects actual data-device I/O, and every crash image is reopened twice through the same complete public lookup oracle used by the precise campaigns. Reports state the witnessed execution and simulated device-loss boundary without claiming a guaranteed physical host power loss or a particular CPU instruction interruption.

## Progress

- [done] (2026-09-30) Recover exact failed job evidence.
- [done] (2026-09-30) Reproduce omitted guest replay cutpoint.
- [done] (2026-09-30) Repair guest fallback path selection.
- [done] Review witnessed running campaign design.
- [done] Add failing running campaign contracts.
- [done] Implement continuous writer and faults.
- [done] Validate recovery prefixes and coverage.
- [done] (2026-09-30) Complete format and Maven/native checks.
- [done] Publish first scoped repair head.
- [done] Add causal telemetry and phase regressions.
- [done] Repair counters and activity claim semantics.
- [done] Verify focused native and Python contracts.
- [done] Format and publish causal follow-up.
- [todo] Monitor latest exact-head hosted CI.
- [done] Preserve user-stopped RDFS run evidence.
- [done] Add failing five-second default test.
- [done] Update shared default and documentation.
- [done] Add runtime default coordinator contract.
- [done] Verify coordinator default and overrides.
- [done] Run focused config and coordinator tests.
- [done] Format and publish timeout default repair.
- [done] Record DataAndShapes stall unresolved.
- [in_progress] Verify latest exact-head hosted CI.
- [todo] Resume local ARM64 campaigns if hosted blocked.
- [todo] Record final CI evidence and disposition.

## Surprises & Discoveries

- Observation: the first published head combines a replay completed at any time in a generation with a spill observed at any time before commit. An early in-memory replay followed by a later spill can therefore be misreported as spilled replay. The transaction activity file also remains unchanged while post-commit witnesses are published, so its phase and generation are last announced progress rather than the native subphase at the cut.
  Evidence: the published `CrashPowerCutWriterMain.java` recorded only total replay completions in `afterMapGrowthReplay` and combined it with the observer's generation after commit. The bounded follow-up neutrally extracted the existing progress calculation with matching actual guest coverage, then reproduced both event orders and rejected ambiguous activity labels before repairing behavior.
- Observation: matching pre/post extraction native tests passed (2 tests each). The compiled guest helper then failed replay-before-spill with `Spill after replay must not qualify as a spilled replay`; spill-before-replay and foreign-generation controls passed. Two activity-label regressions failed, and the readiness gate accepted missing causal-observation evidence.
  Evidence: `logs/ci-6073-job-109798032840/causal-{extraction-pre-green,extraction-post-green,order-red,activity-red,readiness-red}.log`; exact commands and compact snippets are appended to `initial-evidence.txt` before the behavior repair.

- Observation: job 109798032840 in run 36687945047 uses the same SHA as PR 6073 and this checkout, `bb9a22102e2a9db96eb7f69275f214a0d56d5a49`. Calibration and acknowledged namespace recovery passed; the first precise replay scenario failed before any recovery cut was made.
  Evidence: `logs/ci-6073-job-109798032840/job-109798032840-full.log` and the extracted campaign artifact.
- Observation: whole-write capacity preflight avoids the native map-full journal replay path. The in-module `LmdbCrashRecoveryTest.PausingTripleStore` was adapted to select that path with a zero estimate; the external guest writer was not. Increasing transaction size would make the test depend on estimates rather than its intended path.
  Evidence: the actual guest/controller regression failed with `native commit returned before the requested pre-publication cut`; its pre-fix report is `logs/ci-6073-job-109798032840/pre-fix-mixed-replay-red.log`. The guest-only selection override makes all three actual cutpoint tests pass.
- Observation: the existing canonical Python suite needs local loopback/socket permission in this sandbox. Its authorized rerun passed 75 tests in 8.374 seconds.
  Evidence: `logs/ci-6073-job-109798032840/python-harness-suite-escalated.log`.

## Decision Log

- Decision: count a generation's spilled replay only when the spill observer has already recorded that generation at the native replay-completion hook. The hook updates only atomics; readiness explicitly records `spilled_replay_observation: before_replay_hook`, which the shared readiness validator requires.
  Rationale: combining independent events after commit permits an early in-memory replay and a later spill to masquerade as a spilled replay. Observer lag may miss genuine coverage but cannot retroactively qualify an earlier replay. Event-order contracts exercise the same `RunningProgress` helper used by the actual guest, without reflection or source-string assertions.
  Date/Author: 2026-09-30, Codex implementation worker.
- Decision: name the activity fields `last_published_generation`, `last_published_returned_generation`, and `last_published_phase: before_transaction`; the backend and gate share validation, and the gate requires equality with the actual CUT trace.
  Rationale: the publication remains in place during native work and post-commit marker forcing. It attests the last published continuous progress, not an exact native instruction or phase at the device cut. Preserve continuous-writer and real interrupted-I/O evidence without introducing a per-transaction handshake or extra native-hook I/O.
  Date/Author: 2026-09-30, Codex implementation worker.

- Decision: preserve every precise paused scenario and add independent running coverage.
  Rationale: exact publication-boundary guarantees and races during continuing execution test different failure surfaces. The user explicitly requested both running code and possible interruptions within I/O.
  Date/Author: 2026-09-30, Codex coordinator.
- Decision: use a continuous stream with independent finalized transaction witnesses, and validate exact complete prefixes rather than allowing any old/new outcome.
  Rationale: an unpaused writer may complete several transactions between host observations. A static A/B oracle could accept lost acknowledged commits or reject a legitimately later complete state.
  Date/Author: 2026-09-30, Codex coordinator.
- Decision: review automatic fault selection at real NBD requests before implementation.
  Rationale: waiting a host delay after one marker can cut after the interesting write has finished. A future I/O-triggered fault can interrupt a live writer without a Java pause and has an exact device event trace. Sector-level behavior and witness ordering require explicit review.
  Date/Author: 2026-09-30, Codex coordinator.
- Decision: require four running trials, covering WRITE interruption and an interior nonempty FLUSH or FUA interruption for each of normal-preflight and journal-replay profiles, with distinct seeds.
  Rationale: dropping cached sectors during WRITE proves request interruption; interrupting persistence after one sector and before the last also samples a partially persisted device request. A single old/new paused test does not establish either running surface.
  Date/Author: 2026-09-30, Codex coordinator after Sol source review.
- Decision: arm an automatic one-shot backend fault only after at least two finalized continuous transactions; every replay trial also requires a genuine spilled replay observed without a pause.
  Rationale: multiple acknowledged generations establish actual continuing work and a nontrivial lower durability bound. Device-triggered cutting avoids host-delay cuts after a finite transaction has finished. Replay completion demonstrates fallback coverage, but does not prove interruption inside replay.
  Date/Author: 2026-09-30, Codex coordinator after Sol source review.
- Decision: strictly require returned generation R and attempted generation K to satisfy R <= K <= R+1, with exact complete recovered prefix R or K.
  Rationale: one sequential writer publishes its returned witness before publishing the next attempted payload. Interrupted commit-marker publication may leave a committed K legitimately unobserved; generations below R lose acknowledged state, while any mixed prefix is corruption.
  Date/Author: 2026-09-30, Codex coordinator after Sol source review.
- Decision: provision the documented local ARM64 guest and attempt the combined campaigns after final source/build acceptance.
  Rationale: this macOS host has QEMU ARM64 with HVF/TCG, firmware, qemu-img and hdiutil. A missing task-owned guest image is a reversible prerequisite that the repository's supported provisioning workflow can supply. The user authorized verification; no host-wide installs or cloud resources are needed. Local ARM64 results remain distinct from the unrun exact hosted x86_64/KVM job.
  Date/Author: 2026-09-30, Codex coordinator.
- Decision: prioritize exact-head hosted validation after scoped publication; defer local provisioning while it is available.
  Rationale: the user authorized commit and push before further work. The hosted Linux x86_64/KVM job can provide exact-head evidence; the local ARM64/HVF guest remains a fallback if hosted validation cannot complete.
  Date/Author: 2026-09-30, Codex coordinator.

## Outcomes & Retrospective

The causal follow-up passed three compiled event-order regressions, all 21 gate tests, the complete 92-test Python suite, and all ten compiled actual guest tests (three paused boundaries, all four running trial identities, and three direct progress-order tests). The actual replay trials observed spill before their replay-completion hook and still completed multiple sequential transactions without controller releases; both independent recovery copies passed the unchanged complete public oracle. The first sandboxed Python attempt failed because local socket creation was denied; the identical authorized local-socket rerun passed. Logs are `logs/ci-6073-job-109798032840/causal-{order-green,gate-green,native-final,python-final-escalated}.log`; exact red/green evidence and neutral-extraction hit proof are appended to `initial-evidence.txt`. Source review, copyright verification, Python compilation, and `git diff --check` passed. Formatting/publication transfer to Luna after source ownership release; exact-head hosted device-cut evidence remains the coordinator's separate validation responsibility.

The original guest cutpoint defect is locally reproduced and repaired. Final acceptance before publication passed the root quick clean install, configured Spotless formatting, copyright checks, 15 focused `LmdbCrashRecoveryTest` cases, and all seven actual native guest tests after formatting. The canonical Python suite passes 88 tests. The post-format focused Surefire report is retained under `core/sail/lmdb/target/surefire-reports/`; runner logs and earlier reports remain under `logs/ci-6073-job-109798032840`. Local inventory found ARM64 QEMU/HVF, both firmware images, qemu-img and hdiutil, but no provisioned Ubuntu Java 25 qcow2; `/dev/kvm` is absent. The user authorized a scoped commit/push, so exact-head hosted CI is now the priority and local provisioning is deferred. Hosted CI has not yet been checked at the new head.

## Context and Orientation

Work from `/Users/havardottestad/Documents/Programming/rdf4j-6` on the existing `GH-6070-lmdb-native-snapshot-isolation` branch. Preserve every preexisting untracked artifact. `scripts/lmdb-crashlab/run_ci_campaigns.py` builds an unconditional ordered campaign list. `run_powercut_campaign.py` prepares QEMU writer and recovery guests, operates the volatile NBD device, preserves the crash image, and performs two recoveries. NBD is a network block device: QEMU sends reads, writes, and durability requests to `volatile_nbd.py`, whose synced image survives a cut while unsynced sectors can be discarded. `ci_gate.py` rejects missing, failed, skipped, or inconsistent reports. `.github/workflows/lmdb-qemu-durability.yml` runs the job and packages its evidence through `package_ci_artifact.py`.

Guest sources are outside the production module. `guest/CrashPowerCutWriterMain.java` writes exact A/B payloads and pauses at selected publication boundaries. `guest/powercut-writer-controller.py` independently forces those payload witnesses and signals the host when a precise boundary has been reached. `guest/CrashPowerCutFixtures.java` represents statements, namespace changes, expected explicit and inferred states, nested quoted values, and canonical state hashes. `guest/CrashPowerCutOracleMain.java` validates recovered state through public APIs including explicit/inferred statements, contexts, namespace values, quoted terms, and SPARQL. The added local `test_guest_writer_cutpoints.py` compiles the exact guest sources with Java release 25 and runs the actual controller against the built native LMDB classes, with explicit classpath input and no QEMU.

A complete prefix is all effects of transactions 1 through N and no effects from any later transaction. Every running transaction must have a finalized attempted payload before it starts native mutation. A finalized returned-commit witness can appear only after `SailConnection.commit()` returns successfully with forceSync enabled. Recovery must be at least the highest returned generation and at most the highest attempted generation, and its complete public state must exactly equal one candidate prefix in that interval. There can be an unacknowledged transaction whose native commit completed before its independent witness; this is why the upper bound includes the attempted frontier. Witnesses use unique files and atomic final publication so torn temporary writes cannot become false acknowledged generations. These bounds are collected only after the device has been fenced and the writer guest stopped, when no further witness can appear.

## Plan of Work

First complete read-only review of the native guest fix and the running witness/fault/oracle design. Record the final fault schedule, workload generation, and coverage rules here before expanding source edits. Add the smallest failing automated contracts in the native harness framework and immediately append their exact command and report snippet to `initial-evidence.txt`. Preserve reports under `logs/ci-6073-job-109798032840` before another run overwrites them.

Then add continuous running modes beside the precise modes. The guest writer must use no deliberate wait, latch, controller barrier, or replay pause after continuous execution starts. One profile exercises normal whole-write preflight and another selects journal fallback so replay is covered while execution continues. Transactions must include explicit/inferred changes, deletion and readdition, promotion, namespaces, contexts, and quoted terms as appropriate to their deterministic numbered payloads. Publish nonblocking progress telemetry and exact finalized witnesses. Keep the writer active until its device is cut; completing a finite B transaction and remaining merely alive is not running workload coverage. At least two returned generations must be finalized before the backend is armed. Capture replay counters and spilled-journal evidence in the hook without filesystem forcing or controller waits while the native coordination lock is held; finalize telemetry through the ordinary writer path.

Add a seeded, repeatable fault schedule to the runner/device interface. Arm state is held under the backend's device lock, counts future qualifying nonempty WRITE or FLUSH/FUA requests, and fires exactly once at a seeded interior sector boundary. Fence the device first, preserve the actual request/sector-progress CUT report, then raise the existing PowerLost error so the interrupted request cannot receive a successful reply. A WRITE case requires at least one admitted sector and fewer than all; a persistence case requires at least one persisted sector and fewer than all. Successful FLUSH/FUA operations retain their complete guarantees. Reject missing qualifying I/O or an exited writer. Reports include profile, trial identity, seed, selected future eligible request ordinal and sector boundary, arming and cut times, actual I/O observations, live guest evidence, writer progress, and preserved witnesses. The device cuts while the guest still runs; only then does the host stop and reap the guest. Never wait for a Java pause hook in a running trial, weaken the recovery oracle, insert a delay to conceal a race, or label process liveness alone as active transaction/I/O evidence. Actual data-device activity may include filesystem background I/O; report block-request interruption and observed continuous-writer activity without attributing a particular Java instruction or LMDB subphase from timing alone.

Extend the existing full-state oracle to build candidate complete prefixes from exact witness payloads and compare recovered public state to a valid prefix. Both recoveries use separate copies of the same immutable image, must return equal generation/outcome and state hash, and must complete every existing public check. Extend `ci_gate.py`, ordered CI campaign generation, the workflow, documentation, and curated evidence packaging to require the running matrix and its coverage. A missing trial, wrong seed/profile, absent activity or cut evidence, unchanged workload, failed recovery, or missing repeated run must fail the gate rather than be skipped.

## Concrete Steps

All commands run from the checkout root unless a command says otherwise. The required initial root install already passed. Preserve the original job evidence and existing `initial-evidence.txt` content.

    python3 scripts/lmdb-crashlab/test_guest_writer_cutpoints.py --host-classpath-file logs/ci-6073-job-109798032840/host-classpath.txt -v
    bash scripts/lmdb-crashlab/run-tests.sh
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCrashRecoveryTest --retain-logs

The execution worker must record the actual local classpath filename and focused running test selectors here when established. If local socket binding is sandbox-denied, retry that same harness suite with the already-authorized local-network permission; preserve both outputs and distinguish the setup failure from a product failure. Maven always uses `.m2_repo` and offline mode first; tests never use `-am` or `-q`. `mvnf` serializes the required root install and focused verify; retain logs and archive reports before subsequent runner cleanup. Missing dependencies permit one exact online retry, then return offline.

Before finalization, run the copyright checker for edited/new Java files, verify the actual branch formatter configuration, format only the necessary scope where possible, and run `git diff --check`. The user authorized a scoped GH-6070 commit and push on the current branch; stage only the task workflow, harness, tests, documentation and this ExecPlan. Do not amend, force-push, change branches, send GitHub messages, or include logs, crash dumps, evidence backups, or unrelated artifacts.

The local preflight found `/opt/homebrew/bin/qemu-system-aarch64` with HVF/TCG, firmware under `/opt/homebrew/opt/qemu/share/qemu`, `qemu-img`, and hdiutil; no provisioned Ubuntu ARM64 Java 25 qcow2 exists, and `/dev/kvm` is unavailable. After the user-authorized push, prioritize the exact-head hosted Linux x86_64/KVM job. Defer local provisioning while hosted acceptance can provide complete evidence. If that path is unavailable or incomplete, follow the existing `scripts/lmdb-crashlab/README.md` section `Recreate the Java 25 guest and exact test classpath` to provision the official immutable Ubuntu ARM64 image in a fresh task-owned scratch root. Verify the official checksum, create the cloud-init Java 25 package seed and qcow2 overlay, and boot with HVF, copied writable vars, user-mode networking, serial/QMP logs, and bounded process cleanup. Guest package installation stays inside the new VM; do not install host software or allocate cloud resources.

If local fallback resumes, capture a Linux ARM64 guest classpath from the already built module and `.m2_repo`, with the workspace test/classes roots. Run filesystem/device calibration first, then all four running identities and the repaired precise replay campaign, sequentially in independent roots/ports. If practical, finish the acknowledged namespace and the other two precise cuts too, allowing the complete strict matrix gate to validate the local reports. Preserve every image, guest log, witness, cut schedule, repeated recovery and source/build checksum. Local success proves this ARM64 Linux/ext4/NBD simulation and must not be reported as success of the hosted x86_64/KVM job.

## Validation and Acceptance

The original smallest guest replay test must retain its recorded red and pass after the guest-only fix. All three precise boundary tests must pass with exact witness booleans and no B acknowledgment at the cut. New tests must prove that a running writer completes multiple transactions without a controller release between them, that its replay profile observes real replay without pausing, and that the I/O fault schedule cuts a live workload rather than a completed child. Fault-policy tests must cover arm validation, single firing, seeded selection, request/sector boundaries, flush behavior, and control cleanup. Oracle tests must reject lost returned commits, partial transactions, invalid prefix intervals, malformed/torn witness finals, and differing repeated recoveries while accepting a complete unacknowledged frontier.

The full canonical harness suite, actual native guest tests, focused module recovery tests, and relevant report/packaging tests must pass. Hosted acceptance requires a future exact-head Linux x86_64/KVM run with every precise and running trial, the selected Java no-skip gate, both independent recoveries per image, coverage evidence, and preserved seed/witness/I/O records. Local host tests and simulated device tests do not by themselves establish hosted QEMU acceptance; report any unavailable environment exactly.

## Idempotence and Recovery

Every running trial owns a fresh scratch directory, image, port range, witness directory, QEMU process group, and seed-derived fault schedule. Reject preexisting nonempty or unsafe roots. On any failure, stop/reap owned process groups, preserve runner logs and fault/witness/image records, and propagate the original failure. Do not remove unrelated directories, use Git restore/reset/clean, or overwrite tracked history. Retry a trial into a fresh owned directory and record that it is a new execution, preserving the original evidence.

## Artifacts and Notes

Original failing snippet:

    test_mixed_replay_before_native_commit ... FAIL
    AssertionError: mixed-replay-before-native-commit did not reach AFTER_FULL_REPLAY_BEFORE_AUTHORITATIVE_COMMIT: RuntimeError: native commit returned before the requested pre-publication cut
    Ran 1 test in 1.393s
    FAILED (failures=1)

Original post-fix actual-guest report: `logs/ci-6073-job-109798032840/post-fix-native-cutpoints.log`. Exact downloaded job log, ZIP, extracted evidence, preexisting evidence backups, and subsequent reports remain under the same job evidence root. Root install output is `maven-build.log`.

## Interfaces and Dependencies

Use the standard-library Python unittest/subprocess/filesystem modules, existing process lifecycle helpers, the bundled JDK, and existing RDF4J/LWJGL classpath. Add no dependency. Keep faults and instrumentation entirely in the checked-in crashlab harness and same-package guest test subclasses; do not add production flags or change production LMDB commit semantics. Preserve existing paused scenario contract fields and require distinct running fields rather than reinterpreting historical reports as running coverage.

Revision note (2026-09-30): expanded the active task after the user requested running campaigns capable of racing active code and I/O. Precise cutpoints remain required; the new continuous prefix/oracle and device-trigger design is undergoing read-only review before implementation.

Revision note (2026-09-30): Sol source review approved the design with strict atomic witness publication, a one-transaction optional frontier, four mandatory profile/fault trials, automatic interior device faults, and lightweight replay instrumentation. Expanded source ownership transfers to the existing Sol reviewer for complete implementation and native/Python validation; the existing Luna worker retains Maven/final-format serialization and all original evidence.

Revision note (2026-09-30): after implementation, retired the original Luna worker because its completed task context had been idle for more than 30 minutes, and assigned a fresh Luna Max final acceptance worker. Corrected local prerequisites: ARM64 firmware and tools exist; the provisioned Java 25 qcow2 does not. Expanded verification to the documented reversible local provisioning and actual combined campaigns after serialized Maven/format acceptance.

Revision note (2026-09-30): initial running backend/frontier contracts fail (6 errors: missing arm/frontier support), and both actual guest running tests fail because the current controller rejects running scenarios. Complete outputs are preserved in running-contracts-red.log and running-native-red.log and appended to initial-evidence.txt before feature edits.

Revision note (2026-09-30): Sol implementation and native/Python validation are complete. Final green logs: logs/ci-6073-job-109798032840/running-python-final.log (88 tests, 10.118s) and running-native-final.log (7 tests, 43.945s). Exact red and green snippets are append-preserved in initial-evidence.txt. Copyright check passed; git diff --check and Python compilation passed. Final formatter and selected Java checks transfer to the serialized Maven worker after the coordinator audits its idle age (replace a worker idle over 30 minutes with the same model/effort and a concise handoff). No Maven, Git mutation, commit, push, or GitHub message was performed by the Sol feature worker. Local QEMU binaries are installed, but this macOS host has no /dev/kvm, so the mandatory Linux x86_64/KVM matrix remains unrun. Source edit ownership is released for final format and coordinator review.

Follow-up revision note (2026-09-30): hook-time spill correlation and explicit last-published progress labels are implemented and verified. Final follow-up green logs are `causal-native-final.log` (10 tests, 49.659s) and `causal-python-final-escalated.log` (92 tests, 10.378s). Source ownership is released to the serialized formatting/publication worker. The cached-context retention rule still requires retiring and replacing any completed/stopped worker after more than 30 minutes of inactivity, with the same model/effort and a concise handoff; active work remains uninterrupted.
