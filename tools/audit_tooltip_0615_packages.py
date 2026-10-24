"""Bound the compact-tooltip delivery to display code and added bilingual labels."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import io
import json

ROOT = Path(__file__).resolve().parents[1]
OLD, NEW = '0.6.14', '0.6.15'
TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')
HOOK = 'EquipmentTooltipAttributesMixin'
CLASS_NAMES = {
    'dev/guogaology/mining/' + name + '.class' for name in (
        'OrdinalGear', 'OrdinalArmor', 'OrdinalBowItem', 'DenxiManuscript',
        'EquipmentTooltips', 'EquipmentTooltipRules')
} | {f'dev/guogaology/client/mixin/{HOOK}.class'}


def read_zip(path):
    with ZipFile(path) as archive:
        return {n: archive.read(n) for n in archive.namelist() if not n.endswith('/')}


def audit():
    results = []
    for target in TARGETS:
        old = ROOT / f'build/releases/{OLD}/guogaology-dimension-{target}-{OLD}.jar'
        new = ROOT / f'build/releases/{NEW}/guogaology-dimension-{target}-{NEW}.jar'
        a, b = read_zip(old), read_zip(new)
        resources = {n for n in a.keys() | b.keys() if n.startswith(('assets/', 'data/'))}
        assert resources <= a.keys() and resources <= b.keys(), 'All resource IDs retained'
        for name in resources:
            if name in ('assets/guogaology/lang/zh_cn.json', 'assets/guogaology/lang/en_us.json'):
                before, after = json.loads(a[name]), json.loads(b[name])
                assert all(after.get(k) == v for k, v in before.items()), 'Existing language/GUI text unchanged'
                extra = after.keys() - before.keys()
                assert len(extra) == 28 and all(k.startswith('mining.guogaology.tooltip.') for k in extra)
            else:
                assert a[name] == b[name], name
        assert json.loads(a['guogaology.mixins.json']) == json.loads(b['guogaology.mixins.json'])
        before, after = (json.loads(x['guogaology.client.mixins.json']) for x in (a, b))
        assert after['client'].count(HOOK) == 1
        after['client'].remove(HOOK)
        assert before == after, 'Only one client-only display hook added'
        changed = sorted(n for n in a.keys() | b.keys() if a.get(n) != b.get(n))
        changed_classes = {n for n in changed if n.endswith('.class')}
        assert changed_classes <= CLASS_NAMES, changed_classes - CLASS_NAMES
        assert {'dev/guogaology/mining/EquipmentTooltips.class',
                'dev/guogaology/mining/EquipmentTooltipRules.class',
                f'dev/guogaology/client/mixin/{HOOK}.class'} <= changed_classes
        for name in [n for n in a if n.endswith('-refmap.json')]:
            left, right = json.loads(a[name]), json.loads(b[name])
            for source, destination in [(left['mappings'], right['mappings'])] + [
                    (values, right['data'][kind]) for kind, values in left.get('data', {}).items()]:
                assert all(destination.get(k) == v for k, v in source.items()), 'Previous mappings retained'
                expected = {f'dev/guogaology/client/mixin/{HOOK}'} if name.startswith('client-') else set()
                assert set(destination) - set(source) == expected
        assert json.loads(a['fabric.mod.json'].decode().replace(OLD, NEW)) == json.loads(b['fabric.mod.json'])
        for nested_name in [n for n in a if n.startswith('META-INF/jars/')]:
            with ZipFile(io.BytesIO(a[nested_name])) as x, ZipFile(io.BytesIO(b[nested_name.replace(OLD, NEW)])) as y:
                assert set(x.namelist()) == set(y.namelist())
                for name in x.namelist():
                    left, right = x.read(name), y.read(name)
                    if name == 'fabric.mod.json':
                        assert json.loads(left.decode().replace(OLD, NEW)) == json.loads(right)
                    else:
                        assert left == right, f'Unexpected nested bridge delta:{name}'
        results.append({'minecraft': target, 'success': True, 'changedEntries': changed,
                        'unchangedDataFiles': sum(n.startswith('data/') for n in resources),
                        'resourceIdsRetained': len(resources),
                        'baselineSha256': hashlib.sha256(old.read_bytes()).hexdigest(),
                        'sha256': hashlib.sha256(new.read_bytes()).hexdigest()})
    output = ROOT / 'build/tooltip-0615/package-delta.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({'success': True, 'targets': results,
                                  'scope': 'Display-only packaged delta; no world replay claimed.'}, indent=2) + '\n', 'utf8')
    print(f'TOOLTIP0615_PACKAGE_DELTA_OK targets={len(results)} output={output}')


if __name__ == '__main__':
    audit()
