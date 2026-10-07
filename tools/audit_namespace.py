"""Reject old runtime identifiers, including nested JAR classes and gzip NBT.

Alice's source snapshot is deliberately outside runtime resource roots. Source
credits in THIRD_PARTY.md/COPYRIGHT retain their original spelling. The only old
class constants allowed are three exact input aliases in the outer ID adapter;
they are translated to the new namespace before registration.
"""
import argparse
import gzip
import io
import json
from pathlib import Path
import re
import struct
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
OLD_WORD = re.compile(r'googology', re.I)
RUNTIME_TOKEN = re.compile(
    r'googology(?:[:_/.]|-dimension|-outer)|dev[./]googology|Googology[A-Z]|guogaology_outer', re.I)
SOURCE_CLASS_ALIASES = {
    'dev/guogaology/outer/GuogaologyMod.class': frozenset({
        'googology', 'googology_portal', 'googology_portal_frame'}),
}
SOURCE_NOTICE_FILES = frozenset({'THIRD_PARTY.md', 'COPYRIGHT'})


class AuditError(AssertionError):
    pass


class Reader:
    def __init__(self, raw):
        self.raw = raw
        self.offset = 0

    def take(self, count):
        if count < 0 or self.offset + count > len(self.raw):
            raise AuditError('Truncated or invalid binary length')
        result = self.raw[self.offset:self.offset + count]
        self.offset += count
        return result

    def number(self, fmt):
        return struct.unpack(fmt, self.take(struct.calcsize(fmt)))[0]

    def string(self):
        return self.take(self.number('>H')).decode('utf8')


def nbt_strings(blob):
    """Independently parse every NBT tag/string and validate lengths and EOF."""
    reader = Reader(gzip.decompress(blob) if blob.startswith(b'\x1f\x8b') else blob)
    strings = []

    def string():
        value = reader.string()
        strings.append(value)
        return value

    def payload(tag, depth=0):
        if depth > 512:
            raise AuditError('Excessively nested NBT')
        widths = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
        if tag in widths:
            reader.take(widths[tag])
        elif tag in (7, 11, 12):
            reader.take(reader.number('>i') * {7: 1, 11: 4, 12: 8}[tag])
        elif tag == 8:
            string()
        elif tag == 9:
            child, count = reader.number('>B'), reader.number('>i')
            if count < 0 or count > len(reader.raw) or (child == 0 and count):
                raise AuditError('Invalid NBT list')
            for _ in range(count):
                payload(child, depth + 1)
        elif tag == 10:
            while (child := reader.number('>B')):
                string()
                payload(child, depth + 1)
        else:
            raise AuditError(f'Invalid NBT tag {tag}')

    root = reader.number('>B')
    if root != 10:
        raise AuditError('Structure NBT root must be a compound')
    string()
    payload(root)
    if reader.offset != len(reader.raw):
        raise AuditError('Trailing NBT bytes')
    return strings


def class_strings(raw):
    """Read constant-pool UTF8 entries rather than searching compressed bytes."""
    reader = Reader(raw)
    if reader.take(4) != b'\xca\xfe\xba\xbe':
        raise AuditError('Invalid class magic')
    reader.take(4)
    count, index = reader.number('>H'), 1
    strings = []
    widths = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
              11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = reader.number('>B')
        if tag == 1:
            # Class files use modified UTF8; all forbidden tokens are ASCII.
            strings.append(reader.take(reader.number('>H')).decode('utf8', errors='replace'))
        elif tag in widths:
            reader.take(widths[tag])
            if tag in (5, 6):
                index += 1
        else:
            raise AuditError(f'Unknown constant-pool tag {tag}')
        index += 1
    return strings


def reject_runtime(value, where):
    if value.lower() == 'googology' or RUNTIME_TOKEN.search(value):
        raise AuditError(f'Old runtime identifier at {where}: {value[:180]!r}')


def inspect_json(value, where):
    if isinstance(value, dict):
        for key, child in value.items():
            if OLD_WORD.search(key):
                raise AuditError(f'Old JSON key at {where}: {key}')
            inspect_json(child, where + '/' + key)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            inspect_json(child, f'{where}[{index}]')
    elif isinstance(value, str):
        reject_runtime(value, where)


def inspect_entry(name, raw, where):
    if OLD_WORD.search(name) or 'guogaology_outer' in name:
        raise AuditError(f'Old runtime path: {where}')
    if name.endswith(('.json', '.mcmeta')):
        inspect_json(json.loads(raw), where)
    elif name.endswith('.nbt'):
        for value in nbt_strings(raw):
            reject_runtime(value, where)
    elif name.endswith('.class'):
        allowed = SOURCE_CLASS_ALIASES.get(name, ())
        for value in class_strings(raw):
            if value in allowed:
                continue
            if OLD_WORD.search(value) or 'guogaology_outer' in value:
                raise AuditError(f'Old class constant at {where}: {value[:180]!r}')
    elif name not in SOURCE_NOTICE_FILES and (name.endswith((
            '.properties', '.cfg', '.toml', '.accesswidener', '.mf', '.MF',
            '.mcfunction', '.yaml', '.yml', '.xml', '.txt', '.md'))
            or name.startswith('META-INF/services/')):
        reject_runtime(raw.decode('utf8'), where)


def audit_archive(archive, label='<jar>'):
    counts = {'entries': 0, 'classes': 0, 'nbt': 0, 'nested_jars': 0}
    names = archive.namelist()
    if len(set(names)) != len(names):
        raise AuditError(f'Duplicate ZIP entries in {label}')
    for name in names:
        if name.endswith('/'):
            if OLD_WORD.search(name):
                raise AuditError(f'Old runtime directory: {label}!/{name}')
            continue
        where = label + '!/' + name
        raw = archive.read(name)
        inspect_entry(name, raw, where)
        counts['entries'] += 1
        counts['classes'] += name.endswith('.class')
        counts['nbt'] += name.endswith('.nbt')
        if name.endswith('.jar'):
            with ZipFile(io.BytesIO(raw)) as nested:
                for key, number in audit_archive(nested, where).items():
                    counts[key] += number
            counts['nested_jars'] += 1
    return counts


def audit_resources(resources):
    resources = Path(resources)
    if not resources.is_dir():
        raise AuditError(f'Missing resource root: {resources}')
    counts = {'entries': 0, 'nbt': 0}
    for path in resources.rglob('*'):
        name = path.relative_to(resources).as_posix()
        if OLD_WORD.search(name) or 'guogaology_outer' in name:
            raise AuditError(f'Old resource path: {path}')
        if not path.is_file():
            continue
        inspect_entry(name, path.read_bytes(), str(path))
        counts['entries'] += 1
        counts['nbt'] += name.endswith('.nbt')
    return counts


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resources', type=Path, action='append')
    parser.add_argument('--jar', type=Path, action='append', default=[])
    args = parser.parse_args()
    roots = args.resources if args.resources is not None else ([] if args.jar else [ROOT / 'src/main/resources', ROOT / 'src/client/resources'])
    result = {str(path): audit_resources(path) for path in roots}
    for path in args.jar:
        with ZipFile(path) as archive:
            result[str(path)] = audit_archive(archive, str(path))
    print(json.dumps(result, ensure_ascii=False, indent=2))
