"""Derive matching plain confection models from the embossed block's body.

Keep the shared frosting sprites, inset shape, lighting and harvesting profile.
No texture painting is needed: the emoji is separate model geometry.
"""
import copy,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/googology'
DATA=RES/'data/googology'
COLORS=('amber','berry','lime','azure')
def write(p,v):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def main():
    balance_path=RES/'googology/block_balance.json'
    balance=json.loads(balance_path.read_text('utf8'))
    for color in COLORS:
        old=color+'_guogao';name='plain_'+old
        model=json.loads((ASSETS/f'models/block/{old}.json').read_text('utf8'))
        model['elements']=model['elements'][:1]
        model['textures']={k:v for k,v in model['textures'].items() if k in ('body','particle')}
        write(ASSETS/f'models/block/{name}.json',model)
        write(ASSETS/f'models/item/{name}.json',{'parent':'googology:block/'+name})
        write(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'googology:block/'+name}}})
        loot=(DATA/f'loot_table/blocks/{old}.json').read_text('utf8').replace('googology:'+old,'googology:'+name)
        (DATA/f'loot_table/blocks/{name}.json').write_text(loot,encoding='utf8')
        balance[name]=copy.deepcopy(balance[old])
    write(balance_path,balance)
    tag_path=DATA/'tags/item/fruit_guogao.json';tag=json.loads(tag_path.read_text('utf8'))
    for color in COLORS:
        value='googology:plain_'+color+'_guogao'
        if value not in tag['values']:tag['values'].append(value)
    write(tag_path,tag)
if __name__=='__main__':main()
