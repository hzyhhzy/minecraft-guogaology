"""The 64-pixel native logo: a reference-proportioned Guogao face and capital Omega.

All geometry lives on the same integer grid. SVG and PNG share the exact pixels;
nearest-neighbour exports preserve the deliberate steps without smooth edges.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def run():
    im = Image.new('RGBA', (64, 64))
    d = ImageDraw.Draw(im)
    # Indigo / electric-blue background, without additional symbols or scenery.
    mask=Image.new('1',im.size)
    ImageDraw.Draw(mask).polygon([(7,0),(56,0),(56,1),(59,1),(59,3),(61,3),(61,5),
               (63,5),(63,58),(61,58),(61,61),(58,61),(58,63),
               (5,63),(5,61),(2,61),(2,58),(0,58),(0,7),
               (1,7),(1,4),(3,4),(3,2),(7,2)],fill=1)
    bg=['#11132f','#15183d','#191e4a','#1c2657','#1b2e61','#172957','#122347','#101c3d']
    for y in range(64):
        for x in range(64):
            if mask.getpixel((x,y)):
                im.putpixel((x,y),(*bytes.fromhex(bg[y//8][1:]),255))
    # A literal capital Ω: arched upper loop and separated, broad baseline feet.
    omega=[(7,56),(24,56),(24,51),(20,49),(15,44),(11,37),(10,30),
           (11,22),(15,15),(21,10),(28,8),(36,8),(43,10),(49,15),
           (53,22),(54,30),(53,37),(49,44),(44,49),(40,51),(40,56),(57,56)]
    d.line(omega,fill='#326eab',width=7)
    d.line(omega,fill='#72e8f1',width=4)
    d.line([(15,15),(21,10),(28,8),(36,8),(43,10),(49,15)],fill='#c7fbff',width=2)
    background = im
    im = Image.new('RGBA', (64,64))
    d = ImageDraw.Draw(im)
    # Discrete blue-to-gold bands, not a blurred or dithered gradient.
    face = Image.new('1',im.size)
    fd = ImageDraw.Draw(face)
    fd.ellipse((15,15,49,49),fill=1)
    d.ellipse((14,14,50,50),fill='#ffeaa1')
    bands=[(15,21,'#619ce9'),(22,24,'#88bee8'),(25,26,'#acd0d9'),
           (27,28,'#d1d598'),(29,36,'#f8d563'),(37,42,'#f2be43'),
           (43,49,'#eaa32e')]
    for y0,y1,color in bands:
        for y in range(y0,y1+1):
            for x in range(15,50):
                if face.getpixel((x,y)):
                    im.putpixel((x,y),(*bytes.fromhex(color[1:]),255))
    # Reference brows x=23..55 / 104..136, y=46..62; eyes y=76..95.
    # Long, curved tapered brows sit outwards, not above the inner eye corners.
    dark='#654830'
    brow=[(18,26),(20,26),(23,25),(26,23),(25,25),(23,27),(19,27)]
    d.polygon(brow,fill='#465659')
    d.polygon([(64-x,y) for x,y in brow],fill='#465659')
    for x in (25,35):
        d.ellipse((x,31,x+4,36),fill='#67420f')
        d.ellipse((x+1,32,x+3,35),fill='#966920')
    # The reference is a broad, shallow worried frown, not a round O mouth.
    # Both corners droop; the raised centre of the lower lip stays face-coloured.
    # Reference face y=9..153, dark mouth y=114..129: centre at 78.125%.
    # Here face y=14..50, mouth y=40..44: 77.778%, within 0.125 native pixel.
    d.polygon([(26,44),(26,42),(28,40),(36,40),(38,42),(38,44),
               (36,44),(35,43),(29,43),(28,44)],fill=dark)
    d.line([(28,41),(36,41)],fill='#94622c',width=1)
    # Large icy sweat drop: an actual pixel outline with a small white gleam.
    d.polygon([(19,31),(21,35),(23,40),(23,43),(21,46),(18,46),
               (15,43),(15,40),(17,35)],fill='#28b9eb')
    d.line([(19,34),(17,40),(17,42)],fill='#dcfbf4',width=1)
    # Scale the whole expression together, retaining the measured feature ratios.
    # 37 native pixels -> 30 (80%, rounded to the pixel grid), centered at (32,30),
    # the centre of Omega's upper loop rather than the whole icon including feet.
    expression = im.crop((14,14,51,51)).resize((30,30),Image.Resampling.NEAREST)
    background.alpha_composite(expression,(17,15))
    im = background
    out = ROOT/'docs/branding'
    out.mkdir(parents=True,exist_ok=True)
    im.save(out/'googology-mark-64.png')
    im.resize((512,512),Image.Resampling.NEAREST).save(out/'googology-mark.png')
    im.resize((128,128),Image.Resampling.NEAREST).save(ROOT/'src/main/resources/assets/googology/icon.png')
    # Editable SVG with horizontal runs; every run sits on the same pixel grid.
    svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" shape-rendering="crispEdges">',
         '<title>果糕逻辑 · Guogaology</title>',
         '<desc>64像素网格：大写Ω与中央惊惧流汗表情，深靛蓝背景。</desc>']
    for y in range(64):
        x=0
        while x<64:
            color=im.getpixel((x,y));end=x+1
            while end<64 and im.getpixel((end,y))==color:end+=1
            if color[3]:
                rgb='#{:02x}{:02x}{:02x}'.format(*color[:3])
                svg.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{rgb}"/>')
            x=end
    svg.append('</svg>')
    (out/'googology-mark.svg').write_text('\n'.join(svg)+'\n',encoding='utf8')
    preview=Image.new('RGB',(704,536),'#e9eff1')
    large=im.resize((512,512),Image.Resampling.NEAREST)
    preview.paste(large,(12,12),large)
    for n,y in [(128,20),(64,196),(32,300),(16,380)]:
        small=im.resize((n,n),Image.Resampling.NEAREST)
        preview.paste(small,(556,y),small)
    preview.save(out/'size-check.png')


if __name__=='__main__':run()
