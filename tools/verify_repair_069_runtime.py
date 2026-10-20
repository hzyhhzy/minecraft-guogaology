"""Fail closed on the actual repaired native observations, not runner completion."""
from pathlib import Path
import json


def load(folder, name):
    return json.loads((folder / name).read_text(encoding='utf-8'))


def verify(folder: Path, target: str):
    modern = target == '26.3'
    client = load(folder, 'audit068-client.json' if modern else 'audit068-client-focus.json')
    server = load(folder, 'audit068-server.json' if modern else 'audit068-server-focus.json')
    assert client['completed'] and server['completed']
    if modern:
        assert not client['missingTranslations'] and not client['modelFailures']
        assert client['items'] == 291 and client['modelContexts'] == len(client['models']) == 2037
        assert client['blockStates'] == server['stateCodecRoundTrips']['states'] == 2722
        assert all(not row['empty'] for row in client['models'])
        assert len(server['suites']) == 6 and {row['suite'] for row in server['suites']} == {
            'native-recipes', 'inventory-menu-actions', 'native-enchant-socket-exclusion',
            'manuscript-effects-and-ownership', 'bow-launch-hit-reload', 'survival-harvest-and-experience'}
        assert all(row['passed'] for row in server['suites']), server['suites']
        assert not server['numericMismatches'], server['numericMismatches']
        assert not server['stateCodecRoundTrips']['failures']
        assert len(server['allNativeCraftingRecipes']) == 145
        assert all(row['matched'] and row['outputMatches'] for row in server['allNativeCraftingRecipes'])
        assert len(server['deathDuringManuscriptEdit']) == 4
        assert all(row['exactMultisetConserved'] and row['duplicateCloseConserved'] for row in server['deathDuringManuscriptEdit'])
        combat = server['nativeMeleeBurstCooldown']
    else:
        assert all(row['translationPresent'] for row in client['boats'])
        combat = load(folder, 'audit068-combat-focus.json')['nativeMeleeBurstCooldown']
    placements = server['nativeFourWayPlacements' if modern else 'placements']
    assert len(placements) == 20
    clockwise = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}
    mirror = {'north': 'north', 'south': 'south', 'east': 'west', 'west': 'east'}
    ids = {'server_rack', 'office_monitor', 'chair', 'nuke_chair', 'snake_head'}
    assert {row['block'] for row in placements} == ids
    assert {(row['block'], row['playerYaw' if modern else 'yaw']) for row in placements} == {
        (name, yaw) for name in ids for yaw in (0, 90, 180, 270)}
    for row in placements:
        yaw = row['playerYaw' if modern else 'yaw']
        expected = ['north', 'east', 'south', 'west'][int(yaw // 90)]
        assert row['placed'] and row['facing'] == expected, row
        assert row['rotatedFacing'] == clockwise[expected], row
        assert row['mirroredFacing'] == mirror[expected], row
    stripping = server['axeStripping']
    assert len(stripping) == 12
    assert {(row['input' if modern else 'block'], row['axis']) for row in stripping} == {
        (name, axis) for name in ('laver_vein', 'laver_wood', 'dread_log', 'dread_wood') for axis in 'xyz'}
    for row in stripping:
        name = row['input' if modern else 'block']
        family = 'laver' if name.startswith('laver_') else 'dread'
        form = 'wood' if name.endswith('_wood') else 'log'
        assert row['result'] == f'guogaology:{family}_stripped_{form}', row
        assert row['axis'] == row['resultAxis'], row
    assert len(combat) == 2
    for row in combat:
        assert row['actualNativeDirectAccepted'] and row['targetsQueryVisible'], row
        assert abs(row['directObserved'] - row['directExpected']) < 1e-4, row
        for key in ('additionalOnDirectTarget', 'additionalOnNeighbor'):
            assert abs(row[key] - row['additionalExpected']) < 1e-4, row
        assert row['cooldownAfterBurst'] == (0 if row['controlClearsActualDamageCooldown'] else row['cooldownAfterDirect']), row
    result = {'passed': True, 'minecraft': target, 'placements': 20,
              'axisPreservingStrips': 12, 'meleeControls': 2,
              'bowNumericFailures': 0 if modern else None,
              'scope': 'unchanged published audit observations are not accepted as repair verification'}
    (folder / 'repair069-native-verification.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    return result
