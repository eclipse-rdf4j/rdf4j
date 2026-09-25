#!/usr/bin/env python3
"""Run current packed-I/O regressions plus the existing UTF-8, binding, key and sort suites.

All compilation is offline. Manifests label external API doubles and the extracted
UTF-8/key units; this is not a full RDF4J dependency or Janino integration build.
The inherited historical CSF suite expected the pre-previous-revision datatype
lookup count, so its vector and page comparisons are invoked by PackedIoSuite
instead of running that obsolete assertion against the immediate baseline.
"""
from pathlib import Path
import argparse
import importlib.util
import os
import subprocess
import sys

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
PKG = "org.eclipse.rdf4j.sail.lmdb.evaluation."


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    source, baseline, out = (p.resolve() for p in (args.source, args.baseline, args.out))
    for tree in (source, baseline):
        if tree == out or tree in out.parents or out in tree.parents:
            parser.error("Output must be separate from source/baseline")
    home = Path(os.environ["JAVA_HOME"])
    out.mkdir(parents=True, exist_ok=True)
    subprocess.run([sys.executable, str(ROOT / "tools/lmdb-packed/run.py"),
                    "--source", str(source), "--baseline", str(baseline),
                    "--out", str(out / "packed")], check=True)
    spec = importlib.util.spec_from_file_location("previous", ROOT / "tools/lmdb-performance/run.py")
    old = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(old)
    java = [home / "bin/java", "-ea", "-Xms256m", "-Xmx2g",
            "--enable-native-access=ALL-UNNAMED", "-cp"]
    classes = out / "packed/build/classes"
    old.run(java + [classes, PKG + "Utf8PerfSuite", "test"], out / "Utf8PerfSuite.txt")
    binding = out / "binding"
    binding.mkdir(exist_ok=True)
    classes = old.prepare_binding(source, baseline, binding, home)
    old.run(java + [classes, PKG + "BindingDomainPerfSuite", "test"], binding / "BindingDomainPerfSuite.txt")
    keys = out / "keys"
    keys.mkdir(exist_ok=True)
    classes = old.prepare_keys(source, keys, home)
    for name in ("GeneratedKeyRegression", "OptimizedGeneratedKeyRegression", "NativeSortPerfSuite"):
        old.run(java + [classes, PKG + name], keys / (name + ".txt"))
    print("All six component suites passed. Full application integration not compiled.")


if __name__ == "__main__":
    main()
