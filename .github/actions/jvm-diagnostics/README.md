# JVM diagnostics in GitHub Actions

Use this composite action with `mode: setup` after Java setup and before running JVM tests. End the job with `mode: upload` and `if: ${{ always() }}`. Supply an artifact name containing run ID, attempt, job ID and matrix index. Artifacts expire after seven days. `extra-paths` adds host-backed QEMU guest output.

Setup preserves existing `JAVA_TOOL_OPTIONS` and appends only diagnostic options. Surefire, Failsafe, Maven, direct child JVMs and Spring Boot inherit them. It changes neither heap limits nor fork policy. Tests that deliberately sanitize JVM options retain only the dedicated `RDF4J_CI_JVM_DIAGNOSTIC_OPTIONS` value.

Java 25 writes compressed HPROF on JVM-detected Java heap exhaustion, including OOMs caught by test code. HotSpot normally captures the first such event in a JVM. Native fatal crashes can produce `hs_err_pid*.log`, not a guaranteed Java heap dump. SIGKILL, host OOM kills, deliberate power cuts, runner loss, cancellation, insufficient disk and interrupted writes can prevent a complete dump or upload. Assertion failures and deliberately constructed `OutOfMemoryError` objects do not trigger heap capture.

Docker/Tomcat and Jetty use a host bind that survives `down -v`, with a new output directory on every restart. Elasticsearch Testcontainers use unique host binds and portable raw HPROF for bundled-JDK compatibility; the artifact upload compresses raw files. Their `ES_JAVA_OPTS` override Elasticsearch's default dump paths. QEMU guests write compressed heaps and fatal logs directly to the writable host-backed results share, in per-boot directories. The compact QEMU package leaves those files in place only when separately uploaded by CI.

HotSpot creates mode-0600 heaps. Before upload, the Linux runner restores ownership of otherwise-unreadable container diagnostic output in the dedicated scratch directory. It does not follow symlinks, scan unrelated directories for ownership changes, or make the whole workspace writable.

Dumps can be multiple GiB and can contain all in-memory data, including secrets. Treat artifacts in a public repository as accessible to signed-in readers. Use test data without secrets, retain sufficient runner disk/artifact capacity, and download needed evidence before expiry. Compression saves space but cannot guarantee a small dump.

Run `python3 scripts/test_ci_jvm_diagnostics.py` for host/job/launcher contracts. Guest and container contracts are included in `scripts/lmdb-crashlab/run-tests.sh`. Keep both suites and the workflow inventory current when adding another JVM test job or process boundary.
