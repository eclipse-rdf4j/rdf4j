#!/usr/bin/env python3
"""Layer 3 comparison. Usage: l3diff.py <results-dir> <corpus> [stores]

Checks, per (case, query):
  X  develop vs branch, same store + phase                        (the differential)
  P  branch phase consistency: seq1/seq2/cold agree; write1/write2/reopen agree with postcold
  S  branch store consistency: memory vs native vs lmdb (cold and postcold)
  J  Jena oracle vs develop/branch (pre=cold, post=postcold), with lang-tag/numeric normalization
"""
import collections
import decimal
import re
import sys

R = sys.argv[1]
corpus = sys.argv[2]
stores = sys.argv[3].split(',') if len(sys.argv) > 3 else ['memory', 'native', 'lmdb']


def load(path):
    d = {}
    try:
        for line in open(path, encoding='utf-8'):
            p = line.rstrip('\n').split('\t')
            if len(p) < 5:
                continue
            d[(p[0], p[1], p[3])] = (p[4], p[5] if len(p) > 5 else '')
    except FileNotFoundError:
        pass
    return d


def key_of(v):
    c = v[0]
    if c.startswith('ERROR'):
        return c.split(' ')[0] + ' ' + (c.split(' ')[1] if ' ' in c else '')
    return c


NUM = re.compile(r'"([^"]*)"\^\^<http://www.w3.org/2001/XMLSchema#(integer|decimal|double|float|int|long|short|byte)>')


def jnorm(c):
    """Normalization used only for Jena comparisons (implementation-defined lexical forms)."""
    c = re.sub(r'"@([A-Za-z-]+)', lambda m: '"@' + m.group(1).lower(), c)

    def num(m):
        try:
            return 'NUM(%s)' % decimal.Decimal(m.group(1)).normalize()
        except Exception:
            return m.group(0)
    c = NUM.sub(num, c)
    if c.startswith('ROWS'):
        head, _, body = c.partition(' :: ')
        rows = sorted(body.split(' ;; ')) if body else []
        c = head + ' :: ' + ' ;; '.join(rows)
    return c


queries = {}
cur = None
for line in open(corpus, encoding='utf-8'):
    if line.startswith('### CASE'):
        case = line.split()[2]
    elif line.startswith('### QUERY'):
        cur = (case, line.split()[2])
        queries[cur] = ''
    elif line.startswith('###'):
        cur = None
    elif cur:
        queries[cur] += line

jena = load(f'{R}/l3.jena.tsv')
out = collections.defaultdict(list)
cnt = collections.Counter()
data = {}
for st in stores:
    for side in ('dev', 'br'):
        data[(side, st)] = load(f'{R}/l3.{side}.{st}.tsv')

for st in stores:
    dv, br = data[('dev', st)], data[('br', st)]
    if not dv or not br:
        continue
    for k in sorted(set(dv) & set(br)):
        cnt['X-compared:' + st] += 1
        if key_of(dv[k]) != key_of(br[k]):
            cnt['X-diff:' + st] += 1
            out['X'].append((st, k, dv[k][0][:300], br[k][0][:300]))
    for k in sorted(set(dv) ^ set(br)):
        cnt['X-missing:' + st] += 1

for side in ('br', 'dev'):
    for st in stores:
        d = data[(side, st)]
        for (case, q, ph), v in d.items():
            if q == '-':
                if v[0] != 'OK':
                    out['U'].append((side, st, case, ph, v))
                continue
            if ph in ('seq1', 'seq2'):
                ref = d.get((case, q, 'cold'))
            elif ph in ('write1', 'write2', 'reopen'):
                ref = d.get((case, q, 'postcold'))
            else:
                continue
            if ref is None:
                continue
            cnt[f'P-compared:{side}:{st}'] += 1
            if key_of(ref) != key_of(v) and 'TIMEOUT' not in (ref[0], v[0]):
                cnt[f'P-diff:{side}:{st}'] += 1
                out['P'].append((side, st, (case, q, ph), ref[0][:300], v[0][:300]))
    for i, a in enumerate(stores):
        for b in stores[i + 1:]:
            da, db = data[(side, a)], data[(side, b)]
            for k in da:
                if k[2] in ('cold', 'postcold') and k in db:
                    cnt[f'S-compared:{side}:{a}-{b}'] += 1
                    if key_of(da[k]) != key_of(db[k]):
                        cnt[f'S-diff:{side}:{a}-{b}'] += 1
                        out['S'].append((side, a, b, k, da[k][0][:300], db[k][0][:300]))

for side in ('dev', 'br'):
    for st in stores:
        d = data[(side, st)]
        for (case, q, ph), v in d.items():
            jp = {'cold': 'pre', 'postcold': 'post'}.get(ph)
            if not jp or q == '-':
                continue
            j = jena.get((case, q, jp))
            if j is None or j[0].startswith('ERROR') or v[0].startswith('ERROR') or v[0] == 'TIMEOUT':
                continue
            cnt[f'J-compared:{side}:{st}'] += 1
            if jnorm(j[0]) != jnorm(v[0]):
                cnt[f'J-diff:{side}:{st}'] += 1
                out['J'].append((side, st, (case, q, ph), j[0][:300], v[0][:300]))

for k in sorted(cnt):
    print(f'{k}\t{cnt[k]}')
with open(f'{R}/l3diff.details.txt', 'w', encoding='utf-8') as f:
    for cat in ('X', 'P', 'S', 'U', 'J'):
        f.write(f'===== {cat} ({len(out[cat])})\n')
        for item in out[cat]:
            f.write(repr(item) + '\n')
            key = None
            for x in item:
                if isinstance(x, tuple) and len(x) == 3:
                    key = x
            if key and (key[0], key[1]) in queries:
                f.write('   Q: ' + ' '.join(queries[(key[0], key[1])].split()) + '\n')
