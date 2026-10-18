# 0.6.1 — optional JEI client tag protection

The user reported two problems on a remote Minecraft26.3 server using0.6.0: axes did not accelerate Laver/Dread Fir timber, and the first Numeric Stone pickup disconnected the client while decoding `minecraft:recipe_book_add`. The user authorizes repairing the main Mod so JEI may be present or absent independently on each side. Work remains on `codex/0.6.1-tool-mining`, with the released baseline `1900d1e` preserved at `codex/backup-0.6.0-before-tool-mining`. No main merge, push or Release authorization.

## Diagnosis

The exact published26.3 JAR (`a101bfd94cdf5cb27646bba2b10f881665bad89e180e6e59cf67121cdf83fe39`) passes3,054 native mining/tool assertions on a dedicated server. Real loopback TCP controls identify JEI31.14.0.72's client-only fallback as the cause: without JEI both functions work; adding only client JEI reproduces both symptoms; adding the same JEI and MezzConfig0.6.8 to both sides restores both. The original disconnect and the control share `Optional.orElseThrow -> ByteBufCodecs$34.decode:734`, a missing named-tag lookup. The server's synchronized tags are complete. JEI's vanilla-only fallback replaces the active client's tags, removing `guogaology:number_stones` and the custom members of `minecraft:mineable/axe`.

Official source: <https://raw.githubusercontent.com/mezz/JustEnoughItems/26.3/Common/src/main/java/mezz/jei/common/recipes/VanillaClientRecipeLoader.java>. No JEI source is copied into this project.

## Repair

Only the26.3 client gains a separate optional Mixin configuration. `JeiTagCompatPlugin` enables the hook only when installed JEI metadata is exactly31.14.0.72. It never loads a JEI class during discovery. No JEI or MezzConfig dependency is added to the main Mod.

`JeiVanillaTagsMixin` redirects only the FIRST `TagLoader.loadTagsForExistingRegistries` call in `VanillaClientRecipeLoader.loadVanillaRecipeRegistry`, returning an empty pending-tag list. `buildUpdatedLookups` therefore reuses the already synchronized server lookups. The second call still builds and applies tags to JEI's new temporary WORLD registries. This preserves JEI's fallback recipe loading without mutating the live registry or retaining unbound replacement tag wrappers. The target/version and injection count are deliberately narrow; future JEI versions are not intercepted blindly.

No mining numbers, recipes, loot, saved IDs or network packet formats change. The compatibility code and its configuration must be absent from the three other Minecraft targets. An updated26.3 client is required; installing this client-only repair solely on a server cannot repair an old client.

The user subsequently explicitly requested repairing JEI's missing Numeric Pickaxe/Sword entries too. An unchanged0.6.0 two-sided-JEI control confirms this separate omission: JEI automatically synchronizes only `minecraft` recipe serializers, excluding `guogaology:numeric_tool`. This is unrelated to the shaped-recipe superclass or the original disconnection.

`RecipeSyncCompat`, a26.3-only main entrypoint AFTER `GuogaologyMod`, now registers the vanilla and Guogaology serializers through Fabric's public `RecipeSynchronization` API on both physical sides. Registration is idempotent with JEI. A complete set is essential: synchronizing only Numeric recipes would make JEI select an incomplete snapshot and omit vanilla recipes. Unconditional physical-side registration also supports an unmodified logical LAN host without JEI serving a JEI-enabled visitor. Supported clients receive a full recipe snapshot on normal login/data reload, including clients without JEI; no tick task or client-local JSON reconstruction is added. The data remains authoritative to the server and its data packs.

The full query repair requires the new main Mod on the server as well as the client; neither end requires JEI. The independently verified new-client/old0.6.0-server case retains the original tag/disconnection repair but cannot invent recipe data the old server does not publish.

## Validation and delivery

Independent static source and packaged-bytecode review passes. The Python suite has84 passing tests, including checks rejecting a JEI hard dependency, a server-loaded hook and leakage to other targets.

All four targets build and pass the final package audit. Recursive comparison against released0.6.0, including the nested1.21.1 bridge, finds only version metadata changes in the three other targets.26.3 adds exactly three compatibility classes, the optional configuration, its metadata entrypoints and the two extra client-only manifest entries. Artwork, game recipes, mining properties and all original classes are unchanged (`build/delta-061-final.json`).

Final26.3 SHA-256: `1cf73cbd73b65bb04870f4b0c0dc5d772573a3d486196acaacc2c8b2e5b0ea30`.

| JEI installation | Final independent run | Result |
|---|---|---|
| Neither end | `final061-none-a` | Pass |
| Client only | `final061-jei-client-a` | Pass |
| Server only | `final061-jei-server-a` | Pass |
| Both ends | `final061-jei-both-a` | Pass |

Each final same-version run verifies2,189 actual Fabric-synchronized recipes, including numeric tools and vanilla recipes; all457 block tags/18,953 memberships and523 item tags/5,561 memberships match the server fingerprints. Actual first ItemEntity pickup unlocks all three expected native recipe-book displays without disconnection. All seven axe materials have correct speeds for both custom woods and oak. When JEI is present, its real runtime queries the two Numeric tools, Numeric Stone→Cobblestone, sticks, Stone Pickaxe and charcoal, checking ingredients/outputs. Each Numeric input expands all ten digits. Crafting1,411/smelting77 entries remain available. The actual Numeric Pickaxe JEI page is shown, settled and captured; screenshots are visually inspected. All checks use the final production JAR, not a development classpath.

Every runtime test is a fresh, hidden, mouse-unlocked, loopback-only pair with nonempty listener evidence, clean JVM shutdown, closed ports and unchanged normal-instance inventory. Normal accounts, configurations and saves are not used as test worlds. Summary: ignored `build/network-qa263/final061-four-way-summary.json`; per-run evidence is in the directories named above.

A fifth run, `final061-jei-client-oldserver-a`, connects the FINAL new client to the exact released0.6.0 server, with JEI only on the client. It passes native pickup/unlocks, all tag fingerprints, seven axes and the three original vanilla JEI query checks. As expected that old server publishes no full Fabric recipe snapshot; Numeric tools are absent from its JEI query data. This verifies backward-compatible disconnection protection without claiming that updating only the client can supply missing authoritative recipes. Final five-run summary: `build/network263-final061-summary.json`; QA helper SHA-256 `dac32d548099d200beea6022cc5fd6a719e9aedc5320e23e8d60db5212b1e20a`.

The standalone final main Mod files are under `build/releases/0.6.1/`. Closed normalfo262 was updated under the standing authorization with a backed-up0.6.0 JAR, verified sole main Mod/SHA and232 unchanged protected hashes (other mods/configuration/normalfo263_2 mods). Its26.2 payload is version-only; no new26.2 runtime behavior was introduced. Normalfo263_2 was not replaced. Installation record: `build/install-061.json`. No GitHub publication this turn.

The isolated helpers under `ports/qa263-mining*` and `ports/qa263-network*` are opt-in source sets and must never enter production JARs. Original negative-control evidence is under ignored `build/network-qa263/release060-*`; final positive controls use `candidate061-*`.
