# Qualify borrowed factor reader lifetime

Purpose: capture the reviewed test-only v2 baseline for the common reader-before-source release contract. No production change is authorized by this plan.

- [done] Complete Group v3 verification and preserve reports.
- [done] Verify v2 manifest and apply tests.
- [done] Capture smallest projection EOF first red.
- [done] Run v2 boundaries and full class.
- [done] Preserve reports and classify routes.

The v2 proposal is `/private/tmp/rdf4j-factor-reader-lifetime-test-proposal-v2-20261103/`. Apply only `tests-only.patch` when its complete source manifest matches the live checkout and the new target is absent. Keep the original smallest test byte-identical; the v2 additions cover SelectingFilter unary/product/tuple and Kernel factor/marginal/count paths. Run `projectionEofClosesReadersBeforeInputOwner` first using a named `mvnf` workspace, then preserve its report before running the full class. Classify source/consumer/route setup failures separately from reader/source ownership failures. Do not change production, public contracts, assertions, or physical routing.

Use offline Maven with the workspace runner, no `-am` or `-q` on tests. Preserve each Surefire report and verify/install logs under task-specific artifacts and create `initial-evidence.<workspace>.txt` for the first invocation. The existing Group v3 plan and evidence remain separate.

The smallest projection-EOF selector reached the ownership assertion and errored because the source was released with one live reader. The complete v2 class ran 16 cases: 13 reader-before-source ownership errors across projection, expanded, selecting, and kernel paths; one cleanup aggregation assertion failure; and the cancellation and explicit early-close controls passed. Full reports and logs are preserved under `.agent/artifacts/factor-reader-lifetime-v2-runnerfresh-20261103/runs/`; no production file was changed. The test-only baseline and classification are complete; source changes remain outside this plan pending a separate reviewed authorization.
