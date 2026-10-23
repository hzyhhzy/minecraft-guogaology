"""Read-only block texture/UV audit against the local Minecraft 26.2 assets.

Writes evidence outside the production resource tree. This does not audit item
icons and never starts Minecraft or Gradle. Re-run after regenerated block art.
"""
from __future__ import annotations
import argparse
import base64
from collections import Counter, defaultdict
from datetime import datetime, timezone
from functools import lru_cache
import hashlib
import html
from io import BytesIO
import json
import math
from pathlib import Path
import re
import statistics
import zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
A = RES / 'assets/guogaology'
DEFAULT_OUT = ROOT.parent / 'outputs/20261018-block-pixels'
DEFAULT_JAR = Path.home() / '.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
TINT_ATLAS = 'guogaology:block/core_lod_tints'
REGIONAL_CORE_ROOTS = ('sequence_core','power_tower_core','hydra_bud','lho_trace',
                       'laver_core','astra_critical_core','boundary_core','guogao_heart')
DISPLAYED_CORE_IDS = frozenset(
    {root+suffix for root in REGIONAL_CORE_ROOTS for suffix in ('','_lv2','_lv3')}
    | {'ordinal_crystal_lv2','ordinal_crystal_lv3','ordinal_crystal_lv4'})


def read_json(path):
    return json.loads(path.read_text('utf8'))


def ident(value):
    return value if ':' in value else 'minecraft:' + value


def rounded(value):
    return round(value, 4)


def vector_length(a, b):
    return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


class Audit:
    def __init__(self, jar):
        self.jar_path = jar
        self.jar = zipfile.ZipFile(jar)
        self.names = set(self.jar.namelist())
        self.issues = set()
        self.texture_info = {}
        self.model_info = {}
        self.model_sources = {}

    def resource(self, namespace, kind, name, suffix):
        resource = f'assets/{namespace}/{kind}/{name}{suffix}'
        path = RES / resource
        if path.exists():
            return path.read_bytes(), str(path.relative_to(ROOT)).replace('\\', '/')
        if resource in self.names:
            return self.jar.read(resource), str(self.jar_path) + '!/' + resource
        return None, resource

    def json_resource(self, identifier, kind):
        namespace, name = ident(identifier).split(':', 1)
        data, source = self.resource(namespace, kind, name, '.json')
        if data is None:
            self.issues.add(('missing_' + kind, identifier, source))
            return {}, source
        if kind=='models':
            self.model_sources[ident(identifier)]=source
        return json.loads(data), source

    @lru_cache(None)
    def model(self, identifier, trail=()):
        identifier = ident(identifier)
        if identifier in trail:
            self.issues.add(('model_parent_cycle', identifier, str(trail)))
            return {'textures': {}, 'elements': [], '_chain': [identifier]}
        if identifier.startswith('minecraft:builtin/'):
            return {'textures': {}, 'elements': [], '_chain': [identifier], '_builtin': identifier}
        raw, source = self.json_resource(identifier, 'models')
        parent = self.model(ident(raw['parent']), trail+(identifier,)) if raw.get('parent') else {}
        result = {**parent, **raw}
        result['textures'] = {**parent.get('textures', {}), **raw.get('textures', {})}
        result['_chain'] = [identifier] + parent.get('_chain', [])
        result['_source'] = source
        return result

    def texture(self, value, slots, trail=()):
        # 26.2 accepts material objects (sprite plus render flags) in slots.
        if isinstance(value,dict):
            value=value['sprite']
        if not value.startswith('#'):
            return ident(value)
        key = value[1:]
        if key in trail or key not in slots:
            self.issues.add(('texture_alias', value, json.dumps(slots, sort_keys=True)))
            return 'UNRESOLVED:' + value
        return self.texture(slots[key], slots, trail+(key,))

    @lru_cache(None)
    def image(self, identifier):
        if identifier.startswith('UNRESOLVED:'):
            return None
        namespace, name = ident(identifier).split(':', 1)
        data, source = self.resource(namespace, 'textures', name, '.png')
        if data is None:
            self.issues.add(('missing_texture', identifier, source))
            return None
        im = Image.open(BytesIO(data)).convert('RGBA')
        meta_data, _ = self.resource(namespace, 'textures', name, '.png.mcmeta')
        animation = json.loads(meta_data).get('animation') if meta_data else None
        w, h = im.size
        fw, fh = w, h
        if animation is not None:
            if 'width' in animation and 'height' in animation:
                fw, fh = animation['width'], animation['height']
            elif 'width' in animation:
                fw, fh = animation['width'], h
            elif 'height' in animation:
                fw, fh = w, animation['height']
            else:
                fw = fh = min(w, h)
        colors = Counter(im.getdata())
        self.texture_info[identifier] = {
            'texture': identifier, 'source': source, 'png_size': [w, h],
            'frame_size': [fw, fh], 'animation': animation,
            'physical_frame_count': w//fw*(h//fh),
            'sha256': hashlib.sha256(data).hexdigest(), 'rgba_color_count': len(colors),
            'partial_alpha_pixels': sum(n for c,n in colors.items() if 0 < c[3] < 255),
            'interpretation': 'PNG dimensions describe stored texels, not proof of a pixel-art style',
        }
        return im

    def uv_sample(self, texture, uv, edge_lengths=None, source='model_face'):
        self.image(texture)
        info = self.texture_info.get(texture)
        if not info:
            return None
        w, h = info['frame_size']
        us, vs = [v[0] for v in uv], [v[1] for v in uv]
        span = [(max(us)-min(us))*w, (max(vs)-min(vs))*h]
        density = []
        if edge_lengths:
            for end, length in zip((1, 3), edge_lengths):
                if length > 1e-9:
                    px = math.hypot((uv[end][0]-uv[0][0])*w, (uv[end][1]-uv[0][1])*h)
                    if px > 1e-9:
                        density.append(px*16/length)
        return {'texture': texture, 'span_px': span, 'density_texels_per_block': density,
                'bounds_normalized': [min(us), min(vs), max(us), max(vs)], 'source': source}

    @staticmethod
    def aggregate(samples):
        by_texture = defaultdict(list)
        for sample in samples:
            if sample:
                by_texture[sample['texture']].append(sample)
        result = []
        for texture, group in sorted(by_texture.items()):
            sizes = Counter(tuple(rounded(n) for n in s['span_px']) for s in group)
            densities = [n for s in group for n in s['density_texels_per_block']]
            result.append({
                'texture': texture, 'face_count': len(group),
                'face_sample_sizes_px': [{'size': list(size), 'faces': count} for size,count in sizes.most_common()],
                'density_texels_per_block': None if not densities else {
                    'min': rounded(min(densities)), 'median': rounded(statistics.median(densities)),
                    'max': rounded(max(densities)),
                },
                'overall_uv_bounds': [rounded(min(s['bounds_normalized'][0] for s in group)),
                                      rounded(min(s['bounds_normalized'][1] for s in group)),
                                      rounded(max(s['bounds_normalized'][2] for s in group)),
                                      rounded(max(s['bounds_normalized'][3] for s in group))],
            })
        return result

    def analyze_model(self, identifier):
        identifier = ident(identifier)
        if identifier in self.model_info:
            return self.model_info[identifier]
        model = self.model(identifier)
        slots = model.get('textures', {})
        resolved = {slot: self.texture(value, slots) for slot,value in slots.items()}
        for value in resolved.values():
            self.image(value)
        samples, rotations = [], 0
        for element in model.get('elements', []):
            a, b = element['from'], element['to']
            x1,y1,z1 = a; x2,y2,z2 = b
            rotations += bool(element.get('rotation', {}).get('angle'))
            # Vanilla's implicit face UV projection, in model units (0..16).
            defaults = {'down':[x1,16-z2,x2,16-z1], 'up':[x1,z1,x2,z2],
                        'north':[16-x2,16-y2,16-x1,16-y1], 'south':[x1,16-y2,x2,16-y1],
                        'west':[z1,16-y2,z2,16-y1], 'east':[16-z2,16-y2,16-z1,16-y1]}
            sizes = {'up':(abs(x2-x1),abs(z2-z1)), 'down':(abs(x2-x1),abs(z2-z1)),
                     'north':(abs(x2-x1),abs(y2-y1)), 'south':(abs(x2-x1),abs(y2-y1)),
                     'east':(abs(z2-z1),abs(y2-y1)), 'west':(abs(z2-z1),abs(y2-y1))}
            rotation=element.get('rotation',{})
            if rotation.get('rescale') and rotation.get('angle'):
                axis=rotation['axis'];factor=1/abs(math.cos(math.radians(rotation['angle'])))
                axes={'up':('x','z'),'down':('x','z'),'north':('x','y'),
                      'south':('x','y'),'east':('z','y'),'west':('z','y')}
                sizes={face:tuple(length*(1 if direction==axis else factor)
                                  for length,direction in zip(lengths,axes[face]))
                       for face,lengths in sizes.items()}
            for direction, face in element.get('faces', {}).items():
                texture = self.texture(face['texture'], slots)
                u1,v1,u2,v2 = [n/16 for n in face.get('uv', defaults[direction])]
                uv = [[u1,v1],[u2,v1],[u2,v2],[u1,v2]]
                turns = face.get('rotation',0)//90
                if turns:
                    uv = uv[turns:]+uv[:turns]
                samples.append(self.uv_sample(texture, uv, sizes[direction]))
        result = {'model': identifier, 'source': model.get('_source'), 'parent_chain': model['_chain'],
                  'builtin': model.get('_builtin'), 'element_count': len(model.get('elements', [])),
                  'rotated_elements': rotations, 'texture_slot_definitions': slots,
                  'declared_texture_slots': resolved,
                  'face_texture_usage': self.aggregate(samples)}
        self.model_info[identifier] = result
        return result

    def analyze_mesh(self, block):
        path = A/f'core_meshes/{block}.json'
        if not path.exists():
            return None
        mesh = read_json(path)
        slots = mesh['textures']
        samples, fallback = [], []
        smooth = 0
        for q in mesh['quads']:
            texture = self.texture('#'+q['t'],slots)
            edges = [vector_length(q['v'][0], q['v'][end]) for end in (1,3)]
            samples.append(self.uv_sample(texture,q['uv'],edges,'custom_mesh'))
            if q.get('lod_t'):
                fallback.append(self.uv_sample(self.texture('#'+q['lod_t'],slots),q.get('lod_uv',q['uv']),edges,'voxy_lod'))
            normals = q.get('n',[])
            if normals and any(any(abs(x-y)>1e-5 for x,y in zip(normals[0],n)) for n in normals[1:]):
                smooth += 1
        resolved = {key:self.texture(value,slots) for key,value in slots.items()}
        for value in resolved.values():
            self.image(value)
        return {'source':str(path.relative_to(ROOT)).replace('\\','/'), 'quad_count':len(mesh['quads']),
                'quads_with_interpolated_vertex_normals':smooth, 'motion':mesh.get('motion'),
                'declared_texture_slots':resolved,'face_texture_usage':self.aggregate(samples),
                'lod_texture_usage':self.aggregate(fallback),
                'renderer_source':'ports/common/client/dev/guogaology/client/CoreMeshModels.java',
                'renderer_notes':'World geometry is replaced by core_meshes. Upper grades use CoreAnimationRenderer; Voxy uses static fallback. UVs here are normalized 0..1, unlike vanilla model 0..16.'}


