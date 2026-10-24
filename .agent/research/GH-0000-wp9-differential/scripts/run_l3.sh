#!/bin/bash
# run_l3.sh <side br|dev> <store> <corpus> <outprefix> [phases] [regex]
R=/Users/havardottestad/Documents/Programming/rdf4j-small-things/.agent/research/GH-0000-wp9-differential
SP=/private/tmp/claude-501/-Users-havardottestad-Documents-Programming-rdf4j-small-things/fe2b5b67-a109-4362-a39f-72d9e6f6dd73/scratchpad
side=$1; store=$2; corpus=$3; out=$4; phases=${5:-cold,seq,postcold}; rx=${6:-.}
mkdir -p $SP/l3tmp/$side-$store
exec java -Xmx6g ${DIFF_JAVA_OPTS} -Dlogback.configurationFile=$R/scripts/logback-quiet.xml -Ddiff.tmp=$SP/l3tmp/$side-$store \
  -cp $R/${DIFF_BUILD:-build}/$side:$(cat $R/results/$side.cp) DiffRunner $corpus $out.$side.$store.tsv $store $phases "$rx"
