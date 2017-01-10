#!/usr/bin/env bash
mvn install && java -ea -jar target/rdf4j-benchmark.jar -wi 10 -i 10 -f 3 -gc

