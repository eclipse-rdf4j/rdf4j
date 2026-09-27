#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$script_dir"
exec python3 -m unittest -v test_runner_common test_volatile_nbd test_powercut_campaign test_ci_gate test_package_ci_artifact test_qemu_kvm_preflight test_process_lifecycle
