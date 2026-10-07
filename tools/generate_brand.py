"""Pixel logo: capital Omega and the giant Christmas-tree globe's Guogao face.

All geometry lives on the same integer grid. SVG and PNG share the exact pixels;
nearest-neighbour exports preserve the deliberate steps without smooth edges.
"""
from pathlib import Path
import math
import re
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]


def globe_face(size=60):
    """Sample the same large-globe formula as MosaicMotifs.facePixel (no dark rim)."""
    source = (ROOT/'src/main/java/dev/guogaology/survival/MosaicMotifs.java').read_text(encoding='utf8')
    palette = re.search(r'COLORS=\{(.*?)\};',source,re.S).group(1)
    colors = [int(x,16) for x in re.findall(r'0x([0-9a-f]{6})',palette)]
    assert len(colors)==16
    face = Image.new('RGBA',(size,size))
    r=size*.5;c=(size-1)*.5
    for y in range(size):
        for x in range(size):
            u=(x-c)/r;v=(c-y)/r;a=abs(u)
            if u*u+v*v>1:continue
            color=(10 if v>.70 else 9 if v>.46 else 8) if v>.25 else 1 if v>.02 else 3 if v<-.60 else 2
            brow=.48-.63*(a-.20)
            if .18<a<.67 and abs(v-brow)<.065:color=6
            if ((a-.30)/.115)**2+((v+.04)/.155)**2<1:color=5
            if a<.32 and -.64+.30*u*u<v<-.40-.75*u*u:color=5
            t=(.11-v)/.79
            if 0<=t<=1:
                w=.22*math.sin(math.pi*t)**.70
                if abs(u+.72)<w:
                    color=0 if -.82<u<-.72 and .34<t<.77 else 7 if u>-.66 else 8
            rgb=colors[color]
            face.putpixel((x,y),((rgb>>16)&255,(rgb>>8)&255,rgb&255,255))
    return face


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
    # Keep the large-globe contour but sample it at 30 pixels for broader steps.
    # 60/128 has the approved footprint (~80% of the old face), rendered at 2x.
    # The face sits at (64,60), the upper loop's centre.
    im = im.resize((128,128),Image.Resampling.NEAREST)
    im.alpha_composite(globe_face(30).resize((60,60),Image.Resampling.NEAREST),(34,30))
    out = ROOT/'docs/branding'
    out.mkdir(parents=True,exist_ok=True)
    im.save(out/'guogaology-mark-128.png')
    im.resize((512,512),Image.Resampling.NEAREST).save(out/'guogaology-mark.png')
    im.resize((128,128),Image.Resampling.NEAREST).save(ROOT/'src/main/resources/assets/guogaology/icon.png')
    # Editable SVG with horizontal runs; every run sits on the same pixel grid.
    svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" shape-rendering="crispEdges">',
         '<title>果糕逻辑 · Guogaology</title>',
         '<desc>128像素网格：大写Ω与居中的果糕巨球表情，深靛蓝背景。</desc>']
    for y in range(128):
        x=0
        while x<128:
            color=im.getpixel((x,y));end=x+1
            while end<128 and im.getpixel((end,y))==color:end+=1
            if color[3]:
                rgb='#{:02x}{:02x}{:02x}'.format(*color[:3])
                svg.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{rgb}"/>')
            x=end
    svg.append('</svg>')
    (out/'guogaology-mark.svg').write_text('\n'.join(svg)+'\n',encoding='utf8')
    preview=Image.new('RGB',(704,536),'#e9eff1')
    large=im.resize((512,512),Image.Resampling.NEAREST)
    preview.paste(large,(12,12),large)
    for n,y in [(128,20),(64,196),(32,300),(16,380)]:
        small=im.resize((n,n),Image.Resampling.NEAREST)
        preview.paste(small,(556,y),small)
    preview.save(out/'size-check.png')


if __name__=='__main__':run()
