"""Small native item-model silhouettes, sharing vanilla textures across all targets."""
THEMES=('matrix','power','hydra','absence','weaver','astra','frontier','guogao')

def models():
    result={}
    for theme in THEMES:
        boxes=[]
        def b(a,c,texture):
            boxes.append({'from':a,'to':c,'faces':{d:{'texture':'#'+texture} for d in ('north','south','east','west','up','down')}})
        textures={'base':'minecraft:block/smooth_stone','pale':'minecraft:block/quartz_block_bottom',
                  'blue':'minecraft:block/light_blue_concrete','dark':'minecraft:block/deepslate_tiles',
                  'wood':'minecraft:block/dark_oak_planks','roof':'minecraft:block/orange_terracotta',
                  'green':'minecraft:block/green_concrete','mint':'minecraft:block/cyan_concrete',
                  'glass':'minecraft:block/light_blue_stained_glass','gold':'minecraft:block/gold_block'}
        if theme=='matrix':
            b([1,0,1],[15,2,15],'base')
            for x,h in [(2,8),(5,12),(8,9),(11,14)]:
                b([x,2,4],[x+3,h,12],'blue');b([x-.3,h,3.7],[x+3.3,h+1,12.3],'pale')
            b([6,2,11],[10,5,14],'dark')
        elif theme=='power':
            b([5,0,5],[11,14,11],'wood')
            for y,r in [(1,7),(4,6),(7,5),(10,4),(13,3)]:
                b([8-r,y,8-r],[8+r,y+1,8+r],'roof')
            b([7,14,7],[9,16,9],'gold')
        elif theme=='hydra':
            b([1,0,1],[15,2,15],'base')
            for x,z in [(3,4),(7,2),(11,4),(3,10),(7,12),(11,10)]:
                b([x,2,z],[x+2,9,z+2],'wood');b([x-1,8,z-1],[x+3,11,z+3],'green')
            b([4,10,4],[12,12,12],'green');b([7,12,7],[9,15,9],'gold')
        elif theme=='absence':
            for x,z,y in [(1,1,3),(10,1,6),(10,10,4),(1,10,7)]:
                b([x,y,z],[x+5,y+1,z+5],'pale');b([x,y+1,z],[x+1,y+6,z+1],'glass')
            b([5,6,2],[11,7,4],'glass');b([2,7,5],[4,8,11],'glass')
        elif theme=='weaver':
            b([1,0,1],[15,2,15],'base')
            for y,r in [(2,7),(5,5),(8,3),(11,1)]:b([8-r,y,5],[8+r,y+3,11],'mint')
            b([5,2,11],[6,8,12],'pale');b([10,2,11],[11,8,12],'pale');b([7,3,11],[9,6,12],'dark')
        elif theme=='astra':
            b([1,0,1],[15,2,15],'pale')
            for x in (2,10):
                b([x,2,3],[x+4,13,12],'pale')
                for y in (4,8,12):b([x-.1,y,2.8],[x+4.1,y+1,3.1],'mint')
            b([6,3,5],[10,5,12],'mint');b([7,5,6],[9,10,10],'dark')
        elif theme=='frontier':
            b([1,0,1],[15,2,15],'pale')
            for x in (2,11):
                b([x,2,3],[x+3,13,12],'dark');b([x-.3,13,2.7],[x+3.3,14,12.3],'pale')
            b([5,3,5],[11,5,12],'green');b([6,6,7],[10,9,10],'gold')
        else:
            b([7,0,7],[9,13,9],'wood')
            for y,r in [(1,7),(4,6),(7,4),(10,2)]:b([8-r,y,8-r],[8+r,y+2,8+r],'green')
            b([6,13,6],[10,16,10],'gold');b([2,3,7],[4,5,9],'roof');b([11,6,7],[13,8,9],'roof')
        textures['particle']=textures['base']
        result['landmark_emblem_'+theme]={'parent':'minecraft:block/block','textures':textures,'elements':boxes,'gui_light':'front',
            'display':{'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.75,.75,.75]}}}
    return result

if __name__=='__main__':
    import json
    from pathlib import Path
    folder=Path(__file__).resolve().parents[1]/'src/main/resources/assets/guogaology/models/item'
    for name,value in models().items():(folder/(name+'.json')).write_text(json.dumps(value,indent=2)+'\n',encoding='utf8')
