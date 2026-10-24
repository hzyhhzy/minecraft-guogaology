"""Postcheck native mining-pattern observations, exact action cases and persistence files."""
from pathlib import Path
import json


def verify(game: Path, target: str, release: str = '0.6.12'):
    game = Path(game)
    result = json.loads((game / 'mining0612-native.json').read_text('utf8'))
    assert result['success'] and result['version'] == release
    assert result['checks'] >= 180 and result['nativeActionPackets'] == 68
    assert result['compressedItemDiskRoundTrips'] == 3 and result['menuSessions'] == 7
    assert result['realDeathCases'] == 2
    cases = result['cases']
    direction = [r for r in cases if r['case'] == 'direction']
    assert len(direction) == 12 and {(r['heading'], r['pitch']) for r in direction} == {
        (heading, pitch) for heading in range(4) for pitch in (-80, 0, 80)}
    assert all(r['broken'] == 5 and r['downwardCellBroken'] and r['yawChangedAfterStart']
               and 0 < r['elapsedNanos'] < 2_000_000_000 for r in direction)
    assert sum(r.get('redstoneLitMutation', False) for r in direction) == 1
    capacity = [r for r in cases if r['case'] == 'capacity']
    assert len(capacity) == 11 and {r['extras'] for r in capacity} == set(range(1, 12))
    cells = [[], [17], [7, 17], [11, 13, 17], [7, 11, 13, 17], [11, 13, 16, 17, 18],
             [7, 11, 13, 16, 17, 18], [6, 8, 11, 13, 16, 17, 18],
             [6, 7, 8, 11, 13, 16, 17, 18], [2, 6, 7, 8, 11, 13, 16, 17, 18],
             [1, 3, 6, 7, 8, 11, 13, 16, 17, 18], [1, 2, 3, 6, 7, 8, 11, 13, 16, 17, 18]]
    assert all(r['reference'] == 12 and r['mask'] == sum(1 << c for c in cells[r['extras']]) and
               r['totalBroken'] == r['cobblestoneDrops'] == r['extras'] + 1 for r in capacity)
    policies = [r for r in cases if r['case'] == 'strict-target']
    assert len(policies) == 12 and {r['policy'] for r in policies} == {
        'air', 'bedrock', 'container', 'wood', 'core', 'higherOre', 'sneak',
        'empty', 'aborted', 'withoutStart', 'worldBorder', 'protectedEvent'}
    assert all(r['fallbackUntouched'] and r['primaryWear'] == 1 for r in policies)
    assert len(list((game / 'mining0612-nbt').glob('*.nbt'))) == 3
    persistence = json.loads((game / 'mining0612-persistence.json').read_text('utf8'))
    assert len(persistence) == 3 and all(all(row[key] for key in (
        'canonicalNativeNbtEqual', 'nativeCoreListMatches', 'nonCoreComponentsMatch', 'patternMatches'))
        for row in persistence)
    visual = json.loads((game / 'mining0612-visual.json').read_text('utf8'))
    assert all(visual[key] for key in ('success', 'serverSynchronizedNativeMenu',
                                      'nativeClientButtonPacket', 'actualProductionPreviewExtraction'))
    photos = visual['pictures']
    assert len(photos) == 3 and {row['file'] for row in photos} == {
        'mining0612-editor-zh.png', 'mining0612-editor-en.png', 'mining0612-world-preview.png'}
    assert {row['language'] for row in photos} == {'zh_cn', 'en_us'}
    assert all((game / 'screenshots' / row['file']).stat().st_size > 1000 for row in photos)
    proof = {'success': True, 'minecraft': target, 'version': release,
             'checks': result['checks'], 'nativeActionPackets': result['nativeActionPackets'],
             'directionAndPitchCases': len(direction), 'exactCapacityCases': len(capacity),
             'strictTargetPolicies': len(policies), 'nativeDeathCases': 2,
             'compressedItemDiskRoundTrips': 3,
             'maximumActionMillis': max(r['elapsedNanos'] for r in direction) / 1e6,
             'totalServerProbeMillis': result['elapsedNanos'] / 1e6,
             'visual': visual, 'scope': result['scope']}
    (game / 'mining0612-verification.json').write_text(json.dumps(proof, ensure_ascii=False, indent=2) + '\n', 'utf8')
    return proof


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('game', type=Path)
    parser.add_argument('--target', choices=('26.2', '26.3'), required=True)
    parser.add_argument('--release', default='0.6.12')
    args = parser.parse_args()
    print(json.dumps(verify(args.game, args.target, args.release), ensure_ascii=False, indent=2))
