# LMDB crash-durability harness

This directory contains the test-only volatile NBD device model adapted from
the supplied RDF4J CrashLab archive. The archive's full MIT license is in
`LICENSE`. The model is not production storage and must only receive regular,
disposable image files created for a test run.

The disk model separates writes acknowledged to a guest from durable sectors:
ordinary writes enter a volatile cache, successful FLUSH persists every earlier
cached sector, and FUA persists only the addressed write range. A cut fences
further I/O and can drop, randomly retain, or retain all dirty sectors. The
wire-level tests exercise negotiation, FLUSH, FUA scoping, disconnect behavior,
and a lying-flush negative control. The disk-model tests also interrupt a
multi-sector FLUSH and check sector-prefix outcomes.

Run runner-safety, backend-model, NBD wire-protocol, and power-cut contract
tests with the canonical script:

```sh
cd scripts/lmdb-crashlab
./run-tests.sh
```

The exact default unittest command is
`python3 -m unittest -v test_runner_common test_volatile_nbd test_powercut_campaign test_ci_gate`.
It currently runs 50 tests, including report rejection controls for missing,
skipped, failed, duplicate, wrong-scenario, and incomplete evidence.

The wire tests bind a localhost TCP port. They need a platform that permits
loopback sockets; the RDF4J test process itself does not need Python.

## RDF4J process-crash regression

The repository test is
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCrashRecoveryTest.java`.
It runs real RDF4J `LmdbStore` transactions with `forceSync=true`, records each
returned commit in an external forced witness, kills only its own child JVM,
and checks the recovered complete RDF state and public subject, predicate,
object, context, namespace, and SPARQL reads. The two deterministic in-progress
cut tests stop inside the current namespace-to-triple and autogrow-replay
boundaries. They assert that interrupted work recovers atomically and remain in
the required CI selection.

Use the project's runner for the process test on a normal writable checkout:

```sh
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCrashRecoveryTest \
  --module core/sail/lmdb --retain-logs
```

The test accepts two optional properties for a guest run:

- `rdf4j.crashlab.dataRoot` places each LMDB database under that directory.
- `rdf4j.crashlab.witnessRoot` places external witness files under that directory.

When `dataRoot` is set, the test requires `witnessRoot` to resolve to a
different mounted filesystem. This prevents a durability campaign from
mistaking a second directory on the same simulated disk for an independent
witness. For example, after mounting a disposable NBD-backed ext4 filesystem
at `/mnt/crash-data` and an independent OS/host share at `/mnt/witness`:

```sh
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCrashRecoveryTest \
  --module core/sail/lmdb --retain-logs -- \
  -Drdf4j.crashlab.dataRoot=/mnt/crash-data \
  -Drdf4j.crashlab.witnessRoot=/mnt/witness
```

This selector proves process death only. It does not remove the virtual data
device or prove power-loss behavior; the parent test process and host kernel
remain alive.

## NBD and guest storage contract

The backend can serve an existing test-owned, nonempty, sector-aligned regular
file. It refuses symlinks and block devices. Keep its event log, control socket,
witness, OS disk, and preserved crash image outside the NBD data image. Example
backend launch on a disposable Linux host:

```sh
python3 scripts/lmdb-crashlab/volatile_nbd.py serve \
  --image /tmp/rdf4j-crashlab-run/data.raw \
  --control /tmp/rdf4j-crashlab-run/control.sock \
  --port 10809 \
  --log /tmp/rdf4j-crashlab-run/nbd-events.jsonl
