#!/bin/bash
SP=/private/tmp/claude-501/-Users-havardottestad-Documents-Programming-rdf4j-small-things/fe2b5b67-a109-4362-a39f-72d9e6f6dd73/scratchpad
R=/Users/havardottestad/Documents/Programming/rdf4j-small-things/.agent/research/GH-0000-wp9-differential
cd $SP/wt-br
REP=.mvnf/workspaces/diffsweep/build/org.eclipse.rdf4j/rdf4j-sparql-compliance/6.2.0-SNAPSHOT/failsafe-reports
for mode in none streaming-correlated memoized-correlated materialized-hash; do
  m=$mode; [ "$mode" = none ] && m=
  python3 .codex/skills/mvnf/scripts/mvnf.py --workspace diffsweep --threads 3 compliance/sparql --retain-logs -- \
    "-Dit.test=MemoryHint*" -Ddiff.existsMode=$m -Ddiff.dump.dir=$R/results/dumps/l2-$mode > $R/results/l2/$mode.log 2>&1
  echo "exit $?" >> $R/results/l2/$mode.log
  rm -rf $R/results/l2/reports-$mode; cp -R $REP $R/results/l2/reports-$mode
done
