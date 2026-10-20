"""The backport guard must reject gameplay, resource and nested-code drift."""
import io
import json
from pathlib import Path
import sys
import unittest
from zipfile import ZipFile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_jei_patch_delta import COMPAT, compare, normalized_entries


def jar(version, extra=None, nested=None):
    output = io.BytesIO()
    metadata = {'id': 'guogaology', 'version': version}
    entries = {'data/guogaology/recipe/example.json': b'unchanged', **(extra or {})}
    if nested is not None:
        path = f'META-INF/jars/bridge-{version}.jar'
        metadata['jars'] = [{'file': path}]
        entries[path] = nested
    entries['fabric.mod.json'] = json.dumps(metadata).encode()
    with ZipFile(output, 'w') as archive:
        for name, value in entries.items():
            archive.writestr(name, value)
    return output.getvalue()


class PatchDeltaTests(unittest.TestCase):
    def test_only_metadata_and_nested_version_changes_are_normalized(self):
        old = normalized_entries(jar('0.6.1', nested=jar('0.6.1')), '0.6.1')
        new = normalized_entries(jar('0.6.1a', nested=jar('0.6.1a')), '0.6.1a')
        self.assertEqual(compare(old, new, '1.21.1'), [])

    def test_client_guard_changes_are_26_3_only(self):
        old = {COMPAT + 'JeiTagCompatPlugin.class': b'old'}
        new = {COMPAT + 'JeiTagCompatPlugin.class': b'new', COMPAT + 'JeiTagCompatShape.class': b'new'}
        self.assertEqual(len(compare(old, new, '26.3')), 2)
        with self.assertRaisesRegex(AssertionError, 'Unexpected'):
            compare(old, new, '26.2')

    def test_gameplay_resources_and_unrelated_classes_are_not_ignored(self):
        for path in ('data/guogaology/recipe/example.json', 'dev/guogaology/Equipment.class',
                     'dev/guogaology/compat/RecipeSyncCompat.class', COMPAT + 'JeiVanillaTagsMixin.class'):
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, 'Unexpected'):
                compare({path: b'old'}, {path: b'new'}, '26.3')

    def test_nested_classes_cannot_hide_even_under_a_compat_name(self):
        name = COMPAT + 'JeiTagCompatPlugin.class'
        with self.assertRaisesRegex(AssertionError, 'Unexpected'):
            compare({'nested.jar': {name: b'old'}}, {'nested.jar': {name: b'new'}}, '26.3')

    def test_only_literal_json_line_endings_are_ignored_and_recorded(self):
        ending_changes = []
        name = 'assets/guogaology/core_meshes/example.json'
        self.assertEqual(compare({name: b'{"a":1}\r\n'}, {name: b'{"a":1}\n'}, '26.3',
                                 line_endings=ending_changes), [])
        self.assertEqual(ending_changes, [name])
        for after in (b'{"a":2}\n', b'{"a":1} \n', b'{ "a":1}\n'):
            with self.subTest(after=after), self.assertRaisesRegex(AssertionError, 'Unexpected'):
                compare({name: b'{"a":1}\r\n'}, {name: after}, '26.3')


if __name__ == '__main__':
    unittest.main()
