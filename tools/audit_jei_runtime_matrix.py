"""Independently certify completed 26.3 JEI cases, not just a success marker."""
from __future__ import annotations

import argparse
from hashlib import sha256
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CLIENTS = {
    '0.6.8': 'c15eb5f74ee645ab6253ec5333fd35fd9760cd97faf5e9ed408ac0d85beeabff',
    '0.6.1a': 'fc9863898c01eda5a6e6f00070a850f7064fec9797c708febc6229de025637f5',
}
SERVERS = {**CLIENTS,
    '0.6.0': 'a101bfd94cdf5cb27646bba2b10f881665bad89e180e6e59cf67121cdf83fe39',
    '0.6.1': '1cf73cbd73b65bb04870f4b0c0dc5d772573a3d486196acaacc2c8b2e5b0ea30',
    '0.6.7': '2d10fab0c7fae647064c4af9ab3530da252e290f685335759a79612bda371b98',
}
HELPER = '45a1376ab382be6a92687c952e01c2192eace450f25421f2a30d2925d01d5111'
BRANCHES = {'guogaology:' + name for name in (
    'epsilon_symbol', 'omega_symbol', 'great_omega_symbol', 'zeta_symbol', 'psi_symbol', 'phi_symbol')}


def read(path):
    return json.loads(path.read_text('utf-8-sig'))


