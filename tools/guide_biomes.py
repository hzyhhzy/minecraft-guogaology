"""Authored biome progression and capability-based core copy, not recipe dumps."""

# Representative scenery; LHO has a dedicated three-letter cube, never a core.
BIOME_ICONS={'matrix':'y_sequence_stone','power':'power_bricks','hydra':'epsilon_bloom',
             'absence':'lho_guide_emblem','weaver':'iblp_node','astra':'nuke_chair',
             'frontier':'turing_tape','guogao':'guogao_lantern'}

# theme, biome, representative raw icon, material groups, material title/description,
# three (title, capability) pairs in Chinese and English.
THEME_GUIDES = {
 'matrix': ('ordinal_crags','matrix_sheet',[['matrix_sheet']],
  ('折峰之间的一页','A Page between Peaks'),
  ('从山脉晶页采集矩阵薄片。平面的矩阵，也能展开成立体山脉。','Gather a Matrix Sheet from a mountain page. A flat matrix can unfold into a mountain.'),
  [('多挖一格','One More Block','镶入镐可额外开采；装入手稿则同时增加挖速加值和急迫乘区。','Socket into a pick for extra excavation, or a manuscript for flat and multiplicative mining speed.'),
   ('并行开采','Parallel Excavation','更高阶的矩阵扩大额外开采能力，也加强手稿的两项挖速。','A stronger matrix expands extra excavation and both manuscript mining bonuses.'),
   ('矩阵展开','The Matrix Unfolds','将开采能力推进到三级；额外开采不会向选中方块下方延伸。','Reach third-grade excavation. Extra mining never targets blocks below the selected block.')]),
 'power': ('power_desert','arrow_knot',[['arrow_knot'],['tree_node_red','tree_node_green','tree_node_blue']],
  ('箭头与三色枝端','Arrows and Colored Tips'),
  ('收集箭纹木节与任一种TREE彩色节点，辨认荒漠中箭头、树与图的踪迹。','Collect an Arrow Knot and any colored TREE node: traces of arrows, trees and graphs in the desert.'),
  [('箭头的锋芒','The Edge of an Arrow','镶入剑、弓或手稿可加强攻击，武器与手稿分别计入。','Socket into a sword, bow or manuscript for attack; weapon and manuscript contributions are separate.'),
   ('力量迭代','Iterating Power','将攻击晶核推进到二级；同一物品上的同类加成平方求和开根号。','Advance the attack core to Lv2; matching copies in one carrier use root-sum-squares.'),
   ('幂塔升阶','A Taller Power Tower','三级幂塔进一步加强攻击，在里界与地府表现为更强的独立乘区。','Third-grade Tower strengthens attack further, with an independent multiplier in the deep realms.')]),
 'hydra': ('epsilon_meadow','hydra_sprig',[['hydra_sprig']],
  ('剪下一枝，不斩九头','A Sprig, Not Nine Heads'),
  ('采集Hydra芽穗，观察树状分枝；这里的Hydra是景观植物。','Gather a Hydra Sprig from branching scenery. These Hydras are plants.'),
  [('更多的收获','A Richer Harvest','镐获得时运II并可切换精准采集；剑对应抢夺，弓对应无限，手稿延长触距。','Picks gain Fortune II and switchable Silk Touch; swords gain Looting, bows Infinity, manuscripts reach.'),
   ('一枝化多枝','One Branch Becomes Many','二级提升时运与抢夺，并让弓获得多重射击。','Lv2 improves Fortune and Looting and grants bows Multishot.'),
   ('枝端之外','Beyond the Branch Tips','三级提供更高时运与抢夺、弓的穿透，或更远的手稿触距。','Lv3 offers higher Fortune and Looting, bow Piercing, or longer manuscript reach.')]),
 'absence': ('lho_absence','ordinal_crystal',[['ordinal_crystal'],['fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z']],
  ('让虚无停留','Let Absence Remain'),
  ('普通序数晶体稳定周围两圈，即5×5×5范围；ψ、Z、fffZ、FOS会变色。收集一枚这些材料和一块普通晶体。','A base Ordinal Crystal stabilizes two rings: a 5×5×5 area. ψ, Z, fffZ and FOS change color. Obtain one such material and a base crystal.'),
  [('存在感渐淡','A Fading Presence','装入手稿后，普通野怪的主动索敌距离缩短；装入装备则保护耐久。','In a manuscript, shorten ordinary mob detection range; on equipment, reduce durability consumption.'),
   ('无人留意','Unnoticed','手稿使普通野怪不再主动索敌，并提供夜视；特殊索敌机制仍保留。','A manuscript prevents ordinary proactive targeting and grants Night Vision; special detection remains.'),
   ('无声的回击','A Silent Reply','手稿连普通报复索敌也不再触发；装备上的耐久保护更强。','A manuscript also prevents ordinary retaliation targeting; equipment gains stronger wear protection.')]),
 'weaver': ('laver_tablelands','laver_gel',[['laver_gel']],
  ('紫菜与软糖之间','Between Laver and Gummy'),
  ('从紫菜植株收集紫菜胶质；镶边石旁通向数字砖的一行图案可以弹奏。','Collect Laver Gel from Laver plants. An inlay can play a diagram row ending in a number brick.'),
  [('潮声与回响','Tides and Echoes','头盔减少耗氧并提供水下速掘，靴子改善水中移动；手稿每4秒治疗。','Helmets reduce oxygen use and grant Aqua Affinity, boots improve underwater movement, manuscripts heal every four seconds.'),
   ('水下亦从容','At Ease Underwater','二级继续加强水下能力；手稿同时提供治疗与饥饿下限保障。','Lv2 strengthens aquatic benefits; manuscripts combine healing with a minimum food level.'),
   ('深水无息','Breathless Depths','三级头盔不再耗氧，靴子提供更强水中移动；手稿治疗与饥饿保障也升级。','A Lv3 helmet stops oxygen consumption, boots improve swimming further, and manuscripts improve healing and food protection.')]),
 'astra': ('astra_awakening','cloud_pearl',[['cloud_pearl'],['compute_chip']],
  ('云中的算力','Computing in the Clouds'),
  ('收集核爆凝珠与算力芯片，连接云端扭结中的炽核与地面的机柜。','Collect a Nuclear Blast Pearl and Compute Chip, linking the hot nuclei of cloud knots with ground-level server racks.'),
  [('震撼的一击','An Astonishing Hit','装入剑或弓，命中会对目标与附近生物产生爆裂伤害，不破坏地形。','In a sword or bow, hits burst against the target and nearby creatures without damaging terrain.'),
   ('连锁的余波','Spreading Shockwaves','二级加强命中的爆裂；核爆之心不适用于手稿。','Lv2 strengthens the on-hit burst. Heart of Nuclear Awe cannot be socketed into manuscripts.'),
   ('临界已至','The Critical Point','三级爆裂更强；多颗同核的爆裂比例平方求和开根号。','Lv3 bursts are stronger; matching burst contributions combine by root-sum-squares.')]),
 'frontier': ('limit_highlands','set_membrane',[['set_membrane','turing_endcap']],
  ('集合与纸带的边缘','Between Sets and Tape'),
  ('收集集合膜或纸带端帽，观察集合晶壳与长纸带。','Collect a Set Membrane or Tape Endcap and examine set shells and long Turing tapes.'),
  [('落地从容','A Gentle Landing','护甲提供额外防护；手稿降低摔落与撞击伤害、提高安全摔落高度，并增加跳高与步速。','Armor adds protection; manuscripts reduce fall and impact damage, raise safe fall distance, and improve jumping and walking.'),
   ('离地而行','Above the Ground','装入手稿可切换缓速飞行，并获得防火；护甲仍用于防护。','In a manuscript, enable slow flight and fire protection; on armor, retain defensive protection.'),
   ('越过界限','Beyond the Boundary','装入手稿可自由飞行并冲刺；里界与地府的冲刺飞行更快。','In a manuscript, gain free flight and sprint; deep-realm sprint flight is faster.')]),
 'guogao': ('guogao_forest','dread_resin',[['dread_resin']],
  ('冥杉凝下的一滴','A Drop of Dread Resin'),
  ('从地府冥杉景物收集树脂；留意冥林、错层与无穷降链。','Gather resin from Underworld Dread Fir scenery; explore forests, misaligned strata and descending chains.'),
  [('令人流汗','Breaking into a Sweat','剑与弓使目标缓慢、中毒；手稿增加生命，装备护甲本身不直接加生命。','Swords and bows apply Slowness and Poison; manuscripts add health, while armor itself adds none.'),
   ('诡异开始蔓延','The Unease Spreads','武器施加更强负面状态；手稿增加生命、免疫部分状态，并优先使用背包图腾保命。','Weapons apply stronger debuffs; manuscripts add health, resist selected effects, and prioritize real inventory Totems.'),
   ('难以直视','Hard to Behold','三级武器可使目标凋零、虚弱、反胃并有概率失明；手稿强化生命与状态免疫。','Lv3 weapons inflict Wither, Weakness and Nausea with a chance of Blindness; manuscripts improve health and immunities.')]),
}