```

Attach it to the disposable Linux guest with QEMU writeback and device write
cache enabled, while retaining guest FLUSH/FUA support:

```sh
-drive file=nbd://127.0.0.1:10809/crashlab,format=raw,if=none,id=crashdisk,cache=writeback,werror=report,rerror=report
-device virtio-blk-pci,drive=crashdisk,write-cache=on,serial=CRASHLAB-NBD
```

Use a separately created qcow2 overlay for the guest OS. Do not use global
QEMU `-snapshot`, `cache=unsafe`, or `ignore_flush=true` in the valid baseline.
`--ignore-flush` is reserved for an explicitly labelled false-device
sensitivity run. The valid baseline must retain QEMU flushes and show backend
FLUSH/FUA events before any recovered result is called durable.

Before the database campaign, calibrate through the actual guest filesystem:

1. Create ext4 only on the newly created disposable NBD data disk, mount it,
   write a probe, and `fsync` both the file and containing directory.
2. Unmount cleanly and verify the probe survived a backend cut/restart.
3. Overwrite the probe's allocated block with one aligned raw block write,
   without a later FLUSH/FUA or guest shutdown; cut the backend and guest, then
   restart both and confirm the prior synced probe remains while the volatile
   overwrite is absent.
4. Preserve the NBD image and independent witness before mounting/reopening for
   recovery. Record the event stream and the exact QEMU command/config.

The checked-in Python suite validates the backend state machine and NBD
wire-protocol implementation. A guest ext4 calibration additionally proves
that QEMU, virtio, and the filesystem actually propagate FLUSH/FUA to this
backend. Neither test tier simulates loss of the physical host, host filesystem,
SSD controller, or device firmware. The external campaign must report only the
guest/device-backend fault model it actually exercised.

## Reproducible QEMU guest tier

The host runners in this directory reproduce ext4 calibration, the acknowledged
RDF4J writer/device cut/reopen workflow, and recovery-only checks from a
preserved image. They do not run Maven or modify the checkout. They share the
checkout and `.m2_repo` into the guest through read-only 9p mounts. Use a
quiescent build: no Maven process may rewrite `target` or `.m2_repo` while a
guest is reading it.

The local runner supports an ARM64 guest with `qemu-system-aarch64` and TCG, or
an x86_64 guest with `qemu-system-x86_64` and KVM; both also need `qemu-img`,
firmware, and either macOS `hdiutil` or Linux `genisoimage` for cloud-init seed
ISOs. A provisioned guest must contain Java 25 (`java` and `javac`), cloud-init,
Python 3, ext4/e2fsprogs tools (`mkfs.ext4`, `filefrag`, `blkid`, `blockdev`,
`lsblk`), and the 9p and virtio block drivers. The runner creates only new
per-run overlays, seed ISOs, raw data images, and logs beneath the explicitly
supplied scratch root. It refuses a scratch root that already exists. It never
formats or mounts a host device, installs host packages, or removes an earlier
run. The supplied OS image and firmware files are inputs; each guest boot uses
a fresh qcow2 overlay and copied UEFI variables.

### Required GitHub pull-request gate

`.github/workflows/lmdb-qemu-durability.yml` runs unconditionally for every
`pull_request` on `ubuntu-24.04`. It adds a read/write ACL for the job user's
UID on `/dev/kvm`. The preflight captures process and device metadata, verifies
the KVM API and creates/closes a test VM, then records a direct-user QEMU file
open trace and available AppArmor/kernel/audit denial records. Its tested
`qemu_kvm_preflight.py` controller handles QEMU child exits and the QMP greeting
with bounded waits; failure of the optional direct-user diagnostic still leads
to the required group-context probe. For that required probe, `sudo -n`
launches `setpriv`, which drops every UID to the job user's numeric UID, uses
the device's primary GID, retains the job's supplementary groups plus that
device group, clears Linux capabilities, and enables `no_new_privs`. Before
QEMU starts, the controller verifies all four real/effective/saved/filesystem
UID and GID values from `/proc/self/status`. QEMU must then stay QMP-responsive
while configured with `q35`, `accel=kvm`, and `cpu=host`; that exact wrapper is
used for guest provisioning and each campaign. The preflight artifact also
captures available udev device data, rules mentioning KVM, and udev journal
entries. Missing or unusable KVM fails the job rather than falling back to
slow TCG or skipping the guest tier. It provisions an Ubuntu
x86_64 Java 25 guest with OVMF and runs the actual Linux guest FLUSH/FUA
calibration, acknowledged namespace-only commit, and all three checked-in
transaction cut campaigns. Each cut campaign reopens the same preserved image
twice and compares the full public-oracle outcome and state hash. The job also
runs the crashlab Python tests and explicitly selects the Java test classes
below; a report validator fails if any selected class is missing, ran zero
tests, failed, errored, or skipped a test. The artifact step always retains
diagnostic logs, NBD traces, guest serial output, campaign reports, and Maven
test reports without changing the job result; regenerable OS images and
overlays are excluded.

The required Java selection is `LmdbCrashRecoveryTest`,
`LmdbStoreFlushReproductionTest`, `TripleStoreAutoGrowTest`,
`ValueStoreTermIndexRecoveryTest`, `LmdbConstructorCleanupTest`,
`PersistentSetFactoryTransactionOwnershipTest`,
`LmdbSnapshotValueLifetimeTest`, `LmdbValueRetirementRecoveryTest`,
`TxnMutationJournalTest`, and the `LmdbStoreModelLifecycleIT` integration test.
The gate requires zero skipped tests in each selected report. This does not
claim that every test in the LMDB module is enabled: separate long-running
theme/benchmark and sketch-placement cases are intentionally `@Disabled`,
`SailSourceModelTest` and `LongMultithreadedTransactions` are disabled, the
optional `LmdbRegressionPlanCaptureTest` is property-gated, and one oversized
native-map probe in `TripleStoreInitializationTest` uses a platform assumption.
Those unrelated exclusions remain outside this focused required gate.

### Recreate the Java 25 guest and exact test classpath

The recorded runs used the official Ubuntu Server 26.04 ARM64 cloud image from
the immutable `release-20260717` directory. Download the image and checksum
file, then verify the image before using it:

```sh
setup_root=$(mktemp -d /tmp/rdf4j-crashlab-setup.XXXXXX)
image_name=ubuntu-26.04-server-cloudimg-arm64.img
release_url=https://cloud-images.ubuntu.com/releases/server/26.04/release-20260717
curl -fL "$release_url/$image_name" -o "$setup_root/$image_name"
curl -fL "$release_url/SHA256SUMS" -o "$setup_root/SHA256SUMS"
grep -E " [*]?$image_name$" "$setup_root/SHA256SUMS" \
  | (cd "$setup_root" && shasum -a 256 -c -)
