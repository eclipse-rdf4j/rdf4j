import decimal, re
def load(path):
    d = {}
    try:
        for line in open(path, encoding='utf-8'):
            p = line.rstrip('\n').split('\t')
            if len(p) >= 5:
                d[(p[0], p[1], p[3])] = (p[4], p[5] if len(p) > 5 else '')
    except FileNotFoundError:
        pass
    return d
def key_of(v):
    c = v[0]
    if c.startswith('ERROR'):
        return ' '.join(c.split(' ')[:2])
    return c
NUM = re.compile(r'"([^"]*)"\^\^<http://www.w3.org/2001/XMLSchema#(integer|decimal|double|float|int|long|short|byte)>')
def jnorm(c):
    c = re.sub(r'"@([A-Za-z-]+)', lambda m: '"@' + m.group(1).lower(), c)
    def num(m):
        try:
            return 'NUM(%s)' % decimal.Decimal(m.group(1)).normalize()
        except Exception:
            return m.group(0)
    c = NUM.sub(num, c)
    if c.startswith('ROWS'):
        head, _, body = c.partition(' :: ')
        c = head + ' :: ' + ' ;; '.join(sorted(body.split(' ;; ')) if body else [])
    return c
def parse_corpus(path):
    cases, queries = {}, {}
    case = sec = None; buf = []
    def flush():
        if sec == 'DATA': cases[case]['data'] = ''.join(buf)
        elif sec == 'UPDATE': cases[case]['update'] = ''.join(buf)
        elif sec and sec.startswith('QUERY'): queries[(case, sec.split()[1])] = ''.join(buf)
    for line in open(path, encoding='utf-8'):
        if line.startswith('### '):
            flush(); buf = []
            parts = line[4:].strip()
            if parts.startswith('CASE'):
                case = parts.split()[1]; cases[case] = {}; sec = None
            else:
                sec = parts
            continue
        buf.append(line)
    return cases, queries
