#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
DATA=/mnt/crash-data
trap 'rc=$?; printf "writer bootstrap failed with exit %s\n" "$rc" > "$RESULTS/actual-writer-failed"; exit "$rc"' ERR
for marker in actual-writer-guest-started actual-writer-setup-failed actual-writer-failed \
  actual-lmdb-final-ack actual-ack-witness.tsv actual-namespace-witness.tsv \
  actual-writer-controller.log actual-writer-controller-events.jsonl; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting marker: $marker" >&2; exit 22; fi
done
mkdir "$DATA"
date -Is > "$RESULTS/actual-writer-guest-started"
java -version > "$RESULTS/actual-writer-java-version.txt" 2>&1
javac -version > "$RESULTS/actual-writer-javac-version.txt" 2>&1
uname -a > "$RESULTS/actual-writer-uname.txt"
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/actual-writer-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '134217728' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 128 MiB NBD data device' > "$RESULTS/actual-writer-setup-failed"; exit 23; fi
if blkid -o value -s TYPE "$data_dev" >/dev/null 2>&1; then
  echo "refusing nonblank data device: $data_dev" > "$RESULTS/actual-writer-setup-failed"
  exit 24
fi
printf 'actual_data_device=%s\n' "$data_dev" >> "$RESULTS/actual-writer-lsblk.txt"
mkfs.ext4 -F -m 0 -L CRASHLAB-NBD "$data_dev" > "$RESULTS/actual-mkfs-ext4.log" 2>&1
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" "$DATA"
mkdir "$DATA/store"
CP="$(cat "$RESULTS/actual-build-classpath-guest.txt")"
CLASS_DIR="$(mktemp -d /tmp/rdf4j-crashlab-classes.XXXXXX)"
export RDF4J_CRASHLAB_CLASS_DIR="$CLASS_DIR"
javac -cp "$CP" -d "$CLASS_DIR" \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/Rdf4jCrashlabOracle.java \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/CrashPostBaselineWriterMain.java \
  > "$RESULTS/actual-writer-javac.log" 2>&1
java -cp "$CLASS_DIR:$CP" org.eclipse.rdf4j.sail.lmdb.Rdf4jCrashlabOracle expected "$RESULTS" \
  > "$RESULTS/actual-expected-witness.log" 2>&1
python3 /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/writer-controller.py
