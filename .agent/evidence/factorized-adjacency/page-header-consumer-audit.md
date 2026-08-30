# Adjacency page-header consumer audit

This audit covers every production `openPageCursor` call and every current ANALYTICS lowering that can prove a
term-kind or literal-datatype predicate at page grain. Page traits are authoritative only for an exact immutable
base view: a null page cursor means overlays, tombstones, or another source shape must retain the exact generic/run
path.

| Consumer / shape | Column trait | Current use | Required action |
|---|---|---|---|
| Datatype histogram (ANALYTICS q5) | OSC root kind + root literal datatype | Bulk-counts uniform literal/datatype pages and resolves only a representative legacy literal | Complete; retain |
| Type-matrix predicate admission (q8) | SOC neighbor kind | Rejects predicate planes proved to contain no resource neighbor and collects exact page/root/fiber bounds for arbitration | Complete; retain |
| Type-matrix page morsels (q8 candidate) | SOC neighbor kind | Rejects uniform non-resource pages before source-type lookup and edge decoding | Complete; selected when the header-derived work interval strictly beats sideways morsels |
| Type-matrix sideways morsels (q8 candidate) | SOC neighbor kind through retained run/page coordinate | A physical-copy propagation prototype replaced scalar fiber classification but added equivalent page checks on predominantly one-fiber roots | Rejected after 47.401 and 46.792 ms/op measurements failed to beat the 47.119 ms/op control |
| All-predicate root-domain groups (ANALYTICS q9) | OSC root kind | Pushes recognized root-kind predicates into exact page cursors, rejects uniform non-IRI pages, and retains the row filter as oracle | Complete; retain |
| Root/fiber primitive batches | Derived batch traits | Reclassifies decoded IDs inside each batch | Keep scalar classification: propagating page traits into predominantly one-fiber q8 copies did not produce a credible end-to-end win |

q8 now costs both complete physical candidates. The ANALYTICS corpus selects sideways type morsels and measures
43.125 +/- 0.727 ms/op, while the literal-heavy regression selects page morsels and proves that its header rejection
executes. Telemetry reports the winner, both work intervals, eligible/rejectable pages, root rows, and fibers. This is
dataset-sensitive without retaining a synopsis or changing the persisted CSF layout.

q9's OSC root-kind prefilter improves matched no-synopsis execution from 36.521 +/- 0.325 to
27.824 +/- 0.563 ms/op while preserving the scalar row filter as the semantic oracle.

The other ANALYTICS shapes do not have a sound page-trait reduction:

- q0/q3 count or group `rdf:type` objects, which RDF permits to be any RDF term.
- q6/q7 count all outgoing statements, including literal fibers.
- q10 intersects root domains without a term-kind predicate.
- q11 uses plane counts and root counts; payload classification is irrelevant.
- q12 classifies the predicate once per plane; predicates are already IRIs by the statement model.

Literal-datatype uniformity has no additional current ANALYTICS consumer beyond q5. Applying it to numeric or
lexical expressions would still require a datatype-specific algebra proof and, for non-core legacy references, a
representative value-store lookup. That is future work rather than an unconditional scan shortcut.
