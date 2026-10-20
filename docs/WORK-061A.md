# 0.6.1a — isolated JEI compatibility backport

## Scope and status

On 2026-10-20 the user authorized a robust JEI compatibility repair based on current 0.6.7 for 0.6.8, plus a separate backport to the actual 0.6.1 source. The user then added plant item-display and wooden door/slab/sign bug fixes to both lines. This worktree is the backport only: `codex/0.6.1a-jei-compat`, directory `Guogaology-061a`.

The frozen structural guard is ported. The Mixin body and original RecipeSyncCompat remain unchanged. The additional wooden-product fixes use vanilla blocks, native SignItem where present, native SignBlockEntity and its existing text/save/renderer paths. No block, item or block-entity ID is added. Documentation and QA-only tools distinguish this source line from later releases.

The final expanded backport passes all 101 Python tests, all four offline builds, package checks and the exact artifact-change allowlist. The parent task's final 22-case native matrix also passes: this backport contributes 10 actual TCP cases, 20 verified first-pickup stages and 59 JEI recipe queries. Its targeted native item/wood checks pass 70 server cases and 56 rendered-model checks. The earlier JEI-only build was an intermediate snapshot, not the final package. The backport remains on its authorized local branch; no main merge, push or Release is authorized.

## Exact baseline

- Source: `0a890b94fb883bc30adc31653dd172d541348368` (0.6.1 installation and portal audit record).
- Backup reference: `codex/backup-0.6.1-before-061a-jei-compat`, pointing to the same commit.
- The earlier implementation commit is `372126e`; its difference to the selected source contains only `AGENTS.md`, `docs/WORK-061.md` and `docs/PORTAL-ROUTING-AUDIT-061.md`. No production source or resources changed between them.
- The four local published 0.6.1 JAR hashes were recalculated and match their original manifest and embedded version. The 26.3 hash also matches the original `docs/WORK-061.md` native-test record.

| Minecraft | Exact 0.6.1 SHA-256 |
|---|---|
| 1.21.1 | `f3c7b09d090d5dc040e785ea1f704daa60bd96cf514ea5aa702a967f4b30abae` |
| 1.21.11 | `f6d1fb2f672547071f97704bdb14baaf1b4094bad42fd5174259b0799ddbf98b` |
| 26.2 | `a35b23860f654f79acb7b44daa54d02e38827344695bdcc5ff919f0270e681fc` |
| 26.3 | `1cf73cbd73b65bb04870f4b0c0dc5d772573a3d486196acaacc2c8b2e5b0ea30` |

Original 26.3 compatibility class bytes, before this backport:

| Class | SHA-256 |
|---|---|
| `dev/guogaology/client/compat/jei/JeiTagCompatPlugin.class` | `9a676566ef6c0654fe783d3a8d17327e779d0ec95026aa9de51772b881407cae` |
| `dev/guogaology/client/compat/jei/JeiVanillaTagsMixin.class` | `c935267365c212db49ae7984fd2ef55127b35f430b138c5cb5623469c2039841` |
| `dev/guogaology/compat/RecipeSyncCompat.class` | `57e075064ab9f6db43a9eb579ddb1cd68470b2c4870561deb25b99f1cf93029f` |

The original source and JARs are provenance evidence, not a claim that a new build has already reproduced their bytecode. Later portal, blueprint, Absence Core, material balance, XP/UI and loot-conversion changes must not be imported into this branch.

## Minimal repair

The plugin reads JEI class bytes through Mixin's bytecode provider without loading JEI classes. Instead of matching one version string, the matcher verifies the exact fallback descriptor and the provenance of the active registry, temporary WORLD registry, both pending-tag lists, lookup builders and apply consumers. Unknown structures are skipped with a warning. Before injection it repeats the check against the actual target ClassNode; unexpected transformation drift is rejected rather than redirecting an unverified call.

The original redirect still bypasses only the first live-registry tag load. The temporary WORLD-registry load remains unchanged. The matcher rejects additional uses or escapes of pending lists, including lambda capture, fields/arrays, merged sources and incompatible concrete-type casts. The independently reviewed implementation passed the main worktree's pure ASM checks on 14 actual JEI artifacts and 23 synthetic cases. Packaged native checks are recorded separately below.

Frozen source SHA-256, verified again during the byte-for-byte copy:

| Source | SHA-256 |
|---|---|
| `JeiTagCompatPlugin.java` | `1f9bcfb8674751deae3fe3cc91b29c9aa447e2a4762f447e86c0e1d91bcac36d` |
| `JeiTagCompatShape.java` | `ed513f97b9ef80480930bff3033c09a78efd63101fd244154320e8e0e6a22fe7` |

## Additional item-display and native wooden-product fixes

