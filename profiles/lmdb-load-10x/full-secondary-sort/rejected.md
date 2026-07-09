# Rejected full secondary-key sort

This experiment replaced the existing one-field stable radix sort for secondary indexes with four stable radix passes
covering the complete index field sequence. The intent was to improve ordinary cursor-put locality without using
`MDB_APPEND` or `MDB_APPENDDUP`.

The primitive lexicographic sort contract passed, followed by all 27 `TripleStoreTest` tests and both aligned
sort/reset tests. Performance nevertheless regressed decisively, so the production and test experiment was removed.

| Isolation | Main-cursor checkpoint pooled (ms/op) | Full secondary sort (ms/op) | Change |
| --- | ---: | ---: | ---: |
| NONE | 705.864 | 779.954 | +10.50% |
| READ_COMMITTED | 712.789 | 771.224 | +8.20% |

The three additional radix passes per secondary index cost more than the B-tree comparison and locality work they
saved. A confirmation run was unnecessary given the large, symmetric regression and stable iteration values.
