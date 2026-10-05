"""Regression coverage for cross-platform gzip structure validation."""
import gzip
import io
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from package_multiversion import ROOT, verify_structure_template


class StructureValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.name='data/googology_outer/structure/bms_aco.nbt'
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
