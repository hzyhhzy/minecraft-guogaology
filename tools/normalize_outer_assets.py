"""Drop excluded donor entrypoints; keep referenced donor art and shared aliases."""
from pathlib import Path
import json,re
from import_outer_content import ROOT,ALIASES,NS
A=ROOT/'src/main/resources/assets'/NS
def excluded(name):return name in ALIASES or any(t in name for t in ('hydra','ordinal_heart','raw_omega','portal_generator','guide_book')) or re.search(r'_(?:den|axe|shovel|hoe)$',name)
def run():
 for directory in ('blockstates','items','models/item'):
  for p in (A/directory).glob('*.json'):
   if excluded(p.stem):p.unlink()
 for p in (A/'lang').glob('*.json'):
  old=json.loads(p.read_text('utf8'));new={}
  for k,v in old.items():
   if not k.startswith(('block.','item.','biome.','entity.','tooltip.')):continue
   name=k.split('.',2)[-1]
   if excluded(name.split('.')[0]):continue
   new[k.replace('.googology.','.'+NS+'.')]=v
  p.write_text(json.dumps(new,ensure_ascii=False,indent=2)+'\n','utf8')
 # Translate the namespace in actual tooltip calls, not arbitrary identifiers.
 for folder in (ROOT/'ports/common/main/dev/googology/outer',ROOT/'ports/26.3/src/main/java/dev/googology/outer'):
  for p in folder.rglob('*.java'):
   s=p.read_text('utf8');p.write_text(s.replace('"tooltip.googology.','"tooltip.'+NS+'.'),'utf8')
if __name__=='__main__':run()
