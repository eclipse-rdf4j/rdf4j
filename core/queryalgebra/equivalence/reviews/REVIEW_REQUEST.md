# Independent review requests

These requests apply only to candidate
`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.
They are ready to copy into a pull-request review request, issue comment, or direct message to a qualified reviewer.
The implementation author must not complete either sign-off record on a reviewer's behalf.
The published review location is `https://github.com/eclipse-rdf4j/rdf4j/pull/5948`, which contains candidate transport
commit `3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6`. Later dossier or sign-off-record commits do not change the authoritative
base-plus-digest semantic identity.

## RDF4J evaluator reviewer

> Please independently review the GH-5905 algebra equivalence checker candidate identified as
> `bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.
> Start with `core/queryalgebra/equivalence/reviews/REVIEW_PACKET.md`, verify
> `SOURCE_MANIFEST.sha256`, and audit the runtime semantics, accepted-rule preconditions, target-correct witness path,
> fail-closed boundary, and retained machine evidence. Please record `APPROVE` or `REQUEST CHANGES`, your identity and
> RDF4J expertise, the exact candidate identifier, UTC date, findings, and a verifiable review URL in
> `RDF4J_EVALUATOR_SIGNOFF.md` (or return an equivalent signed review that can be transcribed without changing its
> meaning). This review must be independent of the implementation work.

## Formal-methods reviewer

> Please independently review the GH-5905 algebra equivalence checker candidate identified as
> `bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.
> Start with `core/queryalgebra/equivalence/reviews/REVIEW_PACKET.md`, verify
> `SOURCE_MANIFEST.sha256`, and audit the Lean semantic transcription, rule/profile theorem matrix, composite
> soundness theorem, executable certificate decoder/replayer, axiom inventory, and documented trusted-base limit.
> Please record `APPROVE` or `REQUEST CHANGES`, your identity and relevant formal-methods expertise, the exact candidate
> identifier, UTC date, findings, and a verifiable review URL in `FORMAL_METHODS_SIGNOFF.md` (or return an equivalent
> signed review that can be transcribed without changing its meaning). This review must be independent of the
> implementation work.

## Acceptance of returned reviews

A returned review satisfies its gate only when all of these conditions hold:

- the reviewer is a named human independent of the implementation;
- the review states relevant RDF4J evaluator or formal-methods expertise;
- the decision is unambiguous and applies to the exact candidate identifier above;
- the record is dated and has a verifiable source, such as a pull-request review or signed issue comment;
- every requested change is resolved, followed by a new candidate digest and both reviews if semantic source changed.

An issue reference, automated result, acknowledgement, or approval of a different digest is not a sign-off.

## Routing notes

Repository history provides a practical route for the evaluator review: excluding the implementation author, Jeen
Broekstra and Jerven Bolleman have respectively 195 and 116 authored commits across `core/queryalgebra/evaluation` and
`core/queryalgebra/model`. Ken Wenzel opened GH-5905 and can also route the request. These facts identify potential
contacts only; they are not endorsements, proof of independence, or review decisions.

No repository-history result establishes the required Lean/formal-methods competence. That request must be routed to
a named human who can independently audit Lean 4 proofs and the semantic transcription, with the expertise stated in
the returned record.
