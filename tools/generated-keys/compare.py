#!/usr/bin/env python3
"""Reproducible paired component benchmarks, retained object graphs, and actual C2 captures.
Both sides compile their own production files against the SAME explicit offline doubles and benchmark sources.
This does not build the full RDF4J application, simulate LMDB, or measure string transformations.
"""
from __future__ import annotations
import argparse, hashlib, json, os, re, statistics, subprocess, sys
from pathlib import Path
TOOL=Path(__file__).resolve().parent
ROOT=TOOL.parents[1]
CLASS='org.eclipse.rdf4j.sail.lmdb.evaluation.'

def execute(command, path):
    result=subprocess.run([str(x) for x in command],text=True,capture_output=True)
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError(f'Command failed ({result.returncode}): {command}; see {path}')
    return result.stdout

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--baseline',type=Path,required=True);ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--java-home',type=Path,default=Path(os.environ.get('JAVA_HOME','/invalid')))
    ap.add_argument('--forks',type=int,default=5);ap.add_argument('--bench',action='store_true');ap.add_argument('--memory',action='store_true');ap.add_argument('--c2',action='store_true')
    a=ap.parse_args();a.out=a.out.resolve();a.baseline=a.baseline.resolve()
    if a.forks<1 or not (a.java_home/'bin/javac').is_file():ap.error('Positive forks and JDK 25 or newer required')
    a.out.mkdir(parents=True,exist_ok=True);java=a.java_home/'bin/java';built={}
    for variant,root in [('baseline',a.baseline),('current',ROOT)]:
        target=a.out/variant
        execute([sys.executable,TOOL/'run.py','--out',target,'--source',root,'--java-home',a.java_home],a.out/f'{variant}-tests.txt')
        built[variant]=target/'build/classes'
    jvm=[java,'-ea','-Xms256m','-Xmx2g']
    env=execute([java,'-XshowSettings:vm','-version'],a.out/'environment-jvm.txt')
    (a.out/'parameters.json').write_text(json.dumps({'baseline':str(a.baseline),'current':str(ROOT),'forks':a.forks,'rows':131072,'warmup_rounds':10,'measurement_rounds':9,'jvm':[str(x) for x in jvm],'timing':'separate JVM per case; alternate variant order between forks; no CPU pinning'},indent=2)+'\n')
    if a.bench:
        cases=[('value',64),('value',8192),('value',131072),('decoded',64),('decoded',8192),('decoded',131072),('language',8192),('distinct',64),('distinct',8192),('distinct',131072),('store',8192),('collision',1024)]
        records=[]
        for fork in range(a.forks):
            for operation,cardinality in cases:
                for variant in (['baseline','current'] if fork%2==0 else ['current','baseline']):
                    name=f'{operation}-{cardinality}-{variant}-fork{fork+1}.txt'
                    text=execute(jvm+['-cp',built[variant],CLASS+'GeneratedKeyOptimizationBenchmark',operation,cardinality,131072,10,9],a.out/'benchmarks'/name)
                    line=next(line for line in text.splitlines() if line.startswith('RESULT '));print(name,line,flush=True)
                    record=dict(re.findall(r'(\w+)=([^\s]+)',line));record.update(variant=variant,fork=fork+1)
                    records.append(record)
        (a.out/'benchmark-forks.json').write_text(json.dumps(records,indent=2)+'\n')
        summary=[]
        for operation,cardinality in cases:
            row={'operation':operation,'cardinality':cardinality}
            for variant in ('baseline','current'):
                data=[r for r in records if r['operation']==operation and int(r['cardinality'])==cardinality and r['variant']==variant]
                times=[float(r['ns']) for r in data];allocations=[float(r['bytes']) for r in data]
                row[variant]={'ns_median':statistics.median(times),'ns_forks':times,'bytes_median':statistics.median(allocations),'bytes_forks':allocations,'calls':[int(r['calls']) for r in data],'reads':[int(r['reads']) for r in data]}
            row['speedup']=row['baseline']['ns_median']/row['current']['ns_median'];summary.append(row)
        (a.out/'benchmark-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    if a.memory:
        out=a.out/'footprint';out.mkdir(exist_ok=True)
        manifest=out/'agent.mf';manifest.write_text('Premain-Class: '+CLASS+'GeneratedKeyFootprint\n\n')
        opens=['--add-opens=java.base/'+p+'=ALL-UNNAMED' for p in ('java.lang','java.util','java.util.concurrent','java.util.concurrent.atomic')]
        for variant,classes in built.items():
            agent=out/f'agent-{variant}.jar'
            execute([a.java_home/'bin/jar','cfm',agent,manifest,'-C',classes,CLASS.replace('.','/')+'GeneratedKeyFootprint.class'],out/f'jar-{variant}.txt')
            for cardinality in (0,64,8192,131072):
                for groups in ('keys','groups'):
                    execute(jvm+opens+['-javaagent:'+str(agent),'-cp',classes,CLASS+'GeneratedKeyFootprint',cardinality,groups],out/f'{variant}-{cardinality}-{groups}.txt')
    if a.c2:
        methods=['NativeGeneratedKeyAuthority::intern','NativeGeneratedKeyAuthority::canonicalTermKey','NativeRuntimeValueTable::intern','NativeRuntimeValueTable::internString','NativeRuntimeValueTable::publishedValue','NativeRuntimeValueTable::metadata','codegen.KernelRuntime$LongIntMap::getOrInsert']
        for variant,classes in built.items():
            for operation,cardinality in [('value',8192),('decoded',8192),('language',8192)]:
                file=a.out/'c2'/f'{variant}-{operation}.log'
                execute(jvm+['-XX:+UnlockDiagnosticVMOptions']+['-XX:CompileCommand=print,'+CLASS+m for m in methods]+['-cp',classes,CLASS+'GeneratedKeyOptimizationBenchmark',operation,cardinality,131072,10,3],file)
                execute([sys.executable,ROOT/'tools/value-overlay/disassemble.py',file,a.out/'c2'/f'{variant}-{operation}-asm'],a.out/'c2'/f'{variant}-{operation}-summary.txt')
    print('DONE',flush=True)
if __name__=='__main__':main()
