#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 6 || $# -gt 8 ]]; then
	printf 'usage: %s SCRATCH_ROOT QEMU QEMU_IMG GENISOIMAGE FIRMWARE_CODE FIRMWARE_VARS_TEMPLATE [arm64|x86_64] [tcg|kvm]\n' "$0" >&2
	exit 2
fi

scratch_root=$(realpath -m "$1")
qemu=$(realpath -e "$2")
qemu_img=$(realpath -e "$3")
genisoimage=$(realpath -e "$4")
firmware_code=$(realpath -e "$5")
firmware_vars_template=$(realpath -e "$6")
architecture=${7:-arm64}
acceleration=${8:-tcg}
[[ -d $scratch_root && ! -L $scratch_root ]] || {
	printf 'scratch root must be a real directory: %s\n' "$scratch_root" >&2
	exit 1
}
for input in "$qemu" "$qemu_img" "$genisoimage" "$firmware_code" "$firmware_vars_template"; do
	[[ -f $input && -r $input ]] || {
		printf 'required guest-provisioning input is unavailable: %s\n' "$input" >&2
		exit 1
	}
done

case $architecture in
arm64)
	image_name=ubuntu-24.04-server-cloudimg-arm64.img
	guest_image=$scratch_root/ubuntu-arm64-java25.qcow2
	qemu_machine=virt
	guest_serial=ttyAMA0
	guest_deb_arch=arm64
	case $acceleration in
	tcg)
		qemu_acceleration=tcg,thread=multi
		qemu_cpu=max
		;;
	*)
		printf 'unsupported acceleration for %s: %s\n' "$architecture" "$acceleration" >&2
		exit 2
		;;
	esac
	;;
x86_64)
	image_name=ubuntu-24.04-server-cloudimg-amd64.img
	guest_image=$scratch_root/ubuntu-x86_64-java25.qcow2
	qemu_machine=q35
	guest_serial=ttyS0
	guest_deb_arch=amd64
	case $acceleration in
	kvm)
		qemu_acceleration=kvm
		qemu_cpu=host
		;;
	tcg)
		qemu_acceleration=tcg,thread=multi
		qemu_cpu=max
		;;
	*)
		printf 'unsupported acceleration for %s: %s\n' "$architecture" "$acceleration" >&2
		exit 2
		;;
	esac
	;;
*)
	printf 'unsupported guest architecture: %s\n' "$architecture" >&2
	exit 2
	;;
esac

image_root=$scratch_root/image
seed_root=$scratch_root/seed
for directory in "$image_root" "$seed_root"; do
	[[ ! -e $directory && ! -L $directory ]] || {
		printf 'refusing pre-existing guest-provisioning path: %s\n' "$directory" >&2
		exit 1
	}
	mkdir "$directory"
done

release_url=https://cloud-images.ubuntu.com/releases/24.04/release
base_image=$image_root/$image_name
curl --fail --location --retry 3 --retry-all-errors "$release_url/$image_name" --output "$base_image"
curl --fail --location --retry 3 --retry-all-errors "$release_url/SHA256SUMS" --output "$image_root/SHA256SUMS"
awk -v name="$image_name" '{ file=$2; sub(/^[*]/, "", file); if (file == name) print $1 "  " name }' \
	"$image_root/SHA256SUMS" > "$image_root/SHA256SUMS.selected"
[[ $(wc -l < "$image_root/SHA256SUMS.selected" | tr -d ' ') -eq 1 ]] || {
	printf 'Ubuntu cloud image checksum manifest lacks exactly one %s entry\n' "$image_name" >&2
	exit 1
}
(cd "$image_root" && sha256sum --check SHA256SUMS.selected)

"$qemu_img" create -f qcow2 -F qcow2 -b "$base_image" "$guest_image"
"$qemu_img" resize "$guest_image" 8G
vars_file=$scratch_root/firmware-vars.fd
cp --no-clobber "$firmware_vars_template" "$vars_file"

cat > "$seed_root/user-data" <<'EOF'
#cloud-config
growpart:
  mode: auto
  devices: ['/']
resize_rootfs: true
write_files:
  - path: /usr/local/sbin/provision-rdf4j-crashlab
    owner: root:root
    permissions: '0755'
    content: |
      #!/usr/bin/env bash
      set -euo pipefail
      exec > >(tee /dev/__GUEST_SERIAL__) 2>&1
      apt-get update
      DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ca-certificates curl gnupg e2fsprogs util-linux
      install -d -m 0755 /etc/apt/keyrings
      curl --fail --location --retry 3 https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor --yes --output /etc/apt/keyrings/adoptium.gpg
      chmod 0644 /etc/apt/keyrings/adoptium.gpg
      printf '%s\n' 'deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb noble main' \
        > /etc/apt/sources.list.d/adoptium.list
      apt-get update
      DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends temurin-25-jdk
      test "$(dpkg --print-architecture)" = __GUEST_DEB_ARCH__
      java -version
      javac -version
      for command in mkfs.ext4 filefrag blkid blockdev lsblk mount umount python3; do command -v $command; done
      printf 'CRASHLAB_PROVISIONED\n' > /dev/__GUEST_SERIAL__
runcmd:
  - [bash, /usr/local/sbin/provision-rdf4j-crashlab]
power_state:
  mode: poweroff
  message: RDF4J crashlab guest provisioning complete
  timeout: 60
  condition: true
EOF
sed "s/__GUEST_SERIAL__/$guest_serial/g; s/__GUEST_DEB_ARCH__/$guest_deb_arch/g" \
	"$seed_root/user-data" > "$seed_root/user-data.rendered"
