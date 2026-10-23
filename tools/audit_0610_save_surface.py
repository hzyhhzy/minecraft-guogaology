"""Read-only archive comparison; native disk loading is a separate required check."""
from hashlib import sha256
import json
from pathlib import Path
from audit_069_save_compat import archive, states, identity_surfaces, string_constants
from audit_jei_patch_delta import TARGETS

ROOT = Path(__file__).resolve().parents[1]
NEW_BLOCKS = {f'{wood}_{form}_sign' for wood in ('laver', 'dread')
              for form in ('hanging', 'wall_hanging')}


def main():
    rows = []
    for target in TARGETS:
        old = ROOT / f'build/releases/0.6.9/guogaology-dimension-{target}-0.6.9.jar'
        new = ROOT / f'build/releases/0.6.10/guogaology-dimension-{target}-0.6.10.jar'
        a, b = archive(old, '0.6.9'), archive(new, '0.6.10')
        sa, sb = states(a), states(b)
        assert len(sa) == 226 and len(sb) == 230
        assert sb.keys() - sa.keys() == NEW_BLOCKS and not sa.keys() - sb.keys()
        assert all(sa[name] == sb[name] for name in sa), 'Old blockstate resources changed'
        surfaces_a, surfaces_b = identity_surfaces(a), identity_surfaces(b)
        for prefix, names in surfaces_a.items():
            assert set(names) <= set(surfaces_b[prefix]), f'Removed old identity: {target}/{prefix}'
        protected = [name for name in a if name.startswith((
            'data/guogaology/dimension/', 'data/guogaology/dimension_type/',
            'data/guogaology/worldgen/', 'assets/guogaology/items/'))]
        # Preserve component/portal/arrow bookkeeping and the LHO stability logic.
        stems = ('/MiningContent', '/PortalState', '/BowVolleyLedger',
                 '/Lho', '/LHO', '/Absence', '/VoidFragment', '/Stability', '/JeiTagCompat')
        protected += [name for name in a if name.endswith('.class') and any(stem in name for stem in stems)]
        formatting_only = []
        for name in protected:
            if a[name] == b[name]:
                continue
            assert name.endswith('.json') and json.loads(a[name]) == json.loads(b[name]), f'Save/stability behavior drift: {target}/{name}'
            formatting_only.append(name)
        registry_classes = [name for name in a if name.endswith((
            '/GuogaologyBlocks.class', '/outer/registry/ModBlocks.class',
            '/outer/registry/ModWoodDecorBlocks.class', '/outer/registry/ModBoats.class'))]
        for name in registry_classes:
            assert not set(string_constants(a[name])) - set(string_constants(b[name])), f'Old registry literal removed: {name}'
        rows.append({'minecraft': target, 'oldSha256': sha256(old.read_bytes()).hexdigest(),
                     'sha256': sha256(new.read_bytes()).hexdigest(), 'oldBlockIdsRetained': len(sa),
                     'oldBlockstateResourcesIdentical': True, 'addedBlocks': sorted(NEW_BLOCKS),
                     'unchangedProtectedEntries': protected, 'jsonFormattingOnly': formatting_only,
                     'registryLiteralsRetained': registry_classes})
    result = {'passed': True, 'baseline': '0.6.9', 'candidate': '0.6.10', 'targets': rows,
              'scope': 'Static identity, old blockstates, codec, dimensions and LHO/JEI guard retention; not native save certification.'}
    out = ROOT / 'build/repair-0610/save-surface.json'
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('SAVE_SURFACE_0610_OK', len(rows))


if __name__ == '__main__':
    main()
