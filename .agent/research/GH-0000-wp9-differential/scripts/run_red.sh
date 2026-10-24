#!/bin/bash
cd /Users/havardottestad/Documents/Programming/rdf4j-small-things
R=.agent/research/GH-0000-wp9-differential
mkdir -p $R/results/red
for t in ExistsSubstitutionExpressionErrorTest GraphVariableBoundToLiteralTest LmdbDifferentialRegressionTest LmdbPackedPlanningBlowupTest; do
  python3 .codex/skills/mvnf/scripts/mvnf.py --workspace diffred --threads 3 $t --retain-logs > $R/results/red/$t.log 2>&1
  echo "exit $?" >> $R/results/red/$t.log
done
