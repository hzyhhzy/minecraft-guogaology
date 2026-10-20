"""Verify a JEI-only binary patch against its exact release line, including nested JARs."""
from __future__ import annotations

import argparse
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

TARGETS = ('1.21.1', '1.21.11', '26.2', '26.3')
COMPAT = 'dev/guogaology/client/compat/jei/'


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def manifest_attributes(raw):
    lines = []
    for line in raw.decode('utf8').replace('\r\n', '\n').replace('\r', '').splitlines():
        if line.startswith(' '):
            assert lines, 'Invalid manifest continuation'
            lines[-1] += line[1:]
        elif line:
            lines.append(line)
    attributes = dict(line.split(': ', 1) for line in lines)
    assert len(attributes) == len(lines), 'Unexpected duplicate manifest attributes'
    # New matcher classes must be classified client-only by Loom. Removing just
    # those entries here permits their addition without ignoring any other flag.
    key = 'Fabric-Loom-Client-Only-Entries'
    if key in attributes:
        attributes[key] = ';'.join(name for name in attributes[key].split(';')
                                   if not name.startswith(COMPAT + 'JeiTagCompatShape'))
    return json.dumps(attributes, sort_keys=True).encode()


def normalized_entries(data, expected_version):
    with ZipFile(BytesIO(data)) as archive:
        entries = {name: archive.read(name) for name in archive.namelist() if not name.endswith('/')}
    metadata = json.loads(entries['fabric.mod.json'])
    assert metadata['version'] == expected_version, (metadata['version'], expected_version)
    metadata['version'] = '<release>'
    for nested in metadata.get('jars', []):
        original = nested['file']
        normalized = original.replace(expected_version, '<release>')
        nested['file'] = normalized
        entries[normalized] = normalized_entries(entries.pop(original), expected_version)
    entries['fabric.mod.json'] = json.dumps(metadata, sort_keys=True, ensure_ascii=False).encode()
    if 'META-INF/MANIFEST.MF' in entries:
        entries['META-INF/MANIFEST.MF'] = manifest_attributes(entries['META-INF/MANIFEST.MF'])
    return entries


def compare(old, new, target, where='', line_endings=None):
    changed = []
    for name in sorted(old.keys() | new.keys()):
        before, after = old.get(name), new.get(name)
        path = where + name
        if isinstance(before, dict) and isinstance(after, dict):
            changed.extend(compare(before, after, target, path + '!/', line_endings))
        elif before != after:
            if (name.endswith('.json') and isinstance(before, bytes) and isinstance(after, bytes)
                    and before.replace(b'\r\n', b'\n') == after.replace(b'\r\n', b'\n')):
                if line_endings is not None:
                    line_endings.append(path)
                continue
            # The optional client plugin/matcher is the only production behavior
            # allowed to differ. Resources, sync code and the redirect stay fixed.
            allowed = (not where and target == '26.3' and name.startswith(COMPAT)
                       and (name == COMPAT + 'JeiTagCompatPlugin.class'
                            or name.startswith(COMPAT + 'JeiTagCompatShape'))
                       and name.endswith('.class'))
            assert allowed, f'Unexpected non-JEI production change: {target}/{path}'
            changed.append(path)
    return changed


def audit(baseline, candidate, old_version, new_version):
    rows = []
    for target in TARGETS:
        old = baseline / f'guogaology-dimension-{target}-{old_version}.jar'
        new = candidate / f'guogaology-dimension-{target}-{new_version}.jar'
        line_endings = []
        changes = compare(normalized_entries(old.read_bytes(), old_version),
                          normalized_entries(new.read_bytes(), new_version), target, line_endings=line_endings)
        if target == '26.3':
            assert COMPAT + 'JeiTagCompatPlugin.class' in changes, 'The new JEI guard is absent'
            assert any(name.startswith(COMPAT + 'JeiTagCompatShape') for name in changes), 'Matcher absent'
        else:
            assert not changes, f'JEI patch leaked into {target}'
        rows.append({'minecraft': target, 'baselineVersion': old_version, 'version': new_version,
                     'baselineSha256': digest(old), 'sha256': digest(new), 'changedProductionEntries': changes,
                     'jsonLineEndingOnlyEntries': line_endings})
    return {'success': True, 'onlyVersionAndJeiGuardChanged': True,
            'normalization': 'Only JSON CRLF/LF, release metadata and matcher client-manifest additions; all other bytes compared',
            'targets': rows}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--old-version', required=True)
    parser.add_argument('--new-version', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.baseline, args.candidate, args.old_version, args.new_version)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
