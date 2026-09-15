# Compact generated terminal keys — latest revision

Read **OPTIMIZATION_REPORT.md** first for the current implementation, measurements, admission limits and integration status. The older REPORT.md below describes the preceding implementation; its per-value maps and benchmark numbers are no longer current.

```bash
export JAVA_HOME=/path/to/jdk-26
python3 tools/generated-keys/run.py --out /tmp/generated-keys-tests
python3 tools/generated-keys/compare.py --baseline /path/to/extracted/generated-value-keys-source \
  --out /tmp/generated-keys-comparison --forks 5 --bench --memory --c2
```

The new runtime-value budget is `rdf4j.lmdb.runtimeValues.maxBytes`. It is an admission estimate, not a full heap/RSS limit. Runtime payload spilling is not implemented. Both complete-source and production-only patches apply to the preceding generated-value-keys-source.zip; choose one patch. The cumulative patch targets the original judges' tree.

---

# Generated terminal keys

This directory contains offline tests, a direct key-path microbenchmark, and separate full-repository integration tests for dictionary-free generated DISTINCT/GROUP BY keys. Production code is under `java/` in the source root.

Read `REPORT.md` for supported plan shapes, exact identity rules, memory costs and validation limits. Automatic admission is default-on; `-Drdf4j.lmdb.generatedKeys.enabled=false` retains store-first keying. This is independent of value-overlay settings.

## Run the offline tests

```bash
export JAVA_HOME=/path/to/jdk-26
python3 tools/generated-keys/run.py --out /tmp/generated-keys --bench --forks 5
```

The runner has no downloaded dependencies. It uses explicit test doubles under `stubs/` and compiles selected complete production files and exact production class/method bodies. The generated build manifest identifies every source origin. It is not a full RDF4J build and its benchmark simulates no LMDB latency. Only the marked generated `build/` directory under the requested output may be replaced.

`src/` contains regression tests and the direct kernel benchmark. `ParseJava.java` is a syntax-only compilation-unit checker. `integration/` contains a JUnit test to copy into the full repository's LMDB module test tree; it is excluded from the offline runner. Do NOT install the stub classes into the application or run them alongside real RDF4J model classes.

## Full integration

Copy `integration/org/eclipse/rdf4j/sail/lmdb/evaluation/GeneratedValueKeysIntegrationTest.java` into the same package under `core/sail/lmdb/src/test/java/` and run the normal project test build. That suite compares generic/store-first/native/interpreted/compiled results and includes an activation assertion and dictionary-forbidden test using actual RDF4J Values. The delivered environment could syntax-parse but could not compile or execute it.
