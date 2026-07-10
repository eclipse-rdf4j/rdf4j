# Packed local value dictionary checkpoint

The retained implementation writes fresh, default-evaluator loads as two ordinary LMDB block streams: a local RDF value dictionary and 512-statement quad-ID blocks. Both use `MDB_RESERVE`; neither uses `MDB_APPEND` or `MDB_APPENDDUP`.

The authoritative paired run is `forked-paired-after-namespace-fix.json`: JDK 26, G1, `-Xms2G -Xmx2G`, five one-second warmups, five one-second measurements, one fork, and `automaticEvaluationStrategy=false`.

| Isolation | Baseline ms/op | Checkpoint ms/op | Speedup | Allocated B/op |
| --- | ---: | ---: | ---: | ---: |
| NONE | 780.972 | 98.847 | 7.90x | 107,660,034 |
| READ_COMMITTED | 831.080 | 152.981 | 5.43x | 132,219,959 |

The pre-fix macOS async-profiler CPU recording is `async-none-cpu-jfr/java-command-cpu-45748.jfr`. Its stack-bearing samples identify an accidental conversion back to legacy indexes:

```text
AbstractRepositoryConnection.add
  SailRepositoryConnection.setNamespace
    LmdbSailSink.setNamespace
      LmdbSailSink.startTransaction
        TripleStore.materializePackedIfNeeded
```

An RDF `Model` can carry multiple namespaces. Under `NONE`, the first namespace sink committed the packed transaction and the second namespace sink started another store transaction, which was incorrectly treated as a data mutation. Namespace-only transactions now preserve the packed representation; a later statement mutation still materializes it before writing.
