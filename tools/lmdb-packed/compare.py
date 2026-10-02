#!/usr/bin/env python3
"""Paired independent benchmark processes, alternating order. Run run.py for both trees first."""
from pathlib import Path
import argparse,json,os,re,statistics,subprocess
CASES=[('write','1'),('write','3'),('write','8'),('write','typed8'),('write','64'),('write','13'),('write','typed13'),('write','63'),('write','mixed'),('write','delta31'),
       ('encode','8'),('encode','13'),('encode','mixed'),('copy','1'),('copy','2'),('copy','3'),('copy','7'),('copy','8'),('copy','typed3'),('copy','typed7'),('copy','13'),('copy','typed13'),('copy','31'),
       ('copy','56'),('copy','63'),('copy','64'),('copy','mixed'),('copy','delta31'),
       ('slice','3'),('slice','13'),('slice','mixed'),('heap-get','3'),('heap-get','13'),('heap-get','63'),('heap-get','mixed'),
       ('native-get','13'),('page','page-core-write'),('page','page-mixed'),('page','measure-typed')]

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base',type=Path,required=True);p.add_argument('--tuned',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--forks',type=int,default=5);p.add_argument('--filter',default='');p.add_argument('--cpu',type=int,help='Linux-only: pin the benchmark JVM to one allowed logical CPU');a=p.parse_args()
 a.out.mkdir(parents=True,exist_ok=True);home=Path(os.environ['JAVA_HOME']);results=[]
 for case in CASES:
  name='-'.join(case)
  if a.filter and name not in a.filter.split(','):continue
  for fork in range(a.forks):
   for version in (('base','tuned') if fork%2==0 else ('tuned','base')):
    cls='PackedIoSuite';args=case
    cmd=[str(home/'bin/java'),'-ea','-Xms512m','-Xmx2g','--enable-native-access=ALL-UNNAMED','-cp',str(getattr(a,version)/'build/classes'),'org.eclipse.rdf4j.sail.lmdb.csf.'+cls,*args]
    if a.cpu is not None:cmd=['taskset','-c',str(a.cpu),*cmd]
    path=a.out/f'{name}-{version}-{fork}.txt'
    with path.open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
    text=path.read_text();m=re.search(r'RESULT (\S+) ns/op=([\d.]+) bytes/op=([\d.]+) operations=(\d+)',text)
    if not m:raise RuntimeError(path)
    row=dict(case=name,version=version,fork=fork,ns=float(m[2]),bytes=float(m[3]),operations=int(m[4]),command=cmd);cpu=re.search(r'RESULT .*cpu-ns/op=([\d.]+)',text)
    row['cpu_ns']=float(cpu[1]) if cpu else None;results.append(row)
    (a.out/'raw-results.json').write_text(json.dumps(results,indent=2)+'\n')
    print(name,version,fork,row['ns'],row['bytes'],flush=True)
 summary=[]
 for case in sorted(set(r['case'] for r in results)):
  row={'case':case}
  for version in ('base','tuned'):
   rows=[r for r in results if r['case']==case and r['version']==version];ns=[r['ns'] for r in rows]
   row[version]={'ns':statistics.median(ns),'range':[min(ns),max(ns)],'bytes':statistics.median(r['bytes'] for r in rows),'cpu_ns':statistics.median(r['cpu_ns'] for r in rows) if all(r['cpu_ns'] is not None for r in rows) else None}
  row['speedup']=row['base']['ns']/row['tuned']['ns'];summary.append(row)
 (a.out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
 for r in summary:print(r['case'],round(r['speedup'],3),flush=True)
if __name__=='__main__':main()