- Eight existing plant/jar item wrappers now use native block-item ground, hand and fixed transforms; dropped-item scale is 0.25. Their existing GUI transform, world models and textures remain unchanged. Modern item selectors reference the wrapper so old-target imports cannot replace it with a bare block model.
- Laver Door's two halves previously both had an unconditional self-drop. Its host table now permits only `half=lower`, matching native doors. Dread Door already had that rule; native upper/lower-break controls confirm its single-item behavior is retained.
- Laver double slabs previously lost one of the two placed items. Their native `type=double` loot now returns two; top/bottom slabs return one. Existing Dread rules remain unchanged. A shared source generator and final donor-overlay/package guards prevent restoration of the bad tables.
- Both standing and wall sign blocks join the native SIGN entity's valid block set. 1.21.1/1.21.11 use Fabric `BlockEntityType.SIGN.addSupportedBlock`; 26.2/26.3 use `BlockEntityTypes.SIGN.addValidBlock`. The older three engines use native SignItem for automatic editing. 26.3 removed that item class and opens the editor through SignBlock.setPlacedBy, so its ordinary standing/wall item remains appropriate.
- 1.21.1/1.21.11 do not have the modern sign-board block templates. Their generated models are native-style particle-only models and the existing 32x32 artwork is rearranged, without scaling, into the native 64x32 entity atlas. 26.2/26.3 models/artwork remain unchanged. No custom renderer or text/save schema is introduced.
- Only four sign block IDs and two sign items exist. Neither line registers Laver/Dread hanging signs; this fix does not add them.

The six door/slab and three sign-resource tests pass in both worktrees. The backport's pre-existing inline 26.3 loot converter was lifted verbatim to a public function for the new host overlay; AST identity was asserted. It does not import 0.6.7's later TagEntry conversion or other gameplay.

