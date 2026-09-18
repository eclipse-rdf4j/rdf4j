# RDF4J evaluator review

Status: **PENDING — no reviewer has signed this record.**

This review is an acceptance gate for the soundness claim in `../CERTIFICATION.md`. The reviewer must be independent
of the implementation work and experienced with RDF4J query-algebra evaluation.

Candidate source-bundle identifier:
`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.

## Review checklist

- [ ] The runtime semantic model matches optimizer-free RDF4J evaluation at the source identifier in the dossier.
- [ ] Incoming bindings, dataset selection, errors, evaluation order, side effects, bags, sets, sequences, and ASK are
      represented conservatively.
- [ ] Every accepted runtime rule's preconditions are sufficient for RDF4J behavior.
- [ ] The preserved `runtime-set / reduced-hidden-by-set` regression is fixed at the semantic rule, not special-cased.
- [ ] Runtime counterexamples are target-tagged, tied to the recorded RDF4J revision, optimizer-free, and replayed.
- [ ] Unsupported native or extension algebra fails closed with `UNKNOWN`.
- [ ] The bounded universe, corpus inventory, release campaigns, and mutation report support the recorded claim.

## Reviewer decision

- Reviewer name:
- Affiliation or RDF4J role:
- Reviewed source identifier: `bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`
- Review date (UTC):
- Decision: `APPROVE` / `REQUEST CHANGES`
- Notes or issue links:
- Signature (name or verifiable review URL):
