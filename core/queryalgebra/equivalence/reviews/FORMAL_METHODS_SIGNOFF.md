# Formal-methods review

Status: **PENDING — no reviewer has signed this record.**

This review is an acceptance gate for the soundness claim in `../CERTIFICATION.md`. The reviewer must be independent
of the implementation work and competent to audit Lean proofs and executable certificate kernels.

Candidate source-bundle identifier:
`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.

## Review checklist

- [ ] The Lean model faithfully transcribes the documented RDF4J, SPARQL 1.1, and dated SPARQL 1.2 semantic fragment.
- [ ] Every theorem-backed rule/profile cell has the required target, observation, context, purity, binding, scope,
      ordering, and collapsed-error premises.
- [ ] Every inapplicable matrix cell corresponds to a Java proof step the accepting registries cannot emit or accept.
- [ ] `accepted_executable_certificate_sound` establishes the positive-verdict claim for accepted version-2 inputs.
- [ ] The executable decoder/replayer checks exact source trees and rejects malformed, extra, and non-root-local steps.
- [ ] `lake build` contains no `sorry`, `admit`, project-defined axiom, or undeclared theorem axiom.
- [ ] The Java/Lean bounded and campaign correspondence evidence is sufficient for the documented trusted-base limit.

## Reviewer decision

- Reviewer name:
- Affiliation or relevant expertise:
- Reviewed source identifier: `bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`
- Review date (UTC):
- Decision: `APPROVE` / `REQUEST CHANGES`
- Notes or issue links:
- Signature (name or verifiable review URL):
