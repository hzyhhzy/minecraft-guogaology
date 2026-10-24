"""Lamp Shaper geometry checks: square dies and the complete union boundary."""
from collections import Counter
import json
from pathlib import Path
import sys
import unittest

TOOLS=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(TOOLS))
import generate_lamp_shaper as shaper


class LampShaperGeometryTests(unittest.TestCase):
    def test_both_expression_dies_are_six_by_six_with_complete_top_art(self):
        boxes=shaper.bench_boxes()
        dies=[box for box in boxes if box['faces']['up']['texture'] in ('#die','#other')]
        self.assertEqual(len(dies),2)
        model=shaper.resources()['assets/guogaology/models/block/guogao_lamp_shaper.json']
        for die in dies:
            self.assertEqual([die['to'][i]-die['from'][i] for i in (0,2)],[6,6])
            top=[element for element in model['elements'] if
                 element['faces'].get('up',{}).get('texture')==die['faces']['up']['texture']]
            self.assertEqual(len(top),1)
            self.assertEqual(top[0]['faces']['up']['uv'],[0,0,16,16])
            self.assertEqual(top[0]['from'],[die['from'][0],15,die['from'][2]])
            self.assertEqual(top[0]['to'],[die['to'][0],15,die['to'][2]])
        self.assertEqual(model['textures']['die'],'guogaology:block/guogao_lantern')
        self.assertEqual(model['textures']['other'],'guogaology:block/guogao_lantern_sad')

    def test_mesh_is_exactly_one_face_per_exposed_union_patch(self):
        # Independent fixed silhouette, including the corrected right die.
        boxes=[((1,0,2),(4,10,14)),((12,0,2),(15,10,14)),
               ((3,2,5),(13,4,11)),((0,10,0),(16,13,16)),
               ((1,13,1),(15,14,15)),((2,14,3),(8,15,9)),
               ((9,14,3),(15,15,9)),((3,14,12),(13,15,13)),
               ((11,14,11),(13,16,13))]
        occupied={(x,y,z) for a,b in boxes for x in range(a[0],b[0])
                  for y in range(a[1],b[1]) for z in range(a[2],b[2])}
        directions={'west':(0,-1),'east':(0,1),'down':(1,-1),
                    'up':(1,1),'north':(2,-1),'south':(2,1)}
        expected=set()
        for p in occupied:
            for side,(axis,sign) in directions.items():
                neighbor=list(p);neighbor[axis]+=sign
                if tuple(neighbor) not in occupied:
                    u,v=[i for i in range(3) if i!=axis]
                    expected.add((side,p[axis]+(sign>0),p[u],p[v]))
        model=shaper.resources()['assets/guogaology/models/block/guogao_lamp_shaper.json']
        actual=Counter()
        for element in model['elements']:
            self.assertEqual(len(element['faces']),1)
            side,face=next(iter(element['faces'].items()))
            axis,_=directions[side];u,v=[i for i in range(3) if i!=axis]
            self.assertEqual(element['from'][axis],element['to'][axis])
            self.assertTrue(all(0<=value<=16 for value in face['uv']))
            for x in range(element['from'][u],element['to'][u]):
                for y in range(element['from'][v],element['to'][v]):
                    actual[side,element['from'][axis],x,y]+=1
        self.assertEqual(set(actual),expected)
        self.assertEqual(set(actual.values()),{1},'no buried, contact or duplicate patch remains')

    def test_clipped_uvs_keep_the_source_face_orientation(self):
        box=shaper.element([2,4,6],[10,12,14],'frame')
        lo,hi=[2,8,6],[6,12,10]
        expected={'up':[0,0,8,8],'down':[0,8,8,16],
                  'north':[8,0,16,8],'south':[0,0,8,8],
                  'west':[0,0,8,8],'east':[8,0,16,8]}
        for side,uv in expected.items():self.assertEqual(shaper.face_uv(box,side,lo,hi),uv)

    def test_generated_resources_are_current(self):
        shaper.generate(check=True)
        path=shaper.RESOURCE/'assets/guogaology/models/block/guogao_lamp_shaper.json'
        self.assertEqual(json.loads(path.read_text('utf8')),
                         shaper.resources()['assets/guogaology/models/block/guogao_lamp_shaper.json'])


if __name__=='__main__':unittest.main()
