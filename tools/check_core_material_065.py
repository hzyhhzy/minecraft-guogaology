"""Compare actual0.6.4/current pure painters: resource counts, complete bounds, support and clipping."""
import argparse,csv,hashlib,json,os,statistics,subprocess
from pathlib import Path
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[1]

def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--java-home',type=Path,default=Path(os.environ.get('JAVA_HOME','C:/Users/sigmoid/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon')))
 p.add_argument('--baseline-jar',type=Path,default=ROOT/'ports/26.2/build/libs/guogaology-dimension-26.2-0.6.4.jar')
 a=p.parse_args();baseline=a.baseline_jar.resolve();assert baseline.is_file(),baseline
 with ZipFile(baseline)as archive:
  identity=json.loads(archive.read('fabric.mod.json'));assert identity['id']=='guogaology'and identity['version']=='0.6.4','Use the actual0.6.4 production baseline'
 out=ROOT/'build/core-material-065';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 logs=[]
 commands=[[str(a.java_home/'bin/javac.exe'),'--release','21','-encoding','UTF-8','-sourcepath',str(ROOT/'src/main/java'),'-d',str(classes),str(ROOT/'tools/tests/CoreMaterial065GenerationChecks.java')],
 [str(a.java_home/'bin/java.exe'),'-Xmx768m','-cp',os.pathsep.join([str(baseline),str(classes)]),'CoreMaterial065GenerationChecks',str(out/'baseline'),'--baseline'],
 [str(a.java_home/'bin/java.exe'),'-Xmx768m','-cp',str(classes),'CoreMaterial065GenerationChecks',str(out/'current')]]
 for command in commands:
  r=subprocess.run(command,cwd=ROOT,text=True,encoding='utf8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  print(r.stdout,end='',flush=True);logs.append(r.stdout)
  (out/'checks.log').write_text('\n'.join(logs),'utf8')
  if r.returncode:raise SystemExit(r.returncode)
 old=list(csv.DictReader((out/'baseline/graphs.csv').open()));new=list(csv.DictReader((out/'current/graphs.csv').open()));assert len(old)==len(new)==1024
 results={}
 for kind in ('TREE','SCG'):
  aa=[x for x in old if x['kind']==kind];bb=[x for x in new if x['kind']==kind]
  cols=('red','green','blue') if kind=='TREE' else ('scg',)
  counts={c:{'old':sum(int(x[c])for x in aa),'new':sum(int(x[c])for x in bb)}for c in cols}
  assert all(n['new']==n['old']*3 for n in counts.values())
  bounds={}
  for axis in ('X','Y','Z'):
   diffs=[(int(y['max'+axis])-int(y['min'+axis]))-(int(x['max'+axis])-int(x['min'+axis]))for x,y in zip(aa,bb)]
   assert min(diffs)>=-1 and max(diffs)<=1
   bounds[axis]={'mean_span_change':statistics.mean(diffs),'min_span_change':min(diffs),'max_span_change':max(diffs)}
  edge_old=statistics.mean(float(x['meanEdgeLength'])for x in aa);edge_new=statistics.mean(float(x['meanEdgeLength'])for x in bb);assert edge_new<edge_old*.65
  results[kind]={'samples':len(aa),'recoverable_centers':counts,'voxel_bounds':bounds,'mean_edge_length_old':edge_old,'mean_edge_length_new':edge_new,'minimum_new_rounded_core_distance':min(float(x['minCoreSeparation'])for x in bb)}
 oldplants=json.loads((out/'baseline/plants.json').read_text());newplants=json.loads((out/'current/plants.json').read_text());assert oldplants['treeWithoutNewSprigsSha256']==newplants['treeWithoutNewSprigsSha256'];assert oldplants['unchangedMushroomsSha256']==newplants['unchangedMushroomsSha256']
 newplants['sprigs_per_tree']=newplants['sprigs']/newplants['sample'];assert 1.95<newplants['sprigs_per_tree']<2.05
 def absence(version):
  return {(int(r['sample']),int(r['x']),int(r['y']),int(r['z'])):int(r['material'])for r in csv.DictReader((out/version/'absence.csv').open())}
 absence_old=absence('baseline');absence_new=absence('current');assert all(absence_new.get(k)==v for k,v in absence_old.items())
 assert {k:v for k,v in absence_old.items()if v>=50}=={k:v for k,v in absence_new.items()if v>=50}
 assert (out/'baseline/absence-body.sha256').read_text()==(out/'current/absence-body.sha256').read_text()
 rare={k:v for k,v in absence_new.items()if v in(11,12)};seen={k for k,v in absence_old.items()if v in(11,12)};todo=list(seen)
 while todo:
  i,x,y,z=todo.pop();material=rare[(i,x,y,z)]
  for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
   q=(i,x+dx,y+dy,z+dz)
   if q not in seen and rare.get(q)==material:seen.add(q);todo.append(q)
 assert seen==set(rare),'Every final fragment must be face-connected to an original same-type center; no overwritten-center remnants'
 before=sum(v in (11,12)for v in absence_old.values());after=sum(v in (11,12)for v in absence_new.values());assert 2.45<after/before<2.55
 report={'status':'PASS','baseline_jar':str(baseline),'baseline_sha256':hashlib.sha256(baseline.read_bytes()).hexdigest(),'scope':'Standalone actual production voxel painters; deterministic salts, not an in-world density estimate. Actual FrontierSpacing acceptance has a separate explicitly gated native preLaunch census.','graph':results,'small_birch_omega_tree':newplants,'absence_complete_painters':{'samples':4096,'old_centers':before,'new_fragment_voxels':after,'ratio':after/before,'every_old_center_and_type_preserved':True,'all_final_fragments_connected_to_old_same_type_center':True,'all_original_nonfragment_body_voxels_identical':True,'L_H_O_positions_identical':True},'unchanged_tree_body_verified':True,'unchanged_omega_mushroom_verified':True,'stdout':logs}
 (out/'audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf8')
 print('CORE_MATERIAL065_COMPARISON_OK '+str(out/'audit.json'))
if __name__=='__main__':main()
