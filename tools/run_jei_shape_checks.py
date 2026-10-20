"""Run the pure ASM guard against real official JEI JARs and unsafe synthetic flows.

No Minecraft/JEI classes enter the test JVM classpath; JARs are read as data.
Pass the loader's ASM core/tree/analysis JAR paths, separated by os.pathsep.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
FIXTURE=ROOT/'tools/tests/fixtures/jei-26.3-71-86.json'
SOURCE=ROOT/'ports/26.3/src/client/java/dev/guogaology/client/compat/jei/JeiTagCompatShape.java'
HARNESS=ROOT/'tools/tests/JeiTagCompatShapeChecks.java'

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home',default=os.environ.get('JAVA25_HOME'))
    parser.add_argument('--asm-classpath',required=True)
    parser.add_argument('--jei-dir',type=Path,default=ROOT/'build/branch-pickup-061/deps')
    parser.add_argument('--out',type=Path,default=ROOT/'build/jei-shape-068')
    args=parser.parse_args()
    if not args.java_home:parser.error('--java-home or JAVA25_HOME is required (JDK25)')
    java_home=Path(args.java_home);suffix='.exe' if os.name=='nt' else ''
    asm=[Path(value).resolve() for value in args.asm_classpath.split(os.pathsep)]
    assert len(asm)==3 and all(path.is_file() for path in asm),'Provide exactly ASM core/tree/analysis JARs'
    classes_available=set()
    for path in asm:
        with ZipFile(path) as archive:classes_available.update(archive.namelist())
    assert all(name in classes_available for name in ('org/objectweb/asm/ClassReader.class',
        'org/objectweb/asm/tree/ClassNode.class','org/objectweb/asm/tree/analysis/Analyzer.class')),'Incomplete ASM libraries'
    assert not any(name.startswith(('net/minecraft/','mezz/jei/')) for name in classes_available),'Test classpath must exclude Minecraft and JEI'
    expected=json.loads(FIXTURE.read_text('utf8'))['artifacts']
    jars=[];records=[]
    for artifact in expected:
        version=artifact['version']
        path=(args.jei_dir/f'jei-26.3-fabric-{version}.jar').resolve()
        assert digest(path)==artifact['sha256'],'JEI JAR differs from official fixture hash: '+str(path)
        with ZipFile(path) as archive:
            metadata=json.loads(archive.read('fabric.mod.json'))
            assert metadata['id']=='jei' and metadata['version']==version,'Wrong real JEI artifact: '+str(path)
        jars.append(path);records.append({'version':version,'file':str(path),'sha256':digest(path)})
    out=args.out.resolve();assert out.is_relative_to((ROOT/'build').resolve()),'Output must be inside build/'
    out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
    compile_command=[str(java_home/'bin'/('javac'+suffix)),'-proc:none','-cp',os.pathsep.join(map(str,asm)),
                     '-d',str(classes),str(SOURCE),str(HARNESS)]
    compiled=subprocess.run(compile_command,capture_output=True,text=True,encoding='utf8',errors='replace')
    (out/'compile.log').write_text(compiled.stdout+compiled.stderr,encoding='utf8')
    assert compiled.returncode==0,'javac failed: '+compiled.stderr
    command=[str(java_home/'bin'/('java'+suffix)),'-cp',os.pathsep.join(map(str,[classes,*asm])),
             'JeiTagCompatShapeChecks',*map(str,jars)]
    checked=subprocess.run(command,capture_output=True,text=True,encoding='utf8',errors='replace')
    (out/'checks.log').write_text(checked.stdout+checked.stderr,encoding='utf8')
    print(checked.stdout,end='')
    assert checked.returncode==0,'ASM checks failed: '+checked.stderr
    marker=re.search(r'JEI_SHAPE_OK checks=(\d+) realJars=(\d+)',checked.stdout)
    assert marker and int(marker[2])==len(expected),'Missing complete real-JAR checks'
    (out/'results.json').write_text(json.dumps({'success':True,'checks':int(marker[1]),'real_jars':len(jars),
        'synthetic_cases':int(marker[1])-len(jars),'classpath_contains_minecraft_or_jei':False,
        'source_sha256':{str(path.relative_to(ROOT)):digest(path) for path in (SOURCE,HARNESS)},
        'asm':[{'file':str(path),'sha256':digest(path)} for path in asm],'jei':records},indent=2),encoding='utf8')

if __name__=='__main__':main()
