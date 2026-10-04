"""Current player-facing wording, applied after historical resource generators."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((Path(__file__).with_suffix('.json')).read_text(encoding='utf8'))

def normalize(path, data):
    path = Path(path)
    if path.parent.name != 'lang' or path.parent.parent.name != 'googology' or path.stem not in CATALOG['translations']:
        return data
    result = dict(data)
    for key in CATALOG['removed']:
        result.pop(key, None)
    result.update(CATALOG['translations'][path.stem])
    return result

def apply():
    for locale in CATALOG['translations']:
        path = ROOT / f'src/main/resources/assets/googology/lang/{locale}.json'
        data = normalize(path, json.loads(path.read_text(encoding='utf8')))
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf8')

if __name__ == '__main__':
    apply()
