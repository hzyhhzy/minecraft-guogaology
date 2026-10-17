"""Run pure 0.5.15 generation checks and 2.0184 km² LHO proposal comparisons; no Gradle or Minecraft."""
from pathlib import Path
import argparse
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Android/Android Studio/jbr'))
    parser.add_argument('--astra516-only', action='store_true', help='Only fixed-axis Astra and exact Underworld-axis regression; no LHO census')
    args = parser.parse_args()
    output = ROOT / ('build/astra-0516' if args.astra516_only else 'build/material-0515')
    classes = output / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    commands = [
        [str(args.java_home / 'bin/javac.exe'), '--release', '21', '-encoding', 'UTF-8',
         '-sourcepath', str(ROOT / 'src/main/java'), '-d', str(classes), str(ROOT / 'tools/tests/Material515GenerationChecks.java')],
        [str(args.java_home / 'bin/java.exe'), '-Xmx768m', '-cp', str(classes), 'Material515GenerationChecks'] + (['--astra516-only'] if args.astra516_only else []),
    ]
    logs = []
    for command in commands:
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding='utf8')
        print(result.stdout, end='', flush=True)
        if result.stderr:
            print(result.stderr, end='', flush=True)
        logs.append(result.stdout + result.stderr)
        if result.returncode:
            (output / 'run.log').write_text('\n'.join(logs), 'utf8')
            raise SystemExit(result.returncode)
    (output / 'run.log').write_text('\n'.join(logs), 'utf8')
    evidence = {
        'status': 'PASS', 'area_km2_per_seed': 2.0184, 'seeds': [515515, 20261017],
        'method': 'Same seed and 2.0184 km² windows: previous 58-block/one-third candidates versus current 48-block/full candidates, identical terrain and 49-block same-tree arbitration. Fragment painters use identical proposal geometry/base and unchanged L/H/O. Final landmark/table/3D placement filters remain in production and are excluded from this proposal census.',
        'stdout': logs,
    }
    if args.astra516_only:
        evidence = {'status': 'PASS', 'method': 'Pure fixed-axis Astra checks and620544 Underworld axis coordinates compared by SHA256 of exact double bits against pre-change0.5.15 production methods; original height selectors and straight city reach checked. No Minecraft bootstrap or LHO census.', 'stdout': logs}
    (output / 'audit.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', 'utf8')


if __name__ == '__main__':
    main()
