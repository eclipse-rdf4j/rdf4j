#!/usr/bin/env python3
"""Paired independent JVM processes, alternating execution order. Run run.py for both trees first."""
import argparse,json,os,subprocess,re,statistics
from pathlib import Path
CASES=[('csf','measure-typed'),('csf','measure-random'),('csf','measure-typed-seq'),
       ('csf','write-typed'),('csf','page-core'),('csf','page-legacy'),('csf','page-mixed'),('csf','page-core-write'),
       ('utf8','ascii'),('utf8','heap'),('utf8','unicode'),('utf8','malformed'),('utf8','long'),
       ('sort','random','16384'),('sort','sorted','16384'),('sort','reverse','16384'),('sort','runs','16384'),
       ('sort','duplicates','16384'),('sort','random','64'),('domain','16','false'),('domain','128','false'),('domain','16','true')]
CLASSES={'csf':'csf.CsfPerfSuite','utf8':'evaluation.Utf8PerfSuite','sort':'evaluation.NativeSortPerfSuite','domain':'evaluation.BindingDomainPerfSuite'}
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base',type=Path,required=True);p.add_argument('--tuned',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--forks',type=int,default=5);p.add_argument('--filter',default='');a=p.parse_args()
    home=Path(os.environ['JAVA_HOME']);a.out.mkdir(parents=True,exist_ok=True);results=[]
    for case in CASES:
        name='-'.join(case)
        if a.filter and not any(f in name for f in a.filter.split(',')):continue
        for fork in range(a.forks):
            for version in (['base','tuned'] if fork%2==0 else ['tuned','base']):
                root=getattr(a,version);classes=root/('keys/build/classes' if case[0]=='sort' else 'binding/build/classes' if case[0]=='domain' else 'build/classes')
                cmd=[str(home/'bin/java'),'-ea','-Xms512m','-Xmx2g','--enable-native-access=ALL-UNNAMED','-cp',str(classes),'org.eclipse.rdf4j.sail.lmdb.'+CLASSES[case[0]],*case[1:]]
                path=a.out/f'{name}-{version}-{fork}.log'
                with path.open('w') as log:code=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT).returncode
                if code:raise RuntimeError(f'{path}: {path.read_text()}')
                text=path.read_text();m=re.search(r'RESULT (\S+) ns/op=([\d.]+) bytes/op=([\d.]+) operations=(\d+)',text)
                if not m:raise RuntimeError(f'No result: {path}')
                row=dict(case=name,version=version,fork=fork,ns=float(m[2]),bytes=float(m[3]),ops=int(m[4]),command=cmd)
                results.append(row);print(name,version,fork,row['ns'],row['bytes'],flush=True)
                (a.out/'raw-results.json').write_text(json.dumps(results,indent=2)+'\n')
    summary=[]
    for case in sorted(set(r['case'] for r in results)):
        vs={v:[r for r in results if r['case']==case and r['version']==v] for v in ('base','tuned')}
        row={'case':case}
        for v,rs in vs.items():
            values=[r['ns'] for r in rs];row[v]={'ns':statistics.median(values),'range':[min(values),max(values)],'bytes':statistics.median(r['bytes'] for r in rs)}
        row['speedup']=row['base']['ns']/row['tuned']['ns'];summary.append(row)
    (a.out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    for row in summary:print(row)
if __name__=='__main__':main()
