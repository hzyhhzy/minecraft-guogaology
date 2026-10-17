# 0.6.0 release

On 2026-10-18 the user canceled the three-jade preview and explicitly authorized renaming the current version to 0.6.0, merging into the real `main` branch, pushing and publishing a Release. This authorization is specific to this release; future pushes/releases still require the user's request.

The release preserves the complete 0.5.16 production behavior at `e0c37e9`, including the existing 64-state Astra woven-crown gradient. The isolated jade preview under the parent outputs directory is not part of the Mod or the release source.

Backup: `codex/backup-0.5.16-before-0.6.0` at `e0c37e9`. Preparation branch: `codex/0.6.0-release`. At the start, local `main` was `69edcfd` and remote `origin/main` was an ancestor of the current version. Latest public Release was v0.5.3. Preserve Alice (AliceLdl)'s original 0.5.4.1 authorship and all license/provenance records.

Checklist:

- Bump the shared version and refresh current bilingual README/release notes without rewriting historical documents.
- Build and validate all four targets, run the current Python checks and a hidden disposable fo262 check of the final release JAR.
- Confirm 0.6.0 changes only version metadata relative to the already verified 0.5.16 runtime payload.
- Merge into `main`, create the corresponding source archive/checksums and publish four matching JARs plus installation/license/provenance material.
- Verify the uploaded Release and GitHub four-target workflow. Normal fo262 is running at preparation time; do not replace its JAR while the game is open. fo263_2 stays unchanged.

Validation completed before publication:

- All four final 0.6.0 targets build and pass the complete package audit (`build/build-060-all.log`). The 74 Python tests pass (`build/tests-060.log`); resource and bilingual checks retain 2,378 assets with no missing/orphan references and 642 keys per locale without key/parameter errors.
- Recursive comparison of every uncompressed JAR entry, including the 1.21.1 legacy bridge, confirms that only version metadata and the corresponding bridge filename/reference change from 0.5.16. All classes, game assets and game data are byte-identical (`build/version-only-060.json`). No jade proposal or other gameplay adjustment is included.
- The exact final 26.2 release passes the hidden disposable fo262/Sodium/Voxy run (`build/runtime-060-a.log`): 229,735 Astra, 1,296 Laver, 75,730 scenery and 16 native physics/light checks. Three final screenshots were inspected. Markers are `ASTRA516_RUNTIME_OK` and `PRODUCTION_PORT_OK 26.2`. This is a focused native test, not a new full survival, dedicated-server, 1.21.1 Connector or other-version runtime run.
- Final 26.2 SHA-256: `2d04fff9dd08e960e04d5d940e0f88ac8f8ffafb19b72056325d7a6c6769bf18`. The packaged JAR is the same file used by the disposable test.

Publication targets and evidence:

- Real branch `main`; tag `v0.6.0`; public Release `https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.0`. The preparation branch is a descendant of both local and remote main, so the integration preserves the existing Alice merge and can fast-forward without discarding history.
- Export the complete source from the final main commit, retain both executable Gradle wrappers, and compare every archived file against its Git blob. The source archive must include the matching version, imported content, build tools, authorship and all license notices. Recompute checksums for every attached file, including the source archive.
- Final upload/source checks and GitHub workflow results are recorded in ignored `build/bundle-060-proof.json`, `build/release-060-remote.json` and the repository's four-target workflow, rather than changing the release commit after packaging.

Local installation is deferred while normal fo262 is running, following the user's standing rule. It remains on 0.5.16 until closed; no normal saves, configurations, other mods or fo263_2 files are changed by publication.
