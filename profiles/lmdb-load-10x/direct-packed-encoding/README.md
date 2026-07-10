# Rejected direct packed-value encoder

This candidate replaced the retained `ArrayList<byte[]>` encoding stage with one size pass and one direct UTF-8 pass into `MDB_RESERVE` buffers. A byte-equivalence test covered IRIs, blank nodes, typed and directional language literals, triple terms, non-ASCII text, supplementary characters, and malformed surrogates. Packed reopen tests also passed.

The exact paired JDK 26, G1, 2 GiB, five-warmup/five-measurement result is `candidate.json`.

| Isolation | Retained 32-bit checkpoint ms/op | Direct encoder ms/op | Retained B/op | Direct B/op |
| --- | ---: | ---: | ---: | ---: |
| NONE | 102.174 | 155.472 | 94,965,668 | 29,846,125 |
| READ_COMMITTED | 157.360 | 206.957 | 119,524,052 | 54,397,857 |

The manual direct encoder removes roughly 65 MB/op, but its two character scans and byte-at-a-time direct-buffer writes lose HotSpot's optimized `String.getBytes(UTF_8)` implementation. The candidate regresses elapsed time by 33-52 ms and is therefore removed. A future encoding candidate must retain the JDK encoder or prove an ASCII bulk-copy fast path.
