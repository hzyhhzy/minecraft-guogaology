"""Restore legacy custom-solid classification in 26.3's native motion tag tree."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'tools/reference/block-motion263.json'


def classification():
    data = json.loads(CATALOG.read_text(encoding='utf-8'))
    groups = [data[key] for key in ('motion_no_leaves', 'leaf_blocks', 'legacy_non_motion')]
    members = [name for group in groups for name in group]
    assert len(members) == len(set(members)), 'Duplicate motion classification'
    assets = {path.stem for path in (ROOT / 'src/main/resources/assets/guogaology/blockstates').glob('*.json')}
    assert set(members) == assets, 'Classify every new/removed block before building26.3 motion tags'
    return data


def prepare(out):
    data = classification()
    path = Path(out) / 'data/minecraft/tags/block/blocks_motion_no_leaves.json'
    existing = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace': False, 'values': []}
    assert not existing.get('replace', False), 'Never replace the vanilla motion tag'
    excluded = {'guogaology:' + name for key in ('leaf_blocks', 'legacy_non_motion') for name in data[key]}
    assert not (set(existing['values']) & excluded), 'Imported motion tag contains an excluded custom block'
    existing['values'] = list(dict.fromkeys(existing['values'] + ['guogaology:' + name for name in data['motion_no_leaves']]))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(existing, indent=2) + '\n', encoding='utf-8')
    return path
