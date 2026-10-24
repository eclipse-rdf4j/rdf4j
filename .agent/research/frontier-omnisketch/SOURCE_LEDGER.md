# Frontier OmniSketch source ledger

This ledger identifies the sources that informed the Frontier OmniSketch
design. Checksums for user-supplied bytes are in [CHECKSUMS.md](CHECKSUMS.md).
None of the local artifacts listed here are build or runtime dependencies.

## Primary research inputs

| ID | Source | Role in the design | Repository treatment |
| --- | --- | --- | --- |
| `FOM-MATH-2026-07-24` | User-supplied `frontier-omnisketch-mathematical-resolution-2026-07-24.zip`, received at `/Users/havardottestad/Downloads/frontier-omnisketch-mathematical-resolution-2026-07-24.zip` | Formal positive composability result, impossibility boundary, variance decomposition, SPARQL kernel analysis, proof-status ledger, and executable finite-model checks | Metadata and checksums only. The archive has no explicit license file. Its prose, PDF, Python, shell, JSON, log, and candidate patch are not copied or redistributed. The Java implementation is independent. |
| `OMNISKETCH-2023` | Wieger R. Punter, Odysseas Papapetrou, and Minos Garofalakis, “OmniSketch: Efficient Multi-Dimensional High-Velocity Stream Analytics with Arbitrary Predicates,” arXiv:2309.06051v1 (2023), [public record](https://arxiv.org/abs/2309.06051) | Original coordinated multi-attribute sketch and sampling context | Cite the public record. The supplied PDF at `/Users/havardottestad/Downloads/2309.06051v1.pdf` is not redistributed. |
| `OMNISKETCH-JOIN-2025` | David Justen and Matthias Boehm, “Join Cardinality Estimation with OmniSketches,” arXiv:2508.17931v1 (2025), [public record](https://arxiv.org/abs/2508.17931) | OmniSketch interoperability and join-extension baseline; empirical witness-loss motivation | Cite the public record. The supplied PDFs at `/Users/havardottestad/Downloads/2508.17931v1.pdf` and `/Users/havardottestad/Downloads/2508.17931v1-2.pdf` are not redistributed. |
| `OMNISKETCH-CPP-MIT-2025` | David Justen, `OmniSketchCpp`, user-supplied snapshot received at `/Users/havardottestad/Downloads/OmniSketchCpp-main-2`, [public repository](https://github.com/d-justen/OmniSketchCpp) | Executable interpretation of the join paper: cell insertion/probing, K-minwise set/vector representations, PK-sample combination, secondary sketches, and alpha-acyclic query-graph traversal | The supplied snapshot contains an MIT license, copyright 2025 David Justen. Short attributed excerpts are reproduced in the technical design. RDF4J uses an independently designed primitive, disk-resident Java adaptation rather than a mechanical port. |
| `SPARQL-QUERY-1.1` | W3C, “SPARQL 1.1 Query Language,” especially [Negation](https://www.w3.org/TR/sparql11-query/#negation), [OPTIONAL](https://www.w3.org/TR/sparql11-query/#optionals), and [EXISTS](https://www.w3.org/TR/sparql11-query/#sparqlAlgebra) | Normative bag semantics, compatibility, bound domains, `MINUS` versus `NOT EXISTS`, and optional matching | Normative public specification. RDF4J behavior is tested against the specification and existing RDF4J semantics. |

## Supporting mathematical literature

| ID | Source | Design use | Boundary |
| --- | --- | --- | --- |
| `PRIORITY-SAMPLING-2005` | Nick Duffield, Carsten Lund, and Mikkel Thorup, “Sampling to estimate arbitrary subset sums,” arXiv:cs/0509026, [public record](https://arxiv.org/abs/cs/0509026) | Priority-style weighted sampling and adjusted subset-sum estimators | Does not by itself prove the correctness of RDF bridge propagation or RDF4J's implementation. The exact inclusion design must be named and tested. |
| `DSB-2022` | Kyle Deeds, Dan Suciu, Magda Balazinska, and Walter Cai, “Degree Sequence Bound For Join Cardinality Estimation,” arXiv:2201.04166, [public record](https://arxiv.org/abs/2201.04166) | Deterministic degree-sequence upper bounds as underestimate guardrails | Bounds are separate evidence from the random point estimator and require their own synopsis invariants. |
| `ADAPTIVE-SKETCHES-2025` | Edith Cohen, Mihir Singhal, and Uri Stemmer, “Breaking the Quadratic Barrier: Robust Cardinality Sketches for Adaptive Queries,” ICML 2025, [PMLR record](https://proceedings.mlr.press/v267/cohen25c.html) | Establishes that repeated adaptive exposure is a separate robustness problem | Ordinary independent-lane confidence intervals are not claimed to provide reusable adaptive-query guarantees. Initial RDF4J use is limited to design lanes plus held-out audit lanes. |

## Source-to-implementation mapping

The sources establish or motivate requirements; they do not supply production RDF4J code.
The implementation mapping is:

| Research requirement | RDF4J implementation obligation |
| --- | --- |
| Frontier measure rather than scalar-only cardinality | Query-local immutable evidence state beside the existing scalar `BagEstimate` value |
| Exact RDF equality | Ordered bindings contain snapshot-scoped LMDB term IDs; hashes select samples only |
| Bound-mask semantics | State partitions particles and exact mass by bound-variable mask |
| Exact-heavy plus residual | Disjoint storage and accounting, with inherited uncertainty retained after deterministic propagation |
| Support-correct bridge transfer | Validate full support, multiplicity, draw count, and adjustment before claiming measure-unbiased evidence |
| Conditionally unbiased bounded resampling | Reference multinomial path first; bypass resampling when support fits |
| Correlated products are unsafe | Prefer exact RHS probes; otherwise require independent lanes or return unresolved |
| Fixed-budget impossibility | Monitor ESS, maximum weight, variance, bounds, and refinement budget; degrade explicitly on degeneration |
| Canonical logical state | Stateless versioned seeds and one state key per logical factor subset/frontier/mask/snapshot/lane role |
| SPARQL difference and optional semantics | Per-outer exact probes, domain-overlap checks for `MINUS`, and separate matched/unmatched mask strata |
| Empirical performance only | Benchmark q-error, coverage, regret, latency, memory, build cost, and disk tiers before making production claims |

## Non-sources

The following are deliberately not treated as implementation sources:

- the Python reference model and tests inside `FOM-MATH-2026-07-24`;
- the candidate C++ witness-exhaustion patch inside that archive;
- the older supplied OmniSketch C++ tree without a recorded license, or any generated research bundle;
- local copies of papers whose public bibliographic records are sufficient; and
- benchmark targets stated in a research plan before RDF4J measurements exist.

The separately recorded `OMNISKETCH-CPP-MIT-2025` snapshot is not a non-source: its license and exact bytes have
been reviewed for the short attributed excerpts in the design. Any substantial source reuse still requires a
source-specific compatibility and notice review.
