"""Real geometry regressions for the offline core overlap surgery."""
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sanitize_core_shells import sanitize
from audit_core_surfaces import face_polygons, signed_area


def face(points, part=0, reverse=False):
    if reverse:
        points = list(reversed(points))
    return {'v': points, 'n': [[0, 0, -1 if reverse else 1]]*4,
            'uv': [[p[0], p[1]] for p in points], 'c': [0xffffffff]*4,
            't': 'glass', 'part': part}


def total_area(data):
    return sum(abs(signed_area(f['poly'])) / abs(f['plane'][f['axis']])
               for i, q in enumerate(data['quads']) for f in face_polygons(q, i)[0])


class SurfaceSanitizerTest(unittest.TestCase):
    def test_overlap_keeps_union_and_uv(self):
        a = face([[0, 0, 0], [2, 0, 0], [2, 2, 0], [0, 2, 0]])
        b = face([[1, 1, 0], [3, 1, 0], [3, 3, 0], [1, 3, 0]])
        result, stats = sanitize({'quads': [a, b]})
        self.assertEqual(1, stats['changed_faces'])
        self.assertAlmostEqual(7, total_area(result))
        for q in result['quads']:
            for v, uv in zip(q['v'], q['uv']):
                self.assertEqual(v[:2], uv)
        self.assertEqual(result, sanitize(result)[0])

    def test_opposite_backside_and_independent_motion_are_retained(self):
        vertices = [[0, 0, 0], [2, 0, 0], [2, 2, 0], [0, 2, 0]]
        data = {'quads': [face(vertices), face(vertices, reverse=True), face(vertices, part=2)]}
        result, stats = sanitize(data)
        self.assertEqual(data, result)
        self.assertEqual(0, stats['changed_faces'])

    def test_planar_crossed_beam_reorders_perimeter_with_vertex_material(self):
        q = face([[0, 0, 0], [2, 2, 0], [0, 2, 0], [2, 0, 0]])
        result, stats = sanitize({'quads': [q]})
        self.assertEqual(1, stats['changed_faces'])
        self.assertEqual(1, len(result['quads']))
        self.assertEqual(1, stats['repaired_beam_sides'])
        self.assertAlmostEqual(4, total_area(result))
        self.assertEqual(0, stats['redundant_area_removed'])
        self.assertEqual(result, sanitize(result)[0])

    def test_diagonal_basis_does_not_trim_disjoint_beams(self):
        a = face([[2.77107, 2.13693, 8.4484], [5.37769, 4.74355, 8.4484],
                  [5.37769, 4.74355, 7.5516], [2.77107, 2.13693, 7.5516]])
        b = face([[5.76591, 5.13177, 8.4484], [8.53891, 7.90477, 8.4484],
                  [8.53891, 7.90477, 7.5516], [5.76591, 5.13177, 7.5516]])
        data = {'quads': [a, b]}
        self.assertEqual(data, sanitize(data)[0])

    def test_reviewed_solid_caps_remove_only_hidden_contact(self):
        a = face([[0, 0, 0], [2, 0, 0], [2, 2, 0], [0, 2, 0]], part=1)
        b = face([[1, 1, 0], [3, 1, 0], [3, 3, 0], [1, 3, 0]], part=1, reverse=True)
        a['t'] = b['t'] = 'optical_clean_255'
        result, stats = sanitize({'quads': [a, b]}, 'sequence_core_lv2')
        self.assertAlmostEqual(6, total_area(result))
        self.assertAlmostEqual(2, stats['internal_cap_area_removed'])
        self.assertEqual(result, sanitize(result, 'sequence_core_lv2')[0])

    def test_reviewed_caps_do_not_remove_complete_two_sided_page(self):
        vertices = [[0, 0, 0], [2, 0, 0], [2, 2, 0], [0, 2, 0]]
        a, b = face(vertices, part=1), face(vertices, part=1, reverse=True)
        a['t'] = b['t'] = 'optical_clean_255'
        result, stats = sanitize({'quads': [a, b]}, 'sequence_core_lv2')
        self.assertEqual([a['v'], b['v']], [q['v'] for q in result['quads']])
        self.assertEqual(0, stats['internal_cap_area_removed'])
        self.assertEqual(result, sanitize(result, 'sequence_core_lv2')[0])


if __name__ == '__main__':
    unittest.main()
