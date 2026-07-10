# Packed 32-bit local IDs

The fresh-load packed format has a transaction-local value dictionary, so quad records only need local dictionary IDs. This increment stores those four IDs as 32-bit integers instead of 64-bit longs. Legacy packed records that contain global value-store IDs remain 64-bit. No LMDB append flag is used.

The three files form a same-host candidate-control-candidate sequence. Every run used JDK 26, G1, `-Xms2G -Xmx2G`, five one-second warmups, five one-second measurements, one fork, `automaticEvaluationStrategy=false`, and the exact end-anchored benchmark method.

| Run | NONE ms/op | READ_COMMITTED ms/op | NONE B/op | READ_COMMITTED B/op |
| --- | ---: | ---: | ---: | ---: |
| 32-bit candidate A | 119.603 | 170.107 | 94,965,757 | 119,527,344 |
| 64-bit control | 123.404 | 170.083 | 107,660,185 | 132,220,912 |
| 32-bit candidate B | 102.174 | 157.360 | 94,965,668 | 119,524,052 |

Pooling the bracketing candidate runs gives 110.889 ms/op for `NONE` and 163.734 ms/op for `READ_COMMITTED`, respectively 10.14% and 3.73% below the intervening control. More importantly, both candidates consistently remove about 12.69 MB/op, and the persisted quad stream is half the previous size. The candidate is therefore retained as a small throughput and substantial memory/I/O increment.
