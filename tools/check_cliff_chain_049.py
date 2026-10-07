"""Audit real large-only cliff chains and optionally profile terrain, without Minecraft."""
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
    parser.add_argument('--terrain-profile', action='store_true')
    parser.add_argument('--radius', type=int, default=22, help='Owner-cell radius of each of three complete density samples; CELL48 gives 4.46 km2 per seed')
    parser.add_argument('--width', type=int, default=4096)
    parser.add_argument('--step', type=int, default=16)
    args = parser.parse_args()
    if args.width <= 0 or args.width % 8 or args.step <= 0 or args.step % 4 or args.width % args.step:
        parser.error('Width must be positive and divisible by 8 and step; step must be a positive multiple of 4.')
    suffix = '.exe' if os.name == 'nt' else ''
    choices = [args.java_home, Path(os.environ.get('JAVA_HOME', 'missing')), Path('C:/Program Files/Android/Android Studio/jbr')]
    jdk = next((p for p in choices if p and (p/'bin'/('javac'+suffix)).is_file()), None)
    javac = str(jdk/'bin'/('javac'+suffix)) if jdk else shutil.which('javac')
    java = str(jdk/'bin'/('java'+suffix)) if jdk else shutil.which('java')
    if not javac or not java:
        parser.error('A JDK 21 or newer is required; supply --java-home.')
    output = ROOT/'build/cliff-chain-0409'
    tag = 'terrain-profile-current' if args.terrain_profile else 'audit'
    classes = output/'classes'
    classes.mkdir(parents=True, exist_ok=True)
    files = ['UnderworldLandforms', 'UnderworldRegions', 'TerrainField', 'CliffDescentChains']
    hashes = {name: hashlib.sha256((ROOT/f'src/main/java/dev/googology/world/{name}.java').read_bytes()).hexdigest() for name in files}
    log = []

    def save(report):
        (output/f'{tag}.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')

    def run(command):
        captured = []
        with subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding='utf8') as process:
            for line in process.stdout:
                captured.append(line)
                print(line, end='', flush=True)
            code = process.wait()
        result = ''.join(captured)
        log.append(result)
        (output/f'{tag}.log').write_text(''.join(log), encoding='utf8')
        if code:
            save({'status': 'FAIL', 'sourceHashes': hashes, 'command': command, 'error': result[-10000:]})
            raise subprocess.CalledProcessError(code, command)
        return result.splitlines()

    save({'status': 'RUNNING', 'sourceHashes': hashes})
    run([javac, '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(ROOT/'src/main/java'),
         '-d', str(classes), str(ROOT/'tools/tests/CliffChain049Checks.java')])
    command = [java, '-Xmx1600m', '-cp', str(classes), 'CliffChain049Checks']
    if args.terrain_profile:
        lines = run(command+['terrain', str(args.width), str(args.step)])
        rows = [json.loads(line.removeprefix('TERRAIN049 ')) for line in lines if line.startswith('TERRAIN049 ')]
        save({'scope': 'Independent high-cliff availability; coarse counts are screening candidates, not accepted production chains',
              'sourceHashes': hashes, 'samples': rows})
        return
    lines = run(command+[str(args.radius)])
    samples = [json.loads(line.removeprefix('CLIFF049_SAMPLE ')) for line in lines if line.startswith('CLIFF049_SAMPLE ')]
    density = [json.loads(line.removeprefix('CLIFF049_DENSITY ')) for line in lines if line.startswith('CLIFF049_DENSITY ')]
    summary = next(json.loads(line.removeprefix('CLIFF049_SUMMARY ')) for line in lines if line.startswith('CLIFF049_SUMMARY '))
    expected = [line for line in lines if line.startswith('CLIFF049_PLAN ')]
    replay = run(command+['replay']+[f"{s['seed']},{s['cell'][0]},{s['cell'][1]}" for s in samples])
    if expected != [line for line in replay if line.startswith('CLIFF049_PLAN ')]:
        save({'status': 'FAIL', 'sourceHashes': hashes, 'error': 'Fresh-JVM plan replay differs'})
        raise AssertionError('Fresh-JVM plan replay differs')
    save({**summary, 'sourceHashes': hashes, 'coldCacheReplayExact': True, 'densitySamples': density, 'geometrySamples': samples,
          'scope': 'All accepted owner plans: real high anchors, low supported feet, exterior physical geometry and near-pair collisions; representative hollow rings and shuffled chunk replay'})
    print(f"CLIFF_CHAIN_0409_OK: {summary['plans']} full physical plans, {summary['densityPerKm2']:.2f}/km2, cold/chunk replay exact")


if __name__ == '__main__':
    main()
