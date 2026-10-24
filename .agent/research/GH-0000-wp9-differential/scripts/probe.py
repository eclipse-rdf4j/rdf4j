#!/usr/bin/env python3
"""Three-way probe: probe.py <store> <mode> <data-ttl-body> <query-body> [update-body] ...  (several queries separated by ' ||| ')"""
import os, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__)); R = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from shrink import Side
PFX = "PREFIX : <http://ex.org/>\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
TPFX = "@prefix : <http://ex.org/> .\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
store, mode, data = sys.argv[1], sys.argv[2], TPFX + sys.argv[3] + '\n'
qs = [PFX + q.strip() for q in sys.argv[4].split('|||')]
upd = PFX + (sys.argv[5] if len(sys.argv) > 5 else 'INSERT DATA { }')
corpus = f'{R}/results/probe.corpus'
with open(corpus, 'w') as o:
    o.write(f'### CASE p\n### DATA\n{data}')
    for i, q in enumerate(qs):
        o.write(f'### QUERY {i} \n{q}\n')
    o.write(f'### UPDATE\n{upd}\n### END\n')
cp = open(f'{R}/results/jena.cp').read().strip()
subprocess.run(['java', '-cp', f'{R}/build/jena:{cp}', 'JenaRunner', corpus, f'{R}/results/probe.jena.tsv'], stderr=subprocess.DEVNULL)
jena = {}
for l in open(f'{R}/results/probe.jena.tsv'):
    p = l.rstrip('\n').split('\t')
    jena[(p[1], p[3])] = '\t'.join(p[4:])
dev, br = Side('dev'), Side('br')
for i, q in enumerate(qs):
    print('Q:', ' '.join(q.split('\n')[2:]))
    print('   dev :', dev.ask(store, mode, data, upd, q)[:300])
    print('   br  :', br.ask(store, mode, data, upd, q)[:300])
    print('   jena:', jena.get((str(i), 'post' if mode in ('post', 'write') else 'pre'), '?')[:300])
dev.close(); br.close()
