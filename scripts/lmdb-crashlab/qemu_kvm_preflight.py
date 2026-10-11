#!/usr/bin/env python3
"""Bounded, diagnostic QEMU/KVM process preflight for the crashlab workflow."""

import argparse
import fcntl
import json
import os
import signal
import socket
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path


KVM_GET_API_VERSION = 0xAE00
KVM_CREATE_VM = 0xAE01
QEMU_ARGUMENTS = (
    "-machine",
    "q35,accel=kvm",
    "-cpu",
    "host",
    "-m",
    "64",
    "-nodefaults",
    "-display",
    "none",
    "-S",
    "-monitor",
    "none",
    "-serial",
    "none",
)


class ProbeFailure(RuntimeError):
    """A required forced-KVM QEMU process did not pass its bounded QMP check."""


@dataclass(frozen=True)
class ProbeOutcome:
    label: str
    passed: bool
    detail: str
    status: int | None


class QmpClient:
    def __init__(self, sock):
        self.sock = sock
        self.stream = sock.makefile("rwb", buffering=0)
        self.sequence = 0
        greeting = self._read_message()
        if "QMP" not in greeting:
            raise RuntimeError(f"unexpected QMP greeting: {greeting}")
        self.execute("qmp_capabilities")

    def _read_message(self):
        raw = self.stream.readline()
        if not raw:
            raise RuntimeError("QMP closed before replying")
        return json.loads(raw)

    def execute(self, command):
        self.sequence += 1
        request_id = self.sequence
        request = json.dumps({"execute": command, "id": request_id}).encode("ascii") + b"\r\n"
        self.stream.write(request)
        while True:
            message = self._read_message()
            if message.get("id") != request_id:
                continue
            if "error" in message:
                raise RuntimeError(f"QMP {command} failed: {message['error']}")
            if "return" not in message:
                raise RuntimeError(f"QMP {command} returned an invalid response: {message}")
            return message["return"]

    def close(self):
        try:
            self.stream.close()
        finally:
            self.sock.close()


def _record_result(root, message):
    with (root / "result.txt").open("a", encoding="utf-8") as result:
        result.write(message + "\n")
    print(message, flush=True)


def _run_capture(command, timeout=5):
    try:
        return subprocess.run(command, text=True, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, check=False, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired) as error:
        return error


def _capture_context(path, commands):
    with path.open("w", encoding="utf-8") as output:
        for command in commands:
            output.write("$ " + " ".join(command) + "\n")
            result = _run_capture(command)
            if isinstance(result, subprocess.CompletedProcess):
                output.write(result.stdout or "")
                if result.returncode:
                    output.write(f"exit status: {result.returncode}\n")
            else:
                output.write(f"unavailable: {result}\n")


def capture_process_context(directory, qemu_binary, runner_uid, runner_user, device_group):
    commands = [
        ["id", "-a"],
        ["stat", "-Lc", "device=%A %U:%G mode=%a major_minor=%t:%T path=%n", "/dev/kvm"],
        ["getfacl", "--numeric", "--absolute-names", "/dev/kvm"],
        ["stat", "-Lc", "qemu=%A %U:%G mode=%a path=%n", qemu_binary],
        ["file", qemu_binary],
        ["getcap", qemu_binary],
    ]
    _capture_context(directory / "process-context.txt", commands)
    with (directory / "process-context.txt").open("a", encoding="utf-8") as output:
        output.write(f"runner uid={runner_uid} user={runner_user} device group={device_group}\n")


def capture_kvm_api_context(probe_root, qemu_binary, runner_uid, runner_user, device_group):
    commands = [
        ["id", "-a"],
        ["stat", "-Lc", "device=%A %U:%G mode=%a major_minor=%t:%T path=%n", "/dev/kvm"],
        ["getfacl", "--numeric", "--absolute-names", "/dev/kvm"],
        ["stat", "-Lc", "qemu=%A %U:%G mode=%a path=%n", qemu_binary],
        ["file", qemu_binary],
        ["getcap", qemu_binary],
    ]
    path = probe_root / "kvm-api-context.txt"
    _capture_context(path, commands)
    with path.open("a", encoding="utf-8") as output:
        output.write(f"captured_at={time.strftime('%Y-%m-%d %H:%M:%S%z')}\n")
        output.write(f"runner uid={runner_uid} user={runner_user} device group={device_group}\n")


