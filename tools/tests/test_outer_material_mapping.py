"""Regression checks for donor palette/state conversion into the shared registry."""
import gzip
import struct
import sys
from pathlib import Path
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from import_outer_content import remap_id, walk, nbt_transform, LAMPS, ROOT, NS, SOURCE_NS
from audit_namespace import nbt_strings, reject_runtime


def string(value):
    data=value.encode('utf8')
    return struct.pack('>H',len(data))+data


def state_blob(identity,properties,modern):
    """A minimal real NBT state compound, independent of the converter."""
    names=('id','properties') if modern else ('Name','Properties')
    body=b'\x08'+string(names[0])+string(identity)
    if properties:
        body+=b'\x0a'+string(names[1])
        body+=b''.join(b'\x08'+string(key)+string(value) for key,value in properties.items())+b'\0'
    return gzip.compress(b'\x0a\0\0'+body+b'\0',mtime=0)


class MaterialMapping(unittest.TestCase):
    def test_shared_registry_and_vanilla(self):
        for old,new in [('ordinal_stone','minecraft:stone'),('hell_ordinal_stone','minecraft:netherrack'),
                        ('christmas_log','minecraft:spruce_log'),('christmas_leaves','guogaology:christmas_leaves'),
                        ('christmas_sapling','minecraft:spruce_sapling'),('hell_christmas_log','minecraft:dark_oak_log'),
                        ('lho_glass','guogaology:absence_glass'),('laver_log','guogaology:laver_vein'),
                        ('bashicu_block','guogaology:ordinal_bricks'),('loquat','guogaology:loquat')]:
            self.assertEqual(remap_id('googology:'+old),new)
        self.assertEqual(remap_id('googology:bms_matrix'),'guogaology:bms_matrix')
        self.assertEqual(remap_id('googology:googology'),'guogaology:outer')
        self.assertEqual(remap_id('googology:googology_portal'),'guogaology:guogao_portal')
        self.assertEqual(remap_id('googology:googology_portal_frame'),'guogaology:guogao_portal_frame')
        self.assertEqual(remap_id('guogaology:guogaology'),'guogaology:guogaology')
        self.assertEqual(remap_id('guogaology:outer'),'guogaology:outer')

    def test_number_glyph_and_log_axis_survive(self):
        self.assertEqual(walk({'Name':'googology:bashicu_block','Properties':{'digit':'15'}}),
                         {'Name':'guogaology:ordinal_bricks','Properties':{'number':'15'}})
        self.assertEqual(walk({'Name':'googology:laver_log','Properties':{'axis':'x'}}),
                         {'Name':'guogaology:laver_vein','Properties':{'axis':'x'}})

    def test_lamp_nbt_all_colors_and_format_generations(self):
        for color,name in enumerate(LAMPS):
            raw=state_blob('googology:christmas_light',{'color':str(color)},True)
            for modern in (True,False):
                expected=state_blob('guogaology:'+name+'_light',{},modern)
                self.assertEqual(gzip.decompress(nbt_transform(raw,legacy=not modern)),gzip.decompress(expected))
                self.assertEqual(walk({'id':'googology:christmas_light','properties':{'color':str(color)}}),
                                 {'id':'guogaology:'+name+'_light'})

    def test_matrix_nbt_all_values(self):
        for number in range(16):
            raw=state_blob('googology:bashicu_block',{'digit':str(number)},True)
            expected=state_blob('guogaology:ordinal_bricks',{'number':str(number)},False)
            self.assertEqual(gzip.decompress(nbt_transform(raw,legacy=True)),gzip.decompress(expected))

    def test_source_identity_and_asset_registry_boundary(self):
        self.assertEqual((SOURCE_NS,NS),('googology','guogaology'))
        # Texture/model names are not block aliases, but still use the host namespace.
        self.assertEqual(walk({'parent':'googology:block/bashicu_block'},asset=True),
                         {'parent':'guogaology:block/bashicu_block'})
        self.assertEqual(walk(['#googology:woods','googology:googology']),
                         ['#guogaology:woods','guogaology:outer'])
        manifest=(ROOT/'content/outer-1.0.0/manifest.json').read_text('utf8')
        self.assertIn('googology-dimension-1.0.0.jar',manifest)
        self.assertIn('Googology Dimension Team',manifest)

    def test_all_real_templates_parse_and_use_new_ids(self):
        sources=sorted((ROOT/'content/outer-1.0.0/structure').rglob('*.nbt'))
        self.assertEqual(len(sources),61)
        for path in sources:
            source=path.read_bytes()
            original=nbt_strings(source)
            for legacy in (True,False):
                with self.subTest(template=path.name,legacy=legacy):
                    converted=nbt_transform(source,legacy)
                    strings=nbt_strings(converted)
                    for value in strings:reject_runtime(value,path.name)
                    # Re-importing already converted templates cannot change IDs.
                    self.assertEqual(gzip.decompress(nbt_transform(converted,legacy)),gzip.decompress(converted))
                    self.assertEqual(sum(':' in s for s in original),sum(':' in s for s in strings))

    def test_source_and_target_nbt_state_formats(self):
        for source_modern in (False,True):
            for target_modern in (False,True):
                source=state_blob('googology:bashicu_block',{'digit':'13'},source_modern)
                actual=nbt_transform(source,legacy=not target_modern)
                expected=state_blob('guogaology:ordinal_bricks',{'number':'13'},target_modern)
                self.assertEqual(gzip.decompress(actual),gzip.decompress(expected))


if __name__=='__main__':
    unittest.main()
