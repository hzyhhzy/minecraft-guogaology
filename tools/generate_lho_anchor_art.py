"""Visible cyan/gold stabilization for the four natural LHO fragment families."""
from pathlib import Path
import json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/guogaology'
ROOTS=('fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z')

def generate():
    for name in ROOTS:
        model_path=ASSETS/'models/block'/f'{name}.json'
        data=json.loads(model_path.read_text('utf8'))
        textures={}
        for key,value in data['textures'].items():
            if not value.startswith('guogaology:'):continue
            source=ASSETS/'textures'/(value.split(':')[1]+'.png')
            target=source.with_name(source.stem+'_anchored.png')
            if not source.exists():raise FileNotFoundError(source)
            im=Image.open(source).convert('RGBA')
            pixels=[]
            for r,g,b,a in im.getdata():
                brightness=(r+g+b)/765
                # Keep original discontinuous alpha geometry, but make the real
                # anchor field visibly teal instead of the cold ghost palette.
                pixels.append((round(30+brightness*90),round(130+brightness*120),round(110+brightness*130),min(240,round(a*1.35))))
            im.putdata(pixels);im.save(target)
            textures[key]=value+'_anchored'
        data['textures'].update(textures)
        (ASSETS/'models/block'/f'{name}_anchored.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf8')
        states={'variants':{'anchored=false':{'model':f'guogaology:block/{name}'},'anchored=true':{'model':f'guogaology:block/{name}_anchored'}}}
        (ASSETS/'blockstates'/f'{name}.json').write_text(json.dumps(states,indent=2)+'\n','utf8')

if __name__=='__main__':generate()
