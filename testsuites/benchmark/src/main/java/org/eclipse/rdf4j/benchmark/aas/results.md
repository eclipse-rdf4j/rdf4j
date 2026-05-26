# Without sketches:
```
/Users/havardottestad/.sdkman/candidates/java/26-zulu/zulu-26.jdk/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=60327 -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath /Users/havardottestad/Documents/Programming/rdf4j/testsuites/benchmark/target/classes:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:/Users/havardottestad/.m2/repository/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar:/Users/havardottestad/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar:/Users/havardottestad/Documents/Programming/rdf4j/testsuites/benchmark-common/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/model/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/model-api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/annotation/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/exception/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/text/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/io/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/iterator/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/model-vocabulary/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/xml/target/classes:/Users/havardottestad/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:/Users/havardottestad/.m2/repository/com/github/jsonld-java/jsonld-java/0.13.4/jsonld-java-0.13.4.jar:/Users/havardottestad/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar:/Users/havardottestad/.m2/repository/tools/jackson/core/jackson-databind/3.1.2/jackson-databind-3.1.2.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/query/target/classes:/Users/havardottestad/.m2/repository/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar:/Users/havardottestad/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/queryalgebra/model/target/classes:/Users/havardottestad/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.21.0/jackson-databind-2.21.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/repository/sail/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/repository/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/transaction/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/common/order/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/collection-factory/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/http/client/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/http/protocol/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/http/client-api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/http/client-jdk/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/http/client-apache5/target/classes:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/client5/httpclient5/5.4.3/httpclient5-5.4.3.jar:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/core5/httpcore5/5.3.4/httpcore5-5.3.4.jar:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/core5/httpcore5-h2/5.3.4/httpcore5-h2-5.3.4.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/queryresultio/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/queryresultio/binary/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/queryresultio/sparqlxml/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/queryparser/api/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/queryalgebra/evaluation/target/classes:/Users/havardottestad/.m2/repository/org/apache/datasketches/datasketches-java/9.0.0/datasketches-java-9.0.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/repository/sparql/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/queryparser/sparql/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/trig/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/inferencer/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/base/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/model/target/classes:/Users/havardottestad/.m2/repository/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/memory/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/lmdb/target/classes:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-linux.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-linux-arm64.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-linux-ppc64le.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-macos.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-macos-arm64.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-windows.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl-lmdb/3.4.1/lwjgl-lmdb-3.4.1-natives-windows-arm64.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-linux.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-linux-arm64.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-linux-ppc64le.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-macos.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-macos-arm64.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-windows.jar:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-windows-arm64.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/collection-factory/mapdb3/target/classes:/Users/havardottestad/.m2/repository/org/mapdb/mapdb/3.1.0/mapdb-3.1.0.jar:/Users/havardottestad/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/1.9.25/kotlin-stdlib-1.9.25.jar:/Users/havardottestad/.m2/repository/org/jetbrains/annotations/13.0/annotations-13.0.jar:/Users/havardottestad/.m2/repository/org/eclipse/collections/eclipse-collections-api/10.4.0/eclipse-collections-api-10.4.0.jar:/Users/havardottestad/.m2/repository/org/eclipse/collections/eclipse-collections/10.4.0/eclipse-collections-10.4.0.jar:/Users/havardottestad/.m2/repository/org/eclipse/collections/eclipse-collections-forkjoin/10.4.0/eclipse-collections-forkjoin-10.4.0.jar:/Users/havardottestad/.m2/repository/org/mapdb/elsa/3.0.0-M5/elsa-3.0.0-M5.jar:/Users/havardottestad/.m2/repository/at/yawk/lz4/lz4-java/1.10.4/lz4-java-1.10.4.jar:/Users/havardottestad/.m2/repository/com/google/guava/guava/32.1.3-jre/guava-32.1.3-jre.jar:/Users/havardottestad/.m2/repository/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar:/Users/havardottestad/.m2/repository/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/sail/nativerdf/target/classes:/Users/havardottestad/.m2/repository/tools/jackson/core/jackson-core/3.1.2/jackson-core-3.1.2.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/queryrender/target/classes:/Users/havardottestad/.m2/repository/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar:/Users/havardottestad/.m2/repository/com/google/errorprone/error_prone_annotations/2.41.0/error_prone_annotations-2.41.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/turtle/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/datatypes/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/languages/target/classes:/Users/havardottestad/.m2/repository/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/binary/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/nquads/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/ntriples/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/rdfjson/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/jsonld/target/classes:/Users/havardottestad/.m2/repository/no/hasmac/hasmac-json-ld/0.10.2/hasmac-json-ld-0.10.2.jar:/Users/havardottestad/.m2/repository/org/glassfish/jakarta.json/2.0.1/jakarta.json-2.0.1.jar:/Users/havardottestad/.m2/repository/jakarta/json/jakarta.json-api/2.0.1/jakarta.json-api-2.0.1.jar:/Users/havardottestad/.m2/repository/org/glassfish/jakarta.json/2.0.1/jakarta.json-2.0.1-module.jar:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/httpclient/4.5.14/httpclient-4.5.14.jar:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar:/Users/havardottestad/.m2/repository/org/apache/httpcomponents/httpclient-cache/4.5.14/httpclient-cache-4.5.14.jar:/Users/havardottestad/.m2/repository/org/slf4j/jcl-over-slf4j/2.0.17/jcl-over-slf4j-2.0.17.jar:/Users/havardottestad/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.21.0/jackson-core-2.21.0.jar:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/rdfxml/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/trix/target/classes:/Users/havardottestad/Documents/Programming/rdf4j/core/rio/n3/target/classes:/Users/havardottestad/.m2/repository/org/jline/jline/3.6.2/jline-3.6.2.jar:/Users/havardottestad/.m2/repository/org/junit/vintage/junit-vintage-engine/6.0.2/junit-vintage-engine-6.0.2.jar:/Users/havardottestad/.m2/repository/org/junit/platform/junit-platform-engine/6.0.2/junit-platform-engine-6.0.2.jar:/Users/havardottestad/.m2/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:/Users/havardottestad/.m2/repository/org/junit/platform/junit-platform-commons/6.0.2/junit-platform-commons-6.0.2.jar:/Users/havardottestad/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar:/Users/havardottestad/.m2/repository/org/hamcrest/hamcrest-core/3.0/hamcrest-core-3.0.jar:/Users/havardottestad/.m2/repository/org/hamcrest/hamcrest/3.0/hamcrest-3.0.jar:/Users/havardottestad/.m2/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar:/Users/havardottestad/.m2/repository/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:/Users/havardottestad/.m2/repository/org/junit/jupiter/junit-jupiter-engine/6.0.2/junit-jupiter-engine-6.0.2.jar:/Users/havardottestad/.m2/repository/org/junit/jupiter/junit-jupiter-api/6.0.2/junit-jupiter-api-6.0.2.jar:/Users/havardottestad/.m2/repository/org/assertj/assertj-core/3.27.7/assertj-core-3.27.7.jar:/Users/havardottestad/.m2/repository/net/bytebuddy/byte-buddy/1.18.3/byte-buddy-1.18.3.jar org.openjdk.jmh.Main org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.*
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by org.openjdk.jmh.util.Utils (file:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar)
WARNING: Please consider reporting this to the maintainers of class org.openjdk.jmh.util.Utils
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
# JMH version: 1.37
# VM version: JDK 26, OpenJDK 64-Bit Server VM, 26+35
# VM invoker: /Users/havardottestad/.sdkman/candidates/java/26-zulu/zulu-26.jdk/Contents/Home/bin/java
# VM options: -Xms1G -Xmx4G
# Blackhole mode: compiler (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op
# Benchmark: org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query1PropertyProjection
# Parameters: (driveUnitCount = 100, productionLineCount = 100, sensorCount = 100, store = lmdb)

# Run progress: 0.00% complete, ETA 00:01:00
# Fork: 1 of 1
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by org.openjdk.jmh.util.Utils (file:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar)
WARNING: Please consider reporting this to the maintainers of class org.openjdk.jmh.util.Utils
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
# Warmup Iteration   1: SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by org.lwjgl.system.Library$$Lambda/0x00003fc001092ad0 in an unnamed module (file:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled

Number of statements: 2374037
Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_77dbc54db0fb24332ac2d242de66236ae0123456, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_77dbc54db0fb24332ac2d242de66236ae0123456, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_67dbc54db0fb24332ac2d242de66236ae012345, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_77dbc54db0fb24332ac2d242de66236ae0123456, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_67dbc54db0fb24332ac2d242de66236ae012345, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_27dbc54db0fb24332ac2d242de66236ae01, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_27dbc54db0fb24332ac2d242de66236ae01, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_47dbc54db0fb24332ac2d242de66236ae0123, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_47dbc54db0fb24332ac2d242de66236ae0123, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

0.097 ms/op
# Warmup Iteration   2: 0.057 ms/op
# Warmup Iteration   3: 0.057 ms/op
# Warmup Iteration   4: 0.056 ms/op
# Warmup Iteration   5: 0.056 ms/op
# Warmup Iteration   6: 0.058 ms/op
# Warmup Iteration   7: 0.057 ms/op
# Warmup Iteration   8: 0.057 ms/op
# Warmup Iteration   9: 0.057 ms/op
# Warmup Iteration  10: 0.057 ms/op
Iteration   1: 0.057 ms/op
Iteration   2: 0.057 ms/op
Iteration   3: 0.058 ms/op
Iteration   4: 0.057 ms/op
Iteration   5: 0.056 ms/op
Iteration   6: 0.057 ms/op
Iteration   7: 0.057 ms/op
Iteration   8: 0.057 ms/op
Iteration   9: 0.057 ms/op
Iteration  10: Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_37670137dbc54db0fb24332ac2d242de66236ae012, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_37670137dbc54db0fb24332ac2d242de66236ae012, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_27670137dbc54db0fb24332ac2d242de66236ae01, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_37670137dbc54db0fb24332ac2d242de66236ae012, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_27670137dbc54db0fb24332ac2d242de66236ae01, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_86670137dbc54db0fb24332ac2d242de66236ae01234567, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_86670137dbc54db0fb24332ac2d242de66236ae01234567, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_07670137dbc54db0fb24332ac2d242de66236ae, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_07670137dbc54db0fb24332ac2d242de66236ae, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

0.058 ms/op


Result "org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query1PropertyProjection":
  0.057 ±(99.9%) 0.001 ms/op [Average]
  (min, avg, max) = (0.056, 0.057, 0.058), stdev = 0.001
  CI (99.9%): [0.056, 0.058] (assumes normal distribution)


# JMH version: 1.37
# VM version: JDK 26, OpenJDK 64-Bit Server VM, 26+35
# VM invoker: /Users/havardottestad/.sdkman/candidates/java/26-zulu/zulu-26.jdk/Contents/Home/bin/java
# VM options: -Xms1G -Xmx4G
# Blackhole mode: compiler (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op
# Benchmark: org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query2ThresholdCount
# Parameters: (driveUnitCount = 100, productionLineCount = 100, sensorCount = 100, store = lmdb)

# Run progress: 33.33% complete, ETA 00:01:17
# Fork: 1 of 1
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by org.openjdk.jmh.util.Utils (file:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar)
WARNING: Please consider reporting this to the maintainers of class org.openjdk.jmh.util.Utils
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
# Warmup Iteration   1: WARNING: A restricted method in java.lang.System has been called
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
WARNING: java.lang.System::load has been called by org.lwjgl.system.Library$$Lambda/0x000003f001092ad0 in an unnamed module (file:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled

Number of statements: 2374037
Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_7abd9535abacb426784c0dd0168331c770123456, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_7abd9535abacb426784c0dd0168331c770123456, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_6abd9535abacb426784c0dd0168331c77012345, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_7abd9535abacb426784c0dd0168331c770123456, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_6abd9535abacb426784c0dd0168331c77012345, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_2abd9535abacb426784c0dd0168331c7701, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_2abd9535abacb426784c0dd0168331c7701, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_4abd9535abacb426784c0dd0168331c770123, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_4abd9535abacb426784c0dd0168331c770123, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

136.417 ms/op
# Warmup Iteration   2: 115.622 ms/op
# Warmup Iteration   3: 109.985 ms/op
# Warmup Iteration   4: 105.965 ms/op
# Warmup Iteration   5: 114.952 ms/op
# Warmup Iteration   6: 108.645 ms/op
# Warmup Iteration   7: 106.987 ms/op
# Warmup Iteration   8: 104.579 ms/op
# Warmup Iteration   9: 102.597 ms/op
# Warmup Iteration  10: 104.478 ms/op
Iteration   1: 105.438 ms/op
Iteration   2: 107.578 ms/op
Iteration   3: 110.957 ms/op
Iteration   4: 119.223 ms/op
Iteration   5: 114.633 ms/op
Iteration   6: 121.779 ms/op
Iteration   7: 124.177 ms/op
Iteration   8: 124.206 ms/op
Iteration   9: 130.612 ms/op
Iteration  10: Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_6802abd9535abacb426784c0dd0168331c77012345, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_6802abd9535abacb426784c0dd0168331c77012345, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_5802abd9535abacb426784c0dd0168331c7701234, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_6802abd9535abacb426784c0dd0168331c77012345, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_5802abd9535abacb426784c0dd0168331c7701234, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_1802abd9535abacb426784c0dd0168331c770, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_1802abd9535abacb426784c0dd0168331c770, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_3802abd9535abacb426784c0dd0168331c77012, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_3802abd9535abacb426784c0dd0168331c77012, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

118.764 ms/op


Result "org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query2ThresholdCount":
  117.737 ±(99.9%) 12.124 ms/op [Average]
  (min, avg, max) = (105.438, 117.737, 130.612), stdev = 8.019
  CI (99.9%): [105.613, 129.860] (assumes normal distribution)


# JMH version: 1.37
# VM version: JDK 26, OpenJDK 64-Bit Server VM, 26+35
# VM invoker: /Users/havardottestad/.sdkman/candidates/java/26-zulu/zulu-26.jdk/Contents/Home/bin/java
# VM options: -Xms1G -Xmx4G
# Blackhole mode: compiler (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op
# Benchmark: org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query3LineAggregates
# Parameters: (driveUnitCount = 100, productionLineCount = 100, sensorCount = 100, store = lmdb)

# Run progress: 66.67% complete, ETA 00:00:39
# Fork: 1 of 1
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by org.openjdk.jmh.util.Utils (file:/Users/havardottestad/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar)
WARNING: Please consider reporting this to the maintainers of class org.openjdk.jmh.util.Utils
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
# Warmup Iteration   1: SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by org.lwjgl.system.Library$$Lambda/0x000000e001092ad0 in an unnamed module (file:/Users/havardottestad/.m2/repository/org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled

Number of statements: 2374037
Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_75c7d21efd94a4ad8a0184b4041ecc1680123456, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_75c7d21efd94a4ad8a0184b4041ecc1680123456, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_65c7d21efd94a4ad8a0184b4041ecc168012345, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_75c7d21efd94a4ad8a0184b4041ecc1680123456, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_65c7d21efd94a4ad8a0184b4041ecc168012345, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_25c7d21efd94a4ad8a0184b4041ecc16801, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_25c7d21efd94a4ad8a0184b4041ecc16801, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_45c7d21efd94a4ad8a0184b4041ecc1680123, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_45c7d21efd94a4ad8a0184b4041ecc1680123, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

285.398 ms/op
# Warmup Iteration   2: 196.269 ms/op
# Warmup Iteration   3: 196.242 ms/op
# Warmup Iteration   4: 194.842 ms/op
# Warmup Iteration   5: 194.456 ms/op
# Warmup Iteration   6: 190.896 ms/op
# Warmup Iteration   7: 189.057 ms/op
# Warmup Iteration   8: 188.098 ms/op
# Warmup Iteration   9: 192.101 ms/op
# Warmup Iteration  10: 198.538 ms/op
Iteration   1: 198.650 ms/op
Iteration   2: 196.250 ms/op
Iteration   3: 188.777 ms/op
Iteration   4: 184.515 ms/op
Iteration   5: 186.705 ms/op
Iteration   6: 181.451 ms/op
Iteration   7: 198.660 ms/op
Iteration   8: 197.611 ms/op
Iteration   9: 205.155 ms/op
Iteration  10: Projection
╠══ ProjectionElemList
║     ProjectionElem "c"
╚══ Extension
   ├── Group ()
   │  ╠══ Join (JoinIterator)
   │  ║  ├── StatementPattern (costEstimate=3.3K, resultSizeEstimate=10.0K) [left]
   │  ║  │     s: Var (name=p1) (bindingState=unbound)
   │  ║  │     p: Var (name=_const_41bce0eb_uri, value=https://admin-shell.io/aas/3/idShort, anonymous)
   │  ║  │     o: Var (name=_const_96c05be1_lit_e2eec718, value="ratedPower", anonymous)
   │  ║  └── Join (JoinIterator) [right]
   │  ║     ╠══ Filter (costEstimate=500, resultSizeEstimate=250.3K, plannedFilterPassRatioLower=0, plannedFilterPassRatio=1.00, plannedFilterPassRatioUpper=1.00, plannedFilterConfidence=0.50, plannedEstimateSource=cardinality, filterSelectivitySource=cardinality) [left]
   │  ║     ║  ├── Compare (>)
   │  ║     ║  │     Var (name=v1) (bindingState=bound)
   │  ║     ║  │     ValueConstant (value="15.0"^^<http://www.w3.org/2001/XMLSchema#decimal>)
   │  ║     ║  └── StatementPattern (resultSizeEstimate=250.3K)
   │  ║     ║        s: Var (name=p1) (bindingState=bound)
   │  ║     ║        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║     ║        o: Var (name=v1) (bindingState=unbound)
   │  ║     ╚══ Join (JoinIterator) [right]
   │  ║        ├── ArbitraryLengthPath (costEstimate=1.1K, resultSizeEstimate=4.7M) [left]
   │  ║        │     Var (name=_anon_path_01415c7d21efd94a4ad8a0184b4041ecc168, anonymous) (bindingState=unbound)
   │  ║        │     StatementPattern (resultSizeEstimate=250.3K)
   │  ║        │        s: Var (name=_anon_path_01415c7d21efd94a4ad8a0184b4041ecc168, anonymous) (bindingState=unbound)
   │  ║        │        p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║        │        o: Var (name=p1) (bindingState=bound)
   │  ║        │     Var (name=p1) (bindingState=bound)
   │  ║        └── Join (JoinIterator) [right]
   │  ║           ╠══ StatementPattern (costEstimate=112, resultSizeEstimate=50.2K) [left]
   │  ║           ║     s: Var (name=_anon_path_90415c7d21efd94a4ad8a0184b4041ecc168012345678, anonymous) (bindingState=unbound)
   │  ║           ║     p: Var (name=_const_2b34e09d_uri, value=https://admin-shell.io/aas/3/submodelElement, anonymous)
   │  ║           ║     o: Var (name=_anon_path_01415c7d21efd94a4ad8a0184b4041ecc168, anonymous) (bindingState=bound)
   │  ║           ╚══ Join (JoinIterator) [right]
   │  ║              ├── StatementPattern (costEstimate=82, resultSizeEstimate=60.2K) [left]
   │  ║              │     s: Var (name=aas) (bindingState=unbound)
   │  ║              │     p: Var (name=_const_a1d3dd5f_uri, value=https://admin-shell.io/aas/3/submodel, anonymous)
   │  ║              │     o: Var (name=_anon_path_90415c7d21efd94a4ad8a0184b4041ecc168012345678, anonymous) (bindingState=bound)
   │  ║              └── Join (JoinIterator) [right]
   │  ║                 ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=20.1K) [left]
   │  ║                 ║     s: Var (name=aas) (bindingState=bound)
   │  ║                 ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║                 ║     o: Var (name=_const_9fa43ff8_uri, value=https://admin-shell.io/aas/3/AssetAdministrationShell, anonymous)
   │  ║                 ╚══ Join (JoinIterator) [right]
   │  ║                    ├── StatementPattern (costEstimate=71, resultSizeEstimate=20.1K) [left]
   │  ║                    │     s: Var (name=aas) (bindingState=bound)
   │  ║                    │     p: Var (name=_const_1bbfde92_uri, value=https://admin-shell.io/aas/3/assetInformation, anonymous)
   │  ║                    │     o: Var (name=_anon_path_50415c7d21efd94a4ad8a0184b4041ecc16801234, anonymous) (bindingState=unbound)
   │  ║                    └── Join (JoinIterator) [right]
   │  ║                       ╠══ StatementPattern (costEstimate=71, resultSizeEstimate=20.0K) [left]
   │  ║                       ║     s: Var (name=_anon_path_50415c7d21efd94a4ad8a0184b4041ecc16801234, anonymous) (bindingState=bound)
   │  ║                       ║     p: Var (name=_const_8ef14163_uri, value=https://admin-shell.io/aas/3/specificAssetId, anonymous)
   │  ║                       ║     o: Var (name=_anon_bnode_70415c7d21efd94a4ad8a0184b4041ecc1680123456, anonymous) (bindingState=unbound)
   │  ║                       ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=10.0K) [right]
   │  ║                             s: Var (name=_anon_bnode_70415c7d21efd94a4ad8a0184b4041ecc1680123456, anonymous) (bindingState=bound)
   │  ║                             p: Var (name=_const_b1a99cbb_uri, value=https://admin-shell.io/aas/3/value, anonymous)
   │  ║                             o: Var (name=_const_25f5320e_lit_e2eec718, value="DriveUnit", anonymous)
   │  ╚══ GroupElem (c)
   │        Count
   └── ExtensionElem (c)
         Count

205.351 ms/op


Result "org.eclipse.rdf4j.benchmark.aas.AASQueriesBenchmark.query3LineAggregates":
  194.313 ±(99.9%) 12.766 ms/op [Average]
  (min, avg, max) = (181.451, 194.313, 205.351), stdev = 8.444
  CI (99.9%): [181.547, 207.078] (assumes normal distribution)


# Run complete. Total time: 00:02:01

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

NOTE: Current JVM experimentally supports Compiler Blackholes, and they are in use. Please exercise
extra caution when trusting the results, look into the generated code to check the benchmark still
works, and factor in a small probability of new VM bugs. Additionally, while comparisons between
different JVMs are already problematic, the performance difference caused by different Blackhole
modes can be very significant. Please make sure you use the consistent Blackhole mode for comparisons.

Benchmark                                     (driveUnitCount)  (productionLineCount)  (sensorCount)  (store)  Mode  Cnt    Score    Error  Units
AASQueriesBenchmark.query1PropertyProjection               100                    100            100     lmdb  avgt   10    0.057 ±  0.001  ms/op
AASQueriesBenchmark.query2ThresholdCount                   100                    100            100     lmdb  avgt   10  117.737 ± 12.124  ms/op
AASQueriesBenchmark.query3LineAggregates                   100                    100            100     lmdb  avgt   10  194.313 ± 12.766  ms/op

Process finished with exit code 0

```
