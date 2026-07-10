# Rejected contiguous packed-value collector

This candidate replaced the two pre-sized `ArrayList` collectors with a custom `Value[]` collector and exact `byte[][]` encoded-record array. The primitive equality map, IDs, and encoded bytes were unchanged, and three packed round-trip/reopen tests passed.

The exact paired JDK 26 result is `candidate.json`.

| Isolation | Primitive-map checkpoint ms/op | Array collector ms/op | Allocation change |
| --- | ---: | ---: | ---: |
| NONE | 92.354 | 105.363 | neutral |
| READ_COMMITTED | 145.334 | 167.662 | neutral |

Allocation did not change at JMH precision because both retained `ArrayList`s were already exactly pre-sized. The custom recursive collector added call and bounds overhead that HotSpot did not optimize as well as the standard lists. The candidate is removed.