The added sign methods exist at every declared Fabric API minimum: `addSupportedBlock` in 0.102.0+1.21 and 0.141.6+1.21.11, and `addValidBlock` in 0.153.0+26.2 and 0.161.0+26.3. The exact 0.102.0 official Maven JAR was inspected in memory, including the registered Mixin implementation and interface-injection metadata; the modern minimum JARs were inspected from the local cache. This agrees with the [official 0.102.0 API documentation](https://maven.fabricmc.net/docs/fabric-api-0.102.0%2B1.21/net/fabricmc/fabric/api/object/builder/v1/block/entity/FabricBlockEntityType.html). No dependency floor was raised; this is static API evidence, not an old-API game run.

## Superseded JEI-only intermediate snapshot

Before the user added the item-display and wooden-product fixes, an intermediate JEI-only snapshot passed four builds and 90 Python tests. Its logs are `build/backport-061a-build-all.log`, `build/backport-061a-python-tests.log` and `build/jei-only-delta-061a.json`; `build/061a-delivery.json` records its obsolete hashes. Those files describe that intermediate phase only. The final release directory has been replaced with the expanded-scope artifacts listed below.

The final scope also includes native sign registration, eight item wrappers, the Laver door/slab tables and old-target sign resources. It must not be described as only three JEI classes or version metadata.

## Final expanded-scope build

The final frozen sources passed `python -X utf8 -m unittest discover -s tools/tests -p 'test_*.py'`: 101 tests. The coordinated offline build then passed on all four targets, including the legacy bridge, native sign API compilation, final door/slab loot checks, sign model/texture checks, resource/localization checks and production-package QA exclusions. The initial sandbox attempt could not connect to its local Gradle daemon; the same offline build succeeded with local loopback access. No game was launched by this worktree's build.

JDK 21 is Android Studio's `jbr`; JDK 25 is the launcher's `java-runtime-epsilon`. The build uses the worktree's Gradle 8.13 wrapper for 1.21.1 and workspace Gradle 9.6.0 for the modern targets, with the shared `.gradle-user` cache. Gradle was not run concurrently with the parent task. Log: `build/backport-061a-final-build-all.log`.

| Minecraft | Final 0.6.1a SHA-256 |
|---|---|
| 1.21.1 | `736f7bc718f73e0860eb029e151e3cb93dfa5cb4b5454e943059c8b69453a782` |
| 1.21.11 | `8647f7d5104f8131e69ac22cd65aca9e6ec7cd195b4279e51bfb5b1c4c222e58` |
| 26.2 | `4450f0c1cd51ad51eac47a1ae3a9ebd18f8c6d059e16e8f8721c562acb14e942` |
| 26.3 | `fc9863898c01eda5a6e6f00070a850f7064fec9797c708febc6229de025637f5` |

All 12 release files were copied without alteration to the parent worktree's `build/releases/0.6.1a/`; every copied file's SHA-256 was compared with its source. Both worktrees contain `build/061a-final-delivery.json` with the complete copied-file inventory. These final hashes supersede the initial JEI-only delivery. No installation or publication is implied by build success.

## Final artifact-change audit

The parent task ran `tools/audit_068_repairs.py` against exact published 0.6.1 and these final 0.6.1a JARs. Every reviewed entry exactly matches the allowlist, including the nested legacy JAR. The same per-target counts passed independently for the 0.6.7 to 0.6.8 line. Non-sign world models, source textures, recipes, IDs, loot outside the Laver door/slab fix and unrelated Java classes remain unchanged. Development QA is excluded from production packages.

| Minecraft | Reviewed changed entries | Separate formatting-only JSON entries |
|---|---:|---:|
| 1.21.1 | 30 | 37 |
| 1.21.11 | 23 | 37 |
| 26.2 | 11 | 37 |
| 26.3 | 14 | 39 |

Version metadata is normalized before comparison. Formatting-only entries are recorded separately and require parsed JSON equality. The 37 shared entries are the earlier CRLF/LF-only mesh/model files; 26.3 additionally reformats the existing Dread door/slab tables without changing their loot semantics. No source assets or built JARs were rewritten to conceal these differences. Final evidence is in the parent worktree's `build/jei-068/final-delta-061a.json` and `build/jei-068/final-delta-068.json`.

## Final native validation

The parent task completed 22/22 final Minecraft 26.3 loopback cases across both release lines: 44 verified first-pickup stages and 144 actual JEI recipe queries, with no pending cases. The independent `tools/audit_jei_runtime_matrix.py` audit passes. This backport's 10/10 cases contribute 20 first-pickup stages and 59 queries; each case picks up a branch stone and then Numeric Stone through actual ItemEntity pickup and the native TCP recipe-display path. Recipes are not directly awarded and packets are not mocked. Across these cases all six branch materials are exercised.

| Server Mod | JEI placement/version | First branch | JEI queries | Fallback confirmed |
|---|---|---|---:|---|
| 0.6.0 | Both / 86 | phi | 8 | No |
| 0.6.0 | Client / 72 | epsilon | 6 | Yes |
| 0.6.0 | Client / 86 | epsilon | 6 | Yes |
| 0.6.0 | None | epsilon | 0 | No |
| 0.6.0 | Server / 86 | epsilon | 0 | No |
| 0.6.1 | Client / 86 | epsilon | 13 | No |
| 0.6.1a | Both / 86 | psi | 13 | No |
| 0.6.1a | Client / 86 | great omega | 13 | No |
| 0.6.1a | None | omega | 0 | No |
| 0.6.1a | Server / 86 | zeta | 0 | No |

All ten use the final 26.3 JAR SHA above, Loader 0.20.0 and Fabric API 0.163.0+26.3. The actual JEI artifacts are 31.14.0.72 and 31.16.0.86. The 14-artifact static ASM set also contains 71 and 75; those are not native runs in this final matrix. The two client-only 0.6.0 cases explicitly reach and pass JEI's fallback. Complete block/item tag fingerprints remain server-authoritative, and real JEI queries validate usable recipe data. Old-server controls retain the old server's recipe availability; updating the client does not add recipes the server never synchronizes.

The backport's no-JEI case also passes 70 real placement/destruction checks and 56 native item-model checks with zero issues. Twenty sign rows cover native `minecraft:sign` support, automatic editing permission, front/back native text updates and compressed NBT disk round trips; a real remote SignEditScreen is observed. Door/slab break controls conserve item counts. A real Graham block break drops exactly one ItemEntity; native ground scale is 0.25, first-person 0.4, third-person 0.375 and GUI 1. Two final native screenshots were inspected. Earlier observe-only controls reproduce the unfixed Laver door/slab and invalid-sign defects while confirming Dread Door was already correct; those intermediate controls are excluded from the 22 final passes.

Final runtime helper SHA-256 is `45a1376ab382be6a92687c952e01c2192eace450f25421f2a30d2925d01d5111`. Evidence in the parent worktree: `build/jei-068/runtime-summary.json`, `build/network-candidate-matrix/output-final-d/summary.json` and `build/network-candidate-matrix/final-postcheck.json`. Every final runner, client and server exits with code 0; source/input hashes and normal-instance protection checks pass, and all local listening ports close after shutdown. Interrupted startups, deliberate pause guards and earlier intermediate candidates are not counted as successful final cases.

Games use separate hidden, mouse-unlocked disposable directories, offline QA identity and `127.0.0.1` only. Normal accounts, configurations and saves are not test inputs. This backport's new runtime evidence is Minecraft 26.3; 1.21.1, 1.21.11 and 26.2 have build/package/static coverage only in this backport task. No Connector or remote user-server run is claimed. The local source commit and matching archive are recorded in the final release directory's `SOURCE.txt`; no normal installation, main merge, push or Release publication is part of this backport task.