```

The verified ARM64 image SHA-256 is
`592fc4747816a381241b79d04a4545a8da7bd75779ed7690d74763d630a46df4`.
The provisioned qcow2 used for the campaigns was created as an overlay from
that read-only base and has SHA-256
`4319c69689fc32f70287d462f0f564acecbca385b6e7af8c22695012a1a56afd`.

To create a fresh provisioned guest, make a qcow2 overlay and a NoCloud seed
containing the following `user-data` and `meta-data` files. Boot the overlay
with the seed as a read-only drive, QEMU's AArch64 UEFI code/vars pflash
drives, and a user-mode network interface so cloud-init can install packages.
The provisioned guest used `openjdk-25-jdk` 25.0.4.1, Maven 3.9.12,
`e2fsprogs`, and `openssh-server`; package versions can change with Ubuntu's
repositories, so record the installed versions in the guest log.

The following commands create the cloud-init seed, overlay, and NoCloud ISO
on macOS. The firmware paths below use the installed Homebrew QEMU files. The
guest runs cloud-init's package install and powers itself off when complete.
Run the download and provisioning blocks in the same shell so `setup_root` and
`image_name` remain set. On Linux, create the equivalent NoCloud seed with
`cloud-localds "$setup_root/seed.iso" "$setup_root/seed/user-data" "$setup_root/seed/meta-data"`
from the cloud-image-utils package.

```sh
firmware_directory="$(brew --prefix qemu)/share/qemu"
firmware_code="$firmware_directory/edk2-aarch64-code.fd"
firmware_vars_template="$firmware_directory/edk2-arm-vars.fd"
mkdir "$setup_root/seed"
cat > "$setup_root/seed/user-data" <<'EOF'
#cloud-config
package_update: true
package_upgrade: false
packages:
  - openjdk-25-jdk
  - maven
  - e2fsprogs
  - openssh-server
