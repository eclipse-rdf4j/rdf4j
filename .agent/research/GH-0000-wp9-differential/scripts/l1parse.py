#!/usr/bin/env python3
"""Flatten Failsafe TEST-*.xml into TSV: class, method (phrased), status, message. Duplicate names get #n."""
import glob, sys
import xml.etree.ElementTree as ET
res = {}
for f in sorted(glob.glob(sys.argv[1] + '/TEST-*.xml')):
    for tc in ET.parse(f).getroot().iter('testcase'):
        cls, name = tc.get('classname'), tc.get('name')
        name = name.split('!/', 1)[-1] if '!/' in name else name
        st, msg = 'PASS', ''
        for tag, lab in (('failure', 'FAIL'), ('error', 'ERROR'), ('skipped', 'SKIP')):
            e = tc.find(tag)
            if e is not None:
                st = lab
                msg = ((e.get('type') or '') + ': ' + (e.get('message') or ''))[:200]
                break
        k, i = (cls, name), 1
        while k in res:
            i += 1
            k = (cls, name + ' #%d' % i)
        res[k] = (st, ' '.join(msg.split()))
for (c, n), (st, msg) in sorted(res.items()):
    print('\t'.join([c, ' '.join(n.split()), st, msg]))
