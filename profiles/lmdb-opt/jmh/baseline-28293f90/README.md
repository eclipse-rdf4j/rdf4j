# ValueComparator baseline at 28293f90

The detached source tree was produced with `git archive 28293f90` at
`/tmp/rdf4j-28293f90-comparator`. The current `ValueComparatorBenchmark.java` was copied into it unchanged; both
copies had SHA-256 `af22e71d097cdf912054f121822d7f4bf76cc336f3ae95c8379283ba6f6bde13`.

Both trees used Zulu JDK 25, JMH 1.37, four 2-second warmups, four 2-second measurements, three forks, Serial GC,
`-Xms8G`, `-Xmx8G`, and `-Xmn4G`. The baseline POM received only the same explicit JMH test annotation-processor
configuration needed for JDK 25 benchmark discovery; production sources were unchanged.

`sortCalendars` failed in every STANDARD and STRICT fork during the first warmup with:

    java.lang.IllegalArgumentException: Comparison method violates its general contract!
        at java.base/java.util.TimSort.mergeHi(TimSort.java:903)

Its JSON result is therefore an empty array. The copy and comparison-control JSON files contain the completed baseline
measurements. The matching current-revision JSON files are in the parent directory.
