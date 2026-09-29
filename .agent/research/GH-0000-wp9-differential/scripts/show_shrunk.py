#!/usr/bin/env python3
"""Group shrunk reproducers by normalized query shape: show_shrunk.py <dir>..."""
import glob, json, re, sys, collections
groups = collections.defaultdict(list)
for d in sys.argv[1:]:
    for f in sorted(glob.glob(d + '/*.json')):
        r = json.load(open(f))
        q = ' '.join(l for l in r['query'].split('\n') if not l.startswith('PREFIX'))
        q = ' '.join(q.split())
        data = ' '.join(l for l in r['data'].split('\n') if l and not l.startswith('@prefix'))
        groups[q].append((f.split('/')[-1], r['store'], r['mode'], data, r['dev'][:160], r['br'][:160]))
for q, items in sorted(groups.items(), key=lambda x: -len(x[1])):
    print(f'[{len(items)}] Q: {q}')
    for it in items[:2]:
        print('     ', it[0], it[1], it[2], '| DATA:', it[3])
        print('        dev:', it[4]); print('        br :', it[5])
