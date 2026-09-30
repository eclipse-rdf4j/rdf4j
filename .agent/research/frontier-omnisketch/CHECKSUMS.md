# Frontier OmniSketch input checksums

All digests use SHA-256. Paths record where the user-supplied inputs were found
at intake on 2026-07-25 and 2026-08-14. The files are not copied into the repository and are
not required to build or run RDF4J.

## Supplied files

```text
bbdcd88f1797fdb0a6495d5a6e2f7b72448e41ee8fc4b7eed99865d8e408a9ca  /Users/havardottestad/Downloads/frontier-omnisketch-mathematical-resolution-2026-07-24.zip
b3836ed8cb876f120ebb180019226923928d3cebed7fd96e47b1dab65180fffc  /Users/havardottestad/Downloads/2309.06051v1.pdf
c7a2508b6ee1f5281dbbb66df9514791859420bb309c3dcd28befd843ef652a4  /Users/havardottestad/Downloads/2508.17931v1.pdf
c7a2508b6ee1f5281dbbb66df9514791859420bb309c3dcd28befd843ef652a4  /Users/havardottestad/Downloads/2508.17931v1-2.pdf
```

The two `2508.17931v1` paths contain identical bytes.

## Selected files from the MIT-licensed C++ snapshot

The tree was received at `/Users/havardottestad/Downloads/OmniSketchCpp-main-2` on 2026-08-14. It is not vendored.

```text
8745153f8260187012520a67770366327d02ce6c02a5b51f2313d7b586f86d01  LICENSE
e8d0274c14fe4db1b84e27185ee5a311ee115a56e46af61c0acec5737c38d3e3  src/omni_sketch/omni_sketch.cpp
a20b8c2bda2a9f4cc793db443a77bea92ab795ee2786866d7957f2169d561034  src/omni_sketch/omni_sketch_cell.cpp
3fae2f3e5bbcc3c60f527f7b2fbf5b3ce98bab7206de9b9c47aeae69c80395dc  src/include/omni_sketch/pre_joined_omni_sketch.hpp
359343a45d3355b65f11358a5ad35ee30e44319922be4c69e1b3edf90ebb9857  src/combinator.cpp
8a38da5a0e7753945a3dc3fa006491ded1a7b2faecf2c98128a889ffc47235cd  src/execution/query_graph.cpp
6ee2de4b913a9207f984f830d0f6385bd1a30eee464858ebbdfda7a51cfd29a4  src/include/registry.hpp
334855f2e0022c48ee97f40a87707fca6fc04981d1d66bc208b0f5c842e05f07  src/min_hash_sketch/min_hash_sketch_set.cpp
16382fdf187972c61f80e3dc084ac3459ecd61d6f9cf2ac2860da205916f15a3  src/include/util/hash.hpp
```

## Selected members of the proof archive

These hashes identify the uncompressed byte stream of each named archive
member. They are recorded for auditability only; the members are not vendored.

```text
42bcaeb976e0bb142c2908adbe623418d29811ad2feefcf81498bcca6ad81a84  README.md
ec95f435736df2417c3bc14a6a8d680b863458c195ec66a2d98ecf73537b54dd  THEORY.md
801696d7f9d4891af18beeedbf8d619e3b7d84edce216850c06032ea4404356a  PROOF_STATUS.md
f2607150fdc209eff8a02d91586e4115a02e6ea695dacf28ca4d0a125846b930  Frontier_OmniSketch_Mathematical_Resolution.pdf
384e62ed3528afae08236b46b76380cce0f0e010e45a303e9594799ed1ca75f5  frontier_reference.py
f192232b0009a085a21c1970e38782f4d6f503a1fb74f1274a2896e5f7ef7680  test_frontier_reference.py
28551020b4f2c1449777c668eed8bf0a136be994946be847b9471f436c09826d  exhaustive_checks.py
bdfb2c9c8d37e46ca014cd8b9ec66c2644ac581ce61c83a69ee04d3bd10d2bf0  run_experiments.py
dbe875e67e91a6369d22c10bc7db95de80c74d0e7f10d1866dcacd50767a5a85  verification.log
b2beabe2f6ed4802ba26dfb9430f1cc4239c260a5d1266918ae7e481741f09ed  omni_join_witness_exhaustion_candidate_fix.patch
0df9a6c0ea04207e6c10c25c38ee635007c0d9a8afed01c8efa93f46135a9738  SHA256SUMS
```

To recheck an outer file:

```bash
shasum -a 256 /absolute/path/to/file
```

To recheck an archive member without extracting it:

```bash
unzip -p /absolute/path/to/archive.zip MEMBER | shasum -a 256
```
