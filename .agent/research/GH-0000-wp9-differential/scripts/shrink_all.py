#!/usr/bin/env python3
"""Shrink every distinct develop-vs-branch divergence for a store: shrink_all.py <results> <corpus> <store> [jobs]"""
import collections, concurrent.futures, json, os, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from l3diff_lib import load, key_of
R, corpus, st = sys.argv[1], sys.argv[2], sys.argv[3]
jobs = int(sys.argv[4]) if len(sys.argv) > 4 else 4
dv, br = load(f'{R}/l3.dev.{st}.tsv'), load(f'{R}/l3.br.{st}.tsv')
ph = collections.defaultdict(set)
for k in set(dv) & set(br):
    if k[1] != '-' and key_of(dv[k]) != key_of(br[k]):
        ph[(k[0], k[1])].add(k[2])
os.makedirs(f'{R}/shrunk/{st}', exist_ok=True)
def mode_of(p):
    for m, keys in (('cold', ('cold',)), ('post', ('postcold',)), ('warm', ('seq1', 'seq2')), ('write', ('write1', 'write2'))):
        if any(k in p for k in keys):
            return m
    return 'reopen'
def one(item):
    (c, q), p = item
    m = mode_of(p)
    out = f'{R}/shrunk/{st}/{c}_q{q}.json'
    if m == 'reopen' or os.path.exists(out) or c in os.environ.get('SKIP_CASES', '').split(','):
        return c, q, m, 'skip'
    r = subprocess.run([sys.executable, f'{HERE}/shrink.py', corpus, c, q, st, m, out], capture_output=True, text=True, timeout=1800)
    return c, q, m, 'ok' if os.path.exists(out) else r.stdout[-200:]
with concurrent.futures.ThreadPoolExecutor(jobs) as ex:
    for res in ex.map(one, sorted(ph.items())):
        print(*res)
