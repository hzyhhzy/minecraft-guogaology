"""Observe actual 26.3 input on the unchanged 0.7.0 production JAR.

Only the disposable QA helper and its entrypoint are changed. Normal instances,
settings and saves are never modified or copied. A completed audit may find bugs.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse, json, os, re, shutil, subprocess
import run_release_263_qa as base
from run_tooltip_0615_qa import snapshot


def run(a):
    instance = a.instance.resolve()
    base.require(instance.name == 'fo263_2' and instance.parent.name == 'versions', 'Exact read-only 26.3 installation required')
    base.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*', a.label) and not a.label.endswith(('.', ' ')), 'Fresh safe directory label required')
    base.require(a.label.upper().split('.')[0] not in {'CON', 'PRN', 'AUX', 'NUL', *(f'COM{i}' for i in range(1, 10)), *(f'LPT{i}' for i in range(1, 10))}, 'Invalid Windows directory label')
    root = base.ROOT
    main = root / 'ports/26.3/build/libs/guogaology-dimension-26.3-0.7.0.jar'
    helper = root / 'ports/26.3/build/qa/guogaology-release-263-qa-0.7.0.jar'
    entry = 'dev.guogaology.releaseqa263.PatternWindow070Audit'
    metadata = base.read_json(instance / 'fo263_2.json')
    client = instance / 'fo263_2.jar'
    minecraft = instance.parent.parent
    with ZipFile(client) as z:
        base.require(json.loads(z.read('version.json'))['id'] == '26.3', 'Native 26.3 client required')
    info = base.mod_metadata(main)
    base.require(info['id'] == 'guogaology' and info['version'] == '0.7.0' and info['depends']['minecraft'] == '26.3', 'Exact production identity required')
    expected = '17bd1e1fbc631ebcbab29711d842d9083952d3e01be1baacf36cccab734248bf'
    base.require(base.sha256(main) == expected, 'Unchanged published 0.7.0 production JAR required')
    runtime = (root / 'build/runtime-qa263').resolve()
    game = (runtime / a.label).resolve()
    base.require(game.parent == runtime and not game.exists(), 'Fresh direct-child disposable directory required')
    before = snapshot(instance)
    game.mkdir(parents=True)
    mods = game / 'mods'; mods.mkdir()
    temp = game / 'temp'; temp.mkdir()
    (game / 'config').mkdir()
    inputs = {'minecraft': '26.3', 'productionVersion': '0.7.0', 'productionSha256': expected, 'normalReadOnlyInstance': str(instance), 'mods': []}
    try:
        for source, mod in [(main, info), (helper, base.mod_metadata(helper)), *base.selected_mods(instance, a)]:
            dest = mods / source.name
            base.require(not dest.exists(), 'Unique staged artifacts required')
            if source == helper:
                mod = json.loads(json.dumps(mod)); mod['entrypoints'] = {'client': [entry]}
                with ZipFile(source) as src, ZipFile(dest, 'x') as dst:
                    base.require(entry.replace('.', '/') + '.class' in src.namelist(), 'Compile focused audit helper first')
                    for item in src.infolist():
                        dst.writestr(item, json.dumps(mod, ensure_ascii=False, indent=2).encode() if item.filename == 'fabric.mod.json' else src.read(item.filename))
            else:
                shutil.copy2(source, dest)
                base.require(base.sha256(source) == base.sha256(dest), 'Exact production/runtime artifact copy')
            inputs['mods'].append({'id': mod['id'], 'file': dest.name, 'sha256': base.sha256(dest), 'sourceSha256': base.sha256(source)})
        base.write_json_new(game / 'pattern-window070-inputs.json', inputs)
        base.write_json_new(game / 'qa263-isolated.json', {'mode': 'pattern-window070-audit', 'minecraft': '26.3'})
        (game / 'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:3\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:zh_cn\nguiScale:3\ntextureFiltering:1\nmaxAnisotropyBit:1\n', encoding='utf-8')
        java = base.java_executable(a.java_home)
        command = [str(java), '-Xmx4G', '-XX:StackShadowPages=32', '--enable-native-access=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.misc=ALL-UNNAMED', '-Dfabric.development=false', '-Dfile.encoding=UTF-8', '-Dguogaology.patternWindow070=true', f'-Djava.io.tmpdir={temp}', f'-Djna.tmpdir={temp}', f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}', '-cp', ';'.join(map(str, base.library_classpath(metadata, minecraft, client))), metadata['mainClass'], '--username', 'GuogaologyQA', '--version', '26.3', '--gameDir', str(game), '--assetsDir', str(minecraft / 'assets'), '--assetIndex', metadata['assetIndex']['id'], '--uuid', '00000000000000000000000000000001', '--accessToken', '0', '--userType', 'legacy', '--versionType', 'release', '--width', '1600', '--height', '1000']
        base.write_json_new(game / 'staging.json', {'inputs': inputs, 'command': command})
        env = os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
            env.pop(key, None)
        env.update(TEMP=str(temp), TMP=str(temp))
        with (game / 'launch-output.log').open('x', encoding='utf-8') as output:
            proc = subprocess.Popen(command, cwd=game, env=env, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            base.write_json_new(game / 'qa-process.json', {'pid': proc.pid, 'gameDirectory': str(game), 'minecraft': '26.3', 'mode': 'pattern-window070-audit'})
            try:
                code = proc.wait(timeout=a.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try: proc.wait(timeout=15)
                except subprocess.TimeoutExpired: proc.kill(); proc.wait(timeout=15)
                raise RuntimeError('Own isolated native pattern-input JVM timed out')
        base.require(code == 0 and (game / 'pattern-window070-ok.txt').is_file(), 'Native audit incomplete: ' + str(game / 'launch-output.log'))
        result = base.read_json(game / 'pattern-window070-results.json')
        base.require(result['minecraft'] == '26.3' and result['productionVersion'] == '0.7.0', 'Native target identity mismatch')
        for name in result.get('screenshots', []): base.verify_png(game, name)
        for row in inputs['mods']: base.require(base.sha256(mods / row['file']) == row['sha256'], 'Staged JAR changed')
        base.require(base.sha256(main) == expected, 'Production JAR changed')
        print('PATTERN_WINDOW070_AUDIT_COMPLETE ' + str(game), flush=True)
        print(json.dumps(result, ensure_ascii=False), flush=True)
    finally:
        after = snapshot(instance)
        changed = sorted(k for k in before.keys() | after.keys() if before.get(k) != after.get(k))
        base.write_json_new(game / 'normal-instance-after.json', {'unchanged': not changed, 'changedEntries': changed, 'protectedFiles': len(before), 'scope': 'Normal mods/launcher metadata SHA256; normal settings, accounts and saves never copied or modified'})
        base.require(not changed, 'Normal instance changed during isolated audit')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--instance', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--label', required=True)
    parser.add_argument('--without-sodium', action='store_true')
    parser.add_argument('--timeout', type=int, default=600)
    args = parser.parse_args(); args.voxy = False
    run(args)
