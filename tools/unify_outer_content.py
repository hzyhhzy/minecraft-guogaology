"""Synchronize imported aliases and the creative catalog in the host namespace.

Only owned Java declarations are edited. Artwork and world data have their own
source generators. This is safe to rerun after changing the imported catalog.
"""
import re
from import_outer_content import ROOT,ALIASES,CONTENT_IDS,DATA_NAMES,NS,SOURCE_NS

def run():
    for p in (ROOT/'ports/common/main/dev/guogaology/outer/GuogaologyMod.java',ROOT/'ports/26.3/src/main/java/dev/guogaology/outer/GuogaologyMod.java'):
        text=p.read_text('utf8')
        text=re.sub(r'    private static final java.util.Set<String> CONTENT=.*?;\n','',text)
        # The retained Alice aliases accept both the donor's original symbols
        # and renamed host call sites; neither becomes an old runtime ID.
        def case(name,target):
            names=[name]
            if name.startswith(SOURCE_NS+'_'):names.append(NS+name[len(SOURCE_NS):])
            keys=', '.join('"'+key+'"' for key in names)
            return f'            case {keys} -> "{target if ":" in target else NS+":"+target}";'
        cases='\n'.join(case(name,target) for name,target in ALIASES.items())
        method='    private static String alias(String name){return switch(name){\n'+cases+'\n        default -> null;};}'
        text=re.sub(r'    private static String alias\(String name\).*?default -> null;};}',method,text,flags=re.S)
        assert DATA_NAMES=={SOURCE_NS:'outer'},'Review Java resource renames when extending the map'
        text=re.sub(r'    public static Identifier id\(String name\)\{.*?\}',
            '    public static Identifier id(String name){String shared=alias(name);return shared!=null?Identifier.parse(shared):Identifier.fromNamespaceAndPath("guogaology",(name.equals("googology")||name.equals("guogaology"))?"outer":name);}',text)
        p.write_text(text,'utf8')
    names=','.join('"'+n+'"' for n in sorted(CONTENT_IDS-ALIASES.keys()))
    for p in (ROOT/'src/main/java/dev/guogaology/CreativeCatalog.java',ROOT/'ports/common/main/dev/guogaology/CreativeCatalog.java'):
        text=p.read_text('utf8')
        text=re.sub(r'    private static final Set<String> IMPORTED=.*?;\n','    private static final Set<String> IMPORTED=Set.of('+names+');\n',text)
        p.write_text(text,'utf8')
    print('Imported aliases and creative entries synchronized under guogaology')

if __name__=='__main__':run()
