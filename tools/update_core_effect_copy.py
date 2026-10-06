"""Current bilingual wording for the 0.3.11 core roles, after older generators."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def generate():
    path = ROOT / 'tools/copy_catalog.json'
    catalog = json.loads(path.read_text(encoding='utf8'))
    gear = [('增产','Yield'),('攻击力','Attack'),('范围开采／呼吸','Area mining / breathing'),
            ('触及距离','Reach'),('耐久保护','Wear protection'),('命中爆裂','Impact burst'),
            ('防护','Protection'),('束缚','Binding'),('通用增强','Universal enhancement')]
    book = [('增产','Yield'),('攻击力','Attack'),('挖掘速度','Mining speed'),
            ('跳跃／飞行','Jump / flight'),('治疗／夜视','Healing / night vision'),
            ('投射爆裂／防火','Projectile burst / fire resistance'),('防护','Protection'),
            ('生命／免疫','Health / immunity'),('通用增强','Universal enhancement')]
    extra = {
        'manuscript.open':('右键：直接镶嵌晶核','Right-click: socket cores directly'),
        'manuscript_label':('扽西手稿','Denxi Manuscript'),
        'manuscript_drag_hint':('直接取放晶核 · 手稿保持在手中','Move cores directly · keep this manuscript held'),
        'status_attack':('攻击：%s HP','Attack: %s HP'),
        'status_mining':('挖速：×%s','Mining: ×%s'),
        'status_yield':('增产等级：%s','Yield level: %s'),
        'status_range':('额外开采：%s 格','Extra mining: %s blocks'),
        'status_reach':('触距：+%s 格','Reach: +%s blocks'),
        'status_healing':('治疗：%s HP / 4 秒','Healing: %s HP / 4 s'),
        'status_health':('生命上限：+%s HP','Max health: +%s HP'),
        'status_defense':('承伤：×%s','Damage taken: ×%s'),
        'status_wear':('耐久保护：×%s','Wear protection: ×%s'),
        'status_jump':('跳高：+%s 格','Jump height: +%s blocks'),
        'status_flight':('飞行：%s','Flight: %s'),
        'flight_slow':('乐魂式慢速飞行','Slow flight'),
        'flight_normal':('创造式飞行','Creative-style flight'),
        'flight_deep':('创造式飞行 · 冲刺八倍','Creative-style flight · 8× sprint'),
    }
    for locale, index in [('zh_cn',0),('en_us',1)]:
        values = catalog['translations'][locale]
        for level in range(1,4):
            suffix = '' if level == 1 else f'_lv{level}'
            values[f'block.googology.laver_core{suffix}'] = ('紫菜凝核' if index == 0 else 'Laver Condensation Core') + f' · Lv{level}'
        for i in range(9):
            values[f'mining.googology.effect.{i}'] = gear[i][index]
            values[f'mining.googology.manuscript.effect.{i}'] = book[i][index]
        values['mining.googology.manuscript.passive'] = ('副手持有：被动生效；右键打开镶嵌界面' if index == 0 else 'Held offhand: passive effects; right-click to socket cores')
        values['mining.googology.effect.7'] = ('束缚' if index == 0 else 'Binding')
        for key, pair in extra.items():
            values['mining.googology.'+key] = pair[index]
    path.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    for locale in ('zh_cn','en_us'):
        path = ROOT / f'src/main/resources/assets/googology/lang/{locale}.json'
        values = json.loads(path.read_text(encoding='utf8'))
        for key in catalog['removed']:
            values.pop(key,None)
        values.update(catalog['translations'][locale])
        path.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('Updated bilingual 0.3.11 core roles and Laver Condensation Core name')

if __name__ == '__main__':
    generate()
