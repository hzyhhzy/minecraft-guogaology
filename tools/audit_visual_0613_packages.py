"""Bound the visual repair delta against the exact local0.6.12 packages."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import io
import json

ROOT = Path(__file__).resolve().parents[1]
OLD, NEW = '0.6.12', '0.6.13'
TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')


def content(archive):
    return {n: archive.read(n) for n in archive.namelist() if not n.endswith('/')}


def semantic_equal(name, a, b):
    if name.endswith('.json'):
        return json.loads(a) == json.loads(b)
    return a == b


def audit():
    results = []
    allowed_art = {'assets/guogaology/models/block/guogao_lamp_shaper.json'}
    allowed_art |= {f'assets/guogaology/textures/item/{tier}_bow{suffix}.png'
                    for tier in ('omega', 'epsilon', 'gamma', 'true_omega')
                    for suffix in ('', '_pulling_0', '_pulling_1', '_pulling_2')}
    for target in TARGETS:
        old = ROOT / f'build/releases/{OLD}/guogaology-dimension-{target}-{OLD}.jar'
        new = ROOT / f'build/releases/{NEW}/guogaology-dimension-{target}-{NEW}.jar'
        with ZipFile(old) as archive:
            a = content(archive)
        with ZipFile(new) as archive:
            b = content(archive)
        resources = {n for n in a.keys() | b.keys() if n.startswith(('assets/', 'data/'))}
        assert resources <= a.keys() and resources <= b.keys(), 'Existing resource IDs retained'
        changed = sorted(n for n in a.keys() | b.keys() if a.get(n) != b.get(n))
        for name in resources - allowed_art:
            if name in ('assets/guogaology/lang/zh_cn.json', 'assets/guogaology/lang/en_us.json'):
                before, after = json.loads(a[name]), json.loads(b[name])
                assert before.pop('mining.guogaology.sneak')
                assert before == after, 'Only the requested tooltip text is retired'
            else:
                assert semantic_equal(name, a[name], b[name]), name
        for name in ('guogaology.mixins.json',):
            assert json.loads(a[name]) == json.loads(b[name]), 'Server mechanics/mixins unchanged'
        mixin = 'OrdinalBowHandSelectionMixin'
        before_mixins, after_mixins = (json.loads(x['guogaology.client.mixins.json']) for x in (a, b))
        assert after_mixins['client'].count(mixin) == 1
        after_mixins['client'].remove(mixin)
        assert before_mixins == after_mixins, 'All previous client hooks retained'
        hook = f'dev/guogaology/client/mixin/{mixin}.class'
        assert hook not in a and hook in b
        assert {n for n in changed if n.endswith('.class')} == {
            'dev/guogaology/mining/OrdinalGear.class', hook}, 'No balance or save code changed'
        for name in [n for n in a if n.endswith('-refmap.json')]:
            left, right = json.loads(a[name]), json.loads(b[name])
            for source, destination in [(left['mappings'], right['mappings'])] + [
                    (values, right['data'][kind]) for kind, values in left.get('data', {}).items()]:
                assert all(destination.get(k) == v for k, v in source.items()), 'Existing Mixin mappings retained'
                expected = {f'dev/guogaology/client/mixin/{mixin}'} if name.startswith('client-') else set()
                assert set(destination) - set(source) == expected
        before, after = json.loads(a['fabric.mod.json']), json.loads(b['fabric.mod.json'])
        assert json.loads(json.dumps(before).replace(OLD, NEW)) == after
        for nested_name in [n for n in a if n.startswith('META-INF/jars/')]:
            new_name = nested_name.replace(OLD, NEW)
            with ZipFile(io.BytesIO(a[nested_name])) as x, ZipFile(io.BytesIO(b[new_name])) as y:
                assert set(x.namelist()) == set(y.namelist())
                for name in x.namelist():
                    left, right = x.read(name), y.read(name)
                    if name == 'fabric.mod.json':
                        assert json.loads(left.decode().replace(OLD, NEW)) == json.loads(right)
                    else:
                        assert left == right, f'Unexpected nested legacy change:{name}'
        results.append({'minecraft': target, 'success': True,
                        'sha256': hashlib.sha256(new.read_bytes()).hexdigest(),
                        'baselineSha256': hashlib.sha256(old.read_bytes()).hexdigest(),
                        'changedEntries': changed,
                        'unchangedDataFiles': sum(n.startswith('data/') for n in resources),
                        'resourceIdsRetained': len(resources)})
    output = ROOT / 'build/visual-0613/package-delta.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({'success': True, 'targets': results,
                                  'scope': 'Packaged asset/data/ID delta, not a complete old-world replay.'},
                                 indent=2) + '\n', 'utf8')
    print(f'VISUAL0613_PACKAGE_DELTA_OK targets={len(results)} output={output}')


if __name__ == '__main__':
    audit()
