# Historical source experiments

All numbered patches are independent production diffs against `lmdb-packed-performance-source.zip`.
Apply one to a fresh copy of that baseline, not to another experiment or the final release. The common
current `run.py --source /path/to/experiment --out /tmp/experiment --compile-only` prepares a timing
build. The final regression suite intentionally checks some bugs that old experiments/baseline do
not fix, so older variants are not claimed to pass every final assertion.

1. `01-packed-index.patch`: one word replaces occupancy, fingerprint, full hash and head;
   hash is in the upper half. Reduces bytes and build overhead, but mixed probes regressed.
2. `02-low-hash.patch`: put hash in the low half so mismatch comparison needs no shift;
   hit-only cases improve, but mixed probes are still not robust.
3. `03-fused-batch.patch`: stage packed entries in the old hash scratch and fuse hashing loops.
   Batch preparation improves, but fused multi-column hash-only cases regress.
4. `04-single-key.patch`: single-key prepared probes avoid generic tuple indexing.
5. `05-long-hash-loop.patch`: restore separate narrowing after homogeneous long mixing.
6. `06-match-first.patch`: match-first prepared head loops; the empty encoded word still returns -1.
7. `07-exact-scalar.patch`: skip hash filtering for single keys and compare the actual key directly.
   Exact, but negative probes take much longer; rejected.
8. `08-byte-controls.patch`: keep compact one-byte occupancy/fingerprint metadata for scalar probes,
   while batch probes consume staged full-hash/head words. Faster, more robust misses than packed-only.
9. `09-original-hash.patch`: restore the original complete hashBatch method; retain packed staging,
   byte controls, exact comparison, duplicate-aware growth and zero-payload allocation savings.
   This was the first five-fork candidate; its full matrix is retained separately.
10. `10-scalar-control.patch`: direct scalar one-column control-byte probing hoists the input
    value and eliminates counted key/hash loops; the generic/composite path remains. This is
    the release candidate, with only data-byte-accounting documentation subsequently clarified.

The evidence archive has two-fork exploratory screens as well as a separate five-fork final matrix.
Early screens are diagnostics, not evidence of final performance or a reason to discard a slow final
fork. Native and IR hash functions are deliberately not changed, and no key's identity is a hash.

The C2 captures of the fused and original multi-column hash loops are scalar. The experiment
was motivated partly by a vectorization hypothesis, but these captures do not establish SIMD
vectorization or that loss of vectorization explains the regression. The fused loop instead
produced a larger normal C2 body despite fewer source passes.
