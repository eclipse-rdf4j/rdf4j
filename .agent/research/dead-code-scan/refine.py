import os,re,subprocess,collections,sys
S=os.environ['S']
SKIP=('/target/','/.mvnf/','/papers2/','/.git/','/.m2_repo/','/profiles/')
ident=re.compile(r'[A-Za-z_][A-Za-z0-9_]*')
files=[]
for dp,dn,fn in os.walk('.'):
    if any(s in dp+'/' for s in SKIP): dn[:]=[]; continue
    for f in fn:
        if f.endswith('.java'): files.append(os.path.join(dp,f))
srcs={f:open(f,encoding='utf-8',errors='ignore').read() for f in files}
cnt=collections.defaultdict(lambda: collections.Counter())  # ident -> {file: count}
for f,t in srcs.items():
    for i in ident.findall(t): cnt[i][f]+=1
# class internal name -> source path
def srcpath(cls):
    top=cls.split('$')[0]
    for base in ('core/sail/lmdb/src/main/java/','core/queryalgebra/evaluation/src/main/java/'):
        p='./'+base+top+'.java'
        if p in srcs: return p
    return None
main_files=set(subprocess.run(['git','ls-tree','-r','--name-only','main'],capture_output=True,text=True).stdout.split('\n'))
main_cache={}
def main_src(p):
    rel=p[2:]
    if rel not in main_files: return None
    if rel not in main_cache: main_cache[rel]=subprocess.run(['git','show','main:'+rel],capture_output=True,text=True).stdout
    return main_cache[rel]
out=open(S+'/refined.tsv','w')
out.write('KIND\tCLASS\tMEMBER\tFLAGS\tSRC\tNOTE\tONMAIN\tOVERRIDE\tSRCREFS\n')
for line in open(S+'/dead.tsv'):
    parts=line.rstrip('\n').split('\t')
    if parts[0]=='KIND' or parts[0]=='OUTER-ONLY-CLASS': continue
    kind,cls,member,flags,src,note=parts[:6]
    p=srcpath(cls)
    t=srcs.get(p,'') if p else ''
    ms=main_src(p) if p else None
    name=member.split('(')[0].split(' ')[0]
    if name=='<init>': name=cls.split('$')[-1].split('/')[-1]
    onmain='n/a'
    if ms is not None:
        onmain='yes' if re.search(r'\b'+re.escape(name)+r'\b',ms) else 'no'
    elif p: onmain='newfile'
    override=''
    if kind.endswith('METHOD') and t:
        for m in re.finditer(r'\b'+re.escape(name)+r'\s*\(',t):
            pre=t[max(0,m.start()-300):m.start()]
            if '@Override' in pre.split(';')[-1].split('}')[-1]: override='@Override'; break
    srcrefs=''
    if kind.startswith('UNREF-CONST') or kind.endswith('FIELD'):
        c=cnt.get(name,{}); own=c.get(p,0); other_main=sum(v for f,v in c.items() if f!=p and '/src/main/' in f); other_test=sum(v for f,v in c.items() if f!=p and '/src/test/' in f)
        srcrefs=f'own={own};main={other_main};test={other_test}'
        if kind.startswith('UNREF-CONST'):
            if own<=1 and other_main==0 and other_test==0: kind='DEAD-CONST'
            elif own<=1 and other_main==0: kind='TESTONLY-CONST'
            else: continue
    out.write('\t'.join([kind,cls,member,flags,src,note,onmain,override,srcrefs])+'\n')
out.close()