def capture_group_context(directory, qemu_binary, runner_uid, runner_user, device_group,
                          device_gid, runner_groups):
    runner_uid = int(runner_uid)
    device_gid = int(device_gid)
    command_prefix = credential_drop_prefix(runner_uid, device_gid, runner_groups)
    commands = [
        [*command_prefix, "id", "-a"],
        [*command_prefix, "stat", "-Lc",
         "device=%A %U:%G mode=%a major_minor=%t:%T path=%n", "/dev/kvm"],
        [*command_prefix, "getfacl", "--numeric", "--absolute-names", "/dev/kvm"],
        [*command_prefix, "stat", "-Lc", "qemu=%A %U:%G mode=%a path=%n", qemu_binary],
        [*command_prefix, "file", qemu_binary],
        [*command_prefix, "getcap", qemu_binary],
    ]
    context_path = directory / "process-context.txt"
    _capture_context(context_path, commands)
    identity = _run_capture([*command_prefix, "cat", "/proc/self/status"])
    with context_path.open("a", encoding="utf-8") as output:
        output.write("$ " + " ".join([*command_prefix, "cat", "/proc/self/status"]) + "\n")
        if isinstance(identity, subprocess.CompletedProcess):
            output.write(identity.stdout or "")
            if identity.returncode:
                output.write(f"exit status: {identity.returncode}\n")
        else:
            output.write(f"unavailable: {identity}\n")
    if not isinstance(identity, subprocess.CompletedProcess) or identity.returncode:
        raise RuntimeError("could not inspect credentials after dropping to the runner/KVM group")
    validate_dropped_credentials(identity.stdout, runner_uid, device_gid)
    with context_path.open("a", encoding="utf-8") as output:
        output.write(f"runner uid={runner_uid} user={runner_user} device group={device_group}\n")
        output.write(f"verified unprivileged runner uid={runner_uid}, primary gid={device_gid}\n")


def credential_drop_prefix(runner_uid, device_gid, runner_groups):
    group_ids = {int(value) for value in runner_groups.split(",") if value}
    group_ids.add(int(device_gid))
    groups = ",".join(str(value) for value in sorted(group_ids))
    return [
        "sudo", "-n", "setpriv", f"--reuid={int(runner_uid)}", f"--regid={int(device_gid)}",
        f"--groups={groups}", "--inh-caps=-all", "--bounding-set=-all", "--no-new-privs",
        "--reset-env", "--",
    ]


def validate_dropped_credentials(status_text, runner_uid, device_gid):
    fields = {}
    for line in status_text.splitlines():
        key, separator, value = line.partition(":")
        if separator and key in {"Uid", "Gid", "Groups"}:
            fields[key] = [int(item) for item in value.split()]
        elif separator and key in {"CapInh", "CapPrm", "CapEff", "CapBnd", "CapAmb", "NoNewPrivs"}:
            fields[key] = int(value.strip(), 16) if key.startswith("Cap") else int(value.strip())
    if fields.get("Uid") != [int(runner_uid)] * 4:
        raise RuntimeError(f"credential drop retained a privileged or mismatched UID: {fields.get('Uid')}")
    if fields.get("Gid") != [int(device_gid)] * 4:
        raise RuntimeError(f"credential drop did not set every GID to the KVM device group: {fields.get('Gid')}")
    if int(device_gid) not in fields.get("Groups", []):
        raise RuntimeError("credential drop omitted the KVM device group from supplementary groups")
    if fields.get("NoNewPrivs") != 1:
        raise RuntimeError("credential drop did not enable no_new_privs")
    if any(fields.get(capability) != 0 for capability in
           ("CapInh", "CapPrm", "CapEff", "CapBnd", "CapAmb")):
        raise RuntimeError("credential drop retained Linux capabilities")


def capture_udev_context(probe_root):
    commands = [
        ["udevadm", "info", "--query=all", "--name=/dev/kvm"],
        ["udevadm", "info", "--attribute-walk", "--name=/dev/kvm"],
        ["journalctl", "-u", "systemd-udevd.service", "--since", "-15 minutes", "--no-pager"],
    ]
    _capture_context(probe_root / "udev-context.txt", commands)
    rule_roots = (Path("/etc/udev/rules.d"), Path("/usr/lib/udev/rules.d"),
                  Path("/lib/udev/rules.d"))
    matching_rules = []
    seen = set()
    for root in rule_roots:
        if not root.is_dir():
            continue
        for rule in sorted(root.glob("*.rules")):
            resolved = rule.resolve()
            if resolved in seen or not resolved.is_file():
                continue
            seen.add(resolved)
            try:
                lines = resolved.read_text(encoding="utf-8", errors="replace").splitlines()
            except OSError:
                continue
            matching = [f"{resolved}:{number}:{line}" for number, line in enumerate(lines, 1)
                        if "kvm" in line.lower()]
            matching_rules.extend(matching)
    if not matching_rules:
        matching_rules = ["No installed udev rule line mentioning KVM was found."]
    (probe_root / "udev-rules.txt").write_text("\n".join(matching_rules) + "\n", encoding="utf-8")


