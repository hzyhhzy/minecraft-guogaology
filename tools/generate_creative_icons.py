"""Seven presentation-only creative-tab sprites on a native 32px grid.

No world art, ordinary item sprites, recipes or loot are touched. Modern item
definitions are supplied by prepare_port_resources.convert_items as usual.
"""
from io import BytesIO
from pathlib import Path
import argparse
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/googology'
TABS = ('googology', 'architecture', 'numbers', 'lighting', 'cores', 'mining', 'materials')
LABELS = ('Terrain', 'Building', 'Numbers', 'Lights', 'Cores', 'Equipment', 'Materials')
INK = '#182739'
DIGITS = ('111101101101111', '010110010010111', '111001111100111', '111001111001111')


def polygon(draw, points, fill, outline=True):
    draw.polygon(points, fill=fill)
    if outline:
        draw.line(points+[points[0]], fill=INK, width=2)


def sprite(tab):
    image = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(image)
    if tab == 'googology':
        polygon(d, [(3,14),(16,8),(29,14),(29,24),(16,30),(3,24)], '#79503f')
        d.polygon([(16,19),(28,14),(28,23),(16,29)], fill='#503c38')
        polygon(d, [(3,14),(16,8),(29,14),(16,21)], '#62c865')
        d.polygon([(6,14),(16,10),(24,14),(16,17)], fill='#b3ed78')
        d.rectangle((6,20,9,22), fill='#bb8760')
        d.rectangle((11,24,13,26), fill='#bb8760')
        d.rectangle((22,22,25,24), fill='#9b7052')
        d.line((16,15,16,5), fill=INK, width=4)
        d.line((16,15,16,5), fill='#d2f29e', width=2)
        polygon(d, [(15,10),(9,9),(6,5),(12,4),(16,7)], '#42a950')
        polygon(d, [(16,8),(18,3),(25,2),(24,7),(19,10)], '#86df65')
        d.line((18,7,22,4), fill='#d3ffa0', width=2)
    elif tab == 'architecture':
        polygon(d, [(2,10),(16,2),(30,10),(28,13),(4,13)], '#ca955b')
        d.polygon([(7,9),(16,4),(25,9)], fill='#ffe6ac')
        d.rectangle((5,12,27,26), fill='#4c667e')
        d.rectangle((13,15,19,27), fill=INK)
        d.rectangle((14,17,17,25), fill='#84b9cd')
        for x in (5,23):
            d.rectangle((x-1,12,x+4,27), fill=INK)
            d.rectangle((x,13,x+3,26), fill='#d3c9b3')
            d.rectangle((x,13,x+1,25), fill='#fff3d5')
            d.rectangle((x-1,12,x+4,14), fill='#f1ce87')
        d.rectangle((3,26,29,28), fill=INK)
        d.rectangle((4,26,28,27), fill='#d3c9b3')
        d.rectangle((1,29,31,30), fill='#8396a0')
    elif tab == 'numbers':
        for x in (1,27):
            d.rectangle((x,2,x+3,29), fill=INK)
            d.rectangle((x+1,3,x+2,28), fill='#91c7e5')
        for y in (2,27):
            d.rectangle((2,y,6,y+2), fill='#91c7e5')
            d.rectangle((26,y,29,y+2), fill='#91c7e5')
        for value, glyph in enumerate(DIGITS):
            x0,y0=8+(value%2)*10,4+(value//2)*14
            for n,pixel in enumerate(glyph):
                if pixel=='1':
                    x,y=x0+n%3*2,y0+n//3*2
                    d.rectangle((x-1,y-1,x+2,y+2),fill=INK)
            for n,pixel in enumerate(glyph):
                if pixel=='1':
                    x,y=x0+n%3*2,y0+n//3*2
                    d.rectangle((x,y,x+1,y+1),fill='#7fe2ec' if value<2 else '#e8d976')
    elif tab == 'lighting':
        d.line([(2,3),(2,7),(7,9),(16,13),(25,8),(29,4)],fill=INK,width=4)
        d.line([(2,3),(2,7),(7,9),(16,13),(25,8),(29,4)],fill='#b6c8bb',width=2)
        for x,y,color,light in ((6,10,'#ecb337','#fff1a2'),(16,17,'#d7569e','#ffc1dc'),(26,9,'#37bacb','#a6fbef')):
            d.rectangle((x-2,y-3,x+1,y),fill=INK)
            polygon(d,[(x-4,y),(x+3,y),(x+4,y+7),(x+1,y+10),(x-2,y+10),(x-5,y+7)],color)
            d.rectangle((x-2,y+2,x,y+6),fill=light)
    elif tab == 'cores':
        polygon(d,[(3,13),(8,8),(13,15),(12,25),(7,29),(2,23)],'#9166cd')
        d.polygon([(4,14),(8,10),(8,25),(4,22)],fill='#dcb2fb')
        polygon(d,[(23,11),(28,14),(30,23),(24,28),(19,23)],'#438dba')
        d.polygon([(24,13),(27,16),(26,24),(23,26)],fill='#9ce0ef')
        polygon(d,[(16,1),(23,11),(22,24),(16,30),(9,24),(9,11)],'#45bebb')
        d.polygon([(16,3),(16,26),(11,22),(11,12)],fill='#b4ffdf')
        d.polygon([(16,3),(21,12),(16,16)],fill='#eefff7')
        d.polygon([(17,17),(21,13),(20,23),(17,27)],fill='#299389')
    elif tab == 'mining':
        d.line((5,28,22,9),fill=INK,width=6)
        d.line((5,27,22,8),fill='#c49053',width=3)
        polygon(d,[(11,2),(21,3),(30,12),(27,18),(24,11),(18,7),(11,6)],'#42bdd0')
        d.line((13,3,20,4,27,11),fill='#b6fff1',width=2)
        polygon(d,[(2,2),(9,4),(24,19),(19,24),(4,9)],'#8caabd')
        d.polygon([(4,4),(8,5),(21,19),(19,21),(6,8)],fill='#effcff')
        d.line((21,22,28,29),fill=INK,width=6)
        d.line((21,22,27,28),fill='#cb9550',width=3)
        d.line((18,25,25,18),fill=INK,width=6)
        d.line((18,24,24,18),fill='#f9da83',width=3)
    elif tab == 'materials':
        polygon(d,[(6,3),(21,3),(27,9),(25,16),(4,16),(1,10)],'#3989ac')
        d.polygon([(6,5),(20,5),(24,9),(4,9)],fill='#a6eaf2')
        d.polygon([(4,11),(24,11),(23,14),(5,14)],fill='#58b4cf')
        polygon(d,[(10,16),(25,16),(31,22),(28,29),(8,29),(4,23)],'#b47a34')
        d.polygon([(10,18),(24,18),(28,22),(8,22)],fill='#ffe29a')
        d.polygon([(8,24),(28,24),(26,27),(9,27)],fill='#e5b75c')
        d.rectangle((12,18,20,19),fill='#fff4cb')
    else:
        raise ValueError(tab)
    assert image.getbbox() and len(image.getcolors(1024))<32
    return image


def encoded(value):
    return (json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf8')


def generate(root=ROOT, *, check=False):
    root=Path(root);assets=root/'src/main/resources/assets/googology'
    expected={};images={}
    for tab in TABS:
        name='creative_icon_'+tab;image=sprite(tab);images[tab]=image
        buffer=BytesIO();image.save(buffer,format='PNG')
        expected[assets/f'textures/item/{name}.png']=buffer.getvalue()
        expected[assets/f'models/item/{name}.json']=encoded({'parent':'minecraft:item/generated','textures':{'layer0':'googology:item/'+name}})
    catalog_path=root/'tools/copy_catalog.json'
    catalog=json.loads(catalog_path.read_text('utf8'))
    for locale in ('zh_cn','en_us'):
        values=catalog['translations'][locale]
        for tab in TABS:
            values['item.googology.creative_icon_'+tab]=values['itemGroup.googology.'+tab]
        lang_path=assets/f'lang/{locale}.json';lang=json.loads(lang_path.read_text('utf8'))
        lang.update({key:value for key,value in values.items() if key.startswith('item.googology.creative_icon_')})
        expected[lang_path]=encoded(lang)
    expected[catalog_path]=encoded(catalog)
    changed=[]
    for path,data in expected.items():
        if path.exists() and path.read_bytes()==data:continue
        changed.append(path)
        if not check:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    if not check:
        out=root/'build/art-0404-creative';out.mkdir(parents=True,exist_ok=True)
        sheet=Image.new('RGB',(7*152,316),'#202631');d=ImageDraw.Draw(sheet)
        d.text((12,10),'Creative tabs — 32px native art / 16px slots / light and dark backgrounds',fill='#ecf2f5')
        for index,(tab,label) in enumerate(zip(TABS,LABELS)):
            x=index*152+12;image=images[tab]
            d.text((x,34),label,fill='#d7e3ed')
            big=image.resize((128,128),Image.Resampling.NEAREST);sheet.paste(big,(x,56),big)
            d.text((x,194),'32px',fill='#a8bdc8');sheet.paste(image,(x+52,188),image)
            small=image.resize((16,16),Image.Resampling.NEAREST)
            for y,color in ((230,'#c6c6c6'),(273,'#303745')):
                d.rectangle((x,y,x+127,y+31),fill=color)
                for offset,size in ((8,16),(54,32)):
                    sample=small.resize((size,size),Image.Resampling.NEAREST)
                    sheet.paste(sample,(x+offset,y+(32-size)//2),sample)
        sheet.save(out/'creative-tabs.png')
        light_sheet=Image.new('RGB',(7*152,208),'#c6c6c6');ld=ImageDraw.Draw(light_sheet)
        for index,(tab,label) in enumerate(zip(TABS,LABELS)):
            x=index*152+12;image=images[tab]
            ld.text((x,10),label,fill='#30343b')
            sample=image.resize((128,128),Image.Resampling.NEAREST)
            light_sheet.paste(sample,(x,32),sample)
            light_sheet.paste(image,(x+12,170),image)
            tiny=image.resize((16,16),Image.Resampling.NEAREST)
            light_sheet.paste(tiny,(x+76,178),tiny)
        light_sheet.save(out/'creative-tabs-light.png')
    print(f'CREATIVE_ICONS_044 icons=7 native=32px files_changed={len(changed)} check={check}')
    return changed


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args()
    if generate(check=args.check) and args.check:raise SystemExit(1)
