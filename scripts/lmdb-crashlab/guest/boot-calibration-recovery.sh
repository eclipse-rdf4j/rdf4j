#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
trap 'rc=$?; printf "calibration recovery failed with exit %s\n" "$rc" > "$RESULTS/calibration-recovery-failed"; exit "$rc"' ERR
for marker in calibration-recovery-done calibration-recovery-failed; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting calibration artifact: $marker" >&2; exit 22; fi
done
java -version > "$RESULTS/calibration-recovery-java-version.txt" 2>&1
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/calibration-recovery-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '67108864' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 64 MiB NBD-backed block device' >&2; exit 23; fi
mkdir -p /mnt/crashlab
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" /mnt/crashlab
python3 - <<'PY'
import hashlib
import pathlib

data = pathlib.Path("/mnt/crashlab/probe.bin").read_bytes()
print(f"recovered_bytes={len(data)} sha256={hashlib.sha256(data).hexdigest()} prefix={data[:16]!r}")
if data != b"A" * 4096:
    raise SystemExit("synced probe was not recovered exactly")
print("PASS synced baseline survived backend restart; volatile overwrite was absent")
PY
umount /mnt/crashlab
date -Is > "$RESULTS/calibration-recovery-done"
sync -f "$RESULTS/calibration-recovery-done"
poweroff
