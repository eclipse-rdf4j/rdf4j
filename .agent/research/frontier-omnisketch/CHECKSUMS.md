# Frontier OmniSketch input checksums

All digests use SHA-256. Paths record where the user-supplied inputs were found
at intake on 2026-07-25. The files are not copied into the repository and are
not required to build or run RDF4J.

## Supplied files

```text
bbdcd88f1797fdb0a6495d5a6e2f7b72448e41ee8fc4b7eed99865d8e408a9ca  /Users/havardottestad/Downloads/frontier-omnisketch-mathematical-resolution-2026-07-24.zip
b3836ed8cb876f120ebb180019226923928d3cebed7fd96e47b1dab65180fffc  /Users/havardottestad/Downloads/2309.06051v1.pdf
c7a2508b6ee1f5281dbbb66df9514791859420bb309c3dcd28befd843ef652a4  /Users/havardottestad/Downloads/2508.17931v1.pdf
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