def append_biome_guides(node,item,names,inventory,travel,ns):
    roots=('sequence_core','power_tower_core','hydra_bud','lho_trace','laver_core','astra_critical_core','boundary_core','guogao_heart')
    themes=('matrix','power','hydra','absence','weaver','astra','frontier','guogao')
    manual={'done':{'trigger':'minecraft:impossible'}}
    for theme,core in zip(themes,roots):
        biome,icon,groups,title,description,levels=THEME_GUIDES[theme]
        root='guide/enter_underworld' if theme=='guogao' else 'guide/biome_'+theme
        if theme!='guogao':
            node(root,'guide/enter_inner',BIOME_ICONS[theme],'走进'+names['zh_cn']['biome.guogaology.'+biome],
                 'Visit '+names['en_us']['biome.guogaology.'+biome],
                 '进入里界的'+names['zh_cn']['biome.guogaology.'+biome]+'，寻找本地材料与巨构。',
                 'Enter '+names['en_us']['biome.guogaology.'+biome]+' in Inner to discover its materials and megastructure.',manual)
        item('material_'+theme,root,icon,title[0],title[1],description[0],description[1],*groups)
        for level,(zh,en,detail,english) in enumerate(levels,1):
            name=core if level==1 else core+'_lv'+str(level)
            cn=names['zh_cn']['block.guogaology.'+core].removesuffix(' · Lv1');eng=names['en_us']['block.guogaology.'+core].removesuffix(' · Lv1')
            item('core_'+theme+'_'+str(level),'guide/material_'+theme if level==1 else 'guide/core_'+theme+'_'+str(level-1),name,
                 zh,en,'获得'+str(level)+'级'+cn+'。'+detail,'Obtain '+eng+' Lv'+str(level)+'. '+english,[name],frame='goal' if level==3 else 'task')
        building=names['zh_cn']['structure.guogaology.'+theme];enbuilding=names['en_us']['structure.guogaology.'+theme]
        node('guide/landmark_'+theme,root,'landmark_emblem_'+theme,'探索'+building,'Explore '+enbuilding,
             '进入'+building+'的建筑空间，探索它的通路与主题陈列。','Enter the built space of '+enbuilding+' and explore its paths and exhibits.',manual,frame='goal')
