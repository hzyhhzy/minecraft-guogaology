# 0.7.1 — native mining-pattern input repair

The user authorizes repairing the 26.3 mining-pattern window, committing, pushing and publishing the fix. Preparation uses `codex/0.7.1-pattern-input-fix` from the completed observation-only audit `01aaa7f`. Released main `46fd287` is preserved as `codex/backup-0.7.0-before-pattern-input-fix`. No unrelated backpack work is included.

## Confirmed baseline and repair

The exact published 26.3 / 0.7.0 JAR reproduces five failed native actions: Escape, inventory-key exit, left-click Back, left-click selection, and right-click reference selection. Old GLFW numeric controls and server-forced closing work. Ten real Button entry controls succeed, and ten ownership snapshots retain exactly one pick and one installed core. Full evidence and limits are in `docs/AUDIT-070-PATTERN-WINDOW.md`.

Only the two maintained `EnhancementScreen` sources change production behavior:

- Modern targets use native `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`, `KeyEvent.isEscape()` and `keyInventory.matches(event)`.
- The 1.21.1 source uses named GLFW constants and the native configured inventory-key matcher.
- The first exit returns from the modal editor to the existing enhancement page. Subsequent exit uses the original container closing path.

No changes to server menus, ownership, networking, item/block identities, codecs, mining budgets/default masks, materials, world generation, recipes, localization or assets are intended. This includes the existing fffZ/FOS/ψ/Z stability rules and all old item readers.

## Validation plan and evidence

Four release targets build and pass standard package checks. `tools/audit_pattern_input_071_packages.py` fixes the four 0.7.0 baseline hashes and compares every production entry. Only `EnhancementScreen.class`, exact Fabric release metadata, and the 1.21.1 embedded bridge's version metadata/path change; the bridge's actual classes remain unchanged. Ten vanilla wooden-mechanism block/item tag JSON files additionally change CRLF to LF after existing generators/tests rewrite working-tree line endings. The audit allows only the exact ten named paths and the exact CRLF-to-LF byte substitution, with before/after SHA recorded; it rejects content, key ordering or array ordering changes. Every other resource/data byte is unchanged. The initial strict audit rejected these line endings before this bounded nonsemantic exception was verified.

Final native QA uses the exact final 0.7.1 packages, true production screen input handlers, real menu packets and server-queue snapshots in fresh disposable worlds. 26.3 is primary; 26.2 is the focused regression. Both use hidden, unfocusable, mouse-unlocked windows and read only normal launcher libraries/selected mods. Normal accounts, settings and saves are not copied or modified, and no world is opened to LAN.

All 145 existing Python tests pass. Build log: `build/release-071/build-four.log`. Four-target strict delta: `build/release-071/package-delta.json`. The historical 0.7.0 audit is retained unmodified. No old numeric input is accepted as a positive substitute for real native actions.

The first 26.3 candidate run (`pattern-window071-263-final-a`) correctly returns with the native Esc and acknowledges the server state, then stops on an invalid QA equality oracle: Java component equality compares the freshly decoded nested `ItemStack` list by object identity. The final helper instead compares registry-aware, complete `ItemStack.CODEC` NBT trees before and after real compressed-file I/O, with explicit item identity/count and nested-core/pattern checks. It normalizes or ignores no field, and stores both complete structures alongside each sample. Production JARs remain unchanged; the failed attempt is not counted as a passing run.

The initial 26.2 attempt (`pattern-window071-262-final-b`) stops before creating a world because the new safety assertion detects a shown GLFW window. Native `Minecraft` calls `glfwShowWindow` after the older helper's hidden creation hint. A QA-only redirect now suppresses that second show call, preserving the explicit human-review opt-in. The final run continuously verifies both invisibility and lack of focus, as well as the existing mouse-grab cancellation. This test-tool repair is excluded from all production JARs; the aborted attempt is not certified as a hidden run.

| Target | Final native directory | Checks | Cases | Real Button entries | Server acknowledgements | Complete NBT disk samples | Protected normal files unchanged |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 26.3 | `build/runtime-qa263/pattern-window071-263-final-b` | 660 | 16 | 19 | 59 | 16 | 50 |
| 26.2 | `build/runtime-qa/pattern-window071-262-final-c` | 660 | 16 | 19 | 59 | 16 | 58 |

Both final suites pass: native Esc and inventory-key exit, Back, left selection/deselection, right reference changes including a selected reference, Clear, Reset, reference-cell no-op, middle-button no-op, rejection beyond the four-cell budget, server close, reinitialization followed by Back, repeated real Button entry, and edited-pattern reentry. Every case closes into the actual player's inventory with exactly one pick, one installed core and no loose core, then preserves the complete item through registry-aware native compressed NBT I/O. Each action verifies the same menu ID, empty cursor, exact reference/mask and server acknowledgement. The 16 authored item samples are not a complete historical-world upgrade test.

All six native screenshots were reviewed: the editor's initial custom pattern, empty state after Clear and altered reference/selection after reentry in both versions. They illustrate actual production rendering; closing behavior is proved by the recorded client/server states. Final normal-instance mod and launcher hashes are unchanged. No normal save, account or configuration was copied or opened for QA.

| Target | Final JAR SHA-256 |
| --- | --- |
| 1.21.1 | `e8d9327c05c56af3cdd605bbf02d024e3992267c1bf6d906c59988070605b836` |
| 1.21.11 | `587e737db4d15312da6bf8e605cc2b87c2522aefa48552d60ff635c2c9411bfc` |
| 26.2 | `3c5032e09634cadf71ce719836a3c47c79663914775cb5d6bced97d581f752f1` |
| 26.3 | `4bc61f602a6cd5f66b089ced6126446370b0c80bb347d692ce478322b106b741` |

## Publication

After validation, commit the final sources/documentation and publish v0.7.1 with four exact tested binaries and complete source exported from the exact tagged Git blobs, including generators, ports, QA helpers, license notices and provenance. Verify remote refs, exact release notes and every uploaded attachment hash. Do not force-update any existing release/tag. Publication permission applies only to this fix.

The release is prepared for a fast-forward into actual `main` and an atomic non-force main/tag push. Local publication evidence is kept under `build/release-071/`; the immutable tagged source and published assets identify the exact final source commit. Normal fo263_2 is not replaced. Any standing fo262 installation happens only after QA completes and a successful process check confirms the normal game is closed; it never gates the GitHub publication.
