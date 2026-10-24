#!/usr/bin/env python3
"""hangcase.py <corpus> <case> <q> <side> <store> <mode> <timeout>: one oracle request in a fresh JVM; HANG on timeout."""
import os, sys, threading, time
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
from shrink import Side
from l3diff_lib import parse_corpus
corpus, case, q, side, store, mode, to = sys.argv[1:8]
cases, queries = parse_corpus(corpus)
s = Side(side); res = []
t0 = time.time()
t = threading.Thread(target=lambda: res.append(s.ask(store, mode, cases[case]['data'], cases[case]['update'], queries[(case, q)])), daemon=True)
t.start(); t.join(float(to))
print(case, q, side, store, mode, ('DONE %.1fs ' % (time.time() - t0) + res[0][:100]) if res else 'HANG pid=%d' % s.p.pid)
if res: s.close()
else: s.p.kill()
