#!/usr/bin/env python3
"""Compare compliance result dumps (DiffDump) of two sides: dumpdiff.py <dev-dir> <br-dir>."""
import collections, glob, os, re, sys

def norm(s):
    return re.sub(r'\S*?/rdf4j-sparql-testsuite[^!]*!/', '', re.sub(r'(jar:)?file:[^ !]*!/', '', s))

def canon(body):
    body = re.sub(r'<jar:file:[^>!]*!/', '<JAR!/', body)
    body = re.sub(r'_:[A-Za-z0-9_-]+', '_:b', body)
    out, block = [], []
    for line in body.split('\n'):
        if line.startswith(('TUPLE', 'GRAPH', 'BOOLEAN', 'ERROR', 'ORDERED', '#DUP')) or line == '':
            out.extend(sorted(block)); block = []; out.append(line)
        else:
            block.append(line)
    out.extend(sorted(block))
    return '\n'.join(out)

def load(root):
    out = {}
    for store in sorted(os.listdir(root)):
        for f in glob.glob(os.path.join(root, store, '*.txt')):
            lines = open(f).read().split('\n')
            key = (store, norm(lines[0]), norm(lines[1]))
            body = canon('\n'.join(lines[2:]))
            out[key] = out[key] + '\n#DUP\n' + body if key in out else body
    return out

a, b = load(sys.argv[1]), load(sys.argv[2])
tot = collections.Counter()
for k in sorted(set(a) | set(b)):
    if k not in a or k not in b:
        tot['missing-' + ('dev' if k not in a else 'br')] += 1
        print('MISSING', 'dev' if k not in a else 'br', k)
    elif a[k] != b[k]:
        tot['diff'] += 1
        print('DIFF', k, '\n  dev:', a[k][:500].replace('\n', ' / '), '\n  br :', b[k][:500].replace('\n', ' / '))
    else:
        tot['same:' + k[0].split('.')[-1]] += 1
print(dict(tot), 'total same', sum(v for k, v in tot.items() if k.startswith('same')))
