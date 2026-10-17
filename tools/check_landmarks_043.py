"""Check 0.5.5 acquisition exhibits, retained top arena and downgraded chest loot offline."""
from pathlib import Path
import argparse
import json
import os
import re
import shutil
import subprocess

from generate_sanctuary_loot import audit

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--baseline', type=Path, help='Optional pre-change SanctuaryCheck .vox directory')
    parser.add_argument('--baseline-jar', type=Path, help='Optional pre-reduction main Mod JAR; load only pure blueprint classes to snapshot it')
    args = parser.parse_args()
    if args.baseline and args.baseline_jar:
        parser.error('Use --baseline or --baseline-jar, not both.')
    candidates = [args.java_home, Path(os.environ.get('JAVA_HOME', 'missing')),
                  Path('C:/Program Files/Android/Android Studio/jbr')]
    suffix = '.exe' if os.name == 'nt' else ''
    jdk = next((p for p in candidates if p and (p/'bin'/('javac'+suffix)).is_file()), None)
    javac = str(jdk/'bin'/('javac'+suffix)) if jdk else shutil.which('javac')
    java = str(jdk/'bin'/('java'+suffix)) if jdk else shutil.which('java')
    if not javac or not java:
        parser.error('A JDK 21 or newer is required; supply --java-home.')
    output = ROOT/'build/landmark-0403-classes'
    output.mkdir(parents=True, exist_ok=True)
    world = ['WorldNoise', 'OrdinalLightBand']
    survival = ['SanctuaryLayout', 'SanctuaryMotifs', 'SanctuaryOrnaments', 'SanctuaryInteriors',
                'SanctuaryResources', 'CourtLaverPatterns', 'SanctuaryArenas', 'SurvivalTheme',
                'MosaicMotifs', 'MosaicTreeLights', 'AstraCrown', 'EmojiRelief', 'FrontierSanctuary', 'PowerPagoda']
    sources = [ROOT/f'src/main/java/dev/guogaology/world/{name}.java' for name in world]
    sources += [ROOT/f'src/main/java/dev/guogaology/survival/{name}.java' for name in survival]
    sources += [ROOT/'tools/tests/Landmark043Checks.java', ROOT/'tools/tests/LandmarkChest043Checks.java']
    subprocess.run([javac, '--release', '21', '-encoding', 'UTF-8', '-d', str(output), *map(str, sources)], check=True)
    if args.baseline_jar:
        args.baseline = ROOT/'build/landmark-restoration-baseline'
        subprocess.run([java, '-Xmx768m', '-cp', os.pathsep.join([str(args.baseline_jar.resolve()), str(output)]),
                        'dev.guogaology.survival.Landmark043Checks', '--write-baseline', str(args.baseline)], check=True)
    command = [java, '-Xmx768m', '-cp', os.pathsep.join([str(output), str(ROOT/'src/main/resources')]),
               'dev.guogaology.survival.Landmark043Checks']
    if args.baseline:
        command += [str(args.baseline.resolve())]
    result = subprocess.run(command, text=True, encoding='utf8', capture_output=True)
    print(result.stdout, end='')
    if result.returncode:
        print(result.stderr, end='')
        raise SystemExit(result.returncode)
    chest_result = subprocess.run([java, '-Xmx768m', '-cp', os.pathsep.join([str(output), str(ROOT/'src/main/resources')]),
                                   'dev.guogaology.survival.LandmarkChest043Checks'], text=True, encoding='utf8', capture_output=True)
    print(chest_result.stdout, end='')
    if chest_result.returncode:
        print(chest_result.stderr, end='')
        raise SystemExit(chest_result.returncode)
    # Every material alias must also resolve to ordinary decorative lighting.
    for folder in ('src/main/java', 'ports/common/main'):
        source = (ROOT/f'{folder}/dev/guogaology/survival/SurvivalStructures.java').read_text('utf8')
        light = next(line for line in source.splitlines() if 'p[SanctuaryLayout.LIGHT]=' in line)
        assert 'ORDINAL_CRYSTAL' not in light and 'ASTRA_LIGHT' in light, folder
        assert 'CoreGrades.levels(finaleCore).get(0)' in source, 'Final eight cores must be displayed Lv1'
    loot = audit()
    report = {'status': 'PASS', 'scope': '0.5.5 material exhibits, eight cores per landmark, retained top arena and downgraded loot', 'blueprint_output': result.stdout.splitlines(),
              'chest_geometry_output': chest_result.stdout.splitlines(),
              'baseline_compared': str(args.baseline) if args.baseline else None,
              'loot_tables': loot['tables'], 'sampled_chests': loot['sampled_chests'],
              'protected_underworld': {'number_lamps': 87, 'crystals_per_number_lamp': 0,
                                      'guogao_drop_deposits': 24, 'finale_lv1_cores': 8,
                                      'finale_crystals': 0, 'finale_z': 1}}
    (ROOT/'build/landmark-0403.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', 'utf8')
    print(f"LANDMARK_RESOURCES_055_OK: ordinary renderer palettes, {loot['tables']} current loot tables, {loot['sampled_chests']} sampled chests")


if __name__ == '__main__':
    main()
