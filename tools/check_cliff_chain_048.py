"""Audit real cliff descent chains with only a JDK; never starts Gradle or Minecraft."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    suffix = '.exe' if os.name == 'nt' else ''
    candidates = [args.java_home, Path(os.environ.get('JAVA_HOME', 'missing')),
                  Path('C:/Program Files/Android/Android Studio/jbr')]
    jdk = next((p for p in candidates if p and (p/'bin'/('javac'+suffix)).is_file()), None)
    javac = str(jdk/'bin'/('javac'+suffix)) if jdk else shutil.which('javac')
    java = str(jdk/'bin'/('java'+suffix)) if jdk else shutil.which('java')
    if not javac or not java:
        parser.error('A JDK 21 or newer is required; supply --java-home.')
    output = ROOT/'build/cliff-chain-0408'
    classes = output/'classes'
    classes.mkdir(parents=True, exist_ok=True)
    log = []
    inputs = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in (
        ROOT/'src/main/java/dev/guogaology/world/CliffDescentChains.java',
        ROOT/'tools/tests/CliffChain048Checks.java')}
    (output/'audit.json').write_text(json.dumps({'status': 'RUNNING', 'inputs': inputs}, indent=2)+'\n', encoding='utf8')

    def run(command):
        result = subprocess.run(command, cwd=ROOT, text=True, encoding='utf8', capture_output=True)
        log.append(result.stdout + result.stderr)
        print(result.stdout, end='')
        if result.stderr:
            print(result.stderr, end='')
        (output/'run.log').write_text(''.join(log), encoding='utf8')
        if result.returncode:
            (output/'audit.json').write_text(json.dumps({'status': 'FAIL', 'inputs': inputs,
                'command': command, 'exitCode': result.returncode, 'error': result.stderr}, indent=2)+'\n', encoding='utf8')
        result.check_returncode()
        return result.stdout.splitlines()

    run([javac, '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(ROOT/'src/main/java'),
         '-d', str(classes), str(ROOT/'tools/tests/CliffChain048Checks.java')])
    command = [java, '-Xmx1200m', '-cp', str(classes), 'CliffChain048Checks']
    lines = run(command)
    samples = [json.loads(line.removeprefix('CLIFF_SAMPLE ')) for line in lines if line.startswith('CLIFF_SAMPLE ')]
    summary = next(json.loads(line.removeprefix('CLIFF_SUMMARY ')) for line in lines if line.startswith('CLIFF_SUMMARY '))
    expected = [line for line in lines if line.startswith('CLIFF_PLAN ')]
    # A second JVM starts with every production cache empty, independently of
    # the first JVM's natural-site discovery and deliberately shuffled queries.
    replay = run(command + ['replay'] + [f"{s['seed']},{s['cell'][0]},{s['cell'][1]}" for s in samples])
    assert expected == [line for line in replay if line.startswith('CLIFF_PLAN ')], 'Cold-cache plan replay differs'
    assert len(samples) >= 10 and len({s['seed'] for s in samples}) >= 3
    assert {s['kind'] for s in samples} == {'valleyFoot', 'bridge'}, 'Both authorized chain designs must be exercised'
    assert any(s['kind'] == 'bridge' and s['footAboveValley'] > 8 for s in samples), 'Include a real bridge ending above the valley floor'
    regressions = [json.loads(line.removeprefix('CLIFF_FOOT_REGRESSION ')) for line in lines if line.startswith('CLIFF_FOOT_REGRESSION ')]
    report = {**summary, 'coldCacheReplayExact': True, 'inputs': inputs,
              'scope': 'Production cliff-foot and valley-bridge terrain, voxel hollow loops, actual protection footprint, shuffled chunk discovery and rendering',
              'footRegressions': regressions,
              'samples': samples}
    (output/'audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print(f"CLIFF_CHAIN_0408_OK: {len(samples)} natural plans; full/chunk geometry and cold-cache replay exact")


if __name__ == '__main__':
    main()
