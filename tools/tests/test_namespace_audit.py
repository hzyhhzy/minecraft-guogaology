"""Adversarial archive fixtures prove old identifiers cannot hide in binaries."""
import gzip
import io
import json
from pathlib import Path
import struct
import sys
import unittest
from zipfile import ZipFile

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from audit_namespace import AuditError, audit_archive, nbt_strings


def archive(entries):
    buffer=io.BytesIO()
    with ZipFile(buffer,'w') as jar:
        for path,content in entries.items():jar.writestr(path,content)
    return buffer.getvalue()


def class_fixture(value):
    # A valid empty public class with the tested UTF8 constant in its pool.
    strings=['dev/guogaology/Example','java/lang/Object',value]
    utf=lambda s:b'\x01'+struct.pack('>H',len(s.encode()))+s.encode()
    pool=utf(strings[0])+b'\x07\0\x01'+utf(strings[1])+b'\x07\0\x03'+utf(strings[2])
    return b'\xca\xfe\xba\xbe\0\0\0\x41\0\x06'+pool+struct.pack('>7H',0x21,2,4,0,0,0,0)


def nbt_fixture(value):
    raw=value.encode()
    return gzip.compress(b'\x0a\0\0\x08\0\x04Name'+struct.pack('>H',len(raw))+raw+b'\0',mtime=0)


def audit(entries):
    with ZipFile(io.BytesIO(archive(entries))) as jar:return audit_archive(jar)


class NamespaceAudit(unittest.TestCase):
    def test_old_paths_and_json_keys_values(self):
        for entries in [
            {'assets/googology/item.json':'{}'},
            {'data/guogaology/test.json':'{"value":"#googology:logs"}'},
            {'data/guogaology/test.json':'{"googology:component":1}'},
            {'fabric.mod.json':'{"id":"googology"}'},
            {'guogaology.mixins.json':'{"package":"dev.googology.mixin"}'},
            {'guogaology.mixins.json':'{"client":["GoogologySkyLightMixin"]}'},
            {'assets/guogaology/test.json':'{"name":"googology_outer:wood"}'},
            {'assets/guogaology/test.json':'{"name":"guogaology_outer:wood"}'},
        ]:
            with self.subTest(entries=entries),self.assertRaises(AuditError):audit(entries)

    def test_gzip_nbt_and_nested_legacy_class(self):
        with self.assertRaises(AuditError):
            audit({'data/guogaology/structure/test.nbt':nbt_fixture('googology:stone')})
        nested=archive({'dev/guogaology/Example.class':class_fixture('googology:packet')})
        with self.assertRaises(AuditError):audit({'META-INF/jars/guogaology-outer.jar':nested})
        with self.assertRaises(AuditError):
            audit({'dev/guogaology/Example.class':class_fixture('dev/googology/Example')})

    def test_length_changing_byte_replacement_is_rejected(self):
        raw=gzip.decompress(nbt_fixture('googology:stone')).replace(b'googology',b'guogaology')
        with self.assertRaises(AuditError):nbt_strings(gzip.compress(raw))

    def test_source_alias_allowlist_is_exact_and_local(self):
        path='dev/guogaology/outer/GuogaologyMod.class'
        for value in ('googology','googology_portal','googology_portal_frame'):
            self.assertEqual(audit({path:class_fixture(value)})['classes'],1)
            with self.assertRaises(AuditError):audit({'dev/guogaology/Example.class':class_fixture(value)})
        with self.assertRaises(AuditError):audit({path:class_fixture('googology:googology')})

    def test_new_ids_and_real_source_credits_are_allowed(self):
        entries={
            'fabric.mod.json':json.dumps({'id':'guogaology','name':'果糕逻辑 · Guogaology'}),
            'assets/guogaology/lang/en_us.json':json.dumps({'guide.guogaology.subject':'Googology studies large numbers.'}),
            'THIRD_PARTY.md':b'Original: googology-dimension-1.0.0.jar, Googology Dimension Team.',
            'data/guogaology/structure/test.nbt':nbt_fixture('guogaology:stone'),
            'META-INF/jars/guogaology-outer.jar':archive({'dev/guogaology/Example.class':class_fixture('guogaology:packet')}),
        }
        result=audit(entries)
        self.assertEqual((result['classes'],result['nbt'],result['nested_jars']),(1,1,1))


if __name__=='__main__':unittest.main()
