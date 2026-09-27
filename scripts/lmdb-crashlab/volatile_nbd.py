#!/usr/bin/env python3
"""Disposable, single-client NBD disk with an explicitly volatile sector cache.

Normal WRITEs acknowledge RAM, FLUSH persists all prior WRITEs, FUA persists only
its own range. A cut freezes I/O before discarding selected dirty sectors. This
is a test-only device model, not production storage. See README.md.
"""
# Adapted from the RDF4J CrashLab source archive; see the accompanying MIT LICENSE.
from __future__ import annotations
import argparse
import errno
import hashlib
import json
import os
from pathlib import Path
import random
import socket
import socketserver
import stat
import struct
import threading
import time
from typing import Iterable

NBD_MAGIC = 0x4E42444D41474943
OPT_MAGIC = 0x49484156454F5054
REP_MAGIC = 0x3E889045565A9
REQ_MAGIC = 0x25609513
SIMPLE_MAGIC = 0x67446698
EXPORT_FLAGS = 1 | 4 | 8  # HAS_FLAGS, SEND_FLUSH, SEND_FUA; no multi-conn
MAX_REQUEST = 32 * 1024 * 1024


def recvn(sock: socket.socket, n: int) -> bytes:
    parts = bytearray()
    while len(parts) < n:
        b = sock.recv(n - len(parts))
        if not b:
            raise EOFError("peer disconnected")
        parts.extend(b)
    return bytes(parts)


def pwrite_all(fd: int, data: bytes, offset: int) -> None:
    view = memoryview(data)
    while view:
        n = os.pwrite(fd, view, offset)
        if n <= 0:
            raise OSError(errno.EIO, "short pwrite")
        offset += n
        view = view[n:]


def sync_file(fd: int) -> None:
    """Persist a file descriptor using the strongest portable POSIX primitive."""
    # Linux fdatasync avoids metadata work when possible. macOS and other POSIX
    # platforms without fdatasync use fsync, which also persists file metadata.
    getattr(os, "fdatasync", os.fsync)(fd)


class PowerLost(OSError):
    def __init__(self) -> None:
        super().__init__(errno.EIO, "simulated disk power is off")