def collect_security_context(directory, since):
    apparmor_profiles = _run_capture(
        ["sudo", "-n", "cat", "/sys/kernel/security/apparmor/profiles"]
    )
    if isinstance(apparmor_profiles, subprocess.CompletedProcess):
        detail = apparmor_profiles.stdout or ""
        if apparmor_profiles.returncode:
            detail += "AppArmor profile list could not be read.\n"
    else:
        detail = "AppArmor profile list unavailable.\n"
    (directory / "apparmor-profiles.txt").write_text(detail, encoding="utf-8")

    kernel_log = _run_capture(
        ["sudo", "-n", "journalctl", "-k", "--since", since, "--no-pager"], timeout=5
    )
    kernel_lines = []
    if isinstance(kernel_log, subprocess.CompletedProcess):
        kernel_lines = [line for line in (kernel_log.stdout or "").splitlines()
                        if any(token in line.lower() for token in
                               ("apparmor", "audit")) and "denied" in line.lower()]
    if not kernel_lines:
        kernel_lines = ["No matching recent kernel denial record was available."]
    (directory / "kernel-denials.txt").write_text("\n".join(kernel_lines) + "\n", encoding="utf-8")

    audit = _run_capture(["sudo", "-n", "tail", "-n", "500", "/var/log/audit/audit.log"], timeout=5)
    audit_lines = []
    if isinstance(audit, subprocess.CompletedProcess):
        audit_lines = [line for line in (audit.stdout or "").splitlines()
                       if "denied" in line.lower()]
    if not audit_lines:
        audit_lines = ["Audit log unavailable or no matching denial record was available."]
    (directory / "audit-denials.txt").write_text("\n".join(audit_lines) + "\n", encoding="utf-8")


def native_kvm_api_probe(device="/dev/kvm"):
    device_fd = os.open(device, os.O_RDWR | os.O_CLOEXEC)
    try:
        api_version = fcntl.ioctl(device_fd, KVM_GET_API_VERSION, 0)
        if api_version != 12:
            raise RuntimeError(f"unsupported KVM API version: {api_version}")
        vm_fd = fcntl.ioctl(device_fd, KVM_CREATE_VM, 0)
        try:
            return f"KVM API {api_version}; created test VM fd {vm_fd}"
        finally:
            os.close(vm_fd)
    finally:
        os.close(device_fd)


def _stop_process(process):
    if process is None or process.poll() is not None:
        return
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        pass
    try:
        process.wait(timeout=1)
        return
    except subprocess.TimeoutExpired:
        pass
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    try:
        process.wait(timeout=2)
    except subprocess.TimeoutExpired:
        pass


def run_qemu_probe(label, command, directory, *, required, startup_timeout=5.0,
                   stable_duration=2.0, poll_interval=0.05, context_capture=None,
                   security_capture=collect_security_context):
    directory.mkdir(parents=True, exist_ok=False)
    if context_capture is not None:
        context_capture(directory)
    start_time = time.strftime("%Y-%m-%d %H:%M:%S%z")
    qmp_path = directory / "qmp.sock"
    full_command = [*command, *QEMU_ARGUMENTS, "-qmp",
                    f"unix:{qmp_path},server=on,wait=off"]
    process = None
    client = None
    status = None

    def failed(detail):
        if client is not None:
            try:
                client.close()
            except OSError:
                pass
        _stop_process(process)
        if security_capture is not None:
            try:
                security_capture(directory, start_time)
            except (OSError, subprocess.SubprocessError):
                pass
        outcome = ProbeOutcome(label, False, detail, process.poll() if process else None)
        _record_result(directory.parent, f"QEMU {label} probe failed: {detail}")
        if required:
            raise ProbeFailure(f"required QEMU {label} KVM probe failed: {detail}")
        return outcome

    try:
        log = (directory / "qemu.log").open("wb")
        try:
            process = subprocess.Popen(full_command, stdin=subprocess.DEVNULL,
                                       stdout=log, stderr=subprocess.STDOUT,
                                       start_new_session=True)
        finally:
            log.close()
    except OSError as error:
        return failed(f"could not start command: {error}")

    deadline = time.monotonic() + startup_timeout
    while time.monotonic() < deadline:
        status = process.poll()
        if qmp_path.exists():
            sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            sock.settimeout(min(0.5, max(0.05, deadline - time.monotonic())))
            try:
                sock.connect(str(qmp_path))
                client = QmpClient(sock)
                client.execute("query-status")
                break
            except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as error:
                sock.close()
                status = process.poll()
                if status is not None:
                    return failed(f"process exited after QMP socket appeared but before greeting (status {status})")
                time.sleep(poll_interval)
        else:
            if status is not None:
                return failed(f"process exited before QMP handshake (status {status})")
            time.sleep(poll_interval)
    else:
        return failed("QMP socket/greeting did not become ready before the startup deadline")

    stable_deadline = time.monotonic() + stable_duration
    while time.monotonic() < stable_deadline:
        status = process.poll()
        if status is not None:
            return failed(f"process exited during the stable QMP check (status {status})")
        try:
            client.execute("query-status")
        except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as error:
            return failed(f"QMP stopped responding during the stable check: {error}")
        time.sleep(min(poll_interval, max(0.0, stable_deadline - time.monotonic())))

    try:
        client.execute("quit")
        client.close()
        client = None
        status = process.wait(timeout=5)
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError, subprocess.TimeoutExpired) as error:
        return failed(f"QMP shutdown failed: {error}")
    if status != 0:
        return failed(f"process exited after QMP quit with status {status}")
    outcome = ProbeOutcome(label, True, "QMP remained responsive and exited cleanly", status)
    _record_result(directory.parent,
                   f"QEMU {label} KVM probe passed; QMP stayed responsive for {stable_duration:g} seconds.")
    return outcome


