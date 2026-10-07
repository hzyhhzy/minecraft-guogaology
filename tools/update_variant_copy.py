"""Update portable appearance names and current decorative-item wording."""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parents[1]
COPY = {
    'block.guogaology.nuke_chair': ('震惊瘫坐椅', 'Shock-Slump Chair'),
    'item.guogaology.expression_variant': ('%s · %s', '%s · %s'),
    'item.guogaology.color_variant': ('%s · 色号 %s', '%s · Color %s'),
    'item.guogaology.palette_color': ('色板：%s / %s', 'Palette color: %s / %s'),
    'item.guogaology.chime_note': ('下次音符：第 %s / 8 项', 'Next note: %s / 8'),
    'item.guogaology.lantern_expression': ('表情：%s', 'Expression: %s'),
    # Verified from emotion=0/1 blockstate models AND the actual mouth pixels.
    # These user-requested names are intentional words; do not replace by emoji.
    'item.guogaology.lantern_emotion.0': ('馃槹（张嘴）', 'Guogao (open mouth)'),
    'item.guogaology.lantern_emotion.1': ('馃槬（闭嘴）', 'Guogua (closed mouth)'),
    'item.guogaology.tape_ink.0': ('空心墨点', 'Hollow ink dot'),
    'item.guogaology.tape_ink.1': ('实心墨点', 'Solid ink dot'),
    'block.guogaology.amber_light': ('琥珀彩灯', 'Amber Light'),
    'block.guogaology.cyan_light': ('青蓝彩灯', 'Cyan Light'),
    'block.guogaology.rose_light': ('玫红彩灯', 'Rose Light'),
    'block.guogaology.lime_light': ('青柠彩灯', 'Lime Light'),
    'block.guogaology.violet_light': ('紫罗兰彩灯', 'Violet Light'),
    'block.guogaology.scarlet_light': ('绯红彩灯', 'Scarlet Light'),
}


def generate(root=ROOT, *, check=False):
    root = Path(root)
    path = root / 'tools/copy_catalog.json'
    catalog = json.loads(path.read_text(encoding='utf-8'))
    expected = {}
    for index, locale in enumerate(('zh_cn', 'en_us')):
        values = {key: pair[index] for key, pair in COPY.items()}
        catalog['translations'][locale].update(values)
        lang_path = root / f'src/main/resources/assets/guogaology/lang/{locale}.json'
        language = json.loads(lang_path.read_text(encoding='utf-8'))
        language.update(values)
        expected[lang_path] = language
    expected[path] = catalog
    changed = []
    for path, value in expected.items():
        if json.loads(path.read_text(encoding='utf-8')) == value:
            continue
        changed.append(path)
        if not check:
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    if check and changed:
        raise AssertionError('Stale variant copy: '+', '.join(str(path) for path in changed))
    print('VARIANT_COPY_OK localized keys=' + str(len(COPY)) + ' changed=' + str(len(changed)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(check=parser.parse_args().check)
