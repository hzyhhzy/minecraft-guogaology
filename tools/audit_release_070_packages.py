"""Prove that the 0.7.0 release only renames the validated 0.6.16 version.

No class/resource allowlist: every non-metadata payload must match byte for
byte. ZIP container compression/timestamps may differ; JSON metadata is checked
both semantically and against the exact permitted version-byte substitution.
"""
from collections import Counter
from copy import deepcopy
from pathlib import Path, PurePosixPath
from zipfile import ZipFile
import hashlib
import io
import json
import struct


ROOT = Path(__file__).resolve().parents[1]
OLD, NEW = '0.6.16', '0.7.0'
TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')
CATALOG = 'data/guogaology/notation/catalog.json'
LICENSE = 'GPL-3.0-only WITH GPL-3.0-linking-exception'
OUTPUT = ROOT / 'build/release-070/package-delta.json'
EXPECTED_COUNTS = {1: 1, 2: 3, 3: 8, 4: 14, 5: 14,
                   **{size: 15 for size in range(6, 70)}}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def read_zip(source):
    raw = source.read_bytes() if isinstance(source, Path) else source
    with ZipFile(io.BytesIO(raw)) as archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        require(len(names) == len(set(names)), 'Duplicate ZIP entry names')
        entries, sizes = {}, {}
        for info in infos:
            if info.is_dir():
                continue
            name, parts = info.filename, PurePosixPath(info.filename).parts
            require(not name.startswith('/') and '..' not in parts,
                    f'Unsafe ZIP entry: {name}')
            lower = name.lower()
            forbidden_parts = {'tools', 'offline', 'qa', 'qa263', 'releaseqa263',
                               'mergeqa', 'connectorqa', 'worldqa', 'test',
                               'tests', 'gametest', 'testmod'}
            require(not forbidden_parts & {part.lower() for part in parts}
                    and not any(token in lower for token in
                                ('connector-qa', 'port-qa', 'visualqa', 'portalgroundchecks'))
                    and not lower.endswith(('.py', '.pyc', '.xlsx')),
                    f'Offline source/tool/QA entry shipped: {name}')
            entries[name] = archive.read(info)
            sizes[name] = {'rawBytes': info.file_size,
                           'compressedBytes': info.compress_size}
    return {'entries': entries, 'sizes': sizes, 'raw': raw}


def manifest_hash(entries, names):
    digest = hashlib.sha256()
    for name in sorted(names):
        digest.update(name.encode('utf8') + b'\0')
        digest.update(hashlib.sha256(entries[name]).digest())
    return digest.hexdigest()


def metadata_delta(before, after, label, allow_bridge_reference=False):
    old, new = json.loads(before), json.loads(after)
    require(old.get('version') == OLD and new.get('version') == NEW,
            f'{label}: unexpected mod version')
    expected = deepcopy(old)
    expected['version'] = NEW
    if allow_bridge_reference:
        for jar in expected.get('jars', []):
            jar['file'] = jar['file'].replace(f'-{OLD}.jar', f'-{NEW}.jar')
    require(expected == new, f'{label}: non-version metadata change')
    require(before.replace(OLD.encode(), NEW.encode()) == after,
            f'{label}: bytes differ beyond exact version substitution')
    require(new['license'] == LICENSE, f'{label}: stale or missing license')
    return {'semanticVersionOnly': True,
            'byteExactAfterVersionSubstitution': True,
            'beforeSha256': sha256(before), 'afterSha256': sha256(after)}


def class_checks(entries, java, label):
    names = {name for name in entries if name.endswith('.class')}
    require(names, f'{label}: no production classes')
    versions = Counter()
    for name in names:
        data = entries[name]
        require(len(data) >= 8 and data[:4] == b'\xca\xfe\xba\xbe',
                f'{label}: invalid class header: {name}')
        minor, major = struct.unpack('>HH', data[4:8])
        require(minor != 65535 and 45 <= major <= java + 44,
                f'{label}: unsupported or preview class version: {name}/{major}.{minor}')
        versions[major] += 1
    return {'count': len(names),
            'classMajorVersions': dict(sorted(versions.items())),
            'payloadManifestSha256': manifest_hash(entries, names)}


