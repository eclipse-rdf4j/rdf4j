#!/usr/bin/env python3
"""Print surefire.test.class.path recorded in a Failsafe/Surefire TEST-*.xml report (all entries must exist)."""
import glob, os, re, sys
f = sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*Memory*.xml')))[0]
cp = [p for p in re.search(r'name="surefire.test.class.path" value="([^"]*)"', open(f).read()).group(1).split(':') if p]
missing = [p for p in cp if not os.path.exists(p)]
if missing:
    sys.exit('missing: %s' % missing[:5])
print(':'.join(cp))
