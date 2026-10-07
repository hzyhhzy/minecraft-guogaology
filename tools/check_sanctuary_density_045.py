"""Compile/run pure production sanctuary sampling; never starts Gradle or Minecraft."""
from pathlib import Path
import argparse
import csv
import json
import hashlib
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--radius', type=int, default=20)
    parser.add_argument('--inner-scale', type=float)
    parser.add_argument('--rates', help='Eight explicit candidate probabilities, in SurvivalTheme order, for offline calibration only')
    parser.add_argument('--under-scale', type=float, default=.45)
    parser.add_argument('--holdout', action='store_true')
    parser.add_argument('--verify', action='store_true')
    parser.add_argument('--biomes', action='store_true')
    parser.add_argument('--output', type=Path, default=ROOT/'build/density-0405')
    args = parser.parse_args()
    suffix = '.exe' if os.name == 'nt' else ''
    candidates = [args.java_home, Path(os.environ.get('JAVA_HOME', 'missing')), Path('C:/Program Files/Android/Android Studio/jbr')]
    jdk = next((p for p in candidates if p and (p/'bin'/('javac'+suffix)).is_file()), None)
    javac = str(jdk/'bin'/('javac'+suffix)) if jdk else shutil.which('javac')
    java = str(jdk/'bin'/('java'+suffix)) if jdk else shutil.which('java')
    if not javac or not java:
        parser.error('A JDK 21 or newer is required; supply --java-home.')
    classes = ROOT/'build/density-0405-classes'; classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([javac, '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(ROOT/'src/main/java'),
                    '-d', str(classes), str(ROOT/'tools/tests/SanctuaryDensity045Checks.java')], check=True)
    command = [java, '-Xmx768m', '-cp', os.pathsep.join([str(classes), str(ROOT/'src/main/resources')]),
               'SanctuaryDensity045Checks', f'--radius={args.radius}', f'--output={args.output.resolve()}']
    for flag in ['holdout', 'verify', 'biomes']:
        if getattr(args, flag):
            command.append('--'+flag)
    if args.inner_scale is not None:
        command += [f'--inner-scale={args.inner_scale}', f'--under-scale={args.under_scale}']
    if args.rates:
        command += ['--rates='+args.rates]
    args.output.mkdir(parents=True, exist_ok=True)
    lines = []
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf8', cwd=ROOT) as process:
        for line in process.stdout:
            print(line, end='', flush=True); lines.append(line.rstrip())
        code = process.wait()
    tables = {}
    for name in ['dimensions', 'summary', 'themes']:
        path = args.output/f'{name}.csv'
        if path.is_file():
            with path.open(encoding='utf8', newline='') as stream:
                tables[name] = list(csv.DictReader(stream))
    report = {'status': 'PASS' if code == 0 else 'FAIL', 'method': 'Exact production Sampler, final terrain/lake/boundary/height/same-theme separation filters; no world chunks generated',
              'radius_cells': args.radius, 'holdout': args.holdout, 'verify': args.verify,
              'relative_probability_scales': None if args.inner_scale is None else {'inner': args.inner_scale, 'underworld': args.under_scale},
              'explicit_rates': args.rates,
              'production_sha256': {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
                    for path in [ROOT/'src/main/java/dev/googology/survival/SanctuaryPlacement.java',
                                 ROOT/'src/main/java/dev/googology/survival/SanctuaryLayout.java',
                                 ROOT/'src/main/java/dev/googology/world/TerrainField.java',
                                 ROOT/'src/main/java/dev/googology/world/UnderworldLakes.java',
                                 ROOT/'src/main/java/dev/googology/world/BiomeRegions.java']},
              'stdout': lines, 'tables': tables}
    (args.output/'audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', 'utf8')
    (args.output/'run.log').write_text('\n'.join(lines)+'\n', 'utf8')
    raise SystemExit(code)


if __name__ == '__main__':
    main()