class Disk:
    def __init__(self, path: Path, sector: int = 512, seed: int = 1,
                 max_cache_mib: int = 256, sector_delay_us: int = 0,
                 log_path: Path | None = None, ignore_flush: bool = False):
        if sector not in (512, 4096):
            raise ValueError("sector must be 512 or 4096")
        st = path.lstat()
        if not stat.S_ISREG(st.st_mode) or st.st_size == 0 or st.st_size % sector:
            raise ValueError("image must be a nonempty regular file, sector-aligned; no devices/symlinks")
        self.fd = os.open(path, os.O_RDWR | getattr(os, "O_NOFOLLOW", 0))
        # A second backend must never silently corrupt this image.
        import fcntl
        try:
            fcntl.flock(self.fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BaseException:
            os.close(self.fd)
            raise
        self.size = st.st_size
        self.sector = sector
        self.dirty: dict[int, bytes] = {}
        self.lock = threading.RLock()
        self.off = False
        self.rng = random.Random(seed)
        self.max_sectors = max(1, max_cache_mib * 1024 * 1024 // sector)
        self.delay = sector_delay_us / 1_000_000
        self.ignore_flush = ignore_flush
        self.counts = dict(reads=0, writes=0, flushes=0, fua=0, destaged=0, cuts=0,
                           write_bytes=0, completed_flushes=0)
        self.log = open(log_path, "a", buffering=1) if log_path else None
        self.event("OPEN", image=str(path), size=self.size, sector=sector,
                   cache_limit_mib=max_cache_mib, ignore_flush=ignore_flush)

    def event(self, kind: str, **fields) -> None:
        if self.log:
            self.log.write(json.dumps(dict(event=kind, time_ns=time.time_ns(), **fields)) + "\n")

    def check(self, offset: int = 0, length: int = 0) -> None:
        if self.off:
            raise PowerLost()
        if offset < 0 or length < 0 or offset + length > self.size:
            raise OSError(errno.EINVAL, "out of bounds")
        if offset % self.sector or length % self.sector:
            raise OSError(errno.EINVAL, "unaligned request")

    def read(self, offset: int, length: int) -> bytes:
        with self.lock:
            self.check(offset, length)
            self.counts["reads"] += 1
            data = bytearray()
            for off in range(offset, offset + length, self.sector):
                b = self.dirty.get(off)
                if b is None:
                    b = os.pread(self.fd, self.sector, off)
                if len(b) != self.sector:
                    raise OSError(errno.EIO, "short read")
                data.extend(b)
            return bytes(data)

    def _destage(self, offsets: Iterable[int], kind: str) -> None:
        # Release the lock between sectors. A cut may interrupt a multi-sector
        # WRITE/FUA/FLUSH. A successful FLUSH still guarantees its whole prefix.
        for off in offsets:
            with self.lock:
                self.check()
                b = self.dirty.get(off)
                if b is not None:
                    pwrite_all(self.fd, b, off)
                    del self.dirty[off]
                    self.counts["destaged"] += 1
            if self.delay:
                time.sleep(self.delay)
        with self.lock:
            self.check()
            sync_file(self.fd)
            self.event(kind, **self.counts, dirty_sectors=len(self.dirty))

    def write(self, offset: int, data: bytes, fua: bool = False) -> None:
        with self.lock:
            self.check(offset, len(data))
            self.counts["writes"] += 1
            self.counts["write_bytes"] += len(data)
            self.counts["fua"] += int(fua)
            for i in range(0, len(data), self.sector):
                self.dirty[offset + i] = data[i:i + self.sector]
            self.event("WRITE", offset=offset, length=len(data), fua=fua,
                       sha256=hashlib.sha256(data).hexdigest())
            overflow = max(0, len(self.dirty) - self.max_sectors)
            # Oldest dirty sectors are legal spontaneous writeback. This bounds
            # RAM, and is recorded rather than silently pretending all is volatile.
            old = list(self.dirty)[:overflow]
        if fua and not self.ignore_flush:
            self._destage(range(offset, offset + len(data), self.sector), "FUA_DONE")
        if old:
            self._destage(old, "CACHE_PRESSURE_DESTAGE")

    def flush(self) -> None:
        with self.lock:
            self.check()
            self.counts["flushes"] += 1
            self.event("FLUSH_BEGIN", dirty_sectors=len(self.dirty), **self.counts)
            offsets = list(self.dirty)
        if not self.ignore_flush:
            self._destage(offsets, "FLUSH_PERSISTED")
        with self.lock:
            self.check()
            self.counts["completed_flushes"] += 1
            self.event("FLUSH_DONE", ignored=self.ignore_flush, **self.counts)

    def cut(self, survival: str = "drop", probability: float = 0.5,
            seed: int | None = None) -> dict:
        if survival not in ("drop", "random", "all") or not 0 <= probability <= 1:
            raise ValueError("invalid cut policy")
        with self.lock:
            if self.off:
                raise ValueError("disk already off")
            self.off = True  # I/O fence is first, never flush as part of guest shutdown.
            rng = random.Random(seed) if seed is not None else self.rng
            before = len(self.dirty)
            kept = []
            for off, data in self.dirty.items():
                if survival == "all" or (survival == "random" and rng.random() < probability):
                    pwrite_all(self.fd, data, off)
                    kept.append(off)
            self.dirty.clear()
            # This sync is on the HOST image AFTER choosing the crash state. It
            # cannot rescue discarded guest writes; it preserves the evidence.
            sync_file(self.fd)
            self.counts["cuts"] += 1
            report = dict(off=True, survival=survival, probability=probability, seed=seed,
                          dirty_before=before, kept_sectors=len(kept), kept_offsets=kept,
                          **self.counts)
            self.event("CUT", **report)
            if self.log:
                self.log.flush()
                os.fsync(self.log.fileno())
            return report

    def reset(self) -> dict:
        with self.lock:
            if not self.off:
                raise ValueError("reset requires a prior cut")
            if self.dirty:
                raise AssertionError("dirty cache survived cut")
            self.off = False
            self.event("POWER_ON")
            return self.status()

    def status(self) -> dict:
        with self.lock:
            return dict(off=self.off, size=self.size, sector=self.sector,
                        dirty_sectors=len(self.dirty), ignore_flush=self.ignore_flush, **self.counts)

    def close(self) -> None:
        # Deliberately DO NOT flush volatile data.
        with self.lock:
            os.close(self.fd)
            if self.log:
                self.log.close()


class NBDHandler(socketserver.BaseRequestHandler):
    def reply_option(self, opt: int, typ: int, data: bytes = b"") -> None:
        self.request.sendall(struct.pack(">QIII", REP_MAGIC, opt, typ, len(data)) + data)

    def negotiate(self) -> bool:
        s = self.request
        disk = self.server.disk
        s.sendall(struct.pack(">QQH", NBD_MAGIC, OPT_MAGIC, 3))
        flags, = struct.unpack(">I", recvn(s, 4))
        if flags & ~3 or not flags & 1:
            raise ValueError("unsupported client flags")
        while True:
            magic, opt, n = struct.unpack(">QII", recvn(s, 16))
            if magic != OPT_MAGIC or n > 65536:
                raise ValueError("invalid option")
            payload = recvn(s, n)
            if opt == 1:  # EXPORT_NAME
                if payload not in (b"", b"crashlab"):
                    return False
                s.sendall(struct.pack(">QH", disk.size, EXPORT_FLAGS))
                if not flags & 2:
                    s.sendall(bytes(124))
                return True
            if opt == 2:  # ABORT
                self.reply_option(opt, 1)
                return False
            if opt in (6, 7):  # INFO, GO
                if len(payload) < 6:
                    self.reply_option(opt, 0x80000003)
                    continue
                namelen, = struct.unpack_from(">I", payload)
                if namelen > len(payload) - 6:
                    self.reply_option(opt, 0x80000003)
                    continue
                name = payload[4:4+namelen]
                ninfos, = struct.unpack_from(">H", payload, 4+namelen)
                if len(payload) != 6 + namelen + 2*ninfos:
                    self.reply_option(opt, 0x80000003)
                    continue
                if name not in (b"", b"crashlab"):
                    self.reply_option(opt, 0x80000006)  # UNKNOWN export
                    continue
                self.reply_option(opt, 3, struct.pack(">HQH", 0, disk.size, EXPORT_FLAGS))
                self.reply_option(opt, 3, struct.pack(">HIII", 3, disk.sector, 4096, MAX_REQUEST))
                self.reply_option(opt, 1)
                if opt == 7:
                    return True
            else:
                # No TLS/structured replies/meta contexts. QEMU can fall back.
                self.reply_option(opt, 0x80000001)

    def handle(self) -> None:
        srv = self.server
        if not srv.client_lock.acquire(False):
            return
        try:
            if srv.disk.off:
                return
            with srv.connections_lock:
                srv.connections.add(self.request)
            if not self.negotiate():
                return
            while True:
                magic, flags, cmd, handle, offset, n = struct.unpack(">IHHQQI", recvn(self.request, 28))
                if magic != REQ_MAGIC or n > MAX_REQUEST:
                    raise ValueError("bad NBD request")
                if cmd == 2:
                    return  # DISC never flushes
                payload = recvn(self.request, n) if cmd == 1 else b""
                result = b""
                error = 0
                try:
                    if flags & ~1 or (flags and cmd != 1):
                        raise OSError(errno.EINVAL, "unsupported command flags")
                    if cmd == 0:
                        result = srv.disk.read(offset, n)
                    elif cmd == 1:
                        srv.disk.write(offset, payload, fua=bool(flags & 1))
                    elif cmd == 3 and offset == 0 and n == 0:
                        srv.disk.flush()
                    else:
                        raise OSError(errno.EINVAL, "unsupported command")
                except PowerLost:
                    return
                except OSError as e:
                    error = e.errno or errno.EIO
                    result = b""
                    srv.disk.event("IO_ERROR", command=cmd, error=error)
                self.request.sendall(struct.pack(">IIQ", SIMPLE_MAGIC, error, handle) + result)
        except (EOFError, ConnectionError, OSError, ValueError) as e:
            srv.disk.event("DISCONNECT", reason=str(e))
        finally:
            with srv.connections_lock:
                srv.connections.discard(self.request)
            srv.client_lock.release()


class NBDServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True
    def __init__(self, address: tuple[str, int], disk: Disk):
        self.disk = disk
        self.client_lock = threading.Lock()
        self.connections_lock = threading.Lock()
        self.connections: set[socket.socket] = set()
        super().__init__(address, NBDHandler)

    def disconnect_all(self) -> None:
        with self.connections_lock:
            for s in list(self.connections):
                try:
                    s.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass


class ControlHandler(socketserver.StreamRequestHandler):
    def handle(self) -> None:
        try:
            raw = self.rfile.readline(65537)
            if len(raw) > 65536:
                raise ValueError("request too large")
            req = json.loads(raw)
            srv = self.server.nbd
            action = req["action"]
            if action == "status":
                reply = srv.disk.status()
            elif action == "cut":
                reply = srv.disk.cut(req.get("survival", "drop"),
                                     float(req.get("probability", 0.5)), req.get("seed"))
                srv.disconnect_all()
            elif action == "reset":
                # A reconnecting old VM must not write into the recovered image.
                # Caller must first kill the VM; this also checks live connections.
                with srv.connections_lock:
                    if srv.connections:
                        raise ValueError("NBD clients still connected; kill the VM first")
                reply = srv.disk.reset()
            else:
                raise ValueError("unknown control action")
            self.wfile.write((json.dumps(dict(ok=True, result=reply))+"\n").encode())
        except Exception as e:
            self.wfile.write((json.dumps(dict(ok=False, error=str(e)))+"\n").encode())


class ControlServer(socketserver.ThreadingUnixStreamServer):
    daemon_threads = True


def control(path: str, req: dict) -> dict:
    with socket.socket(socket.AF_UNIX) as s:
        s.settimeout(60)
        s.connect(path)
        s.sendall((json.dumps(req) + "\n").encode())
        with s.makefile("rb") as f:
            reply = json.loads(f.readline())
    if not reply.get("ok"):
        raise RuntimeError(reply.get("error", "control failed"))
    return reply["result"]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest="command", required=True)
    serve = sub.add_parser("serve")
    serve.add_argument("--image", required=True, type=Path)
    serve.add_argument("--control", required=True, type=Path)
    serve.add_argument("--port", type=int, default=10809)
    serve.add_argument("--sector", type=int, choices=(512,4096), default=512)
    serve.add_argument("--seed", type=int, default=1)
    serve.add_argument("--max-cache-mib", type=int, default=256)
    serve.add_argument("--sector-delay-us", type=int, default=0)
    serve.add_argument("--log", type=Path, required=True)
    serve.add_argument("--ignore-flush", action="store_true", help="LYING-DISK NEGATIVE CONTROL ONLY")
    ctl = sub.add_parser("ctl")
    ctl.add_argument("--control", required=True)
    ctl.add_argument("action", choices=("status","cut","reset"))
    ctl.add_argument("--survival", choices=("drop","random","all"), default="drop")
    ctl.add_argument("--probability", type=float, default=.5)
    ctl.add_argument("--seed", type=int, default=1)
    a = ap.parse_args()
    if a.command == "ctl":
        print(json.dumps(control(a.control, dict(action=a.action, survival=a.survival,
                                                probability=a.probability, seed=a.seed)), indent=2))
        return
    if a.control.exists():
        raise SystemExit("Control socket exists; refusing to replace an existing backend.")
    if a.max_cache_mib < 1 or a.sector_delay_us < 0:
        raise SystemExit("invalid cache/delay")
    disk = Disk(a.image, a.sector, a.seed, a.max_cache_mib, a.sector_delay_us, a.log, a.ignore_flush)
    nbd = NBDServer(("127.0.0.1", a.port), disk)
    ctlserver = ControlServer(str(a.control), ControlHandler)
    ctlserver.nbd = nbd
    os.chmod(a.control, 0o600)
    threading.Thread(target=ctlserver.serve_forever, daemon=True).start()
    print(json.dumps(dict(nbd=nbd.server_address, control=str(a.control))), flush=True)
    try:
        nbd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        nbd.disconnect_all()
        nbd.server_close()
        ctlserver.shutdown()
        ctlserver.server_close()
        a.control.unlink(missing_ok=True)
        disk.close()


if __name__ == "__main__":
    main()
