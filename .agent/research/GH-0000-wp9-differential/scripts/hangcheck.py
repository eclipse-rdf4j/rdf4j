#!/usr/bin/env python3
"""hangcheck.py <side> <store> <timeout-s> <data-body> <query-body>: run once in a fresh oracle JVM; report HANG on timeout."""
import os, subprocess, sys, threading
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
from shrink import Side
PFX = "PREFIX : <http://ex.org/>\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
TPFX = "@prefix : <http://ex.org/> .\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
side, store, to, data, q = sys.argv[1], sys.argv[2], float(sys.argv[3]), sys.argv[4], sys.argv[5]
s = Side(side)
res = []
t = threading.Thread(target=lambda: res.append(s.ask(store, 'cold', TPFX + data, PFX + 'INSERT DATA {}', PFX + q)), daemon=True)
t.start(); t.join(to)
if res:
    print('DONE', res[0][:300]); s.close()
else:
    print('HANG'); s.p.kill()
