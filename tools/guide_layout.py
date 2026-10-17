"""Stable guide coordinates; file/hash iteration never decides branch order."""
THEMES=('matrix','power','hydra','absence','weaver','astra','frontier','guogao')
# Between native 1.0 spacing and the earlier 1.45 / 1.4 guide layout.
COLUMN_SPACING=1.2
LEAF_SPACING=1.2

def positions(nodes):
    children={key:[] for key in nodes}
    for key,node in nodes.items():
        if 'parent' in node:children[node['parent'].removeprefix('guogaology:')].append(key)
    preferred={
        'guide/enter_outer':['first_ore','return_ready','lamp_shaper','eat_guogao'],
        'guide/first_ore':['first_tool','first_omega'],
        'guide/first_tool':['first_book','full_set'],
        'guide/enter_inner':['shards','number_tools']+['biome_'+t for t in THEMES[:-1]]+['enter_underworld','landmarks_all'],
        'guide/crystal':['ordinal1','station'],
        'guide/station':['socketed','higher_station'],
        'guide/ultimate_station':['omega_mastery'],
        'guide/material_astra':['core_astra_1','compute_setup'],
    }
    for theme in THEMES:
        root='guide/enter_underworld' if theme=='guogao' else 'guide/biome_'+theme
        preferred[root]=['material_'+theme,'landmark_'+theme]+(['play_laver'] if theme=='weaver' else [])
    result={};next_leaf=0
    def visit(key,depth):
        nonlocal next_leaf
        order=['guide/'+k for k in preferred.get(key,())]
        branch=sorted(children[key],key=lambda child:(order.index(child) if child in order else len(order),child))
        if branch:
            rows=[visit(child,depth+1) for child in branch];y=(rows[0]+rows[-1])/2
        else:y=next_leaf;next_leaf+=LEAF_SPACING
        result['guogaology:'+key]=[round(depth*COLUMN_SPACING,4),round(y,4)]
        return y
    visit('arrival',0)
    assert len(result)==len(nodes),'Unpositioned guide nodes'
    return dict(sorted(result.items()))

def reading_order(nodes):
    """Keep the generated bilingual reference grouped like the displayed branches."""
    xy=positions(nodes);children={key:[] for key in nodes}
    for key,node in nodes.items():
        if 'parent' in node:children[node['parent'].removeprefix('guogaology:')].append(key)
    result=[]
    def visit(key):
        result.append(key)
        for child in sorted(children[key],key=lambda child:xy['guogaology:'+child][1]):visit(child)
    visit('arrival')
    return result
