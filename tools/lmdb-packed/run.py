#!/usr/bin/env python3
"""Compile the complete production CSF package with the existing explicit external API doubles.
Compare against a class-renamed full production baseline, not a reimplementation of the codec.
No complete RDF4J/Janino application build is claimed. Requires JDK 26 and output outside inputs.
"""
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess, sys
from pathlib import Path
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[2]
TOOL=Path(__file__).resolve().parent

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--baseline',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--compile-only',action='store_true');a=p.parse_args()
 for n in ('source','baseline','out'):setattr(a,n,getattr(a,n).resolve())
 for root in (a.source,a.baseline):
  if root==a.out or root in a.out.parents or a.out in root.parents: p.error('Output must be separate from source/baseline')
 home=Path(os.environ['JAVA_HOME']);a.out.mkdir(parents=True,exist_ok=True)
 spec=importlib.util.spec_from_file_location('previous',ROOT/'tools/lmdb-performance/run.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
 classes=m.prepare(a.source,a.baseline,a.out,home)
 # The reference encoder must not share the changed little-endian helper with the candidate.
 src=a.out/'build/src/org/eclipse/rdf4j/sail/lmdb/csf'
 old_helper=(a.baseline/'java/org/eclipse/rdf4j/sail/lmdb/csf/LeBytes.java').read_text()
 helper=src/'BaselineLeBytes.java'
 helper.write_text(re.sub(r'\bLeBytes\b','BaselineLeBytes',old_helper))
 reference=[helper]
 manifest_path=a.out/'manifest-csf.json';manifest=json.loads(manifest_path.read_text())
 for name in ('BaselinePackedLongVector.java','BaselineCompactCsfPageEncoder.java'):
  f=src/name;f.write_text(re.sub(r'\bLeBytes\b','BaselineLeBytes',f.read_text()));reference.append(f)
 for f in reference:
  manifest[str(f.relative_to(a.out/'build/src'))]={'origin':'complete baseline production, class/helper names only renamed','sha256':hashlib.sha256(f.read_bytes()).hexdigest()}
 manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
 m.run([home/'bin/javac','--release','26','-cp',classes,'-d',classes,*reference],a.out/'compile-reference.log')
 files=sorted((TOOL/'src').rglob('*.java'))
 m.run([home/'bin/javac','--release','26','-cp',classes,'-d',classes,*files],a.out/'compile-packed.log')
 paths=[*files,*sorted((a.source/'java/org/eclipse/rdf4j/sail/lmdb/csf').glob('*.java'))]
 (a.out/'packed-inputs.json').write_text(json.dumps({str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in paths},indent=2)+'\n')
 m.run([home/'bin/java','-version'],a.out/'jdk.txt')
 if not a.compile_only:
  m.run([home/'bin/java','-ea','-Xms256m','-Xmx2g','--enable-native-access=ALL-UNNAMED','-cp',classes,'org.eclipse.rdf4j.sail.lmdb.csf.PackedIoSuite','test'],a.out/'PackedIoSuite.txt')
if __name__=='__main__':main()
