# LMDB native engine rollout ledger

Behavior-risk changes default on behind `rdf4j.lmdb.native.<feature>=false` kill switches and live for at
most two releases unless a measured reason justifies retention. Bit-identical changes do not require a
switch. Defaults and removal decisions are filled as implementations land.

| Feature | Property | Default | Measurement | Removal decision |
| --- | --- | --- | --- | --- |
| Proposal dispatch | Pending | Pending | Pending | Pending |
| Adaptive filter decorator | Pending | Pending | Pending | Pending |
| Elastic parallel admission | Pending | Pending | Pending | Pending |
| Vectorized external worker root | Existing property | Off, to re-evaluate | Pending | Pending |
| Shared batch columns | Pending | Pending | Pending | Pending |
| Parallel sort and merge | Pending | Pending | Pending | Pending |
| Radix native sort | Pending | Pending | Pending | Pending |
| Order-aware sort elimination | Pending | Pending | Pending | Pending |
| Range pushdown | Pending | Pending | Pending | Pending |
| Encoding v2 | Store-creation setting | New stores only | Pending | Permanent version gate |
| Leapfrog intersection | Pending | Pending | Pending | Pending |
| Outer accumulation | Pending | Pending | Pending | Pending |
| Frontier property paths | Pending | Pending | Pending | Pending |
| Persistent value hash cache | Existing config | Off, to re-evaluate | Pending | Pending |
| Specialization generator | Existing properties | On, to re-evaluate | Pending | Pending |
| CSR cache tunables | Existing properties | Existing defaults | Pending | Pending |

Revision note (2026-07-19, Codex): Created the Phase I rollout ledger from the inherited and new feature
flags enumerated in `13-verification.md`.
