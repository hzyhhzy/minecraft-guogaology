"""Regression coverage for cross-platform gzip structure validation."""
import gzip
import io
import json
from zipfile import ZipFile
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from package_multiversion import ROOT, verify_structure_template, verify_client_hooks, verify_jei_compat


class ClientHookValidationTests(unittest.TestCase):
    def names(self,target='26.2'):
        common=['InventoryOriginAccessor','ManuscriptVerticalFlightMixin','RecipeBookSectionsMixin','RecipeBookCollectionsMixin']
        return common+([] if target=='1.21.1' else ['RecipeBookTabTooltipMixin','RecipeBookBrandIconMixin'])

    def metadata(self,target='26.2'):
        return {'mixins':[{'config':'client.json','environment':'client'}],
                'depends':{'minecraft':target},'icon':'assets/guogaology/icon.png'}

    def archive(self, names=None, classes=True, target='26.2'):
        if names is None:names=self.names(target)
        stream=io.BytesIO()
        with ZipFile(stream,'w') as out:
            out.writestr('client.json',json.dumps({'package':'dev.guogaology.client.mixin','client':names}))
            # The checker reads the exact CONSTANT_Utf8 icon literal at the helper
            # and javac-inlined render site, not an arbitrary substring.
            literal=b'guogaology:icon.png';constant=b'\x01'+len(literal).to_bytes(2,'big')+literal
            out.writestr('assets/guogaology/icon.png',b'fixture PNG')
            if classes:
                for name in names:out.writestr('dev/guogaology/client/mixin/'+name+'.class',b'fixture'+constant)
                out.writestr('dev/guogaology/client/recipebook/RecipeBookSections.class',b'fixture'+constant)
                if target=='1.21.1':out.writestr('dev/guogaology/client/recipebook/SectionRecipeBookButton.class',b'fixture'+constant)
                else:out.writestr('dev/guogaology/client/recipebook/SectionRecipeBookCategory.class',b'fixture')
        return ZipFile(stream)

    def test_client_hooks_are_packaged_and_enabled(self):
        for target in ('1.21.1','1.21.11','26.2','26.3'):
            with self.subTest(target=target),self.archive(target=target) as archive:
                verify_client_hooks(archive,self.metadata(target))

    def test_dropping_client_config_is_rejected(self):
        with self.archive() as archive:
            metadata=self.metadata();metadata['mixins']=[]
            with self.assertRaisesRegex(AssertionError,'Unregistered client hook'):
                verify_client_hooks(archive,metadata)
        for target in ('1.21.1','1.21.11','26.2','26.3'):
            for required in self.names(target):
                with self.subTest(target=target,required=required),self.archive([name for name in self.names(target) if name!=required],target=target) as archive:
                    with self.assertRaisesRegex(AssertionError,required):verify_client_hooks(archive,self.metadata(target))

    def test_registering_an_unpackaged_class_is_rejected(self):
        with self.archive(classes=False) as archive:
            with self.assertRaisesRegex(AssertionError,'Missing registered mixin'):
                verify_client_hooks(archive,self.metadata())


class OptionalJeiValidationTests(unittest.TestCase):
    def archive(self, include=True, missing=None):
        stream=io.BytesIO()
        with ZipFile(stream,'w') as out:
            if include:
                out.writestr('guogaology.jei-compat.mixins.json',json.dumps({
                    'plugin':'dev.guogaology.client.compat.jei.JeiTagCompatPlugin',
                    'client':['JeiVanillaTagsMixin']}))
                for name in ('JeiTagCompatPlugin','JeiTagCompatShape','JeiTagCompatShape$Flow'):
                    if name!=missing:out.writestr('dev/guogaology/client/compat/jei/'+name+'.class',b'fixture')
                out.writestr('dev/guogaology/compat/RecipeSyncCompat.class',b'fixture')
        return ZipFile(stream)

    def metadata(self):
        return {'depends':{},'entrypoints':{'main':['dev.guogaology.GuogaologyMod','dev.guogaology.compat.RecipeSyncCompat']},'mixins':[{'config':'guogaology.jei-compat.mixins.json','environment':'client'}]}

    def test_263_optional_client_patch_is_allowed(self):
        with self.archive() as archive:verify_jei_compat(archive,self.metadata(),'26.3')

    def test_missing_structural_guard_classes_are_rejected(self):
        for name in ('JeiTagCompatPlugin','JeiTagCompatShape','JeiTagCompatShape$Flow'):
            with self.subTest(name=name),self.archive(missing=name) as archive:
                with self.assertRaisesRegex(AssertionError,'structural guard class: '+name.replace('$',r'\$')):
                    verify_jei_compat(archive,self.metadata(),'26.3')

    def test_hook_on_dedicated_server_is_rejected(self):
        metadata=self.metadata();metadata['mixins']=['guogaology.jei-compat.mixins.json']
        with self.archive() as archive:
            with self.assertRaisesRegex(AssertionError,'client-only'):verify_jei_compat(archive,metadata,'26.3')

    def test_jei_hard_dependency_is_rejected(self):
        metadata=self.metadata();metadata['depends']['jei']='*'
        with self.archive() as archive:
            with self.assertRaisesRegex(AssertionError,'optional'):verify_jei_compat(archive,metadata,'26.3')

    def test_other_targets_exclude_workaround(self):
        for target in ('1.21.1','1.21.11','26.2'):
            with self.archive(False) as archive:verify_jei_compat(archive,{'depends':{},'mixins':[]},target)
            with self.archive() as archive:
                with self.assertRaisesRegex(AssertionError,'leaked'):verify_jei_compat(archive,self.metadata(),target)


class StructureValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.name='data/guogaology/structure/bms_aco.nbt'
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
        self.assertIn(b'guogaology:ordinal_bricks',self.nbt)
        changed=self.nbt.replace(b'guogaology:ordinal_bricks',b'guogaology:missing_bricks')
        with self.assertRaisesRegex(AssertionError,'Structure content mismatch: '+self.name):
            verify_structure_template(gzip.compress(changed,mtime=0),self.original,self.name)

    def test_corrupt_archive_checksum_is_rejected(self):
        damaged=bytearray(self.original)
        damaged[-8]^=1
        with self.assertRaises(gzip.BadGzipFile):
            verify_structure_template(bytes(damaged),self.original,self.name)


if __name__=='__main__':
    unittest.main()