mv "$seed_root/user-data.rendered" "$seed_root/user-data"
cat > "$seed_root/meta-data" <<'EOF'
instance-id: rdf4j-crashlab-ci-guest
local-hostname: rdf4j-crashlab
EOF
seed_iso=$scratch_root/seed.iso
"$genisoimage" -output "$seed_iso" -volid cidata -joliet -rock "$seed_root" \
	> "$scratch_root/seed-iso.log" 2>&1

serial_log=$scratch_root/provision-serial.log
qemu_log=$scratch_root/provision-qemu.log
qmp_log=$scratch_root/provision-qmp.log
qmp_socket_name=provision-qmp.sock
qmp_socket=$scratch_root/$qmp_socket_name
[[ ! -e $qmp_socket && ! -L $qmp_socket ]] || {
	printf 'refusing pre-existing QMP socket path: %s\n' "$qmp_socket" >&2
	exit 1
}

set +e
(
	cd "$scratch_root"
	python3 - "$qmp_socket_name" "$qemu_log" "$qemu" \
	-machine "$qemu_machine" -accel "$qemu_acceleration" -cpu "$qemu_cpu" -smp 4 -m 4096 \
	-display none -monitor none -serial "file:$serial_log" -no-reboot -no-shutdown \
	-qmp "unix:$qmp_socket_name,server=on,wait=off" \
	-drive "if=pflash,format=raw,unit=0,file=$firmware_code,readonly=on" \
	-drive "if=pflash,format=raw,unit=1,file=$vars_file" \
	-drive "file=$guest_image,format=qcow2,if=none,id=osdisk,cache=writeback" \
	-device virtio-blk-pci,drive=osdisk,bootindex=0 \
	-drive "file=$seed_iso,format=raw,if=none,id=seed,readonly=on" \
	-device virtio-blk-pci,drive=seed \
	-netdev user,id=net0 -device virtio-net-pci,netdev=net0,romfile= \
	> "$qmp_log" 2>&1 <<'PY'
import json
import socket
import subprocess
import sys
import time

qmp_path, qemu_log, *command = sys.argv[1:]
deadline = time.monotonic() + 1800


def read_message(stream, connection):
	remaining = deadline - time.monotonic()
	if remaining <= 0:
		raise TimeoutError("timed out waiting for guest shutdown")
	connection.settimeout(remaining)
	line = stream.readline()
	if not line:
		raise RuntimeError("QMP connection closed before guest shutdown")
	return json.loads(line)


def execute(stream, connection, command_name):
	request_id = f"crashlab-{command_name}"
	stream.write((json.dumps({"execute": command_name, "id": request_id}) + "\r\n").encode())
	while True:
		message = read_message(stream, connection)
		if message.get("id") != request_id:
			continue
		if "error" in message:
			raise RuntimeError(f"QMP {command_name} failed: {message['error']}")
		return message.get("return")


with open(qemu_log, "wb") as qemu_output:
	process = subprocess.Popen(command, stdout=qemu_output, stderr=subprocess.STDOUT)
	connection = None
	try:
		while True:
			if process.poll() is not None:
				raise RuntimeError(f"QEMU exited before QMP was ready (status {process.returncode})")
			if time.monotonic() >= deadline:
				raise TimeoutError("QEMU did not expose QMP within 30 minutes")
			try:
				connection = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
				connection.connect(qmp_path)
				break
			except (FileNotFoundError, ConnectionRefusedError):
				if connection is not None:
					connection.close()
				connection = None
				time.sleep(0.1)
		stream = connection.makefile("rwb", buffering=0)
		greeting = read_message(stream, connection)
		if "QMP" not in greeting:
			raise RuntimeError(f"unexpected QMP greeting: {greeting}")
		execute(stream, connection, "qmp_capabilities")
		print("QMP ready; waiting for guest-originated SHUTDOWN", flush=True)
		while True:
			message = read_message(stream, connection)
			if message.get("event") != "SHUTDOWN":
				continue
			data = message.get("data", {})
			print(f"QMP SHUTDOWN event: {json.dumps(data, sort_keys=True)}", flush=True)
			if data.get("guest") is not True:
				raise RuntimeError("QEMU shutdown was not initiated by the guest")
			break
		execute(stream, connection, "quit")
		status = process.wait(timeout=30)
		if status != 0:
			raise RuntimeError(f"QEMU exited with status {status} after guest shutdown")
		print("QEMU exited cleanly after guest-originated SHUTDOWN", flush=True)
	except BaseException:
		if process.poll() is None:
			process.terminate()
			try:
				process.wait(timeout=10)
			except subprocess.TimeoutExpired:
				process.kill()
				process.wait()
		raise
	finally:
		if connection is not None:
			connection.close()
PY
)
qmp_status=$?
set -e

if [[ $qmp_status -ne 0 ]] || ! grep -Fq 'CRASHLAB_PROVISIONED' "$serial_log"; then
	printf 'guest provisioning failed (QMP supervisor status %s); see %s, %s, and %s\n' \
		"$qmp_status" "$serial_log" "$qemu_log" "$qmp_log" >&2
	if [[ -s $serial_log ]]; then
		sed -n '1,200p' "$serial_log" >&2
	fi
	if [[ -s $qemu_log ]]; then
		cat "$qemu_log" >&2
	fi
	if [[ -s $qmp_log ]]; then
		cat "$qmp_log" >&2
	fi
	exit 1
fi

sha256sum "$base_image" "$guest_image" "$firmware_code" "$firmware_vars_template" \
	> "$scratch_root/provisioned-input-sha256.txt"
printf 'Provisioned Ubuntu %s Java 25 guest (%s): %s\n' "$architecture" "$acceleration" "$guest_image"
printf 'Serial evidence: %s\n' "$serial_log"
