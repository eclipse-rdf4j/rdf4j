#!/usr/bin/env python3
"""Compile the self-contained overlay, test it, and reproduce the storage/read experiments.

Requires Python 3.9+ and a JDK with final FFM (22+; this submission was tested on 26).
No Maven dependencies or network access are needed. Baseline is an extracted source
root containing java/, normally the previously delivered round-2 source ZIP.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import statistics
import subprocess
import sys
import tempfile

PACKAGE = "org.eclipse.rdf4j.sail.lmdb.valueoverlay"
REL = Path("org/eclipse/rdf4j/sail/lmdb/valueoverlay")
SCENARIOS = ("rank-tagged", "rank-random", "routes", "text", "prefix", "near-fixed")


def run(command: list[str], log: Path) -> None:
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("w") as target:
        result = subprocess.run(command, stdout=target, stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        print(log.read_text(errors="replace"), file=sys.stderr)
        raise RuntimeError(f"Command failed ({result.returncode}); see {log}")
    print(f"OK {log}", flush=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, help="Extracted baseline source root (not a ZIP)")
    parser.add_argument("--out", type=Path, help="Output directory; defaults to a new temporary directory")
    parser.add_argument("--bench", action="store_true", help="Also run the isolated read benchmarks")
    parser.add_argument("--scenarios", nargs="+", choices=SCENARIOS, default=SCENARIOS, help="Benchmark subset; default all six")
    parser.add_argument("--forks", type=int, default=3, help="Independent JVMs per benchmark, default 3")
    parser.add_argument("--c2", action="store_true", help="Capture emitted C2 bytes (objdump for x86-64 decoding)")
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("--forks must be positive")
    source = Path(__file__).resolve().parents[2]
    java_home = os.environ.get("JAVA_HOME")
    javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
    java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
    if not javac or not java:
        parser.error("Set JAVA_HOME to a complete JDK or put java and javac on PATH")
    version = subprocess.check_output([javac, "-version"], stderr=subprocess.STDOUT, text=True)
    match = re.search(r"javac (\d+)", version)
    if not match or int(match.group(1)) < 22:
        parser.error(f"JDK 22+ required, found {version.strip()}")
    out = args.out.resolve() if args.out else Path(tempfile.mkdtemp(prefix="value-overlay-storage-"))
    # Keep generated output out of the submitted source snapshot and baseline.
    roots = [source] + ([args.baseline.resolve()] if args.baseline else [])
    if any(out == root or root in out.parents for root in roots):
        parser.error("--out must be outside the source and baseline trees")
    out.mkdir(parents=True, exist_ok=True)
    print(f"Source: {source}\nOutput: {out}\nCompiler: {version.strip()}", flush=True)
    run([java, "-version"], out / "runtime.txt")
    tool_dir = source / "tools/value-overlay/src" / REL
    builds: dict[str, Path] = {}
    for name, root in [("work", source)] + ([("baseline", args.baseline.resolve())] if args.baseline else []):
        production = sorted((root / "java" / REL).glob("*.java"))
        if not production:
            parser.error(f"No overlay sources in {root / 'java' / REL}")
        if name == "work":
            tools = sorted(tool_dir.glob("*.java"))
        else:
            benchmark = root / "tools/value-overlay/src" / REL / "OverlayBenchmark.java"
            # The original judges' source has no harness. Its production package can still be
            # compiled with the shared harness; old baseline code remains unmodified.
            if not benchmark.is_file():
                benchmark = tool_dir / "OverlayBenchmark.java"
            tools = [benchmark, tool_dir / "OverlayStorageAudit.java", tool_dir / "OverlayStorageReadBenchmark.java"]
        classes = out / "classes" / name
        classes.mkdir(parents=True, exist_ok=True)
        run([javac, "-d", str(classes), *(str(path) for path in production + tools)], out / f"compile-{name}.txt")
        builds[name] = classes
    runtime = [java, "--enable-native-access=ALL-UNNAMED", "-ea", "-Xms256m", "-Xmx512m"]
    baseline_arg = [str(builds["baseline"])] if "baseline" in builds else []
    for test, filename in [("OverlayRegressionTest", "full-regression.txt"),
                           ("OverlayStorageRegressionTest", "storage-regression.txt")]:
        run(runtime + ["-cp", str(builds["work"]), f"{PACKAGE}.{test}"] + baseline_arg, out / filename)
        print((out / filename).read_text(), end="", flush=True)
    for name, classes in builds.items():
        run(runtime + ["-cp", str(classes), f"{PACKAGE}.OverlayStorageAudit"], out / f"storage-{name}.csv")
    flags = [java, "--enable-native-access=ALL-UNNAMED", "-XX:ActiveProcessorCount=2",
             "-XX:CICompilerCount=2", "-XX:+UseSerialGC", "-Xms256m", "-Xmx512m",
             "-Dbench.warmup=6", "-Dbench.rounds=5", "-Dbench.millis=75"]
    if args.bench:
        summary: dict[str, dict[str, list[float]]] = {}
        for fork in range(1, args.forks + 1):
            names = (["baseline", "work"] if fork % 2 else ["work", "baseline"]) if args.baseline else ["work"]
            for scenario in args.scenarios:
                for name in names:
                    log = out / f"final-{scenario}-{name}-{fork}.txt"
                    run(flags + ["-cp", str(builds[name]), f"{PACKAGE}.OverlayStorageReadBenchmark", scenario], log)
                    content = log.read_text()
                    if "BLACKHOLE," not in content:
                        raise RuntimeError(f"Incomplete benchmark: {log}")
                    for line in content.splitlines():
                        fields = line.split(",")
                        if len(fields) == 6 and fields[0] == "RESULT":
                            summary.setdefault(fields[1], {}).setdefault(name, []).append(float(fields[2]))
        if not summary or any(len(samples) != args.forks for variants in summary.values() for samples in variants.values()):
            raise RuntimeError("Missing benchmark results; inspect the per-fork logs")
        (out / "benchmark-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
        medians = {operation: {name: statistics.median(samples) for name, samples in variants.items()}
                   for operation, variants in summary.items()}
        (out / "benchmark-medians.json").write_text(json.dumps(medians, indent=2) + "\n")
    if args.c2:
        pages = ["PageCodec::encodedRange", "PageCodec::prefixResidualSum", "PageCodec::mediumPrefixSum"]
        rank = ["PackedVector::indexOf", "PackedVector::denseValue", "CompressedValueOverlay::rank"]
        captures = [("work", "near-fixed", pages), ("work", "text", pages), ("work", "rank-tagged", rank)]
        if "baseline" in builds:
            captures.insert(0, ("baseline", "near-fixed", pages))
        for name, scenario, methods in captures:
            log = out / f"c2-{name}-{scenario}.log"
            commands = ["-XX:+UnlockDiagnosticVMOptions"] + [f"-XX:CompileCommand=print,{PACKAGE}.{method}" for method in methods]
            run(flags + commands + ["-cp", str(builds[name]), f"{PACKAGE}.OverlayStorageReadBenchmark", scenario], log)
            if "Compiled method (c2)" not in log.read_text(errors="replace"):
                raise RuntimeError(f"No C2 compilation captured in {log}")
            architecture = subprocess.check_output([java, "-XshowSettings:properties", "-version"], stderr=subprocess.STDOUT, text=True)
            if shutil.which("objdump") and re.search(r"os.arch = (amd64|x86_64)", architecture):
                run([sys.executable, str(source / "tools/value-overlay/disassemble.py"), str(log), str(out / f"asm-{name}-{scenario}")],
                    out / f"disassemble-{name}-{scenario}.txt")
            else:
                print(f"Retained native C2 log; x86-64/objdump decoding not available for {scenario}")
    print(f"All requested runs completed: {out}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error
