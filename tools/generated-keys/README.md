# Compact generated terminal keys — latest revision

Read **OPTIMIZATION_REPORT.md** first for the current implementation, measurements, admission limits and integration status. The older REPORT.md below describes the preceding implementation; its per-value maps and benchmark numbers are no longer current.

```bash
export JAVA_HOME=/path/to/jdk-25-or-newer
python3 tools/generated-keys/run.py --out /tmp/generated-keys-tests
python3 tools/generated-keys/compare.py --baseline /path/to/extracted/generated-value-keys-source \
  --out /tmp/generated-keys-comparison --forks 5 --bench --memory --c2
```

The new runtime-value budget is `rdf4j.lmdb.runtimeValues.maxBytes`. It is an admission estimate, not a full heap/RSS limit. Runtime payload spilling is not implemented. Both complete-source and production-only patches apply to the preceding generated-value-keys-source.zip; choose one patch. The cumulative patch targets the original judges' tree.

---

# Generated terminal keys

This directory contains offline tests, a direct key-path microbenchmark, and full-repository integration tests for dictionary-free generated DISTINCT/GROUP BY keys. The runner accepts the repository source layout (`core/sail/lmdb/src/main/java/`) and the legacy standalone layout (`java/`) through one source-root resolver. Production code remains in the repository's LMDB module; the offline runner copies selected production files into its marked disposable output.

Read `REPORT.md` for supported plan shapes, exact identity rules, memory costs and validation limits. Automatic admission is default-on; `-Drdf4j.lmdb.generatedKeys.enabled=false` retains store-first keying. This is independent of value-overlay settings.

## Run the offline tests

```bash
export JAVA_HOME=/path/to/jdk-25-or-newer
python3 tools/generated-keys/run.py --out /tmp/generated-keys --bench --forks 5
```

The runner has no downloaded dependencies. It uses explicit test doubles under `stubs/` and compiles selected complete production files and exact production class/method bodies. When a source bundle contains `KernelPeerCancelledException`, the production class replaces its compatibility double; older bundles without that class retain the explicitly labeled double. The generated build manifest identifies every source origin. It is not a full RDF4J build and its benchmark simulates no LMDB latency. Only the marked generated `build/` directory under the requested output may be replaced.

`src/` contains regression tests and the direct kernel benchmark. `ParseJava.java` is a syntax-only compilation-unit checker. The full-repository JUnit integration test is wired into `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/GeneratedValueKeysIntegrationTest.java`; there is no second integration-test source under this tools directory. Do NOT install the stub classes into the application or run them alongside real RDF4J model classes.

## Full integration

The normal LMDB module test discovery includes `GeneratedValueKeysIntegrationTest.java`. Run that test with the project's regular Maven test workflow. It compares generic/store-first/native/interpreted/compiled results and includes activation, dictionary-forbidden, budget-failure and actual-model checks. The offline runner continues to exclude this test because it uses real RDF4J dependencies rather than doubles.