growpart:
  mode: auto
  devices: ['/']
resize_rootfs: true
runcmd:
  - [bash, -lc, "java -version 2>&1; javac -version 2>&1; dpkg-query -W openjdk-25-jdk maven e2fsprogs"]
power_state:
  mode: poweroff
  message: Crashlab guest provisioning complete
  timeout: 30
  condition: true
EOF
cat > "$setup_root/seed/meta-data" <<'EOF'
instance-id: rdf4j-crashlab-java25
local-hostname: rdf4j-crashlab
EOF
qemu-img create -f qcow2 -F qcow2 -b "$setup_root/$image_name" \
  "$setup_root/ubuntu-java25.qcow2"
hdiutil makehybrid -o "$setup_root/seed.iso" -hfs -joliet -iso \
  -default-volume-name cidata "$setup_root/seed"
cp "$firmware_vars_template" "$setup_root/firmware-vars.fd"
qemu-system-aarch64 -machine virt -accel hvf -cpu host -smp 4 -m 4096 \
  -display none -monitor none -serial "file:$setup_root/provision-serial.log" \
  -no-reboot \
  -drive "if=pflash,format=raw,unit=0,file=$firmware_code,readonly=on" \
  -drive "if=pflash,format=raw,unit=1,file=$setup_root/firmware-vars.fd" \
  -drive "file=$setup_root/ubuntu-java25.qcow2,format=qcow2,if=none,id=osdisk,cache=writeback" \
  -device virtio-blk-pci,drive=osdisk,bootindex=0 \
  -drive "file=$setup_root/seed.iso,format=raw,if=none,id=seed,readonly=on" \
  -device virtio-blk-pci,drive=seed \
  -netdev user,id=net0 -device virtio-net-pci,netdev=net0
```

Keep the base image, seed, writable qcow2 overlay, and copied UEFI vars file in
the test-owned scratch directory. Campaigns then use the provisioned image as
`--os-base-image` and create separate per-boot overlays, so provisioning
writes never mix with the faulted NBD data image.

After the LMDB test classes have been compiled, create the one-line host
classpath expected by the runner. This Maven dependency-plugin command
includes test-scope dependencies, including the `lwjgl-lmdb` Linux ARM64
native jar; the runner maps the checkout and `.m2_repo` read-only and removes
native jars for other platforms when it writes the guest classpath.

```sh
classpath_root=$(mktemp -d /tmp/rdf4j-crashlab-classpath.XXXXXX)
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -DincludeScope=test -Dmdep.outputFile="$classpath_root/dependencies.txt"
printf '%s:' "$PWD/core/sail/lmdb/target/test-classes" \
  "$PWD/core/sail/lmdb/target/classes" > "$classpath_root/host-classpath.txt"
tr -d '\n' < "$classpath_root/dependencies.txt" >> "$classpath_root/host-classpath.txt"
printf '\n' >> "$classpath_root/host-classpath.txt"
grep -q 'lwjgl-lmdb-[^:]*-natives-linux-arm64.jar' "$classpath_root/host-classpath.txt"
```

If the plugin is not yet in `.m2_repo`, run the same Maven command once without
`-o`, then return to offline mode. The generated classpath file must remain
one line and must be passed unchanged to `--host-classpath-file`; do not add an
empty trailing classpath element.

Run the filesystem/device calibration first, choosing an unused pair of TCP
ports and a new output path:

```sh
python3 scripts/lmdb-crashlab/run_calibration.py \
  --scratch-root /tmp/rdf4j-crashlab-calibration-run-unique \
  --repo-root "$PWD" \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port-base 12080
