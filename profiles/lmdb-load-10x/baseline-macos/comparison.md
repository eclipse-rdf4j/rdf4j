| Benchmark | (automaticEvaluationStrategy) | (isolationLevel) | Mode | Units | Score [run-1] | Score [run-2] | Diff Score [run-2 - run-1] | Diff % [run-2 - run-1] | Status [run-2 vs run-1] |
| :--- | :--- | :--- | :--- | :--- | ---: | ---: | ---: | ---: | :--- |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction | false | NONE | avgt | ms/op | 804.502 | 757.442 | -47.06 | -5.850% | improvement |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction | false | READ_COMMITTED | avgt | ms/op | 876.109 | 786.051 | -90.058 | -10.279% | improvement |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.alloc.rate | false | NONE | avgt | MB/sec | 412.976 | 454.907 | 41.931 | 10.153% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.alloc.rate | false | READ_COMMITTED | avgt | MB/sec | 511.676 | 572.481 | 60.805 | 11.883% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.alloc.rate.norm | false | NONE | avgt | B/op | 351714260 | 365185010 | 13470749.6 | 3.830% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.alloc.rate.norm | false | READ_COMMITTED | avgt | B/op | 471326264 | 474489311 | 3163047.2 | 0.671% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.count | false | NONE | avgt | counts | 5 | 5 | 0 | 0.000% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.count | false | READ_COMMITTED | avgt | counts | 5 | 5 | 0 | 0.000% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.time | false | NONE | avgt | ms | 5 | 5 | 0 | 0.000% | regression |
| DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction:gc.time | false | READ_COMMITTED | avgt | ms | 5 | 5 | 0 | 0.000% | regression |
