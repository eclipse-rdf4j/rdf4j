#!/usr/bin/env python3
"""Remove Java members listed in a manifest. Manifest TSV: KIND, CLASS(internal), MEMBER(name+desc | 'field desc' | '-')."""
import os,re,sys,collections
S=os.environ['S']
LOG=open(S+'/remove.log','a')
def log(*a): print(*a); print(*a,file=LOG)
PRIM={'B':'byte','C':'char','D':'double','F':'float','I':'int','J':'long','S':'short','Z':'boolean','V':'void'}
def parse_desc(desc):
    # returns list of simple param type names
    assert desc.startswith('(')
    i=1; out=[]
    while desc[i]!=')':
        dims=0
        while desc[i]=='[': dims+=1; i+=1
        if desc[i]=='L':
            j=desc.index(';',i); t=desc[i+1:j]; i=j+1
            t=t.split('/')[-1].split('$')[-1]
        else: t=PRIM[desc[i]]; i+=1
        out.append(t+'[]'*dims)
    return out
def code_mask(t):
    """mask[i]=True if char i is code (not in comment/string/char)."""
    n=len(t); m=[True]*n; i=0
    while i<n:
        c=t[i]
        if c=='/' and i+1<n and t[i+1]=='/':
            j=t.find('\n',i); j=n if j<0 else j
            for k in range(i,j): m[k]=False
            i=j
        elif c=='/' and i+1<n and t[i+1]=='*':
            j=t.find('*/',i+2); j=n if j<0 else j+2
            for k in range(i,j): m[k]=False
            i=j
        elif c=='"':
            if t.startswith('"""',i):
                j=t.find('"""',i+3); j=n if j<0 else j+3
            else:
                j=i+1
                while j<n and t[j]!='"':
                    if t[j]=='\\': j+=1
                    j+=1
                j+=1
            for k in range(i,min(j,n)): m[k]=False
            i=j
        elif c=="'":
            j=i+1
            while j<n and t[j]!="'":
                if t[j]=='\\': j+=1
                j+=1
            j+=1
            for k in range(i,min(j,n)): m[k]=False
            i=j
        else: i+=1
    return m
def depths(t,m):
    d=[0]*(len(t)+1); cur=0
    for i,c in enumerate(t):
        d[i]=cur
        if m[i]:
            if c=='{': cur+=1
            elif c=='}': cur-=1
    d[len(t)]=cur
    return d
def find_type_body(t,m,d,chain):
    """chain: list of simple names from top-level. returns (body_open_index, member_depth) or None"""
    start=0; end=len(t); depth=0
    for name in chain:
        pat=re.compile(r'\b(class|interface|enum|record|@interface)\s+'+re.escape(name)+r'\b')
        found=None
        for mm in pat.finditer(t,start,end):
            if not m[mm.start()] or d[mm.start()]!=depth: continue
            found=mm; break
        if not found: return None
        # body open: first '{' at this depth after decl
        i=found.end()
        while i<end and not (m[i] and t[i]=='{' and d[i]==depth): i+=1
        if i>=end: return None
        start=i+1; depth+=1
        # find close
        j=start
        while j<end and not (m[j] and t[j]=='}' and d[j]==depth): j+=1
        end=j
    return (start,end,depth)
def decl_start(t,i):
    """extend backward from member start over annotations and an adjacent javadoc/comment block; return index at line start."""
    # move to line start
    ls=t.rfind('\n',0,i)+1
    # walk backward over lines that are annotations or comment lines
    while True:
        prev_end=ls-1
        if prev_end<0: break
        pls=t.rfind('\n',0,prev_end)+1
        line=t[pls:prev_end].strip()
        if line.startswith('@') and not line.startswith('@interface'):
            ls=pls; continue
        if line.startswith('*') or line.startswith('/*') or line.startswith('//'):
            ls=pls; continue
        break
    return ls
def member_end(t,m,d,i,depth):
    """from index i inside a member header at `depth` (member depth), find end index (exclusive) after ';' or matching '}'."""
    j=i; n=len(t)
    while j<n:
        if m[j]:
            if t[j]==';' and d[j]==depth: return j+1
            if t[j]=='{' and d[j]==depth:
                k=j+1
                while k<n and not (m[k] and t[k]=='}' and d[k]==depth+1): k+=1
                # d[k] is depth+1 at the closing brace char (since depth increments after '{')
                e=k+1
                while e<n and t[e] in ' \t': e+=1
                if e<n and t[e]==';': return e+1
                return k+1
        j+=1
    return None
def cut(t,a,b):
    # remove [a,b) and trailing newline; collapse multiple blank lines
    if b<len(t) and t[b]=='\n': b+=1
    new=t[:a]+t[b:]
    # collapse 3+ newlines to 2 around a
    seg_start=max(0,a-2); seg=new[seg_start:a+2]
    new=new[:seg_start]+re.sub(r'\n{3,}','\n\n',seg)+new[a+2:]
    return new
