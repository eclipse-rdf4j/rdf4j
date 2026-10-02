#!/usr/bin/env python3
"""Independent paired JVMs, alternating order. Logs every fork and sample, including regressions."""
from pathlib import Path
import argparse,json,os,re,statistics,subprocess
# op, key columns, payload columns, build rows, unique groups, miss percentage, scattered selection
CASES={
 'build-unique':('native-build',1,1,131072,131072,0,False),
 'ir-build-unique':('ir-build',1,1,131072,131072,0,False),
 'build-composite':('native-build',3,3,65536,65536,0,False),
 'ir-build-composite':('ir-build',3,3,65536,65536,0,False),
 'build-duplicates':('native-build',1,1,131072,8192,0,False),
 'ir-build-duplicates':('ir-build',1,1,131072,8192,0,False),
 'build-count-only':('native-build',1,0,131072,8192,0,False),
 'ir-build-count-only':('ir-build',1,0,131072,8192,0,False),
 'probe-hit':('native-probe',1,1,131072,131072,0,False),
 'probe-miss':('native-probe',1,1,131072,131072,100,False),
 'probe-mixed':('native-probe',1,1,131072,131072,50,False),
 'ir-probe-hit':('ir-probe',1,1,131072,131072,0,False),
 'ir-probe-miss':('ir-probe',1,1,131072,131072,100,False),
 'ir-probe-mixed':('ir-probe',1,1,131072,131072,50,False),
 'batch-mixed':('native-batch',1,1,131072,131072,50,False),
 'batch-composite':('native-batch',3,3,65536,65536,50,False),
 'batch-scattered':('native-batch',3,3,65536,65536,50,True),
 'batch-count':('native-count',1,0,131072,8192,50,False),
 'hash-single':('hash',1,1,32768,32768,50,False),
 'hash-composite':('hash',3,1,32768,32768,50,False),
 'hash-scattered':('hash',3,1,32768,32768,50,True),
 'chain-native':('native-chain',1,1,131072,8192,50,False),
 'chain-ir':('ir-chain',1,1,131072,8192,50,False),
 'probe-high-load':('native-probe',1,1,196608,196608,50,False),
 'ir-probe-high-load':('ir-probe',1,1,196608,196608,50,False),
 'batch-high-load':('native-batch',1,1,196608,196608,50,False),
 'probe-small':('native-probe',1,1,128,128,50,False),
 'batch-small':('native-batch',1,1,128,128,50,False),
 'probe-composite':('native-probe',3,3,65536,65536,50,False),
 'ir-probe-composite':('ir-probe',3,3,65536,65536,50,False),
 'shape-cycle':('shape-cycle',1,1,65536,65536,50,False),
 'threshold-duplicates':('native-build',1,0,131072,24,0,False),
}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base',type=Path,required=True);p.add_argument('--tuned',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--forks',type=int,default=5);p.add_argument('--filter',default='');p.add_argument('--cpu',type=int);a=p.parse_args()
 if a.forks<1:p.error('forks must be positive')
 a.out.mkdir(parents=True,exist_ok=True);results=[];home=Path(os.environ['JAVA_HOME'])
 for name,case in CASES.items():
  if a.filter and name not in a.filter.split(','):continue
  for fork in range(a.forks):
   for version in (('base','tuned') if fork%2==0 else ('tuned','base')):
    command=[str(home/'bin/java'),'-ea','-Xms512m','-Xmx2g','-cp',str(getattr(a,version)/'build/classes'),'org.eclipse.rdf4j.sail.lmdb.evaluation.JoinBenchmark',*[str(x).lower() if isinstance(x,bool) else str(x) for x in case]]
    if a.cpu is not None:command=['taskset','-c',str(a.cpu),*command]
    path=a.out/f'{name}-{version}-{fork}.txt'
    with path.open('w') as f:subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,check=True)
    text=path.read_text();m=re.search(r'RESULT (\S+) ns/row=([\d.]+) bytes/row=([\d.]+) cpu-ns/row=([\d.]+).*native-bytes=(\d+) ir-bytes=(\d+)',text)
    if not m:raise RuntimeError(path)
    row=dict(case=name,version=version,fork=fork,ns=float(m[2]),bytes=float(m[3]),cpu_ns=float(m[4]),native_bytes=int(m[5]),ir_bytes=int(m[6]),command=command);results.append(row)
    (a.out/'raw-results.json').write_text(json.dumps(results,indent=2)+'\n');print(name,version,fork,row['ns'],row['bytes'],flush=True)
 summary=[]
 for case in CASES:
  if not any(r['case']==case for r in results):continue
  row={'case':case}
  for version in ('base','tuned'):
   rs=[r for r in results if r['case']==case and r['version']==version];ns=[r['ns'] for r in rs]
   row[version]={'ns':statistics.median(ns),'range':[min(ns),max(ns)],'bytes':statistics.median(r['bytes'] for r in rs),'cpu_ns':statistics.median(r['cpu_ns'] for r in rs),'native_bytes':rs[0]['native_bytes'],'ir_bytes':rs[0]['ir_bytes']}
  row['speedup']=row['base']['ns']/row['tuned']['ns'];summary.append(row)
 (a.out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
 for r in summary:print(r['case'],round(r['speedup'],3),flush=True)
if __name__=='__main__':main()
