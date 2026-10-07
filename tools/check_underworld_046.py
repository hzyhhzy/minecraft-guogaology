"""Validate the pure underworld terrain and its invariants against the saved 0.4.5 commit."""
from pathlib import Path
import argparse
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Android/Android Studio/jbr'))
    parser.add_argument('--baseline-ref', default='8612b56')
    parser.add_argument('--radius', type=int, default=10)
    args = parser.parse_args()
    output = ROOT / 'build/underworld-0406'
    current = output / 'classes'
    old_sources = output / 'baseline-src'
    old_classes = output / 'baseline-classes'
    for path in (current, old_sources, old_classes):
        path.mkdir(parents=True, exist_ok=True)
    javac, java = (str(args.java_home / 'bin' / (tool + '.exe')) for tool in ('javac', 'java'))
    log = []

    def run(command):
        result = subprocess.run(command, cwd=ROOT, text=True, encoding='utf8', capture_output=True)
        print(result.stdout, end='')
        if result.stderr:
            print(result.stderr, end='')
        log.append(result.stdout)
        result.check_returncode()
        return result.stdout

    tests = ROOT / 'tools/tests'
    run([javac, '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(ROOT/'src/main/java'),
         '-d', str(current), str(tests/'Underworld046Checks.java'), str(tests/'Underworld046Fingerprint.java')])
    for seed in (123456789, 20261007, -719311):
        run([java, '-Xmx768m', '-cp', str(current), 'Underworld046Checks', str(args.radius), str(seed)])
    before_files = 'WorldNoise WaterField UnderworldLakes TerrainField BiomeRegions CaveField TerrainSamples'.split()
    for name in before_files:
        relative = f'src/main/java/dev/guogaology/world/{name}.java'
        source = subprocess.run(['git', 'show', f'{args.baseline_ref}:{relative}'], cwd=ROOT, check=True, capture_output=True).stdout
        target = old_sources/'dev/guogaology/world'/f'{name}.java'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(source)
    run([javac, '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(old_sources), '-d', str(old_classes), str(tests/'Underworld046Fingerprint.java')])
    before = run([java, '-Xmx768m', '-cp', str(old_classes), 'Underworld046Fingerprint'])
    after = run([java, '-Xmx768m', '-cp', str(current), 'Underworld046Fingerprint'])
    assert before == after, 'Inner terrain or old natural-lake positions changed'
    (output/'run.log').write_text(''.join(log), 'utf8')
    (output/'audit.json').write_text(json.dumps({'status': 'PASS', 'baseline': args.baseline_ref,
                                                'inner_and_lakes_exact': True, 'log': log}, ensure_ascii=False, indent=2)+'\n', 'utf8')
    print('PASS: interpolated chambers/tunnels, player clearance, lake buffer, exact Inner and natural lake invariants')


if __name__ == '__main__':
    main()
