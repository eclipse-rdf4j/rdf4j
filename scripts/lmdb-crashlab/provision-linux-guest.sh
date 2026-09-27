#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 6 ]]; then
	printf 'usage: %s SCRATCH_ROOT QEMU QEMU_IMG GENISOIMAGE FIRMWARE_CODE FIRMWARE_VARS_TEMPLATE\n' "$0" >&2
	exit 2
fi

scratch_root=$(realpath -m "$1")
qemu=$(realpath -e "$2")
qemu_img=$(realpath -e "$3")
genisoimage=$(realpath -e "$4")
firmware_code=$(realpath -e "$5")
firmware_vars_template=$(realpath -e "$6")
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

image_root=$scratch_root/image
seed_root=$scratch_root/seed
for directory in "$image_root" "$seed_root"; do
	[[ ! -e $directory && ! -L $directory ]] || {
		printf 'refusing pre-existing guest-provisioning path: %s\n' "$directory" >&2
		exit 1
	}
	mkdir "$directory"
done

image_name=ubuntu-24.04-server-cloudimg-arm64.img
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

guest_image=$scratch_root/ubuntu-arm64-java25.qcow2
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
      exec > >(tee /dev/ttyAMA0) 2>&1
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
      test "$(dpkg --print-architecture)" = arm64
      java -version
      javac -version
      for command in mkfs.ext4 filefrag blkid blockdev lsblk mount umount python3; do command -v "$command"; done
      printf 'CRASHLAB_PROVISIONED\n' > /dev/ttyAMA0
runcmd:
  - [bash, /usr/local/sbin/provision-rdf4j-crashlab]
power_state:
  mode: poweroff
  message: RDF4J crashlab guest provisioning complete
  timeout: 60
  condition: true
EOF
cat > "$seed_root/meta-data" <<'EOF'
instance-id: rdf4j-crashlab-ci-guest
local-hostname: rdf4j-crashlab
EOF
seed_iso=$scratch_root/seed.iso
"$genisoimage" -output "$seed_iso" -volid cidata -joliet -rock "$seed_root" \
	> "$scratch_root/seed-iso.log" 2>&1

serial_log=$scratch_root/provision-serial.log
qemu_log=$scratch_root/provision-qemu.log
set +e
timeout --foreground --signal=TERM --kill-after=30s 1800s "$qemu" \
	-machine virt -accel tcg,thread=multi -cpu max -smp 4 -m 4096 \
	-display none -monitor none -serial "file:$serial_log" -no-reboot \
	-drive "if=pflash,format=raw,unit=0,file=$firmware_code,readonly=on" \
	-drive "if=pflash,format=raw,unit=1,file=$vars_file" \
	-drive "file=$guest_image,format=qcow2,if=none,id=osdisk,cache=writeback" \
	-device virtio-blk-pci,drive=osdisk,bootindex=0 \
	-drive "file=$seed_iso,format=raw,if=none,id=seed,readonly=on" \
	-device virtio-blk-pci,drive=seed \
	-netdev user,id=net0 -device virtio-net-pci,netdev=net0,romfile= \
	> "$qemu_log" 2>&1
qemu_status=$?
set -e
if [[ $qemu_status -ne 0 ]] || ! grep -Fq 'CRASHLAB_PROVISIONED' "$serial_log"; then
	printf 'guest provisioning failed (qemu status %s); see %s and %s\n' \
		"$qemu_status" "$serial_log" "$qemu_log" >&2
	if [[ -s $serial_log ]]; then
		sed -n '1,200p' "$serial_log" >&2
	fi
	if [[ -s $qemu_log ]]; then
		cat "$qemu_log" >&2
	fi
	exit 1
fi

sha256sum "$base_image" "$guest_image" "$firmware_code" "$firmware_vars_template" \
	> "$scratch_root/provisioned-input-sha256.txt"
printf 'Provisioned Ubuntu ARM64 Java 25 guest: %s\n' "$guest_image"
printf 'Serial evidence: %s\n' "$serial_log"
