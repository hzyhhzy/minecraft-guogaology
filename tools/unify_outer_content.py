"""Synchronize imported aliases and the creative catalog in the host namespace.

Only owned Java declarations are edited. Artwork and world data have their own
source generators. This is safe to rerun after changing the imported catalog.
"""
import re
from import_outer_content import ROOT,ALIASES,CONTENT_IDS,DATA_NAMES

def run():
    for p in (ROOT/'ports/common/main/dev/googology/outer/GoogologyMod.java',ROOT/'ports/26.3/src/main/java/dev/googology/outer/GoogologyMod.java'):
        text=p.read_text('utf8')
        text=re.sub(r'    private static final java.util.Set<String> CONTENT=.*?;\n','',text)
        cases='\n'.join(f'            case "{name}" -> "{target if ":" in target else "googology:"+target}";' for name,target in ALIASES.items())
        method='    private static String alias(String name){return switch(name){\n'+cases+'\n        default -> null;};}'
        text=re.sub(r'    private static String alias\(String name\).*?default -> null;};}',method,text,flags=re.S)
        assert DATA_NAMES=={'googology':'outer'},'Review Java resource renames when extending the map'
        text=re.sub(r'    public static Identifier id\(String name\)\{.*?\}',
            '    public static Identifier id(String name){String shared=alias(name);return shared!=null?Identifier.parse(shared):Identifier.fromNamespaceAndPath("googology",name.equals("googology")?"outer":name);}',text)
        p.write_text(text,'utf8')
    names=','.join('"'+n+'"' for n in sorted(CONTENT_IDS-ALIASES.keys()))
    for p in (ROOT/'src/main/java/dev/googology/CreativeCatalog.java',ROOT/'ports/common/main/dev/googology/CreativeCatalog.java'):
        text=p.read_text('utf8')
        text=re.sub(r'    private static final Set<String> IMPORTED=.*?;\n','    private static final Set<String> IMPORTED=Set.of('+names+');\n',text)
        p.write_text(text,'utf8')
    print('Imported aliases and creative entries synchronized under googology')

if __name__=='__main__':run()