```

The calibration writes and fsyncs a probe on guest ext4, checks the backend's
FLUSH/FUA log, then issues one direct unflushed overwrite and cuts the virtual
device. On restart, the guest must recover the synced probe and reject the
volatile overwrite. `ext4-crash-calibration-report.json`, both NBD event logs,
the preserved pre-recovery image, guest serial logs, and exact QEMU commands
remain under the new scratch root.

For an end-to-end workload, first capture the exact one-line host classpath
used by the already compiled LMDB test build. It must include
`core/sail/lmdb/target/test-classes`, `target/classes`, and its dependencies
from `.m2_repo`; the runner checks Java class major 69 and maps only repository
and `.m2_repo` paths into the guest. Then run:

```sh
python3 scripts/lmdb-crashlab/run_campaign.py \
  --scratch-root /tmp/rdf4j-crashlab-campaign-run-unique \
  --repo-root "$PWD" \
  --host-classpath-file /path/to/actual-build-classpath-host.txt \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port-base 12090
```

The end-to-end runner opens the current compiled RDF4J classes and `.m2_repo`
read-only, provisions a fresh ext4 data image inside the scratch root, records
each acknowledged RDF4J commit in an fsynced independent witness, cuts the
volatile NBD backend immediately after the witnessed namespace-only commit,
and runs the public recovery/index/context/SPARQL oracle. It returns status 2
when the oracle completes but reports findings; inspect
`results/actual-campaign-report.json` and
`results/actual-recovery-oracle.log` rather than treating process exit alone as
the result.

To rerun only the oracle from an existing preserved 128 MiB image, use
`run_recovery_only.py` with a fresh scratch root and the source image, ACK
witness, namespace witness, and final-ACK marker as explicit read-only inputs.
The runner first copies and hashes the image and witness into its new output
root, then attaches only the copy to QEMU.

```sh
python3 scripts/lmdb-crashlab/run_recovery_only.py \
  --scratch-root /tmp/rdf4j-crashlab-recovery-run-unique \
  --repo-root "$PWD" \
  --host-classpath-file /path/to/actual-build-classpath-host.txt \
  --source-image /path/to/preserved/pre-recovery-image/data.raw \
  --source-witness /path/to/preserved/actual-ack-witness.tsv \
  --source-namespace-witness /path/to/preserved/actual-namespace-witness.tsv \
  --source-final-ack /path/to/preserved/actual-lmdb-final-ack \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port 12100
