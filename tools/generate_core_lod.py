"""Static vanilla-model fallback materials for renderers which do not use FRAPI.

Voxy's vanilla-quad path has no vertex color support. Bake a modest quantized
face tint into ONE padded atlas, preserving the full nearby mesh independently.
"""
from pathlib import Path
from PIL import Image
import json,math
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/googology'
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,separators=(',',':'))+'\n','utf8')
def generate():
    meshes=[];keys=set()
    for p in sorted((A/'core_meshes').glob('*.json')):
        v=json.loads(p.read_text('utf8'));v['textures'].pop('lod_tints',None)
        for q in v['quads']:
            q.pop('lod_t',None);q.pop('lod_uv',None)
            if q.get('part',1)>=10:continue
            if set(q['c'])=={0xffffffff}:continue
            rgba=tuple(min(255,round(sum((c>>shift)&255 for c in q['c'])/4/32)*32) for shift in (0,8,16,24))
            q['_lod_key']=(v['textures'][q['t']],rgba);keys.add(q['_lod_key'])
        meshes.append((p,v))
    keys=sorted(keys);size=2**math.ceil(math.log2(math.ceil(math.sqrt(len(keys)))*18))
    columns=size//18;atlas=Image.new('RGBA',(size,size));locations={}
    for i,(texture,rgba) in enumerate(keys):
        source=A/'textures'/(texture.split(':',1)[1]+'.png')
        tile=Image.open(source).convert('RGBA').resize((16,16),Image.Resampling.BILINEAR)
        tile.putdata([tuple(round(c[j]*rgba[j]/255) for j in range(4)) for c in tile.getdata()])
        x=i%columns*18;y=i//columns*18
        atlas.paste(tile,(x+1,y+1));atlas.paste(tile.crop((0,0,1,16)),(x,y+1));atlas.paste(tile.crop((15,0,16,16)),(x+17,y+1))
        atlas.paste(tile.crop((0,0,16,1)),(x+1,y));atlas.paste(tile.crop((0,15,16,16)),(x+1,y+17))
        locations[(texture,rgba)]=(x+1,y+1)
    atlas.save(A/'textures/block/core_lod_tints.png')
    for p,v in meshes:
        tinted=False
        for q in v['quads']:
            key=q.pop('_lod_key',None)
            if key is None:continue
            x,y=locations[key];q['lod_t']='lod_tints';q['lod_uv']=[[(x+u*16)/size,(y+w*16)/size] for u,w in q['uv']];tinted=True
        if tinted:v['textures']['lod_tints']='googology:block/core_lod_tints'
        write(p,v)
        model=A/f'models/block/{p.stem}.json';m=json.loads(model.read_text('utf8'));m['textures']=v['textures'];write(model,m)
    # Keep the inspectable grade inventory consistent with the actual art.
    # Shape revisions must not leave historical face counts/motion metadata.
    manifest=ROOT/'src/main/resources/googology/core_grades.json'
    grades=json.loads(manifest.read_text('utf8'))
    models={p.stem:v for p,v in meshes}
    for grade in grades:
        if grade['id'] not in models:continue
        mesh=models[grade['id']]
        grade.update(quads=len(mesh['quads']),
                     dynamic_quads=sum(q.get('part',0)>0 for q in mesh['quads']),
                     motion=mesh.get('motion','still'),extended=mesh.get('extended',False))
    write(manifest,grades)
    print(f'LOD material atlas: {len(keys)} tint tiles in one {size}x{size} sprite; {len(meshes)} static fallbacks')
if __name__=='__main__':generate()
