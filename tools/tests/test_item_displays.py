"""Exercise the final import paths that previously erased plant item transforms."""
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from generate_item_displays import ASSETS, ITEMS, ROOT, generate
from prepare_outer_resources import prepare
from prepare_port_resources import convert_items


class ItemDisplays(unittest.TestCase):
    def copy_assets(self,root):
        assets=root/'assets/guogaology'
        for name in ITEMS:
            for relative in (f'models/item/{name}.json',f'models/block/{name}.json',f'items/{name}.json'):
                source=ASSETS/relative
                if source.exists():
                    target=assets/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
        return assets

    def test_non_gui_scale_survives_all_final_import_paths(self):
        (ROOT/'build').mkdir(exist_ok=True)
        for target in ('1.21.1','1.21.11','26.2','26.3'):
            with self.subTest(target=target),tempfile.TemporaryDirectory(dir=ROOT/'build') as directory:
                root=Path(directory);assets=self.copy_assets(root)
                before={name:(assets/f'models/block/{name}.json').read_bytes() for name in ITEMS}
                if target!='1.21.1':convert_items(root)
                # In particular, the legacy donor importer used to recreate a bare
                # wrapper whenever the modern definition pointed at block/<id>.
                prepare(target,root)
                for name in ITEMS:
                    actual=json.loads((assets/f'models/item/{name}.json').read_text('utf8'))
                    self.assertEqual(actual['parent'],'guogaology:block/'+name)
                    self.assertEqual(actual['display']['ground']['scale'],[.25]*3)
                    self.assertEqual(actual['display']['fixed']['scale'],[.5]*3)
                    for hand in ('righthand','lefthand'):
                        self.assertEqual(actual['display']['firstperson_'+hand]['scale'],[.4]*3)
                        self.assertEqual(actual['display']['thirdperson_'+hand]['scale'],[.375]*3)
                    self.assertNotIn('gui',actual['display'],'existing GUI keeps its original transformation')
                    self.assertEqual((assets/f'models/block/{name}.json').read_bytes(),before[name])
                    definition=assets/f'items/{name}.json'
                    if definition.exists():self.assertEqual(json.loads(definition.read_text('utf8'))['model']['model'],'guogaology:item/'+name)

    def test_regeneration_detects_stale_scale_preserves_gui_and_never_writes_world_geometry(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'build') as directory:
            assets=self.copy_assets(Path(directory))
            path=assets/'models/item/graham.json';value=json.loads(path.read_text('utf8'))
            gui={'rotation':[0,0,30],'translation':[1,2,3],'scale':[.7]*3}
            value['display']['gui']=gui;value['display']['ground']['scale']=[1]*3
            path.write_text(json.dumps(value),encoding='utf8');stale=path.read_bytes()
            world={name:(assets/f'models/block/{name}.json').read_bytes() for name in ITEMS}
            with self.assertRaisesRegex(SystemExit,'Stale non-GUI item displays'):generate(True,assets)
            self.assertEqual(path.read_bytes(),stale,'check mode must not alter a file')
            self.assertTrue(generate(False,assets));self.assertFalse(generate(True,assets))
            fixed=json.loads(path.read_text('utf8'));self.assertEqual(fixed['display']['gui'],gui)
            self.assertEqual(fixed['display']['ground']['scale'],[.25]*3)
            for name,data in world.items():self.assertEqual((assets/f'models/block/{name}.json').read_bytes(),data)


if __name__=='__main__':unittest.main()
