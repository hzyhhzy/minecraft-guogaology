"""Regression coverage for native entity names, including finite boat IDs."""
import json
from pathlib import Path
import tempfile
import unittest

from audit_localization import audit, registered_entity_names

ROOT = Path(__file__).resolve().parents[2]


class EntityLocalizationTests(unittest.TestCase):
    def fixture(self, woods=('laver', 'dread'), missing=()):
        build = (ROOT / 'build').resolve()
        build.mkdir(exist_ok=True)
        temporary = tempfile.TemporaryDirectory(prefix='entity-localization-', dir=build)
        root = Path(temporary.name)
        assert root.resolve().is_relative_to(build)
        self.addCleanup(temporary.cleanup)
        registry = root / 'src/main/java/example'
        registry.mkdir(parents=True)
        (registry / 'ModBlocks.java').write_text(
            'class ModBlocks { static final List<String> WOOD_IDS = List.of('
            + ','.join(json.dumps(wood) for wood in woods) + '); }', encoding='utf-8')
        (registry / 'ModEntities.java').write_text('''class ModEntities {
            // EntityType<Fake> ignored = register("not_live", builder);
            static final EntityType<Seat> SEAT = register("seat", builder);
            private static EntityType<?> register(String id, Object builder) {
                return Registry.register(BuiltInRegistries.ENTITY_TYPE, GuogaologyMod.id(id), builder);
            }
        }''', encoding='utf-8')
        (registry / 'ModBoats.java').write_text('''class ModBoats {
            void initialize() {
                for (String family : ModBlocks.WOOD_IDS) {
                    var id = GuogaologyMod.id(family + "_boat");
                    Registry.register(BuiltInRegistries.ENTITY_TYPE, id, hull);
                }
            }
        }''', encoding='utf-8')
        keys = ['item.guogaology.laver_boat', 'entity.guogaology.seat',
                'entity.guogaology.laver_boat', 'entity.guogaology.dread_boat',
                *['modmenu.' + kind + 'Translation.guogaology'
                  for kind in ('name', 'summary', 'description')]]
        translations = {locale: {key: 'Test' for key in keys if key not in missing}
                        for locale in ('en_us', 'zh_cn')}
        assets = root / 'src/main/resources/assets/guogaology'
        (assets / 'lang').mkdir(parents=True)
        (assets / 'models/item').mkdir(parents=True)
        (assets / 'models/item/laver_boat.json').write_text('{}', encoding='utf-8')
        for locale, values in translations.items():
            (assets / f'lang/{locale}.json').write_text(json.dumps(values), encoding='utf-8')
        (root / 'src/main/resources/fabric.mod.json').write_text(
            json.dumps({'description': 'Guogaology 果糕逻辑'}), encoding='utf-8')
        (root / 'tools').mkdir()
        (root / 'tools/copy_catalog.json').write_text(
            json.dumps({'translations': translations, 'removed': []}), encoding='utf-8')
        return root

    def test_literal_and_dynamic_entity_names_are_required(self):
        report = audit(self.fixture())
        self.assertEqual(report['entity_names'], 3)
        self.assertEqual(report['non_block_item_names'], 1)

    def test_existing_item_name_does_not_hide_missing_entity_name(self):
        with self.assertRaisesRegex(ValueError, r'Missing translation entity\.guogaology\.laver_boat'):
            audit(self.fixture(missing=('entity.guogaology.laver_boat',)))

    def test_new_wood_is_discovered_without_a_hardcoded_boat_list(self):
        with self.assertRaisesRegex(ValueError, r'Missing translation entity\.guogaology\.new_wood_boat'):
            audit(self.fixture(woods=('laver', 'dread', 'new_wood')))

    def test_unknown_dynamic_registration_fails_closed(self):
        with self.assertRaisesRegex(ValueError, 'Cannot resolve entity registration ID'):
            registered_entity_names({Path('NewEntity.java'): '''class NewEntity {
                void register() {
                    var id = GuogaologyMod.id(runtimeId);
                    Registry.register(BuiltInRegistries.ENTITY_TYPE, id, entity);
                }
            }'''})

    def test_target_override_entity_registrations_are_checked(self):
        root = self.fixture()
        path = root / 'ports/26.3/src/main/java/example/NewEntity.java'
        path.parent.mkdir(parents=True)
        path.write_text('''class NewEntity {
            void register() {
                Registry.register(BuiltInRegistries.ENTITY_TYPE,
                    GuogaologyMod.id("new_entity"), entity);
            }
        }''', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, r'Missing translation entity\.guogaology\.new_entity'):
            audit(root)


if __name__ == '__main__':
    unittest.main()