```

All guest data drives use QEMU `cache=writeback` and virtio `write-cache=on`;
the NBD export advertises FLUSH and FUA. The runners reject `-snapshot`, unsafe
cache settings, and flush bypass flags. A successful result establishes only
the behavior of the Linux guest, ext4, QEMU/virtio, and the checked-in volatile
NBD device model. It does not establish physical Mac power loss, APFS behavior,
host-kernel loss, or SSD/controller/firmware-cache durability.

## Three deterministic transaction power-cut campaigns

`run_powercut_campaign.py` uses the current compiled LMDB test artifacts and
the Linux ARM64 native library from `.m2_repo`; it does not run Maven or edit
the checkout. A helper compiled inside the guest uses a tiny 40 KiB TripleStore
map for the replay campaign. The independent writable 9p results share records
the complete A transaction only after its commit returns, then fsyncs the exact
attempted B operation payload before the guest applies it. The child fsyncs a
`B_COMMIT_INVOKED` marker when the connection enters its commit lifecycle. This
distinguishes an attempted transaction that has entered commit from a B payload
that was only staged and never submitted for commit.

Each run first asks the NBD backend to fence the device and drop volatile
sectors while the guest is still alive, then abruptly stops and reaps the QEMU
process group. If the backend cut fails, the runner still stops the guest and
propagates the original cut error. It then saves and hashes the pre-recovery raw
image and every external witness, and runs the public RDF4J recovery oracle
twice. Each recovery starts from a separate writable copy of the same immutable
preserved image. The exact public RDF state, explicit/inferred visibility,
namespace map, and statement index lookups are checked. Both recoveries must
return the same allowed transaction outcome and complete-state hash.

Previously completed local externally orchestrated guest campaigns used the
older stop-QEMU-before-cut ordering. Their results remain evidence for their
recorded outcomes, but do not validate this fence-first boundary. No historical
GitHub Actions run reached the real campaign tier. The CI campaigns must run
calibration and all three cuts with the current ordering.

The contracts differ by boundary:

| Scenario | Cut boundary | Recovery contract |
| --- | --- | --- |
| `mixed-replay-before-native-commit` | After the complete replay of B's mixed mutation journal inside `SailConnection.commit()`, before the authoritative TripleStore transaction commits | Exactly A; B commit-invoked is present and native-commit-returned is absent |
| `dictionary-before-triple-commit` | After the dictionary commit returns, while `TripleStore.commit()` is paused before native RDF/namespace publication | Exactly A; B was invoked but native commit did not return |
| `commit-returned-before-ack` | After `SailConnection.commit()` returns, before any B acknowledgment witness is written | Exactly A+B; a returned force-synchronous commit must survive even when the separate acknowledgment is absent |

The replay B includes deletion, delete/re-add of an A statement, promotion of
an inferred statement to explicit, a namespace update, nested quoted-term data
in A, and a 20,000-statement context-varied growth payload. The other two B
transactions retain the delete/re-add, promotion, dictionary additions, and
namespace update with a smaller 64-statement payload. The cutpoint witness
records `B_acknowledged=false`; the guest does not create a B acknowledgment
file in any scenario.

Use a fresh scratch root and port pair for each invocation. The input paths are
the same provisioned guest images described above:

```sh
python3 scripts/lmdb-crashlab/run_powercut_campaign.py \
  --scenario mixed-replay-before-native-commit \
  --scratch-root /tmp/rdf4j-crashlab-replay-cut-unique \
  --repo-root "$PWD" \
  --host-classpath-file /path/to/actual-build-classpath-host.txt \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port-base 12110

python3 scripts/lmdb-crashlab/run_powercut_campaign.py \
  --scenario dictionary-before-triple-commit \
  --scratch-root /tmp/rdf4j-crashlab-dictionary-cut-unique \
  --repo-root "$PWD" \
  --host-classpath-file /path/to/actual-build-classpath-host.txt \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port-base 12120

python3 scripts/lmdb-crashlab/run_powercut_campaign.py \
  --scenario commit-returned-before-ack \
  --scratch-root /tmp/rdf4j-crashlab-returned-cut-unique \
  --repo-root "$PWD" \
  --host-classpath-file /path/to/actual-build-classpath-host.txt \
  --os-base-image /path/to/provisioned-ubuntu-arm64-java25.qcow2 \
  --firmware-code /path/to/edk2-aarch64-code.fd \
  --firmware-vars-template /path/to/edk2-arm-vars.fd \
  --qemu "$(command -v qemu-system-aarch64)" \
  --qemu-img "$(command -v qemu-img)" \
  --hdiutil "$(command -v hdiutil)" \
  --port-base 12130
```

Each scratch root retains `results/actual-powercut-campaign-report.json`, the
writer and both recovery logs, NBD event streams, exact QEMU command records,
the immutable `results/pre-recovery-image/data.raw`, and SHA-256 records for
the image and independent witnesses. The first two cutpoints require exactly
A because their hooks stop before the authoritative native commit. The replay
cut has entered `SailConnection.commit()` but has not published the authoritative
TripleStore transaction. The third has no external B acknowledgment; therefore
it uses an unknown-outcome oracle that accepts only a complete A or complete
A+B state and rejects every partial state. The repeated-recovery comparison prevents treating two different
outcomes from the same image as stable recovery behavior.
