#!/usr/bin/env python3
"""Run Jena on shrunk reproducers and print a verdict per file: jena_verdict.py <out.tsv> <shrunk-dir>..."""
import glob, json, os, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__)); R = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from l3diff_lib import jnorm
files = [f for d in sys.argv[2:] for f in sorted(glob.glob(d + '/*.json'))]
corpus = f'{R}/results/shrunk.corpus'
with open(corpus, 'w') as out:
    for i, f in enumerate(files):
        r = json.load(open(f))
        out.write(f"### CASE k{i}\n### DATA\n{r['data'].rstrip()}\n### QUERY 0 \n{r['query'].rstrip()}\n### UPDATE\n{r['update'].rstrip()}\n### END\n")
cp = open(f'{R}/results/jena.cp').read().strip()
subprocess.run(['java', '-cp', f'{R}/build/jena:{cp}', 'JenaRunner', corpus, f'{R}/results/shrunk.jena.tsv'], stderr=subprocess.DEVNULL)
jena = {}
for l in open(f'{R}/results/shrunk.jena.tsv'):
    p = l.rstrip('\n').split('\t')
    jena[(p[0], p[3])] = p[4] + ('\t' + p[5] if len(p) > 5 else '')
rows = []
for i, f in enumerate(files):
    r = json.load(open(f))
    j = jena.get((f'k{i}', 'post' if r['mode'] in ('post', 'write') else 'pre'), '?')
    d, b = r['dev'].split('\t')[0], r['br'].split('\t')[0]
    jn = jnorm(j.split('\t')[0])
    v = 'dev=jena' if jnorm(d) == jn else 'br=jena' if jnorm(b) == jn else 'neither'
    rows.append((v, os.path.basename(f), d[:120], b[:120], j[:160]))
with open(sys.argv[1], 'w') as o:
    for row in rows:
        o.write('\t'.join(row) + '\n')
        print(*row, sep='\n   ')