def references(state):
    result = []
    for condition, values in state.get('variants',{}).items():
        for value in values if isinstance(values,list) else [values]:
            result.append({'condition':condition,**value})
    for number, part in enumerate(state.get('multipart',[])):
        values = part['apply']
        for value in values if isinstance(values,list) else [values]:
            result.append({'condition':'multipart '+str(number)+': '+json.dumps(part.get('when',{}),ensure_ascii=False),**value})
    return result


def registry_inventory():
    """Bounded static expansion of this repository's actual registration families."""
    sources = {}
    alias_source='ports/common/main/dev/guogaology/outer/GuogaologyMod.java'
    alias_text=(ROOT/alias_source).read_text('utf8')
    aliases={name:target for case,target in re.findall(r'case\s+([^\n]+?)\s*->\s*"([a-z0-9_:]+)"',alias_text)
             for name in re.findall(r'"([a-z0-9_]+)"',case)}
    def add(name, source):
        if '/outer/' in source and name in aliases:
            target=aliases[name]
            if not target.startswith('guogaology:'):
                return
            source+=' via '+alias_source+' '+name+' → '+target
            name=target.split(':',1)[1]
        sources.setdefault(name,set()).add(source)
    main='src/main/java/dev/guogaology/GuogaologyBlocks.java'
    text=(ROOT/main).read_text('utf8')
    for name in re.findall(r'\b(?:block|translucent|landscape|relic|lantern|jelly|flower|symbol|plainLamp|digitLamp)\("([a-z0-9_]+)"',text):
        add(name,main)
    for n in range(10):add('ordinal_stone'+('' if n==0 else '_'+str(n)),main+' numberStones loop')
    core='src/main/java/dev/guogaology/CoreResources.java'
    for name in re.findall(r'\b(?:node|ore)\("([a-z0-9_]+)"',(ROOT/core).read_text('utf8')):add(name,core)
    grade='src/main/java/dev/guogaology/CoreGrades.java'
    roots=re.findall(r'"([a-z0-9_]+)"',(ROOT/grade).read_text('utf8').split('ROOTS=List.of(',1)[1].split(');',1)[0])
    for name in roots:
        for level in range(2,5 if name=='ordinal_crystal' else 4):add(name+'_lv'+str(level),grade+' level loop')
    outer='ports/common/main/dev/guogaology/outer/registry/ModBlocks.java'
    text=(ROOT/outer).read_text('utf8')
    for name in re.findall(r'\bregister(?:Item|NoItem)\(\s*"([a-z0-9_]+)"',text):add(name,outer)
    def strings(constant):
        match=re.search(r'\b'+constant+r'\s*=\s*List.of\(([^;]+)\)',text)
        return re.findall(r'"([a-z0-9_]+)"',match.group(1))
    decor='ports/common/main/dev/guogaology/outer/registry/ModWoodDecorBlocks.java'
    decor_shapes=re.findall(r'"([a-z0-9_]+)"',(ROOT/decor).read_text('utf8').split('DECOR_SHAPES = List.of(',1)[1].split(');',1)[0])
    for family in strings('WOOD_IDS'):
        for shape in strings('WOOD_SHAPES'):add(family+'_'+shape,outer+' wood loop')
        for shape in decor_shapes+['sign','wall_sign','hanging_sign','wall_hanging_sign']:add(family+'_'+shape,decor+' decor loop')
    for family in strings('FOLIAGE_IDS'):
        for shape in ('leaves','sapling'):add(family+'_'+shape,outer+' foliage loop')
    mining='src/main/java/dev/guogaology/mining/MiningContent.java'
    rules=(ROOT/'src/main/java/dev/guogaology/mining/EquipmentRules.java').read_text('utf8')
    minerals=re.findall(r'"([a-z0-9_]+)"',re.search(r'MINERALS=\{([^}]+)',rules).group(1))
    for mineral in minerals:
        for suffix in ('_block','_ore'):add(mineral+suffix,mining+' mineral loop')
        add('nether_'+mineral+'_ore',mining+' mineral loop')
    for rank in range(1,4):add('enhancement_table'+('' if rank==1 else '_'+str(rank)),mining+' table loop')
    lamp='src/main/java/dev/guogaology/lamp/LampShapingContent.java'
    add(re.search(r'ID="([a-z0-9_]+)"',(ROOT/lamp).read_text('utf8')).group(1),lamp)
    return {name:sorted(value) for name,value in sorted(sources.items())}


