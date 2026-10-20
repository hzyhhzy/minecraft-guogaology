"""Guard the real native damage-cooldown boundary, not Entity invulnerability."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_port_sources import adapt

ROOT = Path(__file__).resolve().parents[2]


class ExtraHitPortTests(unittest.TestCase):
    def test_native_263_field_and_restoration_in_both_real_call_sites(self):
        for name in ('BowEffects.java', 'MiningEffects.java'):
            with self.subTest(name=name):
                source = (ROOT / 'ports/common/main/dev/guogaology/mining' / name).read_text(encoding='utf-8')
                result = adapt(source, '26.3', name)
                self.assertIn('int timer=other.damageCooldownTime;', result)
                self.assertIn('other.damageCooldownTime=0;', result)
                self.assertIn('finally{other.damageCooldownTime=timer;', result)
                self.assertIn('guogaology$lastDamage(previous)', result)
                self.assertNotIn('other.invulnerableTime', result)
                self.assertNotIn('other.setInvulnerableTime', result)

    def test_other_native_targets_keep_their_actual_hurt_timer(self):
        for target in ('1.21.11', '26.2'):
            for name in ('BowEffects.java', 'MiningEffects.java'):
                with self.subTest(target=target, name=name):
                    source = (ROOT / 'ports/common/main/dev/guogaology/mining' / name).read_text(encoding='utf-8')
                    result = adapt(source, target, name)
                    self.assertIn('int timer=other.invulnerableTime;', result)
                    self.assertNotIn('damageCooldownTime', result)


if __name__ == '__main__':
    unittest.main()
