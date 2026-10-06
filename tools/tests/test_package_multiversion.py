"""Regression coverage for cross-platform gzip structure validation."""
import gzip
import io
import json
from zipfile import ZipFile
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from package_multiversion import ROOT, verify_structure_template, verify_client_hooks


class ClientHookValidationTests(unittest.TestCase):
    def archive(self, names, classes=True):
        stream=io.BytesIO()
        with ZipFile(stream,'w') as out:
            out.writestr('client.json',json.dumps({'package':'dev.googology.client.mixin','client':names}))
            if classes:
                for name in names:out.writestr('dev/googology/client/mixin/'+name+'.class',b'fixture')
        return ZipFile(stream)

    def test_client_hooks_are_packaged_and_enabled(self):
        with self.archive(['InventoryOriginAccessor','ManuscriptVerticalFlightMixin']) as archive:
            verify_client_hooks(archive,{'mixins':[{'config':'client.json','environment':'client'}]})

    def test_dropping_client_config_is_rejected(self):
        with self.archive(['InventoryOriginAccessor','ManuscriptVerticalFlightMixin']) as archive:
            with self.assertRaisesRegex(AssertionError,'Unregistered client hook'):
                verify_client_hooks(archive,{'mixins':[]})

    def test_registering_an_unpackaged_class_is_rejected(self):
        with self.archive(['InventoryOriginAccessor','ManuscriptVerticalFlightMixin'],False) as archive:
            with self.assertRaisesRegex(AssertionError,'Missing registered mixin'):
                verify_client_hooks(archive,{'mixins':['client.json']})


class StructureValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.name='data/googology/structure/bms_aco.nbt'
        cls.original=(ROOT/'src/main/resources'/cls.name).read_bytes()
        cls.nbt=gzip.decompress(cls.original)

    def test_platform_headers_do_not_change_structure(self):
        for os_byte in (3,10,255):  # Unix, NTFS, Python's platform-neutral header.
            with self.subTest(os_byte=os_byte):
                changed=bytearray(self.original)
                changed[9]=os_byte
                verify_structure_template(bytes(changed),self.original,self.name)

    def test_recompression_and_metadata_do_not_change_structure(self):
        for level in (0,1,9):
            with self.subTest(level=level):
                stream=io.BytesIO()
                with gzip.GzipFile(filename='different-name.nbt',fileobj=stream,mode='wb',compresslevel=level,mtime=1234) as out:
                    out.write(self.nbt)
                verify_structure_template(stream.getvalue(),self.original,self.name)

    def test_changed_block_palette_is_rejected(self):
        self.assertIn(b'googology:ordinal_bricks',self.nbt)
        changed=self.nbt.replace(b'googology:ordinal_bricks',b'googology:missing_bricks')
        with self.assertRaisesRegex(AssertionError,'Structure content mismatch: '+self.name):
            verify_structure_template(gzip.compress(changed,mtime=0),self.original,self.name)

    def test_corrupt_archive_checksum_is_rejected(self):
        damaged=bytearray(self.original)
        damaged[-8]^=1
        with self.assertRaises(gzip.BadGzipFile):
            verify_structure_template(bytes(damaged),self.original,self.name)


if __name__=='__main__':
    unittest.main()