def density_range(usages):
    values=[value['density_texels_per_block'] for value in usages if value.get('density_texels_per_block')]
    return None if not values else [min(value['min'] for value in values),max(value['max'] for value in values)]


def resolution_label(info):
    w,h=info['png_size'];fw,fh=info['frame_size']
    return f'{w}×{h}'+(f' → {fw}×{fh}/帧，{info["physical_frame_count"]}帧' if info['animation'] is not None else '')


def export_texture_images(audit,out):
    """One byte-identical exported source PNG per resource ID, shared by rows."""
    for identifier,info in audit.texture_info.items():
        namespace,name=identifier.split(':',1)
        relative=Path('texture-images')/namespace/(name+'.png')
        path=out/relative;path.parent.mkdir(parents=True,exist_ok=True)
        data,_=audit.resource(namespace,'textures',name,'.png')
        assert hashlib.sha256(data).hexdigest()==info['sha256']
        if not path.exists() or path.read_bytes()!=data:path.write_bytes(data)
        info['exported_image']=relative.as_posix()
        animation=info['animation']
        first=animation.get('frames',[0])[0] if animation and animation.get('frames') else 0
        if isinstance(first,dict):first=first['index']
        fw,fh=info['frame_size'];columns=info['png_size'][0]//fw
        info['preview_frame_index']=first
        info['preview_rect']=[first%columns*fw,first//columns*fh,fw,fh]


def ordered_textures(block,report):
    counts=Counter()
    for usage in block['world_texture_usage']:counts[usage['texture']]+=usage['face_count']
    def priority(name):
        generic=bool(re.search(r'/(?:polished_|optical_clean_|surface_|d_optical_|d_frame_)',name))
        return (name==TINT_ATLAS,name not in counts,generic,
                report['textures'].get(name,{}).get('rgba_color_count',0)<=2,-counts[name],name)
    return sorted(block['textures'],key=priority)


def texture_card(name,report,world_used,compact=False):
    esc=lambda value:html.escape(str(value))
    info=report['textures'].get(name)
    if not info:return '<span>未解析 '+esc(name)+'</span>'
    w,h=info['png_size'];x,y,fw,fh=info['preview_rect'];scale=min(64/fw,64/fh)
    role='LOD图集' if name==TINT_ATLAS else '世界表面' if name in world_used else '声明/粒子/后备'
    animation=info['animation'] is not None
    label=resolution_label(info)+(' · 首帧' if animation else '')
    image=(f'<span class="texture-swatch"><span class="texture-window" style="width:{fw*scale:g}px;height:{fh*scale:g}px">'
           f'<img loading="lazy" decoding="async" src="{esc(info["exported_image"])}" alt="{esc(name)}" '
           f'style="width:{w*scale:g}px;height:{h*scale:g}px;left:{-x*scale:g}px;top:{-y*scale:g}px"></span></span>')
    title=name.rsplit('/',1)[-1] if compact else name
    return (f'<figure class="texture-card {"compact" if compact else ""}"><button type="button" class="texture-open" '
            f'data-texture="{esc(name)}" title="{esc(name)} · {esc(label)} · 点击放大" aria-label="放大{esc(name)}">{image}</button>'
            f'<figcaption><code title="{esc(name)}">{esc(title)}</code><small>{esc(label)}</small>'
            f'<span class="texture-role {"atlas-role" if name==TINT_ATLAS else ""}">{role}</span></figcaption></figure>')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft-jar',type=Path,default=DEFAULT_JAR)
    parser.add_argument('--out',type=Path,default=DEFAULT_OUT)
    args=parser.parse_args();out=args.out.resolve();out.mkdir(parents=True,exist_ok=True)
    audit=Audit(args.minecraft_jar)
    version=json.loads(audit.jar.read('version.json'))['id']
    if version!='26.2':raise SystemExit('Expected actual 26.2 resources, got '+version)
    lang=read_json(A/'lang/zh_cn.json')
    registry=registry_inventory()
    special_path=out/'special-cases.json'
    if not special_path.exists():special_path=ROOT/'tools/reference/block-pixel-special-cases.json'
    special=read_json(special_path) if special_path.exists() else {}
    blocks=[]
    for path in sorted((A/'blockstates').glob('*.json')):
        block=path.stem;state=read_json(path);refs=references(state)
        model_names=sorted(set(ident(ref['model']) for ref in refs))
        models=[audit.analyze_model(name) for name in model_names]
        mesh=audit.analyze_mesh(block)
        world_usages=mesh['face_texture_usage'] if mesh else [u for m in models for u in m['face_texture_usage']]
        declared=set(value for m in models for value in m['declared_texture_slots'].values())
        if mesh:declared.update(mesh['declared_texture_slots'].values())
        used={u['texture'] for u in world_usages}
        missing=[value for value in declared if value not in audit.texture_info]
        infos=[audit.texture_info[value] for value in sorted(declared) if value in audit.texture_info]
        resolutions=sorted(set(resolution_label(info) for info in infos))
        categories=sorted(set(('动画' if info['animation'] is not None else '图集' if info['texture']==TINT_ATLAS else str(info['png_size'][0]) if info['png_size'][0]==info['png_size'][1] else '非方形') for info in infos))
        notes=[]
        if mesh:notes.append('实际世界使用自定义网格；JSON方块模型另作静态/后备参考，分开统计')
        if any(info['texture']==TINT_ATLAS for info in infos):notes.append('core_lod_tints是512×512后备颜色图集；16×16内容图块外加1px护边，不按512px单面计')
        if any(info['animation'] is not None for info in infos):notes.append('动画长图按单帧尺寸计，不把整条高度算作一面精度')
        for info in infos:
            if info['png_size'][0]!=info['png_size'][1] and info['animation'] is None:
                notes.append(info['texture']+' 是静态非方形'+str(info['png_size'][0])+'×'+str(info['png_size'][1])+'单图，不是动画条')
        if block=='ordinal_bricks':notes.append('基底为16×16原图最近邻放大到32×32，数字笔画另按matrix_masks网格写入；不能当作全部细节原生32设计')
        if block=='guogao_drop':notes.append('用户最新要求32×32原生/约28×28表情区/轻圆角；本行尺寸取当前实际PNG，最终生成后重跑')
        geometry='自定义四边形网格' if mesh else '旋转长方体模型' if any(m['rotated_elements'] for m in models) else '长方体/交叉片模型'
        style='待人工确认；已确认纹理尺寸与UV'
        blocks.append({'id':'guogaology:'+block,'path':block,'name_zh':lang.get('block.guogaology.'+block,block),
                       'is_displayed_core':block in DISPLAYED_CORE_IDS,
                       'blockstate':str(path.relative_to(ROOT)).replace('\\','/'),'registration_evidence':registry.get(block,[]),
                       'model_reference_count':len(refs),'model_count':len(model_names),'state_models':refs,
                       'models':model_names,'custom_mesh':mesh,'source_resolutions':resolutions,'resolution_categories':categories,
                       'declared_texture_count':len(declared),'world_used_texture_count':len(used),
                       'textures':sorted(declared),'world_texture_usage':world_usages,
                       'effective_density_texels_per_block':density_range(world_usages),
                       'geometry':geometry,'visual_style':style,'notes':notes,'unresolved_textures':missing})
    states={b['path'] for b in blocks}
    assert len(DISPLAYED_CORE_IDS)==27 and DISPLAYED_CORE_IDS<=states
    profiles=set(read_json(RES/'guogaology/block_balance.json'))
    names={key.removeprefix('block.guogaology.') for key in lang if key.startswith('block.guogaology.') and '.' not in key.removeprefix('block.guogaology.')}
    manifest=read_json(ROOT/'tools/outer_generated_resources.json')
    manifest_states={Path(p).stem for p in manifest if '/blockstates/' in p}
    coverage={'static_registered_ids':len(registry),'blockstates':len(states),
              'registered_without_blockstate':sorted(set(registry)-states),
              'blockstates_without_static_registration_evidence':sorted(states-set(registry)),
              'block_balance_without_blockstate':sorted(profiles-states),
              'translated_block_ids_without_blockstate':sorted(names-states),
              'outer_manifest_blockstates_missing':sorted(manifest_states-states),
              'method':'Static Java helper calls and explicit loop families, cross-checked with blockstate/language/balance/Outer resource manifest. No runtime registry claimed.'}
    metadata={'mod_version':next(line.split('=',1)[1] for line in (ROOT/'gradle.properties').read_text('utf8').splitlines() if line.startswith('mod_version=')),
              'generated_utc':datetime.now(timezone.utc).isoformat(),'target_minecraft':version,
              'minecraft_assets_jar':str(args.minecraft_jar),'minecraft_assets_jar_sha256':hashlib.sha256(args.minecraft_jar.read_bytes()).hexdigest(),
              'scope':'All guogaology blockstates, all variants/multipart models and inherited models/textures; item icons excluded.',
              'limitations':['No resource packs or runtime shader effects included.','PNG storage dimensions alone do not establish pixel-art style.','Effective density uses raw mesh/model coordinates (including vanilla element rescale), source texels per world block along face edges; animation-part scale is not folded into this static baseline. It is not an on-screen pixel count.','Geometry changes, normals, motion, alpha and filtering may affect visual smoothness independently of texture size.']}
    report={'metadata':metadata,'coverage':coverage,'blocks':blocks,'models':audit.model_info,
            'core_filter':{'excluded_exact_ids':['guogaology:'+name for name in sorted(DISPLAYED_CORE_IDS)],
                           'ordinary_ordinal_crystal_retained':True,'default_hidden':False},
            'resolved_model_sources_including_parents':audit.model_sources,'textures':audit.texture_info,
            'special_cases':special,'unresolved':[{'type':kind,'id':identifier,'detail':detail} for kind,identifier,detail in sorted(audit.issues)]}
    apply_special_evidence(report)
    apply_style_evidence(report,out)
    report['summary']={'blocks':len(blocks),'referenced_models':len(audit.model_info),
                       'resolved_models_including_parents':len(audit.model_sources),'source_textures':len(audit.texture_info),
                       'texture_sizes':dict(Counter(resolution_label(info) for info in audit.texture_info.values())),
                       'block_resolution_groups':dict(Counter(' / '.join(b['resolution_categories']) for b in blocks)),
                       'visual_styles':dict(Counter(b['visual_style'] for b in blocks)),
                       'custom_mesh_blocks':sum(bool(b['custom_mesh']) for b in blocks),'unresolved':len(audit.issues)}
    export_texture_images(audit,out)
    (out/'block-pixels.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf8')
    make_contact_sheets(audit,out)
    render_html(report,out)
    print(json.dumps({'out':str(out),'summary':report['summary'],'coverage':coverage},ensure_ascii=False))


def apply_style_evidence(report,out):
    """Optional reviewed classifications are evidence, never inferred from size."""
    path=out/'style-evidence.json'
    if not path.exists():path=ROOT/'tools/reference/block-pixel-styles.json'
    evidence=read_json(path) if path.exists() else {'blocks':{},'texture_groups':[]}
    report['style_evidence']=evidence
    for block in report['blocks']:
        entry=evidence.get('blocks',{}).get(block['path'])
        hashes=entry.get('texture_sha256',{}) if entry else {}
        if entry and set(hashes)==set(block['textures']) and all(report['textures'].get(name,{}).get('sha256')==digest for name,digest in hashes.items()):
            block['visual_style']=entry['style'];block['style_evidence']=entry
        elif entry:
            block['notes'].append('人工风格证据的纹理哈希已变化，需要复查；本行保留待确认')


def apply_special_evidence(report):
    special=report['special_cases'];blocks={b['id']:b for b in report['blocks']}
    for evidence in special.get('meshes',[]):
        block=blocks.get(evidence['block_id'])
        if not block:continue
        block['special_rendering_evidence']=evidence
        labels=evidence.get('labels',[])
        if 'interpolated_vertex_normals' in labels:
            block['geometry']='自定义四边形网格；平滑顶点法线'
            block['notes'].append('曲面使用变化顶点法线，明暗可连续插值；与PNG像素精度分开判断')
        if 'interpolated_vertex_color' in labels:
            block['notes'].append('表面含顶点色插值，材质颜色可连续变化，非运行时生成PNG')
        if 'effective_bilinear_lod_downsample' in labels:
            block['notes'].append('静态LOD图块含实际BILINEAR缩小；近处仍用原始源纹理')
        if block.get('custom_mesh') and evidence.get('mesh_sha256'):
            actual=hashlib.sha256((ROOT/block['custom_mesh']['source']).read_bytes()).hexdigest()
            if actual!=evidence['mesh_sha256']:
                block['notes'].append('特殊渲染旁证快照的mesh哈希与当前不同，需复查；主表几何/UV仍由当前文件重算')
    for evidence in special.get('animated_portals',[]):
        block=blocks.get(evidence['block_id'])
        if block:
            block['special_rendering_evidence']=evidence
            block['notes'].append('原生32×32序列帧，interpolate只做帧间时间颜色插值；不提升单帧空间分辨率')
    for evidence in special.get('contextual_overlays',[]):
        for identifier in evidence.get('block_ids',[]):
            block=blocks.get(identifier)
            if block:
                block.setdefault('contextual_overlays',[]).append(evidence)
                block['notes'].append('特定演奏/节拍条件下叠加无纹理颜色薄层；不改变PNG原生尺寸')
    for evidence in special.get('vanilla_block_entity_rendering',{}).get('signs',[]):
        block=blocks.get(evidence['block_id'])
        if block:
            block['special_rendering_evidence']=evidence
            block['notes'].append('26.2告示牌板身来自普通JSON模型；原版BE另画玩家文字，字体字形不作为固定方块纹理像素数')


def make_contact_sheets(audit,out):
    items=sorted(audit.texture_info)
    directory=out/'texture-samples';directory.mkdir(exist_ok=True)
    try:font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',11)
    except OSError:font=ImageFont.load_default()
    for page in range(math.ceil(len(items)/80)):
        im=Image.new('RGB',(1000,880),'#d8dce1');draw=ImageDraw.Draw(im)
        for n,identifier in enumerate(items[page*80:(page+1)*80]):
            x=(n%10)*100;y=(n//10)*110
            tex=audit.image(identifier);x0,y0,fw,fh=audit.texture_info[identifier]['preview_rect']
            crop=tex.crop((x0,y0,x0+fw,y0+fh));crop.thumbnail((64,64),Image.Resampling.NEAREST)
            for yy in range(y,y+68,8):
                for xx in range(x,x+96,8):draw.rectangle((xx,yy,xx+7,yy+7),fill='#a4a9b0' if ((xx-x)//8+(yy-y)//8)%2 else '#c4c8ce')
            im.paste(crop,(x+(96-crop.width)//2,y+(68-crop.height)//2),crop)
            name=identifier.split('/')[-1];draw.text((x+2,y+70),name[:15],font=font,fill='#17202b')
            draw.text((x+2,y+83),name[15:30],font=font,fill='#17202b')
            draw.text((x+2,y+96),str(audit.texture_info[identifier]['png_size']),font=font,fill='#253d62')
        im.save(directory/f'page-{page+1:02}.png')
    (directory/'index.json').write_text(json.dumps([{'page':i//80+1,'cell':i%80,'texture':value} for i,value in enumerate(items)],ensure_ascii=False,indent=2)+'\n','utf8')


def render_html(report,out):
    esc=lambda value:html.escape(str(value))
    summary=report['summary'];coverage=report['coverage']
    rows=[]
    for block in report['blocks']:
        density=block['effective_density_texels_per_block']
        density_text='无可测贴图面' if density is None else f'{density[0]:g}–{density[1]:g} texel/格'
        world_used={value['texture'] for value in block['world_texture_usage']}
        ordered=ordered_textures(block,report)
        preview='<div class="texture-preview">'+''.join(texture_card(name,report,world_used,True) for name in ordered[:6])+'</div>'
        gallery='<div class="texture-gallery">'+''.join(texture_card(name,report,world_used) for name in ordered)+'</div>'
        usages=[]
        for value in block['world_texture_usage']:
            size=value['face_sample_sizes_px'];examples='、'.join('×'.join(f'{n:g}' for n in pair['size'])+f'（{pair["faces"]}面）' for pair in size[:8])
            if len(size)>8:examples+=f' 等{len(size)}种'
            d=value['density_texels_per_block'];dt='—' if not d else f'{d["min"]:g}–{d["max"]:g}；中位{d["median"]:g}'
            usages.append('<li><code>'+esc(value['texture'])+'</code><br>单面采样像素跨度：'+esc(examples)+'；密度 '+esc(dt)+'</li>')
        lod=[]
        if block['custom_mesh']:
            for value in block['custom_mesh']['lod_texture_usage']:
                sizes=value['face_sample_sizes_px'];examples='、'.join('×'.join(f'{n:g}' for n in pair['size']) for pair in sizes[:10])
                if len(sizes)>10:examples+=f' 等{len(sizes)}种UV跨度'
                lod.append('<li><code>'+esc(value['texture'])+'</code> · 单面实际采样 '+esc(examples)+' px；来源16×16内容图块，另有护边；此处使用lod_uv，未拿世界uv直接乘512。</li>')
        style_evidence=block.get('style_evidence',{})
        evidence_text=style_evidence.get('reason','')
        evidence_sources=''.join('<li><code>'+esc(source)+'</code></li>' for source in style_evidence.get('evidence_sources',[]))
        details='<details class="all-textures"><summary>展开全部'+str(len(ordered))+'张纹理、变体、UV与模型</summary><p>各状态/变体用图全部列在下方；图片可点开在页内放大。原PNG不含模型顶点色、游戏光照和几何效果。声明/粒子/后备图与实际世界表面分开标记。</p>'+gallery+'<h4>实际世界表面UV</h4><ul>'+''.join(usages)+'</ul>'+('<h4>Voxy/静态后备图集采样</h4><ul>'+''.join(lod)+'</ul>' if lod else '')+'<h4>全部状态引用模型</h4><ul>'+''.join('<li><code>'+esc(name)+'</code></li>' for name in block['models'])+'</ul><h4>风格审查依据</h4><p>'+esc(evidence_text)+'</p><ul>'+evidence_sources+'</ul><p>逐状态条件、parent链、别名和完整UV统计见机器JSON。</p></details>'
        note='；'.join(block['notes'])
        mesh=block['custom_mesh']
        if mesh:note+=f'；{mesh["quad_count"]}四边形，{mesh["quads_with_interpolated_vertex_normals"]}面使用变化顶点法线'
        search=' '.join([block['name_zh'],block['id'],block['visual_style'],block['geometry'],note,*block['models'],*block['textures']])
        rows.append('<tr data-id="'+esc(block['id'])+'" data-core="'+('1' if block['is_displayed_core'] else '0')+'" data-search="'+esc(search)+'" data-category="'+esc(' '.join(block['resolution_categories']))+'" data-style="'+esc(block['visual_style'])+'"><td><strong>'+esc(block['name_zh'])+'</strong><code>'+esc(block['id'])+'</code><small>'+str(block['model_count'])+'个模型 / '+str(block['model_reference_count'])+'条状态引用</small></td><td>'+esc('；'.join(block['source_resolutions']))+'<small>纹理 '+str(block['declared_texture_count'])+' 张；世界实际用 '+str(block['world_used_texture_count'])+' 张</small>'+preview+'</td><td>'+esc(block['visual_style'])+'<small>'+esc(block['geometry'])+'</small></td><td>'+esc(density_text)+'<small>'+esc(note)+'</small>'+details+'</td></tr>')
    unresolved='<pre>'+esc(json.dumps(report['unresolved'],ensure_ascii=False,indent=2))+'</pre>' if report['unresolved'] else '<p class="good">所有模型及纹理引用均已找到真实资源；没有未解析项。</p>'
    categories=sorted({category for block in report['blocks'] for category in block['resolution_categories']},key=lambda x:(not x.isdigit(),int(x) if x.isdigit() else x))
    options=''.join(f'<option>{esc(value)}</option>' for value in categories)
    styles=''.join(f'<option>{esc(value)}</option>' for value in sorted({b['visual_style'] for b in report['blocks']}))
    cards=''.join(f'<div class="metric"><b>{number}</b><span>{label}</span></div>' for number,label in ((summary['blocks'],'方块ID'),(summary['referenced_models'],'关联模型'),(summary['source_textures'],'真实源纹理'),(summary['custom_mesh_blocks'],'自定义网格方块'),(summary['unresolved'],'未解析引用')))
    samples=''.join('<a href="texture-samples/'+path.name+'">抽样联系表 '+str(i+1)+'</a>' for i,path in enumerate(sorted((out/'texture-samples').glob('page-*.png'))))
    text='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>果糕逻辑方块像素审计</title><style>
    *{box-sizing:border-box}body{margin:0;background:#101721;color:#dce7ed;font:14px/1.6 system-ui,"Microsoft YaHei",sans-serif}main{max-width:1500px;margin:auto;padding:34px 24px}h1{font-size:32px;margin:4px 0 12px}h2{font-size:22px;margin:30px 0 12px}p{max-width:1100px;color:#afc2d1}a{color:#90d8ce}code{font:11px/1.5 Consolas,monospace;overflow-wrap:anywhere}td>code,small{display:block}small{color:#8da8bc;font-size:11px;margin-top:6px}header>small{letter-spacing:1.5px;color:#79c7c3}.metrics{display:flex;flex-wrap:wrap;gap:12px;margin:24px 0}.metric{background:#1a2938;padding:15px 24px;border:1px solid #385065;border-radius:10px}.metric b{display:block;font-size:28px;color:#d7efe4}.metric span{color:#9bb4c5;font-size:11px}.controls{position:sticky;top:0;padding:14px;background:#101721ee;backdrop-filter:blur(8px);display:flex;gap:12px;align-items:center;z-index:2;flex-wrap:wrap}input,select{background:#1d2d3c;color:#e3eef1;border:1px solid #466376;border-radius:7px;padding:10px}input{flex:1;min-width:230px}select{max-width:330px}table{border-collapse:collapse;width:100%;background:#152231}th,td{padding:14px 16px;text-align:left;border-bottom:1px solid #334b60;vertical-align:top}th{font-size:12px;color:#89cfca;background:#1e3243}td:nth-child(1){width:19%}td:nth-child(2){width:20%}td:nth-child(3){width:21%}td:nth-child(4){width:40%}td strong{display:block;font-size:15px;color:#ebf2f5}details{margin-top:10px;color:#adbfcd;font-size:11px}summary{cursor:pointer;color:#86d6c9}details p{font-size:11px}li{margin:5px 0}h4{margin:14px 0 5px;color:#a4d5d0}pre{white-space:pre-wrap;word-break:break-word;background:#1a2938;padding:16px;color:#c3d2df;font-size:11px}.good{color:#97d9b5}.samples{display:flex;flex-wrap:wrap;gap:15px}.coverage{font-size:12px}footer{color:#728d9e;font-size:11px;padding:24px 0}@media(max-width:850px){main{padding:20px 10px}table{font-size:11px}th,td{padding:10px 8px}td:nth-child(n){width:auto}.controls{position:static}h1{font-size:26px}}
    .texture-preview,.texture-gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(76px,1fr));gap:12px;margin-top:12px}.texture-gallery{grid-template-columns:repeat(auto-fill,minmax(125px,1fr));max-height:650px;overflow:auto;padding:8px;background:#101b27}.texture-card{margin:0;min-width:0;text-align:center}.texture-open{border:1px solid #527187;background:#142330;padding:4px;border-radius:6px;cursor:zoom-in;display:block;width:100%}.texture-swatch{height:68px;display:flex;align-items:center;justify-content:center;overflow:hidden;background-color:#8a929c;background-image:conic-gradient(#bcc2c9 25%,transparent 0 50%,#bcc2c9 0 75%,transparent 0);background-size:16px 16px}.texture-window{display:block;position:relative;overflow:hidden;flex:none}.texture-window img{position:absolute;max-width:none;image-rendering:pixelated}.texture-card figcaption code{display:block;font-size:10px;line-height:1.4;margin-top:5px}.texture-card.compact figcaption code{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.texture-card small{font-size:9px;line-height:1.35;margin-top:3px}.texture-role{font-size:9px;color:#91c7bc}.atlas-role{color:#efc580}.texture-open:hover,.texture-open:focus-visible{outline:2px solid #88d8ca}dialog{width:min(900px,94vw);max-height:92vh;padding:20px;background:#172737;color:#e2edf1;border:1px solid #749aae;border-radius:12px}dialog::backdrop{background:#050b13cc}dialog h3{font-size:16px;overflow-wrap:anywhere;margin:0 50px 8px 0}dialog p{font-size:12px;margin:10px 0}.viewer-close{position:absolute;right:12px;top:10px}.viewer-tools{display:flex;gap:10px;align-items:center;margin:12px 0}.viewer-tools button,.viewer-close{border:1px solid #607e94;background:#273e50;color:#e1eef3;border-radius:5px;padding:7px 12px;cursor:pointer}.viewer-stage{overflow:auto;max-height:62vh;background-color:#8a929c;background-image:conic-gradient(#bcc2c9 25%,transparent 0 50%,#bcc2c9 0 75%,transparent 0);background-size:24px 24px;padding:16px}.viewer-stage .texture-window{margin:auto}.viewer-tools a{font-size:12px}
    .core-toggle{display:inline-flex;gap:7px;align-items:center;color:#d0dfe6;font-size:12px;white-space:nowrap;cursor:pointer}.core-toggle input{flex:none;min-width:0;width:auto;margin:0;accent-color:#8bd4c8}
    </style><main><header><small>GUOGAOLOGY · BLOCK TEXTURE / MODEL / UV AUDIT</small><h1>全部方块的源纹理尺寸与实际采样</h1>'''
    text+=f'<p>当前模组 {esc(report["metadata"]["mod_version"])}；自有资源四版共享，原版继承依赖按本机 Minecraft 26.2 JAR 实际读取。覆盖全部 {summary["blocks"]} 个方块状态文件、variants/multipart、层层 parent 和纹理别名。只审计方块，不包含物品图标。</p></header><div class="metrics">{cards}</div>'
    text+='<p><b>如何读表：</b>16/32/64表示PNG原生尺寸；一张512颜色图集不等于每面512像素，32×512动画长图也不等于单帧512像素。有效密度按UV像素跨度÷模型原始边长计算为 texel/格（含原版元素rescale，动画缩放另计）；小装饰可整张贴图缩到一小面，所以数值可能很高。曲面、细分网格、平滑法线与像素纹理可以共存，不能仅由PNG大小判断视觉风格。</p>'
    text+='<p><b>证据边界：</b>本页为资源与源代码审计，不使用游戏截图估算屏幕像素，不假设资源包/Shader效果。未有生成源或视觉审查证据的风格会标为待确认。每个方块的所有源纹理与模型可展开，完整状态条件、parent链及UV分布在 <a href="block-pixels.json">机器JSON</a> 中。</p>'
    text+='<p><b>看贴图：</b>每行先展示最多6张代表图，优先世界表面用图；展开后可看全部变体贴图。点击图片可在页内放大，透明处显示棋盘底，均以最近邻方式显示。动画缩略图只显示首帧，可切换原始长图；LOD图集单独标记。原图未叠加模型顶点色、游戏光照或几何效果。</p>'
    special=report['special_cases'];by_id={b['id']:b for b in report['blocks']}
    def names(identifiers):
        return '<br>'.join(esc(by_id[value]['name_zh'])+' <code>'+esc(value)+'</code>' for value in identifiers if value in by_id)
    smooth=[m['block_id'] for m in special.get('meshes',[]) if 'interpolated_vertex_normals' in m.get('labels',[])]
    colored=[m['block_id'] for m in special.get('meshes',[]) if 'interpolated_vertex_color' in m.get('labels',[])]
    filtered=[m['block_id'] for m in special.get('meshes',[]) if 'effective_bilinear_lod_downsample' in m.get('labels',[])]
    gradient=[b['id'] for b in report['blocks'] if '连续渐变' in b['visual_style']]
    resampled=[b['id'] for b in report['blocks'] if '平滑重采样' in b['visual_style']]
    extra_rows=[]
    for title,ids,description in (
        ('连续渐变与像素表情混合',gradient,'128px表情/搪瓷底色有密集的连续冷暖渐变；表情笔画仍离散。包括六色表情灯、塑形台的小预览面与三阶果糕之心。'),
        ('明确平滑重采样材质',resampled,'d_optical_clear_glass原生绘图128px，再以LANCZOS缩至64px；已重建逐RGBA字节比对。其余像素/纯色纹理不因此变成平滑。'),
        ('平滑曲面顶点法线',smooth,'同一面不同顶点有不同法线，曲面明暗会插值。这是几何/着色，不是纹理变成无像素。'),
        ('顶点色插值',colored,'20个自定义晶核含连续变化的顶点色，叠加在实际源纹理上。'),
        ('LOD实际双线性缩图',filtered,'静态后备中有13款使用的源纹理缩成16×16时BILINEAR与NEAREST结果不同。512图集中的369个使用图块已逐字节验证一致，近处世界模型仍使用原图。'),
    ):
        extra_rows.append('<tr><td>'+esc(title)+'<small>'+str(len(ids))+'个方块ID</small></td><td>'+esc(description)+'</td><td colspan="2"><details><summary>展开对应方块</summary>'+names(ids)+'</details></td></tr>')
    text+='<h2>需要分开看的特殊材质与几何</h2><p>这里列出有证据的平滑或混合因素，避免把所有64/128纹理或所有晶核一概称为“非像素”。另有3个传送门使用32×32动画帧，4个告示牌附加原版字体文字，部分灯/纤维在演奏时叠加无纹理颜色薄层；这些已在对应方块备注中标明。</p><table><tbody>'+''.join(extra_rows)+'</tbody></table>'
    text+='<h2>完整方块表</h2><div class="controls"><input id="query" placeholder="搜索中文名、ID、纹理、模型或备注"><select id="category"><option value="">全部尺寸类型</option>'+options+'</select><select id="style"><option value="">全部风格</option>'+styles+'</select><label class="core-toggle"><input type="checkbox" id="hide-cores">隐藏九类晶核</label><span id="count"></span></div><p>“隐藏九类晶核”仅排除八系Lv1–3与三阶序数晶核，共27个方块；普通无等级序数晶体、fffZ/FOS和TREE材料保留。</p><table id="block-table"><thead><tr><th>方块 / 注册ID</th><th>实际PNG尺寸与数量</th><th>风格与几何</th><th>实际UV采样、密度及备注</th></tr></thead><tbody>'+''.join(rows)+'</tbody></table>'
    text+='<h2>覆盖率与未确认项</h2><pre class="coverage">'+esc(json.dumps(coverage,ensure_ascii=False,indent=2))+'</pre>'+unresolved+'<h2>源纹理抽样联系表</h2><p>全部关联源纹理的首帧，最近邻显示；图集缩略图仅便于定位，UV区域统计以原始数据为准。表格编号与具体纹理ID见 texture-samples/index.json。</p><div class="samples">'+samples+'</div><h2>统计明细</h2><pre>'+esc(json.dumps(summary,ensure_ascii=False,indent=2))+'</pre><footer>生成时间 '+esc(report['metadata']['generated_utc'])+' · 可维护脚本 Guogaology/tools/audit_block_pixels.py · 生产资源保持只读。</footer></main>'
    text+='''<dialog id="texture-viewer" aria-labelledby="texture-title"><button type="button" id="texture-close" class="viewer-close" aria-label="关闭贴图放大">关闭 ×</button><h3 id="texture-title"></h3><p id="texture-meta"></p><div class="viewer-tools"><button type="button" id="texture-frame">显示首帧</button><button type="button" id="texture-original">显示原始长图</button><a id="texture-file" target="_blank" rel="noopener">打开原PNG</a></div><div class="viewer-stage" id="texture-stage"><div class="texture-window" id="texture-window"><img id="texture-large" alt=""></div></div><p>实际源PNG，最近邻放大；不含模型顶点色、光照、透明混合或曲面着色。Esc或点击窗口外可关闭。</p></dialog>'''
    catalog={name:{'url':info['exported_image'],'png':info['png_size'],'rect':info['preview_rect'],
                   'animated':info['animation'] is not None,'label':resolution_label(info),
                   'atlas':name==TINT_ATLAS} for name,info in report['textures'].items()}
    text+='<script>const textureCatalog='+json.dumps(catalog,ensure_ascii=False).replace('<','\\u003c')+';'
    text+='''const q=document.querySelector('#query'),c=document.querySelector('#category'),s=document.querySelector('#style'),h=document.querySelector('#hide-cores'),rows=[...document.querySelectorAll('#block-table tbody tr')];function filter(){let n=0;const value=q.value.toLowerCase().trim();for(const r of rows){const match=(!value||r.dataset.search.toLowerCase().includes(value))&&(!c.value||r.dataset.category.split(' ').includes(c.value))&&(!s.value||r.dataset.style===s.value)&&(!h.checked||r.dataset.core!=='1');r.hidden=!match;if(match)n++}document.querySelector('#count').textContent=n+' / '+rows.length+' 方块'}[q,c,s,h].forEach(e=>e.addEventListener('input',filter));filter();
    const viewer=document.querySelector('#texture-viewer'),large=document.querySelector('#texture-large'),crop=document.querySelector('#texture-window'),stage=document.querySelector('#texture-stage'),frameButton=document.querySelector('#texture-frame'),originalButton=document.querySelector('#texture-original');let selectedTexture=null;
    function showTextureMode(original){const t=textureCatalog[selectedTexture];if(!t)return;const r=original?[0,0,...t.png]:t.rect;const scale=original&&t.animated?4:Math.min(16,Math.max(1,Math.floor(384/Math.max(r[2],r[3]))));crop.style.width=r[2]*scale+'px';crop.style.height=r[3]*scale+'px';large.style.width=t.png[0]*scale+'px';large.style.height=t.png[1]*scale+'px';large.style.left=-r[0]*scale+'px';large.style.top=-r[1]*scale+'px';large.src=t.url;large.alt=selectedTexture;document.querySelector('#texture-meta').textContent=t.label+(t.atlas?' · LOD颜色图集，内容图块16×16':t.animated?(original?' · 当前显示原始动画长图，可滚动查看':' · 当前显示首帧，可切换原始长图'):' · 静态源纹理');stage.scrollTop=0;stage.scrollLeft=0;frameButton.disabled=!original;originalButton.disabled=original;}
    function openTexture(identifier){if(!textureCatalog[identifier])return;selectedTexture=identifier;const t=textureCatalog[identifier];document.querySelector('#texture-title').textContent=identifier;document.querySelector('#texture-meta').textContent=t.label+(t.atlas?' · LOD颜色图集，内容图块16×16':t.animated?' · 当前显示首帧，可切换原始长图':' · 静态源纹理');document.querySelector('#texture-file').href=t.url;frameButton.hidden=!t.animated;originalButton.hidden=!t.animated;showTextureMode(false);viewer.showModal();}
    document.addEventListener('click',event=>{const button=event.target.closest('.texture-open');if(button)openTexture(button.dataset.texture)});document.querySelector('#texture-close').addEventListener('click',()=>viewer.close());viewer.addEventListener('click',event=>{if(event.target!==viewer)return;const r=viewer.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)viewer.close()});frameButton.addEventListener('click',()=>showTextureMode(false));originalButton.addEventListener('click',()=>showTextureMode(true));
    </script></html>'''
    (out/'方块像素审计.html').write_text(text,'utf8')


if __name__=='__main__':
    main()
