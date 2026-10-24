"""Compare unchanged save/resource surfaces with the exact released0.6.11 packages."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import io
import json

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def manifest(data):
    result = {}
    key = None
    for line in data.decode().replace('\r', '').splitlines():
        if line.startswith(' ') and key:
            result[key] += line[1:]
        elif ': ' in line:
            key, value = line.split(': ', 1)
            result[key] = value
    return result


def check_metadata_delta(name, old, new):
    if name == 'META-INF/MANIFEST.MF':
        a, b = manifest(old), manifest(new)
        key = 'Fabric-Loom-Client-Only-Entries'
        before, after = set(a.pop(key, '').split(';')), set(b.pop(key, '').split(';'))
        assert a == b and before <= after
        assert after - before <= {'dev/guogaology/client/MiningPatternPreview.class',
                                  'dev/guogaology/client/MiningPatternPreview$Outline.class'}
    elif name.endswith('-refmap.json'):
        a, b = json.loads(old), json.loads(new)
        for mappings, new_mappings in [(a['mappings'], b['mappings'])] + [
                (v, b['data'][k]) for k, v in a.get('data', {}).items()]:
            assert all(new_mappings.get(k) == v for k, v in mappings.items())
            assert set(new_mappings) - set(mappings) == {'dev/guogaology/mixin/MiningPatternStartMixin'}
    else:
        assert json.loads(old.decode().replace('0.6.11', '0.6.12')) == json.loads(new)


def check_nested_legacy(a, b):
    old_name = 'META-INF/jars/guogaology-outer-legacy-0.6.11.jar'
    new_name = 'META-INF/jars/guogaology-outer-legacy-0.6.12.jar'
    if old_name not in a:
        return set()
    with ZipFile(io.BytesIO(a[old_name])) as old, ZipFile(io.BytesIO(b[new_name])) as new:
        assert set(old.namelist()) == set(new.namelist())
        for name in old.namelist():
            if old.read(name) != new.read(name):
                assert name in {'fabric.mod.json', 'META-INF/MANIFEST.MF'}, name
                check_metadata_delta(name, old.read(name), new.read(name))
    return {old_name, new_name}


def audit():
    results = []
    allowed = {'fabric.mod.json', 'guogaology.mixins.json',
               'assets/guogaology/lang/zh_cn.json', 'assets/guogaology/lang/en_us.json'}
    for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
        old = ROOT / f'build/releases/0.6.11/guogaology-dimension-{target}-0.6.11.jar'
        new = ROOT / f'build/releases/0.6.12/guogaology-dimension-{target}-0.6.12.jar'
        with ZipFile(old) as before, ZipFile(new) as after:
            a = {n: before.read(n) for n in before.namelist() if not n.endswith('/')}
            b = {n: after.read(n) for n in after.namelist() if not n.endswith('/')}
        resources = {n for n in a.keys() | b.keys() if n.startswith(('assets/', 'data/'))}
        changed = sorted(n for n in a.keys() | b.keys() if a.get(n) != b.get(n))
        format_only = {n for n in changed if n in resources and n.endswith('.json') and
                       n in a and n in b and json.loads(a[n]) == json.loads(b[n])}
        nested = check_nested_legacy(a, b)
        metadata = {n for n in changed if n == 'META-INF/MANIFEST.MF' or n.endswith('-refmap.json')}
        for name in metadata:
            check_metadata_delta(name, a[name], b[name])
        check_metadata_delta('fabric.mod.json', a['fabric.mod.json'], b['fabric.mod.json'])
        unexpected = [n for n in changed if not n.endswith('.class') and n not in allowed | format_only | nested | metadata]
        assert not unexpected, f'{target}: unexpected non-class delta: {unexpected}'
        assert {n for n in resources if n not in a or n not in b} == set(), 'Existing IDs/resources stay'
        assert json.loads(b['fabric.mod.json'])['version'] == '0.6.12'
        added_mixins = set(json.loads(b['guogaology.mixins.json'])['mixins']) - set(json.loads(a['guogaology.mixins.json'])['mixins'])
        assert added_mixins == {'MiningPatternStartMixin'}
        assert {n for n in changed if n.startswith('data/')} <= format_only, 'Worldgen, loot, tags, recipes and advancement IDs stay semantically identical'
        results.append({'minecraft': target, 'success': True, 'sha256': sha(new),
                        'baselineSha256': sha(old), 'byteIdenticalResources': sum(a[n] == b[n] for n in resources),
                        'formatOnlyJsonResources': sorted(format_only),
                        'semanticResourceChanges': [n for n in changed if n in resources and n not in format_only],
                        'changedClasses': sum(n.endswith('.class') for n in changed),
                        'extraMixin': sorted(added_mixins)})
    output = ROOT / 'build/mining-0612/save-surface.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({'success': True, 'targets': results,
                                 'scope': 'Static packaged resource/ID comparison, not a complete old-world loading certification.'}, indent=2) + '\n', 'utf8')
    print(json.dumps({'success': True, 'targets': len(results), 'output': str(output)}, indent=2))


if __name__ == '__main__':
    audit()
