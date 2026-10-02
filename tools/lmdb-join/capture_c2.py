#!/usr/bin/env python3
"""Capture normal tiered C2 emitted bytes, and decode only when every byte is present."""
from pathlib import Path
import argparse,json,os,subprocess

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--build',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--filter',default='');a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
 java=Path(os.environ['JAVA_HOME'])/'bin/java';tool=Path(__file__).resolve().parent
 cases={
  'hash-single':(('hash',1,1,32768,32768,50,'false'),['PrimitiveHashJoinTable::hashBatch']),
  'hash-composite':(('hash',3,1,32768,32768,50,'false'),['PrimitiveHashJoinTable::hashBatch']),
  'probe-mixed':(('native-probe',1,1,131072,131072,50,'false'),['PrimitiveHashJoinTable::lookup','PrimitiveHashJoinTable::lookupPrepared','PrimitiveHashJoinTable::lookupSingle','PrimitiveHashJoinTable::lookupScalarSingle','PrimitiveHashJoinTable::matches']),
  'ir-probe-mixed':(('ir-probe',1,1,131072,131072,50,'false'),['codegen.KernelRuntime$LongRowMap::lookup','codegen.KernelRuntime$LongRowMap::lookupSingle','codegen.KernelRuntime$LongRowMap::lookupScalarSingle']),
  'batch-mixed':(('native-batch',1,1,131072,131072,50,'false'),['PrimitiveHashJoinTable::entryBatch','PrimitiveHashJoinTable::headBatch','PrimitiveHashJoinTable::lookupPrepared','PrimitiveHashJoinTable::lookupSingle','JoinBenchmark$Fixture::run']),
  'build-unique':(('native-build',1,1,131072,131072,0,'false'),['PrimitiveHashJoinTable::add','PrimitiveHashJoinTable::find','PrimitiveHashJoinTable::growBuckets']),
 }
 commands={}
 for name,(case,methods) in cases.items():
  if a.filter and name not in a.filter.split(','):continue
  cmd=[str(java),'-Xms512m','-Xmx2g','-XX:+UnlockDiagnosticVMOptions',*['-XX:CompileCommand=print,org.eclipse.rdf4j.sail.lmdb.evaluation.'+m for m in methods],'-cp',str(a.build/'build/classes'),'org.eclipse.rdf4j.sail.lmdb.evaluation.JoinBenchmark',*[str(x) for x in case]]
  commands[name]=cmd
  with (a.out/(name+'.log')).open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
  decode=['python3',str(tool.parent/'value-overlay/disassemble.py'),str(a.out/(name+'.log')),str(a.out/(name+'-asm'))]
  with (a.out/(name+'-decode.txt')).open('w') as f:subprocess.run(decode,stdout=f,stderr=subprocess.STDOUT,check=True)
 (a.out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
if __name__=='__main__':main()
