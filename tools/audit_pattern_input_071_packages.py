"""Bound the 0.7.1 pattern-window input repair against exact published 0.7.0.

Only EnhancementScreen.class, exact version metadata substitutions, and exact
CRLF-to-LF substitutions in ten named vanilla wood tags may change. ZIP
timestamps/compression are immaterial; every other payload is compared byte
for byte, including the nested 1.21.1 legacy bridge classes.
This is a package-scope proof, not native input or old-save validation.
"""
from copy import deepcopy
from pathlib import Path
import json
import struct

from audit_release_070_packages import (
    catalog_checks, class_checks, manifest_hash, metrics, packaged_notices,
    read_zip, release_manifest, require, sha256,
)


ROOT = Path(__file__).resolve().parents[1]
OLD, NEW = '0.7.0', '0.7.1'
TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')
SCREEN = 'dev/guogaology/client/EnhancementScreen.class'
LICENSE = 'GPL-3.0-only WITH GPL-3.0-linking-exception'
OUTPUT = ROOT / 'build/release-071/package-delta.json'
LINE_ENDING_ONLY_PATHS = {
    f'data/minecraft/tags/{kind}/{tag}.json'
    for kind in ('block', 'item')
    for tag in ('fence_gates', 'wooden_buttons', 'wooden_doors',
                'wooden_pressure_plates', 'wooden_trapdoors')
}
# These are the exact published binaries, not a subsequently rebuilt baseline.
BASELINE_SHA256 = {
    '1.21.1': '01b54fe68184e499ed4be3709c1af5ede56ddbc18ff29945006c5c4d1cd354d6',
    '1.21.11': '9cd502f96deda992b179bf84860b07435bdbf1b05b58937582e560c97a3ae135',
    '26.2': '4b325a23581c1158eb0ffd872c13a2ec96fdac777ecf6a9671d2452267339764',
    '26.3': '17bd1e1fbc631ebcbab29711d842d9083952d3e01be1baacf36cccab734248bf',
}


def artifact_paths(target, version):
    """Prefer staged release JARs; allow auditing immediately after four builds."""
    name = f'guogaology-dimension-{target}-{version}.jar'
    project = ROOT if target == '1.21.1' else ROOT / 'ports' / target
    return (ROOT / 'build/releases' / version / name,
            project / 'build/libs' / name)


def artifact(target, version):
    candidates = artifact_paths(target, version)
    present = [path for path in candidates if path.is_file()]
    require(present, f'{target}/{version}: no exact-version production JAR found')
    selected = present[0]
    digest = sha256(selected.read_bytes())
    for path in present:
        require(sha256(path.read_bytes()) == digest,
                f'{target}/{version}: staged and build JARs disagree: {path}')
    if version == OLD:
        require(digest == BASELINE_SHA256[target],
                f'{target}: baseline is not the exact published {OLD} binary')
    return selected


def metadata_delta(before, after, label, bridge_reference=False):
    old, new = json.loads(before), json.loads(after)
    require(old.get('version') == OLD and new.get('version') == NEW,
            f'{label}: unexpected mod version')
    expected = deepcopy(old)
    expected['version'] = NEW
    if bridge_reference:
        for jar in expected.get('jars', []):
            jar['file'] = jar['file'].replace(f'-{OLD}.jar', f'-{NEW}.jar')
    require(new == expected, f'{label}: metadata changed beyond version')
    require(after == before.replace(OLD.encode(), NEW.encode()),
            f'{label}: bytes changed beyond exact version substitution')
    require(new.get('license') == LICENSE, f'{label}: missing or stale license')
    return {'semanticVersionOnly': True,
            'byteExactAfterVersionSubstitution': True,
            'beforeSha256': sha256(before), 'afterSha256': sha256(after)}


def line_ending_only_delta(a, b, target):
    require(LINE_ENDING_ONLY_PATHS <= a.keys() and LINE_ENDING_ONLY_PATHS <= b.keys(),
            f'{target}: named vanilla wood tag entry is missing')
    changes = {}
    for name in sorted(LINE_ENDING_ONLY_PATHS):
        if a[name] == b[name]:
            continue
        require(a[name] != b[name] and a[name].replace(b'\r\n', b'\n') == b[name],
                f'{target}: wood tag changed beyond exact CRLF-to-LF substitution: {name}')
        changes[name] = {'beforeSha256': sha256(a[name]),
                         'afterSha256': sha256(b[name]),
                         'exactCrlfToLfSubstitution': True}
    return changes


