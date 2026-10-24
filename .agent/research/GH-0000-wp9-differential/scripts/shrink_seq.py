#!/usr/bin/env python3
"""Sequence shrinker for L4: warm [A, B, T] x2, update, run B, run T; bug = branch T returns 'ROWS 0'."""
import json, os, re, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__)); R = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from l3diff_lib import parse_corpus
from shrink import all_spans
SP = '/private/tmp/claude-501/-Users-havardottestad-Documents-Programming-rdf4j-small-things/fe2b5b67-a109-4362-a39f-72d9e6f6dd73/scratchpad'
cases, qs = parse_corpus(R + '/results/l3.corpus')
c = 's20260929c388'
data, upd = cases[c]['data'], cases[c]['update']
A, B, T = qs[(c, '1')], qs[(c, '10')], qs[(c, '11')]
corpus = SP + '/seqshrink.corpus'
def run(side, data, A, B, T):
    with open(corpus, 'w') as o:
        o.write(f'### CASE x\n### DATA\n{data}### QUERY 0 \n{A}### QUERY 1 \n{B}### QUERY 2 \n{T}### UPDATE\n{upd}### END\n')
    cp = open(f'{R}/results/{side}.cp').read().strip()
    return subprocess.run(['java', '-Ddiff.lmdbIndexes=spoc,posc', f'-Ddiff.tmp={SP}/seqtmp',
        f'-Dlogback.configurationFile={R}/scripts/logback-quiet.xml', '-cp', f'{R}/build2/{side}:{cp}', 'SeqRepro',
        corpus, 'x', '2', '0,1,2', 'lmdb', '1'], capture_output=True, text=True).stdout.strip()
def bug(data, A, B, T):
    return run('br', data, A, B, T).startswith('ROWS 0')
assert bug(data, A, B, T)
changed = True
while changed:
    changed = False
    for which in ('A', 'B'):
        q = A if which == 'A' else B
        for s, e in all_spans(q):
            cand = re.sub(r'\{\s*\}', '{ }', q[:s] + q[e:])
            args = (data, cand, B, T) if which == 'A' else (data, A, cand, T)
            if bug(*args):
                if which == 'A': A = cand
                else: B = cand
                changed = True
                break
        if changed: break
    if changed: continue
    lines = data.split('\n')
    for i, l in enumerate(lines):
        if l.startswith('@prefix') or not l.strip(): continue
        cand = '\n'.join(lines[:i] + lines[i + 1:])
        if bug(cand, A, B, T):
            data, changed = cand, True
            break
res = {'data': data, 'update': upd, 'A': A, 'B': B, 'T': T, 'br': run('br', data, A, B, T), 'dev': run('dev', data, A, B, T)}
json.dump(res, open(R + '/results/shrunk/l4-seq.json', 'w'), indent=1)
print(json.dumps(res, indent=1))
