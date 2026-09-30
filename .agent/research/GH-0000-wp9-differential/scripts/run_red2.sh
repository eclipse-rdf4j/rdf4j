#!/bin/bash
cd /Users/havardottestad/Documents/Programming/rdf4j-small-things
R=.agent/research/GH-0000-wp9-differential
while pgrep -f run_l2_props.sh > /dev/null; do sleep 20; done
python3 .codex/skills/mvnf/scripts/mvnf.py --workspace diffred --threads 3 LmdbDifferentialRegressionTest --retain-logs > $R/results/red/LmdbDifferentialRegressionTest.run2.log 2>&1
echo "exit $?" >> $R/results/red/LmdbDifferentialRegressionTest.run2.log
