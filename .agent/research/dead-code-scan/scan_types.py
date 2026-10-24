import os, re, sys, collections
ROOT='.'
SKIP=('/target/','/.mvnf/','/papers2/','/.git/','/node_modules/','/.m2_repo/','/profiles/')
ident=re.compile(r'[A-Za-z_][A-Za-z0-9_]*')
decl=re.compile(r'\b(?:class|interface|enum|record)\s+([A-Z][A-Za-z0-9_]*)')
files=[]; resfiles=[]
for dp,dn,fn in os.walk(ROOT):
    if any(s in dp+'/' for s in SKIP): dn[:]=[]; continue
    for f in fn:
        p=os.path.join(dp,f)
        if f.endswith('.java'): files.append(p)
        elif f.endswith(('.xml','.properties','.md','.txt','.json','.yml','.yaml','.kt','.sh','.py','.template','.csv')) or 'META-INF' in dp: resfiles.append(p)
index=collections.defaultdict(set)   # ident -> files (java)
resindex=collections.defaultdict(set)
srcs={}
for f in files:
    t=open(f,encoding='utf-8',errors='ignore').read(); srcs[f]=t
    for i in set(ident.findall(t)): index[i].add(f)
for f in resfiles:
    try: t=open(f,encoding='utf-8',errors='ignore').read()
    except: continue
    if len(t)>5_000_000: continue
    for i in set(ident.findall(t)): resindex[i].add(f)
SCOPE=[p for p in files if ('/core/sail/lmdb/src/main/' in p) or ('/core/queryalgebra/evaluation/src/main/' in p and '/optimizer/' in p)]
print("kind\tname\tfile\tmainrefs\ttestrefs\townrefs\tresrefs")
for f in sorted(SCOPE):
    t=srcs[f]; top=os.path.basename(f)[:-5]
    names=set(decl.findall(t)); names.add(top)
    for n in sorted(names):
        if len(n)<3: continue
        refs=index.get(n,set())-{f}
        main=[g for g in refs if '/src/main/' in g]; test=[g for g in refs if '/src/test/' in g]
        if not main:
            own=len(re.findall(r'\b'+n+r'\b',t))
            print('\t'.join(map(str,['TOP' if n==top else 'NESTED',n,f,len(main),len(test),own,len(resindex.get(n,()))])))
