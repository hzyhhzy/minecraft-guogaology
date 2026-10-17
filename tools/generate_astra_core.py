"""Restore the original Astra Lv1 and its three inventory-grade illustrations.

The authoritative source is the exact 0.5.11 mesh/model, before the 0.5.12
conductor-and-chip redesign. Higher-grade world geometry is independent and
is never written here. The existing core body/glass/clasp materials are kept.
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A = ROOT/'src/main/resources/assets/guogaology'
NAME = 'astra_critical_core'
REFERENCE = ROOT/'tools/reference/astra-lv1.json'


def write(path,value):
    path.write_bytes((json.dumps(value,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf8'))


def generate():
    source=json.loads(REFERENCE.read_text('utf8'))
    mesh=copy.deepcopy(source['mesh'])
    write(A/f'core_meshes/{NAME}.json',mesh)
    write(A/f'models/block/{NAME}.json',source['block_model'])
    manifest=ROOT/'src/main/resources/guogaology/core_design_sources.json'
    sources=json.loads(manifest.read_text('utf8'))
    for entry in sources:
        if entry['id']==NAME:
            entry['revision_source']='tools/reference/astra-lv1.json; exact 2769de8 pre-0.5.12 mesh and materials'
    write(manifest,sources)
    grades=ROOT/'src/main/resources/guogaology/core_grades.json'
    inventory=json.loads(grades.read_text('utf8'))
    for entry in inventory:
        if entry['id']==NAME:
            entry.update(quads=len(mesh['quads']),dynamic_quads=sum(q.get('part',0)>0 for q in mesh['quads']),
                         motion=mesh.get('motion','still'),extended=mesh.get('extended',False))
    write(grades,inventory)
    import core_item_projection
    core_item_projection.mesh.cache_clear();core_item_projection.project.cache_clear()
    from generate_inventory_art import core_icon
    for grade in (1,2,3):
        suffix='' if grade==1 else f'_lv{grade}'
        core_icon(NAME,grade).save(A/f'textures/item/{NAME}{suffix}.png')
    # Only remove these two rejected-design textures once no current resource
    # uses them; leave all other families and retained materials alone.
    for name in ('astra_lead','astra_chip'):
        resource='guogaology:block/core_d/'+name
        if not any(resource in path.read_text('utf8') for path in A.rglob('*.json')):
            path=A/f'textures/block/core_d/{name}.png'
            if path.exists():path.unlink()
    print(f'ASTRA_CORE_0515_OK original_quads={len(mesh["quads"])} icons=3 source=2769de8 higher_world_meshes=unchanged')


if __name__=='__main__':generate()