def embedded_delta(a, b, target):
    old_paths = {name for name in a if name.startswith('META-INF/jars/')}
    new_paths = {name for name in b if name.startswith('META-INF/jars/')}
    expected_old = ({f'META-INF/jars/guogaology-outer-legacy-{OLD}.jar'}
                    if target == '1.21.1' else set())
    expected_new = {name.replace(OLD, NEW) for name in expected_old}
    require(old_paths == expected_old and new_paths == expected_new,
            f'{target}: unexpected embedded bridge set')
    reports = []
    for old_path in sorted(old_paths):
        new_path = old_path.replace(OLD, NEW)
        before, after = read_zip(a[old_path]), read_zip(b[new_path])
        x, y = before['entries'], after['entries']
        require(x.keys() == y.keys(), f'{target}: bridge entry set changed')
        metadata = metadata_delta(x['fabric.mod.json'], y['fabric.mod.json'],
                                  f'{target}: embedded fabric.mod.json')
        unchanged = set(x) - {'fabric.mod.json'}
        for name in unchanged:
            require(x[name] == y[name], f'{target}: embedded payload changed: {name}')
        reports.append({'beforeEntry': old_path, 'afterEntry': new_path,
                        'beforeSha256': sha256(before['raw']),
                        'afterSha256': sha256(after['raw']),
                        'unchangedPayloads': len(unchanged),
                        'unchangedManifestSha256': manifest_hash(x, unchanged),
                        'classes': class_checks(y, 21, f'{target}: bridge'),
                        'metadata': metadata})
    return old_paths, new_paths, reports


def packaged_notices(entries, target):
    license_entry = ('LICENSE_guogaology-dimension-1.21.1'
                     if target == '1.21.1' else 'LICENSE')
    source_names = {'LICENSE': license_entry,
                    **{name: name for name in ('LICENSE-MINECRAFT-EXCEPTION',
                                              'COPYRIGHT', 'THIRD_PARTY.md')}}
    hashes = {}
    for source_name, entry in source_names.items():
        require(entries[entry] == (ROOT / source_name).read_bytes(),
                f'{target}: missing/stale license or credit: {entry}')
        hashes[entry] = sha256(entries[entry])
    require(b'Googology Dimension Team' in entries['THIRD_PARTY.md']
            and b'MIT' in entries['THIRD_PARTY.md'],
            f'{target}: existing donor attribution/license is missing')
    return hashes


def catalog_checks(payload):
    require(payload == (ROOT / 'src/main/resources' / CATALOG).read_bytes(),
            'Packaged catalog differs from formal source bytes')
    catalog = json.loads(payload)
    require(len(catalog['iblp']) == 1000 and len(catalog['bms']) == 1070,
            'Validated 1000 iBLP / 1070 BMS catalog changed')
    histogram = Counter(len(record['rows']) for record in catalog['iblp'])
    require(dict(histogram) == EXPECTED_COUNTS, 'iBLP row quotas changed')
    return {'iblpPatterns': 1000, 'bmsPatterns': 1070,
            'rowSizeQuotas': dict(sorted(histogram.items())),
            'rawBytes': len(payload), 'sha256': sha256(payload),
            'formalSourceByteIdentity': True}


def metrics(archive):
    sizes = archive['sizes'][CATALOG]
    total = len(archive['raw'])
    return {'jarBytes': total, 'jarSha256': sha256(archive['raw']),
            'catalogRawBytes': sizes['rawBytes'],
            'catalogCompressedBytes': sizes['compressedBytes'],
            'catalogCompressedShareOfJar': sizes['compressedBytes'] / total,
            'catalogCompressedShareOfJarPercent': 100 * sizes['compressedBytes'] / total}


def release_manifest(version):
    directory = ROOT / f'build/releases/{version}'
    rows = json.loads((directory / 'manifest.json').read_bytes())
    require(len(rows) == len(TARGETS) and {row['minecraft'] for row in rows} == set(TARGETS),
            f'{version}: release manifest must contain exactly four targets')
    checksums = {}
    for line in (directory / 'SHA256SUMS.txt').read_text('utf8').splitlines():
        if line.strip():
            digest, filename = line.split(maxsplit=1)
            require(filename not in checksums, f'{version}: duplicate checksum entry')
            checksums[filename] = digest
    return {row['minecraft']: row for row in rows}, checksums