def embedded_delta(a, b, target):
    old_paths = {name for name in a if name.startswith('META-INF/jars/')}
    new_paths = {name for name in b if name.startswith('META-INF/jars/')}
    expected_old = ({f'META-INF/jars/guogaology-outer-legacy-{OLD}.jar'}
                    if target == '1.21.1' else set())
    expected_new = {name.replace(OLD, NEW) for name in expected_old}
    require(old_paths == expected_old and new_paths == expected_new,
            f'{target}: unexpected embedded dependency set')
    reports = []
    for old_path in sorted(old_paths):
        new_path = old_path.replace(OLD, NEW)
        before, after = read_zip(a[old_path]), read_zip(b[new_path])
        x, y = before['entries'], after['entries']
        require(x.keys() == y.keys(), f'{target}: legacy bridge entry set changed')
        metadata = metadata_delta(x['fabric.mod.json'], y['fabric.mod.json'],
                                  f'{target}: legacy bridge fabric.mod.json')
        unchanged = set(x) - {'fabric.mod.json'}
        for name in sorted(unchanged):
            require(x[name] == y[name],
                    f'{target}: legacy bridge non-version payload changed: {name}')
        mod = json.loads(y['fabric.mod.json'])
        require(mod['depends']['minecraft'] == target,
                f'{target}: legacy bridge Minecraft dependency changed')
        reports.append({'beforeEntry': old_path, 'afterEntry': new_path,
                        'beforeSha256': sha256(before['raw']),
                        'afterSha256': sha256(after['raw']),
                        'unchangedPayloadEntries': len(unchanged),
                        'unchangedPayloadManifestSha256': manifest_hash(x, unchanged),
                        'classes': class_checks(y, 21, f'{target}: legacy bridge'),
                        'changedClasses': [], 'fabricMetadata': metadata})
    return old_paths, new_paths, reports


def staged_manifests(version):
    directory = ROOT / 'build/releases' / version
    exists = [(directory / name).is_file()
              for name in ('manifest.json', 'SHA256SUMS.txt')]
    staged_jars = any(artifact_paths(target, version)[0].is_file()
                      for target in TARGETS)
    require(not any(exists) or all(exists),
            f'{version}: incomplete staged release manifest/checksums')
    require(not staged_jars or all(exists),
            f'{version}: staged release JARs lack manifest/checksums')
    return release_manifest(version) if all(exists) else None


def verify_manifest(manifests, version, target, path, archive, java):
    if manifests is None:
        return False
    rows, checksums = manifests
    row, info = rows[target], metrics(archive)
    mod = json.loads(archive['entries']['fabric.mod.json'])
    require(row['mod'] == version and row['file'] == path.name
            and row['bytes'] == info['jarBytes']
            and row['sha256'] == info['jarSha256']
            and row['java'] == java and row['license'] == LICENSE
            and row['requires'] == mod['depends']
            and checksums.get(path.name) == info['jarSha256'],
            f'{version}/{target}: release manifest/checksum mismatch')
    return True


