"""Give each custom core an actual vanilla particle slot, also used after LOD generation."""
from pathlib import Path
import argparse
import json

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/googology'
PARTICLES={
    'sequence_core':'minecraft:block/light_blue_stained_glass',
    'power_tower_core':'googology:block/core_d/power_tower_core',
    'hydra_bud':'googology:block/core_d/hydra_bud',
    'lho_trace':'googology:block/lho_c1',
    'laver_core':'googology:block/core_d/laver_core',
    'astra_critical_core':'googology:block/core_d/astra_critical_core',
    'boundary_core':'googology:block/core_d/boundary_core_outer',
    'guogao_heart':'googology:block/core_d/guogao_lantern_enamel',
    'ordinal_crystal':'googology:block/core_d/ordinal_crystal',
}
def particle_texture(name):
    return PARTICLES[name.split('_lv')[0]]

def generate(check=False):
    changed=[]
    for mesh in sorted((A/'core_meshes').glob('*.json')):
        if mesh.stem.split('_lv')[0] not in PARTICLES:continue
        for path in [mesh,A/'models/block'/mesh.name]:
            data=json.loads(path.read_text('utf8'));texture=particle_texture(mesh.stem)
            if data['textures'].get('particle')==texture:continue
            changed.append(path);data['textures']['particle']=texture
            if not check:path.write_text(json.dumps(data,ensure_ascii=False,separators=(',',':'))+'\n','utf8')
    print(f'Core particle bindings: {len(changed)} '+('outdated' if check else 'repaired'))
    return changed

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    if generate(args.check) and args.check:raise SystemExit(1)
