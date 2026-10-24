"""Prove that 0.6.16 only trims the shipped iBLP catalog and bumps versions.

This is a byte-level release guard, not a Java source allowlist or a world test.
JSON membership/provenance checks are reported separately from packaged-byte
identity. ZIP timestamps/compression are not required to be identical.
"""
from collections import Counter
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from zipfile import ZipFile
import hashlib
import gzip
import io
import json


ROOT = Path(__file__).resolve().parents[1]
OLD, NEW = '0.6.15', '0.6.16'
TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')
CATALOG = 'data/guogaology/notation/catalog.json'
SOURCE = ROOT / 'src/main/resources' / CATALOG
OUTPUT = ROOT / 'build/iblp-0616/package-delta.json'
EXPECTED_COUNTS = {1: 1, 2: 3, 3: 8, 4: 14, 5: 14,
                   **{size: 15 for size in range(6, 70)}}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    """Strict record key: ignore object-key order, retain types and list order."""
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(',', ':')).encode('utf8')


def top_value_bytes(blob, field):
    """Extract a top-level JSON value without reformatting its UTF-8 bytes."""
    text, decoder = blob.decode('utf8'), json.JSONDecoder()
    position = text.index('{') + 1
    while True:
        while text[position] in ' \r\n\t,':
            position += 1
        require(text[position] != '}', f'Missing top-level JSON field: {field}')
        key, position = decoder.raw_decode(text, position)
        while text[position] in ' \r\n\t':
            position += 1
        require(text[position] == ':', 'Invalid JSON object separator')
        position += 1
        while text[position] in ' \r\n\t':
            position += 1
        start = position
        _, position = decoder.raw_decode(text, position)
        if key == field:
            return text[start:position].encode('utf8')


@dataclass
class Archive:
    entries: dict
    sizes: dict
    raw_bytes: bytes


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
            name = info.filename
            parts = PurePosixPath(name).parts
            require(not name.startswith('/') and '..' not in parts,
                    f'Unsafe ZIP entry: {name}')
            require(not {'tools', 'offline'} & {part.lower() for part in parts}
                    and not name.lower().endswith(('.py', '.pyc', '.xlsx')),
                    f'Offline source/tool accidentally shipped: {name}')
            entries[name] = archive.read(info)
            sizes[name] = {'rawBytes': info.file_size,
                           'compressedBytes': info.compress_size}
    return Archive(entries, sizes, raw)


def manifest_hash(entries, names):
    digest = hashlib.sha256()
    for name in sorted(names):
        digest.update(name.encode('utf8') + b'\0')
        digest.update(hashlib.sha256(entries[name]).digest())
    return digest.hexdigest()


def version_metadata(before, after, label):
    old, new = json.loads(before), json.loads(after)
    require(old.get('version') == OLD and new.get('version') == NEW,
            f'{label}: exact version expected')
    replaced = before.replace(OLD.encode('ascii'), NEW.encode('ascii'))
    require(json.loads(replaced) == new,
            f'{label}: metadata changed beyond version substitution')
    # This stronger condition records formatting/content identity as well as
    # semantic identity; it does not silently accept a reformatted resource.
    require(replaced == after,
            f'{label}: bytes differ after the permitted version substitution')
    return {'semanticVersionSubstitutionOnly': True,
            'byteExactAfterVersionSubstitution': True,
            'beforeSha256': sha256(before), 'afterSha256': sha256(after)}


