#!/usr/bin/env python3
"""Collect normal-tiered C2 machine bytes separately from benchmarks, then verify/disassemble.
No compilation threshold, force-inline, or compile-only changes. Requires GNU objdump.
"""
from pathlib import Path
import argparse,json,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parents[2]
PKG='org.eclipse.rdf4j.sail.lmdb.csf.'
CASES={
 'control':(['native-get','13'],['PackedLongVector::nativeGet','PackedLongVector::decodeNativeValue','PackedLongVector::readBits']),
 'write':(['write','mixed'],['PackedLongVector::writeNative','PackedLongVector::writeBlockNative','PackedLongVector::writeBits','PackedLongVector::packBlock']),
 'heap':(['heap-get','13'],['LeBytes::getLong','PackedLongVector::readBits','PackedLongVector$ArrayReader::get']),
 'narrow':(['copy','3'],['PackedLongVector::nativeCopy','PackedLongVector::decodeNativeRange','PackedLongVector::unpackForRaw','PackedLongVector::unpackNarrow','PackedLongVector::unpack3']),
 'mixed':(['copy','mixed'],['PackedLongVector::decodeNativeRange','PackedLongVector::unpackForRaw','PackedLongVector::unpackNarrow','PackedLongVector::unpack1','PackedLongVector::unpack3','PackedLongVector::unpack7']),
}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--build',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--only',default='');a=p.parse_args();home=Path(os.environ['JAVA_HOME']);a.out.mkdir(parents=True,exist_ok=True);commands={}
 for name,(args,methods) in CASES.items():
  if a.only and name not in a.only.split(','):continue
  command=[str(home/'bin/java'),'-ea','-Xms512m','-Xmx2g','--enable-native-access=ALL-UNNAMED','-XX:+UnlockDiagnosticVMOptions']
  command+=['-XX:CompileCommand=print,'+PKG+m for m in methods]
  command+=['-cp',str(a.build/'build/classes'),PKG+'PackedIoSuite',*args];commands[name]=command
  (a.out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
  for attempt in range(3):
   log=a.out/(name+'.log');asm=a.out/(name+'-asm');decode=a.out/(name+'-decode.txt')
   with log.open('w') as f:subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,check=True)
   if asm.exists():shutil.rmtree(asm)
   with decode.open('w') as f:code=subprocess.run([sys.executable,str(ROOT/'tools/value-overlay/disassemble.py'),str(log),str(asm)],stdout=f,stderr=subprocess.STDOUT).returncode
   summary=asm/'summary.txt'
   if code==0 and summary.exists() and summary.read_text().strip():
    print(summary.read_text(),flush=True);break
   log.rename(a.out/f'{name}-rejected-{attempt}.log');decode.rename(a.out/f'{name}-rejected-{attempt}-decode.txt')
  else:raise RuntimeError(f'Missing C2 bytes: {name}; rejected captures retained')
if __name__=='__main__':main()
