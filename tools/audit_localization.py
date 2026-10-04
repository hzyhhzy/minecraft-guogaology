"""Check bilingual coverage, Minecraft format arguments, and live UI references.

Standard-library only. Validates the committed language files used by all four
targets, including dynamic block/item entrypoints, rather than counting keys alone.
"""
import argparse
from collections import Counter
import json
from pathlib import Path
import re
from audit_resources import LEGACY_ENTRIES

ROOT = Path(__file__).resolve().parents[1]
HAN = re.compile(r'[\u3400-\u9fff]')
JAVA_STRING = r'"(?:[^"\\]|\\.)*"'
JAVA_TOKEN = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|' + JAVA_STRING)


def read(path):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f'Duplicate JSON key in {path}: {key}')
            result[key] = value
        return result
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)


def arguments(text):
    """Minecraft supports %s, indexed %1$s, and escaped %%; keep argument usage."""
    result = Counter()
    implicit = 1
    for match in re.finditer(r'%(?:(\d+)\$)?([s%])|%', text):
        if match.group(2) is None:
            raise ValueError(f'Unsupported or unescaped format marker: {text}')
        if match.group(2) == '%':
            if match.group(1):
                raise ValueError(f'Indexed percent escape: {text}')
            continue
        index = int(match.group(1)) if match.group(1) else implicit
        if index < 1:
            raise ValueError(f'Invalid argument index: {text}')
        result[index] += 1
        if not match.group(1):
            implicit += 1
    return result


def audit(root):
    root = root.resolve()
    assets = root / 'src/main/resources/assets/googology'
    locales = {code: read(assets / f'lang/{code}.json') for code in ('en_us', 'zh_cn')}
    en, zh = locales['en_us'], locales['zh_cn']
    if en.keys() != zh.keys():
        raise ValueError(f'Language key mismatch: missing English={sorted(zh.keys()-en.keys())}; '
                         f'missing Chinese={sorted(en.keys()-zh.keys())}')
    for key in en:
        for code, lang in locales.items():
            if not isinstance(lang[key], str) or not lang[key].strip():
                raise ValueError(f'Empty or non-string translation: {code}/{key}')
        if HAN.search(en[key]):
            raise ValueError(f'Chinese text in English translation: {key}')
        if arguments(en[key]) != arguments(zh[key]):
            raise ValueError(f'Format argument mismatch: {key}')

    catalog = read(root / 'tools/copy_catalog.json')
    for code, lang in locales.items():
        for key, value in catalog['translations'][code].items():
            if lang.get(key) != value:
                raise ValueError(f'Stale catalog translation: {code}/{key}')
        if any(key in lang for key in catalog['removed']):
            raise ValueError(f'Retired translation returned: {code}')

    references = set()

    def require(key, source, prefix=False):
        references.add(key)
        if key not in en and not (prefix and any(k.startswith(key) for k in en)):
            raise ValueError(f'Missing translation {key} referenced by {source}')

    # The development checkout may retain these six unregistered old aliases;
    # their live models belong to lantern variants, not additional registry IDs.
    blocks = {p.stem for p in (assets / 'blockstates').glob('*.json')} - LEGACY_ENTRIES
    for name in sorted(blocks):
        require('block.googology.' + name, 'blockstate')
    items = {p.stem for p in (assets / 'models/item').glob('*.json')} - blocks - LEGACY_ENTRIES
    for name in sorted(items):
        require('item.googology.' + name, 'item model')
    for folder, kind in (('worldgen/biome', 'biome'), ('dimension', 'dimension')):
        for path in (root / 'src/main/resources/data/googology' / folder).glob('*.json'):
            require(f'{kind}.googology.{path.stem}', path)

    def components(node, source):
        if isinstance(node, dict):
            if isinstance(node.get('translate'), str) and '.googology.' in node['translate']:
                require(node['translate'], source)
            for value in node.values():
                components(value, source)
        elif isinstance(node, list):
            for value in node:
                components(value, source)
    for path in (root / 'src/main/resources/data').rglob('*.json'):
        components(read(path), path)
    sounds = assets / 'sounds.json'
    if sounds.exists():
        for event in read(sounds).values():
            if 'subtitle' in event:
                require(event['subtitle'], sounds)

    source_roots = [root / 'src/main/java', root / 'src/client/java', root / 'ports/common']
    source_roots.extend((root / 'ports').glob('*/src'))
    java_files = 0
    for directory in source_roots:
        for path in directory.rglob('*.java'):
            if any(part.lower().startswith('qa') for part in path.relative_to(directory).parts):
                continue
            java_files += 1
            source = path.read_text(encoding='utf-8')
            # Preserve string contents while excluding commented-out historical code.
            source = JAVA_TOKEN.sub(lambda m: m[0] if m[0].startswith('"') else ' ', source)
            for literal in re.findall(JAVA_STRING, source):
                if HAN.search(literal):
                    raise ValueError(f'Hardcoded Chinese Java string: {path.relative_to(root)}')
            for match in re.finditer(r'(?:Text|Component)\.translatable\(\s*"([^"]+)"', source):
                if '.googology.' in match[1]:
                    require(match[1], path, prefix=True)
            if '"mining.googology."+key' in source:
                for match in re.finditer(r'\btr\(\s*"([^"]+)"', source):
                    require('mining.googology.' + match[1], path, prefix=True)
            for match in re.finditer(r'(?:Text|Component)\.literal\(\s*"([^"]+)"', source):
                if re.search(r'[A-Za-z\u3400-\u9fff]', match[1]):
                    raise ValueError(f'Untranslated player-facing literal in {path.relative_to(root)}: {match[1]}')

    for kind in ('name', 'summary', 'description'):
        require(f'modmenu.{kind}Translation.googology', 'Mod Menu')
    metadata = read(root / 'src/main/resources/fabric.mod.json')
    if not re.search(r'[A-Za-z]{3}', metadata['description']) or not HAN.search(metadata['description']):
        raise ValueError('Static launcher description must remain readable in English and Chinese')
    return {'languages': list(locales), 'keys_per_language': len(en), 'block_names': len(blocks),
            'non_block_item_names': len(items), 'java_files_checked': java_files,
            'referenced_keys_or_prefixes': len(references), 'missing_translations': 0,
            'format_mismatches': 0, 'hardcoded_chinese_ui': 0}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--check', action='store_true', help='Validate both languages; failures are fatal')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    report = audit(args.root)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))
