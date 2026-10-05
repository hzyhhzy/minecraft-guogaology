"""Import the authorized 1.0.0 archive without shipping its conflicting entrypoint.

Run once against the author's JAR. The source snapshot is retained for future
upstream comparisons; runtime resources use a separate namespace, except explicit
shared-item aliases. No executable code from the archive is loaded by this tool.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse,hashlib,json,re,struct,gzip,io

ROOT=Path(__file__).resolve().parents[1]
NS='googology_outer'
ALIASES={
    'bashicu_block':'ordinal_bricks','gummy_block':'amber_guogao',
    'fruit_cake':'guogao_slice','christmas_light':'mosaic_light',
    'laver_log':'laver_vein','laver_planks':'laver_planks',
    'googology_portal':'guogao_portal','googology_portal_frame':'guogao_portal_frame',
}
for material in ('omega','epsilon','gamma','true_omega'):
    ALIASES[material+'_stone']=material+'_material'
    for part in ('ore','block','pickaxe','sword','helmet','chestplate','leggings','boots'):
        ALIASES[material+'_'+part]=material+'_'+part
    ALIASES['hell_'+material+'_ore']='nether_'+material+'_ore'

def remap_id(value):
    if not value.startswith('googology:'):return value
    name=value.split(':',1)[1]
    return ('googology:'+ALIASES[name]) if name in ALIASES else NS+':'+name

def walk(v,asset=False):
    if isinstance(v,dict):
        v={k:walk(w,asset) for k,w in v.items()}
        # Canonical shared number bricks use NUMBER, not the donor's DIGIT.
        if v.get('Name')=='googology:ordinal_bricks' and 'digit' in v.get('Properties',{}):v['Properties']['number']=v['Properties'].pop('digit')
        if v.get('id')=='googology:ordinal_bricks' and 'digit' in v.get('properties',{}):v['properties']['number']=v['properties'].pop('digit')
        if v.get('Name',v.get('id'))=='googology:laver_vein':
            v.get('Properties',v.get('properties',{})).pop('axis',None)
        return v
    if isinstance(v,list):return [walk(w,asset) for w in v]
    if isinstance(v,str):
        # Model and texture identifiers are separate from registry identifiers.
        return v.replace('googology:',NS+':') if asset else re.sub(r'googology:[a-z0-9_./-]+',lambda m:remap_id(m[0]),v)
    return v

def write(path,v):
    path.parent.mkdir(parents=True,exist_ok=True)
    if isinstance(v,(dict,list)):v=(json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode()
    path.write_bytes(v)

def nbt_transform(blob,legacy=False):
    src=io.BytesIO(gzip.decompress(blob))
    def readstr():return src.read(struct.unpack('>H',src.read(2))[0]).decode('utf8')
    def payload(t):
        if t in (1,2,3,4,5,6):return src.read({1:1,2:2,3:4,4:8,5:4,6:8}[t])
        if t in (7,11,12):
            n=src.read(4);return n+src.read(struct.unpack('>i',n)[0]*{7:1,11:4,12:8}[t])
        if t==8:return readstr()
        if t==9:
            child=src.read(1)[0];n=struct.unpack('>i',src.read(4))[0];return (child,[payload(child) for _ in range(n)])
        if t==10:
            result=[]
            while (child:=src.read(1)[0])!=0:
                name=readstr();result.append((child,name,payload(child)))
            return result
        raise ValueError(t)
    def st(s):b=s.encode('utf8');return struct.pack('>H',len(b))+b
    def encode(t,v):
        if t in (1,2,3,4,5,6,7,11,12):return v
        if t==8:return st(remap_id(v))
        if t==9:return bytes([v[0]])+struct.pack('>i',len(v[1]))+b''.join(encode(v[0],x) for x in v[1])
        if t==10:
            # 26.3 palette states use id/properties. Earlier StructureTemplate
            # silently reads them as AIR unless converted to Name/Properties.
            # Restrict this conversion to state compounds, not entity/item NBT.
            keys={b for a,b,c in v}
            is_state=keys<= {'id','properties','Name','Properties'} and bool(keys & {'id','Name'})
            if is_state:
                identity=next(c for a,b,c in v if b in ('id','Name'))
                updated=[]
                for a,b,c in v:
                    if b in ('properties','Properties'):
                        if identity in ('googology:bashicu_block','googology:ordinal_bricks'):c=[(aa,'number' if bb=='digit' else bb,cc) for aa,bb,cc in c]
                        if identity=='googology:laver_log':c=[x for x in c if x[1]!='axis']
                        b='Properties' if legacy else 'properties'
                    elif b in ('id','Name'):b='Name' if legacy else 'id'
                    updated.append((a,b,c))
                v=updated
            return b''.join(bytes([a])+st(b)+encode(a,c) for a,b,c in v)+b'\0'
    t=src.read(1)[0];name=readstr();v=payload(t)
    return gzip.compress(bytes([t])+st(name)+encode(t,v),mtime=0)

def run(archive):
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    if digest!='5310b51a48821bc49bb754068b04d89c787806679fd36303ca16a282af9f2fb4':raise ValueError('Unexpected donor archive; review changes before importing')
    res=ROOT/'src/main/resources';canonical=ROOT/'content/outer-1.0.0'
    with ZipFile(archive) as z:
        meta=json.loads(z.read('fabric.mod.json'))
        for name in z.namelist():
            if name.startswith('data/minecraft/') and name.endswith('.json'):
                write(canonical/'minecraft'/name[len('data/minecraft/'):],z.read(name))
            if name.startswith('data/googology/'):
                tail=name[len('data/googology/'):]
                if tail.endswith('.json'):
                    raw=json.loads(z.read(name));write(canonical/tail,raw)
                    if tail.startswith(('worldgen/','tags/','loot_table/','recipe/')):write(res/'data'/NS/tail,walk(raw))
                elif tail.endswith('.nbt'):
                    write(canonical/tail,z.read(name));write(res/'data'/NS/tail,nbt_transform(z.read(name)))
            elif name.startswith('assets/googology/') and not name.endswith('/'):
                tail=name[len('assets/googology/'):]
                raw=z.read(name)
                if tail.endswith('.json'):raw=walk(json.loads(raw),True)
                write(res/'assets'/NS/tail,raw)
        # Exact donor material/tool/armor art, registered once by our equipment module.
        for m in ('omega','epsilon','gamma','true_omega'):
            for part in ('pickaxe','sword','helmet','chestplate','leggings','boots'):
                write(res/f'assets/googology/textures/item/{m}_{part}.png',z.read(f'assets/googology/textures/item/{m}_{part}.png'))
            write(res/f'assets/googology/textures/item/{m}_material.png',z.read(f'assets/googology/textures/item/{m}_stone.png'))
            for part in ('ore','block'):
                write(res/f'assets/googology/textures/block/{m}_{part}.png',z.read(f'assets/googology/textures/block/{m}_{part}.png'))
            write(res/f'assets/googology/textures/block/nether_{m}_ore.png',z.read(f'assets/googology/textures/block/hell_{m}_ore.png'))
            for layer,name in [(1,'humanoid'),(2,'humanoid_leggings')]:
                image=z.read(f'assets/googology/textures/entity/equipment/{name}/{m}.png')
                write(res/f'assets/googology/textures/models/armor/{m}_layer_{layer}.png',image)
                write(res/f'assets/googology/textures/entity/equipment/{name}/{m}.png',image)
    write(canonical/'manifest.json',{'source':'googology-dimension-1.0.0.jar','sha256':digest,'authors':meta['authors'],'declared_license':meta['license'],'permission':'User reports author permission to import code/content and all material art, 2026-10-05.','aliases':ALIASES,'policy':'Preserve source terrain and biome parameters; API format adaptations only. Shared progression and travel use Guogaology.'})
    print('Imported source worldgen/templates and authorized assets; final registry/resource filtering still required.')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('archive',type=Path);run(p.parse_args().archive)
