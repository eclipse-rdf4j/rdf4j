#!/usr/bin/env python3
"""Decode HotSpot's emitted C2 machine bytes using objdump when hsdis is absent.
No assembly is inferred from Java source. Every byte must be present in the JVM log.
Usage: python3 disassemble.py c2.log output-directory
"""
from pathlib import Path
import re
import subprocess
import sys

if len(sys.argv) != 3:
    raise SystemExit(__doc__)
source = Path(sys.argv[1]).read_text()
out = Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
results = []
for number, part in enumerate(re.split(r'(?=^Compiled method \(c2\))', source, flags=re.M)):
    if not part.startswith('Compiled method (c2)'):
        continue
    title = part.splitlines()[0]
    name_match = re.search(r'(\S+::\S+) (?:@ \d+ )?\(', title)
    if not name_match:
        continue
    name = name_match.group(1)
    range_match = re.search(r'main code\s+\[0x([0-9a-f]+),0x([0-9a-f]+)\]', part)
    if not range_match:
        raise RuntimeError('No main-code range: ' + title)
    start, end = (int(x, 16) for x in range_match.groups())
    data = {}
    code = part.split('[MachCode]', 1)[1].split('[/MachCode]', 1)[0]
    for match in re.finditer(r'^\s*0x([0-9a-f]+):\s*([0-9a-f][0-9a-f |]*)(?:\n|$)', code, re.M):
        address = int(match.group(1), 16)
        text = match.group(2).replace('|', '').replace(' ', '')
        raw = bytes.fromhex(text)
        for i, byte in enumerate(raw):
            if start <= address+i < end:
                if address+i in data and data[address+i] != byte:
                    raise RuntimeError('Inconsistent bytes')
                data[address+i] = byte
    missing = [address for address in range(start, end) if address not in data]
    if missing:
        raise RuntimeError(f'Missing {len(missing)} machine bytes: {title}; first {missing[:4]}')
    binary = bytes(data[address] for address in range(start, end))
    filename = f'{number:02d}-' + name.rsplit('.', 1)[-1].replace('::', '-').replace('$', '-')
    path = out / (filename + '.bin')
    path.write_bytes(binary)
    command = ['objdump', '-D', '-b', 'binary', '-m', 'i386:x86-64', '-M', 'intel', '--adjust-vma='+str(start), str(path)]
    result = subprocess.run(command, check=True, capture_output=True, text=True).stdout
    (out / (filename + '.asm')).write_text(title + '\n\n' + result)
    instructions = re.findall(r'^\s*[0-9a-f]+:\s+(?:[0-9a-f]{2} )+\s+(.+)$', result, re.M)
    summary = f'{number:02d} {name} main_code_bytes={len(binary)} instructions={len(instructions)} calls={sum(bool(re.match(r"call\b", x)) for x in instructions)}'
    results.append(summary)
(out / 'summary.txt').write_text('\n'.join(results)+'\n')
print('\n'.join(results))
