"""Prepare an isolated QA-only native tape-density preLaunch source set. Does not build or launch."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'build/material-065-census'
SRC=BASE/'src'
for filename in ('FrontierTape065Census.java','SingleBiomeMixin.java','TapeDensityMixin.java'):
 package='dev/guogaology/world' if filename.startswith('Frontier') else 'dev/guogaology/material065/mixin'
 path=SRC/package/filename;path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text((ROOT/'tools/tests/material065'/filename).read_text('utf-8-sig'),'utf8')
res=BASE/'resources';res.mkdir(parents=True,exist_ok=True)
(res/'fabric.mod.json').write_text(json.dumps({'schemaVersion':1,'id':'guogaology_material065_census','version':'1','entrypoints':{'preLaunch':['dev.guogaology.world.FrontierTape065Census']},'mixins':['material065.mixins.json']}),'utf8')
(res/'material065.mixins.json').write_text(json.dumps({'required':True,'package':'dev.guogaology.material065.mixin','compatibilityLevel':'JAVA_21','mixins':['SingleBiomeMixin','TapePassMixin','TapeRetentionMixin'],'injectors':{'defaultRequire':1}}),'utf8')
init=BASE/'census.init.gradle'
init.write_text('''allprojects { p ->
 p.afterEvaluate {
  if (p == p.rootProject) {
   p.sourceSets { material065Census {
    java.srcDir 'build/material-065-census/src'
    resources.srcDir 'build/material-065-census/resources'
    compileClasspath += p.sourceSets.main.output + p.sourceSets.main.compileClasspath
    runtimeClasspath += output + compileClasspath + p.sourceSets.main.runtimeClasspath
   } }
   p.loom {
    mods { guogaology_material065_census { sourceSet p.sourceSets.material065Census } }
    runs { material065Census {
     server()
     runDir 'build/material-065-census/run'
     vmArg '-Xmx3G'
     vmArg "-Dguogaology.tapeProfiles=${p.findProperty('tapeProfiles') ?: '1,4,6,8'}"
     vmArg "-Dguogaology.tapeCell=${p.findProperty('tapeCell') ?: '40'}"
     vmArg "-Dguogaology.tapeOutput=${p.file(p.findProperty('tapeOutput') ?: 'build/material-065/tape-density.json').absolutePath}"
    } }
   }
   p.tasks.named('runMaterial065Census') { dependsOn p.tasks.named('material065CensusClasses'); classpath += p.sourceSets.material065Census.output }
  }
 }
}
''','utf8')
print(init)
