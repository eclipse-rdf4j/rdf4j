#!/usr/bin/env python3
"""Offline, source-hashed tests of actual CSF code, sort code, and extracted UTF-8 helpers.
External RDF CoreDatatype enumeration and logging/annotations are explicit test doubles.
No test double supplies compression, native I/O, sorting, or UTF-8 behavior.
Requires JDK 26. Does not compile the full RDF4J application.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess, sys
sys.dont_write_bytecode = True
ROOT=Path(__file__).resolve().parents[2]
TOOL=Path(__file__).resolve().parent
PKG=Path('org/eclipse/rdf4j/sail/lmdb')

def load_generated_runner():
    path=ROOT/'tools/generated-keys/run.py'
    spec=importlib.util.spec_from_file_location('generated_runner',path)
    mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
    return mod

def run(cmd, log):
    log.parent.mkdir(parents=True,exist_ok=True)
    with log.open('w') as out:
        p=subprocess.run([str(x) for x in cmd],stdout=out,stderr=subprocess.STDOUT)
    if p.returncode:
        print(log.read_text(errors='replace')); raise RuntimeError(f'{p.returncode}: {cmd}; {log}')
    print('OK',log,flush=True)

def prepare(root, baseline, out, java_home):
    build=out/'build';marker=build/'.lmdb-performance-build'
    if build.exists():
        if not marker.exists():raise ValueError(f'Refusing unmarked build dir {build}')
        shutil.rmtree(build)
    src=build/'src';src.mkdir(parents=True);marker.write_text('Disposable test build\n')
    manifest={}
    def write(rel,text,origin):
        path=src/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
        manifest[str(rel)]={'origin':origin,'sha256':hashlib.sha256(text.encode()).hexdigest()}
    for file in (TOOL/'stubs').rglob('*.java'):
        write(file.relative_to(TOOL/'stubs'),file.read_text(),'explicit external API double')
    for file in (root/'java'/PKG/'csf').glob('*.java'):
        write(file.relative_to(root/'java'),file.read_text(),'complete production file')
    for name in ('ValueIds.java','LmdbRuntimeProperties.java'):
        write(PKG/name,(root/'java'/PKG/name).read_text(),'complete production file')
    # Reference implementations never run in timed benchmark processes.
    for name in ('PackedLongVector','CompactCsfPageEncoder'):
        txt=(baseline/'java'/PKG/'csf'/f'{name}.java').read_text()
        txt=re.sub(r'\bPackedLongVector\b','BaselinePackedLongVector',txt)
        txt=re.sub(r'\bCompactCsfPageEncoder\b','BaselineCompactCsfPageEncoder',txt)
        write(PKG/'csf'/f'Baseline{name}.java',txt,'baseline production, class names only renamed')
    generated=load_generated_runner()
    for name, tree in [('Utf8UnderTest',root),('BaselineUtf8',baseline)]:
        text=(tree/'java'/PKG/'evaluation/LmdbNativeValueCodec.java').read_text()
        members='\n'.join(generated.body(text,d) for d in ('private static byte[] utf8Scratch(', 'private static String decodeUtf8('))
        prefix='''package org.eclipse.rdf4j.sail.lmdb.evaluation;
import java.nio.ByteBuffer; import java.nio.charset.StandardCharsets;
final class NAME {
private static final int MAX_UTF8_SCRATCH_BYTES = 64 << 10;
private static final ThreadLocal<byte[]> UTF8_SCRATCH = new ThreadLocal<>();
static String decode(ByteBuffer b,int from,int length) { return decodeUtf8(b,from,length); }
static int scratchSize() { byte[] b=UTF8_SCRATCH.get(); return b==null?0:b.length; }
'''.replace('NAME',name)
        write(PKG/'evaluation'/f'{name}.java',prefix+members+'\n}\n','two complete extracted production methods; identical constants/ThreadLocal; test entrypoint')
    for file in (TOOL/'src').rglob('*.java'):
        if file.name.startswith('NativeSort'):continue
        if file.name.startswith('BindingDomain'):continue
        write(file.relative_to(TOOL/'src'),file.read_text(),'test/benchmark')
    classes=build/'classes';classes.mkdir()
    files=sorted(src.rglob('*.java'));args=build/'javac.args'
    args.write_text('\n'.join(['--release','26','-d',str(classes)]+[str(x) for x in files]))
    run([java_home/'bin/javac','@'+str(args)],out/'compile-csf.log')
    (out/'manifest-csf.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return classes

def prepare_keys(root,out,java_home):
    mod=load_generated_runner();build=mod.prepare(out,root);src=build/'src'
    for file in (TOOL/'src').rglob('NativeSort*.java'):
        dest=src/file.relative_to(TOOL/'src');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,dest)
    classes=build/'classes';classes.mkdir();files=sorted(src.rglob('*.java'))
    args=build/'javac.args';args.write_text('\n'.join(['--release','26','-d',str(classes)]+[str(x) for x in files]))
    run([java_home/'bin/javac','@'+str(args)],out/'compile.log')
    (out/'extra-source-manifest.json').write_text(json.dumps({str(f.relative_to(src)):{'origin':'test/benchmark','sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in src.rglob('NativeSort*.java')},indent=2)+'\n')
    return classes

def prepare_binding(root,baseline,out,java_home):
    build=out/'build';marker=build/'.lmdb-performance-build'
    if build.exists():
        if not marker.exists():raise ValueError(f'Refusing unmarked build dir {build}')
        shutil.rmtree(build)
    src=build/'src';src.mkdir(parents=True);marker.write_text('Disposable test build\n')
    manifest={}
    def write(rel,text,origin):
        path=src/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
        manifest[str(rel)]={'origin':origin,'sha256':hashlib.sha256(text.encode()).hexdigest()}
    for file in (TOOL/'binding-stubs').rglob('*.java'):
        write(file.relative_to(TOOL/'binding-stubs'),file.read_text(),'explicit external API double')
    for file in (TOOL/'stubs/org/eclipse/rdf4j/common').rglob('*.java'):
        write(file.relative_to(TOOL/'stubs'),file.read_text(),'external annotation double')
    for name in ['RowBindingSetView','NativeSlotLayout','QueryWideVarLayout','SlotBindingSetView']:
        rel=PKG/'evaluation'/f'{name}.java'
        write(rel,(root/'java'/rel).read_text(),'complete production file')
    old=(baseline/'java'/PKG/'evaluation/RowBindingSetView.java').read_text()
    write(PKG/'evaluation/BaselineRowBindingSetView.java',re.sub(r'\bRowBindingSetView\b','BaselineRowBindingSetView',old),'complete baseline production, class name only changed')
    for file in (TOOL/'src').rglob('BindingDomain*.java'):
        write(file.relative_to(TOOL/'src'),file.read_text(),'test/benchmark')
    classes=build/'classes';classes.mkdir();files=sorted(src.rglob('*.java'))
    args=build/'javac.args';args.write_text('\n'.join(['--release','26','-d',str(classes)]+[str(x) for x in files]))
    run([java_home/'bin/javac','@'+str(args)],out/'compile.log')
    (out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return classes

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--baseline',type=Path,required=True)
    ap.add_argument('--source',type=Path,default=ROOT)
    ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--compile-only',action='store_true')
    ap.add_argument('--skip-keys',action='store_true')
    a=ap.parse_args();a.out=a.out.resolve();a.source=a.source.resolve();a.baseline=a.baseline.resolve()
    for r in [a.source,a.baseline]:
        if a.out==r or r in a.out.parents or a.out in r.parents:ap.error('Use output outside input source trees')
    home=Path(os.environ.get('JAVA_HOME','/usr/lib/jvm/default-java'))
    a.out.mkdir(parents=True,exist_ok=True)
    production = [
        'csf/PackedLongVector.java', 'csf/CompactCsfPageEncoder.java',
        'evaluation/LmdbNativeSort.java', 'evaluation/LmdbNativeValueCodec.java',
        'evaluation/RowBindingSetView.java',
    ]
    hashes = {rel: hashlib.sha256((a.source/'java'/PKG/rel).read_bytes()).hexdigest()
              for rel in production}
    (a.out/'production-input-sha256.json').write_text(json.dumps(hashes, indent=2)+'\n')
    run([home/'bin/java','-version'],a.out/'java-version.txt')
    classes=prepare(a.source,a.baseline,a.out,home)
    cmd=[home/'bin/java','-ea','-Xms256m','-Xmx2g','--enable-native-access=ALL-UNNAMED','-cp',classes]
    if not a.compile_only:
        for cls in ['csf.CsfPerfSuite','evaluation.Utf8PerfSuite']:
            run(cmd+['org.eclipse.rdf4j.sail.lmdb.'+cls,'test'],a.out/(cls.rsplit('.',1)[-1]+'.txt'))
    out=a.out/'binding';out.mkdir(exist_ok=True);binding=prepare_binding(a.source,a.baseline,out,home)
    if not a.compile_only:
        run(cmd[:-1]+[binding,'org.eclipse.rdf4j.sail.lmdb.evaluation.BindingDomainPerfSuite','test'],out/'BindingDomainPerfSuite.txt')
    if not a.skip_keys:
        out=a.out/'keys';out.mkdir(exist_ok=True);keys=prepare_keys(a.source,out,home)
        if not a.compile_only:
            for cls in ('GeneratedKeyRegression','OptimizedGeneratedKeyRegression','NativeSortPerfSuite'):
                run(cmd[:-1]+[keys,'org.eclipse.rdf4j.sail.lmdb.evaluation.'+cls],out/(cls+'.txt'))
    print('Build/tests complete. Source manifests distinguish production, reference and API doubles.')
if __name__=='__main__':main()
