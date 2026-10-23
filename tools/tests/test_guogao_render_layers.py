"""Guard the solid relief/glass boundary without changing authored geometry."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from repair_guogao_render_layers import ASSETS, NAME, ROOT, generate, repaired
from prepare_port_sources import adapt


class GuogaoRenderLayers(unittest.TestCase):
    def test_current_art_partition_and_frozen_069_geometry(self):
        mesh = generate(check=True)
        self.assertEqual(sum(q.get('render_layer') == 'solid' for q in mesh['quads']), 736)
        self.assertEqual(sum('render_layer' not in q for q in mesh['quads']), 114)
        for q in mesh['quads']:
            q.pop('render_layer', None)
        digest = hashlib.sha256(json.dumps(mesh, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        self.assertEqual(digest, 'd9069c7ee30f0ff65919433894155577e9bfee4a079f6e6fdae6611d3d295695',
                         'Layer repair must not edit vertices, glass, UVs, normals, LOD or colors')

    def test_translucent_pixels_and_vertex_alpha_stay_in_glass_pass(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'build') as directory:
            assets = Path(directory)/'assets/guogaology'
            (assets/'textures').mkdir(parents=True)
            Image.new('RGBA', (1, 1), (30, 80, 90, 255)).save(assets/'textures/solid.png')
            Image.new('RGBA', (1, 1), (30, 80, 90, 120)).save(assets/'textures/glass.png')
            sample = {'part': 0, 't': 'solid', 'c': [0xffffffff]*4, 'v': [[0,0,0]]*4}
            vertex = copy.deepcopy(sample); vertex['c'][2] = 0x80ffffff
            glass = copy.deepcopy(sample); glass['t'] = 'glass'
            mesh = {'textures': {'solid': 'guogaology:solid', 'glass': 'guogaology:glass'},
                    'quads': [sample, vertex, glass]}
            fixed = repaired(mesh, assets)
            self.assertEqual(fixed['quads'][0]['render_layer'], 'solid')
            self.assertNotIn('render_layer', fixed['quads'][1])
            self.assertNotIn('render_layer', fixed['quads'][2])
            self.assertNotIn('render_layer', mesh['quads'][0], 'No input mutation')
            mesh['quads'][0]['part'] = 1
            with self.assertRaisesRegex(ValueError, 'moving part'):
                repaired(mesh, assets)

    def test_modern_adapters_keep_per_face_solid_choice_and_sodium_workaround(self):
        source = (ROOT/'ports/common/client/dev/guogaology/client/CoreMeshModels.java').read_text(encoding='utf-8')
        self.assertIn('opaque?ChunkSectionLayer.SOLID:ChunkSectionLayer.TRANSLUCENT', source)
        for target in ('1.21.11', '26.2', '26.3'):
            result = adapt(source, target, 'CoreMeshModels.java')
            if target in ('26.2', '26.3'):
                self.assertIn('.chunkLayer(opaque?ChunkSectionLayer.SOLID:ChunkSectionLayer.TRANSLUCENT)', result)
                self.assertNotIn('.renderLayer(', result)
                self.assertIn('new Material.Baked(material.sprite(),false):material', result,
                              'postMaterialBake must not force explicitly opaque faces back into transparency')
            if target == '26.3':
                self.assertIn('quad.pos(0,quad.x(0),quad.y(0),quad.z(0))', result)
            self.assertIn('opaque=source.opaque', result)


if __name__ == '__main__':
    unittest.main()
