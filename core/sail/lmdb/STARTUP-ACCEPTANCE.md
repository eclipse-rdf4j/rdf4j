# Reproduce bounded LMDB startup

Close the repository before running the acceptance command. The runner opens an existing repository and measures from `new LmdbStore(...)` through `SailRepository.init()`. It does not generate or import statements. The user supplies the billion-statement repository; local runs on smaller stores do not establish the billion-statement timing target.

From the repository root:

```bash
scripts/lmdb-startup-acceptance.sh --store /path/to/closed/repository \
  --copy-to /path/to/new/retained-copy --max-ready-ms 10000
```

The copy destination must not exist. Copies and result files are retained. Omitting `--copy-to` opens the supplied directory directly. Normal startup writes its ordinary metadata, and background index maintenance can change derived indexes. The repository's directory lock prevents opening a live repository concurrently; close it before copying as well so both native environments belong to one coherent image.

Use `--config /path/to/config.ttl` to apply the repository's normal Sail settings, including its existing durability and optimizer settings. The Turtle file must contain exactly one `rdf4j:LmdbStore` Sail node. Without this option, the runner uses standard defaults and the persisted literal/index settings. Explicit `--triple-indexes` and `--triple-term-indexes` override the corresponding configuration values.

The runner uses the repository's `mvnf` workflow and the workspace-local `.m2_repo`. Its JSON result records the actual explicit/inferred statement cardinalities from bounded B-tree statistics, constructor-to-ready milliseconds, logical native reads by category and their total, native tree-drop calls, exact-count trust, pending background work, and first-query milliseconds separately. Cardinality inspection runs after both measured intervals. Retained durable value candidates, pending count verification and optional adjacency/overlay work remain visible even after the auxiliary initializer finishes. It always asserts at most 1,000 foreground logical native reads and no foreground native tree drops. `--max-ready-ms` adds an optional wall-clock assertion; omit it when collecting diagnostic evidence on slower hardware.

The read counter calls the real LWJGL methods through the existing Mockito test dependency. It counts each outer Java/native operation once, including wrapper overload delegation. The `txn_next_page` category separately includes native transaction-header reads performed by `LmdbUtil` without a JNI call; final totals combine both sources. The code inventory test rejects production `nmdb_*` bypasses. Background jobs begin only after the owner's readiness gate; construction must not delegate dataset-sized work to a helper and wait for it. Separate regressions guard queue draining, cache-byte reuse, eager namespace decoding and optimizer sidecar loading because one native operation or a file stream can otherwise hide unbounded work. The timing includes the counter's observation overhead; Maven installation and instrumentation setup happen before the measured interval.

To request a different layout while measuring readiness on the existing committed indexes:

```bash
scripts/lmdb-startup-acceptance.sh --store /path/to/closed/repository \
  --copy-to /path/to/new/retained-copy \
  --triple-indexes spoc,posc,ospc --triple-term-indexes spoc,cspo,posc
```

Index completion is reported separately from readiness. Normal reads and writes use committed query generations while incomplete staging indexes receive transactional maintenance. A failed or interrupted background build must leave those committed indexes usable. Missing or mismatched reference-count authority permits conservative reads and writes while reclamation and identifier reuse remain suspended; explicit offline reconstruction restores count authority after checking the complete owner graph. The proof's checksum validates its fixed metadata record, not every payload byte against arbitrary storage corruption. Existing durability settings are preserved; this work adds no power-loss guarantee.

After readiness, trusted value maintenance attempts one bounded retirement pass and one bounded persisted-unused pass. Later publications may perform bounded maintenance as well. These passes preserve current readers and retain candidates whose liveness cannot be established with bounded index probes. Eligible candidates beyond the pass also remain durable. `backgroundWorkPending` and retained-maintenance status describe that remaining work; waiting for index or auxiliary jobs does not certify that value queues are empty. The verification and reconstruction operations below do not promise to exhaust retained queues; this change does not add a public full queue-drain operation.

Both explicit operations require a closed repository and acquire its directory lock. Pass the same configuration used to open that repository:

```java
File directory = new File("/path/to/closed/repository");
LmdbStoreConfig config = existingConfiguration;

// Read-only exhaustive dictionary, index, retirement-queue and schema verification.
LmdbStore.verifyStoreOffline(directory, config);

// Rebuild component counts from the complete active owner graph, then publish exact-count proof.
LmdbStore.reconstructReferenceCountsOffline(directory, config);
```

Verification checks both supported formats with their actual codecs and does not compare every stored count with a reconstructed count table. Reconstruction validates owner relationships, including nested terms, and writes counts and the final proof. Interrupted or failed reconstruction leaves count authority UNKNOWN. Neither operation opens public connections or runs background builders while holding the directory.

On an initialized store, inspect status or wait for requested statement and triple-term index layouts:

```java
LmdbStore.StartupStatus status = store.getStartupStatus();
boolean exactCounts = status.exactReferenceCounts();
boolean retainedWork = status.backgroundWorkPending();
boolean requestedIndexesFinished = store.awaitIndexRebuild(30, TimeUnit.SECONDS);
```

These diagnostics do not schedule a build or restore count trust. The wait covers requested index layouts; optional accelerators have their existing separate waits. A successful index wait does not certify that retained value maintenance is complete.
