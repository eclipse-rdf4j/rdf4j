#!/usr/bin/env python3
"""Delta-debugging shrinker for develop-vs-branch divergences (WP9 3.11.6 Layer 3).

Usage: shrink.py <corpus> <case> <query-idx> <store> <mode: cold|post|warm|write> [out.json]

Keeps the divergence *kind* fixed: (dev error class or 'RES', br error class or 'RES') and requires the two sides to
differ. Reduces query parts (triples, OPTIONAL/MINUS/FILTER/BIND/VALUES/sub-select/UNION/GRAPH/LATERAL blocks at any
depth) and data triples until a fixpoint.
"""
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from l3diff_lib import parse_corpus, jnorm  # noqa: E402

SP = '/private/tmp/claude-501/-Users-havardottestad-Documents-Programming-rdf4j-small-things/' \
     'fe2b5b67-a109-4362-a39f-72d9e6f6dd73/scratchpad'


class Side:
    def __init__(self, side):
        cp = open(f'{R}/results/{side}.cp').read().strip()
        os.makedirs(f'{SP}/oracle-{side}', exist_ok=True)
        opts = os.environ.get('DIFF_JAVA_OPTS', '').split()
        self.p = subprocess.Popen(
            ['java', '-Xmx' + os.environ.get('DIFF_XMX', '2g'), f'-Dlogback.configurationFile={R}/scripts/logback-quiet.xml',
             f'-Ddiff.tmp={SP}/oracle-{side}'] + opts + ['-cp', f'{R}/build/{side}:{cp}', 'Oracle'],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, encoding='utf-8')

    def ask(self, kind, mode, data, update, query):
        enc = lambda s: s.replace('\n', '\u001e')
        self.p.stdin.write('\u001f'.join([kind, mode, enc(data), enc(update), enc(query)]) + '\n')
        self.p.stdin.flush()
        return self.p.stdout.readline().rstrip('\n')

    def close(self):
        self.p.stdin.close()
        self.p.wait(timeout=60)


def kind_of(res):
    if res.startswith('ERROR'):
        return res.split('\t')[0].split(' ')[1] if ' ' in res.split('\t')[0] else 'ERROR'
    return 'RES'


def comparable(res):
    return res.split('\t')[0] if res.startswith('ERROR') else res.split('\t')[0]


def read_balanced(s, i, open_ch, close_ch):
    """s[i] == open_ch; return index after the matching close."""
    depth, j, inq = 0, i, False
    while j < len(s):
        c = s[j]
        if inq:
            if c == '\\':
                j += 2
                continue
            if c == '"':
                inq = False
        elif c == '"':
            inq = True
        elif c == open_ch:
            depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0:
                return j + 1
        j += 1
    raise ValueError('unbalanced')


def skip_ws(s, i):
    while i < len(s) and s[i].isspace():
        i += 1
    return i


