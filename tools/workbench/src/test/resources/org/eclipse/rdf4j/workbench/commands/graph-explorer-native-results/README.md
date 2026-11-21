# Generic result fixtures for native graph-explorer tests

These are reviewed **expected results**, produced with LMDB native evaluation disabled. Ordinary generated-query tests execute the real frontend query generators and both native backends, and compare native answers with these files. They do not run the generic reference query or a generic LIMIT membership witness. Native cold/repeated executions, telemetry explanations and physical-work assertions remain enabled. Two inexpensive differential controls (an empty query and a plain statement scan) still compare stored results with the generic engine; special independent semantic regression tests remain separate.

## Identity and RDF fidelity

Each versioned JSON file records the exact query SHA-256, canonical dataset SHA-256, shared-data fingerprint SHA-256, expected comparison mode and a sorted multiset of typed binding maps. Counts preserve duplicate rows; omitted bindings remain unbound. Typed terms preserve IRI spelling, **raw dataset-defined blank-node IDs**, literal lexical spelling/datatype/language/base direction, and recursive RDF 1.2 triple terms. These are not blank-node-isomorphism snapshots: changing a fixed blank ID changes the dataset and invalidates the fixture. Queries allocating fresh blank nodes, clock/random/UUID functions and undeclared or volatile extension functions cannot be persisted.

Dataset identity distinguishes the repository-default dataset from an explicit empty dataset and includes sorted default/named/remove graphs, insert graph and inferred inclusion. The shared input fingerprint uses sorted, lossless typed quads, including graph context. Missing files, stale identities, unsupported/corrupt formats and mismatched comparison modes fail; ordinary tests never regenerate files. Each file retains its original generic-engine source revision. A manifest distinguishes the current generation-run revision from these per-file origins and records the exact generated corpus. The corpus audit rejects missing and orphaned JSON files and conflicting case IDs.

For an unordered outer LIMIT without OFFSET, the file stores the **complete unsliced answer bag**, retaining DISTINCT/projection. Native answers must have `min(limit, complete-result-size)` rows and be a sub-bag with sufficient multiplicity. The query itself determines the required comparison mode; a file cannot change the cap. For other shapes the existing exact-bag contract is preserved. In particular, ordered and nested-LIMIT queries are not newly treated as arbitrary subsets, nor does this change add an ordered-sequence assertion. Existing small ordered/volatile semantic controls remain live. Pagination request inputs come from sorted persisted first/next-page results, making exclusions stable between generation and replay. Run replay in a fresh JVM after regeneration to catch unstable nested-limit selections.

## Explicit regeneration

Use Java 25+, Maven, the workspace-local `.m2_repo`, and the Workbench Node installation. The Java tests still invoke the real TypeScript generators with Node; `skipFrontend` skips rebuilding the frontend, not this dependency. Bootstrap normally once if `tools/workbench/target/node/node` is absent. Full Java-module coverage also needs the compiled classic scripts under `target/frontend/scripts`; if frontend building was skipped, run `target/node/node node_modules/typescript/bin/tsc -p tsconfig.classic.json` from `tools/workbench` first.

From repository root, install the current source before tests (never use `-am` with tests):

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick -DskipFrontend=true install > maven-build.log 2>&1

Then explicitly regenerate:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/workbench \
      -P-skipSlowTests -DskipFrontend=true \
      -Dtest=GraphExplorerNativeVariantsTest#regenerateGenericExpectedResults \
      -Drdf4j.workbench.nativeFixtures.regenerate=true \
      -Drdf4j.workbench.nativeFixtures.revision="$(git rev-parse HEAD)" test

Generation can be slow: it deliberately runs the expensive generic reference **once per distinct case**, including complete bags for outer unordered LIMIT. Valid existing files are reused so an interrupted run can resume; only this explicit command may replace missing/stale files. To review a change to the generic engine itself, add `-Drdf4j.workbench.nativeFixtures.force=true` to recompute every answer rather than reuse old answers. Reused files retain their original generator revision, including when a resumed run uses a newer checkout; their origin is never relabeled as the current run.

For development, `-Drdf4j.workbench.nativeFixtures.cases='range-29-default'` selects case IDs by full regular-expression match. Pagination prerequisite fixtures are also generated. A partial run does not write a complete manifest; finish the full command before committing.

Review the JSON and manifest diff: query/dataset/data identity changes must be intentional, and unexpected answer changes need investigation rather than automatic acceptance. There is no implicit “update golden files” mode in regular tests.

## Replay and verification

After the prerequisite install, run contract tests and the full native variants class in a fresh process:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/workbench \
      -P-skipSlowTests -DskipFrontend=true \
      -Dtest=NativeQueryExpectedResultsTest,GraphExplorerNativeFixturePolicyTest,GraphExplorerNativeVariantsTest test

Then run the remaining Workbench Java coverage as appropriate. Keep `-P-skipSlowTests` when testing this slow-tagged class explicitly. Performance validation should compare matching selections and preserve efficient-operator/work-count assertions; do not add fragile per-query wall-clock limits for shared CI runners.
