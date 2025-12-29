# Optimizer Harness

This module provides a deterministic harness for UNION/OPTIONAL optimizer work. It generates a synthetic dataset, runs a fixed set of UNION/OPTIONAL query families, compares baseline vs candidate results, and emits per-node CSV metrics using RDF4J's `Explanation.Level.Timed` plan tracking.

## Run (module jar)

From repo root:

    mvn -o -Dmaven.repo.local=.m2_repo -pl tools/optimizer-harness -am -Pquick clean install
    mvn -o -Dmaven.repo.local=.m2_repo -pl tools/optimizer-harness \
      dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/classpath.txt
    java -cp "$(cat tools/optimizer-harness/target/classpath.txt):tools/optimizer-harness/target/rdf4j-optimizer-harness-5.3.0-SNAPSHOT.jar" \
      org.eclipse.rdf4j.tools.optimizer.harness.HarnessRunner

## Options

    --profile <tiny|small|medium>
    --seed <long>
    --subjects <int>
    --fanout <int>
    --objects <int>
    --optional-rate <double>
    --union-skew <double>
    --filter-selectivity <double>
    --max-regression <double>
    --output <dir>
    --baseline-only

Outputs are written to `tools/optimizer-harness/target/harness/run-<timestamp>/` by default.

## CSV columns

The harness CSV includes planner metrics (`costEstimate`, `resultSizeEstimate`, `resultSizeActual`, `totalTimeMs`) plus
CardinalityEstimator outputs (`cardRowsEstimate`, `cardWorkEstimate`) so you can compare estimator expectations with
observed counts and time.
