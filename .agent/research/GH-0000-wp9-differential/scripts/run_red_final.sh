#!/bin/bash
cd /Users/havardottestad/Documents/Programming/rdf4j-small-things
R=.agent/research/GH-0000-wp9-differential
B=.mvnf/workspaces/diffred/build/org.eclipse.rdf4j
mkdir -p $R/results/red/final
for spec in "lmdb:LmdbDifferentialRegressionTest" "lmdb:LmdbStaleAnswerAfterInsertTest" "lmdb:LmdbPackedPlanningBlowupTest" "memory:ExistsSubstitutionExpressionErrorTest" "memory:GraphVariableBoundToLiteralTest"; do
  mod=${spec%%:*}; t=${spec##*:}
  python3 .codex/skills/mvnf/scripts/mvnf.py --workspace diffred --threads 3 $t --retain-logs > $R/results/red/final/$t.log 2>&1
  echo "exit $?" >> $R/results/red/final/$t.log
  mkdir -p $R/results/red/final/$t && cp $B/rdf4j-sail-$mod/6.2.0-SNAPSHOT/surefire-reports/*$t* $R/results/red/final/$t/ 2>/dev/null
done
echo DONE > $R/results/red/final/DONE
