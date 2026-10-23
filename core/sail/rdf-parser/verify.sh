#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
offline=(-o)
if [[ "${MVN_OFFLINE:-true}" == false ]]; then offline=(); fi
mkdir -p logs/rdf-parser-integration
# Install all current artifacts first; tests below resolve modules from this local repository.
mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C "${offline[@]}" \
  -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '
    /\[WARNING\]/ { next }
    /\[ERROR\]/ { print; next }
    /Reactor Summary/ { summary=1 }
    summary { print }
  '
mvn -B -ntp "${offline[@]}" -Dmaven.repo.local=.m2_repo \
  -pl core/rio/api,core/rio/turtle,core/rio/trig,core/rio/ntriples,core/rio/nquads,core/sail/rdf-parser \
  verify 2>&1 | tee logs/rdf-parser-integration/verify.log
mvn -B -ntp "${offline[@]}" -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation \
  -Dtest=NativeTripleTermMatchingTest,EvaluationStrategyWithTripleTermsTest,TupleFunctionResultBindingTest,FilterOptimizerTest verify \
  2>&1 | tee logs/rdf-parser-integration/evaluator-verify.log
