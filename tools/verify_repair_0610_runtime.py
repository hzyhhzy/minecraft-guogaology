"""Require the new native0610 checks, not a success marker alone."""
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def verify_wood(server, client):
    families = {'laver', 'dread'}
    fuel = server['fuel']
    names = {'vein', 'wood', 'stripped_log', 'stripped_wood', 'planks', 'stairs', 'slab',
             'fence', 'fence_gate', 'trapdoor', 'button', 'pressure_plate', 'door', 'sign',
             'hanging_sign', 'sapling', 'boat', 'chest_boat'}
    assert len(fuel) == 18 and {row['item'] for row in fuel} == {'laver_' + name for name in names}
    assert all(row['actualTicks'] == row['nativeTicks'] > 0 for row in fuel)
    fences = server['fences']
    neighbors = {'laver_fence', 'dread_fence', 'minecraft:oak_fence', 'minecraft:nether_brick_fence'}
    assert len(fences) == 4 and {row['neighbor'] for row in fences} == neighbors
    assert all(row['east'] == row['west'] == row['expected'] ==
               (row['neighbor'] != 'minecraft:nether_brick_fence') for row in fences)
    assert server['ordinarySignNoCollisionStates'] == 80
    signs = server['hangingSigns']
    assert len(signs) == 4 and {row['block'] for row in signs} == {
        family + suffix for family in families for suffix in ('_hanging_sign', '_wall_hanging_sign')}
    assert all(all(row[key] for key in ('nativeItemPlacement', 'nativeBlockEntity',
                                      'diskNbtBothSides', 'nativeCollisionMatches')) and
               row['collisionEmpty'] == row['nativeCollisionEmpty'] for row in signs)
    boats = server['chestBoats']
    assert len(boats) == 2 and {row['wood'] for row in boats} == families
    assert all(row['slots'] == 27 and all(row[key] for key in
               ('actualNativeMenu', 'diskNbtRoundtrip', 'destructionConserved')) for row in boats)
    textures = client['textures']
    assert len(textures) == 2 and {row['wood'] for row in textures} == families
    assert all(row['wholeDoorIcon'] and row['guiMatchesLightFacePixels'] == 288 for row in textures)


def verify(game, target):
    game = Path(game)
    evidence = {}
    for role in ('server', 'client'):
        result = read(game / f'repair0610-{role}-ok.json')
        assert result['success'] and result['version'] == '0.6.10', role
        assert result['wood']['passed'], role
        evidence[role] = result
    verify_wood(evidence['server']['wood'], evidence['client']['wood'])
    if target == '26.2':
        control = evidence['server']['wood']['nativeChestBoatControl']
        assert control['wood'] == 'oak' and control['slots'] == 27
        assert all(control[key] for key in ('actualNativeMenu', 'diskNbtRoundtrip', 'destructionConserved'))
    pickup = read(game / 'bow0610-server.json')
    draw = read(game / 'bow0610-client.json')
    assert pickup['passed'] and len(pickup['tiers']) == 4
    assert {row['tier'] for row in pickup['tiers']} == {1, 2, 3, 4}
    assert all(all(row[key] for key in ('creativePickup', 'paidSurvivalPickup',
                                      'infiniteSurvivalPickupBlocked', 'specialAmmoConserved'))
               for row in pickup['tiers'])
    assert draw['passed'] and len(draw['models']) == 280
    identities = {(row['tier'], row['hand'], row['drawTicks'], row['context']) for row in draw['models']}
    assert len(identities) == 280, 'No duplicated/missing native bow draw contexts'
    small = read(game / 'small0610-server.json')
    assert small['passed']
    assert small['tnt']['validSandMixtures'] == 16
    assert small['tnt']['rejectedMissingOrWrongGrids'] == 288
    assert small['gummyJar']['actualPrimaryBreaks'] == 360
    assert small['gummyJar']['actualSpreadStoneBreaks'] == 56
    assert small['coreShapes']['absenceStates'] == 12
    assert small['coreShapes']['neighborCoreStates'] == 49
    if target == '26.3':
        client = read(game / 'audit068-client.json')
        assert client['items'] == 295 and client['modelContexts'] == 295 * 7
        assert len(client['models']) == 2065 and client['blockStates'] == 2866
        assert not client['missingTranslations'] and not client['modelFailures']
        assert all(not row['empty'] for row in client['models'])
        server = read(game / 'audit068-server.json')
        assert len(server['suites']) == 6 and all(row['passed'] for row in server['suites'])
        assert not server['numericMismatches']
        assert not server['stateCodecRoundTrips']['failures']
        assert len(server['deathDuringManuscriptEdit']) == 4
        assert all(row['exactMultisetConserved'] and row['duplicateCloseConserved']
                   for row in server['deathDuringManuscriptEdit'])
        assert len(client['blockStateProperties']) == 230
        assert server['stateCodecRoundTrips']['states'] == client['blockStates']
        assert len(server['allNativeCraftingRecipes']) == 149
        assert all(row['matched'] and row['outputMatches'] and 'error' not in row
                   for row in server['allNativeCraftingRecipes'])
    result = {'passed': True, 'minecraft': target, 'version': '0.6.10',
              'creativeAndSurvivalArrowPickup': True, 'nativeBowDrawCases': 280,
              'sandMixtures': 16, 'rejectedCraftingGrids': 288,
              'actualGummyJarBreaks': 360, 'extraMiningStoneBreaks': 56,
              'absenceStates': 12, 'otherCoreStates': 49, 'strictWood': evidence}
    (game / 'repair0610-native-verification.json').write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return result
