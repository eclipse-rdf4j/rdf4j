#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
DATA=/mnt/crash-data
trap 'rc=$?; printf "power-cut writer bootstrap failed with exit %s\n" "$rc" > "$RESULTS/actual-powercut-writer-failed"; sync -f "$RESULTS/actual-powercut-writer-failed" 2>/dev/null || true; exit "$rc"' ERR
for marker in actual-powercut-writer-started actual-powercut-writer-failed \
  actual-powercut-writer-controller.log actual-powercut-writer-events.jsonl \
  actual-A-payload-child.tsv actual-A-ack-witness.tsv actual-A-commit-ack.json \
  actual-B-payload-child.tsv actual-B-attempt-witness.tsv actual-cutpoint-child.marker \
  actual-cutpoint-witness.json actual-powercut-writer-failed; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting marker: $marker" >&2; exit 22; fi
done
mkdir "$DATA"
date -Is > "$RESULTS/actual-powercut-writer-started"
java -version > "$RESULTS/actual-powercut-writer-java-version.txt" 2>&1
javac -version > "$RESULTS/actual-powercut-writer-javac-version.txt" 2>&1
uname -a > "$RESULTS/actual-powercut-writer-uname.txt"
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/actual-powercut-writer-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '134217728' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 128 MiB NBD data device' > "$RESULTS/actual-powercut-writer-setup-failed"; exit 23; fi
if blkid -o value -s TYPE "$data_dev" >/dev/null 2>&1; then
  echo "refusing nonblank data device: $data_dev" > "$RESULTS/actual-powercut-writer-setup-failed"
  exit 24
fi
printf 'actual_data_device=%s\n' "$data_dev" >> "$RESULTS/actual-powercut-writer-lsblk.txt"
mkfs.ext4 -F -m 0 -L CRASHLAB-NBD "$data_dev" > "$RESULTS/actual-powercut-mkfs-ext4.log" 2>&1
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" "$DATA"
mkdir "$DATA/store"
CP="$(cat "$RESULTS/actual-build-classpath-guest.txt")"
CLASS_DIR="$(mktemp -d /tmp/rdf4j-crashlab-powercut-classes.XXXXXX)"
export RDF4J_CRASHLAB_CLASS_DIR="$CLASS_DIR"
javac -cp "$CP" -d "$CLASS_DIR" \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/CrashPowerCutFixtures.java \
  /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/CrashPowerCutWriterMain.java \
  > "$RESULTS/actual-powercut-writer-javac.log" 2>&1
python3 /mnt/rdf4j-ro/scripts/lmdb-crashlab/guest/powercut-writer-controller.py
