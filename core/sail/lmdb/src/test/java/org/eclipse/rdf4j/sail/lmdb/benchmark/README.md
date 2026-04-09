# LMDB Benchmark Suite

Benchmarks for measuring RDF4J LMDB Store read/write performance.

## Available Benchmarks

| Benchmark | Description |
|-----------|-------------|
| `FullIndexBenchmark` | Tests with all 6 indexes enabled (spoc,sopc,posc,ospc,opsc,cspo) at 100K, 1M, 10M, 100M scale. Generates markdown report. |
| `SingleIndexBenchmark` | Compares each of the 6 single index variants at 1M scale. |
| `IndexConfigBenchmark` | Tests different index combinations (1, 2, 3, 4 indexes) at 1M scale. |
| `BulkLoadBenchmark` | Tests bulk loading with different transaction batch sizes (single, 10K, 100K per txn). |
| `SimpleConcurrentBenchmark` | Multi-threaded read/write performance with 1-8 threads. |

## Running Benchmarks

From the project root directory:

```bash
# Compile first
mvn compile test-compile -pl core/sail/lmdb -am -DskipTests

# Run a specific benchmark
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.FullIndexBenchmark" \
  -Dexec.classpathScope=test
```

### Run Each Benchmark

```bash
# Full Index Benchmark (100K to 100M scale, generates report)
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.FullIndexBenchmark" \
  -Dexec.classpathScope=test

# Single Index Benchmark (compare 6 index variants)
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.SingleIndexBenchmark" \
  -Dexec.classpathScope=test

# Index Config Benchmark (index combinations)
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.IndexConfigBenchmark" \
  -Dexec.classpathScope=test

# Bulk Load Benchmark (transaction batch sizes)
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.BulkLoadBenchmark" \
  -Dexec.classpathScope=test

# Concurrent Benchmark (multi-threaded)
mvn exec:java -pl core/sail/lmdb \
  -Dexec.mainClass="org.eclipse.rdf4j.sail.lmdb.benchmark.SimpleConcurrentBenchmark" \
  -Dexec.classpathScope=test
```

## Output

- Console output shows real-time progress and results
- `FullIndexBenchmark` generates a timestamped markdown report: `lmdb-benchmark-report-YYYY-MM-DD_HH-mm-ss.md`

## Index Configurations

LMDB supports 6 index types based on SPOC (Subject, Predicate, Object, Context) ordering:

| Index | Optimized For |
|-------|---------------|
| `spoc` | Lookups by subject |
| `sopc` | Lookups by subject + object |
| `posc` | Lookups by predicate |
| `ospc` | Lookups by object |
| `opsc` | Lookups by object + predicate |
| `cspo` | Lookups by context (named graph) |

## Requirements

- Java 11+
- Maven 3.6+
- Sufficient disk space (100M scale test uses ~7GB temporary storage)
- Sufficient RAM for large-scale tests