def audit_case(folder, listening_ports):
    report = read(folder / 'network-result.json')
    certificate = read(folder / 'network-verification-ok.json')
    assert report['candidateRegression'] and report['success']
    version, server = report['version'], report['serverProductionVersion']
    assert certificate['inputSha256'] == {'client': CLIENTS[version], 'server': SERVERS[server], 'qa': HELPER}
    assert report['productionSha256'] == CLIENTS[version] and report['serverProductionSha256'] == SERVERS[server]
    assert all(certificate[key] is True for key in (
        'runnerProtectionComplete', 'nativeVerificationComplete', 'normalInstanceUnchanged', 'portClosedAfterShutdown'))
    assert certificate['clientExit'] == certificate['serverExit'] == 0 and certificate['verifiedPickupStages'] == 2
    assert read(folder / 'normal-instance-unchanged.json')['unchanged'] is True
    client_jei = [entry for entry in report['clientModMetadata'] if entry['id'] == 'jei']
    server_jei = [entry for entry in report['serverModMetadata'] if entry['id'] == 'jei']
    mode = 'both' if client_jei and server_jei else 'client' if client_jei else 'server' if server_jei else 'none'
    assert report['jeiMode'] == mode
    jei = (client_jei or server_jei or [{}])[0].get('version')
    for role, mod_entries in (('client', report['clientModMetadata']), ('server', report['serverModMetadata'])):
        for entry in mod_entries:
            actual = folder / role / 'mods' / entry['file']
            assert sha256(actual.read_bytes()).hexdigest() == entry['sha256'], f'Mod input changed: {actual}'
    assert report['bind'] == '127.0.0.1'
    assert report['listeners'] and all(row[1].startswith('127.0.0.1:') for row in report['listeners'])
    assert report['port'] not in listening_ports, 'QA port still has a TCP listener'
    numeric = report['clientResult']
    assert numeric['twoPickupsOneConnection'] is True
    phases = ((numeric['branchPhase'], '', report['pickupItem']), (numeric, '-numeric', 'guogaology:ordinal_stone'))
    fingerprints = []
    recipe_queries = 0
    for phase, suffix, pickup in phases:
        assert phase['success'] and phase['pickupItem'] == pickup
        assert all(phase[key] is True for key in ('realTcp', 'hidden', 'notFocusable', 'mouseUnlocked'))
        assert phase['integratedServer'] is False
        before = read(folder / 'server' / f'network-server-before{suffix}.json')
        ready = read(folder / 'client' / f'network-client-ready{suffix}.json')
        after = read(folder / 'server' / f'network-server-after{suffix}.json')
        assert after['actualItemEntityPickedUp'] and after['inventoryHasPickupItem']
        assert ready['targetDisplaysAbsentBefore'] == phase['expectedDisplayIds']
        assert set(phase['expectedDisplayIds']) <= set(phase['receivedDisplayIds'])
        fingerprints.extend(snapshot['staticTagFingerprints'] for snapshot in (before, ready, after, phase))
        assert set(phase['branchTagMembers']) == BRANCHES
        assert len(phase['numberTagMembers']) == 10 and all(phase['numberMembership'].values())
        if suffix == '':
            assert phase['hydraDisplay']['namedBranchSlots'] == 4
        assert phase['jei']['installed'] is bool(client_jei)
        assert after['jeiInstalled'] is bool(server_jei)
        if client_jei:
            assert phase['jei']['checked'] and phase['jei']['actualRecipeScreen'] == 'mezz.jei.gui.recipes.RecipesGui'
            assert phase['jeiGuiSettleTicks'] >= 90
            fingerprints.append(phase['settledStaticTagFingerprints'])
            recipe_queries += len(phase['jei']['recipes'])
        if report['fullRecipeSyncExpected']:
            assert phase['fabricRecipeSync']['recipeIds'] == before['serverRecipeIds']
        screenshot = folder / 'client/screenshots' / phase['screenshot']
        assert screenshot.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    assert all(value == fingerprints[0] for value in fingerprints), 'Full tag set drifted during a connection'
    log = (folder / 'client/client-output.log').read_text('utf8', errors='replace')
    assert 'Skipping optional JEI' not in log, 'Known JEI structure was not accepted'
    if server == '0.6.0' and mode == 'client':
        assert report['fallbackConfirmed'] is True
        assert all(phase['fabricRecipeSync']['count'] == 0 for phase, _, _ in phases)
        assert f'Protecting server tags in JEI {jei}: compatible fallback bytecode' in log
    item_regression = report.get('itemRegression', {'mode': 'off'})
    if item_regression['mode'] == 'verify':
        assert version == server and certificate['itemRegression'] == item_regression
        assert item_regression['serverExpectedMatches'] and item_regression['clientExpectedMatches']
        assert item_regression['serverCases'] == 70 and item_regression['nativeModels'] == 56
    return {'label': folder.name, 'client': version, 'server': server, 'placement': mode, 'jei': jei,
            'loader': report['expectedLoaderVersion'], 'fabricApi': report['expectedFabricApiVersion'],
            'branchPickup': report['pickupItem'], 'verifiedPickupStages': 2, 'jeiRecipeQueries': recipe_queries,
            'fallbackConfirmed': report['fallbackConfirmed'], 'tagFingerprints': fingerprints[0],
            'itemRegression': item_regression, 'success': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--partial', action='store_true', help='Inspect finished cases without claiming complete matrix coverage')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/jei-068/runtime-summary.json')
    parser.add_argument('--runner-summary', type=Path, action='append', default=[],
                        help='Actual subprocess runner results, mandatory for full certification')
    args = parser.parse_args()
    base = ROOT / 'build/network-qa263'
    # A timed-out Windows connect_ex may return WSAEWOULDBLOCK, which is not
    # proof of refusal. Inspect the local TCP listener inventory instead.
    listening_ports = {int(row[1].rsplit(':', 1)[1]) for line in
                       subprocess.check_output(['netstat', '-ano', '-p', 'tcp'], text=True).splitlines()
                       if (row := line.split()) and row[0] == 'TCP' and 'LISTENING' in row}
    completed, excluded, pending, intermediate = [], [], [], []
    for folder in sorted(base.iterdir()):
        if not folder.is_dir() or not folder.name.startswith(('fix068-', 'fix061a-', 'final068-', 'final061a-')):
            continue
        if (folder / 'intermediate-candidate.json').exists():
            assert read(folder / 'intermediate-candidate.json')['countAsFinal'] is False
            intermediate.append(folder.name)
        elif (folder / 'excluded-control.json').exists():
            excluded.append(folder.name)
        elif (folder / 'network-verification-ok.json').exists():
            completed.append(audit_case(folder, listening_ports))
        else:
            pending.append(folder.name)
    if not args.partial:
        assert not pending, f'Uncertified cases remain: {pending}'
        assert len(completed) >= 22, 'Incomplete planned case set'
        for version in CLIENTS:
            rows = [row for row in completed if row['client'] == version]
            for server in (version, '0.6.0'):
                assert {row['placement'] for row in rows if row['server'] == server} == {'none', 'client', 'server', 'both'}
            assert any(row['server'] == '0.6.1' for row in rows)
            assert {row['branchPickup'] for row in rows} == BRANCHES
            assert any(row['jei'] == '31.14.0.72' and row['fallbackConfirmed'] for row in rows)
        assert any(row['client'] == '0.6.8' and row['server'] == '0.6.7' for row in completed)
        assert any(row['loader'] == '0.19.5' and row['fabricApi'] == '0.161.0+26.3' for row in completed)
        assert {row['client'] for row in completed if row['itemRegression']['mode'] == 'verify'} == set(CLIENTS)
        assert args.runner_summary, 'Actual runner exit results are mandatory'
    runner_rows = {}
    for source in args.runner_summary:
        run = read(source)
        for row in run['cases']:
            if row.get('passed') is True:
                assert row['label'] not in runner_rows, 'Duplicate actual-run claim'
                assert row['returnCode'] == 0 and row['elapsedSeconds'] > 0 and row['startUtc'] < row['endUtc']
                runner_rows[row['label']] = row
    for row in completed:
        if not args.partial or args.runner_summary:
            actual = runner_rows[row['label']]
            assert actual['result']['productionSha256'] == CLIENTS[row['client']]
            assert actual['result']['serverProductionSha256'] == SERVERS[row['server']]
            row['runnerExit'] = actual['returnCode']
            row['elapsedSeconds'] = actual['elapsedSeconds']
    result = {'success': not args.partial, 'partial': args.partial, 'actualNativeCases': len(completed),
              'verifiedPickupStages': sum(row['verifiedPickupStages'] for row in completed),
              'jeiRecipeQueries': sum(row['jeiRecipeQueries'] for row in completed),
              'excludedStartupOrInterruptedCases': excluded, 'earlierIntermediateCases': intermediate,
              'runnerSummaries': [str(path) for path in args.runner_summary],
              'pending': pending, 'cases': completed}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', 'utf8')
    print(json.dumps({key: value for key, value in result.items() if key != 'cases'}, ensure_ascii=False))


if __name__ == '__main__':
    main()
