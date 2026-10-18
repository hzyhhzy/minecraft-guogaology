"""Run narrowly scoped giant/city production geometry checks without Minecraft."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main():
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(errors='backslashreplace')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, default=Path(os.environ.get('JAVA_HOME', 'C:/Users/sigmoid/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon')))
    args = parser.parse_args()
    suffix = '.exe' if os.name == 'nt' else ''
    executables = []
    for name in ('javac', 'java'):
        candidate = args.java_home / 'bin' / (name + suffix)
        resolved = str(candidate) if candidate.is_file() else shutil.which(name)
        if not resolved:
            parser.error('A JDK 21 or newer is required; pass --java-home.')
        executables.append(resolved)
    output = ROOT / 'build/astra-giant-063'
    output.mkdir(parents=True, exist_ok=True)
    commands = [
        [executables[0], '-J-Duser.language=en', '-J-Duser.country=US', '--release', '21', '-encoding', 'UTF-8', '-sourcepath', str(ROOT / 'src/main/java'), '-d', str(output), str(ROOT / 'tools/tests/AstraGiant063Checks.java')],
        [executables[1], '-Xmx1G', '-cp', os.pathsep.join([str(output), str(ROOT / 'src/main/resources')]), 'AstraGiant063Checks'],
    ]
    with (output / 'checks.log').open('w', encoding='utf-8') as log:
        for command in commands:
            result = subprocess.run(command, cwd=ROOT, text=True, encoding='utf-8', errors='replace', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            print(result.stdout, end='')
            log.write(result.stdout)
            if result.returncode:
                raise SystemExit(result.returncode)


if __name__ == '__main__':
    main()