def nested_bridges(before, after, target):
    a, b = before.entries, after.entries
    old_names = {name for name in a if name.startswith('META-INF/jars/')}
    new_names = {name for name in b if name.startswith('META-INF/jars/')}
    expected_old = ({f'META-INF/jars/guogaology-outer-legacy-{OLD}.jar'}
                    if target == '1.21.1' else set())
    expected_new = {name.replace(OLD, NEW) for name in expected_old}
    require(old_names == expected_old and new_names == expected_new,
            f'{target}: unexpected embedded JAR set')
    reports = []
    for old_name in sorted(old_names):
        new_name = old_name.replace(OLD, NEW)
        x, y = read_zip(a[old_name]), read_zip(b[new_name])
        require(x.entries.keys() == y.entries.keys(),
                f'{target}: embedded bridge entry IDs changed')
        require(CATALOG not in y.entries,
                f'{target}: duplicate catalog inside legacy bridge')
        meta = version_metadata(x.entries['fabric.mod.json'],
                                y.entries['fabric.mod.json'],
                                f'{target}: embedded fabric.mod.json')
        unchanged = set(x.entries) - {'fabric.mod.json'}
        for name in unchanged:
            require(x.entries[name] == y.entries[name],
                    f'{target}: embedded bridge bytes changed: {name}')
        classes = {name for name in x.entries if name.endswith('.class')}
        reports.append({'beforeEntry': old_name, 'afterEntry': new_name,
                        'beforeSha256': sha256(x.raw_bytes),
                        'afterSha256': sha256(y.raw_bytes),
                        'unchangedEntries': len(unchanged),
                        'unchangedClasses': len(classes),
                        'classManifestSha256': manifest_hash(x.entries, classes),
                        'metadata': meta})
    return old_names, new_names, reports


def catalog_checks(before_bytes, after_bytes):
    old, new = json.loads(before_bytes), json.loads(after_bytes)
    require(old.keys() == new.keys() == {'metadata', 'bms', 'iblp'},
            'Catalog top-level fields changed')
    require(len(old['bms']) == len(new['bms']) == 1070
            and canonical(old['bms']) == canonical(new['bms']),
            'All 1070 BMS full records must be unchanged and ordered')
    old_bms, new_bms = (top_value_bytes(blob, 'bms')
                        for blob in (before_bytes, after_bytes))
    require(old_bms == new_bms, 'BMS JSON subtree bytes must remain identical')
    previous, selected = old['iblp'], new['iblp']
    require(len(previous) == 14661 and len(selected) == 1000,
            'iBLP parent/selected counts must be 14661/1000')
    keys = [canonical(record) for record in previous]
    require(len(set(keys)) == len(keys), 'Parent iBLP has duplicate full records')
    indices_by_record = {key: index for index, key in enumerate(keys)}
    selected_keys = [canonical(record) for record in selected]
    require(len(set(selected_keys)) == len(selected_keys),
            'Selected iBLP contains duplicate records')
    require(all(key in indices_by_record for key in selected_keys),
            'A retained record differs from or is absent in the parent catalog')
    indices = [indices_by_record[key] for key in selected_keys]
    require(indices == sorted(indices) and len(set(indices)) == 1000,
            'Selected full records must retain parent relative order')
    histogram = Counter(len(record['rows']) for record in selected)
    require(dict(histogram) == EXPECTED_COUNTS,
            f'Unexpected 1..69 row quotas: {dict(sorted(histogram.items()))}')
    original_small = [canonical(record) for record in previous
                      if len(record['rows']) <= 3]
    retained_small = [canonical(record) for record in selected
                      if len(record['rows']) <= 3]
    require(retained_small == original_small and len(retained_small) == 12,
            'All original 12 small patterns must remain exactly')
    require(sum(count for size, count in histogram.items() if size >= 4) == 988,
            'Wild 4..69-row pattern quota must be 988')
    first_by_size = {}
    for index, record in enumerate(previous):
        first_by_size.setdefault(len(record['rows']), index)
    require(set(first_by_size.values()) <= set(indices),
            'First mother-pool record of every row size must remain')
    maximum_step = max(max(record['steps']) for record in previous)
    maximum_step_index = next(index for index, record in enumerate(previous)
                              if max(record['steps']) == maximum_step)
    require(maximum_step_index in indices,
            'Earliest global maximum-step record must remain')
    provenance = provenance_checks(old['metadata'], new['metadata'],
                                   before_bytes, selected)
    require(new['metadata']['selection']['maximumStep'] == maximum_step
            and new['metadata']['selection']['maximumStepSourceIndex'] == maximum_step_index,
            'Maximum-step provenance differs from mother-pool calculation')
    return {'semanticChecks': {'parentIblpPatterns': len(previous),
                               'retainedIblpPatterns': len(selected),
                               'unchangedBmsRecords': len(new['bms']),
                               'exactParentRecordMembership': True,
                               'parentRelativeOrderPreserved': True,
                               'rowSizesCovered': sorted(histogram),
                               'rowSizeQuotas': dict(sorted(histogram.items())),
                               'smallPatterns': 12, 'wildTablePatterns': 988,
                               'bmsSubtreeByteIdentity': True,
                               'bmsSubtreeBytes': len(new_bms),
                               'bmsSubtreeSha256': sha256(new_bms),
                               'bmsCanonicalSha256': sha256(canonical(new['bms'])),
                               'firstRecordOfEverySizeRetained': True,
                               'maximumStep': maximum_step,
                               'maximumStepParentIndex': maximum_step_index,
                               'selectedParentIndicesSha256': sha256(canonical(indices)),
                               'provenance': provenance},
            'beforeSha256': sha256(before_bytes),
            'afterSha256': sha256(after_bytes)}


