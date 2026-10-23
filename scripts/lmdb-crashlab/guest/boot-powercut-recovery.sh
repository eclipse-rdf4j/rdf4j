#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
DATA=/mnt/crash-data
RUN_ID="$(cat "$RESULTS/actual-active-recovery-run.txt")"
SCENARIO="$(cat "$RESULTS/actual-powercut-scenario.txt")"
FAIL_MARKER="$RESULTS/actual-recovery-${RUN_ID}-failed"
trap 'rc=$?; printf "power-cut recovery %s failed with exit %s\n" "$RUN_ID" "$rc" > "$FAIL_MARKER"; sync -f "$FAIL_MARKER" 2>/dev/null || true; exit "$rc"' ERR
if [ -z "$RUN_ID" ] || [[ ! "$RUN_ID" =~ ^[12]$ ]]; then
  echo "invalid recovery run id: $RUN_ID" >&2
  exit 22
fi
for marker in "actual-recovery-$RUN_ID-started" "actual-recovery-$RUN_ID-failed" \
  "actual-recovery-$RUN_ID-done" "actual-recovery-$RUN_ID-oracle.json"; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting marker: $marker" >&2; exit 23; fi
done
mkdir "$DATA"
date -Is > "$RESULTS/actual-recovery-$RUN_ID-started"
java -version > "$RESULTS/actual-recovery-$RUN_ID-java-version.txt" 2>&1
javac -version > "$RESULTS/actual-recovery-$RUN_ID-javac-version.txt" 2>&1
uname -a > "$RESULTS/actual-recovery-$RUN_ID-uname.txt"
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/actual-recovery-$RUN_ID-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '134217728' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 128 MiB NBD data device' >&2; exit 24; fi
printf 'actual_data_device=%s\n' "$data_dev" >> "$RESULTS/actual-recovery-$RUN_ID-lsblk.txt"
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" "$DATA"
CP="$(cat "$RESULTS/actual-build-classpath-guest.txt")"
CLASS_DIR="$(mktemp -d /tmp/rdf4j-crashlab-powercut-recovery.XXXXXX)"
javac -cp "$CP" -d "$CLASS_DIR" \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/CrashPowerCutFixtures.java \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/CrashPowerCutOracleMain.java \
  > "$RESULTS/actual-recovery-$RUN_ID-javac.log" 2>&1
java -cp "$CLASS_DIR:$CP" org.eclipse.rdf4j.sail.lmdb.CrashPowerCutOracleMain \
  "$DATA/store" "$RESULTS" "$SCENARIO" "$RUN_ID" \
  > "$RESULTS/actual-recovery-$RUN_ID-oracle.log" 2>&1
sync
umount "$DATA"
date -Is > "$RESULTS/actual-recovery-$RUN_ID-done"
sync -f "$RESULTS/actual-recovery-$RUN_ID-done"
poweroff