def audit():
    manifests = {version: release_manifest(version) for version in (OLD, NEW)}
    results = []
    for target in TARGETS:
        old_path = ROOT / f'build/releases/{OLD}/guogaology-dimension-{target}-{OLD}.jar'
        new_path = ROOT / f'build/releases/{NEW}/guogaology-dimension-{target}-{NEW}.jar'
        before, after = read_zip(old_path), read_zip(new_path)
        a, b = before['entries'], after['entries']
        old_nested, new_nested, bridges = embedded_delta(a, b, target)
        require((set(a) - old_nested) | new_nested == set(b),
                f'{target}: production entry set changed')
        metadata = metadata_delta(a['fabric.mod.json'], b['fabric.mod.json'],
                                  f'{target}: fabric.mod.json', True)
        unchanged = set(a) - old_nested - {'fabric.mod.json'}
        for name in sorted(unchanged):
            require(a[name] == b[name], f'{target}: non-version payload changed: {name}')
        java = 21 if target.startswith('1.21.') else 25
        mod = json.loads(b['fabric.mod.json'])
        require(mod['id'] == 'guogaology' and mod['depends']['minecraft'] == target
                and mod['depends']['java'] == f'>={java}'
                and mod['name'] == '果糕逻辑 · Guogaology',
                f'{target}: wrong target, Java requirement, or brand')
        require(b['dev/guogaology/GuogaologyMod.class'][6:8] == struct.pack('>H', java + 44),
                f'{target}: entrypoint must be compiled for Java {java}')
        classes = class_checks(b, java, target)
        resources = {name for name in a if name.startswith(('assets/', 'data/'))}
        require(resources <= unchanged, f'{target}: all assets/data must be byte-identical')
        mixins = {name for name in a
                  if name.endswith(('mixins.json', '-refmap.json'))}
        require(mixins <= unchanged, f'{target}: all mixins/refmaps must be byte-identical')
        notices = packaged_notices(b, target)
        catalog = catalog_checks(b[CATALOG])
        for version, path, archive in ((OLD, old_path, before), (NEW, new_path, after)):
            rows, checksums = manifests[version]
            row, info = rows[target], metrics(archive)
            metadata_for_manifest = json.loads(archive['entries']['fabric.mod.json'])
            require(row['mod'] == version and row['file'] == path.name
                    and row['bytes'] == info['jarBytes']
                    and row['sha256'] == info['jarSha256']
                    and row['java'] == java and row['license'] == LICENSE
                    and row['requires'] == metadata_for_manifest['depends']
                    and checksums.get(path.name) == info['jarSha256'],
                    f'{version}/{target}: release manifest/checksum mismatch')
        results.append({'minecraft': target, 'java': java, 'success': True,
                        'beforePath': str(old_path.relative_to(ROOT)),
                        'afterPath': str(new_path.relative_to(ROOT)),
                        'before': metrics(before), 'after': metrics(after),
                        'classes': classes, 'unchangedAssetDataEntries': len(resources),
                        'unchangedMixinRefmapEntries': sorted(mixins),
                        'unchangedPayloadEntries': len(unchanged),
                        'unchangedPayloadManifestSha256': manifest_hash(a, unchanged),
                        'onlyChangedEntries': ['fabric.mod.json'] + sorted(old_nested | new_nested),
                        'fabricMetadata': metadata, 'embeddedBridges': bridges,
                        'catalog': catalog, 'noticeSha256': notices,
                        'qaOrOfflineEntries': [], 'releaseManifestAndChecksumsMatch': True})
    report = {'success': True, 'baselineVersion': OLD, 'releaseVersion': NEW,
              'scope': 'Version-only package identity; inherits validated 0.6.16 gameplay without claiming a new native/world test.',
              'byteProof': 'Every production class, asset/data entry including catalog, mixin/refmap, manifest and other non-fabric payload is byte-identical; only fabric version and declared embedded-bridge version filename references change.',
              'targets': results}
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f'RELEASE070_PACKAGE_DELTA_OK targets={len(results)} output={OUTPUT}')


if __name__ == '__main__':
    audit()
