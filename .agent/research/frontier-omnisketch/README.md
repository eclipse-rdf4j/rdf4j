# Frontier OmniSketch research provenance

This directory records the research inputs and claim boundaries used to design
the RDF4J Frontier OmniSketch estimator. It is a provenance record, not a copy
of the supplied research package or literature.

## Implementation provenance

The RDF4J production implementation is designed independently in Java against:

1. the mathematical contracts summarized below;
2. RDF4J's existing estimator, packed-planner, and LMDB abstractions;
3. normative SPARQL semantics; and
4. tests authored in this repository.

No Python implementation or unlicensed C++ artifact is ported, translated, or used as a source template. A later
user-supplied `OmniSketchCpp-main-2` snapshot contains an MIT license, copyright 2025 David Justen. Its behavior was
analyzed and short attributed excerpts are reproduced in
[the implementation design](../../../docs/query-optimizer/frontier-omnisketch-v2-technical-design.md). The Java
storage and estimator architecture remains an RDF-specific, primitive, disk-resident adaptation rather than a
mechanical port. Numerical examples and finite-model identities may be independently re-expressed as test inputs
because they state mathematical facts.

The earlier supplied archive contains a formal manuscript, a claim ledger, executable
reference checks, and a candidate C++ patch. It contains no license file or
other explicit redistribution grant. The two supplied paper PDFs are also
external literature rather than repository source. Consequently:

- the archive, its source files, its PDF, and the supplied literature PDFs are
  not checked into this provenance directory;
- this repository records only bibliographic metadata, public landing-page
  URLs, intake locations, and cryptographic checksums;
- user access to licensed literature is not treated as permission to
  redistribute that literature in RDF4J; and
- any later code reuse requires a source-specific license, authorship, notice, and compatibility review. The
  separately recorded MIT C++ snapshot has passed that review only for the short attributed design excerpts.

See [SOURCE_LEDGER.md](SOURCE_LEDGER.md) for the source-by-source record and
[CHECKSUMS.md](CHECKSUMS.md) for exact artifact identities.

## Mathematical contract adopted for design

For each retained frontier and SPARQL bound-mask stratum, the estimator models
an unnormalized finite random measure

```text
exact-heavy mass + sum_i(weight_i * point-mass(binding_i)).
```

The primary correctness obligation is measure unbiasedness for every supported
future continuation, not merely unbiased scalar cardinality. In particular,
the design relies on the following research conclusions:

- exact-heavy mass is disjoint from the sampled residual;
- a sampled bridge transfer uses a full-support proposal and the corresponding
  importance or Horvitz--Thompson adjustment;
- bounded resampling must be conditionally unbiased, while a residual whose
  support fits is retained without adding resampling variance;
- inherited, bridge-mutation, and resampling uncertainty remain distinct;
- repeated bridge composition is justified only while each transfer satisfies
  its support and weighting assumptions;
- multiplying correlated random messages is not automatically unbiased, so a
  join must use an exact probe, independent lanes, or a proved covariance-aware
  construction;
- bound-variable masks and ordered tuple frontiers are semantic state, not
  optional metadata;
- only a database proof may classify a result as an exact zero; and
- fixed-capacity particles cannot guarantee uniformly useful relative accuracy
  over unrestricted many-to-many bridge chains.

These are design obligations. Their proof in a research artifact does not prove
that a particular Java implementation satisfies them. RDF4J conformance must
be established by repository tests, invariants, and empirical evaluation.

## Claim boundary

### Mathematical, under stated assumptions

The supplied manuscript classifies as proved:

- pointwise/measure unbiasedness for supported linear frontier transforms;
- importance-weighted with-replacement bridge mutation and
  Horvitz--Thompson mutation under the correct sampling design;
- conditionally unbiased fixed-capacity resampling;
- repeated-bridge unbiasedness by induction;
- the inherited/mutation/resampling variance decomposition;
- the relationship between path relative variance, particle count, and
  proposal mismatch;
- impossibility of uniformly reliable fixed-budget multiplicative accuracy for
  arbitrary future continuations;
- covariance bias in same-lane random-message products;
- exact per-outer bag kernels for `EXISTS`, `NOT EXISTS`, `MINUS`, and
  `OPTIONAL`, including the nonempty-domain-overlap rule for `MINUS`; and
- the need for retained tuple frontiers when future continuations depend on
  multiple variables.

The proof package is an unreviewed research input. These claims are accepted as
the design basis only within its finite-snapshot model and explicit
assumptions; they have not undergone independent peer review in this project.

### Conditional

The following properties hold only when their implementation preconditions are
checked:

- deterministic memory bounds require explicit caps on particles, emissions,
  heavy entries, coalescing, and output;
- a certified interval requires the stated boundedness, independence, or
  variance-envelope assumptions;
- exact existence and absence require database probes or other exact evidence;
- repeated composition requires support-correct proposals at every bridge;
- exact RDF equality requires snapshot-scoped term IDs, not hash equality;
- adaptive optimization requires a valid design/audit separation; and
- a persistent synopsis is valid only for the snapshot and hash schema named by
  its manifest.

Failure of a precondition must produce an explicit degraded or unresolved
state and use the existing scalar estimator as the safe fallback.

### Empirical

The mathematics does not determine:

- workload q-error or confidence calibration;
- optimizer plan ranking, runtime regret, or catastrophic-plan frequency;
- LMDB probe and refinement latency;
- synopsis build, merge, update, or ingest cost;
- memory and disk overhead at one or ten billion triples;
- the best allocation among particles, heavy tables, characteristic strata,
  adjacency samples, and deterministic bounds; or
- the fraction of real workloads whose useful frontier width fits the
  implementation.

Those properties require measurements against the implemented storage engine,
hardware, data distribution, and query workload. No disk size or production
accuracy target is claimed as a theorem.

## Maintenance rules

- Add a source to `SOURCE_LEDGER.md` before it influences implementation.
- Record the exact bytes used for local inputs in `CHECKSUMS.md`.
- Prefer public paper landing pages over bundled PDFs.
- Label derived implementation requirements as design decisions, not quotations
  or upstream code.
- Keep proof assumptions, Java invariants, and measured system behavior
  separately identifiable in code reviews and benchmark reports.