def parts_of_group(s, gstart):
    """s[gstart] == '{'. Returns (parts, subgroups): parts = [(start,end)], subgroups = [start of nested '{']."""
    gend = read_balanced(s, gstart, '{', '}')
    i = skip_ws(s, gstart + 1)
    parts, subs = [], []
    if s.startswith('SELECT', i):  # sub-select: descend into its WHERE group only
        w = s.find('WHERE', i)
        if w != -1 and w < gend:
            subs.append(s.index('{', w))
        return parts, subs
    while i < gend - 1:
        st = i
        if s.startswith(('OPTIONAL', 'MINUS', 'LATERAL'), i) or s.startswith('GRAPH', i):
            b = s.index('{', i)
            subs.append(b)
            i = read_balanced(s, b, '{', '}')
        elif s.startswith('FILTER', i):
            k = skip_ws(s, i + 6)
            if s[k] == '(':
                inner_ex = s.find('EXISTS', k)
                e = read_balanced(s, k, '(', ')')
                if inner_ex != -1 and inner_ex < e:
                    subs.append(s.index('{', inner_ex))
                i = e
            else:
                b = s.index('{', k)
                subs.append(b)
                i = read_balanced(s, b, '{', '}')
        elif s.startswith('BIND', i):
            k = s.index('(', i)
            e = read_balanced(s, k, '(', ')')
            ex = s.find('EXISTS', k)
            if ex != -1 and ex < e:
                subs.append(s.index('{', ex))
            i = e
        elif s.startswith('VALUES', i):
            b = s.index('{', i)
            i = read_balanced(s, b, '{', '}')
        elif s[i] == '{':
            subs.append(i)
            i = read_balanced(s, i, '{', '}')
            k = skip_ws(s, i)
            while s.startswith('UNION', k):
                b = s.index('{', k)
                subs.append(b)
                i = read_balanced(s, b, '{', '}')
                k = skip_ws(s, i)
        else:  # triple pattern, terminated by ' .'
            depth, j, inq = 0, i, False
            while j < gend - 1:
                c = s[j]
                if inq:
                    if c == '"':
                        inq = False
                elif c == '"':
                    inq = True
                elif c in '({':
                    depth += 1
                elif c in ')}':
                    depth -= 1
                elif c == '.' and depth == 0 and s[j - 1] == ' ' and (j + 1 >= len(s) or s[j + 1] in ' }'):
                    break
                j += 1
            i = j + 1
        parts.append((st, i))
        i = skip_ws(s, i)
    return parts, subs


def all_spans(q):
    if '{' not in q:
        return []
    start = q.index('{')
    spans, todo, seen = [], [start], set()
    while todo:
        g = todo.pop()
        if g in seen:
            continue
        seen.add(g)
        try:
            parts, subs = parts_of_group(q, g)
        except (ValueError, IndexError):
            continue
        spans.extend(parts)
        todo.extend(subs)
    spans.sort(key=lambda x: -(x[1] - x[0]))
    return spans


def main():
    corpus, case, qi, store, mode = sys.argv[1:6]
    cases, queries = parse_corpus(corpus)
    data, update, query = cases[case]['data'], cases[case].get('update', ''), queries[(case, qi)]
    dev, br = Side('dev'), Side('br')

    def probe(d, q):
        a, b = dev.ask(store, mode, d, update, q), br.ask(store, mode, d, update, q)
        return a, b

    a0, b0 = probe(data, query)
    target = (kind_of(a0), kind_of(b0))
    if comparable(a0) == comparable(b0):
        print('NO DIVERGENCE', a0[:200], b0[:200])
        return

    def still(d, q):
        a, b = probe(d, q)
        if 'MalformedQuery' in a or 'MalformedQuery' in b:
            return False, a, b
        return (kind_of(a), kind_of(b)) == target and comparable(a) != comparable(b), a, b

    changed = True
    while changed:
        changed = False
        # query parts
        for (s, e) in all_spans(query):
            cand = query[:s] + query[e:]
            cand = re.sub(r'\{\s*\}', '{ }', cand)
            ok, a, b = still(data, cand)
            if ok:
                query, changed = cand, True
                break
        if changed:
            continue
        # projection -> *
        m = re.search(r'SELECT (DISTINCT )?([?\w ]+) WHERE', query.split('{', 1)[0] + '{')
        if m and m.group(2).strip() != '*':
            cand = query.replace(m.group(0), 'SELECT * WHERE', 1)
            ok, a, b = still(data, cand)
            if ok:
                query, changed = cand, True
                continue
        # data triples
        lines = data.split('\n')
        for i, l in enumerate(lines):
            if l.startswith('@prefix') or not l.strip() or l.strip() in (':g {', '}'):
                continue
            cand = '\n'.join(lines[:i] + lines[i + 1:])
            ok, a, b = still(cand, query)
            if ok:
                data, changed = cand, True
                break
    a, b = probe(data, query)
    res = {'case': case, 'q': qi, 'store': store, 'mode': mode, 'data': data, 'update': update,
           'query': query, 'dev': a, 'br': b}
    dev.close()
    br.close()
    print(json.dumps(res, indent=1))
    if len(sys.argv) > 6:
        json.dump(res, open(sys.argv[6], 'w'), indent=1)


if __name__ == '__main__':
    main()