def run_qemu_process_probes(probe_root, direct_command, group_command, *,
                            startup_timeout=5.0, stable_duration=2.0,
                            poll_interval=0.05, direct_context=None,
                            group_context=None, security_capture=collect_security_context):
    direct = run_qemu_probe(
        "direct-user", direct_command, probe_root / "direct-user", required=False,
        startup_timeout=startup_timeout, stable_duration=stable_duration,
        poll_interval=poll_interval, context_capture=direct_context,
        security_capture=security_capture,
    )
    group = run_qemu_probe(
        "primary-group", group_command, probe_root / "primary-group", required=True,
        startup_timeout=startup_timeout, stable_duration=stable_duration,
        poll_interval=poll_interval, context_capture=group_context,
        security_capture=security_capture,
    )
    return direct, group


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--probe-root", type=Path, required=True)
    parser.add_argument("--qemu", required=True)
    parser.add_argument("--qemu-wrapper", required=True)
    parser.add_argument("--runner-uid", required=True)
    parser.add_argument("--runner-user", required=True)
    parser.add_argument("--device-group", required=True)
    parser.add_argument("--device-gid", required=True, type=int)
    parser.add_argument("--runner-groups", required=True,
                        help="comma-separated numeric supplementary groups for the runner")
    parser.add_argument("--strace", required=True)
    args = parser.parse_args(argv)

    args.probe_root.mkdir(parents=True, exist_ok=False)
    if os.getuid() != int(args.runner_uid) or os.geteuid() == 0:
        raise RuntimeError("KVM preflight must run as the requested unprivileged job user")
    result_file = args.probe_root / "result.txt"
    acl = _run_capture(["getfacl", "--numeric", "--omit-header", "--absolute-names", "/dev/kvm"])
    if isinstance(acl, subprocess.CompletedProcess):
        (args.probe_root / "acl.txt").write_text(acl.stdout or "", encoding="utf-8")
    capture_kvm_api_context(args.probe_root, args.qemu, args.runner_uid,
                            args.runner_user, args.device_group)
    try:
        api_result = native_kvm_api_probe()
    except (OSError, RuntimeError) as error:
        result_file.write_text(f"KVM API preflight failed: {error}\n", encoding="utf-8")
        try:
            collect_security_context(args.probe_root, time.strftime("%Y-%m-%d %H:%M:%S%z"))
        except OSError:
            pass
        raise
    _record_result(args.probe_root, f"runner uid={args.runner_uid} user={args.runner_user} "
                   f"device group={args.device_group}")
    _record_result(args.probe_root, api_result)
    capture_udev_context(args.probe_root)

    direct_context = lambda directory: capture_process_context(
        directory, args.qemu, args.runner_uid, args.runner_user, args.device_group
    )
    group_context = lambda directory: capture_group_context(
        directory, args.qemu, args.runner_uid, args.runner_user, args.device_group,
        args.device_gid, args.runner_groups
    )
    direct_command = [args.strace, "-f", "-yy", "-e", "trace=%file", "-o",
                      str(args.probe_root / "direct-user" / "open.trace"), args.qemu]
    run_qemu_process_probes(
        args.probe_root, direct_command, [args.qemu_wrapper],
        direct_context=direct_context, group_context=group_context,
    )
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ProbeFailure, OSError, RuntimeError, ValueError) as error:
        print(f"KVM preflight failed: {error}", file=sys.stderr)
        sys.exit(1)