def provenance_checks(old, new, before_bytes, selected):
    """Verify the writer's explicit, frozen parent-pool/selection metadata."""
    require(new.keys() == old.keys() | {'selection'},
            'Only selection provenance may be added to catalog metadata')
    selection = new['selection']
    pool = selection['sourcePool']
    require(canonical(pool['metadata']) == canonical(old),
            'Full parent metadata snapshot must be exactly preserved')
    source_count = sum(not record['operations'] for record in selected)
    require(new.get('sourcePatterns') == source_count
            and new.get('derivedPatterns') == len(selected) - source_count,
            'Root metadata source/derived counts must describe retained patterns')
    for key in old.keys() - {'sourcePatterns', 'derivedPatterns'}:
        require(canonical(new[key]) == canonical(old[key]),
                f'Original provenance changed: {key}')
    expected_pool = {'patterns': 14661, 'originalPatterns': old['sourcePatterns'],
                     'derivedPatterns': old['derivedPatterns'],
                     'uncompressedBytes': len(before_bytes),
                     'sha256': sha256(before_bytes),
                     'offlineArchive': 'tools/reference/iblp-catalog-full.json.gz',
                     'metadata': old}
    require(canonical(pool) == canonical(expected_pool),
            'Parent full catalog provenance is missing or inaccurate')
    expected_selection = {'algorithm': 'marked-density-stratified-farthest-point-v1',
                          'selectedPatterns': 1000,
                          'selectedOriginalPatterns': source_count,
                          'selectedDerivedPatterns': 1000 - source_count,
                          'smallPatterns': 12, 'wildPatterns': 988,
                          'rowQuotas': {str(size): count
                                        for size, count in EXPECTED_COUNTS.items()}}
    for key, value in expected_selection.items():
        require(canonical(selection.get(key)) == canonical(value),
                f'Selection metadata differs from retained records: {key}')
    archive_path = ROOT / pool['offlineArchive']
    archive_bytes = archive_path.read_bytes()
    require(gzip.decompress(archive_bytes) == before_bytes,
            'Offline compressed mother catalog must preserve exact parent bytes')
    return {'unchangedParentMetadataPath': ['selection', 'sourcePool', 'metadata'],
            'parentCatalogSha256Path': ['selection', 'sourcePool', 'sha256'],
            'retainedSourcePatterns': source_count,
            'retainedDerivedPatterns': len(selected) - source_count,
            'workbookSha256': new['workbookSha256'],
            'rulesSha256': new['rulesSha256'],
            'offlineMotherArchive': pool['offlineArchive'],
            'offlineMotherArchiveBytes': len(archive_bytes),
            'offlineMotherArchiveSha256': sha256(archive_bytes),
            'offlineMotherDecompressedByteIdentity': True}


def metrics(archive):
    catalog = archive.sizes[CATALOG]
    jar_bytes = len(archive.raw_bytes)
    return {'jarBytes': jar_bytes,
            'catalogRawBytes': catalog['rawBytes'],
            'catalogCompressedBytes': catalog['compressedBytes'],
            'catalogCompressedShareOfJar': catalog['compressedBytes'] / jar_bytes,
            'catalogCompressedShareOfJarPercent': 100 * catalog['compressedBytes'] / jar_bytes,
            'jarSha256': sha256(archive.raw_bytes),
            'catalogSha256': sha256(archive.entries[CATALOG])}


