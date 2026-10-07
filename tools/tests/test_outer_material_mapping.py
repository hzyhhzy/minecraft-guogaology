"""Regression checks for donor palette/state conversion into the shared registry."""
import gzip
import struct
import sys
from pathlib import Path
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from import_outer_content import remap_id, walk, nbt_transform, LAMPS


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
                        ('christmas_log','minecraft:spruce_log'),('christmas_leaves','googology:christmas_leaves'),
                        ('christmas_sapling','minecraft:spruce_sapling'),('hell_christmas_log','minecraft:dark_oak_log'),
                        ('lho_glass','googology:absence_glass'),('laver_log','googology:laver_vein'),
                        ('bashicu_block','googology:ordinal_bricks'),('loquat','googology:loquat')]:
            self.assertEqual(remap_id('googology:'+old),new)
        self.assertEqual(remap_id('googology:bms_matrix'),'googology:bms_matrix')
        self.assertEqual(remap_id('googology:googology'),'googology:outer')

    def test_number_glyph_and_log_axis_survive(self):
        self.assertEqual(walk({'Name':'googology:bashicu_block','Properties':{'digit':'15'}}),
                         {'Name':'googology:ordinal_bricks','Properties':{'number':'15'}})
        self.assertEqual(walk({'Name':'googology:laver_log','Properties':{'axis':'x'}}),
                         {'Name':'googology:laver_vein','Properties':{'axis':'x'}})

    def test_lamp_nbt_all_colors_and_format_generations(self):
        for color,name in enumerate(LAMPS):
            raw=state_blob('googology:christmas_light',{'color':str(color)},True)
            for modern in (True,False):
                expected=state_blob('googology:'+name+'_light',{},modern)
                self.assertEqual(gzip.decompress(nbt_transform(raw,legacy=not modern)),gzip.decompress(expected))
                self.assertEqual(walk({'id':'googology:christmas_light','properties':{'color':str(color)}}),
                                 {'id':'googology:'+name+'_light'})

    def test_matrix_nbt_all_values(self):
        for number in range(16):
            raw=state_blob('googology:bashicu_block',{'digit':str(number)},True)
            expected=state_blob('googology:ordinal_bricks',{'number':str(number)},False)
            self.assertEqual(gzip.decompress(nbt_transform(raw,legacy=True)),gzip.decompress(expected))


if __name__=='__main__':
    unittest.main()
