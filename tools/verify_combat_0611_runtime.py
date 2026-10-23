"""Strict postcheck of actual native combat acceptance, not merely a success marker."""
from __future__ import annotations
import json
import math
from pathlib import Path


def verify(game: Path, target: str):
    game = Path(game)
    value = json.loads((game / 'combat0611-native.json').read_text('utf8'))
    assert value['success'] and value['checks'] > 1000
    assert value['realDimensions'] == 4 and value['criticalTrials'] == 48
    assert value['realArrowsReleased'] == 144 and value['realPlayerAttacks'] == 90
    assert value['compressedArrowDiskRoundTrips'] == 4 and value['legacyGearDiskRefreshes'] == 4
    rows = value['cases']
    assert len(rows) == 186
    dimensions = {'minecraft:overworld', 'guogaology:outer', 'guogaology:guogaology', 'guogaology:guogao'}
    assert {r['dimension'] for r in rows} == dimensions
    for row in rows:
        assert math.isfinite(row['actual']) and math.isclose(row['actual'], row['expected'], abs_tol=.003, rel_tol=.00001), row
    final = []
    for dimension in sorted(dimensions):
        deep = dimension in {'guogaology:guogaology', 'guogaology:guogao'}
        expected = {
            ('arrow', 'omega-max-weapon-and-book'): 660.6857023834318 if deep else 56.696938456699066,
            ('sword', 'omega-max-weapon-and-book'): 1627.0982297193164 if deep else 60.15278257941478,
            ('sword', 'external-cap-no-book'): 2048,
            ('sword', 'external-cap-with-bare-book'): 2054.4 if deep else 2052,
            ('arrow', 'powerV-no-book'): 30,
            ('arrow', 'powerV-max-book'): 247.75713839378693 if deep else 48.696938456699066,
        }
        if deep:
            expected[('sword', 'mixed-ordinal-over-native-cap')] = 16*(1+4*math.sqrt(8))*1.4*(1+2*math.sqrt(4))*(1+.4*math.sqrt(2))
        for (kind, name), amount in expected.items():
            matching = [r for r in rows if r['dimension'] == dimension and r['kind'] == kind and r['case'] == name]
            assert len(matching) == 1 and math.isclose(matching[0]['actual'], amount, abs_tol=.003, rel_tol=.00001), matching
            final.extend(matching)
        for tier, amount in enumerate((6, 8, 10, 12), 1):
            matching = [r for r in rows if r['dimension'] == dimension and r['kind'] == 'arrow' and r['case'] == f'bare-tier{tier}']
            assert len(matching) == 1 and math.isclose(matching[0]['actual'], amount, abs_tol=.003), matching
    assert len(list((game / 'combat0611-nbt').glob('*.nbt'))) == 8
    proof = {'success': True, 'minecraft': target, 'version': value['version'], 'checks': value['checks'],
             'realArrowsReleased': value['realArrowsReleased'], 'realPlayerAttacks': value['realPlayerAttacks'],
             'criticalTrials': 48, 'compressedArrowDiskRoundTrips': 4, 'legacyGearDiskRefreshes': 4,
             'numericCases': len(rows), 'keyComparisonCases': final,
             'scope': value['scope']}
    (game / 'combat0611-verification.json').write_text(json.dumps(proof, ensure_ascii=False, indent=2) + '\n', 'utf8')
    return proof


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('game', type=Path)
    parser.add_argument('--target', choices=('26.2', '26.3'), required=True)
    args = parser.parse_args()
    print(json.dumps(verify(args.game, args.target), ensure_ascii=False, indent=2))
