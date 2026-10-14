#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
DATA=/mnt/crash-data
trap 'rc=$?; printf "recovery bootstrap failed with exit %s\n" "$rc" > "$RESULTS/actual-recovery-failed"; exit "$rc"' ERR
for marker in actual-recovery-guest-started actual-recovery-failed actual-lmdb-recovery-done; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting marker: $marker" >&2; exit 22; fi
done
mkdir "$DATA"
date -Is > "$RESULTS/actual-recovery-guest-started"
java -version > "$RESULTS/actual-recovery-java-version.txt" 2>&1
javac -version > "$RESULTS/actual-recovery-javac-version.txt" 2>&1
uname -a > "$RESULTS/actual-recovery-uname.txt"
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/actual-recovery-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '134217728' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 128 MiB NBD data device' >&2; exit 23; fi
printf 'actual_data_device=%s\n' "$data_dev" >> "$RESULTS/actual-recovery-lsblk.txt"
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" "$DATA"
CP="$(cat "$RESULTS/actual-build-classpath-guest.txt")"
CLASS_DIR="$(mktemp -d /tmp/rdf4j-crashlab-classes.XXXXXX)"
javac -cp "$CP" -d "$CLASS_DIR" \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/Rdf4jCrashlabOracle.java \
  > "$RESULTS/actual-recovery-javac.log" 2>&1
java -Drdf4j.crashlab.dataRoot="$DATA" -Drdf4j.crashlab.witnessRoot="$RESULTS" \
  -cp "$CLASS_DIR:$CP" org.eclipse.rdf4j.sail.lmdb.Rdf4jCrashlabOracle \
  verifyNamespaceCut "$DATA/store" "$RESULTS/actual-ack-witness.tsv" \
  "$RESULTS/actual-namespace-witness.tsv" "$RESULTS/actual-lmdb-final-ack" \
  > "$RESULTS/actual-recovery-oracle.log" 2>&1
sync
umount "$DATA"
date -Is > "$RESULTS/actual-lmdb-recovery-done"
sync -f "$RESULTS/actual-lmdb-recovery-done"
poweroff
