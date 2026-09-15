#!/usr/bin/env python3
"""Compile the supplied production key/primitive/spill code against explicit offline test doubles.
This is not a full RDF4J build. No LMDB/overlay I/O is simulated in timing results.
All unchanged classes and extracted production bodies are hashed in the build manifest.
"""
from __future__ import annotations
import argparse, hashlib, json, os, shutil, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
TOOL=Path(__file__).resolve().parent
PKG='org/eclipse/rdf4j/sail/lmdb/evaluation/'

def balanced(text: str,start: int) -> int:
    depth=0; i=start; state='code'
    while i<len(text):
        c=text[i]; d=text[i:i+2]
        if state=='line':
            if c=='\n':state='code'
        elif state=='block':
            if d=='*/':state='code'; i+=1
        elif state in ('"',"'"):
            if c=='\\':i+=1
            elif c==state:state='code'
        elif d=='//':state='line';i+=1
        elif d=='/*':state='block';i+=1
        elif c in ('"',"'"):state=c
        elif c=='{':depth+=1
        elif c=='}':
            depth-=1
            if depth==0:return i+1
        i+=1
    raise ValueError('Unbalanced source')

def body(text: str,decl: str) -> str:
    start=text.index(decl); return text[start:balanced(text,text.index('{',start))]

def prepare(out: Path) -> Path:
    build=out/'build'
    marker=build/'.generated-keys-build'
    if build.exists():
        if not marker.is_file():
            raise ValueError(f'Refusing to replace an unmarked build directory: {build}')
        shutil.rmtree(build)
    src=build/'src';src.mkdir(parents=True)
    marker.write_text('Disposable generated-key test build.\n')
    manifest={}
    def write(name: str,text: str,origin: str):
        dest=src/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(text)
        manifest[name]={'origin':origin,'sha256':hashlib.sha256(text.encode()).hexdigest()}
    for file in (TOOL/'stubs').rglob('*.java'):
        write(str(file.relative_to(TOOL/'stubs')),file.read_text(),'explicit test double')
    files=['NativeGeneratedKeyPlan.java','NativeGeneratedKeyAuthority.java','NativeGeneratedKeyHooks.java',
      'NativeExecutionContext.java','PlanValueCatalog.java','NativeValueResolver.java','NativeCanonicalTermKeys.java',
      'LmdbNativeTermAuthority.java','NativeTermAuthority.java','NativeTermRef.java','NativeIdKind.java','TermProbeDisposition.java',
      'NativeCountGroupStore.java','LmdbNativeSort.java','NativeSliceMath.java',
      'KernelGroupSink.java','KernelOrderSink.java',
      'codegen/KernelHooks.java','codegen/KernelRuntime.java','codegen/KernelCancellation.java',
      'codegen/KernelCancelledException.java','codegen/KernelQueryCancelledException.java','codegen/KernelQuadCursor.java']
    for name in files:
        file=ROOT/'java'/PKG/name;write(PKG+name,file.read_text(),'complete production file: java/'+PKG+name)
    file=ROOT/'java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryMemoryManager.java'
    write('org/eclipse/rdf4j/sail/lmdb/LmdbQueryMemoryManager.java',file.read_text(),'complete production file: '+str(file.relative_to(ROOT)))
    text=(ROOT/'java'/PKG/'LmdbNativeRowState.java').read_text()
    header='''package org.eclipse.rdf4j.sail.lmdb.evaluation;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.*;
import org.eclipse.rdf4j.model.*; import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator; import java.util.concurrent.atomic.AtomicLong;
'''
    write(PKG+'CopyBinding.java',header+body(text,'final class CopyBinding'),'extracted complete production class from LmdbNativeRowState.java')
    text=(ROOT/'java'/PKG/'LmdbNativePrimitiveTupleTable.java').read_text()
    imports=text[:text.index('@Experimental\nfinal class PrimitiveTupleTable')]
    parts=[body(text,'final class '+n) for n in ('PrimitiveTupleTable','NativeDistinctTracker','NativeTermReferenceCache')]
    write(PKG+'PrimitiveTupleTable.java',imports+'\n\n'.join(parts),'three complete extracted production classes: LmdbNativePrimitiveTupleTable.java')
    text=(ROOT/'java'/PKG/'LmdbSyntheticValueSource.java').read_text()
    start=text.index('class SyntheticValueSource');end=text.index('\n\t@Override\n\tpublic long idOf(Value value)')
    part=text[start:end]
    for decl in ('public long idOf(Value value)','public Object valueLookupScope()', 'public Value lazyValue(long id)'):
        part+='\n'+body(text,decl)
    part+='\n}\n'
    write(PKG+'SyntheticValueSource.java',header+part,'exact production fields/constructors/evaluation/ingress/key/probe-guard/value-resolution methods; unrelated I/O adapters omitted')
    for file in (TOOL/'src').rglob('*.java'):
        write(str(file.relative_to(TOOL/'src')),file.read_text(),'test or benchmark')
    (out/'build-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return build

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--java-home',type=Path,default=None,help='JDK 26; defaults to JAVA_HOME or javac on PATH')
    ap.add_argument('--compile-only',action='store_true')
    ap.add_argument('--bench',action='store_true')
    ap.add_argument('--forks',type=int,default=3)
    a=ap.parse_args()
    if a.forks < 1: ap.error('--forks must be positive')
    if a.java_home is None:
        home=os.environ.get('JAVA_HOME')
        compiler=shutil.which('javac')
        if not home and not compiler: ap.error('Set JAVA_HOME or --java-home to JDK 26')
        a.java_home=Path(home) if home else Path(compiler).resolve().parents[1]
    if not (a.java_home/'bin/javac').is_file(): ap.error('No javac under --java-home')
    a.out=a.out.resolve()
    if a.out == ROOT or a.out in ROOT.parents or a.out == TOOL: ap.error('Use a separate output directory')
    a.out.mkdir(parents=True,exist_ok=True);build=prepare(a.out)
    classes=build/'classes';classes.mkdir()
    sources=sorted((build/'src').rglob('*.java'))
    args=build/'javac.args';args.write_text('\n'.join(['-d',str(classes),'--release','26']+[str(f) for f in sources]))
    result=subprocess.run([str(a.java_home/'bin/javac'),'@'+str(args)],text=True,capture_output=True)
    (a.out/'compile.log').write_text(result.stdout+result.stderr)
    if result.returncode:print(result.stdout+result.stderr);raise SystemExit(result.returncode)
    print('Compiled',len(sources),'Java files; manifest distinguishes production code from test doubles.')
    if a.compile_only:return
    java=[str(a.java_home/'bin/java'),'-ea','-Xms256m','-Xmx2g','-cp',str(classes)]
    result=subprocess.run(java+['org.eclipse.rdf4j.sail.lmdb.evaluation.GeneratedKeyRegression'],text=True,capture_output=True)
    (a.out/'regression.txt').write_text(result.stdout+result.stderr);print(result.stdout+result.stderr)
    if result.returncode:raise SystemExit(result.returncode)
    if a.bench:
        for fork in range(a.forks):
            result=subprocess.run(java+['org.eclipse.rdf4j.sail.lmdb.evaluation.GeneratedKeyBenchmark'],text=True,capture_output=True)
            (a.out/f'benchmark-{fork+1}.txt').write_text(result.stdout+result.stderr)
            print(result.stdout+result.stderr)
            if result.returncode:raise SystemExit(result.returncode)
if __name__=='__main__':main()
