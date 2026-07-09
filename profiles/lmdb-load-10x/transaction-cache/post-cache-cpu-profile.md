# CPU profile after transaction value caching

Revision profiled: `8db4d1f05c GH-0000 Cache LMDB value IDs per transaction`.

The exact method `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` ran in-process under JDK 26,
G1, a 2 GiB heap, async-profiler 4.4 CPU sampling at 5 ms, and `automaticEvaluationStrategy=false`. These are
diagnostic profiles; forked paired JMH remains the timing authority.

For `NONE`, 5,989 samples put the LWJGL LMDB native library at 29.37%, platform and stub `memcmp` at 9.45%, sink
`flush` at 4.73%, radix sorting at 2.14%, `memmove` at 2.02%, and `ObjectLongHashMap` probing at 0.35%. The diagnostic
score was 759.210 ms/op.

For `READ_COMMITTED`, 6,594 samples put the native LMDB library at 23.57%, platform and stub `memcmp` at 9.16%,
condition waits at 11.71%, sink `flush` at 2.79%, `ValueStore.getId` at 1.06%, radix sorting at 1.27%, and
`ObjectLongHashMap` probing at 0.14%. The diagnostic score was 815.882 ms/op.

The profiles confirm that the new transaction dictionaries are not a material CPU cost and that further gains must
come primarily from reducing native B-tree operations or their comparison work.
