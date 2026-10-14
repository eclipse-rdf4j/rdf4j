#!/bin/bash
set -euo pipefail
set -o noclobber
RESULTS=/mnt/run-results
trap 'rc=$?; printf "calibration phase1 failed with exit %s\n" "$rc" > "$RESULTS/calibration-phase1-failed"; exit "$rc"' ERR
for marker in phase1_synced_ready phase1_unsynced_ready allow_unsynced allow_guest_exit raw-overwrite-offset calibration-phase1-failed; do
  if [ -e "$RESULTS/$marker" ]; then echo "refusing preexisting calibration artifact: $marker" >&2; exit 22; fi
done
java -version > "$RESULTS/calibration-java-version.txt" 2>&1
uname -a > "$RESULTS/calibration-uname.txt"
lsblk -b -o NAME,TYPE,SIZE,FSTYPE,LABEL > "$RESULTS/calibration-lsblk.txt"
data_dev=''
for candidate in /dev/vd[b-z]; do
  if [ -b "$candidate" ] && [ "$(blockdev --getsize64 "$candidate" 2>/dev/null || true)" = '67108864' ]; then
    data_dev="$candidate"
    break
  fi
done
if [ -z "$data_dev" ]; then echo 'FAIL no 64 MiB NBD-backed block device' >&2; exit 23; fi
if blkid -o value -s TYPE "$data_dev" >/dev/null 2>&1; then
  echo "refusing nonblank calibration device: $data_dev" >&2
  exit 24
fi
printf 'calibration_data_device=%s\n' "$data_dev" >> "$RESULTS/calibration-lsblk.txt"
mkfs.ext4 -F -m 0 -E lazy_itable_init=0,lazy_journal_init=0 -L CRASHLAB "$data_dev" \
  > "$RESULTS/calibration-mkfs-ext4.log" 2>&1
mkdir -p /mnt/crashlab
mount -t ext4 -o data=ordered,barrier=1 "$data_dev" /mnt/crashlab
python3 - <<'PY'
import os
path = "/mnt/crashlab/probe.bin"
fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
try:
    os.write(fd, b"A" * 4096)
    os.fsync(fd)
finally:
    os.close(fd)
directory = os.open("/mnt/crashlab", os.O_RDONLY)
try:
    os.fsync(directory)
finally:
    os.close(directory)
PY
sync -f /mnt/crashlab/probe.bin
sha256sum /mnt/crashlab/probe.bin > "$RESULTS/calibration-probe-baseline.sha256"
extent_range="$(filefrag -v -b4096 /mnt/crashlab/probe.bin | awk '$1 == "0:" { print $4; exit }')"
physical_block="${extent_range%%..*}"
if ! [[ "$physical_block" =~ ^[0-9]+$ ]]; then
  echo "could not resolve probe extent from filefrag: $extent_range" >&2
  exit 25
fi
physical_offset=$((physical_block * 4096))
printf '%s\n' "$physical_offset" > "$RESULTS/raw-overwrite-offset"
echo "probe data block=$physical_block byte_offset=$physical_offset" >> "$RESULTS/calibration-lsblk.txt"
umount /mnt/crashlab
date -Is > "$RESULTS/phase1_synced_ready"
while [ ! -e "$RESULTS/allow_unsynced" ]; do sleep 0.05; done
dd if=/dev/zero of="$data_dev" bs=4096 seek="$physical_block" count=1 oflag=direct status=none
date -Is > "$RESULTS/phase1_unsynced_ready"
while [ ! -e "$RESULTS/allow_guest_exit" ]; do sleep 0.05; done
poweroff -f
