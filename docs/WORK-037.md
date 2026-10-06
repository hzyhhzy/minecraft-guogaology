# 0.3.7 — wood families, enhancement tables and absence cores

## Authorized scope

- [x] Remove the Christmas/underworld Christmas custom wood series and obsolete stone conversion recipes. Keep Christmas-tree scenery with vanilla wood and existing scenic foliage.
- [x] Remove both Loquat carpentry families and replace their trunks/products with vanilla oak. Preserve Loquat foliage, saplings and their fruit drops as requested.
- [x] Add a separate Dread Fir plank/product family. All Dread Fir wood products are nonflammable; other wood families are flammable.
- [x] Rework the advanced/ultimate enhancement-table recipes using the previous table and eight universal ordinal crystals (Lv1/Lv2), without biome-specific materials. Replace the appearance of all three tables.
- [x] Reshape small Outer-world Christmas trees into layered conical Christmas-tree crowns without changing height.
- [x] Rework compute-chip crafting into server racks and terminals, preserving a non-duplicating harvest/unpacking loop.
- [x] Rename numeric tools to 数字石镐 / 数字石剑 (Numeric Stone Pickaxe / Sword).
- [x] Reconstruct all three absence-core grades using the original C1 broken ghostly material. Grade I has one main face; grade II has independently moving rings and a pole; grade III has nested interiors and the existing outer silhouette.
- [x] Audit all 28 core models for real same-part coplanar overlap; retain deliberate readable two-sided sheets and moving-part independence.
- [x] Verify source/resource generation, bilingual wording, crafting, wood flammability and core animation.
- [x] Verify static LOD appearance through the installed Voxy baker.
- [x] Build all four Minecraft targets. Run the actual client only in isolated hidden fo262.
- [x] Install the main mod when normal Minecraft is closed, backing up the old JAR and verifying hashes and the unique main mod.

## Implementation and validation

The complete checklist is implemented, validated and installed. Existing 0.3.6 changes are preserved. No old-save migration or GitHub publication is included.

The Loquat foliage and saplings are retained independently of the removed carpentry. Both tree types have vanilla oak trunks and their own leaves; fruit still drops from leaves. Dread Fir uses its own plank/product textures, fire-safe blocks/items/boat entity and the nonflammable-wood tags; its old charcoal recipe is removed. Other woods retain normal fire behavior. The 26.3 stripping adapter uses Fabric BlockTransformerHelper; the earlier targets retain their existing stripping API.

Advanced and ultimate tables respectively surround the previous table with eight universal ordinal Lv1/Lv2 crystals. Server racks cost four iron, four chips and one redstone; terminals cost three glass, two iron, two chips and two redstone. Each machine unpacks into two chips, so crafting and dismantling cannot multiply chips. The three tables use dedicated static block models and continue to open the existing ranked interface.

The original C1 model and original palette are stored as canonical art inputs. Lv1 keeps one main inscription layer. Lv2 uses two rings plus an independently moving pixel-stair slash sampled from that same inscription. Lv3 adds the smaller nested set and refines its outer arcs from three to ten segments; the outer arcs stay smooth per the latest instruction. The redundant mist box is removed, and the static frame uses a volume union. A shared offline sanitizer repairs actual co-planar overlap and crossed beam faces while preserving material interpolation and explicit two-sided sheets; the same source meshes feed the Voxy static fallback after its UV regeneration.

Outer Christmas trees keep their original trunk height and placement distributions. Only their tree crown changes to three narrowing conical skirts; the LHO sparse placement shares this feature. Inner and Underworld tree generators retain their existing shapes.

The final triangle-level audit and an independent GEOS cross-check cover all 28 actual meshes. They find no substantive same-part, same-direction overlap, including no fixed-shell overlap. Normal reverse-facing readable sheets remain intact. The report separately records source-coordinate rounding slivers (maximum area 1.742e-8 block squared), rather than describing them as exact geometric zero. The art generators reproduce the cleanup, and all 28 meshes are idempotent under a second pass. Fourteen Python regression tests pass, including material/NBT mappings, cross-version templates, overlap clipping, UV preservation and readable backsides. Resource and language audits report 2,179 reachable assets, 364 keys per language, no missing references, no orphans and no format mismatch. Local audit evidence is in `build/core-surface-audit/`.

All four binaries and source JARs build and pass the release verifier, including the 1.21.1 source bridge, shared assets, licenses, current namespace and absence of the QA helper. `maintenance-037-a` runs the actual packaged 26.2 Mod with Fabric API and Sodium in a hidden disposable fo262 copy. It passes 203 mechanics/template checks, 434 material checks, 145 maintenance checks and 25 conifer checks, plus the existing coast/air-gap/plain-gummy regression. Actual tree samples retain trunk heights 7/8/9/10 and total heights 9–12. All three enhancement menus open at the correct rank; real loaded recipes, vanilla plank compatibility, leaf fruit drops and Dread Fir fire/fuel/boat rules pass. Screenshots `merge-9`, `merge-12`, `merge-11` and `merge-8` were inspected for both C1 animation phases, the new stations/woods, and conical crowns. The client saves and exits successfully.

The new recipe catalog is rebuilt from the four release JARs, with 128 recipes, no duplicate input groups, no unresolved names, and equal recipe content across targets. Local HTML: `outputs/20261006-recipes-037/合成配方总表.html` in the parent workspace.

`voxy-037-a` uses the user's installed Voxy with Fabric API and Sodium in another disposable hidden copy. It checks 204 blocks, 2,676 states and 2,261 distinct models; all 28 core grades have visible six-direction bakes. No empty model or invisible model variant is reported. The nearby screenshot was inspected and the client saves and exits successfully. Both runtime copies use the exact release hash.

Installation completed after checking that Minecraft was closed. The normal `fo262/mods` contains one main Mod, `googology-dimension-26.2-0.3.7.jar`, SHA-256 `ce66c432916bc5b7ba9f20da2184040526f27796ca8c3c097bf9ed6a47e7bc43`. The previous 0.3.6 JAR is backed up in `build/backups/fo262-before-0.3.7/`. Hashes of the other 55 files are unchanged; normal saves and configuration are untouched.