def split_params(s):
    out=[]; depth=0; cur=''
    for c in s:
        if c in '<([': depth+=1
        elif c in '>)]': depth-=1
        if c==',' and depth==0: out.append(cur); cur=''
        else: cur+=c
    if cur.strip(): out.append(cur)
    return [p.strip() for p in out]
def simple_type(p):
    p=re.sub(r'@\w+(\([^)]*\))?\s*','',p)
    p=re.sub(r'\bfinal\s+','',p).strip()
    # remove generics
    depth=0; out=''
    for c in p:
        if c=='<': depth+=1
        elif c=='>': depth-=1
        elif depth==0: out+=c
    out=out.strip()
    var=out.endswith('...')
    if var: out=out[:-3].strip()
    # type then name
    parts=out.split()
    if len(parts)<2: return None
    ty=parts[0]; name=parts[-1]
    dims=name.count('[')  # C-style arrays
    ty=ty.split('.')[-1]
    if var: ty+='[]'
    ty+='[]'*dims
    return ty
def remove_method(t,cls_chain,name,desc,is_ctor,inner_nonstatic):
    m=code_mask(t); d=depths(t,m)
    body=find_type_body(t,m,d,cls_chain)
    if not body: return t,'type-not-found'
    start,end,depth=body
    want=parse_desc(desc)
    pat=re.compile(r'\b'+re.escape(name)+r'\s*\(')
    cands=[]
    for mm in pat.finditer(t,start,end):
        i=mm.start()
        if not m[i] or d[i]!=depth: continue
        # previous non-space char
        k=i-1
        while k>=0 and t[k] in ' \t\r\n': k-=1
        prev=t[k] if k>=0 else ''
        if prev in '=.(,!&|+-*/?:' or t[max(0,k-5):k+1].endswith('return') or t[max(0,k-2):k+1].endswith('new'): continue
        # parse params
        p=mm.end(); dep=1; j=p
        while j<end and dep>0:
            if m[j]:
                if t[j]=='(': dep+=1
                elif t[j]==')': dep-=1
            j+=1
        params=split_params(t[p:j-1])
        types=[simple_type(x) for x in params] if params else []
        cands.append((i,j,types))
    def match(types,want):
        if len(types)!=len(want): return False
        for a,b in zip(types,want):
            if a is None: return False
            if a==b: continue
            if len(a)==1 and a.isupper(): continue  # generic type var
            if a.endswith('[]') and b.endswith('[]') and len(a.rstrip('[]'))==1: continue
            return False
        return True
    hits=[c for c in cands if match(c[2],want)]
    if not hits and is_ctor and inner_nonstatic and want: hits=[c for c in cands if match(c[2],want[1:])]
    if not hits:
        # generic-erasure fallback: unique by arity
        same=[c for c in cands if len(c[2])==len(want)]
        if len(same)==1 and any(a is not None and (len(a)==1 or a=='Object') for a in same[0][2]): hits=same
    if len(hits)!=1: return t,f'ambiguous-or-missing({len(hits)} of {len(cands)} cands: {[c[2] for c in cands]}) want={want}'
    i,j,_=hits[0]
    e=member_end(t,m,d,j,depth)
    if e is None: return t,'no-end'
    a=decl_start(t,i)
    return cut(t,a,e),'ok'
def remove_field(t,cls_chain,name):
    m=code_mask(t); d=depths(t,m)
    body=find_type_body(t,m,d,cls_chain)
    if not body: return t,'type-not-found'
    start,end,depth=body
    pat=re.compile(r'\b'+re.escape(name)+r'\s*(=|;|,)')
    hits=[]
    pd=[0]*(len(t)+1); cur=0
    for ii,c in enumerate(t):
        pd[ii]=cur
        if m[ii]:
            if c=='(': cur+=1
            elif c==')': cur-=1
    for mm in pat.finditer(t,start,end):
        i=mm.start()
        if not m[i] or d[i]!=depth or pd[i]!=0: continue
        # ensure declaration: previous token is a type (identifier or > or ])
        k=i-1
        while k>=0 and t[k] in ' \t\r\n': k-=1
        if k<0 or not (t[k].isalnum() or t[k] in '>]_'): continue
        # ensure statement-start: walk back to previous ';' '{' '}' at this depth and check no '=' between
        s=k
        while s>=0 and not (m[s] and t[s] in ';{}' and (d[s]==depth or (t[s]=='{' and d[s]==depth-1))): s-=1
        seg=t[s+1:i]
        if '=' in re.sub(r'@\w+\([^)]*\)','',seg): continue
        if ',' in seg.split('\n')[-1] and mm.group(1)!=',': pass
        hits.append((i,s+1))
    if len(hits)!=1: return t,f'field-ambiguous({len(hits)})'
    i,s=hits[0]
    # multi-declarator check
    e=member_end(t,m,d,i,depth)
    if e is None: return t,'no-end'
    decl=t[s:e]
    if re.search(r',\s*\w+\s*(=|;)',re.sub(r'\([^)]*\)|\{[^}]*\}|"[^"]*"','',decl.split('=')[0])): return t,'multi-declarator'
    # declaration start: line start of the first non-space char after s, then extend over annotations/javadoc
    k=s
    while k<i and t[k] in ' \t\r\n': k+=1
    a=decl_start(t,k)
    t=cut(t,a,e)
    # remove simple self-assignments of the field within the type body: this.name = <ident or literal>;
    m2=code_mask(t); d2=depths(t,m2); body2=find_type_body(t,m2,d2,cls_chain)
    if body2:
        s2,e2,dp2=body2
        seg=t[s2:e2]
        newseg,cnt=re.subn(r'\n[ \t]*this\.'+re.escape(name)+r'\s*=\s*(?:[A-Za-z_][A-Za-z0-9_.]*|-?\d[\dLl.]*|true|false|null)\s*;[ \t]*(?=\n)','',seg)
        if cnt: t=t[:s2]+newseg+t[e2:]
        rem=re.findall(r'\n[ \t]*(this\.'+re.escape(name)+r'\s*=[^;]*;)',t[s2:s2+len(newseg)])
        if rem: log('MANUAL-ASSIGN',cls_chain[-1],name,rem)
    return t,'ok'