def audit():
    manifests = {version: staged_manifests(version) for version in (OLD, NEW)}
    results = []
    for target in TARGETS:
        old_path, new_path = (artifact(target, version) for version in (OLD, NEW))
        before, after = read_zip(old_path), read_zip(new_path)
        a, b = before['entries'], after['entries']
        old_nested, new_nested, bridges = embedded_delta(a, b, target)
        require((set(a) - old_nested) | new_nested == set(b),
                f'{target}: production entry set changed beyond bridge version path')
        metadata = metadata_delta(a['fabric.mod.json'], b['fabric.mod.json'],
                                  f'{target}: fabric.mod.json', True)
        line_endings = line_ending_only_delta(a, b, target)
        unchanged = set(a) - old_nested - {'fabric.mod.json', SCREEN} - set(line_endings)
        require(SCREEN in a and SCREEN in b and a[SCREEN] != b[SCREEN],
                f'{target}: expected pattern-window input repair is absent')
        for name in sorted(unchanged):
            require(a[name] == b[name],
                    f'{target}: payload changed outside input repair: {name}')
        changed_classes = {name for name in set(a) | set(b)
                           if name.endswith('.class') and a.get(name) != b.get(name)}
        require(changed_classes == {SCREEN},
                f'{target}: unexpected production class delta: {sorted(changed_classes)}')
        java = 21 if target.startswith('1.21.') else 25
        mod = json.loads(b['fabric.mod.json'])
        require(mod['id'] == 'guogaology' and mod['depends']['minecraft'] == target
                and mod['depends']['java'] == f'>={java}'
                and mod['name'] == '果糕逻辑 · Guogaology',
                f'{target}: wrong saved namespace, target, Java requirement, or brand')
        classes = class_checks(b, java, target)
        own_classes = {name for name in b
                       if name.startswith('dev/guogaology/') and name.endswith('.class')}
        for name in sorted(own_classes):
            require(b[name][6:8] == struct.pack('>H', java + 44),
                    f'{target}: production class is not compiled for Java {java}: {name}')
        resources = {name for name in a if name.startswith(('assets/', 'data/'))}
        mixins = {name for name in a if name.endswith(('mixins.json', '-refmap.json'))}
        require(resources | mixins <= unchanged | set(line_endings),
                f'{target}: resource, language, data, mixin, or saved-ID payload changed')
        unchanged_resources = resources - set(line_endings)
        notices = packaged_notices(b, target)
        catalog = catalog_checks(b['data/guogaology/notation/catalog.json'])
        manifest_status = {
            version: verify_manifest(manifests[version], version, target,
                                     path, archive, java)
            for version, path, archive in ((OLD, old_path, before), (NEW, new_path, after))
        }
        results.append({'minecraft': target, 'java': java, 'success': True,
                        'beforePath': str(old_path.relative_to(ROOT)),
                        'afterPath': str(new_path.relative_to(ROOT)),
                        'before': metrics(before), 'after': metrics(after),
                        'exactPublishedBaselineSha256': BASELINE_SHA256[target],
                        'changedClasses': sorted(changed_classes),
                        'screenBeforeSha256': sha256(a[SCREEN]),
                        'screenAfterSha256': sha256(b[SCREEN]),
                        'unchangedClassEntries': len(own_classes - {SCREEN}),
                        'unchangedClassManifestSha256': manifest_hash(a, own_classes - {SCREEN}),
                        'classes': classes,
                        'preservedAssetDataEntries': len(resources),
                        'unchangedAssetDataEntries': len(unchanged_resources),
                        'unchangedAssetDataManifestSha256': manifest_hash(a, unchanged_resources),
                        'unchangedMixinRefmapEntries': sorted(mixins),
                        'unchangedPayloadEntries': len(unchanged),
                        'unchangedPayloadManifestSha256': manifest_hash(a, unchanged),
                        'onlyChangedEntries': ['fabric.mod.json', SCREEN] + sorted(old_nested | new_nested | set(line_endings)),
                        'lineEndingOnlyEntries': sorted(line_endings),
                        'lineEndingOnlySha256': line_endings,
                        'fabricMetadata': metadata, 'embeddedBridges': bridges,
                        'catalog': catalog, 'noticeSha256': notices,
                        'qaOrOfflineEntries': [],
                        'releaseManifestAndChecksumsVerified': manifest_status})
    report = {'success': True, 'baselineVersion': OLD, 'releaseVersion': NEW,
              'scope': 'Static package delta for the pattern-window client input repair, exact version metadata, and CRLF-to-LF-only changes in ten explicitly named vanilla wood tag files; native input, ownership and old-save validation are separate evidence.',
              'allowedChangedClasses': [SCREEN],
              'allowedLineEndingOnlyEntries': sorted(LINE_ENDING_ONLY_PATHS),
              'byteProof': 'Only EnhancementScreen.class, exact fabric version substitutions, and exact CRLF-to-LF substitutions in the ten named vanilla wood tag files change. The tag contents and ordering are retained. All other classes, assets, data, languages, IDs, mixins, notices and nested legacy-bridge payloads are byte-identical to the exact published 0.7.0 binaries.',
              'targets': results}
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f'PATTERN_INPUT071_PACKAGE_DELTA_OK targets={len(results)} output={OUTPUT}')


if __name__ == '__main__':
    audit()
