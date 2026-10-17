# 0.6.0 release

On 2026-10-18 the user canceled the three-jade preview and explicitly authorized renaming the current version to 0.6.0, merging into the real `main` branch, pushing and publishing a Release. This authorization is specific to this release; future pushes/releases still require the user's request.

The release preserves the 0.5.16 production design at `e0c37e9`, including the existing 64-state Astra woven-crown gradient. Actual26.3 testing subsequently exposed resource-codec and heightmap-tag compatibility gaps, repaired in the26.3 adapter before publication. The isolated jade preview under the parent outputs directory is not part of the Mod or the release source.

Backup: `codex/backup-0.5.16-before-0.6.0` at `e0c37e9`. Preparation branch: `codex/0.6.0-release`. At the start, local `main` was `69edcfd` and remote `origin/main` was an ancestor of the current version. Latest public Release was v0.5.3. Preserve Alice (AliceLdl)'s original 0.5.4.1 authorship and all license/provenance records.

Checklist:

- Bump the shared version and refresh current bilingual README/release notes without rewriting historical documents.
- Build and validate all four targets, run the current Python checks and a hidden disposable fo262 check of the final release JAR.
- Compare every release entry with0.5.16: three targets retain the already verified payload apart from version metadata, while26.3 additionally contains only the documented compatibility repairs.
- Merge into `main`, create the corresponding source archive/checksums and publish four matching JARs plus installation/license/provenance material.
- Verify the uploaded Release and GitHub four-target workflow. Installation remains gated on normal-game exit; the confirmed fo262 installation is recorded below. fo263_2 stays unchanged.

Validation completed before publication:

- All four0.6.0 targets build and pass the package audit (`build/build-060-all.log`, subsequent26.3 repair build and `build/package-060-final.log`). The initial74 Python tests passed; the final suite includes the added codec checks (`build/tests-060-final.log`). Resource and bilingual checks retain2,378 assets with no missing/orphan references and642 keys per locale without key/parameter errors.
- Recursive comparison of every uncompressed JAR entry, including the1.21.1 legacy bridge, confirms metadata-only changes for1.21.1/1.21.11/26.2.26.3 additionally has the documented compatibility repairs; JSON line-ending differences are tracked separately. Class files and artwork retain their content (`build/version-only-060.json`). No jade proposal or balance adjustment is included.
- The exact final26.2 release passes the hidden disposable fo262/Sodium/Voxy run (`build/runtime-060-a.log`):229,735 Astra,1,296 Laver,75,730 scenery and16 native physics/light checks. Three final screenshots were inspected. Markers are `ASTRA516_RUNTIME_OK` and `PRODUCTION_PORT_OK 26.2`. This run is a focused native test, not a new full survival, dedicated-server or1.21.1 Connector test; the additional26.3 checks are recorded below.
- Final 26.2 SHA-256: `2d04fff9dd08e960e04d5d940e0f88ac8f8ffafb19b72056325d7a6c6769bf18`. The packaged JAR is the same file used by the disposable test.

Publication targets and evidence:

- Real branch `main`; tag `v0.6.0`; public Release `https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.0`. The preparation branch is a descendant of both local and remote main, so the integration preserves the existing Alice merge and can fast-forward without discarding history.
- Export the complete source from the final main commit, retain both executable Gradle wrappers, and compare every archived file against its Git blob. The source archive must include the matching version, imported content, build tools, authorship and all license notices. Recompute checksums for every attached file, including the source archive.
- Final upload/source checks and GitHub workflow results are recorded in ignored `build/bundle-060-proof.json`, `build/release-060-remote.json` and the repository's four-target workflow, rather than changing the release commit after packaging.

After the user confirmed exit, installed0.6.0 into closed normal fo262. The previous0.5.16 JAR is backed up; exactly one main Mod and the tested SHA were verified (`build/install-060.json`). All55 other mods,129 configuration files and48 fo263_2 mod files remain unchanged. Normal saves were not opened.

The user then explicitly requested actual26.3 runtime testing as well, while forbidding any replacement of fo263_2. This overrides the earlier26.2-only runtime restriction for this release. All additional checks use fresh, hidden, mouse-unlocked disposable directories; no normal instance settings or saves are copied or modified.

26.3 findings and repairs:

- The isolated launcher initially omitted two JVM arguments present in the official26.3 metadata (`StackShadowPages=32` and the `jdk.internal.misc` export). Both no-main-Mod controls and production launches could exit natively. Using the official arguments resolved startup; no driver cause was established. No normal options/runtime were changed.
- Native registry loading rejected the legacy `{Name: ...}` BlockState targets in Ordinal Ore. The26.3 adapter now recursively converts feature states to native string or `id`/`properties` form. The source configuration and all three other targets are unchanged.
- Native26.3 motion heightmaps use block tags. Missing custom-solid memberships left Inner `MOTION_BLOCKING` maps empty despite real stone terrain, affecting portal site searches. An audited193-solid/2-actual-LeafBlock/31-nonmotion catalog covers all226 registered blocks; the26.3-only final overlay extends the existing base motion tag without replacing vanilla. The native tag hierarchy supplies its three dependent classifications. One existing vanilla pressure-plate inheritance difference is retained explicitly.
- The final Python suite has80 passing tests. Final package checks independently reject obsolete feature-state keys and inspect the exact emitted motion tag after all imported overlays.
- The final26.3 JAR SHA-256 is `a101bfd94cdf5cb27646bba2b10f881665bad89e180e6e59cf67121cdf83fe39`. The isolated Sodium0.9.2 run (`build/runtime-060-263-final-pass.log`) passes356 checks:28 core appearances/native GUI models,18 animated bodies actually submitted,226 default-state collision/tag comparisons, three generated dimensions with9FULL chunks and49 real column samples each, and return to Overworld. All three sampled motion maps are populated and WORLD_SURFACE agrees with direct voxels. Seven native screenshots are captured. This is a fresh-world smoke test, not a full portal ritual, survival playthrough, Voxy26.3 test or old-save migration.
- A second fresh26.3 run WITHOUT Sodium (`build/runtime-060-263-native-pass.log`) independently passes the same356 checks and captures another seven native screenshots using the same final production JAR. Both runners exit0 with `PRODUCTION_PORT_OK 26.3`; no special texture-filter workaround is used. The final package comparator permits exactly the ore-codec and motion-tag semantic changes plus51 JSON line-ending-only differences relative to0.5.16; all classes/art retain their content and the three other JARs are unchanged from their original0.6.0 checks.
- All seven Sodium screenshots were visually inspected, along with the native-renderer core close-up, Inner and Underworld scenes; no missing textures or obvious missing geometry were found. The normalfo263_2 before/after audit passes183 content hashes/sizes (48mods,133configs and its client metadata/JAR) plus356 saved-file paths/sizes/timestamps, with no new/missing/changed entries (`build/fo263-060-after-proof.json`). The baseline does not include top-level options.txt; the runner writes options only inside its disposable game directory. Normalfo263_2 was never replaced or opened by the tests.
