#!/usr/bin/env python3
"""Compile actual join tables, factor borrowing, and complete KernelRuntime against the inherited
explicit external API doubles. NativeBatch and the build estimate are exact extracted production
units. This does not compile the complete LMDB repository/query planner or generated Janino source.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, sys
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
TOOL=Path(__file__).resolve().parent
PKG='org/eclipse/rdf4j/sail/lmdb/evaluation/'

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source',type=Path,default=ROOT)
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--compile-only',action='store_true')
    a=p.parse_args(); a.out=a.out.resolve();a.source=a.source.resolve()
    if a.out==a.source or a.source in a.out.parents or a.out in a.source.parents:
        p.error('Output must be separate from source')
    home=Path(os.environ['JAVA_HOME']);a.out.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('generated_keys',ROOT/'tools/generated-keys/run.py')
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    build=m.prepare(a.out,a.source);src=build/'src'
    manifest=json.loads((a.out/'build-manifest.json').read_text())
    def write(name,text,origin):
        file=src/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(text)
        manifest[name]={'origin':origin,'sha256':hashlib.sha256(text.encode()).hexdigest()}
    # Replace the inherited NativeBatch API double with the actual extracted production class.
    dep=PKG+'HarnessDependencies.java'
    text=(src/dep).read_text()
    text=text.replace(m.body(text,'final class NativeBatch'),'')
    write(dep,text,'inherited explicit external API doubles; NativeBatch double removed')
    for name in ('PrimitiveHashJoinTable.java','PrimitiveHashJoinFactorSource.java'):
        f=a.source/'java'/PKG/name;write(PKG+name,f.read_text(),'complete production: java/'+PKG+name)
    for name in ('BorrowedTupleBatch.java','TupleSelection.java','SelectedTupleSource.java','FactorSelection.java'):
        rel='org/eclipse/rdf4j/sail/lmdb/factor/'+name
        f=a.source/'java'/rel;write(rel,f.read_text(),'complete production: java/'+rel)
    native=(a.source/'java'/PKG/'LmdbNativeBatch.java').read_text()
    unit=m.body(native,'final class NativeBatch')
    write(PKG+'NativeBatch.java','package org.eclipse.rdf4j.sail.lmdb.evaluation;\nimport java.util.concurrent.atomic.AtomicLong;\n'+unit,
          'complete extracted NativeBatch class from LmdbNativeBatch.java; unrelated cursors omitted')
    estimate=(a.source/'java'/PKG/'LmdbNativeHashJoin.java').read_text()
    unit=m.body(estimate,'static long estimateBuildBytes(')
    write(PKG+'JoinBuildEstimate.java','package org.eclipse.rdf4j.sail.lmdb.evaluation;\nfinal class JoinBuildEstimate {\n'+unit+'\n}\n',
          'exact extracted estimateBuildBytes method from LmdbNativeHashJoin.java')
    # Exercise the exact production refill/probe methods, with a minimal test-only shell.
    probe_methods='\n'.join(m.body(estimate,decl) for decl in (
        'private int nextProbeChainCount()', 'private int nextProbePayload()', 'int nextProbeBucket()'))
    shell="""package org.eclipse.rdf4j.sail.lmdb.evaluation;