def remove_nested_type(t,cls_chain):
    m=code_mask(t); d=depths(t,m)
    parent=find_type_body(t,m,d,cls_chain[:-1]) if len(cls_chain)>1 else (0,len(t),0)
    if not parent: return t,'parent-not-found'
    start,end,depth=parent
    name=cls_chain[-1]
    pat=re.compile(r'\b(class|interface|enum|record|@interface)\s+'+re.escape(name)+r'\b')
    for mm in pat.finditer(t,start,end):
        i=mm.start()
        if not m[i] or d[i]!=depth: continue
        e=member_end(t,m,d,mm.end(),depth)
        a=decl_start(t,i)
        return cut(t,a,e),'ok'
    return t,'nested-not-found'
def prune_imports(t):
    lines=t.split('\n'); body='\n'.join(l for l in lines if not l.startswith('import '))
    out=[]; removed=0
    for l in lines:
        mm=re.match(r'import\s+(static\s+)?([\w.]+)\s*;',l)
        if mm and not mm.group(2).endswith('.*'):
            simple=mm.group(2).split('.')[-1]
            if not re.search(r'\b'+re.escape(simple)+r'\b',body): removed+=1; continue
        out.append(l)
    return '\n'.join(out),removed
def find_source(cls):
    top=cls.split('$')[0]
    for base in ('core/sail/lmdb/src/main/java/','core/queryalgebra/evaluation/src/main/java/'):
        p=base+top+'.java'
        if os.path.exists(p): return p,[top.split('/')[-1]]+cls.split('$')[1:]
    # search for a top-level declaration in same package dir
    pkg='/'.join(top.split('/')[:-1]); simple=top.split('/')[-1]
    for base in ('core/sail/lmdb/src/main/java/','core/queryalgebra/evaluation/src/main/java/'):
        dpath=base+pkg
        if not os.path.isdir(dpath): continue
        for f in os.listdir(dpath):
            if not f.endswith('.java'): continue
            tt=open(dpath+'/'+f).read()
            if re.search(r'\b(class|interface|enum|record)\s+'+re.escape(simple)+r'\b',tt):
                return dpath+'/'+f,[simple]+cls.split('$')[1:]
    return None,None
def main():
    manifest=[l.rstrip('\n').split('\t') for l in open(sys.argv[1]) if l.strip() and not l.startswith('KIND')]
    byfile=collections.defaultdict(list)
    for kind,cls,member,*rest in manifest:
        p,chain=find_source(cls)
        if not p: log('NOSRC',cls,member); continue
        byfile[p].append((kind,cls,member,chain,rest))
    for p,items in byfile.items():
        t=open(p).read(); orig=t; n=0
        # order: nested types first? no - remove members first then nested types; also process longer chains first
        items.sort(key=lambda x:(x[0]!='DEAD-CLASS',))
        for kind,cls,member,chain,rest in items:
            if kind=='DEAD-CLASS':
                if len(chain)==1: log('WHOLE-FILE',p); continue
                t,st=remove_nested_type(t,chain)
            elif 'FIELD' in kind or 'CONST' in kind:
                t,st=remove_field(t,chain,member.split(' ')[0])
            else:
                name=member[:member.index('(')]; desc=member[member.index('('):]
                is_ctor=name=='<init>'
                inner=len(chain)>1 and 'static' not in ' '.join(rest)
                t,st=remove_method(t,chain,chain[-1] if is_ctor else name,desc,is_ctor,inner)
            if st=='ok': n+=1
            else: log('SKIP',st,cls,member)
        if t!=orig:
            t,ri=prune_imports(t)
            open(p,'w').write(t)
            log('EDITED',p,'removed',n,'imports',ri)
main()
