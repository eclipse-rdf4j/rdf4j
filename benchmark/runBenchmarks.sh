#!/usr/bin/env bash
mvn clean install
java -jar target/rdf4j-benchmark.jar -wi 10 -i 10 -f 3

