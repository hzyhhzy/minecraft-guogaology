"""Verify repaired native 26.3/26.2 pattern-window inputs on exact 0.7.1 packages.

Reads only installed client metadata/libraries and selected Fabric API/Sodium JARs.
All generated settings, stacks, compressed NBT samples and worlds stay under build/.
No accounts, normal configuration or saves are copied or opened.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse
import json
import os
import re
import shutil
import subprocess

import run_release_263_qa as base
from run_tooltip_0615_qa import snapshot

RELEASE = '0.7.1'
CASES = [
    'native_escape', 'native_inventory_key', 'native_left_back',
    'native_left_select', 'native_left_deselect', 'native_right_reference',
    'native_right_selected_reference', 'native_clear', 'native_reset',
    'reference_left_noop', 'middle_button_noop', 'budget_four_rejects_fifth',
    'server_close', 'reinit_then_back', 'repeated_real_button_entry',
    'edited_pattern_reentry',
]


def run(args):
    target = args.minecraft
    instance = args.instance.resolve()
    expected = 'fo263_2' if target == '26.3' else 'fo262'
    base.require(instance.name == expected and instance.parent.name == 'versions', 'Exact read-only native installation required')
    base.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*', args.label) and not args.label.endswith(('.', ' ')), 'Fresh safe directory label required')
    base.require(args.label.upper().split('.')[0] not in {'CON', 'PRN', 'AUX', 'NUL', *(f'COM{i}' for i in range(1, 10)), *(f'LPT{i}' for i in range(1, 10))}, 'Invalid Windows directory label')
    base.require(args.timeout > 0, 'Positive own-JVM timeout required')
    root = base.ROOT
    current = re.search(r'^mod_version\s*=\s*(\S+)\s*$', (root / 'gradle.properties').read_text('utf-8-sig'), re.M)[1]
    base.require(current == RELEASE, 'Exact 0.7.1 working release required')
    main = root / f'ports/{target}/build/libs/guogaology-dimension-{target}-{RELEASE}.jar'
    qa_id = 'guogaology-release-263-qa' if target == '26.3' else 'guogaology-merge-qa'
    helper = root / f'ports/{target}/build/qa/{qa_id}-{RELEASE}.jar'
    entry = ('dev.guogaology.releaseqa263.' if target == '26.3' else 'dev.guogaology.mergeqa.') + 'PatternWindow071Checks'
    metadata = base.read_json(instance / (expected + '.json'))
    client = instance / (expected + '.jar')
    minecraft = instance.parent.parent
    base.require(metadata['mainClass'] == 'net.fabricmc.loader.impl.launch.knot.KnotClient', 'Installed Fabric Knot main class required')
    base.require(metadata.get('javaVersion', {}).get('majorVersion') == 25, 'Native Java25 metadata required')
    with ZipFile(client) as archive:
        base.require(json.loads(archive.read('version.json'))['id'] == target, 'Actual native client target mismatch')
    info = base.mod_metadata(main)
    base.require(info['id'] == 'guogaology' and info['version'] == RELEASE and info['depends']['minecraft'] == target, 'Exact production identity required')
    helper_info = base.mod_metadata(helper)
    base.require(helper_info['id'] == qa_id and helper_info['depends']['minecraft'] == target, 'Exact target QA identity required')
    with ZipFile(helper) as archive:
        base.require(entry.replace('.', '/') + '.class' in archive.namelist(), 'Build the new focused native QA helper first')
    production_hash = base.sha256(main)
    helper_hash = base.sha256(helper)
    runtime = (root / ('build/runtime-qa263' if target == '26.3' else 'build/runtime-qa')).resolve()
    game = (runtime / args.label).resolve()
    base.require(game.parent == runtime and not game.exists(), 'Fresh direct-child disposable directory required')
    before = snapshot(instance)
    game.mkdir(parents=True)
    mods = game / 'mods'; mods.mkdir()
    temp = game / 'temp'; temp.mkdir()
    (game / 'config').mkdir()
    inputs = {'minecraft': target, 'release': RELEASE, 'productionVersion': RELEASE, 'productionSha256': production_hash, 'helperSourceSha256': helper_hash, 'helperMetadataVersion': helper_info['version'], 'normalReadOnlyInstance': str(instance), 'mods': []}
    try:
        for source, mod in [(main, info), (helper, helper_info), *base.selected_mods(instance, args)]:
            dest = mods / source.name
            base.require(not dest.exists(), 'Unique staged artifacts required')
            if source == helper:
                mod = json.loads(json.dumps(mod)); mod['entrypoints'] = {'client': [entry]}
                with ZipFile(source) as src, ZipFile(dest, 'x') as dst:
                    for item in src.infolist():
                        dst.writestr(item, json.dumps(mod, ensure_ascii=False, indent=2).encode() if item.filename == 'fabric.mod.json' else src.read(item.filename))
            else:
                shutil.copy2(source, dest)
                base.require(base.sha256(source) == base.sha256(dest), 'Exact staged artifact copy required')
            inputs['mods'].append({'id': mod['id'], 'metadataVersion': mod['version'], 'file': dest.name, 'sha256': base.sha256(dest), 'sourceSha256': base.sha256(source)})
        base.write_json_new(game / 'normal-instance-before.json', before)
        base.write_json_new(game / 'pattern-window071-inputs.json', inputs)
        if target == '26.3':
            base.write_json_new(game / 'qa263-isolated.json', {'mode': 'pattern-window071', 'minecraft': target, 'release': RELEASE})
        texture = 'textureFiltering:1\nmaxAnisotropyBit:1\n' if target == '26.3' else ''
        (game / 'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:3\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:zh_cn\nguiScale:3\n' + texture, encoding='utf-8')
        java = base.java_executable(args.java_home)
        command = [str(java), '-Xmx4G', '-XX:StackShadowPages=32', '--enable-native-access=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.misc=ALL-UNNAMED', '-Dfabric.development=false', '-Dfile.encoding=UTF-8', '-Dguogaology.patternWindow071=true', f'-Djava.io.tmpdir={temp}', f'-Djna.tmpdir={temp}', f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}', '-cp', ';'.join(map(str, base.library_classpath(metadata, minecraft, client))), metadata['mainClass'], '--username', 'GuogaologyQA', '--version', target, '--gameDir', str(game), '--assetsDir', str(minecraft / 'assets'), '--assetIndex', metadata['assetIndex']['id'], '--uuid', '00000000000000000000000000000001', '--accessToken', '0', '--userType', 'legacy', '--versionType', 'release', '--width', '1600', '--height', '1000']
        base.write_json_new(game / 'staging.json', {'inputs': inputs, 'command': command})
        if args.prepare_only:
            print('PATTERN_WINDOW071_STAGED ' + str(game), flush=True)
            return
        env = os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
            env.pop(key, None)
        env.update(TEMP=str(temp), TMP=str(temp))
        with (game / 'launch-output.log').open('x', encoding='utf-8') as output:
            proc = subprocess.Popen(command, cwd=game, env=env, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            base.write_json_new(game / 'qa-process.json', {'pid': proc.pid, 'gameDirectory': str(game), 'minecraft': target, 'mode': 'pattern-window071'})
            try:
                code = proc.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try: proc.wait(timeout=15)
                except subprocess.TimeoutExpired: proc.kill(); proc.wait(timeout=15)
                raise RuntimeError('Own isolated native pattern-input JVM timed out')
        base.require(code == 0 and (game / 'pattern-window071-ok.txt').is_file(), 'Native pattern-input QA failed: ' + str(game / 'launch-output.log'))
        result = base.read_json(game / 'pattern-window071-results.json')
        base.require(result['success'] is True and result['minecraft'] == target and result['productionVersion'] == RELEASE, 'Native target identity/result mismatch')
        base.require(result['inputs'] == inputs, 'Native recorded provenance mismatch')
        base.require(result['caseCount'] == 16 and [row['case'] for row in result['cases']] == CASES, 'Complete ordered sixteen-case native suite required')
        base.require(result['realButtonEntries'] == 19 and result['serverAcknowledgements'] >= 48, 'All actual Button entries and server acknowledgement coverage required')
        constants = result['nativeInputConstants']
        native = (41, 1, 3) if target == '26.3' else (256, 0, 1)
        base.require((constants['KEY_ESCAPE'], constants['MOUSE_BUTTON_LEFT'], constants['MOUSE_BUTTON_RIGHT']) == native, 'Correct target native input constants required')
        base.require(result['nativeDiskSamples'] == 16 and len(result['screenshots']) == 3, 'Sixteen native disk samples and three screenshots required')
        for row in result['cases']:
            base.require(row['passed'] is True and row['initialPattern'] == {'reference': 12, 'mask': (1 << 7) | (1 << 11), 'count': 2} and row['budget'] == 4, 'Every native case starts from explicit budget4/two-cell custom fixture')
            base.require(all(step['passed'] is True for step in row['steps']), 'Every native input must pass; observed defects cannot become success')
            closed = row['closedInventory']
            base.require(closed['enhancementMenu'] is False and closed['onePickOneNestedCoreNoDuplication'] is True and closed['nativeDiskRoundTripPassed'] is True and closed['nativeDiskCodecStructurallyEqual'] is True and closed['restoredCoreCount'] == 1, 'Actual closed inventory ownership and complete native disk codecs required')
            sample = game / closed['savedItemFile']
            base.require(sample.is_relative_to(game / 'pattern-window071-nbt') and sample.is_file() and sample.stat().st_size > 0, 'Real disposable saved native NBT sample required')
        for name in result['screenshots']: base.verify_png(game, name)
        for row in inputs['mods']: base.require(base.sha256(mods / row['file']) == row['sha256'], 'Staged JAR changed during runtime')
        base.require(base.sha256(main) == production_hash and base.sha256(helper) == helper_hash, 'Workspace source artifacts changed during QA')
        base.write_json_new(game / 'pattern-window071-runner-ok.json', {'success': True, 'minecraft': target, 'release': RELEASE, 'checks': result['checks'], 'cases': 16, 'realButtonEntries': 19, 'serverAcknowledgements': result['serverAcknowledgements'], 'productionSha256': production_hash, 'nativeDiskSamples': 16, 'screenshots': 3})
        print('PATTERN_WINDOW071_NATIVE_OK ' + str(game), flush=True)
    finally:
        after = snapshot(instance)
        changed = sorted(k for k in before.keys() | after.keys() if before.get(k) != after.get(k))
        base.write_json_new(game / 'normal-instance-after.json', {'unchanged': not changed, 'changedEntries': changed, 'protectedFiles': len(before), 'scope': 'Normal mods/launcher metadata SHA256; normal settings, accounts and saves never copied, opened or modified'})
        base.require(not changed, 'Normal instance changed during isolated QA')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', choices=('26.2', '26.3'), required=True)
    parser.add_argument('--instance', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--label', required=True)
    parser.add_argument('--without-sodium', action='store_true')
    parser.add_argument('--prepare-only', action='store_true')
    parser.add_argument('--timeout', type=int, default=600)
    args = parser.parse_args(); args.voxy = False
    run(args)