def audit():
    source = SOURCE.read_bytes()
    results, baseline_catalog = [], None
    for target in TARGETS:
        old_path = ROOT / f'build/releases/{OLD}/guogaology-dimension-{target}-{OLD}.jar'
        new_path = ROOT / f'build/releases/{NEW}/guogaology-dimension-{target}-{NEW}.jar'
        a, b = read_zip(old_path), read_zip(new_path)
        if baseline_catalog is None:
            baseline_catalog = a.entries[CATALOG]
        require(a.entries[CATALOG] == baseline_catalog,
                f'{target}: baseline catalog differs across targets')
        require(b.entries[CATALOG] == source,
                f'{target}: packaged catalog bytes differ from formal source JSON')
        catalog = catalog_checks(a.entries[CATALOG], b.entries[CATALOG])
        old_nested, new_nested, bridges = nested_bridges(a, b, target)
        canonical_names = (set(a.entries) - old_nested) | new_nested
        require(canonical_names == set(b.entries),
                f'{target}: top-level ZIP entry IDs added or removed: '
                f'{sorted(canonical_names ^ set(b.entries))}')
        metadata = version_metadata(a.entries['fabric.mod.json'],
                                    b.entries['fabric.mod.json'],
                                    f'{target}: fabric.mod.json')
        unchanged = set(a.entries) - {CATALOG, 'fabric.mod.json'} - old_nested
        for name in sorted(unchanged):
            require(a.entries[name] == b.entries[name],
                    f'{target}: non-catalog production entry bytes changed: {name}')
        resources = {name for name in a.entries if name.startswith(('assets/', 'data/'))}
        require(resources == {name for name in b.entries
                              if name.startswith(('assets/', 'data/'))},
                f'{target}: assets/data entry IDs changed')
        classes = {name for name in a.entries if name.endswith('.class')}
        require(classes == {name for name in b.entries if name.endswith('.class')},
                f'{target}: production class set changed')
        require(classes <= unchanged, f'{target}: no class may be exempted')
        mixins = {name for name in unchanged
                  if name.endswith(('mixins.json', '-refmap.json'))}
        before, after = metrics(a), metrics(b)
        results.append({'minecraft': target, 'success': True,
                        'beforePath': str(old_path.relative_to(ROOT)),
                        'afterPath': str(new_path.relative_to(ROOT)),
                        'before': before, 'after': after,
                        'jarBytesSaved': before['jarBytes'] - after['jarBytes'],
                        'catalogCompressedBytesSaved': before['catalogCompressedBytes'] - after['catalogCompressedBytes'],
                        'formalSourceByteIdentity': True,
                        'unchangedProductionClasses': len(classes),
                        'unchangedClassManifestSha256': manifest_hash(a.entries, classes),
                        'retainedAssetDataEntries': len(resources),
                        'unchangedAssetDataEntries': len(resources - {CATALOG}),
                        'unchangedMixinRefmapEntries': sorted(mixins),
                        'unchangedOtherEntries': len(unchanged),
                        'unchangedEntryManifestSha256': manifest_hash(a.entries, unchanged),
                        'changedEntries': [CATALOG, 'fabric.mod.json'] + sorted(old_nested | new_nested),
                        'fabricMetadata': metadata, 'embeddedBridges': bridges,
                        'catalog': catalog})
    report = {'success': True, 'baselineVersion': OLD, 'version': NEW,
              'scope': 'Only the runtime iBLP catalog and version metadata may change; no native/world replay claimed.',
              'byteProof': 'All other ZIP entry payloads, every production class, all mixins/refmaps and all non-catalog assets/data are identical; ZIP container timestamps/compression are not gameplay resources.',
              'formalSource': str(SOURCE.relative_to(ROOT)),
              'formalSourceBytes': len(source), 'formalSourceSha256': sha256(source),
              'targets': results}
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f'IBLP0616_PACKAGE_DELTA_OK targets={len(results)} output={OUTPUT}')


if __name__ == '__main__':
    audit()
