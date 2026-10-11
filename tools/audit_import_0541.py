"""Compare a rebuilt 26.3 archive with the reviewed, optional 0.5.4.1 reference.

The reference is never needed to build or run the Mod. JSON formatting and NBT
compression are ignored; every other non-class resource must be accounted for.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

REFERENCE_SHA256 = '056f959d2fa0c60a7686075fe9b971b78655fb7bb9f64eaf4cec96bc5e17b18d'


def normalized(name, data):
    if name.endswith('.nbt'):
        return gzip.decompress(data)
    if name.endswith('.json'):
        result = json.loads(data)
        if name == 'guogaology/block_balance.json':
            # The reference's developer notes incorrectly claimed colored loot.
            result['gummy_jar'].pop('drop', None)
            result['gummy_jar'].pop('use', None)
        if name == 'assets/guogaology/items/gummy_jar.json':
            # The common item parent is equivalent to its direct block parent.
            if result['model']['model'] == 'guogaology:item/gummy_jar':
                result['model']['model'] = 'guogaology:block/gummy_jar'
        return result
    return data


def audit(reference, rebuilt):
    assert hashlib.sha256(reference.read_bytes()).hexdigest() == REFERENCE_SHA256, 'Unreviewed reference JAR'
    with ZipFile(reference) as source, ZipFile(rebuilt) as target:
        def resources(z):
            return {n for n in z.namelist() if not n.endswith(('/', '.class')) and n != 'META-INF/MANIFEST.MF'}
        a, b = resources(source), resources(target)
        missing, extra = sorted(a-b), sorted(b-a)
        differences = [n for n in sorted(a & b) if normalized(n, source.read(n)) != normalized(n, target.read(n))]
        result = {'reference_sha256': REFERENCE_SHA256, 'rebuilt_sha256': hashlib.sha256(rebuilt.read_bytes()).hexdigest(),
                  'compared_resources': len(a & b), 'missing': missing, 'extra': extra, 'differences': differences}
        assert not (missing or extra or differences), result
        # Validate the equivalent indirection rather than blindly exempting it.
        assert json.loads(target.read('assets/guogaology/models/item/gummy_jar.json')) == {'parent': 'guogaology:block/gummy_jar'}
        return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reference', type=Path)
    parser.add_argument('rebuilt', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    report = audit(args.reference, args.rebuilt)
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text, encoding='utf8')
    print(text)
