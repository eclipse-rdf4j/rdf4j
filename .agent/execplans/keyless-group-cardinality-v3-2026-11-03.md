# Preserve Group laws and certificate contracts

Purpose: complete the isolated keyless Group correction while retaining provider callback/error behavior, physical work, event/replay identity and contextual fallbacks. This is a scratch-only Routine D proposal, with the repository's PLANS.md preserved in baseline.

- [done] Preserve frozen v2 and captured first-reds.
- [done] Audit certificate evaluation and replay ownership.
- [done] Prepare minimal source and test revisions.
- [done] Independently review v3 contract and deltas.
- [x] Capture additive test-only baseline and positives.
- [x] Classify baseline reds and controls.
- [x] Await parent review before production work.
- [x] Apply reviewed production patch.
- [done] Freeze post-format evidence and source hashes.
- [done] Verify isolated candidate checkpoint closure.
- [done] Checkpoint exact files if green.
- [in_progress] Continue integration after Group checkpoint.

Exactly one step is in progress. The approved `production-v3.patch` applied to six production files and each resulting file matches the independently reviewed proposal hash exactly. The serialized test-only baseline is complete: the 13-case packed class compiled with seven assertion failures and six passes, while the public incompatible-join empty result and optional outer-population-three positive passed (2/2). Source pre/post state is preserved under `.agent/artifacts/keyless-group-v3-production-runnerfresh-20261103-1/`; test-first reports and logs are under `.agent/artifacts/keyless-group-v3-test-baseline-runnerfresh-20261103-1/`. The original real Group/scalar/refinement reds and outer-population green remain preserved in the prior evidence.

After independent review, the runner applies only tests-v3.patch (or the exact tests-v2-to-v3.patch if its separately verified source already has v2's test file), preserving all existing sources/reports. The new full file's baseline is absent in this audit. Proposed focused baseline command:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace keyless-group-v3-runnerfresh-20261103 PackedIsolatedGroupEvidenceTest --module core/queryalgebra/evaluation --retain-logs

The new finite-certifier assertion is an additional missing-hook control against v2; the invalid and exception cases preserve pre-existing behavior on the original baseline. Exact evidence remains a positive and must not force a hook. The serialized baseline reached the affected assertions. The cold/replay case stops at the initial zero row estimate, before cache lookup, costing trace inspection, or replay. Preserve all reports before production application.

Only after review and baseline evidence, apply production-v3.patch under the runner's source/build lock, format with exact resulting hashes captured, then rerun the same focused selection. Rerun original PackedGlobalGroupCardinalityTest and LmdbGlobalGroupCardinalityPlanTest plus the additive public compatible-join bag. Preserve keyed-empty zero, retained inner Group one before HAVING/LIMIT, outer population three, UNION/subquery bags, rejected evidence removal, valid evidence/work, and nonempty-prefix/incompatible-context zero.

Use the scoped cache/lifetime/Frontier/declared-work selections already enumerated in prior-v2/EXECPLAN.md, plus PackedPlanCacheTest#positiveFrontierRootCertificateSuppressesScalarRootCertification. Acceptance requires correct cold and replay events, unchanged provider ownership, unchanged appendFactor/contextual totals, certifier called where applicable, NaN fallback retained, invalid result rejected and exact-evidence precedence retained. No broad telemetry/feedback changes and no performance timing are part of this milestone.

Post-source `PackedIsolatedGroupEvidenceTest` passed 13/13. The packed original+provider/frontier neighbor selection passed 59/59 after a source-contract fixture correction: its Mockito request had a null query despite the real `PackedPlanValidationRequest` constructor requiring a nonnull query. The helper now supplies an encoded real `PackedQueryView` and asserts the recorded relation id 2 is a statement pattern; its replay assertions are unchanged. The first 59-case report with six such pre-assertion NPEs and the corrected matching report are both retained under `.agent/artifacts/keyless-group-v3-production-runnerfresh-20261103-1/runs/`.

The complete public `LmdbGlobalGroupCardinalityPlanTest` passed 12/12, including the original incompatible-join and optional outer-population controls. Its report and workspace logs are preserved in `runs/post-source-LmdbGlobalGroupCardinalityPlanTest/`.

Scoped copyright/SPDX/signature audit passed for all ten owned Java files. The repository-wide checker was started but stopped after it began traversing preserved `.mvnf/workspaces` generated POM copies; its complete-output replacement audit used byte-identical copies of only the ten touched files and passed. Scoped Spotless found and formatted four owned files (two production and two tests); the saved exact diff contains only line wrapping/Javadoc reflow. The post-format scoped Spotless check passed, and HEAD, index, protected benchmark hashes, and the other six owned source/test files remained unchanged. The exact input/post-format hashes and logs are in `runs/format-and-post-format-gate/`.

Matching post-format runs passed: `PackedIsolatedGroupEvidenceTest` 13/13, the packed global Group plus provider/Frontier/replay/physical/session selection 59/59, and the complete public `LmdbGlobalGroupCardinalityPlanTest` 12/12. Every Surefire report and Maven log has a preserved artifact copy under the corresponding `runs/post-format-*` directory.

Group v3 source, focused, neighbor, formatting, and post-format verification is complete. Durable completion summary and hash manifest: `.agent/artifacts/keyless-group-v3-production-runnerfresh-20261103-1/COMPLETION.md` and `final-state.json`.

An independent source snapshot was built from `git archive` at the exact recorded HEAD `65e9a38dcf2497f9528b99ef741a851a8764ca2d`, then overlaid with exactly the ten reviewed post-format Group files. The archive SHA-256 is `c0d0025d32d150182a21d7de3dd50e86df53db18d2d11bc2891c09aa3ca5a350`; its full tracked inventory and overlay manifest SHA-256 `3e1d6333747e83a1a1d9ca2a3ec52417736f00e67e5c9572421cf5c87fabe80c` are preserved in `.agent/artifacts/keyless-group-v3-isolated-head-runnerfresh-20261103/source-inventory.json`. All isolated gates passed against that source: 13 focused, 59 packed/provider/frontier/replay, and 12 public LMDB Group cases. Reports and logs are in the same artifact's `runs/` directories; `initial-evidence.keyless-group-headplus10-runnerfresh-20261103.txt` records all three selections. Maven 3.10.0 with Azul OpenJDK 27 ran offline through `mvnf`'s private workspace repository head and the source snapshot's read-through `.m2_repo` tail.

Immediately before checkpointing, revalidate the ten live file hashes, HEAD/index, staged-path emptiness, and protected files. `ThemeQueryBenchmark.java` changed externally after the earlier protected snapshot; its current user-owned hash is recorded separately as `11b0e4730d3df9c28bf77d7baba0e04016971c69751fb01db3a8ed85ec958d33`. It was not copied into the isolated candidate and must not be staged or modified. Only the ten Group paths may be staged and committed.

The isolated gates and exact-file staging conditions passed. Commit `45f608d11a275dc72a64a4490c4aa908471fd629` (`query: correct keyless group cardinality`) contains exactly the ten tested paths, with every committed blob independently checked against the isolated snapshot. The user-owned Theme file was excluded. Full checkpoint receipt: `.agent/artifacts/keyless-group-v3-isolated-head-runnerfresh-20261103/CHECKPOINT-COMPLETION.md` and `postcommit-state.json`.

All Maven work remains with the serialized runner. Use the required offline/local repository workflow, no -am or -q with tests, and no simultaneous source formatting/builds. The no-build scope here is this audit assignment, not a user-wide restriction.
