#!/usr/bin/env python3
"""Capture normal-tiered C2 bytes; require complete bytes before objdump disassembly.
No compile-only, forced-inline, or compilation-threshold options are used.
UTF8 is the exact extracted production method in the explicitly named test wrapper.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

TOOL = Path(__file__).resolve().parent
ROOT = TOOL.parents[1]
CASES = {
    'vector': ('build/classes', 'csf.CsfPerfSuite', ['measure-typed-seq'], [
        'csf.PackedLongVector::planBlock', 'csf.PackedLongVector::fillBlock',
        'csf.PackedLongVector::measure']),
    'page': ('build/classes', 'csf.CsfPerfSuite', ['page-core-write'], [
        'csf.CompactCsfPageEncoder::tryMeasure', 'csf.CompactCsfPageEncoder$VectorValues::next',
        'csf.CompactCsfPageEncoder$VectorValues::nextBlock',
        'csf.CompactCsfPageEncoder$VectorValues::copyContextTails']),
    'utf8': ('build/classes', 'evaluation.Utf8PerfSuite', ['ascii'], [
        'evaluation.Utf8UnderTest::decodeUtf8', 'evaluation.Utf8UnderTest::utf8Scratch']),
    'sort': ('keys/build/classes', 'evaluation.NativeSortPerfSuite', ['duplicates', '16384'], [
        'evaluation.NativeSortBuffer::sort', 'evaluation.NativeSortBuffer::orderedRun',
        'evaluation.NativeSortBuffer::orderedIndices', 'evaluation.NativeSortBuffer::merge',
        'evaluation.NativeSortBuffer::compareRows']),
    'domain': ('binding/build/classes', 'evaluation.BindingDomainPerfSuite', ['16', 'false'], [
        'evaluation.RowBindingSetView::slotReplaced', 'evaluation.RowBindingSetView::size']),
}
for mode in ('sorted', 'reverse'):
    cp, cls, args, methods = CASES['sort']
    CASES['sort-' + mode] = (cp, cls, [mode, '16384'], methods)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--only', default='')
    parser.add_argument('--inlining', action='store_true')
    options = parser.parse_args()
    home = Path(os.environ['JAVA_HOME'])
    options.out.mkdir(parents=True, exist_ok=True)
    command_file = options.out / 'commands.json'
    commands = json.loads(command_file.read_text()) if command_file.exists() else {}
    for name, (cp, cls, args, methods) in CASES.items():
        if options.only and name not in options.only.split(','):
            continue
        command = [str(home / 'bin/java'), '-ea', '-Xms512m', '-Xmx2g',
                   '--enable-native-access=ALL-UNNAMED', '-XX:+UnlockDiagnosticVMOptions']
        if options.inlining:
            command.append('-XX:+PrintInlining')
        command += ['-XX:CompileCommand=print,org.eclipse.rdf4j.sail.lmdb.' + method
                    for method in methods]
        command += ['-cp', str(options.build / cp), 'org.eclipse.rdf4j.sail.lmdb.' + cls, *args]
        commands[name] = command
        command_file.write_text(json.dumps(commands, indent=2) + '\n')
        log = options.out / (name + '.log')
        assembly = options.out / (name + '-asm')
        decode_log = options.out / (name + '-decode.txt')
        for attempt in range(1, 4):
            with log.open('w') as output:
                subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=True)
            if assembly.exists():
                shutil.rmtree(assembly)
            with decode_log.open('w') as output:
                decoded = subprocess.run([
                    sys.executable, str(ROOT / 'tools/value-overlay/disassemble.py'),
                    str(log), str(assembly)], stdout=output, stderr=subprocess.STDOUT)
            summary = assembly / 'summary.txt'
            if decoded.returncode == 0 and summary.exists() and summary.read_text().strip():
                print(decode_log.read_text(), end='', flush=True)
                break
            # Application output can interleave with HotSpot byte output. Never guess missing bytes:
            # retain the rejected log, then collect a new independent diagnostic JVM recording.
            log.rename(options.out / f'{name}-rejected-{attempt}.log')
            decode_log.rename(options.out / f'{name}-rejected-{attempt}-decode.txt')
        else:
            raise RuntimeError(f'No complete C2 recording for {name}; inspect retained rejected logs')


if __name__ == '__main__':
    main()
