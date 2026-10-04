"""Audit model/texture reachability; never infer retirement from a numeric suffix.

--prune removes only graph-orphaned assets and the six unregistered legacy sad
lamp entrypoints. Their *models* remain if a live lantern state references them.
Standard-library only; run from a source checkout before compiling all targets.
"""
import argparse
import json
from pathlib import Path

LANTERNS = ('guogao_lantern', 'cyan_guogao_lantern', 'rose_guogao_lantern',
            'lime_guogao_lantern', 'violet_guogao_lantern', 'scarlet_guogao_lantern')
LEGACY_ENTRIES = {name + '_sad' for name in LANTERNS}
MINERALS = ('omega', 'epsilon', 'gamma', 'psi', 'strata', 'proof')


def audit(root, prune=False):
    root = root.resolve()
    assets = root / 'src/main/resources/assets/googology'
    models = assets / 'models'
    retained = set()
    pending = []
    missing = set()

    def add(path):
        if not path.is_file():
            missing.add(path.relative_to(root).as_posix())
        elif path not in retained:
            retained.add(path)
            if path.suffix == '.json':
                pending.append(path)

    def texture(value):
        if isinstance(value, str) and value.startswith('googology:'):
            path = assets / ('textures/' + value.split(':', 1)[1] + '.png')
            add(path)
            if path.with_suffix('.png.mcmeta').exists():
                add(path.with_suffix('.png.mcmeta'))

    def walk(value):
        if isinstance(value, list):
            for child in value:
                walk(child)
        elif isinstance(value, dict):
            for key, child in value.items():
                if key in ('model', 'parent') and isinstance(child, str) and child.startswith('googology:'):
                    add(models / (child.split(':', 1)[1] + '.json'))
                elif key == 'textures' and isinstance(child, dict):
                    for entry in child.values():
                        texture(entry)
                else:
                    walk(child)

    for folder in ('blockstates', 'models/item', 'core_meshes'):
        for path in (assets / folder).rglob('*.json'):
            if path.stem not in LEGACY_ENTRIES:
                add(path)
    # These are addressed by Java / vanilla equipment rendering, not block JSON.
    add(assets / 'textures/gui/enhancement.png')
    for mineral in MINERALS:
        for layer in (1, 2):
            add(assets / f'textures/models/armor/{mineral}_layer_{layer}.png')
        for layer in ('humanoid', 'humanoid_leggings'):
            add(assets / f'textures/entity/equipment/{layer}/{mineral}.png')
        add(assets / f'equipment/{mineral}.json')
    while pending:
        walk(json.loads(pending.pop().read_text(encoding='utf8')))
    candidates = {p for directory in ('blockstates', 'models', 'textures', 'equipment', 'core_meshes')
                  for p in (assets / directory).rglob('*') if p.is_file()}
    unused = sorted(candidates - retained)
    result = {
        'retained_assets': len(retained),
        'unused_assets': len(unused),
        'unused_bytes': sum(p.stat().st_size for p in unused),
        'missing_references': sorted(missing),
        'unused_paths': [p.relative_to(root).as_posix() for p in unused],
        'notes': ['Block/item entrypoints retained except six unregistered *_sad aliases.',
                  'All live blockstate variants, item overrides and 28 core meshes traversed.',
                  'Both legacy and modern armor textures retained; game IDs are not renamed.'],
    }
    if missing:
        raise ValueError('Missing referenced resources: ' + ', '.join(sorted(missing)))
    if prune:
        # Individual audited files only: no recursive removal or external target.
        for path in unused:
            if not path.resolve().is_relative_to(assets.resolve()):
                raise ValueError('Asset escapes resource directory: ' + str(path))
            path.unlink()
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--report', type=Path)
    parser.add_argument('--prune', action='store_true')
    parser.add_argument('--check', action='store_true', help='Fail if orphan assets remain')
    args = parser.parse_args()
    result = audit(args.root, args.prune)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print(json.dumps({k: v for k, v in result.items() if k != 'unused_paths'}, ensure_ascii=False, indent=2))
    if args.check and result['unused_assets']:
        raise SystemExit('Unreachable assets remain; review the report before pruning.')
