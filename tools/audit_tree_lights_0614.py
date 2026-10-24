"""Compare whole-row lamp planning against the exact0.6.13 source; no game bootstrap."""
import argparse
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]


def run(*args):
    return subprocess.run([str(a) for a in args],cwd=ROOT,check=True,capture_output=True,text=True).stdout


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home',type=Path,required=True)
    parser.add_argument('--baseline',default='ec0d4ac')
    parser.add_argument('--out',type=Path,default=ROOT/'build/lamps0614/audit')
    args=parser.parse_args();out=args.out.resolve();out.mkdir(parents=True,exist_ok=True)
    java=args.java_home/'bin/java.exe';javac=args.java_home/'bin/javac.exe'
    commit=run('git','rev-parse',args.baseline).strip()
    old=out/'baseline-source';old.mkdir(parents=True,exist_ok=True)
    paths=run('git','ls-tree','-r','--name-only',commit,'src/main/java/dev/guogaology/world').splitlines()
    for file in paths:
        if not file.endswith('.java'):continue
        path=old/Path(file).relative_to('src/main/java');path.parent.mkdir(parents=True,exist_ok=True)
        # Source bytes, not shell-escaped contents or a conversion through git checkout.
        blob=subprocess.run(['git','show',f'{commit}:{file}'],cwd=ROOT,check=True,capture_output=True).stdout
        path.write_bytes(blob)
    report={'baselineCommit':commit,'scope':'Finite candidate fixtures and warm planning CPU; native paint, terrain and final-map tests remain separate.','lines':{}}
    for label,source in [('baseline',old),('current',ROOT/'src/main/java')]:
        classes=out/label;classes.mkdir(parents=True,exist_ok=True)
        tests=[ROOT/'tools/tests/TreeLights0614Census.java']
        if label=='current':tests.append(ROOT/'tools/tests/TreeLights0614Checks.java')
        run(javac,'-sourcepath',source,'-d',classes,*tests)
        row={'census':json.loads(run(java,'-Xmx768m','-cp',classes,'TreeLights0614Census')),
             'benchmark':json.loads(run(java,'-Xmx768m','-cp',classes,'TreeLights0614Census','--benchmark'))}
        if label=='current':row['invariants']=run(java,'-Xmx768m','-cp',classes,'TreeLights0614Checks').strip()
        report['lines'][label]=row
    for family in report['lines']['current']['census']['families']:
        for key in ('brokenConsecutiveEdges','duplicates','nearDifferentBandPairs','nearNonlocalSelfPairs'):
            assert family[key]==0,(family['family'],key,family[key])
    path=out/'comparison.json';path.write_text(json.dumps(report,indent=2)+'\n','utf8');print(path)


if __name__=='__main__':
    main()
