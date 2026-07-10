# Rejected adjacent identity cache

This candidate remembered the immediately previous subject, predicate, object, and context object while scanning statements. An identical Java object could reuse its packed ID without hashing; every miss used the retained collision-safe primitive map. Three packed round-trip/reopen tests passed.

The exact paired JDK 26 result is `candidate.json`.

| Isolation | Primitive-map checkpoint ms/op | Identity cache ms/op | Allocation change |
| --- | ---: | ---: | ---: |
| NONE | 92.354 | 98.083 | neutral |
| READ_COMMITTED | 145.334 | 157.492 | neutral |

Four identity branches and state updates per statement cost more than the adjacent identity hits save. The candidate is removed and the primitive-map checkpoint restored.
