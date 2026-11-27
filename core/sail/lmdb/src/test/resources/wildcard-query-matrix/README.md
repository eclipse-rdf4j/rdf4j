# DCAT variable-predicate workload

These SELECT queries use the Belgian data.gov.be DCAT snapshot loaded in the
`dcat-lmdb` repository. The original distribution query is
`01-chain-catalog.rq`. Its predicate variable is intentionally omitted from the
projection, so comparisons preserve solution multiplicity.

Each row in [manifest.tsv](manifest.tsv) names a query file without its `.rq`
extension. Predicate partitions describe syntactic variable occurrences. For
example, `2+1` means that one variable occurs twice and a second variable
occurs once. A query can constrain the two variables to differ. A zigzag chain
includes an edge in the reverse direction along the chain. Repeated-predicate
chains use that shape because the catalog has shared values and no observed
forward chain with the same predicate on distinct adjacent nodes.

Selective cases use a fixed catalog record, two known records, or a fixed file
format. Broad cases reach larger parts of the catalog. Shapes that could
generate large result sets use `LIMIT 10`. The original full query has 35,380
rows on the snapshot. The three-variable directed chain with a fixed CSV
format has 2,774 rows.

## Run against an existing server

Run from the repository root. The runner uses Python 3 and curl 8.4 or later.
It submits queries one at a time, requests a server timeout for each query,
and bounds the response size and client runtime.

```sh
python3 scripts/lmdb-wildcard-live-matrix.py \
  --query '03-chain-*' --telemetry auto nestedLoop wildcardPredicateBatch irKernel
```

Omit `--query` to run the whole matrix. Use `--server` and `--repository` if
the endpoint differs from `http://localhost:8080` and `dcat-lmdb`. The runner
emits JSON Lines on stdout. Each case gets one warmup and three measured
requests per strategy by default, rotating strategy order between rounds. The
configuration record includes a SHA-256 digest for each selected query.

The row requests use the SPARQL endpoint with TSV results. `firstByteMs`
measures the first HTTP response byte and `totalMs` includes transport and
serialization. Separate Workbench telemetry requests show the selected route
and available first- and last-row timings. A requested strategy can be
ineligible or can fall back. Use the route in telemetry when interpreting
timings.

The `comparison` column specifies the result check. `bag` compares unordered
TSV result hashes, preserving duplicate rows and preferring a successful
measured `nestedLoop` result as the reference. A case name indicates that each
limited result must fit inside that full result bag when both queries run.
For `slice`, compare row counts. Unordered LIMIT results can choose different
valid rows.

To repeat a capture against a different build, supply its server URL and
use `--reference` to load the earlier JSON Lines capture. The runner checks row
counts for every common query and full bags where the manifest specifies
`bag`. Run both halves of each full/limited pair in the same capture to check
slice membership.

```sh
python3 scripts/lmdb-wildcard-live-matrix.py \
  --server http://localhost:18080 --reference baseline.jsonl \
  --query '03-chain-independent-format-csv*' --query '03-chain-*-shared-*' \
  --query '03-chain-shared-*' --query 03-mixed-two-shared-limit10 \
  --telemetry auto nestedLoop wildcardPredicateBatch irKernel > after.jsonl
```
