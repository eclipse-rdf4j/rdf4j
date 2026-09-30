#!/usr/bin/env python3
"""Per-query triage view of develop-vs-branch differences: triage.py <results-dir> <corpus> <store> [maxN]"""
import sys, collections, re
sys.path.insert(0, sys.argv[0].rsplit('/', 1)[0])
R, corpus, st = sys.argv[1], sys.argv[2], sys.argv[3]
maxn = int(sys.argv[4]) if len(sys.argv) > 4 else 999
from l3diff_lib import load, key_of, jnorm, parse_corpus
cases, queries = parse_corpus(corpus)
dv, br, je = load(f'{R}/l3.dev.{st}.tsv'), load(f'{R}/l3.br.{st}.tsv'), load(f'{R}/l3.jena.tsv')
diff = collections.OrderedDict()
for k in sorted(set(dv) & set(br), key=lambda k: (k[0], k[1])):
    if k[1] != '-' and key_of(dv[k]) != key_of(br[k]):
        diff.setdefault((k[0], k[1]), []).append(k[2])
n = 0
for (c, q), phases in diff.items():
    n += 1
    if n > maxn: break
    print('=' * 100)
    print(f'{c} q{q} phases={phases}')
    print('DATA:', ' '.join(l for l in cases[c]['data'].split('\n') if l and not l.startswith('@prefix')))
    print('UPD :', ' '.join(cases[c]['update'].split('\n')[2:]))
    print('Q   :', ' '.join(queries[(c, q)].split('\n')[2:]))
    for ph in phases[:2]:
        jp = {'cold': 'pre', 'seq1': 'pre', 'seq2': 'pre'}.get(ph, 'post')
        j = je.get((c, q, jp), ('-', ''))
        d, b = dv[(c, q, ph)], br[(c, q, ph)]
        verdict = 'dev=jena' if jnorm(d[0]) == jnorm(j[0]) else ('br=jena' if jnorm(b[0]) == jnorm(j[0]) else 'neither')
        print(f'  [{ph}] {verdict}')
        print('    dev :', d[0][:400], d[1][:150])
        print('    br  :', b[0][:400], b[1][:150])
        print('    jena:', j[0][:400], j[1][:150])
