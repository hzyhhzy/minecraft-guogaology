"""Native wood-mechanism registration and immutable-donor overlay guards."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

TOOLS=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(TOOLS))
from generate_wood_mechanisms_0614 import ROOT,RESOURCE,FAMILIES,MECHANISMS,apply_pack,verify_archive
from prepare_outer_resources import prepare
from prepare_outer_legacy import legacy
from prepare_port_sources import adapt


class DirectoryArchive:
    def __init__(self,root):self.root=Path(root)
    def read(self,name):return (self.root/name).read_bytes()


class NativeWoodMechanismTests(unittest.TestCase):
    def test_current_tags_include_both_native_wood_families(self):
        verify_archive(DirectoryArchive(RESOURCE))

    def test_native_control_properties_survive_every_target_source_adapter(self):
        relative='dev/guogaology/outer/registry/ModWoodDecorBlocks.java'
        common=(ROOT/'ports/common/main'/relative).read_text('utf8')
        for target in ('1.21.1','1.21.11','26.2','26.3'):
            source=(ROOT/'ports/26.3/src/main/java'/relative).read_text('utf8') if target=='26.3' else common
            source=legacy(source,'ModWoodDecorBlocks.java') if target=='1.21.1' else adapt(source,target,'ModWoodDecorBlocks.java')
            with self.subTest(target=target):
                self.assertIn('Properties.ofFullCopy(original).sound(SoundType.WOOD)',source)
                for shape in ('FENCE','FENCE_GATE','DOOR','TRAPDOOR','BUTTON','PRESSURE_PLATE','SIGN','HANGING_SIGN'):
                    self.assertIn('Blocks.OAK_'+shape,source)
                    self.assertIn('Blocks.CRIMSON_'+shape,source)
                for shape in ('PressurePlate','Button','Door','TrapDoor'):
                    wrapper=(ROOT/f'ports/common/main/dev/guogaology/outer/block/Mod{shape}Block.java').read_text('utf8')
                    self.assertIn('extends '+shape+'Block',wrapper)
                    self.assertNotIn('@Override',wrapper,'native mechanics stay inherited')

    def test_all_donor_imports_keep_native_members_and_reimport_is_idempotent(self):
        with tempfile.TemporaryDirectory(prefix='wood0614-',dir=ROOT/'build') as folder:
            root=Path(folder)
            for target in ('1.21.1','1.21.11','26.2','26.3'):
                out=root/target
                with self.subTest(target=target):
                    prepare(target,out)
                    verify_archive(DirectoryArchive(out))
                    self.assertEqual(apply_pack(out),[])
                    snapshot={p.relative_to(out):p.read_bytes() for p in out.rglob('*.json') if p.parts[-3:-1]==('tags','block') or p.parts[-3:-1]==('tags','item')}
                    prepare(target,out)
                    verify_archive(DirectoryArchive(out))
                    for path,value in snapshot.items():self.assertEqual((out/path).read_bytes(),value)

    def test_overlay_is_additive_and_final_verifier_rejects_missing_laver(self):
        with tempfile.TemporaryDirectory(prefix='wood0614-sentinel-',dir=ROOT/'build') as folder:
            out=Path(folder);path=out/'data/minecraft/tags/block/wooden_pressure_plates.json'
            optional={'id':'other:optional','required':False}
            sentinels=['minecraft:oak_pressure_plate','other:sentinel',optional]
            path.parent.mkdir(parents=True);path.write_text(json.dumps({'replace':False,'values':sentinels}))
            apply_pack(out)
            values=json.loads(path.read_text())
            self.assertEqual(values['values'][:3],sentinels)
            verify_archive(DirectoryArchive(out))
            values['values'].remove('guogaology:laver_pressure_plate');path.write_text(json.dumps(values))
            with self.assertRaises(AssertionError):verify_archive(DirectoryArchive(out))


if __name__=='__main__':unittest.main()
