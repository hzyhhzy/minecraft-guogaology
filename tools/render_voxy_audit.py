"""Contact sheet from the installed Voxy baker's captured six-face RGBA output."""
import argparse,json,math
from PIL import Image,ImageDraw,ImageFont

def render(source,destination):
    entries=[b for b in json.loads(source.read_text('utf8'))['bakes'] if 'rgba' in b]
    rows=math.ceil(len(entries)/2);font=ImageFont.load_default(size=13)
    image=Image.new('RGB',(1190,rows*70+8),(24,30,40));draw=ImageDraw.Draw(image)
    for index,block in enumerate(entries):
        x=index//rows*595;y=index%rows*70
        draw.text((x+8,y+3),block['id'].split(':')[1],font=font,fill=(223,237,249))
        for face,colors in enumerate(block['rgba']):
            tile=Image.new('RGBA',(16,16));tile.putdata([(c&255,(c>>8)&255,(c>>16)&255,c>>24) for c in colors])
            tile=tile.resize((40,40),Image.Resampling.NEAREST);background=Image.new('RGBA',(40,40),(70,78,89,255));background.alpha_composite(tile)
            image.paste(background,(x+8+face*94,y+24));draw.text((x+50+face*94,y+40),str(block['face_pixels'][face]),font=font,fill=(152,178,199))
    image.save(destination)

if __name__=='__main__':
    from pathlib import Path
    parser=argparse.ArgumentParser();parser.add_argument('source',type=Path);parser.add_argument('destination',type=Path);args=parser.parse_args();render(args.source,args.destination)