import java.io.IOException;
final class JoinProbeDriver {
  static final int END_OF_PROBE=-2;
  final PrimitiveHashJoinTable table;
  final NativeBatch probeBatch;
  final int[] keySlots,probeHashes,probeBuckets,probeHeads;
  final long[] probeHashState;
  int probeIndex,probeCount,currentProbeRow;
  ProbeInput probeCursor;
  interface ProbeInput { int fill(NativeBatch batch) throws IOException; }
  JoinProbeDriver(PrimitiveHashJoinTable table,int[] keys,int slots,int capacity,ProbeInput input) {
    this.table=table;keySlots=keys;probeBatch=new NativeBatch(slots,capacity);probeCursor=input;
    probeHashes=new int[capacity];probeBuckets=new int[capacity];probeHeads=new int[capacity];probeHashState=new long[capacity];
  }
  int head() throws IOException { return nextProbePayload(); }
  int count() throws IOException { return nextProbeChainCount(); }
"""+probe_methods+'\n}\n'
    write(PKG+'JoinProbeDriver.java',shell,'exact three HashJoinBatchCursor refill/probe methods; test-only construction/field/input shell')
    # Compile-time adapter mirrors the two versions' actual cursor call sequence. No reflection
    # or adapter version branch occurs inside a measured loop.
    table=(a.source/'java'/PKG/'PrimitiveHashJoinTable.java').read_text()
    packed='void entryBatch(' in table
    prepare='t.entryBatch(h, n, b, scratch);' if packed else 't.headBatch(h, n, b, heads);'
    candidate='scratch[i]' if packed else 'heads[i]'
    extra=', scratch[i]' if packed else ''
    adapter="""package org.eclipse.rdf4j.sail.lmdb.evaluation;
final class JoinBatchDriver {
  static void prepare(PrimitiveHashJoinTable t, int[] h, int n, int[] b, int[] heads, long[] scratch) {PREPARE}
  static int head(PrimitiveHashJoinTable t, NativeBatch batch, int row, int[] keys, int[] h, int[] b, int[] heads, long[] scratch, int i) {
    return t.lookupPrepared(batch, row, keys, h[i], b[i], CANDIDATE);
  }
  static int count(PrimitiveHashJoinTable t, NativeBatch batch, int row, int[] keys, int[] h, int[] b, int[] heads, long[] scratch, int i) {
    return t.lookupPreparedChainCount(batch, row, keys, h[i], b[i]EXTRA);
  }
  static int bucket(PrimitiveHashJoinTable t, NativeBatch batch, int row, int[] keys, int[] h, int[] b, int[] heads, long[] scratch, int i) {
    return t.lookupPreparedBucket(batch, row, keys, h[i], b[i]EXTRA);
  }
}
""".replace('PREPARE',prepare).replace('CANDIDATE',candidate).replace('EXTRA',extra)
    write(PKG+'JoinBatchDriver.java',adapter,'test adapter: source-selected actual old/new batch preparation/probe signatures; no probe logic')
    for file in (TOOL/'src').rglob('*.java'):
        write(str(file.relative_to(TOOL/'src')),file.read_text(),'join test/benchmark')
    (a.out/'build-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    classes=build/'classes';classes.mkdir();sources=sorted(src.rglob('*.java'))
    args=build/'javac.args';args.write_text('\n'.join(['--release','26','-d',str(classes),*[str(f) for f in sources]]))
    def run(cmd,log):
        with (a.out/log).open('w') as out: subprocess.run([str(x) for x in cmd],stdout=out,stderr=subprocess.STDOUT,check=True)
    run([home/'bin/javac','@'+str(args)],'compile.log')
    run([home/'bin/java','-version'],'jdk.txt')
    print('Compiled',len(sources),'Java files',flush=True)
    changed = [a.source/'java'/PKG/name for name in (
        'PrimitiveHashJoinTable.java','PrimitiveHashJoinFactorSource.java','HashJoinFactorCursor.java',
        'LmdbNativeHashJoin.java','codegen/KernelRuntime.java')]
    run([home/'bin/java','-cp',classes,PKG.replace('/','.')+'SourceSyntaxCheck',*changed],'source-parse.txt')

    if not a.compile_only:
        for name in ('JoinRegression','GeneratedKeyRegression','OptimizedGeneratedKeyRegression'):
            run([home/'bin/java','-ea','-Xms256m','-Xmx2g','-cp',classes,PKG.replace('/','.')+name],name+'.txt')
            print((a.out/(name+'.txt')).read_text(),flush=True)
if __name__=='__main__':main()
